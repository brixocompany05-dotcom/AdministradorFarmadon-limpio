package com.app.administradorfarmadon.inventario.crearproductogeneral.logica

/**
 * Filtro estructural mínimo. Atrapa SOLO basura obvia
 * (vacío, símbolos, saturación de un carácter).
 * NO decide si es producto farmacéutico —” eso lo hace la IA Cloud.
 * NO aplica reglas fonéticas a nombres cortos, para no bloquear marcas legítimas.
 */
object StructuralProductValidator {

    fun detectarBasuraEvidente(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false

        // 1. Puro símbolo (sin letras ni dígitos): "###", "---", "..."
        if (s.none { it.isLetterOrDigit() }) return true

        // 2. Saturación: >=70% mismo carácter, longitud >=4: "zzzzz", "aaaaaa"
        if (s.length >= 4) {
            val maxFrec = s.groupingBy { it }.eachCount().maxOfOrNull { it.value } ?: 0
            if (maxFrec >= s.length * 0.7) return true
        }

        // 3. Teclazo adyacente puro (qwerty|asdfgh|zxcvbn) sin otros chars
        if (esTeclazoPuro(s)) return true

        return false
    }

    private fun esTeclazoPuro(s: String): Boolean {
        if (s.length < 5) return false
        val lower = s.lowercase()
        val patrones = listOf("qwerty", "asdfgh", "zxcvbn", "qazwsx")
        return patrones.any { p ->
            lower.contains(p) && lower.length <= p.length + 2
        }
    }
}
