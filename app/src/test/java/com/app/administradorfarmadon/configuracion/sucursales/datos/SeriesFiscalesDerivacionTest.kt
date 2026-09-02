package com.app.administradorfarmadon.configuracion.sucursales.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeriesFiscalesDerivacionTest {

    @Test
    fun derivarSeriesPorIndice_indice1_generaSeriesEsperadas() {
        val series = derivarSeriesPorIndice(1L)
        assertEquals("B001", series.serieBoleta)
        assertEquals("F001", series.serieFactura)
        assertEquals("BC01", series.serieNotaCreditoBoleta)
        assertEquals("FC01", series.serieNotaCreditoFactura)
    }

    @Test
    fun derivarSeriesPorIndice_indice2_generaSeriesSiguienteSede() {
        val series = derivarSeriesPorIndice(2L)
        assertEquals("B002", series.serieBoleta)
        assertEquals("F002", series.serieFactura)
        assertEquals("BC02", series.serieNotaCreditoBoleta)
        assertEquals("FC02", series.serieNotaCreditoFactura)
    }

    @Test
    fun derivarSeriesPorIndice_dosIndicesDistintos_nuncaChocan() {
        val sede1 = derivarSeriesPorIndice(1L)
        val sede2 = derivarSeriesPorIndice(2L)

        assertNotEquals(sede1.serieBoleta, sede2.serieBoleta)
        assertNotEquals(sede1.serieFactura, sede2.serieFactura)
        assertNotEquals(sede1.serieNotaCreditoBoleta, sede2.serieNotaCreditoBoleta)
        assertNotEquals(sede1.serieNotaCreditoFactura, sede2.serieNotaCreditoFactura)
    }

    @Test
    fun derivarSeriesPorIndice_cumpleLongitudExactaSunatUbl21() {
        for (i in 1L..20L) {
            val s = derivarSeriesPorIndice(i)
            assertEquals("Boleta debe tener 4 caracteres", 4, s.serieBoleta.length)
            assertEquals("Factura debe tener 4 caracteres", 4, s.serieFactura.length)
            assertEquals("NC Boleta debe tener 4 caracteres", 4, s.serieNotaCreditoBoleta.length)
            assertEquals("NC Factura debe tener 4 caracteres", 4, s.serieNotaCreditoFactura.length)
            assertEquals('B', s.serieBoleta.first())
            assertEquals('F', s.serieFactura.first())
            assertEquals("BC", s.serieNotaCreditoBoleta.take(2))
            assertEquals("FC", s.serieNotaCreditoFactura.take(2))
        }
    }
}
