package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PanelEliminarProducto(
    product: MoldeProductos,
    movements: List<MovimientoInventario> = emptyList(),
    isPrivileged: Boolean,
    onEliminar: (motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onEliminadoExito: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val totalLotes = product.lotes.size
    val totalUnidades = product.lotes.values.sumOf { it.cantidad + it.cantidadBloqueada }
    val tieneStock = totalUnidades > 0.01
    val tieneVentas = movements.any {
        it.tipo.uppercase().contains("VENTA") || it.tipo.uppercase().contains("DISPENSACION")
    }
    val bloqueadoPorStock = tieneStock
    val bloqueadoPorVentas = tieneVentas
    val bloqueado = bloqueadoPorStock || bloqueadoPorVentas
    val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val emailActual = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val isCreator = product.creadoPorUid.isNotBlank() && product.creadoPorUid == uidActual ||
            product.auditCreatedByEmail.isNotBlank() && product.auditCreatedByEmail.equals(
        emailActual,
        ignoreCase = true
    )
    val puedeBorrarPorRol = isPrivileged || isCreator || totalLotes == 0

    val mensajeReal = when {
        bloqueadoPorVentas -> {
            val c = movements.count {
                it.tipo.uppercase().contains("VENTA") || it.tipo.uppercase()
                    .contains("DISPENSACION")
            }
            "Tiene $c venta(s). No se puede borrar. Usa Pausar."
        }

        bloqueadoPorStock -> {
            val ejemplo = product.lotes.values.filter { it.cantidad + it.cantidadBloqueada > 0 }
                .sortedBy { it.numero }.take(2)
                .joinToString(", ") { "${it.numero} (${it.cantidad.toInt()})" }
            "Tiene ${totalUnidades.toInt()} unidades en ${if (ejemplo.isNotBlank()) ejemplo else "$totalLotes lote(s)"}. Anula el lote."
        }

        !puedeBorrarPorRol -> "Solo el creador o admin puede borrar."
        else -> null
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (mensajeReal != null) {
            Text(
                text = mensajeReal,
                style = FDType.Caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FDColors.TextSecondary
                )
            )
        }

        Button(
            onClick = { mostrarDialogo = true },
            enabled = !bloqueado && puedeBorrarPorRol && !isProcesando,
            colors = ButtonDefaults.buttonColors(
                containerColor = FDColors.Error,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (bloqueado || !puedeBorrarPorRol) "No se puede borrar" else "Eliminar producto",
                style = FDType.Label.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Unspecified
                )
            )
        }

        if (mensajeError != null) {
            Text(
                mensajeError!!,
                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.Error)
            )
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { if (!isProcesando) mostrarDialogo = false },
            icon = {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    null,
                    tint = FDColors.Error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    "¿Eliminar ${product.nombre}?",
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                // Regla "Bloquear Teclado": este diálogo tampoco abre el teclado si está activada.
                AplicarBloqueoTecladoVentana()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Se libera el código y queda en auditoría. No se puede deshacer.",
                        style = FDType.Caption.copy(color = FDColors.TextSecondary)
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it; mensajeError = null },
                        label = {
                            Text(
                                "Motivo (mín. 10) *",
                                style = FDType.Caption.copy(fontSize = 11.sp)
                            )
                        },
                        placeholder = {
                            Text(
                                "Ej: Prueba creada por error",
                                style = FDType.Caption.copy(color = FDColors.TextTertiary)
                            )
                        },
                        minLines = 2,
                        maxLines = 3,
                        enabled = !isProcesando,
                        isError = motivo.isNotBlank() && motivo.trim().length < 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (motivo.isNotBlank() && motivo.trim().length < 10) {
                        Text(
                            "Mínimo 10 caracteres",
                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Error)
                        )
                    }
                    if (mensajeError != null) {
                        Text(mensajeError!!, style = FDType.Caption.copy(color = FDColors.Error))
                    }
                }
            },
                    confirmButton = {
                Button(
                    onClick = {
                        if (motivo.trim().length < 10 || isProcesando) return@Button
                        isProcesando = true
                        mensajeError = null
                        onEliminar(motivo) { result ->
                            isProcesando = false
                            if (result.isSuccess) {
                                mostrarDialogo = false
                                onEliminadoExito()
                            } else {
                                mensajeError =
                                    result.exceptionOrNull()?.message ?: "No se pudo eliminar."
                            }
                        }
                    },
                    enabled = motivo.trim().length >= 10 && !isProcesando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    if (isProcesando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Eliminando...",
                            style = FDType.Label.copy(color = Color.Unspecified)
                        )
                    } else {
                        Text(
                            "Sí, eliminar",
                            style = FDType.Label.copy(color = Color.Unspecified)
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isProcesando) mostrarDialogo = false },
                    enabled = !isProcesando
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = FDColors.SurfaceElevated
        )
    }
}
