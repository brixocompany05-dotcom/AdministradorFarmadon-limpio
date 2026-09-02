package com.app.administradorfarmadon.ventas.ventasdia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.ventas.ventasdia.logica.VentasDiaViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA DE VENTAS DEL DÍA (R8 Verdad Vigente).
 * Muestra el historial en tiempo real de las operaciones realizadas en el día,
 * métricas en vivo, desglose de métodos reales, re-impresión de tickets y enlace a devoluciones.
 */
@Composable
fun SubmoduloVentasDia(
    simboloMoneda: String = "S/",
    onDevolver: (Venta) -> Unit = {},
    viewModel: VentasDiaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Reconectar listener vivo cada vez que se entra al submódulo
    LaunchedEffect(Unit) {
        viewModel.reconectar()
    }

    // Auto-limpieza de notificaciones
    LaunchedEffect(uiState.mensajeExito, uiState.error) {
        if (uiState.mensajeExito != null || uiState.error != null) {
            delay(4000)
            viewModel.limpiarMensajes()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Notificaciones en vivo
        if (uiState.mensajeExito != null) {
            POSNotificationBar(
                mensaje = uiState.mensajeExito ?: "",
                tipo = TipoEstadoFarmadon.EXITO,
                icono = Icons.Default.CheckCircle
            )
        }
        if (uiState.error != null) {
            POSNotificationBar(
                mensaje = uiState.error ?: "",
                tipo = TipoEstadoFarmadon.PELIGRO,
                icono = Icons.Default.ErrorOutline
            )
        }

        // 1. FILA DE MÉTRICAS PRINCIPALES EN VIVO (6 ITEMS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            POSMetricCard(
                titulo = "Ventas Totales (Neto)",
                valor = String.format(Locale.US, "%.2f", uiState.totalVentasNeto),
                simbolo = simboloMoneda,
                icono = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            POSMetricCard(
                titulo = "Transacciones",
                valor = "${uiState.totalOperaciones}",
                tipo = TipoEstadoFarmadon.EXITO,
                icono = Icons.Default.Receipt,
                modifier = Modifier.weight(0.85f)
            )
            POSMetricCard(
                titulo = "Ticket Promedio",
                valor = String.format(Locale.US, "%.2f", uiState.ticketPromedio),
                simbolo = simboloMoneda,
                icono = Icons.Default.Analytics,
                modifier = Modifier.weight(0.95f)
            )
            POSMetricCard(
                titulo = "Descuentos",
                valor = String.format(Locale.US, "%.2f", uiState.totalDescuentos),
                simbolo = simboloMoneda,
                tipo = if (uiState.totalDescuentos > 0.0) TipoEstadoFarmadon.ALERTA else TipoEstadoFarmadon.NEUTRO,
                icono = Icons.Default.Sell,
                modifier = Modifier.weight(0.9f)
            )
            POSMetricCard(
                titulo = "Devoluciones",
                valor = String.format(Locale.US, "%.2f", uiState.totalDevoluciones),
                simbolo = simboloMoneda,
                tipo = if (uiState.totalDevoluciones > 0.0) TipoEstadoFarmadon.PELIGRO else TipoEstadoFarmadon.NEUTRO,
                icono = Icons.Default.History,
                modifier = Modifier.weight(0.95f)
            )
            POSMetricCard(
                titulo = "Con Devolución",
                valor = "${uiState.totalConDevolucion}",
                tipo = if (uiState.totalConDevolucion > 0) TipoEstadoFarmadon.ALERTA else TipoEstadoFarmadon.NEUTRO,
                icono = Icons.AutoMirrored.Filled.AssignmentReturn,
                modifier = Modifier.weight(0.85f)
            )
        }

        // 2. FILA DE RECAUDACIÓN POR MÉTODOS DE PAGO REALES (Solo métodos que existieron en el día)
        if (uiState.ventasPorMetodo.isNotEmpty()) {
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.35f),
                shape = FDShapes.Small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "RECAUDADO POR MÉTODO:",
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                        color = FDColors.TextTertiary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.ventasPorMetodo.entries.toList()) { (metodo, monto) ->
                            val nombre = when (metodo) {
                                "EFECTIVO" -> "Efectivo"
                                "YAPE" -> "Yape"
                                "PLIN" -> "Plin"
                                "TARJETA_POS" -> "Tarjeta"
                                "TRANSFERENCIA" -> "Transferencia"
                                "CHEQUE" -> "Cheque"
                                else -> metodo
                            }
                            Surface(
                                color = FDColors.Surface,
                                shape = CircleShape,
                                border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(nombre, style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                                    Text(
                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", monto)}",
                                        style = FDType.Numeric.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, color = FDColors.Primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. SPLIT LAYOUT: HISTORIAL DE VENTAS (65%) | DETALLE DE OPERACIÓN (35%)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Panel Historial (Izquierda - 65%)
            POSSurfacePanel(
                titulo = "Historial de Ventas del Día",
                modifier = Modifier.weight(0.65f)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Barra de Búsqueda y Filtros Rápidos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FDSearchField(
                            busqueda = uiState.filtroTexto,
                            onBusquedaChange = { viewModel.setFiltroTexto(it) },
                            placeholder = "Buscar por comprobante, cliente o cajero...",
                            modifier = Modifier.weight(1f)
                        )

                        // Selector de Estado
                        val estados = listOf("TODOS" to "Todos", "COMPLETADA" to "Completas", "DEVOLUCIONES" to "Devueltas")
                        estados.forEach { (clave, label) ->
                            val isSel = uiState.filtroEstado == clave
                            Surface(
                                onClick = { viewModel.setFiltroEstado(clave) },
                                color = if (isSel) FDColors.Primary else FDColors.InputBackground,
                                shape = FDShapes.Small,
                                modifier = Modifier.height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                    Text(
                                        label,
                                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold),
                                        color = if (isSel) FDColors.PrimaryText else FDColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                    // Contenedor de Tabla Viva
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (uiState.cargando && uiState.ventas.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.5.dp)
                            }
                        } else if (uiState.ventasFiltradas.isEmpty()) {
                            POSEmptyState(
                                icono = Icons.AutoMirrored.Filled.ReceiptLong,
                                titulo = if (uiState.ventas.isEmpty()) "Sin ventas registradas hoy" else "Sin resultados para el filtro",
                                subtitulo = if (uiState.ventas.isEmpty()) "Las ventas efectuadas hoy aparecerán aquí en vivo." else "Intenta ajustando el texto de búsqueda o el filtro de estado."
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Encabezados de Tabla
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("HORA", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.7f))
                                        Text("COMPROBANTE", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.3f))
                                        Text("CLIENTE", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.6f))
                                        Text("CAJERO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                        Text("TOTAL", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text("ESTADO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                    }
                                }

                                val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(uiState.ventasFiltradas) { venta ->
                                        val isSel = uiState.ventaSeleccionada?.id == venta.id
                                        val hora = if (venta.fechaHoraMs > 0) fmtHora.format(Date(venta.fechaHoraMs)) else "--:--"

                                        val (tipoBadge, textoBadge) = when (venta.estado) {
                                            Venta.ESTADO_COMPLETADA -> TipoEstadoFarmadon.EXITO to "COMPLETADA"
                                            Venta.ESTADO_DEVOLUCION_PARCIAL -> TipoEstadoFarmadon.ALERTA to "DEV. PARCIAL"
                                            Venta.ESTADO_DEVOLUCION_TOTAL -> TipoEstadoFarmadon.PELIGRO to "DEV. TOTAL"
                                            else -> TipoEstadoFarmadon.NEUTRO to venta.estado
                                        }

                                        Surface(
                                            onClick = { viewModel.seleccionarVenta(venta) },
                                            color = if (isSel) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
                                            shape = FDShapes.Small,
                                            border = BorderStroke(1.dp, if (isSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(hora, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.7f))
                                                Text(
                                                    venta.numeroCompleto,
                                                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                    color = FDColors.TextPrimary,
                                                    modifier = Modifier.weight(1.3f)
                                                )
                                                Text(
                                                    venta.cliente.nombre,
                                                    style = FDType.Body.copy(fontSize = 12.sp),
                                                    color = FDColors.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1.6f)
                                                )
                                                Text(
                                                    venta.cajeroNombre.ifBlank { "Mostrador" },
                                                    style = FDType.Caption.copy(fontSize = 11.sp),
                                                    color = FDColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    "$simboloMoneda ${String.format(Locale.US, "%.2f", venta.total)}",
                                                    style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextPrimary,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                                Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                                                    POSBadge(texto = textoBadge, tipo = tipoBadge)
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

            // Panel Detalle de Operación (Derecha - 35%)
            POSSurfacePanel(
                titulo = "Detalle de Operación",
                modifier = Modifier.weight(0.35f)
            ) {
                val venta = uiState.ventaSeleccionada
                if (venta == null) {
                    DetalleVaciaState()
                } else {
                    DetalleVentaContenido(
                        venta = venta,
                        simbolo = simboloMoneda,
                        onImprimir = { viewModel.imprimirTicket(context, venta) },
                        onDevolver = { onDevolver(venta) }
                    )
                }
            }
        }
    }
}

// ───────────────────────────── COMPONENTES AUXILIARES ─────────────────────────────

@Composable
private fun DetalleVaciaState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.TouchApp,
            null,
            modifier = Modifier.size(56.dp),
            tint = FDColors.TextTertiary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Detalle de Venta",
            style = FDType.Heading3.copy(fontWeight = FontWeight.Black),
            color = FDColors.TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Selecciona una venta de la lista para ver el desglose de productos, formas de pago e imprimir el comprobante.",
            style = FDType.Body,
            color = FDColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetalleVentaContenido(
    venta: Venta,
    simbolo: String,
    onImprimir: () -> Unit,
    onDevolver: () -> Unit
) {
    val fmtFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val fechaTexto = if (venta.fechaHoraMs > 0) fmtFecha.format(Date(venta.fechaHoraMs)) else "--"

    val (tipoBadge, textoBadge) = when (venta.estado) {
        Venta.ESTADO_COMPLETADA -> TipoEstadoFarmadon.EXITO to "COMPLETADA"
        Venta.ESTADO_DEVOLUCION_PARCIAL -> TipoEstadoFarmadon.ALERTA to "DEV. PARCIAL"
        Venta.ESTADO_DEVOLUCION_TOTAL -> TipoEstadoFarmadon.PELIGRO to "DEV. TOTAL"
        else -> TipoEstadoFarmadon.NEUTRO to venta.estado
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // 1. Cabecera del Detalle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${venta.tipoComprobante}: ${venta.numeroCompleto}",
                            style = FDType.Heading3.copy(fontWeight = FontWeight.Black, fontSize = 16.sp)
                        )
                        Text(fechaTexto, style = FDType.Caption, color = FDColors.TextTertiary)
                    }
                    POSBadge(texto = textoBadge, tipo = tipoBadge)
                }
            }

            // 2. Información del Cliente
            item {
                Surface(
                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("CLIENTE", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                        Text(venta.cliente.nombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = FDColors.TextPrimary)
                        if (venta.cliente.numeroDocumento.isNotBlank() && venta.cliente.tipoDocumento != "NINGUNO") {
                            Text("${venta.cliente.tipoDocumento}: ${venta.cliente.numeroDocumento}", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
                        }
                    }
                }
            }

            // 3. Ítems Vendidos
            item {
                Text("PRODUCTOS (${venta.items.size})", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
            }

            items(venta.items) { item ->
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Small,
                    border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${item.cantidad}x ${item.nombreProducto}",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = FDColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$simbolo ${String.format(Locale.US, "%.2f", item.subtotal)}",
                                style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 12.sp)
                            )
                        }
                        Text(
                            "Pres: ${item.presentacionNombre} · P.U. $simbolo ${String.format(Locale.US, "%.2f", item.precioUnitario)}",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextTertiary
                        )
                        if (item.cantidadDevuelta > 0) {
                            Text(
                                "Devueltas: ${item.cantidadDevuelta} unidades",
                                style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.Error, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 4. Desglose de Pagos y Liquidación
            item {
                Surface(
                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("$simbolo ${String.format(Locale.US, "%.2f", venta.subtotal)}", style = FDType.Numeric.copy(fontSize = 12.sp))
                        }
                        if (venta.descuento > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Descuento:", style = FDType.Caption, color = FDColors.Success)
                                Text("- $simbolo ${String.format(Locale.US, "%.2f", venta.descuento)}", style = FDType.Numeric.copy(fontSize = 12.sp, color = FDColors.Success))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:", style = FDType.Body.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                            Text("$simbolo ${String.format(Locale.US, "%.2f", venta.total)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 14.sp, color = FDColors.Primary))
                        }

                        if (venta.totalDevuelto > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Devoluciones:", style = FDType.Caption, color = FDColors.Error)
                                Text("- $simbolo ${String.format(Locale.US, "%.2f", venta.totalDevuelto)}", style = FDType.Numeric.copy(fontSize = 12.sp, color = FDColors.Error))
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        Text("PAGOS REALIZADOS", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                        venta.pagos.forEach { p ->
                            val opStr = if (p.numeroOperacion.isNotBlank()) " (Op: ${p.numeroOperacion})" else ""
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${p.nombreMetodo}$opStr", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
                                Text("$simbolo ${String.format(Locale.US, "%.2f", p.monto)}", style = FDType.Numeric.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
                            }
                        }

                        if (venta.vuelto > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vuelto:", style = FDType.Caption, color = FDColors.Success)
                                Text("$simbolo ${String.format(Locale.US, "%.2f", venta.vuelto)}", style = FDType.Numeric.copy(fontSize = 11.5.sp, color = FDColors.Success))
                            }
                        }
                    }
                }
            }
        }

        // 5. Botones de Acción
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onImprimir,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(16.dp), tint = FDColors.Primary)
                    Spacer(Modifier.width(6.dp))
                    Text("IMPRIMIR", style = FDType.Label.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                }

                val permiteDevolver = venta.estado != Venta.ESTADO_DEVOLUCION_TOTAL
                Button(
                    onClick = onDevolver,
                    enabled = permiteDevolver,
                    modifier = Modifier.weight(1.1f).height(44.dp),
                    shape = FDShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.AssignmentReturn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DEVOLVER", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}
