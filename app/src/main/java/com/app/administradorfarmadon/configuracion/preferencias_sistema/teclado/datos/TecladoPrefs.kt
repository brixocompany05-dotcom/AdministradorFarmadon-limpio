package com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.datos

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestión de preferencias del Teclado y Escáner Externo.
 * Submódulo: Teclado
 */
object TecladoPrefs {
    private const val PREF_NAME = "pref_sistema_teclado"
    private var prefs: SharedPreferences? = null

    private val _bloquearTecladoFlow = MutableStateFlow(false)
    val bloquearTecladoFlow: StateFlow<Boolean> = _bloquearTecladoFlow

    /**
     * Si es TRUE, el teclado del dispositivo NUNCA debe abrirse en toda la app
     * (ventana principal y diálogos). Ideal para uso con pistolas de escaneo físicas.
     */
    var bloquearTeclado: Boolean
        get() = prefs?.getBoolean("bloquear_teclado", false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean("bloquear_teclado", value)?.apply()
            _bloquearTecladoFlow.value = value
        }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            _bloquearTecladoFlow.value = bloquearTeclado
        }
    }
}
