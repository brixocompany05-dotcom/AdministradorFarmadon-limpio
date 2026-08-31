package com.app.administradorfarmadon.inventario.compartido.modelo

import com.google.firebase.database.DataSnapshot

/**
 * MoldeProductos —” Fachada UNIFICADA con fuente única.
 * Fuente real: productoBase + precioStock + loteInfo (3 objetos).
 * Los 65 vars antiguos delegan a esos 3 —” no hay duplicación, no hay dato a medias.
 * Nuevas pantallas: usen ProductoBase / PrecioStock / LoteInfo directo.
 * Pantallas viejas: siguen usando molde.nombre etc. (delegado).
 */
data class MoldeProductos(
    var productoBase: ProductoBase = ProductoBase(),
    var precioStock: PrecioStock = PrecioStock(),
    var loteInfo: LoteInfo = LoteInfo()
) {
    var indice: String
        get() = productoBase.indice
        set(value) { productoBase.indice = value }

    var nombre: String
        get() = productoBase.nombre
        set(value) { productoBase.nombre = value }

    var codigo: String
        get() = productoBase.codigo
        set(value) { productoBase.codigo = value }

    var categoriaPrincipal: String
        get() = productoBase.categoriaPrincipal
        set(value) { productoBase.categoriaPrincipal = value }

    var etiquetas: List<String>
        get() = productoBase.etiquetas
        set(value) { productoBase.etiquetas = value }

    var categoriaNombre: String
        get() = productoBase.categoriaNombre
        set(value) { productoBase.categoriaNombre = value }

    var categoriasLista: List<String>
        get() = productoBase.categoriasLista
        set(value) { productoBase.categoriasLista = value }

    var empaque: String
        get() = precioStock.empaque
        set(value) { precioStock.empaque = value }

    var precioCompra: Double
        get() = precioStock.precioCompra
        set(value) { precioStock.precioCompra = value }

    var stockMinimoBase: Double
        get() = precioStock.stockMinimoBase
        set(value) { precioStock.stockMinimoBase = value }

    var unidadBase: String
        get() = precioStock.unidadBase
        set(value) { precioStock.unidadBase = value }

    var unidadVisualInventario: String
        get() = precioStock.unidadVisualInventario
        set(value) { precioStock.unidadVisualInventario = value }

    var presentacionPrincipalId: String
        get() = precioStock.presentacionPrincipalId
        set(value) { precioStock.presentacionPrincipalId = value }

    var requiereReceta: Boolean
        get() = productoBase.requiereReceta
        set(value) { productoBase.requiereReceta = value }

    var activo: Boolean
        get() = productoBase.activo
        set(value) { productoBase.activo = value }

    var tieneCodigoBarra: Boolean
        get() = productoBase.tieneCodigoBarra
        set(value) { productoBase.tieneCodigoBarra = value }

    var slug: String
        get() = productoBase.slug
        set(value) { productoBase.slug = value }

    var concentracion: String
        get() = productoBase.concentracion
        set(value) { productoBase.concentracion = value }

    var concentracionUnidad: String
        get() = productoBase.concentracionUnidad
        set(value) { productoBase.concentracionUnidad = value }

    var contenido: String
        get() = productoBase.contenido
        set(value) { productoBase.contenido = value }

    var contenidoUnidad: String
        get() = productoBase.contenidoUnidad
        set(value) { productoBase.contenidoUnidad = value }

    var ubicacionId: String
        get() = loteInfo.ubicacionId
        set(value) { loteInfo.ubicacionId = value }

    var ubicacion: String
        get() = loteInfo.ubicacion
        set(value) { loteInfo.ubicacion = value }

    var ubicacionSecundaria: String
        get() = loteInfo.ubicacionSecundaria
        set(value) { loteInfo.ubicacionSecundaria = value }

    var proveedorBaseId: String
        get() = loteInfo.proveedorBaseId
        set(value) { loteInfo.proveedorBaseId = value }

    var proveedorBaseNombre: String
        get() = loteInfo.proveedorBaseNombre
        set(value) { loteInfo.proveedorBaseNombre = value }

    var clasificacionControl: String
        get() = loteInfo.clasificacionControl
        set(value) { loteInfo.clasificacionControl = value }

    var temperaturaAlmacenamiento: String
        get() = loteInfo.temperaturaAlmacenamiento
        set(value) { loteInfo.temperaturaAlmacenamiento = value }

    var inventarioPerfilUnidadSingular: String
        get() = precioStock.inventarioPerfilUnidadSingular
        set(value) { precioStock.inventarioPerfilUnidadSingular = value }

    var inventarioPerfilUnidadPlural: String
        get() = precioStock.inventarioPerfilUnidadPlural
        set(value) { precioStock.inventarioPerfilUnidadPlural = value }

    var inventarioPerfilContenidoPorUnidad: String
        get() = precioStock.inventarioPerfilContenidoPorUnidad
        set(value) { precioStock.inventarioPerfilContenidoPorUnidad = value }

    var inventarioPerfilUnidadContenido: String
        get() = precioStock.inventarioPerfilUnidadContenido
        set(value) { precioStock.inventarioPerfilUnidadContenido = value }

    var inventarioPerfilResumen: String
        get() = precioStock.inventarioPerfilResumen
        set(value) { precioStock.inventarioPerfilResumen = value }

    var basePorUnidad: Double
        get() = precioStock.basePorUnidad
        set(value) { precioStock.basePorUnidad = value }

    var auditCreatedByEmail: String
        get() = loteInfo.auditCreatedByEmail
        set(value) { loteInfo.auditCreatedByEmail = value }

    var auditCreatedAt: String
        get() = loteInfo.auditCreatedAt
        set(value) { loteInfo.auditCreatedAt = value }

    var auditEditedAt: String
        get() = loteInfo.auditEditedAt
        set(value) { loteInfo.auditEditedAt = value }

    var creadoPorUid: String
        get() = loteInfo.creadoPorUid
        set(value) { loteInfo.creadoPorUid = value }

    var creadoEnMillis: Long
        get() = loteInfo.creadoEnMillis
        set(value) { loteInfo.creadoEnMillis = value }

    var categoriaId: String
        get() = productoBase.categoriaId
        set(value) { productoBase.categoriaId = value }

    var sugerenciasEnvase: List<String>
        get() = productoBase.sugerenciasEnvase
        set(value) { productoBase.sugerenciasEnvase = value }

    var sugerenciasPerfil: List<String>
        get() = productoBase.sugerenciasPerfil
        set(value) { productoBase.sugerenciasPerfil = value }

    var principioActivo: String
        get() = productoBase.principioActivo
        set(value) { productoBase.principioActivo = value }

    var laboratorio: String
        get() = productoBase.laboratorio
        set(value) { productoBase.laboratorio = value }

    var registroSanitario: String
        get() = productoBase.registroSanitario
        set(value) { productoBase.registroSanitario = value }

    var presentaciones: MutableList<PresentacionProducto>
        get() = precioStock.presentaciones
        set(value) { precioStock.presentaciones = value }

    /** Verdad de precio: hay al menos una presentación con precio de venta mayor a 0. */
    val tienePrecioVenta: Boolean
        get() = presentaciones.any { it.precioventa > 0.0 }

    var lotes: Map<String, LoteProducto>
        get() = loteInfo.lotes
        set(value) { loteInfo.lotes = value }

    var lotePrioritarioId: String
        get() = loteInfo.lotePrioritarioId
        set(value) { loteInfo.lotePrioritarioId = value }

    var lotePrioritarioPor: String
        get() = loteInfo.lotePrioritarioPor
        set(value) { loteInfo.lotePrioritarioPor = value }

    var lotePrioritarioPorRol: String
        get() = loteInfo.lotePrioritarioPorRol
        set(value) { loteInfo.lotePrioritarioPorRol = value }

    var fefoAutomatico: Boolean
        get() = loteInfo.fefoAutomatico
        set(value) { loteInfo.fefoAutomatico = value }

    var diasAlertaVencimiento: Int
        get() = loteInfo.diasAlertaVencimiento
        set(value) { loteInfo.diasAlertaVencimiento = value }

    var codigosSecundarios: List<String>
        get() = precioStock.codigosSecundarios
        set(value) { precioStock.codigosSecundarios = value }

    var etiquetaPendienteReimpresion: Boolean
        get() = loteInfo.etiquetaPendienteReimpresion
        set(value) { loteInfo.etiquetaPendienteReimpresion = value }

    var etiquetaPendienteDetalle: String
        get() = loteInfo.etiquetaPendienteDetalle
        set(value) { loteInfo.etiquetaPendienteDetalle = value }

    var etiquetaPendientePresentacionId: String
        get() = loteInfo.etiquetaPendientePresentacionId
        set(value) { loteInfo.etiquetaPendientePresentacionId = value }

    var etiquetaPendientePrecio: Double
        get() = loteInfo.etiquetaPendientePrecio
        set(value) { loteInfo.etiquetaPendientePrecio = value }

    var etiquetasPendientesLista: List<EtiquetaPendienteItem>
        get() = loteInfo.etiquetasPendientesLista
        set(value) { loteInfo.etiquetasPendientesLista = value }

    var etiquetasImpresasPreviamente: Boolean
        get() = loteInfo.etiquetasImpresasPreviamente
        set(value) { loteInfo.etiquetasImpresasPreviamente = value }

    var permiteFraccionar: Boolean
        get() = precioStock.permiteFraccionar
        set(value) { precioStock.permiteFraccionar = value }

    // Compatibilidad: constructor con los 65 campos antiguos (88 pantallas existentes)
    constructor(
        indice: String = "",
        nombre: String = "",
        codigo: String = "",
        categoriaPrincipal: String = "",
        etiquetas: List<String> = emptyList(),
        categoriaNombre: String = "",
        categoriasLista: List<String> = emptyList(),
        empaque: String = "",
        precioCompra: Double = 0.0,
        stockMinimoBase: Double = 0.0,
        unidadBase: String = "",
        unidadVisualInventario: String = "",
        presentacionPrincipalId: String = "",
        requiereReceta: Boolean = false,
        activo: Boolean = true,
        tieneCodigoBarra: Boolean = false,
        slug: String = "",
        concentracion: String = "",
        concentracionUnidad: String = "",
        contenido: String = "",
        contenidoUnidad: String = "",
        ubicacionId: String = "",
        ubicacion: String = "",
        ubicacionSecundaria: String = "",
        proveedorBaseId: String = "",
        proveedorBaseNombre: String = "",
        laboratorio: String = "",
        clasificacionControl: String = "",
        temperaturaAlmacenamiento: String = "",
        inventarioPerfilUnidadSingular: String = "",
        inventarioPerfilUnidadPlural: String = "",
        inventarioPerfilContenidoPorUnidad: String = "",
        inventarioPerfilUnidadContenido: String = "",
        inventarioPerfilResumen: String = "",
        basePorUnidad: Double = 0.0,
        auditCreatedByEmail: String = "",
        auditCreatedAt: String = "",
        auditEditedAt: String = "",
        creadoPorUid: String = "",
        creadoEnMillis: Long = 0L,
        categoriaId: String = "",
        sugerenciasEnvase: List<String> = emptyList(),
        sugerenciasPerfil: List<String> = emptyList(),
        principioActivo: String = "",
        registroSanitario: String = "",
        presentaciones: MutableList<PresentacionProducto> = mutableListOf(),
        lotes: Map<String, LoteProducto> = emptyMap(),
        diasAlertaVencimiento: Int = 90,
        codigosSecundarios: List<String> = emptyList(),
        etiquetaPendienteReimpresion: Boolean = false,
        etiquetaPendienteDetalle: String = "",
        etiquetaPendientePresentacionId: String = "",
        etiquetaPendientePrecio: Double = 0.0,
        etiquetasPendientesLista: List<EtiquetaPendienteItem> = emptyList(),
        etiquetasImpresasPreviamente: Boolean = false,
        permiteFraccionar: Boolean = false
    ) : this() {
        this.indice = indice
        this.nombre = nombre
        this.codigo = codigo
        this.categoriaPrincipal = categoriaPrincipal
        this.etiquetas = etiquetas
        this.categoriaNombre = categoriaNombre
        this.categoriasLista = categoriasLista
        this.empaque = empaque
        this.precioCompra = precioCompra
        this.stockMinimoBase = stockMinimoBase
        this.unidadBase = unidadBase
        this.unidadVisualInventario = unidadVisualInventario
        this.presentacionPrincipalId = presentacionPrincipalId
        this.requiereReceta = requiereReceta
        this.activo = activo
        this.tieneCodigoBarra = tieneCodigoBarra
        this.slug = slug
        this.concentracion = concentracion
        this.concentracionUnidad = concentracionUnidad
        this.contenido = contenido
        this.contenidoUnidad = contenidoUnidad
        this.ubicacionId = ubicacionId
        this.ubicacion = ubicacion
        this.ubicacionSecundaria = ubicacionSecundaria
        this.proveedorBaseId = proveedorBaseId
        this.proveedorBaseNombre = proveedorBaseNombre
        this.laboratorio = laboratorio
        this.clasificacionControl = clasificacionControl
        this.temperaturaAlmacenamiento = temperaturaAlmacenamiento
        this.inventarioPerfilUnidadSingular = inventarioPerfilUnidadSingular
        this.inventarioPerfilUnidadPlural = inventarioPerfilUnidadPlural
        this.inventarioPerfilContenidoPorUnidad = inventarioPerfilContenidoPorUnidad
        this.inventarioPerfilUnidadContenido = inventarioPerfilUnidadContenido
        this.inventarioPerfilResumen = inventarioPerfilResumen
        this.basePorUnidad = basePorUnidad
        this.auditCreatedByEmail = auditCreatedByEmail
        this.auditCreatedAt = auditCreatedAt
        this.auditEditedAt = auditEditedAt
        this.creadoPorUid = creadoPorUid
        this.creadoEnMillis = creadoEnMillis
        this.categoriaId = categoriaId
        this.sugerenciasEnvase = sugerenciasEnvase
        this.sugerenciasPerfil = sugerenciasPerfil
        this.principioActivo = principioActivo
        this.registroSanitario = registroSanitario
        this.presentaciones = presentaciones
        this.lotes = lotes
        this.diasAlertaVencimiento = diasAlertaVencimiento
        this.codigosSecundarios = codigosSecundarios
        this.etiquetaPendienteReimpresion = etiquetaPendienteReimpresion
        this.etiquetaPendienteDetalle = etiquetaPendienteDetalle
        this.etiquetaPendientePresentacionId = etiquetaPendientePresentacionId
        this.etiquetaPendientePrecio = etiquetaPendientePrecio
        this.etiquetasPendientesLista = etiquetasPendientesLista
        this.etiquetasImpresasPreviamente = etiquetasImpresasPreviamente
        this.permiteFraccionar = permiteFraccionar
    }

}

