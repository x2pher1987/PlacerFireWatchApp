package com.placer.firewatch.responder

import android.view.View
import android.widget.PopupMenu
import coil.load
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ItemIncidentBinding
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.ReportStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shared between the dashboard's RecyclerView rows and the Live Fire Map's
 * marker-detail dialog — both display the same incident fields the same way.
 */

private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

fun ItemIncidentBinding.bindIncident(incident: Incident) {
    val context = root.context

    textBarangay.text = incident.barangay?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.responder_unknown_barangay)

    textCoordinates.text = context.getString(
        R.string.responder_coordinates_format, incident.latitude, incident.longitude
    )

    textTime.text = incident.timestamp?.let { dateFormat.format(it) }
        ?: context.getString(R.string.responder_time_unknown)

    textReporter.text = context.getString(R.string.responder_reporter_format, incident.userId)

    textNote.text = incident.note?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.responder_no_note)

    textStatus.text = incident.status

    val photoUrl = incident.photoUrl
    if (photoUrl != null) {
        imagePhoto.visibility = View.VISIBLE
        imagePhoto.load(photoUrl)
    } else {
        imagePhoto.visibility = View.GONE
    }
}

fun showIncidentStatusMenu(anchor: View, incident: Incident, onStatusSelected: (Incident, String) -> Unit) {
    val popup = PopupMenu(anchor.context, anchor)
    ReportStatus.RESPONDER_ACTIONS.forEachIndexed { index, status ->
        popup.menu.add(0, index, index, status)
    }
    popup.setOnMenuItemClickListener { item ->
        onStatusSelected(incident, item.title.toString())
        true
    }
    popup.show()
}
