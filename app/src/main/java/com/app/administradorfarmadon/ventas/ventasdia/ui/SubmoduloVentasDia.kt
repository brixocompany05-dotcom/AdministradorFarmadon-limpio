package com.app.administradorfarmadon.ventas.ventasdia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon

@Composable
fun SubmoduloVentasDia(simboloMoneda: String) {
    val s = recordarMedidaAdaptativa()
    var mostrarDetalle by remember { mutableStateOf(false) }
    
    // Simulación de estados honestos
    val cargando = false
    val error: String? = null
    val ventasVacias = true

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(s.padModule)) {
            POSHeader(
                titulo = "Historial de Ventas",
                cajaNombre = "Caja Principal - Sucursal Centro",
                usuarioNombre = "Administrador Sistema"
            )

            // Indicadores Financieros Rápidos
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = s.gapMedium),
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                POSSummaryCard(titulo = "Ventas Totales", valor = "0.00", simbolo = simboloMoneda, modifier = Modifier.weight(1f))
                POSSummaryCard(titulo = "Tickets Emitidos", valor = "0", modifier = Modifier.weight(1f))
                POSSummaryCard(titulo = "Devoluciones", valor = "0.00", simbolo = simboloMoneda, tipo = TipoEstadoFarmadon.PELIGRO, modifier = Modifier.weight(1f))
                POSSummaryCard(titulo = "Efectivo Neto", valor = "0.00", simbolo = simboloMoneda, tipo = TipoEstadoFarmadon.EXITO, modifier = Modifier.weight(1f))
            }

            // Filtros y Tabla Enterprise
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra de Filtros
                    FiltrosBar()

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                    // Tabla con Estados Visuales
                    POSStatusOverlay(
                        loading = cargando,
                        error = error,
                        empty = ventasVacias,
                        emptyIcon = Icons.Default.History,
                        emptyTitle = "No se encontraron ventas",
                        emptySub = "Ajuste los filtros o realice una nueva venta para ver resultados aquí."
                    ) {
                        POSTable(
                            headers = listOf("Hora", "N° Venta", "Cliente", "Cajero", "Método", "Total", "Estado")
                        ) {
                            // En la fase 2 se mapearán las ventas reales:
                            // items(ventas) { venta -> POSTableRow(...) }
                        }
                    }
                }
            }
        }

        // Side Sheet Detalle Trazable
        DetalleVentaSideSheet(
            visible = mostrarDetalle,
            onClose = { mostrarDetalle = false },
            simbolo = simboloMoneda
        )
    }
}

@Composable
private fun FiltrosBar() {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Buscar por ticket, DNI o nombre...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.width(300.dp).height(48.dp),
            shape = FDShapes.Small,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FDColors.InputBackground,
                unfocusedContainerColor = FDColors.InputBackground
            )
        )
        
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text("HOY", style = FDType.Label.copy(fontSize = 10.sp)) },
            leadingIcon = { Icon(Icons.Default.Today, null, modifier = Modifier.size(14.dp)) },
            shape = CircleShape
        )

        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("MÉTODO: TODOS", style = FDType.Label.copy(fontSize = 10.sp)) },
            shape = CircleShape
        )
        
        TextButton(onClick = { /* Limpiar */ }) {
            Text("LIMPIAR FILTROS", style = FDType.Label.copy(fontSize = 9.sp, color = FDColors.Error))
        }

        Spacer(Modifier.weight(1f))
        
        IconButton(onClick = { /* Exportar */ }) {
            Icon(Icons.Default.FileDownload, null, tint = FDColors.Primary)
        }
    }
}

@Composable
private fun DetalleVentaSideSheet(visible: Boolean, onClose: () -> Unit, simbolo: String) {
    POSSideSheet(
        visible = visible,
        onClose = onClose,
        titulo = "Detalle de Venta",
        subtitulo = "Operación #0001-000042",
        acciones = {
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(48.dp), shape = FDShapes.Small) {
                Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("REIMPRIMIR")
            }
            Button(onClick = {}, modifier = Modifier.weight(1f).height(48.dp), shape = FDShapes.Small, colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error)) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("DEVOLVER")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Sección: Info General
            DetalleSection(titulo = "INFORMACIÓN GENERAL") {
                InfoRow("Fecha y Hora", "31/08/2026 14:22")
                InfoRow("Cajero", "Juan Pérez")
                InfoRow("Caja", "Caja 01")
                InfoRow("Estado", "COMPLETADA", tipo = TipoEstadoFarmadon.EXITO)
            }

            // Sección: Cliente
            DetalleSection(titulo = "CLIENTE") {
                InfoRow("Nombre", "Consumidor Final")
                InfoRow("Documento", "---")
            }

            // Sección: Productos
            DetalleSection(titulo = "PRODUCTOS (0)") {
                Text("No hay productos registrados en este ticket.", style = FDType.Caption, color = FDColors.TextTertiary)
            }

            // Sección: Pagos
            DetalleSection(titulo = "PAGOS") {
                InfoRow("Método", "Efectivo")
                InfoRow("Total Pagado", "$simbolo 0.00")
            }

            // Sección: Trazabilidad
            DetalleSection(titulo = "TRAZABILIDAD") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                    Text("Esta venta no tiene devoluciones ni anulaciones asociadas.", style = FDType.Caption, color = FDColors.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun DetalleSection(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = titulo, style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
        Spacer(Modifier.height(12.dp))
        Surface(color = FDColors.InputBackground.copy(alpha = 0.5f), shape = FDShapes.Small, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, valor: String, tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.NEUTRO) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = FDType.BodySmall, color = FDColors.TextSecondary)
        if (tipo != TipoEstadoFarmadon.NEUTRO) {
            POSBadge(texto = valor, tipo = tipo)
        } else {
            Text(valor, style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
        }
    }
}
