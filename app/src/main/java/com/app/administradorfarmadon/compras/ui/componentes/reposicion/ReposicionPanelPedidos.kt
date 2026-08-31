package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

/**
 * Panel de pedidos de Reposición. Dos situaciones distintas:
 *  - REALIZADOS: pedidos hechos (consulta). Al tocar un proveedor se abre su pedido.
 *  - RECIBIR: mercadería pendiente de ingreso (asentar recepción, ajustes, cancelación).
 * No existe borrador: armar un pedido se hace en el catálogo del proveedor.
 */
@Composable
fun ReposicionPanelPedidos(
    pedidosGuardados: List<PedidoCompra>,
    subTabPedidosDerecha: String,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    paddingTarjeta: Dp,
    onSeleccionarSubTab: (String) -> Unit,
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

    val pedidosPendientes = remember(pedidosGuardados) {
        pedidosGuardados.filter { it.estado == "ENVIADO" || it.estado == "ENTREGA_PARCIAL" }
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
                    }
                )
            } else {
                // Tabs: REALIZADOS (consulta) y RECIBIR (ingreso de mercadería)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                ) {
                    listOf(
                        "REALIZADOS" to "REALIZADOS",
                        "RECIBIR" to "RECIBIR"
                    ).forEach { (clave, etiqueta) ->
                        val isSelected = subTabPedidosDerecha == clave
                        Column(
                            modifier = Modifier
                                .clickable { onSeleccionarSubTab(clave) }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                            ) {
                                Text(
                                    text = etiqueta,
                                    style = FDType.Label.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                                )
                                Surface(
                                    color = if (isSelected) FDColors.Primary.copy(alpha = 0.1f) else FDColors.TextPrimary.copy(
                                        alpha = 0.05f
                                    ),
                                    shape = FDShapes.XSmall
                                ) {
                                    Text(
                        text = "${if (subTabPedidosDerecha == "RECIBIR") pedidosPendientes.size else pedidosGuardados.size}",
                                        style = FDType.Label.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (isSelected) FDColors.Primary else FDColors.TextTertiary,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 1.dp
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            AnimatedVisibility(visible = isSelected) {
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(24.dp)
                                        .clip(FDShapes.Full)
                                        .background(FDColors.Primary)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(s.sm))

                if (subTabPedidosDerecha == "RECIBIR") {
                    // Situación RECIBIR: asentar la llegada de mercadería
                    if (pedidosPendientes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(s.padCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tienes pedidos pendientes de recibir. Los pedidos realizados aparecerán aquí para asentar la llegada de mercadería.",
                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                        ) {
                            items(
                                pedidosPendientes,
                                key = { it.id }) { pedidoGuardado ->
                                TarjetaPedidoEnviado(
                                    pedido = pedidoGuardado,
                                    simboloMoneda = simboloMoneda,
                                    onRecibirMercaderia = { onRecibirMercaderia(pedidoGuardado) },
                                    onCerrarConAjuste = { onCerrarConAjuste(pedidoGuardado) },
                                    onDescartarProducto = { prodId ->
                                        onDescartarProducto(pedidoGuardado.id, prodId)
                                    },
                                    onCancelar = { onCancelarPedido(pedidoGuardado.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Situación REALIZADOS: consulta del pedido hecho
                    if (pedidosGuardados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(s.padCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no hay pedidos realizados. Cuando envíes un pedido, aparecerá aquí con su detalle.",
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
                            items(
                                pedidosGuardados,
                                key = { it.id }) { pedido ->
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
private fun FilaPedidoRealizado(
    pedido: PedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    onClick: () -> Unit
) {
    val estadoTexto = when (pedido.estado) {
        "ENTREGA_PARCIAL" -> "ENTREGA PARCIAL"
        "RECIBIDO" -> "RECIBIDO"
        "COMPLETADA_AJUSTE" -> "CERRADO CON AJUSTE"
        "CANCELADO" -> "CANCELADO"
        else -> "REALIZADO"
    }
    val estadoColor = when (pedido.estado) {
        "ENTREGA_PARCIAL" -> FDColors.Warning
        "RECIBIDO" -> FDColors.Primary
        "COMPLETADA_AJUSTE" -> FDColors.TextTertiary
        "CANCELADO" -> FDColors.Error
        else -> FDColors.Success
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(FDColors.TextPrimary.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = pedido.proveedorNombre,
                    style = FDType.Heading3.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        pedido.numeroOrden.takeIf { it.isNotBlank() },
                        pedido.fechaEmision.takeIf { it.isNotBlank() }
                    ).joinToString("  ·  "),
                    style = FDType.BodySmall.copy(fontSize = 11.sp),
                    color = FDColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    color = estadoColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(s.radiusChip),
                    border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = estadoTexto,
                        style = FDType.Label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = estadoColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "$simboloMoneda ${String.format(Locale.US, "%,.2f", pedido.totalInversion)}",
                    style = FDType.Numeric.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "${pedido.totalProductos} productos · ${pedido.totalUnidades} unidades",
                    style = FDType.Label.copy(fontSize = 9.5.sp),
                    color = FDColors.TextTertiary
                )
            }
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))
    }
}
