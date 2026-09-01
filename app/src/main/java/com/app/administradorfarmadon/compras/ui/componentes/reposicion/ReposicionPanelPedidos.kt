package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

/**
 * Panel de pedidos de Reposición. UNA sola lista, sin pestañas ni doble trabajo:
 *  - POR RECIBIR: pedidos que siguen en camino (enviados o parciales), con sus
 *    acciones: ingresar mercadería a stock, cerrar con ajuste, descartar faltantes
 *    o cancelar. Así la persona sabe exactamente cuál va a recibir.
 *  - HISTORIAL: pedidos ya resueltos (recibido, recibido parcial, cerrado por ajuste,
 *    cancelado), con su estado y su detalle. Si un pedido no se resolvió, sigue en POR RECIBIR.
 * No existe borrador: armar un pedido se hace en el catálogo del proveedor.
 */
@Composable
fun ReposicionPanelPedidos(
    pedidosGuardados: List<PedidoCompra>,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    paddingTarjeta: Dp,
    onEditarPedido: (PedidoCompra, List<ItemPedidoCompra>) -> Unit,
    onEliminarPedido: (PedidoCompra) -> Unit,
    procesandoEdicion: Boolean,
    procesandoEliminacion: Boolean,
    onRecibirMercaderia: (PedidoCompra) -> Unit,
    onCerrarConAjuste: (PedidoCompra) -> Unit,
    onDescartarProducto: (String, String) -> Unit,
    onCancelarPedido: (String) -> Unit
) {
    var pedidoAbiertoId by remember { mutableStateOf<String?>(null) }

    // Verdad vigente: el detalle siempre se resuelve desde la lista actual por id,
    // nunca desde una referencia vieja (otro usuario pudo editar o eliminar el pedido).
    val abierto = pedidoAbiertoId?.let { id -> pedidosGuardados.find { it.id == id } }

    // El botón atrás del sistema cierra el detalle abierto antes de salir de la pestaña.
    BackHandler(enabled = pedidoAbiertoId != null) {
        pedidoAbiertoId = null
    }

    // Si el pedido abierto ya no existe (lo borró otro usuario), se cierra solo.
    LaunchedEffect(pedidosGuardados) {
        if (pedidoAbiertoId != null && abierto == null) {
            pedidoAbiertoId = null
        }
    }

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(s.radiusCard),
        border = BorderStroke(s.borderWidth, FDColors.Border),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingTarjeta)
        ) {
            if (abierto != null) {
                ReposicionDetallePedidoRealizado(
                    pedido = abierto,
                    simboloMoneda = simboloMoneda,
                    procesandoEdicion = procesandoEdicion,
                    procesandoEliminacion = procesandoEliminacion,
                    s = s,
                    onVolver = { pedidoAbiertoId = null },
                    onEditarPedido = { pedido, items ->
                        onEditarPedido(pedido, items)
                    },
                    onEliminarPedido = { pedido ->
                        onEliminarPedido(pedido)
                        pedidoAbiertoId = null
                    },
                    onRecibirMercaderia = { onRecibirMercaderia(abierto) },
                    onCerrarConAjuste = { onCerrarConAjuste(abierto) },
                    onCancelar = { onCancelarPedido(abierto.id) }
                )
            } else {
                // UNA sola lista de pedidos: primero lo que está POR RECIBIR (con sus
                // acciones), luego el HISTORIAL de lo ya resuelto. Cero doble trabajo.
                val pedidosPorRecibir = remember(pedidosGuardados) {
                    pedidosGuardados
                        .filter { it.estado == "ENVIADO" || it.estado == "ENTREGA_PARCIAL" }
                        .sortedByDescending { it.fechaEmisionMs }
                }
                val pedidosHistorial = remember(pedidosGuardados) {
                    pedidosGuardados
                        .filter { it.estado != "ENVIADO" && it.estado != "ENTREGA_PARCIAL" }
                        .sortedByDescending { it.fechaEmisionMs }
                }

                if (pedidosGuardados.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(s.padCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay pedidos. Cuando envíes un pedido al proveedor, aparecerá aquí.",
                            style = FDType.BodySmall.copy(fontSize = 12.sp),
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                    ) {
                        if (pedidosPorRecibir.isNotEmpty()) {
                            item(key = "seccion_por_recibir") {
                                EncabezadoSeccion("POR RECIBIR")
                            }
                            items(pedidosPorRecibir, key = { it.id }) { pedido ->
                                TarjetaPedidoEnviado(
                                    pedido = pedido,
                                    simboloMoneda = simboloMoneda,
                                    onRecibirMercaderia = { onRecibirMercaderia(pedido) },
                                    onCerrarConAjuste = { onCerrarConAjuste(pedido) },
                                    onDescartarProducto = { prodId ->
                                        onDescartarProducto(pedido.id, prodId)
                                    },
                                    onCancelar = { onCancelarPedido(pedido.id) },
                                    onAbrirDetalle = { pedidoAbiertoId = pedido.id }
                                )
                            }
                        }
                        if (pedidosHistorial.isNotEmpty()) {
                            item(key = "seccion_historial") {
                                EncabezadoSeccion("HISTORIAL")
                            }
                            items(pedidosHistorial, key = { it.id }) { pedido ->
                                FilaPedidoRealizado(
                                    pedido = pedido,
                                    simboloMoneda = simboloMoneda,
                                    s = s,
                                    onClick = { pedidoAbiertoId = pedido.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoSeccion(texto: String) {
    Text(
        text = texto,
        style = FDType.Label.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        ),
        color = FDColors.Primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun FilaPedidoRealizado(
    pedido: PedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    onClick: () -> Unit
) {
    val (etiquetaEstado, colorEstado) = when (pedido.estado) {
        "ENTREGA_PARCIAL" -> "RECIBIDO PARCIAL" to FDColors.Warning
        "RECIBIDO" -> "RECIBIDO" to FDColors.Success
        "COMPLETADA_AJUSTE" -> "CERRADO POR AJUSTE" to FDColors.Warning
        "CANCELADO" -> "CANCELADO" to FDColors.Error
        else -> "ENVIADO" to FDColors.Primary
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = pedido.proveedorNombre.uppercase(),
                    style = FDType.Label.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ORDEN: ${pedido.numeroOrden.ifBlank { "SIN NRO" }}",
                        style = FDType.Numeric.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = FDColors.TextTertiary
                    )
                    Surface(
                        color = colorEstado.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, colorEstado.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = etiquetaEstado,
                            style = FDType.Label.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = colorEstado,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    color = FDColors.TextPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${pedido.totalProductos} PRODUCTOS",
                        style = FDType.Label.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = FDColors.Border.copy(alpha = 0.3f)
            )
        }
    }
}
