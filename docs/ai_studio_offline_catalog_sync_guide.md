# Handover Guide: Offline Catalog Sync & Editing Catalog Details from App

This guide outlines the changes required in the Android app to:
1. **Prevent UI hangs** when saving a scanned shelf label while offline.
2. **Queue catalog scans** when offline using a new Room database table (`pending_catalog_items`) and upload them dynamically using the background `MasterSyncWorker`.
3. **Allow viewing, editing, and deleting catalog items** directly from the price comparison dialog inside the "Smart Shopping" tab.

---

## 🛠️ Proposed Changes

### 1. Define the Offline Catalog Sync Entity

#### [NEW] [PendingCatalogItem.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/data/PendingCatalogItem.kt)

Create a new Room entity to store shelf labels scanned while offline:

```kotlin
package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_catalog_items")
data class PendingCatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val brand: String?,
    val category: String?,
    val price: Double,
    val unitPrice: Double?,
    val weight: Double?,
    val discountLabel: String?,
    val storeName: String,
    val vatNumber: String?,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

### 2. Update Room Database Schema

#### [MODIFY] [Database.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/data/Database.kt)

1. Add `PendingCatalogItem::class` to the `@Database` entities list.
2. Increment the database version from `14` to `15` and keep `.fallbackToDestructiveMigration()` active.
3. Add the following DAO operations to the `GroceryDao` interface:

```kotlin
    // --- Pending Catalog Items (Offline Queue) ---
    @Query("SELECT * FROM pending_catalog_items ORDER BY timestamp ASC")
    suspend fun getPendingCatalogItems(): List<PendingCatalogItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingCatalogItem(item: PendingCatalogItem): Long

    @Query("DELETE FROM pending_catalog_items WHERE id = :id")
    suspend fun deletePendingCatalogItemById(id: Int)
```

---

### 3. Update repository to support Dao operations

#### [MODIFY] [Repository.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/data/Repository.kt)

Add methods to handle the offline catalog queue:

```kotlin
    suspend fun getPendingCatalogItems(): List<PendingCatalogItem> = groceryDao.getPendingCatalogItems()
    suspend fun insertPendingCatalogItem(item: PendingCatalogItem) = groceryDao.insertPendingCatalogItem(item)
    suspend fun deletePendingCatalogItemById(id: Int) = groceryDao.deletePendingCatalogItemById(id)
