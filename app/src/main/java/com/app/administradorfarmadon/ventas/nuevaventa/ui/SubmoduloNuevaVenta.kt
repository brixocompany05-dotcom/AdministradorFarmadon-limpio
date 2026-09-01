package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.compartido.ui.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmoduloNuevaVenta(simboloMoneda: String) {
    val s = recordarMedidaAdaptativa()
    var busquedaProducto by remember { mutableStateOf("") }
    
    // ESTADOS DE UI PARA EL FLUJO MAESTRO
    var flujoCobroActivo by remember { mutableStateOf(false) }
    var resultadoVenta by remember { mutableStateOf<TipoEstadoFarmadon?>(null) } // null = en curso, EXITO = éxito, PELIGRO = error
    var mostrarVentasSuspendidas by remember { mutableStateOf(false) }
    
    // Simulación de estados honestos (vacíos inicialmente)
    val carritoItems = remember { mutableStateListOf<NuevaVentaItem>() }
    val estaProcesandoVenta = false

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(s.padModule)) {
            POSHeader(
                titulo = "Nueva Venta",
                cajaNombre = "Caja Principal - Sucursal Centro",
                usuarioNombre = "Juan Pérez",
                estaConectado = true
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // PANEL IZQUIERDO: BUSCADOR Y CARRITO (60%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Buscador Enterprise con Estados de Búsqueda
                        SearchPanel(
                            busqueda = busquedaProducto,
                            onBusquedaChange = { busquedaProducto = it }
                        )

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        POSStatusOverlay(
                            empty = carritoItems.isEmpty(),
                            emptyIcon = Icons.Default.ShoppingCart,
                            emptyTitle = "Carrito Vacío",
                            emptySub = "Busque productos o use el escáner para comenzar la venta."
                        ) {
                            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(carritoItems) { item ->
                                    CartItemRow(item, simboloMoneda)
                                }
                            }
                        }

                        // Footer Panel Izquierdo
                        Row(
                            modifier = Modifier.fillMaxWidth().background(FDColors.InputBackground.copy(alpha = 0.3f)).padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCodeScanner, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Scanner listo", style = FDType.Caption, color = FDColors.TextTertiary)
                                }
                                POSShortcutHint(tecla = "F2", label = "BUSCAR")
                                POSShortcutHint(tecla = "ESC", label = "LIMPIAR")
                            }
                            
                            TextButton(onClick = { mostrarVentasSuspendidas = true }) {
                                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("VENTAS SUSPENDIDAS (0)", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black))
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
                            
                            // Pantalla de Total
                            TotalDisplay(simboloMoneda, carritoItems.sumOf { it.precio * it.cantidad }, carritoItems.size)

                            // Datos del Cliente (UI Preparada)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "CLIENTE / FACTURACIÓN", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                                    shape = FDShapes.Small,
                                    modifier = Modifier.fillMaxWidth().clickable { /* Buscar Cliente */ }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Consumidor Final", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.Search, null, tint = FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Resumen de Importes
                            ResumenImportes(simboloMoneda, carritoItems.sumOf { it.precio * it.cantidad })
                        }

                        Column(modifier = Modifier.background(FDColors.SurfaceElevated).padding(24.dp)) {
                            FDBotonPrimario(
                                texto = "COBRAR AHORA (F1)",
                                onClick = { flujoCobroActivo = true },
                                icono = Icons.Default.Payments,
                                habilitado = carritoItems.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- OVERLAYS DE FLUJO ---
        
        // 1. Pantalla de Cobro (Multi-método y Vuelto)
        PaymentOverlay(
            visible = flujoCobroActivo,
            total = carritoItems.sumOf { it.precio * it.cantidad },
            simbolo = simboloMoneda,
            onClose = { flujoCobroActivo = false },
            onConfirm = { resultadoVenta = TipoEstadoFarmadon.EXITO; flujoCobroActivo = false }
        )

        // 2. Resultado de Venta (Éxito/Error)
        ResultOverlay(
            resultado = resultadoVenta,
            onClose = { resultadoVenta = null; carritoItems.clear() }
        )

        // 3. Ventas Suspendidas
        POSSideSheet(
            visible = mostrarVentasSuspendidas,
            onClose = { mostrarVentasSuspendidas = false },
            titulo = "Ventas Suspendidas",
            subtitulo = "Recupere una venta guardada previamente para continuar el proceso"
        ) {
            POSEmptyState(
                icono = Icons.Default.Inventory,
                titulo = "No hay ventas suspendidas",
                subtitulo = "Use el botón 'Suspender' en el carrito para guardar una venta y atenderla más tarde."
            )
        }
    }
}

