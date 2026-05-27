package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val paidBy: String, // "Io" or "Partner" (as designated in Section 7)
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    val receiptItemsJson: String? = null,
    val client_uuid: String = UUID.randomUUID().toString(),
    val is_synced: Boolean = false
)
