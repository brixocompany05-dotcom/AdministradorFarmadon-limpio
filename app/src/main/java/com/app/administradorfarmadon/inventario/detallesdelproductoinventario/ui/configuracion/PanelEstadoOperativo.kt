package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Estado del producto en inventario.
 * Activo = se puede vender si hay stock. Pausado = queda guardado pero oculto, aunque tenga stock.
 */
@Composable
fun PanelEstadoOperativo(
    isActivo: Boolean,
    onActivoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onActivoChange(!isActivo) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isActivo) "Activo" else "Pausado",
                        style = FDType.Body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FDColors.TextPrimary
                        )
                    )
                    Text(
                        text = if (isActivo) "Con stock, se puede vender." else "Guardado en inventario, pero no aparece para vender aunque tenga stock.",
                        style = FDType.Caption.copy(
                            fontSize = 11.5.sp,
                            color = FDColors.TextSecondary
                        )
                    )
                }
                Switch(
                    checked = isActivo,
                    onCheckedChange = onActivoChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FDColors.Primary,
                        checkedTrackColor = FDColors.Primary.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
