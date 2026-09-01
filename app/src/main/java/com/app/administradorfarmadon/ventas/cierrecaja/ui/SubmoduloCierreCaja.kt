package com.app.administradorfarmadon.ventas.cierrecaja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun SubmoduloCierreCaja(simboloMoneda: String) {
    val s = recordarMedidaAdaptativa()
    
    // ESTADOS DEL CICLO DE TURNO
    var cajaAbierta by remember { mutableStateOf(true) }
    var mostrandoArqueo by remember { mutableStateOf(false) }
    var mostrarNuevoMovimiento by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(s.padModule)) {
        POSHeader(
            titulo = "Gestión de Caja",
            cajaNombre = "Caja Principal #01",
            usuarioNombre = "Supervisor Farmadon",
            cajaCerrada = !cajaAbierta
        )

        if (!cajaAbierta) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                POSEmptyState(
                    icono = Icons.Default.Lock,
                    titulo = "Caja Cerrada",
                    subtitulo = "Debe realizar la apertura de caja para comenzar a registrar ventas y movimientos."
                )
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.BottomCenter) {
                    FDBotonPrimario(texto = "REALIZAR APERTURA DE CAJA", onClick = { cajaAbierta = true }, icono = Icons.Default.VpnKey)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // PANEL IZQUIERDO: MOVIMIENTOS Y ARQUEO (60%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tabs de Control
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TabButton("MOVIMIENTOS DE CAJA", !mostrandoArqueo) { mostrandoArqueo = false }
                            TabButton("ARQUEO FÍSICO", mostrandoArqueo) { mostrandoArqueo = true }
                        }
                        
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        if (mostrandoArqueo) {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                item {
                                    Text("CONTEO DE EFECTIVO POR DENOMINACIÓN", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                    Spacer(Modifier.height(16.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("200", "100", "50", "20", "10", "5", "2", "1", "0.50", "0.20", "0.10").forEach { denom ->
                                            POSDenominationCounter(denominacion = denom, cantidad = 0, onCantidadChange = {}, simbolo = simboloMoneda)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("INGRESOS Y EGRESOS DEL TURNO", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                    TextButton(onClick = { mostrarNuevoMovimiento = true }) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("NUEVO MOVIMIENTO", style = FDType.Label.copy(fontSize = 10.sp))
                                    }
                                }
                                POSStatusOverlay(empty = true, emptyIcon = Icons.AutoMirrored.Filled.CompareArrows, emptyTitle = "Sin movimientos manuales", emptySub = "Use el botón superior para registrar ingresos o retiros de efectivo (Caja chica, gastos, etc.)") {
                                    // items(movimientos)
                                }
                            }
                        }
                    }
                }

                // PANEL DERECHO: RESUMEN Y CIERRE (40%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text("RESUMEN DE TURNO ACTUAL", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                            
                            Surface(color = FDColors.InputBackground.copy(alpha = 0.5f), shape = FDShapes.Small, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ResumenArqueoRow("Fondo Inicial", "$simboloMoneda 0.00")
                                    ResumenArqueoRow("Ventas en Efectivo", "$simboloMoneda 0.00", TipoEstadoFarmadon.EXITO)
                                    ResumenArqueoRow("Gastos / Retiros", "- $simboloMoneda 0.00", TipoEstadoFarmadon.PELIGRO)
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                                    ResumenArqueoRow("ESPERADO EN CAJA", "$simboloMoneda 0.00", resaltar = true)
                                }
                            }

                            if (mostrandoArqueo) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("RESULTADO DEL ARQUEO", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                    ResumenArqueoRow("Total Contado", "$simboloMoneda 0.00")
                                    ResumenArqueoRow("Diferencia", "$simboloMoneda 0.00", TipoEstadoFarmadon.ALERTA, resaltar = true)
                                }
                            }
                        }

                        Column(modifier = Modifier.background(FDColors.SurfaceElevated).padding(24.dp)) {
                            FDBotonPrimario(
                                texto = "CERRAR TURNO Y BLOQUEAR CAJA",
                                onClick = { cajaAbierta = false },
                                icono = Icons.Default.Lock,
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Side Sheet para Nuevo Movimiento
    POSSideSheet(
        visible = mostrarNuevoMovimiento,
        onClose = { mostrarNuevoMovimiento = false },
        titulo = "Nuevo Movimiento de Caja",
        subtitulo = "Registre un ingreso o retiro de efectivo manual",
        acciones = {
            FDBotonPrimario(texto = "GUARDAR MOVIMIENTO", onClick = { mostrarNuevoMovimiento = false }, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TIPO DE OPERACIÓN", style = FDType.Label.copy(fontSize = 10.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("INGRESO") }, shape = CircleShape)
                    FilterChip(selected = false, onClick = {}, label = { Text("EGRESO / GASTO") }, shape = CircleShape)
                }
            }
            
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Monto") }, prefix = { Text("$simboloMoneda ") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Motivo / Concepto") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
    }
}

@Composable
private fun TabButton(texto: String, activo: Boolean, onClick: () -> Unit) {
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
private fun ResumenArqueoRow(label: String, valor: String, tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.NEUTRO, resaltar: Boolean = false) {
    val color = when (tipo) {
        TipoEstadoFarmadon.EXITO -> FDColors.Success
        TipoEstadoFarmadon.PELIGRO -> FDColors.Error
        TipoEstadoFarmadon.ALERTA -> FDColors.Warning
        TipoEstadoFarmadon.NEUTRO -> if (resaltar) FDColors.Primary else FDColors.TextPrimary
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (resaltar) FDType.Body.copy(fontWeight = FontWeight.Bold) else FDType.BodySmall, color = if (resaltar) FDColors.TextPrimary else FDColors.TextSecondary)
        Text(valor, style = if (resaltar) FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 20.sp) else FDType.Numeric.copy(fontSize = 14.sp), color = color)
    }
}
