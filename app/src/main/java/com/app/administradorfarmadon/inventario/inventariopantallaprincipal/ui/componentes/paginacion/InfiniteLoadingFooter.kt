package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.paginacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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
 * Pie silencioso de paginación infinita.
 * El empleado nunca ve "50" ni números de página; solo ve lista infinita
 * y, al desplazar hasta el final, un pie honesto: "Cargando más..." o nada.
 */
@Composable
fun InfiniteLoadingFooter(
    isLoadingMore: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FDColors.Primary)
                Text("Cargando más...", style = FDType.Body.copy(color = FDColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            }
            errorMessage != null -> Text(errorMessage, style = FDType.Caption.copy(color = FDColors.TextSecondary, fontSize = 12.sp))
            else -> Spacer(Modifier.height(1.dp))
        }
    }
}
