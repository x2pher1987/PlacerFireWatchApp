package com.placer.firewatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.alert.AlertSender
import com.placer.firewatch.databinding.ActivityMainBinding
import com.placer.firewatch.databinding.DialogReportFireBinding
import com.placer.firewatch.detection.AlertTrigger
import com.placer.firewatch.location.LocationProvider
import com.placer.firewatch.report.FireReportDraft
import com.placer.firewatch.report.FireReportRepository
import com.placer.firewatch.responder.apply.ResponderApplicationActivity
import com.placer.firewatch.util.Prefs
import java.io.File
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var monitoringActive = false

    private val requiredPermissions: Array<String> by lazy {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startCameraPreview()
        } else {
            binding.statusText.text = getString(R.string.permissions_required)
        }
    }

    private var reportDialogBinding: DialogReportFireBinding? = null
    private var pendingPhotoUri: Uri? = null
    private var attachedPhotoUri: Uri? = null

    private val reportLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginFireReportFlow()
        } else {
            Toast.makeText(this, R.string.report_location_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val photoUri = pendingPhotoUri
        if (success && photoUri != null) {
            attachedPhotoUri = photoUri
            reportDialogBinding?.imagePhotoPreview?.apply {
                setImageURI(photoUri)
                visibility = View.VISIBLE
            }
            reportDialogBinding?.btnAttachPhoto?.setText(R.string.report_retake_photo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Defensive: this screen should only ever be reached through
        // LandingActivity's sign-in gate. Bounce back if that's somehow
        // not the case (e.g. a stale task stack).
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        monitoringActive = Prefs.isMonitoringEnabled(this)
        updateToggleButtonLabel()

        binding.btnOneTapReport.setOnClickListener { onReportFireTapped() }
        binding.btnToggleMonitoring.setOnClickListener { toggleMonitoring() }
        binding.btnReportFire.setOnClickListener { sendManualReport() }
        binding.btnCallBfp.setOnClickListener { callBfp() }
        binding.linkApplyResponder.setOnClickListener {
            startActivity(Intent(this, ResponderApplicationActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestNeededPermissions()
    }

    override fun onResume() {
        super.onResume()
        monitoringActive = Prefs.isMonitoringEnabled(this)
        updateToggleButtonLabel()
    }

    private fun requestNeededPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startCameraPreview()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                binding.statusText.text = getString(R.string.status_idle)
            } catch (e: Exception) {
                binding.statusText.text = "Camera error: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleMonitoring() {
        monitoringActive = !monitoringActive
        Prefs.setMonitoringEnabled(this, monitoringActive)

        val serviceIntent = Intent(this, MonitoringService::class.java)
        if (monitoringActive) {
            ContextCompat.startForegroundService(this, serviceIntent)
            binding.statusText.text = getString(R.string.status_monitoring)
        } else {
            stopService(serviceIntent)
            binding.statusText.text = getString(R.string.status_idle)
        }
        updateToggleButtonLabel()
    }

    private fun updateToggleButtonLabel() {
        binding.btnToggleMonitoring.text = getString(
            if (monitoringActive) R.string.stop_monitoring else R.string.start_monitoring
        )
    }

    private fun sendManualReport() {
        lifecycleScope.launch {
            val location = LocationProvider(this@MainActivity).getLastKnownLocation()
            val sender = AlertSender(this@MainActivity)
            val message = sender.buildMessage(
                AlertTrigger.FIRE,
                Prefs.getLocationLabel(this@MainActivity),
                location?.latitude,
                location?.longitude
            ).replace(
                "Automated alert from an unattended camera sensor",
                "Manual report from a resident"
            )
            val sent = sender.sendSms(Prefs.getBfpNumbers(this@MainActivity), message)
            binding.statusText.text = if (sent) {
                getString(R.string.status_alert_sent)
            } else {
                "Failed to send — check SMS permission and signal"
            }
        }
    }

    private fun callBfp() {
        val number = Prefs.getBfpNumbers(this).firstOrNull() ?: "911"
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun onReportFireTapped() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            beginFireReportFlow()
        } else {
            reportLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun beginFireReportFlow() {
        Toast.makeText(this, R.string.report_getting_location, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val location = LocationProvider(this@MainActivity).getCurrentLocation()
            if (location == null) {
                Toast.makeText(this@MainActivity, R.string.report_location_failed, Toast.LENGTH_LONG).show()
            } else {
                showReportConfirmationDialog(location.latitude, location.longitude)
            }
        }
    }

    private fun showReportConfirmationDialog(latitude: Double, longitude: Double) {
        attachedPhotoUri = null
        val dialogBinding = DialogReportFireBinding.inflate(layoutInflater)
        reportDialogBinding = dialogBinding
        dialogBinding.textLocationSummary.text =
            getString(R.string.report_location_summary, latitude, longitude)

        dialogBinding.btnAttachPhoto.setOnClickListener {
            val uri = createPhotoUri()
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.report_confirm_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.report_send) { _, _ ->
                val note = dialogBinding.editReportNote.text?.toString()?.trim().orEmpty()
                submitFireReport(latitude, longitude, note, attachedPhotoUri)
            }
            .setNegativeButton(R.string.report_cancel, null)
            .setOnDismissListener { reportDialogBinding = null }
            .show()
    }

    private fun createPhotoUri(): Uri {
        val photoDir = File(cacheDir, "fire_report_photos").apply { mkdirs() }
        val photoFile = File(photoDir, "report_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
    }

    private fun submitFireReport(latitude: Double, longitude: Double, note: String, photoUri: Uri?) {
        Toast.makeText(this, R.string.report_submitting, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val draft = FireReportDraft(latitude, longitude, note, photoUri, Prefs.getLocationLabel(this@MainActivity))
            val result = FireReportRepository().submit(draft)
            val messageRes = if (result.isSuccess) {
                R.string.report_submitted_success
            } else {
                R.string.report_submitted_failure
            }
            Toast.makeText(this@MainActivity, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}
