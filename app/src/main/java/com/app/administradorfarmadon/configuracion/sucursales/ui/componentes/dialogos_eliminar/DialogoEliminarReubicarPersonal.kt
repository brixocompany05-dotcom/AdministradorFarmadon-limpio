package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.dialogos_eliminar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.logica.ColaboradorItem
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun DialogoEliminarReubicarPersonal(
    nombreSucursal: String,
    colaboradores: List<ColaboradorItem>,
    sedesDisponibles: List<Sucursal>,
    subOpcionReubicar: String,
    sedeDestinoTodosId: String,
    mapaDestinoIndividual: Map<String, String>,
    onSetSubOpcionReubicar: (String) -> Unit,
    onSetSedeDestinoTodos: (String) -> Unit,
    onSetSedeDestinoIndividual: (String, String) -> Unit,
    onVolverPaso: () -> Unit,
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
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Text(
                    text = "Destino del Personal (2/2)",
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
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
                    text = "Selecciona cómo reubicar al personal de \"$nombreSucursal\":",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.SemiBold, fontSize = s.textBody.value.sp * 0.92f),
                    color = colores.textoPrincipal
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    RadioButton(
                        selected = subOpcionReubicar == "TODOS_IGUAL",
                        onClick = { onSetSubOpcionReubicar("TODOS_IGUAL") },
                        colors = RadioButtonDefaults.colors(selectedColor = colores.botonPrimarioFondo)
                    )
                    Text(
                        text = "Reubicar a todos a la misma sede",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                        color = colores.textoPrincipal,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (subOpcionReubicar == "TODOS_IGUAL") {
                    SelectorSucursalDropdown(
                        label = "Sede Destino para todo el personal",
                        sucursales = sedesDisponibles,
                        sucursalSeleccionadaId = sedeDestinoTodosId,
                        onSucursalSelected = onSetSedeDestinoTodos,
                        s = s
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    RadioButton(
                        selected = subOpcionReubicar == "INDIVIDUAL",
                        onClick = { onSetSubOpcionReubicar("INDIVIDUAL") },
                        colors = RadioButtonDefaults.colors(selectedColor = colores.botonPrimarioFondo)
                    )
                    Text(
                        text = "Asignar individualmente por colaborador",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                        color = colores.textoPrincipal,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (subOpcionReubicar == "INDIVIDUAL") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(s.xs),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                    ) {
                        colaboradores.forEach { colab ->
                            val actualId = mapaDestinoIndividual[colab.id] ?: sedeDestinoTodosId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                Text(
                                    text = "${colab.nombre} (${colab.rol})",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontWeight = FontWeight.Bold, fontSize = s.textBody.value.sp * 0.88f),
                                    color = colores.textoPrincipal,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1.2f)) {
                                    SelectorSucursalDropdown(
                                        label = "",
                                        sucursales = sedesDisponibles,
                                        sucursalSeleccionadaId = actualId,
                                        onSucursalSelected = { nuevaId ->
                                            onSetSedeDestinoIndividual(colab.id, nuevaId)
                                        },
                                        s = s
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmarEliminar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.botonPrimarioFondo,
                    contentColor = colores.botonPrimarioTexto
                ),
                shape = RoundedCornerShape(s.radiusChip)
            ) {
                Text(
                    text = "CONFIRMAR Y ELIMINAR",
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

@Composable
private fun SelectorSucursalDropdown(
    label: String,
    sucursales: List<Sucursal>,
    sucursalSeleccionadaId: String,
    onSucursalSelected: (String) -> Unit,
    s: MedidaAdaptativa
) {
    val colores = TokensFarmadon.colores
    var expanded by remember { mutableStateOf(false) }

    val actualNombre = sucursales.find { it.id == sucursalSeleccionadaId }?.nombre
        ?: if (sucursalSeleccionadaId == "todas") "Todas las Sedes (Itinerante)" else "Sede Principal"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.82f), color = colores.textoTerciario)
        }
        Box {
            Surface(
                onClick = { expanded = true },
                color = colores.cardElevada,
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(s.borderWidth * 0.8f, colores.cardBorde)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = actualNombre,
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.SemiBold, fontSize = s.textBody.value.sp * 0.88f),
                        color = colores.textoPrincipal,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▼", fontSize = 10.sp, color = colores.textoTerciario)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todas las Sedes (Itinerante)", style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.88f)) },
                    onClick = {
                        onSucursalSelected("todas")
                        expanded = false
                    }
                )
                sucursales.forEach { suc ->
                    DropdownMenuItem(
                        text = { Text(suc.nombre, style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.88f)) },
                        onClick = {
                            onSucursalSelected(suc.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
