package com.placer.firewatch.responder.apply

import com.google.firebase.firestore.DocumentId

/**
 * One application per citizen (document ID == their own UID, see
 * ResponderApplicationRepository). Re-submitting overwrites the previous
 * application rather than creating duplicates.
 */
data class ResponderApplication(
    @DocumentId val uid: String = "",
    val fullName: String = "",
    val birthdate: String = "",
    val address: String = "",
    val barangay: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val governmentIdUrl: String = "",
    val barangayCertificationUrl: String = "",
    val selfieUrl: String = "",
    val emergencyContact: String = "",
    val bloodType: String = "",
    val occupation: String = "",
    val reason: String = "",
    val status: String = ApplicationStatus.PENDING
)

object ApplicationStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}
