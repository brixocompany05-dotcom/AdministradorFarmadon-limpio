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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.compras.logica.ComprasViewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.ui.componentes.DialogoAnularFactura
import com.app.administradorfarmadon.compras.ui.componentes.DialogoCrearProveedor
import com.app.administradorfarmadon.compras.ui.componentes.DialogoNotaCredito
import com.app.administradorfarmadon.compras.ui.componentes.DialogoProrrogarVencimiento
import com.app.administradorfarmadon.compras.ui.componentes.PestanaHistorialPedidos
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
    pestanaInicial: String? = null,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pestanaInicial) {
        if (!pestanaInicial.isNullOrBlank()) {
            val tab = when (pestanaInicial.uppercase()) {
                "PROVEEDORES", "COMPRAS_PROVEEDORES", "INVENTARIO_PROVEEDORES", "COMPRAS_DISTRIBUIDORES" -> "PROVEEDORES"
                "CUENTAS", "CUENTAS_POR_PAGAR", "COMPRAS_FACTURAS", "FACTURAS" -> "CUENTAS"
                else -> "REPOSICION"
            }
            viewModel.seleccionarTab(tab)
        }
    }

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
            proveedoresExistentes = state.proveedores,
            guardando = state.guardandoProveedor,
            errorGuardado = state.errorGuardadoProveedor,
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
                onConfirmarNota = { num, monto, motivo, prodId, prodNombre, lote, cant ->
                    viewModel.registrarNotaCredito(facturaViva.id, num, monto, motivo, prodId, prodNombre, lote, cant)
                }
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
                    TabQuiet(label = "Pedido a Proveedor", selected = state.tabSeleccionada == "REPOSICION", onClick = { viewModel.seleccionarTab("REPOSICION") }, s = s)
                    TabQuiet(label = "Historial de Pedidos", selected = state.tabSeleccionada == "HISTORIAL", onClick = { viewModel.seleccionarTab("HISTORIAL") }, s = s)
                    TabQuiet(label = "Proveedores", selected = state.tabSeleccionada == "PROVEEDORES", onClick = { viewModel.seleccionarTab("PROVEEDORES") }, s = s)
                    TabQuiet(label = "Facturas / Cuentas", selected = state.tabSeleccionada == "CUENTAS", onClick = { viewModel.seleccionarTab("CUENTAS") }, s = s)
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
                            contribuidoresCarrito = state.contribuidoresCarrito,
                            onModificarCantidadProducto = { prod, delta -> viewModel.modificarCantidadProducto(prod, delta) },
                            onReponerSugeridosProveedor = { viewModel.reponerSugeridosDeProveedor(it) },
                            onRealizarPedido = { viewModel.confirmarPedidoEnviado(it) },
                            onLimpiarPedidoProveedor = { viewModel.limpiarPedidoProveedor(it) },
                            enviandoPedido = state.enviandoPedido,
                            enCaminoPorProducto = state.enCaminoPorProducto,
                            productoPendienteConfirmar = state.productoPendienteConfirmar,
                            cantidadExtraPropuesta = state.cantidadExtraPropuesta,
                            onConfirmarAdicionExtra = { viewModel.confirmarAdicionExtra() },
                            onDescartarAdicionExtra = { viewModel.descartarAdicionExtra() },
                            onVincularProducto = { prod, prov -> viewModel.vincularProductoAProveedor(prod.id, prov) },
                            onRecibirMercaderia = { viewModel.abrirDialogoRecepcion(it) },
                            onCerrarConAjuste = { viewModel.cerrarOrdenConAjuste(it.id) },
                            onDescartarProducto = { pid, prodId -> viewModel.descartarProductoDePedido(pid, prodId) },
                            onCancelarPedido = { viewModel.cancelarPedidoEnviado(it) },
                            listaState = viewModel.listaReposicion,
                            cargando = state.cargando,
                            errorEscucha = state.errorEscucha,
                            envioExitosoProveedor = state.envioExitosoProveedor,
                            onConsumirEnvioExitoso = { viewModel.consumirEnvioExitoso() }
                        )
                        "HISTORIAL" -> PestanaHistorialPedidos(
                            pedidosGuardados = state.pedidosGuardados,
                            simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" },
                            s = s,
                            onRecibirMercaderia = { viewModel.abrirDialogoRecepcion(it) },
                            onCerrarConAjuste = { viewModel.cerrarOrdenConAjuste(it.id) },
                            onDescartarProducto = { pid, prodId -> viewModel.descartarProductoDePedido(pid, prodId) },
                            onCancelarPedido = { viewModel.cancelarPedidoEnviado(it) },
                            anexandoFactura = state.anexandoFactura,
                            onAnexarFactura = { pid, num, monto, cond, venc, emi ->
                                viewModel.anexarFacturaAOrden(pid, num, monto, cond, venc, emi)
                            }
                        )
                        "PROVEEDORES" -> {
                            val provSel = state.proveedorSeleccionado
                            val deuda = if (provSel != null) state.deudaPendienteProveedor(provSel) else 0.0
                            val cantFact = if (provSel != null) state.facturasPendientesCountProveedor(provSel) else 0
                            PestanaProveedores(
                                proveedores = state.proveedoresFiltrados,
                                proveedorSeleccionado = state.proveedorSeleccionado,
                                deudaPendiente = deuda, facturasPendientesCount = cantFact, subTabActual = state.subTabProveedor,
                                productosDelProveedor = state.productosDelProveedorSeleccionado,
                                todosLosProductos = state.todosLosProductos,
                                onSeleccionarSubTab = { viewModel.seleccionarSubTabProveedor(it) },
                                onSeleccionarProveedor = { viewModel.seleccionarProveedor(it) },
                                onCrearProveedor = { viewModel.abrirDialogoCrearProveedor() },
                                onEditarProveedor = { viewModel.abrirDialogoEditarProveedor(it) },
                                onEliminarProveedor = { prov -> viewModel.eliminarProveedor(prov, cantFact) { _, _ -> } },
                                onVincularProducto = { prod, prov -> viewModel.vincularProductoAProveedor(prod.id, prov) },
                                onDesvincularProducto = { prod -> viewModel.desvincularProductoDeProveedor(prod.id) },
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
                onConfirmar = { motivo, plata, metodo, referencia, conDevolucion -> viewModel.confirmarAnulacionFactura(motivo, plata, metodo, referencia, conDevolucion) }
            )
        }

        state.pedidoParaRecepcionar?.let { pedido ->
            if (state.mostrarDialogoRecepcion) {
                if (state.cargandoIndiceRecepcion) {
                    Dialog(onDismissRequest = {}) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = FDColors.SurfaceElevated,
                            border = BorderStroke(1.dp, FDColors.Border)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = FDColors.Primary,
                                    strokeWidth = 3.dp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "Preparando recepción...",
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary
                                    )
                                    Text(
                                        "Verificando lotes e historial del pedido",
                                        style = FDType.Caption,
                                        color = FDColors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    RecepcionMercaderiaPanel(
                        pedido = pedido,
                        facturaExistente = state.facturaRecepcionExistente,
                        procesando = state.procesandoRecepcion,
                        indiceLotes = state.indiceLotesOrden,
                        // Estricto por ID: con nombres duplicados, el nombre pinta saldo ajeno.
                        saldoAFavorDisponible = state.proveedores.firstOrNull {
                            it.id.isNotBlank() && it.id == pedido.proveedorId
                        }?.saldoAFavor ?: 0.0,
                        metodosPago = state.metodosPago,
                        facturasExistentes = state.facturas,
                        onDismiss = { viewModel.cerrarDialogoRecepcion() },
                        onAsentarRecepcion = { numFact, condPago, fVencPago, fEmisionPapel, montFact, pagado, metodoPago, pagosRec, saldoUsado, itemsRec, cerrarConAj ->
                            viewModel.asentarRecepcionPedido(pedido.id, numFact, condPago, fVencPago, fEmisionPapel, montFact, pagado, metodoPago, pagosRec, saldoUsado, itemsRec, cerrarConAj)
                        }
                    )
                }
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
private fun TabQuiet(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Surface(
        color = if (selected) FDColors.SurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(topStart = s.radiusInput, topEnd = s.radiusInput),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = s.gapXLarge, vertical = s.gapMedium),
            verticalAlignment = Alignment.CenterVertically
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
        }
    }
}
