package com.app.administradorfarmadon.base_datos

import java.text.Normalizer
import java.util.Locale

object PresentacionHelper {

    fun normalizarClave(texto: String): String {
        if (texto.isBlank()) return ""

        val sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return sinAcentos
            .replace("\\s+".toRegex(), " ")
            .lowercase(Locale.getDefault())
    }

    fun sonNombresEquivalentes(a: String?, b: String?): Boolean {
        return normalizarClave(a.orEmpty()) == normalizarClave(b.orEmpty())
    }

    fun capitalizarNombre(texto: String): String {
        val limpio = texto.trim().replace("\\s+".toRegex(), " ")
        if (limpio.isBlank()) return ""

        return limpio.lowercase(Locale.getDefault())
            .replaceFirstChar { letra ->
                if (letra.isLowerCase()) letra.titlecase(Locale.getDefault()) else letra.toString()
            }
    }

    fun etiquetaCantidadCorta(cantidad: Int, unidadBase: String): String {
        val cantidadSegura = cantidad.coerceAtLeast(1)

        return when (normalizarClave(unidadBase)) {
            "g" -> "$cantidadSegura g"
            "ml" -> "$cantidadSegura mL"
            else -> "$cantidadSegura unid."
        }
    }

    fun resumenPresentacionUi(
        nombreVisible: String,
        cantidad: Int,
        unidadBase: String
    ): String {
        val nombre = capitalizarNombre(nombreVisible).ifBlank { "Presentación" }
        return "$nombre (${etiquetaCantidadCorta(cantidad, unidadBase)})"
    }

    fun formatearPresentacionGuardadaParaUi(textoGuardado: String): String {
        val limpio = textoGuardado.trim().replace("\\s+".toRegex(), " ")
        if (limpio.isBlank()) return "Sin presentación"

        val formatoActual = Regex("^(.*?)[\\s]*\\(([^)]+)\\)$").find(limpio)
        if (formatoActual != null) {
            val nombre = capitalizarNombre(formatoActual.groupValues[1])
            val detalle = formatoActual.groupValues[2].trim()
            return if (detalle.isBlank()) nombre else "$nombre ($detalle)"
        }

        val formatoAnterior = Regex("^(.*?)[\\s]+(\\d+)\\s+([A-Za-zÁí‰íí“íšáéíóúÑñ.]+)$").find(limpio)
        if (formatoAnterior != null) {
            val nombre = capitalizarNombre(formatoAnterior.groupValues[1])
            val cantidad = formatoAnterior.groupValues[2].toIntOrNull() ?: 1
            val unidadCruda = formatoAnterior.groupValues[3].trim()
            val unidadNormalizada = normalizarClave(unidadCruda)

            return when {
                unidadNormalizada == "g" -> resumenPresentacionUi(nombre, cantidad, "g")
                unidadNormalizada == "ml" -> resumenPresentacionUi(nombre, cantidad, "mL")
                unidadNormalizada.startsWith("unid") || unidadNormalizada == "unidad" || unidadNormalizada == "unidades" ->
                    resumenPresentacionUi(nombre, cantidad, "unidad")
                else -> "$nombre ($cantidad $unidadCruda)"
            }
        }

        return capitalizarNombre(extraerNombreBase(limpio)).ifBlank { limpio }
    }

    fun extraerNombreBase(texto: String): String {
        val limpio = texto.trim()
        if (limpio.isBlank()) return ""

        val sinParentesis = limpio.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
        return sinParentesis.replace(Regex("\\s+\\d+\\s+.*$"), "").trim()
    }

    fun normalizarNombreBase(texto: String): String {
        return normalizarClave(extraerNombreBase(texto))
    }

    fun claveFirebaseSegura(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return sinAcentos
            .replace("[.#$\\[\\]/]".toRegex(), " ")
            .replace("[^A-Za-z0-9]+".toRegex(), "_")
            .trim('_')
            .lowercase(Locale.getDefault())
    }
}
