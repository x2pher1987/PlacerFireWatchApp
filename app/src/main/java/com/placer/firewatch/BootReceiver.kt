package com.placer.firewatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.placer.firewatch.util.Prefs

/**
 * If the phone loses power and restarts (common with a device left plugged
 * in 24/7), this resumes monitoring automatically instead of requiring
 * someone to physically walk over and reopen the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isMonitoringEnabled(context)) {
            val serviceIntent = Intent(context, MonitoringService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
