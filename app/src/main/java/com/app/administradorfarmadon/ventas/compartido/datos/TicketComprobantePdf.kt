package com.app.administradorfarmadon.ventas.compartido.datos

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion.LabelPdfExporter
import com.app.administradorfarmadon.ventas.compartido.modelo.EmisorComprobante
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GENERADOR DE TICKET POS DE 80MM (R3/R12).
 * Formato estándar para impresoras térmicas de 80mm (226 pt).
 * Genera PDF limpio sin adornos ni gráficos pesados e invoca la impresión nativa.
 */
object TicketComprobantePdf {

    private const val TAG = "TicketComprobantePdf"
    private const val ANCHO_TICKET_PT = 226 // 80mm estándar en puntos

    fun generarPdf(
        context: Context,
        venta: Venta,
        emisor: EmisorComprobante
    ): File {
        val pdfDocument = PdfDocument()

        // Cálculo dinámico de altura para que el ticket sea continuo
        val lineasItems = venta.items.size * 2
        val lineasPagos = venta.pagos.size
        val alturaEstimada = (240 + (lineasItems * 12) + (lineasPagos * 12) + 120).coerceAtLeast(350)

        val pageInfo = PdfDocument.PageInfo.Builder(ANCHO_TICKET_PT, alturaEstimada, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 8.5f
        }

        val paintBold = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            isFakeBoldText = true
            textSize = 9.5f
        }

