package com.example.data

import kotlinx.coroutines.flow.Flow

class GroceryRepository(private val dao: GroceryDao) {

    // --- Items ---
    val allItems: Flow<List<GroceryItem>> = dao.getAllItemsFlow()
    val shoppingList: Flow<List<GroceryItem>> = dao.getShoppingListFlow()

    suspend fun insertItem(item: GroceryItem): Long = dao.insertItem(item)
    suspend fun updateItem(item: GroceryItem) = dao.updateItem(item)
    suspend fun deleteItem(item: GroceryItem) = dao.deleteItem(item)
    suspend fun deleteItemById(id: Int) = dao.deleteItemById(id)
    suspend fun deleteItemsByTimestampAndStore(timestamp: Long, storeName: String) = dao.deleteItemsByTimestampAndStore(timestamp, storeName)
    suspend fun clearAllItems() = dao.deleteAllItems()

    // --- Pending Receipts ---
    val pendingReceipts: Flow<List<PendingReceipt>> = dao.getPendingReceiptsFlow()

    suspend fun getPendingReceiptById(id: Long): PendingReceipt? {
        return dao.getPendingReceiptById(id)
    }

    suspend fun insertPendingReceipt(receipt: PendingReceipt): Long = dao.insertPendingReceipt(receipt)
    suspend fun deletePendingReceipt(receipt: PendingReceipt) = dao.deletePendingReceipt(receipt)
    suspend fun deletePendingReceiptById(id: Int) = dao.deletePendingReceiptById(id)

    // --- Pending Catalog Items ---
    val pendingCatalogItems: Flow<List<PendingCatalogItem>> = dao.getPendingCatalogItemsFlow()
    suspend fun getAllPendingCatalogItems(): List<PendingCatalogItem> = dao.getAllPendingCatalogItems()
    suspend fun insertPendingCatalogItem(item: PendingCatalogItem): Long = dao.insertPendingCatalogItem(item)
    suspend fun deletePendingCatalogItem(item: PendingCatalogItem) = dao.deletePendingCatalogItem(item)
    suspend fun deletePendingCatalogItemById(id: Int) = dao.deletePendingCatalogItemById(id)

    // --- Ledger Entries ---
    val ledgerEntries: Flow<List<LedgerEntry>> = dao.getLedgerEntriesFlow()

    suspend fun getLedgerEntryById(id: Int): LedgerEntry? = dao.getLedgerEntryById(id)

    suspend fun getAllLedgerEntries(): List<LedgerEntry> = dao.getAllLedgerEntries()

    suspend fun insertLedgerEntry(entry: LedgerEntry): Long = dao.insertLedgerEntry(entry)
    suspend fun updateLedgerEntry(entry: LedgerEntry) = dao.updateLedgerEntry(entry)
    suspend fun settleAllLedgerEntries() = dao.settleAllLedgerEntries()
    suspend fun deleteLedgerEntry(entry: LedgerEntry) = dao.deleteLedgerEntry(entry)

    // --- Store Info Registry ---
    val allStores: Flow<List<StoreInfo>> = dao.getAllStoresFlow()

    suspend fun getStoreByName(name: String): StoreInfo? = dao.getStoreByName(name)
    suspend fun getStoreByVat(vatNumber: String): StoreInfo? = dao.getStoreByVat(vatNumber)
    suspend fun insertStore(store: StoreInfo): Long = dao.insertStore(store)
    suspend fun updateStore(store: StoreInfo) = dao.updateStore(store)
    suspend fun deleteStore(store: StoreInfo) = dao.deleteStore(store)

    // --- Notification Acks (Offline Queue) ---
    suspend fun getUnsyncedNotificationAcks(): List<NotificationAck> = dao.getUnsyncedNotificationAcks()
    suspend fun insertNotificationAck(ack: NotificationAck): Long = dao.insertNotificationAck(ack)
    suspend fun updateNotificationAck(ack: NotificationAck) = dao.updateNotificationAck(ack)
    suspend fun deleteSyncedNotificationAcks(ids: List<Int>) = dao.deleteSyncedNotificationAcks(ids)

    val unreadNotifications: Flow<List<BackendNotificationEntity>> = dao.getUnreadNotificationsFlow()
    val allNotifications: Flow<List<BackendNotificationEntity>> = dao.getAllNotificationsFlow()
    
    suspend fun markNotificationAsRead(id: Int) = dao.markNotificationAsRead(id)
    suspend fun deleteNotification(id: Int) = dao.deleteNotification(id)
    suspend fun deleteAllNotifications() = dao.deleteAllNotifications()

    // --- Spending Groups ---
    val spendingGroups: Flow<List<SpendingGroup>> = dao.getSpendingGroupsFlow()
    suspend fun insertSpendingGroups(groups: List<SpendingGroup>) {
        dao.deleteAllSpendingGroups()
        dao.insertSpendingGroups(groups)
    }

    // --- Shopping Lists ---
    val shoppingLists: Flow<List<ShoppingList>> = dao.getShoppingListsFlow()
    suspend fun insertShoppingLists(lists: List<ShoppingList>) {
        dao.deleteAllShoppingLists()
        dao.insertShoppingLists(lists)
    }
}
