package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryState
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryViewModel

@Composable
fun SeccionLoteYVencimiento(
    state: StockEntryState,
    viewModel: StockEntryViewModel
) {
    var mostrarEscanerCamara by remember { mutableStateOf(false) }

    if (mostrarEscanerCamara) {
        Gs1CameraScannerDialog(
            onCodeScanned = { viewModel.onCodigoGs1Escaneado(it) },
            onDismiss = { mostrarEscanerCamara = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 1. Alerta Sanitaria de Cadena de Frío
        if (state.esRefrigerado) {
            Surface(
                color = FDColors.Primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.AcUnit,
                        contentDescription = "Cadena de frío",
                        tint = FDColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Necesita refrigeración 2° a 8°",
                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary)
                        )
                    }
                }
            }
        }

        // 2. Alerta de Fiscalización Sanitaria / Psicotrópicos
        if (state.esMedicamentoControlado) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Medicamento Controlado",
                        tint = FDColors.Warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Medicamento controlado — ${state.clasificacionControl}",
                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Warning)
                        )
                    }
                }
            }
        }

        // 3. Alerta por vencimiento corto (Vencimiento Corto entre 1 y 6 meses)
        if (state.esVencimientoCorto && !state.esVencidoOInvalido) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = "Alerta vencimiento corto",
                        tint = FDColors.Warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "VENCE EN ${state.mesesParaVencer} MESES — vender primero",
                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Warning)
                        )
                    }
                }
            }
        }

        // 4. Identificación y Vencimiento del Lote
        val loteExistente = state.loteExistenteDetectado

        if (state.camposBloqueadosPorLoteExistente && loteExistente != null) {
            // ── MODO A: REPOSICIÓN A LOTE DESTINO FIJADO (0 INPUTS INNECESARIOS) ──
            Surface(
                color = FDColors.Primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Layers, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Lote: ${loteExistente.numero}",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary, fontFamily = FontFamily.Monospace)
                            )
                        }
                        TextButton(onClick = { viewModel.onDesbloquearCamposLote() }) {
                            Text("Cambiar Lote", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextSecondary))
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val totalLote = loteExistente.cantidad + loteExistente.cantidadBloqueada
                            Text("En este lote", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                            Text(
                                text = "${totalLote.toInt()} ${state.empaque}${if (loteExistente.cantidadBloqueada > 0) " (${loteExistente.cantidadBloqueada.toInt()} en cuarentena)" else ""}",
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Vence", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                            Text(loteExistente.vencimiento, style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary, fontFamily = FontFamily.Monospace))
                        }
                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Proveedor", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                            Text(loteExistente.proveedorNombre.ifBlank { "Almacén General" }, style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            // ── MODO B: ENTRADA NORMAL / LOTE NUEVO (Simetría 2 Columnas de 52dp) ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                EnterpriseTextField(
                    label = "Lote *",
                    placeholder = "",
                    value = state.numeroLote,
                    leadingIcon = Icons.Outlined.Pin,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { mostrarEscanerCamara = true }) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = "Escanear GS1 DataMatrix",
                                tint = FDColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    onValueChange = { viewModel.onLoteChanged(it) }
                )

                MonthYearPickerEnterprise(
                    selectedMonth = state.mesVencimiento,
                    selectedYear = state.anoVencimiento,
                    enabled = state.loteExistenteDetectado == null || state.camposBloqueadosPorLoteExistente,
                    onMonthYearSelected = { mes, ano ->
                        viewModel.onMesVencimientoChanged(mes)
                        viewModel.onAnoVencimientoChanged(ano)
                    },
                    modifier = Modifier.weight(1f)
                )
            }



            // Detección en vivo de Lote Existente
            if (loteExistente != null) {
                val vencNoCoincide = state.tieneVencimientoNoCoincideConLoteExistente
                Surface(
                    color = if (vencNoCoincide) FDColors.Error.copy(alpha = 0.08f) else FDColors.Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (vencNoCoincide) FDColors.Error.copy(alpha = 0.5f) else FDColors.Primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (vencNoCoincide) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                                null,
                                tint = if (vencNoCoincide) FDColors.Error else FDColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (vencNoCoincide) "Fecha distinta al lote guardado (${loteExistente.numero})" else "Lote existente detectado en inventario (${loteExistente.numero})",
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = if (vencNoCoincide) FDColors.Error else FDColors.Primary)
                                )
                                val totalLote = loteExistente.cantidad + loteExistente.cantidadBloqueada
                                Text(
                                    text = "Stock actual: ${totalLote.toInt()} ${state.empaque}${if (loteExistente.cantidadBloqueada > 0) " (${loteExistente.cantidadBloqueada.toInt()} en cuarentena)" else ""} • Vencimiento oficial: ${loteExistente.vencimiento}" +
                                        if (vencNoCoincide) " • Tú ingresas: ${state.fechaVencimiento}" else "",
                                    style = FDType.Caption.copy(color = if (vencNoCoincide) FDColors.Error else FDColors.TextSecondary)
                                )
                                if (vencNoCoincide) {
                                    Text(
                                        text = "Cambia la fecha a ${loteExistente.vencimiento} o toca Agregar a este lote.",
                                        style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.onAplicarDatosLoteExistente(loteExistente) },
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Agregar a este lote", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
