package com.app.administradorfarmadon.analitica_reportes.resumen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.DesgloseLineaFinanciera
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.EstadoVacioSaaS
import com.app.administradorfarmadon.analitica_reportes.logica.AlertaVerdadNegocio
import com.app.administradorfarmadon.analitica_reportes.logica.ComprasAnalytics
import com.app.administradorfarmadon.analitica_reportes.logica.DineroYCajaAnalytics
import com.app.administradorfarmadon.analitica_reportes.logica.EstadoResultadoNegocio
import com.app.administradorfarmadon.analitica_reportes.logica.MetricasVentas
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import java.util.Locale
import kotlin.math.abs

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: RESUMEN GENERAL EJECUTIVO (BALANCE FINANCIERO & ARQUEOS REALES)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaResumenSection(
    metricas: MetricasVentas,
    estadoResultado: EstadoResultadoNegocio = EstadoResultadoNegocio(),
    dineroYCaja: DineroYCajaAnalytics = DineroYCajaAnalytics(),
    alertasVerdad: List<AlertaVerdadNegocio> = emptyList(),
    compras: ComprasAnalytics = ComprasAnalytics(),
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa,
    esVacio: Boolean,
    onNavegarSubSeccion: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── ENCABEZADO EJECUTIVO ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Resumen General",
                    style = FDType.Heading1.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Período: $periodo · Sede: $sedeNombre",
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = FDColors.TextSecondary
                )
            }
        }

        // ── BANNER DE ALERTAS SOBRIAS DE LA VERDAD ────────────────────────────
        if (alertasVerdad.isNotEmpty()) {
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    alertasVerdad.forEach { alerta ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (alerta.severidad == "ERROR") Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (alerta.severidad == "ERROR") FDColors.Error else FDColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${alerta.titulo}:",
                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = if (alerta.severidad == "ERROR") FDColors.Error else FDColors.TextPrimary
                            )
                            Text(
                                text = alerta.descripcion,
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = FDColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (esVacio) {
            EstadoVacioSaaS(
                titulo = "Sin movimientos en este período",
                subtitulo = "No se registraron ventas, cobros, compras ni aperturas de caja en $periodo para $sedeNombre."
            )
        } else {
            // ── 4 TARJETAS PRINCIPALES DEL NEGOCIO (SOBRIAS, DIRECTAS) ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Ganancia Real
                TarjetaResumenEjecutiva(
                    titulo = "GANANCIA NETA REAL",
                    monto = "S/ %.2f".format(Locale.US, estadoResultado.utilidadNeta),
                    subtitulo = "Margen neto: %.1f%%".format(Locale.US, estadoResultado.margenNeto),
                    tag = if (estadoResultado.utilidadNeta >= 0.0) "Neto positivo" else "Déficit",
                    icono = Icons.Default.AttachMoney,
                    destacado = true,
                    esNegativo = estadoResultado.utilidadNeta < 0.0,
                    modifier = Modifier.weight(1f)
                )

                // 2. Ventas Totales
                TarjetaResumenEjecutiva(
                    titulo = "VENTAS COBRADAS",
                    monto = "S/ %.2f".format(Locale.US, metricas.ventasNetas),
                    subtitulo = "${metricas.cantidadTransacciones} operaciones",
                    tag = "Ticket S/ %.2f".format(Locale.US, metricas.ticketPromedio),
                    icono = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                // 3. Compras Realizadas
                TarjetaResumenEjecutiva(
                    titulo = "COMPRAS FACTURADAS",
                    monto = "S/ %.2f".format(Locale.US, compras.comprasNetas),
                    subtitulo = "${compras.cantidadFacturasCompra} facturas",
                    tag = "Contado S/ %.2f".format(Locale.US, compras.comprasContado),
                    icono = Icons.Default.LocalShipping,
                    modifier = Modifier.weight(1f)
                )

                // 4. Dinero en Caja
                val subCaja = when {
                    dineroYCaja.diferenciaCajaTotal < -0.01 -> "Faltante: -S/ %.2f".format(Locale.US, abs(dineroYCaja.diferenciaCajaTotal))
                    dineroYCaja.diferenciaCajaTotal > 0.01 -> "Sobrante: +S/ %.2f".format(Locale.US, dineroYCaja.diferenciaCajaTotal)
                    else -> "Gavetas cuadradas"
                }
                TarjetaResumenEjecutiva(
                    titulo = "DINERO EN CAJA",
                    monto = "S/ %.2f".format(Locale.US, dineroYCaja.cajaEsperadaTotal),
                    subtitulo = subCaja,
                    tag = "${dineroYCaja.cantidadTurnosCerrados} turnos",
                    icono = Icons.Default.PointOfSale,
                    esNegativo = dineroYCaja.diferenciaCajaTotal < -0.01,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── BALANCE FINANCIERO DEL PERÍODO (P&L REAL Y CONTROL DE GAVETA) ──
            PanelEstadoResultados(
                metricas = metricas,
                estadoResultado = estadoResultado,
                dineroYCaja = dineroYCaja,
                compras = compras
            )
        }
    }
}

/** Balance Financiero del Negocio: Estado de Resultados Económico y Flujo de Gaveta. */
@Composable
private fun PanelEstadoResultados(
    metricas: MetricasVentas,
    estadoResultado: EstadoResultadoNegocio,
    dineroYCaja: DineroYCajaAnalytics,
    compras: ComprasAnalytics
) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "BALANCE FINANCIERO DEL PERÍODO (P&L REAL)",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Ingresos netos, costos históricos de mercadería y deducciones reales del negocio",
                    style = FDType.Caption.copy(fontSize = 10.sp),
                    color = FDColors.TextSecondary
                )
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

            DesgloseLineaFinanciera("Ventas netas de mostrador", "S/ %.2f".format(Locale.US, metricas.ventasNetas), FDColors.TextPrimary)
            DesgloseLineaFinanciera("Costo de mercadería vendida (COGS)", "− S/ %.2f".format(Locale.US, estadoResultado.costoVentasCogs), FDColors.TextSecondary)
            DesgloseLineaFinanciera("Ganancia comercial bruta", "= S/ %.2f (%.1f%%)".format(Locale.US, estadoResultado.utilidadBruta, estadoResultado.margenBruto), FDColors.TextPrimary, esDestacada = true)

            if (estadoResultado.gastosOperativos > 0.0) {
                DesgloseLineaFinanciera("Gastos operativos y retiros de gaveta", "− S/ %.2f".format(Locale.US, estadoResultado.gastosOperativos), FDColors.TextSecondary)
            }
            if (estadoResultado.faltanteCaja > 0.0) {
                DesgloseLineaFinanciera("Pérdida por faltante de dinero en gaveta", "− S/ %.2f".format(Locale.US, estadoResultado.faltanteCaja), FDColors.Error)
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

            Surface(
                color = if (estadoResultado.utilidadNeta >= 0.0) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Error.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    DesgloseLineaFinanciera(
                        "GANANCIA NETA REAL",
                        "= S/ %.2f".format(Locale.US, estadoResultado.utilidadNeta),
                        if (estadoResultado.utilidadNeta >= 0.0) FDColors.Primary else FDColors.Error,
                        esDestacada = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CONTROL DE FLUJO DE EFECTIVO Y ARQUEOS",
                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextSecondary
                )
            }

            DesgloseLineaFinanciera(
                "Compras de mercadería a droguerías / proveedores",
                "S/ %.2f (Contado: S/ %.2f · Crédito: S/ %.2f)".format(Locale.US, compras.comprasNetas, compras.comprasContado, compras.comprasCredito),
                FDColors.TextSecondary
            )
            DesgloseLineaFinanciera("Fondo inicial entregado a cajas", "S/ %.2f".format(Locale.US, dineroYCaja.aperturasTotal), FDColors.TextSecondary)
            DesgloseLineaFinanciera("Efectivo total esperado en gavetas", "S/ %.2f".format(Locale.US, dineroYCaja.cajaEsperadaTotal), FDColors.TextPrimary)
            DesgloseLineaFinanciera("Efectivo contado físicamente en cierres", "S/ %.2f".format(Locale.US, dineroYCaja.cajaContadaTotal), FDColors.TextPrimary)

            val difTexto = when {
                dineroYCaja.diferenciaCajaTotal < -0.01 -> "-S/ %.2f (Faltante en gavetas)".format(Locale.US, abs(dineroYCaja.diferenciaCajaTotal))
                dineroYCaja.diferenciaCajaTotal > 0.01 -> "+S/ %.2f (Sobrante en gavetas)".format(Locale.US, dineroYCaja.diferenciaCajaTotal)
                else -> "S/ 0.00 (Cuadrada exacta)"
            }
            DesgloseLineaFinanciera(
                "Diferencia neta en arqueos",
                difTexto,
                if (dineroYCaja.diferenciaCajaTotal < -0.01) FDColors.Error else FDColors.TextPrimary,
                esDestacada = true
            )
        }
    }
}

/** Tarjeta ejecutiva sobria para los indicadores superiores del resumen general. */
@Composable
private fun TarjetaResumenEjecutiva(
    titulo: String,
    monto: String,
    subtitulo: String,
    tag: String,
    icono: ImageVector,
    destacado: Boolean = false,
    esNegativo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (esNegativo) FDColors.Error.copy(alpha = 0.6f)
            else if (destacado) FDColors.Primary.copy(alpha = 0.4f)
            else FDColors.Border
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = if (esNegativo) FDColors.Error else if (destacado) FDColors.Primary else FDColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = titulo,
                        style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = if (esNegativo) FDColors.ErrorSubtle else if (destacado) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tag,
                        style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                        color = if (esNegativo) FDColors.Error else if (destacado) FDColors.Primary else FDColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = monto,
                style = FDType.Heading2.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterPremium,
                    letterSpacing = (-0.3).sp
                ),
                color = if (esNegativo) FDColors.Error else FDColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitulo,
                style = FDType.Caption.copy(fontSize = 10.sp),
                color = if (esNegativo) FDColors.Error else FDColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
