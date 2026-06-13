package com.example.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.PendingCatalogItem
import com.example.api.LocalBackendServiceClient
import com.example.api.LedgerItemDto
import com.example.api.LedgerSubmitRequest
import com.example.api.SyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
            val unsyncedMutes = dao.getUnsyncedMutedStores()

            val prefs = applicationContext.getSharedPreferences("smart_grocery_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("user_token", null)
            val deviceUuid = prefs.getString("device_uuid", "") ?: ""
            
            if (token.isNullOrBlank()) {
                Log.d("MasterSyncWorker", "No auth token found, aborting sync")
                return@withContext Result.success()
            }

            // Sync offline-queued PendingStoreReports
            try {
                val pendingStoreReports = dao.getUnsyncedStoreReports()
                if (pendingStoreReports.isNotEmpty()) {
                    Log.d("MasterSyncWorker", "Found ${pendingStoreReports.size} pending store reports to sync")
                    for (report in pendingStoreReports) {
                        val req = com.example.api.StoreReportRequest(
                            name = report.name,
                            displayName = report.displayName,
                            address = report.address,
                            city = report.city,
                            province = report.province,
                            latitude = report.latitude,
                            longitude = report.longitude,
                            storeType = report.storeType
                        )
                        val res = LocalBackendServiceClient.reportStoreOnServer(token, req)
                        if (res != null) {
                            dao.deletePendingStoreReport(report.id)
                            Log.d("MasterSyncWorker", "Successfully synced pending store report: ${report.name}")
                        } else {
                            Log.e("MasterSyncWorker", "Failed to sync pending store report: ${report.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MasterSyncWorker", "Error syncing pending store reports", e)
            }

            // Sync offline-queued PendingCatalogItems
            try {
                val pendingCatalogItems = dao.getAllPendingCatalogItems()
                if (pendingCatalogItems.isNotEmpty()) {
                    Log.d("MasterSyncWorker", "Found ${pendingCatalogItems.size} pending catalog items to sync")
                    for (pending in pendingCatalogItems) {
                        val createDto = com.example.api.CatalogItemCreate(
                            barcode = pending.barcode,
                            name = pending.name,
                            brand = pending.brand,
                            category = pending.category,
                            price = pending.price,
                            unitPrice = pending.unitPrice,
                            weight = pending.weight,
                            discountLabel = pending.discountLabel,
                            storeName = pending.storeName,
                            vatNumber = pending.vatNumber
                        )
                        val success = LocalBackendServiceClient.submitShelfLabel(token, deviceUuid, createDto)
                        if (success) {
                            dao.deletePendingCatalogItem(pending)
                            Log.d("MasterSyncWorker", "Successfully synced pending catalog item: ${pending.barcode}")
                        } else {
                            Log.e("MasterSyncWorker", "Failed to sync pending catalog item: ${pending.barcode}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MasterSyncWorker", "Error syncing pending catalog items", e)
            }

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
                
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date(entry.timestamp))
                
                LedgerSubmitRequest(
                    storeName = storeNameMatch,
                    amount = entry.amount,
                    date = dateStr,
                    paidBy = entry.paidBy,
                    isShared = true,
                    clientUuid = entry.client_uuid,
                    items = dtoList,
                    groupId = entry.groupId,
                    paidByUserId = entry.paidByUserId,
                    createdAt = entry.created_at
                )
            }
            
            val ackIds = unsyncedAcks.map { it.notificationId }
            val muteRequests = unsyncedMutes.map { mute ->
                com.example.api.StoreMuteSyncDto(
                    storeId = mute.storeId,
                    reason = mute.reason,
                    customComment = mute.customComment
                )
            }
            
            val syncRequest = SyncRequest(
                deviceUuid = deviceUuid,
                pendingLedgerEntries = ledgerRequests,
                pendingNotificationAcks = ackIds,
                pendingStoreMutes = muteRequests
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

                // Mark successfully synced mutes
                if (response.syncedStoreMutes.isNotEmpty()) {
                    for (storeId in response.syncedStoreMutes) {
                        dao.markMutedStoreSynced(storeId)
                    }
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
                
                // --- NUOVA PARTE: Sincronizzazione scontrini di gruppo e anagrafica negozi ---
                try {
                    val groupEntries = com.example.api.LocalBackendServiceClient.fetchGroupLedgerEntries(token)
                    if (groupEntries.isNotEmpty()) {
                        val localLedgers = dao.getAllLedgerEntries()
                        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                        val itemsType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.api.ParsedItem::class.java)
                        val adapter = moshi.adapter<List<com.example.api.ParsedItem>>(itemsType)
                        
                        for (entry in groupEntries) {
                            val clientUuid = entry.clientUuid ?: ""
                            val alreadyExists = localLedgers.any { it.client_uuid == clientUuid }
                            
                            if (!alreadyExists && clientUuid.isNotEmpty()) {
                                // 1. Converti gli articoli DTO nel formato locale ParsedItem atteso dalla UI
                                val parsedItems = entry.items.map { dto ->
                                    com.example.api.ParsedItem(
                                        name = dto.name,
                                        brand = dto.brand ?: "",
                                        price = dto.price,
                                        unitPrice = dto.unitPrice ?: 0.0,
                                        category = dto.category,
                                        isShared = true,
                                        barcode = dto.barcode ?: "",
                                        weight = dto.weight,
                                        confidence = 1.0
                                    )
                                }
                                val itemsJson = adapter.toJson(parsedItems)
                                
                                // 2. Calcola la descrizione formattata per essere letta nativamente da scanner e contabilità
                                val descFormatted = if (!entry.storeVat.isNullOrBlank()) {
                                    "Frazione Scontrino ${entry.storeName} (PIVA ${entry.storeVat})"
                                } else {
                                    "Acquisto rapido ${entry.storeName}"
                                }
                                
                                // 3. Parsing del timestamp stringa del server (UTC ISO 8601) in millisecondi Unix Epoch
                                val parsedTime = try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(entry.timestamp)?.time 
                                        ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                                
                                // 4. Salva lo scontrino nel database Room locale come già sincronizzato (is_synced = true)
                                val newLocalEntry = com.example.data.LedgerEntry(
                                    description = descFormatted,
                                    amount = entry.amount,
                                    paidBy = entry.paidBy,
                                    paidByUserId = entry.paidByUserId,
                                    groupId = entry.groupId,
                                    timestamp = parsedTime,
                                    isSettled = false,
                                    receiptItemsJson = itemsJson,
                                    client_uuid = clientUuid,
                                    is_synced = true,
                                    created_at = entry.createdAt
                                )
                                dao.insertLedgerEntry(newLocalEntry)
                                
                                // 5. Registra ed associa il negozio locale per aggiornare la mappa geografica e geofence
                                val existingStore = dao.getStoreByName(entry.storeName) ?: (if (!entry.storeVat.isNullOrBlank()) dao.getStoreByVat(entry.storeVat) else null)
                                if (existingStore == null) {
                                    val newStore = com.example.data.StoreInfo(
                                        name = entry.storeName.lowercase().trim(),
                                        displayName = entry.storeName,
                                        vatNumber = entry.storeVat,
                                        address = entry.storeAddress,
                                        lastSeen = parsedTime
                                    )
                                    dao.insertStore(newStore)
                                } else {
                                    if (parsedTime > existingStore.lastSeen) {
                                        dao.updateStore(existingStore.copy(
                                            lastSeen = parsedTime,
                                            address = entry.storeAddress ?: existingStore.address
                                        ))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MasterSyncWorker", "Errore nell'aggiornamento degli scontrini di gruppo scaricati", e)
                }

                // --- NUOVA PARTE: Sincronizzazione anagrafica negozi da /api/v1/sync/stores ---
                try {
                    var reqLat: Double? = null
                    var reqLng: Double? = null
                    var reqCity: String? = null

                    val location = getLastLocation(applicationContext)
                    if (location != null) {
                        reqLat = location.latitude
                        reqLng = location.longitude
                        val detectedCity = getCityFromLocation(applicationContext, location)
                        if (!detectedCity.isNullOrBlank()) {
                            reqCity = detectedCity
                            prefs.edit().putString("user_city", detectedCity).apply()
                        }
                    } else {
                        reqCity = prefs.getString("user_city", "Milano")
                    }

                    val remoteStores = com.example.api.LocalBackendServiceClient.syncStoresFromServer(
                        token = token,
                        latitude = reqLat,
                        longitude = reqLng,
                        city = reqCity
                    )
                    if (remoteStores.isNotEmpty()) {
                        for (remote in remoteStores) {
                            val normalizedName = remote.name.lowercase().trim()
                            val existingStore = dao.getStoreByName(normalizedName) ?: (if (!remote.vatNumber.isNullOrBlank()) dao.getStoreByVat(remote.vatNumber) else null)
                            if (existingStore == null) {
                                val newStore = com.example.data.StoreInfo(
                                    name = normalizedName,
                                    displayName = remote.displayName,
                                    vatNumber = remote.vatNumber,
                                    address = remote.address,
                                    latitude = remote.latitude,
                                    longitude = remote.longitude,
                                    phone = remote.phone,
                                    isCertified = remote.isCertified,
                                    lastSeen = System.currentTimeMillis()
                                )
                                dao.insertStore(newStore)
                            } else {
                                dao.updateStore(existingStore.copy(
                                    displayName = remote.displayName,
                                    vatNumber = remote.vatNumber ?: existingStore.vatNumber,
                                    address = remote.address ?: existingStore.address,
                                    phone = remote.phone ?: existingStore.phone,
                                    latitude = remote.latitude ?: existingStore.latitude,
                                    longitude = remote.longitude ?: existingStore.longitude,
                                    isCertified = remote.isCertified,
                                    lastSeen = java.lang.Math.max(existingStore.lastSeen, System.currentTimeMillis())
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MasterSyncWorker", "Errore nella sincronizzazione dell'anagrafica dei negozi", e)
                }
                
                return@withContext Result.success()
            } else {
                return@withContext Result.retry()
            }

        } catch (e: Exception) {
            Log.e("MasterSyncWorker", "Error in sync", e)
            return@withContext Result.retry()
        }
    }

    private suspend fun getLastLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnCompleteListener { task ->
                if (continuation.isActive) {
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MasterSyncWorker", "Failed to retrieve location", e)
            if (continuation.isActive) continuation.resume(null)
        }
    }

    private suspend fun getCityFromLocation(context: Context, location: Location): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                addresses?.firstOrNull()?.locality
            } catch (e: Exception) {
                Log.e("MasterSyncWorker", "Error getting city from geocoder", e)
                null
            }
        }
    }
}
