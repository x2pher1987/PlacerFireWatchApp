package com.placer.firewatch.responder

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ActivityResponderDashboardBinding
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.IncidentRepository
import kotlinx.coroutines.launch

/**
 * Live incident feed for logged-in responders. Requires the signed-in
 * account's UID to have a matching responders/{uid} document in Firestore
 * — see the README's responder setup section — otherwise the listener
 * below surfaces a permission-denied error.
 */
class ResponderDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResponderDashboardBinding
    private val repository = IncidentRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var adapter: IncidentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResponderDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = IncidentAdapter { incident, newStatus -> updateIncidentStatus(incident.id, newStatus) }
        binding.recyclerIncidents.layoutManager = LinearLayoutManager(this)
        binding.recyclerIncidents.adapter = adapter

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenForIncidents(
            onUpdate = { incidents ->
                adapter.submitList(incidents)
                binding.textEmpty.visibility = if (incidents.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { e ->
                Toast.makeText(
                    this,
                    getString(R.string.responder_dashboard_load_failed, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun updateIncidentStatus(reportId: String, newStatus: String) {
        lifecycleScope.launch {
            val result = repository.updateStatus(reportId, newStatus)
            if (result.isFailure) {
                Toast.makeText(this@ResponderDashboardActivity, R.string.responder_status_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
