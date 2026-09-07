package com.app.administradorfarmadon.ventas.compartido.modelo

/**
 * MODELOS REALES DEL PUNTO DE VENTA (R3/R12: cero campos de relleno).
 *
 * Todo lo que ve el cajero nace de estos documentos en Firestore:
 *   - Venta            → sucursal/ventas/{id}          (comprobante con serie y correlativo)
 *   - CajaSesion       → sucursal/caja_sesiones/{id}   (turno abierto/cerrado)
 *   - EstadoCaja       → sucursal/caja_sesiones/actual (puntero atómico del turno vigente)
 *   - MovimientoCaja   → sucursal/caja_movimientos/{id}
 *   - VentaSuspendida  → sucursal/ventas_suspendidas/{id}
 *   - DevolucionVenta  → sucursal/devoluciones/{id}
 */

// ───────────────────────────── CLIENTE EN LA VENTA ─────────────────────────────

data class ClienteDeVenta(
    /** NINGUNO = Consumidor Final · DNI (8) → Boleta · RUC (11) → Factura */
    val tipoDocumento: String = "NINGUNO",
    val numeroDocumento: String = "",
    val nombre: String = "Consumidor Final",
    val direccion: String = "",
    /** Si el cliente ya existe en el directorio de la farmacia, queda enlazado. */
    val clienteId: String = ""
) {
    val esConsumidorFinal: Boolean get() = tipoDocumento == "NINGUNO"
}

// ───────────────────────────── ÍTEMS DE LA VENTA ─────────────────────────────

/** Qué lote físico salió y cuánto (unidades físicas). Trazabilidad pieza por pieza. */
data class LoteConsumido(
    val loteId: String = "",
    val loteNumero: String = "",
    val vencimiento: String = "",
    val cantidadFisica: Double = 0.0,
    val costoUnitarioReal: Double = 0.0
)

data class ItemVenta(
    val productoId: String = "",
    val nombreProducto: String = "",
    val empaque: String = "",
    val presentacionId: String = "",
    val presentacionNombre: String = "",
    /** Unidades vendidas de ESA presentación (siempre entero: 2 blíster, 1 caja…). */
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0,
    val requiereReceta: Boolean = false,
    /** La cajera marcó este producto como receta verificada en el mostrador. */
    val recetaVerificada: Boolean = false,
    /** Trazabilidad de anaquel y lote sugerido por FEFO (FASE 11 H2). */
    val loteSugerido: String = "",
    val loteVencimientoSugerido: String = "",
    val ubicacionAnaquel: String = "",
    /** Lotes reales que se descontaron para esta línea (FEFO/prioridad del dueño). */
    val lotesConsumidos: List<LoteConsumido> = emptyList(),
    /** Cuántas unidades de esta línea ya regresaron por devolución. */
    val cantidadDevuelta: Int = 0,
    val costoTotalReal: Double = 0.0
) {
    val cantidadDevolvible: Int get() = (cantidad - cantidadDevuelta).coerceAtLeast(0)
}

// ───────────────────────────── PAGOS DE LA VENTA ─────────────────────────────

/** Una porción del cobro con un método concreto (pagos mixtos reales). */
data class PagoVenta(
    /** Tipo fijo: EFECTIVO, YAPE, PLIN, TRANSFERENCIA, TARJETA_POS, CHEQUE. */
    val tipoId: String = "",
    val instanciaId: String = "",
    val nombreMetodo: String = "",
    val monto: Double = 0.0,
    val numeroOperacion: String = ""
)

// ───────────────────────────── LA VENTA (COMPROBANTE) ─────────────────────────────

/**
 * Venta cerrada y cobrada. Documento único de verdad del comprobante.
 * Se escribe dentro de UNA transacción junto con stock, kardex y caja.
 */
