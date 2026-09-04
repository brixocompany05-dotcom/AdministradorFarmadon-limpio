package com.app.administradorfarmadon.inventario.detallesdelproductoinventario

import com.app.administradorfarmadon.inventario.compartido.modelo.ExpedienteReclamoProveedor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReclamoProveedorTest {

    @Test
    fun `un reclamo recien creado nace en estado EN_REVISION_DROGUERIA`() {
        val reclamo = ExpedienteReclamoProveedor(
            id = "rec-1",
            productoNombre = "Paracetamol 500mg",
            loteNumero = "L100",
            cantidadDevuelta = 10.0,
            motivo = "Vencimiento corto"
        )

        assertEquals("EN_REVISION_DROGUERIA", reclamo.estado)
        assertEquals("Paracetamol 500mg", reclamo.productoNombre)
        assertEquals(10.0, reclamo.cantidadDevuelta, 0.001)
    }

    @Test
    fun `un reclamo resuelto contiene su observacion y fecha de resolucion`() {
        val reclamo = ExpedienteReclamoProveedor(
            id = "rec-2",
            estado = "APROBADO_NOTA_CREDITO",
            fechaResolucionStr = "04/09/2026 10:30",
            observacionesResolucion = "Nota de crédito emitida por droguería"
        )

        assertEquals("APROBADO_NOTA_CREDITO", reclamo.estado)
        assertEquals("04/09/2026 10:30", reclamo.fechaResolucionStr)
        assertEquals("Nota de crédito emitida por droguería", reclamo.observacionesResolucion)
    }

    @Test
    fun `un reclamo rechazado conserva su motivo obligatorio`() {
        val reclamo = ExpedienteReclamoProveedor(
            id = "rec-3",
            estado = "RECHAZADO_DISPUTA",
            observacionesResolucion = "Droguería indica que producto venció fuera del plazo de garantía"
        )

        assertEquals("RECHAZADO_DISPUTA", reclamo.estado)
        assertTrue(reclamo.observacionesResolucion.length >= 5)
    }
}
