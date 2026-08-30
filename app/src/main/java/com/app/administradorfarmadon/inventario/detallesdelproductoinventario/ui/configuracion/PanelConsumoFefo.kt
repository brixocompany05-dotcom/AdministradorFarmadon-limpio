package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Consumo de lotes: FEFO automático (el que vence antes sale primero) o elección manual.
 * Al apagar FEFO se conserva como principal el lote que FEFO venía consumiendo.
 */
@Composable
fun PanelConsumoFefo(
    fefoAutomatico: Boolean,
    onFefoChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (fefoAutomatico) FDColors.Primary.copy(alpha = 0.12f) else FDColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        null,
                        tint = if (fefoAutomatico) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "FEFO automático",
                        style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        if (fefoAutomatico)
                            "Encendido · el lote que vence antes se consume primero."
                        else
                            "Apagado · eliges manualmente qué lote se consume primero.",
                        style = FDType.Caption.copy(fontSize = 11.5.sp),
                        color = FDColors.TextSecondary
                    )
                }
                Switch(
                    checked = fefoAutomatico,
                    onCheckedChange = onFefoChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = FDColors.Primary,
                        checkedThumbColor = FDColors.PrimaryText
                    )
                )
            }
        }

        Surface(
            color = if (fefoAutomatico) FDColors.Primary.copy(alpha = 0.06f) else FDColors.Warning.copy(alpha = 0.07f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (fefoAutomatico)
                    "Mientras esté encendido, no se puede elegir lote a mano: el sistema revisa todos los lotes con stock y ordena la venta por vencimiento, del que vence antes al que vence después."
                else
                    "Se conserva como principal el lote que FEFO venía consumiendo. Puedes cambiarlo desde la ficha del lote (3 · Estado) con “Elegir este lote de consumo”.",
                style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary, lineHeight = 16.sp),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
