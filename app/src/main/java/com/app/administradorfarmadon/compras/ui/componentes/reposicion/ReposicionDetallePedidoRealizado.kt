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
 * La recepción de mercadería vive en la pestaña RECIBIR, nunca aquí.
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
    onEliminarPedido: (PedidoCompra) -> Unit
) {
    var modoEdicion by remember { mutableStateOf(false) }
    var itemsEditando by remember { mutableStateOf<List<ItemPedidoCompra>?>(null) }
    var confirmarEliminacion by remember { mutableStateOf(false) }

    val puedeEditar = pedido.estado == "ENVIADO" &&
            pedido.recepciones.isEmpty() &&
            pedido.totalUnidadesRecibidas == 0

    // Sale del modo edición solo cuando el servidor confirma los cambios guardados.
    LaunchedEffect(pedido.items, procesandoEdicion) {
        val editando = itemsEditando
        if (!procesandoEdicion && modoEdicion && editando != null && pedido.items == editando) {
            modoEdicion = false
            itemsEditando = null
        }
    }

    val itemsVisibles = itemsEditando ?: pedido.items
    val hayCambios = itemsEditando != null && itemsEditando != pedido.items
    val puedeGuardar = hayCambios && itemsEditando.orEmpty().any { it.cantidad > 0 }
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

    // En modo edición, el botón atrás del sistema cancela la edición (vuelve a consulta).
    BackHandler(enabled = modoEdicion) {
        modoEdicion = false
        itemsEditando = null
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // Cabecera del detalle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
        ) {
            OutlinedButton(
                onClick = {
                    if (modoEdicion) {
                        modoEdicion = false
                        itemsEditando = null
                    } else {
                        onVolver()
                    }
                },
                shape = RoundedCornerShape(s.radiusButton),
                border = BorderStroke(s.borderWidth, FDColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextPrimary),
                modifier = Modifier.height(s.btnMediumH)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    null,
                    tint = FDColors.TextPrimary,
                    modifier = Modifier.size(s.iconSmall)
                )
                Spacer(Modifier.width(s.xs))
                Text(
                    "VOLVER",
                    style = FDType.Label.copy(
                        fontSize = s.textLabel.value.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = pedido.proveedorNombre,
                    style = FDType.Heading1.copy(fontSize = s.textTitle.value.sp),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        pedido.numeroOrden.takeIf { it.isNotBlank() },
                        pedido.fechaEmision.takeIf { it.isNotBlank() },
                        pedido.usuarioEmisor.takeIf { it.isNotBlank() }
                    ).joinToString("  ·  "),
                    style = FDType.BodySmall.copy(fontSize = s.textLabel.value.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (puedeEditar && !modoEdicion) {
                OutlinedButton(
                    onClick = {
                        itemsEditando = pedido.items.map { it.copy() }
                        modoEdicion = true
                    },
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(s.borderWidth, FDColors.Primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Primary),
                    modifier = Modifier.height(s.btnMediumH)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(s.iconSmall))
                    Spacer(Modifier.width(s.xs * 0.7f))
                    Text(
                        "EDITAR",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            if (puedeEditar && !modoEdicion) {
                OutlinedButton(
                    onClick = { confirmarEliminacion = true },
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(s.borderWidth, FDColors.Error.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Error),
                    modifier = Modifier.height(s.btnMediumH)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall))
                    Spacer(Modifier.width(s.xs * 0.7f))
                    Text(
                        "ELIMINAR",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Surface(
                color = estadoColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = estadoTexto,
                    style = FDType.Label.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = estadoColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${itemsVisibles.size} PRODUCTOS · ${itemsVisibles.sumOf { it.cantidad }} UNIDADES",
                        style = FDType.Label.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = s.textLabel.value.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = FDColors.TextSecondary
                    )
                    if (pedido.montoFacturadoReal > 0) {
                        Text(
                            text = "FACTURADO REAL: $simboloMoneda ${String.format(Locale.US, "%,.2f", pedido.montoFacturadoReal)}",
                            style = FDType.Label.copy(
                                fontSize = s.textLabel.value.sp * 0.95f,
                                fontWeight = FontWeight.Black
                            ),
                            color = FDColors.Primary
                        )
                    }
                }
                Text(
                    text = "$simboloMoneda ${String.format(Locale.US, "%,.2f", pedido.totalInversion)}",
                    style = FDType.Heading1.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = s.textSubtitle.value.sp
                    ),
                    color = FDColors.TextPrimary
                )
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
            text = "Sin registros de bitácora para este pedido.",
            style = FDType.BodySmall.copy(fontSize = 11.sp),
            color = FDColors.TextTertiary
        )
    } else {
        bitacora.sortedByDescending { it.fechaMs }.forEach { entrada ->
            Surface(
                color = FDColors.TextPrimary.copy(alpha = 0.03f),
                shape = RoundedCornerShape(s.radiusInput * 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (entrada.tipo == "CREACION") "CREACIÓN" else "EDICIÓN",
                            style = FDType.Label.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (entrada.tipo == "CREACION") FDColors.Primary else FDColors.Warning
                        )
                        Text(
                            text = entrada.fecha,
                            style = FDType.Label.copy(fontSize = 9.5.sp),
                            color = FDColors.TextTertiary
                        )
                    }
                    Text(
                        text = "Quién: ${entrada.usuario.ifBlank { "Sistema" }}",
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = "Qué: ${entrada.detalle.ifBlank { "Sin detalle" }}",
                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                        color = FDColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(s.gapTiny * 0.7f))
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
            text = "Este pedido aún no tiene entregas registradas.",
            style = FDType.BodySmall.copy(fontSize = 11.sp),
            color = FDColors.TextTertiary
        )
    } else {
        recepciones.sortedByDescending { it.fechaMs }.forEach { rec ->
            Surface(
                color = FDColors.TextPrimary.copy(alpha = 0.03f),
                shape = RoundedCornerShape(s.radiusInput * 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FACTURA ${rec.numeroFactura.ifBlank { "S/C" }}",
                            style = FDType.Label.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = FDColors.Primary
                        )
                        Text(
                            text = rec.fechaLegible,
                            style = FDType.Label.copy(fontSize = 9.5.sp),
                            color = FDColors.TextTertiary
                        )
                    }
                    Text(
                        text = "Monto: $simboloMoneda ${String.format(Locale.US, "%,.2f", rec.montoFactura)} · ${rec.items.sumOf { it.cantidadTotal }} unidades",
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = "Recibió: ${rec.usuarioNombre.ifBlank { "Sistema" }} · ${rec.condicionPago}",
                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                        color = FDColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(s.gapTiny * 0.7f))
        }
    }
}
