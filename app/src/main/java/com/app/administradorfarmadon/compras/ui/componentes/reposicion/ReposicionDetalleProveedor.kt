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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.compras.logica.ProductoEnCamino
import com.app.administradorfarmadon.compras.ui.componentes.CampoBuscadorModerno
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
    ordenEnCamino: PedidoCompra? = null,
    contribuidoresProv: Map<String, Map<String, Int>> = emptyMap(),
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

    // Orden visual estable: lo que se acaba arriba (fondo naranja), lo demás abajo.
    // Se ordena por stock, no por carrito, para que la fila no salte al marcar cantidad.
    val urgentesVisibles = remember(productosVisibles) {
        productosVisibles.filter {
            it.stock <= 0 || (it.minStock > 0 && it.stock <= it.minStock)
        }
    }
    val restoVisibles = remember(productosVisibles) {
        productosVisibles.filterNot {
            it.stock <= 0 || (it.minStock > 0 && it.stock <= it.minStock)
        }
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
                    ),
                    color = FDColors.TextPrimary
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
                        ),
                        color = FDColors.Primary
                    )
                }
            }
        }

        HorizontalDivider(
            color = FDColors.Border.copy(alpha = 0.5f),
            thickness = s.separatorH,
            modifier = Modifier.padding(top = s.gapMedium)
        )

        if (ordenEnCamino != null) {
            Surface(
                color = FDColors.Primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(s.radiusCard),
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = s.gapSmall)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ORDEN EN CAMINO ABIERTA: ${ordenEnCamino.numeroOrden}",
                            style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Black),
                            color = FDColors.Primary
                        )
                        Text(
                            text = "Este proveedor ya tiene ${ordenEnCamino.items.size} productos (${ordenEnCamino.totalUnidades} unidades) en camino. Los productos que agregues aquí se sumarán directamente a esta orden.",
                            style = FDType.BodySmall.copy(fontSize = 11.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }
        }

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
                    CampoBuscadorModerno(
                        busqueda = busquedaLocal,
                        onBusquedaChange = { busquedaLocal = it },
                        placeholder = "Buscar producto, categoría o laboratorio…",
                        altura = s.inputMinH
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
                        if (urgentesVisibles.isNotEmpty()) {
                            item(key = "cab_urgentes") {
                                Text(
                                    text = "LO QUE SE ACABA (${urgentesVisibles.size})",
                                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                    color = FDColors.Warning,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                            items(urgentesVisibles, key = { it.id }) { prod ->
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
                        if (restoVisibles.isNotEmpty()) {
                            item(key = "cab_resto") {
                                Text(
                                    text = "LO DEMÁS",
                                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                    color = FDColors.TextTertiary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                            items(restoVisibles, key = { it.id }) { prod ->
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

                val todosContribuidores = remember(contribuidoresProv) {
                    contribuidoresProv.values.flatMap { it.keys }.distinct().filter { it.isNotBlank() }
                }
                if (todosContribuidores.size > 1) {
                    Surface(
                        color = FDColors.Primary.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.People, null, tint = FDColors.Primary, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Equipo colaborando en vivo: ${todosContribuidores.joinToString(", ") { it.substringBefore('@') }}",
                                style = FDType.BodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

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
                                    // Quién marcó cada unidad: siempre visible, chico y
                                    // discreto, para que cualquier "está sumando de más"
                                    // se compruebe al instante sin discutir.
                                    val contribs = contribuidoresProv[prod.id].orEmpty()
                                    if (contribs.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = FDColors.TextTertiary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = contribs.entries.joinToString(" · ") { (quien, aporte) ->
                                                    "${nombreCortoColaborador(quien)} agregó $aporte"
                                                },
                                                style = FDType.BodySmall.copy(fontSize = 9.5.sp),
                                                color = FDColors.TextTertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
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
                            // Raya entre productos del carrito: gris visible en claro,
                            // gris blanquecino en oscuro (100% token, sin color fijo).
                            HorizontalDivider(color = FDColors.TextTertiary.copy(alpha = 0.55f))
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

                        // Estándar global: primario negro+blanco en claro, marfil+negro
                        // en oscuro (se invierte solo con el tema). Apagado: mismo
                        // botón atenuado, sin perder contraste.
                        val puedeEnviar = pedidoActivo?.tieneItems == true && !enviandoPedido
                        Button(
                            onClick = { pedidoActivo?.let(onRealizarPedido) },
                            enabled = puedeEnviar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FDColors.Primary,
                                contentColor = FDColors.PrimaryText,
                                disabledContainerColor = FDColors.Primary,
                                disabledContentColor = FDColors.PrimaryText
                            ),
                            shape = RoundedCornerShape(s.radiusButton),
                            modifier = Modifier
                                .weight(1f)
                                .height(s.btnMediumH)
                                .alpha(if (puedeEnviar) 1f else 0.45f)
                        ) {
                            if (enviandoPedido) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(s.iconSmall),
                                    color = FDColors.PrimaryText,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (ordenEnCamino != null) "SUMAR A ORDEN (${ordenEnCamino.numeroOrden})" else "REALIZAR PEDIDO",
                                    style = FDType.Label.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp,
                                        letterSpacing = 0.4.sp
                                    ),
                                    color = FDColors.PrimaryText
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
                        ),
                        color = FDColors.PrimaryText
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
                        ),
                        color = FDColors.Primary
                    )
                }
            }
        )
    }
}

/** Nombre corto y amable para la línea de "quién agregó": sin correo, con mayúscula. */
private fun nombreCortoColaborador(clave: String): String {
    val base = clave.substringBefore('@').trim()
    if (base.isBlank()) return "Equipo"
    return base.replaceFirstChar { it.uppercase() }
}
