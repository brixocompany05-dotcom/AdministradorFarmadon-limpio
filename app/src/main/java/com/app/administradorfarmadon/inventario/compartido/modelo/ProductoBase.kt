package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Identidad del producto —” catálogo y ficha sanitaria.
 * Responsabilidad única: qué ES el producto, sin precio ni stock.
 * Extraído de MoldeProductos (God-Model 65 campos) para legibilidad.
 */
data class ProductoBase(
    var indice: String = "",
    var nombre: String = "",
    var codigo: String = "",
    var slug: String = "",
    var categoriaPrincipal: String = "",
    var categoriaNombre: String = "",
    var categoriasLista: List<String> = emptyList(),
    var etiquetas: List<String> = emptyList(),
    var principioActivo: String = "",
    var laboratorio: String = "",
    var registroSanitario: String = "",
    var concentracion: String = "",
    var concentracionUnidad: String = "",
    var contenido: String = "",
    var contenidoUnidad: String = "",
    var categoriaId: String = "",
    var sugerenciasEnvase: List<String> = emptyList(),
    var sugerenciasPerfil: List<String> = emptyList(),
    var requiereReceta: Boolean = false,
    var activo: Boolean = true,
    var tieneCodigoBarra: Boolean = false,
    var actualizadoEn: Any? = null
)

/** Conversión desde el modelo monolítico (fachada) —” sin duplicar datos. */
fun MoldeProductos.toProductoBase(): ProductoBase = ProductoBase(
    indice = indice,
    nombre = nombre,
    codigo = codigo,
    slug = slug,
    categoriaPrincipal = categoriaPrincipal,
    categoriaNombre = categoriaNombre,
    categoriasLista = categoriasLista,
    etiquetas = etiquetas,
    principioActivo = principioActivo,
    laboratorio = laboratorio,
    registroSanitario = registroSanitario,
    concentracion = concentracion,
    concentracionUnidad = concentracionUnidad,
    contenido = contenido,
    contenidoUnidad = contenidoUnidad,
    categoriaId = categoriaId,
    sugerenciasEnvase = sugerenciasEnvase,
    sugerenciasPerfil = sugerenciasPerfil,
    requiereReceta = requiereReceta,
    activo = activo,
    tieneCodigoBarra = tieneCodigoBarra
)
