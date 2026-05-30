package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "spending_groups")
data class SpendingGroup(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdByUserId: String,
    val isDefault: Boolean = false
)
