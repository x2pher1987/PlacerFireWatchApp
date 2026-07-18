package com.placer.firewatch.responder.apply

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** What the user has assembled in the application form, before it's submitted. */
data class ResponderApplicationDraft(
    val fullName: String,
    val birthdate: String,
    val address: String,
    val barangay: String,
    val contactNumber: String,
    val email: String,
    val governmentIdUri: Uri,
    val barangayCertificationUri: Uri,
    val selfieUri: Uri,
    val emergencyContact: String,
    val bloodType: String,
    val occupation: String,
    val reason: String
)

/**
 * Writes a responder application to Firestore at responder_applications/{uid}
 * (one per account — resubmitting overwrites), uploading the 3 required
 * documents to Storage first. Approval is a manual admin action (see
 * AdminHomeActivity) that promotes the matching users/{uid} doc.
 */
class ResponderApplicationRepository {

    companion object {
        private const val TAG = "ResponderApplicationRepo"
        private const val COLLECTION = "responder_applications"
        private const val STORAGE_FOLDER = "responder_applications"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    suspend fun submit(draft: ResponderApplicationDraft): Result<Unit> {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(IllegalStateException("No signed-in user"))

            val governmentIdUrl = uploadDocument(uid, "government_id.jpg", draft.governmentIdUri)
            val barangayCertificationUrl = uploadDocument(uid, "barangay_certification.jpg", draft.barangayCertificationUri)
            val selfieUrl = uploadDocument(uid, "selfie.jpg", draft.selfieUri)

            val data: Map<String, Any?> = mapOf(
                "fullName" to draft.fullName,
                "birthdate" to draft.birthdate,
                "address" to draft.address,
                "barangay" to draft.barangay,
                "contactNumber" to draft.contactNumber,
                "email" to draft.email,
                "governmentIdUrl" to governmentIdUrl,
                "barangayCertificationUrl" to barangayCertificationUrl,
                "selfieUrl" to selfieUrl,
                "emergencyContact" to draft.emergencyContact,
                "bloodType" to draft.bloodType,
                "occupation" to draft.occupation,
                "reason" to draft.reason,
                "status" to ApplicationStatus.PENDING
            )

            setDocument(uid, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit responder application", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadDocument(uid: String, fileName: String, uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            val ref = storage.reference.child("$STORAGE_FOLDER/$uid/$fileName")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    ref.downloadUrl
                }
                .addOnSuccessListener { url -> cont.resume(url.toString()) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    private suspend fun setDocument(uid: String, data: Map<String, Any?>): Unit =
        suspendCancellableCoroutine { cont ->
            firestore.collection(COLLECTION).document(uid).set(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
