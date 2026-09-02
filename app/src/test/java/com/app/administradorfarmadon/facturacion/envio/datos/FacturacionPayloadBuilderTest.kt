package com.app.administradorfarmadon.facturacion.envio.datos

import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacturacionPayloadBuilderTest {

    @Test
    fun construirFileName_formatoOficialSunat() {
        val fileNameBoleta = FacturacionPayloadBuilder.construirFileName(
            rucEmisor = "20601234567",
            tipoDoc = "BOLETA",
            serie = "B001",
            correlativo = 42L
        )
        assertEquals("20601234567-03-B001-00000042", fileNameBoleta)

        val fileNameFactura = FacturacionPayloadBuilder.construirFileName(
            rucEmisor = "20601234567",
            tipoDoc = "FACTURA",
            serie = "F001",
            correlativo = 1L
        )
        assertEquals("20601234567-01-F001-00000001", fileNameFactura)

        val fileNameNotaCredito = FacturacionPayloadBuilder.construirFileName(
            rucEmisor = "20601234567",
            tipoDoc = "NOTA_CREDITO",
            serie = "BC01",
            correlativo = 5L
        )
        assertEquals("20601234567-07-BC01-00000005", fileNameNotaCredito)
    }

    @Test
    fun calcularDesgloseIgv_calculoMatematicoExacto() {
        val desglose = FacturacionPayloadBuilder.calcularDesgloseIgv(10.0)
        assertEquals(10.0, desglose.total, 0.001)
        assertEquals(8.47, desglose.baseGravada, 0.001)
        assertEquals(1.53, desglose.igv, 0.001)
        assertEquals(desglose.total, desglose.baseGravada + desglose.igv, 0.01)

        val desgloseCien = FacturacionPayloadBuilder.calcularDesgloseIgv(118.0)
        assertEquals(118.0, desgloseCien.total, 0.001)
        assertEquals(100.0, desgloseCien.baseGravada, 0.001)
        assertEquals(18.0, desgloseCien.igv, 0.001)
    }

    @Test
    fun construirSendBillPayload_boletaConItemsReales() {
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "BOTICA FARMADON S.A.C.",
            direccionFiscal = "Av. Principal 123",
            personaId = "pid_test",
            personaToken = "token_test",
            verificadoOk = true
        )

        val doc = FacturacionDocumento(
            id = "doc123",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 42L,
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678",
            clienteNombre = "Juan Perez",
            total = 10.0
        )

        val venta = Venta(
            id = "venta123",
            items = listOf(
                ItemVenta(
                    productoId = "p1",
                    nombreProducto = "Paracetamol 500mg",
                    cantidad = 1,
                    precioUnitario = 10.0,
                    subtotal = 10.0
                )
            ),
            cliente = ClienteDeVenta(
                tipoDocumento = "DNI",
                numeroDocumento = "12345678",
                nombre = "Juan Perez"
            )
        )

        val payload = FacturacionPayloadBuilder.construirSendBillPayload(emisor, doc, venta)

        assertEquals("pid_test", payload["personaId"])
        assertEquals("token_test", payload["personaToken"])
        assertEquals("20601234567-03-B001-00000042", payload["fileName"])

        @Suppress("UNCHECKED_CAST")
        val documentMap = payload["document"] as Map<String, Any?>
        val documentBodyMap = payload["documentBody"] as Map<String, Any?>
        assertEquals("03", documentMap["documento"])
        assertEquals("03", documentBodyMap["documento"])
        assertEquals("B001", documentMap["serie"])
        assertEquals("42", documentMap["correlativo"])

        @Suppress("UNCHECKED_CAST")
        val clienteMap = documentMap["cliente"] as Map<String, Any?>
        assertEquals("1", clienteMap["tipoDoc"])
        assertEquals("12345678", clienteMap["numDoc"])
        assertEquals("Juan Perez", clienteMap["rznSocial"])

        @Suppress("UNCHECKED_CAST")
        val totalesMap = documentMap["totales"] as Map<String, Any?>
        assertEquals(8.47, totalesMap["gravadas"])
        assertEquals(1.53, totalesMap["igv"])
        assertEquals(10.00, totalesMap["total"])
    }

    @Test
    fun construirSendBillPayload_notaCreditoConReferenciaAVenta() {
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "BOTICA FARMADON S.A.C.",
            direccionFiscal = "Av. Principal 123",
            personaId = "pid_test",
            personaToken = "token_test",
            verificadoOk = true
        )

        val doc = FacturacionDocumento(
            id = "nc123",
            tipo = "NOTA_CREDITO",
            serie = "BC01",
            correlativo = 1L,
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678",
            clienteNombre = "Juan Perez",
            total = 10.0,
            motivo = "DEVOLUCIÓN DE MERCADERÍA"
        )

        val ventaOriginal = Venta(
            id = "vOriginal",
            serie = "B001",
            correlativo = 42L,
            tipoComprobante = "BOLETA"
        )

        val payload = FacturacionPayloadBuilder.construirSendBillPayload(emisor, doc, ventaOriginal)

        @Suppress("UNCHECKED_CAST")
        val documentMap = payload["document"] as Map<String, Any?>
        assertEquals("07", documentMap["documento"])

        @Suppress("UNCHECKED_CAST")
        val docModificado = documentMap["docModificado"] as? Map<String, Any?>
        assertNotNull(docModificado)
        assertEquals("03", docModificado?.get("documento"))
        assertEquals("B001", docModificado?.get("serie"))
        assertEquals("42", docModificado?.get("correlativo"))
        assertEquals("DEVOLUCIÓN DE MERCADERÍA", docModificado?.get("motivo"))
    }

    @Test
    fun construirSendBillPayload_notaCreditoConItemsDeDevolucionParcial_noUsaItemsDeVentaCompleta() {
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "BOTICA FARMADON S.A.C.",
            direccionFiscal = "Av. Principal 123",
            personaId = "pid_test",
            personaToken = "token_test",
            verificadoOk = true
        )

        // Venta original con 3 líneas distintas (Total 105.00)
        val ventaOriginal = Venta(
            id = "vOriginal",
            serie = "B001",
            correlativo = 42L,
            tipoComprobante = "BOLETA",
            total = 105.0,
            items = listOf(
                ItemVenta(productoId = "p1", nombreProducto = "Paracetamol 500mg", cantidad = 5, precioUnitario = 10.0, subtotal = 50.0),
                ItemVenta(productoId = "p2", nombreProducto = "Amoxicilina 500mg", cantidad = 2, precioUnitario = 20.0, subtotal = 40.0),
                ItemVenta(productoId = "p3", nombreProducto = "Ibuprofeno 400mg", cantidad = 1, precioUnitario = 15.0, subtotal = 15.0)
            )
        )

        // Devolución parcial de SOLO 1 ítem (Ibuprofeno por 15.00)
        val devolucion = com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta(
            id = "dev_001",
            ventaId = "vOriginal",
            tipoDocumento = "NOTA_CREDITO",
            serie = "BC01",
            correlativo = 8L,
            numeroCompleto = "BC01-00000008",
            montoReembolso = 15.0,
            motivo = "PRODUCTO DEFECTUOSO",
            items = listOf(
                com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion(
                    productoId = "p3",
                    nombreProducto = "Ibuprofeno 400mg",
                    cantidad = 1,
                    precioUnitario = 15.0,
                    monto = 15.0
                )
            )
        )

        val docNC = FacturacionDocumento(
            id = "nc_dev_001",
            tipo = "NOTA_CREDITO",
            serie = "BC01",
            correlativo = 8L,
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678",
            clienteNombre = "Consumidor Final",
            total = 15.0,
            motivo = "PRODUCTO DEFECTUOSO"
        )

        val payload = FacturacionPayloadBuilder.construirSendBillPayload(
            emisor = emisor,
            doc = docNC,
            venta = ventaOriginal,
            devolucion = devolucion
        )

        @Suppress("UNCHECKED_CAST")
        val documentMap = payload["document"] as Map<String, Any?>
        assertEquals("07", documentMap["documento"])

        @Suppress("UNCHECKED_CAST")
        val detalles = documentMap["detalles"] as List<Map<String, Any?>>
        // FIX C-F1: Debe contener exactamente 1 ítem (el devuelto), NO los 3 ítems de la venta original
        assertEquals(1, detalles.size)
        assertEquals("p3", detalles[0]["codItem"])
        assertEquals("Ibuprofeno 400mg", detalles[0]["descripcion"])
        assertEquals(1, detalles[0]["cantidad"])
        assertEquals(15.0, detalles[0]["mtoPrecioUnitario"])

        @Suppress("UNCHECKED_CAST")
        val totalesMap = documentMap["totales"] as Map<String, Any?>
        assertEquals(15.0, totalesMap["total"])
        assertEquals(12.71, totalesMap["gravadas"])
        assertEquals(2.29, totalesMap["igv"])

        @Suppress("UNCHECKED_CAST")
        val docModificado = documentMap["docModificado"] as? Map<String, Any?>
        assertNotNull(docModificado)
        assertEquals("03", docModificado?.get("documento"))
        assertEquals("B001", docModificado?.get("serie"))
        assertEquals("42", docModificado?.get("correlativo"))
    }
}