data class EtiquetaPendienteItem(
    var presentacionId: String = "",
    var nombre: String = "",
    var precio: Double = 0.0,
    var cantidad: Int = 1
)

data class PresentacionProducto(
    var presentacionId: String = "",
    var nombre: String = "",
    var empaque: String = "",
    var cantidad: Int = 0,
    var unidadMedida: String = "",
    var precioventa: Double = 0.0,
    var codigoBarras: String = ""
)

/**
 * Resultado de resolución de código para mostrador de caja o inventario.
 * Si una presentación fue descontinuada/eliminada pero la etiqueta física sigue en el estante,
 * devuelve el cálculo proporcional seguro sin romper la operación.
 */
data class ResolvedProductPresentation(
    val presentacionId: String,
    val nombrePresentacion: String,
    val cantidadUnidades: Int,
    val precioVenta: Double,
    val esDescontinuada: Boolean = false
)

/**
 * Resuelve de forma infalible la presentación, cantidad y precio que corresponden a un código escaneado.
 */
fun MoldeProductos.resolverPresentacionPorCodigo(codigoEscaneado: String): ResolvedProductPresentation {
    val codLimpio = codigoEscaneado.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
    val precioBaseUnitario = (presentaciones.firstOrNull { it.cantidad == 1 } ?: presentaciones.firstOrNull())?.let {
        if (it.cantidad > 0) it.precioventa / it.cantidad else it.precioventa
    } ?: 0.0

    // 1. Coincidencia directa en códigos personalizados de presentaciones activas
    val coincidenciaDirecta = presentaciones.firstOrNull {
        it.codigoBarras.isNotBlank() && it.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio
    }
    if (coincidenciaDirecta != null) {
        return ResolvedProductPresentation(
            presentacionId = coincidenciaDirecta.presentacionId,
            nombrePresentacion = coincidenciaDirecta.nombre,
            cantidadUnidades = coincidenciaDirecta.cantidad.coerceAtLeast(1),
            precioVenta = coincidenciaDirecta.precioventa
        )
    }

    // 2. Extraer sufijo de fracción (-B{cant} o -U{cant})
    val matchFraccion = Regex("-(B|U)(\\d+)$", RegexOption.IGNORE_CASE).find(codLimpio)
    if (matchFraccion != null) {
        val cantidad = matchFraccion.groupValues[2].toIntOrNull() ?: 1

        // 2.1 Buscar si existe una presentación activa con esa misma cantidad
        val presPorCantidad = presentaciones.firstOrNull { it.cantidad == cantidad }
        if (presPorCantidad != null) {
            return ResolvedProductPresentation(
                presentacionId = presPorCantidad.presentacionId,
                nombrePresentacion = presPorCantidad.nombre,
                cantidadUnidades = cantidad,
                precioVenta = presPorCantidad.precioventa
            )
        }

        // 2.2 Fallback Infalible: La presentación fue eliminada, pero la etiqueta física tiene la cantidad
        val precioCalculado = Math.round(precioBaseUnitario * cantidad * 100.0) / 100.0
        val nombreGenerico = if (cantidad == 1) "Unidad suelta" else "Fracción x $cantidad unidades"
        return ResolvedProductPresentation(
            presentacionId = "descontinuada_$cantidad",
            nombrePresentacion = nombreGenerico,
            cantidadUnidades = cantidad,
            precioVenta = precioCalculado,
            esDescontinuada = true
        )
    }

    // 3. Código base del producto (Caja o presentación principal)
    val presPrincipal = presentaciones.maxByOrNull { it.cantidad } ?: presentaciones.firstOrNull()
    return ResolvedProductPresentation(
        presentacionId = presPrincipal?.presentacionId ?: "base",
        nombrePresentacion = presPrincipal?.nombre ?: "Unidad",
        cantidadUnidades = presPrincipal?.cantidad?.coerceAtLeast(1) ?: 1,
        precioVenta = presPrincipal?.precioventa ?: precioBaseUnitario
    )
}

