package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Fuente de verdad canónica para condiciones de temperatura y conservación de fármacos (v2026).
 * Almacenado como códigos estándar en Firebase: `temperaturaAlmacenamiento`.
 */
object CondicionTemperatura {
    const val AMBIENTE = "AMBIENTE"
    const val REFRIGERADO_2_8C = "REFRIGERADO_2_8C"
    const val CONGELADO = "CONGELADO"
    const val SENSIBLE_LUZ = "SENSIBLE_LUZ"
    const val SENSIBLE_HUMEDAD = "SENSIBLE_HUMEDAD"

    val OPCIONES_ETIQUETAS = listOf(
        "Temperatura Ambiente (15-25°C)",
        "Refrigerado (2-8°C)",
        "Congelado (-18°C o menor)",
        "Protegido de la Luz",
        "Protegido de la Humedad"
    )

    fun requiereRefrigeracion(code: String?): Boolean {
        if (code == null) return false
        val clean = code.trim().uppercase()
        return clean == REFRIGERADO_2_8C || clean == CONGELADO || clean == "REFRIGERADO"
    }

    fun etiqueta(code: String?): String {
        if (code.isNullOrBlank()) return ""
        return when (code.trim().uppercase()) {
            AMBIENTE -> "Temperatura Ambiente (15-25°C)"
            REFRIGERADO_2_8C, "REFRIGERADO" -> "Refrigerado (2-8°C)"
            CONGELADO -> "Congelado (-18°C o menor)"
            SENSIBLE_LUZ -> "Protegido de la Luz"
            SENSIBLE_HUMEDAD -> "Protegido de la Humedad"
            else -> ""
        }
    }

    fun codeDesdeEtiqueta(label: String?): String {
        if (label.isNullOrBlank()) return ""
        val clean = label.trim()
        return when {
            clean.contains("Ambiente", ignoreCase = true) || clean.contains("15-25", ignoreCase = true) -> AMBIENTE
            clean.contains("2-8", ignoreCase = true) || clean.contains("Refrigerado", ignoreCase = true) -> REFRIGERADO_2_8C
            clean.contains("Congelado", ignoreCase = true) -> CONGELADO
            clean.contains("Luz", ignoreCase = true) -> SENSIBLE_LUZ
            clean.contains("Humedad", ignoreCase = true) -> SENSIBLE_HUMEDAD
            else -> ""
        }
    }
}