        val paintTitulo = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            isFakeBoldText = true
            textSize = 12f
        }

        var y = 20f

        fun drawCentrado(texto: String, paint: Paint) {
            val ancho = paint.measureText(texto)
            val x = ((ANCHO_TICKET_PT - ancho) / 2f).coerceAtLeast(6f)
            canvas.drawText(texto, x, y, paint)
            y += paint.textSize + 4f
        }

        fun drawLineaSeparadora() {
            canvas.drawLine(10f, y, (ANCHO_TICKET_PT - 10).toFloat(), y, paintText)
            y += 8f
        }

        fun drawFilaDosColumnas(col1: String, col2: String, bold: Boolean = false, textSize: Float = 8.5f) {
            val p = if (bold) paintBold else paintText
            val pPrevSize = p.textSize
            p.textSize = textSize

            canvas.drawText(col1, 10f, y, p)
            val ancho2 = p.measureText(col2)
            canvas.drawText(col2, ANCHO_TICKET_PT - 10f - ancho2, y, p)
            y += p.textSize + 4f
            p.textSize = pPrevSize
        }

        // ── 1. CABECERA DEL EMISOR ──
        drawCentrado(emisor.nombreFarmacia.uppercase(), paintTitulo)
        if (emisor.ruc.isNotBlank()) {
            drawCentrado("RUC: ${emisor.ruc}", paintText)
        }
        if (emisor.direccion.isNotBlank()) {
            drawCentrado(emisor.direccion, paintText)
        }
        if (emisor.sucursalNombre.isNotBlank()) {
            drawCentrado("Sede: ${emisor.sucursalNombre}", paintText)
        }
        y += 4f
        drawLineaSeparadora()

        // ── 2. DATOS DEL COMPROBANTE ──
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
            timeZone = VentasRepository.TIMEZONE_LIMA
        }.format(Date(venta.fechaHoraMs))
        drawCentrado("${venta.tipoComprobante}: ${venta.numeroCompleto}", paintBold)
        drawFilaDosColumnas("Fecha:", fechaFormateada)
        drawFilaDosColumnas("Cajero:", venta.cajeroNombre.ifBlank { "Mostrador" })

        if (venta.cliente.tipoDocumento != "NINGUNO" && venta.cliente.numeroDocumento.isNotBlank()) {
            drawFilaDosColumnas("Cliente:", venta.cliente.nombre)
            drawFilaDosColumnas("${venta.cliente.tipoDocumento}:", venta.cliente.numeroDocumento)
        } else {
            drawFilaDosColumnas("Cliente:", "Consumidor Final")
        }

        y += 4f
        drawLineaSeparadora()

        // ── 3. ÍTEMS VENDIDOS ──
        drawFilaDosColumnas("CANT / DESCRIPCIÓN", "TOTAL (S/)", bold = true)
        y += 2f

        for (item in venta.items) {
            val descBruta = "${item.nombreProducto} (${item.presentacionNombre})"
            val desc = if (descBruta.length > 30) descBruta.take(27) + "..." else descBruta
            val subtotalStr = String.format(Locale.US, "%.2f", item.subtotal)
            drawFilaDosColumnas("${item.cantidad}x $desc", subtotalStr)
            val puStr = "P.U. S/ ${String.format(Locale.US, "%.2f", item.precioUnitario)}"

            // FASE 11 H2: Trazabilidad lote + vencimiento para el cliente
            val loteStr = if (item.lotesConsumidos.isNotEmpty()) {
                item.lotesConsumidos.joinToString(", ") { "Lot: ${it.loteNumero} Venc: ${it.vencimiento}" }
            } else if (item.loteSugerido.isNotBlank()) {
                "Lot: ${item.loteSugerido} Venc: ${item.loteVencimientoSugerido}"
            } else ""

            val infoDetalle = if (loteStr.isNotBlank()) "$puStr | $loteStr" else puStr
            canvas.drawText("    $infoDetalle", 14f, y, paintText)
            y += paintText.textSize + 3f
        }

        y += 4f
        drawLineaSeparadora()

        // ── 4. TOTALES ──
        drawFilaDosColumnas("SUBTOTAL:", "S/ " + String.format(Locale.US, "%.2f", venta.subtotal))
        if (venta.descuento > 0.0) {
            drawFilaDosColumnas("DESCUENTO:", "- S/ " + String.format(Locale.US, "%.2f", venta.descuento))
        }
        drawFilaDosColumnas("TOTAL A PAGAR:", "S/ " + String.format(Locale.US, "%.2f", venta.total), bold = true, textSize = 11f)

        y += 4f
        drawLineaSeparadora()

        // ── 5. FORMAS DE PAGO Y VUELTO ──
        for (p in venta.pagos) {
            val opStr = if (p.numeroOperacion.isNotBlank()) " (Op: ${p.numeroOperacion})" else ""
            drawFilaDosColumnas("${p.nombreMetodo}$opStr:", "S/ " + String.format(Locale.US, "%.2f", p.monto))
        }
        drawFilaDosColumnas("Monto Recibido:", "S/ " + String.format(Locale.US, "%.2f", venta.montoRecibido))
        drawFilaDosColumnas("Vuelto:", "S/ " + String.format(Locale.US, "%.2f", venta.vuelto), bold = true)

        y += 8f
        drawLineaSeparadora()

        // ── 6. PIE DE TICKET ──
        drawCentrado("Comprobante interno — no válido para efectos tributarios", paintText)
        drawCentrado("¡Gracias por su preferencia!", paintBold)

        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "tickets_pos").apply { mkdirs() }
        val pdfFile = File(cacheDir, "ticket_${venta.id}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Genera el ticket e invoca directamente el spooler nativo de Android.
     */
    fun imprimirTicket(
        context: Context,
        venta: Venta,
        emisor: EmisorComprobante,
        onImpresionConfirmada: () -> Unit = {}
    ): Result<Unit> {
        return try {
            val pdf = generarPdf(context, venta, emisor)
            LabelPdfExporter.imprimirPdfDirecto(
                context = context,
                pdfFile = pdf,
                nombreTrabajo = "Ticket_${venta.numeroCompleto}",
                onImpresionConfirmada = onImpresionConfirmada
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar o imprimir ticket: ${e.message}", e)
            Result.failure(e)
        }
    }
}
