package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.core.content.FileProvider
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * Generador matemático de código de barras estándar Code 128 (Subconjunto B).
 * 100% nativo, cero librerías pesadas externas, compatible con el 100% de lectores láser y cámaras.
 */
object Code128BarcodeGenerator {

    // Patrones binarios Code 128 (107 patrones estándar)
    private val PATTERNS = arrayOf(
        "11011001100", "11001101100", "11001100110", "10010011000", "10010001100", // 0-4
        "10001001100", "10011001000", "10011000100", "10001100100", "11001001000", // 5-9
        "11001000100", "11000100100", "10110011100", "10011011100", "10011001110", // 10-14
        "10111001100", "10011101100", "10011100110", "11001110010", "11001011100", // 15-19
        "11001001110", "11011100100", "11001110100", "11101101110", "11101001100", // 20-24
        "11100101100", "11100100110", "11101100100", "11100110100", "11100110010", // 25-29
        "11011011000", "11011000110", "11000110110", "10100011000", "10001011000", // 30-34
        "10001000110", "10110001000", "10001101000", "10001100010", "11010001000", // 35-39
        "11000101000", "11000100010", "10110111000", "10110001110", "10001101110", // 40-44
        "10111011000", "10111000110", "10001110110", "11101110110", "11010001110", // 45-49
        "11000101110", "11011101000", "11011100010", "11011101110", "11101011000", // 50-54
        "11101000110", "11100010110", "11101101000", "11101100010", "11100011010", // 55-59
        "11101111010", "11001000010", "11110001010", "10100110000", "10100001100", // 60-64
        "10010110000", "10010000110", "10000101100", "10000100110", "10110010000", // 65-69
        "10110000100", "10011010000", "10011000010", "10000110100", "10000110010", // 70-74
        "11000010010", "11001010000", "11110111010", "11000010100", "10001111010", // 75-79
        "10100111100", "10010111100", "10010011110", "10111100100", "10011110100", // 80-84
        "10011110010", "11110100100", "11110010100", "11110010010", "11011011110", // 85-89
        "11011110110", "11110110110", "10101111000", "10100011110", "10001011110", // 90-94
        "10111101000", "10111100010", "11110101000", "11110100010", "10111011110", // 95-99
        "10111101110", "11101011110", "11110101110", "11010000100", "11010010000", // 100-104 (104 = Start B)
        "11010011100", "1100011101011" // 105 = Start C, 106 = Stop
    )

    private const val START_B_INDEX = 104
    private const val STOP_INDEX = 106

    /**
     * Codifica una cadena de texto en una secuencia binaria de barras y espacios (1s y 0s).
     * Aplica sanitización estricta para garantizar que lectores láser nunca fallen.
     */
    fun codificarCode128B(texto: String): String {
        val input = texto.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (input.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append(PATTERNS[START_B_INDEX])

        var checksum = START_B_INDEX

        for (i in input.indices) {
            val charCode = input[i].code
            val index = charCode - 32 // En Code 128B los caracteres empiezan en espacio (ASCII 32 = index 0)
            if (index in 0..95) {
                builder.append(PATTERNS[index])
                checksum += index * (i + 1)
            } else {
                builder.append(PATTERNS[0])
            }
        }

        val checksumIndex = checksum % 103
        builder.append(PATTERNS[checksumIndex])
        builder.append(PATTERNS[STOP_INDEX])

        return builder.toString()
    }
}

/**
 * Componente Compose de alta precisión que dibuja las barras reales en Canvas vectorial.
 */
@Composable
fun BarcodeCanvas(
    codigo: String,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = com.app.administradorfarmadon.disenotemaapp.ui.FDColors.TextPrimary
) {
    val bitSequence = remember(codigo) {
        Code128BarcodeGenerator.codificarCode128B(codigo)
    }

    if (bitSequence.isEmpty()) return

    Canvas(modifier = modifier) {
        val totalModules = bitSequence.length
        val moduleWidth = size.width / totalModules.toFloat()
        val height = size.height

        for (i in bitSequence.indices) {
            if (bitSequence[i] == '1') {
                drawRect(
                    color = barColor,
                    topLeft = Offset(x = i * moduleWidth, y = 0f),
                    size = Size(width = moduleWidth + 0.5f, height = height)
                )
            }
        }
    }
}

/**
 * Generador nativo de Documentos PDF con etiquetas de venta para impresión oficial o compartir.
 */
object LabelPdfExporter {

