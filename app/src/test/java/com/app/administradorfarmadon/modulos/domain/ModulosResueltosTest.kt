package com.app.administradorfarmadon.modulos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModulosResueltosTest {

    private val inventario = CatalogoHerramienta(
        id = "t1", modulo = "inventario", nombre = "Inventario", categoria = "operacion",
        orden = 1, estado = "activo",
        hijos = listOf(
            CatalogoHijo("importar", "Importar productos"),
            CatalogoHijo("transferencias", "Transferencia entre sucursales"),
            CatalogoHijo("alertas", "Alertas de vencimiento")
        )
    )

    private val caja = CatalogoHerramienta(
        id = "t2", modulo = "caja", nombre = "Caja", categoria = "operacion",
        orden = 2, estado = "activo"
    )

    private val catalogo = listOf(inventario, caja)

    @Test
    fun `padre en el plan con hijos activos se resuelve`() {
        val resueltos = ModulosResueltos.resolver(
            catalogo = catalogo,
            featuresPlan = setOf("inventario")
        )

        assertEquals(1, resueltos.size)
        assertEquals("inventario", resueltos[0].modulo)
        assertEquals(listOf("importar", "transferencias", "alertas"), resueltos[0].hijosActivos)
    }

    @Test
    fun `padre fuera del plan no se resuelve`() {
        val resueltos = ModulosResueltos.resolver(catalogo, setOf("ventas"))

        assertTrue(resueltos.isEmpty())
    }

    @Test
    fun `override del cliente apaga el padre completo`() {
        val resueltos = ModulosResueltos.resolver(
            catalogo, setOf("inventario"),
            overridesCliente = mapOf("inventario" to false)
        )

        assertTrue(resueltos.isEmpty())
    }

    @Test
    fun `herramienta hoja sin hijos se resuelve si esta en el plan`() {
        val resueltos = ModulosResueltos.resolver(catalogo, setOf("caja"))

        assertEquals(1, resueltos.size)
        assertEquals("caja", resueltos[0].modulo)
    }

    @Test
    fun `se ordenan por orden del catalogo`() {
        val resueltos = ModulosResueltos.resolver(catalogo, setOf("caja", "inventario"))

        assertEquals(listOf("inventario", "caja"), resueltos.map { it.modulo })
    }

    @Test
    fun `transicional acepta nombre si el catalogo no tiene modulo`() {
        val sinModulo = inventario.copy(modulo = "")
        val resueltos = ModulosResueltos.resolver(listOf(sinModulo), setOf("Inventario"))

        assertEquals(1, resueltos.size)
        assertEquals("Inventario", resueltos[0].nombre)
    }

    @Test
    fun `plan guardado por NOMBRE funciona aunque el catalogo ya tenga modulo`() {
        // F3: un plan creado antes de la normalización guarda nombres. Aunque la
        // herramienta ahora tenga código canónico, debe seguir resolviéndose.
        val resueltos = ModulosResueltos.resolver(catalogo, setOf("Inventario"))

        assertEquals(1, resueltos.size)
        assertEquals("inventario", resueltos[0].modulo)
    }

    @Test
    fun `override del cliente por NOMBRE apaga el padre`() {
        val resueltos = ModulosResueltos.resolver(
            catalogo, setOf("inventario"),
            overridesCliente = mapOf("Inventario" to false)
        )

        assertTrue(resueltos.isEmpty())
    }
}
