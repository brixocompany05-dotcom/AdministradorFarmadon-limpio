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
internal fun DialogoRegistroMermaDrawer(
    product: MoldeProductos,
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirm: (cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    // R3: UI alineada con servidor — solo disponible es mermable, cuarentena requiere desbloqueo
    val totalLote = lote.cantidad
    val totalReal = lote.cantidad + lote.cantidadBloqueada
    var modoCantidadIdx by remember { mutableIntStateOf(0) } // 0: Parcial, 1: Todo
    var cantidadStr by remember { mutableStateOf("") }
    var motivoMerma by remember { mutableStateOf("Vencimiento / Caducado") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val costoUnitario = if (lote.costoCompraUnitario > 0) lote.costoCompraUnitario
    else if (lote.costoUltimoIngresoUnitario > 0) lote.costoUltimoIngresoUnitario
    else if (lote.cantidad > 0) lote.costoUltimoIngreso / lote.cantidad
    else 0.0

    val cantNum = if (modoCantidadIdx == 1) totalLote else (cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0)
    val totalPerdida = cantNum * costoUnitario

    EnterpriseModalShell(
        title = "Merma — Lote ${lote.numero}",
        icon = Icons.Outlined.DeleteOutline,
        iconColor = FDColors.Error,
        onDismiss = onDismiss,
        isProcesando = isProcesando,
        mensajeError = mensajeError,
        leftContent = {
            // 1. Selector de Alcance de Cantidad Unificado
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "¿Cuánto se perdió? *",
                    style = FDType.Label.copy(
                        fontSize = 10.5.sp,
                        color = FDColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
                EnterpriseSegmentedControl(
                    options = listOf("Parcial", "Todo (${totalLote.toInt()} ${product.empaque})"),
                    selectedIndex = modoCantidadIdx,
                    onSelect = {
                        modoCantidadIdx = it
                        if (it == 1) {
                            cantidadStr = totalLote.toInt().toString()
                        } else {
                            cantidadStr = ""
                        }
                        mensajeError = null
                    },
                    enabled = !isProcesando
                )

                if (modoCantidadIdx == 0) {
                    val errorMerma = cantNum > totalLote || (cantidadStr.isNotBlank() && cantNum <= 0)
                    EnterpriseInputField(
                        value = cantidadStr,
                        onValueChange = { 
                            cantidadStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            mensajeError = null
                        },
                        enabled = !isProcesando,
                        label = "Unidades a descontar *",
                        placeholder = "Máximo: ${totalLote.toInt()} ${product.empaque}",
                        isError = errorMerma,
                        errorMessage = if (cantNum > totalLote) "Supera el saldo total (${totalLote.toInt()} ${product.empaque})" else "Ingresa una cantidad mayor a 0"
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("¿Por qué se perdió?", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp))
                EnterpriseReasonList(
                    reasons = listOf(
                        "Vencimiento / Caducado",
                        "Frasco Roto / Daño Físico",
                        "Pérdida Cadena Frío",
                        "Defecto de Fábrica"
                    ),
                    selectedReason = motivoMerma,
                    onSelectReason = { motivoMerma = it; mensajeError = null },
                    enabled = !isProcesando
                )
            }
        },
        rightContent = {
            // 2. Documento de Merma
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EXPEDIENTE DE MERMA",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        color = FDColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                EnterpriseInfoRow("Lote Afectado", lote.numero)
                EnterpriseInfoRow("Saldo Total en Lote", "${totalLote.toInt()} ${product.empaque} disponibles / ${lote.cantidadBloqueada.toInt()} en cuarentena")
                EnterpriseInfoRow("Costo Unitario", "$ ${String.format(Locale.US, "%.2f", costoUnitario)}", isMonospace = true)
                if (lote.cantidadBloqueada > 0) {
                    Text("⚠ ️ ${lote.cantidadBloqueada.toInt()} en cuarentena — desbloquea primero para mermar.", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Warning, fontWeight = FontWeight.Bold))
                }
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 3. Pérdida Financiera Directa
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("IMPACTO CONTABLE DE PÉRDIDA", style = FDType.Label.copy(fontSize = 10.sp, color = FDColors.Error, letterSpacing = 0.5.sp))
                EnterpriseInfoRow("Pérdida Monetaria:", "$ ${String.format(Locale.US, "%.2f", totalPerdida)}", valueColor = FDColors.Error, isMonospace = true)
            }

            Spacer(Modifier.height(8.dp))

            // 4. Acción Integrada
            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true
                    mensajeError = null
                    onConfirm(cantNum, motivoMerma) { result ->
                        isProcesando = false
                        if (result.isFailure) {
                            mensajeError = result.exceptionOrNull()?.message ?: "Error al registrar la merma."
                        }
                    }
                },
                enabled = cantNum > 0 && cantNum <= totalLote && !isProcesando,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error, contentColor = FDColors.PrimaryText)
            ) {
                if (isProcesando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText)
                    Spacer(Modifier.width(10.dp))
                    Text("REGISTRANDO...", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                } else {
                    Text("CONFIRMAR MERMA", style = FDType.Label.copy(fontWeight = FontWeight.ExtraBold))
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