    private fun truncarTextoAlAncho(paint: Paint, texto: String, maxAnchoPt: Float): String {
        if (paint.measureText(texto) <= maxAnchoPt) return texto
        var temp = texto
        while (temp.isNotEmpty() && paint.measureText("$temp...") > maxAnchoPt) {
            temp = temp.dropLast(1)
        }
        return if (temp.isEmpty()) "" else "$temp..."
    }

    fun generarPdfEtiquetas(
        context: Context,
        producto: MoldeProductos,
        copias: Int = 1,
        formatoGondola: Boolean = true,
        nombrePresentacion: String = "",
        precioVenta: Double = 0.0,
        codigoPersonalizado: String? = null
    ): File {
        val pdfDocument = PdfDocument()

        val anchoPt = if (formatoGondola) 142 else 95
        val altoPt = if (formatoGondola) 90 else 60

        // Determinar código exclusivo por presentación para evitar cobros erróneos de cajas enteras
        val presCoincidente = producto.presentaciones.firstOrNull { it.nombre.equals(nombrePresentacion, ignoreCase = true) }
        val maxCantidadPres = producto.presentaciones.maxByOrNull { it.cantidad }?.cantidad ?: 1
        val esFraccion = presCoincidente != null && (presCoincidente.cantidad < maxCantidadPres || producto.presentaciones.size > 1)

        val codBase = codigoPersonalizado?.ifBlank { null } ?: producto.codigo
        val codLimpio = codBase.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()

        val codFinal = if (presCoincidente != null && presCoincidente.codigoBarras.isNotBlank()) {
            presCoincidente.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        } else if (esFraccion && presCoincidente != null && presCoincidente.cantidad > 1) {
            "$codLimpio-B${presCoincidente.cantidad}"
        } else if (esFraccion && presCoincidente != null && presCoincidente.cantidad == 1 && producto.presentaciones.any { it.cantidad > 1 }) {
            "$codLimpio-U1"
        } else {
            codLimpio
        }

        val bitSequence = Code128BarcodeGenerator.codificarCode128B(codFinal)

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val paintBar = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val copiasFinales = copias.coerceIn(1, 50)

        for (p in 1..copiasFinales) {
            val pageInfo = PdfDocument.PageInfo.Builder(anchoPt, altoPt, p).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Fondo blanco limpio para impresión
            canvas.drawColor(Color.WHITE)

            if (formatoGondola) {
                // 1. Título del Producto + Presentación con protección de bordes
                paintText.textSize = 7.5f
                paintText.isFakeBoldText = true
                val tituloRaw = if (nombrePresentacion.isNotBlank()) {
                    "${producto.nombre} ($nombrePresentacion)"
                } else {
                    producto.nombre
                }
                val tituloProtegido = truncarTextoAlAncho(paintText, tituloRaw, anchoPt - 12f)
                canvas.drawText(tituloProtegido, 6f, 11f, paintText)

                // 2. Concentración y Ubicación
                paintText.textSize = 5.5f
                paintText.isFakeBoldText = false
                val infoExtraRaw = "${producto.concentracion.ifBlank { "Unidad" }} · Ubic: ${producto.ubicacion.ifBlank { "Mostrador" }}"
                val infoExtraProtegida = truncarTextoAlAncho(paintText, infoExtraRaw, anchoPt - 12f)
                canvas.drawText(infoExtraProtegida, 6f, 19f, paintText)

                // 3. Código de barras (Canvas) con zona intocable
                if (bitSequence.isNotEmpty()) {
                    val barWidth = (anchoPt - 16f) / bitSequence.length.toFloat()
                    val barTop = 23f
                    val barHeight = 35f

                    for (i in bitSequence.indices) {
                        if (bitSequence[i] == '1') {
                            canvas.drawRect(
                                8f + (i * barWidth),
                                barTop,
                                8f + ((i + 1) * barWidth),
                                barTop + barHeight,
                                paintBar
                            )
                        }
                    }

                    // 4. Número del código debajo de las barras
                    paintText.textSize = 5.5f
                    val textWidth = paintText.measureText(codFinal)
                    canvas.drawText(codFinal, (anchoPt - textWidth) / 2f, 66f, paintText)
                }

                // 5. Precio de Venta (PVP)
                val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }
                val precioTxt = format.format(precioVenta)
                paintText.textSize = 8.5f
                paintText.isFakeBoldText = true
                canvas.drawText("PVP: $precioTxt", 6f, 78f, paintText)

            } else {
                // Mini Etiqueta para Frasco / Blíster con límites estrictos
                paintText.textSize = 6.5f
                paintText.isFakeBoldText = true
                val miniTituloRaw = if (nombrePresentacion.isNotBlank()) {
                    "$nombrePresentacion - ${producto.nombre}"
                } else {
                    producto.nombre
                }
                val miniTituloProtegido = truncarTextoAlAncho(paintText, miniTituloRaw, anchoPt - 8f)
                canvas.drawText(miniTituloProtegido, 4f, 9f, paintText)

                if (bitSequence.isNotEmpty()) {
                    val barWidth = (anchoPt - 8f) / bitSequence.length.toFloat()
                    val barTop = 13f
                    val barHeight = 26f

                    for (i in bitSequence.indices) {
                        if (bitSequence[i] == '1') {
                            canvas.drawRect(
                                4f + (i * barWidth),
                                barTop,
                                4f + ((i + 1) * barWidth),
                                barTop + barHeight,
                                paintBar
                            )
                        }
                    }

                    paintText.textSize = 5f
                    paintText.isFakeBoldText = false
                    val textWidth = paintText.measureText(codFinal)
                    canvas.drawText(codFinal, (anchoPt - textWidth) / 2f, 46f, paintText)
                }
            }

            pdfDocument.finishPage(page)
        }

