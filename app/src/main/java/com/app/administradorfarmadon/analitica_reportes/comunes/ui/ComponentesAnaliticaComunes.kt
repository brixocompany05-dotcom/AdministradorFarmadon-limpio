package com.app.administradorfarmadon.analitica_reportes.comunes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.util.Locale

@Composable
fun MetricKpiSaaS(
    titulo: String,
    valor: String,
    tag: String,
    esPositivo: Boolean = true,
    icono: ImageVector = Icons.Default.TrendingUp,
    s: MedidaAdaptativa? = null,
    modifier: Modifier = Modifier,
    colorValor: Color = FDColors.TextPrimary
) {
    Surface(
        color = if (FDColors.isDark) FDColors.Surface else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = titulo,
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = FDColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = if (esPositivo) FDColors.SuccessSubtle else FDColors.ErrorSubtle,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tag,
                        style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                        color = if (esPositivo) FDColors.Success else FDColors.Error,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = valor,
                style = FDType.Heading2.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterPremium
                ),
                color = colorValor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricMiniSaaS(
    label: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valor, style = FDType.Heading2.copy(fontSize = 14.sp, fontWeight = FontWeight.Black), color = color)
            Text(label, style = FDType.Caption.copy(fontSize = 9.5.sp), color = FDColors.TextSecondary)
        }
    }
}

@Composable
fun EstadoVacioSaaS(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (FDColors.isDark) FDColors.Surface else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = FDColors.TextTertiary, modifier = Modifier.size(32.dp))
            Text(titulo, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = FDColors.TextPrimary)
            Text(subtitulo, style = FDType.Caption.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun DesgloseLineaFinanciera(
    concepto: String,
    monto: String,
    colorMonto: Color,
    esDestacada: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = concepto,
            style = if (esDestacada) FDType.Body.copy(fontWeight = FontWeight.Bold) else FDType.BodySmall,
            color = if (esDestacada) FDColors.TextPrimary else FDColors.TextSecondary
        )
        Text(
            text = monto,
            style = if (esDestacada) FDType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Black) else FDType.Body.copy(fontWeight = FontWeight.SemiBold),
            color = colorMonto
        )
    }
}

