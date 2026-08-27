package com.app.administradorfarmadon.notificaciones

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.app.administradorfarmadon.PantallaPrincipal
import com.app.administradorfarmadon.R

/**
 * Despachador de notificaciones de stock.
 * Construye notificaciones individuales y agrupadas respetando los estándares de Android.
 */
object StockNotificationDispatcher {

    private const val GROUP_KEY_STOCK = "com.app.administradorfarmadon.STOCK_ALERTS"
    private const val SUMMARY_NOTIFICATION_ID = 9000

    /**
     * Muestra una notificación de stock bajo para un producto específico.
     */
    fun showStockAlert(
        context: Context,
        productId: String,
        productName: String,
        currentStock: Double,
        minStock: Double
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, PantallaPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PRODUCT_ID", productId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            productId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatStock = if (currentStock % 1.0 == 0.0) currentStock.toInt().toString() else currentStock.toString()
        val formatMin = if (minStock % 1.0 == 0.0) minStock.toInt().toString() else minStock.toString()

        val title = "⚠️ Stock Bajo: $productName"
        val message = "Stock disponible: $formatStock (Mínimo requerido: $formatMin)"

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_STOCK_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_STOCK)
            .build()

        val summaryNotification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_STOCK_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("Alertas de Stock Bajo")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY_STOCK)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        val notificationId = kotlin.math.abs(productId.hashCode())
        notificationManager.notify(notificationId, notification)
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }
}
