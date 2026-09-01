package com.app.administradorfarmadon.compras.saldoafavor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.app.administradorfarmadon.compras.saldoafavor.SaldoAFavorLiquidacion
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import java.util.Locale

/**
 * Recuerda al usuario que el proveedor nos debe plata y ofrece aplicarla
 * como descuento en esta factura, con la liquidación en vivo (total − saldo = a pagar).
 */
@Composable
fun SaldoAFavorBanner(
    liquidacion: SaldoAFavorLiquidacion,
    usar: Boolean,
    onUsarChange: (Boolean) -> Unit,
    simboloMoneda: String,
    modifier: Modifier = Modifier
) {
    if (liquidacion.disponible <= 0.01) return

    Surface(
        color = FDColors.Primary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Este proveedor tiene $simboloMoneda ${String.format(Locale.US, "%.2f", liquidacion.disponible)} a favor",
                        style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.Primary
                    )
                    Text(
                        "Plata que nos debe. Puedes usarla como descuento en esta factura.",
                        style = FDType.Caption.copy(fontSize = 10.5.sp),
                        color = FDColors.TextSecondary
                    )
                }
                Switch(
                    checked = usar,
                    onCheckedChange = onUsarChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = FDColors.Primary,
                        checkedThumbColor = FDColors.PrimaryText
                    )
                )
            }

            if (usar) {
                HorizontalDivider(color = FDColors.Primary.copy(alpha = 0.2f), thickness = 0.5.dp)
                FilaBanner("Total factura", "$simboloMoneda ${String.format(Locale.US, "%.2f", liquidacion.totalFactura)}", FDColors.TextPrimary)
                FilaBanner("− Saldo a favor aplicado", "− $simboloMoneda ${String.format(Locale.US, "%.2f", liquidacion.aplicado)}", FDColors.Success)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL A PAGAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                    Text(
                        "$simboloMoneda ${String.format(Locale.US, "%.2f", liquidacion.netoAPagar)}",
                        style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                        color = FDColors.Primary
                    )
                }
                Text(
                    "Se aplica al guardar la recepción. Si cierras sin guardar, el saldo queda intacto.",
                    style = FDType.Caption.copy(fontSize = 10.sp),
                    color = FDColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun FilaBanner(etiqueta: String, valor: String, colorValor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
        Text(valor, style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = colorValor)
    }
}
