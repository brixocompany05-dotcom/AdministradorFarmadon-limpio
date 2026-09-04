package com.app.administradorfarmadon.analitica_reportes.caja.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.app.administradorfarmadon.analitica_reportes.logica.MetricasVentas
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.ui.TablaHistorialCajaSesiones

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: CAJA (TURNOS DE CAJA Y ARQUEOS CON MEDIOS DE PAGO INTEGRADOS)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaCajaSection(
    metricas: MetricasVentas,
    estadoCaja: EstadoCaja = EstadoCaja(),
    historialSesiones: List<CajaSesion> = emptyList(),
    movimientos: List<MovimientoCaja> = emptyList(),
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    metodosConfigurados: List<InstanciaPago> = emptyList(),
    s: MedidaAdaptativa,
    onNavegarSubSeccion: (String) -> Unit = {}
) {
    // Si la caja actual está abierta y no está en la lista de sesiones, se integra para auditoría en vivo
    val sesionActiva = remember(estadoCaja, historialSesiones) {
        if (estadoCaja.estado == CajaSesion.ESTADO_ABIERTA &&
            historialSesiones.none { it.id == estadoCaja.sesionId }
        ) {
            listOf(
                CajaSesion(
                    id = estadoCaja.sesionId.ifBlank { "turno-en-curso" },
                    estado = CajaSesion.ESTADO_ABIERTA,
                    fondoInicial = estadoCaja.fondoInicial,
                    aperturaMs = estadoCaja.aperturaMs,
                    aperturaLegible = estadoCaja.fechaAperturaLegible(),
                    abiertoPorNombre = estadoCaja.abiertoPorNombre,
                    ventasPorMetodo = estadoCaja.ventasPorMetodo,
                    totalVentas = estadoCaja.totalVentas,
                    ingresos = estadoCaja.ingresos,
                    retiros = estadoCaja.retiros,
                    devolucionesEfectivo = estadoCaja.devolucionesEfectivo,
                    cantidadVentas = estadoCaja.cantidadVentas,
                    cantidadDevoluciones = estadoCaja.cantidadDevoluciones,
                    efectivoEsperado = estadoCaja.efectivoEsperado
                )
            )
        } else emptyList()
    }

    // Puerta única de la verdad: sesiones y movimientos reales provenientes de Firestore
    val sesionesUnificadas = remember(historialSesiones, sesionActiva) {
        (sesionActiva + historialSesiones).distinctBy { it.id }
    }

    TablaHistorialCajaSesiones(
        sesiones = sesionesUnificadas,
        movimientos = movimientos,
        simboloMoneda = "S/",
        modifier = Modifier.fillMaxSize()
    )
}