@Composable
private fun SearchPanel(busqueda: String, onBusquedaChange: (String) -> Unit) {
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusquedaChange,
        placeholder = { Text("Buscar producto (Nombre, Lote, Código)...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.Primary) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busqueda.isNotEmpty()) IconButton(onClick = { onBusquedaChange("") }) { Icon(Icons.Default.Close, null) }
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                IconButton(onClick = { /* Escáner */ }) { Icon(Icons.Default.QrCodeScanner, null, tint = FDColors.Primary) }
            }
        },
        singleLine = true,
        shape = FDShapes.Small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FDColors.InputBackground,
            unfocusedContainerColor = FDColors.InputBackground,
            focusedBorderColor = FDColors.Primary,
            unfocusedBorderColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(20.dp).height(60.dp)
    )
}

@Composable
private fun TotalDisplay(simbolo: String, total: Double, itemsCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = "TOTAL A COBRAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = FDColors.TextTertiary)
        Text(text = "$simbolo " + String.format(Locale.US, "%.2f", total), style = FDType.Numeric.copy(fontSize = 54.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
        Surface(color = FDColors.Primary.copy(alpha = 0.1f), shape = CircleShape) {
            Text(text = "$itemsCount PRODUCTOS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, color = FDColors.Primary))
        }
    }
}

@Composable
private fun ResumenImportes(simbolo: String, total: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ResumenRow("Subtotal gravado", "$simbolo " + String.format(Locale.US, "%.2f", total / 1.18))
        ResumenRow("IGV (18%)", "$simbolo " + String.format(Locale.US, "%.2f", total - (total / 1.18)))
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL NETO", style = FDType.Body.copy(fontWeight = FontWeight.Bold)); Text("$simbolo " + String.format(Locale.US, "%.2f", total), style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black))
        }
    }
}

@Composable
private fun ResumenRow(label: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = FDType.BodySmall, color = FDColors.TextSecondary)
        Text(valor, style = FDType.Numeric.copy(fontSize = 13.sp), color = FDColors.TextSecondary)
    }
}

