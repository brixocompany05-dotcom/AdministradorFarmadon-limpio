package com.app.administradorfarmadon.analitica_reportes.exportacion

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.app.administradorfarmadon.analitica_reportes.logica.ReporteTabla
import com.app.administradorfarmadon.analitica_reportes.logica.ReportesDatasetBuilder
import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Motor de exportación e impresión nativa de reportes oficiales (Enterprise SaaS 2026).
 *
 * R2/R4: Cero dependencias externas pesadas (como Apache POI de 35MB).
 * - CSV: Estándar RFC 4180 con BOM UTF-8 (\uFEFF) para apertura inmediata en Excel / Sheets con doble clic.
 * - PDF: Generado con Android PdfDocument nativo con membrete oficial, paginación y diseño corporativo.
 * - Impresión: PrintManager nativo vía PrintDocumentAdapter.
 * - Enviar / Compartir: Intent.ACTION_SEND con FileProvider.
 */
object ReporteExportador {

    private const val TAG = "ReporteExportador"

    /**
     * Genera el contenido CSV con BOM UTF-8 compatible nativamente con Microsoft Excel.
     */
    fun generarCsv(tabla: ReporteTabla): String {
        val sb = StringBuilder()
        // BOM UTF-8 para que Excel reconozca tildes, caracteres en español y separación de columnas
        sb.append('\uFEFF')

        // Membrete oficial
        sb.append(escaparCsv(tabla.razonSocial.ifBlank { "RAZON SOCIAL NO CONFIGURADA" })).append("\n")
        sb.append("RUC:,").append(escaparCsv(tabla.ruc.ifBlank { "—" })).append(",SEDE:,").append(escaparCsv(tabla.nombreSede)).append("\n")
        sb.append("REPORTE:,").append(escaparCsv(tabla.tipoReporte)).append("\n")
        sb.append("PERÍODO:,").append(escaparCsv(tabla.periodo)).append(",EMISIÓN (LIMA):,").append(escaparCsv(tabla.fechaGeneracionLima)).append("\n")
        if (!tabla.disclaimerTributario.isNullOrBlank()) {
            sb.append("AVISO TRIBUTARIO:,").append(escaparCsv(tabla.disclaimerTributario)).append("\n")
        }
        sb.append("\n")

        // Encabezados de columnas
        val headers = tabla.columnas.joinToString(",") { escaparCsv(it.titulo) }
        sb.append(headers).append("\n")

        // Filas de datos
        for (fila in tabla.filas) {
            val linea = fila.celdas.joinToString(",") { escaparCsv(it) }
            sb.append(linea).append("\n")
        }

        // Fila de totales
        if (tabla.totales.isNotEmpty()) {
            sb.append("\n")
            sb.append("TOTALES LIQUIDADOS (Σ FILAS):\n")
            for ((k, v) in tabla.totales) {
                sb.append(escaparCsv(k)).append(",").append(escaparCsv(v)).append("\n")
            }
        }

        return sb.toString()
    }

    private fun escaparCsv(valor: String): String {
        val limpio = valor.replace("\"", "\"\"")
        return if (limpio.contains(",") || limpio.contains("\n") || limpio.contains("\"")) {
            "\"$limpio\""
        } else {
            limpio
        }
    }

    /**
     * Guarda el archivo CSV en el cache de la aplicación.
     */
    fun guardarCsvEnCache(context: Context, tabla: ReporteTabla): File {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val nombreLimpio = tabla.tipoReporte.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "_")
            .take(35)
        val fileFinal = File(dir, "reporte_${nombreLimpio}.csv")
        val fileTmp = File(dir, "reporte_${nombreLimpio}.csv.tmp")

