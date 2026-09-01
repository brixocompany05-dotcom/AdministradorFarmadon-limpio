package com.app.administradorfarmadon.configuracion.preferencias_sistema.impresion.datos

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Gestión de preferencias de Impresión y Periféricos de salida.
 * Submódulo: Impresión
 */
object ImpresionPrefs {
    private const val PREF_NAME = "pref_sistema_impresion"
    private var prefs: SharedPreferences? = null

    var impresionAutomatica: Boolean by BooleanPreference("impresion_automatica", false)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    private class BooleanPreference(val key: String, val defaultValue: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            return prefs?.getBoolean(key, defaultValue) ?: defaultValue
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            prefs?.edit()?.putBoolean(key, value)?.apply()
        }
    }
}
