package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CHIPS_DIAS_VENCIMIENTO = listOf(30, 60, 90, 120)

/**
 * Sub-módulo 3: Alerta Preventiva de Vencimiento de Lotes.
 * Calcula en memoria y en tiempo real (0ms) la fecha exacta en que se activará la alerta
 * para el lote más próximo a vencer, garantizando que el personal conozca el día exacto de canje.
 */
@Composable
fun PanelAlertaVencimiento(
    diasVencimientoActual: Int,
    lotes: Map<String, LoteProducto>,
    onDiasVencimientoChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isGuardando: Boolean = false
) {
    var inputTexto by remember(diasVencimientoActual) {
        mutableStateOf(diasVencimientoActual.toString())
    }

    LaunchedEffect(inputTexto) {
        val num = inputTexto.toIntOrNull()
        if (num != null && num in 15..365 && num != diasVencimientoActual) {
            delay(600)
            onDiasVencimientoChange(num)
        }
    }
    DisposableEffect(inputTexto, diasVencimientoActual) {
        onDispose {
            val num = inputTexto.toIntOrNull()
            if (num != null && num in 15..365 && num != diasVencimientoActual) {
                onDiasVencimientoChange(num)
            }
        }
    }

    // Cálculo en vivo del lote más próximo a vencer y su fecha exacta de alerta
    val lotesActivos = remember(lotes, FDColors.isDark) {
        lotes.values.filter { it.cantidad > 0 }
    }

    val infoLoteCritico = remember(lotesActivos, diasVencimientoActual, FDColors.isDark) {
        calcularLoteMasProximo(lotesActivos, diasVencimientoActual)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Cuántos días antes de vencer avisa.",
            style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary)
        )

        // TARJETA DE ESTADO EN VIVO CON FECHA EXACTA DE ALERTA
        if (infoLoteCritico != null) {
            val enRiesgo = infoLoteCritico.diasRestantes <= diasVencimientoActual
            val colorBorde = if (enRiesgo) FDColors.Warning else FDColors.Success
            val iconAlerta = if (enRiesgo) Icons.Outlined.Warning else Icons.Outlined.CheckCircle

            Surface(
                color = colorBorde.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colorBorde.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = colorBorde.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconAlerta,
                                contentDescription = null,
                                tint = colorBorde,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (enRiesgo)
                                "LOTE EN ALERTA: ${infoLoteCritico.numeroLote}"
                            else
                                "LOTE VIGENTE: ${infoLoteCritico.numeroLote}",
                            style = FDType.Heading3.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorBorde
                            )
                        )
                        Text(
                            text = if (enRiesgo)
                                "Vence el ${infoLoteCritico.fechaVencimiento} (${infoLoteCritico.diasRestantes} días restantes). En alerta desde el ${infoLoteCritico.fechaInicioAlertaTexto}. Tramitar canje."
                            else
                                "Vence el ${infoLoteCritico.fechaVencimiento} (${infoLoteCritico.diasRestantes} días restantes). La alerta se activará el ${infoLoteCritico.fechaInicioAlertaTexto} ($diasVencimientoActual días antes).",
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                color = FDColors.TextSecondary
                            )
                        )
                    }
                }
            }
        } else {
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, FDColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = FDColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "No hay lotes con existencias activas en este momento. La regla de $diasVencimientoActual días antes del vencimiento se aplicará automáticamente en cuanto ingreses nuevo stock.",
                        style = FDType.Caption.copy(
                            fontSize = 11.5.sp,
                            color = FDColors.TextSecondary
                        )
                    )
                }
            }
        }

        // Chips de selección rápida con scroll horizontal
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "AVISAR CON ANTICIPACIÓN ANTES DE VENCER:",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FDColors.TextSecondary
                    )
                )
                if (isGuardando) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = FDColors.Primary)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CHIPS_DIAS_VENCIMIENTO.forEach { diasChip ->
                    val isSelected = diasVencimientoActual == diasChip
                    Surface(
                        color = if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) FDColors.Primary else FDColors.Border
                        ),
                        modifier = Modifier
                            .clickable {
                                inputTexto = diasChip.toString()
                                onDiasVencimientoChange(diasChip)
                            }
                            .bounceClick()
                    ) {
                        Text(
                            text = if (diasChip == 90) "$diasChip Días antes (Recomendado droguerías)" else "$diasChip Días antes",
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        ConfiguracionTextField(
            label = "Días de Anticipación Personalizados (15 a 365 días) *",
            value = inputTexto,
            onValueChange = { nuevo ->
                val soloDigitos = nuevo.filter { it.isDigit() }.take(3)
                inputTexto = soloDigitos
            },
            placeholder = "90",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Outlined.HourglassBottom
        )

    }
}

private data class LoteProximoInfo(
    val numeroLote: String,
    val fechaVencimiento: String,
    val diasRestantes: Long,
    val fechaInicioAlertaTexto: String
)

private fun calcularLoteMasProximo(
    lotes: List<LoteProducto>,
    diasAnticipacion: Int
): LoteProximoInfo? {
    if (lotes.isEmpty()) return null

    var mejorLote: LoteProximoInfo? = null
    var menorDias = Long.MAX_VALUE

    val hoy = LocalDate.now()
    val outFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-CO"))

    for (lote in lotes) {
        val raw = lote.vencimiento.trim()
        if (raw.isBlank()) continue

        val dias = FechaVencimientoHelper.diasHastaVencer(raw)
        if (dias != null) {
            if (dias < menorDias) {
                menorDias = dias.toLong()
                val fechaInicioAlerta = hoy.plusDays((dias - diasAnticipacion).toLong())
                val fechaInicioTexto = fechaInicioAlerta.format(outFormatter)

                mejorLote = LoteProximoInfo(
                    numeroLote = lote.numero.ifBlank { "Sin número" },
                    fechaVencimiento = raw,
                    diasRestantes = dias.toLong().coerceAtLeast(0L),
                    fechaInicioAlertaTexto = fechaInicioTexto
                )
            }
        }
    }

    return mejorLote
}
