package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_catalog_items")
data class PendingCatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val unitPrice: Double? = null,
    val weight: Double? = null,
    val discountLabel: String? = null,
    val storeName: String? = null,
    val vatNumber: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
