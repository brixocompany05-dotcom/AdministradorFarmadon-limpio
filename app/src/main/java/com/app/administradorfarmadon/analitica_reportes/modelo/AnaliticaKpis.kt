package com.app.administradorfarmadon.analitica_reportes.modelo

import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta

/**
 * CATÁLOGO FORMAL DE KPIS — ANALÍTICA FARMADON (9 MÓDULOS)
 * CAPA MODELO: MOLDES Y CONTRATOS DE DATOS
 *
 * Regla de oro: "Una sola capa calcula netas, costo, IGV. Cero fórmulas dobles."
 */

// ═════════════════════════════════════════════════════════════════════════════
// 1. MODELOS FORMALES DE SALIDA PARA LOS 9 MÓDULOS DE ANALÍTICA
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Dimensión 1: Resultado Económico del Negocio (Rentabilidad Real)
 * Contrato: Ventas Netas − Costo de Ventas − Gastos Operativos = Utilidad Neta
 */
data class EstadoResultadoNegocio(
    val ventasNetas: Double = 0.0,
    val costoVentasCogs: Double = 0.0,
    val utilidadBruta: Double = 0.0,
    val margenBruto: Double = 0.0,
    val gastosOperativos: Double = 0.0,
    val faltanteCaja: Double = 0.0,
    val utilidadNeta: Double = 0.0,
    val margenNeto: Double = 0.0,
    val tieneCostosEstimados: Boolean = false,
    val diferenciaVsPeriodoAnteriorPct: Double? = null
)

/**
 * Dimensión 2 y 3: Dinero, Flujo y Control Físico de Caja
 * Contrato: Aperturas + Cobros + Otros Ingresos − Egresos = Flujo Neto
 * Control de Caja: Esperado vs Contado = Diferencia (Faltante / Sobrante)
 */
data class DineroYCajaAnalytics(
    val aperturasTotal: Double = 0.0,
    val cobrosTotales: Double = 0.0,
    val otrosIngresos: Double = 0.0,
    val egresosTotales: Double = 0.0,
    val flujoNetoDinero: Double = 0.0,
    val cajaEsperadaTotal: Double = 0.0,
    val cajaContadaTotal: Double = 0.0,
    val diferenciaCajaTotal: Double = 0.0,
    val cantidadTurnosCerrados: Int = 0,
    val cantidadCajasConFaltante: Int = 0,
    val cantidadCajasConSobrante: Int = 0,
    val turnosPendientesDeCierre: Int = 0,
    val fechaCajaPendiente: String = ""
)

data class AlertaVerdadNegocio(
    val clave: String,
    val titulo: String,
    val descripcion: String,
    val severidad: String = "INFO", // INFO, WARNING, ERROR
    val monto: Double? = null
)

/** Módulo 1: Resumen General Ejecutivo */
data class ResumenAnalytics(
    val ventasNetas: Double = 0.0,
    val ventasBrutas: Double = 0.0,
    val totalDevoluciones: Double = 0.0,
    val costoNeto: Double = 0.0,
    val utilidadBruta: Double = 0.0,
    val margenBruto: Double = 0.0,
    val igvTotal: Double = 0.0,
    val cantidadTransacciones: Int = 0,
    val ticketPromedio: Double = 0.0,
    val tieneCostosEstimados: Boolean = false
)

/** Módulo 2: Ventas */
data class VentasAnalytics(
    val totalDocumentos: Int = 0,
    val totalDocumentosEfectivos: Int = 0,
    val ventasNetas: Double = 0.0,
    val ventasBrutas: Double = 0.0,
    val devoluciones: Double = 0.0,
    val ticketPromedio: Double = 0.0,
    val ventasPorHoraTurno: Map<Int, Double> = emptyMap(),
    val ticketsPorHora: Map<Int, Int> = emptyMap(),
    val ventasPorVendedor: Map<String, Double> = emptyMap(),
    val ticketsPorVendedor: Map<String, Int> = emptyMap()
)

