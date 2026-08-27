package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryState
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryViewModel
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.TipoCondicionPago

/**
 * Sección de Factura Comercial, Distribuidor y Condiciones de Pago.
 * Simetría y alineación estricta de 52dp en todos los controles.
 */
@Composable
fun SeccionComprobanteYProveedor(
    state: StockEntryState,
    viewModel: StockEntryViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 1. N° de Factura y Selector de Distribuidor (Ambos a 52dp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnterpriseTextField(
                label = if (state.esMedicamentoControlado) "Factura *" else "Factura",
                placeholder = "",
                value = state.numeroFactura,
                leadingIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                enabled = true,
                modifier = Modifier.weight(1f),
                onValueChange = { viewModel.onFacturaChanged(it) }
            )

            val esProveedorFijoPorFactura = state.facturaDetectadaInfo != null

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.esMedicamentoControlado) "Proveedor *" else "Proveedor",
                        style = FDType.Label.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!esProveedorFijoPorFactura) FDColors.TextSecondary else FDColors.TextTertiary
                        )
                    )
                    if (esProveedorFijoPorFactura) {
                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Asignado por factura",
                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Primary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(enabled = !esProveedorFijoPorFactura) { viewModel.onAbrirDialogoProveedores() },
                    shape = RoundedCornerShape(8.dp),
                    color = if (!esProveedorFijoPorFactura) FDColors.SurfaceElevated else FDColors.Background.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        when {
                            state.tieneInconsistenciaFacturaSinProveedor -> FDColors.Warning
                            !esProveedorFijoPorFactura -> FDColors.BorderStrong
                            else -> FDColors.Border.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Business,
                                null,
                                tint = if (state.proveedorNombre.isNotBlank()) FDColors.Primary else FDColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = state.proveedorNombre.ifBlank { "Elegir proveedor..." },
                                    style = FDType.Body.copy(
                                        color = if (state.proveedorNombre.isNotBlank()) FDColors.TextPrimary else FDColors.TextTertiary,
                                        fontWeight = if (state.proveedorNombre.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.rucProveedorSeleccionado.isNotBlank()) {
                                    Text(
                                        text = "RUC: ${state.rucProveedorSeleccionado}",
                                        style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                                    )
                                }
                            }
                        }

                        if (state.proveedorNombre.isNotBlank() && !esProveedorFijoPorFactura) {
                            IconButton(
                                onClick = { viewModel.onLimpiarProveedor() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = FDColors.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        } else if (!esProveedorFijoPorFactura) {
                            Icon(Icons.Default.Search, null, tint = FDColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (state.tieneInconsistenciaFacturaSinProveedor) {
                    Text(
                        "Elige el proveedor de ${state.numeroFactura} para continuar",
                        style = FDType.Caption.copy(color = FDColors.Warning, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
                // Prevención para controlados: avisa antes de Guardar, no después
                if (state.esMedicamentoControlado && (state.numeroFactura.isBlank() || state.proveedorNombre.isBlank())) {
                    Text(
                        "Medicamento controlado — factura y proveedor obligatorios",
                        style = FDType.Caption.copy(color = FDColors.Warning, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }

        // 2. Sugerencia en tiempo real desde Firebase — no local, no maquillaje. Se oculta si escribes diferente, vuelve si borras o coincide parcial
        if (state.mostrarSugerenciaUltimaFactura && state.ultimaFacturaSugerida != null) {
            val sugerida = state.ultimaFacturaSugerida
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.BorderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.EventRepeat, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "¿Repetir ${sugerida.numeroFactura} de ${sugerida.proveedorNombre}?",
                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                        )
                        Text(
                            "Última factura de esta farmacia (en vivo)",
                            style = FDType.Caption.copy(color = FDColors.TextSecondary)
                        )
                    }
                    Button(
                        onClick = { viewModel.onUsarSugerenciaUltimaFactura() },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Usar", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 2b. Factura existente detectada — requiere Aplicar explícito, no auto-rellena (bloquea colisión)
        val facturaDetectada = state.facturaDetectadaInfo
        if (facturaDetectada != null && state.numeroFactura.isNotBlank()) {
            val yaAplicada = facturaDetectada.proveedorNombre == state.proveedorNombre && facturaDetectada.montoTotal.toString() == state.montoTotalFactura
            if (!yaAplicada) {
                Surface(
                    color = FDColors.Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Factura ya registrada con ${facturaDetectada.proveedorNombre}", style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                            Text("Monto ${String.format("%.2f", facturaDetectada.montoTotal)} • ${facturaDetectada.proveedorNombre}", style = FDType.Caption.copy(color = FDColors.TextSecondary))
                        }
                        Button(onClick = { viewModel.onAplicarFacturaExistente() }, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText), modifier = Modifier.height(36.dp)) {
                            Text("Aplicar", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        if (state.numeroFactura.isNotBlank() && state.proveedorNombre.isNotBlank() && facturaDetectada == null) {
            EnterpriseTextField(
                label = "MONTO TOTAL DE LA FACTURA EN PAPEL (\$) (TOPE OFICIAL)",
                placeholder = "",
                value = state.montoTotalFactura,
                leadingIcon = Icons.Outlined.Paid,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { viewModel.onMontoTotalFacturaChanged(it) }
            )
            if (state.faltaMontoTotalEnFacturaNueva) {
                Text(
                    "Escribe el total que dice el papel (ej: 1200) para no superar el tope",
                    style = FDType.Caption.copy(color = FDColors.Warning, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // 3. Condición Comercial de Pago (Chips con altura 44dp)
        if (state.numeroFactura.isNotBlank() && state.proveedorNombre.isNotBlank() && facturaDetectada == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONDICIÓN DE PAGO (CUENTAS POR PAGAR)",
                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
                    )
                }

                // Fluido: 2 opciones — Contado o Crédito con días editables
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CondicionPagoChip(
                        titulo = "Contado",
                        seleccionado = state.tipoCondicionPago == TipoCondicionPago.CONTADO,
                        onClick = { viewModel.onTipoCondicionPagoChanged(TipoCondicionPago.CONTADO) },
                        modifier = Modifier.weight(1f)
                    )
                    CondicionPagoChip(
                        titulo = "Crédito",
                        seleccionado = state.tipoCondicionPago != TipoCondicionPago.CONTADO,
                        onClick = { viewModel.onTipoCondicionPagoChanged(TipoCondicionPago.CREDITO_PERSONALIZADO) },
                        modifier = Modifier.weight(1f)
                    )
                    if (state.tipoCondicionPago != TipoCondicionPago.CONTADO) {
                        EnterpriseTextField(
                            label = "Días",
                            placeholder = "",
                            value = if (state.tipoCondicionPago == TipoCondicionPago.CREDITO_PERSONALIZADO) state.diasCreditoPersonalizado else when(state.tipoCondicionPago) {
                                TipoCondicionPago.CREDITO_15 -> "15"
                                TipoCondicionPago.CREDITO_30 -> "30"
                                TipoCondicionPago.CREDITO_45 -> "45"
                                TipoCondicionPago.CREDITO_60 -> "60"
                                else -> state.diasCreditoPersonalizado
                            },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(0.7f),
                            onValueChange = {
                                viewModel.onDiasCreditoPersonalizadoChanged(it)
                                if (state.tipoCondicionPago != TipoCondicionPago.CREDITO_PERSONALIZADO) viewModel.onTipoCondicionPagoChanged(TipoCondicionPago.CREDITO_PERSONALIZADO)
                            }
                        )
                    }
                }

                if (state.tipoCondicionPago != TipoCondicionPago.CONTADO) {
                    Surface(
                        color = if (state.fechaVencimientoPagoCalculada.isNotBlank()) FDColors.Primary.copy(alpha = 0.08f)
                        else FDColors.Error.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Event, null, tint = if (state.fechaVencimientoPagoCalculada.isNotBlank()) FDColors.Primary else FDColors.Error, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (state.fechaVencimientoPagoCalculada.isNotBlank())
                                    "Vencimiento programado: ${state.fechaVencimientoPagoCalculada}"
                                else "Escribe los días de crédito para fijar la fecha real de pago",
                                style = FDType.Caption.copy(
                                    color = if (state.fechaVencimientoPagoCalculada.isNotBlank()) FDColors.Primary else FDColors.Error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