// ── REGLA íšNICA DE UNIDADES ────────────────────────────────────────────────
// lote.cantidad          = unidades FÍSICAS del producto (cajas, frascos, etc.)
// PresentacionProducto.cantidad = unidades de CONTENIDO en esa presentación (tabletas, mL, etc.)
// factorContenido        = cantidad de la presentación mayor = contenido declarado al crear el producto
//
// EJEMPLO: Panadol 180 Tab, stock 3 Cajas
//   Vender "1 Caja"    (cantidad=180) → -180í·180 = -1.0 caja  → quedan 2 Cajas (360 Tab)
//   Vender "1 Tableta" (cantidad=1)   → -1í·180   = -0.00556 c → quedan 2.994 Cajas (539 Tab)
//
// EL MÓDULO DE VENTAS DEBE USAR UnidadVentaHelper.stockFisicoParaVender()
// para calcular cuánto descontar del lote. Nunca descontar presentacion.cantidad directo.
// ──────────────────────────────────────────────────────────────────────────

/**
 * Stock disponible en UNIDADES FÍSICAS (cajas, frascos · ).
 * Es la suma de lote.cantidad de todos los lotes sin bloquear.
 */
val MoldeProductos.stockDisponibleFisico: Double
    get() = lotes.values.sumOf { it.cantidad.coerceAtLeast(0.0) }

