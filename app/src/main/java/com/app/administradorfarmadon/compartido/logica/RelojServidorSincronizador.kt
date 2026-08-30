package com.app.administradorfarmadon.compartido.logica

import android.content.Context
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/**
 * Fuente única para medir y guardar el desfase contra el reloj de Firestore.
 * Centraliza la lógica que antes duplicaban InventarioViewModel y SidebarViewModel.
 *
 * - [cargar]: al arrancar la APP restaura el último offset bueno conocido (persistido),
 *   para no caer a reloj local si el arranque es offline.
 * - [sincronizar]: mide el offset contra el servidor y lo persiste.
 *
 * Si NUNCA se obtuvo un offset (offline total desde el primer uso), [HoraServidor]
 * queda sin offset conocido y la UI debe advertir al usuario (no usar reloj local en silencio).
 */
object RelojServidorSincronizador {
    private const val PREF_NAME = "reloj_servidor_v1"
    private const val KEY_OFFSET = "offset_ms"

    suspend fun sincronizar(context: Context): Boolean {
        repeat(3) { intento ->
            try {
                val db = FarmadonFirestore.db
                val docRef = db.collection("_health").document("ping")
                    .collection("hora_sync").document()
                com.google.android.gms.tasks.Tasks.await(
                    docRef.set(mapOf("ts" to FieldValue.serverTimestamp()))
                )
                val srv = docRef.get().await().getTimestamp("ts")?.toDate()?.time
                if (srv != null) {
                    val offset = srv - System.currentTimeMillis()
                    HoraServidor.establecerOffset(offset)
                    guardar(context, offset)
                    android.util.Log.i("RelojServidor", "offset servidor instalado=$offset ms (intento ${intento + 1})")
                    return true
                }
            } catch (e: Exception) {
                android.util.Log.w("RelojServidor", "Sync hora falló (intento ${intento + 1}): ${e.message}")
            }
        }
        return false
    }

    fun cargar(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val offset = prefs.getLong(KEY_OFFSET, Long.MIN_VALUE)
        if (offset != Long.MIN_VALUE) {
            HoraServidor.establecerOffset(offset)
        } else {
            HoraServidor.invalidarOffset()
        }
    }

    private fun guardar(context: Context, offset: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_OFFSET, offset).apply()
    }
}
