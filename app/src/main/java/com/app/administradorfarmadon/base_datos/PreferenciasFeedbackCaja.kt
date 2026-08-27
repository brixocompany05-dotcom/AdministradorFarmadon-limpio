package com.app.administradorfarmadon.base_datos

import android.content.Context
import android.content.SharedPreferences

object PreferenciasFeedbackCaja {
    private const val PREFS = "preferencias_feedback_caja"
    private const val KEY_POS_ACTIVO = "pos_activo"
    private const val KEY_ACTIVO = "vibracion_activa"
    private const val KEY_SONIDO_ACTIVO = "sonido_activo"
    private const val KEY_HAPTICA_ACTIVA = "haptica_activa"
    private const val KEY_IA_INVENTARIO_ACTIVA = "ia_inventario_activa"

    fun estaActivo(context: Context): Boolean {
        return obtenerPrefs(context)?.getBoolean(KEY_ACTIVO, true) ?: true
    }

    fun setActivo(context: Context, activo: Boolean) {
        obtenerPrefs(context)?.edit()?.putBoolean(KEY_ACTIVO, activo)?.apply()
    }

    fun setSonidoActivo(context: Context, activo: Boolean) {
        obtenerPrefs(context)?.edit()?.putBoolean(KEY_SONIDO_ACTIVO, activo)?.apply()
    }

    fun setHapticaActiva(context: Context, activo: Boolean) {
        obtenerPrefs(context)?.edit()?.putBoolean(KEY_HAPTICA_ACTIVA, activo)?.apply()
    }

    fun estaSonidoActivo(context: Context): Boolean {
        return obtenerPrefs(context)?.getBoolean(KEY_SONIDO_ACTIVO, true) ?: true
    }

    fun estaHapticaActiva(context: Context): Boolean {
        return obtenerPrefs(context)?.getBoolean(KEY_HAPTICA_ACTIVA, true) ?: true
    }

    fun estaIaInventarioActiva(context: Context): Boolean {
        return obtenerPrefs(context)?.getBoolean(KEY_IA_INVENTARIO_ACTIVA, true) ?: true
    }

    fun setIaInventarioActiva(context: Context, activo: Boolean) {
        obtenerPrefs(context)?.edit()?.putBoolean(KEY_IA_INVENTARIO_ACTIVA, activo)?.apply()
    }

    private fun obtenerPrefs(context: Context): SharedPreferences? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
