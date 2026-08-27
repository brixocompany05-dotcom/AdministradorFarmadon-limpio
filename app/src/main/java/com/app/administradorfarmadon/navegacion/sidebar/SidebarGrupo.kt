package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

@Composable
fun SidebarGrupo(
    label: String,
    isActive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Título de categoría sutil y profesional
        Text(
            text = label.uppercase(),
            style = FDType.Label.copy(
                color = if (isActive) SidebarTheme.Accent else SidebarTheme.TextSecondary.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier.padding(top = 4.dp),
            content = content
        )
    }
}
