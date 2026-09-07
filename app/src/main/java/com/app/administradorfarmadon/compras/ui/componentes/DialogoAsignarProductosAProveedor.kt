package com.app.administradorfarmadon.compras.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct

@Composable
fun DialogoAsignarProductosAProveedor(
    proveedor: Proveedor,
    todosLosProductos: List<PharmProduct>,
    onVincularProducto: (PharmProduct, Proveedor) -> Unit,
    onDismiss: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    var busquedaProducto by rememberSaveable { mutableStateOf("") }
    var filtroRapido by rememberSaveable { mutableStateOf("TODOS") } // "TODOS" | "SIN_PROVEEDOR"
    var idProductoEnProceso by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(todosLosProductos) {
        idProductoEnProceso = null
    }

    val totalSinProveedor = remember(todosLosProductos) {
        todosLosProductos.count { prod ->
            val provActual = prod.proveedor.trim()
            prod.proveedorId.isBlank() && (provActual.isBlank() ||
                listOf("SIN PROVEEDOR", "N/A", "GENÉRICO", "GENERICO", "SIN ASIGNAR").any { it.equals(provActual, ignoreCase = true) })
        }
    }

    val productosFiltrados = remember(todosLosProductos, busquedaProducto, filtroRapido, proveedor.id, proveedor.nombre) {
        val q = busquedaProducto.trim().lowercase()
        todosLosProductos.filter { prod ->
            val provActual = prod.proveedor.trim()
            val esSinProv = prod.proveedorId.isBlank() && (provActual.isBlank() ||
                listOf("SIN PROVEEDOR", "N/A", "GENÉRICO", "GENERICO", "SIN ASIGNAR").any { it.equals(provActual, ignoreCase = true) })

            val coincideFiltro = when (filtroRapido) {
                "SIN_PROVEEDOR" -> esSinProv
                else -> true
            }
            if (!coincideFiltro) return@filter false

            if (q.isBlank()) return@filter true
            prod.name.lowercase().contains(q) ||
                prod.code.lowercase().contains(q) ||
                prod.category.lowercase().contains(q) ||
                prod.laboratory.lowercase().contains(q)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 520.dp, max = 640.dp)
                .fillMaxWidth(0.55f)
                .fillMaxHeight(0.86f)
                .heightIn(min = 520.dp),
            shape = RoundedCornerShape(18.dp),
            color = FDColors.SurfaceElevated,
            border = BorderStroke(1.dp, FDColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── CABECERA CÓMODA Y PROPORCIONADA ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FDColors.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "ASIGNAR PRODUCTOS AL PROVEEDOR",
                                style = FDType.Heading3.copy(
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Droguería:",
                                    style = FDType.Caption.copy(fontSize = 11.5.sp),
                                    color = FDColors.TextTertiary
                                )
                                Text(
                                    text = proveedor.nombre,
                                    style = FDType.BodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 1.dp)

                // ── BUSCADOR DE PRODUCTOS CÓMODO Y ESPACIOSO ──
                CampoBuscadorModerno(
                    busqueda = busquedaProducto,
                    onBusquedaChange = { busquedaProducto = it },
                    placeholder = "Buscar por nombre, código o laboratorio...",
                    altura = 48.dp
                )

                // ── PESTAÑAS DE FILTRO RÁPIDO ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "TODOS" to "TODOS (${todosLosProductos.size})",
                        "SIN_PROVEEDOR" to "SOLO SIN PROVEEDOR ($totalSinProveedor)"
                    ).forEach { (idFiltro, label) ->
                        val isSel = filtroRapido == idFiltro
                        Surface(
                            onClick = { filtroRapido = idFiltro },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) FDColors.Primary.copy(alpha = 0.12f) else FDColors.Surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = label,
                                style = FDType.Caption.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isSel) FDColors.Primary else FDColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "${productosFiltrados.size} resultados",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary
                    )
                }

                // ── LISTA SCROLLEABLE AMPLIA (OCUPA TODO EL ALTO RESTANTE) ──
                Surface(
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (productosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = FDColors.TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No se encontraron productos",
                                    style = FDType.Heading3.copy(fontSize = 14.sp),
                                    color = FDColors.TextPrimary
                                )
                                Text(
                                    text = if (busquedaProducto.isNotBlank())
                                        "No hay coincidencias para '$busquedaProducto'"
                                    else
                                        "No hay productos en esta categoría",
                                    style = FDType.BodySmall.copy(fontSize = 12.sp),
                                    color = FDColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(productosFiltrados, key = { it.id }) { prod ->
                                val provActual = prod.proveedor.trim()
                                val esDeEsteProveedor = (prod.proveedorId.isNotBlank() && prod.proveedorId == proveedor.id) ||
                                    provActual.equals(proveedor.nombre, ignoreCase = true) ||
                                    (proveedor.idFiscal.isNotBlank() && provActual == proveedor.idFiscal)
                                val esSinProv = prod.proveedorId.isBlank() && (provActual.isBlank() ||
                                    listOf("SIN PROVEEDOR", "N/A", "GENÉRICO", "GENERICO", "SIN ASIGNAR").any { it.equals(provActual, ignoreCase = true) })

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (esDeEsteProveedor) FDColors.Success.copy(alpha = 0.06f) else FDColors.SurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (esDeEsteProveedor) FDColors.Success.copy(alpha = 0.35f) else FDColors.Border.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (esDeEsteProveedor) FDColors.Success.copy(alpha = 0.12f)
                                                    else FDColors.TextPrimary.copy(alpha = 0.05f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Medication,
                                                contentDescription = null,
                                                tint = if (esDeEsteProveedor) FDColors.Success else FDColors.TextTertiary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                style = FDType.Body.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            val detalle = listOfNotNull(
                                                prod.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                                                prod.empaque.takeIf { it.isNotBlank() },
                                                "Stock: ${prod.stock}u"
                                            ).joinToString(" · ")
                                            Text(
                                                text = detalle,
                                                style = FDType.Caption.copy(fontSize = 11.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }

                                        // Estado y Botón de acción
                                        if (esDeEsteProveedor) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = FDColors.Success.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = FDColors.Success,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "VINCULADO",
                                                        style = FDType.Caption.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.5.sp
                                                        ),
                                                        color = FDColors.Success
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                if (!esSinProv) {
                                                    Text(
                                                        text = "De: $provActual",
                                                        style = FDType.Caption.copy(fontSize = 9.5.sp),
                                                        color = FDColors.TextTertiary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                val enProceso = idProductoEnProceso == prod.id
                                                FDBotonPrimario(
                                                    texto = if (enProceso) "VINCULANDO..." else if (esSinProv) "+ VINCULAR" else "ASIGNAR A ESTE",
                                                    icono = if (enProceso) null else if (esSinProv) Icons.Default.Add else Icons.Default.SwapHoriz,
                                                    habilitado = idProductoEnProceso == null,
                                                    cargando = enProceso,
                                                    onClick = {
                                                        idProductoEnProceso = prod.id
                                                        onVincularProducto(prod, proveedor)
                                                    },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── PIE DE DIÁLOGO: CÓMODO Y CLARO ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Toca '+ VINCULAR' para asociar el producto a esta droguería.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary
                    )

                    FDBotonSecundario(
                        texto = "LISTO / CERRAR",
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
    }
}
