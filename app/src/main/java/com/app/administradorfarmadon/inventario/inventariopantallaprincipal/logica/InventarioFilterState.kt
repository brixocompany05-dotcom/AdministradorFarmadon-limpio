package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

data class InventarioFilterState(
    val estadoStock: Set<String> = emptySet(),
    val clasificacion: Set<String> = emptySet(),
    val categorias: Set<String> = emptySet(),
    val laboratorio: String = "",
    val vencimiento: String = "",
    val precioMax: Float = Float.MAX_VALUE,
    val ubicacion: String = "",
    val soloConLotes: Boolean = false,
    val requiereReceta: Boolean = false,
    val soloRefrigerados: Boolean = false,
    val stockBajoMinimo: Boolean = false
) {
    val hayFiltrosActivos: Boolean get() =
        estadoStock.isNotEmpty() || clasificacion.isNotEmpty() ||
        categorias.isNotEmpty() || laboratorio.isNotEmpty() ||
        vencimiento.isNotEmpty() || precioMax < Float.MAX_VALUE ||
        ubicacion.isNotEmpty() || soloConLotes || requiereReceta ||
        soloRefrigerados || stockBajoMinimo
}