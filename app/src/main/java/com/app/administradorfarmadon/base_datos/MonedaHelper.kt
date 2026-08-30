package com.app.administradorfarmadon.base_datos

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager

/**
 * Utility for uniform currency formatting throughout the app.
 * Uses SessionManager configuration for symbol and currency code.
 */
object MonedaHelper {

    private val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    private fun simbolo(): String = SessionManager.monedaSimbolo.ifBlank { "S/" }
    private fun codigo(): String = SessionManager.monedaCodigo.ifBlank { "PEN" }

    /**
     * Normaliza una cadena monetaria eliminando el símbolo de moneda, espacios
     * y convirtiendo coma decimal a punto, sin convertir a número.
     * íštil cuando se necesita el texto limpio (ej. para guardar en Firebase).
     */
    fun normalizarTexto(texto: String): String {
        return texto
            .replace(Regex("[^\\d,.-]"), "")
            .replace(",", ".")
            .trim()
    }

    fun parsear(texto: String): Double? {
        // Eliminar todo lo que no sea digito, punto o coma.
        val limpio = texto
            .replace(Regex("[^\\d,.-]"), "")
            .replace(",", ".")

        if (limpio.isBlank() || limpio == "-" || limpio == "." || limpio == "-.") {
            return null
        }

        // Conserva solo el primer punto decimal y descarta puntos extra sin
        // usar look-behind variable, porque ese patron rompe en Android.
        val negativo = limpio.startsWith("-")
        val cuerpo = limpio.removePrefix("-")
        val primerPunto = cuerpo.indexOf('.')

        val normalizado = if (primerPunto >= 0) {
            val entero = cuerpo.substring(0, primerPunto + 1)
            val decimal = cuerpo.substring(primerPunto + 1).replace(".", "")
            entero + decimal
        } else {
            cuerpo
        }

        return ((if (negativo) "-" else "") + normalizado).toDoubleOrNull()
    }

    /** Simbolo de la moneda activa (ej. "S/", "$"). Util para prefijos de inputs. */
    fun simboloMoneda(): String = simbolo()

    /**
     * Formats a double value: "S/ 1,250.00 (PEN)"
     */
    fun formatear(monto: Any?): String {
        val valor = when (monto) {
            is Number -> monto.toDouble()
            is String -> monto.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return "${simbolo()} ${format.format(valor)} (${codigo()})"
    }

    /**
     * Formats with sign for balance/movements: "+ S/ 50.00" or "- S/ 20.00"
     */
    fun formatearConSigno(monto: Double): String {
        val signo = when {
            monto > 0.009 -> "+ "
            monto < -0.009 -> "- "
            else -> ""
        }
        return "$signo${simbolo()} ${format.format(abs(monto))}"
    }

    /**
     * Simple format without currency code: "S/ 1,250.00"
     */
    fun formatearSimple(monto: Double): String {
        return "${simbolo()} ${format.format(monto)}"
    }

    /**
     * Numeric format without currency symbol/code: "1,250.00"
     */
    fun formatearNumero(monto: Any?): String {
        val valor = when (monto) {
            is Number -> monto.toDouble()
            is String -> monto.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return format.format(valor)
    }
}
