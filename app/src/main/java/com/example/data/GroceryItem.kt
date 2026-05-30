package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String = "",
    val price: Double = 0.0,
    val unitPrice: Double = 0.0, // price per kg or liter
    val category: String = "Dispensa",
    val isShared: Boolean = true, // Smart-Split
    val listId: String = "", // Added to link to a ShoppingList
    val isPurchased: Boolean = false,
    val urgencyColor: String = "GREEN", // RED, YELLOW, GREEN based on Daily Need
    val purchaseCount: Int = 1,
    val averageDeltaDays: Double = 5.0, // average consumption interval (μ)
    val lastPurchaseTimestamp: Long = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L), // default: 5 days ago
    val storeName: String = "",
    val barcode: String = ""
)
