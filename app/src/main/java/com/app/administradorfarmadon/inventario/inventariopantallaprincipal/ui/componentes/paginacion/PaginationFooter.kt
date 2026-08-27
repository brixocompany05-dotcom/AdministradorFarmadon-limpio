package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.paginacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*
/**
 * @deprecated Pagination numerada expuesta al empleado — reemplazada por InfiniteLoadingFooter.
 * Se conserva solo por compatibilidad historica; InventarioScreen ya no la usa.
 * La lista infinita silenciosa muestra solo "Cargando más..." (isLoadingMore).
 */
@Deprecated("Usar InfiniteLoadingFooter — paginacion silenciosa sin numeros")


@Composable
fun PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    itemsPerPage: Int,
    onPageChange: (Int) -> Unit,
    s: MedidaAdaptativa
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contenedor de Paginación Centralizado y Cómodo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Botón Anterior
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = currentPage > 1) { onPageChange(currentPage - 1) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, null,
                    tint = if (currentPage > 1) Color.White else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "ANTERIOR",
                    color = if (currentPage > 1) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.05f)))

            // Números de página
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                for (i in 1..totalPages.coerceAtMost(7)) {
                    PageNumberButton(
                        number = i,
                        isSelected = i == currentPage,
                        onClick = { onPageChange(i) }
                    )
                }
                
                if (totalPages > 7) {
                    Text("...", color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 4.dp))
                    PageNumberButton(
                        number = totalPages,
                        isSelected = totalPages == currentPage,
                        onClick = { onPageChange(totalPages) }
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.05f)))

            // Botón Siguiente
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = currentPage < totalPages) { onPageChange(currentPage + 1) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SIGUIENTE",
                    color = if (currentPage < totalPages) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = if (currentPage < totalPages) Color.White else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PageNumberButton(number: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .border(
                width = 0.5.dp, 
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.05f), 
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
        )
    }
}