        try {
            val csvContent = generarCsv(tabla)
            fileTmp.writeText(csvContent, Charsets.UTF_8)
            if (!fileTmp.exists() || fileTmp.length() == 0L) {
                throw IllegalStateException("El archivo temporal generado está vacío o corrupto")
            }
            if (fileFinal.exists()) fileFinal.delete()
            if (!fileTmp.renameTo(fileFinal)) {
                fileTmp.copyTo(fileFinal, overwrite = true)
                fileTmp.delete()
            }
            return fileFinal
        } catch (e: Exception) {
            if (fileTmp.exists()) fileTmp.delete()
            throw e
        }
    }

    /**
     * Genera un documento PDF paginado nativo con formato horizontal A4.
     */
    fun generarPdfEnCache(context: Context, tabla: ReporteTabla): File {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val nombreLimpio = tabla.tipoReporte.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "_")
            .take(35)
        val fileFinal = File(dir, "reporte_${nombreLimpio}.pdf")
        val fileTmp = File(dir, "reporte_${nombreLimpio}.pdf.tmp")

        val pdfDoc = PdfDocument()

        // Formato A4 apaisado / landscape: 842 x 595 puntos (pt)
        val pageWidth = 842
        val pageHeight = 595
        val marginLeft = 40f
        val marginRight = 40f
        val marginTop = 40f
        val marginBottom = 40f
        val contentWidth = pageWidth - marginLeft - marginRight

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(20, 24, 33)
            textSize = 9f
        }
        val paintTextBold = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(20, 24, 33)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintHeaderTitle = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSubtitle = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
        }
        val paintBgHeader = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val paintBgZebra = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val paintLine = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val paintPrimaryBar = Paint().apply {
            color = Color.rgb(14, 165, 233) // FDColors.Primary
            style = Paint.Style.FILL
        }

        val rowsPerPage = 18
        val totalPages = ((tabla.filas.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

        val totalPesos = tabla.columnas.sumOf { it.peso.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = tabla.columnas.map { (it.peso / totalPesos) * contentWidth }

        for (pagina in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pagina + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = marginTop

            // Barra de acento superior
            canvas.drawRect(marginLeft, currentY, marginLeft + contentWidth, currentY + 3f, paintPrimaryBar)
            currentY += 16f

            // Encabezado institucional
            val nombreFarmacia = tabla.razonSocial.ifBlank { "FARMADON ENTERPRISE" }
            canvas.drawText(nombreFarmacia.uppercase(Locale.US), marginLeft, currentY, paintHeaderTitle)
            val pageStr = "Página ${pagina + 1} de $totalPages"
            canvas.drawText(pageStr, pageWidth - marginRight - paintSubtitle.measureText(pageStr), currentY, paintSubtitle)
            currentY += 13f

            val infoEmpresa = "RUC: ${tabla.ruc.ifBlank { "—" }}  •  Sede: ${tabla.nombreSede}  •  Período: ${tabla.periodo}"
            canvas.drawText(infoEmpresa, marginLeft, currentY, paintSubtitle)
            currentY += 13f

            val repTitle = "REPORTE OFICIAL: ${tabla.tipoReporte.uppercase(Locale.US)}"
            canvas.drawText(repTitle, marginLeft, currentY, paintTextBold)
            val emisionStr = "Emisión Lima: ${tabla.fechaGeneracionLima}"
            canvas.drawText(emisionStr, pageWidth - marginRight - paintSubtitle.measureText(emisionStr), currentY, paintSubtitle)
            currentY += 16f

            // Encabezado de la tabla
            val headerHeight = 22f
            canvas.drawRect(marginLeft, currentY, marginLeft + contentWidth, currentY + headerHeight, paintBgHeader)
            canvas.drawRect(marginLeft, currentY, marginLeft + contentWidth, currentY + headerHeight, paintLine)

            var colX = marginLeft
            for (i in tabla.columnas.indices) {
                val col = tabla.columnas[i]
                val w = colWidths[i]
                val txt = col.titulo
                val textX = if (col.esMonto) {
                    colX + w - paintTextBold.measureText(txt) - 6f
                } else {
                    colX + 6f
                }
                canvas.drawText(txt, textX, currentY + 14f, paintTextBold)
                colX += w
            }
            currentY += headerHeight

            // Filas de datos para esta página
            val startIdx = pagina * rowsPerPage
            val endIdx = (startIdx + rowsPerPage).coerceAtMost(tabla.filas.size)
            val rowHeight = 20f

            for (r in startIdx until endIdx) {
                val fila = tabla.filas[r]
                if (r % 2 == 1) {
                    canvas.drawRect(marginLeft, currentY, marginLeft + contentWidth, currentY + rowHeight, paintBgZebra)
                }
                canvas.drawRect(marginLeft, currentY, marginLeft + contentWidth, currentY + rowHeight, paintLine)

                var x = marginLeft
                for (c in tabla.columnas.indices) {
                    val w = colWidths[c]
                    val celda = fila.celdas.getOrNull(c) ?: ""
                    val isMonto = tabla.columnas[c].esMonto
                    val p = if (isMonto) paintTextBold else paintText
                    val textX = if (isMonto) {
                        x + w - p.measureText(celda) - 6f
                    } else {
                        x + 6f
                    }
                    val visibleText = if (p.measureText(celda) > w - 10f) {
                        celda.take(18) + "…"
                    } else {
                        celda
                    }
                    canvas.drawText(visibleText, textX, currentY + 13.5f, p)
                    x += w
                }
                currentY += rowHeight
            }

            // Si es la última página, pintar totales
            if (pagina == totalPages - 1 && tabla.totales.isNotEmpty()) {
                currentY += 8f
                canvas.drawLine(marginLeft, currentY, marginLeft + contentWidth, currentY, paintLine)
                currentY += 12f
                canvas.drawText("RESUMEN DE TOTALES (Σ FILAS MOSTRADAS):", marginLeft, currentY, paintTextBold)
                currentY += 14f

                var totX = marginLeft
                for ((k, v) in tabla.totales) {
                    val totStr = "$k: $v"
                    canvas.drawText(totStr, totX, currentY, paintTextBold)
                    totX += paintTextBold.measureText(totStr) + 24f
                }
            }

            // Pie de página de validez SaaS
            val footerY = pageHeight - marginBottom + 20f
            canvas.drawLine(marginLeft, footerY - 8f, marginLeft + contentWidth, footerY - 8f, paintLine)
            val legalText = "Documento verificado por Farmadon SaaS • Integridad contable garantizada • Prohibida la alteración de datos"
            canvas.drawText(legalText, marginLeft, footerY, paintSubtitle)

            pdfDoc.finishPage(page)
        }

        try {
            FileOutputStream(fileTmp).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            if (!fileTmp.exists() || fileTmp.length() == 0L) {
                throw IllegalStateException("El PDF temporal generado está vacío o corrupto")
            }
            if (fileFinal.exists()) fileFinal.delete()
            if (!fileTmp.renameTo(fileFinal)) {
                fileTmp.copyTo(fileFinal, overwrite = true)
                fileTmp.delete()
            }
            return fileFinal
        } catch (e: Exception) {
            pdfDoc.close()
            if (fileTmp.exists()) fileTmp.delete()
            throw e
        }
    }

    /**
     * Envía el reporte a la impresora del sistema mediante PrintManager.
     */
    fun imprimir(context: Context, pdfFile: File, nombreTrabajo: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(nombreTrabajo)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    Log.e(TAG, "Error escribiendo documento a imprimir: ${e.message}", e)
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(nombreTrabajo, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Comparte el archivo PDF o CSV generado mediante el intent oficial de Android con FileProvider.
     */
    fun compartirArchivo(context: Context, archivo: File, mimeType: String, titulo: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            archivo
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir Reporte Oficial"))
    }

    /**
     * Genera el Paquete de información para el contador en un archivo ZIP ligero.
     * Incluye 7 datasets clave en CSV con BOM UTF-8 + archivo de texto resumen y disclaimers.
     * Cero afirmaciones de ser libro contable oficial (entrega trazable de operaciones).
     */
    fun generarPaqueteContadorZip(
        context: Context,
        exito: AnaliticaUiState.Exito,
        periodo: String,
        nombreSede: String,
        razonSocial: String,
        ruc: String
    ): File = generarPaqueteContadorZip(File(context.cacheDir, "reportes"), exito, periodo, nombreSede, razonSocial, ruc)

    fun generarPaqueteContadorZip(
        directorioDestino: File,
        exito: AnaliticaUiState.Exito,
        periodo: String,
        nombreSede: String,
        razonSocial: String,
        ruc: String
    ): File {
        directorioDestino.mkdirs()
        val periodoLimpio = periodo.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_")
        val zipFinal = File(directorioDestino, "paquete_contador_${periodoLimpio}.zip")
        val zipTmp = File(directorioDestino, "paquete_contador_${periodoLimpio}.zip.tmp")

        val reportes = listOf(
            "01_ventas_detalladas.csv" to ReportesDatasetBuilder.construirReporte("VENTAS", "Ventas Detalladas por Producto", exito, periodo, nombreSede, razonSocial, ruc),
            "02_resumen_ventas_por_dia.csv" to ReportesDatasetBuilder.construirReporte("VENTAS", "Resumen de Ventas por Día", exito, periodo, nombreSede, razonSocial, ruc),
            "03_anulaciones_y_devoluciones.csv" to ReportesDatasetBuilder.construirReporte("VENTAS", "Reporte de Anulaciones y Devoluciones", exito, periodo, nombreSede, razonSocial, ruc),
            "04_caja_movimientos.csv" to ReportesDatasetBuilder.construirReporte("CAJA", "Movimientos de Ingresos y Egresos", exito, periodo, nombreSede, razonSocial, ruc),
            "05_inventario_valorizado.csv" to ReportesDatasetBuilder.construirReporte("INVENTARIO", "Stock Actual Valorizado", exito, periodo, nombreSede, razonSocial, ruc),
            "06_conciliacion_fiscal_sunat.csv" to ReportesDatasetBuilder.construirReporte("TRIBUTARIOS", "Registro de Ventas e Ingresos SUNAT", exito, periodo, nombreSede, razonSocial, ruc),
            "07_compras_proveedores.csv" to ReportesDatasetBuilder.construirReporte("TRIBUTARIOS", "Compras a Proveedores y Droguerías", exito, periodo, nombreSede, razonSocial, ruc)
        )

        val sbResumen = StringBuilder().apply {
            append("PAQUETE DE INFORMACIÓN OPERATIVA PARA EL CONTADOR\n")
            append("FARMADON ENTERPRISE • BRIXO PRODUCTO\n")
            append("─────────────────────────────────────────────────────────────────\n")
            append("Empresa / Razón Social: ").append(razonSocial).append("\n")
            append("RUC: ").append(ruc).append("\n")
            append("Sede Operativa: ").append(nombreSede).append("\n")
            append("Período Evaluado: ").append(periodo).append("\n")
            append("Fecha Emisión (Lima): ").append(reportes.first().second.fechaGeneracionLima).append("\n\n")
            append("AVISO LEGAL / CONTABLE IMPORTANTE:\n")
            append("Este paquete contiene información de trazabilidad operativa generada por el sistema\n")
            append("de punto de venta (POS), gaveta de caja y almacén de la farmacia.\n")
            append("No constituye libro contable oficial ni reemplaza la propuesta SIRE/RCE de SUNAT.\n")
            append("En compras: Base e IGV son DERIVADOS/ESTIMADOS del importe total registrado.\n")
            append("El profesional contable valida y aplica el crédito fiscal en su software contable (CONCAR/SIRE/PLE).\n\n")
            append("RESUMEN DE OPERACIONES REGISTRADAS:\n")
            append("• Ventas Brutas: S/ %.2f\n".format(Locale.US, exito.metricas.ventasBrutas))
            append("• Devoluciones / Reembolsos: S/ %.2f\n".format(Locale.US, exito.metricas.devoluciones))
            append("• Ventas Netas: S/ %.2f\n".format(Locale.US, exito.metricas.ventasNetas))
            append("• Compras Netas Registradas: S/ %.2f\n".format(Locale.US, exito.compras.comprasNetas))
            append("• Comprobantes Aceptados SUNAT: %d\n".format(exito.
            conciliacionFiscal.aceptados))
            append("• Comprobantes Pendientes SUNAT: %d\n".format(exito.conciliacionFiscal.pendientes))
            append("• Comprobantes Rechazados SUNAT: %d\n".format(exito.conciliacionFiscal.rechazados))
            append("• Diferencia Cuadre Caja: S/ %.2f (%s)\n".format(
                Locale.US,
                exito.conciliacionCaja.diferenciaTotal,
                if (exito.conciliacionCaja.hayDiferencia) "CON DESCUADRE" else "CUADRADO"
            ))
            if (exito.conciliacionCaja.hayMovimientosSinMetodo) {
                append("• ALERTA: Existen cobros en caja sin método especificado.\n")
            }
            if (exito.fuentesFallidas.isNotEmpty()) {
                append("• ESTADO DE CARGA: PARCIAL (Fuentes pendientes: ${exito.fuentesFallidas.keys.joinToString { it.label }})\n")
            }
        }

        val sbIncidencias = StringBuilder().apply {
            append("INCIDENCIAS Y OBSERVACIONES DEL PERÍODO\n")
            append("─────────────────────────────────────────────────────────────────\n")
            var hayIncidencias = false
            if (exito.fuentesFallidas.isNotEmpty()) {
                hayIncidencias = true
                append("⚠️ FUENTES DE DATOS CON INCIDENCIA DE RED:\n")
                for ((fuente, err) in exito.fuentesFallidas) {
                    append("   • ${fuente.label}: $err\n")
                }
            }
            if (exito.conciliacionCaja.hayDiferencia) {
                hayIncidencias = true
                append("⚠️ DESCUADRE EN CAJA: Diferencia total de S/ %.2f entre ventas POS y gaveta.\n".format(Locale.US, exito.conciliacionCaja.diferenciaTotal))
            }
            if (exito.conciliacionCaja.hayMovimientosSinMetodo) {
                hayIncidencias = true
                append("⚠️ INTEGRIDAD CAJA: Existen movimientos sin método de pago registrado.\n")
            }
            if (exito.conciliacionFiscal.pendientes > 0) {
                hayIncidencias = true
                append("⚠️ FACTURACIÓN SUNAT: %d comprobante(s) pendiente(s) de envío a SUNAT (CDR NO DISPONIBLE).\n".format(exito.conciliacionFiscal.pendientes))
            }
            if (exito.conciliacionFiscal.rechazados > 0) {
                hayIncidencias = true
                append("🚨 FACTURACIÓN SUNAT: %d comprobante(s) rechazado(s) por SUNAT.\n".format(exito.conciliacionFiscal.rechazados))
            }
            if (exito.inventario.cantidadQuiebresStock > 0) {
                hayIncidencias = true
                append("ℹ️ INVENTARIO: %d producto(s) en quiebre de stock.\n".format(exito.inventario.cantidadQuiebresStock))
            }
            if (!hayIncidencias) {
                append("Sin incidencias operativas ni descuadres registrados en este período.\n")
            }
        }

        try {
            ZipOutputStream(FileOutputStream(zipTmp)).use { zos ->
                // Archivos CSV
                for ((nombreArchivo, tabla) in reportes) {
                    val csvContent = generarCsv(tabla)
                    val entry = ZipEntry(nombreArchivo)
                    zos.putNextEntry(entry)
                    zos.write(csvContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                // Resumen TXT
                val resumenEntry = ZipEntry("08_resumen_periodo_contador.txt")
                zos.putNextEntry(resumenEntry)
                zos.write(sbResumen.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Incidencias TXT
                val incEntry = ZipEntry("09_incidencias_y_observaciones.txt")
                zos.putNextEntry(incEntry)
                zos.write(sbIncidencias.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            if (!zipTmp.exists() || zipTmp.length() == 0L) {
                throw IllegalStateException("El archivo ZIP temporal generado está vacío o corrupto")
            }
            java.util.zip.ZipFile(zipTmp).use { zf ->
                if (zf.size() < 7) {
                    throw IllegalStateException("El paquete ZIP no contiene los archivos mínimos (encontrados: ${zf.size()})")
                }
            }

            if (zipFinal.exists()) zipFinal.delete()
            if (!zipTmp.renameTo(zipFinal)) {
                zipTmp.copyTo(zipFinal, overwrite = true)
                zipTmp.delete()
            }
            return zipFinal
        } catch (e: Exception) {
            if (zipTmp.exists()) zipTmp.delete()
            throw e
        }
    }

    /**
     * Limpia de raíz todos los archivos temporales de reportes exportados (PDFs, CSVs, ZIPs)
     * para que nada quede en memoria local ni en disco al cerrar sesión o cambiar de farmacia (R1/R7).
     */
    fun limpiarCache(context: Context) {
        try {
            val dir = File(context.cacheDir, "reportes")
            if (dir.exists() && dir.isDirectory) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando caché de reportes: ${e.message}", e)
        }
    }
}
