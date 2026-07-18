package com.placer.firewatch.responder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ItemIncidentBinding
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.IncidentRepository
import com.placer.firewatch.report.ReportStatus
import com.placer.firewatch.report.ReportType
import kotlinx.coroutines.launch

/**
 * Read-only geographic view of the same incident feed the dashboard shows
 * as a list, plus the same status actions from a marker's detail dialog.
 * Requires the same responders/{uid} allowlist access as the dashboard —
 * see IncidentRepository / firestore.rules.
 */
class LiveFireMapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        // Placer, Masbate, Philippines — used only as the initial camera
        // position before any incidents have loaded.
        private val DEFAULT_CAMERA_TARGET = LatLng(12.35, 123.35)
        private const val DEFAULT_ZOOM = 11f
        private const val INCIDENT_ZOOM = 12f
    }

    private val repository = IncidentRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private var googleMap: GoogleMap? = null
    private var latestIncidents: List<Incident> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_fire_map)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CAMERA_TARGET, DEFAULT_ZOOM))
        map.setOnMarkerClickListener { marker ->
            (marker.tag as? Incident)?.let { showIncidentDialog(it) }
            true
        }
        renderMarkers()
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenForIncidents(
            onUpdate = { incidents ->
                latestIncidents = incidents
                renderMarkers()
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

    private fun renderMarkers() {
        val map = googleMap ?: return
        map.clear()
        latestIncidents.forEach { incident ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(incident.latitude, incident.longitude))
                    .title(incident.barangay ?: getString(R.string.responder_unknown_barangay))
                    .snippet(incident.type)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHueFor(incident.type)))
                    .alpha(markerAlphaFor(incident.status))
            )
            marker?.tag = incident
        }
        latestIncidents.firstOrNull()?.let {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), INCIDENT_ZOOM))
        }
    }

    /** Color keys off report type (what it is), not status, so type is visible at a glance regardless of progress. */
    private fun markerHueFor(type: String): Float = when (type) {
        ReportType.FIRE -> BitmapDescriptorFactory.HUE_RED
        ReportType.SMOKE -> BitmapDescriptorFactory.HUE_AZURE
        ReportType.SUSPECTED_FIRE -> BitmapDescriptorFactory.HUE_ORANGE
        else -> BitmapDescriptorFactory.HUE_RED
    }

    /** Status is layered on as opacity: full for anything still active, dimmed once resolved. */
    private fun markerAlphaFor(status: String): Float = when (status) {
        ReportStatus.FIRE_OUT, ReportStatus.FALSE_ALARM -> 0.45f
        else -> 1.0f
    }

    private fun showIncidentDialog(incident: Incident) {
        val detailBinding = ItemIncidentBinding.inflate(layoutInflater)
        detailBinding.bindIncident(incident)
        detailBinding.btnUpdateStatus.setOnClickListener { anchor ->
            showIncidentStatusMenu(anchor, incident) { inc, newStatus -> updateIncidentStatus(inc.id, newStatus) }
        }
        MaterialAlertDialogBuilder(this)
            .setView(detailBinding.root)
            .setPositiveButton(R.string.responder_dialog_close, null)
            .show()
    }

    private fun updateIncidentStatus(reportId: String, newStatus: String) {
        lifecycleScope.launch {
            val result = repository.updateStatus(reportId, newStatus)
            if (result.isFailure) {
                Toast.makeText(this@LiveFireMapActivity, R.string.responder_status_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
