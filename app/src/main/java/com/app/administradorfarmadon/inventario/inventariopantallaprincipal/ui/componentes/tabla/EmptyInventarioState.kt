package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa

@Composable
fun EmptyInventarioState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    s: MedidaAdaptativa,
    tab: String = "TODOS"
) {
    val titulo = when {
        tab == "POR_REPONER" -> "¡TODO TU STOCK ESTÁ AL DÍA!"
        tab == "POR_VENCER" -> "CERO ALERTAS DE VENCIMIENTO"
        tab == "PAUSADOS" -> "NO TIENES PRODUCTOS PAUSADOS"
        hasFilters -> "NO SE ENCONTRARON RESULTADOS"
        else -> "TU INVENTARIO ESTÁ VACÍO"
    }

    val subtitulo = when {
        tab == "POR_REPONER" -> "Ningún producto de tu farmacia se encuentra por debajo del stock mínimo."
        tab == "POR_VENCER" -> "No tienes lotes ni medicamentos próximos a vencer en los siguientes 60 días."
        tab == "PAUSADOS" -> "Todos tus productos se encuentran activos y disponibles para venta."
        hasFilters -> "Intenta ajustar los términos de búsqueda o filtros\npara encontrar lo que buscas."
        else -> "No hay productos registrados en esta sección.\nUsa el botón superior para agregar uno nuevo."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono Estilizado Linear
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FDColors.Glass, RoundedCornerShape(16.dp))
                .border(0.5.dp, FDColors.Border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasFilters) Icons.Default.SearchOff else Icons.Default.FilterListOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = FDColors.TextTertiary
            )
        }
      
        Spacer(modifier = Modifier.height(24.dp))
      
        Text(
            text = titulo,
            color = FDColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
      
        Spacer(modifier = Modifier.height(8.dp))
      
        Text(
            text = subtitulo,
            color = FDColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
      
        Spacer(modifier = Modifier.height(32.dp))
      
        if (hasFilters || tab != "TODOS") {
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FDColors.Glass, 
                    contentColor = FDColors.TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp).border(0.5.dp, FDColors.Border, RoundedCornerShape(10.dp))
            ) {
                Text(if (tab != "TODOS" && !hasFilters) "VER TODOS LOS PRODUCTOS" else "LIMPIAR FILTROS", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}
