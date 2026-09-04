package com.app.administradorfarmadon.ventas.compartido.modelo

/**
 * Puerta única Checklist de Apertura de Sede para el Punto de Venta (R1/R3/R8/R12).
 *
 * El POS NO permite cobrar ni emitir comprobantes a ciegas si alguno de los 8
 * pilares obligatorios de la sede o farmacia no está formalmente verificado y guardado.
 */
data class ChecklistAperturaSede(
    val farmaciaActiva: Boolean = false,
    val farmaciaNombre: String = "",
    val suscripcionValida: Boolean = false,
    val suscripcionDetalle: String = "",
    val sedeActiva: Boolean = false,
    val sedeNombre: String = "",
    val emisorFiscalCompleto: Boolean = false,
    val emisorRuc: String = "",
    val seriesFiscalesCompletas: Boolean = false,
    val seriesDetalle: String = "",
    val metodosPagoConfigurados: Boolean = false,
    val cantidadMetodosActivos: Int = 0,
    val posConfigGuardado: Boolean = false,
    val posConfigDetalle: String = "",
    val cajaAbierta: Boolean = false,
    val cajaDetalle: String = "",
    val cajaPendienteDeCierreAnterior: Boolean = false,
    val fechaCajaPendiente: String = ""
) {
    val totalRequisitos: Int = 8

    val cajaOperativaHoy: Boolean
        get() = cajaAbierta && !cajaPendienteDeCierreAnterior

    val totalCompletados: Int
        get() = listOf(
            farmaciaActiva,
            suscripcionValida,
            sedeActiva,
            emisorFiscalCompleto,
            seriesFiscalesCompletas,
            metodosPagoConfigurados,
            posConfigGuardado,
            cajaOperativaHoy
        ).count { it }

    val todoListo: Boolean
        get() = farmaciaActiva &&
                suscripcionValida &&
                sedeActiva &&
                emisorFiscalCompleto &&
                seriesFiscalesCompletas &&
                metodosPagoConfigurados &&
                posConfigGuardado &&
                cajaOperativaHoy

    fun obtenerItems(): List<ItemChecklistApertura> = listOf(
        ItemChecklistApertura(
            clave = "FARMACIA",
            titulo = "Farmacia Registrada y Activa",
            completado = farmaciaActiva,
            detalle = if (farmaciaActiva) farmaciaNombre.ifBlank { "Cuenta activa" } else "Farmacia no encontrada o inactiva en Brixo",
            rutaNavegacion = null,
            textoAccion = null
        ),
        ItemChecklistApertura(
            clave = "SUSCRIPCION",
            titulo = "Suscripción BRIXO Vigente",
            completado = suscripcionValida,
            detalle = if (suscripcionValida) suscripcionDetalle.ifBlank { "Plan al día" } else "Suscripción vencida o no contratada",
            rutaNavegacion = null,
            textoAccion = null
        ),
        ItemChecklistApertura(
            clave = "SEDE",
            titulo = "Sede Habilitada",
            completado = sedeActiva,
            detalle = if (sedeActiva) sedeNombre.ifBlank { "Sede operativa" } else "Esta sucursal está marcada como inactiva",
            rutaNavegacion = "config_sucursales",
            textoAccion = "Ver Sedes"
        ),
        ItemChecklistApertura(
            clave = "EMISOR",
            titulo = "Emisor Fiscal SUNAT",
            completado = emisorFiscalCompleto,
            detalle = if (emisorFiscalCompleto) "RUC $emisorRuc verificado con APISUNAT" else "Falta completar o verificar credenciales APISUNAT",
            rutaNavegacion = "facturacion_emisor",
            textoAccion = "Configurar Emisor"
        ),
        ItemChecklistApertura(
            clave = "SERIES",
            titulo = "Series Fiscales (B / F / BC / FC)",
            completado = seriesFiscalesCompletas,
            detalle = if (seriesFiscalesCompletas) seriesDetalle.ifBlank { "4/4 series activas" } else "Faltan asignar series fiscales a esta sede",
            rutaNavegacion = "config_sucursales",
            textoAccion = "Verificar Series"
        ),
        ItemChecklistApertura(
            clave = "METODOS",
            titulo = "Métodos de Pago Activos",
            completado = metodosPagoConfigurados,
            detalle = if (metodosPagoConfigurados) "$cantidadMetodosActivos método(s) activo(s)" else "Sin métodos de pago activos para cobrar",
            rutaNavegacion = "config_metodos_pago",
            textoAccion = "Activar Métodos"
        ),
        ItemChecklistApertura(
            clave = "POS_CONFIG",
            titulo = "Reglas de Venta y Caja (POS)",
            completado = posConfigGuardado,
            detalle = if (posConfigGuardado) posConfigDetalle.ifBlank { "Reglas vigentes guardadas en BD" } else "Borrador sin guardar — Guarda para operar",
            rutaNavegacion = "config_pos",
            textoAccion = "Guardar Reglas"
        ),
        ItemChecklistApertura(
            clave = "CAJA",
            titulo = if (cajaPendienteDeCierreAnterior) "Caja pendiente de cierre ($fechaCajaPendiente)" else "Apertura de Caja",
            completado = cajaOperativaHoy,
            detalle = when {
                cajaPendienteDeCierreAnterior -> "Existe una caja abierta del $fechaCajaPendiente. Debes cerrarla en Cierre de Caja para operar hoy."
                cajaAbierta -> cajaDetalle.ifBlank { "Turno abierto" }
                else -> "Caja física cerrada para este turno"
            },
            rutaNavegacion = "caja",
            textoAccion = if (cajaPendienteDeCierreAnterior) "Cerrar Turno Pendiente" else "Abrir Caja"
        )
    )
}

data class ItemChecklistApertura(
    val clave: String,
    val titulo: String,
    val completado: Boolean,
    val detalle: String,
    val rutaNavegacion: String?,
    val textoAccion: String?
)
