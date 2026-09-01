package com.app.administradorfarmadon.ventas.devoluciones.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon

@Composable
fun SubmoduloDevoluciones(simboloMoneda: String) {
    val s = recordarMedidaAdaptativa()
    var busquedaTicket by remember { mutableStateOf("") }
    var viendoHistorial by remember { mutableStateOf(false) }
    
    // ESTADOS DEL FLUJO
    var ticketSeleccionado by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(s.padModule)) {
        POSHeader(
            titulo = "Devoluciones y Anulaciones",
            cajaNombre = "Módulo de Control",
            usuarioNombre = "Administrador"
        )

        // Switch de Vista
        Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TabOption("GESTIONAR DEVOLUCIÓN", !viendoHistorial) { viendoHistorial = false }
            TabOption("HISTORIAL DE DEVOLUCIONES", viendoHistorial) { viendoHistorial = true }
        }

        if (viendoHistorial) {
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            placeholder = { Text("Buscar devolución por número o fecha...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.width(350.dp).height(48.dp),
                            shape = FDShapes.Small,
                            singleLine = true
                        )
                    }
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                    POSStatusOverlay(empty = true, emptyIcon = Icons.AutoMirrored.Filled.AssignmentReturn, emptyTitle = "No hay devoluciones registradas", emptySub = "Todas las notas de crédito y devoluciones de productos aparecerán aquí.") {
                        // Table Historial
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // PANEL IZQUIERDO: BÚSQUEDA Y SELECCIÓN (60%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Buscador de Tickets
                        OutlinedTextField(
                            value = busquedaTicket,
                            onValueChange = { busquedaTicket = it },
                            placeholder = { Text("Escanee ticket o ingrese N° de Venta...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Receipt, null, tint = FDColors.Primary) },
                            modifier = Modifier.fillMaxWidth().padding(20.dp).height(60.dp),
                            shape = FDShapes.Small,
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = FDColors.InputBackground)
                        )

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        if (ticketSeleccionado == null) {
                            POSEmptyState(
                                icono = Icons.Default.FindInPage,
                                titulo = "Búsqueda de Venta",
                                subtitulo = "Ingrese el número de comprobante para listar los productos vendidos y gestionar la devolución."
                            )
                        } else {
                            // UI de Selección de Productos para devolver
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { Text("PRODUCTOS DE LA VENTA #$ticketSeleccionado", style = FDType.Label.copy(fontWeight = FontWeight.Black)) }
                                // items(productosVenta) { ... Row con checkbox y selector de cantidad ... }
                            }
                        }
                    }
                }

                // PANEL DERECHO: LIQUIDACIÓN (40%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text("CONFIGURACIÓN DE DEVOLUCIÓN", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                            
                            // Motivo
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MOTIVO DE DEVOLUCIÓN", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                                Surface(
                                    onClick = {},
                                    color = FDColors.InputBackground,
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Seleccione un motivo...", style = FDType.Body, color = FDColors.TextSecondary)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }

                            // Reembolso
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("RESUMEN ECONÓMICO", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                Surface(color = FDColors.InputBackground.copy(alpha = 0.5f), shape = FDShapes.Small, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ReembolsoRow("Monto Original", "$simboloMoneda 0.00")
                                        ReembolsoRow("Monto a Devolver", "$simboloMoneda 0.00", resaltado = true)
                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                                        Text(text = "El reembolso se realizará mediante el mismo método de pago original.", style = FDType.Caption, color = FDColors.TextTertiary)
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.background(FDColors.SurfaceElevated).padding(24.dp)) {
                            FDBotonPrimario(
                                texto = "GENERAR NOTA DE CRÉDITO",
                                onClick = { mostrarConfirmacion = true },
                                icono = Icons.Default.CheckCircle,
                                habilitado = ticketSeleccionado != null,
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Overlay de Confirmación
    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            confirmButton = {
                Button(onClick = { mostrarConfirmacion = false; ticketSeleccionado = null }, colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)) {
                    Text("SÍ, CONFIRMAR DEVOLUCIÓN")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("CANCELAR") }
            },
            title = { Text("Confirmar Devolución", style = FDType.Heading2) },
            text = { Text("¿Está seguro que desea procesar esta devolución? Se generará una Nota de Crédito y los productos volverán al stock físico.") },
            shape = FDShapes.Large,
            containerColor = FDColors.Surface
        )
    }
}

@Composable
private fun TabOption(texto: String, activo: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = texto,
            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = if (activo) FontWeight.Black else FontWeight.Medium),
            color = if (activo) FDColors.Primary else FDColors.TextTertiary
        )
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.width(40.dp).height(3.dp).background(if (activo) FDColors.Primary else Color.Transparent, CircleShape))
    }
}

@Composable
private fun ReembolsoRow(label: String, valor: String, resaltado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (resaltado) FDType.Body.copy(fontWeight = FontWeight.Bold) else FDType.BodySmall)
        Text(valor, style = if (resaltado) FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Error) else FDType.Numeric.copy(fontSize = 14.sp))
    }
}
