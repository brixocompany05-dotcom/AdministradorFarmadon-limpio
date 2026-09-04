package com.app.administradorfarmadon.clientes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.clientes.logica.ClientesUiState
import com.app.administradorfarmadon.clientes.logica.ClientesViewModel
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA PRINCIPAL DEL DIRECTORIO DE CLIENTES (R1/R3/R8/R12).
 * Arquitectura Enterprise: Lista de Clientes (55%) + Ficha & Historial de Compras (45%).
 */
@Composable
fun ClientesScreen(
    onVolver: () -> Unit = {},
    viewModel: ClientesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── CABECERA PRINCIPAL (CONSULTA INFORMATIVA) ──
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Medium,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text("Directorio de Clientes", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))
                        Text("Consulta de clientes e historial de compras registrado desde Punto de Venta (POS)", style = FDType.Caption, color = FDColors.TextSecondary)
                    }
                }

                FDSearchField(
                    busqueda = uiState.filtroTexto,
                    onBusquedaChange = { viewModel.setFiltroTexto(it) },
                    placeholder = "Buscar por nombre, DNI, RUC o teléfono...",
                    modifier = Modifier.width(380.dp)
                )
            }
        }

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

        // ── SPLIT VIEW: DIRECTORIO (55%) | FICHA & HISTORIAL (45%) ──
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            POSSurfacePanel(
                titulo = if (uiState.filtroTexto.isBlank()) {
                    val total = if (uiState.totalClientesServidor > 0) uiState.totalClientesServidor else uiState.clientes.size
                    "Clientes Registrados ($total)"
                } else {
                    "Clientes Encontrados (${uiState.clientesFiltrados.size})"
                },
                modifier = Modifier.weight(0.55f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.cargando && uiState.clientes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                        }
                    } else if (uiState.clientesFiltrados.isEmpty()) {
                        POSEmptyState(
                            icono = Icons.Default.PeopleOutline,
                            titulo = if (uiState.clientes.isEmpty()) "Directorio Vacío" else "Sin coincidencias",
                            subtitulo = if (uiState.clientes.isEmpty()) "Los clientes se registran automáticamente al identificarlos durante una venta en el Punto de Venta (POS)." else "Prueba con otro término de búsqueda."
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Encabezados
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("DOCUMENTO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                    Text("NOMBRE / RAZÓN SOCIAL", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.8f))
                                    Text("TELÉFONO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(uiState.clientesFiltrados) { cliente ->
                                    val isSel = uiState.clienteSeleccionado?.id == cliente.id

                                    Surface(
                                        onClick = { viewModel.seleccionarCliente(cliente) },
                                        color = if (isSel) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
                                        shape = FDShapes.Small,
                                        border = BorderStroke(1.dp, if (isSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                POSBadge(
                                                    texto = cliente.tipoDocumento,
                                                    tipo = if (cliente.esDni) TipoEstadoFarmadon.EXITO else TipoEstadoFarmadon.ALERTA
                                                )
                                                Text(
                                                    cliente.numeroDocumento,
                                                    style = FDType.Numeric.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                    color = FDColors.TextPrimary
                                                )
                                            }

                                            Text(
                                                cliente.nombre,
                                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1.8f)
                                            )

                                            Text(
                                                cliente.telefono.ifBlank { "---" },
                                                style = FDType.Caption.copy(fontSize = 11.5.sp),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ───────────────────────────── PANEL DERECHO: FICHA & HISTORIAL (45%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Ficha del Cliente e Historial",
                modifier = Modifier.weight(0.45f)
            ) {
                val cliente = uiState.clienteSeleccionado
                if (cliente == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ContactPage,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = FDColors.TextTertiary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Selecciona un Cliente", style = FDType.Heading3.copy(fontWeight = FontWeight.Black), color = FDColors.TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("Elige un cliente de la lista para ver su ficha completa, datos de contacto e historial de compras.", style = FDType.Body, color = FDColors.TextTertiary, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tarjeta Principal de Ficha
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cliente.nombre, style = FDType.Heading3.copy(fontWeight = FontWeight.Black, fontSize = 16.sp), color = FDColors.TextPrimary)
                                        Text("${cliente.tipoDocumento}: ${cliente.numeroDocumento}", style = FDType.Caption.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary)
                                    }
                                    Surface(
                                        color = FDColors.PrimarySubtle,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${cliente.tipoDocumento} REGISTRADO",
                                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (cliente.telefono.isNotBlank() || cliente.direccion.isNotBlank()) {
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                                    if (cliente.telefono.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                                            Text(cliente.telefono, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                                        }
                                    }
                                    if (cliente.direccion.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                                            Text(cliente.direccion, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Métricas del Cliente (Verdad Financiera)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = FDColors.Surface,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TOTAL COMPRADO", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                    Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.totalComprasCliente)}", style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, color = FDColors.Primary))
                                }
                            }

                            Surface(
                                color = FDColors.Surface,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("COMPROBANTES", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                    Text("${uiState.totalOperacionesCliente} compras", style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black))
                                }
                            }

                            Surface(
                                color = FDColors.Surface,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TICKET PROMEDIO", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                    Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.ticketPromedioCliente)}", style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, color = FDColors.TextPrimary))
                                }
                            }
                        }

                        // Historial de Compras en Vivo
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("HISTORIAL DE COMPRAS EN ESTA SUCURSAL", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)

                            if (uiState.cargandoHistorial) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                                }
                            } else if (uiState.historialVentas.isEmpty()) {
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.3f),
                                    shape = FDShapes.Small,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                        Text("Este cliente aún no registra compras en esta sucursal.", style = FDType.Caption, color = FDColors.TextTertiary, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                val fmtFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.historialVentas) { venta ->
                                        val fecha = if (venta.fechaHoraMs > 0) fmtFecha.format(Date(venta.fechaHoraMs)) else "--"
                                        val (tipoBadge, textoBadge) = when (venta.estado) {
                                            Venta.ESTADO_COMPLETADA -> TipoEstadoFarmadon.EXITO to "COMPLETADA"
                                            Venta.ESTADO_DEVOLUCION_PARCIAL -> TipoEstadoFarmadon.ALERTA to "DEV. PARCIAL"
                                            Venta.ESTADO_DEVOLUCION_TOTAL -> TipoEstadoFarmadon.PELIGRO to "DEV. TOTAL"
                                            Venta.ESTADO_ANULADA -> TipoEstadoFarmadon.PELIGRO to "ANULADA"
                                            else -> TipoEstadoFarmadon.NEUTRO to venta.estado
                                        }

                                        Surface(
                                            color = FDColors.Surface,
                                            shape = FDShapes.Small,
                                            border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(venta.numeroCompleto, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = FDColors.TextPrimary)
                                                    Text(fecha, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", venta.total)}",
                                                        style = FDType.Numeric.copy(
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (venta.estado == Venta.ESTADO_ANULADA) FDColors.TextTertiary else FDColors.TextPrimary
                                                        )
                                                    )
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
        }
    }
}