/** Módulo 3: Rentabilidad */
data class RentabilidadAnalytics(
    val utilidadBruta: Double = 0.0,
    val margenGeneral: Double = 0.0,
    val costoVentasCogs: Double = 0.0,
    val tieneCostosEstimados: Boolean = false,
    val porcentajeSinCostoCongelado: Double = 0.0,
    val cantidadVentasSinCosto: Int = 0,
    val margenPorCategoria: List<MargenCategoriaItem> = emptyList(),
    val productosMayorMargen: List<MargenProductoItem> = emptyList(),
    val productosMayorGananciaDinero: List<MargenProductoItem> = emptyList(),
    val productosMenorMargen: List<MargenProductoItem> = emptyList(),
    val productosConPerdida: List<MargenProductoItem> = emptyList(),
    val todosLosProductos: List<MargenProductoItem> = emptyList()
)

data class MargenCategoriaItem(
    val categoria: String,
    val ventasNetas: Double,
    val costoNeto: Double,
    val utilidad: Double,
    val margenPorcentaje: Double
)

data class MargenProductoItem(
    val productoId: String,
    val nombre: String,
    val presentacion: String = "",
    val unidadesVendidas: Int = 0,
    val unidadesDevueltas: Int = 0,
    val ventasNetas: Double,
    val costoNeto: Double,
    val utilidad: Double,
    val margenPorcentaje: Double,
    val esEstimado: Boolean
) {
    val unidadesEfectivas: Int get() = (unidadesVendidas - unidadesDevueltas).coerceAtLeast(0)
    val esPerdida: Boolean get() = utilidad < -0.001
}

data class ProductoQuiebreKpi(
    val nombre: String,
    val empaque: String = "Caja",
    val unidadMedida: String = "Unidad",
    val unidadMedidaPlural: String = "Unidades",
    val stockActual: Double,
    val stockMinimo: Double,
    val costoUnitarioCompra: Double = 0.0,
    val unidadesFaltantes: Double = 0.0,
    val costoReposicionFaltante: Double = 0.0,
    val costoPedidoMinimoCompleto: Double = 0.0,
    val origenCosto: String = "",
    val proveedorNombre: String = ""
) {
    val costoReposicion: Double get() = costoReposicionFaltante
}

data class ProductoInmovilizadoKpi(
    val nombre: String,
    val empaque: String = "Caja",
    val unidadMedida: String = "Unidad",
    val unidadMedidaPlural: String = "Unidades",
    val stockActual: Double,
    val costoUnitarioCompra: Double = 0.0,
    val valorEstancado: Double = 0.0,
    val cantidadLotes: Int = 1,
    val esCostoPromedio: Boolean = false
)

/** Módulo 4: Inventario Contable y Rotación de Capital */
data class InventarioAnalytics(
    val valorInventarioTotal: Double = 0.0,
    val capitalEnRotacion: Double = 0.0,
    val capitalInmovilizado: Double = 0.0,
    val tasaRotacionPorcentaje: Double = 0.0,
    val capitalRiesgoQuiebre: Double = 0.0, // Inversión necesaria para nivelar faltantes
    val capitalPedidoMinimoCompleto: Double = 0.0, // Inversión si se pide el lote mínimo completo
    val cantidadQuiebresStock: Int = 0,
    val productosSinCostoDefinido: Int = 0, // Alerta auditoría R3/R12: productos con stock físico pero costo 0
    val detalleQuiebres: List<ProductoQuiebreKpi> = emptyList(),
    val detalleInmovilizados: List<ProductoInmovilizadoKpi> = emptyList(),
    // Compatibilidad legada
    val totalUnidadesDisponibles: Double = 0.0,
    val productosEnQuiebre: List<String> = emptyList(),
    val cantidadPorVencer30Dias: Int = 0,
    val valorInmovilizado90Dias: Double = 0.0,
    val productosInmovilizados: List<String> = emptyList()
)

/** Módulo 5: Compras */
data class ComprasAnalytics(
    val comprasNetas: Double = 0.0,
    val comprasContado: Double = 0.0,
    val comprasCredito: Double = 0.0,
    val totalPagadoProveedores: Double = 0.0,
    val deudaPendienteProveedores: Double = 0.0,
    val cantidadFacturasCompra: Int = 0,
    val comprasPorProveedor: Map<String, Double> = emptyMap(),
    val productosConAumentoCosto: List<VariacionCostoCompra> = emptyList(),
    val facturas: List<com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra> = emptyList()
)

data class VariacionCostoCompra(
    val productoId: String,
    val nombreProducto: String,
    val costoAnterior: Double,
    val costoNuevo: Double,
    val porcentajeAumento: Double
)

