package com.placer.firewatch.report.export

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.placer.firewatch.report.Incident
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** One-time fetch (not a live listener) of every report, for admin export. */
class ReportExportRepository {

    companion object {
        val HEADERS = listOf("Type", "Status", "Barangay", "Date/Time", "Reporter", "Note", "Latitude", "Longitude")
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    suspend fun fetchAllAsRows(): Result<List<List<String>>> {
        return try {
            val snapshot = suspendCancellableCoroutine<com.google.firebase.firestore.QuerySnapshot> { cont ->
                firestore.collection("fire_reports")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
            val incidents = snapshot.documents.mapNotNull { it.toObject(Incident::class.java) }
            Result.success(incidents.map { it.toRow() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Incident.toRow(): List<String> = listOf(
        type,
        status,
        barangay.orEmpty(),
        timestamp?.let { dateFormat.format(it) }.orEmpty(),
        userId,
        note.orEmpty(),
        latitude.toString(),
        longitude.toString()
    )
}
