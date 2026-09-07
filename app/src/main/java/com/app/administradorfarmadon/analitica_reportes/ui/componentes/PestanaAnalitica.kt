package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.analitica_reportes.modelo.FuenteDatos
import com.app.administradorfarmadon.analitica_reportes.caja.ui.AnaliticaCajaSection
import com.app.administradorfarmadon.analitica_reportes.clientes.ui.AnaliticaClientesSection
import com.app.administradorfarmadon.analitica_reportes.compras.ui.AnaliticaComprasSection
import com.app.administradorfarmadon.analitica_reportes.inventario.ui.AnaliticaInventarioSection
import com.app.administradorfarmadon.analitica_reportes.rentabilidad.ui.AnaliticaRentabilidadSection
import com.app.administradorfarmadon.analitica_reportes.resumen.ui.AnaliticaResumenSection
import com.app.administradorfarmadon.analitica_reportes.ventas.ui.AnaliticaVentasSection
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import androidx.compose.ui.graphics.Color

/**
 * PESTAÑA 1: ANALÍTICA ("¿Cómo está funcionando mi farmacia?")
 *
 * Muestra las secciones de analítica con datos reales y alta densidad útil (R8/R12).
 */
@Composable
fun PestanaAnalitica(
    uiState: AnaliticaUiState,
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa,
    onBuscarComprobante: (String) -> Unit = {},
    onVerDetalleVenta: (Venta) -> Unit = {},
    onReintentar: () -> Unit = {},
    onSeleccionarSede: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var subSeccionSeleccionada by remember { mutableStateOf("RESUMEN") }

    val subSecciones = listOf(
        SubSeccionSaaS("RESUMEN", "Resumen General", Icons.Default.Dashboard),
        SubSeccionSaaS("VENTAS", "Ventas", Icons.Default.TrendingUp),
        SubSeccionSaaS("RENTABILIDAD", "Rentabilidad", Icons.Default.AttachMoney),
        SubSeccionSaaS("INVENTARIO", "Inventario", Icons.Default.Inventory2),
        SubSeccionSaaS("COMPRAS", "Compras", Icons.Default.LocalShipping),
        SubSeccionSaaS("CLIENTES", "Clientes", Icons.Default.Groups),
        SubSeccionSaaS("CAJA", "Caja & Pagos", Icons.Default.PointOfSale)
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── PESTAÑAS NAVEGABLES DE SUB-SECCIÓN (SIN FONDOS DE CARD / TRANSPARENTES) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subSecciones.forEach { item ->
                    val esSel = subSeccionSeleccionada == item.clave
                    Box(
                        modifier = Modifier
                            .clickable { subSeccionSeleccionada = item.clave }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.titulo,
                                style = FDType.Label.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (esSel) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = InterPremium
                                ),
                                color = if (esSel) FDColors.Primary else FDColors.TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        color = if (esSel) FDColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = FDColors.Border.copy(alpha = 0.35f),
                thickness = 1.dp
            )
        }

        // ── WORKSPACE DE CONTENIDO CON ESTADOS HONESTOS (R3 / R8 / R9) ─────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState) {
                is AnaliticaUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FDColors.Primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Consolidando métricas del período...",
                                style = FDType.BodySmall,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                }

                is AnaliticaUiState.Error -> {
                    Surface(
                        color = FDColors.ErrorSubtle,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = FDColors.Error)
                                Text(
                                    text = "Error al cargar analítica",
                                    style = FDType.Heading2.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextPrimary
                                )
                            }
                            Text(
                                text = uiState.mensaje,
                                style = FDType.BodySmall,
                                color = FDColors.TextSecondary
                            )
                            Button(
                                onClick = onReintentar,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Error,
                                    contentColor = FDColors.PrimaryText
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                is AnaliticaUiState.Exito -> {
                    Crossfade(
                        targetState = subSeccionSeleccionada,
                        label = "AnaliticaSubSectionTransition"
                    ) { sub ->
                        val errSub = when (sub) {
                            "INVENTARIO" -> uiState.fuentesFallidas[FuenteDatos.INVENTARIO]
                            "COMPRAS" -> uiState.fuentesFallidas[FuenteDatos.COMPRAS]
                            "CAJA" -> uiState.fuentesFallidas[FuenteDatos.CAJA]
                            else -> null
                        }

                        if (errSub != null) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFDC2626))
                                        Text("Datos no disponibles por incidencia de red", style = FDType.Heading2.copy(fontSize = 14.sp), color = Color(0xFF991B1B))
                                    }
                                    Text("No se pudo sincronizar esta sección desde el servidor ($errSub). Las ventas y caja de la farmacia no están afectadas.", style = FDType.BodySmall, color = Color(0xFF7F1D1D))
                                    Button(
                                        onClick = onReintentar,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Reintentar sincronización")
                                    }
                                }
                            }
                        } else {
                            when (sub) {
                                "RESUMEN" -> AnaliticaResumenSection(
                                    metricas = uiState.metricas,
                                    estadoResultado = uiState.estadoResultado,
                                    dineroYCaja = uiState.dineroYCaja,
                                    alertasVerdad = uiState.alertasVerdad,
                                    compras = uiState.compras,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s,
                                    esVacio = uiState.esVacio,
                                    onNavegarSubSeccion = { subSeccionSeleccionada = it }
                                )
                                "VENTAS" -> AnaliticaVentasSection(
                                    ventas = uiState.listaVentas,
                                    devoluciones = uiState.listaDevoluciones,
                                    metricas = uiState.metricas,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s,
                                    onVerDetalleVenta = onVerDetalleVenta
                                )
                                "CAJA" -> AnaliticaCajaSection(
                                    metricas = uiState.metricas,
                                    estadoCaja = uiState.estadoCaja,
                                    historialSesiones = uiState.historialSesionesCaja,
                                    movimientos = uiState.listaMovimientosCaja,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    metodosConfigurados = uiState.metodosConfigurados,
                                    s = s,
                                    onNavegarSubSeccion = { subSeccionSeleccionada = it }
                                )
                                "RENTABILIDAD" -> AnaliticaRentabilidadSection(
                                    rentabilidad = uiState.rentabilidad,
                                    estadoResultado = uiState.estadoResultado,
                                    tieneCostosEstimados = uiState.metricas.tieneCostosEstimados,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s
                                )
                                "INVENTARIO" -> AnaliticaInventarioSection(
                                    inventario = uiState.inventario,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s
                                )
                                "COMPRAS" -> AnaliticaComprasSection(
                                    compras = uiState.compras,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s
                                )
                                "CLIENTES" -> AnaliticaClientesSection(
                                    clientes = uiState.clientes,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s
                                )
                                else -> AnaliticaResumenSection(
                                    metricas = uiState.metricas,
                                    estadoResultado = uiState.estadoResultado,
                                    dineroYCaja = uiState.dineroYCaja,
                                    alertasVerdad = uiState.alertasVerdad,
                                    compras = uiState.compras,
                                    periodo = periodo,
                                    sedeNombre = sedeNombre,
                                    s = s,
                                    esVacio = uiState.esVacio,
                                    onNavegarSubSeccion = { subSeccionSeleccionada = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SubSeccionSaaS(
    val clave: String,
    val titulo: String,
    val icono: ImageVector
)