@Composable
fun BadgeEstadoVenta(estado: String) {
    val (bg, fg, label) = when (estado) {
        Venta.ESTADO_COMPLETADA -> Triple(FDColors.SuccessSubtle, FDColors.Success, "Cobrada")
        Venta.ESTADO_DEVOLUCION_PARCIAL -> Triple(FDColors.WarningSubtle, FDColors.Warning, "Dev. Parcial")
        Venta.ESTADO_DEVOLUCION_TOTAL -> Triple(FDColors.ErrorSubtle, FDColors.Error, "Devuelta")
        Venta.ESTADO_ANULADA -> Triple(FDColors.ErrorSubtle, FDColors.Error, "Anulada")
        else -> Triple(FDColors.SurfaceElevated, FDColors.TextSecondary, estado)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DialogoDetalleVenta(
    venta: Venta,
    devoluciones: List<DevolucionVenta> = emptyList(),
    onCerrar: () -> Unit
) {
    val fmtFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US) }
    val fechaEmisionTexto = if (venta.fechaHoraMs > 0L) fmtFecha.format(Date(venta.fechaHoraMs)) else venta.diaClave

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. CABECERA PRINCIPAL CON NÚMERO, ESTADO Y CLIENTE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${venta.tipoComprobante}: ${venta.numeroCompleto.ifBlank { venta.id.take(8) }}",
                                style = FDType.Heading1.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                            BadgeEstadoVenta(venta.estado)
                        }
                        val docCliente = if (venta.cliente.numeroDocumento.isNotBlank()) " (${venta.cliente.tipoDocumento}: ${venta.cliente.numeroDocumento})" else ""
                        Text(
                            text = "Cliente: ${venta.cliente.nombre.ifBlank { "Consumidor Final" }}$docCliente • Cajero: ${venta.cajeroNombre.ifBlank { "Cajero" }} • Emitido: $fechaEmisionTexto",
                            style = FDType.Caption.copy(fontSize = 11.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FDColors.TextSecondary)
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2. AUDITORÍA: BANNER VISIBLE SI LA VENTA ESTÁ ANULADA (CON MOTIVO, RESPONSABLE, HORA Y NOTA DE CRÉDITO)
                    if (venta.estado == Venta.ESTADO_ANULADA) {
                        Surface(
                            color = FDColors.Error.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = FDColors.Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "COMPROBANTE ANULADO (SIN EFECTO EN CAJA NI EN STOCK)",
                                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.Error
                                    )
                                }

                                Text(
                                    text = "MOTIVO DE LA ANULACIÓN:",
                                    style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextSecondary
                                )

                                Surface(
                                    color = FDColors.Surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = venta.anulacionMotivo.ifBlank { "Sin motivo detallado registrado en el sistema" },
                                        style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                        color = FDColors.TextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                val fechaAnulacionStr = if (venta.anuladaEnMs > 0L) fmtFecha.format(Date(venta.anuladaEnMs)) else "Hora no registrada"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Anulado por: ${venta.anuladaPorNombre.ifBlank { "Usuario del sistema" }} • $fechaAnulacionStr",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp),
                                        color = FDColors.TextSecondary
                                    )
                                    if (venta.numeroNotaCredito.isNotBlank()) {
                                        Text(
                                            text = "Nota de Crédito SUNAT: ${venta.numeroNotaCredito}",
                                            style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. AUDITORÍA: BANNER SI TIENE DEVOLUCIÓN PARCIAL O TOTAL (CON MOTIVO, NC Y RESPONSABLE)
                    val tieneDevolucion = venta.estado == Venta.ESTADO_DEVOLUCION_PARCIAL ||
                            venta.estado == Venta.ESTADO_DEVOLUCION_TOTAL ||
                            venta.totalDevueltoProrrateado > 0.0 ||
                            venta.totalDevuelto > 0.0 ||
                            venta.devolucionMotivo.isNotBlank()
                    if (tieneDevolucion && venta.estado != Venta.ESTADO_ANULADA) {
                        val devsDeEstaVenta = remember(venta.id, devoluciones) {
                            devoluciones.filter { it.ventaId == venta.id }
                        }
                        val montoDevuelto = if (venta.totalDevueltoProrrateado > 0.0) venta.totalDevueltoProrrateado else venta.totalDevuelto

                        Surface(
                            color = FDColors.Warning.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = FDColors.Warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (venta.estado == Venta.ESTADO_DEVOLUCION_TOTAL) "DEVOLUCIÓN TOTAL REGISTRADA (STOCK Y DINERO RESTITUIDOS)" else "DEVOLUCIÓN PARCIAL REGISTRADA",
                                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.Warning
                                    )
                                }

                                if (devsDeEstaVenta.isNotEmpty()) {
                                    devsDeEstaVenta.forEachIndexed { idx, dev ->
                                        if (idx > 0) HorizontalDivider(color = FDColors.Warning.copy(alpha = 0.25f))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "MOTIVO DE LA DEVOLUCIÓN:",
                                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.TextSecondary
                                            )
                                            Surface(
                                                color = FDColors.Surface,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, FDColors.Warning.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = dev.motivo.ifBlank { "Sin motivo detallado registrado" },
                                                    style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                                    color = FDColors.TextPrimary,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }

                                            val devFechaTexto = if (dev.fechaMs > 0L) fmtFecha.format(Date(dev.fechaMs)) else ""
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Reembolso: -S/ %.2f (%s) • Por: %s • %s".format(
                                                        Locale.US,
                                                        dev.montoReembolso,
                                                        dev.metodoReembolso,
                                                        dev.usuarioNombre.ifBlank { "Cajero" },
                                                        devFechaTexto
                                                    ),
                                                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                                                    color = FDColors.TextSecondary
                                                )
                                                if (dev.numeroCompleto.isNotBlank()) {
                                                    Text(
                                                        text = "Nota de Crédito SUNAT: ${dev.numeroCompleto}",
                                                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.Warning
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Lectura directa de los campos de devolución guardados en la Venta
                                    val motivoDev = venta.devolucionMotivo.ifBlank { "Devolución de productos registrada en el POS" }
                                    Text(
                                        text = "MOTIVO DE LA DEVOLUCIÓN:",
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextSecondary
                                    )
                                    Surface(
                                        color = FDColors.Surface,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, FDColors.Warning.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = motivoDev,
                                            style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                            color = FDColors.TextPrimary,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    val devFechaTexto = if (venta.devolucionEnMs > 0L) fmtFecha.format(Date(venta.devolucionEnMs)) else ""
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reembolso: -S/ %.2f%s • Por: %s • %s".format(
                                                Locale.US,
                                                montoDevuelto,
                                                if (venta.devolucionMetodoReembolso.isNotBlank()) " (${venta.devolucionMetodoReembolso})" else "",
                                                venta.devolucionPorNombre.ifBlank { "Cajero" },
                                                devFechaTexto
                                            ),
                                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                                            color = FDColors.TextSecondary
                                        )
                                        if (venta.devolucionNumeroNotaCredito.isNotBlank()) {
                                            Text(
                                                text = "Nota de Crédito SUNAT: ${venta.devolucionNumeroNotaCredito}",
                                                style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.Warning
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. LÍNEAS Y COSTOS GUARDADOS AL VENDER
                    Text(
                        text = "LÍNEAS DE VENTA Y COSTOS REALES EN LA OPERACIÓN",
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )

                    venta.items.forEach { item ->
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = "${item.cantidad}x ${item.nombreProducto}",
                                            style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                                            color = FDColors.TextPrimary
                                        )
                                        if (item.cantidadDevuelta > 0.0) {
                                            Surface(
                                                color = FDColors.Warning.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Devuelto: ${item.cantidadDevuelta} u",
                                                    style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = FDColors.Warning,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "S/ %.2f".format(Locale.US, item.subtotal),
                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                                        color = if (venta.estado == Venta.ESTADO_ANULADA) FDColors.TextTertiary else FDColors.Primary
                                    )
                                }
                                val costoStr = if (item.costoTotalReal > 0.0) "Costo real de compra de la línea: S/ %.2f".format(Locale.US, item.costoTotalReal) else "Costo histórico no registrado (Venta previa al módulo)"
                                Text(costoStr, style = FDType.Caption, color = FDColors.TextSecondary)

                                if (item.lotesConsumidos.isNotEmpty()) {
                                    Text(
                                        text = "Lotes descontados de inventario:",
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextTertiary
                                    )
                                    item.lotesConsumidos.forEach { lc ->
                                        Text(
                                            text = " • Lote ${lc.loteNumero} | Descontado: ${lc.cantidadFisica} u | Costo unitario real: S/ %.2f".format(Locale.US, lc.costoUnitarioReal),
                                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                                            color = FDColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. MEDIOS DE PAGO REGISTRADOS
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "MEDIOS DE PAGO UTILIZADOS",
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )
                    Surface(
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            venta.pagos.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💳 ${p.nombreMetodo.ifBlank { p.tipoId }}",
                                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = FDColors.TextPrimary
                                    )
                                    Text(
                                        text = "S/ %.2f".format(Locale.US, p.monto),
                                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                            if (venta.vuelto > 0.0 || venta.montoRecibido > 0.0) {
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (venta.montoRecibido > 0.0) {
                                        Text(
                                            text = "Recibido: S/ %.2f".format(Locale.US, venta.montoRecibido),
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextSecondary
                                        )
                                    }
                                    if (venta.vuelto > 0.0) {
                                        Text(
                                            text = "Vuelto entregado: S/ %.2f".format(Locale.US, venta.vuelto),
                                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Warning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                // 6. LIQUIDACIÓN FINANCIERA FINAL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (venta.estado == Venta.ESTADO_ANULADA) {
                            Text(
                                text = "Monto original emitido: S/ %.2f".format(Locale.US, venta.total),
                                style = FDType.Caption.copy(fontWeight = FontWeight.Medium),
                                color = FDColors.TextTertiary
                            )
                            Text(
                                text = "Total neto: S/ 0.00 (Anulada)",
                                style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Error
                            )
                        } else {
                            val costo = venta.costoTotalReal
                            val totalCobrado = venta.total
                            val devuelto = if (venta.totalDevueltoProrrateado > 0.0) venta.totalDevueltoProrrateado else venta.totalDevuelto
                            val netoCobrado = (totalCobrado - devuelto).coerceAtLeast(0.0)

                            if (devuelto > 0.0) {
                                Text(
                                    text = "Original: S/ %.2f • Reembolsos: -S/ %.2f".format(Locale.US, totalCobrado, devuelto),
                                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                                    color = FDColors.TextSecondary
                                )
                            } else if (costo > 0.0) {
                                val utilidad = netoCobrado - costo
                                val margenPct = if (netoCobrado > 0.0) (utilidad / netoCobrado) * 100.0 else 0.0
                                Text(
                                    text = "Costo: S/ %.2f • Utilidad Bruta: S/ %.2f (%.1f%%)".format(Locale.US, costo, utilidad, margenPct),
                                    style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                                    color = if (utilidad >= 0) FDColors.Success else FDColors.Error
                                )
                            }
                            Text(
                                text = "Total Venta: S/ %.2f".format(Locale.US, netoCobrado),
                                style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Primary
                            )
                        }
                    }
                    Button(
                        onClick = onCerrar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.PrimarySubtle,
                            contentColor = FDColors.Primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cerrar Auditoría", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
