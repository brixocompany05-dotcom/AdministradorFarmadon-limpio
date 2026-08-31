package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa

/**
 * Confirmaciones críticas de un pedido enviado: cerrar con ajuste,
 * cancelar orden y descartar un faltante. Solo confirman; la decisión
 * y sus consecuencias se ejecutan en el ViewModel.
 */
@Composable
fun DialogosPedidoEnviado(
    proveedorNombre: String,
    unidadesPendientes: Int,
    confirmarAjuste: Boolean,
    confirmarCancelacion: Boolean,
    productoParaDescartar: ItemPedidoCompra?,
    s: MedidaAdaptativa,
    onConfirmarAjuste: () -> Unit,
    onCancelarAjuste: () -> Unit,
    onConfirmarCancelacion: () -> Unit,
    onCancelarCancelacion: () -> Unit,
    onConfirmarDescartar: (String) -> Unit,
    onCancelarDescartar: () -> Unit
) {
    if (confirmarAjuste) {
        AlertDialog(
            onDismissRequest = onCancelarAjuste,
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "¿Cerrar con ajuste?",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Text(
                    "Las $unidadesPendientes unidades que faltan NO llegarán nunca. La orden queda cerrada y sus faltantes dejan de contar como \"en camino\". Lo ya recibido NO se toca.",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmarAjuste,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "SÍ, CERRAR CON AJUSTE",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelarAjuste,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "NO, MEJOR NO",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    if (confirmarCancelacion) {
        AlertDialog(
            onDismissRequest = onCancelarCancelacion,
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "¿Cancelar esta orden?",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Text(
                    "Se cancela la orden completa de $proveedorNombre. Todavía no se recibió mercadería en ella y deja de contar como \"en camino\".",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmarCancelacion,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "SÍ, CANCELAR ORDEN",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelarCancelacion,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "NO, MEJOR NO",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    productoParaDescartar?.let { prod ->
        AlertDialog(
            onDismissRequest = onCancelarDescartar,
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "¿Descartar el faltante?",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Text(
                    "'${prod.productoNombre}': las ${prod.saldoPendiente} unidades que faltan se marcan como quebradas en el proveedor y dejan de esperarse. La orden se recalcula sola y lo ya recibido no se toca.",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmarDescartar(prod.productoId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "SÍ, DESCARTAR FALTANTE",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelarDescartar,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "NO, MEJOR NO",
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
