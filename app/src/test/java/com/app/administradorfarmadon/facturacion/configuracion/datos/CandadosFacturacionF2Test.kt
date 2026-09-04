package com.app.administradorfarmadon.facturacion.configuracion.datos

import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandadosFacturacionF2Test {

    @Test
    fun puedeCobrar_sinEmisorCompleto_siempreRetornaFalse() {
        // POS completamente listo para cobrar (caja abierta, carrito con items, pago exacto en efectivo)
        val stateListoSinEmisor = NuevaVentaUiState(
            emisorCompleto = false, // CANDADO F2: emisor pendiente o no verificado
            estadoCaja = EstadoCaja(estado = CajaSesion.ESTADO_ABIERTA),
            carrito = listOf(
                ItemVenta(
                    productoId = "p1",
                    presentacionId = "pr1",
                    nombreProducto = "Paracetamol 500mg",
                    precioUnitario = 10.0,
                    cantidad = 1
                )
            ),
            lineasPago = listOf(
                PagoVenta(
                    tipoId = "EFECTIVO",
                    instanciaId = "inst_efectivo",
                    nombreMetodo = "Efectivo",
                    monto = 10.0
                )
            )
        )

        // El botón cobrar DEBE estar deshabilitado
        assertFalse("No se puede cobrar sin emisor completo", stateListoSinEmisor.puedeCobrar)
    }

    @Test
    fun puedeCobrar_conEmisorCompleto_retornaTrueCuandoTodoEstaListo() {
        val stateListoConEmisor = NuevaVentaUiState(
            emisorCompleto = true, // CANDADO F2: emisor verificado ok
            checklist = com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede(
                farmaciaActiva = true,
                suscripcionValida = true,
                sedeActiva = true,
                emisorFiscalCompleto = true,
                seriesFiscalesCompletas = true,
                metodosPagoConfigurados = true,
                cantidadMetodosActivos = 1,
                posConfigGuardado = true,
                cajaAbierta = true
            ),
            estadoCaja = EstadoCaja(estado = CajaSesion.ESTADO_ABIERTA),
            carrito = listOf(
                ItemVenta(
                    productoId = "p1",
                    presentacionId = "pr1",
                    nombreProducto = "Paracetamol 500mg",
                    precioUnitario = 10.0,
                    cantidad = 1
                )
            ),
            lineasPago = listOf(
                PagoVenta(
                    tipoId = "EFECTIVO",
                    instanciaId = "inst_efectivo",
                    nombreMetodo = "Efectivo",
                    monto = 10.0
                )
            )
        )

        assertTrue("Debe permitir cobrar cuando el emisor está verificado", stateListoConEmisor.puedeCobrar)
    }

    @Test
    fun puedeCobrar_conEmisorCompletoPeroCajaCerrada_retornaFalse() {
        val stateCajaCerrada = NuevaVentaUiState(
            emisorCompleto = true,
            estadoCaja = EstadoCaja(estado = CajaSesion.ESTADO_CERRADA),
            carrito = listOf(
                ItemVenta(
                    productoId = "p1",
                    presentacionId = "pr1",
                    nombreProducto = "Paracetamol 500mg",
                    precioUnitario = 10.0,
                    cantidad = 1
                )
            ),
            lineasPago = listOf(
                PagoVenta(
                    tipoId = "EFECTIVO",
                    instanciaId = "inst_efectivo",
                    nombreMetodo = "Efectivo",
                    monto = 10.0
                )
            )
        )

        assertFalse("No se puede cobrar con caja cerrada", stateCajaCerrada.puedeCobrar)
    }
}
