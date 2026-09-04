package com.app.administradorfarmadon.inventario.compartido.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FacturaCompraTest {

    @Test
    fun `el total de la factura no se reduce por una entrega parcial`() {
        val factura = FacturaCompra(
            condicionPago = "Contado",
            montoTotal = 1000.0,
            montoAcumulado = 600.0,
            montoPagado = 0.0,
            items = listOf(ItemFacturaCompra(costoTotal = 600.0))
        )

        assertEquals(1000.0, factura.totalEfectivo, 0.001)
        assertEquals(1000.0, factura.saldoPendienteReal, 0.001)
        assertFalse(factura.esTotalmentePagada)
    }

    @Test
    fun `una factura contado solo queda pagada cuando existe pago real`() {
        val factura = FacturaCompra(
            condicionPago = "Contado",
            montoTotal = 1000.0,
            montoAcumulado = 600.0,
            montoPagado = 1000.0
        )

        assertEquals(0.0, factura.saldoPendienteReal, 0.001)
        assertTrue(factura.esTotalmentePagada)
    }

    @Test
    fun `al quitar el pago contado la factura vuelve a quedar pendiente`() {
        val facturaRevertida = FacturaCompra(
            condicionPago = "Contado",
            montoTotal = 1000.0,
            montoPagado = 0.0,
            estadoPago = "PENDIENTE"
        )

        assertEquals(1000.0, facturaRevertida.saldoPendienteReal, 0.001)
        assertFalse(facturaRevertida.esTotalmentePagada)
    }

    @Test
    fun `una nota de credito reduce la deuda viva y salda la factura`() {
        val factura = FacturaCompra(
            condicionPago = "Contado",
            montoTotal = 1200.0,
            montoAcumulado = 800.0,
            montoPagado = 800.0,
            ajustesFactura = listOf(
                AjusteFactura(
                    numeroDocumento = "NC-014",
                    monto = 400.0,
                    motivo = "Mercadería facturada que nunca llegó"
                )
            )
        )

        assertEquals(1200.0, factura.totalPapel, 0.001)
        assertEquals(400.0, factura.totalAjustes, 0.001)
        assertEquals(800.0, factura.totalEfectivo, 0.001)
        assertEquals(0.0, factura.saldoPendienteReal, 0.001)
        assertTrue(factura.esTotalmentePagada)
    }

    @Test
    fun `sin ajustes el total efectivo es el total del papel`() {
        val factura = FacturaCompra(
            montoTotal = 1200.0,
            montoAcumulado = 600.0
        )

        assertEquals(1200.0, factura.totalEfectivo, 0.001)
        assertEquals(0.0, factura.totalAjustes, 0.001)
        assertFalse(factura.esTotalmentePagada)
    }

    @Test
    fun `fecha de emision del papel se conserva independientemente de la fecha de registro`() {
        val factura = FacturaCompra(
            montoTotal = 500.0,
            fechaEmision = "05/08/2026",
            fechaRegistro = "03/09/2026"
        )

        assertEquals("05/08/2026", factura.fechaEmision)
        assertEquals("03/09/2026", factura.fechaRegistro)
    }

    @Test
    fun `factura sin fecha de emision de papel maneja string vacio sin fallar`() {
        val factura = FacturaCompra(
            montoTotal = 500.0,
            fechaEmision = "",
            fechaRegistro = "03/09/2026"
        )

        assertEquals("", factura.fechaEmision)
        assertEquals("03/09/2026", factura.fechaRegistro)
    }
}
