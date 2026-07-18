package com.placer.firewatch.user

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Reads/writes the users/{uid} collection — every account's role and
 * (for responders) approval status. See firestore.rules: a user may only
 * create their own doc (forced to role=citizen at signup) and read it
 * back; role changes (approving a responder, promoting an admin) happen
 * out-of-band via the Firebase console until Section 4 builds real admin
 * tooling — see the README's "Create responder accounts" section.
 */
class UserRepository {

    companion object {
        private const val COLLECTION = "users"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun createCitizen(uid: String, fullName: String, email: String): Unit =
        suspendCancellableCoroutine { cont ->
            val data = mapOf(
                "fullName" to fullName,
                "email" to email,
                "role" to UserRole.CITIZEN,
                "responderStatus" to ResponderStatus.NONE,
                "suspended" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION).document(uid).set(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    suspend fun fetchUser(uid: String): AppUser? =
        suspendCancellableCoroutine { cont ->
            firestore.collection(COLLECTION).document(uid).get()
                .addOnSuccessListener { snapshot -> cont.resume(snapshot.toObject(AppUser::class.java)) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
