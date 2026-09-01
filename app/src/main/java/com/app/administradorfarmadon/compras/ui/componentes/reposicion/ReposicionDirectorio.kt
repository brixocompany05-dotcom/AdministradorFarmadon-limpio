package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.compras.logica.ProductoEnCamino
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import java.util.Locale

/**
 * Directorio de proveedores de Reposición: cada proveedor es una fila clicable.
 * Al elegirlo, el panel pasa al detalle de sus productos (pantalla completa).
 */
@Composable
fun ReposicionDirectorio(
    gruposFiltrados: Map<String, List<PharmProduct>>,
    pedidosPorProveedor: Map<String, Map<String, Int>>,
    enCaminoPorProducto: Map<String, ProductoEnCamino>,
    simboloMoneda: String,
    totalCriticosGlobal: Int,
    montoEnBorrador: Double,
    totalProdsGlobal: Int,
    totalCriticosTabs: Int,
    totalSinProveedor: Int,
    filtroRapido: String,
    busquedaProducto: String,
    s: MedidaAdaptativa,
    listaState: LazyListState,
    onCambiarFiltro: (String) -> Unit,
    onCambiarBusqueda: (String) -> Unit,
    onAbrirProveedor: (String) -> Unit,
    onReponerSugeridosProveedor: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(s.padCardLarge)
    ) {
        // Buscador arriba como único elemento de cabecera
        OutlinedTextField(
            value = busquedaProducto,
            onValueChange = onCambiarBusqueda,
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

        Spacer(modifier = Modifier.height(s.gapMedium))

        // Underline tabs de inventario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(
                Triple("TODOS", "TODOS LOS PRODUCTOS", totalProdsGlobal),
                Triple("CRITICOS", "STOCK POR AGOTARSE", totalCriticosTabs),
                Triple("SIN_PROVEEDOR", "SIN PROVEEDOR", totalSinProveedor)
            ).forEach { (idFiltro, label, count) ->
                val isSel = filtroRapido == idFiltro
                Column(
                    modifier = Modifier
                        .clickable { onCambiarFiltro(idFiltro) }
                        .padding(bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                    ) {
                        Text(
                            text = label,
                            style = FDType.Label.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                                letterSpacing = 1.sp
                            ),
                            color = if (isSel) FDColors.Primary else FDColors.TextSecondary
                        )
                        Surface(
                            color = if (isSel) FDColors.Primary.copy(alpha = 0.1f) else FDColors.TextPrimary.copy(
                                alpha = 0.05f
                            ),
                            shape = FDShapes.XSmall
                        ) {
                            Text(
                                text = "$count",
                                style = FDType.Label.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (isSel) FDColors.Primary else FDColors.TextTertiary,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 1.dp
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(visible = isSel) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(32.dp)
                                .clip(FDShapes.Full)
                                .background(FDColors.Primary)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

        Spacer(Modifier.height(12.dp))

        // Encabezado de tabla
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "PROVEEDOR",
                style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                color = FDColors.TextTertiary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                Text("PRODUCTOS", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp), color = FDColors.TextTertiary)
                Text("PEDIDO", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp), color = FDColors.TextTertiary)
            }
        }

        LazyColumn(
            state = listaState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall)
        ) {
            items(
                items = gruposFiltrados.entries.toList(),
                key = { it.key }
            ) { (claveGrupo, productos) ->
                val esSinProveedor = filtroRapido == "SIN_PROVEEDOR" || esProveedorPlaceholder(claveGrupo)
                val carroProv = if (esSinProveedor) {
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
                    pedidosPorProveedor[claveGrupo] ?: emptyMap()
                }

                FilaProveedorDirectorio(
                    proveedorNombre = claveGrupo,
                    productos = productos,
                    carroProv = carroProv,
                    enCaminoPorProducto = enCaminoPorProducto,
                    simboloMoneda = simboloMoneda,
                    esSinProveedor = esSinProveedor,
                    s = s,
                    onClick = { onAbrirProveedor(claveGrupo) },
                    onReponerSugeridos = { onReponerSugeridosProveedor(claveGrupo) }
                )
            }
        }
    }
}

@Composable
private fun FilaProveedorDirectorio(
    proveedorNombre: String,
    productos: List<PharmProduct>,
    carroProv: Map<String, Int>,
    enCaminoPorProducto: Map<String, ProductoEnCamino>,
    simboloMoneda: String,
    esSinProveedor: Boolean,
    s: MedidaAdaptativa,
    onClick: () -> Unit,
    onReponerSugeridos: () -> Unit
) {
    val productosEnPedido = carroProv.count { it.value > 0 }
    val subtotalProv = productos.sumOf { (carroProv[it.id] ?: 0) * it.purchasePrice }
    val criticosPendientes = productos.count {
        it.stock <= it.minStock &&
                (carroProv[it.id] ?: 0) == 0 &&
                (enCaminoPorProducto[it.id]?.unidades ?: 0) == 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (productosEnPedido > 0) FDColors.Primary.copy(alpha = 0.04f) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (esSinProveedor) "SIN PROVEEDOR" else proveedorNombre,
                            style = FDType.Heading3.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = FDColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (esSinProveedor) "${productos.size} productos sin afiliar" else "${productos.size} productos suministrados",
                            style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    if (criticosPendientes > 0) {
                        Surface(
                            color = FDColors.Warning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(s.radiusChip),
                            border = BorderStroke(0.8.dp, FDColors.Warning.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$criticosPendientes críticos",
                                style = FDType.Label.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = FDColors.Warning,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (productosEnPedido > 0) {
                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(s.radiusChip),
                            border = BorderStroke(0.8.dp, FDColors.Primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", subtotalProv)} · $productosEnPedido prod.",
                                style = FDType.Label.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = FDColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = FDColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
        }

        if (criticosPendientes > 0 && !esSinProveedor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReponerSugeridos)
                    .background(FDColors.Primary.copy(alpha = 0.04f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.gapTiny)
            ) {
                Icon(
                    Icons.Default.Bolt,
                    null,
                    tint = FDColors.Primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Pedir los $criticosPendientes faltantes sugeridos",
                    style = FDType.Label.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.Primary
                )
            }
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
    }
}
