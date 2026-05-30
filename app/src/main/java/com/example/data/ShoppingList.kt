package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdByUserId: Int,
    val isShared: Boolean = false,
    val sharedWithGroupIds: List<String> = emptyList(),
    val sharedWithUserIds: List<String> = emptyList(),
    val items: List<com.example.api.ShoppingListItemResponse> = emptyList()
)
