package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

/**
 * Lógica pura de filtrado — sin dependencias de Android/Compose.
 * Responsable de aplicar todos los filtros a una lista de productos.
 */
object InventarioFilterLogic {

    fun applyFilters(
        productos: List<PharmProduct>,
        filtros: InventarioFilterState
    ): List<PharmProduct> {
        return productos
            .filterByEstadoStock(filtros.estadoStock)
            .filterByClasificacion(filtros.clasificacion)
            .filterByCategorias(filtros.categorias)
            .filterByLaboratorio(filtros.laboratorio)
            .filterByVencimiento(filtros.vencimiento)
            .filterByPrecio(filtros.precioMax)
            .filterByUbicacion(filtros.ubicacion)
            .filterByOpciones(filtros)
    }

    private fun List<PharmProduct>.filterByEstadoStock(
        estados: Set<String>
    ): List<PharmProduct> {
        if (estados.isEmpty()) return this
        return filter { producto ->
            estados.any { estado ->
                when (estado) {
                    "Disponible" -> producto.status == "Disponible"
                    "Stock bajo" -> producto.status == "Stock bajo"
                    "Agotado" -> producto.stock <= 0
                    "En cuarentena" -> producto.status == "En cuarentena"
                    "Por vencer" -> producto.status == "Por vencer"
                    "Vencido" -> producto.status == "Vencido" || (producto.expiryTimestamp in 1L until com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs() && producto.expiryDate.isNotBlank())
                    else -> false
                }
            }
        }
    }

    private fun List<PharmProduct>.filterByClasificacion(
        clasificaciones: Set<String>
    ): List<PharmProduct> {
        if (clasificaciones.isEmpty()) return this
        return filter { p ->
            clasificaciones.any { c ->
                when (c) {
                    "venta_libre" -> p.clasificacionControl == "VENTA_LIBRE"
                    "receta" -> p.clasificacionControl in listOf("RX", "RX_RETENCION") || p.controlReceta
                    "controlado" -> p.clasificacionControl in listOf("RX_RETENCION", "ESTUPEFACIENTES")
                    "refrigerado" -> p.requiereRefrigeracion
                    else -> false
                }
            }
        }
    }

    private fun List<PharmProduct>.filterByCategorias(
        categorias: Set<String>
    ): List<PharmProduct> {
        if (categorias.isEmpty()) return this
        return filter { product -> 
            // Soporte para categorías individuales en el Set y categorías compuestas (comma-separated) en el producto
            val productCategories = (product.categories + product.category.split(",").map { it.trim() })
                .filter { it.isNotBlank() }
                .toSet()
            
            productCategories.any { it in categorias }
        }
    }

    private fun List<PharmProduct>.filterByLaboratorio(
        laboratorio: String
    ): List<PharmProduct> {
        if (laboratorio.isBlank()) return this
        return filter { it.laboratory == laboratorio }
    }

    private fun List<PharmProduct>.filterByVencimiento(
        vencimiento: String
    ): List<PharmProduct> {
        if (vencimiento.isBlank()) return this
        val hoy = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
        return filter { p ->
            when (vencimiento) {
                "Próx. 30 días" -> p.expiryTimestamp in hoy..(hoy + 30L * 86_400_000)
                "Próx. 90 días" -> p.expiryTimestamp in hoy..(hoy + 90L * 86_400_000)
                "Próx. 6 meses" -> p.expiryTimestamp in hoy..(hoy + 180L * 86_400_000)
                "Ya vencido" -> p.expiryTimestamp in 1L until hoy
                else -> false
            }
        }
    }

    private fun List<PharmProduct>.filterByPrecio(
        precioMax: Float
    ): List<PharmProduct> {
        if (precioMax >= Float.MAX_VALUE) return this
        return filter { it.purchasePrice <= precioMax }
    }

    private fun List<PharmProduct>.filterByUbicacion(
        ubicacion: String
    ): List<PharmProduct> {
        if (ubicacion.isBlank()) return this
        return filter { it.ubicacion == ubicacion }
    }

     private fun List<PharmProduct>.filterByOpciones(
        filtros: InventarioFilterState
    ): List<PharmProduct> {
        var lista = this
        if (filtros.soloConLotes) {
            lista = lista.filter { it.stock > 0 }
        }
        if (filtros.requiereReceta) {
            lista = lista.filter { it.controlReceta || it.clasificacionControl in listOf("RX", "RX_RETENCION") }
        }
        if (filtros.soloRefrigerados) {
            lista = lista.filter { it.requiereRefrigeracion }
        }
        if (filtros.stockBajoMinimo) {
            lista = lista.filter { it.stock <= it.minStock }
        }
        return lista
    }

    // ── Helpers para poblar los dropdowns dinámicamente ──
    fun getLaboratorios(productos: List<PharmProduct>): List<String> =
        productos.map { it.laboratory }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun getUbicaciones(productos: List<PharmProduct>): List<String> =
        productos.map { it.ubicacion }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun getCategorias(productos: List<PharmProduct>): List<String> =
        productos.flatMap { it.categories + listOf(it.category) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun getPrecioMax(productos: List<PharmProduct>): Float =
        productos.maxOfOrNull { it.purchasePrice.toFloat() } ?: 500f
}





