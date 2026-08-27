package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.BuscadorIndicesManager.normalizarTexto

class ProductSimilarityResolver {

    /**
     * Distancia de Levenshtein clásica entre dos cadenas.
     * Número mínimo de inserciones, eliminaciones o sustituciones.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // eliminación
                    dp[i][j - 1] + 1,       // inserción
                    dp[i - 1][j - 1] + cost // sustitución
                )
            }
        }
        return dp[len1][len2]
    }

    /**
     * Devuelve el nombre original del producto más parecido a la consulta,
     * o null si no se encuentra una sugerencia aceptable.
     *
     * @param query       Texto de búsqueda (se normalizará internamente)
     * @param products    Lista completa de productos en inventario
     * @param aliasMap    Mapa de alias normalizados -> nombre canónico original
     */
    fun suggestProduct(
        query: String,
        products: List<PharmProduct>,
        aliasMap: Map<String, String> = emptyMap()
    ): String? {
        val normalizedQuery = normalizarTexto(query)
        if (normalizedQuery.length < 3) return null

        // 1. Verificar si la consulta coincide con algún alias
        aliasMap[normalizedQuery]?.let { aliasCanonico ->
            return aliasCanonico
        }

        // 2. Calcular distancia de Levenshtein contra cada producto
        val umbralMaximo = if (normalizedQuery.length <= 5) 1 else 2
        var mejorDistancia = Int.MAX_VALUE
        var mejorNombreOriginal: String? = null

        for (product in products) {
            val nombreNormalizado = normalizarTexto(product.name)
            val distancia = levenshteinDistance(normalizedQuery, nombreNormalizado)
            if (distancia < mejorDistancia && distancia <= umbralMaximo) {
                mejorDistancia = distancia
                mejorNombreOriginal = product.name
            }
        }

        return mejorNombreOriginal
    }
}
