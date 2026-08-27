package com.app.administradorfarmadon.configuracion.plan.datos

import androidx.compose.runtime.Immutable

@Immutable
data class MetodoPagoBrixoItem(
    val id: String = "",
    val bancoNombre: String = "",
    val tipoCuenta: String = "CORRIENTE",
    val moneda: String = "PEN",
    val numeroCuenta: String = "",
    val numeroCci: String = "",
    val titular: String = "",
    val iconoKey: String = ""
)

@Immutable
data class BrixoCanalesPagoInfo(
    val razonSocial: String = "",
    val ruc: String = "",
    val whatsappCobranzas: String = "",
    val emailCobranzas: String = "",
    val mensajePrellenado: String = "Hola BRIXO, adjunto comprobante de pago de mi farmacia.",
    val metodosActivos: List<MetodoPagoBrixoItem> = emptyList()
)

@Immutable
data class PlanCatalogoItem(
    val id: String = "",
    val nombre: String = "",
    val precioMensual: Double = 0.0,
    val maxSucursales: Int = 1,
    val diasPrueba: Int = 0,
    val descripcion: String = "",
    val esRecomendado: Boolean = false,
    val activo: Boolean = true,
    val paisIso: String = "",
    val monedaCodigo: String = "PEN",
    val monedaSimbolo: String = "S/"
)

@Immutable
data class PlanFacturacionInfo(
    val planId: String = "",
    val planNombre: String = "Plan Estándar",
    val precioMensual: Double = 0.0,
    val precioProximaRenovacion: Double? = null,
    val periodo: String = "mensual",
    val maxSucursales: Int = 1,
    val sucursalesActivas: Int = 0,
    val features: List<String> = emptyList(),
    // Moneda viva del contrato (B4 — per-client). Si BrixoPanel cambia país/moneda
    // solo para este cliente, Farmadon la refleja sin relogin.
    val monedaCodigo: String = "PEN",
    val monedaSimbolo: String = "S/",
    val estadoSuscripcion: String = "activa",
    val esPrueba: Boolean = false,
    val diasPruebaContratados: Int = 0,
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val fechaInicioMs: Long = 0L,
    val fechaFinMs: Long = 0L,
    val diasRestantesTotal: Int = 0,
    val diasRestantesPrueba: Int = 0,
    val enPeriodoPrueba: Boolean = false,
    val fechaFinPrueba: String = "",
    // Fecha de fin ORIGINAL antes de cualquier cortesía/prórroga (BRIXO la escribe).
    // Necesaria para replicar la regla de BRIXO: la cortesía solo cuenta si ya
    // venció la fecha original. Sin esto, Farmadon calculaba la cortesía mal.
    val fechaFinOriginal: String = "",
    val pagosRealizados: Int = 0,
    val ultimoPagoFecha: String = "",
    val precioPagado: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    // Cortesía / Días de Gracia
    val tieneBeneficioCortesia: Boolean = false,
    val diasCortesia: Int = 0,
    val motivoCortesia: String = "",
    val fechaOtorgamientoCortesia: String = "",
    val sincronizadoServidor: Boolean = true
)

@Immutable
data class HistorialPagoItem(
    val id: String = "",
    val fecha: String = "",
    val accion: String = "PAGO_REGISTRADO",
    val concepto: String = "Mensualidad de Servicio",
    val monto: Double = 0.0,
    val motivo: String = "",
    val admin: String = "BRIXO Central",
    val vigenciaHasta: String = "",
    val banco: String = "",
    val numeroOperacion: String = "",
    val comprobanteUrl: String = ""
)

@Immutable
data class SolicitudPagoInfo(
    val id: String = "",
    val estado: String = "PENDIENTE",
    val banco: String = "",
    val montoReportado: Double = 0.0,
    val numeroOperacion: String = "",
    val comprobanteUrl: String = "",
    val mensajeBrixo: String = "",
    val notaAclaratoriaCliente: String = "",
    val fechaReporte: String = "",
    val fechaObservacion: String = ""
)

