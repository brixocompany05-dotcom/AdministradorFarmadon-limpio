package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus

@Composable
fun ConnectionStatusIndicator(status: NetworkStatus) {
    val (color, label) = when (status) {
        NetworkStatus.CONECTADO -> Color(0xFF10B981) to "CONEXIÓN ESTABLE"
        NetworkStatus.CONEXION_LENTA -> Color(0xFFF59E0B) to "CONEXIÓN LENTA"
        NetworkStatus.ESTADO_DEGRADADO -> Color(0xFFF59E0B) to "SINCRONIZACIÓN LENTA"
        NetworkStatus.SIN_SALIDA -> Color(0xFFEF4444) to "SIN INTERNET"
        NetworkStatus.DESCONECTADO -> Color(0xFF6B7280) to "DESCONECTADO"
        else -> Color(0xFF6B7280) to "DESCONECTADO"
    }


    Row(
        modifier = Modifier
            .height(28.dp)
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .border(0.5.dp, color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
