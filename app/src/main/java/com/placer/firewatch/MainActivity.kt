package com.placer.firewatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.placer.firewatch.alert.AlertSender
import com.placer.firewatch.databinding.ActivityMainBinding
import com.placer.firewatch.detection.AlertTrigger
import com.placer.firewatch.location.LocationProvider
import com.placer.firewatch.util.Prefs
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        monitoringActive = Prefs.isMonitoringEnabled(this)
        updateToggleButtonLabel()

        binding.btnToggleMonitoring.setOnClickListener { toggleMonitoring() }
        binding.btnReportFire.setOnClickListener { sendManualReport() }
        binding.btnCallBfp.setOnClickListener { callBfp() }
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
}
