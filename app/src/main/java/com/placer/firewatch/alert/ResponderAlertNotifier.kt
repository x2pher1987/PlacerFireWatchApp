package com.placer.firewatch.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.placer.firewatch.R
import com.placer.firewatch.report.ReportType
import com.placer.firewatch.responder.ResponderDashboardActivity

/**
 * Section 8 per-report-type responder alerting: distinct notification
 * channel, priority, and vibration pattern per Fire / Smoke / Suspected
 * Fire.
 *
 * There is no server-side trigger for this yet — that needs a Cloud
 * Function watching Firestore writes, which needs the project on the
 * Blaze billing plan (a manual account decision, not a code change). Until
 * that exists, ResponderDashboardActivity calls notifyNewIncident() itself
 * whenever its live listener sees a new Pending incident appear while the
 * dashboard is open. FireWatchMessagingService.onMessageReceived() calls
 * the same function, so the channel/priority/vibration behavior will be
 * identical once a real push arrives.
 */
object ResponderAlertNotifier {

    const val CHANNEL_FIRE = "responder_alert_fire"
    const val CHANNEL_SMOKE = "responder_alert_smoke"
    const val CHANNEL_SUSPECTED = "responder_alert_suspected"

    private const val NOTIFICATION_ID_BASE = 5000

    private val FIRE_VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800)
    private val SMOKE_VIBRATION_PATTERN = longArrayOf(0, 200)
    private val SUSPECTED_VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FIRE, context.getString(R.string.alert_channel_fire), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = FIRE_VIBRATION_PATTERN
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SMOKE, context.getString(R.string.alert_channel_smoke), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = SMOKE_VIBRATION_PATTERN
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SUSPECTED, context.getString(R.string.alert_channel_suspected), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = SUSPECTED_VIBRATION_PATTERN
            }
        )
    }

    /** Fire is ongoing (can't be swiped away) and keeps vibrating until [acknowledge] is called. */
    fun notifyNewIncident(context: Context, type: String, incidentId: String, barangay: String?) {
        ensureChannels(context)

        val channelId: String
        val pattern: LongArray
        val ongoing: Boolean
        val priority: Int
        val title: Int
        when (type) {
            ReportType.FIRE -> {
                channelId = CHANNEL_FIRE; pattern = FIRE_VIBRATION_PATTERN; ongoing = true
                priority = NotificationCompat.PRIORITY_HIGH; title = R.string.alert_title_fire
            }
            ReportType.SMOKE -> {
                channelId = CHANNEL_SMOKE; pattern = SMOKE_VIBRATION_PATTERN; ongoing = false
                priority = NotificationCompat.PRIORITY_LOW; title = R.string.alert_title_smoke
            }
            else -> {
                channelId = CHANNEL_SUSPECTED; pattern = SUSPECTED_VIBRATION_PATTERN; ongoing = false
                priority = NotificationCompat.PRIORITY_DEFAULT; title = R.string.alert_title_suspected
            }
        }

        val openIntent = PendingIntent.getActivity(
            context, incidentId.hashCode(),
            Intent(context, ResponderDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val body = if (barangay.isNullOrBlank()) {
            context.getString(R.string.alert_body_no_barangay)
        } else {
            context.getString(R.string.alert_body_with_barangay, barangay)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(title))
            .setContentText(body)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setContentIntent(openIntent)
            .setVibrate(pattern)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_BASE + incidentId.hashCode(), notification)

        if (ongoing) vibrateUntilCancelled(context, pattern)
    }

    /** Call when a responder acts on an incident (any status change) — dismisses the alert and stops vibration. */
    fun acknowledge(context: Context, incidentId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_BASE + incidentId.hashCode())
        vibrator(context).cancel()
    }

    private fun vibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrateUntilCancelled(context: Context, pattern: LongArray) {
        val v = vibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, 0)
        }
    }
}
