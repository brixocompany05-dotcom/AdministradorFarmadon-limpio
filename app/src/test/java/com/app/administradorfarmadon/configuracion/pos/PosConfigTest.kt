package com.app.administradorfarmadon.configuracion.pos

import com.app.administradorfarmadon.configuracion.pos.modelo.PosConfig
import org.junit.Assert.*
import org.junit.Test

class PosConfigTest {

    @Test
    fun `defaults seguros de PosConfig son comerciales y protegen la caja`() {
        val config = PosConfig()

        // Topes de Descuentos (Reglas Fijas)
        assertEquals(10.0, config.descuento.maxPct, 0.001)
        assertEquals(50.0, config.descuento.maxMonto, 0.001)

        // Topes de Caja
        assertEquals(500.0, config.caja.retiroMax, 0.001)
        assertEquals(200.0, config.caja.vueltoMax, 0.001)
        assertTrue(config.caja.entregaCiegaTurno)

        // Ticket y receta
        assertEquals(1, config.ticket.copias)
        assertEquals("Gracias por su compra", config.ticket.pie)
        assertTrue(config.receta.exigirConfirmacion)
    }

    @Test
    fun `evaluacion de limites maximos de descuento detecta excesos`() {
        val config = PosConfig()

        // Dentro del rango (8% y S/ 40) -> Permiso automático para el cajero
        assertFalse(config.excedeLimitesDescuento(pct = 8.0, monto = 40.0))

        // Excede porcentaje (11% > 10%) -> Bloqueo duro
        assertTrue(config.excedeLimitesDescuento(pct = 11.0, monto = 40.0))

        // Excede monto en soles (S/ 55.00 > S/ 50.00) -> Bloqueo duro
        assertTrue(config.excedeLimitesDescuento(pct = 8.0, monto = 55.0))
    }

    @Test
    fun `posConfig estaVigente solo cuando existe en servidor y no es borrador`() {
        // Recién instanciado (sin guardar en servidor)
        val nuevo = PosConfig()
        assertFalse(nuevo.existeEnServidor)
        assertTrue(nuevo.esBorrador)
        assertFalse(nuevo.estaVigente)

        // Sede nueva con copia-borrador generada pero aún no revisada ni guardada
        val borradorHeredado = PosConfig(existeEnServidor = true, esBorrador = true)
        assertFalse(borradorHeredado.estaVigente)

        // Guardado formal por el administrador
        val vigente = PosConfig(existeEnServidor = true, esBorrador = false)
        assertTrue(vigente.estaVigente)
    }

    @Test
    fun `ChecklistAperturaSede bloquea cuando falta cualquiera de los 8 pilares`() {
        val baseCompleta = com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede(
            farmaciaActiva = true,
            suscripcionValida = true,
            sedeActiva = true,
            emisorFiscalCompleto = true,
            seriesFiscalesCompletas = true,
            metodosPagoConfigurados = true,
            cantidadMetodosActivos = 2,
            posConfigGuardado = true,
            cajaAbierta = true
        )

        assertTrue(baseCompleta.todoListo)
        assertEquals(8, baseCompleta.totalCompletados)

        // 1. Falla si farmacia no activa
        assertFalse(baseCompleta.copy(farmaciaActiva = false).todoListo)

        // 2. Falla si suscripción vencida
        assertFalse(baseCompleta.copy(suscripcionValida = false).todoListo)

        // 3. Falla si sede inactiva
        assertFalse(baseCompleta.copy(sedeActiva = false).todoListo)

        // 4. Falla si emisor no verificado
        assertFalse(baseCompleta.copy(emisorFiscalCompleto = false).todoListo)

        // 5. Falla si series fiscales incompletas
        assertFalse(baseCompleta.copy(seriesFiscalesCompletas = false).todoListo)

        // 6. Falla si cero métodos de pago activos (cierre del bypass)
        assertFalse(baseCompleta.copy(metodosPagoConfigurados = false, cantidadMetodosActivos = 0).todoListo)

        // 7. Falla si posConfig no guardado en servidor (borrador o ausente)
        assertFalse(baseCompleta.copy(posConfigGuardado = false).todoListo)

        // 8. Falla si caja cerrada
        assertFalse(baseCompleta.copy(cajaAbierta = false).todoListo)
    }

    @Test
    fun `ChecklistAperturaSede entrega rutas de navegacion correctas para resolver faltantes`() {
        val checklistIncompleto = com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede(
            farmaciaActiva = true,
            suscripcionValida = true,
            sedeActiva = true,
            emisorFiscalCompleto = false,
            seriesFiscalesCompletas = false,
            metodosPagoConfigurados = false,
            posConfigGuardado = false,
            cajaAbierta = false
        )

        val items = checklistIncompleto.obtenerItems()
        val emisorItem = items.first { it.clave == "EMISOR" }
        assertEquals("facturacion_emisor", emisorItem.rutaNavegacion)
        assertEquals("Configurar Emisor", emisorItem.textoAccion)

        val posItem = items.first { it.clave == "POS_CONFIG" }
        assertEquals("config_pos", posItem.rutaNavegacion)
        assertEquals("Guardar Reglas", posItem.textoAccion)

        val metodosItem = items.first { it.clave == "METODOS" }
        assertEquals("config_metodos_pago", metodosItem.rutaNavegacion)
        assertEquals("Activar Métodos", metodosItem.textoAccion)

        val cajaItem = items.first { it.clave == "CAJA" }
        assertEquals("caja", cajaItem.rutaNavegacion)
        assertEquals("Abrir Caja", cajaItem.textoAccion)
    }
}
