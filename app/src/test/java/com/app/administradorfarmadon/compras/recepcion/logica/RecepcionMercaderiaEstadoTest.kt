package com.app.administradorfarmadon.compras.recepcion.logica

import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.datos.RecepcionEntrega
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecepcionMercaderiaEstadoTest {

    @Test
    fun `una segunda recepcion muestra lo recibido antes y continua la misma factura`() {
        val pedido = PedidoCompra(
            id = "pedido-1",
            proveedorId = "proveedor-1",
            items = listOf(
                ItemPedidoCompra(
                    productoId = "producto-1",
                    productoNombre = "Producto real",
                    cantidad = 10,
                    cantidadRecibida = 6
                )
            ),
            recepciones = listOf(
                RecepcionEntrega(
                    numeroFactura = "F-100",
                    montoFactura = 1000.0,
                    condicionPago = "Crédito",
                    fechaVencimientoPago = "28/09/2026"
                )
            )
        )
        val factura = FacturaCompra(
            id = "proveedor-1__F-100",
            numeroFactura = "F-100",
            proveedorId = "proveedor-1",
            montoTotal = 1000.0,
            condicionPago = "Crédito",
            fechaVencimientoPago = "28/09/2026"
        )

        val estado = RecepcionMercaderiaEstado(
            pedido = pedido,
            facturaExistente = factura
        )

        assertEquals("F-100", estado.numeroFactura)
        assertEquals("1000.00", estado.montoFacturaManual)
        assertEquals(6, estado.unidadesRecibidasAntes)
        assertEquals(4, estado.unidadesEstaEntrega)
        assertEquals(0, estado.unidadesPendientesDespues)
        assertTrue(estado.facturaContinua)
    }
}
