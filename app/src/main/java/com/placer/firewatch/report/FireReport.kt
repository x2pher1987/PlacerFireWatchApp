package com.placer.firewatch.report

import android.net.Uri

/** What the user has assembled in the report dialog, before it's submitted. */
data class FireReportDraft(
    val latitude: Double,
    val longitude: Double,
    val note: String,
    val photoUri: Uri?,
    val barangay: String,
    val type: String
)
