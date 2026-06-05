package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_info")
data class StoreInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Normalized name (e.g., "esselunga", "conad")
    val displayName: String, // Display name
    val vatNumber: String? = null, // Partita IVA (11 digits, optional for unique key)
    val address: String? = null, // Address (if present)
    val phone: String? = null, // Telephone number (optional)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadius: Float = 100f, // Meters
    val isCertified: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
