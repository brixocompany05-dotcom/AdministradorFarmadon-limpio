package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.compras.ui.componentes.reposicion.*
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct

/**
 * Pestaña Reposición — raíz delgada.
 * Decide entre dos vistas:
 *  - Directorio de proveedores + panel de pedidos en camino (dos columnas).
 *  - Detalle del proveedor elegido (workspace con catálogo y carrito).
 * "REALIZAR PEDIDO" envía directo: sin pantallas intermedias; el resultado
 * (éxito o error real) llega desde el ViewModel.
 */
@Composable
fun PestanaReposicion(
    productosAgrupadosPorProveedor: Map<String, List<PharmProduct>>,
    pedidosActivos: List<PedidoProveedor>,
    pedidosGuardados: List<PedidoCompra> = emptyList(),
    proveedores: List<Proveedor> = emptyList(),
    pedidosPorProveedor: Map<String, Map<String, Int>>,
    contribuidoresCarrito: Map<String, Map<String, Map<String, Int>>> = emptyMap(),
    enviandoPedido: Boolean = false,
    onModificarCantidadProducto: (PharmProduct, Int) -> Unit,
    onReponerSugeridosProveedor: (String) -> Unit = {},
    onRealizarPedido: (PedidoProveedor) -> Unit = {},
    onLimpiarPedidoProveedor: (String) -> Unit,
    enCaminoPorProducto: Map<String, com.app.administradorfarmadon.compras.logica.ProductoEnCamino> = emptyMap(),
    productoPendienteConfirmar: PharmProduct? = null,
    cantidadExtraPropuesta: Int = 0,
    onConfirmarAdicionExtra: () -> Unit = {},
    onDescartarAdicionExtra: () -> Unit = {},
    onVincularProducto: (PharmProduct, Proveedor) -> Unit = { _, _ -> },
    onRecibirMercaderia: (PedidoCompra) -> Unit = {},
    onCerrarConAjuste: (PedidoCompra) -> Unit = {},
    onDescartarProducto: (String, String) -> Unit = { _, _ -> },
    onCancelarPedido: (String) -> Unit = {},
    listaState: LazyListState = LazyListState(),
    cargando: Boolean = false,
    errorEscucha: String? = null,
    envioExitosoProveedor: String? = null,
    onConsumirEnvioExitoso: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }

    // Navegación local de la pestaña: qué proveedor está abierto y qué búsqueda se usa.
    // Sin filtros por pestaña: una sola lista de proveedores (incluye SIN PROVEEDOR como fila).
    var proveedorAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    var busquedaProducto by rememberSaveable { mutableStateOf("") }

    val focusManagerRepo = LocalFocusManager.current
    val keyboardControllerRepo = LocalSoftwareKeyboardController.current
    val densityRepo = LocalDensity.current
    val isKeyboardVisibleRepo =
        androidx.compose.foundation.layout.WindowInsets.ime.getBottom(densityRepo) > 0

    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleRepo -> {
                keyboardControllerRepo?.hide(); focusManagerRepo.clearFocus(force = true)
            }

            productoPendienteConfirmar != null -> onDescartarAdicionExtra()
            proveedorAbierto != null -> proveedorAbierto = null
            else -> {}
        }
    }

    // Escudo anti doble pedido
    productoPendienteConfirmar?.let { prod ->
        DialogoAntiDoblePedido(
            prod = prod,
            info = enCaminoPorProducto[prod.id],
            cantidadExtraPropuesta = cantidadExtraPropuesta,
            onConfirmar = onConfirmarAdicionExtra,
            onDescartar = onDescartarAdicionExtra
        )
    }

    // El detalle del proveedor se cierra SOLO cuando el envío terminó con ÉXITO
    // (evento explícito del ViewModel). Si el envío falla, la persona queda en el
    // detalle con su borrador restaurado para reintentar — jamás se le bota.
    LaunchedEffect(envioExitosoProveedor) {
        val prov = envioExitosoProveedor ?: return@LaunchedEffect
        if (proveedorAbierto == prov) proveedorAbierto = null
        onConsumirEnvioExitoso()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val totalWidth = maxWidth
                val isPantallaGrande = totalWidth >= 960.dp
                val isPantallaMediana = totalWidth in 720.dp..959.dp

                val paddingHorizontal =
                    if (isPantallaGrande) 24.dp else if (isPantallaMediana) 16.dp else 12.dp
                val paddingVertical = if (isPantallaGrande) 16.dp else 12.dp
                val espacioEntreColumnas = if (isPantallaGrande) 16.dp else 12.dp
                val paddingTarjeta = if (isPantallaGrande) 18.dp else 14.dp

                // Directorio: una sola lista (búsqueda por texto). Sin pestañas de filtro.
                // Incluye a los sin afiliar como una fila más; nada se esconde.
                val gruposFiltrados = remember(productosAgrupadosPorProveedor, busquedaProducto) {
                    productosAgrupadosPorProveedor.mapValues { (_, prods) ->
                        prods.filter { p ->
                            busquedaProducto.isBlank() ||
                                    p.name.contains(busquedaProducto, ignoreCase = true) ||
                                    p.category.contains(busquedaProducto, ignoreCase = true) ||
                                    p.laboratory.contains(busquedaProducto, ignoreCase = true)
                        }
                    }.filter { it.value.isNotEmpty() }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
                    horizontalArrangement = Arrangement.spacedBy(espacioEntreColumnas)
                ) {
                    if (proveedorAbierto != null) {
                        val nombreAbierto = proveedorAbierto ?: return@Row
                        val esSinProveedorAbierto = esProveedorPlaceholder(nombreAbierto)
                        val carroAbierto = if (esSinProveedorAbierto) {
                            val agregado = mutableMapOf<String, Int>()
                            pedidosPorProveedor.forEach { (prov, carro) ->
                                if (esProveedorPlaceholder(prov)) {
                                    carro.forEach { (idProd, cant) ->
                                        agregado[idProd] = (agregado[idProd] ?: 0) + cant
                                    }
                                }
                            }
                            agregado
                        } else {
                            pedidosPorProveedor[nombreAbierto] ?: emptyMap()
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            ReposicionDetalleProveedor(
                                proveedorNombre = nombreAbierto,
                                productos = if (esSinProveedorAbierto) {
                                    productosAgrupadosPorProveedor.entries
                                        .filter { esProveedorPlaceholder(it.key) }
                                        .flatMap { it.value }
                                } else {
                                    productosAgrupadosPorProveedor[nombreAbierto] ?: emptyList()
                                },
                                carroProv = carroAbierto,
                                enCaminoPorProducto = enCaminoPorProducto,
                                proveedores = proveedores,
                                simboloMoneda = simboloMoneda,
                                esSinProveedor = esSinProveedorAbierto,
                                pedidoActivo = pedidosActivos.firstOrNull {
                                    it.proveedorNombre == nombreAbierto
                                },
                                enviandoPedido = enviandoPedido,
                                s = s,
                                ordenEnCamino = pedidosGuardados.firstOrNull {
                                    it.proveedorNombre.equals(nombreAbierto, ignoreCase = true) &&
                                    it.estado == "ENVIADO" &&
                                    it.recepciones.isEmpty()
                                },
                                contribuidoresProv = contribuidoresCarrito[nombreAbierto].orEmpty(),
                                onVolver = { proveedorAbierto = null },
                                onModificarCantidad = { prod, delta ->
                                    onModificarCantidadProducto(prod, delta)
                                },
                                onReponerSugeridos = {
                                    onReponerSugeridosProveedor(nombreAbierto)
                                },
                                onLimpiarPedido = {
                                    onLimpiarPedidoProveedor(nombreAbierto)
                                },
                                onRealizarPedido = { pedido ->
                                    // El panel NO se cierra aquí: se cierra solo cuando
                                    // el envío termina con éxito (carrito consumido). Si
                                    // falla, la persona queda donde estaba para reintentar.
                                    onRealizarPedido(pedido)
                                },
                                onVincular = onVincularProducto
                            )
                        }
                    } else {
                        // Columna izquierda: Directorio de proveedores y productos para armar pedidos (~58%)
                        Box(
                            modifier = Modifier
                                .weight(1.35f)
                                .fillMaxHeight()
                        ) {
                            Surface(
                                color = FDColors.Surface,
                                shape = RoundedCornerShape(s.radiusCard),
                                border = BorderStroke(s.borderWidth, FDColors.Border),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Verdad del catálogo: cargando / error / vacío real / lista.
                                // "Vacío" solo se declara cuando la carga terminó SIN error;
                                // jamás se confunde con "aún no llega la data".
                                if (productosAgrupadosPorProveedor.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(s.padCardLarge),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            cargando -> Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                                            ) {
                                                CircularProgressIndicator(color = FDColors.Primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                                                Text(
                                                    text = "Cargando catálogo de productos…",
                                                    style = FDType.Body.copy(fontSize = 13.5.sp),
                                                    color = FDColors.TextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            errorEscucha != null -> Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                                            ) {
                                                Text(
                                                    text = "No se pudo cargar el catálogo",
                                                    style = FDType.Heading2.copy(fontSize = 17.sp),
                                                    color = FDColors.TextPrimary,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "Motivo real: $errorEscucha. Usa el botón REINTENTAR de arriba.",
                                                    style = FDType.Body.copy(fontSize = 13.sp),
                                                    color = FDColors.TextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            else -> Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                                            ) {
                                                Text(
                                                    text = "Catálogo de Productos Vacío",
                                                    style = FDType.Heading2.copy(fontSize = 17.sp),
                                                    color = FDColors.TextPrimary
                                                )
                                                Text(
                                                    text = "No se encontraron productos registrados en el inventario. Al agregar productos y asignarles proveedor o laboratorio, se organizarán aquí.",
                                                    style = FDType.Body.copy(fontSize = 13.sp),
                                                    color = FDColors.TextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    ReposicionDirectorio(
                                        gruposFiltrados = gruposFiltrados,
                                        pedidosPorProveedor = pedidosPorProveedor,
                                        enCaminoPorProducto = enCaminoPorProducto,
                                        simboloMoneda = simboloMoneda,
                                        busquedaProducto = busquedaProducto,
                                        s = s,
                                        listaState = listaState,
                                        onCambiarBusqueda = { busquedaProducto = it },
                                        onAbrirProveedor = { proveedorAbierto = it },
                                        onReponerSugeridosProveedor = onReponerSugeridosProveedor
                                    )
                                }
                            }
                        }

                        // Columna derecha: Pedidos en camino por recibir (con botón Recibir Mercadería) (~42%)
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                        ) {
                            ReposicionPanelPedidos(
                                pedidosGuardados = pedidosGuardados,
                                simboloMoneda = simboloMoneda,
                                s = s,
                                paddingTarjeta = paddingTarjeta,
                                onRecibirMercaderia = onRecibirMercaderia,
                                onCerrarConAjuste = onCerrarConAjuste,
                                onDescartarProducto = onDescartarProducto,
                                onCancelarPedido = onCancelarPedido
                            )
                        }
                    }
                }
            }
    }
}
