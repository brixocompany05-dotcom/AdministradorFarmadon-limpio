package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.datos.EntradaBitacoraPedido
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.datos.RecepcionEntrega
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

/**
 * Detalle de un pedido REALIZADO: consulta, edición con bitácora interna
 * (qué cambió, quién, cuándo) y eliminación total del rastro si el pedido muere.
 * Para los pedidos que siguen en camino, aquí también se recibe la mercadería
 * (botón RECIBIR MERCADERÍA) y se resuelve lo que nunca llegará (cancelar o cerrar
 * con ajuste). El pedido resuelto queda como historial en la misma lista.
 */
@Composable
fun ReposicionDetallePedidoRealizado(
    pedido: PedidoCompra,
    simboloMoneda: String,
    procesandoEdicion: Boolean,
    procesandoEliminacion: Boolean,
    s: MedidaAdaptativa,
    onVolver: () -> Unit,
    onEditarPedido: (PedidoCompra, List<ItemPedidoCompra>) -> Unit,
    onEliminarPedido: (PedidoCompra) -> Unit,
    onRecibirMercaderia: () -> Unit = {},
    onCerrarConAjuste: () -> Unit = {},
    onCancelar: () -> Unit = {}
) {
    var modoEdicion by remember { mutableStateOf(false) }
    var itemsEditando by remember { mutableStateOf<List<ItemPedidoCompra>?>(null) }
    var confirmarEliminacion by remember { mutableStateOf(false) }
    var confirmarAjuste by remember { mutableStateOf(false) }
    var confirmarCancelacion by remember { mutableStateOf(false) }

    val itemsVisibles = itemsEditando ?: pedido.items
    val hayCambios = itemsEditando != null && itemsEditando != pedido.items
    val puedeGuardar = hayCambios && itemsEditando.orEmpty().any { it.cantidad > 0 }
    val esPendiente = pedido.estado == "ENVIADO" || pedido.estado == "ENTREGA_PARCIAL"

    // En modo edición, el botón atrás del sistema cancela la edición (vuelve a consulta).
    BackHandler(enabled = modoEdicion) {
        modoEdicion = false
        itemsEditando = null
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // Cabecera profesional integrada
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapTiny)
        ) {
            IconButton(
                onClick = {
                    if (modoEdicion) {
                        modoEdicion = false
                        itemsEditando = null
                    } else {
                        onVolver()
                    }
                },
                modifier = Modifier.size(s.iconMedium)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    null,
                    tint = FDColors.TextPrimary,
                    modifier = Modifier.size(s.iconSmall)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pedido.proveedorNombre.uppercase(),
                    style = FDType.Label.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "DOC: ${pedido.numeroOrden.ifBlank { "SIN NRO" }} · ${estadoLegiblePedido(pedido.estado)}",
                    style = FDType.Numeric.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextTertiary
                )
            }
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

        if (pedido.notas.isNotBlank()) {
            Text(
                text = "Motivo: ${pedido.notas}",
                style = FDType.BodySmall.copy(fontSize = 11.sp),
                color = FDColors.TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Items del pedido (consulta o edición)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
        ) {
            items(itemsVisibles, key = { it.productoId }) { item ->
                if (modoEdicion) {
                    FilaItemEdicionPedido(
                        item = item,
                        simboloMoneda = simboloMoneda,
                        s = s,
                        onCambiarCantidad = { delta ->
                            itemsEditando = itemsEditando?.map { itm ->
                                if (itm.productoId == item.productoId) {
                                    itm.copy(cantidad = (itm.cantidad + delta).coerceAtLeast(itm.cantidadRecibida))
                                } else itm
                            }
                        },
                        onQuitar = {
                            itemsEditando = itemsEditando?.filterNot { it.productoId == item.productoId }
                        }
                    )
                } else {
                    FilaItemConsultaPedido(item = item, simboloMoneda = simboloMoneda, s = s)
                }
            }
            item(key = "bitacora") {
                SeccionBitacora(pedido.bitacora, s = s)
            }
            item(key = "recepciones") {
                SeccionRecepciones(
                    recepciones = pedido.recepciones,
                    simboloMoneda = simboloMoneda,
                    s = s
                )
            }
        }

        // Pie: edición o total
        if (modoEdicion) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
            ) {
                OutlinedButton(
                    onClick = {
                        modoEdicion = false
                        itemsEditando = null
                    },
                    enabled = !procesandoEdicion,
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextSecondary),
                    modifier = Modifier
                        .weight(0.4f)
                        .height(s.btnMediumH)
                ) {
                    Text(
                        "CANCELAR",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Button(
                    onClick = { itemsEditando?.let { onEditarPedido(pedido, it) } },
                    enabled = !procesandoEdicion && puedeGuardar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusButton),
                    modifier = Modifier
                        .weight(1f)
                        .height(s.btnMediumH)
                ) {
                    if (procesandoEdicion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(s.iconSmall),
                            color = FDColors.PrimaryText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "GUARDAR CAMBIOS",
                            style = FDType.Label.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }
        } else {
            if (esPendiente) {
                // Pedido en camino: aquí se recibe la mercadería y se resuelve
                // lo que nunca llegará. Nada queda a medias sin explicación.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    Button(
                        onClick = onRecibirMercaderia,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusButton),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.btnMediumH)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            null,
                            modifier = Modifier.size(s.iconSmall)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "RECIBIR MERCADERÍA",
                            style = FDType.Label.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                        )
                    }
                    if (pedido.estado == "ENVIADO") {
                        OutlinedButton(
                            onClick = { confirmarCancelacion = true },
                            shape = RoundedCornerShape(s.radiusButton),
                            border = BorderStroke(s.borderWidth, FDColors.Warning.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Warning),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.btnSmallH * 0.85f)
                        ) {
                            Text(
                                "CANCELAR ORDEN (no llegará)",
                                style = FDType.Label.copy(
                                    fontSize = s.textLabel.value.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { confirmarAjuste = true },
                            shape = RoundedCornerShape(s.radiusButton),
                            border = BorderStroke(s.borderWidth, FDColors.Warning.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Warning),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.btnSmallH * 0.85f)
                        ) {
                            Text(
                                "CERRAR CON AJUSTE (quiebre de stock)",
                                style = FDType.Label.copy(
                                    fontSize = s.textLabel.value.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pedido.montoFacturadoReal > 0) {
                    Text(
                        text = "FACTURADO: $simboloMoneda ${String.format(Locale.US, "%.2f", pedido.montoFacturadoReal)}",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.Primary
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "TOTAL INVERSIÓN",
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                        color = FDColors.TextTertiary
                    )
                    Text(
                        text = "$simboloMoneda ${String.format(Locale.US, "%.2f", pedido.totalInversion)}",
                        style = FDType.Numeric.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                }
            }
        }
    }

    // Confirmación: eliminar borra el pedido completo y todo su rastro
    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "¿Eliminar este pedido?",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Text(
                    "Se borrará el pedido completo de ${pedido.proveedorNombre}, incluida su bitácora. Esta acción no se puede deshacer.",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarEliminacion = false
                        onEliminarPedido(pedido)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Error,
                        contentColor = FDColors.Surface
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    if (procesandoEliminacion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(s.iconSmall),
                            color = FDColors.Surface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "SÍ, ELIMINAR TODO",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmarEliminacion = false },
                    enabled = !procesandoEliminacion,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "CANCELAR",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    DialogosPedidoEnviado(
        proveedorNombre = pedido.proveedorNombre,
        unidadesPendientes = pedido.items.sumOf { it.saldoPendiente },
        confirmarAjuste = confirmarAjuste,
        confirmarCancelacion = confirmarCancelacion,
        productoParaDescartar = null,
        s = s,
        onConfirmarAjuste = {
            confirmarAjuste = false
            onCerrarConAjuste()
        },
        onCancelarAjuste = { confirmarAjuste = false },
        onConfirmarCancelacion = {
            confirmarCancelacion = false
            onCancelar()
        },
        onCancelarCancelacion = { confirmarCancelacion = false },
        onConfirmarDescartar = {},
        onCancelarDescartar = {}
    )
}

private fun estadoLegiblePedido(estado: String): String = when (estado) {
    "ENTREGA_PARCIAL" -> "RECIBIDO PARCIAL"
    "RECIBIDO" -> "RECIBIDO"
    "COMPLETADA_AJUSTE" -> "CERRADO POR AJUSTE"
    "CANCELADO" -> "CANCELADO"
    else -> "ENVIADO"
}

@Composable
private fun FilaItemConsultaPedido(
    item: ItemPedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = item.productoNombre,
                style = FDType.Body.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = FDColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.cantidad} ${item.presentacion.ifBlank { "Und" }} x $simboloMoneda ${String.format(Locale.US, "%.2f", item.precioCompra)}",
                style = FDType.BodySmall.copy(fontSize = 11.sp),
                color = FDColors.TextTertiary
            )
            if (item.cantidadRecibida > 0) {
                Text(
                    text = "Recibido ${item.cantidadRecibida} · Falta ${item.saldoPendiente}",
                    style = FDType.Label.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (item.saldoPendiente > 0) FDColors.Warning else FDColors.Success
                )
            }
        }
        Text(
            text = "$simboloMoneda ${String.format(Locale.US, "%.2f", item.subtotal)}",
            style = FDType.Numeric.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black
            ),
            color = FDColors.TextPrimary,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))
}

