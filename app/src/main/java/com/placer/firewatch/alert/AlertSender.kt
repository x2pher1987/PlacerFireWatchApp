package com.placer.firewatch.alert

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.placer.firewatch.detection.AlertTrigger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertSender(private val context: Context) {

    companion object {
        private const val TAG = "AlertSender"
    }

    /**
     * Builds an SMS body that always identifies itself as automated and asks
     * the recipient to verify — this is a real fire department's line, and
     * an unverified automated system should not read as a confirmed dispatch
     * order. Includes a Google Maps link built from raw coordinates so
     * responders can navigate directly even if the location label is vague.
     */
    fun buildMessage(
        trigger: AlertTrigger,
        locationLabel: String,
        latitude: Double?,
        longitude: Double?
    ): String {
        val kind = if (trigger == AlertTrigger.FIRE) "FIRE" else "SMOKE"
        val timestamp = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        val mapsLink = if (latitude != null && longitude != null) {
            "https://maps.google.com/?q=$latitude,$longitude"
        } else {
            "location unavailable"
        }
        return "PLACER FIREWATCH ALERT: Possible $kind detected near $locationLabel. " +
            "Time: $timestamp. Map: $mapsLink. Automated alert from an unattended " +
            "camera sensor — please verify before dispatch."
    }

    /** Returns true only if every recipient's send call succeeded. */
    fun sendSms(numbers: List<String>, message: String): Boolean {
        if (numbers.isEmpty()) {
            Log.w(TAG, "No recipient numbers configured, cannot send alert")
            return false
        }

        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var allSucceeded = true
        for (number in numbers) {
            try {
                val parts = manager.divideMessage(message)
                manager.sendMultipartTextMessage(number, null, parts, null, null)
                Log.i(TAG, "Alert SMS sent to $number")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send alert SMS to $number", e)
                allSucceeded = false
            }
        }
        return allSucceeded
    }
}
