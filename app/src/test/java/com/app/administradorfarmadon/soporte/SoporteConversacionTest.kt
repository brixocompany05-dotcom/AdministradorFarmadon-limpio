package com.app.administradorfarmadon.soporte

import com.app.administradorfarmadon.soporte.datos.SoporteRepository
import com.app.administradorfarmadon.soporte.modelo.MensajeSoporte
import com.app.administradorfarmadon.soporte.modelo.SoporteAsignacion
import com.app.administradorfarmadon.soporte.modelo.SoporteCategorias
import com.app.administradorfarmadon.soporte.modelo.SoporteEstado
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import com.app.administradorfarmadon.soporte.modelo.combinarHistorialSoporte
import com.app.administradorfarmadon.soporte.modelo.debeIgnorarDobleEnvio
import org.junit.Assert.*
import org.junit.Test

/**
 * Blindaje puro del módulo Soporte (sin Firebase):
 * estados, cierre irreversible, categorías, orden e idempotencia.
 */
class SoporteConversacionTest {

    @Test
    fun `estados legacy se normalizan sin romper tickets viejos`() {
        assertEquals(SoporteEstado.OPEN, SoporteEstado.normalizar("ABIERTO"))
        assertEquals(SoporteEstado.IN_PROGRESS, SoporteEstado.normalizar("EN_PROCESO"))
        assertEquals(SoporteEstado.RESOLVED, SoporteEstado.normalizar("RESUELTO"))
        assertEquals(SoporteEstado.CLOSED, SoporteEstado.normalizar("CLOSED"))
        assertEquals(SoporteEstado.OPEN, SoporteEstado.normalizar(null))
    }

    @Test
    fun `CLOSED y RESUELTO bloquean escritura, el resto permite`() {
        assertFalse(SoporteTicket(estado = "CLOSED").puedeEscribir)
        assertFalse(SoporteTicket(estado = "RESUELTO").puedeEscribir)
        assertFalse(SoporteTicket(estado = "RESOLVED").puedeEscribir)
        assertTrue(SoporteTicket(estado = "OPEN").puedeEscribir)
        assertTrue(SoporteTicket(estado = "WAITING_CUSTOMER").puedeEscribir)
        assertTrue(SoporteTicket(estado = "WAITING_BRIXO").puedeEscribir)
        assertTrue(SoporteTicket(estado = "ABIERTO").puedeEscribir)
    }

    @Test
    fun `categorias oficiales tienen etiqueta humana`() {
        assertEquals("Caja", SoporteCategorias.etiqueta("CAJA"))
        assertEquals("Facturación electrónica", SoporteCategorias.etiqueta("FACTURACION_ELECTRONICA"))
        assertEquals("Inventario", SoporteCategorias.etiqueta("INVENTARIO_STOCK"))
        assertEquals("Otro", SoporteCategorias.etiqueta("LOQUESEA"))
    }

    @Test
    fun `mensajes se deduplican por clientMessageId y se ordenan determinista`() {
        val ticket = SoporteTicket(
            id = "tk_1",
            descripcion = "No puedo cerrar caja",
            fechaMs = 1000L,
            mensajes = listOf(
                MensajeSoporte(id = "a", clientMessageId = "k1", texto = "Hola", fechaMs = 2000L),
                MensajeSoporte(id = "a-duplicado", clientMessageId = "k1", texto = "Hola", fechaMs = 2000L),
                MensajeSoporte(id = "b", clientMessageId = "k2", texto = "¿Qué mensaje aparece?", fechaMs = 1500L)
            )
        )
        val ordenados = ticket.obtenerTodosLosMensajes()
        // descripcion (1000) + k2 (1500) + k1 (2000). El duplicado k1 no aparece dos veces.
        assertEquals(3, ordenados.size)
        assertTrue(ordenados[0].fechaMs <= ordenados[1].fechaMs)
        assertTrue(ordenados[1].fechaMs <= ordenados[2].fechaMs)
        assertEquals(1, ordenados.count { it.clientMessageId == "k1" })
    }

