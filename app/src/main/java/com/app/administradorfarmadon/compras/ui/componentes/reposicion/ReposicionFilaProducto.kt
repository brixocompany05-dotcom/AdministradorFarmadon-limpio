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
        esAgotado -> "Agotado en tienda · Mín $stockMinimo"
        esCritico -> "En tienda: $stockActual · Mín $stockMinimo"
        else -> "En tienda: $stockActual"
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
            // Columna principal: Nombre del producto y detalles
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = prod.name,
                    style = FDType.Heading3.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = FDColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                val detalleSub = prod.laboratory.takeIf { it.isNotBlank() && it != "Genérico" }.orEmpty()
                val subTexto = if (detalleSub.isNotBlank()) "$detalleSub  ·  $empaqueStr" else empaqueStr
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subTexto,
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (enCamino > 0) {
                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$enCamino en camino",
                                style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    if (!esSinProveedor) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Cambiar proveedor",
                            tint = FDColors.TextTertiary.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { mostrarDialogoCambiar = true }
                        )
                    }
                }
            }

            // Columna central-derecha: Stock y Precio de compra
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = textoStock,
                    style = FDType.Label.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = colorStock,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", prod.purchasePrice)}",
                    style = FDType.Numeric.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = FDColors.TextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            // Columna derecha: cantidad siempre visible (- 0 +) o Vincular.
            // No se esconde el control: en 0 se ve apagado para que se entienda solo mirando.
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.padding(start = 4.dp)
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
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BotonMasMenos(
                            icono = Icons.Default.Remove,
                            habilitado = cantPedir > 0,
                            onClick = { onModificarCantidad(-1) },
                            s = s
                        )
                        Surface(
                            color = if (tienePedido) FDColors.SurfaceElevated else FDColors.TextPrimary.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(s.radiusInput * 0.55f),
                            border = BorderStroke(s.borderWidth, FDColors.Border),
                            modifier = Modifier.width(38.dp).height(s.btnMediumH)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$cantPedir",
                                    style = FDType.Numeric.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                                    color = if (tienePedido) FDColors.TextPrimary else FDColors.TextTertiary
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
