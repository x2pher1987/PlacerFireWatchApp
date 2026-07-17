package com.placer.firewatch.report

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.placer.firewatch.util.Prefs
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Writes one-tap fire reports to Firestore, uploading the attached photo
 * (if any) to Storage first so its download URL can be included in the
 * same document write.
 *
 * Requires a real app/google-services.json from your own Firebase project
 * (Firestore + Storage enabled) to actually reach a backend — see the
 * README's "One-tap fire reporting (Firebase setup)" section. The repo
 * ships a placeholder file so the project still compiles and builds
 * without one.
 */
class FireReportRepository {

    companion object {
        private const val TAG = "FireReportRepository"
        private const val COLLECTION = "fire_reports"
        private const val STORAGE_FOLDER = "fire_report_photos"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    suspend fun submit(context: Context, draft: FireReportDraft): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION).document()
            val photoUrl = draft.photoUri?.let { uploadPhoto(docRef.id, it) }

            val data: Map<String, Any?> = mapOf(
                "latitude" to draft.latitude,
                "longitude" to draft.longitude,
                "timestamp" to FieldValue.serverTimestamp(),
                "userId" to Prefs.getOrCreateUserId(context),
                "photoUrl" to photoUrl,
                "note" to draft.note.ifBlank { null },
                "status" to "Pending"
            )

            setDocument(docRef, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit fire report", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadPhoto(reportId: String, uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            val ref = storage.reference.child("$STORAGE_FOLDER/$reportId.jpg")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    ref.downloadUrl
                }
                .addOnSuccessListener { url -> cont.resume(url.toString()) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    private suspend fun setDocument(docRef: DocumentReference, data: Map<String, Any?>): Unit =
        suspendCancellableCoroutine { cont ->
            docRef.set(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
