package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    // --- Grocery Items ---
    @Query("SELECT * FROM items ORDER BY id DESC")
    fun getAllItemsFlow(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM items WHERE isPurchased = 0 ORDER BY id DESC")
    fun getShoppingListFlow(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GroceryItem): Long

    @Update
    suspend fun updateItem(item: GroceryItem)

    @Delete
    suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM items WHERE lastPurchaseTimestamp = :timestamp AND storeName = :storeName")
    suspend fun deleteItemsByTimestampAndStore(timestamp: Long, storeName: String)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    // --- Pending Receipts ---
    @Query("SELECT * FROM pending_receipts ORDER BY timestamp DESC")
    fun getPendingReceiptsFlow(): Flow<List<PendingReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingReceipt(receipt: PendingReceipt): Long

    @Delete
    suspend fun deletePendingReceipt(receipt: PendingReceipt)

    @Query("DELETE FROM pending_receipts WHERE id = :id")
    suspend fun deletePendingReceiptById(id: Int)

    // --- Ledger Entries ---
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getLedgerEntriesFlow(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE id = :id LIMIT 1")
    suspend fun getLedgerEntryById(id: Int): LedgerEntry?

    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    suspend fun getAllLedgerEntries(): List<LedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntry): Long

    @Update
    suspend fun updateLedgerEntry(entry: LedgerEntry)

    @Query("UPDATE ledger_entries SET isSettled = 1 WHERE isSettled = 0")
    suspend fun settleAllLedgerEntries()

    @Delete
    suspend fun deleteLedgerEntry(entry: LedgerEntry)

    // --- Store Info Registry ---
    @Query("SELECT * FROM store_info ORDER BY lastSeen DESC")
    fun getAllStoresFlow(): Flow<List<StoreInfo>>

    @Query("SELECT * FROM store_info WHERE name = :name LIMIT 1")
    suspend fun getStoreByName(name: String): StoreInfo?

    @Query("SELECT * FROM store_info WHERE vatNumber = :vatNumber LIMIT 1")
    suspend fun getStoreByVat(vatNumber: String): StoreInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreInfo): Long

    @Update
    suspend fun updateStore(store: StoreInfo)

    @Delete
    suspend fun deleteStore(store: StoreInfo)

    // --- Notification Acks (Offline Queue) ---
    @Query("SELECT * FROM notification_acks WHERE isSynced = 0")
    suspend fun getUnsyncedNotificationAcks(): List<NotificationAck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationAck(ack: NotificationAck): Long

    @Update
    suspend fun updateNotificationAck(ack: NotificationAck)

    @Query("DELETE FROM notification_acks WHERE isSynced = 1")
    suspend fun deleteSyncedNotificationAcks()
}

@Database(
    entities = [GroceryItem::class, PendingReceipt::class, LedgerEntry::class, StoreInfo::class, NotificationAck::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groceryDao(): GroceryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_grocery_database"
                )
                    .fallbackToDestructiveMigration() // ensures safety during rapid development updates
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
