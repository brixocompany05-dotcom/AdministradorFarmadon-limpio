package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.dialogos_eliminar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.sucursales.logica.ColaboradorItem
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun DialogoEliminarAdvertenciaBaja(
    nombreSucursal: String,
    colaboradores: List<ColaboradorItem>,
    onVolverPaso: () -> Unit,
    onConfirmarEliminar: () -> Unit
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    AlertDialog(
        onDismissRequest = onVolverPaso,
        shape = RoundedCornerShape(s.radiusCard),
        containerColor = colores.cardBase,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge * 1.4f)
                        .clip(CircleShape)
                        .background(colores.estadoPeligro.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = colores.estadoPeligro,
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Text(
                    text = "Confirmación de Baja (2/2)",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Surface(
                color = colores.estadoPeligro.copy(alpha = 0.08f),
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(s.sm),
                    verticalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Text(
                        text = "⚠️ ADVERTENCIA DE ELIMINACIÓN PERMANENTE",
                        style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp),
                        color = colores.estadoPeligro
                    )
                    Text(
                        text = "Estás a punto de eliminar PERMANENTEMENTE los accesos de ${colaboradores.size} colaborador${if (colaboradores.size > 1) "es" else ""} de \"$nombreSucursal\":",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                        color = colores.textoPrincipal
                    )
                    Text(
                        text = colaboradores.joinToString("\n") { "• ${it.nombre} (${it.rol})" },
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontWeight = FontWeight.Bold, fontSize = s.textBody.value.sp * 0.88f),
                        color = colores.textoSecundario
                    )
                    Text(
                        text = "Estas personas ya NO podrán iniciar sesión ni acceder a la app de la farmacia.",
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.85f),
                        color = colores.estadoPeligro
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmarEliminar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.estadoPeligro,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text(
                    text = "SÍ, ELIMINAR SEDE Y PERSONAL",
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onVolverPaso) {
                Text(
                    text = "VOLVER",
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp),
                    color = colores.textoSecundario
                )
            }
        }
    )
}
