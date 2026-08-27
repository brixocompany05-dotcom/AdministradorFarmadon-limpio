package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Lotes, vencimientos y logística — dónde está y cuándo vence.
 * Responsabilidad única: trazabilidad sanitaria y ubicación física.
 * Extraído de MoldeProductos para evitar God-Model.
 */
data class LoteInfo(
    var lotes: Map<String, LoteProducto> = emptyMap(),
    var lotePrioritarioId: String = "",
    var lotePrioritarioPor: String = "",
    var lotePrioritarioPorRol: String = "",
    var diasAlertaVencimiento: Int = 90,
    var ubicacionId: String = "",
    var ubicacion: String = "",
    var proveedorBaseId: String = "",
    var proveedorBaseNombre: String = "",
    var clasificacionControl: String = "",
    var temperaturaAlmacenamiento: String = "",
    var auditCreatedByEmail: String = "",
    var auditCreatedAt: String = "",
    var auditEditedAt: String = "",
    var creadoPorUid: String = "",
    var creadoEnMillis: Long = 0L,
    var etiquetaPendienteReimpresion: Boolean = false,
    var etiquetaPendienteDetalle: String = "",
    var etiquetaPendientePresentacionId: String = "",
    var etiquetaPendientePrecio: Double = 0.0,
    var etiquetasPendientesLista: List<EtiquetaPendienteItem> = emptyList(),
    var etiquetasImpresasPreviamente: Boolean = false
)

fun MoldeProductos.toLoteInfo(): LoteInfo = LoteInfo(
    lotes = lotes,
    lotePrioritarioId = lotePrioritarioId,
    lotePrioritarioPor = lotePrioritarioPor,
    lotePrioritarioPorRol = lotePrioritarioPorRol,
    diasAlertaVencimiento = diasAlertaVencimiento,
    ubicacionId = ubicacionId,
    ubicacion = ubicacion,
    proveedorBaseId = proveedorBaseId,
    proveedorBaseNombre = proveedorBaseNombre,
    clasificacionControl = clasificacionControl,
    temperaturaAlmacenamiento = temperaturaAlmacenamiento,
    auditCreatedByEmail = auditCreatedByEmail,
    auditCreatedAt = auditCreatedAt,
    auditEditedAt = auditEditedAt,
    creadoPorUid = creadoPorUid,
    creadoEnMillis = creadoEnMillis,
    etiquetaPendienteReimpresion = etiquetaPendienteReimpresion,
    etiquetaPendienteDetalle = etiquetaPendienteDetalle,
    etiquetaPendientePresentacionId = etiquetaPendientePresentacionId,
    etiquetaPendientePrecio = etiquetaPendientePrecio,
    etiquetasPendientesLista = etiquetasPendientesLista,
    etiquetasImpresasPreviamente = etiquetasImpresasPreviamente
)
