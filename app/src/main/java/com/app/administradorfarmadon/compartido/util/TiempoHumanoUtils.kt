package com.app.administradorfarmadon.compartido.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Formateador conversacional y humano de marcas de tiempo para FARMADON.
 * Convierte cualquier fecha, timestamp o ISO en lenguaje natural en español:
 * - "En línea ahora"
 * - "Hace 2 minutos"
 * - "Hace 1 hora" / "Hace 3 horas (2:15 PM)"
 * - "Ayer a las 4:30 PM"
 * - "Hace 3 días (Mié 19 de Ago, 3:00 PM)"
 * - "27 de agosto a las 3:15 PM"
 * - "15 de marzo de 2025 a las 11:30 AM"
 */
object TiempoHumanoUtils {

    private val isoFormatTL = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val horaMinutoTL = ThreadLocal.withInitial {
        SimpleDateFormat("h:mm a", Locale.forLanguageTag("es-PE"))
    }

    private val diaMesHoraTL = ThreadLocal.withInitial {
        SimpleDateFormat("d 'de' MMMM 'a las' h:mm a", Locale.forLanguageTag("es-PE"))
    }

    private val diaMesAnoHoraTL = ThreadLocal.withInitial {
        SimpleDateFormat("d 'de' MMMM, yyyy 'a las' h:mm a", Locale.forLanguageTag("es-PE"))
    }

    private val diaSemanaCortoTL = ThreadLocal.withInitial {
        SimpleDateFormat("EEE d 'de' MMM", Locale.forLanguageTag("es-PE"))
    }

    fun parseTimestampMs(valor: Any?): Long {
        if (valor == null) return 0L
        return when (valor) {
            is Timestamp -> valor.toDate().time
            is Date -> valor.time
            is Long -> valor
            is Number -> valor.toLong()
            is String -> {
                if (valor.isBlank()) return 0L
                try {
                    isoFormatTL.get()?.parse(valor)?.time
                        ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(valor)?.time
                        ?: 0L
                } catch (_: Exception) {
                    0L
                }
            }
            else -> 0L
        }
    }

    /**
     * Formatea una marca de tiempo en una cadena humana conversacional en español.
     */
    fun formatear(valor: Any?): String {
        val ms = parseTimestampMs(valor)
        if (ms <= 0L) return "Sin registro de actividad"

        val ahora = System.currentTimeMillis()
        val diffMs = ahora - ms
        if (diffMs < 0L) return "En línea ahora"

        val diffMinutos = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val diffHoras = TimeUnit.MILLISECONDS.toHours(diffMs)
        val diffDias = TimeUnit.MILLISECONDS.toDays(diffMs)

        val calFecha = Calendar.getInstance().apply { timeInMillis = ms }
        val calHoy = Calendar.getInstance().apply { timeInMillis = ahora }
        val calAyer = Calendar.getInstance().apply {
            timeInMillis = ahora
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val esMismoDia = calFecha.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                calFecha.get(Calendar.DAY_OF_YEAR) == calHoy.get(Calendar.DAY_OF_YEAR)

        val esAyer = calFecha.get(Calendar.YEAR) == calAyer.get(Calendar.YEAR) &&
                calFecha.get(Calendar.DAY_OF_YEAR) == calAyer.get(Calendar.DAY_OF_YEAR)

        val esMismoAno = calFecha.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR)
        val horaStr = horaMinutoTL.get()?.format(Date(ms)) ?: ""

        return when {
            diffMinutos <= 1L -> "En línea ahora"
            diffMinutos < 60L -> "Hace $diffMinutos minutos"
            esMismoDia -> if (diffHoras == 1L) "Hace 1 hora ($horaStr)" else "Hace $diffHoras horas ($horaStr)"
            esAyer -> "Ayer a las $horaStr"
            diffDias in 2..6 -> {
                val diaCorto = diaSemanaCortoTL.get()?.format(Date(ms))?.replaceFirstChar { it.uppercase() } ?: ""
                "Hace $diffDias días ($diaCorto, $horaStr)"
            }
            esMismoAno -> diaMesHoraTL.get()?.format(Date(ms)) ?: "Hace $diffDias días"
            else -> diaMesAnoHoraTL.get()?.format(Date(ms)) ?: "Hace $diffDias días"
        }
    }

    /**
     * Versión compacta para listas, tickets y notificaciones.
     */
    fun formatearCorto(valor: Any?): String {
        val ms = parseTimestampMs(valor)
        if (ms <= 0L) return "Sin registro"

        val ahora = System.currentTimeMillis()
        val diffMs = ahora - ms
        if (diffMs < 0L) return "En línea ahora"

        val diffMinutos = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val diffHoras = TimeUnit.MILLISECONDS.toHours(diffMs)
        val diffDias = TimeUnit.MILLISECONDS.toDays(diffMs)

        val calFecha = Calendar.getInstance().apply { timeInMillis = ms }
        val calHoy = Calendar.getInstance().apply { timeInMillis = ahora }
        val calAyer = Calendar.getInstance().apply {
            timeInMillis = ahora
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val esMismoDia = calFecha.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                calFecha.get(Calendar.DAY_OF_YEAR) == calHoy.get(Calendar.DAY_OF_YEAR)

        val esAyer = calFecha.get(Calendar.YEAR) == calAyer.get(Calendar.YEAR) &&
                calFecha.get(Calendar.DAY_OF_YEAR) == calAyer.get(Calendar.DAY_OF_YEAR)

        val esMismoAno = calFecha.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR)
        val horaStr = horaMinutoTL.get()?.format(Date(ms)) ?: ""

        return when {
            diffMinutos <= 1L -> "En línea ahora"
            diffMinutos < 60L -> "Hace $diffMinutos min"
            esMismoDia -> "Hoy a las $horaStr"
            esAyer -> "Ayer a las $horaStr"
            diffDias in 2..6 -> "Hace $diffDias días"
            esMismoAno -> SimpleDateFormat("d 'de' MMM, h:mm a", Locale.forLanguageTag("es-PE")).format(Date(ms))
            else -> SimpleDateFormat("d/MM/yyyy h:mm a", Locale.forLanguageTag("es-PE")).format(Date(ms))
        }
    }
}
