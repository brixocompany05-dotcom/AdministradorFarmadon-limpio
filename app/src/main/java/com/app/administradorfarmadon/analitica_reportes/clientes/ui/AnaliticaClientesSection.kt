package com.app.administradorfarmadon.analitica_reportes.clientes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.EstadoVacioSaaS
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.MetricKpiSaaS
import com.app.administradorfarmadon.analitica_reportes.logica.ClientesAnalytics
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: CLIENTES (CONSUMO NETO, RECURRENCIA Y RANKING)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaClientesSection(
    clientes: ClientesAnalytics,
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 4 KPIs Clave con Verdad Matemática (Fijos en la parte superior) ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricKpiSaaS(
                titulo = "TOTAL ATENCIONES",
                valor = "${clientes.totalClientesAtendidos}",
                tag = "En $periodo",
                icono = Icons.Default.Groups,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "IDENTIFICADOS",
                valor = "${clientes.clientesIdentificados}",
                tag = "Con DNI o RUC",
                icono = Icons.Default.Person,
                colorValor = if (clientes.clientesIdentificados > 0) FDColors.Primary else FDColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "ANÓNIMOS",
                valor = "${clientes.clientesAnonimos}",
                tag = "Consumidor Final",
                icono = Icons.Default.PersonOutline,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "TICKET PROMEDIO",
                valor = "S/ %.2f".format(Locale.US, clientes.ticketPromedioCliente),
                tag = "Neto por compra",
                icono = Icons.Default.Receipt,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Tarjeta Top 10 Clientes de Mayor Consumo (Con Scroll Interno Independiente) ──
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabecera Fija del Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "TOP 10 CLIENTES CON MAYOR CONSUMO",
                            style = FDType.Heading2.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Filtrado por: $periodo • Sede: $sedeNombre (Exclusivo clientes identificados con compras netas)",
                            style = FDType.Caption.copy(fontSize = 10.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    Surface(
                        color = FDColors.PrimarySubtle,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "TOP 10 • SCROLL ACTIVO",
                            style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // Lista con Scroll Interno Independiente (Cero Apretamiento)
                if (clientes.rankingClientes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Sin clientes identificados en este período",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                text = "Todas las operaciones registradas para $periodo fueron emitidas a Consumidor Final sin documento.",
                                style = FDType.Caption.copy(fontSize = 11.sp, textAlign = TextAlign.Center),
                                color = FDColors.TextTertiary
                            )
                        }
                    }
                } else {
                    val formatoFecha = remember { SimpleDateFormat("dd/MM HH:mm", Locale.US) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        clientes.rankingClientes.forEachIndexed { index, c ->
                            Surface(
                                color = if (index % 2 == 1) FDColors.Background else FDColors.Surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Badge de Posición del Ranking
                                        Surface(
                                            color = when (index) {
                                                0 -> FDColors.Primary
                                                1 -> FDColors.Primary.copy(alpha = 0.8f)
                                                2 -> FDColors.Primary.copy(alpha = 0.65f)
                                                else -> FDColors.SurfaceElevated
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = FDType.Caption.copy(
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (index < 3) FDColors.PrimaryText else FDColors.TextSecondary
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = c.nombre,
                                                style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.TextPrimary
                                            )
                                            val docStr = if (c.documento.isNotBlank()) "Doc: ${c.documento}" else "Sin doc. registrado"
                                            val ultimaStr = if (c.ultimaCompraMs > 0) " • Última: ${formatoFecha.format(Date(c.ultimaCompraMs))}" else ""
                                            val promStr = if (c.ticketPromedio > 0.0) " • Prom: S/ %.2f".format(Locale.US, c.ticketPromedio) else ""
                                            Text(
                                                text = "$docStr • ${c.cantidadCompras} compra(s)$promStr$ultimaStr",
                                                style = FDType.Caption.copy(fontSize = 10.sp),
                                                color = FDColors.TextSecondary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "S/ %.2f".format(Locale.US, c.totalCompradoNeto),
                                            style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Primary
                                        )
                                        Text(
                                            text = "Consumo neto",
                                            style = FDType.Caption.copy(fontSize = 9.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
