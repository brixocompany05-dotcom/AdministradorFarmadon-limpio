package com.app.administradorfarmadon.ActivityInventario.dominio

import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios de [CodigoBarraHelper].
 *
 * Valida las funciones puras de formato y limpieza de códigos de barras.
 * Sin dependencias de Android, Firebase ni corrutinas.
 */
class BarcodeValidatorTest {

    @Test
    fun `limpiar elimina caracteres invalidos y pasa a mayusculas`() {
        assertEquals("7750123456789", CodigoBarraHelper.limpiar(" 7750123456789 "))
        assertEquals("ABC123XYZ", CodigoBarraHelper.limpiar("abc-123_xyz!@#").replace("-", "").replace("_", ""))
        assertEquals("7750123456789-B10", CodigoBarraHelper.limpiar("7750123456789-b10"))
    }

    @Test
    fun `baseSinSufijo extrae el codigo base de fracciones`() {
        assertEquals("7750123456789", CodigoBarraHelper.baseSinSufijo("7750123456789-B10"))
        assertEquals("7750123456789", CodigoBarraHelper.baseSinSufijo("7750123456789-U1"))
        assertEquals("7750123456789", CodigoBarraHelper.baseSinSufijo("7750123456789"))
    }

    @Test
    fun `esValido valida correctamente`() {
        assertTrue(CodigoBarraHelper.esValido("7750123456789"))
        assertFalse(CodigoBarraHelper.esValido(""))
        assertFalse(CodigoBarraHelper.esValido("   "))
    }
}
