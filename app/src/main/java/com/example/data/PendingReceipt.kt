package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_receipts")
data class PendingReceipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeName: String,
    val location: String = "Vicino a te",
    val timestamp: Long = System.currentTimeMillis()
)
