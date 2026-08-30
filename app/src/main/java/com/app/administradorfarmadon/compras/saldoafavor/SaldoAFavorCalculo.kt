package com.app.administradorfarmadon.compras.saldoafavor

/**
 * Reglas puras del saldo a favor (plata que el proveedor nos debe).
 * Sin Firebase ni UI: pantalla y guardado usan la MISMA función para no descuadrarse.
 */
data class SaldoAFavorLiquidacion(
    val disponible: Double,
    val aplicado: Double,
    val totalFactura: Double,
    val netoAPagar: Double
)

object SaldoAFavorCalculo {

    /**
     * @param disponible  saldo a favor real del proveedor (plata que nos debe)
     * @param usar        si el usuario decidió aplicarlo como descuento
     * @param totalFactura total que dice el papel de esta factura
     */
    fun liquidacion(disponible: Double, usar: Boolean, totalFactura: Double): SaldoAFavorLiquidacion {
        val saldo = disponible.coerceAtLeast(0.0)
        val total = totalFactura.coerceAtLeast(0.0)
        val aplicado = if (usar) saldo.coerceAtMost(total) else 0.0
        val neto = (total - aplicado).coerceAtLeast(0.0)
        return SaldoAFavorLiquidacion(
            disponible = saldo,
            aplicado = aplicado,
            totalFactura = total,
            netoAPagar = neto
        )
    }
}
