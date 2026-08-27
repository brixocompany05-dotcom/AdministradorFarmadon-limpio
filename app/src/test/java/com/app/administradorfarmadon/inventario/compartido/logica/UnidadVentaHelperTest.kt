package com.app.administradorfarmadon.inventario.compartido.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fija la regla de negocio del PROBLEMA 6 (blíster / unidad):
 * el inventario se cuenta en unidades físicas (caja/blíster = el factor completo),
 * y la venta de una unidad interna (cápsula/pastilla) descuenta SOLO su proporción.
 */
class UnidadVentaHelperTest {

    private fun cajaDe100(): List<PresentacionProducto> = listOf(
        PresentacionProducto("p1", "Caja de 100", "Caja", 100, "Cápsulas", 25.0, "")
    )

    @Test
    fun `factorContenido se deduce de la presentacion de mayor contenido`() {
        assertEquals(100.0, UnidadVentaHelper.factorContenido("", cajaDe100()), 0.000001)
        assertEquals(10.0, UnidadVentaHelper.factorContenido("", listOf(
            PresentacionProducto("b", "Blíster de 10", "Blíster", 10, "Cápsulas", 3.0, "")
        )), 0.000001)
    }

    @Test
    fun `factorContenido se deduce del texto cuando no hay presentaciones`() {
        assertEquals(100.0, UnidadVentaHelper.factorContenido("100 cápsulas", emptyList()), 0.000001)
        assertEquals(1.0, UnidadVentaHelper.factorContenido(null, emptyList()), 0.000001)
    }

    @Test
    fun `vender UNA capsula descuenta un centesimo de caja no una caja entera`() {
        val factor = UnidadVentaHelper.factorContenido("", cajaDe100())
        assertEquals(0.01, UnidadVentaHelper.stockFisicoParaVender(1.0, factor), 0.000001)
    }

    @Test
    fun `vender una caja completa descuenta exactamente una unidad fisica`() {
        val factor = UnidadVentaHelper.factorContenido("", cajaDe100())
        assertEquals(1.0, UnidadVentaHelper.stockFisicoParaVender(100.0, factor), 0.000001)
    }

    @Test
    fun `vender 30 capsulas descuenta 0 punto 3 de caja`() {
        val factor = UnidadVentaHelper.factorContenido("", cajaDe100())
        assertEquals(0.3, UnidadVentaHelper.stockFisicoParaVender(30.0, factor), 0.000001)
    }

    @Test
    fun `costo de una capsula es el costo de la caja dividido entre 100`() {
        val factor = UnidadVentaHelper.factorContenido("", cajaDe100())
        assertEquals(0.10, UnidadVentaHelper.costoPorUnidadInterna(10.0, factor), 0.000001)
    }

    @Test
    fun `producto sellado no puede venderse fraccionado pero si completo`() {
        val factor = 100.0
        assertFalse(UnidadVentaHelper.esFraccionCoherente(1.0, factor, permiteFraccionar = false))
        assertFalse(UnidadVentaHelper.esFraccionCoherente(99.0, factor, permiteFraccionar = false))
        assertTrue(UnidadVentaHelper.esFraccionCoherente(100.0, factor, permiteFraccionar = false))
        assertTrue(UnidadVentaHelper.esFraccionCoherente(1.0, factor, permiteFraccionar = true))
    }

    @Test
    fun `cantidad cero no es una venta valida`() {
        assertFalse(UnidadVentaHelper.esFraccionCoherente(0.0, 100.0, permiteFraccionar = true))
        assertEquals(0.0, UnidadVentaHelper.stockFisicoParaVender(0.0, 100.0), 0.000001)
    }
}