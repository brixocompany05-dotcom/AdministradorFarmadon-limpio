package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

/**
 * Dialog Enterprise para Anulación de Ingreso por Tipeo.
 */
@Composable
fun AnulacionIngresoDialog(
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirmAnulacion: (motivo: String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SaaSBackground,
            border = BorderStroke(1.dp, SaaSBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Undo, contentDescription = null, tint = SaaSError)
                        Text(
                            text = "ANULAR INGRESO DE LOTE",
                            color = SaaSTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SaaSTextSecondary)
                    }
                }

                Text(
                    text = "Lote: ${lote.numero} (${lote.cantidad.toInt()} uds) — Se restará la cantidad del inventario y se revertirá el costo en la factura.",
                    color = SaaSTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                // Input Motivo
                OutlinedTextField(
                    value = motivo,
                    onValueChange = {
                        motivo = it
                        errorMsg = null
                    },
                    label = { Text("Motivo de la anulación", fontSize = 11.sp) },
                    placeholder = { Text("Ej. Cargado por error en factura equivocada", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it, color = SaaSError, fontSize = 10.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaaSSuccess,
                        unfocusedBorderColor = SaaSBorder,
                        focusedTextColor = SaaSTextPrimary,
                        unfocusedTextColor = SaaSTextPrimary
                    )
                )

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = SaaSTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (motivo.trim().length < 3) {
                                errorMsg = "Escriba un motivo válido (mínimo 3 letras)"
                            } else {
                                onConfirmAnulacion(motivo.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaaSError),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("ANULAR REGISTRO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog Enterprise para Registro de Merma / Daño de Lote.
 */
@Composable
fun RegistrarMermaDialog(
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirmMerma: (cantidad: Double, motivo: String) -> Unit
) {
    var cantidadText by remember { mutableStateOf("1") }
    var motivoSeleccionado by remember { mutableStateOf("FRASCO ROTO / DAÑADO") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val motivosPredefinidos = listOf(
        "FRASCO ROTO / DAÑADO",
        "VENCIMIENTO PREMATURO",
        "ROTURA DE TRANSPORTE",
        "DIFERENCIA DE AUDITORÍA"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SaaSBackground,
            border = BorderStroke(1.dp, SaaSBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFFF59E0B))
                        Text(
                            text = "REGISTRAR MERMA / DAÑO",
                            color = SaaSTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SaaSTextSecondary)
                    }
                }

                Text(
                    text = "Lote: ${lote.numero} (Disponible: ${lote.cantidad.toInt()} uds)",
                    color = SaaSTextSecondary,
                    fontSize = 11.sp
                )

                // Cantidad Mermada
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = {
                        cantidadText = it
                        errorMsg = null
                    },
                    label = { Text("Cantidad de unidades mermadas", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it, color = SaaSError, fontSize = 10.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaaSSuccess,
                        unfocusedBorderColor = SaaSBorder,
                        focusedTextColor = SaaSTextPrimary,
                        unfocusedTextColor = SaaSTextPrimary
                    )
                )

                // Causa / Motivo Chips
                Text("CAUSA DE LA MERMA:", color = SaaSTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    motivosPredefinidos.forEach { causa ->
                        val isSelected = causa == motivoSeleccionado
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                            border = BorderStroke(0.5.dp, if (isSelected) Color(0xFFF59E0B) else SaaSBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { motivoSeleccionado = causa }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = causa,
                                color = if (isSelected) Color(0xFFF59E0B) else SaaSTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = SaaSTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cant = cantidadText.toDoubleOrNull()
                            if (cant == null || cant <= 0) {
                                errorMsg = "Ingrese una cantidad válida mayor a 0"
                            } else if (cant > lote.cantidad) {
                                errorMsg = "No puede mermar más del disponible (${lote.cantidad.toInt()} uds)"
                            } else {
                                onConfirmMerma(cant, motivoSeleccionado)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("REGISTRAR MERMA", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
