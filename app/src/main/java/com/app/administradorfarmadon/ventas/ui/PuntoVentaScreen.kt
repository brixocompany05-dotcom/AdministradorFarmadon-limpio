package com.app.administradorfarmadon.ventas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.ventas.nuevaventa.ui.SubmoduloNuevaVenta
import com.app.administradorfarmadon.ventas.ventasdia.ui.SubmoduloVentasDia
import com.app.administradorfarmadon.ventas.cierrecaja.ui.SubmoduloCierreCaja
import com.app.administradorfarmadon.ventas.devoluciones.ui.SubmoduloDevoluciones

@Composable
fun PuntoVentaScreen() {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    
    var submoduloActivo by remember { mutableStateOf("NUEVA VENTA") }
    var mostrarMenuSubmodulos by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // ── CABECERA ESTRATÉGICA (SELECTOR DE SUBMÓDULO) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box {
                    Surface(
                        onClick = { mostrarMenuSubmodulos = true },
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = submoduloActivo,
                                    style = FDType.Heading3.copy(fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                                    color = FDColors.TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "CAMBIAR HERRAMIENTA",
                                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextTertiary
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = FDColors.TextTertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = mostrarMenuSubmodulos,
                        onDismissRequest = { mostrarMenuSubmodulos = false },
                        modifier = Modifier.background(FDColors.SurfaceElevated).width(240.dp)
                    ) {
                        val opciones = listOf(
                            Triple("NUEVA VENTA", Icons.Default.AddShoppingCart, "Caja rápida para atención"),
                            Triple("VENTAS DEL DÍA", Icons.Default.History, "Historial y reimpresión"),
                            Triple("CIERRE DE CAJA", Icons.Default.AccountBalanceWallet, "Arqueo y final de turno"),
                            Triple("DEVOLUCIONES", Icons.AutoMirrored.Filled.AssignmentReturn, "Anulaciones y cambios")
                        )
                        
                        opciones.forEach { (titulo, icono, desc) ->
                            val isSel = submoduloActivo == titulo
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(icono, null, tint = if (isSel) FDColors.Primary else FDColors.TextSecondary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = titulo,
                                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                                color = if (isSel) FDColors.Primary else FDColors.TextPrimary
                                            )
                                            Text(
                                                text = desc,
                                                style = FDType.Caption,
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    submoduloActivo = titulo
                                    mostrarMenuSubmodulos = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(FDColors.Border.copy(alpha = 0.5f)))

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column {
                        Text("VENTAS TURNO", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Text("$simboloMoneda 1,250.00", style = FDType.Numeric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                    }
                    Column {
                        Text("ARTÍCULOS", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Text("42", style = FDType.Numeric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SessionManager.nombreUsuario.uppercase(),
                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary
                )
                Text(
                    text = "TURNO MAÑANA · SEDE PRINCIPAL",
                    style = FDType.Caption,
                    color = FDColors.TextTertiary
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (submoduloActivo) {
                "NUEVA VENTA" -> SubmoduloNuevaVenta(simboloMoneda)
                "VENTAS DEL DÍA" -> SubmoduloVentasDia(simboloMoneda)
                "CIERRE DE CAJA" -> SubmoduloCierreCaja(simboloMoneda)
                "DEVOLUCIONES" -> SubmoduloDevoluciones(simboloMoneda)
            }
        }
    }
}
