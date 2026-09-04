package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.ui.componentes.DialogoCambiarProveedor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import java.util.Locale

/**
 * Fila plana de la tabla de productos de un proveedor. Columnas alineadas con la
 * cabecera (mismos anchos) y sin tarjetas: es una fila de datos, no una app móvil.
 */
@Composable
fun FilaProductoDetalleProveedor(
    prod: PharmProduct,
    cantPedir: Int,
    enCamino: Int,
    simboloMoneda: String,
    esSinProveedor: Boolean,
    proveedores: List<Proveedor>,
    s: MedidaAdaptativa,
    onModificarCantidad: (Int) -> Unit,
    onVincular: ((PharmProduct, Proveedor) -> Unit)?
) {
    val tienePedido = cantPedir > 0
    var mostrarDialogoCambiar by remember { mutableStateOf(false) }
    val stockActual = prod.stock
    val stockMinimo = prod.minStock
    val esAgotado = stockActual <= 0
    val esCritico = stockActual > 0 && stockMinimo > 0 && stockActual <= stockMinimo
    val empaqueStr = prod.empaque.ifBlank { "Und" }

    val textoStock = when {
        esAgotado -> "Agotado · Mín $stockMinimo"
        esCritico -> "Quedan $stockActual · Mín $stockMinimo"
        else -> "Stock $stockActual"
    }
    val colorStock = when {
        esAgotado -> FDColors.Warning
        esCritico -> FDColors.Warning
        else -> FDColors.TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    esAgotado -> FDColors.Warning.copy(alpha = 0.05f)
                    esCritico -> FDColors.Warning.copy(alpha = 0.035f)
                    else -> Color.Transparent
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = prod.name,
                    style = FDType.Heading3.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val detalleSub = listOfNotNull(
                    prod.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                    prod.category.takeIf { it.isNotBlank() }
                ).joinToString("  ·  ")
                val subTexto = if (detalleSub.isNotBlank()) "$detalleSub  ·  $empaqueStr" else empaqueStr
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subTexto,
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!esSinProveedor) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Cambiar proveedor",
                            tint = FDColors.TextTertiary.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { mostrarDialogoCambiar = true }
                        )
                    }
                }
            }

            Text(
                text = textoStock,
                style = FDType.Label.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colorStock,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(AnchoColStock)
            )

            Text(
                text = if (enCamino > 0) "$enCamino en camino" else "—",
                style = FDType.Label.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (enCamino > 0) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (enCamino > 0) FDColors.Primary else FDColors.TextTertiary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(AnchoColEnCamino)
            )

            Text(
                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", prod.purchasePrice)}",
                style = FDType.Numeric.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                ),
                color = FDColors.TextPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(AnchoColPrecio)
            )

            Box(
                modifier = Modifier.width(AnchoColPedir),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (esSinProveedor) {
                    Surface(
                        color = FDColors.Primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(s.radiusInput * 0.55f),
                        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { mostrarDialogoCambiar = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Link, null, tint = FDColors.Primary, modifier = Modifier.size(14.dp))
                            Text("VINCULAR", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.Primary)
                        }
                    }
                } else if (tienePedido) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        BotonMasMenos(
                            icono = Icons.Default.Remove,
                            habilitado = cantPedir > 0,
                            onClick = { onModificarCantidad(-1) },
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
                                    text = "$cantPedir",
                                    style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = s.textInput.value.sp),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                        BotonMasMenos(
                            icono = Icons.Default.Add,
                            habilitado = true,
                            onClick = { onModificarCantidad(1) },
                            s = s
                        )
                    }
                } else {
                    Surface(
                        color = FDColors.Primary,
                        shape = RoundedCornerShape(s.radiusChip),
                        modifier = Modifier
                            .size(s.btnMediumH)
                            .clickable { onModificarCantidad(1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = FDColors.Surface, modifier = Modifier.size(s.iconSmall))
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))
    }

    if (mostrarDialogoCambiar) {
        DialogoCambiarProveedor(
            producto = prod,
            proveedores = proveedores,
            onGuardar = { nuevoProv ->
                onVincular?.invoke(prod, nuevoProv)
                mostrarDialogoCambiar = false
            },
            onDismiss = { mostrarDialogoCambiar = false }
        )
    }
}
