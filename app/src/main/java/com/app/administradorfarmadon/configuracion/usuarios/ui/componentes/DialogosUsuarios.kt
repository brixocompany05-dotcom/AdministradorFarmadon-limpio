package com.app.administradorfarmadon.configuracion.usuarios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon


@Composable
fun DialogoConfirmarSuspension(
    nombreUsuario: String,
    estaActivo: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()
    val accion = if (estaActivo) "Suspender Acceso" else "Reactivar Acceso"
    val colorBoton = if (estaActivo) colores.estadoPeligro else colores.estadoExito

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
                        .size(s.iconLarge * 1.55f)
                        .clip(CircleShape)
                        .background(if (estaActivo) colores.estadoPeligro.copy(alpha = 0.1f) else colores.estadoExito.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = colorBoton,
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Text(
                    text = "¿$accion?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = if (estaActivo) {
                    "¿Confirmas que deseas suspender el acceso de \"$nombreUsuario\"? No podrá abrir caja ni ingresar al sistema, pero su historial de ventas quedará intacto."
                } else {
                    "¿Confirmas que deseas reactivar el acceso de \"$nombreUsuario\" al punto de venta?"
                },
                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                color = colores.textoSecundario,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorBoton,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text(
                    text = if (estaActivo) "SUSPENDER ACCESO" else "REACTIVAR ACCESO",
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), color = colores.textoSecundario)
            }
        }
    )
}

@Composable
fun DialogoDescartarCambiosUsuarios(
    onConfirmDescartar: () -> Unit,
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
                        .size(s.iconLarge * 1.55f)
                        .clip(CircleShape)
                        .background(colores.textoPrincipal.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Text(
                    text = "¿Descartar cambios no guardados?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "Tienes datos sin guardar en el formulario. Si sales ahora, las modificaciones se perderán.",
                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                color = colores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.botonPrimarioFondo,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text("SEGUIR EDITANDO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirmDescartar) {
                Text("DESCARTAR", style = TokensFarmadon.tipografia.etiqueta.copy(color = colores.estadoPeligro, fontWeight = FontWeight.SemiBold, fontSize = s.textLabel.value.sp))
            }
        }
    )
}

@Composable
fun DialogoConfirmarEliminarUsuario(
    nombreUsuario: String,
    motivo: String,
    onMotivoChange: (String) -> Unit,
    onConfirm: () -> Unit,
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
                        .size(s.iconLarge * 1.55f)
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
                    text = "¿Dar de Baja a \"$nombreUsuario\"?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                Text(
                    text = "Perderá el acceso de inmediato y saldrá de la lista activa. Su ficha se archivará: podrás recontratarlo cuando quieras desde la sección DADOS DE BAJA.",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                    color = colores.textoSecundario
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= 200) onMotivoChange(it) },
                    label = { Text("Motivo de la baja (obligatorio)", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp)) },
                    placeholder = { Text("Ej: Renuncia voluntaria / fin de contrato", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = motivo.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.estadoPeligro,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text("DAR DE BAJA", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), color = colores.textoSecundario)
            }
        }
    )
}
