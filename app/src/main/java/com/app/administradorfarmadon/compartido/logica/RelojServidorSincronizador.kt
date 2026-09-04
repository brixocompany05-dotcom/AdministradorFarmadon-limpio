package com.app.administradorfarmadon.compartido.logica

import android.content.Context

/**
 * Gestor de sincronización de hora del servidor.
 */
object RelojServidorSincronizador {
    private const val PREF_NAME = "reloj_servidor_v1"
    private const val KEY_OFFSET = "offset_ms"

    suspend fun sincronizar(context: Context): Boolean {
        cargar(context)
        return true
    }

    fun cargar(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val offset = prefs.getLong(KEY_OFFSET, 0L)
        HoraServidor.establecerOffset(offset)
    }

    private fun guardar(context: Context, offset: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_OFFSET, offset).apply()
    }
}
