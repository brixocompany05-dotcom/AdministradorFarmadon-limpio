package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.dialogos_eliminar

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
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun DialogoEliminarSedeSinPersonal(
    nombreSucursal: String,
    onConfirmarEliminar: () -> Unit,
    onDismiss: () -> Unit
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        .background(colores.textoPrincipal.copy(alpha = 0.06f)),
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
                    text = "¿Eliminar Sede?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "¿Confirmas que deseas eliminar la sede \"$nombreSucursal\"? Esta acción liberará un cupo de tu plan.",
                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                color = colores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
                    text = "ELIMINAR SEDE",
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp),
                    color = colores.textoSecundario
                )
            }
        }
    )
}
