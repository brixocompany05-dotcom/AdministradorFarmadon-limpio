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
import com.app.administradorfarmadon.compras.ui.componentes.DialogoAnularFactura
import com.app.administradorfarmadon.compras.ui.componentes.DialogoCrearProveedor
import com.app.administradorfarmadon.compras.ui.componentes.DialogoNotaCredito
import com.app.administradorfarmadon.compras.ui.componentes.DialogoProrrogarVencimiento
import com.app.administradorfarmadon.compras.ui.componentes.RecepcionMercaderiaPanel
import com.app.administradorfarmadon.compras.ui.componentes.PanelRegistrarPago
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
    // BackHandler quiet —” teclado primero, luego diálogos, luego nada (sin competencia)
    val focusManagerCompras = LocalFocusManager.current
    val keyboardControllerCompras = LocalSoftwareKeyboardController.current
    val densityCompras = LocalDensity.current
    val isKeyboardVisibleCompras = WindowInsets.ime.getBottom(densityCompras) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleCompras -> { keyboardControllerCompras?.hide(); focusManagerCompras.clearFocus(force = true) }
            state.mostrarDialogoProveedor -> viewModel.cerrarDialogoProveedor()
            state.mostrarDialogoAnulacion -> viewModel.cerrarDialogoAnularFactura()
            state.facturaParaAbonarId != null -> viewModel.cerrarDialogoAbono()
            state.facturaParaNotaCreditoId != null -> viewModel.cerrarDialogoNotaCredito()
            state.facturaParaProrrogaId != null -> viewModel.cerrarDialogoProrroga()
            state.mostrarDialogoRecepcion -> viewModel.cerrarDialogoRecepcion()
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
    state.facturaParaProrrogaId?.let { id ->
        val facturaViva = state.facturas.find { it.id == id }
        if (facturaViva != null) {
            DialogoProrrogarVencimiento(factura = facturaViva, estadoFactura = facturaViva.estadoPago, procesando = state.procesandoProrroga, onDismiss = { viewModel.cerrarDialogoProrroga() }, onConfirmarProrroga = { nf -> viewModel.prorrogarVencimientoFactura(facturaViva.id, nf) })
        } else {
            LaunchedEffect(id) { viewModel.cerrarDialogoProrroga() }
        }
    }
    state.facturaParaNotaCreditoId?.let { id ->
        val facturaViva = state.facturas.find { it.id == id }
        if (facturaViva != null) {
            DialogoNotaCredito(
                factura = facturaViva,
                procesando = state.procesandoNotaCredito,
                estadoFactura = facturaViva.estadoPago,
                autorizadoPlata = viewModel.esUsuarioAutorizadoPlata,
                onDismiss = { viewModel.cerrarDialogoNotaCredito() },
                onConfirmarNota = { num, monto, motivo -> viewModel.registrarNotaCredito(facturaViva.id, num, monto, motivo) }
            )
        } else {
            LaunchedEffect(id) { viewModel.cerrarDialogoNotaCredito() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = FDColors.Background,
            modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                Spacer(Modifier.height(s.gapMedium))

                // Tabs tipo navegador —” amplias y con fondo (browser style)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = s.padScreenH),
                    horizontalArrangement = Arrangement.spacedBy(s.gapTiny),
                    verticalAlignment = Alignment.Bottom
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
                            Text("No se pudo actualizar: $motivo", style = FDType.Caption.copy(fontSize = s.textLabel.value.sp, fontFamily = InterPremium), color = FDColors.Error, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.reintentarEscuchas() }) { Text("REINTENTAR", color = FDColors.Error, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
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
                            onModificarCantidadProducto = { prod, delta -> viewModel.modificarCantidadProducto(prod, delta) },
                            onReponerSugeridosProveedor = { viewModel.reponerSugeridosDeProveedor(it) },
                            onRealizarPedido = { viewModel.confirmarPedidoEnviado(it) },
                            onEditarPedidoRealizado = { pedidoId, items -> viewModel.editarPedidoRealizado(pedidoId, items) },
                            onEliminarPedidoRealizado = { viewModel.eliminarPedidoRealizado(it) },
                            procesandoEdicionPedido = state.procesandoEdicionPedido,
                            procesandoEliminacionPedido = state.procesandoEliminacionPedido,
                            onLimpiarPedidoProveedor = { viewModel.limpiarPedidoProveedor(it) },
                            enviandoPedido = state.enviandoPedido,
                            onCancelarPedidoEnviado = { viewModel.cancelarPedidoEnviado(it) },
                            onRecibirMercaderia = { viewModel.abrirDialogoRecepcion(it) },
                            onCerrarOrdenConAjuste = { viewModel.cerrarOrdenConAjuste(it.id) },
                            onDescartarProductoDePedido = { pid, prodId -> viewModel.descartarProductoDePedido(pid, prodId) },
                            enCaminoPorProducto = state.enCaminoPorProducto,
                            productoPendienteConfirmar = state.productoPendienteConfirmar,
                            cantidadExtraPropuesta = state.cantidadExtraPropuesta,
                            onConfirmarAdicionExtra = { viewModel.confirmarAdicionExtra() },
                            onDescartarAdicionExtra = { viewModel.descartarAdicionExtra() },
                            onVincularProducto = { prod, prov -> viewModel.vincularProductoAProveedor(prod.id, prov) },
                            listaState = viewModel.listaReposicion,
                            cargando = state.cargando,
                            errorEscucha = state.errorEscucha,
                            envioExitosoProveedor = state.envioExitosoProveedor,
                            onConsumirEnvioExitoso = { viewModel.consumirEnvioExitoso() }
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
                                onEliminarProveedor = { prov -> viewModel.eliminarProveedor(prov, cantFact) { _, _ -> } },
                                onCobrarSaldoAFavor = { monto, doc, onComplete -> viewModel.cobrarSaldoAFavor(state.proveedorSeleccionado?.id ?: "", monto, doc, onComplete) },
                                onDeclararSaldoPerdido = { monto, motivo, onComplete -> viewModel.declararSaldoPerdido(state.proveedorSeleccionado?.id ?: "", monto, motivo, onComplete) },
                                listaState = viewModel.listaProveedores,
                                cargando = state.cargando,
                                errorEscucha = state.errorEscucha
                            )
                        }
                        "CUENTAS" -> PestanaCuentasPorPagar(
                            facturas = state.facturas, facturaSeleccionada = state.facturaSeleccionada, filtroEstado = state.filtroEstadoFactura, procesandoPago = state.procesandoPago,
                            onSeleccionarFactura = { viewModel.seleccionarFactura(it) }, onCambiarFiltroEstado = { viewModel.setFiltroEstadoFactura(it) },
                            onAbrirDialogoAbono = { viewModel.abrirDialogoAbono(it) },
                            onAbrirDialogoNotaCredito = { viewModel.abrirDialogoNotaCredito(it) },
                            onAbrirDialogoProrroga = { viewModel.abrirDialogoProrroga(it) },
                            onAbrirDialogoAnular = { viewModel.abrirDialogoAnularFactura(it) },
                            listaState = viewModel.listaCuentas
                        )
                    }
                }
            }
        }

        state.facturaParaAnular?.let { fact ->
            DialogoAnularFactura(
                factura = fact,
                lineas = state.lineasAnulacion,
                cargandoLineas = state.cargandoLineasAnulacion,
                procesando = state.procesandoAnulacion,
                autorizadoPlata = viewModel.esUsuarioAutorizadoPlata,
                onDismiss = { viewModel.cerrarDialogoAnularFactura() },
                onConfirmar = { motivo, plata, metodo, referencia -> viewModel.confirmarAnulacionFactura(motivo, plata, metodo, referencia) }
            )
        }

        state.pedidoParaRecepcionar?.let { pedido ->
            if (state.mostrarDialogoRecepcion && !state.cargandoIndiceRecepcion) {
                RecepcionMercaderiaPanel(
                    pedido = pedido,
                    facturaExistente = state.facturaRecepcionExistente,
                    procesando = state.procesandoRecepcion,
                    indiceLotes = state.indiceLotesOrden,
                    saldoAFavorDisponible = state.proveedores.firstOrNull {
                        it.id == pedido.proveedorId || it.nombre.equals(pedido.proveedorNombre, ignoreCase = true)
                    }?.saldoAFavor ?: 0.0,
                    metodosPago = state.metodosPago,
                    onDismiss = { viewModel.cerrarDialogoRecepcion() },
                    onAsentarRecepcion = { numFact, condPago, fVencPago, montFact, pagado, metodoPago, pagosRec, saldoUsado, itemsRec, cerrarConAj ->
                        viewModel.asentarRecepcionPedido(pedido.id, numFact, condPago, fVencPago, montFact, pagado, metodoPago, pagosRec, saldoUsado, itemsRec, cerrarConAj)
                    }
                )
            }
        }

        state.facturaParaAbonarId?.let { id ->
            val facturaViva = state.facturas.find { it.id == id }
            if (facturaViva != null) {
                PanelRegistrarPago(
                    factura = facturaViva,
                    metodosPago = state.metodosPago,
                    procesando = state.procesandoPago,
                    estadoFactura = facturaViva.estadoPago,
                    onVolver = { viewModel.cerrarDialogoAbono() },
                    onGuardar = { m, met, num, pagos -> viewModel.registrarAbonoFactura(facturaViva.id, m, met, num, pagos) }
                )
            } else {
                LaunchedEffect(id) { viewModel.cerrarDialogoAbono() }
            }
        }
    }
}

@Composable
private fun TabQuiet(label: String, count: Int, selected: Boolean, onClick: () -> Unit, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa) {
    Surface(
        color = if (selected) FDColors.SurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(topStart = s.radiusInput, topEnd = s.radiusInput),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = s.gapXLarge, vertical = s.gapMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontFamily = InterPremium
                ),
                color = if (selected) FDColors.Primary else FDColors.TextTertiary
            )
            if (count > 0) {
                Surface(
                    color = if (selected) FDColors.Primary.copy(alpha = 0.12f) else FDColors.TextPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$count",
                        style = FDType.Caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = if (selected) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
