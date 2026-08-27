package com.app.administradorfarmadon.inventario.compartido.logica

data class CostoLoteResultado(
    val costoUnitarioFinal: Double,
    val costoTotalFinal: Double
)

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
