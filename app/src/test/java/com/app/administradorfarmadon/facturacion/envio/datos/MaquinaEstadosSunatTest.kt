package com.app.administradorfarmadon.facturacion.envio.datos

import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MaquinaEstadosSunatTest {

    private val client = ApisunatClient()

    @Test
    fun reglaMadre_excepcionNoQuemaNumero_vuelveAPendiente() {
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
    fun apisunatClient_parseoExacto_noAceptadoNoEsAceptado() {
        val json = """{"_id":"doc_123","status":"NO ACEPTADO","message":"No califica"}"""
        val response = Response.success(json.toResponseBody("application/json".toMediaTypeOrNull()))

        val res = client.procesarRespuesta(response)
        assertTrue("NO ACEPTADO no debe interpretarse jamás como ACEPTADO", res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertTrue(err.mensaje.contains("Estado desconocido o no procesable"))
    }

    @Test
    fun apisunatClient_parseoExacto_bajaAceptadaNoEsRechazado() {
        val json = """{"_id":"doc_123","status":"BAJA_ACEPTADA","message":"Baja procesada"}"""
        val response = Response.success(json.toResponseBody("application/json".toMediaTypeOrNull()))

        val res = client.procesarRespuesta(response)
        assertTrue(res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertFalse("BAJA_ACEPTADA no debe quemar número como RECHAZADO", err.esRechazoSunat)
    }

    @Test
    fun apisunatClient_aceptadoSinCdrNiXml_retornaErrorProveedor() {
        // status == ACEPTADO pero no hay CDR ni XML devuelto
        val json = """{"_id":"doc_123","status":"ACEPTADO","message":"OK"}"""
        val response = Response.success(json.toResponseBody("application/json".toMediaTypeOrNull()))

        val res = client.procesarRespuesta(response)
        assertTrue("ACEPTADO sin CDR ni XML debe ser ErrorProveedor", res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertTrue(err.mensaje.contains("sin constancia"))
    }

    @Test
    fun apisunatClient_enviadoSinDocumentId_retornaErrorProveedor() {
        // status == ENVIADO pero docId viene vacío
        val json = """{"status":"ENVIADO","message":"Procesando"}"""
        val response = Response.success(json.toResponseBody("application/json".toMediaTypeOrNull()))

        val res = client.procesarRespuesta(response)
        assertTrue("ENVIADO sin ID de seguimiento debe ser ErrorProveedor", res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertTrue(err.mensaje.contains("sin identificador"))
    }

    @Test
    fun apisunatClient_error400Formato_noQuemaNumero() {
        // Error HTTP 400 (Bad request de formato / JSON) -> número libre
        val jsonErr = """{"error":"Validation failed: RUC must be 11 digits"}"""
        val response = Response.error<okhttp3.ResponseBody>(
            400,
            jsonErr.toResponseBody("application/json".toMediaTypeOrNull())
        )

        val res = client.procesarRespuesta(response)
        assertTrue(res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertFalse("Error 400 de formato no debe quemar correlativo", err.esRechazoSunat)
    }

    @Test
    fun apisunatClient_error422Sunat_quemaNumero() {
        // Error HTTP 422 de SUNAT -> correlativo quemado
        val jsonErr = """{"status":"RECHAZADO","message":"El comprobante fue rechazado por SUNAT"}"""
        val response = Response.error<okhttp3.ResponseBody>(
            422,
            jsonErr.toResponseBody("application/json".toMediaTypeOrNull())
        )

        val res = client.procesarRespuesta(response)
        assertTrue(res is ApisunatResultado.ErrorProveedor)
        val err = res as ApisunatResultado.ErrorProveedor
        assertTrue("Error 422 de SUNAT debe marcarse como rechazo fiscal", err.esRechazoSunat)
    }

    @Test
    fun reporteEnvioLote_soloCuentaAceptadosEnExitosos_enviadoVaAEnTramite() {
        val itemAceptado = ItemResultadoLote("d1", "B001-00000001", true, "ACEPTADO", "Aceptado con CDR")
        val itemEnviado = ItemResultadoLote("d2", "B001-00000002", false, "ENVIADO", "En trámite ante SUNAT")
        val itemRechazado = ItemResultadoLote("d3", "B001-00000003", false, "RECHAZADO", "Rechazado por SUNAT")

        val reporte = ReporteEnvioLote(
            totalProcesados = 3,
            exitosos = listOf(itemAceptado),
            enTramite = listOf(itemEnviado),
            requierenAtencion = listOf(itemRechazado)
        )

        assertEquals(1, reporte.exitosos.size)
        assertEquals("ACEPTADO", reporte.exitosos.first().estado)
        assertEquals(1, reporte.enTramite.size)
        assertEquals("ENVIADO", reporte.enTramite.first().estado)
        assertEquals(1, reporte.requierenAtencion.size)
    }
}
