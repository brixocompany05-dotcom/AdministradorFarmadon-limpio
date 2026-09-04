package com.app.administradorfarmadon.facturacion.envio.datos

import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FacturacionPayloadBuilderTest {

    private val emisorValido = EmisorFiscal(
        ruc = "20601234567",
        razonSocial = "BOTICA FARMADON S.A.C.",
        direccionFiscal = "Av. Principal 123",
        personaId = "pid_test",
        personaToken = "token_test",
        verificadoOk = true
    )

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
    fun mapearTipoComprobanteSunat_sinElse03_tipoDesconocidoLanzaExcepcion() {
        assertEquals("01", FacturacionPayloadBuilder.mapearTipoComprobanteSunat("FACTURA"))
        assertEquals("03", FacturacionPayloadBuilder.mapearTipoComprobanteSunat("BOLETA"))
        assertEquals("07", FacturacionPayloadBuilder.mapearTipoComprobanteSunat("NOTA_CREDITO"))
        assertEquals("RA", FacturacionPayloadBuilder.mapearTipoComprobanteSunat("COMUNICACION_BAJA"))

        try {
            FacturacionPayloadBuilder.mapearTipoComprobanteSunat("TICKET_INTERNO")
            fail("Debe lanzar excepción para tipos desconocidos en lugar de convertir en boleta 03")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("no soportado") == true)
        }
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
        val doc = FacturacionDocumento(
            id = "doc123",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 42L,
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678",
            clienteNombre = "Juan Perez",
            total = 10.0,
            fechaMs = 1700000000000L
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

        val payload = FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, doc, venta)

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
    fun construirSendBillPayload_notaCreditoConDevolucionReal() {
        val doc = FacturacionDocumento(
            id = "nc123",
            tipo = "NOTA_CREDITO",
            serie = "BC01",
            correlativo = 1L,
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678",
            clienteNombre = "Juan Perez",
            total = 10.0,
            motivo = "DEVOLUCIÓN DE MERCADERÍA",
            fechaMs = 1700000000000L
        )

        val ventaOriginal = Venta(
            id = "vOriginal",
            serie = "B001",
            correlativo = 42L,
            tipoComprobante = "BOLETA"
        )

        val devolucion = DevolucionVenta(
            id = "dev1",
            ventaId = "vOriginal",
            items = listOf(
                ItemDevolucion(
                    productoId = "p1",
                    nombreProducto = "Paracetamol 500mg",
                    cantidad = 1,
                    precioUnitario = 10.0,
                    monto = 10.0
                )
            )
        )

        val payload = FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, doc, ventaOriginal, devolucion)

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
    fun construirSendBillPayload_guardsAdversariales_rechazanAntesDeQuemarNumero() {
        val ventaBase = Venta(
            id = "v1",
            items = listOf(ItemVenta(productoId = "p1", nombreProducto = "Paracetamol", cantidad = 1, precioUnitario = 10.0, subtotal = 10.0))
        )
        val docBase = FacturacionDocumento(
            id = "d1",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 1L,
            total = 10.0,
            fechaMs = 1700000000000L
        )

        // 1. Total <= 0
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docBase.copy(total = 0.0), ventaBase)
            fail("Total 0 debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("mayor a 0") == true)
        }

        // 2. Serie vacía
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docBase.copy(serie = ""), ventaBase)
            fail("Serie vacía debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("serie") == true)
        }

        // 3. Correlativo <= 0
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docBase.copy(correlativo = 0L), ventaBase)
            fail("Correlativo 0 debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("correlativo") == true)
        }

        // 4. Fecha inválida (<= 0)
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docBase.copy(fechaMs = 0L), ventaBase)
            fail("FechaMs <= 0 debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("fechaMs") == true)
        }

        // 5. Items vacíos (sin líneas inventadas)
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docBase, ventaBase.copy(items = emptyList()))
            fail("Venta sin ítems debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("al menos un ítem real") == true)
        }

        // 6. FACTURA con DNI o RUC incompleto
        val docFacturaInvalida = docBase.copy(
            tipo = "FACTURA",
            serie = "F001",
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "12345678"
        )
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docFacturaInvalida, ventaBase)
            fail("Factura con DNI debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("RUC") == true)
        }

        // 7. DNI con formato incompleto (menos de 8 dígitos)
        val docDniCorto = docBase.copy(
            clienteTipoDoc = "DNI",
            clienteNumeroDoc = "123"
        )
        try {
            FacturacionPayloadBuilder.construirSendBillPayload(emisorValido, docDniCorto, ventaBase)
            fail("DNI incompleto debe lanzar excepción")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("DNI") == true)
        }
    }
}
