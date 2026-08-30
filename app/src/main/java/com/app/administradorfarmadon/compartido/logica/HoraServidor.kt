package com.app.administradorfarmadon.compartido.logica

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hora blindada del servidor para INVENTARIO.
 *
 * ¿Por qué existe? Los filtros de vencimiento, las alertas y la validación de lotes
 * comparan fechas contra "hoy". Si "hoy" viene del reloj de la tablet y la tablet
 * está mal ajustada, un lote vigente aparece vencido (o el vencido sigue vendiéndose).
 *
 * Cómo funciona: al arrancar la APP se mide la diferencia entre el reloj del
 * dispositivo y el reloj del servidor (vía Firestore), y desde entonces [ahoraMs]
 * devuelve hora corregida. Si la sincronización no aterriza, se reutiliza el íšLTIMO
 * offset bueno guardado; solo si NUNCA se obtuvo uno, [estaSincronizada] es false y
 * la UI debe advertir (no usar reloj local en silencio).
 *
 * Es puro: sin dependencias de Android/Firebase. La medición y persistencia las hace
 * [RelojServidorSincronizador] (quien tiene la base de datos y el Context a mano).
 */
object HoraServidor {

    private val _offsetMs = MutableStateFlow(0L)
    private val _sincronizada = MutableStateFlow(false)

    /** Hora de referencia corregida por servidor (ms). Sin offset conocido = reloj local. */
    fun ahoraMs(): Long = System.currentTimeMillis() + _offsetMs.value

    /** Instalado tras una lectura exitosa contra el servidor (o al restaurar el guardado). */
    fun establecerOffset(ms: Long) {
        _offsetMs.value = ms
        _sincronizada.value = true
    }

    /** Reinicia el estado a "sin offset conocido" (usado si no hay nada guardado al arrancar). */
    fun invalidarOffset() {
        _sincronizada.value = false
    }

    /** true si contamos con un offset medido alguna vez (aunque venga de un arranque previo). */
    val estaSincronizada: StateFlow<Boolean> get() = _sincronizada
}
