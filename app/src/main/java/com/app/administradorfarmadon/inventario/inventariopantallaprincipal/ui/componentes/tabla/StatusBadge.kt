package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa

@Composable
fun StatusBadge(status: String, s: MedidaAdaptativa) {
    val (dotColor, textColor, bgAlpha) = when (status) {
        "Disponible" -> Triple(Color(0xFF10B981), Color(0xFF10B981), 0.08f)
        "Stock bajo" -> Triple(Color(0xFFF59E0B), Color(0xFFF59E0B), 0.08f)
        "En cuarentena" -> Triple(Color(0xFF8B5CF6), Color(0xFF8B5CF6), 0.10f)
        else -> Triple(Color(0xFF94A3B8), Color(0xFF94A3B8), 0.06f)
    }

    Row(
        modifier = Modifier
            .background(dotColor.copy(alpha = bgAlpha), RoundedCornerShape(6.dp))
            .border(0.5.dp, dotColor.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = status,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp
        )
    }
}
