package com.app.administradorfarmadon.analitica_reportes.compras.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

/**
 * Diálogo Modal de Inspección Profunda de Factura de Compra (Enterprise SaaS).
 * Muestra:
 *  1. Cabecera con datos fiscales del comprobante y droguería emisora.
 *  2. Resumen financiero contable (Total, Base imponible, IGV 18%, Abonado y Saldo).
 *  3. Pestaña de Productos (Ítems): nombre, presentación, lote, vencimiento, cantidad y costos.
 *  4. Pestaña de Modalidad de Pago: Cancelación al contado o Historial cronológico de abonos a crédito.
 */
@Composable
fun DialogoDetalleFacturaCompra(
    factura: FacturaCompra,
    onDismiss: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    BackHandler(enabled = true) {
        if (isKeyboardVisible) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        } else {
            onDismiss()
        }
    }

    var tabSeleccionado by remember { mutableIntStateOf(0) } // 0 = Productos, 1 = Pagos y Abonos

    val esContado = factura.esContado
    val esPagada = factura.esTotalmentePagada
    val saldoRestante = factura.saldoPendienteReal

    val colorEstado = when {
        factura.esAnulada -> FDColors.Error
        esPagada -> FDColors.Success
        factura.totalAbonadoReal > 0.01 -> FDColors.Primary
        else -> FDColors.Warning
    }

    val textoEstado = when {
        factura.esAnulada -> "ANULADA"
        esPagada -> if (esContado) "CONTADO · PAGADA" else "CRÉDITO · LIQUIDADA"
        factura.totalAbonadoReal > 0.01 -> "ABONO PARCIAL"
        else -> "PENDIENTE DE PAGO"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AplicarBloqueoTecladoVentana()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FDColors.Background.copy(alpha = 0.65f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(s.padCard),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // ── 1. CABECERA DEL COMPROBANTE ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FDColors.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = FDColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                val tipoDocStr = factura.tipoDoc.ifBlank { "FACTURA" }.uppercase()
                                val numDocStr = factura.numeroFactura.ifBlank {
                                    if (factura.serie.isNotBlank()) "${factura.serie}-${factura.correlativo}" else "Sin N°"
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$tipoDocStr $numDocStr",
                                        style = FDType.Heading1.copy(fontSize = 17.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.TextPrimary
                                    )
                                    Surface(
                                        color = colorEstado.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, colorEstado.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = textoEstado,
                                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = colorEstado,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append(factura.proveedorNombre.ifBlank { "Proveedor Sin Especificar" })
                                        if (factura.rucProveedor.isNotBlank()) append(" · RUC: ${factura.rucProveedor}")
                                    },
                                    style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = FDColors.TextSecondary
                                )
                                val fEmision = factura.fechaEmision.ifBlank { "—" }
                                val fReg = factura.fechaRegistro.ifBlank { "—" }
                                Text(
                                    text = "Emisión Papel: $fEmision · Recepción Almacén: $fReg",
                                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                                    color = FDColors.TextTertiary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = FDColors.TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── 2. RESUMEN FINANCIERO / CONTABLE (KPIs en vivo) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiFacturaMini(
                            titulo = "TOTAL FACTURA",
                            valor = "S/ %.2f".format(Locale.US, factura.totalPapel),
                            color = FDColors.TextPrimary,
                            modifier = Modifier.weight(1.1f)
                        )
                        KpiFacturaMini(
                            titulo = "BASE IMPONIBLE",
                            valor = "S/ %.2f".format(Locale.US, factura.montoBaseCalculado),
                            color = FDColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiFacturaMini(
                            titulo = "I.G.V. (18%)",
                            valor = "S/ %.2f".format(Locale.US, factura.montoIgvCalculado),
                            color = FDColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiFacturaMini(
                            titulo = "MONTO ABONADO",
                            valor = "S/ %.2f".format(Locale.US, factura.totalAbonadoReal),
                            color = if (factura.totalAbonadoReal > 0.01) FDColors.Success else FDColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiFacturaMini(
                            titulo = "SALDO PENDIENTE",
                            valor = "S/ %.2f".format(Locale.US, saldoRestante),
                            color = if (saldoRestante > 0.01) FDColors.Warning else FDColors.Success,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── 3. PESTAÑAS MODERNAS (*UNDERLINE TABS*) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabUnderlineItem(
                            texto = "PRODUCTOS FACTURADOS",
                            conteo = factura.items.size,
                            seleccionado = tabSeleccionado == 0,
                            onClick = { tabSeleccionado = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabUnderlineItem(
                            texto = "MODALIDAD DE PAGO Y ABONOS",
                            conteo = factura.abonos.size,
                            seleccionado = tabSeleccionado == 1,
                            onClick = { tabSeleccionado = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 1.dp)

                    Spacer(Modifier.height(10.dp))

                    // ── 4. CONTENIDO SEGÚN PESTAÑA (SCROLLABLE) ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (tabSeleccionado == 0) {
                            TabProductosFactura(factura)
                        } else {
                            TabPagosYAbonosFactura(factura)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── 5. PIE DEL DIÁLOGO ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FDBotonPrimario(
                            texto = "CERRAR DETALLE",
                            onClick = onDismiss,
                            modifier = Modifier.height(40.dp).width(160.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUBCOMPONENTES: PESTAÑA PRODUCTOS Y PESTAÑA PAGOS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TabProductosFactura(factura: FacturaCompra) {
    if (factura.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No hay líneas de productos registradas para este comprobante.",
                style = FDType.BodySmall,
                color = FDColors.TextTertiary
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Cabecera de la tabla
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRODUCTO / DESCRIPCIÓN", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(2.6f))
                Text("EMPAQUE", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                Text("LOTE / VENC.", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.4f))
                Text("CANTIDAD", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                Text("COSTO UNIT.", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                Text("SUBTOTAL", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
            }
        }

        // Filas de productos
        factura.items.forEachIndexed { index, item ->
            Surface(
                color = if (index % 2 == 0) FDColors.InputBackground.copy(alpha = 0.35f) else FDColors.Surface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2.6f)) {
                        Text(
                            text = item.productoNombre,
                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.bonificacionGratis > 0) {
                            Text(
                                text = "Incluye ${item.bonificacionGratis.toInt()} un. bonificación gratis",
                                style = FDType.Caption.copy(fontSize = 9.5.sp),
                                color = FDColors.Success
                            )
                        }
                    }
                    Text(
                        text = item.empaque.ifBlank { "Unidad" },
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary,
                        modifier = Modifier.weight(1.1f)
                    )
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = "Lote: ${item.loteNumero.ifBlank { "S/L" }}",
                            style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Venc: ${item.vencimiento.ifBlank { "—" }}",
                            style = FDType.Caption.copy(fontSize = 9.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    Text(
                        text = "${item.cantidadComprada.toInt()}",
                        style = FDType.Numeric.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = FDColors.TextPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = "S/ %.2f".format(Locale.US, item.costoUnitario),
                        style = FDType.Numeric.copy(fontSize = 11.5.sp),
                        color = FDColors.TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "S/ %.2f".format(Locale.US, item.costoTotal),
                        style = FDType.Numeric.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Pie de totales de productos
        Surface(
            color = FDColors.Primary.copy(alpha = 0.05f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LÍNEAS FACTURADAS: ${factura.items.size}   ·   TOTAL UNIDADES: ${factura.items.sumOf { it.cantidadTotal }.toInt()}",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.Primary
                )
                Text(
                    text = "SUMA PRODUCTOS: S/ %.2f".format(Locale.US, factura.items.sumOf { it.costoTotal }),
                    style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary
                )
            }
        }
    }
}

@Composable
private fun TabPagosYAbonosFactura(factura: FacturaCompra) {
    val esContado = factura.esContado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta resumen de la condición comercial
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CONDICIÓN DE COMPRA: ${factura.condicionPago.uppercase()}",
                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Black),
                        color = FDColors.TextPrimary
                    )
                    val venceStr = if (factura.fechaVencimientoPago.isNotBlank()) "Vence el: ${factura.fechaVencimientoPago}" else "Sin fecha de vencimiento fija"
                    Text(
                        text = if (esContado) "Pago inmediato cancelado al 100% en recepción de almacén." else venceStr,
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (esContado) "CANCELADO" else "SALDO: S/ %.2f".format(Locale.US, factura.saldoPendienteReal),
                        style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                        color = if (esContado || factura.esTotalmentePagada) FDColors.Success else FDColors.Warning
                    )
                }
            }
        }

        // Caso A: Factura al Contado
        if (esContado) {
            Surface(
                color = FDColors.Success.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FDColors.Success.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = FDColors.Success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Comprobante Cancelado al Contado",
                            style = FDType.Heading2.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Success
                        )
                        val fechaPago = factura.fechaEmision.ifBlank { factura.fechaRegistro }
                        val abonoContado = factura.abonos.firstOrNull()
                        val metodo = abonoContado?.metodoPago?.trim()?.ifBlank { null } ?: "Sin registro del método de pago"
                        val regPor = abonoContado?.usuarioNombre?.trim()?.ifBlank { null }
                            ?: factura.usuarioRegistroEmail.ifBlank { "Sin registro" }
                        Text(
                            text = "Registrado el $fechaPago · Método: $metodo · Asentado por: $regPor",
                            style = FDType.BodySmall.copy(fontSize = 11.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    Text(
                        text = "S/ %.2f".format(Locale.US, factura.totalPapel),
                        style = FDType.Numeric.copy(fontSize = 14.sp, fontWeight = FontWeight.Black),
                        color = FDColors.Success
                    )
                }
            }
        }

        // Caso B: Factura a Crédito — Historial de Abonos
        Text(
            text = "HISTORIAL DE ABONOS REGISTRADOS",
            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        if (factura.abonos.isEmpty() && !esContado) {
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No se han registrado abonos todavía para esta factura.\nEl total de S/ %.2f se encuentra pendiente de liquidación.".format(Locale.US, factura.saldoPendienteReal),
                        style = FDType.BodySmall.copy(textAlign = TextAlign.Center),
                        color = FDColors.TextTertiary
                    )
                }
            }
        } else if (factura.abonos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                factura.abonos.sortedByDescending { it.fechaMs }.forEach { abono ->
                    Surface(
                        color = if (abono.anulado) FDColors.Error.copy(alpha = 0.05f) else FDColors.InputBackground.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, if (abono.anulado) FDColors.Error.copy(alpha = 0.3f) else FDColors.Border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(FDColors.Success.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = FDColors.Success,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        val opStr = if (abono.numeroOperacion.isNotBlank()) " · Op: ${abono.numeroOperacion}" else ""
                                        Text(
                                            text = "${abono.metodoPago.ifBlank { "Abono a Cuenta" }}$opStr",
                                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                            color = if (abono.anulado) FDColors.Error else FDColors.TextPrimary
                                        )
                                        val userStr = if (abono.usuarioNombre.isNotBlank()) " · Registrado por: ${abono.usuarioNombre}" else ""
                                        Text(
                                            text = "${abono.fechaLegible}$userStr",
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                }
                                Text(
                                    text = "S/ %.2f".format(Locale.US, abono.monto),
                                    style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                                    color = if (abono.anulado) FDColors.Error else FDColors.Success
                                )
                            }
                            if (abono.notas.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Nota: ${abono.notas}",
                                    style = FDType.Caption.copy(fontSize = 10.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                            if (abono.pagos.size > 1) {
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(Modifier.height(4.dp))
                                abono.pagos.forEach { p ->
                                    val numOpSub = if (p.numeroOperacion.isNotBlank()) " (Op: ${p.numeroOperacion})" else ""
                                    Text(
                                        text = "• ${p.metodoPago}$numOpSub: S/ %.2f".format(Locale.US, p.monto),
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        color = FDColors.TextSecondary
                                    )
                                }
                            }
                            if (abono.anulado) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "✖ ABONO ANULADO" + (if (abono.motivoAnulacion.isNotBlank()) ": ${abono.motivoAnulacion}" else ""),
                                    style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.Error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Si tiene Notas de Crédito
        if (factura.ajustesFactura.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "NOTAS DE CRÉDITO APLICADAS",
                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                factura.ajustesFactura.forEach { nc ->
                    Surface(
                        color = FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nota de Crédito N° ${nc.numeroDocumento.ifBlank { "S/N" }}",
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextPrimary
                                )
                                Text(
                                    text = "Motivo: ${nc.motivo.ifBlank { "Ajuste documentado" }} · Fecha: ${nc.fechaLegible}",
                                    style = FDType.Caption.copy(fontSize = 10.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                            Text(
                                text = "- S/ %.2f".format(Locale.US, nc.monto),
                                style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.Warning
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES: KPI MINI Y PESTAÑA UNDERLINE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KpiFacturaMini(
    titulo: String,
    valor: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.InputBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.6.dp, FDColors.Border.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = titulo,
                style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextTertiary,
                maxLines = 1
            )
            Text(
                text = valor,
                style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TabUnderlineItem(
    texto: String,
    conteo: Int,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = texto,
                style = FDType.Heading2.copy(
                    fontSize = 12.sp,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (seleccionado) FDColors.Primary else FDColors.TextSecondary
            )
            Surface(
                color = if (seleccionado) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "[ $conteo ]",
                    style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (seleccionado) FDColors.Primary else FDColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.5.dp)
                .fillMaxWidth(if (seleccionado) 0.9f else 0f)
                .background(if (seleccionado) FDColors.Primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
        )
    }
}