data class Venta(
    val id: String = "",
    /** Número visible en el ticket: B001-000042 (boleta) o F001-000012 (factura). */
    val numeroCompleto: String = "",
    val tipoComprobante: String = "BOLETA", // BOLETA | FACTURA
    val serie: String = "B001",
    val correlativo: Long = 0L,
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val items: List<ItemVenta> = emptyList(),
    val totalItems: Int = 0,
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val total: Double = 0.0,
    val pagos: List<PagoVenta> = emptyList(),
    val montoRecibido: Double = 0.0,
    val vuelto: Double = 0.0,
    val estado: String = ESTADO_COMPLETADA, // COMPLETADA | DEVOLUCION_PARCIAL | DEVOLUCION_TOTAL | ANULADA
    val cajaSesionId: String = "",
    val cajaId: String = "",
    val cajeroId: String = "",
    val cajeroNombre: String = "",
    /** ms corregidos por hora de servidor (consultables y ordenables sin depender del reloj local). */
    val fechaHoraMs: Long = 0L,
    /** "yyyy-MM-dd" para traer SOLO las ventas del día con un filtro simple. */
    val diaClave: String = "",
    // FASE 12: Contrato fiscal y trazabilidad de anulación
    val estadoFiscal: String = "PENDIENTE",
    val moduloOrigen: String = "POS",
    val anuladaPorId: String = "",
    val anuladaPorNombre: String = "",
    val anulacionMotivo: String = "",
    val anuladaEnMs: Long = 0L,
    val numeroNotaCredito: String = "",
    val costoTotalReal: Double = 0.0,
    // Trazabilidad de devolución (origen POS)
    val devolucionMotivo: String = "",
    val devolucionPorId: String = "",
    val devolucionPorNombre: String = "",
    val devolucionEnMs: Long = 0L,
    val devolucionNumeroNotaCredito: String = "",
    val devolucionMetodoReembolso: String = "",
    // Firma de autorización supervisor (POS Config)
    val autorizadoPorId: String = "",
    val autorizadoPorNombre: String = "",
    val autorizadoPorRol: String = "",
    // Rastro de receta verificada en mostrador (quién la pidió y cuándo)
    val recetaVerificada: Boolean = false,
    val recetaVerificadaPor: String = "",
    val recetaVerificadaEnMs: Long = 0L
) {
    companion object {
        const val ESTADO_COMPLETADA = "COMPLETADA"
        const val ESTADO_DEVOLUCION_PARCIAL = "DEVOLUCION_PARCIAL"
        const val ESTADO_DEVOLUCION_TOTAL = "DEVOLUCION_TOTAL"
        const val ESTADO_ANULADA = "ANULADA"
    }

    val totalDevuelto: Double
        get() = items.sumOf { it.cantidadDevuelta * it.precioUnitario }

    /**
     * Devolución prorrateada con el descuento original de la venta.
     * Coincide con el reembolso real calculado en [VentasRepository.registrarDevolucion]:
     * monto = cantidad × precio × (total / subtotal).
     * Sin este prorrateo, las ventas con descuento mostrarían un neto menor al dinero real.
     */
    val totalDevueltoProrrateado: Double
        get() {
            val bruto = totalDevuelto
            if (bruto <= 0.0) return 0.0
            val factor = if (subtotal > 0.0) (total / subtotal).coerceIn(0.0, 1.0) else 1.0
            return kotlin.math.round(bruto * factor * 100.0) / 100.0
        }

    /** Neto real de ESTE comprobante: total − devuelto prorrateado (0 si anulada/total). */
    val totalNetoComprobante: Double
        get() {
            if (estado == ESTADO_ANULADA || estado == ESTADO_DEVOLUCION_TOTAL) return 0.0
            return kotlin.math.round((total - totalDevueltoProrrateado).coerceAtLeast(0.0) * 100.0) / 100.0
        }
}

// ───────────────────────────── CAJA (TURNO) ─────────────────────────────

/** Turno de caja persistido: apertura, cierre y totales finales. */
data class CajaSesion(
    val id: String = "",
    val estado: String = ESTADO_ABIERTA, // ABIERTA | CERRADA
    val fondoInicial: Double = 0.0,
    val aperturaMs: Long = 0L,
    val aperturaLegible: String = "",
    val abiertoPorId: String = "",
    val abiertoPorNombre: String = "",
    val cajaId: String = "",
    // Datos del cierre (vacíos mientras está abierta)
    val cierreMs: Long = 0L,
    val cierreLegible: String = "",
    val cierreExtemporaneo: Boolean = false,
    val cierreFisicoRealMs: Long = 0L,
    val cierreFisicoRealLegible: String = "",
    val cerradoPorId: String = "",
    val cerradoPorNombre: String = "",
    val efectivoContado: Double = 0.0,
    val efectivoEsperado: Double = 0.0,
    val diferenciaEfectivo: Double = 0.0,
    val observaciones: String = "",
    val ventasPorMetodo: Map<String, Double> = emptyMap(),
    val totalVentas: Double = 0.0,
    val ingresos: Double = 0.0,
    val retiros: Double = 0.0,
    val devolucionesEfectivo: Double = 0.0,
    val cantidadVentas: Int = 0,
    val cantidadDevoluciones: Int = 0
) {
    val cajaIdentificador: String
        get() = cajaId.ifBlank { if (abiertoPorId.isNotBlank()) "caja_$abiertoPorId" else "caja_principal" }

    companion object {
        const val ESTADO_ABIERTA = "ABIERTA"
        const val ESTADO_CERRADA = "CERRADA"
    }
}

/**
 * Puntero atómico del turno vigente por cajero (R1/R3/R8: la caja y el turno son individuales por cajero).
 * Acumula en vivo el dinero esperado por método: es lo que el cierre compara
 * contra el conteo físico. Se actualiza SOLO dentro de transacciones.
 */
