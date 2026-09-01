package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.compras.logica.ProductoEnCamino
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import java.util.Locale

/**
 * Pantalla de Reposición de un proveedor: se siente como una vista nueva de
 * escritorio. Barra superior arriba (volver + nombre + acción real), y debajo
 * dos zonas sin cajas anidadas: catálogo (izquierda) y carrito (derecha).
 * Las acciones del carrito viven dentro del carrito, nunca fuera.
 */
@Composable
fun ReposicionDetalleProveedor(
    proveedorNombre: String,
    productos: List<PharmProduct>,
    carroProv: Map<String, Int>,
    enCaminoPorProducto: Map<String, ProductoEnCamino>,
    proveedores: List<Proveedor>,
    simboloMoneda: String,
    esSinProveedor: Boolean,
    pedidoActivo: PedidoProveedor?,
    enviandoPedido: Boolean,
    s: MedidaAdaptativa,
    onVolver: () -> Unit,
    onModificarCantidad: (PharmProduct, Int) -> Unit,
    onReponerSugeridos: () -> Unit,
    onLimpiarPedido: () -> Unit,
    onRealizarPedido: (PedidoProveedor) -> Unit,
    onVincular: ((PharmProduct, Proveedor) -> Unit)? = null
) {
    var busquedaLocal by remember { mutableStateOf("") }
    var confirmarVaciado by remember { mutableStateOf(false) }

    val productosVisibles = remember(productos, busquedaLocal) {
        if (busquedaLocal.isBlank()) productos
        else productos.filter {
            it.name.contains(busquedaLocal, ignoreCase = true) ||
                    it.category.contains(busquedaLocal, ignoreCase = true) ||
                    it.laboratory.contains(busquedaLocal, ignoreCase = true) ||
                    it.code.contains(busquedaLocal)
        }
    }

    val criticosPendientes = productos.count {
        it.stock <= it.minStock &&
                (carroProv[it.id] ?: 0) == 0 &&
                (enCaminoPorProducto[it.id]?.unidades ?: 0) == 0
    }

    // Pedido en vivo: solo lo que tiene cantidad, en el mismo orden del catálogo.
    val itemsPedido = remember(productos, carroProv) {
        productos.mapNotNull { p ->
            val c = carroProv[p.id] ?: 0
            if (c > 0) Triple(p, c, c * p.purchasePrice) else null
        }
    }
    val unidadesPedidas = itemsPedido.sumOf { it.second }
    val subtotal = itemsPedido.sumOf { it.third }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV)
    ) {
        // ── Barra superior de pantalla ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            OutlinedButton(
                onClick = onVolver,
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

            Text(
                text = proveedorNombre,
                style = FDType.Heading1.copy(fontSize = s.textTitle.value.sp),
                color = FDColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (criticosPendientes > 0 && !esSinProveedor) {
                OutlinedButton(
                    onClick = onReponerSugeridos,
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Primary),
                    modifier = Modifier.height(s.btnMediumH)
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(s.iconSmall))
                    Spacer(Modifier.width(s.xs * 0.8f))
                    Text(
                        "PEDIR $criticosPendientes SUGERIDOS",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }

        HorizontalDivider(
            color = FDColors.Border.copy(alpha = 0.5f),
            thickness = s.separatorH,
            modifier = Modifier.padding(top = s.gapMedium)
        )

        // ── Workspace enmarcado: un solo cuadro que abraza catálogo y carrito ──
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(s.radiusCard),
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(s.padCard),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
            // Zona izquierda: catálogo del proveedor
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    OutlinedTextField(
                        value = busquedaLocal,
                        onValueChange = { busquedaLocal = it },
                        placeholder = { Text("Buscar producto, categoría o laboratorio…", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconSmall)) },
                        singleLine = true,
                        shape = RoundedCornerShape(s.radiusInput),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FDColors.SurfaceElevated,
                            unfocusedContainerColor = FDColors.SurfaceElevated,
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = FDColors.Border,
                            focusedTextColor = FDColors.TextPrimary,
                            unfocusedTextColor = FDColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                    )
                }

                Spacer(modifier = Modifier.height(s.gapSmall))

                if (productosVisibles.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (productos.isEmpty()) "Este proveedor aún no tiene productos afiliados."
                            else "Ningún producto coincide con la búsqueda.",
                            style = FDType.BodySmall.copy(fontSize = 12.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(productosVisibles, key = { it.id }) { prod ->
                            FilaProductoDetalleProveedor(
                                prod = prod,
                                cantPedir = carroProv[prod.id] ?: 0,
                                enCamino = enCaminoPorProducto[prod.id]?.unidades ?: 0,
                                simboloMoneda = simboloMoneda,
                                esSinProveedor = esSinProveedor,
                                proveedores = proveedores,
                                s = s,
                                onModificarCantidad = { delta -> onModificarCantidad(prod, delta) },
                                onVincular = onVincular
                            )
                        }
                    }
                }
            }

            // Separador vertical entre catálogo y pedido
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = s.gapSmall),
                thickness = 1.dp,
                color = FDColors.Border.copy(alpha = 0.5f)
            )

            // Zona derecha: carrito (sus acciones viven aquí, no fuera)
            Column(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(s.gapSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PEDIDO",
                        style = FDType.Heading3.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = s.textBody.value.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = if (itemsPedido.isEmpty()) "vacío"
                        else "${itemsPedido.size} productos · $unidadesPedidas unidades",
                        style = FDType.Label.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (itemsPedido.isEmpty()) FDColors.TextTertiary else FDColors.Primary
                    )
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                if (itemsPedido.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no agregaste productos. Usa los botones [+] del catálogo.",
                            style = FDType.BodySmall.copy(fontSize = 12.sp),
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                    ) {
                        items(itemsPedido, key = { it.first.id }) { (prod, cant, subtotalItem) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = prod.name,
                                        style = FDType.Body.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$cant ${prod.empaque.ifBlank { "Und" }} x $simboloMoneda ${String.format(Locale.US, "%.2f", prod.purchasePrice)}",
                                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                                        color = FDColors.TextTertiary
                                    )
                                }
                                Text(
                                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", subtotalItem)}",
                                    style = FDType.Numeric.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = FDColors.TextPrimary
                                )
                            }
                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))
                        }
                    }
                }

                // Pie del carrito: total + referencia + acciones (dentro del carrito)
                Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SUBTOTAL ESTIMADO",
                            style = FDType.Label.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FDColors.TextSecondary
                        )
                        Text(
                            text = "$simboloMoneda ${String.format(Locale.US, "%.2f", subtotal)}",
                            style = FDType.Heading1.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = s.textSubtitle.value.sp
                            ),
                            color = FDColors.TextPrimary
                        )
                    }

                    Text(
                        text = "Precios de referencia de la última compra. Si cambian, se actualizan al recibir la mercadería.",
                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                        color = FDColors.TextTertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                    ) {
                        if (itemsPedido.isNotEmpty()) {
                            IconButton(
                                onClick = { confirmarVaciado = true },
                                modifier = Modifier
                                    .size(s.btnMediumH)
                                    .border(s.borderWidth, FDColors.Warning.copy(alpha = 0.45f), RoundedCornerShape(s.radiusButton))
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Vaciar pedido",
                                    tint = FDColors.Warning,
                                    modifier = Modifier.size(s.iconSmall)
                                )
                            }
                        }

                        Button(
                            onClick = { pedidoActivo?.let(onRealizarPedido) },
                            enabled = pedidoActivo?.tieneItems == true && !enviandoPedido,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FDColors.Primary,
                                contentColor = FDColors.PrimaryText,
                                disabledContainerColor = FDColors.TextPrimary.copy(alpha = 0.05f),
                                disabledContentColor = FDColors.TextTertiary.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(s.radiusButton),
                            modifier = Modifier
                                .weight(1f)
                                .height(s.btnMediumH)
                        ) {
                            if (enviandoPedido) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(s.iconSmall),
                                    color = FDColors.PrimaryText,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "REALIZAR PEDIDO",
                                    style = FDType.Label.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Confirmación contextual antes de vaciar el pedido
    if (confirmarVaciado) {
        AlertDialog(
            onDismissRequest = { confirmarVaciado = false },
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "¿Vaciar el pedido?",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Text(
                    "Se quitarán todos los productos agregados al pedido de $proveedorNombre. Esta acción no se puede deshacer.",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarVaciado = false
                        onLimpiarPedido()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "SÍ, VACIAR",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmarVaciado = false },
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