/** Alias de compatibilidad —” apunta a stockDisponibleFisico. */
val MoldeProductos.stockDisponibleUnidades: Double
    get() = stockDisponibleFisico

val MoldeProductos.stockFisicoTotalUnidades: Double
    get() = lotes.values.sumOf { (it.cantidad + it.cantidadBloqueada).coerceAtLeast(0.0) }

val MoldeProductos.stockTotalUnidades: Double
    get() = stockDisponibleFisico

val MoldeProductos.precioVenta: Double
    get() = presentaciones.firstOrNull { it.presentacionId == presentacionPrincipalId }?.precioventa
        ?: presentaciones.firstOrNull()?.precioventa
        ?: 0.0

/**
 * Stock disponible expresado en UNIDADES DE CONTENIDO (tabletas, mL · ) —” SOLO PARA MOSTRAR.
 * Nunca usar este valor para calcular descuentos: el cálculo correcto es UnidadVentaHelper.
 */
val MoldeProductos.stockDisponibleEnContenido: Double
    get() {
        val factor = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
            .factorContenido(contenido, presentaciones)
        return stockDisponibleFisico * factor
    }

/**
 * Valida si hay stock suficiente para vender [cantidadContenido] unidades de contenido
 * (tabletas, mL, etc. —” lo que dice PresentacionProducto.cantidad).
 *
 * Convierte correctamente de contenido → físico usando UnidadVentaHelper
 * antes de comparar con el stock (que está en unidades físicas).
 *
 * El módulo de ventas llama esto con la presentación a vender para saber si puede vender.
 * Normaliza la unidad de la presentación a la del producto (R3): si el producto habla en
 * "L" y la presentación en "ml", convierte antes de comparar, para no decir "stock insuficiente"
 * falsamente ni dejar pasar una venta que no cabe.
 */
fun MoldeProductos.validarDisponibilidadVenta(presentacion: PresentacionProducto): Pair<Boolean, String?> {
    val factor = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
        .factorContenido(contenido, presentaciones)
    val unidadProducto = contenidoUnidad.ifBlank { empaque }
    // Misma normalización que calcularDescuentoFEFO: la presentación vendida a unidad del producto.
    val cantidadEnUnidadProducto = com.app.administradorfarmadon.inventario.compartido.logica
        .PerfilUnidades.normalizarA(presentacion.cantidad.toDouble(), presentacion.unidadMedida, unidadProducto)
    val fisicoRequerido = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
        .stockFisicoParaVender(cantidadEnUnidadProducto, factor)
    val stockFisico = stockDisponibleFisico

    return if (stockFisico >= fisicoRequerido) {
        true to null
    } else {
        val disponibleContenido = (stockFisico * factor).toLong()
        val nomContenido = contenidoUnidad.ifBlank { "unidades" }
        val mensaje = if (stockFisico <= 0.0) {
            "Producto agotado. No hay stock disponible."
        } else {
            "Stock insuficiente: quedan $disponibleContenido $nomContenido disponibles."
        }
        false to mensaje
    }
}
