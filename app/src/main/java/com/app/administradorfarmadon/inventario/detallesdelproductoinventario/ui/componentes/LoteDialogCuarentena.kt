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
internal fun DialogoCuarentenaSanitaria(
    product: MoldeProductos,
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirm: (ponerEnCuarentena: Boolean, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    val totalDisponible = lote.cantidad
    val totalBloqueada = lote.cantidadBloqueada
    val hayDisponibles = totalDisponible > 0

    var selectedIndex by remember { mutableIntStateOf(if (hayDisponibles) 0 else 1) }
    val accionCuarentena = if (selectedIndex == 0) "BLOQUEAR" else "DESBLOQUEAR"

    val maxPermitido = if (accionCuarentena == "BLOQUEAR") totalDisponible else totalBloqueada
    var modoCantidadIdx by remember { mutableIntStateOf(0) } // 0: Parcial, 1: Todo
    var cantidadStr by remember { mutableStateOf("") }
    var motivo by remember { 
        mutableStateOf(if (accionCuarentena == "BLOQUEAR") "Alerta sanitaria / En investigación" else "Inspección superada / Lote apto") 
    }

    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val cantNum = if (modoCantidadIdx == 1) maxPermitido else (cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0)

    EnterpriseModalShell(
        title = if (accionCuarentena == "BLOQUEAR") "Poner en cuarentena — Lote ${lote.numero}" else "Quitar de cuarentena — Lote ${lote.numero}",
        icon = if (accionCuarentena == "BLOQUEAR") Icons.Outlined.Lock else Icons.Outlined.LockOpen,
        iconColor = if (accionCuarentena == "BLOQUEAR") FDColors.Warning else FDColors.TextPrimary, 
        onDismiss = onDismiss,
        isProcesando = isProcesando,
        mensajeError = mensajeError,
        leftContent = {
            // 1. Selector de Acción (Decisión Principal)
            EnterpriseSegmentedControl(
                options = listOf("Retener (${totalDisponible.toInt()})", "Liberar (${totalBloqueada.toInt()})"),
                selectedIndex = selectedIndex,
                onSelect = { 
                    selectedIndex = it
                    motivo = if (it == 0) "Alerta sanitaria / En investigación" else "Inspección superada / Lote apto"
                    modoCantidadIdx = 0
                    cantidadStr = ""
                    mensajeError = null
                },
                enabled = !isProcesando,
                activeColor = if (selectedIndex == 0) FDColors.Warning else FDColors.Primary
            )

            // 2. Selector de Alcance de Cantidad Unificado
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (accionCuarentena == "BLOQUEAR") "¿Cuánto guardas? *" else "¿Cuánto liberas? *",
                    style = FDType.Label.copy(
                        fontSize = 10.5.sp,
                        color = FDColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
                EnterpriseSegmentedControl(
                    options = listOf("Parcial", "Todo (${maxPermitido.toInt()})"),
                    selectedIndex = modoCantidadIdx,
                    onSelect = {
                        modoCantidadIdx = it
                        if (it == 1) {
                            cantidadStr = maxPermitido.toInt().toString()
                        } else {
                            cantidadStr = ""
                        }
                        mensajeError = null
                    },
                    enabled = !isProcesando
                )

                if (modoCantidadIdx == 0) {
                    val errorCuarentena = cantNum > maxPermitido || (cantidadStr.isNotBlank() && cantNum <= 0)
                    EnterpriseInputField(
                        value = cantidadStr,
                        onValueChange = { 
                            cantidadStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            mensajeError = null
                        },
                        enabled = !isProcesando,
                        label = "Unidades a procesar *",
                        placeholder = "Máximo: ${maxPermitido.toInt()} ${product.empaque}",
                        isError = errorCuarentena,
                        errorMessage = if (cantNum > maxPermitido) "Supera el saldo disponible (${maxPermitido.toInt()})" else "Ingresa una cantidad mayor a 0"
                    )
                }
            }

            EnterpriseInputField(
                value = motivo,
                onValueChange = { motivo = it; mensajeError = null },
                enabled = !isProcesando,
                label = "Motivo de auditoría sanitaria *",
                minLines = 3,
                singleLine = false
            )
        },
        rightContent = {
            // 3. Documento de Saldos
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ESTADO DE SALDOS SANITARIOS",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        color = FDColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                EnterpriseInfoRow("Lote", lote.numero)
                EnterpriseInfoRow("En Mostrador (Venta)", "${totalDisponible.toInt()} ${product.empaque}")
                EnterpriseInfoRow("En Cuarentena (Bloqueado)", "${totalBloqueada.toInt()} ${product.empaque}", valueColor = FDColors.Warning)
                EnterpriseInfoRow("Saldo Total del Lote", "${(totalDisponible + totalBloqueada).toInt()} ${product.empaque}")
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

            Spacer(Modifier.height(8.dp))

            // 4. Acción Integrada
            val esValido = cantNum > 0 && cantNum <= maxPermitido && motivo.isNotBlank()
            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true
                    mensajeError = null
                    onConfirm(accionCuarentena == "BLOQUEAR", cantNum, motivo) { result ->
                        isProcesando = false
                        if (result.isFailure) {
                            mensajeError = result.exceptionOrNull()?.message ?: "Error al actualizar cuarentena."
                        }
                    }
                },
                enabled = esValido && !isProcesando,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (accionCuarentena == "BLOQUEAR") FDColors.Warning else FDColors.TextPrimary, // Neutro, no verde
                    contentColor = FDColors.PrimaryText
                )
            ) {
                if (isProcesando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText)
                    Spacer(Modifier.width(10.dp))
                    Text("PROCESANDO...", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        text = if (accionCuarentena == "BLOQUEAR") "CONFIRMAR CUARENTENA" else "LIBERAR A MOSTRADOR",
                        style = FDType.Label.copy(fontWeight = FontWeight.ExtraBold)
                    )
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
