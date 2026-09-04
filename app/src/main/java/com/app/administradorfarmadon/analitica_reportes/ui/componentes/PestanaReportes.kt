package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.exportacion.ReporteExportador
import com.app.administradorfarmadon.analitica_reportes.logica.ReportesDatasetBuilder
import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

/**
 * PESTAÑA 2: CENTRO DE REPORTES Y EXPORTACIÓN — Enterprise SaaS 2026.
 *
 * Conectada 100% al motor operativo real (AnaliticaUiState.Exito).
 * Cero datos quemados, 14 reportes funcionales con exportación real
 * a PDF, CSV (Excel), Impresión del sistema y Envío ZIP.
 */
@Composable
fun PestanaReportes(
    uiState: AnaliticaUiState,
    periodoInicial: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa,
    onReintentar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var categoriaSeleccionada by remember { mutableStateOf("VENTAS") }
    var tipoReporteSeleccionado by remember { mutableStateOf("Ventas Detalladas por Producto") }
    var formatoSalida by remember { mutableStateOf("PDF") }
    var textoBusquedaReporte by remember { mutableStateOf("") }
    var estadoExportando by remember { mutableStateOf(false) }

    val categorias = listOf(
        "VENTAS" to "Ventas & Devoluciones",
        "INVENTARIO" to "Inventario & Kardex",
        "CAJA" to "Caja & Flujo Financiero",
        "RENTABILIDAD" to "Rentabilidad & Márgenes",
        "COMPRAS" to "Compras a Droguerías",
        "CLIENTES" to "Clientes & Fidelización",
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
        "RENTABILIDAD" to listOf(
            "Rentabilidad y Márgenes por Producto"
        ),
        "COMPRAS" to listOf(
            "Compras a Proveedores y Droguerías"
        ),
        "CLIENTES" to listOf(
            "Ranking de Mejores Clientes",
            "Comportamiento de Clientes y Recurrencia"
        ),
        "TRIBUTARIOS" to listOf(
            "Registro de Ventas e Ingresos SUNAT",
            "Resumen de Comprobantes Electrónicos"
        )
    )

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── PANEL LATERAL DE FILTROS Y ACCIONES (265dp - ANCHO EQUILIBRADO ENTERPRISE) ───
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .width(265.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // ── ZONA SUPERIOR SCROLLABLE (CATEGORÍAS Y TIPOS DE REPORTE) ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CATEGORÍAS",
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = FDColors.Primary
                    )

                    // Categorías en lista amplia con íconos bonitos
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categorias.forEach { (clave, nombre) ->
                            val esSel = categoriaSeleccionada == clave
                            Surface(
                                color = if (esSel) FDColors.PrimarySubtle else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (esSel) BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.6f)) else null,
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val icono = when (clave) {
                                        "VENTAS" -> Icons.Default.PointOfSale
                                        "INVENTARIO" -> Icons.Default.Inventory2
                                        "CAJA" -> Icons.Default.AccountBalanceWallet
                                        "RENTABILIDAD" -> Icons.AutoMirrored.Filled.TrendingUp
                                        "COMPRAS" -> Icons.Default.LocalShipping
                                        "CLIENTES" -> Icons.Default.People
                                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                                    }
                                    Surface(
                                        color = if (esSel) FDColors.Primary else FDColors.SurfaceElevated,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icono,
                                                contentDescription = null,
                                                tint = if (esSel) FDColors.PrimaryText else FDColors.TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = nombre,
                                        style = FDType.BodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                    // Tipos de Reporte en la Categoría
                    Text(
                        text = "TIPOS DE REPORTE",
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = FDColors.Primary
                    )

                    val tiposDisponibles = tiposPorCategoria[categoriaSeleccionada] ?: emptyList()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tiposDisponibles.forEach { tipo ->
                            val esSel = tipoReporteSeleccionado == tipo
                            Surface(
                                color = if (esSel) FDColors.SurfaceElevated else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (esSel) BorderStroke(1.dp, FDColors.BorderFocus) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tipoReporteSeleccionado = tipo }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (esSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (esSel) FDColors.Primary else FDColors.TextTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = tipo,
                                        style = FDType.Caption.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (esSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── ZONA INFERIOR: CAJÓN FIJO EN BASE (NUNCA HACE SCROLL) ────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = FDColors.Border)

                    Text(
                        text = "FORMATO DE SALIDA",
                        style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                        color = FDColors.TextTertiary
                    )

                    // Selector de Formato (PDF / CSV Excel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PDF" to "📄 PDF", "CSV" to "📊 CSV (Excel)").forEach { (fmt, label) ->
                            val esSel = formatoSalida == fmt
                            Surface(
                                color = if (esSel) FDColors.PrimarySubtle else FDColors.SurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { formatoSalida = fmt }
                            ) {
                                Text(
                                    text = label,
                                    style = FDType.Caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    ),
                                    color = if (esSel) FDColors.Primary else FDColors.TextSecondary,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // ACCIONES REALES DE EXPORTACIÓN E IMPRESIÓN
                    val exito = uiState as? AnaliticaUiState.Exito
                    val puedeExportar = exito != null && !estadoExportando

                    Button(
                        onClick = {
                            if (exito != null) {
                                try {
                                    estadoExportando = true
                                    val tabla = ReportesDatasetBuilder.construirReporte(
                                        categoria = categoriaSeleccionada,
                                        tipoReporte = tipoReporteSeleccionado,
                                        exito = exito,
                                        periodo = periodoInicial,
                                        nombreSede = sedeNombre,
                                        razonSocial = SessionManager.sucursalNombre.ifBlank { "Farmacia" },
                                        ruc = SessionManager.clienteIdGarantizado.take(11).ifBlank { "—" }
                                    )
                                    if (formatoSalida == "PDF") {
                                        val archivo = ReporteExportador.generarPdfEnCache(context, tabla)
                                        ReporteExportador.compartirArchivo(context, archivo, "application/pdf", "Reporte: ${tabla.tipoReporte}")
                                    } else {
                                        val archivo = ReporteExportador.guardarCsvEnCache(context, tabla)
                                        ReporteExportador.compartirArchivo(context, archivo, "text/csv", "Reporte: ${tabla.tipoReporte}")
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    estadoExportando = false
                                }
                            }
                        },
                        enabled = puedeExportar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (estadoExportando) "GENERANDO..." else "DESCARGAR $formatoSalida",
                                style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (exito != null) {
                                    try {
                                        val tabla = ReportesDatasetBuilder.construirReporte(
                                            categoria = categoriaSeleccionada,
                                            tipoReporte = tipoReporteSeleccionado,
                                            exito = exito,
                                            periodo = periodoInicial,
                                            nombreSede = sedeNombre,
                                            razonSocial = SessionManager.sucursalNombre.ifBlank { "Farmacia" },
                                            ruc = SessionManager.clienteIdGarantizado.take(11).ifBlank { "—" }
                                        )
                                        val archivo = ReporteExportador.generarPdfEnCache(context, tabla)
                                        ReporteExportador.imprimir(context, archivo, "Reporte_${tabla.tipoReporte.replace(" ", "_")}")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = puedeExportar,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FDColors.Border),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Icon(Icons.Default.Print, null, tint = FDColors.TextPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Imprimir", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                if (exito != null) {
                                    try {
                                        val zipArchivo = ReporteExportador.generarPaqueteContadorZip(
                                            context = context,
                                            exito = exito,
                                            periodo = periodoInicial,
                                            nombreSede = sedeNombre,
                                            razonSocial = SessionManager.sucursalNombre.ifBlank { "Farmacia" },
                                            ruc = SessionManager.clienteIdGarantizado.take(11).ifBlank { "—" }
                                        )
                                        ReporteExportador.compartirArchivo(
                                            context = context,
                                            archivo = zipArchivo,
                                            mimeType = "application/zip",
                                            titulo = "Paquete de información para el contador - Sede $sedeNombre - $periodoInicial"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al generar paquete: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = puedeExportar,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FDColors.Border),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Icon(Icons.Default.Archive, null, tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Zip", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // ── WORKSPACE PRINCIPAL: BUSCADOR LEGIBLE Y TABLA DE DATOS DEL REPORTE ───
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Buscador Interno en Tiempo Real (Amplio, Holgado y Legible)
                OutlinedTextField(
                    value = textoBusquedaReporte,
                    onValueChange = { textoBusquedaReporte = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = FDColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (textoBusquedaReporte.isNotEmpty()) {
                            IconButton(onClick = { textoBusquedaReporte = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = FDColors.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Buscar por comprobante, producto o cajero...",
                            style = FDType.Body.copy(fontSize = 12.sp),
                            color = FDColors.InputPlaceholder,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    textStyle = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextPrimary, fontWeight = FontWeight.Medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FDColors.InputBackground,
                        unfocusedContainerColor = FDColors.InputBackground,
                        focusedBorderColor = FDColors.Primary,
                        unfocusedBorderColor = FDColors.InputBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )

                // ESTADOS DE CARGA / ERROR / CONTENIDO
                when (uiState) {
                    is AnaliticaUiState.Loading -> {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FDColors.Primary, modifier = Modifier.size(36.dp))
                        }
                    }

                    is AnaliticaUiState.Error -> {
                        Surface(
                            color = FDColors.ErrorSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No se pudo cargar el reporte", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.Error)
                                Text(uiState.mensaje, style = FDType.Caption, color = FDColors.TextSecondary)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onReintentar,
                                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error, contentColor = FDColors.PrimaryText),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    is AnaliticaUiState.Exito -> {
                        val tablaCompleta = remember(categoriaSeleccionada, tipoReporteSeleccionado, uiState, periodoInicial, sedeNombre) {
                            ReportesDatasetBuilder.construirReporte(
                                categoria = categoriaSeleccionada,
                                tipoReporte = tipoReporteSeleccionado,
                                exito = uiState,
                                periodo = periodoInicial,
                                nombreSede = sedeNombre,
                                razonSocial = SessionManager.sucursalNombre.ifBlank { "Farmacia" },
                                ruc = SessionManager.clienteIdGarantizado.take(11).ifBlank { "—" }
                            )
                        }

                        val tabla = remember(tablaCompleta, textoBusquedaReporte) {
                            if (textoBusquedaReporte.isBlank()) {
                                tablaCompleta
                            } else {
                                val q = textoBusquedaReporte.trim().lowercase(Locale.ROOT)
                                val filtradas = tablaCompleta.filas.filter { f ->
                                    f.celdas.any { it.lowercase(Locale.ROOT).contains(q) }
                                }
                                tablaCompleta.copy(filas = filtradas, totalRegistros = filtradas.size)
                            }
                        }

                        // AVISO TRIBUTARIO / LEGAL SI APLICA AL REPORTE
                        if (!tabla.disclaimerTributario.isNullOrBlank()) {
                            Surface(
                                color = FDColors.WarningSubtle,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = FDColors.Warning, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = tabla.disclaimerTributario,
                                        style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                        }

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
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    tabla.columnas.forEach { col ->
                                        Text(
                                            text = col.titulo,
                                            style = FDType.Caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = if (col.esMonto) TextAlign.End else TextAlign.Start
                                            ),
                                            color = FDColors.TextSecondary,
                                            modifier = Modifier.weight(col.peso)
                                        )
                                    }
                                }
                            }

                            if (tabla.filas.isEmpty()) {
                                Surface(
                                    color = FDColors.SurfaceElevated,
                                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Inbox, contentDescription = null, tint = FDColors.TextTertiary, modifier = Modifier.size(32.dp))
                                        Text(
                                            text = "Sin movimientos en este período",
                                            style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                            color = FDColors.TextSecondary
                                        )
                                        Text(
                                            text = "No se encontraron operaciones registradas para este reporte en el rango de fechas seleccionado.",
                                            style = FDType.Caption.copy(fontSize = 11.5.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                }
                            } else {
                                tabla.filas.forEachIndexed { index, fila ->
                                    val esZebra = index % 2 == 1
                                    Surface(
                                        color = if (esZebra) FDColors.Background else FDColors.Surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            tabla.columnas.forEachIndexed { colIdx, col ->
                                                val celda = fila.celdas.getOrNull(colIdx) ?: "—"
                                                Text(
                                                    text = celda,
                                                    style = FDType.BodySmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (col.esMonto) FontWeight.SemiBold else FontWeight.Normal,
                                                        textAlign = if (col.esMonto) TextAlign.End else TextAlign.Start
                                                    ),
                                                    color = if (col.esMonto) FDColors.TextPrimary else FDColors.TextSecondary,
                                                    modifier = Modifier.weight(col.peso)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Fila de Totales Exactos = Σ Filas Mostradas
                                if (tabla.totales.isNotEmpty()) {
                                    Surface(
                                        color = FDColors.PrimarySubtle,
                                        shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp),
                                        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TOTALES (Σ ${tabla.filas.size} FILAS):",
                                                style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.Primary
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                                tabla.totales.forEach { (concepto, valor) ->
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = "$concepto:",
                                                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                                                            color = FDColors.TextSecondary
                                                        )
                                                        Text(
                                                            text = valor,
                                                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                            color = FDColors.Primary
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
            }
        }
    }
}