data class EstadoCaja(
    val estado: String = CajaSesion.ESTADO_CERRADA,
    val sesionId: String = "",
    val fondoInicial: Double = 0.0,
    val aperturaMs: Long = 0L,
    val abiertoPorNombre: String = "",
    val abiertoPorId: String = "",
    val cajeroId: String = "",
    val cajaId: String = "",
    /** Acumulado de ventas cobradas por tipo de pago: {EFECTIVO: 120.0, YAPE: 45.0, ...}.
     *  Va NETO: una devolución resta directamente del método por el que se reembolsó. */
    val ventasPorMetodo: Map<String, Double> = emptyMap(),
    val ingresos: Double = 0.0,
    val retiros: Double = 0.0,
    /** Informativo para el desglose del cierre: cuánto salió en reembolsos de efectivo.
     *  NO se resta del esperado (ya está neteado dentro de ventasPorMetodo). */
    val devolucionesEfectivo: Double = 0.0,
    val cantidadVentas: Int = 0,
    val cantidadDevoluciones: Int = 0
) {
    val cajaIdentificador: String
        get() = cajaId.ifBlank {
            val cid = cajeroId.ifBlank { abiertoPorId }
            if (cid.isNotBlank()) "caja_$cid" else "caja_principal"
        }
    val ventasEfectivo: Double get() = ventasPorMetodo["EFECTIVO"] ?: 0.0
    /** Lo que DEBE haber en el cajón de efectivo en este momento. */
    val efectivoEsperado: Double
        get() = fondoInicial + ventasEfectivo + ingresos - retiros
    val totalVentas: Double get() = ventasPorMetodo.values.sum()

    /**
     * Determina si la caja está abierta pero corresponde a un día calendario anterior.
     * En ese caso, el POS debe bloquear nuevas ventas y exigir el cierre formal de la jornada.
     */
    fun esDeJornadaAnterior(ahoraMs: Long = System.currentTimeMillis()): Boolean {
        if (estado != CajaSesion.ESTADO_ABIERTA || aperturaMs <= 0L) return false
        val tz = java.util.TimeZone.getTimeZone("America/Lima")
        val calApertura = java.util.Calendar.getInstance(tz).apply { timeInMillis = aperturaMs }
        val calHoy = java.util.Calendar.getInstance(tz).apply { timeInMillis = ahoraMs }
        return calApertura.get(java.util.Calendar.YEAR) < calHoy.get(java.util.Calendar.YEAR) ||
                (calApertura.get(java.util.Calendar.YEAR) == calHoy.get(java.util.Calendar.YEAR) &&
                 calApertura.get(java.util.Calendar.DAY_OF_YEAR) < calHoy.get(java.util.Calendar.DAY_OF_YEAR))
    }

    fun fechaAperturaLegible(): String {
        if (aperturaMs <= 0L) return ""
        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("America/Lima")
        }
        return fmt.format(java.util.Date(aperturaMs))
    }

    val esCajaPendienteDeOtroDia: Boolean get() = esDeJornadaAnterior()
    val esTurnoVencido: Boolean get() = esDeJornadaAnterior()

    val estadoOperativo: EstadoOperativoTurno
        get() {
            if (estado != CajaSesion.ESTADO_ABIERTA || aperturaMs <= 0L) return EstadoOperativoTurno.CAJA_CERRADA
            return if (esDeJornadaAnterior()) EstadoOperativoTurno.TURNO_VENCIDO else EstadoOperativoTurno.OPERATIVO
        }
}

/** Estado operativo unificado del turno de caja para todo el POS (R1/R3/R14). */
enum class EstadoOperativoTurno {
    /** Turno abierto y vigente para la jornada de hoy → POS 100% operativo. */
    OPERATIVO,
    /** Turno abierto cuya jornada terminó → BLOQUEO OPERATIVO GLOBAL (Solo Cierre de Caja). */
    TURNO_VENCIDO,
    /** No hay turno abierto → Requiere apertura de caja para operar. */
    CAJA_CERRADA
}

/** Movimiento de dinero de la caja: venta, devolución, ingreso manual, retiro o anulación. */
data class MovimientoCaja(
    val id: String = "",
    val tipo: String = "", // VENTA | DEVOLUCION | INGRESO | RETIRO | ANULACION
    val metodoTipo: String = "",
    val metodoNombre: String = "",
    val monto: Double = 0.0,
    val motivo: String = "",
    /** Venta/devolución que originó el movimiento (si aplica). */
    val referenciaId: String = "",
    val referenciaNumero: String = "",
    val cajaSesionId: String = "",
    val cajaId: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val fechaMs: Long = 0L,
    // Firma de autorización supervisor (POS Config)
    val autorizadoPorId: String = "",
    val autorizadoPorNombre: String = "",
    val autorizadoPorRol: String = ""
) {
    companion object {
        const val TIPO_VENTA = "VENTA"
        const val TIPO_DEVOLUCION = "DEVOLUCION"
        const val TIPO_INGRESO = "INGRESO"
        const val TIPO_RETIRO = "RETIRO"
        const val TIPO_ANULACION = "ANULACION"
    }
}

