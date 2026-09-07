package com.app.administradorfarmadon.ventas.compartido.logica

import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoOperativoTurno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReglaBloqueoTurnoCajaTest {

    private fun obtenerTimestampHoy(): Long = System.currentTimeMillis()

    private fun obtenerTimestampAyer(): Long {
        val cal = Calendar.getInstance(ReglaBloqueoTurnoCaja.TIMEZONE_LIMA)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return cal.timeInMillis
    }

    private fun obtenerTimestampHaceTresDias(): Long {
        val cal = Calendar.getInstance(ReglaBloqueoTurnoCaja.TIMEZONE_LIMA)
        cal.add(Calendar.DAY_OF_YEAR, -3)
        return cal.timeInMillis
    }

    @Test
    fun `caja cerrada se clasifica como CAJA_CERRADA y no como vencida`() {
        val caja = EstadoCaja(estado = CajaSesion.ESTADO_CERRADA, aperturaMs = obtenerTimestampAyer())
        val estadoOp = ReglaBloqueoTurnoCaja.obtenerEstadoOperativo(caja)
        assertEquals(EstadoOperativoTurno.CAJA_CERRADA, estadoOp)
        assertFalse(ReglaBloqueoTurnoCaja.esTurnoVencido(caja))
        assertFalse(ReglaBloqueoTurnoCaja.esOperativo(caja))
    }

    @Test
    fun `turno abierto hoy se clasifica como OPERATIVO`() {
        val caja = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_hoy",
            aperturaMs = obtenerTimestampHoy()
        )
        val estadoOp = ReglaBloqueoTurnoCaja.obtenerEstadoOperativo(caja)
        assertEquals(EstadoOperativoTurno.OPERATIVO, estadoOp)
        assertFalse(ReglaBloqueoTurnoCaja.esTurnoVencido(caja))
        assertTrue(ReglaBloqueoTurnoCaja.esOperativo(caja))
    }

    @Test
    fun `turno abierto de ayer se clasifica como TURNO_VENCIDO`() {
        val caja = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_ayer",
            aperturaMs = obtenerTimestampAyer()
        )
        val estadoOp = ReglaBloqueoTurnoCaja.obtenerEstadoOperativo(caja)
        assertEquals(EstadoOperativoTurno.TURNO_VENCIDO, estadoOp)
        assertTrue(ReglaBloqueoTurnoCaja.esTurnoVencido(caja))
        assertFalse(ReglaBloqueoTurnoCaja.esOperativo(caja))
    }

    @Test
    fun `turno abierto de hace tres dias se clasifica como TURNO_VENCIDO`() {
        val caja = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_antigua",
            aperturaMs = obtenerTimestampHaceTresDias()
        )
        val estadoOp = ReglaBloqueoTurnoCaja.obtenerEstadoOperativo(caja)
        assertEquals(EstadoOperativoTurno.TURNO_VENCIDO, estadoOp)
        assertTrue(ReglaBloqueoTurnoCaja.esTurnoVencido(caja))
        assertFalse(ReglaBloqueoTurnoCaja.esOperativo(caja))
    }

    @Test
    fun `turno vencido bloquea terminantemente TODAS las operaciones mutantes de POS y Caja`() {
        val cajaVencida = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_ayer",
            aperturaMs = obtenerTimestampAyer()
        )

        // 1. Venta
        val resVenta = ReglaBloqueoTurnoCaja.validarPermiteVenta(cajaVencida)
        assertTrue(resVenta.isFailure)
        assertTrue(resVenta.exceptionOrNull()?.message?.contains("turno de caja") == true)

        // 2. Agregar al carrito
        val resAgregar = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(cajaVencida)
        assertTrue(resAgregar.isFailure)
        assertTrue(resAgregar.exceptionOrNull()?.message?.contains("vencido") == true)

        // 3. Modificar carrito
        val resModificar = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(cajaVencida)
        assertTrue(resModificar.isFailure)
        assertTrue(resModificar.exceptionOrNull()?.message?.contains("vencido") == true)

        // 4. Devoluciones
        val resDev = ReglaBloqueoTurnoCaja.validarPermiteDevolucion(cajaVencida)
        assertTrue(resDev.isFailure)
        assertTrue(resDev.exceptionOrNull()?.message?.contains("vencido") == true)

        // 5. Anulaciones
        val resAnular = ReglaBloqueoTurnoCaja.validarPermiteAnulacion(cajaVencida)
        assertTrue(resAnular.isFailure)
        assertTrue(resAnular.exceptionOrNull()?.message?.contains("vencido") == true)

        // 6. Suspender ventas
        val resSuspender = ReglaBloqueoTurnoCaja.validarPermiteSuspension(cajaVencida)
        assertTrue(resSuspender.isFailure)
        assertTrue(resSuspender.exceptionOrNull()?.message?.contains("vencido") == true)

        // 7. Reanudar ventas suspendidas
        val resReanudar = ReglaBloqueoTurnoCaja.validarPermiteReanudacion(cajaVencida)
        assertTrue(resReanudar.isFailure)
        assertTrue(resReanudar.exceptionOrNull()?.message?.contains("vencido") == true)

        // 8. Movimientos manuales de efectivo (ingresos / retiros)
        val resMov = ReglaBloqueoTurnoCaja.validarPermiteMovimientoManual(cajaVencida)
        assertTrue(resMov.isFailure)
        assertTrue(resMov.exceptionOrNull()?.message?.contains("vencido") == true)
    }

    @Test
    fun `exigirTurnoOperativo lanza excepcion con mensaje claro si turno esta vencido o caja cerrada`() {
        val cajaVencida = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_ayer",
            aperturaMs = obtenerTimestampAyer()
        )

        val exVencido = assertThrows(IllegalStateException::class.java) {
            ReglaBloqueoTurnoCaja.exigirTurnoOperativo(cajaVencida, "cobrar la venta")
        }
        assertTrue(exVencido.message!!.contains("vencido"))

        val cajaCerrada = EstadoCaja(estado = CajaSesion.ESTADO_CERRADA)
        val exCerrada = assertThrows(IllegalStateException::class.java) {
            ReglaBloqueoTurnoCaja.exigirTurnoOperativo(cajaCerrada, "cobrar la venta")
        }
        assertTrue(exCerrada.message!!.contains("cerrada"))
    }

    @Test
    fun `turno operativo hoy permite todas las operaciones mutantes`() {
        val cajaOperativa = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_hoy_123",
            aperturaMs = obtenerTimestampHoy()
        )

        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteVenta(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteDevolucion(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteAnulacion(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteSuspension(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteReanudacion(cajaOperativa).isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteMovimientoManual(cajaOperativa).isSuccess)

        // exigirTurnoOperativo no debe lanzar excepción
        ReglaBloqueoTurnoCaja.exigirTurnoOperativo(cajaOperativa, "cobrar la venta")
    }

    @Test
    fun `cajero A no puede operar en la caja o turno de cajero B`() {
        // Caja perteneciente al cajero B
        val cajaDeB = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_b_123",
            cajeroId = "cajero_B",
            abiertoPorId = "cajero_B",
            abiertoPorNombre = "Cajero Beta",
            cajaId = "caja_cajero_B",
            aperturaMs = obtenerTimestampHoy()
        )

        // El cajero A intenta operar en la caja de B
        val exAislamiento = assertThrows(IllegalStateException::class.java) {
            ReglaBloqueoTurnoCaja.exigirTurnoOperativo(
                estadoCaja = cajaDeB,
                accion = "cobrar una venta",
                cajeroIdEsperado = "cajero_A"
            )
        }
        assertTrue(exAislamiento.message!!.contains("AISLAMIENTO DE CAJA"))
        assertTrue(exAislamiento.message!!.contains("Cajero Beta"))

        // Todas las validaciones de mutación deben fallar para cajero A
        val resVenta = ReglaBloqueoTurnoCaja.validarPermiteVenta(cajaDeB, cajeroIdEsperado = "cajero_A")
        assertTrue(resVenta.isFailure)
        assertTrue(resVenta.exceptionOrNull()?.message?.contains("AISLAMIENTO DE CAJA") == true)

        val resCarrito = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(cajaDeB, cajeroIdEsperado = "cajero_A")
        assertTrue(resCarrito.isFailure)
        assertTrue(resCarrito.exceptionOrNull()?.message?.contains("AISLAMIENTO DE CAJA") == true)

        val resDev = ReglaBloqueoTurnoCaja.validarPermiteDevolucion(cajaDeB, cajeroIdEsperado = "cajero_A")
        assertTrue(resDev.isFailure)
        assertTrue(resDev.exceptionOrNull()?.message?.contains("AISLAMIENTO DE CAJA") == true)

        val resAnular = ReglaBloqueoTurnoCaja.validarPermiteAnulacion(cajaDeB, cajeroIdEsperado = "cajero_A")
        assertTrue(resAnular.isFailure)
        assertTrue(resAnular.exceptionOrNull()?.message?.contains("AISLAMIENTO DE CAJA") == true)

        val resMov = ReglaBloqueoTurnoCaja.validarPermiteMovimientoManual(cajaDeB, cajeroIdEsperado = "cajero_A")
        assertTrue(resMov.isFailure)
        assertTrue(resMov.exceptionOrNull()?.message?.contains("AISLAMIENTO DE CAJA") == true)

        // esOperativo debe retornar false para el cajero A
        assertFalse(ReglaBloqueoTurnoCaja.esOperativo(cajaDeB, cajeroIdEsperado = "cajero_A"))

        // Pero para el cajero B legítimo, debe ser totalmente operativo
        assertTrue(ReglaBloqueoTurnoCaja.esOperativo(cajaDeB, cajeroIdEsperado = "cajero_B"))
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteVenta(cajaDeB, cajeroIdEsperado = "cajero_B").isSuccess)
    }

    @Test
    fun `turno vencido de cajero A no bloquea ni afecta al cajero B que opera hoy`() {
        // Turno vencido de ayer de cajero A
        val cajaVencidaA = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_ayer_A",
            cajeroId = "cajero_A",
            abiertoPorId = "cajero_A",
            abiertoPorNombre = "Cajero Alpha",
            aperturaMs = obtenerTimestampAyer()
        )

        // Turno vigente de hoy de cajero B
        val cajaOperativaB = EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = "sesion_hoy_B",
            cajeroId = "cajero_B",
            abiertoPorId = "cajero_B",
            abiertoPorNombre = "Cajero Beta",
            aperturaMs = obtenerTimestampHoy()
        )

        // Cajero A está completamente bloqueado en su propio turno vencido
        assertTrue(ReglaBloqueoTurnoCaja.esTurnoVencido(cajaVencidaA))
        assertFalse(ReglaBloqueoTurnoCaja.esOperativo(cajaVencidaA, cajeroIdEsperado = "cajero_A"))
        val exA = assertThrows(IllegalStateException::class.java) {
            ReglaBloqueoTurnoCaja.exigirTurnoOperativo(cajaVencidaA, "cobrar", cajeroIdEsperado = "cajero_A")
        }
        assertTrue(exA.message!!.contains("BLOQUEO OPERATIVO"))

        // Cajero B está 100% operativo en su caja, sin ninguna interferencia
        assertFalse(ReglaBloqueoTurnoCaja.esTurnoVencido(cajaOperativaB))
        assertTrue(ReglaBloqueoTurnoCaja.esOperativo(cajaOperativaB, cajeroIdEsperado = "cajero_B"))
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteVenta(cajaOperativaB, cajeroIdEsperado = "cajero_B").isSuccess)
        assertTrue(ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(cajaOperativaB, cajeroIdEsperado = "cajero_B").isSuccess)
        // No lanza excepción
        ReglaBloqueoTurnoCaja.exigirTurnoOperativo(cajaOperativaB, "cobrar", cajeroIdEsperado = "cajero_B")
    }
}

