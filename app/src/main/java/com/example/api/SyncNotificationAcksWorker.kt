package com.example.api

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.GroceryRepository

class SyncNotificationAcksWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncNotificationWorker", "Avvio sincronizzazione offline delle notifiche...")
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GroceryRepository(database.groceryDao())
        
        try {
            val unsynced = repository.getUnsyncedNotificationAcks()
            if (unsynced.isEmpty()) {
                Log.d("SyncNotificationWorker", "Nessun acknowledge di notifica offline da sincronizzare.")
                return Result.success()
            }
            
            Log.d("SyncNotificationWorker", "Trovati ${unsynced.size} ack non sincronizzati.")
            var successCount = 0
            
            // Re-read preference token from secure/persistent preferences safely
            val sharedPrefs = applicationContext.getSharedPreferences("smart_grocery_prefs", Context.MODE_PRIVATE)
            val token = sharedPrefs.getString("user_token", null)
            
            for (ack in unsynced) {
                val result = LocalBackendServiceClient.acknowledgeNotification(
                    token = token,
                    id = ack.notificationId,
                    deviceUuid = ack.deviceUuid
                )
                if (result) {
                    repository.updateNotificationAck(ack.copy(isSynced = true))
                    successCount++
                }
            }
            
            repository.deleteSyncedNotificationAcks()
            Log.d("SyncNotificationWorker", "Sincronizzati con successo $successCount/${unsynced.size} acks.")
            
            return if (successCount == unsynced.size) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncNotificationWorker", "Errore durante la sincronizzazione delle notifiche", e)
            return Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val request = OneTimeWorkRequestBuilder<SyncNotificationAcksWorker>()
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
