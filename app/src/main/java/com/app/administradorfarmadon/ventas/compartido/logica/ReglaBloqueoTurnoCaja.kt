package com.app.administradorfarmadon.ventas.compartido.logica

import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoOperativoTurno
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * REGLA DE BLOQUEO OPERATIVO POR TURNO VENCIDO (R1/R3/R14).
 *
 * 🔒 Regla Fundamental:
 * "Turno vencido = Solo resolver el cierre"
 *
 * UNA REGLA → UNA FUENTE DE VERDAD → UNA DECISIÓN → UN CAMINO → UNA RESOLUCIÓN.
 *
 * Flujo Único:
 * TURNO VÁLIDO    → POS OPERATIVO
 * TURNO VENCIDO   → BLOQUEO OPERATIVO GLOBAL → CERRAR TURNO PENDIENTE → CIERRE CONFIRMADO → ABRIR TURNO ACTUAL → POS LIMPIO Y OPERATIVO
 */
object ReglaBloqueoTurnoCaja {

    val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")

    const val MENSAJE_BLOQUEO_TURNO_VENCIDO =
        "BLOQUEO OPERATIVO: Existe un turno de caja pendiente de cierre de una fecha anterior. No se permiten ventas ni operaciones en caja. La única acción permitida es resolver el cierre del turno."

