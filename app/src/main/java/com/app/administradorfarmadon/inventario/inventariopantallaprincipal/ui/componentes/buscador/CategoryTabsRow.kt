package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.buscador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

/**
 * Pestañas / Chips horizontales fluidos para filtrar departamentos de productos.
 */
@Composable
fun CategoryTabsRow(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = (category == "Todos" && selectedCategories.isEmpty()) || category in selectedCategories

            OutlinedButton(
                onClick = { onCategoryClick(category) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                    contentColor = if (isSelected) FDColors.TextPrimary else FDColors.TextSecondary
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.dp else 0.5.dp,
                    color = if (isSelected) FDColors.Primary else FDColors.Border
                ),
                modifier = Modifier
                    .height(34.dp)
                    .bounceClick(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = category,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
