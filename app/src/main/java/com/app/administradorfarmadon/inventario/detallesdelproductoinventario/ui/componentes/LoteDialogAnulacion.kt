package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

@Composable
internal fun DialogoAnulacionLegal(
    product: MoldeProductos,
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirm: (motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    var motivo by remember { mutableStateOf("Corrección administrativa por error al ingresar factura") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    EnterpriseModalShell(
        title = "Anular lote — ${lote.numero}",
        icon = Icons.Outlined.Warning,
        iconColor = FDColors.Error,
        onDismiss = onDismiss,
        isProcesando = isProcesando,
        mensajeError = mensajeError,
        leftContent = {
            // Formulario continuo sin cajas anidadas (60%): solo 1 campo 52dp + helper
            EnterpriseInputField(
                value = motivo,
                onValueChange = { motivo = it; mensajeError = null },
                enabled = !isProcesando,
                label = "¿Por qué lo anulas? *",
                placeholder = "Ej: factura duplicada",
                minLines = 4,
                singleLine = false
            )
            Text(
                text = "Esta acción borra el lote y ajusta la factura. No se puede deshacer.",
                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
            )
        },
        rightContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EXPEDIENTE A ELIMINAR",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        color = FDColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                EnterpriseInfoRow("Lote Erróneo", lote.numero)
                EnterpriseInfoRow("Cantidad a Reversar", "${(lote.cantidad + lote.cantidadBloqueada).toInt()} ${product.empaque}")
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true
                    mensajeError = null
                    onConfirm(motivo) { result ->
                        isProcesando = false
                        if (result.isFailure) {
                            mensajeError = result.exceptionOrNull()?.message ?: "Error al anular el registro."
                        }
                    }
                },
                enabled = motivo.isNotBlank() && !isProcesando,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error, contentColor = FDColors.PrimaryText)
            ) {
                if (isProcesando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText)
                    Spacer(Modifier.width(10.dp))
                    Text("ANULANDO...", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                } else {
                    Text("ANULAR REGISTRO ERRÓNEO", style = FDType.Label.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        },
        confirmButton = null,
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcesando,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.height(46.dp)
            ) {
                Text("CANCELAR", style = FDType.Label.copy(color = FDColors.TextSecondary))
            }
        }
    )
}
