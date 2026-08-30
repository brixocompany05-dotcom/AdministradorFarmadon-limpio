package com.app.administradorfarmadon.inventario.compartido.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto

data class CostoLoteResultado(
    val costoUnitarioFinal: Double,
    val costoTotalFinal: Double
)

/**
 * Única fuente de verdad del costo real de un lote, usada IGUAL en pantalla y en el guardado.
 * Regla: costoUnitario si existe; si no, costoCompra / (disponible + cuarentena).
 * El monto siempre se redondea a 2 decimales con la misma función: lo que ves es lo que se guarda.
 */
object CostoRealLote {
    fun redondearMoneda(valor: Double): Double = kotlin.math.round(valor * 100.0) / 100.0

    fun costoUnitario(lote: Map<*, *>): Double {
        val cantidad = (lote["cantidad"] as? Number)?.toDouble() ?: 0.0
        val bloqueada = (lote["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
        val total = cantidad + bloqueada
        val costoUnitario = (lote["costoUnitario"] as? Number)?.toDouble() ?: 0.0
        val costoCompra = (lote["costoCompra"] as? Number)?.toDouble() ?: 0.0
        return when {
            costoUnitario > 0.0 -> costoUnitario
            costoCompra > 0.0 && total > 0.0 -> costoCompra / total
            else -> 0.0
        }
    }

    fun costoUnitario(lote: LoteProducto): Double {
        val total = lote.cantidad + lote.cantidadBloqueada
        return when {
            lote.costoCompraUnitario > 0.0 -> lote.costoCompraUnitario
            lote.costoUltimoIngreso > 0.0 && total > 0.0 -> lote.costoUltimoIngreso / total
            else -> 0.0
        }
    }

    fun monto(cantidad: Double, costoUnitario: Double): Double = redondearMoneda(cantidad * costoUnitario)
}

object CostoLoteCalculator {
    fun calcular(
        cantidadDisponibleAnterior: Double,
        cantidadBloqueadaAnterior: Double,
        costoUnitarioAnterior: Double,
        cantidadTotalNueva: Double,
        cantidadCompradaNueva: Double,
        costoTotalNuevo: Double,
        costoUnitarioNuevo: Double
    ): CostoLoteResultado {
        val totalCantidadAnterior = cantidadDisponibleAnterior + cantidadBloqueadaAnterior
        val costoTotalAnterior = totalCantidadAnterior * costoUnitarioAnterior
        val totalCantidadFinal = totalCantidadAnterior + cantidadTotalNueva
        val totalCostoFinal = costoTotalAnterior + costoTotalNuevo
        val costoUnitarioFinal = if (totalCantidadFinal > 0) totalCostoFinal / totalCantidadFinal else costoUnitarioNuevo
        return CostoLoteResultado(
            costoUnitarioFinal = costoUnitarioFinal,
            costoTotalFinal = totalCostoFinal
        )
    }
}
