package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muted_stores")
data class MutedStoreLocal(
    @PrimaryKey val storeId: Int,
    val reason: String,
    val customComment: String? = null,
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
