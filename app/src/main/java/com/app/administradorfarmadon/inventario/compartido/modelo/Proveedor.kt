package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Movimiento del saldo a favor con un proveedor (dinero que el proveedor nos debe,
 * nacido de anulaciones con pagos). Historial append-only: jamás se edita ni se borra.
 */
data class MovimientoSaldoProveedor(
    val id: String = "",
    val tipo: String = "", // "SALDO_A_FAVOR_ANULACION"
    val monto: Double = 0.0,
    val facturaId: String = "",
    val facturaNumero: String = "",
    val motivo: String = "",
    val documento: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val usuarioNombre: String = "",
    val usuarioEmail: String = ""
)

data class Proveedor(
    val id: String = "",
    val nombre: String = "",
    val idFiscal: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val montoMinimoPedido: Double = 0.0,
    // ── SALDO A FAVOR (el proveedor nos debe esta plata; nace de anulaciones con pagos) ──
    val saldoAFavor: Double = 0.0,
    val historialSaldoAFavor: List<MovimientoSaldoProveedor> = emptyList()
)
