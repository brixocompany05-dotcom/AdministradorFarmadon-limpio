package com.app.administradorfarmadon.inventario.ingresarstockproductos.logica

/**
 * Parser Robusto de Estándares Farmacéuticos GS1 DataMatrix (2D) y Códigos de Barras.
 * Decodifica Identificadores de Aplicación (AI) internacionales:
 * - (01) GTIN / EAN-13 (14 dígitos)
 * - (17) Fecha de Caducidad AAMMDD (6 dígitos)
 * - (10) Número de Lote (Longitud variable)
 * - (21) Número de Serie (Opcional)
 */
data class ParsedGS1Data(
    val gtin: String? = null,
    val lote: String? = null,
    val mesVencimiento: Int? = null,
    val anoVencimiento: Int? = null,
    val rawString: String = ""
)

object GS1DataMatrixParser {

    fun parse(raw: String): ParsedGS1Data {
        val clean = raw.trim()
            .replace("\u0000", "")
            .replace("<GS>", "\u001D")
            .replace("<FNC1>", "\u001D")
            .replace("[GS]", "\u001D")

        if (clean.isBlank()) return ParsedGS1Data(rawString = raw)

        var gtin: String? = null
        var lote: String? = null
        var mes: Int? = null
        var ano: Int? = null

        // 1. CASO A: Formato con Paréntesis estándar de GS1 (ej: (01)07751234567890(17)271231(10)LT2026X9)
        if (clean.contains("(") && clean.contains(")")) {
            val aiRegex = Regex("\\((\\d{2,4})\\)([^()]+)")
            val matches = aiRegex.findAll(clean)

            matches.forEach { match ->
                val ai = match.groupValues[1]
                val value = match.groupValues[2].trim()

                when (ai) {
                    "01" -> gtin = value
                    "10" -> {
                        val endIdx = value.indexOfAny(charArrayOf('\u001D', '\u001E', ' ', '\t'))
                        lote = (if (endIdx != -1) value.substring(0, endIdx) else value).trim().uppercase()
                    }
                    "17" -> {
                        val parsedDate = parseDateAAMMDD(value)
                        if (parsedDate != null) {
                            mes = parsedDate.first
                            ano = parsedDate.second
                        }
                    }
                }
            }
        }

        // 2. CASO B: Formato Raw sin paréntesis con separadores FNC1 / GS (\u001D) o corrido
        if (lote == null && (clean.startsWith("01") || clean.startsWith("]d2") || clean.startsWith("]Q3") || clean.contains("\u001D"))) {
            var working = clean.removePrefix("]d2").removePrefix("]Q3").removePrefix("]e0")
            
            // Si inicia con GTIN (01) de 14 dígitos fijos
            if (working.startsWith("01") && working.length >= 16) {
                gtin = working.substring(2, 16)
                working = working.substring(16)
            }

            // Buscar fecha (17) de 6 dígitos fijos (AAMMDD)
            val idx17 = working.indexOf("17")
            if (idx17 != -1 && working.length >= idx17 + 8) {
                val dateStr = working.substring(idx17 + 2, idx17 + 8)
                val parsedDate = parseDateAAMMDD(dateStr)
                if (parsedDate != null) {
                    mes = parsedDate.first
                    ano = parsedDate.second
                    working = working.substring(0, idx17) + working.substring(idx17 + 8)
                }
            }

            // Buscar lote (10) de longitud variable
            val idx10 = working.indexOf("10")
            if (idx10 != -1) {
                val sub = working.substring(idx10 + 2)
                val endIdx = sub.indexOfAny(charArrayOf('\u001D', '\u001E', '(', ' ', '\t'))
                lote = if (endIdx != -1) sub.substring(0, endIdx).trim().uppercase() else sub.trim().uppercase()
            }
        }

        // 3. CASO C: Fallback para Códigos de Barras Simples — acepta también lotes solo numéricos puros
        if (lote == null && !clean.contains("(") && clean.length in 3..35) {
            val esLoteDirecto = clean.any { it.isLetter() } || clean.contains("-") || clean.contains("_") || clean.all { it.isDigit() }
            if (esLoteDirecto) {
                lote = clean.uppercase()
            }
        }

        return ParsedGS1Data(
            gtin = gtin,
            lote = lote,
            mesVencimiento = mes,
            anoVencimiento = ano,
            rawString = clean
        )
    }

    private fun parseDateAAMMDD(rawDate: String): Pair<Int, Int>? {
        if (rawDate.length < 6) return null
        val clean = rawDate.filter { it.isDigit() }
        if (clean.length < 6) return null

        val yy = clean.substring(0, 2).toIntOrNull() ?: return null
        val mm = clean.substring(2, 4).toIntOrNull() ?: return null
        // En farmacia, si el día viene en 00 (ej: 271200) significa "cualquier día del mes 12 del 2027"

        if (mm !in 1..12) return null
        val fullYear = 2000 + yy

        return Pair(mm, fullYear)
    }
}