@Composable
private fun FilaItemEdicionPedido(
    item: ItemPedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    onCambiarCantidad: (Int) -> Unit,
    onQuitar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = item.productoNombre,
                style = FDType.Body.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = FDColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", item.precioCompra)} c/u",
                style = FDType.BodySmall.copy(fontSize = 11.sp),
                color = FDColors.TextTertiary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BotonMasMenos(
                icono = Icons.Default.Remove,
                habilitado = item.cantidad > item.cantidadRecibida,
                onClick = { onCambiarCantidad(-1) },
                s = s
            )
            Surface(
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(s.radiusInput * 0.55f),
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier.width(44.dp).height(s.btnMediumH)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${item.cantidad}",
                        style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = s.textInput.value.sp),
                        color = FDColors.TextPrimary
                    )
                }
            }
            BotonMasMenos(
                icono = Icons.Default.Add,
                habilitado = true,
                onClick = { onCambiarCantidad(1) },
                s = s
            )
            IconButton(
                onClick = onQuitar,
                modifier = Modifier
                    .size(s.btnMediumH)
                    .border(s.borderWidth, FDColors.Error.copy(alpha = 0.35f), RoundedCornerShape(s.radiusButton))
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Quitar producto",
                    tint = FDColors.Error,
                    modifier = Modifier.size(s.iconSmall * 0.9f)
                )
            }
        }
    }
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))
}

