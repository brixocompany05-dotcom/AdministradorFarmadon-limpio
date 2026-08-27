package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.compras.ui.componentes.DialogoCrearProveedor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor

@Composable
fun ProveedorSelectionDialog(
    proveedores: List<Proveedor>,
    busqueda: String,
    mostrarFormularioNuevo: Boolean,
    onBusquedaChange: (String) -> Unit,
    onSelectProveedor: (Proveedor) -> Unit,
    onToggleNuevoProveedor: (Boolean) -> Unit,
    onGuardarNuevoProveedorConDatos: (
        nombre: String,
        idFiscal: String,
        contacto: String,
        telefono: String,
        email: String,
        direccion: String,
        montoMinimo: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    if (mostrarFormularioNuevo) {
        DialogoCrearProveedor(
            proveedorEditando = null,
            guardando = false,
            onGuardar = { nom, ruc, cont, tel, em, dir, min ->
                onGuardarNuevoProveedorConDatos(nom, ruc, cont, tel, em, dir, min)
            },
            onDismiss = { onToggleNuevoProveedor(false) }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(14.dp),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Business, null, tint = FDColors.Primary, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "SELECCIONAR PROVEEDOR / DROGUERÍA",
                                    style = FDType.Heading2.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                                )
                                Text(
                                    text = "Elige la droguería que emite el comprobante de compra",
                                    style = FDType.Caption.copy(color = FDColors.TextTertiary, fontSize = 11.sp)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = FDColors.TextSecondary)
                        }
                    }

                    HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = onBusquedaChange,
                        placeholder = { Text("Buscar por nombre, RUC o contacto...", style = FDType.Body.copy(color = FDColors.TextTertiary, fontSize = 13.sp)) },
                        textStyle = FDType.Body.copy(color = FDColors.TextPrimary, fontSize = 13.5.sp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FDColors.Background,
                            unfocusedContainerColor = FDColors.Background,
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = FDColors.Border,
                            focusedTextColor = FDColors.TextPrimary,
                            unfocusedTextColor = FDColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(proveedores, key = { it.id }) { prov ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProveedor(prov) },
                                shape = RoundedCornerShape(10.dp),
                                color = FDColors.Background,
                                border = BorderStroke(1.dp, FDColors.Border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Business, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(prov.nombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary, fontSize = 13.5.sp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (prov.idFiscal.isNotBlank()) {
                                                    Text("RUC: ${prov.idFiscal}", style = FDType.Caption.copy(color = FDColors.TextSecondary, fontSize = 11.5.sp))
                                                }
                                                if (prov.telefono.isNotBlank()) {
                                                    Text("• Tel: ${prov.telefono}", style = FDType.Caption.copy(color = FDColors.TextTertiary, fontSize = 11.5.sp))
                                                }
                                                if (prov.montoMinimoPedido > 0) {
                                                    Text("• Mín: $ ${String.format(java.util.Locale.US, "%.2f", prov.montoMinimoPedido)}", style = FDType.Caption.copy(color = FDColors.Primary, fontSize = 11.5.sp))
                                                }
                                            }
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = FDColors.TextTertiary)
                                }
                            }
                        }

                        if (proveedores.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No se encontraron proveedores con esa búsqueda.",
                                        style = FDType.BodySmall.copy(color = FDColors.TextSecondary)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onToggleNuevoProveedor(true) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary.copy(alpha = 0.12f),
                            contentColor = FDColors.Primary
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("+ Registrar nuevo proveedor en esta sucursal", style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                    }
                }
            }
        }
    }
}
