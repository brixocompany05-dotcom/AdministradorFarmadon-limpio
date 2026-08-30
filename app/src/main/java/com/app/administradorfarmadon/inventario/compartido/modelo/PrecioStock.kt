package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Precios, presentaciones y stock —” cuánto cuesta y cuánto hay.
 * Responsabilidad única: valor comercial y disponibilidad física.
 * Extraído de MoldeProductos para evitar God-Model.
 */
data class PrecioStock(
    var empaque: String = "",
    var precioCompra: Double = 0.0,
    var stockMinimoBase: Double = 0.0,
    var unidadBase: String = "",
    var unidadVisualInventario: String = "",
    var presentacionPrincipalId: String = "",
    var inventarioPerfilUnidadSingular: String = "",
    var inventarioPerfilUnidadPlural: String = "",
    var inventarioPerfilContenidoPorUnidad: String = "",
    var inventarioPerfilUnidadContenido: String = "",
    var inventarioPerfilResumen: String = "",
    var basePorUnidad: Double = 0.0,
    var presentaciones: MutableList<PresentacionProducto> = mutableListOf(),
    var codigosSecundarios: List<String> = emptyList(),
    var permiteFraccionar: Boolean = false
)

fun MoldeProductos.toPrecioStock(): PrecioStock = PrecioStock(
    empaque = empaque,
    precioCompra = precioCompra,
    stockMinimoBase = stockMinimoBase,
    unidadBase = unidadBase,
    unidadVisualInventario = unidadVisualInventario,
    presentacionPrincipalId = presentacionPrincipalId,
    inventarioPerfilUnidadSingular = inventarioPerfilUnidadSingular,
    inventarioPerfilUnidadPlural = inventarioPerfilUnidadPlural,
    inventarioPerfilContenidoPorUnidad = inventarioPerfilContenidoPorUnidad,
    inventarioPerfilUnidadContenido = inventarioPerfilUnidadContenido,
    inventarioPerfilResumen = inventarioPerfilResumen,
    basePorUnidad = basePorUnidad,
    presentaciones = presentaciones,
    codigosSecundarios = codigosSecundarios,
    permiteFraccionar = permiteFraccionar
)
