package com.app.administradorfarmadon.ventas.compartido.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Panel derecho de auditoría profunda (40% de ancho) para el turno de caja seleccionado.
 * Cumple R3, R6, R8, R12: desglose veraz de medios de pago, arqueo de gaveta y tickets de vales.
 */
@Composable
fun PanelDetalleTurnoCaja(
    sesion: CajaSesion?,
    movimientos: List<MovimientoCaja>,
    simboloMoneda: String = "S/",
    modifier: Modifier = Modifier
) {
    val tzLima = remember { TimeZone.getTimeZone("America/Lima") }
    val fmtHora = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tzLima } }
    val fmtFechaCorta = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tzLima } }

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        if (sesion != null) {
            val sDet = sesion
            val esAbiertaDet = sDet.estado == CajaSesion.ESTADO_ABIERTA

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Título y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auditoría de Turno",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary
                        )
                        Text(
                            text = sDet.abiertoPorNombre.ifBlank { "Cajero" },
                            style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                    }

                    Surface(
                        color = if (esAbiertaDet) FDColors.SuccessSubtle else FDColors.InputBackground,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (esAbiertaDet) "TURNO EN CURSO" else "TURNO CERRADO",
                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (esAbiertaDet) FDColors.Success else FDColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // 2. Tiempos e Identificación
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "INFORMACIÓN DEL TURNO",
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Apertura:", style = FDType.Caption, color = FDColors.TextSecondary)
                        Text(
                            if (sDet.aperturaMs > 0L) fmtFechaCorta.format(Date(sDet.aperturaMs)) + " " + fmtHora.format(Date(sDet.aperturaMs)) else "--",
                            style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextPrimary
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cierre:", style = FDType.Caption, color = FDColors.TextSecondary)
                        Text(
                            if (sDet.cierreMs > 0L) fmtFechaCorta.format(Date(sDet.cierreMs)) + " " + fmtHora.format(Date(sDet.cierreMs)) else "Turno activo",
                            style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextPrimary
                        )
                    }
                    if (sDet.cerradoPorNombre.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cerrado por:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(sDet.cerradoPorNombre, style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold), color = FDColors.TextPrimary)
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // 3. Cobros por Medios de Pago
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "COBROS POR MEDIO DE PAGO",
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )

                    val metodos = sDet.ventasPorMetodo.ifEmpty { mapOf("EFECTIVO" to sDet.totalVentas) }
                    metodos.forEach { (metodoClave, monto) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = TIPOS_PAGO_FIJOS.find { it.id.equals(metodoClave, ignoreCase = true) }?.nombre ?: metodoClave,
                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                text = "$simboloMoneda %.2f".format(Locale.US, monto),
                                style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                color = FDColors.TextPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL COBRADO", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                        Text("$simboloMoneda %.2f".format(Locale.US, sDet.totalVentas), style = FDType.Label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.Primary)
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // 4. Resultado de Arqueo de Gaveta
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "ARQUEO FÍSICO DE EFECTIVO",
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fondo inicial entregado:", style = FDType.Caption, color = FDColors.TextSecondary)
                        Text("$simboloMoneda %.2f".format(Locale.US, sDet.fondoInicial), style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                    }

                    val ventasEfectivo = sDet.ventasPorMetodo["EFECTIVO"] ?: 0.0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("(+) Ventas cobradas en efectivo:", style = FDType.Caption, color = FDColors.TextSecondary)
                        Text("$simboloMoneda %.2f".format(Locale.US, ventasEfectivo), style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                    }

                    if (sDet.ingresos > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("(+) Ingresos adicionales a caja:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("+ $simboloMoneda %.2f".format(Locale.US, sDet.ingresos), style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.Success)
                        }
                    }

                    if (sDet.retiros > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("(−) Egresos / Gastos de gaveta:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("− $simboloMoneda %.2f".format(Locale.US, sDet.retiros), style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.Error)
                        }
                    }

                    if (sDet.devolucionesEfectivo > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("(−) Devoluciones en efectivo:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text("− $simboloMoneda %.2f".format(Locale.US, sDet.devolucionesEfectivo), style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.Warning)
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.25f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("(=) Efectivo esperado en gaveta:", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                        Text("$simboloMoneda %.2f".format(Locale.US, sDet.efectivoEsperado), style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Efectivo contado en arqueo:", style = FDType.Caption, color = FDColors.TextSecondary)
                        Text(if (esAbiertaDet) "Pendiente cierre" else "$simboloMoneda %.2f".format(Locale.US, sDet.efectivoContado), style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                    }

                    if (!esAbiertaDet) {
                        Surface(
                            color = if (sDet.diferenciaEfectivo < -0.01) FDColors.ErrorSubtle else FDColors.SuccessSubtle,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (sDet.diferenciaEfectivo < -0.01) "RESULTADO: FALTANTE" else if (sDet.diferenciaEfectivo > 0.01) "RESULTADO: SOBRANTE" else "RESULTADO: CUADRADO",
                                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = if (sDet.diferenciaEfectivo < -0.01) FDColors.Error else if (sDet.diferenciaEfectivo > 0.01) FDColors.Primary else FDColors.Success
                                )
                                Text(
                                    text = "$simboloMoneda %.2f".format(Locale.US, sDet.diferenciaEfectivo),
                                    style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                    color = if (sDet.diferenciaEfectivo < -0.01) FDColors.Error else if (sDet.diferenciaEfectivo > 0.01) FDColors.Primary else FDColors.Success
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // 5. SECCIÓN: TICKETS Y VALES INDIVIDUALES DE EGRESO (SALIDAS DE CAJA)
                val movsTurno = remember(sDet.id, movimientos) {
                    movimientos.filter { it.cajaSesionId == sDet.id }
                }
                val egresosTurno = remember(movsTurno) {
                    movsTurno.filter { it.tipo == MovimientoCaja.TIPO_RETIRO }
                }
                val ingresosTurno = remember(movsTurno) {
                    movsTurno.filter { it.tipo == MovimientoCaja.TIPO_INGRESO }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VALES DE SALIDA Y GASTOS (EGRESOS)",
                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextTertiary
                            )
                            if (egresosTurno.isNotEmpty()) {
                                Text(
                                    text = "${egresosTurno.size} ticket(s) de salida registrado(s)",
                                    style = FDType.Caption.copy(fontSize = 9.5.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                        if (sDet.retiros > 0.0) {
                            Text(
                                text = "Total: − $simboloMoneda %.2f".format(Locale.US, sDet.retiros),
                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                color = FDColors.Error
                            )
                        }
                    }

                    if (egresosTurno.isNotEmpty()) {
                        egresosTurno.forEachIndexed { index, mov ->
                            val numTicket = index + 1
                            val horaStr = if (mov.fechaMs > 0L) fmtHora.format(Date(mov.fechaMs)) else "--:--"
                            Surface(
                                color = FDColors.SurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Cabecera del Vale / Ticket
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = FDColors.ErrorSubtle,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "TICKET #$numTicket",
                                                    style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.Error,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = horaStr,
                                                style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium),
                                                color = FDColors.TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "− $simboloMoneda %.2f".format(Locale.US, kotlin.math.abs(mov.monto)),
                                            style = FDType.Label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                            color = FDColors.Error
                                        )
                                    }

                                    // Justificación / Concepto real escrito en mostrador
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "MOTIVO / JUSTIFICACIÓN:",
                                            style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.TextTertiary
                                        )
                                        Text(
                                            text = mov.motivo.ifBlank { "Salida de efectivo registrada en gaveta" },
                                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
                                            color = FDColors.TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.25f))

                                    // Responsable real
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Registrado por: ${mov.usuarioNombre.ifBlank { sDet.abiertoPorNombre }}",
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextSecondary
                                        )
                                        if (mov.autorizadoPorNombre.isNotBlank()) {
                                            Text(
                                                text = "Visto bueno: ${mov.autorizadoPorNombre}",
                                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                color = FDColors.Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (sDet.retiros > 0.0) {
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = FDColors.ErrorSubtle,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "TICKET DE SALIDA",
                                            style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                            color = FDColors.Error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "− $simboloMoneda %.2f".format(Locale.US, sDet.retiros),
                                        style = FDType.Label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                        color = FDColors.Error
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "MOTIVO / JUSTIFICACIÓN:",
                                        style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextTertiary
                                    )
                                    Text(
                                        text = sDet.observaciones.ifBlank { "Salida de efectivo registrada durante el turno" },
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                                        color = FDColors.TextPrimary
                                    )
                                }
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.25f))
                                Text(
                                    text = "Registrado por: ${sDet.cerradoPorNombre.ifBlank { sDet.abiertoPorNombre }}",
                                    style = FDType.Caption.copy(fontSize = 10.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Este turno no registró vales de salida ni gastos imprevistos de caja",
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // 6. Si hubo ingresos de sencillo a gaveta
                    if (ingresosTurno.isNotEmpty()) {
                        ingresosTurno.forEachIndexed { index, mov ->
                            val numIngreso = index + 1
                            val horaStr = if (mov.fechaMs > 0L) fmtHora.format(Date(mov.fechaMs)) else "--:--"
                            Surface(
                                color = FDColors.SurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = FDColors.SuccessSubtle,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "INGRESO #$numIngreso",
                                                    style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.Success,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(horaStr, style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                                        }
                                        Text(
                                            text = "+ $simboloMoneda %.2f".format(Locale.US, kotlin.math.abs(mov.monto)),
                                            style = FDType.Label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                            color = FDColors.Success
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "CONCEPTO:",
                                            style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.TextTertiary
                                        )
                                        Text(
                                            text = mov.motivo.ifBlank { "Ingreso de sencillo adicional a gaveta" },
                                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
                                            color = FDColors.TextPrimary
                                        )
                                    }
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.25f))
                                    Text(
                                        text = "Registrado por: ${mov.usuarioNombre.ifBlank { sDet.abiertoPorNombre }}",
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = FDColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (sDet.observaciones.isNotBlank()) {
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("OBSERVACIONES DEL CAJERO", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Text(sDet.observaciones, style = FDType.BodySmall.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Selecciona un turno para inspeccionar el desglose completo",
                    style = FDType.Caption,
                    color = FDColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
