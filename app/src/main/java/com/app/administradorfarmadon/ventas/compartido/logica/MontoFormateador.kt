package com.app.administradorfarmadon.ventas.compartido.logica

/**
 * Normalizador y validador de montos monetarios (R3/R10).
 * Maneja entradas en formato peruano e internacional con coma o punto decimal ("12,50" o "12.50").
 */
object MontoFormateador {

    /**
     * Convierte una cadena de texto a Double soportando comas y puntos.
     * Retorna null si el texto está vacío, no es numérico, o es NaN/Infinito.
     */
    fun normalizarMonto(texto: String): Double? {
        val limpio = texto.trim()
            .replace(" ", "")
            .replace("S/", "")
            .replace("$", "")
            .replace(",", ".")
        if (limpio.isBlank()) return null
        val valor = limpio.toDoubleOrNull() ?: return null
        return if (valor.isNaN() || valor.isInfinite()) null else valor
    }

    /**
     * Convierte una cadena de texto a Double positivo o cero.
     * Retorna null si es inválido o negativo.
     */
    fun normalizarMontoPositivo(texto: String): Double? {
        val monto = normalizarMonto(texto) ?: return null
        return if (monto >= 0.0) monto else null
    }

    /**
     * Convierte una cadena de texto a Double estrictamente mayor a cero.
     * Retorna null si es inválido, cero o negativo.
     */
    fun normalizarMontoEstricto(texto: String): Double? {
        val monto = normalizarMonto(texto) ?: return null
        return if (monto > 0.0) monto else null
    }
}
