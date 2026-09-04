package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.configuracion.pos.modelo.AutorizacionSupervisor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.facturacion.envio.worker.FacturacionEnvioWorker
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.inventario.compartido.ui.LectorCodigoBarrasCamaraDialog
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemChecklistApertura
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaUiState
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaViewModel
import com.app.administradorfarmadon.ventas.nuevaventa.logica.ResultadoDocUi
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
    viewModel: NuevaVentaViewModel = viewModel(),
    onNavigate: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequesterBuscador = remember { FocusRequester() }
    var mostrarCamaraEscaneo by remember { mutableStateOf(false) }
    var itemParaEditarCantidad by remember { mutableStateOf<ItemVenta?>(null) }

    // FASE 10: Foco permanente en el buscador al entrar a la pantalla
    LaunchedEffect(Unit) {
        delay(150)
        try { focusRequesterBuscador.requestFocus() } catch (_: Exception) {}
    }

    // FASE 10: Devolver el foco al buscador tras cerrar cualquier diálogo o finalizar venta
    LaunchedEffect(
        uiState.mostrarOverlayCobro,
        uiState.mostrarDialogoCliente,
        uiState.mostrarDialogoSuspender,
        uiState.mostrarSheetSuspendidas,
        uiState.ventaExitosa,
        mostrarCamaraEscaneo,
        itemParaEditarCantidad
    ) {
        if (!uiState.mostrarOverlayCobro &&
            !uiState.mostrarDialogoCliente &&
            !uiState.mostrarDialogoSuspender &&
            !uiState.mostrarSheetSuspendidas &&
            uiState.ventaExitosa == null &&
            !mostrarCamaraEscaneo &&
            itemParaEditarCantidad == null
        ) {
            delay(100)
            try { focusRequesterBuscador.requestFocus() } catch (_: Exception) {}
        }
    }

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
            // Alerta crítica: Caja abierta de una fecha anterior pendiente de cierre
            if (uiState.estadoCaja.esDeJornadaAnterior()) {
                Surface(
                    color = FDColors.ErrorSubtle,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.5.dp, FDColors.Error.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(FDColors.Error.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FDColors.Error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "CAJA PENDIENTE DE CIERRE: JORNADA DEL ${uiState.estadoCaja.fechaAperturaLegible()}",
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                        color = FDColors.Error
                                    )
                                    Text(
                                        text = "Por control contable no se puede mezclar ventas de hoy con una caja abierta ayer. Debe cerrarse con arqueo físico antes de operar.",
                                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { onNavigate?.invoke("caja") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Error,
                                    contentColor = Color.White
                                ),
                                shape = FDShapes.Small,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Ir a Cerrar Caja",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }
                        }

                        // Fila de datos del turno rezagado: fácil de entender para cualquier persona
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FDColors.Surface, FDShapes.Small)
                                .border(BorderStroke(1.dp, FDColors.Border), FDShapes.Small)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Person, null, tint = FDColors.Primary, modifier = Modifier.size(15.dp))
                                Text("Cajero(a):", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextSecondary))
                                Text(uiState.estadoCaja.abiertoPorNombre.ifBlank { "Sin asignar" }, style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AttachMoney, null, tint = FDColors.TextTertiary, modifier = Modifier.size(15.dp))
                                Text("Fondo entregado:", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextSecondary))
                                Text("S/ %.2f".format(Locale.US, uiState.estadoCaja.fondoInicial), style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ReceiptLong, null, tint = FDColors.TextTertiary, modifier = Modifier.size(15.dp))
                                Text("Ventas acumuladas:", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextSecondary))
                                Text("${uiState.estadoCaja.cantidadVentas} ops (S/ %.2f)".format(Locale.US, uiState.estadoCaja.totalVentas), style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                            }
                        }
                    }
                }
            }

            // Puerta Única: Checklist de Apertura de Sede (R1/R3/R8/R12)
            if (uiState.checklistCargado && !uiState.checklist.todoListo) {
                ChecklistAperturaBanner(
                    checklist = uiState.checklist,
                    excluirItemCaja = uiState.estadoCaja.esDeJornadaAnterior(),
                    onNavigate = onNavigate
                )
            }

            if (uiState.error != null) {
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
                        // Buscador de Productos con Foco Permanente y Enter Inmediato (FASE 10)
                        FDSearchField(
                            busqueda = uiState.busquedaTexto,
                            onBusquedaChange = { viewModel.onBusquedaChange(it) },
                            placeholder = "Buscar producto por nombre o código de barras...",
                            focusRequester = focusRequesterBuscador,
                            onEnterPressed = {
                                if (uiState.busquedaTexto.isNotBlank()) {
                                    viewModel.ejecutarBusquedaInmediata()
                                } else if (uiState.cajaAbierta && uiState.emisorCompleto && uiState.carrito.isNotEmpty() && (!uiState.requiereReceta || uiState.confirmoReceta)) {
                                    viewModel.abrirOverlayCobro()
                                }
                            },
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
                                    .heightIn(max = 340.dp)
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
                                            onClickCantidad = { itemParaEditarCantidad = item },
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
                val transitionError = rememberInfiniteTransition(label = "panelErrorPulse")
                val pulsoErrorAlpha by transitionError.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alphaError"
                )
                val hayErrorVenta = uiState.error != null
                val bordePanel = if (hayErrorVenta) {
                    BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = pulsoErrorAlpha))
                } else {
                    BorderStroke(1.dp, FDColors.Border)
                }

                Surface(
                    color = if (hayErrorVenta) Color(0xFFEF4444).copy(alpha = 0.04f) else FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = bordePanel,
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

                            // 3. Selector de Cliente Inline (RENIEC / SUNAT sin modal forzado con debounce 800ms)
                            SelectorClientePOSInline(
                                cliente = uiState.cliente,
                                consultando = uiState.consultandoDoc,
                                resultadoDoc = uiState.resultadoConsultaDoc,
                                onConsultarDoc = { doc -> viewModel.consultarDocumentoAuto(doc) },
                                onAplicarResultado = { res -> viewModel.aplicarResultadoCliente(res) },
                                onLimpiarResultado = { viewModel.limpiarResultadoConsultaDoc() },
                                onLimpiarCliente = { viewModel.limpiarCliente() },
                                onAbrirManual = { viewModel.abrirDialogoCliente() }
                            )

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

                                // Fila interactiva de descuento
                                Surface(
                                    onClick = { if (uiState.carrito.isNotEmpty()) viewModel.abrirDialogoDescuento() },
                                    color = if (uiState.descuento > 0.0) FDColors.Success.copy(alpha = 0.08f) else FDColors.InputBackground.copy(alpha = 0.5f),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, if (uiState.descuento > 0.0) FDColors.Success.copy(alpha = 0.3f) else FDColors.Border.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Percent,
                                                contentDescription = null,
                                                tint = if (uiState.descuento > 0.0) FDColors.Success else FDColors.Primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                if (uiState.descuento > 0.0) "Descuento:" else "Aplicar Descuento",
                                                style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                color = if (uiState.descuento > 0.0) FDColors.Success else FDColors.Primary
                                            )
                                        }
                                        Text(
                                            if (uiState.descuento > 0.0) "- $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.descuento)}" else "Configurar",
                                            style = FDType.Numeric.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.descuento > 0.0) FDColors.Success else FDColors.Primary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Botón Primario de Cobro y Tarjeta de Error con Reintento
                        Column(
                            modifier = Modifier
                                .background(FDColors.SurfaceElevated)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val errorCobro = uiState.error
                            if (errorCobro != null) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Error al procesar el cobro:",
                                                style = FDType.Caption.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFDC2626)
                                                )
                                            )
                                        }
                                        Text(
                                            text = errorCobro,
                                            style = FDType.Caption.copy(
                                                fontSize = 11.sp,
                                                color = Color(0xFF991B1B)
                                            )
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.limpiarError() },
                                                shape = FDShapes.Small,
                                                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.35f)),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Descartar", style = FDType.Caption.copy(fontSize = 11.sp, color = Color(0xFF991B1B)))
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.limpiarError()
                                                    viewModel.confirmarVenta()
                                                },
                                                shape = FDShapes.Small,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                modifier = Modifier.weight(1.3f).height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reintentar Cobro", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                            }
                                        }
                                    }
                                }
                            }

                            val textoBoton = when {
                                !uiState.checklist.todoListo -> "APERTURA PENDIENTE (${uiState.checklist.totalCompletados}/${uiState.checklist.totalRequisitos})"
                                else -> "COBRAR AHORA ($simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)})"
                            }
                            FDBotonPrimario(
                                texto = textoBoton,
                                onClick = { viewModel.abrirOverlayCobro() },
                                icono = if (!uiState.checklist.todoListo) Icons.Default.Lock else Icons.Default.Payments,
                                habilitado = uiState.checklist.todoListo && uiState.carrito.isNotEmpty() && (!uiState.requiereReceta || uiState.confirmoReceta),
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

        // Diálogo para Modificar Cantidad Directa (FASE 10)
        itemParaEditarCantidad?.let { item ->
            DialogoModificarCantidad(
                item = item,
                simboloMoneda = simboloMoneda,
                onDismiss = { itemParaEditarCantidad = null },
                onConfirmar = { nuevaCant ->
                    viewModel.setCantidadItem(item.productoId, item.presentacionId, nuevaCant)
                    itemParaEditarCantidad = null
                }
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

        // Diálogo de Descuento con validación de PosConfig
        if (uiState.mostrarDialogoDescuento) {
            DialogoDescuento(
                uiState = uiState,
                simboloMoneda = simboloMoneda,
                onDismiss = { viewModel.cerrarDialogoDescuento() },
                onAplicar = { monto, autorizante ->
                    viewModel.aplicarDescuento(monto, autorizante)
                }
            )
        }

        // Diálogo de Venta Exitosa e Impresión de Ticket
        uiState.ventaExitosa?.let { venta ->
            LaunchedEffect(venta.id) {
                val farmaciaId = SessionManager.clienteIdGarantizado
                FacturacionEnvioWorker.encolarReintento(context, farmaciaId)
            }
            AnimacionConfetiOverlay(
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
private fun DialogoModificarCantidad(
    item: ItemVenta,
    simboloMoneda: String,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var cantidadTexto by remember { mutableStateOf("${item.cantidad}") }
    val focusRequester = remember { FocusRequester() }
    val cantidadValida = cantidadTexto.trim().toIntOrNull()?.let { it > 0 } == true

    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(360.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Modificar Cantidad",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    "${item.nombreProducto} (${item.presentacionNombre})",
                    style = FDType.Body.copy(fontSize = 12.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                FDTextField(
                    value = cantidadTexto,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cantidadTexto = it },
                    label = "CANTIDAD",
                    placeholder = "1",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                val cant = cantidadTexto.trim().toIntOrNull() ?: 1
                                if (cant > 0) {
                                    onConfirmar(cant)
                                    true
                                } else false
                            } else false
                        }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = FDShapes.Small
                    ) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val cant = cantidadTexto.trim().toIntOrNull() ?: 1
                            if (cant > 0) onConfirmar(cant)
                        },
                        enabled = cantidadValida,
                        modifier = Modifier.weight(1.3f).height(40.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("ACEPTAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaCarritoItem(
    item: ItemVenta,
    simbolo: String,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onClickCantidad: () -> Unit,
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
            // Stepper de Cantidad con número clickeable editable (FASE 10)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                Surface(
                    onClick = onClickCantidad,
                    color = FDColors.Primary.copy(alpha = 0.08f),
                    shape = FDShapes.Small,
                    modifier = Modifier.height(28.dp).widthIn(min = 34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text(
                            text = "${item.cantidad}",
                            style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black),
                            color = FDColors.Primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
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

                // FASE 11 H2: Trazabilidad física de lote y anaquel
                val loteInfo = buildString {
                    if (item.loteSugerido.isNotBlank()) {
                        append("Sale: Lote ${item.loteSugerido}")
                        if (item.loteVencimientoSugerido.isNotBlank()) {
                            append(" · Vence: ${item.loteVencimientoSugerido}")
                        }
                    }
                    if (item.ubicacionAnaquel.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(if (item.ubicacionAnaquel.startsWith("Anaquel", ignoreCase = true)) item.ubicacionAnaquel else "Anaquel: ${item.ubicacionAnaquel}")
                    }
                }
                if (loteInfo.isNotBlank()) {
                    Text(
                        text = loteInfo,
                        style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.Primary.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                    )
                }
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
    val stockDisp = producto.stockDisponibleFisico
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Encabezado del Producto: Nombre + Categoría + Stock Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = producto.nombre,
                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                        color = FDColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (producto.categoriaNombre.isNotBlank()) {
                        Surface(
                            color = FDColors.PrimarySubtle,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = producto.categoriaNombre,
                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Stock: ${stockDisp.toInt()} disp.",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (stockDisp > 0) FDColors.Success else FDColors.Error
                )
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)

            // Filas de Presentaciones individuales con precios y botones independientes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                producto.presentaciones.forEach { pres ->
                    Surface(
                        color = FDColors.InputBackground,
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Sell,
                                    contentDescription = null,
                                    tint = FDColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = pres.nombre,
                                    style = FDType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                    color = FDColors.TextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", pres.precioventa)}",
                                    style = FDType.Numeric.copy(fontSize = 13.sp, color = FDColors.Primary, fontWeight = FontWeight.Bold)
                                )

                                Button(
                                    onClick = { onPresentacionClick(pres) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pres.precioventa > 0) FDColors.Primary else FDColors.Error,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        if (pres.precioventa > 0) Icons.Default.AddShoppingCart else Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (pres.precioventa > 0) "+ AGREGAR" else "SIN PRECIO",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
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

                    // FASE 10: Chips de billetes rápidos (solo EFECTIVO)
                    if (instanciaActual?.tipoId == "EFECTIVO") {
                        val faltante = uiState.montoFaltante.takeIf { it > 0 } ?: uiState.total
                        val billetes = listOf(
                            "Exacto" to String.format(Locale.US, "%.2f", faltante),
                            "S/ 10" to "10.00",
                            "S/ 20" to "20.00",
                            "S/ 50" to "50.00",
                            "S/ 100" to "100.00",
                            "S/ 200" to "200.00"
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(billetes) { (label, valor) ->
                                val esSeleccionado = montoInput.trim() == valor
                                Surface(
                                    onClick = { montoInput = valor },
                                    color = if (esSeleccionado) FDColors.Primary.copy(alpha = 0.15f) else FDColors.InputBackground,
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, if (esSeleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Text(
                                            text = if (label == "Exacto") "Exacto ($simboloMoneda ${String.format(Locale.US, "%.2f", faltante)})" else label,
                                            style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = if (esSeleccionado) FDColors.Primary else FDColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val montoParsed = com.app.administradorfarmadon.ventas.compartido.logica.MontoFormateador.normalizarMontoEstricto(montoInput)
                    val montoValido = montoParsed != null
                    val opValida = !requiereOperacion || opInput.trim().isNotBlank()

                    val onAgregarLineaPago = {
                        val m = com.app.administradorfarmadon.ventas.compartido.logica.MontoFormateador.normalizarMontoEstricto(montoInput) ?: 0.0
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = if (instanciaActual?.tipoId == "EFECTIVO") ImeAction.Done else ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                        if (montoValido && opValida) {
                                            onAgregarLineaPago()
                                            true
                                        } else if (montoInput.isBlank() && uiState.puedeCobrar && !uiState.procesandoCobro) {
                                            onConfirmar()
                                            true
                                        } else false
                                    } else false
                                }
                        )

                        // CRÍTICO 2: N° Operación obligatorio u opcional según tipoInfo
                        if (instanciaActual?.tipoId != "EFECTIVO") {
                            FDTextField(
                                value = opInput,
                                onValueChange = { opInput = it },
                                label = if (requiereOperacion) "N° Operación *" else "N° Operación",
                                placeholder = if (requiereOperacion) "Obligatorio" else "Opcional",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                            if (montoValido && opValida) {
                                                onAgregarLineaPago()
                                                true
                                            } else false
                                        } else false
                                    }
                            )
                        }

                        Button(
                            onClick = onAgregarLineaPago,
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onNuevaVenta) {
        Surface(
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .width(440.dp)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter || keyEvent.key == Key.Spacebar)) {
                        onNuevaVenta()
                        true
                    } else false
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Registro Manual de Cliente", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))
                    Text(
                        "Ingresa los datos manualmente como respaldo si la consulta automática no está disponible o falla.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                }

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
                    FDTextField(
                        value = numDoc,
                        onValueChange = { nuevo -> numDoc = nuevo.filter { it.isDigit() }.take(if (tipoDoc == "DNI") 8 else 11) },
                        label = "N° DE $tipoDoc",
                        placeholder = if (tipoDoc == "DNI") "8 dígitos (DNI)" else "11 dígitos (RUC)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FDTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = if (tipoDoc == "RUC") "RAZÓN SOCIAL *" else "NOMBRES Y APELLIDOS *",
                        placeholder = "Nombre completo o razón social (requerido)"
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

@Composable
private fun DialogoDescuento(
    uiState: NuevaVentaUiState,
    simboloMoneda: String,
    onDismiss: () -> Unit,
    onAplicar: (monto: Double, autorizante: AutorizacionSupervisor?) -> Unit
) {
    val subtotal = uiState.subtotal
    val posConfig = uiState.posConfig

    var modoPorcentaje by remember { mutableStateOf(true) }
    var inputStr by remember {
        val pctInicial = if (subtotal > 0.0 && uiState.descuento > 0.0) (uiState.descuento / subtotal) * 100.0 else 0.0
        mutableStateOf(if (pctInicial > 0.0) String.format(Locale.US, "%.1f", pctInicial) else "")
    }

    val montoCalculado: Double = remember(inputStr, modoPorcentaje, subtotal) {
        val num = inputStr.toDoubleOrNull() ?: 0.0
        if (num <= 0.0) 0.0
        else if (modoPorcentaje) kotlin.math.round((subtotal * (num / 100.0)) * 100.0) / 100.0
        else kotlin.math.round(num * 100.0) / 100.0
    }

    val pctCalculado: Double = remember(montoCalculado, subtotal) {
        if (subtotal > 0.0) kotlin.math.round(((montoCalculado / subtotal) * 100.0) * 10.0) / 10.0
        else 0.0
    }

    val excedeTope = posConfig.excedeLimitesDescuento(pctCalculado, montoCalculado)
    val puedeAplicar = montoCalculado > 0.0 && montoCalculado < subtotal && !excedeTope

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = null, tint = FDColors.Primary, modifier = Modifier.size(22.dp))
                        Text("Aplicar Descuento", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FDColors.TextTertiary)
                    }
                }

                // Resumen del Subtotal
                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtotal de la venta:", style = FDType.Body, color = FDColors.TextSecondary)
                        Text("$simboloMoneda ${String.format(Locale.US, "%.2f", subtotal)}", style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 15.sp))
                    }
                }

                // Selector de Modo: % Porcentaje o S/ Monto fijo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            if (!modoPorcentaje) {
                                modoPorcentaje = true
                                inputStr = if (pctCalculado > 0.0) String.format(Locale.US, "%.1f", pctCalculado) else ""
                            }
                        },
                        shape = FDShapes.Small,
                        color = if (modoPorcentaje) FDColors.Primary else FDColors.InputBackground,
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Porcentaje (%)",
                                style = FDType.Label.copy(fontWeight = if (modoPorcentaje) FontWeight.Black else FontWeight.Medium),
                                color = if (modoPorcentaje) FDColors.PrimaryText else FDColors.TextSecondary
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (modoPorcentaje) {
                                modoPorcentaje = false
                                inputStr = if (montoCalculado > 0.0) String.format(Locale.US, "%.2f", montoCalculado) else ""
                            }
                        },
                        shape = FDShapes.Small,
                        color = if (!modoPorcentaje) FDColors.Primary else FDColors.InputBackground,
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Monto ($simboloMoneda)",
                                style = FDType.Label.copy(fontWeight = if (!modoPorcentaje) FontWeight.Black else FontWeight.Medium),
                                color = if (!modoPorcentaje) FDColors.PrimaryText else FDColors.TextSecondary
                            )
                        }
                    }
                }

                // Input de Valor
                OutlinedTextField(
                    value = inputStr,
                    onValueChange = { inputStr = it },
                    label = { Text(if (modoPorcentaje) "Porcentaje de descuento (%)" else "Monto a descontar ($simboloMoneda)") },
                    placeholder = { Text(if (modoPorcentaje) "Ej: 5" else "Ej: 10.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                )

                // Equivalencia y Topes de Sede
                Surface(
                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Equivalente:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("- $simboloMoneda ${String.format(Locale.US, "%.2f", montoCalculado)} ($pctCalculado%)", style = FDType.Numeric.copy(fontWeight = FontWeight.Bold, color = if (excedeTope) FDColors.Error else FDColors.Success, fontSize = 12.sp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tope de la sede:", style = FDType.Caption, color = FDColors.TextTertiary)
                            Text("Máx ${posConfig.descuento.maxPct}% o $simboloMoneda ${String.format(Locale.US, "%.2f", posConfig.descuento.maxMonto)}", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                        }
                    }
                }

                // Reglas Fijas = Permiso Directo (Cero Jefes en Cola, R2)
                if (montoCalculado > 0.0) {
                    if (excedeTope) {
                        Surface(
                            color = FDColors.Warning.copy(alpha = 0.1f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        "DESCUENTO FUERA DE TOPE",
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Black),
                                        color = FDColors.Warning
                                    )
                                    Text(
                                        "El descuento supera el tope de la sede (${posConfig.descuento.maxPct}% o $simboloMoneda ${String.format(Locale.US, "%.2f", posConfig.descuento.maxMonto)}). Ajusta el monto o edita los topes en Configuración > Ventas/POS.",
                                        style = FDType.Caption,
                                        color = FDColors.TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = FDColors.Success.copy(alpha = 0.08f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FDColors.Success, modifier = Modifier.size(18.dp))
                                Text(
                                    "Dentro del tope de la sede. Se aplica directamente por el cajero.",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = FDColors.Success
                                )
                            }
                        }
                    }
                }

                // Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.descuento > 0.0) {
                        OutlinedButton(
                            onClick = { onAplicar(0.0, null) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = FDShapes.Small
                        ) {
                            Text("Quitar", color = FDColors.Error)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = FDShapes.Small
                    ) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }

                    Button(
                        onClick = { onAplicar(montoCalculado, null) },
                        enabled = puedeAplicar,
                        modifier = Modifier.weight(1.3f).height(42.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("APLICAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistAperturaBanner(
    checklist: ChecklistAperturaSede,
    excluirItemCaja: Boolean = false,
    onNavigate: ((String) -> Unit)? = null
) {
    var expandido by remember { mutableStateOf(false) }
    val items = remember(checklist, excluirItemCaja) {
        val todos = checklist.obtenerItems()
        if (excluirItemCaja) todos.filter { it.clave != "CAJA" } else todos
    }
    val pendientes = remember(items) { items.filter { !it.completado } }

    // Si la caja de jornada anterior ya se muestra arriba y no hay otros requisitos faltantes, no duplicamos el banner
    if (pendientes.isEmpty()) {
        return
    }

    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(FDColors.Warning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = FDColors.Warning, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "APERTURA DE SEDE PENDIENTE",
                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                color = FDColors.Warning
                            )
                            Surface(
                                shape = CircleShape,
                                color = FDColors.Warning.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${checklist.totalCompletados}/${checklist.totalRequisitos} listos",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Black, fontSize = 11.sp, color = FDColors.Warning),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "El punto de venta no puede cobrar hasta que se completen formalmente estos requisitos.",
                            style = FDType.Caption.copy(color = FDColors.TextSecondary)
                        )
                    }
                }

                TextButton(onClick = { expandido = !expandido }) {
                    Text(
                        if (expandido) "Ocultar detalle ▲" else "Ver checklist (${pendientes.size} faltantes) ▼",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary)
                    )
                }
            }

            // Barra de progreso visual
            val progreso = checklist.totalCompletados.toFloat() / checklist.totalRequisitos.toFloat()
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (checklist.todoListo) FDColors.Success else FDColors.Warning,
                trackColor = FDColors.InputBackground
            )

            // Resumen de ítems
            val itemsAMostrar = if (expandido) items else pendientes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsAMostrar.forEach { item ->
                    Surface(
                        color = if (item.completado) FDColors.Success.copy(alpha = 0.05f) else FDColors.InputBackground.copy(alpha = 0.5f),
                        shape = FDShapes.Small,
                        border = BorderStroke(
                            1.dp,
                            if (item.completado) FDColors.Success.copy(alpha = 0.2f) else FDColors.Warning.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (item.completado) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (item.completado) FDColors.Success else FDColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        item.titulo,
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FDColors.TextPrimary)
                                    )
                                    Text(
                                        item.detalle,
                                        style = FDType.Caption.copy(fontSize = 11.sp, color = if (item.completado) FDColors.Success else FDColors.TextSecondary)
                                    )
                                }
                            }

                            if (!item.completado && item.rutaNavegacion != null && item.textoAccion != null && onNavigate != null) {
                                Button(
                                    onClick = { onNavigate(item.rutaNavegacion) },
                                    shape = FDShapes.Small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FDColors.Primary,
                                        contentColor = FDColors.PrimaryText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        item.textoAccion,
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText)
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

@Composable
private fun SelectorClientePOSInline(
    cliente: ClienteDeVenta,
    consultando: Boolean,
    resultadoDoc: ResultadoDocUi?,
    onConsultarDoc: (String) -> Unit,
    onAplicarResultado: (ResultadoDocUi.Encontrado) -> Unit,
    onLimpiarResultado: () -> Unit,
    onLimpiarCliente: () -> Unit,
    onAbrirManual: () -> Unit
) {
    var inputDoc by remember { mutableStateOf(cliente.numeroDocumento) }

    // Sincronizar input si el cliente asignado cambia externamente
    LaunchedEffect(cliente.numeroDocumento) {
        if (inputDoc != cliente.numeroDocumento) {
            inputDoc = cliente.numeroDocumento
        }
    }

    // Debounce estricto de 800ms tras dejar de escribir
    LaunchedEffect(inputDoc) {
        val numLimpio = inputDoc.filter { it.isDigit() }.trim()
        if (numLimpio == cliente.numeroDocumento && numLimpio.isNotBlank()) {
            return@LaunchedEffect
        }
        if (numLimpio.length == 8 || numLimpio.length == 11) {
            delay(800L) // 800ms de inactividad
            onConsultarDoc(numLimpio)
        } else if (numLimpio.isBlank()) {
            onLimpiarResultado()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLIENTE / FACTURACIÓN",
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = FDColors.TextTertiary
            )
            if (cliente.numeroDocumento.isNotBlank()) {
                Surface(
                    color = if (cliente.tipoDocumento == "RUC") FDColors.Warning.copy(alpha = 0.15f) else FDColors.Success.copy(alpha = 0.15f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (cliente.tipoDocumento == "RUC") "FACTURA ELECTRÓNICA" else "BOLETA ELECTRÓNICA",
                        style = FDType.Caption.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (cliente.tipoDocumento == "RUC") FDColors.Warning else FDColors.Success
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    "CONSUMIDOR FINAL",
                    style = FDType.Caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.TextTertiary
                    )
                )
            }
        }

        if (cliente.numeroDocumento.isNotBlank()) {
            // Cliente ya asignado y confirmado a la venta: Tarjeta limpia
            Surface(
                color = FDColors.Primary.copy(alpha = 0.06f),
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(FDColors.Primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (cliente.tipoDocumento == "RUC") Icons.Default.Business else Icons.Default.Person,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            cliente.nombre,
                            style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = FDColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${cliente.tipoDocumento}: ${cliente.numeroDocumento}",
                            style = FDType.Caption.copy(fontSize = 11.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            inputDoc = ""
                            onLimpiarResultado()
                            onLimpiarCliente()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Quitar cliente",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Input autónomo (no se bloquea ni se mezcla con el resultado)
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.6f),
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = FDColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = inputDoc,
                        onValueChange = { nuevo ->
                            val soloDigitos = nuevo.filter { it.isDigit() }.take(11)
                            inputDoc = soloDigitos
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = FDType.Body.copy(
                            color = FDColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (inputDoc.isBlank()) {
                                Text(
                                    "DNI (8 dígitos) o RUC (11 dígitos)",
                                    style = FDType.Caption.copy(
                                        color = FDColors.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (inputDoc.isNotBlank()) {
                        IconButton(
                            onClick = {
                                inputDoc = ""
                                onLimpiarResultado()
                                onLimpiarCliente()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar campo",
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onAbrirManual,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "Llenar manual",
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Resultado separado que nace debajo del input (Buscando, Éxito, Error)
            AnimatedVisibility(
                visible = consultando || resultadoDoc != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                when {
                    consultando -> {
                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.06f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = FDColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Consultando padrón oficial (RENIEC / SUNAT)...",
                                    style = FDType.Caption.copy(
                                        fontSize = 11.5.sp,
                                        color = FDColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    resultadoDoc is ResultadoDocUi.Encontrado -> {
                        Surface(
                            color = FDColors.Success.copy(alpha = 0.08f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(FDColors.Success.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (resultadoDoc.tipo == "RUC") Icons.Default.Business else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FDColors.Success,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = resultadoDoc.nombreCompleto,
                                            style = FDType.Body.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = FDColors.TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = FDColors.Success.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${resultadoDoc.tipo} ${resultadoDoc.numero}",
                                                    style = FDType.Caption.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FDColors.Success
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = if (resultadoDoc.esDeDirectorio) "Directorio Local" else "Verificado",
                                                style = FDType.Caption.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = FDColors.TextSecondary
                                            )
                                        }
                                        if (resultadoDoc.direccion.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = resultadoDoc.direccion,
                                                style = FDType.Caption.copy(fontSize = 10.sp),
                                                color = FDColors.TextTertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onAplicarResultado(resultadoDoc) },
                                    shape = FDShapes.Small,
                                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Success),
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Aplicar Cliente a la Venta",
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                    resultadoDoc is ResultadoDocUi.Error -> {
                        Surface(
                            color = FDColors.Error.copy(alpha = 0.08f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = FDColors.Error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No se pudo identificar:",
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = FDColors.Error
                                        )
                                    )
                                    Text(
                                        text = resultadoDoc.mensaje,
                                        style = FDType.Caption.copy(
                                            fontSize = 10.5.sp,
                                            color = FDColors.TextSecondary
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = onAbrirManual,
                                    shape = FDShapes.Small,
                                    border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        "Llenar Manual",
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = FDColors.Error
                                        )
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


