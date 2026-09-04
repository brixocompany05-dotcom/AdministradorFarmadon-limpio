package com.app.administradorfarmadon.ventas.compartido.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * MÓDULO CAJA & PAGOS — PANEL DE NAVEGACIÓN DUAL MASTER-DETAIL (60% TABLA / 40% DESGLOSE).
 *
 * Cumple R1, R3, R4, R5, R6, R8, R12:
 * - Paginación ergonómica de alto rendimiento para miles de turnos (10, 15, 25, 50 por página).
 * - Cero sobrecarga de memoria en Compose.
 * - Desacoplado de responsabilidades: tabla 60% aquí, auditoría 40% en [PanelDetalleTurnoCaja].
 */
@Composable
fun TablaHistorialCajaSesiones(
    sesiones: List<CajaSesion>,
    movimientos: List<MovimientoCaja> = emptyList(),
    simboloMoneda: String = "S/",
    modifier: Modifier = Modifier
) {
    val tzLima = remember { TimeZone.getTimeZone("America/Lima") }
    val fmtHora = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tzLima } }
    val fmtFechaCorta = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tzLima } }

    var queryBusqueda by remember { mutableStateOf("") }
    var sesionSeleccionadaId by remember { mutableStateOf<String?>(null) }

    // ── ESTADO DE PAGINACIÓN DE ALTO RENDIMIENTO ──
    var paginaActual by remember { mutableIntStateOf(1) }
    var itemsPorPagina by remember { mutableIntStateOf(15) }

    // Filtrado en memoria ordenado por fecha descendente
    val sesionesFiltradas = remember(sesiones, queryBusqueda) {
        sesiones.filter { s ->
            if (queryBusqueda.isNotBlank()) {
                val q = queryBusqueda.trim().lowercase()
                val fechaAperturaLegible = fmtFechaCorta.format(Date(s.aperturaMs)).lowercase()
                val nombreAbre = s.abiertoPorNombre.lowercase()
                val nombreCierra = s.cerradoPorNombre.lowercase()
                val obs = s.observaciones.lowercase()
                q in nombreAbre || q in nombreCierra || q in fechaAperturaLegible || q in obs
            } else {
                true
            }
        }.sortedByDescending { if (it.aperturaMs > 0L) it.aperturaMs else it.cierreMs }
    }

    // Resetear a página 1 al cambiar el filtro de búsqueda o el tamaño de la lista
    LaunchedEffect(queryBusqueda, sesiones.size) {
        paginaActual = 1
    }

    // Cálculo matemático de páginas
    val totalPaginas = remember(sesionesFiltradas.size, itemsPorPagina) {
        maxOf(1, (sesionesFiltradas.size + itemsPorPagina - 1) / itemsPorPagina)
    }

    // Asegurar que paginaActual no desborde si se filtran resultados
    LaunchedEffect(totalPaginas) {
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas
        }
    }

    // Ventana paginada para LazyColumn (solo se procesan y renderizan los elementos de la página activa)
    val sesionesPaginadas = remember(sesionesFiltradas, paginaActual, itemsPorPagina) {
        val start = (paginaActual - 1) * itemsPorPagina
        sesionesFiltradas.drop(start).take(itemsPorPagina)
    }

    // Auto-selección del turno (selecciona el actual si existe en la lista, o el primero de la página)
    val sesionActivaDetalle = remember(sesionesFiltradas, sesionesPaginadas, sesionSeleccionadaId) {
        sesionesFiltradas.find { it.id == sesionSeleccionadaId }
            ?: sesionesPaginadas.firstOrNull()
            ?: sesionesFiltradas.firstOrNull()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1. BUSCADOR FIJO ALTO (44dp) ──
        OutlinedTextField(
            value = queryBusqueda,
            onValueChange = { queryBusqueda = it },
            placeholder = {
                Text(
                    "Buscar turno por cajero, fecha u observaciones (ej: Juan, 02/09)...",
                    style = FDType.BodySmall.copy(fontSize = 12.5.sp)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = FDColors.InputPlaceholder,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (queryBusqueda.isNotBlank()) {
                    IconButton(onClick = { queryBusqueda = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = FDColors.InputPlaceholder,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FDColors.BorderFocus,
                unfocusedBorderColor = FDColors.InputBorder,
                focusedContainerColor = FDColors.InputBackground,
                unfocusedContainerColor = FDColors.InputBackground,
                focusedTextColor = FDColors.InputText,
                unfocusedTextColor = FDColors.InputText,
                focusedPlaceholderColor = FDColors.InputPlaceholder,
                unfocusedPlaceholderColor = FDColors.InputPlaceholder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // ── 2. PANEL DUAL MASTER-DETAIL (60% TABLA TIPO EXCEL / 40% AUDITORÍA DETALLADA) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── PANEL IZQUIERDO (60% ANCHO): TABLA ESTILO EXCEL CON PAGINACIÓN INTEGRADA ──
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Cabecera Fija de la Tabla
                    Surface(
                        color = FDColors.SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Turno / Cajero",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.9f)
                            )
                            Text(
                                "Horario",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                "Cobrado",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                "Egresos",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                "Arqueo",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                "Diferencia",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                "Estado",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                    // Cuerpo de la Tabla (Filas Paginadas)
                    if (sesionesFiltradas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = FDColors.TextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    if (queryBusqueda.isNotBlank()) "No se encontraron turnos con el término '$queryBusqueda'"
                                    else "No se registraron turnos en el período seleccionado",
                                    style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(
                                items = sesionesPaginadas,
                                key = { it.id }
                            ) { sesion ->
                                val esSel = sesionActivaDetalle?.id == sesion.id
                                val esAbierta = sesion.estado == CajaSesion.ESTADO_ABIERTA
                                val horaAbre = if (sesion.aperturaMs > 0L) fmtHora.format(Date(sesion.aperturaMs)) else "--:--"
                                val horaCierra = if (sesion.cierreMs > 0L) fmtHora.format(Date(sesion.cierreMs)) else "En curso"

                                Surface(
                                    color = if (esSel) FDColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sesionSeleccionadaId = sesion.id }
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Turno / Cajero
                                            Row(
                                                modifier = Modifier.weight(1.9f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            color = if (esAbierta) FDColors.Success else FDColors.TextDisabled,
                                                            shape = RoundedCornerShape(50)
                                                        )
                                                )
                                                Column {
                                                    Text(
                                                        text = sesion.abiertoPorNombre.ifBlank { "Cajero" },
                                                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (sesion.aperturaMs > 0L) fmtFechaCorta.format(Date(sesion.aperturaMs)) else "",
                                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                                        color = FDColors.TextTertiary
                                                    )
                                                }
                                            }

                                            // 2. Horario
                                            Column(modifier = Modifier.weight(1.4f)) {
                                                Text(
                                                    text = horaAbre,
                                                    style = FDType.Caption.copy(fontSize = 10.5.sp, fontFamily = InterPremium, fontWeight = FontWeight.Medium),
                                                    color = FDColors.TextPrimary
                                                )
                                                Text(
                                                    text = if (esAbierta) "En curso" else horaCierra,
                                                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontFamily = InterPremium),
                                                    color = if (esAbierta) FDColors.Success else FDColors.TextTertiary
                                                )
                                            }

                                            // 3. Cobrado
                                            Text(
                                                text = "$simboloMoneda %.2f".format(Locale.US, sesion.totalVentas),
                                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium, textAlign = TextAlign.End),
                                                color = FDColors.TextPrimary,
                                                modifier = Modifier.weight(1.2f)
                                            )

                                            // 4. Egresos / Retiros
                                            Text(
                                                text = if (sesion.retiros > 0.0) "−$simboloMoneda %.2f".format(Locale.US, sesion.retiros) else "S/ 0.00",
                                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = if (sesion.retiros > 0.0) FontWeight.Bold else FontWeight.Normal, fontFamily = InterPremium, textAlign = TextAlign.End),
                                                color = if (sesion.retiros > 0.0) FDColors.Error else FDColors.TextTertiary,
                                                modifier = Modifier.weight(1.2f)
                                            )

                                            // 5. Arqueo Contado
                                            Text(
                                                text = if (esAbierta) "En curso" else "$simboloMoneda %.2f".format(Locale.US, sesion.efectivoContado),
                                                style = FDType.Label.copy(fontSize = 11.sp, fontFamily = InterPremium, textAlign = TextAlign.End),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.weight(1.2f)
                                            )

                                            // 6. Diferencia
                                            val dif = sesion.diferenciaEfectivo
                                            val textDif = when {
                                                esAbierta -> "S/ 0.00"
                                                dif < -0.01 -> "-S/ %.2f".format(Locale.US, abs(dif))
                                                dif > 0.01 -> "+S/ %.2f".format(Locale.US, dif)
                                                else -> "S/ 0.00"
                                            }
                                            Text(
                                                text = textDif,
                                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium, textAlign = TextAlign.End),
                                                color = if (!esAbierta && dif < -0.01) FDColors.Error else FDColors.TextPrimary,
                                                modifier = Modifier.weight(1.2f)
                                            )

                                            // 7. Estado Badge
                                            Box(
                                                modifier = Modifier.weight(1.0f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = if (esAbierta) FDColors.SuccessSubtle else FDColors.InputBackground,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (esAbierta) "ABIERTA" else "CERRADA",
                                                        style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = if (esAbierta) FDColors.Success else FDColors.TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }

                    // ── 3. BARRA DE PAGINACIÓN ERGONÓMICA AL PIE DE LA TABLA ──
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))
                    Surface(
                        color = FDColors.SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Izquierda: Resumen y selector de items por página
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val startIdx = if (sesionesFiltradas.isEmpty()) 0 else (paginaActual - 1) * itemsPorPagina + 1
                                val endIdx = minOf(paginaActual * itemsPorPagina, sesionesFiltradas.size)

                                Text(
                                    text = "Mostrando $startIdx - $endIdx de ${sesionesFiltradas.size} turnos",
                                    style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    color = FDColors.TextSecondary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Ver:",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp),
                                        color = FDColors.TextTertiary
                                    )
                                    listOf(10, 15, 25, 50).forEach { cant ->
                                        val esSelCant = itemsPorPagina == cant
                                        Surface(
                                            color = if (esSelCant) FDColors.Primary else FDColors.InputBackground,
                                            shape = RoundedCornerShape(4.dp),
                                            border = if (!esSelCant) BorderStroke(0.5.dp, FDColors.Border) else null,
                                            modifier = Modifier.clickable {
                                                itemsPorPagina = cant
                                                paginaActual = 1
                                            }
                                        ) {
                                            Text(
                                                text = "$cant",
                                                style = FDType.Caption.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (esSelCant) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (esSelCant) Color.White else FDColors.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Derecha: Controles de navegación de página
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Primera página
                                if (totalPaginas > 2) {
                                    OutlinedButton(
                                        onClick = { paginaActual = 1 },
                                        enabled = paginaActual > 1,
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("1...", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                    }
                                }

                                // Anterior
                                OutlinedButton(
                                    onClick = { if (paginaActual > 1) paginaActual-- },
                                    enabled = paginaActual > 1,
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(16.dp))
                                    Text("Anterior", style = FDType.Caption.copy(fontSize = 10.sp))
                                }

                                // Indicador de página
                                Surface(
                                    color = FDColors.InputBackground,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, FDColors.Border)
                                ) {
                                    Text(
                                        text = "$paginaActual / $totalPaginas",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                        color = FDColors.TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Siguiente
                                OutlinedButton(
                                    onClick = { if (paginaActual < totalPaginas) paginaActual++ },
                                    enabled = paginaActual < totalPaginas,
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Siguiente", style = FDType.Caption.copy(fontSize = 10.sp))
                                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                                }

                                // Última página
                                if (totalPaginas > 2) {
                                    OutlinedButton(
                                        onClick = { paginaActual = totalPaginas },
                                        enabled = paginaActual < totalPaginas,
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("...$totalPaginas", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── PANEL DERECHO (40% ANCHO): AUDITORÍA Y DESGLOSE COMPLETO DEL TURNO ──
            PanelDetalleTurnoCaja(
                sesion = sesionActivaDetalle,
                movimientos = movimientos,
                simboloMoneda = simboloMoneda,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )
        }
    }
}
