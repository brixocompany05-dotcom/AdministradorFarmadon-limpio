package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import com.app.administradorfarmadon.inventario.compartido.modelo.stockFisicoTotalUnidades
import com.app.administradorfarmadon.inventario.compartido.modelo.calcularStockMaximoPresentacion
import com.app.administradorfarmadon.inventario.compartido.modelo.validarDisponibilidadVenta
import com.app.administradorfarmadon.inventario.compartido.ui.LectorCodigoBarrasCamaraDialog
import com.app.administradorfarmadon.ventas.compartido.logica.MontoFormateador
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemChecklistApertura
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import com.app.administradorfarmadon.ventas.compartido.ui.*
import com.app.administradorfarmadon.ventas.nuevaventa.logica.DisponibilidadItemCarrito
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaUiState
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaViewModel
import com.app.administradorfarmadon.ventas.nuevaventa.logica.OrigenAlertaProducto
import com.app.administradorfarmadon.ventas.nuevaventa.logica.ResultadoDocUi
import com.app.administradorfarmadon.ventas.nuevaventa.logica.SugerenciaPresentacion
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
    val esTurnoVencido = uiState.estadoCaja.esTurnoVencido
    val esCajaCerrada = uiState.estadoCaja.estado != com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion.ESTADO_ABIERTA
    // Mostrador real: sin turno abierto no se toca nada. Recién al abrir se puede buscar, armar y cobrar.
    val bloqueoMostrador = esTurnoVencido || esCajaCerrada
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequesterBuscador = remember { FocusRequester() }
    var mostrarCamaraEscaneo by remember { mutableStateOf(false) }
    var itemParaEditarCantidad by remember { mutableStateOf<ItemVenta?>(null) }
    var origenEdicionCantidad by remember { mutableStateOf(OrigenAlertaProducto.CARRITO) }
    var itemParaCambiarPresentacion by remember { mutableStateOf<Pair<ItemVenta, List<SugerenciaPresentacion>>?>(null) }
    var mostrarResultadosBusqueda by remember { mutableStateOf(false) }

    // Sincronizar apertura del desplegable de búsqueda con el texto escrito
    LaunchedEffect(uiState.busquedaTexto) {
        mostrarResultadosBusqueda = uiState.busquedaTexto.isNotBlank()
    }

    // Gestor de toques fuera del desplegable de búsqueda:
    // Si el usuario toca el carrito (ítems, stepper, scroll), el pie o el panel derecho (liquidación, cobrar, cliente),
    // el desplegable se oculta al instante en el pase inicial sin consumir el toque del elemento seleccionado.
    val ocultarBuscadorAlTocar = Modifier.pointerInput(mostrarResultadosBusqueda) {
        if (!mostrarResultadosBusqueda) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            mostrarResultadosBusqueda = false
            focusManager.clearFocus()
        }
    }

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
        uiState.ventaPendienteReanudacion,
        uiState.ventaExitosa,
        mostrarCamaraEscaneo,
        itemParaEditarCantidad,
        itemParaCambiarPresentacion
    ) {
        if (!uiState.mostrarOverlayCobro &&
            !uiState.mostrarDialogoCliente &&
            !uiState.mostrarDialogoSuspender &&
            !uiState.mostrarSheetSuspendidas &&
            uiState.ventaPendienteReanudacion == null &&
            uiState.ventaExitosa == null &&
            !mostrarCamaraEscaneo &&
            itemParaEditarCantidad == null &&
            itemParaCambiarPresentacion == null
        ) {
            delay(100)
            try { focusRequesterBuscador.requestFocus() } catch (_: Exception) {}
        } else {
            mostrarResultadosBusqueda = false
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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Puerta Única: Checklist de Apertura de Sede (R1/R3/R8/R12)
            if (uiState.checklistCargado && !uiState.checklist.todoListo) {
                ChecklistAperturaBanner(
                    checklist = uiState.checklist,
                    excluirItemCaja = esTurnoVencido,
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Buscador de Productos con Foco Permanente y Enter Inmediato (FASE 10)
                            FDSearchField(
                                busqueda = uiState.busquedaTexto,
                                onBusquedaChange = {
                                    viewModel.onBusquedaChange(it)
                                    mostrarResultadosBusqueda = it.isNotBlank()
                                },
                                enabled = !bloqueoMostrador,
                                placeholder = if (esTurnoVencido) "Turno vencido: búsqueda deshabilitada..." else if (esCajaCerrada) "Abre caja para buscar y vender..." else "Buscar producto por nombre o código de barras...",
                                focusRequester = focusRequesterBuscador,
                                onEnterPressed = {
                                    if (uiState.busquedaTexto.isNotBlank()) {
                                        viewModel.ejecutarBusquedaInmediata()
                                    } else if (uiState.carrito.isNotEmpty()) {
                                        viewModel.abrirOverlayCobro()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .pointerInput(uiState.busquedaTexto, uiState.resultadosBusqueda) {
                                        awaitEachGesture {
                                            awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                            if (uiState.busquedaTexto.isNotBlank() && uiState.resultadosBusqueda.isNotEmpty()) {
                                                mostrarResultadosBusqueda = true
                                            }
                                        }
                                    },
                                trailingContent = {
                                    if (uiState.buscando) {
                                        CircularProgressIndicator(
                                            color = FDColors.Primary,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        // CRÍTICO 3: Botón para abrir el escáner real de cámara
                                        IconButton(
                                            onClick = { if (!bloqueoMostrador) mostrarCamaraEscaneo = true },
                                            enabled = !bloqueoMostrador
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                "Escanear con cámara",
                                                tint = if (!bloqueoMostrador) FDColors.Primary else FDColors.TextTertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                            // Lista de Ítems en Carrito
                            Box(modifier = Modifier.weight(1f).then(ocultarBuscadorAlTocar)) {
                            if (esCajaCerrada && !esTurnoVencido) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = FDColors.InputBackground,
                                        shape = CircleShape,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = FDColors.TextSecondary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "CAJA CERRADA · MOSTRADOR BLOQUEADO",
                                        style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                                        color = FDColors.TextPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Para cuidar el dinero y el stock, sin caja abierta no se puede buscar, armar carrito ni cobrar.\nAbre tu turno y recién ahí empieza a vender.",
                                        style = FDType.Body.copy(textAlign = TextAlign.Center),
                                        color = FDColors.TextSecondary,
                                        modifier = Modifier.widthIn(max = 460.dp)
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = { onNavigate?.invoke("CIERRE DE CAJA") },
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("ABRIR CAJA PARA VENDER", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                    }
                                }
                            } else if (esTurnoVencido) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = FDColors.WarningSubtle,
                                        shape = CircleShape,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.LockClock,
                                                contentDescription = null,
                                                tint = FDColors.Warning,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "BLOQUEO OPERATIVO · TURNO VENCIDO",
                                        style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                                        color = FDColors.Warning
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Existe un turno de caja abierto de una fecha anterior (${uiState.estadoCaja.fechaAperturaLegible()}).\nPor integridad contable y de inventario, no se permite vender ni agregar productos al carrito.\nLa única acción permitida es resolver el cierre del turno pendiente.",
                                        style = FDType.Body.copy(textAlign = TextAlign.Center),
                                        color = FDColors.TextSecondary,
                                        modifier = Modifier.widthIn(max = 460.dp)
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = { onNavigate?.invoke("CIERRE DE CAJA") },
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("RESOLVER CIERRE DE TURNO", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                    }
                                }
                            } else if (uiState.carrito.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    POSEmptyState(
                                        icono = Icons.Default.ShoppingCart,
                                        titulo = "Carrito Vacío",
                                        subtitulo = "Busca productos por nombre o escanea con el lector/cámara para agregarlos al carrito."
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Cabecera del Carrito: CARRITO · X productos (Propuesta unificada)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "CARRITO",
                                            style = FDType.Label.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                letterSpacing = 1.sp
                                            ),
                                            color = FDColors.TextSecondary
                                        )
                                        Text(
                                            text = buildString {
                                                append("${uiState.totalItems} ${if (uiState.totalItems == 1) "producto" else "productos"}")
                                                if (uiState.requiereReceta) {
                                                    append(" · receta ${uiState.recetaVerificados}/${uiState.recetaRequeridos}")
                                                }
                                            },
                                            style = FDType.Caption.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.requiereReceta && !uiState.recetaTodoVerificado) FDColors.Warning else FDColors.TextTertiary
                                            )
                                        )
                                    }
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 1.dp)

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.carrito,
                                            key = { _, item -> "${item.productoId}_${item.presentacionId}" }
                                        ) { index, item ->
                                            val clave = "${item.productoId}_${item.presentacionId}"
                                            val disp = uiState.mapaDisponibilidad[clave] ?: DisponibilidadItemCarrito.Disponible()
                                            val alertaItem = if (
                                                uiState.alertaItemProducto?.productoId == item.productoId &&
                                                uiState.alertaItemProducto?.presentacionId == item.presentacionId &&
                                                uiState.alertaItemProducto?.origen == OrigenAlertaProducto.CARRITO
                                            ) {
                                                uiState.alertaItemProducto
                                            } else null

                                            FilaCarritoItem(
                                                item = item,
                                                disponibilidad = disp,
                                                simbolo = simboloMoneda,
                                                alertaFlotante = alertaItem?.mensaje,
                                                alertaTimestamp = alertaItem?.timestamp ?: 0L,
                                                onSumar = { viewModel.cambiarCantidadItem(item.productoId, item.presentacionId, 1, OrigenAlertaProducto.CARRITO) },
                                                onRestar = { viewModel.cambiarCantidadItem(item.productoId, item.presentacionId, -1, OrigenAlertaProducto.CARRITO) },
                                                onClickCantidad = {
                                                    origenEdicionCantidad = OrigenAlertaProducto.CARRITO
                                                    itemParaEditarCantidad = item
                                                },
                                                onSolicitarCambiarPresentacion = {
                                                    if (disp is DisponibilidadItemCarrito.PresentacionAgotada) {
                                                        itemParaCambiarPresentacion = Pair(item, disp.sugerencias)
                                                    }
                                                },
                                                onEliminar = { viewModel.eliminarItemCarrito(item.productoId, item.presentacionId) },
                                                onDismissAlerta = { viewModel.limpiarAlertaProducto() },
                                                onToggleReceta = { ver -> viewModel.setRecetaItemVerificada(item.productoId, item.presentacionId, ver) }
                                            )
                                            if (index < uiState.carrito.size - 1) {
                                                HorizontalDivider(
                                                    color = FDColors.Border.copy(alpha = 0.4f),
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(horizontal = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Footer Panel Izquierdo: Acciones de Gestión de Carrito (oculto si el mostrador está bloqueado)
                        if (!bloqueoMostrador) {
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(ocultarBuscadorAlTocar)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val operacionFooterEnCurso = uiState.suspendiendoVenta || uiState.reanudandoVentaId != null || uiState.vaciandoCarrito || uiState.procesandoCobro

                                    OutlinedButton(
                                        onClick = { viewModel.suspenderVentaDirecta() },
                                        enabled = uiState.carrito.isNotEmpty() && !operacionFooterEnCurso,
                                        modifier = Modifier.height(42.dp),
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            disabledContentColor = if (uiState.suspendiendoVenta) FDColors.Primary else FDColors.TextTertiary
                                        ),
                                        border = BorderStroke(1.dp, if (uiState.suspendiendoVenta) FDColors.Primary else FDColors.Border)
                                    ) {
                                        if (uiState.suspendiendoVenta) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FDColors.Primary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Pausando...", color = FDColors.Primary, style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                        } else {
                                            Icon(Icons.Default.PauseCircle, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Suspender", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.abrirSheetSuspendidas() },
                                        enabled = !operacionFooterEnCurso,
                                        modifier = Modifier.height(42.dp),
                                        shape = FDShapes.Small,
                                        border = BorderStroke(1.dp, FDColors.Border)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Suspendidas (${uiState.ventasSuspendidas.size})", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            mostrarResultadosBusqueda = false
                                            viewModel.vaciarCarrito()
                                        },
                                        enabled = uiState.carrito.isNotEmpty() && !operacionFooterEnCurso,
                                        modifier = Modifier.height(42.dp),
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = FDColors.TextPrimary,
                                            disabledContentColor = FDColors.TextTertiary
                                        ),
                                        border = BorderStroke(1.dp, FDColors.Border)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (uiState.carrito.isNotEmpty() && !operacionFooterEnCurso) FDColors.TextPrimary else FDColors.TextTertiary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Vaciar", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    // Desplegable Flotante de Resultados de Búsqueda (Overlay elevado sobre el carrito)
                    if (!bloqueoMostrador && mostrarResultadosBusqueda && uiState.busquedaTexto.isNotBlank() && uiState.resultadosBusqueda.isNotEmpty()) {
                        val itemsBusqueda = remember(uiState.resultadosBusqueda) {
                            uiState.resultadosBusqueda.flatMap { prod ->
                                val presentaciones = if (prod.presentaciones.isNotEmpty()) {
                                    prod.presentaciones
                                } else {
                                    listOf(
                                        PresentacionProducto(
                                            presentacionId = "PRES_PRINCIPAL",
                                            nombre = prod.empaque.ifBlank { "Unidad" },
                                            empaque = prod.empaque.ifBlank { "Unidad" },
                                            cantidad = 1,
                                            unidadMedida = prod.unidadBase.ifBlank { "unidad" },
                                            precioventa = prod.precioVenta,
                                            codigoBarras = prod.codigo
                                        )
                                    )
                                }
                                presentaciones.map { pres -> prod to pres }
                            }
                        }

                        Surface(
                            color = FDColors.SurfaceElevated,
                            border = BorderStroke(1.dp, FDColors.Border),
                            shape = FDShapes.Medium,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .padding(horizontal = 14.dp)
                                .padding(top = 60.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FDColors.InputBackground.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Resultados encontrados (${itemsBusqueda.size})",
                                            style = FDType.Caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FDColors.TextSecondary
                                            )
                                        )
                                        if (uiState.busquedaTruncada && uiState.resultadosBusqueda.isNotEmpty()) {
                                            Text(
                                                text = "Hay más coincidencias: escribe más letras para afinar.",
                                                style = FDType.Caption.copy(fontSize = 10.5.sp),
                                                color = FDColors.Warning
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            mostrarResultadosBusqueda = false
                                            viewModel.limpiarBusqueda()
                                            focusManager.clearFocus()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = FDColors.Primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Listo / Cerrar",
                                            style = FDType.Caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FDColors.Primary
                                            )
                                        )
                                    }
                                }
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                    ) {
                                    itemsIndexed(itemsBusqueda, key = { _, (prod, pres) -> "${prod.indice}_${pres.presentacionId}" }) { index, (prod, pres) ->
                                        val alertaItem = if (
                                            uiState.alertaItemProducto?.productoId == prod.indice &&
                                            uiState.alertaItemProducto?.presentacionId == pres.presentacionId &&
                                            uiState.alertaItemProducto?.origen == OrigenAlertaProducto.BUSCADOR
                                        ) {
                                            uiState.alertaItemProducto
                                        } else null

                                        FilaProductoBusqueda(
                                            producto = prod,
                                            presentacion = pres,
                                            simboloMoneda = simboloMoneda,
                                            carrito = uiState.carrito,
                                            alertaFlotante = alertaItem?.mensaje,
                                            alertaTimestamp = alertaItem?.timestamp ?: 0L,
                                            onSumar = {
                                                val enCarrito = uiState.carrito.any { it.productoId == prod.indice && it.presentacionId == pres.presentacionId }
                                                if (enCarrito) {
                                                    viewModel.cambiarCantidadItem(prod.indice, pres.presentacionId, 1, OrigenAlertaProducto.BUSCADOR)
                                                } else {
                                                    viewModel.agregarAlCarrito(prod, pres, 1, OrigenAlertaProducto.BUSCADOR)
                                                }
                                            },
                                            onRestar = {
                                                viewModel.cambiarCantidadItem(prod.indice, pres.presentacionId, -1, OrigenAlertaProducto.BUSCADOR)
                                            },
                                            onClickCantidad = {
                                                origenEdicionCantidad = OrigenAlertaProducto.BUSCADOR
                                                val itemExistente = uiState.carrito.firstOrNull { it.productoId == prod.indice && it.presentacionId == pres.presentacionId }
                                                if (itemExistente != null) {
                                                    itemParaEditarCantidad = itemExistente
                                                } else {
                                                    itemParaEditarCantidad = ItemVenta(
                                                        productoId = prod.indice,
                                                        presentacionId = pres.presentacionId,
                                                        nombreProducto = prod.nombre,
                                                        presentacionNombre = pres.nombre,
                                                        precioUnitario = pres.precioventa,
                                                        cantidad = 1,
                                                        subtotal = pres.precioventa
                                                    )
                                                }
                                            },
                                            onBloqueoClick = { motivo ->
                                                viewModel.notificarAlertaProducto(prod.indice, pres.presentacionId, motivo, OrigenAlertaProducto.BUSCADOR)
                                            },
                                            onDismissAlerta = { viewModel.limpiarAlertaProducto() }
                                        )
                                        if (index < itemsBusqueda.size - 1) {
                                            HorizontalDivider(
                                                color = FDColors.Border.copy(alpha = 0.35f),
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ───────────────────────────── PANEL DERECHO: LIQUIDACIÓN OPERACIONAL (40%) ─────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .then(ocultarBuscadorAlTocar)
                ) {
                    Surface(
                        color = FDColors.Surface,
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Border),
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (bloqueoMostrador) {
                                    Modifier
                                        .blur(14.dp)
                                        .alpha(0.15f)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Spacer(Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = simboloMoneda,
                                    style = FDType.Numeric.copy(fontSize = 18.sp, color = FDColors.TextSecondary)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2f", uiState.total),
                                    style = FDType.Numeric.copy(fontSize = 36.sp, fontWeight = FontWeight.Black),
                                    color = FDColors.TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.6f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${uiState.totalItems} PRODUCTO${if (uiState.totalItems == 1) "" else "S"}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                                    style = FDType.Label.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FDColors.TextSecondary
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                        // 3. CLIENTE Y FACTURACIÓN (Inline)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectorClientePOSInline(
                                cliente = uiState.cliente,
                                consultando = uiState.consultandoDoc,
                                resultadoDoc = uiState.resultadoConsultaDoc,
                                esClienteRegistrado = uiState.cliente.numeroDocumento.isNotBlank() &&
                                    uiState.directorioClientes.any {
                                        it.numeroDocumento.filter { c -> c.isDigit() } == uiState.cliente.numeroDocumento.filter { c -> c.isDigit() }
                                    },
                                onConsultarDoc = { doc -> viewModel.consultarDocumentoAuto(doc) },
                                onAplicarResultado = { res -> viewModel.aplicarResultadoCliente(res) },
                                onLimpiarResultado = { viewModel.limpiarResultadoConsultaDoc() },
                                onLimpiarCliente = { viewModel.limpiarCliente() },
                                onAbrirManual = { viewModel.abrirDialogoCliente() }
                            )

                            // Desglose Financiero
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.35f),
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subtotal:", style = FDType.Body.copy(fontSize = 12.sp), color = FDColors.TextSecondary)
                                        Text(
                                            "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.subtotal)}",
                                            style = FDType.Numeric.copy(fontSize = 12.5.sp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.clickable { if (uiState.carrito.isNotEmpty()) viewModel.abrirDialogoDescuento() }
                                        ) {
                                            Text(
                                                "Descuento ${if (uiState.subtotal > 0) String.format(Locale.US, "%.0f", (uiState.descuento / uiState.subtotal) * 100) else "0"}%:",
                                                style = FDType.Body.copy(fontSize = 12.sp, color = FDColors.TextSecondary)
                                            )
                                            Text(
                                                "Aplicar",
                                                style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = FDColors.Primary)
                                            )
                                        }
                                        Text(
                                            "- $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.descuento)}",
                                            style = FDType.Numeric.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary)
                                        )
                                    }

                                    if (uiState.descuentoInvalido) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "El descuento supera al subtotal. Deja total en S/ 0.00.",
                                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.Error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = { viewModel.abrirDialogoDescuento() }) {
                                                Text("Corregir", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.Primary)
                                            }
                                        }
                                    } else if (uiState.descuentoExcedeTope) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Supera el tope (${uiState.posConfig.descuento.maxPct}% / S/ ${String.format(Locale.US, "%.2f", uiState.posConfig.descuento.maxMonto)}).",
                                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.Error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = { viewModel.ajustarDescuentoAlTope() }) {
                                                Text("Ajustar", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.Primary)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total:", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                                        Text(
                                            "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)}",
                                            style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, color = FDColors.Primary)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. MEDIO DE PAGO (Pestañas + Panel con Muesca)
                        PanelMedioPagoInline(
                            uiState = uiState,
                            simboloMoneda = simboloMoneda,
                            onActualizarLineas = { viewModel.actualizarLineasPago(it) }
                        )
                    }

                    // 5. Botón Operacional de Cobro FIJO ABAJO (fuera del scroll)
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Surface(
                        color = FDColors.Surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            val textoBoton = when {
                                uiState.carrito.isEmpty() -> "COBRAR"
                                !uiState.checklist.todoListo -> "APERTURA PENDIENTE"
                                uiState.descuentoInvalido -> "DESCUENTO INVÁLIDO"
                                uiState.descuentoExcedeTope -> "DESCUENTO EXCEDE TOPE"
                                uiState.hayProblemasDisponibilidad -> "STOCK NO DISPONIBLE"
                                !uiState.recetaTodoVerificado -> "VERIFICAR RECETA EN CARRITO"
                                !uiState.todosMetodosActivos -> "MÉTODO DE PAGO INACTIVO"
                                uiState.montoFaltante > 0.009 -> "FALTA CUBRIR $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoFaltante)}"
                                uiState.vuelto > 0.0 -> "COBRAR · VUELTO $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.vuelto)}"
                                else -> "COBRAR $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)}"
                            }

                            val iconoBoton = when {
                                uiState.puedeCobrar && !bloqueoMostrador -> Icons.Default.CheckCircle
                                else -> Icons.Default.Lock
                            }

                            val botonHabilitado = uiState.puedeCobrar && !bloqueoMostrador && !uiState.procesandoCobro && !uiState.suspendiendoVenta && uiState.reanudandoVentaId == null && !uiState.vaciandoCarrito
                            val containerColor = if (botonHabilitado) FDColors.Success else FDColors.InputBackground.copy(alpha = 0.6f)

                            Button(
                                onClick = { viewModel.confirmarVenta() },
                                enabled = botonHabilitado,
                                shape = FDShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    contentColor = Color.White,
                                    disabledContainerColor = FDColors.InputBackground.copy(alpha = 0.6f),
                                    disabledContentColor = FDColors.TextTertiary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (uiState.procesandoCobro) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("PROCESANDO VENTA...", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                                } else {
                                    Icon(
                                        iconoBoton,
                                        contentDescription = null,
                                        tint = if (botonHabilitado) Color.White else FDColors.TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = textoBoton,
                                        style = FDType.Label.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = if (botonHabilitado) Color.White else FDColors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Si el mostrador está bloqueado (caja cerrada o turno vencido): panel derecho no interactivo (R1/R3/R14)
                if (bloqueoMostrador) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FDColors.Surface.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = true,
                                onClick = {} // Intercepta clics para blindar el panel derecho
                            )
                    )
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
        itemParaEditarCantidad?.let { itemOriginal ->
            val itemVivo = uiState.carrito.firstOrNull {
                it.productoId == itemOriginal.productoId && it.presentacionId == itemOriginal.presentacionId
            } ?: itemOriginal
            val clave = "${itemVivo.productoId}_${itemVivo.presentacionId}"
            val disp = uiState.mapaDisponibilidad[clave]
            val prodMolde = uiState.resultadosBusqueda.firstOrNull { it.indice == itemVivo.productoId }
            val stockMax = if (disp is DisponibilidadItemCarrito.Disponible && disp.stockDisponible != Int.MAX_VALUE) {
                disp.stockDisponible
            } else if (prodMolde != null) {
                val s = prodMolde.calcularStockMaximoPresentacion(itemVivo.presentacionId)
                if (s != Int.MAX_VALUE) s else null
            } else null

            DialogoModificarCantidad(
                item = itemVivo,
                stockMaximo = stockMax,
                simboloMoneda = simboloMoneda,
                onDismiss = { itemParaEditarCantidad = null },
                onConfirmar = { nuevaCant ->
                    viewModel.setCantidadItem(itemVivo.productoId, itemVivo.presentacionId, nuevaCant, origenEdicionCantidad)
                    itemParaEditarCantidad = null
                }
            )
        }

        // Diálogo para Cambiar Presentación Agotada (UX Sobria y Centrada)
        itemParaCambiarPresentacion?.let { (itemOriginal, sugerencias) ->
            val itemVivo = uiState.carrito.firstOrNull {
                it.productoId == itemOriginal.productoId && it.presentacionId == itemOriginal.presentacionId
            } ?: itemOriginal

            DialogoCambiarPresentacion(
                item = itemVivo,
                sugerencias = sugerencias,
                simboloMoneda = simboloMoneda,
                onDismiss = { itemParaCambiarPresentacion = null },
                onSeleccionar = { nuevaPresId ->
                    viewModel.cambiarPresentacionItem(itemVivo.productoId, itemVivo.presentacionId, nuevaPresId)
                    itemParaCambiarPresentacion = null
                }
            )
        }

        // ───────────────────────────── OVERLAYS Y DIÁLOGOS ─────────────────────────────

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

        // Diálogo de Conflicto de Reanudación de Venta Suspendida (R3/R14)
        uiState.ventaPendienteReanudacion?.let { susp ->
            DialogoConflictoReanudacion(
                ventaSuspendida = susp,
                itemsCarritoActual = uiState.totalItems,
                totalCarritoActual = uiState.total,
                simboloMoneda = simboloMoneda,
                estaProcesando = uiState.reanudandoVentaId == susp.id,
                onDismiss = { viewModel.cancelarDialogoReanudacion() },
                onVaciarYReanudar = { viewModel.confirmarReanudacionVaciarYReanudar() },
                onAnadirACarrito = { viewModel.confirmarReanudacionAnadirACarrito() }
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
                    Text(
                        "El precio y el stock reales se confirman al abrir. Nada se pierde al reanudar.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary
                    )
                    uiState.ventasSuspendidas.forEach { susp ->
                        Surface(
                            color = FDColors.InputBackground,
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val tituloCard = if (susp.nota.isNotBlank()) {
                                    susp.nota
                                } else if (susp.cliente.nombre.isNotBlank() &&
                                    !susp.cliente.nombre.equals("Cliente General", ignoreCase = true) &&
                                    !susp.cliente.nombre.equals("Consumidor Final", ignoreCase = true)
                                ) {
                                    susp.cliente.nombre
                                } else {
                                    "Venta en pausa"
                                }
                                val mostrarNotaSeparada = susp.nota.isNotBlank() && tituloCard != susp.nota

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tituloCard,
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", susp.total)}",
                                        style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = FDColors.Primary)
                                    )
                                }
                                Text(
                                    "${susp.items.size} productos · ${susp.creadoPorNombre} · ${com.app.administradorfarmadon.compartido.util.TiempoHumanoUtils.formatearCorto(susp.fechaMs)}",
                                    style = FDType.Caption,
                                    color = FDColors.TextSecondary
                                )
                                if (mostrarNotaSeparada) {
                                    Text("Nota: \"${susp.nota}\"", style = FDType.Caption.copy(color = FDColors.TextTertiary))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val estaBorrandoEste = uiState.descartandoVentaId == susp.id
                                    val estaReanudandoEste = uiState.reanudandoVentaId == susp.id
                                    val operacionEnCurso = uiState.reanudandoVentaId != null || uiState.descartandoVentaId != null || uiState.suspendiendoVenta

                                    OutlinedButton(
                                        onClick = { viewModel.descartarVentaSuspendida(susp.id) },
                                        enabled = !operacionEnCurso,
                                        shape = FDShapes.Small,
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        if (estaBorrandoEste) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = FDColors.Error
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Borrando...", color = FDColors.Error, style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                                        } else {
                                            Text("Eliminar", color = if (operacionEnCurso) FDColors.TextTertiary else FDColors.Error, style = FDType.Caption)
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.solicitarReanudarVenta(susp) },
                                        enabled = !operacionEnCurso,
                                        shape = FDShapes.Small,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FDColors.Primary,
                                            disabledContainerColor = if (estaReanudandoEste) FDColors.Primary else FDColors.InputBackground.copy(alpha = 0.6f),
                                            disabledContentColor = if (estaReanudandoEste) Color.White else FDColors.TextTertiary
                                        ),
                                        modifier = Modifier.weight(1.5f).height(38.dp)
                                    ) {
                                        if (estaReanudandoEste) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Reanudando...", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                        } else {
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
}

// ───────────────────────────── COMPONENTES AUXILIARES ─────────────────────────────

@Composable
private fun PanelMedioPagoInline(
    uiState: NuevaVentaUiState,
    simboloMoneda: String,
    onActualizarLineas: (List<PagoVenta>) -> Unit
) {
    val metodosActivos = uiState.metodosPagoDisponibles

    // Construir la lista dinámica de tipos de pago disponibles para la farmacia
    data class OpcionPagoUi(
        val id: String,
        val nombre: String,
        val icono: ImageVector,
        val instancia: InstanciaPago?
    )

    val opcionesDisponibles = remember(metodosActivos) {
        val lista = mutableListOf<OpcionPagoUi>()
        if (metodosActivos.isNotEmpty()) {
            val agrupados = metodosActivos.groupBy { it.tipoId }
            agrupados.forEach { (tipoId, instancias) ->
                val tipoInfo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == tipoId }
                val inst = instancias.firstOrNull()
                val nombre = tipoInfo?.nombre ?: tipoId
                val icono = tipoInfo?.icono ?: Icons.Outlined.Payments
                lista.add(OpcionPagoUi(id = tipoId, nombre = nombre, icono = icono, instancia = inst))
            }
        }
        lista
    }

    if (opcionesDisponibles.isEmpty()) {
        Surface(
            color = FDColors.WarningSubtle,
            shape = FDShapes.Medium,
            border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = FDColors.Warning,
                    modifier = Modifier.size(22.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Sin métodos de pago en esta sucursal",
                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = "Esta sucursal no tiene métodos de pago activos configurados en Firestore. Configúralos en Configuración > Métodos de Pago para poder cobrar.",
                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                    )
                }
            }
        }
        return
    }

    val permitirPagoMixto = opcionesDisponibles.size > 1
    val idxPagoMixto = if (permitirPagoMixto) opcionesDisponibles.size else -1

    var tabSeleccionado by rememberSaveable(opcionesDisponibles.map { it.id }.joinToString(",")) {
        val primero = uiState.lineasPago.firstOrNull()
        if (permitirPagoMixto && (uiState.lineasPago.size > 1 || (uiState.lineasPago.size == 1 && (primero?.monto ?: 0.0) < uiState.total - 0.009))) {
            mutableIntStateOf(idxPagoMixto)
        } else {
            val idxEncontrado = opcionesDisponibles.indexOfFirst { it.id == primero?.tipoId }
            mutableIntStateOf(if (idxEncontrado >= 0) idxEncontrado else 0)
        }
    }

    LaunchedEffect(opcionesDisponibles, idxPagoMixto) {
        if (tabSeleccionado >= opcionesDisponibles.size && (idxPagoMixto == -1 || tabSeleccionado != idxPagoMixto)) {
            tabSeleccionado = 0
        }
    }

    // Una sola verdad sin sacar al usuario: si está armando el mixto, se queda en mixto
    // aunque lleve 1 sola línea (todavía le falta). Solo sigue cambios externos
    // (método desactivado, reanudación) cuando está en un método simple.
    LaunchedEffect(uiState.lineasPago, opcionesDisponibles, permitirPagoMixto) {
        val lineas = uiState.lineasPago
        if (lineas.isEmpty()) return@LaunchedEffect
        if (permitirPagoMixto && tabSeleccionado == idxPagoMixto) return@LaunchedEffect
        if (lineas.size > 1) {
            if (permitirPagoMixto && tabSeleccionado != idxPagoMixto) tabSeleccionado = idxPagoMixto
        } else {
            val idxReal = opcionesDisponibles.indexOfFirst { it.id == lineas.first().tipoId }
            if (idxReal >= 0 && tabSeleccionado != idxReal && tabSeleccionado < opcionesDisponibles.size) {
                tabSeleccionado = idxReal
            }
        }
    }

    var montoInput by remember(uiState.total, tabSeleccionado) {
        val actual = uiState.lineasPago.firstOrNull()?.monto ?: uiState.total
        mutableStateOf(if (actual > 0.0) String.format(Locale.US, "%.2f", actual) else "")
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "MEDIO DE PAGO",
            style = FDType.Label.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = FDColors.TextTertiary
        )

        // ────── SELECTOR EN CUADRÍCULA VISIBLE (2 columnas, sin scroll horizontal oculto) ──────
        val itemsOpciones: List<Triple<Int, String, ImageVector>> = opcionesDisponibles.mapIndexed { idx, opt ->
            Triple(idx, opt.nombre, opt.icono)
        } + if (permitirPagoMixto) listOf(Triple(idxPagoMixto, "Pago mixto", Icons.Outlined.CompareArrows)) else emptyList()

        val filasOpciones = remember(itemsOpciones) {
            itemsOpciones.chunked(2)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filasOpciones.forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fila.forEach { (idx, titulo, icono) ->
                        val esSel = tabSeleccionado == idx
                        val bgTab = if (esSel) FDColors.Primary else FDColors.InputBackground
                        val fgTab = if (esSel) FDColors.PrimaryText else FDColors.TextSecondary

                        Surface(
                            onClick = {
                                val anteriorTab = tabSeleccionado
                                tabSeleccionado = idx
                                val total = uiState.total
                                if (idx < opcionesDisponibles.size) {
                                    val opt = opcionesDisponibles[idx]
                                    onActualizarLineas(
                                        listOf(
                                            PagoVenta(
                                                tipoId = opt.id,
                                                instanciaId = opt.instancia?.id ?: "",
                                                nombreMetodo = opt.nombre,
                                                monto = total,
                                                numeroOperacion = ""
                                            )
                                        )
                                    )
                                    montoInput = String.format(Locale.US, "%.2f", total)
                                } else if (permitirPagoMixto && idx == idxPagoMixto) {
                                    // Al cambiar a Pago Mixto desde un método único, se limpian las líneas previas
                                    // para que no arrastre el 100% del método anterior y permita desglosar libremente.
                                    if (anteriorTab != idxPagoMixto) {
                                        onActualizarLineas(emptyList())
                                    }
                                }
                            },
                            enabled = !uiState.procesandoCobro,
                            color = bgTab,
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = icono,
                                    contentDescription = null,
                                    tint = fgTab,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = titulo,
                                    style = FDType.Label.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = fgTab,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (fila.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ────── CONTENIDO ESPACIOSO DEL MÉTODO (SIN CAJAS ANIDADAS) ──────
        if (tabSeleccionado < opcionesDisponibles.size) {
            val optSel = opcionesDisponibles[tabSeleccionado]

            if (optSel.id == "EFECTIVO") {
                // ────── EFECTIVO ──────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Fila de Monto Recibido + Botón "Monto Exacto"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = FDColors.InputBackground,
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Border),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = simboloMoneda,
                                    style = FDType.Numeric.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FDColors.TextSecondary)
                                )
                                BasicTextField(
                                    value = montoInput,
                                    enabled = !uiState.procesandoCobro,
                                    onValueChange = { nuevo ->
                                        montoInput = nuevo
                                        val valParsed = MontoFormateador.normalizarMontoEstricto(nuevo) ?: 0.0
                                        onActualizarLineas(
                                            listOf(
                                                PagoVenta(
                                                    tipoId = optSel.id,
                                                    instanciaId = optSel.instancia?.id ?: "",
                                                    nombreMetodo = optSel.nombre,
                                                    monto = valParsed,
                                                    numeroOperacion = ""
                                                )
                                            )
                                        )
                                    },
                                    textStyle = FDType.Numeric.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FDColors.TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (montoInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            montoInput = ""
                                            onActualizarLineas(
                                                listOf(
                                                    PagoVenta(
                                                        tipoId = optSel.id,
                                                        instanciaId = optSel.instancia?.id ?: "",
                                                        nombreMetodo = optSel.nombre,
                                                        monto = 0.0,
                                                        numeroOperacion = ""
                                                    )
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = "Limpiar",
                                            tint = FDColors.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val esMontoExacto = uiState.total > 0 && uiState.vuelto == 0.0 && uiState.montoFaltante <= 0.009
                        OutlinedButton(
                            onClick = {
                                val total = uiState.total
                                montoInput = String.format(Locale.US, "%.2f", total)
                                onActualizarLineas(
                                    listOf(
                                        PagoVenta(
                                            tipoId = optSel.id,
                                            instanciaId = optSel.instancia?.id ?: "",
                                            nombreMetodo = optSel.nombre,
                                            monto = total,
                                            numeroOperacion = ""
                                        )
                                    )
                                )
                            },
                            shape = FDShapes.Small,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (esMontoExacto) FDColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = if (esMontoExacto) FDColors.Primary else FDColors.TextPrimary
                            ),
                            border = BorderStroke(1.dp, if (esMontoExacto) FDColors.Primary else FDColors.Border),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (esMontoExacto) FDColors.Primary else FDColors.TextSecondary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Exacto",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // 2. Billetes Rápidos Cómodos
                    val billetes = listOf("10", "20", "50", "100", "200")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        billetes.forEach { b ->
                            val valBillete = b.toDouble()
                            val esSel = montoInput.trim() == "$b.00" || montoInput.trim() == b
                            Surface(
                                onClick = {
                                    montoInput = "$b.00"
                                    onActualizarLineas(
                                        listOf(
                                            PagoVenta(
                                                tipoId = optSel.id,
                                                instanciaId = optSel.instancia?.id ?: "",
                                                nombreMetodo = optSel.nombre,
                                                monto = valBillete,
                                                numeroOperacion = ""
                                            )
                                        )
                                    )
                                },
                                color = if (esSel) FDColors.Primary.copy(alpha = 0.15f) else FDColors.InputBackground,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "S/ $b",
                                        style = FDType.Label.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (esSel) FontWeight.Black else FontWeight.Bold,
                                            color = if (esSel) FDColors.Primary else FDColors.TextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Ficha de Vuelto de Alto Impacto
                    Surface(
                        color = if (uiState.montoFaltante > 0.009) FDColors.WarningSubtle else if (uiState.vuelto > 0.0) FDColors.SuccessSubtle else FDColors.InputBackground.copy(alpha = 0.4f),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, if (uiState.montoFaltante > 0.009) FDColors.Warning.copy(alpha = 0.4f) else if (uiState.vuelto > 0.0) FDColors.Success.copy(alpha = 0.4f) else FDColors.Border.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.montoFaltante > 0.009) "FALTA RECIBIR" else "VUELTO A ENTREGAR",
                                    style = FDType.Caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.montoFaltante > 0.009) FDColors.Warning else FDColors.TextSecondary
                                    )
                                )
                                Text(
                                    text = if (uiState.montoFaltante > 0.009)
                                        "Faltan $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoFaltante)} para cubrir el total"
                                    else if (uiState.vuelto > 0.0)
                                        "Entregar cambio al cliente"
                                    else
                                        "Monto exacto, sin cambio",
                                    style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.TextTertiary)
                                )
                            }
                            Text(
                                text = if (uiState.montoFaltante > 0.009)
                                    "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoFaltante)}"
                                else
                                    "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.vuelto)}",
                                style = FDType.Numeric.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (uiState.montoFaltante > 0.009) FDColors.Warning else if (uiState.vuelto > 0.0) FDColors.Success else FDColors.TextPrimary
                                )
                            )
                        }
                    }
                }
            } else {
                // ────── MÉTODO DIRECTO (Yape, Plin, Tarjeta, Transferencia): elegir es tocar y listo ──────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sincronizar automáticamente que la línea de pago cubra el 100% del total
                    LaunchedEffect(uiState.total, optSel.id) {
                        val total = uiState.total
                        val lineaActual = uiState.lineasPago.firstOrNull()
                        if (lineaActual == null || lineaActual.tipoId != optSel.id || lineaActual.monto != total) {
                            onActualizarLineas(
                                listOf(
                                    PagoVenta(
                                        tipoId = optSel.id,
                                        instanciaId = optSel.instancia?.id ?: "",
                                        nombreMetodo = optSel.nombre,
                                        monto = total,
                                        numeroOperacion = ""
                                    )
                                )
                            )
                            montoInput = String.format(Locale.US, "%.2f", total)
                        }
                    }

                    // Ficha única y compacta: método + total. Sin cards duplicadas.
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.5f),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = optSel.icono,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = optSel.nombre,
                                style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.total)}",
                                style = FDType.Numeric.copy(fontSize = 17.sp, fontWeight = FontWeight.Black),
                                color = FDColors.Primary
                            )
                        }
                    }

                    // Dónde cobra el cliente (número Yape, cuenta, titular). Esencial, en una línea.
                    val datosMetodo = optSel.instancia?.datos.orEmpty().filterValues { it.isNotBlank() }
                    if (datosMetodo.isNotEmpty()) {
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.BorderStrong),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = datosMetodo.entries.joinToString(" · ") { (k, v) -> "${k.replaceFirstChar { it.uppercase() }}: $v" },
                                    style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ────── PAGO MIXTO DIRECTO ──────
            var metodoNuevoId by remember(opcionesDisponibles, tabSeleccionado) {
                mutableStateOf(opcionesDisponibles.firstOrNull()?.id.orEmpty())
            }
            var montoNuevoInput by remember(tabSeleccionado) { mutableStateOf("") }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Resumen contable: nunca a medias. Exceso sin efectivo e inactivo son error rojo.
                val hayErrorPago = uiState.hayExcesoNoEfectivo || !uiState.todosMetodosActivos
                val colorResumen = if (hayErrorPago) FDColors.Error.copy(alpha = 0.08f) else if (uiState.montoFaltante <= 0.009) FDColors.SuccessSubtle else FDColors.WarningSubtle
                val bordeResumen = if (hayErrorPago) FDColors.Error.copy(alpha = 0.45f) else if (uiState.montoFaltante <= 0.009) FDColors.Success.copy(alpha = 0.4f) else FDColors.Warning.copy(alpha = 0.4f)
                Surface(
                    color = colorResumen,
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, bordeResumen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CUBIERTO", style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextSecondary))
                            Text(
                                "$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.sumaPagos)}",
                                style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val tituloDer = if (hayErrorPago) "REVISAR" else if (uiState.vuelto > 0.0) "VUELTO" else "FALTANTE"
                            val colorDer = if (hayErrorPago) FDColors.Error else if (uiState.vuelto > 0.0) FDColors.Success else if (uiState.montoFaltante > 0) FDColors.Warning else FDColors.TextSecondary
                            val montoDer = if (uiState.hayExcesoNoEfectivo) uiState.excesoNoEfectivo else if (uiState.vuelto > 0.0) uiState.vuelto else uiState.montoFaltante
                            Text(
                                tituloDer,
                                style = FDType.Caption.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorDer
                                )
                            )
                            Text(
                                "$simboloMoneda ${String.format(Locale.US, "%.2f", montoDer)}",
                                style = FDType.Numeric.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorDer
                                )
                            )
                        }
                    }
                }

                // Mensaje contable coherente con lo elegido. Dinero real, sin información rota.
                if (!uiState.todosMetodosActivos) {
                    Text(
                        "Un medio agregado ya no está activo en esta sucursal. Quítalo y elige uno activo.",
                        style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.Error
                    )
                } else if (uiState.hayExcesoNoEfectivo) {
                    Text(
                        "Yape / Plin / Tarjeta no pueden pasar el total (exceso $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.excesoNoEfectivo)}). Cobra lo exacto o combina el resto con efectivo.",
                        style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.Error
                    )
                } else if (uiState.lineasPago.isEmpty()) {
                    Text(
                        "Elige un medio y toca + Añadir. Puedes combinar varios hasta cubrir el total.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary
                    )
                } else if (uiState.montoFaltante > 0.009) {
                    Text(
                        "Faltan $simboloMoneda ${String.format(Locale.US, "%.2f", uiState.montoFaltante)}. Agrega otro medio.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary
                    )
                }

                // 1. Lista de pagos ya agregados
                if (uiState.lineasPago.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.lineasPago.forEachIndexed { idx, p ->
                            val tipoInfo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == p.tipoId }
                            val iconoLinea = tipoInfo?.icono ?: Icons.Outlined.Payments

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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = iconoLinea,
                                            contentDescription = null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = p.nombreMetodo,
                                            style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "$simboloMoneda ${String.format(Locale.US, "%.2f", p.monto)}",
                                            style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                                        )
                                        IconButton(
                                            onClick = {
                                                val l = uiState.lineasPago.toMutableList().apply { removeAt(idx) }
                                                onActualizarLineas(l)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Eliminar línea",
                                                tint = FDColors.TextTertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Formulario amplio de Método y Monto (si falta saldo por cubrir)
                if (uiState.montoFaltante > 0.009 || uiState.lineasPago.isEmpty()) {
                    LaunchedEffect(opcionesDisponibles) {
                        if (opcionesDisponibles.none { it.id == metodoNuevoId }) {
                            metodoNuevoId = opcionesDisponibles.firstOrNull()?.id.orEmpty()
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "PASO 1 · Elige el medio",
                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextTertiary
                        )
                        // Cuadrícula amplia 2 columnas: se lee sin apretujones.
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            opcionesDisponibles.chunked(2).forEach { fila ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    fila.forEach { opt ->
                                        val sel = metodoNuevoId == opt.id
                                        val yaAgregado = uiState.lineasPago.any { it.tipoId == opt.id }
                                        Surface(
                                            onClick = {
                                                metodoNuevoId = opt.id
                                                // Rapidez de mostrador: al tocar el medio ya viene
                                                // escrito lo que falta. Solo toca + Añadir.
                                                val falta = uiState.montoFaltante
                                                montoNuevoInput = String.format(
                                                    Locale.US, "%.2f",
                                                    if (falta > 0.009) falta else uiState.total
                                                )
                                            },
                                            color = if (sel) FDColors.Primary else FDColors.InputBackground,
                                            shape = FDShapes.Small,
                                            border = BorderStroke(
                                                1.dp,
                                                if (sel) FDColors.Primary else if (yaAgregado) FDColors.Success.copy(alpha = 0.5f) else FDColors.Border.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = opt.icono,
                                                        contentDescription = null,
                                                        tint = if (sel) FDColors.PrimaryText else if (yaAgregado) FDColors.Success else FDColors.TextSecondary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    if (yaAgregado) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = if (sel) FDColors.PrimaryText else FDColors.Success,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        opt.nombre,
                                                        style = FDType.Label.copy(
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (sel) FDColors.PrimaryText else FDColors.TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (fila.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        Text(
                            "Si repites un medio, se actualiza su monto, no se duplica.",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextTertiary
                        )
                        Text(
                            "PASO 2 · Pon el monto y añade",
                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextTertiary
                        )

                        // Campo de Monto + Botón de Añadir directo
                        val optSeleccionadaPill = opcionesDisponibles.firstOrNull { it.id == metodoNuevoId } ?: opcionesDisponibles.firstOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FDTextField(
                                value = montoNuevoInput,
                                onValueChange = { montoNuevoInput = it },
                                label = "Monto (${optSeleccionadaPill?.nombre ?: "Pago"})",
                                placeholder = String.format(Locale.US, "%.2f", uiState.montoFaltante),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val m = MontoFormateador.normalizarMontoEstricto(montoNuevoInput) ?: uiState.montoFaltante
                                    val metodoActualId = metodoNuevoId.ifBlank { optSeleccionadaPill?.id.orEmpty() }
                                    if (m > 0.0 && metodoActualId.isNotBlank()) {
                                        val optSel = opcionesDisponibles.firstOrNull { it.id == metodoActualId }
                                            ?: opcionesDisponibles.firstOrNull()
                                        val nuevaLinea = PagoVenta(
                                            tipoId = metodoActualId,
                                            instanciaId = optSel?.instancia?.id ?: "",
                                            nombreMetodo = optSel?.nombre ?: metodoActualId,
                                            monto = m,
                                            numeroOperacion = ""
                                        )
                                        val indexExistente = uiState.lineasPago.indexOfFirst { it.tipoId == metodoActualId }
                                        val lineasActualizadas = if (indexExistente >= 0) {
                                            uiState.lineasPago.toMutableList().apply {
                                                this[indexExistente] = nuevaLinea
                                            }
                                        } else {
                                            uiState.lineasPago + nuevaLinea
                                        }
                                        onActualizarLineas(lineasActualizadas)
                                        montoNuevoInput = ""

                                        val siguienteOpt = opcionesDisponibles.firstOrNull { opt ->
                                            opt.id != metodoActualId && lineasActualizadas.none { it.tipoId == opt.id }
                                        }
                                        if (siguienteOpt != null) {
                                            metodoNuevoId = siguienteOpt.id
                                        }
                                    }
                                },
                                shape = FDShapes.Small,
                                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("+ Añadir", style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp))
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
    stockMaximo: Int? = null,
    simboloMoneda: String,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var cantidadTexto by remember { mutableStateOf("${item.cantidad}") }
    val focusRequester = remember { FocusRequester() }
    val cantNum = cantidadTexto.trim().toIntOrNull() ?: 0
    val excedeMax = stockMaximo != null && cantNum > stockMaximo
    val cantidadValida = cantNum > 0 && !excedeMax

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
                                if (cantidadValida) {
                                    onConfirmar(cantNum)
                                    true
                                } else false
                            } else false
                        }
                )

                if (stockMaximo != null) {
                    Text(
                        text = if (excedeMax) "Supera el stock disponible ($stockMaximo unidades)" else "Stock disponible: $stockMaximo unidades",
                        style = FDType.Caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (excedeMax) FDColors.TextPrimary else FDColors.TextSecondary
                        )
                    )
                }

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
                            if (cantidadValida) onConfirmar(cantNum)
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
private fun GloboAlertaFlotante(
    mensaje: String,
    timestamp: Long = 0L,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.BottomEnd,
    offset: IntOffset = IntOffset(x = -8, y = 4),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(mensaje, timestamp) {
        delay(3200)
        onDismiss()
    }

    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = FDColors.SurfaceElevated,
            border = BorderStroke(1.dp, FDColors.BorderStrong),
            shadowElevation = 10.dp,
            modifier = modifier
                .padding(horizontal = 8.dp)
                .widthIn(min = 200.dp, max = 340.dp)
                .clickable(onClick = onDismiss)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = FDColors.Primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = mensaje,
                    style = FDType.Caption.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FDColors.TextPrimary,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar aviso",
                    tint = FDColors.TextTertiary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onDismiss)
                )
            }
        }
    }
}

