package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.datos.EntradaBitacoraPedido
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.datos.RecepcionEntrega
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.compras.logica.MaquinaEstadosPedido
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

/**
 * Detalle de un pedido REALIZADO: consulta con pestañas (productos, entregas, bitácora),
 * recepción de mercadería y resolución de lo que nunca llegará (cancelar, cerrar con
 * ajuste o descartar por producto). Crecer (+5 del jefe) entra por enviar; encoger
 * por descarte. Sin edición libre ni borrado: la historia no se reescribe.
 */
@Composable
fun ReposicionDetallePedidoRealizado(
    pedido: PedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    onVolver: () -> Unit,
    onRecibirMercaderia: () -> Unit = {},
    onCerrarConAjuste: () -> Unit = {},
    onCancelar: () -> Unit = {},
    onDescartarProducto: (String) -> Unit = {},
    // Solo lectura (historial/vitrina): muestra todo, no ofrece ninguna acción.
    soloLectura: Boolean = false,
    // Anexar papel tardío: solo cuando hay mercadería sin comprobante (orden con S/C).
    mostrarAnexarFactura: Boolean = false,
    anexandoFactura: Boolean = false,
    onAnexarFactura: (String, Double, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    var confirmarAjuste by remember { mutableStateOf(false) }
    var confirmarCancelacion by remember { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var productoParaDescartar by remember { mutableStateOf<ItemPedidoCompra?>(null) }
    var mostrarDialogoAnexar by remember { mutableStateOf(false) }
    // Pestañas del detalle: cada historia en su carril, cero scroll infinito.
    var tabDetalle by remember(pedido.id) { mutableIntStateOf(0) } // 0 productos · 1 entregas · 2 bitácora

    val puedeRecibir = MaquinaEstadosPedido.puedeRecibir(pedido.estado)
    val puedeCancelar = MaquinaEstadosPedido.puedeCancelar(pedido.estado, pedido.recepciones.size)
    val puedeCerrarConAjuste = MaquinaEstadosPedido.puedeCerrarConAjuste(pedido.estado, pedido.recepciones.size)
    val esPendiente = MaquinaEstadosPedido.perteneceAEnCamino(pedido.estado)

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
                onClick = onVolver,
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

            // Menú ⋮ : las acciones secundarias (cancelar, cerrar con ajuste) viven aquí
            // para que el cuerpo muestre solo RECIBIR MERCADERÍA. Sin opciones, sin icono.
            // En solo lectura (historial) no hay menú: la historia no se toca.
            if (!soloLectura && (puedeCancelar || puedeCerrarConAjuste)) {
                Box {
                    IconButton(
                        onClick = { mostrarMenu = true },
                        modifier = Modifier.size(s.iconMedium)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = FDColors.TextPrimary,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }
                    DropdownMenu(
                        expanded = mostrarMenu,
                        onDismissRequest = { mostrarMenu = false },
                        containerColor = FDColors.SurfaceElevated
                    ) {
                        if (puedeCancelar) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Cancelar orden",
                                        style = FDType.Body.copy(fontSize = 12.5.sp),
                                        color = FDColors.Error
                                    )
                                },
                                onClick = {
                                    mostrarMenu = false
                                    confirmarCancelacion = true
                                }
                            )
                        }
                        if (puedeCerrarConAjuste) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Cerrar con ajuste",
                                        style = FDType.Body.copy(fontSize = 12.5.sp),
                                        color = FDColors.TextPrimary
                                    )
                                },
                                onClick = {
                                    mostrarMenu = false
                                    confirmarAjuste = true
                                }
                            )
                        }
                    }
                }
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

        // Pestañas subrayadas con contador: productos, entregas y bitácora
        // viven en carriles separados.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PestanaDetalle(
                texto = "PRODUCTOS",
                conteo = pedido.items.size,
                seleccionado = tabDetalle == 0,
                onClick = { tabDetalle = 0 },
                modifier = Modifier.weight(1f)
            )
            PestanaDetalle(
                texto = "ENTREGAS",
                conteo = pedido.recepciones.size,
                seleccionado = tabDetalle == 1,
                onClick = { tabDetalle = 1 },
                modifier = Modifier.weight(1f)
            )
            PestanaDetalle(
                texto = "BITÁCORA",
                conteo = pedido.bitacora.size,
                seleccionado = tabDetalle == 2,
                onClick = { tabDetalle = 2 },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

        // Items del pedido (solo consulta: crecer es +5 por enviar, encoger es descarte)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
        ) {
            if (tabDetalle == 0) {
                items(pedido.items, key = { it.productoId }) { item ->
                    FilaItemConsultaPedido(
                        item = item,
                        simboloMoneda = simboloMoneda,
                        s = s,
                        // Destino por línea: solo lo pendiente puede declararse "no llegará".
                        // Coca muere aquí; Pepsi sigue esperando. Sin switch global.
                        // En solo lectura no hay link: la historia no se toca.
                        mostrarNoLlegara = !soloLectura && esPendiente && item.saldoPendiente > 0,
                        onNoLlegara = { productoParaDescartar = item }
                    )
                }
            }
            if (tabDetalle == 2) {
                item(key = "bitacora") {
                    SeccionBitacora(pedido.bitacora, s = s)
                }
            }
            if (tabDetalle == 1) {
                item(key = "recepciones") {
                    SeccionRecepciones(
                        recepciones = pedido.recepciones,
                        simboloMoneda = simboloMoneda,
                        s = s
                    )
                }
            }
        }

        // Pie: total + acción
        // Total estilo carrito ENCIMA del botón, separado con raya:
        // "Total" a la izquierda, monto a la derecha.
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Total",
                        style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                        color = FDColors.TextPrimary
                    )
                }
                Text(
                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", pedido.totalInversion)}",
                    style = FDType.Numeric.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = FDColors.TextPrimary
                )
            }
            if (pedido.montoFacturadoReal > 0) {
                Text(
                    text = "FACTURADO: $simboloMoneda ${String.format(Locale.US, "%.2f", pedido.montoFacturadoReal)}",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = FDColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            // Papel tardío: hay mercadería sin comprobante. Nace la deuda, el stock ni se toca.
            if (mostrarAnexarFactura) {
                OutlinedButton(
                    onClick = { mostrarDialogoAnexar = true },
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(s.borderWidth, FDColors.Primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnSmallH * 0.85f)
                ) {
                    Text(
                        "ANEXAR FACTURA",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (!soloLectura && esPendiente) {
                // Pedido en camino: el cuerpo muestra SOLO recibir mercadería.
                // Cancelar y cerrar con ajuste viven en el menú ⋮ de la cabecera.
                if (puedeRecibir) {
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
                }
            }
    }

    DialogosPedidoEnviado(
        proveedorNombre = pedido.proveedorNombre,
        unidadesPendientes = pedido.items.sumOf { it.saldoPendiente },
        confirmarAjuste = confirmarAjuste,
        confirmarCancelacion = confirmarCancelacion,
        productoParaDescartar = productoParaDescartar,
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
        onConfirmarDescartar = { idProd ->
            productoParaDescartar = null
            onDescartarProducto(idProd)
        },
        onCancelarDescartar = { productoParaDescartar = null }
    )

    // Anexar papel tardío: número + total del papel + condición. Nace la deuda;
    // el stock ni se toca (ya entró). La fecha de emisión es hoy salvo que se indique.
    if (mostrarDialogoAnexar) {
        var numero by remember { mutableStateOf("") }
        var montoTxt by remember { mutableStateOf("") }
        var condicion by remember { mutableStateOf("Crédito") }
        var vencimiento by remember { mutableStateOf("") }
        var emision by remember { mutableStateOf("") }
        val montoNum = montoTxt.replace(',', '.').toDoubleOrNull() ?: 0.0
        val numeroLimpio = numero.trim().uppercase()
        val numeroValido = numeroLimpio.isNotBlank() &&
            numeroLimpio != "S/C" && !numeroLimpio.startsWith("S/C")
        // Fechas, si se escriben, con forma de fecha; basura no entra.
        val fechaValida: (String) -> Boolean = {
            it.isBlank() || Regex("^\\d{2}/\\d{2}/\\d{4}$").matches(it)
        }
        val puedeConfirmar = numeroValido && montoNum > 0.0 &&
            fechaValida(vencimiento.trim()) && fechaValida(emision.trim()) && !anexandoFactura
        AlertDialog(
            onDismissRequest = { if (!anexandoFactura) mostrarDialogoAnexar = false },
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "Anexar factura",
                    style = FDType.Heading3.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El papel llegó después de la mercadería. Nace la deuda en Cuentas; el stock no se mueve.",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it.uppercase() },
                        label = { Text("N° FACTURA", fontSize = 10.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = montoTxt,
                        onValueChange = { montoTxt = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("TOTAL DEL PAPEL", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Contado", "Crédito").forEach { cond ->
                            val sel = condicion == cond
                            Surface(
                                color = if (sel) FDColors.Primary.copy(alpha = 0.12f) else FDColors.Surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (sel) FDColors.Primary else FDColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { condicion = cond }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        cond.uppercase(),
                                        style = FDType.Label.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (sel) FontWeight.Black else FontWeight.Medium
                                        ),
                                        color = if (sel) FDColors.Primary else FDColors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                    if (condicion == "Crédito") {
                        OutlinedTextField(
                            value = vencimiento,
                            onValueChange = { vencimiento = it },
                            label = { Text("VENCE (dd/mm/aaaa, opcional)", fontSize = 10.sp) },
                            singleLine = true,
                            isError = !fechaValida(vencimiento.trim()),
                            supportingText = if (!fechaValida(vencimiento.trim())) ({
                                Text("Usa formato dd/mm/aaaa", fontSize = 9.sp)
                            }) else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = emision,
                        onValueChange = { emision = it },
                        label = { Text("EMITIDO (dd/mm/aaaa, hoy si vacío)", fontSize = 10.sp) },
                        singleLine = true,
                        isError = !fechaValida(emision.trim()),
                        supportingText = if (!fechaValida(emision.trim())) ({
                            Text("Usa formato dd/mm/aaaa", fontSize = 9.sp)
                        }) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoAnexar = false
                        onAnexarFactura(numeroLimpio, montoNum, condicion, vencimiento.trim(), emision.trim())
                    },
                    enabled = puedeConfirmar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    if (anexandoFactura) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(s.iconSmall),
                            color = FDColors.PrimaryText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "ANEXAR",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoAnexar = false },
                    enabled = !anexandoFactura,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "CANCELAR",
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        )
    }
}

private fun estadoLegiblePedido(estado: String): String = when (estado) {
    "ENTREGA_PARCIAL" -> "RECIBIDO PARCIAL"
    "RECIBIDO" -> "RECIBIDO"
    "COMPLETADA_AJUSTE" -> "CERRADO POR AJUSTE"
    "CANCELADO" -> "CANCELADO"
    else -> "ENVIADO"
}

/** Pestaña subrayada con contador sutil: la seleccionada lleva la línea inferior. */
@Composable
private fun PestanaDetalle(
    texto: String,
    conteo: Int,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "$texto [ $conteo ]",
            style = FDType.Label.copy(
                fontSize = 10.5.sp,
                fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = if (seleccionado) FDColors.Primary else FDColors.TextTertiary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(2.dp)
                .background(
                    if (seleccionado) FDColors.Primary else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun FilaItemConsultaPedido(
    item: ItemPedidoCompra,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    mostrarNoLlegara: Boolean = false,
    onNoLlegara: () -> Unit = {}
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
            // Link sutil por línea: entierra solo el faltante de ESTE producto.
            if (mostrarNoLlegara) {
                Text(
                    text = "No llegará el faltante (${item.saldoPendiente})",
                    style = FDType.Label.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = FDColors.Error,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onNoLlegara() }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
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
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.8f), thickness = 1.dp)
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
        // Línea de tiempo: punto de color por evento unidos por un hilo vertical.
        // Mismo contenido de siempre, ahora se lee como historia.
        val ordenada = bitacora.sortedByDescending { it.fechaMs }
        ordenada.forEachIndexed { index, entrada ->
            val tituloTipo = when (entrada.tipo) {
                "CREACION" -> "CREACIÓN"
                "CANCELACION" -> "CANCELACIÓN"
                else -> "EDICIÓN"
            }
            val colorTipo = when (entrada.tipo) {
                "CREACION" -> FDColors.Primary
                "CANCELACION" -> FDColors.Error
                else -> FDColors.Warning
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(colorTipo, CircleShape)
                    )
                    if (index < ordenada.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(FDColors.Border.copy(alpha = 0.6f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index < ordenada.lastIndex) 12.dp else 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "$tituloTipo · ${entrada.fecha}",
                        style = FDType.Label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = colorTipo
                    )
                    Text(
                        text = entrada.detalle.ifBlank { "Sin detalle" } +
                            (if (entrada.usuario.isNotBlank()) " — Por ${entrada.usuario}" else ""),
                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                        color = FDColors.TextSecondary
                    )
                }
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