/** Módulo 6: Clientes */
data class ClientesAnalytics(
    val totalClientesAtendidos: Int = 0,
    val clientesIdentificados: Int = 0,
    val clientesAnonimos: Int = 0,
    val clientesNuevos: Int = 0,
    val clientesRecurrentes: Int = 0,
    val ticketPromedioCliente: Double = 0.0,
    val rankingClientes: List<ResumenClienteKpi> = emptyList()
)

data class ResumenClienteKpi(
    val clienteId: String,
    val documento: String,
    val nombre: String,
    val totalCompradoNeto: Double,
    val cantidadCompras: Int,
    val ultimaCompraMs: Long
) {
    val ticketPromedio: Double
        get() = if (cantidadCompras > 0 && totalCompradoNeto > 0.0) {
            kotlin.math.round((totalCompradoNeto / cantidadCompras) * 100.0) / 100.0
        } else 0.0
}

/** Módulo 7: Caja & Turnos */
data class CajaAnalytics(
    val estadoTurno: String = "CERRADA",
    val sesionId: String = "",
    val abiertoPorNombre: String = "",
    val fondoInicial: Double = 0.0,
    val ventasEfectivoNetas: Double = 0.0,
    val ingresosManuales: Double = 0.0,
    val retirosManuales: Double = 0.0,
    val efectivoEsperadoEnCajon: Double = 0.0,
    val cobrosPorMetodo: Map<String, Double> = emptyMap(),
    val diferenciaCuadre: Double = 0.0,
    val hayDescuadre: Boolean = false
)

/** Módulo 8: Sucursales — cada sede calculada con la misma fórmula central, sin divergencia. */
data class ItemSucursalAnalytics(
    val sucursalId: String = "",
    val nombreSede: String = "",
    val ventasNetas: Double = 0.0,
    val utilidad: Double = 0.0,
    val cantidadTransacciones: Int = 0
)

data class SucursalesAnalytics(
    val ventasTotalesCadena: Double = 0.0,
    val utilidadCadena: Double = 0.0,
    val ventasPorSede: List<ItemSucursalAnalytics> = emptyList()
)

/** Módulo 9: Insights con evidencia — cero si todo cuadra, alerta solo con prueba. */
data class InsightsAnalytics(
    val alertasStockMinimo: List<String> = emptyList(),
    val alertasPorVencer: List<String> = emptyList(),
    val alertasInmovilizados: List<String> = emptyList(),
    val alertasMargenBajoCostoCongelado: List<String> = emptyList(),
    val alertasDescuadreFiscalCaja: List<String> = emptyList()
)

// ═════════════════════════════════════════════════════════════════════════════
// 2. ESPECIFICACIÓN FORMAL DE CADA KPI (CONTRATOS DE VERDAD)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Contrato de cada métrica calculada.
 * Todo ejecutor o calculador debe cumplir estrictamente estas especificaciones.
 */
object ContratoKpis {
    const val KPI_VENTAS_NETAS = "ventasBrutas - totalDevoluciones"
    const val KPI_VENTAS_BRUTAS = "sum(total) donde estado != ANULADA"
    const val KPI_DEVOLUCIONES = "sum(montoReembolso)"
    const val KPI_TICKET_PROMEDIO = "ventasNetas / cantidadTransaccionesEfectivas"
    const val KPI_IGV_TOTAL = "sum(igvVentas) - sum(igvDevoluciones)"
    const val KPI_COSTO_BRUTO = "sum(costoTotalReal de ventas no anuladas)"
    const val KPI_COSTO_DEVUELTO = "sum(costoTotalDevuelto)"
    const val KPI_COSTO_NETO = "costoBruto - costoDevuelto"
    const val KPI_UTILIDAD_BRUTA = "ventasNetas - costoNeto"
    const val KPI_MARGEN_BRUTO = "(utilidadBruta / ventasNetas) * 100"
    const val KPI_CAJA_ESPERADO = "fondoInicial + ventasEfectivo + ingresos - retiros"
    const val KPI_VALOR_INVENTARIO = "sum(stockFisico * costoUnitarioLote)"
    const val KPI_COMPRAS_NETAS = "sum(montoFacturadoReal) excluyendo CANCELADO"
    const val KPI_CLIENTE_RESUMEN = "count(ventas), sum(ventas - devs), max(fecha)"
    const val KPI_VENTAS_POR_SEDE = "fanOut(sucursalesActivas) -> ventasNetas(sede)"
}