/**
 * Diálogo espacioso, cómodo y sobrio para seleccionar una presentación alternativa
 * cuando la elegida previamente se queda sin stock físico (Enterprise SaaS UX).
 */
@Composable
private fun DialogoCambiarPresentacion(
    item: ItemVenta,
    sugerencias: List<SugerenciaPresentacion>,
    simboloMoneda: String,
    onDismiss: () -> Unit,
    onSeleccionar: (String) -> Unit
) {
    var seleccionadaId by remember { mutableStateOf(sugerencias.firstOrNull()?.presentacionId ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.BorderStrong),
            shadowElevation = 12.dp,
            modifier = Modifier.width(460.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Cambiar Presentación",
                            style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                            color = FDColors.TextPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.nombreProducto,
                            style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Presentación previa: ${item.presentacionNombre} (Sin stock)",
                            style = FDType.Caption.copy(fontSize = 11.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 1.dp)

                Text(
                    "Selecciona una de las presentaciones disponibles con stock en farmacia:",
                    style = FDType.Caption.copy(fontSize = 12.sp),
                    color = FDColors.TextSecondary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sugerencias.forEach { sug ->
                        val estaSeleccionada = sug.presentacionId == seleccionadaId
                        Surface(
                            onClick = { seleccionadaId = sug.presentacionId },
                            shape = FDShapes.Small,
                            color = if (estaSeleccionada) FDColors.SurfaceElevated else FDColors.Surface,
                            border = BorderStroke(
                                if (estaSeleccionada) 1.5.dp else 1.dp,
                                if (estaSeleccionada) FDColors.Primary else FDColors.Border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = estaSeleccionada,
                                        onClick = { seleccionadaId = sug.presentacionId },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = FDColors.Primary,
                                            unselectedColor = FDColors.TextTertiary
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = sug.nombre,
                                            style = FDType.Body.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = FDColors.TextPrimary
                                        )
                                        Text(
                                            text = "Stock disponible: ${sug.stockDisponible} unidades",
                                            style = FDType.Caption.copy(fontSize = 11.5.sp),
                                            color = FDColors.TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", sug.precioVenta)}",
                                    style = FDType.Numeric.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (seleccionadaId.isNotBlank()) {
                                onSeleccionar(seleccionadaId)
                            }
                        },
                        enabled = seleccionadaId.isNotBlank(),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(42.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text(
                            "CAMBIAR PRESENTACIÓN",
                            style = FDType.Label.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila de ítem en el carrito con altura fija, uniforme y sobria (Enterprise SaaS).
 * Elimina distorsiones verticales, acordeones y colores alarmistas; ofrece acciones directas
 * de cambio de presentación en modal centrado cuando no hay existencias físicas.
 */
@Composable
private fun FilaCarritoItem(
    item: ItemVenta,
    disponibilidad: DisponibilidadItemCarrito,
    simbolo: String,
    alertaFlotante: String? = null,
    alertaTimestamp: Long = 0L,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onClickCantidad: () -> Unit,
    onSolicitarCambiarPresentacion: () -> Unit,
    onEliminar: () -> Unit,
    onDismissAlerta: () -> Unit = {},
    onToggleReceta: (Boolean) -> Unit = {}
) {
    val partesTrazabilidad = remember(item.loteSugerido, item.loteVencimientoSugerido, item.ubicacionAnaquel) {
        val lista = mutableListOf<String>()
        if (item.loteSugerido.isNotBlank()) {
            lista.add("Lote ${item.loteSugerido.trim()}")
        }
        if (item.loteVencimientoSugerido.isNotBlank()) {
            lista.add("Vence ${item.loteVencimientoSugerido.trim()}")
        }
        val estante = item.ubicacionAnaquel
            .replace(Regex("^(anaquel:?|estante:?)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        if (estante.isNotBlank()) {
            lista.add("Estante $estante")
        }
        lista
    }

    val textoTrazabilidad = remember(partesTrazabilidad) {
        if (partesTrazabilidad.isNotEmpty()) {
            partesTrazabilidad.joinToString("  ·  ")
        } else {
            "Lote y estante por asignar"
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fila 1: Nombre del producto + Botón Quitar discreto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buildString {
                        append(item.nombreProducto)
                        if (item.presentacionNombre.isNotBlank() && !item.nombreProducto.contains(item.presentacionNombre, ignoreCase = true)) {
                            append(" · ")
                            append(item.presentacionNombre)
                        }
                    },
                    style = FDType.Body.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onEliminar,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Quitar del carrito",
                        modifier = Modifier.size(16.dp),
                        tint = FDColors.TextTertiary
                    )
                }
            }

            // Fila 2: Cuadro de Trazabilidad Unificado: Lote · Vence · Estante + Receta Neutral
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = textoTrazabilidad,
                            style = FDType.Caption.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = FDColors.TextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (item.requiereReceta) {
                        val verificada = item.recetaVerificada
                        Surface(
                            onClick = { onToggleReceta(!verificada) },
                            color = if (verificada) FDColors.Success.copy(alpha = 0.14f) else FDColors.Warning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (verificada) FDColors.Success.copy(alpha = 0.55f) else FDColors.Warning.copy(alpha = 0.55f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    if (verificada) Icons.Default.CheckCircle else Icons.Default.MedicalServices,
                                    contentDescription = if (verificada) "Receta verificada, toca para quitar la marca" else "Toca para marcar receta verificada",
                                    tint = if (verificada) FDColors.Success else FDColors.Warning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (verificada) "Verificada" else "Ver receta",
                                    style = FDType.Caption.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (verificada) FDColors.Success else FDColors.Warning
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Fila 3: Controles de cantidad o Acción de sustitución según Disponibilidad
            when (disponibilidad) {
                is DisponibilidadItemCarrito.PresentacionAgotada -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Sin stock",
                                    style = FDType.Caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FDColors.TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onSolicitarCambiarPresentacion,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.BorderStrong),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = FDColors.Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Cambiar presentación",
                                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Text(
                            text = "—",
                            style = FDType.Numeric.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = FDColors.TextTertiary
                        )
                    }
                }

                is DisponibilidadItemCarrito.ProductoAgotado -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Agotado en farmacia",
                                style = FDType.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onEliminar,
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.BorderStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                "Retirar",
                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                is DisponibilidadItemCarrito.Disponible -> {
                    val maxStock = disponibilidad.stockDisponible
                    val puedeSumar = item.cantidad < maxStock

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stepper: −  [cantidad]  +
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Botón Menos [ − ]
                            Surface(
                                onClick = onRestar,
                                shape = CircleShape,
                                color = FDColors.SurfaceElevated,
                                border = BorderStroke(1.dp, FDColors.Border),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Disminuir",
                                        tint = FDColors.TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Cantidad numérica (clic para escribir)
                            Surface(
                                onClick = onClickCantidad,
                                shape = RoundedCornerShape(6.dp),
                                color = FDColors.InputBackground.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .widthIn(min = 40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "${item.cantidad}",
                                        style = FDType.Numeric.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }

                            // Botón Más [ + ]
                            Surface(
                                onClick = onSumar,
                                enabled = puedeSumar,
                                shape = CircleShape,
                                color = if (puedeSumar) FDColors.Primary else FDColors.InputBackground.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (puedeSumar) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Aumentar",
                                        tint = if (puedeSumar) Color.White else FDColors.TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (maxStock != Int.MAX_VALUE) {
                                Text(
                                    text = "Stock: $maxStock",
                                    style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Medium)
                                )
                            } else if (item.cantidad > 1) {
                                Text(
                                    text = "($simbolo ${String.format(Locale.US, "%.2f", item.precioUnitario)} c/u)",
                                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                                )
                            }
                        }

                        // Subtotal a la derecha
                        Text(
                            text = "$simbolo ${String.format(Locale.US, "%.2f", item.subtotal)}",
                            style = FDType.Numeric.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = FDColors.TextPrimary
                        )
                    }
                }
            }
        }

        if (alertaFlotante != null) {
            GloboAlertaFlotante(
                mensaje = alertaFlotante,
                timestamp = alertaTimestamp,
                onDismiss = onDismissAlerta,
                alignment = Alignment.BottomStart,
                offset = IntOffset(x = 16, y = 4)
            )
        }
    }
}

@Composable
private fun FilaProductoBusqueda(
    producto: MoldeProductos,
    presentacion: PresentacionProducto,
    simboloMoneda: String,
    carrito: List<ItemVenta>,
    alertaFlotante: String? = null,
    alertaTimestamp: Long = 0L,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onClickCantidad: () -> Unit,
    onBloqueoClick: (String) -> Unit,
    onDismissAlerta: () -> Unit = {}
) {
    val stockMaximo = remember(producto, presentacion.presentacionId) {
        producto.calcularStockMaximoPresentacion(presentacion.presentacionId)
    }
    val stockFisicoTotal = producto.stockFisicoTotalUnidades
    val estaActivo = producto.activo
    val tienePrecioValido = presentacion.precioventa > 0.0
    val validacion = producto.validarDisponibilidadVenta(presentacion)
    val tieneStockSuficiente = validacion.first && stockMaximo > 0
    val motivoFallo = validacion.second
    val habilitadoParaAgregar = estaActivo && tienePrecioValido && tieneStockSuficiente
    val cantEnCarrito = carrito.firstOrNull { it.productoId == producto.indice && it.presentacionId == presentacion.presentacionId }?.cantidad ?: 0
    val puedeSumar = habilitadoParaAgregar && (cantEnCarrito < stockMaximo)

    val (iconoBtn, textoBtn, motivoBloqueo) = when {
        !estaActivo -> Triple(
            Icons.Default.Block,
            "DESACTIVADO",
            "El producto '${producto.nombre}' fue desactivado en inventario y no se puede vender."
        )
        !tienePrecioValido -> Triple(
            Icons.Default.Info,
            "SIN PRECIO",
            "La presentación '${presentacion.nombre}' tiene precio S/ 0.00. Configura su precio antes de vender."
        )
        !tieneStockSuficiente -> when {
            stockMaximo <= 0 && stockFisicoTotal <= 0 -> Triple(
                Icons.Default.Block,
                "SIN STOCK",
                "El producto '${producto.nombre}' no cuenta con existencias en farmacia."
            )
            motivoFallo?.contains("costo", ignoreCase = true) == true -> Triple(
                Icons.AutoMirrored.Filled.ReceiptLong,
                "SIN COSTO",
                motivoFallo
            )
            motivoFallo?.contains("vencido", ignoreCase = true) == true -> Triple(
                Icons.Default.EventBusy,
                "VENCIDO",
                motivoFallo
            )
            else -> Triple(
                Icons.Default.RemoveShoppingCart,
                "INSUFICIENTE",
                motivoFallo ?: "Stock insuficiente para esta presentación."
            )
        }
        else -> Triple(Icons.Default.Add, "", null)
    }

    val nombreCompleto = remember(producto.nombre, presentacion.nombre) {
        if (presentacion.nombre.isNotBlank() && !producto.nombre.contains(presentacion.nombre, ignoreCase = true)) {
            "${producto.nombre} · ${presentacion.nombre}"
        } else {
            producto.nombre
        }
    }

    val (stockTexto, stockColor) = when {
        stockMaximo > 0 -> {
            val etiqueta = if (stockMaximo == Int.MAX_VALUE) {
                "Stock disponible"
            } else if (cantEnCarrito >= stockMaximo) {
                "Stock: $stockMaximo (en canasta)"
            } else {
                "Stock: $stockMaximo disp."
            }
            etiqueta to FDColors.Success
        }
        stockFisicoTotal > 0 -> "Stock: ${stockFisicoTotal.toInt()} (no vendible)" to FDColors.TextTertiary
        else -> "Sin stock" to FDColors.TextTertiary
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Columna Izquierda: Icono + Nombre Completo + Subtítulo (Categoría y Stock)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (habilitadoParaAgregar) {
                        if (cantEnCarrito < stockMaximo) {
                            onSumar()
                        } else {
                            onBloqueoClick("Solo quedan $stockMaximo ${presentacion.nombre} disponibles en inventario.")
                        }
                    } else {
                        onBloqueoClick(motivoBloqueo ?: "No disponible para la venta")
                    }
                }
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Surface(
                color = if (cantEnCarrito > 0) FDColors.Primary.copy(alpha = 0.12f) else FDColors.InputBackground.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Sell,
                        contentDescription = null,
                        tint = if (cantEnCarrito > 0) FDColors.Primary else FDColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = nombreCompleto,
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (producto.categoriaNombre.isNotBlank()) {
                        Text(
                            text = producto.categoriaNombre,
                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                        )
                        Text(
                            text = "·",
                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                        )
                    }
                    Text(
                        text = stockTexto,
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = stockColor
                    )
                }
            }
        }

        // Columna Derecha: Precio y Stepper (+ / -)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", presentacion.precioventa)}",
                style = FDType.Numeric.copy(
                    fontSize = 14.sp,
                    color = FDColors.Primary,
                    fontWeight = FontWeight.Black
                )
            )

            if (habilitadoParaAgregar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Botón Menos [ − ]
                    Surface(
                        onClick = onRestar,
                        enabled = cantEnCarrito > 0,
                        shape = CircleShape,
                        color = if (cantEnCarrito > 0) FDColors.SurfaceElevated else FDColors.InputBackground.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (cantEnCarrito > 0) FDColors.Border else FDColors.Border.copy(alpha = 0.25f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Disminuir",
                                tint = if (cantEnCarrito > 0) FDColors.TextPrimary else FDColors.TextTertiary.copy(alpha = 0.35f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Cantidad numérica (clic para escribir directo)
                    Surface(
                        onClick = onClickCantidad,
                        shape = RoundedCornerShape(6.dp),
                        color = FDColors.InputBackground.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (cantEnCarrito > 0) FDColors.Primary.copy(alpha = 0.6f) else FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "$cantEnCarrito",
                                style = FDType.Numeric.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (cantEnCarrito > 0) FDColors.Primary else FDColors.TextSecondary
                                )
                            )
                        }
                    }

                    // Botón Más [ + ]
                    Surface(
                        onClick = {
                            if (puedeSumar) {
                                onSumar()
                            } else {
                                onBloqueoClick("Solo quedan $stockMaximo ${presentacion.nombre} disponibles en inventario.")
                            }
                        },
                        enabled = puedeSumar,
                        shape = CircleShape,
                        color = if (puedeSumar) FDColors.Primary else FDColors.InputBackground.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (puedeSumar) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = if (puedeSumar) Color.White else FDColors.TextTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = { onBloqueoClick(motivoBloqueo ?: "No disponible para la venta") },
                    color = FDColors.Border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            iconoBtn,
                            contentDescription = null,
                            tint = FDColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = textoBtn,
                            style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (alertaFlotante != null) {
        GloboAlertaFlotante(
            mensaje = alertaFlotante,
            timestamp = alertaTimestamp,
            onDismiss = onDismissAlerta,
            alignment = Alignment.BottomEnd,
            offset = IntOffset(x = -12, y = 4)
        )
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
private fun DialogoConflictoReanudacion(
    ventaSuspendida: VentaSuspendida,
    itemsCarritoActual: Int,
    totalCarritoActual: Double,
    simboloMoneda: String,
    estaProcesando: Boolean = false,
    onDismiss: () -> Unit,
    onVaciarYReanudar: () -> Unit,
    onAnadirACarrito: () -> Unit
) {
    Dialog(onDismissRequest = { if (!estaProcesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FDColors.Warning.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = FDColors.Warning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Reanudar Venta en Espera",
                                style = FDType.Heading2.copy(fontSize = 17.sp, fontWeight = FontWeight.Black),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                "Ya tienes productos en el carrito actual",
                                style = FDType.Caption,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (!estaProcesando) onDismiss() },
                        enabled = !estaProcesando,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (estaProcesando) FDColors.TextTertiary.copy(alpha = 0.4f) else FDColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Banner de procesamiento activo
                if (estaProcesando) {
                    Surface(
                        shape = FDShapes.Small,
                        color = FDColors.Primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = FDColors.Primary
                            )
                            Text(
                                "Restaurando productos en el carrito...",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = FDColors.Primary
                            )
                        }
                    }
                }

                // Resumen comparativo de lo que hay en el carrito vs la venta suspendida
                Surface(
                    shape = FDShapes.Small,
                    color = FDColors.InputBackground.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = FDColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Carrito actual:",
                                    style = FDType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                            Text(
                                "$itemsCarritoActual productos · $simboloMoneda ${String.format(Locale.US, "%.2f", totalCarritoActual)}",
                                style = FDType.Numeric.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = FDColors.TextPrimary
                            )
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = FDColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "${ventaSuspendida.nota.ifBlank { "Venta en pausa" }}:",
                                    style = FDType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = FDColors.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                            Text(
                                "${ventaSuspendida.items.size} productos · $simboloMoneda ${String.format(Locale.US, "%.2f", ventaSuspendida.total)}",
                                style = FDType.Numeric.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }

                Text(
                    "¿Qué deseas hacer con la venta que estás reanudando?",
                    style = FDType.Label.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    color = FDColors.TextSecondary
                )

                // Tarjeta 1: Vaciar carrito y reanudar
                Surface(
                    onClick = { if (!estaProcesando) onVaciarYReanudar() },
                    enabled = !estaProcesando,
                    shape = FDShapes.Small,
                    color = if (estaProcesando) FDColors.Surface.copy(alpha = 0.5f) else FDColors.Surface,
                    border = BorderStroke(1.dp, if (estaProcesando) FDColors.Border.copy(alpha = 0.4f) else FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (estaProcesando) FDColors.Error.copy(alpha = 0.04f) else FDColors.Error.copy(alpha = 0.1f), FDShapes.Small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = if (estaProcesando) FDColors.Error.copy(alpha = 0.4f) else FDColors.Error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "VACIAR CARRITO Y REANUDAR",
                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                color = if (estaProcesando) FDColors.Error.copy(alpha = 0.4f) else FDColors.Error
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Guarda lo actual en pausa automáticamente y carga la venta pausada. Nada se bota.",
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = if (estaProcesando) FDColors.TextTertiary else FDColors.TextSecondary
                            )
                        }
                    }
                }

                // Tarjeta 2: Añadir a la venta actual
                Surface(
                    onClick = { if (!estaProcesando) onAnadirACarrito() },
                    enabled = !estaProcesando,
                    shape = FDShapes.Small,
                    color = if (estaProcesando) FDColors.Surface.copy(alpha = 0.5f) else FDColors.Surface,
                    border = BorderStroke(1.dp, if (estaProcesando) FDColors.Border.copy(alpha = 0.4f) else FDColors.Primary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (estaProcesando) FDColors.Primary.copy(alpha = 0.05f) else FDColors.Primary.copy(alpha = 0.12f), FDShapes.Small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = if (estaProcesando) FDColors.Primary.copy(alpha = 0.4f) else FDColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AÑADIR A LA VENTA ACTUAL",
                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                color = if (estaProcesando) FDColors.Primary.copy(alpha = 0.4f) else FDColors.Primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Une los productos de la venta pausada sumándolos al carrito actual.",
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = if (estaProcesando) FDColors.TextTertiary else FDColors.TextSecondary
                            )
                        }
                    }
                }

                // Botón cancelar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !estaProcesando,
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, if (estaProcesando) FDColors.Border.copy(alpha = 0.4f) else FDColors.Border),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            "Cancelar",
                            style = FDType.Label.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = if (estaProcesando) FDColors.TextTertiary else FDColors.TextSecondary
                        )
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
    esClienteRegistrado: Boolean = true,
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
                text = "CLIENTE Y FACTURACIÓN",
                style = FDType.Label.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = FDColors.TextTertiary
            )
            if (cliente.numeroDocumento.isNotBlank()) {
                Surface(
                    color = if (cliente.tipoDocumento == "RUC") FDColors.Primary.copy(alpha = 0.12f) else FDColors.SuccessSubtle,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (cliente.tipoDocumento == "RUC") "FACTURA ELECTRÓNICA" else "BOLETA ELECTRÓNICA",
                        style = FDType.Caption.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cliente.tipoDocumento == "RUC") FDColors.Primary else FDColors.Success
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (cliente.numeroDocumento.isNotBlank()) {
            // Cliente ya asignado y confirmado a la venta
            Surface(
                color = FDColors.InputBackground,
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FDColors.Surface, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (cliente.tipoDocumento == "RUC") Icons.Default.Business else Icons.Default.Person,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                cliente.nombre,
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (esClienteRegistrado) FDColors.SuccessSubtle else FDColors.Primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Text(
                                    if (esClienteRegistrado) "Cliente registrado" else "Nuevo · se guarda al cobrar",
                                    style = FDType.Caption.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esClienteRegistrado) FDColors.Success else FDColors.Primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${cliente.tipoDocumento}: ${cliente.numeroDocumento}",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextSecondary
                        )
                        if (cliente.direccion.isNotBlank()) {
                            Text(
                                "Dirección: ${cliente.direccion}",
                                style = FDType.Caption.copy(fontSize = 10.5.sp),
                                color = FDColors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            inputDoc = ""
                            onLimpiarResultado()
                            onLimpiarCliente()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(FDColors.InputBackground, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Cambiar cliente",
                            tint = FDColors.TextSecondary,
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


