package com.placer.firewatch.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.placer.firewatch.alert.ResponderAlertNotifier
import com.placer.firewatch.report.ReportType

/**
 * No backend currently sends anything to this app — there's no Cloud
 * Function watching Firestore writes yet (that needs the Blaze billing
 * plan, a manual account decision — see ROADMAP.md). This is ready to
 * receive that push the moment it exists: a message with data payload
 * keys "type" (Fire/Smoke/Suspected Fire), "incidentId", and optionally
 * "barangay" reuses the exact same channel/priority/vibration behavior
 * that ResponderDashboardActivity already triggers locally for incidents
 * seen while the dashboard is open.
 */
class FireWatchMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FireWatchMessaging"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM registration token refreshed")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "FCM message received from ${message.from}")

        val incidentId = message.data["incidentId"] ?: return
        val type = message.data["type"] ?: ReportType.FIRE
        val barangay = message.data["barangay"]
        ResponderAlertNotifier.notifyNewIncident(this, type, incidentId, barangay)
    }
}
