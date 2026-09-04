package com.app.administradorfarmadon.configuracion.pos.modelo

/**
 * Configuración operativa del Punto de Venta (POS) aislada por sucursal (R1/R3/R4).
 *
 * Filosofía Enterprise: Reglas Fijas = Permiso Directo (Cero burocracia, cero jefes en cola).
 * - Dentro de los topes configurados, el cajero cobra y opera directamente sin PIN.
 * - Fuera de los topes, la operación se bloquea limpiamente con guía visible.
 * - El control es posterior: trazabilidad completa en acta (cajero, turno, fecha, motivo).
 *
 * Ruta en Firestore:
 *   farmacias/{f}/sucursales/{s}/catalogos/posConfig
 */
data class PosConfig(
    val farmaciaId: String = "",
    val sucursalId: String = "",
    val descuento: PosDescuentoConfig = PosDescuentoConfig(),
    val caja: PosCajaConfig = PosCajaConfig(),
    val ticket: PosTicketConfig = PosTicketConfig(),
    val receta: PosRecetaConfig = PosRecetaConfig(),
    val actualizadoPorId: String = "",
    val actualizadoPorNombre: String = "",
    val actualizadoPorRol: String = "",
    val actualizadoEnMs: Long = 0L,
    val existeEnServidor: Boolean = false,
    val esBorrador: Boolean = true
) {
    /**
     * Una configuración es VIGENTE únicamente si el documento existe físicamente
     * en Firestore y fue guardado explícitamente por un administrador (R3/R12).
     * Sin documento guardado = PENDIENTE (no vende silenciosamente).
     */
    val estaVigente: Boolean
        get() = existeEnServidor && !esBorrador

    /** Evalúa si un descuento excede los topes duros permitidos para la sede. */
    fun excedeLimitesDescuento(pct: Double, monto: Double): Boolean {
        return pct > descuento.maxPct || monto > descuento.maxMonto
    }
}

/**
 * Topes duros de descuentos en la venta por mostrador.
 * @param maxPct Porcentaje máximo de descuento autorizado para el cajero (por defecto 10%)
 * @param maxMonto Monto máximo en soles de descuento por venta (por defecto S/ 50.00)
 */
data class PosDescuentoConfig(
    val maxPct: Double = 10.0,
    val maxMonto: Double = 50.0
)

/**
 * Políticas de control de efectivo y arqueo.
 * @param retiroMax Monto máximo permitido por retiro individual de caja (S/ 500.00)
 * @param vueltoMax Vuelto máximo permitido por operación en efectivo (S/ 200.00)
 * @param entregaCiegaTurno Si el cajero debe declarar efectivo sin ver el saldo teórico
 */
data class PosCajaConfig(
    val retiroMax: Double = 500.0,
    val vueltoMax: Double = 200.0,
    val entregaCiegaTurno: Boolean = true
)

/**
 * Opciones de impresión de comprobantes de venta.
 * @param copias Número de copias impresas (por defecto 1)
 * @param pie Mensaje de despedida al pie del ticket
 */
data class PosTicketConfig(
    val copias: Int = 1,
    val pie: String = "Gracias por su compra"
)

/**
 * Reglas de validación para medicamentos con receta médica obligatoria.
 * @param exigirConfirmacion Bloquea el cobro hasta que el cajero confirme receta física/digital
 */
data class PosRecetaConfig(
    val exigirConfirmacion: Boolean = true
)

/**
 * Modelo de compatibilidad para lecturas históricas (no bloquea el mostrador).
 */
data class AutorizacionSupervisor(
    val id: String = "",
    val nombre: String = "",
    val rol: String = "",
    val fechaMs: Long = 0L,
    val motivo: String = ""
)
