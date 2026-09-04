package com.app.administradorfarmadon.facturacion.documentos.logica

import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FacturacionDocumentosFiltrosTest {

    private val doc1 = FacturacionDocumento(
        id = "doc1",
        tipo = "BOLETA",
        serie = "B001",
        correlativo = 1L,
        numeroCompleto = "B001-000001",
        clienteNombre = "Juan Perez",
        clienteNumeroDoc = "12345678",
        sucursalId = "principal",
        total = 50.0,
        estadoEnvio = FacturacionDocumento.ESTADO_PENDIENTE
    )

    private val doc2 = FacturacionDocumento(
        id = "doc2",
        tipo = "FACTURA",
        serie = "F001",
        correlativo = 1L,
        numeroCompleto = "F001-000001",
        clienteNombre = "BOTICA CENTRAL S.A.C.",
        clienteNumeroDoc = "20609999999",
        sucursalId = "principal",
        total = 200.0,
        estadoEnvio = FacturacionDocumento.ESTADO_ACEPTADO
    )

    private val doc3 = FacturacionDocumento(
        id = "doc3",
        tipo = "BOLETA",
        serie = "B002",
        correlativo = 1L,
        numeroCompleto = "B002-000001",
        clienteNombre = "Maria Lopez",
        clienteNumeroDoc = "87654321",
        sucursalId = "sede2",
        total = 30.0,
        estadoEnvio = FacturacionDocumento.ESTADO_RECHAZADO,
        motivo = "Error RUC emisor no coincide"
    )

    private val listaMuestra = listOf(doc1, doc2, doc3)

    @Test
    fun metricas_computadasCorrectamenteDesdeLaListaReal() {
        val state = FacturacionDocumentosUiState(documentos = listaMuestra)

        assertEquals(3, state.totalDocumentos)
        assertEquals(1, state.totalPendientes)
        assertEquals(1, state.totalAceptados)
        assertEquals(1, state.totalRechazados)
        assertEquals(2, state.totalBoletas)
        assertEquals(1, state.totalFacturas)
        assertEquals(200.0, state.montoTotalFacturado, 0.01)
    }

    @Test
    fun filtroEstado_soloMuestraDocumentosDelEstadoSeleccionado() {
        val state = FacturacionDocumentosUiState(
            documentos = listaMuestra,
            filtroEstado = FacturacionDocumento.ESTADO_PENDIENTE
        )

        val filtrados = state.documentosFiltrados
        assertEquals(1, filtrados.size)
        assertEquals("B001-000001", filtrados.first().numeroCompleto)
    }

    @Test
    fun filtroTipo_soloMuestraFacturas() {
        val state = FacturacionDocumentosUiState(
            documentos = listaMuestra,
            filtroTipo = "FACTURA"
        )

        val filtrados = state.documentosFiltrados
        assertEquals(1, filtrados.size)
        assertEquals("F001-000001", filtrados.first().numeroCompleto)
    }

    @Test
    fun filtroSede_soloMuestraDocumentosDeSede2() {
        val state = FacturacionDocumentosUiState(
            documentos = listaMuestra,
            filtroSedeId = "sede2"
        )

        val filtrados = state.documentosFiltrados
        assertEquals(1, filtrados.size)
        assertEquals("B002-000001", filtrados.first().numeroCompleto)
    }

    @Test
    fun busquedaTexto_encuentraPorCorrelativoONombre() {
        val state = FacturacionDocumentosUiState(
            documentos = listaMuestra,
            busquedaTexto = "Lopez"
        )

        val filtrados = state.documentosFiltrados
        assertEquals(1, filtrados.size)
        assertEquals("Maria Lopez", filtrados.first().clienteNombre)
    }

    @Test
    fun estadoVacioHonesto_sinDocumentosRetornaCeroYListaVacia() {
        val state = FacturacionDocumentosUiState(documentos = emptyList())
        assertEquals(0, state.totalDocumentos)
        assertTrue(state.documentosFiltrados.isEmpty())
        assertEquals(0.0, state.montoTotalFacturado, 0.01)
    }

    @Test
    fun f5A_dosFamiliasDeEstados_separacionPendienteYRequiereAtencion() {
        val docQuemado = FacturacionDocumento(
            id = "doc4",
            tipo = "BOLETA",
            serie = "B001",
            correlativo = 4L,
            numeroCompleto = "B001-000004",
            estadoEnvio = FacturacionDocumento.ESTADO_PENDIENTE,
            numeroQuemado = true
        )
        val state = FacturacionDocumentosUiState(documentos = listaMuestra + docQuemado)

        // doc1: PENDIENTE (no quemado) -> En cola
        assertEquals(1, state.totalPendientesEnCola)

        // doc3 (RECHAZADO) + doc4 (numeroQuemado = true) -> Requieren atención
        assertEquals(2, state.totalRequierenAtencion)

        // Filtro "PENDIENTE" solo trae los que se reintentan en cola
        val statePendientes = state.copy(filtroEstado = "PENDIENTE")
        val enCola = statePendientes.documentosFiltrados
        assertEquals(1, enCola.size)
        assertEquals("B001-000001", enCola.first().numeroCompleto)

        // Filtro "ATENCION" trae los rechazados o quemados que un humano debe revisar
        val stateAtencion = state.copy(filtroEstado = "ATENCION")
        val atencion = stateAtencion.documentosFiltrados
        assertEquals(2, atencion.size)
        assertTrue(atencion.any { it.id == "doc3" })
        assertTrue(atencion.any { it.id == "doc4" })
    }

    @Test
    fun f5C_reporteEnvioLote_desglosaDetallePorDocumento() {
        val reporte = com.app.administradorfarmadon.facturacion.envio.datos.ReporteEnvioLote(
            totalProcesados = 2,
            exitosos = listOf(
                com.app.administradorfarmadon.facturacion.envio.datos.ItemResultadoLote(
                    docId = "doc1",
                    numeroCompleto = "B001-000001",
                    exito = true,
                    estado = "ACEPTADO",
                    motivo = "Aceptado correctamente"
                )
            ),
            requierenAtencion = listOf(
                com.app.administradorfarmadon.facturacion.envio.datos.ItemResultadoLote(
                    docId = "doc3",
                    numeroCompleto = "B002-000001",
                    exito = false,
                    estado = "RECHAZADO",
                    motivo = "Error RUC emisor no coincide"
                )
            )
        )

        assertEquals(2, reporte.totalProcesados)
        assertEquals(1, reporte.exitosos.size)
        assertEquals(1, reporte.requierenAtencion.size)
        assertEquals("B001-000001", reporte.exitosos.first().numeroCompleto)
        assertEquals("Error RUC emisor no coincide", reporte.requierenAtencion.first().motivo)
    }
}
