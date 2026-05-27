package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.api.BackendNotification

@Entity(tableName = "app_notifications")
data class BackendNotificationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String,
    val type: String,
    val targetStoreId: Int? = null,
    val targetCity: String? = null,
    val targetRegion: String? = null,
    val createdAt: String? = null,
    val isRead: Boolean = false
) {
    fun toApiModel(): BackendNotification {
        return BackendNotification(
            id = id,
            title = title,
            body = body,
            type = type,
            targetStoreId = targetStoreId,
            targetCity = targetCity,
            targetRegion = targetRegion,
            createdAt = createdAt
        )
    }
}
