package com.example.workers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.StoreInfo
import com.example.workers.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val geofencingClient by lazy {
        try {
            LocationServices.getGeofencingClient(context)
        } catch (e: Throwable) {
            android.util.Log.e("GeofenceManager", "Failed to get GeofencingClient", e)
            null
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    @SuppressLint("MissingPermission")
    fun updateGeofences(stores: List<StoreInfo>) {
        val client = geofencingClient ?: return
        
        // Prioritize and cap matching geofences to maximum of 90 (Android official maximum client-side limit)
        val limitedStores = stores.filter { it.latitude != null && it.longitude != null }
            .sortedByDescending { it.lastSeen }
            .take(90)

        val geofences = limitedStores.map { store ->
            Geofence.Builder()
                .setRequestId(store.name)
                .setCircularRegion(
                    store.latitude!!,
                    store.longitude!!,
                    store.geofenceRadius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT) // We only care about exit for now
                .build()
        }

        try {
            if (geofences.isEmpty()) {
                client.removeGeofences(geofencePendingIntent)
                return
            }

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(geofences)
                .build()
                
            // We always try to add, which will update existing ones with same RequestId.
            client.addGeofences(geofencingRequest, geofencePendingIntent)
        } catch (e: Throwable) {
            android.util.Log.e("GeofenceManager", "Error updating geofences", e)
        }
    }
}
