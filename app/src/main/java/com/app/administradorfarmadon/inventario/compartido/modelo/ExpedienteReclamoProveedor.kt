package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Expediente Oficial de Reclamo y Devolución a Droguería / Proveedor.
 * Registra la evidencia legal de mercadería retirada por el chofer y su resolución
 * comercial (Nota de Crédito, Saldo a Favor, Canje Físico o Rechazo en Disputa).
 */
data class ExpedienteReclamoProveedor(
    val id: String = "",
    val clienteId: String = "",
    val productoId: String = "",
    val productoNombre: String = "",
    val empaque: String = "Caja",
    val loteNumero: String = "",
    val cantidadDevuelta: Double = 0.0,
    val costoUnitario: Double = 0.0,
    val montoTotal: Double = 0.0,
    val proveedorId: String = "",
    val proveedorNombre: String = "",
    val facturaOrigen: String = "",
    val guiaRetiro: String = "",           // N° de Guía de Devolución / Acta firmada por el chofer
    val notaCredito: String = "",          // N° de Nota de Crédito oficial (cuando la droguería la emita)
    val motivo: String = "",               // Motivo: "Frascos rotos", "Vencimiento corto", etc.
    val modalidadCompensacion: String = "NOTA_CREDITO_DINERO", // "NOTA_CREDITO_DINERO" | "CANJE_FISICO"
    val estado: String = "EN_REVISION_DROGUERIA", // "EN_REVISION_DROGUERIA" | "APROBADO_NOTA_CREDITO" | "CANJE_COMPLETADO" | "RECHAZADO_DISPUTA"
    val usuarioRegistroEmail: String = "",
    val fechaRegistroStr: String = "",
    val fechaResolucionStr: String = "",
    val observacionesResolucion: String = ""
)
