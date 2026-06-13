package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_store_reports")
data class PendingStoreReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val displayName: String,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val latitude: Double,
    val longitude: Double,
    val storeType: String = "SUPERMARKET",
    val timestamp: Long = System.currentTimeMillis()
)
