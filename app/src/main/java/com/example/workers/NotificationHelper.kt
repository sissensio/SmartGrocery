package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "geofence_channel"
    private const val SYS_CHANNEL_ID = "system_channel"

    fun showGeofenceCheckoutNotification(context: Context, storeName: String, pendingReceiptId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifiche Negozio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche su scontrini in sospeso quando esci da un negozio"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link intent for SI
        val intentSi = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_SCAN_PENDING_RECEIPT"
            putExtra("pending_receipt_id", pendingReceiptId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentSi = PendingIntent.getActivity(
            context,
            pendingReceiptId,
            intentSi,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Broadcast intent for DOPO (dismissing the notification but keeping the pending receipt)
        // Since we already created the PendingReceipt in DB, "DOPO" just dismisses the notification.
        // We can just use standard cancellation or add an explicit action if we want to log it, but standard swipe is enough.
        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = "ACTION_DOPO_DISMISS"
            putExtra("notification_id", pendingReceiptId)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle("Sei uscito da $storeName?")
            .setContentText("Sembra che tu sia uscito da $storeName. Vuoi scansionare lo scontrino ora?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_camera, "SÌ", pendingIntentSi)
            // DOPO just cancels the notification, the receipt is already in DB
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DOPO", 
                PendingIntent.getBroadcast(context, pendingReceiptId + 1000, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()

        notificationManager.notify(pendingReceiptId, notification)
    }

    fun showBackendNotification(context: Context, notificationId: Long, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYS_CHANNEL_ID,
                "Comunicazioni di Sistema",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche generali dal backend"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SYS_CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId.toInt(), notification)
    }
}
