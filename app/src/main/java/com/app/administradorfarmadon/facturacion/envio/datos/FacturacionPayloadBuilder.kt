package com.app.administradorfarmadon.facturacion.envio.datos

import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

object FacturacionPayloadBuilder {

    /**
     * Construye el nombre de archivo oficial exigido por SUNAT:
     * RUC-TIPO-SERIE-CORRELATIVO8 (ej: 20601234567-03-B001-00000042)
     */
    fun construirFileName(rucEmisor: String, tipoDoc: String, serie: String, correlativo: Long): String {
        val codigoTipo = mapearTipoComprobanteSunat(tipoDoc)
        val correlativo8 = String.format(Locale.US, "%08d", correlativo)
        return "${rucEmisor.trim()}-$codigoTipo-${serie.trim()}-$correlativo8"
    }

    /**
     * Mapeo de códigos SUNAT:
     * 01: Factura
     * 03: Boleta
     * 07: Nota de Crédito
     * RA: Comunicación de Baja
     */
    fun mapearTipoComprobanteSunat(tipo: String): String {
        return when (tipo.uppercase().trim()) {
            "FACTURA" -> "01"
            "BOLETA" -> "03"
            "NOTA_CREDITO", "NOTA DE CREDITO", "NC" -> "07"
            "COMUNICACION_BAJA", "BAJA", "ANULADO" -> "RA"
            else -> "03"
        }
    }

    /**
     * Mapeo de tipo de documento del cliente para SUNAT:
     * 1: DNI (8 dígitos)
     * 6: RUC (11 dígitos)
     * 4: Carnet de Extranjería
     * 7: Pasaporte
     * 0: Sin documento / Varios
     */
    fun mapearTipoDocClienteSunat(tipoDoc: String, numDoc: String): String {
        val t = tipoDoc.uppercase().trim()
        val n = numDoc.trim()
        return when {
            t == "DNI" || (n.length == 8 && n.all { it.isDigit() }) -> "1"
            t == "RUC" || (n.length == 11 && n.all { it.isDigit() }) -> "6"
            t == "CE" || t.contains("EXTRANJER") -> "4"
            t == "PASAPORTE" -> "7"
            else -> "0"
        }
    }

    /**
     * Desglose de IGV 18% para una línea de producto.
     * En Perú el precio de venta en farmacia incluye IGV.
     */
    fun calcularDesgloseIgv(montoTotal: Double): DesgloseIgv {
        val total = round2(montoTotal)
        val baseGravada = round2(total / 1.18)
        val igv = round2(total - baseGravada)
        return DesgloseIgv(
            total = total,
            baseGravada = baseGravada,
            igv = igv
        )
    }

    data class DesgloseIgv(
        val total: Double,
        val baseGravada: Double,
        val igv: Double
    )