    /**
     * Verifica si una marca temporal (en ms) corresponde a un día calendario previo según hora de Lima.
     */
    fun esFechaAnterior(timestampMs: Long, ahoraMs: Long = HoraServidor.ahoraMs()): Boolean {
        if (timestampMs <= 0L) return false
        val calTimestamp = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = timestampMs }
        val calHoy = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahoraMs }
        return calTimestamp.get(Calendar.YEAR) < calHoy.get(Calendar.YEAR) ||
                (calTimestamp.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                 calTimestamp.get(Calendar.DAY_OF_YEAR) < calHoy.get(Calendar.DAY_OF_YEAR))
    }

    /**
     * Determina el estado operativo del turno con base en la hora del servidor.
     */
    fun obtenerEstadoOperativo(estadoCaja: EstadoCaja, ahoraMs: Long = HoraServidor.ahoraMs()): EstadoOperativoTurno {
        if (estadoCaja.estado != CajaSesion.ESTADO_ABIERTA || estadoCaja.aperturaMs <= 0L) {
            return EstadoOperativoTurno.CAJA_CERRADA
        }
        val calApertura = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = estadoCaja.aperturaMs }
        val calHoy = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahoraMs }

        val esAnterior = calApertura.get(Calendar.YEAR) < calHoy.get(Calendar.YEAR) ||
                (calApertura.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                 calApertura.get(Calendar.DAY_OF_YEAR) < calHoy.get(Calendar.DAY_OF_YEAR))

        return if (esAnterior) EstadoOperativoTurno.TURNO_VENCIDO else EstadoOperativoTurno.OPERATIVO
    }

    fun esTurnoVencido(estadoCaja: EstadoCaja, ahoraMs: Long = HoraServidor.ahoraMs()): Boolean =
        obtenerEstadoOperativo(estadoCaja, ahoraMs) == EstadoOperativoTurno.TURNO_VENCIDO

    fun esOperativo(estadoCaja: EstadoCaja, cajeroIdEsperado: String = "", ahoraMs: Long = HoraServidor.ahoraMs()): Boolean {
        if (cajeroIdEsperado.isNotBlank()) {
            val propietarioTurno = estadoCaja.cajeroId.ifBlank { estadoCaja.abiertoPorId }.trim()
            if (propietarioTurno.isNotBlank() && propietarioTurno != cajeroIdEsperado.trim()) {
                return false
            }
        }
        return obtenerEstadoOperativo(estadoCaja, ahoraMs) == EstadoOperativoTurno.OPERATIVO
    }

    /**
     * Puerta de seguridad central (UNA SOLA PUERTA - R1/R3/R14):
     * Evalúa: "¿ESTE cajero tiene un turno vigente y válido para operar?"
     *
     * 1. Aislamiento por cajero: cajero A + caja A + turno A NO puede operar sobre cajero B + caja B + turno B.
     * 2. Estado del turno:
     *    - OPERATIVO     → Continúa normalmente.
     *    - TURNO_VENCIDO → BLOQUEO OPERATIVO TOTAL (solo cierre de turno pendiente).
     *    - CAJA_CERRADA  → Requiere apertura de caja previa.
     */
    fun exigirTurnoOperativo(
        estadoCaja: EstadoCaja,
        accion: String,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ) {
        // 1. Verificación de aislamiento estricto por cajero (R1)
        if (cajeroIdEsperado.isNotBlank()) {
            val propietarioTurno = estadoCaja.cajeroId.ifBlank { estadoCaja.abiertoPorId }.trim()
            if (propietarioTurno.isNotBlank() && propietarioTurno != cajeroIdEsperado.trim()) {
                val titular = estadoCaja.abiertoPorNombre.ifBlank { "otro usuario" }
                throw IllegalStateException(
                    "AISLAMIENTO DE CAJA: Este turno de caja pertenece al cajero '$titular'. " +
                    "No puedes operar, cobrar ni modificar transacciones en la caja de otro cajero."
                )
            }
        }

        // 2. Puerta de estado operativo del turno
        when (obtenerEstadoOperativo(estadoCaja, ahoraMs)) {
            EstadoOperativoTurno.OPERATIVO -> return
            EstadoOperativoTurno.TURNO_VENCIDO -> {
                val fecha = estadoCaja.fechaAperturaLegible().ifBlank {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = TIMEZONE_LIMA }
                    fmt.format(Date(estadoCaja.aperturaMs))
                }
                throw IllegalStateException(
                    "BLOQUEO OPERATIVO: Tu turno de caja del $fecha está vencido. " +
                    "No se permite $accion. La única acción permitida es resolver el cierre del turno pendiente."
                )
            }
            EstadoOperativoTurno.CAJA_CERRADA -> {
                throw IllegalStateException("Tu caja está cerrada. Debes realizar la apertura formal antes de $accion.")
            }
        }
    }

    fun validarOperacionMutante(
        estadoCaja: EstadoCaja,
        accion: String,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        try {
            exigirTurnoOperativo(estadoCaja, accion, cajeroIdEsperado, ahoraMs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun validarPermiteVenta(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "cobrar o registrar ventas", cajeroIdEsperado, ahoraMs)

    /**
     * Puerta del mostrador: sin turno operativo NO se arma venta.
     * Caja cerrada o turno vencido bloquean buscar, agregar y modificar.
     * Así el cajero nunca llena un carrito que luego no puede cobrar.
     */
    fun validarPermiteAgregarAlCarrito(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "agregar productos al carrito", cajeroIdEsperado, ahoraMs)

    fun validarPermiteModificarCarrito(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "modificar el carrito de venta", cajeroIdEsperado, ahoraMs)

    fun validarPermiteDevolucion(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "procesar devoluciones o reembolsos", cajeroIdEsperado, ahoraMs)

    fun validarPermiteAnulacion(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "anular comprobantes de venta", cajeroIdEsperado, ahoraMs)

    fun validarPermiteSuspension(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "suspender ventas en curso", cajeroIdEsperado, ahoraMs)

    fun validarPermiteReanudacion(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "recuperar ventas suspendidas", cajeroIdEsperado, ahoraMs)

    fun validarPermiteMovimientoManual(
        estadoCaja: EstadoCaja,
        cajeroIdEsperado: String = "",
        ahoraMs: Long = HoraServidor.ahoraMs()
    ): Result<Unit> =
        validarOperacionMutante(estadoCaja, "registrar ingresos o retiros de caja", cajeroIdEsperado, ahoraMs)
}
