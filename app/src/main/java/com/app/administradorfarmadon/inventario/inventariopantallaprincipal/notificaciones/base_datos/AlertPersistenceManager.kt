package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.base_datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Gestor real de alertas vistas por sede — una sola verdad Firestore.
 * Estructura: farmaciapp/app/farmacias/{farmaciaId}/sucursales/{sucursalId}/alertasInventario/{productId}
 * Compartido por todos los usuarios de la sede: si Ana ve "quedan 2u", Luis no es molestado de nuevo
 * hasta que la condición cambie (stock repuesto y vuelve a caer).
 * Sin mentira: solo se considera visto si el mensaje exacto ya fue guardado y confirmado por el servidor.
 */
object AlertPersistenceManager {
    private const val TAG = "AlertPersistence"
    private val db = FirebaseFirestore.getInstance()

    data class AlertReadRecord(
        val lastMessage: String = "",
        val timestamp: Long = 0L
    )

    fun readKey(productId: String, tipo: String): String = "${productId}_${tipo}"

    suspend fun markAsRead(clienteId: String, productId: String, tipo: String, message: String) {
        if (clienteId.isBlank() || productId.isBlank()) return
        val key = readKey(productId, tipo)
        val doc = FarmadonPaths.alertasInventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(key)
        val data = mapOf(
            "productId" to productId,
            "tipo" to tipo,
            "lastMessage" to message,
            "timestamp" to System.currentTimeMillis(),
            "vistoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        try {
            doc.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo guardar alerta vista $key: ${e.message}", e)
            throw e
        }
    }

    /**
     * Marca alerta como vista para TODA la sede (no por usuario).
     * Falla visible si no hay red — no finge éxito.
     */
    suspend fun markAsRead(clienteId: String, productId: String, message: String) {
        markAsRead(clienteId, productId, "STOCK", message)
    }

    /**
     * Obtiene mapa de alertas ya vistas por la sede.
     * Si hay error de red, propaga excepción para que el panel no mienta mostrando todo como no visto.
     */
    suspend fun getReadAlerts(clienteId: String): Map<String, AlertReadRecord> {
        if (clienteId.isBlank()) return emptyMap()
        return try {
            val snap = FarmadonPaths.alertasInventario(db, clienteId, SessionManager.sucursalIdEfectiva).get().await()
            snap.documents.mapNotNull { doc ->
                val pid = doc.id
                val msg = doc.getString("lastMessage") ?: ""
                val ts = doc.getLong("timestamp") ?: 0L
                if (msg.isBlank()) null else pid to AlertReadRecord(msg, ts)
            }.toMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo alertas vistas: ${e.message}", e)
            throw e
        }
    }

}
