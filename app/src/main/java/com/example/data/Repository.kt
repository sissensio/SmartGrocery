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
    suspend fun clearAllItems() = dao.deleteAllItems()

    // --- Pending Receipts ---
    val pendingReceipts: Flow<List<PendingReceipt>> = dao.getPendingReceiptsFlow()

    suspend fun insertPendingReceipt(receipt: PendingReceipt): Long = dao.insertPendingReceipt(receipt)
    suspend fun deletePendingReceipt(receipt: PendingReceipt) = dao.deletePendingReceipt(receipt)
    suspend fun deletePendingReceiptById(id: Int) = dao.deletePendingReceiptById(id)

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
}
