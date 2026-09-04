package com.app.administradorfarmadon.ventas.devoluciones.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.ventas.devoluciones.logica.DevolucionesUiState
import com.app.administradorfarmadon.ventas.devoluciones.logica.DevolucionesViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA DE DEVOLUCIONES Y NOTAS DE CRÉDITO POS (R1/R3/R8/R12).
 * Arquitectura: 3 Paneles de Alto Total (Búsqueda 30% | Selección de Ítems 40% | Reembolso 30%).
 */
@Composable
fun SubmoduloDevoluciones(
    simboloMoneda: String = "S/",
    ventaInicial: Venta? = null,
    onVentaConsumida: () -> Unit = {},
    viewModel: DevolucionesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Consumo inmediato de venta precargada (Cierra nota 2 de Fase 4)
    LaunchedEffect(ventaInicial) {
        if (ventaInicial != null) {
            viewModel.precargarVenta(ventaInicial)
            onVentaConsumida()
        }
    }

    // Auto-limpieza de mensajes temporales
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
        // Notificaciones de advertencia de caja cerrada o errores
        if (!uiState.cajaAbierta) {
            POSNotificationBar(
                mensaje = "LA CAJA SE ENCUENTRA CERRADA. ABRE EL TURNO PARA PODER EMITIR REEMBOLSOS.",
                tipo = TipoEstadoFarmadon.PELIGRO,
                icono = Icons.Default.Lock
            )
        } else if (uiState.error != null) {
            POSNotificationBar(
                mensaje = uiState.error ?: "",
                tipo = TipoEstadoFarmadon.PELIGRO,
                icono = Icons.Default.ErrorOutline
            )
        } else if (uiState.mensajeExito != null) {
            POSNotificationBar(
                mensaje = uiState.mensajeExito ?: "",
                tipo = TipoEstadoFarmadon.EXITO,
                icono = Icons.Default.CheckCircle
            )
        }

        // Layout de 3 Paneles
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ───────────────────────────── PANEL 1: VENTAS Y TICKETS (30%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Buscar Venta / Ticket",
                modifier = Modifier.weight(0.30f)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FDSearchField(
                        busqueda = uiState.busquedaTexto,
                        onBusquedaChange = { viewModel.onBusquedaChange(it) },
                        placeholder = "Buscar por N° (B001-000042 o '42')...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (uiState.buscando) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                            }
                        } else {
                            val listaAMostrar = if (uiState.busquedaTexto.isNotBlank()) {
                                uiState.resultadosBusqueda
                            } else {
                                uiState.ventasDelDia
                            }

                            if (listaAMostrar.isEmpty()) {
                                POSEmptyState(
                                    icono = Icons.AutoMirrored.Filled.ReceiptLong,
                                    titulo = if (uiState.busquedaTexto.isNotBlank()) "Venta no encontrada" else "Sin ventas con stock devolvible",
                                    subtitulo = if (uiState.busquedaTexto.isNotBlank()) "Verifica el número de comprobante o correlativo." else "Las ventas del día aparecerán aquí."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(listaAMostrar) { venta ->
                                        val isSel = uiState.ventaSeleccionada?.id == venta.id
                                        TarjetaVentaResumen(
                                            venta = venta,
                                            seleccionada = isSel,
                                            simbolo = simboloMoneda,
                                            onClick = { viewModel.seleccionarVenta(venta) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ───────────────────────────── PANEL 2: SELECCIÓN DE ÍTEMS (40%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Ítems a Devolver",
                modifier = Modifier.weight(0.40f)
            ) {
                val venta = uiState.ventaSeleccionada
                if (venta == null) {
                    POSEmptyState(
                        icono = Icons.AutoMirrored.Filled.AssignmentReturn,
                        titulo = "Selecciona una Venta",
                        subtitulo = "Busca por número de ticket en el panel izquierdo para seleccionar los productos a devolver."
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Resumen de Cabecera de la Venta
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${venta.tipoComprobante}: ${venta.numeroCompleto}",
                                        style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                        color = FDColors.TextPrimary
                                    )
                                    Text(
                                        "Cliente: ${venta.cliente.nombre}",
                                        style = FDType.Caption.copy(fontSize = 11.sp),
                                        color = FDColors.TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", venta.total)}",
                                        style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 13.sp, color = FDColors.Primary)
                                    )
                                    if (venta.descuento > 0.0) {
                                        Text(
                                            "Dcto: -$simboloMoneda ${String.format(Locale.US, "%.2f", venta.descuento)}",
                                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Success)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                        // Lista de Ítems Vendidos con Checkbox y Stepper
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(venta.items) { item ->
                                val clave = "${item.productoId}_${item.presentacionId}"
                                val cantElegida = uiState.itemsSeleccionados[clave] ?: 0
                                val estaSeleccionado = cantElegida > 0
                                val tieneStockDevolvible = item.cantidadDevolvible > 0

                                FilaItemDevolucion(
                                    item = item,
                                    simbolo = simboloMoneda,
                                    seleccionado = estaSeleccionado,
                                    cantidadElegida = cantElegida,
                                    habilitado = tieneStockDevolvible,
                                    onToggle = { viewModel.toggleItem(item, it) },
                                    onSumar = { viewModel.setCantidadItem(item, cantElegida + 1) },
                                    onRestar = { viewModel.setCantidadItem(item, cantElegida - 1) }
                                )
                            }
                        }
                    }
                }
            }

            // ───────────────────────────── PANEL 3: LIQUIDACIÓN Y REEMBOLSO (30%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Reembolso y Liquidación",
                modifier = Modifier.weight(0.30f)
            ) {
                val venta = uiState.ventaSeleccionada
                if (venta == null || uiState.totalItemsADevolver == 0) {
                    DetalleReembolsoVacioState()
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Desglose Financiero Prorrateado
                            Surface(
                                color = FDColors.InputBackground,
                                shape = FDShapes.Small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ítems a Devolver:", style = FDType.Caption, color = FDColors.TextSecondary)
                                        Text("${uiState.totalItemsADevolver} unidades", style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Monto Bruto:", style = FDType.Caption, color = FDColors.TextSecondary)
                                        Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoBrutoDevolucion)}", style = FDType.Numeric.copy(fontSize = 12.sp))
                                    }
                                    if (uiState.factorDescuento < 0.999) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Ajuste por Descuento:", style = FDType.Caption, color = FDColors.Warning)
                                            val dif = uiState.montoBrutoDevolucion - uiState.montoReembolsoCalculado
                                            Text("- $simboloMoneda ${String.format(Locale.US, "%.2f", dif)}", style = FDType.Numeric.copy(fontSize = 12.sp, color = FDColors.Warning))
                                        }
                                    }
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("TOTAL A REEMBOLSAR:", style = FDType.Body.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                                        Text(
                                            "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoReembolsoCalculado)}",
                                            style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 18.sp, color = FDColors.Error)
                                        )
                                    }
                                }
                            }

                            // Motivo Obligatorio (mínimo 5 caracteres)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FDTextField(
                                    value = uiState.motivo,
                                    onValueChange = { viewModel.setMotivo(it) },
                                    label = "MOTIVO DE DEVOLUCIÓN *",
                                    placeholder = "Ej: Producto dañado / Error en compra (mín. 5 letras)",
                                    singleLine = false,
                                    minLines = 2
                                )
                                if (uiState.motivo.isNotBlank() && uiState.motivo.trim().length < 5) {
                                    Text(
                                        "El motivo debe tener al menos 5 caracteres (${uiState.motivo.trim().length}/5)",
                                        style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Error)
                                    )
                                }
                            }

                            // Método de Reembolso (Catálogo real de sucursal)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "MÉTODO DE REEMBOLSO",
                                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                    color = FDColors.TextTertiary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(uiState.metodosPagoDisponibles) { instancia ->
                                        val sel = instancia.id == uiState.instanciaReembolsoId
                                        val tipoInfo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == instancia.tipoId }
                                        val nombre = tipoInfo?.nombre ?: instancia.tipoId

                                        Surface(
                                            onClick = { viewModel.setMetodoReembolso(instancia.id) },
                                            color = if (sel) FDColors.Primary else FDColors.InputBackground,
                                            shape = FDShapes.Small,
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                                Text(
                                                    nombre,
                                                    style = FDType.Label.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (sel) FontWeight.Black else FontWeight.Bold
                                                    ),
                                                    color = if (sel) FDColors.PrimaryText else FDColors.TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Botón Primario de Generación
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            FDBotonPrimario(
                                texto = if (uiState.cajaAbierta) "CONFIRMAR DEVOLUCIÓN ($simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoReembolsoCalculado)})" else "CAJA CERRADA",
                                onClick = { viewModel.confirmarDevolucion() },
                                icono = Icons.AutoMirrored.Filled.AssignmentReturn,
                                habilitado = uiState.puedeRegistrarDevolucion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de Éxito de Devolución
        uiState.devolucionExitosa?.let { dev ->
            DialogoDevolucionExitosa(
                devolucion = dev,
                simboloMoneda = simboloMoneda,
                onDismiss = { viewModel.cerrarDialogoExito() }
            )
        }
    }
}

