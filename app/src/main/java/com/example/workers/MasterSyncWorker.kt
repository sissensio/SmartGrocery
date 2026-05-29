package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.api.LocalBackendServiceClient
import com.example.api.LedgerItemDto
import com.example.api.LedgerSubmitRequest
import com.example.api.SyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MasterSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.groceryDao()
            
            val unsyncedLedgers = dao.getUnsyncedLedgerEntries()
            val unsyncedAcks = dao.getUnsyncedNotificationAcks()

            val prefs = applicationContext.getSharedPreferences("smart_grocery_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("user_token", null)
            val deviceUuid = prefs.getString("device_uuid", "") ?: ""

            // Build payload
            val ledgerRequests = unsyncedLedgers.map { entry ->
                var dtoList: List<LedgerItemDto>? = null
                if (!entry.receiptItemsJson.isNullOrBlank()) {
                    try {
                        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.api.ParsedItem::class.java)
                        val items = moshi.adapter<List<com.example.api.ParsedItem>>(type).fromJson(entry.receiptItemsJson)
                        if (items != null) {
                            dtoList = items.map { LedgerItemDto(it.name, it.price, it.category) }
                        }
                    } catch (e: Exception) {
                        Log.e("MasterSyncWorker", "Failed to parse receipt config", e)
                    }
                }

                val storeNameMatch = android.text.TextUtils.split(entry.description, "Frazione Scontrino").lastOrNull()?.trim()?.substringBefore(" (PIVA")?.trim() 
                    ?: android.text.TextUtils.split(entry.description, "Acquisto rapido").lastOrNull()?.trim() 
                    ?: "Negozio Sconosciuto"
                
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(entry.timestamp))
                
                LedgerSubmitRequest(
                    storeName = storeNameMatch,
                    amount = entry.amount,
                    date = dateStr,
                    paidBy = entry.paidBy,
                    isShared = true,
                    clientUuid = entry.client_uuid,
                    items = dtoList
                )
            }
            
            val ackIds = unsyncedAcks.map { it.notificationId }
            
            val syncRequest = SyncRequest(
                deviceUuid = deviceUuid,
                pendingLedgerEntries = ledgerRequests,
                pendingNotificationAcks = ackIds
            )
            
            val response = LocalBackendServiceClient.performUnifiedSync(token, syncRequest)
            
            if (response != null) {
                // Update successfully synced ledgers
                for (uuid in response.syncedLedgerUuids) {
                    val entry = unsyncedLedgers.find { it.client_uuid == uuid }
                    if (entry != null) {
                        dao.updateLedgerEntry(entry.copy(is_synced = true))
                    }
                }
                
                // Delete successfully synced acks
                if (response.syncedNotificationAcks.isNotEmpty()) {
                    dao.deleteSyncedNotificationAcks(response.syncedNotificationAcks)
                }
                
                // Insert new notifications into local Room DB
                if (response.newNotifications.isNotEmpty()) {
                    val localEntities = mutableListOf<com.example.data.BackendNotificationEntity>()
                    
                    for (notif in response.newNotifications) {
                        val exists = dao.hasNotification(notif.id)
                        if (!exists) {
                            // Show System Push Notification ONLY if we don't have it already
                            com.example.workers.NotificationHelper.showBackendNotification(
                                applicationContext,
                                notif.id.toLong(),
                                notif.title,
                                notif.body
                            )
                        }
                        
                        // We still want to map them (e.g. if the user didn't ack them, we can ensure they're in DB)
                        // But if it already exists, we SHOULD NOT overwrite isRead to false! 
                        // So we only insert if it doesn't exist.
                        if (!exists) {
                            localEntities.add(
                                com.example.data.BackendNotificationEntity(
                                    id = notif.id,
                                    title = notif.title,
                                    body = notif.body,
                                    type = notif.type,
                                    targetStoreId = notif.targetStoreId,
                                    targetCity = notif.targetCity,
                                    targetRegion = notif.targetRegion,
                                    createdAt = notif.createdAt,
                                    isRead = false
                                )
                            )
                        }
                    }
                    if (localEntities.isNotEmpty()) {
                        dao.insertNotifications(localEntities)
                    }
                }
                
                // Store device status limits
                prefs.edit().apply {
                    putBoolean("device_blocked", response.deviceStatus.isBlocked)
                    if (response.deviceStatus.customLimit != null) {
                        putFloat("device_limit", response.deviceStatus.customLimit.toFloat())
                    }
                }.apply()
                
                return@withContext Result.success()
            } else {
                return@withContext Result.retry()
            }

        } catch (e: Exception) {
            Log.e("MasterSyncWorker", "Error in sync", e)
            return@withContext Result.retry()
        }
    }
}
