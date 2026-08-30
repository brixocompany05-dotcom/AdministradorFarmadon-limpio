package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun SucursalesFilterTabs(
    selectedFilter: String,
    counts: Map<String, Int>,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    val filters = listOf("TODAS", "OPERANDO", "MANTENIMIENTO")
    
    Row(
        modifier = modifier
            .wrapContentWidth()
            .clip(FDShapes.Medium)
            .background(colores.cardElevada.copy(alpha = 0.5f)) // Fondo del riel del selector
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Espaciado corto entre cápsulas
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            val label = when(filter) {
                "TODAS" -> "Todas"
                "OPERANDO" -> "Operando"
                "MANTENIMIENTO" -> "Mantenimiento"
                else -> filter
            }
            val count = counts[filter] ?: 0

            Surface(
                onClick = { onFilterSelected(filter) },
                color = if (isSelected) colores.botonPrimarioFondo else Color.Transparent,
                shape = FDShapes.Small,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .width(140.dp) // Ancho medido ergonómico
                    .height(38.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label.uppercase(),
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isSelected) colores.botonPrimarioTexto else colores.textoTerciario
                    )
                    
                    if (count > 0 || filter == "TODAS") {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$count",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) colores.botonPrimarioTexto.copy(alpha = 0.7f) else colores.textoTerciario.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
