package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

import java.text.Normalizer
import java.util.Locale

object BuscadorIndicesManager {
    fun normalizarTexto(texto: String): String {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.getDefault())
            .trim()
    }
}
