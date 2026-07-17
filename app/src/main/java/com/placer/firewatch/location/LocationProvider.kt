package com.placer.firewatch.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Requests a fresh GPS fix instead of relying on a possibly stale/null
     * cached [getLastKnownLocation]. Used for the one-tap fire report flow,
     * where an accurate current position matters more than call latency.
     * Falls back to the cached last-known location if the fresh request
     * fails or times out.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val cancellationSource = CancellationTokenSource()
        val fresh = suspendCancellableCoroutine<Location?> { cont ->
            cont.invokeOnCancellation { cancellationSource.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSource.token)
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }
        return fresh ?: getLastKnownLocation()
    }
}
