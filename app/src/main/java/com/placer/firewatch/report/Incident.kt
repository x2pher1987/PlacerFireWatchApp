package com.placer.firewatch.report

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Firestore's POJO mapper (toObject) needs a no-arg constructor, which a
 * Kotlin data class gets automatically as long as every property has a
 * default — hence the somewhat unusual defaults below.
 */
data class Incident(
    @DocumentId val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Date? = null,
    val userId: String = "",
    val photoUrl: String? = null,
    val note: String? = null,
    val status: String = ReportStatus.PENDING,
    val barangay: String? = null
)
