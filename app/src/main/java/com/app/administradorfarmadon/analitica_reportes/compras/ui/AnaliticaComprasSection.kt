package com.app.administradorfarmadon.analitica_reportes.compras.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.EstadoVacioSaaS
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.MetricKpiSaaS
import com.app.administradorfarmadon.analitica_reportes.logica.ComprasAnalytics
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: COMPRAS (FACTURAS, PROVEEDORES Y MODALIDAD CONTADO/CRÉDITO)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaComprasSection(
    compras: ComprasAnalytics,
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa
) {
    val comprasEfectivas = compras

    // Estados de navegación interactiva: proveedor seleccionado y factura en detalle
    var proveedorSeleccionado by remember { mutableStateOf<String?>(null) }
    var facturaIdSeleccionadaParaDetalle by remember { mutableStateOf<String?>(null) }
    var facturaRespaldoParaDetalle by remember { mutableStateOf<FacturaCompra?>(null) }

    // Si la lista de facturas se actualiza en tiempo real, el detalle abierto obtiene automáticamente
    // la versión más fresca del documento (R8 Verdad Vigente).
    val facturaEnDetalle = remember(comprasEfectivas.facturas, facturaIdSeleccionadaParaDetalle, facturaRespaldoParaDetalle) {
        if (facturaIdSeleccionadaParaDetalle == null) null
        else comprasEfectivas.facturas.find { it.id == facturaIdSeleccionadaParaDetalle } ?: facturaRespaldoParaDetalle
    }

    val facturasFiltradas = remember(comprasEfectivas.facturas, proveedorSeleccionado) {
        if (proveedorSeleccionado.isNullOrBlank()) {
            comprasEfectivas.facturas
        } else {
            comprasEfectivas.facturas.filter {
                it.proveedorNombre.trim().equals(proveedorSeleccionado!!.trim(), ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // Fila de KPIs Principales de Compras
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricKpiSaaS(
                titulo = "TOTAL COMPRADO",
                valor = "S/ %.2f".format(Locale.US, comprasEfectivas.comprasNetas),
                tag = "Mercadería Facturada",
                modifier = Modifier.weight(1.1f)
            )
            MetricKpiSaaS(
                titulo = "PAGADO REAL",
                valor = "S/ %.2f".format(Locale.US, comprasEfectivas.totalPagadoProveedores),
                tag = "Contado + Abonos",
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "DEUDA PENDIENTE",
                valor = "S/ %.2f".format(Locale.US, comprasEfectivas.deudaPendienteProveedores),
                tag = "Saldo por Liquidar",
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "COMPROBANTES",
                valor = "${comprasEfectivas.cantidadFacturasCompra}",
                tag = "Facturas y Boletas",
                modifier = Modifier.weight(0.9f)
            )
        }

        // Barra informativa de condición comercial pactada vs saldo pendiente
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.6.dp, FDColors.Border.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Condición pactada: Contado S/ %.2f  ·  Crédito a plazos S/ %.2f".format(
                        Locale.US, comprasEfectivas.comprasContado, comprasEfectivas.comprasCredito
                    ),
                    style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = FDColors.TextSecondary
                )
                Text(
                    text = "Total Pagado: S/ %.2f  ·  Deuda Pendiente: S/ %.2f".format(
                        Locale.US, comprasEfectivas.totalPagadoProveedores, comprasEfectivas.deudaPendienteProveedores
                    ),
                    style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = if (comprasEfectivas.deudaPendienteProveedores > 0.01) FDColors.Warning else FDColors.Success
                )
            }
        }

        // Panel dividido Enterprise SaaS: Panel Izquierdo (Proveedores) / Panel Derecho (Facturas)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── PANEL IZQUIERDO: PROVEEDORES A LOS QUE COMPRAMOS (38%) ──
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.weight(0.38f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Proveedores",
                                style = FDType.Heading2.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                        }
                        Text(
                            "[ ${comprasEfectivas.comprasPorProveedor.size} ]",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary
                        )
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tarjeta para "TODOS LOS PROVEEDORES" (Desfiltrar)
                        val todosSeleccionado = proveedorSeleccionado == null
                        Surface(
                            color = if (todosSeleccionado) FDColors.Primary.copy(alpha = 0.12f) else FDColors.InputBackground.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                if (todosSeleccionado) 1.5.dp else 0.5.dp,
                                if (todosSeleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { proveedorSeleccionado = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TODOS LOS PROVEEDORES",
                                        style = FDType.BodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (todosSeleccionado) FontWeight.Black else FontWeight.Bold
                                        ),
                                        color = if (todosSeleccionado) FDColors.Primary else FDColors.TextPrimary
                                    )
                                    Text(
                                        text = "${comprasEfectivas.facturas.size} facturas emitidas",
                                        style = FDType.Caption.copy(fontSize = 9.5.sp),
                                        color = FDColors.TextSecondary
                                    )
                                }
                                Text(
                                    text = "S/ %.2f".format(Locale.US, comprasEfectivas.comprasNetas),
                                    style = FDType.BodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (todosSeleccionado) FDColors.Primary else FDColors.TextPrimary
                                )
                            }
                        }

                        if (comprasEfectivas.comprasPorProveedor.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                                Text("Sin proveedores registrados", style = FDType.Caption, color = FDColors.TextTertiary)
                            }
                        } else {
                            comprasEfectivas.comprasPorProveedor.toList().sortedByDescending { it.second }.forEach { (prov, monto) ->
                                val estaSeleccionado = proveedorSeleccionado?.trim().equals(prov.trim(), ignoreCase = true)
                                val cantFacturasProv = comprasEfectivas.facturas.count {
                                    it.proveedorNombre.trim().equals(prov.trim(), ignoreCase = true)
                                }

                                Surface(
                                    color = if (estaSeleccionado) FDColors.Primary.copy(alpha = 0.12f) else FDColors.InputBackground.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        if (estaSeleccionado) 1.5.dp else 0.5.dp,
                                        if (estaSeleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            proveedorSeleccionado = if (estaSeleccionado) null else prov
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (estaSeleccionado) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = FDColors.Primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Text(
                                                    text = prov,
                                                    style = FDType.BodySmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (estaSeleccionado) FDColors.Primary else FDColors.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "$cantFacturasProv ${if (cantFacturasProv == 1) "comprobante" else "comprobantes"}",
                                                style = FDType.Caption.copy(fontSize = 9.5.sp),
                                                color = FDColors.TextSecondary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "S/ %.2f".format(Locale.US, monto),
                                            style = FDType.BodySmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (estaSeleccionado) FDColors.Primary else FDColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── PANEL DERECHO: FACTURAS DEL PROVEEDOR / PERÍODO (62%) ──
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.weight(0.62f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val tituloPanel = if (proveedorSeleccionado != null) {
                                "Facturas: ${proveedorSeleccionado!!}"
                            } else {
                                "Todas las Facturas del Período"
                            }
                            Text(
                                text = tituloPanel,
                                style = FDType.Heading2.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val totalFiltrado = facturasFiltradas.sumOf { it.totalEfectivo }
                            Text(
                                text = "Total en este filtro: S/ %.2f".format(Locale.US, totalFiltrado),
                                style = FDType.Caption.copy(fontSize = 10.sp),
                                color = FDColors.TextSecondary
                            )
                        }
                        Text(
                            "[ ${facturasFiltradas.size} ]",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary
                        )
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                    if (facturasFiltradas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EstadoVacioSaaS(
                                titulo = if (proveedorSeleccionado != null) "Sin facturas para este proveedor" else "Sin facturas de compra",
                                subtitulo = if (proveedorSeleccionado != null)
                                    "No se encontraron facturas emitidas por $proveedorSeleccionado en este período."
                                else "No hay facturas ni comprobantes registrados en el período seleccionado."
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            facturasFiltradas.forEach { f ->
                                val esContado = f.esContado
                                val esPagada = f.esTotalmentePagada
                                val colorEstadoBadge = when {
                                    f.esAnulada -> FDColors.Error
                                    esPagada -> FDColors.Success
                                    f.totalAbonadoReal > 0.01 -> FDColors.Primary
                                    else -> FDColors.Warning
                                }
                                val textoEstadoBadge = when {
                                    f.esAnulada -> "ANULADA"
                                    esPagada -> "PAGADA"
                                    f.totalAbonadoReal > 0.01 -> "ABONADA PARCIAL"
                                    else -> "PENDIENTE"
                                }

                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.6.dp, FDColors.Border.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            facturaIdSeleccionadaParaDetalle = f.id
                                            facturaRespaldoParaDetalle = f
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val tipoDocStr = f.tipoDoc.ifBlank { "FACTURA" }.uppercase()
                                                val numDoc = f.numeroFactura.ifBlank {
                                                    if (f.serie.isNotBlank()) "${f.serie}-${f.correlativo}" else "Sin número"
                                                }
                                                Text(
                                                    text = "$tipoDocStr $numDoc",
                                                    style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextPrimary
                                                )

                                                // Badge Condición (Contado / Crédito)
                                                Surface(
                                                    color = if (esContado) FDColors.Success.copy(alpha = 0.1f) else FDColors.Primary.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.8.dp, if (esContado) FDColors.Success.copy(alpha = 0.3f) else FDColors.Primary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = if (esContado) "CONTADO" else "CRÉDITO",
                                                        style = FDType.Label.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                                        color = if (esContado) FDColors.Success else FDColors.Primary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }

                                                // Badge Estado de Pago
                                                Surface(
                                                    color = colorEstadoBadge.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.8.dp, colorEstadoBadge.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = textoEstadoBadge,
                                                        style = FDType.Label.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                                        color = colorEstadoBadge,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(3.dp))

                                            Text(
                                                text = f.proveedorNombre.ifBlank { "Proveedor no especificado" },
                                                style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                                color = FDColors.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            val fechaEmision = f.fechaEmision.ifBlank { f.fechaRegistro.ifBlank { "Fecha s/r" } }
                                            val itemsCount = f.items.size
                                            Text(
                                                text = "Emisión: $fechaEmision · $itemsCount ${if (itemsCount == 1) "producto" else "productos"} facturados",
                                                style = FDType.Caption.copy(fontSize = 10.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        // Total y Saldo a la derecha con cursor de acción
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "S/ %.2f".format(Locale.US, f.totalPapel),
                                                    style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextPrimary
                                                )
                                                if (!esContado && !esPagada && f.saldoPendienteReal > 0.01) {
                                                    Text(
                                                        text = "Saldo: S/ %.2f".format(Locale.US, f.saldoPendienteReal),
                                                        style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.Warning
                                                    )
                                                } else if (f.totalAbonadoReal > 0.01) {
                                                    Text(
                                                        text = "Abonado: S/ %.2f".format(Locale.US, f.totalAbonadoReal),
                                                        style = FDType.Caption.copy(fontSize = 9.5.sp),
                                                        color = FDColors.Success
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Ver detalle",
                                                tint = FDColors.TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Inspección Profunda de Factura (En Vivo)
    if (facturaEnDetalle != null) {
        DialogoDetalleFacturaCompra(
            factura = facturaEnDetalle,
            onDismiss = {
                facturaIdSeleccionadaParaDetalle = null
                facturaRespaldoParaDetalle = null
            }
        )
    }
}

