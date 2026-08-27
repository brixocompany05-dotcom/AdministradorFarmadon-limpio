package com.app.administradorfarmadon.compras.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.logica.ComprasViewModel
import com.app.administradorfarmadon.compras.ui.componentes.DialogoCrearProveedor
import com.app.administradorfarmadon.compras.ui.componentes.DialogoProrrogarVencimiento
import com.app.administradorfarmadon.compras.ui.componentes.RecepcionMercaderiaPanel
import com.app.administradorfarmadon.compras.ui.componentes.DialogoRegistrarAbono
import com.app.administradorfarmadon.compras.ui.componentes.PestanaCuentasPorPagar
import com.app.administradorfarmadon.compras.ui.componentes.PestanaProveedores
import com.app.administradorfarmadon.compras.ui.componentes.PestanaReposicion
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

@Composable
fun ComprasScreen(
    viewModel: ComprasViewModel,
    onNavigateToIngresoStock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // BackHandler quiet — teclado primero, luego diálogos, luego nada (sin competencia)
    val focusManagerCompras = LocalFocusManager.current
    val keyboardControllerCompras = LocalSoftwareKeyboardController.current
    val densityCompras = LocalDensity.current
    val isKeyboardVisibleCompras = WindowInsets.ime.getBottom(densityCompras) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleCompras -> { keyboardControllerCompras?.hide(); focusManagerCompras.clearFocus(force = true) }
            state.mostrarDialogoProveedor -> viewModel.cerrarDialogoProveedor()
            state.facturaParaAbonar != null -> viewModel.cerrarDialogoAbono()
            state.facturaParaProrroga != null -> viewModel.cerrarDialogoProrroga()
            state.mostrarDialogoRecepcion -> viewModel.cerrarDialogoRecepcion()
            state.mostrarModalRevisionPedido -> viewModel.cerrarRevisionPedido()
            state.mostrarModalConfirmacionEnvio -> viewModel.cerrarConfirmacionEnvio()
            state.productoPendienteConfirmar != null -> viewModel.descartarAdicionExtra()
            else -> { /* quiet: sin navegación forzada, deja que el sistema decida */ }
        }
    }

    LaunchedEffect(state.mensajeExito, state.mensajeError) {
        state.mensajeExito?.let { snackbarHostState.showSnackbar(it); viewModel.consumirMensajeExito() }
        state.mensajeError?.let { snackbarHostState.showSnackbar(it); viewModel.consumirMensajeError() }
    }

    if (state.mostrarDialogoProveedor) {
        DialogoCrearProveedor(
            proveedorEditando = state.proveedorEditando,
            guardando = state.guardandoProveedor,
            onGuardar = { nom, ruc, cont, tel, em, dir, min -> viewModel.guardarProveedor(nom, ruc, cont, tel, em, dir, min) },
            onDismiss = { viewModel.cerrarDialogoProveedor() }
        )
    }
    state.facturaParaAbonar?.let { f ->
        DialogoRegistrarAbono(factura = f, procesando = state.procesandoPago, onDismiss = { viewModel.cerrarDialogoAbono() }, onConfirmarAbono = { m, met, num, notas -> viewModel.registrarAbonoFactura(f.id, m, met, num, notas) })
    }
    state.facturaParaProrroga?.let { f ->
        DialogoProrrogarVencimiento(factura = f, onDismiss = { viewModel.cerrarDialogoProrroga() }, onConfirmarProrroga = { nf -> viewModel.prorrogarVencimientoFactura(f.id, nf) })
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = FDColors.Background,
            modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                // Header quiet — una verdad
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.padCard),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.35f), modifier = Modifier.weight(1f)) {
                        Text("Compras y proveedores", style = FDType.Heading2.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                        Text("Reposición inteligente · directorio · cuentas por pagar", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                    }
                }

                // Tabs underline — modernos, sin cajas
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = s.padScreenH),
                    horizontalArrangement = Arrangement.spacedBy(s.gapLarge), verticalAlignment = Alignment.CenterVertically
                ) {
                    TabQuiet(label = "Reposición", count = state.productosAgrupadosPorProveedor.keys.size, selected = state.tabSeleccionada == "REPOSICION", onClick = { viewModel.seleccionarTab("REPOSICION") }, s = s)
                    TabQuiet(label = "Proveedores", count = state.totalProveedores, selected = state.tabSeleccionada == "PROVEEDORES", onClick = { viewModel.seleccionarTab("PROVEEDORES") }, s = s)
                    TabQuiet(label = "Cuentas", count = state.facturasPendientes.size, selected = state.tabSeleccionada == "CUENTAS", onClick = { viewModel.seleccionarTab("CUENTAS") }, s = s)
                }
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = s.separatorH, modifier = Modifier.padding(horizontal = s.padScreenH))

                state.errorEscucha?.let { motivo ->
                    Surface(
                        color = FDColors.Error.copy(alpha = 0.06f), shape = RoundedCornerShape(s.radiusInput),
                        border = BorderStroke(s.borderWidth, FDColors.Error.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.xs)
                    ) {
                        Row(Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                            Icon(Icons.Filled.WifiOff, null, tint = FDColors.Error, modifier = Modifier.size(s.iconSmall * 0.85f))
                            Text("Sincronización interrumpida: $motivo", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp, fontFamily = InterPremium), color = FDColors.Error)
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth().background(FDColors.Background)) {
                    when (state.tabSeleccionada) {
                        "REPOSICION" -> PestanaReposicion(
                            productosAgrupadosPorProveedor = state.productosAgrupadosPorProveedor,
                            pedidosActivos = state.pedidosActivosPorProveedor,
                            pedidosGuardados = state.pedidosGuardados,
                            proveedores = state.proveedores,
                            pedidosPorProveedor = state.pedidosPorProveedor,
                            proveedoresExpandidos = state.proveedoresExpandidos,
                            subTabPedidosDerecha = state.subTabPedidosDerecha,
                            pedidoEnRevision = state.pedidoEnRevision,
                            mostrarModalRevision = state.mostrarModalRevisionPedido,
                            mostrarModalConfirmacionEnvio = state.mostrarModalConfirmacionEnvio,
                            pedidoParaConfirmarEnvio = state.pedidoParaConfirmarEnvio,
                            onToggleExpandirProveedor = { viewModel.toggleProveedorExpandido(it) },
                            onModificarCantidadProducto = { prod, delta -> viewModel.modificarCantidadProducto(prod, delta) },
                            onReponerSugeridosProveedor = { viewModel.reponerSugeridosDeProveedor(it) },
                            onReponerTodosSugeridosGlobal = { viewModel.reponerTodosLosSugeridosGlobal() },
                            onAbrirRevisionPedido = { viewModel.abrirRevisionPedido(it) },
                            onCerrarRevisionPedido = { viewModel.cerrarRevisionPedido() },
                            onLimpiarPedidoProveedor = { viewModel.limpiarPedidoProveedor(it) },
                            onRemoverProductoDePedido = { prov, prodId -> viewModel.removerProductoDePedido(prov, prodId) },
                            onSeleccionarSubTabPedidosDerecha = { viewModel.seleccionarSubTabPedidosDerecha(it) },
                            onPrepararConfirmacionEnvio = { viewModel.prepararConfirmacionEnvio(it) },
                            onConfirmarPedidoEnviado = { viewModel.confirmarPedidoEnviado(it) },
                            onCerrarConfirmacionEnvio = { viewModel.cerrarConfirmacionEnvio() },
                            onCancelarPedidoEnviado = { viewModel.cancelarPedidoEnviado(it) },
                            onRecibirMercaderia = { viewModel.abrirDialogoRecepcion(it) },
                            onCerrarOrdenConAjuste = { viewModel.cerrarOrdenConAjuste(it.id) },
                            onDescartarProductoDePedido = { pid, prodId -> viewModel.descartarProductoDePedido(pid, prodId) },
                            onActualizarTelefonoProveedor = { prov, tel -> viewModel.actualizarTelefonoProveedor(prov, tel) },
                            enCaminoPorProducto = state.enCaminoPorProducto,
                            productoPendienteConfirmar = state.productoPendienteConfirmar,
                            cantidadExtraPropuesta = state.cantidadExtraPropuesta,
                            onConfirmarAdicionExtra = { viewModel.confirmarAdicionExtra() },
                            onDescartarAdicionExtra = { viewModel.descartarAdicionExtra() }
                        )
                        "PROVEEDORES" -> {
                            val provSel = state.proveedorSeleccionado
                            val deuda = if (provSel != null) state.deudaPendienteProveedor(provSel) else 0.0
                            val cantFact = if (provSel != null) state.facturasPendientesCountProveedor(provSel) else 0
                            val context = androidx.compose.ui.platform.LocalContext.current
                            PestanaProveedores(
                                proveedores = state.proveedoresFiltrados,
                                proveedorSeleccionado = state.proveedorSeleccionado,
                                deudaPendiente = deuda, facturasPendientesCount = cantFact, subTabActual = state.subTabProveedor,
                                productosDelProveedor = state.productosDelProveedorSeleccionado,
                                onSeleccionarSubTab = { viewModel.seleccionarSubTabProveedor(it) },
                                onSeleccionarProveedor = { viewModel.seleccionarProveedor(it) },
                                onCrearProveedor = { viewModel.abrirDialogoCrearProveedor() },
                                onEditarProveedor = { viewModel.abrirDialogoEditarProveedor(it) },
                                onEliminarProveedor = { prov -> viewModel.eliminarProveedor(prov, cantFact) { _, msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() } }
                            )
                        }
                        "CUENTAS" -> PestanaCuentasPorPagar(
                            facturas = state.facturas, facturaSeleccionada = state.facturaSeleccionada, filtroEstado = state.filtroEstadoFactura, procesandoPago = state.procesandoPago,
                            onSeleccionarFactura = { viewModel.seleccionarFactura(it) }, onCambiarFiltroEstado = { viewModel.setFiltroEstadoFactura(it) },
                            onRevertirPago = { id, num, prov -> viewModel.revertirPagoFactura(id, num, prov) },
                            onAbrirDialogoAbono = { viewModel.abrirDialogoAbono(it) }, onAnularAbono = { fid, aid -> viewModel.anularAbonoFactura(fid, aid) },
                            onAbrirDialogoProrroga = { viewModel.abrirDialogoProrroga(it) }
                        )
                    }
                }
            }
        }

        state.pedidoParaRecepcionar?.let { pedido ->
            if (state.mostrarDialogoRecepcion && !state.cargandoIndiceRecepcion) {
                RecepcionMercaderiaPanel(
                    pedido = pedido, procesando = state.procesandoRecepcion, indiceLotes = state.indiceLotesOrden,
                    onDismiss = { viewModel.cerrarDialogoRecepcion() },
                    onAsentarRecepcion = { numFact, condPago, fVencPago, montFact, itemsRec, cerrarConAj -> viewModel.asentarRecepcionPedido(pedido.id, numFact, condPago, fVencPago, montFact, itemsRec, cerrarConAj) }
                )
            }
        }
    }
}

@Composable
private fun TabQuiet(label: String, count: Int, selected: Boolean, onClick: () -> Unit, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa) {
    Column(
        Modifier.clickable(onClick = onClick).padding(vertical = s.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)) {
            Text(label.uppercase(), style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold, letterSpacing = 0.5.sp, fontFamily = InterPremium), color = if (selected) FDColors.Primary else FDColors.TextTertiary)
            if (count > 0) {
                Text("[ $count ]", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = if (selected) FDColors.Primary else FDColors.TextTertiary)
            }
        }
        Spacer(Modifier.height(s.xs * 0.7f))
        Box(Modifier.height(s.separatorH * 1.6f).width(if (selected) s.gapXLarge else 0.dp).background(if (selected) FDColors.Primary else Color.Transparent, RoundedCornerShape(100.dp)))
    }
}
