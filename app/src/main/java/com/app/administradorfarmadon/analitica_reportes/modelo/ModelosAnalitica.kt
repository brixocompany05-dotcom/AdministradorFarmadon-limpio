package com.app.administradorfarmadon.analitica_reportes.modelo

import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta

// ═════════════════════════════════════════════════════════════════════════════
// CAPA MODELO (MOLDES): CONTRATOS, ENUMS Y ESTADOS DE ANALÍTICA Y REPORTES
// ═════════════════════════════════════════════════════════════════════════════

enum class PeriodoAnalitica(val label: String) {
    HOY("Hoy"),
    AYER("Ayer"),
    ULTIMOS_7_DIAS("Últimos 7 días"),
    ULTIMOS_15_DIAS("Últimos 15 días"),
    ULTIMOS_30_DIAS("Últimos 30 días"),
    ESTE_MES("Este Mes"),
    MES_ANTERIOR("Mes Anterior"),
    MES("Mes Específico"),
    PERSONALIZADO("Rango Personalizado")
}

enum class FuenteDatos(val label: String, val esCritica: Boolean) {
    VENTAS("Ventas y Facturación POS", esCritica = true),
    CAJA("Caja y Movimientos de Gaveta", esCritica = false),
    INVENTARIO("Inventario y Stock", esCritica = false),
    COMPRAS("Compras a Proveedores", esCritica = false)
}

/**
 * Conciliación fiscal entre ventas registradas en POS y comprobantes electrónicos emitidos.
 */
data class ConciliacionFiscal(
    val totalVentasNoAnuladas: Int = 0,
    val totalDocumentosFiscales: Int = 0,
    val aceptados: Int = 0,
    val pendientes: Int = 0,
    val rechazados: Int = 0,
    val enviados: Int = 0,
    val anulados: Int = 0,
    val diferenciaEmitidosVsFiscal: Int = 0
)

/**
 * Conciliación financiera de caja entre movimientos de gaveta y ventas según POS.
 */
data class ConciliacionCaja(
    val hayDiferencia: Boolean = false,
    val diferenciaTotal: Double = 0.0,
    val hayMovimientosSinMetodo: Boolean = false,
    val sumCajaMovimientos: Map<String, Double> = emptyMap(),
    val ventasSegunPOS: Map<String, Double> = emptyMap(),
    val ventasSegunPunteroCaja: Map<String, Double> = emptyMap(),
    val diferenciasPorMetodo: Map<String, Double> = emptyMap()
)

/**
 * Estado de carga y frescura de cada fuente de datos (R8/R11).
 */
data class EstadoCargaModulo(
    val fuente: FuenteDatos,
    val cargando: Boolean = false,
    val error: String? = null,
    val ultimaActualizacionMs: Long = 0L
)

/**
 * Información representativa de una sucursal para navegación y consolidación multisede.
 */
data class SucursalInfo(
    val id: String,
    val nombre: String,
    val activa: Boolean = true,
    val esPrincipal: Boolean = false
)

/**
 * Métricas consolidadas de ventas para un período y sede (R13).
 */
data class MetricasVentas(
    val ventasBrutas: Double = 0.0,
    val devoluciones: Double = 0.0,
    val ventasNetas: Double = 0.0,
    val costoBruto: Double = 0.0,
    val costoDevuelto: Double = 0.0,
    val costoNeto: Double = 0.0,
    val utilidadBruta: Double = 0.0,
    val margenBruto: Double = 0.0,
    val igvTotal: Double = 0.0,
    val cantidadTransacciones: Int = 0,
    val ticketPromedio: Double = 0.0,
    val tieneCostosEstimados: Boolean = false,
    val porcentajeSinCostoCongelado: Double = 0.0,
    val cantidadVentasSinCosto: Int = 0,
    val ventasPorMetodo: Map<String, Double> = emptyMap(),
    val ticketsPorMetodo: Map<String, Int> = emptyMap()
)

/**
 * Estado general de UI para la pantalla de Analítica y Reportes (SaaS Enterprise).
 */
sealed interface AnaliticaUiState {
    data object Loading : AnaliticaUiState
    data class Error(val mensaje: String) : AnaliticaUiState
    data class Exito(
        val metricas: MetricasVentas,
        val estadoCaja: EstadoCaja,
        val rentabilidad: RentabilidadAnalytics,
        val inventario: InventarioAnalytics,
        val compras: ComprasAnalytics,
        val clientes: ClientesAnalytics,
        val listaVentas: List<Venta>,
        val listaDevoluciones: List<DevolucionVenta>,
        val listaMovimientosCaja: List<MovimientoCaja> = emptyList(),
        val metodosConfigurados: List<InstanciaPago> = emptyList(),
        val esVacio: Boolean,
        val estaActualizando: Boolean = false,
        val leidoEnMs: Long = 0L,
        val fuentesOK: Set<FuenteDatos> = FuenteDatos.entries.toSet(),
        val fuentesFallidas: Map<FuenteDatos, String> = emptyMap(),
        val periodoEtiqueta: String = "",
        val sedeEtiqueta: String = "",
        val historialSesionesCaja: List<CajaSesion> = emptyList(),
        val estadoResultado: EstadoResultadoNegocio = EstadoResultadoNegocio(),
        val dineroYCaja: DineroYCajaAnalytics = DineroYCajaAnalytics(),
        val alertasVerdad: List<AlertaVerdadNegocio> = emptyList(),
        val conciliacionFiscal: ConciliacionFiscal = ConciliacionFiscal(),
        val conciliacionCaja: ConciliacionCaja = ConciliacionCaja(),
        val sucursales: SucursalesAnalytics = SucursalesAnalytics(),
        val insights: InsightsAnalytics = InsightsAnalytics()
    ) : AnaliticaUiState {
        val esParcial: Boolean get() = fuentesFallidas.isNotEmpty()
    }
}
