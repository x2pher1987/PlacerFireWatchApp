package com.placer.firewatch.admin

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.placer.firewatch.databinding.ItemResponderApplicationBinding
import com.placer.firewatch.responder.apply.ResponderApplication

class ResponderApplicationAdapter(
    private val onApprove: (ResponderApplication) -> Unit,
    private val onReject: (ResponderApplication) -> Unit
) : ListAdapter<ResponderApplication, ResponderApplicationAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ResponderApplication>() {
            override fun areItemsTheSame(oldItem: ResponderApplication, newItem: ResponderApplication) =
                oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: ResponderApplication, newItem: ResponderApplication) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResponderApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onApprove, onReject)
    }

    class ViewHolder(private val binding: ItemResponderApplicationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            application: ResponderApplication,
            onApprove: (ResponderApplication) -> Unit,
            onReject: (ResponderApplication) -> Unit
        ) {
            val context = binding.root.context
            binding.textFullName.text = application.fullName
            binding.textBarangay.text = "${application.barangay} — ${application.address}"
            binding.textContact.text = "${application.contactNumber} • ${application.email}"
            binding.textOccupation.text = "${application.occupation} • Born ${application.birthdate} • Emergency: ${application.emergencyContact}"
            binding.textReason.text = application.reason

            binding.btnViewGovernmentId.setOnClickListener { openUrl(context, application.governmentIdUrl) }
            binding.btnViewBarangayCertification.setOnClickListener { openUrl(context, application.barangayCertificationUrl) }
            binding.btnViewSelfie.setOnClickListener { openUrl(context, application.selfieUrl) }

            binding.btnApprove.setOnClickListener { onApprove(application) }
            binding.btnReject.setOnClickListener { onReject(application) }
        }

        private fun openUrl(context: android.content.Context, url: String) {
            if (url.isBlank()) return
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