@Composable
private fun CartItemRow(item: NuevaVentaItem, simbolo: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Controles de cantidad
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = {}, color = FDColors.InputBackground, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
            }
            Text(text = "${item.cantidad}", style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black), color = FDColors.Primary)
            Surface(onClick = {}, color = FDColors.Primary.copy(alpha = 0.08f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = FDColors.Primary) }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = item.nombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.descuento > 0) POSBadge("DESC. ${item.descuento}%", TipoEstadoFarmadon.EXITO)
            }
            Text(text = "Lote: ${item.lote} · Vence: ${item.vencimiento}", style = FDType.Caption, color = FDColors.TextTertiary)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "$simbolo " + String.format(Locale.US, "%.2f", item.precio * item.cantidad), style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black))
            IconButton(onClick = {}, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Delete, null, tint = FDColors.Error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun PaymentOverlay(
    visible: Boolean,
    total: Double,
    simbolo: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    if (visible) {
        var importeRecibido by remember { mutableStateOf("") }
        var metodoSeleccionado by remember { mutableStateOf("EFECTIVO") }
        
        val recibido = try { importeRecibido.toDouble() } catch (e: Exception) { 0.0 }
        val vuelto = (recibido - total).coerceAtLeast(0.0)

        Dialog(onDismissRequest = onClose) {
            Surface(
                modifier = Modifier.width(600.dp).wrapContentHeight(),
                shape = FDShapes.Large,
                color = FDColors.Surface,
                border = BorderStroke(1.dp, FDColors.Border)
            ) {
                Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("FINALIZAR PAGO", style = FDType.Heading2.copy(fontWeight = FontWeight.Black))
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Columna Métodos
                        Column(modifier = Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("MÉTODO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black))
                            listOf("EFECTIVO", "TARJETA", "YAPE / PLIN", "MIXTO").forEach { metodo ->
                                val isSel = metodoSeleccionado == metodo
                                Surface(
                                    onClick = { metodoSeleccionado = metodo },
                                    color = if (isSel) FDColors.Primary else FDColors.InputBackground,
                                    shape = FDShapes.Small,
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(metodo, style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = if (isSel) Color.White else FDColors.TextPrimary))
                                    }
                                }
                            }
                        }

                        // Columna Importes
                        Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Surface(color = FDColors.InputBackground, shape = FDShapes.Medium, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TOTAL A PAGAR", style = FDType.Label.copy(fontSize = 11.sp))
                                    Text("$simbolo " + String.format(Locale.US, "%.2f", total), style = FDType.Numeric.copy(fontSize = 32.sp, fontWeight = FontWeight.Black))
                                }
                            }

                            if (metodoSeleccionado == "EFECTIVO" || metodoSeleccionado == "MIXTO") {
                                OutlinedTextField(
                                    value = importeRecibido,
                                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) importeRecibido = it },
                                    label = { Text("Importe Recibido") },
                                    prefix = { Text("$simbolo ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = FDShapes.Small
                                )

                                if (recibido > 0) {
                                    Surface(
                                        color = if (vuelto >= 0) FDColors.SuccessSubtle else FDColors.ErrorSubtle,
                                        shape = FDShapes.Small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("VUELTO", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = if (vuelto >= 0) FDColors.Success else FDColors.Error)
                                            Text("$simbolo " + String.format(Locale.US, "%.2f", vuelto), style = FDType.Numeric.copy(fontWeight = FontWeight.Black), color = if (vuelto >= 0) FDColors.Success else FDColors.Error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FDBotonPrimario(
                        texto = "CONFIRMAR VENTA",
                        onClick = onConfirm,
                        icono = Icons.Default.CheckCircle,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        habilitado = recibido >= total || metodoSeleccionado != "EFECTIVO"
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultOverlay(resultado: TipoEstadoFarmadon?, onClose: () -> Unit) {
    if (resultado != null) {
        Dialog(onDismissRequest = onClose) {
            Surface(
                modifier = Modifier.width(450.dp).wrapContentHeight(),
                shape = FDShapes.Large,
                color = FDColors.Surface,
                border = BorderStroke(1.dp, FDColors.Border)
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    val isExito = resultado == TipoEstadoFarmadon.EXITO
                    
                    Icon(
                        if (isExito) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = if (isExito) FDColors.Success else FDColors.Error
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isExito) "¡VENTA EXITOSA!" else "ERROR EN LA VENTA", style = FDType.Heading1.copy(fontWeight = FontWeight.Black))
                        Text(if (isExito) "El comprobante ha sido generado correctamente." else "No se pudo procesar la transacción. Intente nuevamente.", textAlign = TextAlign.Center, color = FDColors.TextSecondary)
                    }

                    if (isExito) {
                        Surface(color = FDColors.InputBackground, shape = FDShapes.Small, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("N° Operación", style = FDType.Caption); Text("0001-000042", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Comprobante", style = FDType.Caption); Text("Boleta Electrónica", style = FDType.Body)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isExito) {
                            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(48.dp), shape = FDShapes.Small) {
                                Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("IMPRIMIR")
                            }
                        }
                        Button(
                            onClick = onClose,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = FDShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isExito) FDColors.Primary else FDColors.Error)
                        ) {
                            Text(if (isExito) "NUEVA VENTA" else "REINTENTAR")
                        }
                    }
                }
            }
        }
    }
}

private data class NuevaVentaItem(
    val nombre: String,
    val cantidad: Int,
    val precio: Double,
    val lote: String = "---",
    val vencimiento: String = "---",
    val descuento: Int = 0
)