```

---

### 4. Update the Client API Service

#### [MODIFY] [LocalBackendService.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/api/LocalBackendService.kt)

1. Update `CatalogPriceComparisonItem` and `CatalogItemCompareResponse` data classes to match the new backend properties:
   - `CatalogPriceComparisonItem`: Add `@Json(name = "catalog_item_id") val catalogItemId: Int`
   - `CatalogItemCompareResponse`: Add `@Json(name = "category") val category: String?` and `@Json(name = "weight") val weight: Double?`

2. Implement the API endpoints for updating and deleting catalog items inside the `LocalBackendServiceClient` singleton:

```kotlin
    suspend fun updateCatalogItem(token: String?, itemId: Int, item: CatalogItemCreate): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/scan/catalog/$itemId"
        val json = moshi.adapter(CatalogItemCreate::class.java).toJson(item)
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        val request = reqBuilder.put(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { return@withContext it.isSuccessful }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun deleteCatalogItem(token: String?, itemId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/scan/catalog/$itemId"
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        val request = reqBuilder.delete().build()
        try {
            client.newCall(request).execute().use { return@withContext it.isSuccessful }
        } catch (e: Exception) {
            return@withContext false
        }
    }
```

---

### 5. Prevent UI Hangs on Scanner and Queue Offline Scans

#### [MODIFY] [GroceryViewModel.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/ui/viewmodel/GroceryViewModel.kt)

Update `completeCameraShelfScan(item: com.example.api.CatalogItemCreate)` to check connectivity via `pingBackend()` first. If the backend is offline, save to the offline queue immediately and dismiss the dialog:

```kotlin
    fun completeCameraShelfScan(item: com.example.api.CatalogItemCreate) {
        viewModelScope.launch {
            val token = getApplication<Application>().getSharedPreferences("smart_grocery_prefs", android.content.Context.MODE_PRIVATE).getString("user_token", null)
            val deviceId = getApplication<Application>().getSharedPreferences("smart_grocery_prefs", android.content.Context.MODE_PRIVATE).getString("device_uuid", null) ?: java.util.UUID.randomUUID().toString()
            
            // Fast ping check to see if server is alive
            val isBackendAlive = !isOfflineMode.value && com.example.api.LocalBackendServiceClient.pingBackend()
            
            val barcode = item.barcode
            val price = item.price ?: 0.0
            
            val ok = if (isBackendAlive) {
                com.example.api.LocalBackendServiceClient.submitShelfLabel(token, deviceId, item)
            } else {
                false
            }
            
            if (ok) {
                simulateWebSocketNotification("Assistente Scaffale AR: EAN $barcode registrato a €${String.format(java.util.Locale.US, "%.2f", price)}")
                fetchComparison(barcode, null)
                requestTabSwitch.value = 4 // Index for Stores tab
            } else {
                // If offline, save in Room pending catalog sync queue
                val pendingCatalogItem = com.example.data.PendingCatalogItem(
                    barcode = barcode,
                    name = item.name,
                    brand = item.brand,
                    category = item.category,
                    price = price,
                    unitPrice = item.unitPrice,
                    weight = item.weight,
                    discountLabel = item.discountLabel,
                    storeName = item.storeName ?: "Supermercato",
                    vatNumber = item.vatNumber
                )
                repository.insertPendingCatalogItem(pendingCatalogItem)
                
                // Fallback shopping list item insertion (local list update)
                val gItem = GroceryItem(
                    name = item.name,
                    brand = item.brand ?: "Rilevamento AR",
                    price = price,
                    unitPrice = item.unitPrice ?: price,
                    category = item.category ?: "Dispensa",
                    isShared = true,
                    isPurchased = false,
                    urgencyColor = "GREEN",
                    lastPurchaseTimestamp = System.currentTimeMillis(),
                    barcode = barcode
                )
                repository.insertItem(gItem)
                simulateWebSocketNotification("Salvato in locale: EAN $barcode accodato per il sync offline.")
            }
            // Dismiss scan result card overlay instantly!
            parsedShelfLabelScanResult.value = null
        }
    }
```

Add ViewModel operations to handle edits and deletions:

```kotlin
    fun updateCatalogItem(itemId: Int, item: com.example.api.CatalogItemCreate) {
        viewModelScope.launch {
            val token = getApplication<Application>().getSharedPreferences("smart_grocery_prefs", android.content.Context.MODE_PRIVATE).getString("user_token", null)
            val success = com.example.api.LocalBackendServiceClient.updateCatalogItem(token, itemId, item)
            if (success) {
                simulateWebSocketNotification("Prodotto del catalogo aggiornato con successo!")
                fetchComparison(item.barcode, item.name) // Reload comparison
            } else {
                simulateWebSocketNotification("Errore durante l'aggiornamento del catalogo.")
            }
        }
    }

    fun deleteCatalogItem(itemId: Int, barcode: String?, name: String) {
        viewModelScope.launch {
            val token = getApplication<Application>().getSharedPreferences("smart_grocery_prefs", android.content.Context.MODE_PRIVATE).getString("user_token", null)
            val success = com.example.api.LocalBackendServiceClient.deleteCatalogItem(token, itemId)
            if (success) {
                simulateWebSocketNotification("Prodotto rimosso dal catalogo.")
                fetchComparison(barcode, name) // Reload comparison
            } else {
                simulateWebSocketNotification("Errore durante l'eliminazione.")
            }
        }
    }
```

---

### 6. Implement Dynamic Background Uploading

#### [MODIFY] [MasterSyncWorker.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/workers/MasterSyncWorker.kt)

Add a new routine at the end of the `doWork()` function (before `return@withContext Result.success()`) to upload pending catalog items from the offline database cache:

```kotlin
            // --- NUOVA PARTE: Sincronizzazione scansioni catalog/scaffale accumulate in offline ---
            try {
                val pendingCatalogItems = dao.getPendingCatalogItems()
                if (pendingCatalogItems.isNotEmpty()) {
                    Log.d("MasterSyncWorker", "Found ${pendingCatalogItems.size} pending offline catalog items. Syncing...")
                    for (item in pendingCatalogItems) {
                        val createDto = com.example.api.CatalogItemCreate(
                            barcode = item.barcode,
                            name = item.name,
                            brand = item.brand,
                            category = item.category,
                            price = item.price,
                            unitPrice = item.unitPrice,
                            weight = item.weight,
                            discountLabel = item.discountLabel,
                            storeName = item.storeName,
                            vatNumber = item.vatNumber
                        )
                        val ok = LocalBackendServiceClient.submitShelfLabel(token, deviceUuid, createDto)
                        if (ok) {
                            dao.deletePendingCatalogItemById(item.id)
                            Log.d("MasterSyncWorker", "Synced catalog item: ${item.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MasterSyncWorker", "Errore nella sincronizzazione delle scansioni catalog accumulate", e)
            }
```

---

### 7. Enhance the Price Comparison & Details Dialog

#### [MODIFY] [StoresScreen.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/ui/screens/StoresScreen.kt)

Modify the Dialog `comparison != null` to display additional catalog details (Category, Weight) and add Edit/Delete options next to each store price entry:

1. **Details Display**:
   At the top of the Dialog, show the product information:
   - Category: `comparison!!.category ?: "Dispensa"`
   - Weight: `comparison!!.weight?.let { "${it} kg/L" } ?: "N/D"`

2. **Edit and Delete Actions**:
   Inside the list of comparison prices (`comparison!!.prices`), show:
   - An Edit icon (pencil) that opens a sub-dialog letting the user modify name, brand, category, weight, price, discount label, and store name. When saved, it calls `viewModel.updateCatalogItem(priceItem.catalogItemId, CatalogItemCreate(...))`.
   - A Delete icon (trash) that calls `viewModel.deleteCatalogItem(priceItem.catalogItemId, comparison!!.barcode, comparison!!.productName)`.
