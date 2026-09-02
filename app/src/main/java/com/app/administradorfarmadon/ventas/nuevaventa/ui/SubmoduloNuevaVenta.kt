package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.inventario.compartido.ui.LectorCodigoBarrasCamaraDialog
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaUiState
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA PRINCIPAL DE NUEVA VENTA / PUNTO DE VENTA (POS).
 * Arquitectura: Documento Continuo (60%) + Liquidación Financiera en Vivo (40%).
 */
@Composable
fun SubmoduloNuevaVenta(
    simboloMoneda: String = "S/",
    viewModel: NuevaVentaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var mostrarCamaraEscaneo by remember { mutableStateOf(false) }

    // Auto-limpieza de mensajes temporales
    LaunchedEffect(uiState.mensajeExito, uiState.error) {
        if (uiState.mensajeExito != null || uiState.error != null) {
            delay(4000)
            viewModel.limpiarMensajes()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Notificaciones de advertencia de caja cerrada o errores
            if (!uiState.cajaAbierta) {
                POSNotificationBar(
                    mensaje = "LA CAJA SE ENCUENTRA CERRADA. ABRE EL TURNO EN 'CIERRE DE CAJA' PARA PODER COBRAR.",
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

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ───────────────────────────── PANEL IZQUIERDO: BUSCADOR Y CARRITO (60%) ─────────────────────────────
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Buscador de Productos
                        FDSearchField(
                            busqueda = uiState.busquedaTexto,
                            onBusquedaChange = { viewModel.onBusquedaChange(it) },
                            placeholder = "Buscar producto por nombre, código de barras o lote...",
                            modifier = Modifier.padding(14.dp),
                            trailingContent = {
                                if (uiState.buscando) {
                                    CircularProgressIndicator(
                                        color = FDColors.Primary,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    // CRÍTICO 3: Botón para abrir el escáner real de cámara
                                    IconButton(onClick = { mostrarCamaraEscaneo = true }) {
                                        Icon(Icons.Default.PhotoCamera, "Escanear con cámara", tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        )

                        // Desplegable de Resultados de Búsqueda
                        if (uiState.busquedaTexto.isNotBlank() && uiState.resultadosBusqueda.isNotEmpty()) {
                            Surface(
                                color = FDColors.SurfaceElevated,
                                border = BorderStroke(1.dp, FDColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .padding(horizontal = 14.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(uiState.resultadosBusqueda) { prod ->
                                        ResultadoBusquedaItem(
                                            producto = prod,
                                            simboloMoneda = simboloMoneda,
                                            onPresentacionClick = { pres ->
                                                viewModel.agregarAlCarrito(prod, pres)
                                                viewModel.limpiarBusqueda()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Lista de Ítems en Carrito
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.carrito.isEmpty()) {
                                POSEmptyState(
                                    icono = Icons.Default.ShoppingCart,
                                    titulo = "Carrito Vacío",
                                    subtitulo = "Busca productos por nombre o escanea con el lector/cámara para agregarlos al carrito."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(uiState.carrito) { item ->
                                        FilaCarritoItem(
                                            item = item,
                                            simbolo = simboloMoneda,
                                            onSumar = { viewModel.cambiarCantidadItem(item.productoId, item.presentacionId, 1) },
                                            onRestar = { viewModel.cambiarCantidadItem(item.productoId, item.presentacionId, -1) },
                                            onEliminar = { viewModel.eliminarItemCarrito(item.productoId, item.presentacionId) }
                                        )
                                    }
                                }
                            }
                        }

                        // Footer Panel Izquierdo: Acciones de Gestión de Carrito
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.abrirDialogoSuspender() },
                                    enabled = uiState.carrito.isNotEmpty(),
                                    modifier = Modifier.height(42.dp),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Border)
                                ) {
                                    Icon(Icons.Default.PauseCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Suspender", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = { viewModel.abrirSheetSuspendidas() },
                                    modifier = Modifier.height(42.dp),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Border)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Suspendidas (${uiState.ventasSuspendidas.size})", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = { viewModel.vaciarCarrito() },
                                    enabled = uiState.carrito.isNotEmpty(),
                                    modifier = Modifier.height(42.dp),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Border)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Vaciar", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }

                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ───────────────────────────── PANEL DERECHO: LIQUIDACIÓN (40%) ─────────────────────────────
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Hero Total a Cobrar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "TOTAL A COBRAR",
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = FDColors.TextTertiary
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = simboloMoneda,
                                        style = FDType.Numeric.copy(fontSize = 20.sp, color = FDColors.TextSecondary)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f", uiState.total),
                                        style = FDType.Numeric.copy(fontSize = 38.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.TextPrimary
                                    )
                                }
                                Surface(
                                    color = FDColors.Primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${uiState.totalItems} PRODUCTOS",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                                        style = FDType.Label.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FDColors.Primary
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                            // 2. Control de Receta Médica (si aplica)
                            if (uiState.requiereReceta) {
                                Surface(
                                    color = FDColors.WarningSubtle,
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Checkbox(
                                            checked = uiState.confirmoReceta,
                                            onCheckedChange = { viewModel.setConfirmoReceta(it) }
                                        )
                                        Column {
                                            Text(
                                                "Requiere Receta Médica",
                                                style = FDType.Label.copy(fontWeight = FontWeight.Bold, color = FDColors.Warning)
                                            )
                                            Text(
                                                "Confirmo que el cliente presentó la receta médica vigente.",
                                                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Tarjeta de Cliente
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CLIENTE / FACTURACIÓN",
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = FDColors.TextTertiary
                                )
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.abrirDialogoCliente() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                uiState.cliente.nombre,
                                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (uiState.cliente.numeroDocumento.isNotBlank()) {
                                                Text(
                                                    "${uiState.cliente.tipoDocumento}: ${uiState.cliente.numeroDocumento}",
                                                    style = FDType.Caption.copy(fontSize = 11.sp),
                                                    color = FDColors.TextSecondary
                                                )
                                            }
                                        }
                                        Icon(Icons.Default.Edit, null, tint = FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // 4. Desglose Financiero
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal:", style = FDType.Body, color = FDColors.TextSecondary)
                                    Text(
                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.subtotal)}",
                                        style = FDType.Numeric.copy(fontSize = 13.sp)
                                    )
                                }
                                if (uiState.descuento > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Descuento:", style = FDType.Body, color = FDColors.Success)
                                        Text(
                                            "- $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.descuento)}",
                                            style = FDType.Numeric.copy(fontSize = 13.sp, color = FDColors.Success)
                                        )
                                    }
                                }
                            }
                        }

                        // Botón Primario de Cobro
                        Column(
                            modifier = Modifier
                                .background(FDColors.SurfaceElevated)
                                .padding(18.dp)
                        ) {
                            FDBotonPrimario(
                                texto = if (uiState.cajaAbierta) "COBRAR AHORA ($simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)})" else "CAJA CERRADA",
                                onClick = { viewModel.abrirOverlayCobro() },
                                icono = Icons.Default.Payments,
                                habilitado = uiState.cajaAbierta && uiState.carrito.isNotEmpty() && (!uiState.requiereReceta || uiState.confirmoReceta),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                    }
                }
            }
        }

        // ───────────────────────────── ESCÁNER DE CÁMARA REAL (CRÍTICO 3) ─────────────────────────────
        if (mostrarCamaraEscaneo) {
            LectorCodigoBarrasCamaraDialog(
                onCodigoDetectado = { codigo ->
                    viewModel.onBusquedaChange(codigo)
                    mostrarCamaraEscaneo = false
                },
                onDismiss = { mostrarCamaraEscaneo = false }
            )
        }

        // ───────────────────────────── OVERLAYS Y DIÁLOGOS ─────────────────────────────

        // Overlay de Cobro con Métodos Reales y Vuelto
        if (uiState.mostrarOverlayCobro) {
            OverlayCobro(
                uiState = uiState,
                simboloMoneda = simboloMoneda,
                onClose = { viewModel.cerrarOverlayCobro() },
                onConfirmar = { viewModel.confirmarVenta() },
                onActualizarLineas = { viewModel.actualizarLineasPago(it) }
            )
        }

        // Diálogo de Venta Exitosa e Impresión de Ticket
        uiState.ventaExitosa?.let { venta ->
            DialogoVentaExitosa(
                venta = venta,
                simboloMoneda = simboloMoneda,
                onImprimir = { viewModel.imprimirComprobante(context) },
                onNuevaVenta = { viewModel.finalizarVentaExitosa() }
            )
        }

        // Diálogo de Selección / Consulta de Cliente (RENIEC / SUNAT / Directorio)
        if (uiState.mostrarDialogoCliente) {
            DialogoCliente(
                clienteActual = uiState.cliente,
                directorioClientes = uiState.directorioClientes,
                consultando = uiState.consultandoDoc,
                onDismiss = { viewModel.cerrarDialogoCliente() },
                onConsultar = { tipo, num -> viewModel.consultarDocumentoCliente(tipo, num) },
                onGuardar = { cliente, guardarDir -> viewModel.guardarClienteYEstablecer(cliente, guardarDir) }
            )
        }

        // Diálogo para Suspender Venta
        if (uiState.mostrarDialogoSuspender) {
            DialogoSuspenderVenta(
                onDismiss = { viewModel.cerrarDialogoSuspender() },
                onConfirmar = { nota -> viewModel.suspenderVenta(nota) }
            )
        }

        // SideSheet de Ventas Suspendidas
        POSSideSheet(
            visible = uiState.mostrarSheetSuspendidas,
            onClose = { viewModel.cerrarSheetSuspendidas() },
            titulo = "Ventas Suspendidas"
        ) {
            if (uiState.ventasSuspendidas.isEmpty()) {
                POSEmptyState(
                    icono = Icons.Default.PauseCircle,
                    titulo = "Sin ventas suspendidas",
                    subtitulo = "Las ventas que dejes en pausa aparecerán aquí para retomarlas en cualquier momento."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.ventasSuspendidas.forEach { susp ->
                        Surface(
                            color = FDColors.InputBackground,
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(susp.cliente.nombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", susp.total)}",
                                        style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Primary)
                                    )
                                }
                                Text("${susp.items.size} productos · ${susp.creadoPorNombre}", style = FDType.Caption, color = FDColors.TextSecondary)
                                if (susp.nota.isNotBlank()) {
                                    Text("Nota: \"${susp.nota}\"", style = FDType.Caption.copy(color = FDColors.TextTertiary))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.descartarVentaSuspendida(susp.id) },
                                        shape = FDShapes.Small,
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Eliminar", color = FDColors.Error, style = FDType.Caption)
                                    }
                                    Button(
                                        onClick = { viewModel.reanudarVentaSuspendida(susp) },
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                                        modifier = Modifier.weight(1.5f).height(38.dp)
                                    ) {
                                        Text("Reanudar", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
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

// ───────────────────────────── COMPONENTES AUXILIARES ─────────────────────────────

@Composable
private fun FilaCarritoItem(
    item: ItemVenta,
    simbolo: String,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onEliminar: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stepper de Cantidad
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onRestar,
                    color = FDColors.InputBackground,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = FDColors.TextPrimary)
                    }
                }
                Text(
                    text = "${item.cantidad}",
                    style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary,
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )
                Surface(
                    onClick = onSumar,
                    color = FDColors.Primary.copy(alpha = 0.08f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = FDColors.Primary)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Información del Producto
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.nombreProducto,
                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = FDColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.requiereReceta) {
                        POSBadge(texto = "RECETA", tipo = TipoEstadoFarmadon.ALERTA)
                    }
                }
                Text(
                    text = "Presentación: ${item.presentacionNombre} · P.U. $simbolo ${String.format(Locale.US, "%.2f", item.precioUnitario)}",
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = FDColors.TextTertiary
                )
            }

            Spacer(Modifier.width(12.dp))

            // Subtotal del Ítem
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$simbolo " + String.format(Locale.US, "%.2f", item.subtotal),
                    style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onEliminar, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = FDColors.TextTertiary)
            }
        }
    }
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ResultadoBusquedaItem(
    producto: MoldeProductos,
    simboloMoneda: String,
    onPresentacionClick: (PresentacionProducto) -> Unit
) {
    Surface(
        color = FDColors.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            val stockDisp = producto.stockDisponibleFisico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    producto.nombre,
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    "Stock: ${stockDisp.toInt()} disp.",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = if (stockDisp > 0) FDColors.Success else FDColors.Error)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                producto.presentaciones.forEach { pres ->
                    Surface(
                        onClick = { onPresentacionClick(pres) },
                        color = FDColors.InputBackground,
                        shape = FDShapes.Small,
                        border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(pres.nombre, style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "$simboloMoneda ${String.format(Locale.US, "%.2f", pres.precioventa)}",
                                style = FDType.Numeric.copy(fontSize = 11.sp, color = FDColors.Primary, fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
}

@Composable
private fun OverlayCobro(
    uiState: NuevaVentaUiState,
    simboloMoneda: String,
    onClose: () -> Unit,
    onConfirmar: () -> Unit,
    onActualizarLineas: (List<PagoVenta>) -> Unit
) {
    // CRÍTICO 1: Métodos de pago reales configurados para la sucursal activa
    val metodosActivos = uiState.metodosPagoDisponibles

    var instanciaSeleccionadaId by remember(metodosActivos) {
        mutableStateOf(metodosActivos.firstOrNull { it.tipoId == "EFECTIVO" }?.id ?: metodosActivos.firstOrNull()?.id ?: "")
    }
    var montoInput by remember { mutableStateOf("") }
    var opInput by remember { mutableStateOf("") }

    val instanciaActual = metodosActivos.firstOrNull { it.id == instanciaSeleccionadaId } ?: metodosActivos.firstOrNull()
    val tipoInfoActual = TIPOS_PAGO_FIJOS.firstOrNull { it.id == instanciaActual?.tipoId }
    val requiereOperacion = tipoInfoActual?.requiereOperacion ?: false

    Dialog(onDismissRequest = { if (!uiState.procesandoCobro) onClose() }) {
        Surface(
            modifier = Modifier
                .width(560.dp)
                .wrapContentHeight(),
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Liquidación y Cobro", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 20.sp))
                    IconButton(onClick = onClose, enabled = !uiState.procesandoCobro) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                // Resumen de Total y Pagos
                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total a Pagar:", style = FDType.Body, color = FDColors.TextSecondary)
                            Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 16.sp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monto Recibido:", style = FDType.Body, color = FDColors.TextSecondary)
                            Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.sumaPagos)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        if (uiState.vuelto > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vuelto:", style = FDType.Body.copy(fontWeight = FontWeight.Black), color = FDColors.Success)
                                Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.vuelto)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Success, fontSize = 16.sp))
                            }
                        } else if (uiState.montoFaltante > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Falta por cubrir:", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.Error)
                                Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoFaltante)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Error))
                            }
                        }
                    }
                }

                // CRÍTICO 1: Selector dinámico de Métodos de Pago Activos de la Farmacia
                if (metodosActivos.isEmpty()) {
                    Surface(
                        color = FDColors.WarningSubtle,
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                            Text(
                                "No hay métodos de pago activos configurados en esta sucursal. Actívalos en Configuración → Métodos de Pago.",
                                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextPrimary)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(metodosActivos) { instancia ->
                            val sel = instancia.id == (instanciaActual?.id ?: "")
                            val tipo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == instancia.tipoId }
                            val nombreMostrar = tipo?.nombre ?: instancia.tipoId

                            Surface(
                                onClick = {
                                    instanciaSeleccionadaId = instancia.id
                                    if (montoInput.isBlank() || montoInput == "0.00") {
                                        montoInput = String.format(Locale.US, "%.2f", uiState.montoFaltante.takeIf { it > 0 } ?: uiState.total)
                                    }
                                },
                                color = if (sel) FDColors.Primary else FDColors.InputBackground,
                                shape = FDShapes.Small,
                                modifier = Modifier.height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        nombreMostrar,
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

                    // Inputs para añadir línea de pago
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FDTextField(
                            value = montoInput,
                            onValueChange = { montoInput = it },
                            label = "Monto ($simboloMoneda)",
                            placeholder = "0.00",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.1f)
                        )

                        // CRÍTICO 2: N° Operación obligatorio u opcional según tipoInfo
                        if (instanciaActual?.tipoId != "EFECTIVO") {
                            FDTextField(
                                value = opInput,
                                onValueChange = { opInput = it },
                                label = if (requiereOperacion) "N° Operación *" else "N° Operación",
                                placeholder = if (requiereOperacion) "Obligatorio" else "Opcional",
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        val montoValido = (montoInput.toDoubleOrNull() ?: 0.0) > 0.0
                        val opValida = !requiereOperacion || opInput.trim().isNotBlank()

                        Button(
                            onClick = {
                                val m = montoInput.toDoubleOrNull() ?: 0.0
                                if (m > 0.0 && opValida && instanciaActual != null) {
                                    val tipo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == instanciaActual.tipoId }
                                    val nombreMetodo = tipo?.nombre ?: instanciaActual.tipoId
                                    val nuevaLista = uiState.lineasPago.toMutableList().apply {
                                        add(
                                            PagoVenta(
                                                tipoId = instanciaActual.tipoId,
                                                instanciaId = instanciaActual.id,
                                                nombreMetodo = nombreMetodo,
                                                monto = m,
                                                numeroOperacion = opInput.trim()
                                            )
                                        )
                                    }
                                    onActualizarLineas(nuevaLista)
                                    montoInput = ""
                                    opInput = ""
                                }
                            },
                            enabled = montoValido && opValida && instanciaActual != null,
                            shape = FDShapes.Small,
                            modifier = Modifier.padding(top = 18.dp).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                        ) {
                            Text("+ Agregar", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Líneas de Pago Actuales
                if (uiState.lineasPago.isNotEmpty()) {
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.5f),
                        shape = FDShapes.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            uiState.lineasPago.forEachIndexed { idx, p ->
                                val tipoP = TIPOS_PAGO_FIJOS.firstOrNull { it.id == p.tipoId }
                                val reqOp = tipoP?.requiereOperacion ?: false
                                val faltaOp = reqOp && p.numeroOperacion.isBlank()

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "${p.nombreMetodo}${if (p.numeroOperacion.isNotBlank()) " (Op: ${p.numeroOperacion})" else ""}",
                                            style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        )
                                        if (faltaOp) {
                                            Text(
                                                "Falta N° de operación",
                                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Error)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("$simboloMoneda ${String.format(Locale.US, "%.2f", p.monto)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                                        IconButton(onClick = {
                                            val l = uiState.lineasPago.toMutableList().apply { removeAt(idx) }
                                            onActualizarLineas(l)
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Botón Confirmar
                Button(
                    onClick = onConfirmar,
                    enabled = uiState.puedeCobrar && !uiState.procesandoCobro,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = FDShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    if (uiState.procesandoCobro) {
                        CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CONFIRMAR Y FINALIZAR VENTA", style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoVentaExitosa(
    venta: Venta,
    simboloMoneda: String,
    onImprimir: () -> Unit,
    onNuevaVenta: () -> Unit
) {
    Dialog(onDismissRequest = onNuevaVenta) {
        Surface(
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    color = FDColors.Success.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(36.dp), tint = FDColors.Success)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¡Venta Exitosa!", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 22.sp))
                    Text("Comprobante: ${venta.tipoComprobante} ${venta.numeroCompleto}", style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Cobrado:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("$simboloMoneda ${String.format(Locale.US, "%.2f", venta.total)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black))
                        }
                        if (venta.vuelto > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vuelto Entregado:", style = FDType.Caption, color = FDColors.TextSecondary)
                                Text("$simboloMoneda ${String.format(Locale.US, "%.2f", venta.vuelto)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Bold, color = FDColors.Success))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cliente:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(venta.cliente.nombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onImprimir,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(18.dp), tint = FDColors.Primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Imprimir", style = FDType.Label.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                    }

                    Button(
                        onClick = onNuevaVenta,
                        modifier = Modifier.weight(1.2f).height(46.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("NUEVA VENTA", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoCliente(
    clienteActual: ClienteDeVenta,
    directorioClientes: List<com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia>,
    consultando: Boolean,
    onDismiss: () -> Unit,
    onConsultar: (String, String) -> Unit,
    onGuardar: (ClienteDeVenta, Boolean) -> Unit
) {
    var tipoDoc by remember { mutableStateOf(if (clienteActual.tipoDocumento.isNotBlank() && clienteActual.tipoDocumento != "NINGUNO") clienteActual.tipoDocumento else "DNI") }
    var numDoc by remember { mutableStateOf(clienteActual.numeroDocumento) }
    var nombre by remember { mutableStateOf(if (clienteActual.nombre != "Consumidor Final") clienteActual.nombre else "") }
    var guardarEnDirectorio by remember { mutableStateOf(true) }

    val yaExiste = remember(numDoc, directorioClientes) {
        directorioClientes.any { it.numeroDocumento == numDoc.trim() }
    }

    val sugerencias = remember(numDoc, nombre, tipoDoc, directorioClientes) {
        if (tipoDoc == "NINGUNO" || (numDoc.isBlank() && nombre.isBlank())) emptyList()
        else {
            val qNum = numDoc.trim()
            val qNom = nombre.trim().uppercase()
            directorioClientes.filter { c ->
                (qNum.length >= 2 && c.numeroDocumento.contains(qNum)) ||
                    (qNom.length >= 2 && c.nombre.uppercase().contains(qNom))
            }.take(3)
        }
    }

    val guardarHabilitado = if (tipoDoc == "NINGUNO") true else numDoc.trim().isNotBlank() && nombre.trim().isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Identificación del Cliente", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DNI", "RUC", "NINGUNO").forEach { t ->
                        val sel = tipoDoc == t
                        Surface(
                            onClick = {
                                tipoDoc = t
                                if (t == "NINGUNO") {
                                    numDoc = ""
                                    nombre = "Consumidor Final"
                                }
                            },
                            color = if (sel) FDColors.Primary else FDColors.InputBackground,
                            shape = FDShapes.Small,
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(t, style = FDType.Label.copy(fontSize = 11.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Bold), color = if (sel) FDColors.PrimaryText else FDColors.TextSecondary)
                            }
                        }
                    }
                }

                if (tipoDoc != "NINGUNO") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FDTextField(
                            value = numDoc,
                            onValueChange = { numDoc = it },
                            label = "N° DE $tipoDoc",
                            placeholder = if (tipoDoc == "DNI") "8 dígitos" else "11 dígitos",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { onConsultar(tipoDoc, numDoc) },
                            enabled = !consultando && numDoc.isNotBlank(),
                            shape = FDShapes.Small,
                            modifier = Modifier.padding(top = 18.dp).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                        ) {
                            if (consultando) {
                                CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("RENIEC/SUNAT", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    FDTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = if (tipoDoc == "RUC") "RAZÓN SOCIAL *" else "NOMBRES Y APELLIDOS *",
                        placeholder = "Nombre completo del cliente (requerido)"
                    )

                    // Sugerencias del Directorio de Clientes
                    if (sugerencias.isNotEmpty() && !yaExiste) {
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CLIENTES ENCONTRADOS EN DIRECTORIO:", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                sugerencias.forEach { sug ->
                                    Surface(
                                        onClick = {
                                            tipoDoc = sug.tipoDocumento
                                            numDoc = sug.numeroDocumento
                                            nombre = sug.nombre
                                        },
                                        color = FDColors.Surface,
                                        shape = FDShapes.Small,
                                        border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sug.nombre, style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
                                            Text("${sug.tipoDocumento}: ${sug.numeroDocumento}", style = FDType.Caption.copy(fontSize = 10.5.sp), color = FDColors.Primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Checkbox para guardar ficha nueva en el directorio
                    if (!yaExiste) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Checkbox(
                                checked = guardarEnDirectorio,
                                onCheckedChange = { guardarEnDirectorio = it }
                            )
                            Text(
                                "Guardar en el Directorio de Clientes de la farmacia",
                                style = FDType.Caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(42.dp), shape = FDShapes.Small) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val c = if (tipoDoc == "NINGUNO") {
                                ClienteDeVenta(tipoDocumento = "NINGUNO", nombre = "Consumidor Final", clienteId = "")
                            } else {
                                ClienteDeVenta(
                                    tipoDocumento = tipoDoc,
                                    numeroDocumento = numDoc.trim(),
                                    nombre = nombre.trim(),
                                    clienteId = numDoc.trim()
                                )
                            }
                            onGuardar(c, if (tipoDoc == "NINGUNO") false else guardarEnDirectorio)
                        },
                        enabled = guardarHabilitado,
                        modifier = Modifier.weight(1.2f).height(42.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("GUARDAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoSuspenderVenta(
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var nota by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(400.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Suspender Venta", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))
                Text("La venta se guardará en la nube para retomarla luego desde cualquier caja.", style = FDType.Body, color = FDColors.TextSecondary)

                FDTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = "NOTA / REFERENCIA (OPCIONAL)",
                    placeholder = "Ej: Cliente fue al cajero a retirar dinero",
                    singleLine = false,
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(42.dp), shape = FDShapes.Small) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = { onConfirmar(nota) },
                        modifier = Modifier.weight(1.2f).height(42.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("SUSPENDER", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}
