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
    val barangay: String? = null,
    // Defaults to Fire for reports submitted before Section 6 added report
    // types — those documents genuinely have no "type" field in Firestore.
    val type: String = ReportType.FIRE
)
