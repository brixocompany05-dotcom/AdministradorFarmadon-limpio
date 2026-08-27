package com.app.administradorfarmadon.base_datos

object FirestoreFieldUtils {
    /**
     * Parsea un valor de Firestore a Boolean de forma robusta.
     * Soporta:
     * - Boolean (true/false)
     * - Number (1 -> true, 0 -> false)
     * - String ("true"/"1" -> true)
     */
    fun parseBoolean(valor: Any?, defaultValue: Boolean = false): Boolean {
        return when (valor) {
            is Boolean -> valor
            is Number -> valor.toInt() != 0
            is String -> {
                val normalizado = valor.trim().lowercase()
                normalizado == "true" || normalizado == "1"
            }
            else -> defaultValue
        }
    }

    /**
     * Parsea un valor de Firestore a Long de forma robusta.
     * Soporta:
     * - Number
     * - String
     * - Timestamp
     * - Date
     */
    fun parseLong(valor: Any?, defaultValue: Long = 0L): Long {
        return when (valor) {
            is Number -> valor.toLong()
            is String -> valor.trim().toLongOrNull() ?: defaultValue
            is com.google.firebase.Timestamp -> valor.toDate().time
            is java.util.Date -> valor.time
            else -> defaultValue
        }
    }
}
