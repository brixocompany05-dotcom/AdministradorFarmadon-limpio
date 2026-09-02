package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

/**
 * PESTAÑA 2: CENTRO DE REPORTES Y EXPORTACIÓN — Enterprise SaaS 2026.
 *
 * Filtros discretos en panel lateral (260dp) + Tabla de datos estructurada
 * con buscador en tiempo real, sello de documento oficial y exportación.
 */
@Composable
fun PestanaReportes(
    periodoInicial: String,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf("VENTAS") }
    var tipoReporteSeleccionado by remember { mutableStateOf("Ventas Detalladas por Producto") }
    var formatoSalida by remember { mutableStateOf("PDF") }
    var textoBusquedaReporte by remember { mutableStateOf("") }

    val categorias = listOf(
        "VENTAS" to "Ventas & Devoluciones",
        "INVENTARIO" to "Inventario & Kardex",
        "CAJA" to "Caja & Flujo Financiero",
        "TRIBUTARIOS" to "Tributarios SUNAT / DIGEMID"
    )

    val tiposPorCategoria = mapOf(
        "VENTAS" to listOf(
            "Ventas Detalladas por Producto",
            "Resumen de Ventas por Día",
            "Ventas por Vendedor y Turno",
            "Reporte de Anulaciones y Devoluciones"
        ),
        "INVENTARIO" to listOf(
            "Stock Actual Valorizado",
            "Kardex Físico y Valorizado de Productos",
            "Reporte de Lotes y Próximos a Vencer",
            "Productos Sin Movimiento (Lento)"
        ),
        "CAJA" to listOf(
            "Cierres de Caja por Turno",
            "Movimientos de Ingresos y Egresos",
            "Desglose por Métodos de Pago"
        ),
        "TRIBUTARIOS" to listOf(
            "Registro de Ventas e Ingresos SUNAT",
            "Libro de Medicamentos Controlados DIGEMID",
            "Resumen de Comprobantes Electrónicos"
        )
    )

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── PANEL LATERAL DE FILTROS (260dp FIXED - ACCIONABLE Y DISCRETO) ───────────
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SELECCIÓN DE REPORTE",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = FDColors.TextTertiary
                )

                // Categorías Discretas en Lista
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categorias.forEach { (clave, nombre) ->
                        val esSel = categoriaSeleccionada == clave
                        Surface(
                            color = if (esSel) FDColors.SurfaceElevated else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (esSel) BorderStroke(1.dp, FDColors.BorderFocus) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    categoriaSeleccionada = clave
                                    tipoReporteSeleccionado = tiposPorCategoria[clave]?.firstOrNull() ?: ""
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nombre,
                                    style = FDType.Body.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary
                                )
                                if (esSel) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = FDColors.TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                // Selector de Opción Específica
                Text(
                    text = "TIPO DE REPORTE:",
                    style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextTertiary
                )

                var dropdownTipoAbierto by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = FDColors.InputBackground,
                        shape = RoundedCornerShape(s.radiusInput),
                        border = BorderStroke(1.dp, FDColors.InputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownTipoAbierto = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tipoReporteSeleccionado,
                                style = FDType.Body.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterPremium
                                ),
                                color = FDColors.InputText,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = FDColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownTipoAbierto,
                        onDismissRequest = { dropdownTipoAbierto = false },
                        modifier = Modifier.background(FDColors.SurfaceElevated)
                    ) {
                        (tiposPorCategoria[categoriaSeleccionada] ?: emptyList()).forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = t,
                                        style = FDType.Body.copy(fontSize = 12.sp),
                                        color = if (t == tipoReporteSeleccionado) FDColors.TextPrimary else FDColors.TextSecondary,
                                        fontWeight = if (t == tipoReporteSeleccionado) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    tipoReporteSeleccionado = t
                                    dropdownTipoAbierto = false
                                }
                            )
                        }
                    }
                }

                // Selector de Formato (PDF | EXCEL | CSV)
                Text(
                    text = "FORMATO ARCHIVO:",
                    style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextTertiary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("PDF", "EXCEL", "CSV").forEach { fmt ->
                        val esSel = formatoSalida == fmt
                        Surface(
                            color = if (esSel) FDColors.SurfaceElevated else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (esSel) FDColors.BorderFocus else FDColors.Border.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { formatoSalida = fmt }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fmt,
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ÚNICO BOTÓN PRIMARIO DE ACCIÓN CON COLOR ACCENTO PRIMARIO
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusButton),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "DESCARGAR $formatoSalida",
                            style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Acciones Secundarias en Tono Neutro
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Default.Print, null, tint = FDColors.TextPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Imprimir Documento", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                }

                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Default.Email, null, tint = FDColors.TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar al Contador", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
                }
            }
        }

        // ── WORKSPACE PRINCIPAL: TABLA DE DATOS DEL REPORTE + MEMBRETE ─────────────
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Oficial de la Farmacia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "FARMACIA SANTA FE S.A.C.",
                            style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "RUC: 20601234567 • Sede Principal • Código: REP-20260821-0042",
                            style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                            color = FDColors.TextSecondary
                        )
                        Text(
                            text = "Reporte: $tipoReporteSeleccionado • Período: $periodoInicial",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                    }

                    Surface(
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = FDColors.Success, modifier = Modifier.size(14.dp))
                            Text(
                                text = "DOCUMENTO AUTÉNTICO SAAS",
                                style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                // Buscador Interno en Tiempo Real dentro del Reporte
                OutlinedTextField(
                    value = textoBusquedaReporte,
                    onValueChange = { textoBusquedaReporte = it },
                    placeholder = { Text("🔍 Buscar en este reporte por código, producto o categoría...", style = FDType.BodySmall.copy(fontSize = 12.sp), color = FDColors.InputPlaceholder) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FDColors.InputBackground,
                        unfocusedContainerColor = FDColors.InputBackground,
                        focusedBorderColor = FDColors.BorderFocus,
                        unfocusedBorderColor = FDColors.InputBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )

                // TABLA DE DATOS ESTRUCTURADA
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Header Tabla
                    Surface(
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CÓDIGO", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.2f))
                            Text("DESCRIPCIÓN DEL ÍTEM", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(3f))
                            Text("CATEGORÍA", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.8f))
                            Text("ESTADO", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), color = FDColors.TextSecondary, modifier = Modifier.weight(1.2f))
                            Text("CANTIDAD", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), color = FDColors.TextSecondary, modifier = Modifier.weight(1.2f))
                            Text("TOTAL S/", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.4f))
                        }
                    }

                    // Filas Filtrables
                    val filasMuestra = listOf(
                        ReporteFilaCompleta("PAR-500", "Paracetamol 500mg Tabletas x 100", "Analgésicos", "ACEPTADO", "1,240 u", "S/ 3,720.00"),
                        ReporteFilaCompleta("AMX-500", "Amoxicilina 500mg Cápsulas x 100", "Antibióticos", "ACEPTADO", "820 u", "S/ 6,560.00"),
                        ReporteFilaCompleta("IBU-400", "Ibuprofeno 400mg Tabletas x 100", "Antiinflamatorios", "ACEPTADO", "650 u", "S/ 2,275.00"),
                        ReporteFilaCompleta("VIT-1000", "Vitamina C 1000mg Tab efervescente", "Vitaminas", "ACEPTADO", "410 u", "S/ 8,200.00"),
                        ReporteFilaCompleta("SUT-005", "Jeringa 5ml con aguja 21G x 100", "Material Médico", "ACEPTADO", "1,850 u", "S/ 1,850.00")
                    ).filter {
                        textoBusquedaReporte.isBlank() ||
                            it.nombre.contains(textoBusquedaReporte, ignoreCase = true) ||
                            it.codigo.contains(textoBusquedaReporte, ignoreCase = true) ||
                            it.categoria.contains(textoBusquedaReporte, ignoreCase = true)
                    }

                    filasMuestra.forEach { f ->
                        Surface(
                            color = FDColors.Surface,
                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(f.codigo, style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary, modifier = Modifier.weight(1.2f))
                                Text(f.nombre, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold), color = FDColors.TextPrimary, modifier = Modifier.weight(3f))
                                Text(f.categoria, style = FDType.BodySmall.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(1.8f))
                                Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                                    Surface(color = FDColors.SuccessSubtle, shape = RoundedCornerShape(50)) {
                                        Text("● Aceptado", style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Success, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(f.cantidad, style = FDType.Body.copy(fontSize = 12.sp, textAlign = TextAlign.Center), color = FDColors.TextPrimary, modifier = Modifier.weight(1.2f))
                                Text(f.total, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextPrimary, modifier = Modifier.weight(1.4f))
                            }
                        }
                    }

                    // Sticky Total Row
                    Surface(
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL GENERAL PERÍODO", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary, modifier = Modifier.weight(7.2f))
                            Text("4,970 u", style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), color = FDColors.TextPrimary, modifier = Modifier.weight(1.2f))
                            Text("S/ 22,605.00", style = FDType.Heading2.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End), color = FDColors.TextPrimary, modifier = Modifier.weight(1.4f))
                        }
                    }
                }
            }
        }
    }
}

private data class ReporteFilaCompleta(
    val codigo: String,
    val nombre: String,
    val categoria: String,
    val estado: String,
    val cantidad: String,
    val total: String
)
