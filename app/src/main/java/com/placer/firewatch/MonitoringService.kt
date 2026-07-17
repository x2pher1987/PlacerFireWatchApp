package com.placer.firewatch

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.placer.firewatch.alert.AlertSender
import com.placer.firewatch.detection.AlertTrigger
import com.placer.firewatch.detection.Classifier
import com.placer.firewatch.detection.ClassifierFactory
import com.placer.firewatch.detection.DetectionTracker
import com.placer.firewatch.location.LocationProvider
import com.placer.firewatch.notification.NotificationHelper
import com.placer.firewatch.util.Prefs
import com.placer.firewatch.util.toBitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

/**
 * Keeps the camera running and analyzing frames even when MainActivity is
 * backgrounded or the screen is off — the intended deployment is a phone
 * mounted in a fixed spot, plugged into power, watching one area 24/7.
 *
 * Uses LifecycleService (not a plain Service) because CameraX's
 * bindToLifecycle needs a LifecycleOwner; LifecycleService provides one
 * without requiring a visible Activity.
 */
class MonitoringService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var classifier: Classifier
    private lateinit var tracker: DetectionTracker
    private lateinit var alertSender: AlertSender
    private lateinit var locationProvider: LocationProvider
    private var wakeLock: PowerManager.WakeLock? = null

    private var lastAnalyzedAt = 0L
    private var lastAlertAt = 0L
    private val analysisIntervalMs = 1500L
    private val alertCooldownMs = 5 * 60 * 1000L // don't re-spam BFP for one ongoing event

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationHelper.buildNotification(
            this,
            getString(R.string.notification_monitoring_text, Prefs.getLocationLabel(this))
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        acquireWakeLock()

        cameraExecutor = Executors.newSingleThreadExecutor()
        classifier = ClassifierFactory.create(this)
        val sensitivity = Prefs.getSensitivity(this)
        val threshold = 0.05f + (100 - sensitivity) / 100f * 0.25f
        tracker = DetectionTracker(threshold = threshold)
        alertSender = AlertSender(this)
        locationProvider = LocationProvider(this)

        startCamera()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PlacerFireWatch::MonitoringWakeLock"
        ).apply {
            // Safety timeout so a lock can never be held forever if something
            // goes wrong; the service re-acquires it if Android restarts it.
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_LATEST_ONLY)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val now = System.currentTimeMillis()
                if (now - lastAnalyzedAt >= analysisIntervalMs) {
                    lastAnalyzedAt = now
                    try {
                        val bitmap = imageProxy.toBitmap()
                        val result = classifier.classify(bitmap)
                        val trigger = tracker.update(result)
                        if (trigger != null && now - lastAlertAt >= alertCooldownMs) {
                            lastAlertAt = now
                            handleAlert(trigger)
                        }
                    } finally {
                        imageProxy.close()
                    }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis)
            } catch (e: Exception) {
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleAlert(trigger: AlertTrigger) {
        lifecycleScope.launch {
            val location = locationProvider.getLastKnownLocation()
            val message = alertSender.buildMessage(
                trigger,
                Prefs.getLocationLabel(this@MonitoringService),
                location?.latitude,
                location?.longitude
            )
            alertSender.sendSms(Prefs.getBfpNumbers(this@MonitoringService), message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
        cameraExecutor.shutdown()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