    @Test
    fun `adjunto mayor a 10MB se rechaza con mensaje humano`() {
        val res = SoporteRepository.validarAdjuntoPuro("foto.png", 11L * 1024L * 1024L, "image/png")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull()!!.message!!.contains("10 MB"))
    }

    @Test
    fun `adjunto con mime no permitido se rechaza`() {
        val res = SoporteRepository.validarAdjuntoPuro("app.exe", 1024L, "application/x-msdownload")
        assertTrue(res.isFailure)
    }

    @Test
    fun `el primer mensaje no se pinta dos veces cuando ya llego en vivo`() {
        val base = listOf(
            MensajeSoporte(id = "msg_init_tk1", clientMessageId = "", texto = "No puedo cerrar caja", fechaMs = 1000L),
            MensajeSoporte(id = "x", clientMessageId = "kx", texto = "Viejo", fechaMs = 500L)
        )
        val vivos = listOf(
            MensajeSoporte(id = "m1", clientMessageId = "k1", texto = "No puedo cerrar caja", fechaMs = 1000L, emisor = "FARMACIA")
        )
        val out = combinarHistorialSoporte(base, vivos, "No puedo cerrar caja", 1000L)
        assertEquals(2, out.size)
        assertTrue(out.none { it.id == "msg_init_tk1" })
    }

    @Test
    fun `sin asignacion no nombra agente y pide espera honesta`() {
        val t = SoporteTicket(estado = "OPEN", responsableAgenteId = "", responsableAgenteNombre = "")
        assertFalse(t.tieneAgenteAsignado)
        assertEquals("Esperando atención · en cola", t.textoAtencion)
    }

    @Test
    fun `con asignacion basta con En atención, el quién vive en el chat`() {
        val t = SoporteTicket(
            estado = "IN_PROGRESS",
            responsableAgenteId = "ag1", responsableAgenteNombre = "Carlos",
            assignmentState = SoporteAsignacion.ASSIGNED
        )
        assertTrue(t.tieneAgenteAsignado)
        assertEquals("En atención", t.textoAtencion)
    }

    @Test
    fun `asignacion liberada vuelve a cola sin nombrar dueno anterior`() {
        val t = SoporteTicket(
            estado = "IN_PROGRESS",
            responsableAgenteId = "", responsableAgenteNombre = "",
            assignmentState = SoporteAsignacion.RELEASED
        )
        assertFalse(t.tieneAgenteAsignado)
        assertEquals("Esperando atención · en cola", t.textoAtencion)
    }

    @Test
    fun `optimista desaparece cuando el servidor lo confirma con la misma clave`() {
        val base = emptyList<MensajeSoporte>()
        val vivos = listOf(
            MensajeSoporte(id = "k1", clientMessageId = "k1", texto = "Hola", fechaMs = 1000L, estadoLectura = "ENTREGADO")
        )
        val optimistas = listOf(
            MensajeSoporte(id = "k1", clientMessageId = "k1", texto = "Hola", fechaMs = 1000L, estadoLectura = "ENVIANDO")
        )
        val out = combinarHistorialSoporte(base, vivos, "", 0L, optimistas)
        // Uno solo, y manda la copia del servidor (la verdad), no el borrador.
        assertEquals(1, out.size)
        assertEquals("Hola", out[0].texto)
        assertEquals("ENTREGADO", out[0].estadoLectura)
    }

    @Test
    fun `optimista fallido se queda visible hasta reintentar`() {
        val out = combinarHistorialSoporte(
            emptyList(), emptyList(), "", 0L,
            listOf(MensajeSoporte(id = "k9", clientMessageId = "k9", texto = "Ayuda", fechaMs = 1000L, estadoLectura = "ERROR"))
        )
        assertEquals(1, out.size)
        assertEquals("ERROR", out[0].estadoLectura)
    }

    @Test
    fun `mismo texto dos veces seguidas se ignora, distinto pasa`() {
        assertTrue(debeIgnorarDobleEnvio("hola", 1000L, "hola", 2000L))
        assertFalse(debeIgnorarDobleEnvio("hola", 1000L, "hola", 3000L))
        assertFalse(debeIgnorarDobleEnvio("hola", 1000L, "otra cosa", 1200L))
        assertFalse(debeIgnorarDobleEnvio(null, 0L, "hola", 1000L))
    }

    @Test
    fun `mensaje de cierre es veraz y guía al siguiente paso`() {
        assertTrue(SoporteRepository.ERROR_CONVERSACION_CERRADA.contains("cerrada"))
        assertTrue(SoporteRepository.ERROR_CONVERSACION_CERRADA.contains("nuevo"))
    }
}

