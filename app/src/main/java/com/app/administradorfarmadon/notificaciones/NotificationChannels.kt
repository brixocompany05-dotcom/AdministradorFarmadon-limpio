package com.app.administradorfarmadon.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Gestor de Canales de Notificación de FarmaDON.
 * Centraliza la creación de canales requeridos por Android O (API 26+).
 */
object NotificationChannels {

    const val CHANNEL_STOCK_ALERTS = "stock_alerts"
    const val CHANNEL_EXPIRY_ALERTS = "expiry_alerts"
    const val CHANNEL_SYSTEM_ALERTS = "system_alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val stockChannel = NotificationChannel(
            CHANNEL_STOCK_ALERTS,
            "Alertas de Stock",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de stock bajo y crítico por producto"
            enableVibration(true)
            enableLights(true)
        }

        val expiryChannel = NotificationChannel(
            CHANNEL_EXPIRY_ALERTS,
            "Alertas de Vencimiento",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de lotes por vencer o vencidos"
            enableVibration(true)
            enableLights(true)
        }

        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM_ALERTS,
            "Servicio de Monitoreo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servicio en primer plano para monitoreo de inventario"
            setShowBadge(false)
        }

        manager.createNotificationChannel(stockChannel)
        manager.createNotificationChannel(expiryChannel)
        manager.createNotificationChannel(systemChannel)
    }
}
