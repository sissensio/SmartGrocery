package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_acks")
data class NotificationAck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val notificationId: Int,
    val deviceUuid: String,
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
