package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun DialogoLimitePlan(
    planNombre: String,
    maxSucursales: Int,
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
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Text(
                    text = "Límite de Sedes del Plan",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Text(
                    text = "Tu plan actual ($planNombre) permite hasta $maxSucursales ${if (maxSucursales == 1) "sede" else "sedes"}.",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                    color = colores.textoSecundario,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Para conectar más locales, solicita una ampliación de plan a soporte.",
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f),
                    color = colores.textoTerciario,
                    textAlign = TextAlign.Center
                )
            }
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
                Text("ENTENDIDO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
            }
        }
    )
}

@Composable
fun DialogoConfirmarEliminar(
    nombreSucursal: String,
    colaboradoresAsignados: List<String> = emptyList(),
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
                    text = "¿Eliminar Sucursal?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                Text(
                    text = "¿Confirmas que deseas eliminar la sede \"$nombreSucursal\"? Esta acción liberará un cupo de tu plan.",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                    color = colores.textoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (colaboradoresAsignados.isNotEmpty()) {
                    Surface(
                        color = colores.cardElevada,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.8f, colores.cardBorde),
                        modifier = Modifier.fillMaxWidth().padding(top = s.xs * 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(s.sm), verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                            Text(
                                text = "Personal asignado (${colaboradoresAsignados.size}):",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp),
                                color = colores.textoPrincipal
                            )
                            Text(
                                text = "${colaboradoresAsignados.joinToString(", ")}. Al eliminar la sede, sus cuentas pasarán automáticamente a Sede Itinerante para que sigan operativos.",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f),
                                color = colores.textoSecundario
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.estadoPeligro,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text("ELIMINAR SEDE", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
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
fun DialogoDescartarCambios(
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
