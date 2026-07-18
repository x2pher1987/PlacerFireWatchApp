package com.placer.firewatch.user

import com.google.firebase.firestore.DocumentId

object UserRole {
    const val CITIZEN = "citizen"
    const val RESPONDER = "responder"
    const val ADMIN = "admin"
}

/** Only meaningful once role == RESPONDER; a citizen who hasn't applied stays NONE. */
object ResponderStatus {
    const val NONE = "none"
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

/**
 * Firestore's POJO mapper (toObject) needs a no-arg constructor, which a
 * Kotlin data class gets automatically as long as every property has a
 * default.
 */
data class AppUser(
    @DocumentId val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = UserRole.CITIZEN,
    val responderStatus: String = ResponderStatus.NONE,
    val suspended: Boolean = false
)
