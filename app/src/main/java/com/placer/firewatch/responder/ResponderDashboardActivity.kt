package com.placer.firewatch.responder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.placer.firewatch.BuildConfig
import com.placer.firewatch.LandingActivity
import com.placer.firewatch.R
import com.placer.firewatch.alert.ResponderAlertNotifier
import com.placer.firewatch.databinding.ActivityResponderDashboardBinding
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.IncidentRepository
import com.placer.firewatch.report.ReportStatus
import kotlinx.coroutines.launch

/**
 * Live incident feed for logged-in responders. Requires the signed-in
 * account's UID to have a matching responders/{uid} document in Firestore
 * — see the README's responder setup section — otherwise the listener
 * below surfaces a permission-denied error.
 *
 * Exception: a ⚠ DEVELOPMENT ONLY session (see DevResponderSession) skips
 * Firestore entirely and shows 3 hardcoded sample incidents instead, for
 * testing this screen before a real Firebase project exists.
 */
class ResponderDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResponderDashboardBinding
    private val repository = IncidentRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var adapter: IncidentAdapter
    private var isDevSession = false
    private var devIncidents: List<Incident> = emptyList()
    private var knownIncidentIds: Set<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResponderDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isDevSession = BuildConfig.DEBUG && DevResponderSession.isActive(this)

        adapter = IncidentAdapter { incident, newStatus -> updateIncidentStatus(incident.id, newStatus) }
        binding.recyclerIncidents.layoutManager = LinearLayoutManager(this)
        binding.recyclerIncidents.adapter = adapter

        binding.btnLogout.setOnClickListener {
            if (isDevSession) DevResponderSession.end(this) else FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }

        if (isDevSession) {
            binding.textDevModeBanner.visibility = View.VISIBLE
            // The map isn't wired to show sample data, only live Firestore —
            // hide it here rather than let it open to a broken/erroring screen.
            binding.btnMapView.visibility = View.GONE
        } else {
            binding.btnMapView.setOnClickListener {
                startActivity(Intent(this, LiveFireMapActivity::class.java))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isDevSession) {
            devIncidents = DevResponderSession.sampleIncidents()
            adapter.submitList(devIncidents)
            binding.textEmpty.visibility = if (devIncidents.isEmpty()) View.VISIBLE else View.GONE
            return
        }
        listenerRegistration = repository.listenForIncidents(
            onUpdate = { incidents ->
                adapter.submitList(incidents)
                binding.textEmpty.visibility = if (incidents.isEmpty()) View.VISIBLE else View.GONE
                alertOnNewIncidents(incidents)
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
        knownIncidentIds = null
    }

    /**
     * Fires the Section 8 alert (channel/priority/vibration keyed by report
     * type) only for incidents that appeared after this listener started —
     * the first snapshot is the dashboard's existing backlog, not "new".
     */
    private fun alertOnNewIncidents(incidents: List<Incident>) {
        val previous = knownIncidentIds
        val currentIds = incidents.map { it.id }.toSet()
        if (previous != null) {
            incidents.filter { it.id !in previous && it.status == ReportStatus.PENDING }
                .forEach { ResponderAlertNotifier.notifyNewIncident(this, it.type, it.id, it.barangay) }
        }
        knownIncidentIds = currentIds
    }

    private fun updateIncidentStatus(reportId: String, newStatus: String) {
        ResponderAlertNotifier.acknowledge(this, reportId)
        if (isDevSession) {
            devIncidents = devIncidents.map { if (it.id == reportId) it.copy(status = newStatus) else it }
            adapter.submitList(devIncidents)
            return
        }
        lifecycleScope.launch {
            val result = repository.updateStatus(reportId, newStatus)
            if (result.isFailure) {
                Toast.makeText(this@ResponderDashboardActivity, R.string.responder_status_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
