package com.placer.firewatch.responder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.placer.firewatch.databinding.ItemIncidentBinding
import com.placer.firewatch.report.Incident

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
        fun bind(incident: Incident, onStatusSelected: (Incident, String) -> Unit) {
            binding.bindIncident(incident)
            binding.btnUpdateStatus.setOnClickListener { anchor ->
                showIncidentStatusMenu(anchor, incident, onStatusSelected)
            }
        }
    }
}
