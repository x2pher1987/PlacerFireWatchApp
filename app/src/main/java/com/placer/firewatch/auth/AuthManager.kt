package com.placer.firewatch.auth

import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * There's no login screen — every install just needs a stable identity to
 * tag its fire reports with, so Firebase Auth's Anonymous provider is used
 * as a real, backend-verifiable replacement for the locally-generated UUID
 * this used before. Anonymous sign-in must be enabled for the project in
 * the Firebase console (Authentication → Sign-in method → Anonymous).
 */
object AuthManager {

    private val auth by lazy { FirebaseAuth.getInstance() }

    /** Returns the current user's UID, signing in anonymously first if needed. */
    suspend fun getOrSignInUserId(): String {
        auth.currentUser?.let { return it.uid }
        return suspendCancellableCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid != null) {
                        cont.resume(uid)
                    } else {
                        cont.resumeWithException(IllegalStateException("Anonymous sign-in returned no user"))
                    }
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }
}
