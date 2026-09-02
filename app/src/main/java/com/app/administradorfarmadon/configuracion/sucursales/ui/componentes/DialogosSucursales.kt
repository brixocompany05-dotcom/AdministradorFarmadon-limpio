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
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.logica.ColaboradorItem
import com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.dialogos_eliminar.*
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
fun DialogoEliminarSedePasos(
    nombreSucursal: String,
    colaboradores: List<ColaboradorItem>,
    sedesDisponibles: List<Sucursal>,
    pasoActual: Int,
    opcionMacro: String,
    subOpcionReubicar: String,
    sedeDestinoTodosId: String,
    mapaDestinoIndividual: Map<String, String>,
    onSeleccionarOpcionMacro: (String) -> Unit,
    onSetSubOpcionReubicar: (String) -> Unit,
    onSetSedeDestinoTodos: (String) -> Unit,
    onSetSedeDestinoIndividual: (String, String) -> Unit,
    onVolverPaso: () -> Unit,
    onConfirmarEliminar: () -> Unit,
    onDismiss: () -> Unit
) {
    if (colaboradores.isEmpty() || opcionMacro == "SIN_PERSONAL") {
        DialogoEliminarSedeSinPersonal(
            nombreSucursal = nombreSucursal,
            onConfirmarEliminar = onConfirmarEliminar,
            onDismiss = onDismiss
        )
    } else if (pasoActual == 1) {
        DialogoEliminarPaso1Macro(
            nombreSucursal = nombreSucursal,
            colaboradores = colaboradores,
            onSeleccionarOpcionMacro = onSeleccionarOpcionMacro,
            onDismiss = onDismiss
        )
    } else if (pasoActual == 2 && opcionMacro == "REUBICAR") {
        DialogoEliminarReubicarPersonal(
            nombreSucursal = nombreSucursal,
            colaboradores = colaboradores,
            sedesDisponibles = sedesDisponibles,
            subOpcionReubicar = subOpcionReubicar,
            sedeDestinoTodosId = sedeDestinoTodosId,
            mapaDestinoIndividual = mapaDestinoIndividual,
            onSetSubOpcionReubicar = onSetSubOpcionReubicar,
            onSetSedeDestinoTodos = onSetSedeDestinoTodos,
            onSetSedeDestinoIndividual = onSetSedeDestinoIndividual,
            onVolverPaso = onVolverPaso,
            onConfirmarEliminar = onConfirmarEliminar,
            onDismiss = onDismiss
        )
    } else if (pasoActual == 2 && opcionMacro == "ELIMINAR_TODOS") {
        DialogoEliminarAdvertenciaBaja(
            nombreSucursal = nombreSucursal,
            colaboradores = colaboradores,
            onVolverPaso = onVolverPaso,
            onConfirmarEliminar = onConfirmarEliminar
        )
    }
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
