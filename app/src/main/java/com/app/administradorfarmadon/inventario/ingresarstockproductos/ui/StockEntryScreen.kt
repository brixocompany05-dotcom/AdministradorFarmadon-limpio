package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryViewModel
import com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes.*

/**
 * Ingresar Stock — ADAPTATIVO GEOMÉTRICO 10/10 (2026)
 * Colores 100% FDColors (claro/oscuro) + geometría física MedidaAdaptativa (s) para simetría en 600dp→1360dp.
 * 60% documento continuo | 40% liquidación viva, WindowInsets.systemBars respetados, imePadding vivo.
 */
@Composable
fun StockEntryScreen(
    productId: String,
    loteNumeroPreseleccionado: String? = null,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: StockEntryViewModel = viewModel()
) {
    val s = recordarMedidaAdaptativa()
    LaunchedEffect(productId, loteNumeroPreseleccionado) {
        if (!loteNumeroPreseleccionado.isNullOrBlank()) viewModel.inicializarConLoteNumero(productId, loteNumeroPreseleccionado)
        else viewModel.inicializar(productId)
    }
    val state by viewModel.state.collectAsState()
    val tieneCambios = state.numeroLote.isNotBlank() || state.cantidadBultos.isNotBlank() || state.cantidadDirecta.isNotBlank() || state.costoCompraLote.isNotBlank() || state.numeroFactura.isNotBlank()
    var mostrarSalida by remember { mutableStateOf(false) }
    val focusManagerStock = LocalFocusManager.current
    val keyboardControllerStock = LocalSoftwareKeyboardController.current
    val densityStock = LocalDensity.current
    val isKeyboardVisibleStock = WindowInsets.ime.getBottom(densityStock) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleStock -> {
                keyboardControllerStock?.hide()
                focusManagerStock.clearFocus(force = true)
            }
            state.mostrarDialogoVincularPedido -> viewModel.onDismissVincularPedido()
            state.mostrarDialogoProveedores -> {
                if (state.mostrarFormularioNuevoProveedor) viewModel.onToggleFormularioNuevoProveedor(false)
                else viewModel.onCerrarDialogoProveedores()
            }
            mostrarSalida -> mostrarSalida = false
            tieneCambios && !state.estaGuardando -> mostrarSalida = true
            else -> onNavigateBack()
        }
    }
    if (mostrarSalida) {
        AlertDialog(
            onDismissRequest = { mostrarSalida = false },
            title = { Text("¿Salir sin guardar?", style = FDType.Heading2.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium, color = FDColors.TextPrimary)) },
            text = { Text("Tienes datos sin guardar.", style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontFamily = InterPremium, color = FDColors.TextSecondary)) },
            confirmButton = { Button(onClick = { mostrarSalida = false; onNavigateBack() }, colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error), shape = RoundedCornerShape(s.radiusButton)) { Text("Descartar") } },
            dismissButton = { OutlinedButton(onClick = { mostrarSalida = false }, shape = RoundedCornerShape(s.radiusButton)) { Text("Seguir") } },
            containerColor = FDColors.SurfaceElevated, shape = RoundedCornerShape(s.radiusCard)
        )
    }
    if (state.mostrarDialogoProveedores) {
        ProveedorSelectionDialog(
            proveedores = state.proveedoresFiltrados, busqueda = state.busquedaProveedorDialogo, mostrarFormularioNuevo = state.mostrarFormularioNuevoProveedor,
            onBusquedaChange = { viewModel.onBusquedaProveedorDialogoChanged(it) }, onSelectProveedor = { viewModel.onSeleccionarProveedor(it) },
            onToggleNuevoProveedor = { viewModel.onToggleFormularioNuevoProveedor(it) },
            onGuardarNuevoProveedorConDatos = { a, b, c, d, e, f, g -> viewModel.onGuardarNuevoProveedorConDatos(a, b, c, d, e, f, g) },
            onDismiss = { viewModel.onCerrarDialogoProveedores() }
        )
    }
    state.pedidoPendienteVinculable?.let { pedido ->
        if (state.mostrarDialogoVincularPedido) {
            val saldo = pedido.items.firstOrNull { it.productoId == state.productoId }?.saldoPendiente ?: 0
            AlertDialog(
                onDismissRequest = { viewModel.onDismissVincularPedido() },
                title = { Text("¿Vincular a pedido pendiente?", style = FDType.Heading2.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium, color = FDColors.TextPrimary)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Este producto tiene pedido pendiente ${pedido.numeroOrden} con $saldo unidades por recibir de ${pedido.proveedorNombre}.", style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontFamily = InterPremium, color = FDColors.TextPrimary))
                        Text("Si registras este ingreso como recepción, se descontará del pedido y quedará trazado con factura y lote. Si es compra suelta, quedará como ingreso directo sin tocar el pedido.", style = FDType.BodySmall.copy(fontSize = s.textLabel.value.sp, fontFamily = InterPremium, color = FDColors.TextSecondary))
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmarVincularPedido(onSuccess) }, colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary), shape = RoundedCornerShape(s.radiusButton)) {
                        Text("Sí, vincular a ${pedido.numeroOrden}", style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium))
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.onDismissVincularPedido() }, shape = RoundedCornerShape(s.radiusButton)) { Text("Cancelar", style = FDType.Label.copy(fontFamily = InterPremium)) }
                        TextButton(onClick = { viewModel.onConfirmarIngresoSuelto(onSuccess) }) { Text("No, ingreso suelto", style = FDType.Label.copy(color = FDColors.TextTertiary, fontFamily = InterPremium)) }
                    }
                },
                containerColor = FDColors.SurfaceElevated, shape = RoundedCornerShape(s.radiusCard)
            )
        }
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).imePadding().background(FDColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            CabeceraStockEntry(state = state, onBackClicked = {
                when {
                    state.mostrarDialogoProveedores -> {
                        if (state.mostrarFormularioNuevoProveedor) viewModel.onToggleFormularioNuevoProveedor(false) else viewModel.onCerrarDialogoProveedores()
                    }
                    tieneCambios && !state.estaGuardando -> mostrarSalida = true
                    else -> onNavigateBack()
                }
            }, onScanGs1 = { viewModel.onCodigoGs1Escaneado(it) })

            if (state.mensajeError != null) {
                Surface(color = FDColors.Error.copy(alpha = 0.08f), shape = RoundedCornerShape(s.radiusInput), border = BorderStroke(s.borderWidth, FDColors.Error.copy(alpha = 0.28f)), modifier = Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.xs)) {
                    Row(Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(s.iconSmall))
                        Text(state.mensajeError!!, style = FDType.Body.copy(fontSize = s.textBody.value.sp, color = FDColors.Error, fontWeight = FontWeight.Medium, fontFamily = InterPremium))
                    }
                }
            }

            // Apertura instantánea: nunca bloquea en spinner de pantalla completa. Muestra esqueleto vivo y revalida con servidor.
            if (state.estaCargando && state.productoNombre.isBlank()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = FDColors.Primary, trackColor = FDColors.Border.copy(alpha = 0.25f))
            }
            BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = s.padScreenH, vertical = s.padScreenV)) {
                    val isWide = maxWidth >= 880.dp
                    if (isWide) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                            Column(
                                Modifier.weight(0.60f).fillMaxHeight().verticalScroll(rememberScrollState()).imePadding(),
                                verticalArrangement = Arrangement.spacedBy(s.gapLarge)
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                                    Box(Modifier.size(5.dp).background(FDColors.Primary, androidx.compose.foundation.shape.CircleShape))
                                    Text("* obligatorio", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.92f, fontFamily = InterPremium), color = FDColors.TextSecondary)
                                    Text("·", color = FDColors.TextTertiary, style = FDType.Caption.copy(fontSize = s.textLabel.value.sp))
                                    Text("gris = opcional", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.92f, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                }
                                SeccionLoteYVencimiento(state = state, viewModel = viewModel)
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = s.separatorH)
                                SeccionCantidadYCosto(state = state, viewModel = viewModel)
                                Spacer(Modifier.height(s.xs))
                            }
                            Surface(
                                Modifier.weight(0.40f).fillMaxHeight(), shape = RoundedCornerShape(s.radiusCard), color = FDColors.Surface, border = BorderStroke(s.borderWidth, FDColors.Border), shadowElevation = 1.dp
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(s.padCard).imePadding(), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                                        SeccionComprobanteYProveedor(state = state, viewModel = viewModel)
                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = s.separatorH)
                                        PanelLiquidacionFactura(
                                            state = state, onConfirmarIngreso = { viewModel.guardarIngreso(onSuccess) },
                                            onCambiarStockMinimo = { viewModel.onEditarStockMinimoPropuesto() }, onStockMinimoChanged = { viewModel.onStockMinimoPersonalizadoChanged(it) },
                                            onConfirmarStockMinimo = { viewModel.onConfirmarEdicionStockMinimo() }, onCancelarStockMinimo = { viewModel.onCancelarEdicionStockMinimo() },
                                            modifier = Modifier.fillMaxWidth(), sinMarco = true, sinBoton = true
                                        )
                                    }
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = s.separatorH)
                                    Box(Modifier.fillMaxWidth().background(FDColors.Surface).padding(s.padCard)) {
                                        Button(
                                            onClick = { viewModel.guardarIngreso(onSuccess) }, enabled = state.esFormularioValido && !state.estaGuardando,
                                            shape = RoundedCornerShape(s.radiusButton),
                                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText, disabledContainerColor = FDColors.Primary.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth().height(s.btnMediumH)
                                        ) {
                                            if (state.estaGuardando) { CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(s.iconSmall), strokeWidth = 2.dp); Spacer(Modifier.width(s.xs)); Text("Guardando...", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium)) }
                                            else Text("Guardar ingreso", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(s.gapLarge)) {
                            SeccionLoteYVencimiento(state = state, viewModel = viewModel)
                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = s.separatorH)
                            SeccionCantidadYCosto(state = state, viewModel = viewModel)
                            SeccionComprobanteYProveedor(state = state, viewModel = viewModel)
                            PanelLiquidacionFactura(state = state, onConfirmarIngreso = { viewModel.guardarIngreso(onSuccess) }, onCambiarStockMinimo = { viewModel.onEditarStockMinimoPropuesto() }, onStockMinimoChanged = { viewModel.onStockMinimoPersonalizadoChanged(it) }, onConfirmarStockMinimo = { viewModel.onConfirmarEdicionStockMinimo() }, onCancelarStockMinimo = { viewModel.onCancelarEdicionStockMinimo() }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(s.gapLarge))
                        }
                    }
                }
        }
    }
}
