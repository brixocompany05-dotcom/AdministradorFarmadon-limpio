package com.app.administradorfarmadon.configuracion.datos

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Gestor de configuraciones de Experiencia de Usuario (UX) y comportamiento del Sistema.
 * Persiste las preferencias locales de la tienda (escáner, impresión, etc.).
 */
object UxSettingsManager {
    private const val PREF_NAME = "ux_settings"
    private var prefs: SharedPreferences? = null

    // --- Preferencias de Sistema ---
    var impresionAutomatica: Boolean by BooleanPreference("impresion_automatica", false)
    var sonidosOperacion: Boolean by BooleanPreference("sonidos_operacion", true)
    
    // --- Preferencias de UX ---
    var modoListaCompacta: Boolean by BooleanPreference("modo_lista_compacta", false)
    var confirmarSalidaVentas: Boolean by BooleanPreference("confirmar_salida_ventas", true)
    var busquedaContinua: Boolean by BooleanPreference("busqueda_continua", false)

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