// ───────────────────────────── VENTA SUSPENDIDA (PAUSA) ─────────────────────────────

/** Venta parqueada para seguir atendiendo; vive en la nube y se retoma desde cualquier caja. */
data class VentaSuspendida(
    val id: String = "",
    val items: List<ItemVenta> = emptyList(),
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val total: Double = 0.0,
    val nota: String = "",
    val creadoPorId: String = "",
    val creadoPorNombre: String = "",
    val fechaMs: Long = 0L
)

// ───────────────────────────── DEVOLUCIÓN ─────────────────────────────

/** Un ítem devuelto dentro de una devolución. */
data class ItemDevolucion(
    val productoId: String = "",
    val nombreProducto: String = "",
    val presentacionNombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0,
    val monto: Double = 0.0,
    val costoUnitarioReal: Double = 0.0,
    val montoCosto: Double = 0.0
)

/** Parámetro de solicitud de devolución para una línea vendida. */
data class ItemDevolucionParam(
    val productoId: String = "",
    val presentacionId: String = "",
    val cantidad: Int = 0
)

/** Nota de crédito interna: stock regresa al lote original, dinero sale de la caja. */
data class DevolucionVenta(
    val id: String = "",
    val ventaId: String = "",
    val numeroVenta: String = "",
    val tipoDocumento: String = "NOTA_CREDITO",
    val serie: String = "NC01",
    val correlativo: Long = 0L,
    val numeroCompleto: String = "",
    val estadoFiscal: String = "PENDIENTE",
    val items: List<ItemDevolucion> = emptyList(),
    val montoReembolso: Double = 0.0,
    /** Método por el que se devolvió el dinero (reembolso real al cliente). */
    val metodoReembolso: String = "EFECTIVO",
    val motivo: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val cajaSesionId: String = "",
    val cajaId: String = "",
    val fechaMs: Long = 0L,
    val diaClave: String = "",
    val costoTotalDevuelto: Double = 0.0,
    // Turno donde nació la venta devuelta (trazabilidad entre turnos/cajeros)
    val ventaCajaSesionId: String = "",
    val ventaCajeroId: String = "",
    val ventaCajeroNombre: String = "",
    // Firma de autorización supervisor (POS Config)
    val autorizadoPorId: String = "",
    val autorizadoPorNombre: String = "",
    val autorizadoPorRol: String = ""
)

/** Datos del emisor que van impresos en el ticket (vienen del documento de la farmacia). */
data class EmisorComprobante(
    val nombreFarmacia: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val sucursalNombre: String = ""
)

// ───────────────────────────── ÍNDICE FISCAL (CONTRATO FACTURACIÓN FASE 12) ─────────────────────────────

/**
 * Índice liviano de comprobantes emitidos en POS para el módulo de Facturación Electrónica.
 * Vive en `farmacias/{f}/facturacion_documentos/{id}`.
 */
data class FacturacionDocumento(
    val id: String = "",
    val tipo: String = "BOLETA", // BOLETA | FACTURA | NOTA_CREDITO | COMUNICACION_BAJA
    val serie: String = "B001",
    val correlativo: Long = 0L,
    val numeroCompleto: String = "",
    val clienteTipoDoc: String = "NINGUNO",
    val clienteNumeroDoc: String = "",
    val clienteNombre: String = "Consumidor Final",
    val ventaId: String = "",
    val devolucionId: String = "",
    val sucursalId: String = "",
    val total: Double = 0.0,
    val estadoEnvio: String = ESTADO_PENDIENTE, // PENDIENTE | ENVIADO | ACEPTADO | RECHAZADO | ANULADO
    val fechaMs: Long = 0L,
    val motivo: String = "",
    val moduloOrigen: String = "POS",
    // ── CAMPOS DE FASE F4 (Motor APISUNAT) ──
    val documentIdProveedor: String = "",
    val xmlUrl: String = "",
    val cdrUrl: String = "",
    val pdfUrl: String = "",
    val numeroQuemado: Boolean = false,
    val ultimoError: String = "",
    val responseTimeMs: Long = 0L,
    val reintentos: Int = 0,
    val historialIntentos: List<Map<String, Any?>> = emptyList()
) {
    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ENVIADO = "ENVIADO"
        const val ESTADO_ACEPTADO = "ACEPTADO"
        const val ESTADO_RECHAZADO = "RECHAZADO"
        const val ESTADO_ANULADO = "ANULADO"
    }
}
