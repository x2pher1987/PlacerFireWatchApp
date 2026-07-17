package com.placer.firewatch.responder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ItemIncidentBinding
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.ReportStatus
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentAdapter(
    private val onStatusSelected: (Incident, String) -> Unit
) : ListAdapter<Incident, IncidentAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Incident>() {
            override fun areItemsTheSame(oldItem: Incident, newItem: Incident) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Incident, newItem: Incident) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onStatusSelected)
    }

    class ViewHolder(private val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

        fun bind(incident: Incident, onStatusSelected: (Incident, String) -> Unit) {
            val context = binding.root.context

            binding.textBarangay.text = incident.barangay?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.responder_unknown_barangay)

            binding.textCoordinates.text = context.getString(
                R.string.responder_coordinates_format, incident.latitude, incident.longitude
            )

            binding.textTime.text = incident.timestamp?.let { dateFormat.format(it) }
                ?: context.getString(R.string.responder_time_unknown)

            binding.textReporter.text = context.getString(R.string.responder_reporter_format, incident.userId)

            binding.textNote.text = incident.note?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.responder_no_note)

            binding.textStatus.text = incident.status

            val photoUrl = incident.photoUrl
            if (photoUrl != null) {
                binding.imagePhoto.visibility = View.VISIBLE
                binding.imagePhoto.load(photoUrl)
            } else {
                binding.imagePhoto.visibility = View.GONE
            }

            binding.btnUpdateStatus.setOnClickListener { anchor ->
                showStatusMenu(anchor, incident, onStatusSelected)
            }
        }

        private fun showStatusMenu(
            anchor: View,
            incident: Incident,
            onStatusSelected: (Incident, String) -> Unit
        ) {
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
    }
}
