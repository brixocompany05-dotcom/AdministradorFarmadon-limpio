package com.app.administradorfarmadon.facturacion.configuracion.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmisorFiscalTest {

    @Test
    fun estaCompleta_todosLosCamposValidosYVerificadoOk_retornaTrue() {
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "FARMACIA FARMAVIDA S.A.C.",
            direccionFiscal = "AV. LARCO 456, MIRAFLORES, LIMA",
            personaId = "pid_test_12345",
            personaToken = "tok_secret_99999",
            modo = EmisorFiscal.MODO_DESARROLLO,
            verificadoOk = true
        )

        assertTrue(emisor.estaCompleta)
        assertTrue(emisor.camposFaltantes().isEmpty())
    }

    @Test
    fun estaCompleta_sinVerificadoOk_retornaFalse() {
        // Regla 3: Si los datos están completos pero el ping fiscal no ha pasado, NUNCA está completa
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "FARMACIA FARMAVIDA S.A.C.",
            direccionFiscal = "AV. LARCO 456, MIRAFLORES, LIMA",
            personaId = "pid_test_12345",
            personaToken = "tok_secret_99999",
            modo = EmisorFiscal.MODO_DESARROLLO,
            verificadoOk = false
        )

        assertFalse(emisor.estaCompleta)
        assertTrue(emisor.camposFaltantes().contains("Verificación de conexión con APISUNAT"))
    }

    @Test
    fun estaCompleta_rucInvalido_retornaFalse() {
        // RUC de 10 dígitos
        val emisor1 = EmisorFiscal(
            ruc = "2060123456",
            razonSocial = "FARMACIA FARMAVIDA S.A.C.",
            direccionFiscal = "AV. LARCO 456",
            personaId = "pid_1",
            personaToken = "tok_1",
            verificadoOk = true
        )
        assertFalse(emisor1.estaCompleta)

        // RUC con letras
        val emisor2 = EmisorFiscal(
            ruc = "2060123456A",
            razonSocial = "FARMACIA FARMAVIDA S.A.C.",
            direccionFiscal = "AV. LARCO 456",
            personaId = "pid_1",
            personaToken = "tok_1",
            verificadoOk = true
        )
        assertFalse(emisor2.estaCompleta)
    }

    @Test
    fun estaCompleta_faltaToken_retornaFalseYReportaFaltante() {
        val emisor = EmisorFiscal(
            ruc = "20601234567",
            razonSocial = "FARMACIA FARMAVIDA S.A.C.",
            direccionFiscal = "AV. LARCO 456",
            personaId = "pid_123",
            personaToken = "",
            verificadoOk = false
        )

        assertFalse(emisor.estaCompleta)
        val faltantes = emisor.camposFaltantes()
        assertTrue(faltantes.contains("Token (APISUNAT)"))
        assertTrue(faltantes.contains("Verificación de conexión con APISUNAT"))
    }
}
