package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryBadge(category: String) {
    val (bgColor, textColor) = when {
        category.contains("Gaseosas", ignoreCase = true) || category.contains("Bebidas", ignoreCase = true) -> 
            Color(0xFF6366F1).copy(alpha = 0.08f) to Color(0xFF818CF8)
        category.contains("Alimentos", ignoreCase = true) || category.contains("Pan", ignoreCase = true) -> 
            Color(0xFF3B82F6).copy(alpha = 0.08f) to Color(0xFF60A5FA)
        category.contains("Limpieza", ignoreCase = true) -> 
            Color(0xFF10B981).copy(alpha = 0.08f) to Color(0xFF34D399)
        category.contains("Medicamentos", ignoreCase = true) -> 
            Color(0xFFF59E0B).copy(alpha = 0.08f) to Color(0xFFFBBF24)
        else -> Color.White.copy(alpha = 0.05f) to Color.White.copy(alpha = 0.5f)
    }

    Text(
        text = category,
        color = textColor,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(0.5.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
