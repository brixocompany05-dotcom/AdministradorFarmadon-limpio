package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

class InventoryMetricsCalculator {

    fun calculateTotalValue(products: List<PharmProduct>): Double {
        return products.sumOf { it.totalValue }
    }

    fun calculateLowStockCount(products: List<PharmProduct>): Int {
        return products.count { it.status == "Stock bajo" || it.status == "Agotado" || (it.minStock > 0 && it.stock <= it.minStock) }
    }

    fun calculateNearExpiryCount(products: List<PharmProduct>): Int {
        val hoy = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
        return products.count { p ->
            p.status == "Por vencer" || p.status == "Vencido" ||
            (p.expiryTimestamp in 1L until hoy) || // vencidos por timestamp
            (p.expiryTimestamp in hoy..(hoy + 30L * 86_400_000)) // por vencer <30d
        }
    }

    fun calculateActiveProductsCount(products: List<PharmProduct>): Int {
        return products.count { it.activo }
    }
}
