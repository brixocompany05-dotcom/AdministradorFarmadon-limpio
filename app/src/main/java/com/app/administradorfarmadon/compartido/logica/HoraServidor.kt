package com.app.administradorfarmadon.compartido.logica

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hora blindada del servidor para INVENTARIO.
 *
 * ¿Por qué existe? Los filtros de vencimiento, las alertas y la validación de lotes
 * comparan fechas contra "hoy". Si "hoy" viene del reloj de la tablet y la tablet
 * está mal ajustada, un lote vigente aparece vencido (o el vencido sigue vendiéndose).
 *
 * Cómo funciona: al arrancar el inventario se mide UNA VEZ la diferencia entre el
 * reloj del dispositivo y el reloj del servidor (vía Firestore), y desde entonces
 * [ahoraMs] devuelve hora corregida. Si la sincronización aún no aterriza,
 * devuelve el reloj local tal cual (degradación honesta, jamás inventa fecha).
 *
 * Es puro: sin dependencias de Android/Firebase. La medición la hace quien tiene
 * la base de datos a mano (ver InventarioViewModel.sincronizarHoraServidor).
 */
object HoraServidor {

    private val _offsetMs = MutableStateFlow(0L)

    /** Hora de referencia corregida por servidor (ms). Sin sincronizar = reloj local. */
    fun ahoraMs(): Long = System.currentTimeMillis() + _offsetMs.value

    /** Instalado por quien mide contra el servidor tras una lectura exitosa. */
    fun establecerOffset(ms: Long) {
        _offsetMs.value = ms
    }
}
