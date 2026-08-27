package com.app.administradorfarmadon.inventario.editarproductosinventario.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import org.junit.Assert.*
import org.junit.Test

class EditarProductoStateTest {

    @Test
    fun `isAnyFieldChanged detecta cambio en nombre`() {
        val original = MoldeProductos(
            nombre = "Paracetamol 500mg"
        )
        val state = EditarProductoUiState(
            originalProduct = original,
            nombre = "Paracetamol Forte 500mg"
        )
        assertTrue(state.isAnyFieldChanged)
    }

    @Test
    fun `isAnyFieldChanged devuelve false cuando no hay cambios`() {
        val original = MoldeProductos(
            nombre = "Ibuprofeno 400mg",
            categoriaNombre = "General",
            empaque = "Caja"
        )
        val state = EditarProductoUiState(
            originalProduct = original,
            nombre = "Ibuprofeno 400mg",
            tipoProducto = "GENERAL",
            categoriaNombre = "General",
            empaque = "Caja"
        )
        assertFalse(state.isAnyFieldChanged)
    }

    @Test
    fun `esMedicamento se activa con principio activo o refrigeracion`() {
        val stateConPrincipio = EditarProductoUiState(
            tipoProducto = "GENERAL",
            principioActivo = "Amoxicilina"
        )
        assertTrue(stateConPrincipio.esMedicamento)

        val stateRefrigerado = EditarProductoUiState(
            tipoProducto = "GENERAL",
            esRefrigerado = true
        )
        assertTrue(stateRefrigerado.esMedicamento)
    }
}
