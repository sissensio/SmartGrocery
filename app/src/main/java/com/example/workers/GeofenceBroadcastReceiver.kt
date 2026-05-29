package com.example.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences != null && triggeringGeofences.isNotEmpty()) {
                for (geofence in triggeringGeofences) {
                    val storeName = geofence.requestId // We use Store name as requestId
                    
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val db = com.example.data.AppDatabase.getDatabase(context)
                            
                            val pReceipt = com.example.data.PendingReceipt(
                                storeName = storeName,
                                location = "Uscita rilevata da Geofence reale",
                                timestamp = System.currentTimeMillis()
                            )
                            val id = db.groceryDao().insertPendingReceipt(pReceipt)
                            
                            NotificationHelper.showGeofenceCheckoutNotification(context, storeName, id.toInt())
                        } catch (e: Exception) {
                            Log.e("GeofenceReceiver", "Error saving checkout: \${e.message}")
                        }
                    }
                }
            }
        }
    }
}