        val cacheDir = File(context.cacheDir, "etiquetas_impresion").apply { mkdirs() }
        val pdfFile = File(cacheDir, "etiqueta_${producto.indice.ifBlank { "prod" }}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Genera un lote de etiquetas para todas las presentaciones que cambiaron de precio simultáneamente.
     */
    fun generarPdfEtiquetasLote(
        context: Context,
        producto: MoldeProductos,
        items: List<com.app.administradorfarmadon.inventario.compartido.modelo.EtiquetaPendienteItem>,
        formatoGondola: Boolean = true
    ): File {
        val pdfDocument = PdfDocument()

        val anchoPt = if (formatoGondola) 142 else 95
        val altoPt = if (formatoGondola) 90 else 60

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val paintBar = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val maxCantidadPres = producto.presentaciones.maxByOrNull { it.cantidad }?.cantidad ?: 1
        val codBase = producto.codigo
        val codLimpio = codBase.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()

        items.forEachIndexed { index, item ->
            val pageInfo = PdfDocument.PageInfo.Builder(anchoPt, altoPt, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val presMatch = producto.presentaciones.firstOrNull { it.presentacionId == item.presentacionId }
            val esFraccion = item.cantidad < maxCantidadPres || producto.presentaciones.size > 1

            val codFinal = if (presMatch != null && presMatch.codigoBarras.isNotBlank()) {
                presMatch.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
            } else if (esFraccion && item.cantidad > 1) {
                "$codLimpio-B${item.cantidad}"
            } else if (esFraccion && item.cantidad == 1 && producto.presentaciones.any { it.cantidad > 1 }) {
                "$codLimpio-U1"
            } else {
                codLimpio
            }

            val bitSequence = Code128BarcodeGenerator.codificarCode128B(codFinal)

            if (formatoGondola) {
                paintText.textSize = 7.5f
                paintText.isFakeBoldText = true
                val tituloRaw = "${producto.nombre} (${item.nombre})"
                val tituloProtegido = truncarTextoAlAncho(paintText, tituloRaw, anchoPt - 12f)
                canvas.drawText(tituloProtegido, 6f, 11f, paintText)

                paintText.textSize = 5.5f
                paintText.isFakeBoldText = false
                val infoExtraRaw = "${producto.concentracion.ifBlank { "Unidad" }} · Ubic: ${producto.ubicacion.ifBlank { "Mostrador" }}"
                val infoExtraProtegida = truncarTextoAlAncho(paintText, infoExtraRaw, anchoPt - 12f)
                canvas.drawText(infoExtraProtegida, 6f, 19f, paintText)

                if (bitSequence.isNotEmpty()) {
                    val barWidth = (anchoPt - 16f) / bitSequence.length.toFloat()
                    val barTop = 23f
                    val barHeight = 35f

                    for (i in bitSequence.indices) {
                        if (bitSequence[i] == '1') {
                            canvas.drawRect(
                                8f + (i * barWidth),
                                barTop,
                                8f + ((i + 1) * barWidth),
                                barTop + barHeight,
                                paintBar
                            )
                        }
                    }

                    paintText.textSize = 5.5f
                    val textWidth = paintText.measureText(codFinal)
                    canvas.drawText(codFinal, (anchoPt - textWidth) / 2f, 66f, paintText)
                }

                val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }
                val precioTxt = format.format(item.precio)
                paintText.textSize = 8.5f
                paintText.isFakeBoldText = true
                canvas.drawText("PVP: $precioTxt", 6f, 78f, paintText)
            } else {
                paintText.textSize = 6.5f
                paintText.isFakeBoldText = true
                val miniTituloRaw = "${item.nombre} - ${producto.nombre}"
                val miniTituloProtegido = truncarTextoAlAncho(paintText, miniTituloRaw, anchoPt - 8f)
                canvas.drawText(miniTituloProtegido, 4f, 9f, paintText)

                if (bitSequence.isNotEmpty()) {
                    val barWidth = (anchoPt - 8f) / bitSequence.length.toFloat()
                    val barTop = 13f
                    val barHeight = 26f

                    for (i in bitSequence.indices) {
                        if (bitSequence[i] == '1') {
                            canvas.drawRect(
                                4f + (i * barWidth),
                                barTop,
                                4f + ((i + 1) * barWidth),
                                barTop + barHeight,
                                paintBar
                            )
                        }
                    }

                    paintText.textSize = 5f
                    paintText.isFakeBoldText = false
                    val textWidth = paintText.measureText(codFinal)
                    canvas.drawText(codFinal, (anchoPt - textWidth) / 2f, 46f, paintText)
                }
            }

            pdfDocument.finishPage(page)
        }

        val cacheDir = File(context.cacheDir, "etiquetas_impresion").apply { mkdirs() }
        val pdfFile = File(cacheDir, "etiquetas_lote_${producto.indice.ifBlank { "prod" }}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Dispara el Diálogo Oficial de Impresión Nativa de Android (PrintManager).
     * Solo invoca onImpresionConfirmada si el documento fue efectivamente procesado y enviado a la impresora.
     */
    fun imprimirPdfDirecto(
        context: Context,
        pdfFile: File,
        nombreTrabajo: String,
        onImpresionConfirmada: () -> Unit = {}
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(nombreTrabajo)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    java.io.FileInputStream(pdfFile).use { input ->
                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    onImpresionConfirmada()
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(nombreTrabajo, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Comparte el archivo PDF mediante el Intent oficial de Android con FileProvider.
     */
    fun compartirPdf(context: Context, pdfFile: File, titulo: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir Etiqueta de Medicamento"))
    }
}