    /**
     * Construye el JSON exacto para el endpoint /personas/v1/sendBill según la especificación oficial APISUNAT.
     */
    fun construirSendBillPayload(
        emisor: EmisorFiscal,
        doc: FacturacionDocumento,
        venta: Venta?,
        devolucion: DevolucionVenta? = null
    ): Map<String, Any?> {
        val ruc = emisor.ruc.trim()
        val tipoCodigo = mapearTipoComprobanteSunat(doc.tipo)
        val serie = doc.serie.trim()
        val correlativo = doc.correlativo
        val fileName = construirFileName(ruc, doc.tipo, serie, correlativo)

        val fechaEmision = if (doc.fechaMs > 0L) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(doc.fechaMs))
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        val horaEmision = if (doc.fechaMs > 0L) {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(doc.fechaMs))
        } else {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }

        val tipoDocCliente = mapearTipoDocClienteSunat(doc.clienteTipoDoc, doc.clienteNumeroDoc)
        val numDocCliente = if (tipoDocCliente == "0") "00000000" else doc.clienteNumeroDoc.trim()
        val nombreCliente = doc.clienteNombre.trim().ifBlank { "CLIENTES VARIOS" }

        // Ítems reales:
        // FIX C-F1: Para Nota de Crédito (tipo 07), se desglosan exclusivamente los ítems devueltos en la devolución,
        // jamás las líneas completas de la venta original.
        val itemsSource: List<ItemVenta> = venta?.items ?: emptyList()
        val detalles = if (tipoCodigo == "07" && devolucion != null && devolucion.items.isNotEmpty()) {
            devolucion.items.mapIndexed { index, itm ->
                val subtotalLinea = round2(if (itm.monto > 0.0) itm.monto else (itm.precioUnitario * itm.cantidad))
                val desglose = calcularDesgloseIgv(subtotalLinea)
                val valorUnitario = round2(itm.precioUnitario / 1.18)

                mapOf(
                    "codItem" to itm.productoId.ifBlank { "PROD-${index + 1}" },
                    "descripcion" to itm.nombreProducto.ifBlank { "PRODUCTO DEVUELTO" },
                    "unidad" to "NIU",
                    "cantidad" to itm.cantidad,
                    "mtoValorUnitario" to valorUnitario,
                    "mtoPrecioUnitario" to round2(itm.precioUnitario),
                    "mtoValorVenta" to desglose.baseGravada,
                    "mtoBaseIgv" to desglose.baseGravada,
                    "porcentajeIgv" to 18,
                    "igv" to desglose.igv,
                    "tipAfeIgv" to "10", // Gravado - Operación Onerosa
                    "totalImpuestos" to desglose.igv
                )
            }
        } else if (itemsSource.isNotEmpty()) {
            itemsSource.mapIndexed { index, itm ->
                val subtotalLinea = round2(itm.precioUnitario * itm.cantidad)
                val desglose = calcularDesgloseIgv(subtotalLinea)
                val valorUnitario = round2(itm.precioUnitario / 1.18)

                mapOf(
                    "codItem" to itm.productoId.ifBlank { "PROD-${index + 1}" },
                    "descripcion" to itm.nombreProducto.ifBlank { "PRODUCTO FARMACÉUTICO" },
                    "unidad" to "NIU",
                    "cantidad" to itm.cantidad,
                    "mtoValorUnitario" to valorUnitario,
                    "mtoPrecioUnitario" to round2(itm.precioUnitario),
                    "mtoValorVenta" to desglose.baseGravada,
                    "mtoBaseIgv" to desglose.baseGravada,
                    "porcentajeIgv" to 18,
                    "igv" to desglose.igv,
                    "tipAfeIgv" to "10", // Gravado - Operación Onerosa
                    "totalImpuestos" to desglose.igv
                )
            }
        } else {
            // Si no hubiera ítems cargados, se usa la línea agregada con el total del documento
            val desglose = calcularDesgloseIgv(doc.total)
            listOf(
                mapOf(
                    "codItem" to "ITEM-01",
                    "descripcion" to if (tipoCodigo == "07") "DEVOLUCIÓN DE MERCADERÍA" else "CONSUMO EN FARMACIA",
                    "unidad" to "NIU",
                    "cantidad" to 1,
                    "mtoValorUnitario" to desglose.baseGravada,
                    "mtoPrecioUnitario" to desglose.total,
                    "mtoValorVenta" to desglose.baseGravada,
                    "mtoBaseIgv" to desglose.baseGravada,
                    "porcentajeIgv" to 18,
                    "igv" to desglose.igv,
                    "tipAfeIgv" to "10",
                    "totalImpuestos" to desglose.igv
                )
            )
        }

        val totalCalculado = round2(doc.total)
        val desgloseTotal = calcularDesgloseIgv(totalCalculado)

        val documentBody = mutableMapOf<String, Any?>(
            "documento" to tipoCodigo,
            "serie" to serie,
            "correlativo" to correlativo.toString(),
            "fechaEmision" to fechaEmision,
            "horaEmision" to horaEmision,
            "moneda" to "PEN",
            "formaPago" to mapOf("moneda" to "PEN", "tipo" to "Contado"),
            "emisor" to mapOf(
                "ruc" to ruc,
                "razonSocial" to emisor.razonSocial.trim(),
                "direccion" to emisor.direccionFiscal.trim()
            ),
            "cliente" to mapOf(
                "tipoDoc" to tipoDocCliente,
                "numDoc" to numDocCliente,
                "rznSocial" to nombreCliente
            ),
            "detalles" to detalles,
            "totales" to mapOf(
                "gravadas" to desgloseTotal.baseGravada,
                "igv" to desgloseTotal.igv,
                "total" to desgloseTotal.total
            )
        )

        // Si es Nota de Crédito (tipo 07), se agrega referencia al comprobante afectado
        if (tipoCodigo == "07") {
            val tipoModificado = if (venta != null && venta.tipoComprobante.isNotBlank()) {
                mapearTipoComprobanteSunat(venta.tipoComprobante)
            } else {
                "03"
            }
            val serieModificada = if (venta != null && venta.serie.isNotBlank()) venta.serie else "B001"
            val correlativoModificado = if (venta != null && venta.correlativo > 0L) venta.correlativo.toString() else "1"

            documentBody["docModificado"] = mapOf(
                "documento" to tipoModificado,
                "serie" to serieModificada,
                "correlativo" to correlativoModificado,
                "motivo" to doc.motivo.ifBlank { "DEVOLUCIÓN DE MERCADERÍA" }
            )
        }

        return mapOf(
            "personaId" to emisor.personaId.trim(),
            "personaToken" to emisor.personaToken.trim(),
            "fileName" to fileName,
            "document" to documentBody,
            "documentBody" to documentBody
        )
    }

    private fun round2(valor: Double): Double {
        return round(valor * 100.0) / 100.0
    }
}
