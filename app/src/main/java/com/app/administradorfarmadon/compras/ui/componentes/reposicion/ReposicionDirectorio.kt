package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.logica.ProductoEnCamino
import com.app.administradorfarmadon.compras.ui.componentes.CampoBuscadorModerno
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
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
    busquedaProducto: String,
    s: MedidaAdaptativa,
    listaState: LazyListState,
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
        CampoBuscadorModerno(
            busqueda = busquedaProducto,
            onBusquedaChange = onCambiarBusqueda,
            placeholder = "Buscar producto, categoría o laboratorio…",
            altura = s.inputMinH
        )

        Spacer(modifier = Modifier.height(s.gapMedium))

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
                val esSinProveedor = esProveedorPlaceholder(claveGrupo)
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
