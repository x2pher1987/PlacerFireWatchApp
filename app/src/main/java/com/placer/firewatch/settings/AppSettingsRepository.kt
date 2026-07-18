package com.placer.firewatch.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A single app-wide settings doc at app_settings/config. Currently just
 * the BFP emergency call number (Section 10) — any signed-in user can
 * read it, only an admin can write it (see firestore.rules).
 */
class AppSettingsRepository {

    companion object {
        private const val TAG = "AppSettingsRepository"
        private const val COLLECTION = "app_settings"
        private const val DOCUMENT = "config"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /** Null if the doc doesn't exist yet, or the read failed (e.g. offline) — callers should fall back to a local default. */
    suspend fun getBfpNumber(): String? =
        suspendCancellableCoroutine { cont ->
            firestore.collection(COLLECTION).document(DOCUMENT).get()
                .addOnSuccessListener { snapshot -> cont.resume(snapshot.getString("bfpNumber")) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Could not load app settings", e)
                    cont.resume(null)
                }
        }

    suspend fun setBfpNumber(number: String): Result<Unit> {
        return try {
            setDocument(number)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Could not save app settings", e)
            Result.failure(e)
        }
    }

    private suspend fun setDocument(number: String): Unit =
        suspendCancellableCoroutine { cont ->
            firestore.collection(COLLECTION).document(DOCUMENT)
                .set(mapOf("bfpNumber" to number))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
