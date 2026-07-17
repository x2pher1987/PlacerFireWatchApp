package com.placer.firewatch.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Infrastructure only, for now: no backend currently sends anything to this
 * app, and there's no server-side piece that consumes registration tokens.
 * This exists so Cloud Messaging is fully wired up ahead of a future
 * push-based alert channel (see ROADMAP.md V2-6 — an alternate/fallback
 * alert path for deployments where SMS can't reach a cell network).
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
    }
}