// ───────────────────────────── COMPONENTES AUXILIARES ─────────────────────────────

@Composable
private fun TarjetaVentaResumen(
    venta: Venta,
    seleccionada: Boolean,
    simbolo: String,
    onClick: () -> Unit
) {
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val hora = if (venta.fechaHoraMs > 0) fmtHora.format(Date(venta.fechaHoraMs)) else "--"

    Surface(
        onClick = onClick,
        color = if (seleccionada) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
        shape = FDShapes.Small,
        border = BorderStroke(1.dp, if (seleccionada) FDColors.Primary else FDColors.Border.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    venta.numeroCompleto,
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    "$simbolo ${String.format(Locale.US, "%.2f", venta.total)}",
                    style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = FDColors.Primary)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    venta.cliente.nombre,
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(hora, style = FDType.Caption.copy(fontSize = 10.5.sp), color = FDColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun FilaItemDevolucion(
    item: ItemVenta,
    simbolo: String,
    seleccionado: Boolean,
    cantidadElegida: Int,
    habilitado: Boolean,
    onToggle: (Boolean) -> Unit,
    onSumar: () -> Unit,
    onRestar: () -> Unit
) {
    Surface(
        color = if (seleccionado) FDColors.Primary.copy(alpha = 0.05f) else FDColors.Surface,
        shape = FDShapes.Small,
        border = BorderStroke(1.dp, if (seleccionado) FDColors.Primary.copy(alpha = 0.6f) else FDColors.Border.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = seleccionado,
                onCheckedChange = onToggle,
                enabled = habilitado
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.nombreProducto,
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    color = if (habilitado) FDColors.TextPrimary else FDColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Pres: ${item.presentacionNombre} · P.U. $simbolo ${String.format(Locale.US, "%.2f", item.precioUnitario)}",
                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                    color = FDColors.TextTertiary
                )
                Text(
                    if (item.cantidadDevolvible > 0) "${item.cantidadDevolvible} disp. (vendidas: ${item.cantidad})" else "Totalmente devuelto",
                    style = FDType.Caption.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.cantidadDevolvible > 0) FDColors.Success else FDColors.Error
                    )
                )
            }

            if (seleccionado) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onRestar,
                        color = FDColors.InputBackground,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                        }
                    }
                    Text(
                        "$cantidadElegida",
                        style = FDType.Numeric.copy(fontSize = 14.sp, fontWeight = FontWeight.Black),
                        color = FDColors.Primary,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        onClick = { if (cantidadElegida < item.cantidadDevolvible) onSumar() },
                        color = FDColors.Primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = FDColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalleReembolsoVacioState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.AssignmentReturn,
            null,
            modifier = Modifier.size(48.dp),
            tint = FDColors.TextTertiary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Sin Ítems Seleccionados",
            style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = FDColors.TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Marca los productos y cantidades a devolver en el panel central.",
            style = FDType.Body,
            color = FDColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DialogoDevolucionExitosa(
    devolucion: DevolucionVenta,
    simboloMoneda: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = FDColors.Success.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(32.dp), tint = FDColors.Success)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¡Devolución Procesada!", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 20.sp))
                    Text("Nota de Crédito: ${devolucion.numeroCompleto.ifBlank { devolucion.id }}", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reembolsado:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("$simboloMoneda ${String.format(Locale.US, "%.2f", devolucion.montoReembolso)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Error))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Método:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(devolucion.metodoReembolso, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Motivo:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(devolucion.motivo, style = FDType.Caption.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = FDShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    Text("ENTENDIDO", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}
