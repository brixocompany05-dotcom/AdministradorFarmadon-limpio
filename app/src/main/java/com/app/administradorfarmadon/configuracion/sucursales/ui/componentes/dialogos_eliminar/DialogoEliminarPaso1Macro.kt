package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.dialogos_eliminar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.sucursales.logica.ColaboradorItem
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun DialogoEliminarPaso1Macro(
    nombreSucursal: String,
    colaboradores: List<ColaboradorItem>,
    onSeleccionarOpcionMacro: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(s.radiusCard * 1.1f),
        containerColor = colores.cardBase,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
            ) {
                Surface(
                    color = colores.textoPrincipal.copy(alpha = 0.06f),
                    shape = CircleShape,
                    modifier = Modifier.size(s.iconLarge * 1.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = colores.textoPrincipal,
                            modifier = Modifier.size(s.iconMedium)
                        )
                    }
                }

                Surface(
                    color = colores.botonPrimarioFondo.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(s.radiusChip)
                ) {
                    Text(
                        text = "PASO 1 DE 2",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp * 0.8f,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = colores.botonPrimarioFondo,
                        modifier = Modifier.padding(horizontal = s.xs, vertical = 2.dp)
                    )
                }

                Text(
                    text = "¿Qué deseas hacer con el personal?",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Bold),
                    color = colores.textoPrincipal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                Text(
                    text = "La sede \"$nombreSucursal\" tiene ${colaboradores.size} colaborador${if (colaboradores.size > 1) "es" else ""} asignado${if (colaboradores.size > 1) "s" else ""}. Selecciona cómo proceder:",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                    color = colores.textoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // OPCIÓN 1: REUBICAR PERSONAL
                Surface(
                    onClick = { onSeleccionarOpcionMacro("REUBICAR") },
                    color = colores.cardElevada,
                    shape = RoundedCornerShape(s.radiusCard * 0.85f),
                    border = BorderStroke(s.borderWidth * 1.2f, colores.textoPrincipal.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(s.sm * 1.1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                        Surface(
                            color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = colores.botonPrimarioFondo,
                                    modifier = Modifier.size(s.iconSmall * 1.1f)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "REUBICAR AL PERSONAL",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp,
                                    letterSpacing = 0.4.sp
                                ),
                                color = colores.textoPrincipal
                            )
                            Text(
                                text = "Transferir los colaboradores a otra sucursal para que sigan trabajando",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.88f),
                                color = colores.textoTerciario
                            )
                        }
                    }
                }

                // OPCIÓN 2: ELIMINAR / DAR DE BAJA
                Surface(
                    onClick = { onSeleccionarOpcionMacro("ELIMINAR_TODOS") },
                    color = colores.estadoPeligro.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(s.radiusCard * 0.85f),
                    border = BorderStroke(s.borderWidth * 1.1f, colores.estadoPeligro.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(s.sm * 1.1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                        Surface(
                            color = colores.estadoPeligro.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonOff,
                                    contentDescription = null,
                                    tint = colores.estadoPeligro,
                                    modifier = Modifier.size(s.iconSmall * 1.1f)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "DAR DE BAJA Y ELIMINAR ACCESOS",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp,
                                    letterSpacing = 0.4.sp
                                ),
                                color = colores.estadoPeligro
                            )
                            Text(
                                text = "Eliminar sus cuentas de usuario; ya no podrán ingresar a la app",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.88f),
                                color = colores.textoTerciario
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        fontSize = s.textLabel.value.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colores.textoTerciario
                )
            }
        }
    )
}
