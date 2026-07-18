package com.placer.firewatch.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.WriteBatch
import com.placer.firewatch.responder.apply.ApplicationStatus
import com.placer.firewatch.responder.apply.ResponderApplication
import com.placer.firewatch.user.ResponderStatus
import com.placer.firewatch.user.UserRole
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Admin-only: lists pending responder applications and approves/rejects
 * them. Approval atomically promotes the matching users/{uid} doc to
 * role=responder, responderStatus=approved — see firestore.rules for the
 * isAdmin() gate that makes this possible only for admin accounts.
 */
class AdminApplicationRepository {

    companion object {
        private const val TAG = "AdminApplicationRepo"
        private const val APPLICATIONS_COLLECTION = "responder_applications"
        private const val USERS_COLLECTION = "users"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun listenForPendingApplications(
        onUpdate: (List<ResponderApplication>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(APPLICATIONS_COLLECTION)
            .whereEqualTo("status", ApplicationStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Application listener error", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val applications = snapshot?.documents?.mapNotNull { it.toObject(ResponderApplication::class.java) }
                    ?: emptyList()
                onUpdate(applications)
            }
    }

    suspend fun approve(uid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.update(
                firestore.collection(APPLICATIONS_COLLECTION).document(uid),
                "status", ApplicationStatus.APPROVED
            )
            batch.update(
                firestore.collection(USERS_COLLECTION).document(uid),
                mapOf("role" to UserRole.RESPONDER, "responderStatus" to ResponderStatus.APPROVED)
            )
            commitBatch(batch)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to approve $uid", e)
            Result.failure(e)
        }
    }

    suspend fun reject(uid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.update(
                firestore.collection(APPLICATIONS_COLLECTION).document(uid),
                "status", ApplicationStatus.REJECTED
            )
            batch.update(
                firestore.collection(USERS_COLLECTION).document(uid),
                mapOf("responderStatus" to ResponderStatus.REJECTED)
            )
            commitBatch(batch)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject $uid", e)
            Result.failure(e)
        }
    }

    private suspend fun commitBatch(batch: WriteBatch): Unit =
        suspendCancellableCoroutine { cont ->
            batch.commit()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
