package com.guilherme.honeypot.helper

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationHelper(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        Log.d("Honeypot", "Location obtained: $location")
                        cont.resume(location)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Honeypot", "Location failed: ${e.message}")
                        cont.resume(null)
                    }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }
    }
}