@Composable
private fun SeccionBitacora(
    bitacora: List<EntradaBitacoraPedido>,
    s: MedidaAdaptativa
) {
    Spacer(modifier = Modifier.height(s.gapMedium))
    Text(
        text = "BITÁCORA INTERNA",
        style = FDType.Label.copy(
            fontWeight = FontWeight.Black,
            fontSize = s.textLabel.value.sp,
            letterSpacing = 1.sp
        ),
        color = FDColors.TextTertiary
    )
    Spacer(modifier = Modifier.height(s.gapSmall * 0.5f))
    if (bitacora.isEmpty()) {
        Text(
            text = "Sin registros de bitácora.",
            style = FDType.BodySmall.copy(fontSize = 11.sp),
            color = FDColors.TextTertiary
        )
    } else {
        bitacora.sortedByDescending { it.fechaMs }.forEach { entrada ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (if (entrada.tipo == "CREACION") "CREACIÓN" else "EDICIÓN") + " · ${entrada.fecha}",
                        style = FDType.Label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (entrada.tipo == "CREACION") FDColors.Primary else FDColors.Warning
                    )
                }
                Text(
                    text = entrada.detalle.ifBlank { "Sin detalle" },
                    style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SeccionRecepciones(
    recepciones: List<RecepcionEntrega>,
    simboloMoneda: String,
    s: MedidaAdaptativa
) {
    Spacer(modifier = Modifier.height(s.gapMedium))
    Text(
        text = "ENTREGAS / RECEPCIONES",
        style = FDType.Label.copy(
            fontWeight = FontWeight.Black,
            fontSize = s.textLabel.value.sp,
            letterSpacing = 1.sp
        ),
        color = FDColors.TextTertiary
    )
    Spacer(modifier = Modifier.height(s.gapSmall * 0.5f))
    if (recepciones.isEmpty()) {
        Text(
            text = "Sin entregas registradas.",
            style = FDType.BodySmall.copy(fontSize = 11.sp),
            color = FDColors.TextTertiary
        )
    } else {
        recepciones.sortedByDescending { it.fechaMs }.forEach { rec ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECEPCIÓN · ${rec.fechaLegible}",
                        style = FDType.Label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = FDColors.Primary
                    )
                }
                Text(
                    text = "Factura: ${rec.numeroFactura.ifBlank { "S/C" }} · $simboloMoneda ${String.format(Locale.US, "%.2f", rec.montoFactura)}",
                    style = FDType.BodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Recibió: ${rec.usuarioNombre.ifBlank { "Sistema" }}",
                    style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}
