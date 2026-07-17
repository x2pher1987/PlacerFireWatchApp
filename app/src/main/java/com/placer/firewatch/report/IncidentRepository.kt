package com.placer.firewatch.report

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Read/update side of fire reports, for the responder dashboard. Requires
 * the signed-in account's UID to have a matching responders/{uid} document
 * in Firestore — see firestore.rules and the README's responder setup
 * section — otherwise the listener below fails with a permission error.
 */
class IncidentRepository {

    companion object {
        private const val TAG = "IncidentRepository"
        private const val COLLECTION = "fire_reports"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Live-updating incident feed, newest first. Call the returned
     * [ListenerRegistration.remove] (e.g. in onStop) to stop listening.
     */
    fun listenForIncidents(
        onUpdate: (List<Incident>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Incident listener error", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val incidents = snapshot?.documents?.mapNotNull { it.toObject(Incident::class.java) }
                    ?: emptyList()
                onUpdate(incidents)
            }
    }

    suspend fun updateStatus(reportId: String, newStatus: String): Result<Unit> {
        return try {
            setStatus(reportId, newStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status for $reportId", e)
            Result.failure(e)
        }
    }

    private suspend fun setStatus(reportId: String, newStatus: String): Unit =
        suspendCancellableCoroutine { cont ->
            firestore.collection(COLLECTION).document(reportId)
                .update("status", newStatus)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
