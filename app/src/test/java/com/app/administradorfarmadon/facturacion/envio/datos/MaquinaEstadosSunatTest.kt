package com.app.administradorfarmadon.facturacion.envio.datos

import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaquinaEstadosSunatTest {

    @Test
    fun reglaMadre_excepcionNoQuemaNumero_vuelveAPendiente() {
        // Simulación de la regla SUNAT:
        // EXCEPCION = se rechazó por fallo técnico/servidor sin quemar el número -> se reintenta igual
        val docInicial = FacturacionDocumento(
            id = "doc1",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 100L,
            numeroCompleto = "B001-00000100",
            total = 15.0,
            estadoEnvio = FacturacionDocumento.ESTADO_PENDIENTE,
            numeroQuemado = false
        )

        // Llega EXCEPCION de SUNAT
        val respuestaSunat = "EXCEPCION"
        val mensajeRespuesta = "Servidor SUNAT no disponible temporalmente"

        val estadoFinal = when (respuestaSunat) {
            "ACEPTADO" -> FacturacionDocumento.ESTADO_ACEPTADO
            "RECHAZADO" -> FacturacionDocumento.ESTADO_RECHAZADO
            "EXCEPCION" -> FacturacionDocumento.ESTADO_PENDIENTE
            else -> FacturacionDocumento.ESTADO_ENVIADO
        }
        val numeroQuemado = respuestaSunat == "RECHAZADO"

        val docActualizado = docInicial.copy(
            estadoEnvio = estadoFinal,
            numeroQuemado = numeroQuemado,
            ultimoError = "SUNAT EXCEPCIÓN: $mensajeRespuesta"
        )

        assertEquals(FacturacionDocumento.ESTADO_PENDIENTE, docActualizado.estadoEnvio)
        assertFalse("Una excepción técnica jamás quema el correlativo", docActualizado.numeroQuemado)
        assertTrue(docActualizado.ultimoError.contains("SUNAT EXCEPCIÓN"))
    }

    @Test
    fun reglaMadre_rechazadoQuemaNumeroParaSiempre() {
        // Simulación de la regla SUNAT:
        // RECHAZADO = SUNAT quemó esa serie+número para siempre -> jamás se reintenta igual
        val docInicial = FacturacionDocumento(
            id = "doc2",
            tipo = "FACTURA",
            serie = "F001",
            correlativo = 50L,
            numeroCompleto = "F001-00000050",
            total = 250.0,
            estadoEnvio = FacturacionDocumento.ESTADO_PENDIENTE,
            numeroQuemado = false
        )

        // Llega RECHAZO de SUNAT (ej. RUC no habido o no activo)
        val respuestaSunat = "RECHAZADO"
        val mensajeSunat = "El RUC del receptor no se encuentra en estado ACTIVO"

        val estadoFinal = when (respuestaSunat) {
            "ACEPTADO" -> FacturacionDocumento.ESTADO_ACEPTADO
            "RECHAZADO" -> FacturacionDocumento.ESTADO_RECHAZADO
            "EXCEPCION" -> FacturacionDocumento.ESTADO_PENDIENTE
            else -> FacturacionDocumento.ESTADO_ENVIADO
        }
        val numeroQuemado = respuestaSunat == "RECHAZADO"

        val docActualizado = docInicial.copy(
            estadoEnvio = estadoFinal,
            numeroQuemado = numeroQuemado,
            motivo = "SUNAT RECHAZÓ: $mensajeSunat"
        )

        assertEquals(FacturacionDocumento.ESTADO_RECHAZADO, docActualizado.estadoEnvio)
        assertTrue("El rechazo de SUNAT quema el correlativo para siempre", docActualizado.numeroQuemado)
        assertTrue(docActualizado.motivo.contains("SUNAT RECHAZÓ"))
    }

    @Test
    fun reglaMadre_documentoConNumeroQuemado_noSePuedeReintentar() {
        val docQuemado = FacturacionDocumento(
            id = "doc3",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 101L,
            numeroCompleto = "B001-00000101",
            total = 12.0,
            estadoEnvio = FacturacionDocumento.ESTADO_RECHAZADO,
            numeroQuemado = true,
            motivo = "SUNAT RECHAZÓ: Correlativo ya registrado previamente"
        )

        // Intento de reenvío: la regla prohíbe reintentar un número quemado
        val puedeReintentar = !docQuemado.numeroQuemado && docQuemado.estadoEnvio != FacturacionDocumento.ESTADO_ACEPTADO

        assertFalse("Un documento con numeración quemada jamás puede reintentarse", puedeReintentar)
    }
}
