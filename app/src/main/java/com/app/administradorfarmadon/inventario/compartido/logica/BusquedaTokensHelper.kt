package com.app.administradorfarmadon.inventario.compartido.logica

/**
 * Helper para generación de tokens de búsqueda (Edge N-grams).
 * Genera prefijos desde la 1era letra para permitir búsquedas
 * instantáneas y parciales ("c", "co", "col", "cola") en Cloud Firestore.
 */
object BusquedaTokensHelper {

    /**
     * Genera una lista de tokens de prefijos para indexar en busquedaTokens.
     * Ejemplo: "Coca Cola" -> ["c", "co", "coc", "coca", "col", "cola"]
     */
    fun generarTokens(texto: String): List<String> {
        if (texto.isBlank()) return emptyList()
        val palabras = texto.lowercase().trim()
            .split(Regex("[^a-zA-Z0-9áéíóúñÁÉÍÓÚÑ_-]+"))
            .filter { it.isNotBlank() }

        val tokens = mutableSetOf<String>()
        for (palabra in palabras) {
            var prefijo = ""
            for (char in palabra) {
                prefijo += char
                if (prefijo.length <= 30) {
                    tokens.add(prefijo)
                }
            }
        }
        return tokens.toList()
    }
}
