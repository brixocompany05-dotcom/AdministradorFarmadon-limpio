package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario

// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•
// Mí“DULO 3: KARDEX Y AUDITORíA DE MOVIMIENTOS (TRAZABILIDAD TOTAL)
// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•
@Composable
internal fun ModuloKardexYAuditoria(
    movements: List<MovimientoInventario>,
    p: MoldeProductos,
    busquedaInicial: String = ""
) {
    if (movements.isEmpty()) {
        EnterpriseEmptyState(
            icon = Icons.Outlined.History,
            title = "Sin movimientos registrados en Kardex",
            description = "Las compras, dispensaciones en mostrador, mermas y ajustes aparecerán aquí en tiempo real.",
            buttonText = null,
            onAction = {}
        )
    } else {
        var filtroSeleccionado by remember { mutableStateOf("TODOS") }
        var busquedaKardex by remember(busquedaInicial) { mutableStateOf(busquedaInicial) }

        val movimientosFiltrados = remember(movements, filtroSeleccionado, busquedaKardex) {
            val q = busquedaKardex.trim().lowercase()
            movements.filter { mov ->
                val tipo = mov.tipo.uppercase()
                val coincideFiltro = when (filtroSeleccionado) {
                    "ENTRADAS" -> tipo.contains("ENTRADA") || tipo.contains("COMPRA") || tipo.contains("INGRESO") || tipo.contains("REABASTECIMIENTO") || tipo.contains("CREACION") || (mov.cantidad > 0 && !tipo.contains("DESBLOQUEO"))
                    "VENTAS" -> tipo.contains("VENTA") || tipo.contains("DISPENS")
                    "MERMAS" -> tipo.contains("MERMA") || tipo.contains("BAJA") || tipo.contains("DESTRUC")
                    "DEVOLUCIONES" -> tipo.contains("DEVOLUCION") || tipo.contains("CANJE") || tipo.contains("RECLAMO")
                    "AJUSTES" -> tipo.contains("AJUSTE") || tipo.contains("BLOQUEO") || tipo.contains("CUARENTENA") || tipo.contains("ANULACION")
                    else -> true
                }
                val coincideBusqueda = if (q.isBlank()) true else {
                    mov.tipo.lowercase().contains(q) ||
                    mov.usuarioNombre.lowercase().contains(q) ||
                    mov.referencia.lowercase().contains(q) ||
                    mov.loteNumero.lowercase().contains(q)
                }
                coincideFiltro && coincideBusqueda
            }
        }

        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(0.5.dp, FDColors.Border),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra Superior de Filtros Rápidos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FDColors.Background.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filtros = listOf(
                            "TODOS" to "Todos (${movements.size})",
                            "ENTRADAS" to "Entradas / Compras",
                            "VENTAS" to "Ventas",
                            "MERMAS" to "Mermas",
                            "DEVOLUCIONES" to "Devoluciones / Canjes",
                            "AJUSTES" to "Ajustes"
                        )
                        filtros.forEach { (key, label) ->
                            val sel = filtroSeleccionado == key
                            Surface(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clickable { filtroSeleccionado = key },
                                shape = RoundedCornerShape(6.dp),
                                color = if (sel) FDColors.Primary else FDColors.Background,
                                border = BorderStroke(1.dp, if (sel) FDColors.Primary else FDColors.Border)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = FDType.Caption.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sel) FDColors.PrimaryText else FDColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Buscador rápido en Kardex
                    OutlinedTextField(
                        value = busquedaKardex,
                        onValueChange = { busquedaKardex = it },
                        placeholder = { Text("Buscar en kardex...", style = FDType.Caption.copy(color = FDColors.TextTertiary)) },
                        textStyle = FDType.Caption.copy(color = FDColors.TextPrimary),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextSecondary, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FDColors.SurfaceElevated,
                            unfocusedContainerColor = FDColors.SurfaceElevated,
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = FDColors.Border
                        ),
                        modifier = Modifier.width(220.dp).height(44.dp)
                    )
                }

                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FDColors.Background.copy(alpha = 0.6f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TIPO OPERACIí“N", style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.5f))
                    Text("VARIACIí“N", style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1f))
                    Text("VALOR / COSTO", style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.2f))
                    Text("RESPONSABLE", style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.2f))
                    Text("FECHA Y HORA (SERVIDOR)", style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.4f))
                }

                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                if (movimientosFiltrados.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No se encontraron movimientos para el filtro seleccionado.",
                            style = FDType.BodySmall.copy(color = FDColors.TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(movimientosFiltrados) { mov ->
                            val isAdd = mov.cantidad > 0
                            val colorQty = if (isAdd) FDColors.Success else FDColors.Error

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = ProductDetailMapper.formatearTipoMovimiento(mov.tipo).uppercase(),
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary, fontSize = 12.5.sp)
                                    )
                                    if (mov.referencia.isNotBlank()) {
                                        Text(
                                            text = mov.referencia,
                                            style = FDType.Caption.copy(color = FDColors.TextSecondary, fontSize = 10.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (mov.loteNumero.isNotBlank()) {
                                        Text(
                                            text = "Lote: ${mov.loteNumero}",
                                            style = FDType.Caption.copy(color = FDColors.TextTertiary, fontSize = 9.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (isAdd) "+" else ""}${mov.cantidad.toInt()} ${p.empaque.ifBlank { "Unidad" }}",
                                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = colorQty, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (mov.costoTotal > 0) MonedaHelper.formatearSimple(mov.costoTotal) else "—”",
                                    style = FDType.Body.copy(color = FDColors.TextSecondary, fontSize = 13.sp),
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = mov.usuarioNombre.ifBlank { "Administrador" },
                                    style = FDType.BodySmall.copy(color = FDColors.TextSecondary),
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = ProductDetailMapper.formatRelativeDate(mov.fecha),
                                    style = FDType.Caption.copy(color = FDColors.TextTertiary),
                                    modifier = Modifier.weight(1.4f)
                                )
                            }
                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
