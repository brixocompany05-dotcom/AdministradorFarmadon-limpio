package com.app.administradorfarmadon.inventario.componentes_maestros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Bloqueo de emergencia para productos eliminados.
 * Diseño minimalista Rojo OLED para máxima visibilidad sin lag.
 * Ubicación: Componentes Maestros (Compartido).
 */
@Composable
fun DeletedProductOverlay(
    show: Boolean,
    onBack: () -> Unit,
    eliminadoPor: String = "",
    fechaEliminacion: String = "",
    motivoEliminacion: String = ""
) {
    if (!show) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0B)) // Negro OLED de fondo
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFEF4444), // Rojo Alerta
                modifier = Modifier.size(64.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PRODUCTO ELIMINADO",
                    style = FDType.Heading1.copy(
                        color = Color(0xFFEF4444),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (eliminadoPor.isNotBlank() || fechaEliminacion.isNotBlank()) {
                        buildString {
                            if (eliminadoPor.isNotBlank()) append("Eliminado por $eliminadoPor")
                            if (fechaEliminacion.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(fechaEliminacion)
                            }
                            if (motivoEliminacion.isNotBlank()) {
                                append("\nMotivo: $motivoEliminacion")
                            }
                        }
                    } else "Este registro ya no existe en el sistema. Es probable que haya sido eliminado por otro administrador.",
                    style = FDType.Body.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    "REGRESAR AL INVENTARIO",
                    style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 14.sp)
                )
            }
        }
    }
}
