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
     *
     * Sin else -> 03: Cualquier tipo no soportado lanza IllegalArgumentException para no quemar número.
     */
    fun mapearTipoComprobanteSunat(tipo: String): String {
        return when (tipo.uppercase().trim()) {
            "FACTURA", "01" -> "01"
            "BOLETA", "03" -> "03"
            "NOTA_CREDITO", "NOTA DE CREDITO", "NC", "07" -> "07"
            "COMUNICACION_BAJA", "BAJA", "ANULADO", "RA" -> "RA"
            else -> throw IllegalArgumentException("Tipo de comprobante no soportado para facturación electrónica: '$tipo'")
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
     * Valida de punta a punta antes de emitir para evitar quemar correlativos por errores de formato o datos vacíos.
     */
    fun construirSendBillPayload(
        emisor: EmisorFiscal,
        doc: FacturacionDocumento,
        venta: Venta?,
        devolucion: DevolucionVenta? = null
    ): Map<String, Any?> {
        val ruc = emisor.ruc.trim()
        if (ruc.length != 11 || !ruc.all { it.isDigit() }) {
            throw IllegalArgumentException("RUC de emisor inválido ($ruc): debe contener exactamente 11 dígitos numéricos.")
        }
        if (emisor.personaId.trim().isBlank() || emisor.personaToken.trim().isBlank()) {
            throw IllegalArgumentException("Credenciales APISUNAT incompletas en el emisor (personaId o personaToken vacíos).")
        }

        val tipoCodigo = mapearTipoComprobanteSunat(doc.tipo)
        val serie = doc.serie.trim()
        if (serie.isBlank()) {
            throw IllegalArgumentException("La serie del comprobante no puede estar vacía.")
        }
        val correlativo = doc.correlativo
        if (correlativo <= 0L) {
            throw IllegalArgumentException("El correlativo del comprobante ($correlativo) debe ser un número entero mayor a cero.")
        }
        if (doc.total <= 0.0) {
            throw IllegalArgumentException("El total del comprobante (S/ ${doc.total}) debe ser mayor a 0.00.")
        }
        if (doc.fechaMs <= 0L) {
            throw IllegalArgumentException("Fecha de emisión inválida: fechaMs debe ser un timestamp real mayor a 0.")
        }

        val fileName = construirFileName(ruc, doc.tipo, serie, correlativo)

        val fechaEmision = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(doc.fechaMs))
        val horaEmision = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(doc.fechaMs))

        // Validación estricta del cliente
        val tipoDocCliente = mapearTipoDocClienteSunat(doc.clienteTipoDoc, doc.clienteNumeroDoc)
        val numDocCliente = doc.clienteNumeroDoc.trim()

        if (tipoCodigo == "01") {
            // FACTURA exige cliente con RUC (tipo 6) de 11 dígitos
            if (tipoDocCliente != "6" || numDocCliente.length != 11 || !numDocCliente.all { it.isDigit() }) {
                throw IllegalArgumentException("Factura exige cliente con RUC válido de 11 dígitos numéricos (recibido: '$numDocCliente', tipo: '${doc.clienteTipoDoc}').")
            }
        } else {
            if (doc.clienteTipoDoc.trim().uppercase() == "DNI" && (numDocCliente.length != 8 || !numDocCliente.all { it.isDigit() })) {
                throw IllegalArgumentException("DNI de cliente inválido: debe tener 8 dígitos numéricos (recibido: '$numDocCliente').")
            }
            if (doc.clienteTipoDoc.trim().uppercase() == "RUC" && (numDocCliente.length != 11 || !numDocCliente.all { it.isDigit() })) {
                throw IllegalArgumentException("RUC de cliente inválido: debe tener 11 dígitos numéricos (recibido: '$numDocCliente').")
            }
        }

        val numDocClienteFinal = if (tipoDocCliente == "0") "00000000" else numDocCliente
        val nombreCliente = doc.clienteNombre.trim().ifBlank { "CLIENTES VARIOS" }

        // Ítems reales (sin líneas artificiales ITEM-01 / CONSUMO EN FARMACIA)
        val detalles = if (tipoCodigo == "07") {
            if (devolucion == null || devolucion.items.isEmpty()) {
                throw IllegalArgumentException("Nota de Crédito exige los ítems devueltos reales de la devolución (lista vacía).")
            }
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
        } else {
            val itemsSource: List<ItemVenta> = venta?.items ?: emptyList()
            if (itemsSource.isEmpty()) {
                throw IllegalArgumentException("El comprobante exige al menos un ítem real vendido (sin líneas agregadas artificiales).")
            }
            // Prorrateo del descuento global para que Σ líneas == total (si no, SUNAT rechaza por descuadre).
            val sumaBruta = itemsSource.sumOf { round2(it.precioUnitario * it.cantidad) }
            val factorDesc = if (sumaBruta > 0.0 && venta != null && venta.subtotal > 0.0) {
                (venta.total / venta.subtotal).coerceIn(0.0, 1.0)
            } else 1.0
            itemsSource.mapIndexed { index, itm ->
                val precioNeto = round2(itm.precioUnitario * factorDesc)
                val subtotalLinea = round2(precioNeto * itm.cantidad)
                val desglose = calcularDesgloseIgv(subtotalLinea)
                val valorUnitario = round2(precioNeto / 1.18)

                mapOf(
                    "codItem" to itm.productoId.ifBlank { "PROD-${index + 1}" },
                    "descripcion" to itm.nombreProducto.ifBlank { "PRODUCTO FARMACÉUTICO" },
                    "unidad" to "NIU",
                    "cantidad" to itm.cantidad,
                    "mtoValorUnitario" to valorUnitario,
                    "mtoPrecioUnitario" to precioNeto,
                    "mtoValorVenta" to desglose.baseGravada,
                    "mtoBaseIgv" to desglose.baseGravada,
                    "porcentajeIgv" to 18,
                    "igv" to desglose.igv,
                    "tipAfeIgv" to "10", // Gravado - Operación Onerosa
                    "totalImpuestos" to desglose.igv
                )
            }
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
                "numDoc" to numDocClienteFinal,
                "rznSocial" to nombreCliente
            ),
            "detalles" to detalles,
            "totales" to mapOf(
                "gravadas" to desgloseTotal.baseGravada,
                "igv" to desgloseTotal.igv,
                "total" to desgloseTotal.total
            )
        )

        // Si es Nota de Crédito (tipo 07), se exige y valida referencia al comprobante afectado
        if (tipoCodigo == "07") {
            if (venta == null || venta.serie.isBlank() || venta.correlativo <= 0L) {
                throw IllegalArgumentException("Nota de Crédito exige comprobante origen válido con serie y correlativo.")
            }
            val tipoModificado = mapearTipoComprobanteSunat(venta.tipoComprobante)

            documentBody["docModificado"] = mapOf(
                "documento" to tipoModificado,
                "serie" to venta.serie.trim(),
                "correlativo" to venta.correlativo.toString(),
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
