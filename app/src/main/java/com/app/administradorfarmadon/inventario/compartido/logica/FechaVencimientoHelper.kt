package com.app.administradorfarmadon.inventario.compartido.logica

import com.app.administradorfarmadon.compartido.logica.HoraServidor
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object FechaVencimientoHelper {

    fun formatear(mes: Int, anio: Int): String = String.format("%02d/%d", mes.coerceIn(1, 12), anio)

    fun normalizar(raw: String): String? {
        val v = raw.trim()
        if (v.isBlank()) return null
        return try {
            when {
                v.contains("-") -> {
                    val parts = v.split("-")
                    when (parts.size) {
                        3 -> {
                            if (parts[0].length == 4) {
                                val anio = parts[0].toIntOrNull() ?: return null
                                val mes = parts[1].toIntOrNull() ?: return null
                                if (mes !in 1..12) return null
                                formatear(mes, anio)
                            } else {
                                val mes = parts[1].toIntOrNull() ?: return null
                                val anioRaw = parts[2]
                                val anio = if (anioRaw.length == 2) 2000 + (anioRaw.toIntOrNull()
                                    ?: return null) else anioRaw.toIntOrNull() ?: return null
                                if (mes !in 1..12) return null
                                formatear(mes, anio)
                            }
                        }

                        2 -> {
                            if (parts[0].length == 4) {
                                val anio = parts[0].toIntOrNull() ?: return null
                                val mes = parts[1].toIntOrNull() ?: return null
                                if (mes !in 1..12) return null
                                formatear(mes, anio)
                            } else {
                                val mes = parts[0].toIntOrNull() ?: return null
                                val anioRaw = parts[1]
                                val anio = if (anioRaw.length == 2) 2000 + (anioRaw.toIntOrNull()
                                    ?: return null) else anioRaw.toIntOrNull() ?: return null
                                if (mes !in 1..12) return null
                                formatear(mes, anio)
                            }
                        }

                        else -> null
                    }
                }

                v.contains("/") -> {
                    val parts = v.split("/")
                    when (parts.size) {
                        3 -> {
                            val anioRaw = parts[2]
                            val anio = if (anioRaw.length == 2) 2000 + (anioRaw.toIntOrNull()
                                ?: return null) else anioRaw.toIntOrNull() ?: return null
                            val mesFinal = if (parts[0].toIntOrNull()
                                    ?.let { it in 1..31 } == true && parts[1].toIntOrNull() in 1..12
                            ) {
                                parts[1].toInt()
                            } else {
                                parts[0].toInt()
                            }
                            if (mesFinal !in 1..12) return null
                            formatear(mesFinal, anio)
                        }

                        2 -> {
                            val mes = parts[0].toIntOrNull() ?: return null
                            val anioRaw = parts[1]
                            val anio = if (anioRaw.length == 2) 2000 + (anioRaw.toIntOrNull()
                                ?: return null) else anioRaw.toIntOrNull() ?: return null
                            if (mes !in 1..12) return null
                            formatear(mes, anio)
                        }

                        else -> null
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.w("FechaVencimiento", "parse vencimiento falló", e)
            null
        }
    }

    fun diasHastaVencer(vencimiento: String): Int? {
        if (vencimiento.isBlank()) return null
        return try {
            // Hora blindada del servidor (nunca el reloj de la tablet)
            val hoy = java.time.Instant.ofEpochMilli(HoraServidor.ahoraMs())
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val v = vencimiento.trim()
            if (v.contains("-")) {
                val parts = v.split("-")
                val fechaVence = when (parts.size) {
                    3 -> {
                        if (parts[0].length == 4) {
                            LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                        } else {
                            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                        }
                    }

                    2 -> {
                        if (parts[0].length == 4) YearMonth.of(parts[0].toInt(), parts[1].toInt())
                            .atEndOfMonth()
                        else YearMonth.of(parts[1].toInt(), parts[0].toInt()).atEndOfMonth()
                    }

                    else -> return null
                }
                ChronoUnit.DAYS.between(hoy, fechaVence).toInt()
            } else if (v.contains("/")) {
                val parts = v.split("/")
                val fechaVence = when (parts.size) {
                    3 -> {
                        val anio =
                            if (parts[2].length == 2) 2000 + parts[2].toInt() else parts[2].toInt()
                        LocalDate.of(anio, parts[1].toInt(), parts[0].toInt())
                    }

                    2 -> {
                        val mes = parts[0].toInt()
                        val anio =
                            if (parts[1].length == 2) 2000 + parts[1].toInt() else parts[1].toInt()
                        YearMonth.of(anio, mes).atEndOfMonth()
                    }

                    else -> return null
                }
                ChronoUnit.DAYS.between(hoy, fechaVence).toInt()
            } else null
        } catch (e: Exception) {
            android.util.Log.w("FechaVencimiento", "parse vencimiento falló", e)
            null
        }
    }

    fun llaveLote(numero: String): String {
        return numero.trim().uppercase().replace(".", "__DOT__").replace("#", "__HASH__")
            .replace("$", "__DOLLAR__").replace("[", "__LB__").replace("]", "__RB__")
            .replace("/", "__SLASH__").replace(" ", "__SPACE__")
    }

    fun llaveLoteLegada(numero: String): String {
        return numero.trim().uppercase().replace("/", "-").replace(" ", "_")
    }


    /**
     * Fuente única para resolver un lote por su número.
     * Busca en orden: llave actual → llave legada → scan por campo numero (ignora mayúsculas/espacios).
     * Retorna Pair(claveReal, dataMutable) o null si no existe. Robusto y sin repetición.
     */
    fun resolverLote(
        lotesMap: MutableMap<Any?, Any?>, numero: String
    ): Pair<String, MutableMap<String, Any>>? {
        if (numero.isBlank()) return null
        val cleanKey = llaveLote(numero)
        val legacyKey = llaveLoteLegada(numero)

        val data0 = lotesMap[cleanKey] as? Map<*, *>
        if (data0 != null) {
            @Suppress("UNCHECKED_CAST") return Pair(
                cleanKey,
                (data0 as Map<String, Any>).toMutableMap()
            )
        }
        val data1 = lotesMap[legacyKey] as? Map<*, *>
        if (data1 != null) {
            @Suppress("UNCHECKED_CAST") return Pair(
                legacyKey,
                (data1 as Map<String, Any>).toMutableMap()
            )
        }
        val entry = lotesMap.entries.firstOrNull { e ->
            val m = e.value as? Map<*, *>
            val num = (m?.get("numero") as? String) ?: e.key.toString()
            num.trim().equals(numero.trim(), ignoreCase = true)
        }
        if (entry != null) {
            val k = entry.key.toString()
            val m = entry.value as? Map<*, *> ?: return null
            @Suppress("UNCHECKED_CAST") return Pair(k, (m as Map<String, Any>).toMutableMap())
        }
        return null
    }

    fun claveRealLote(lotesMap: Map<*, *>, numero: String, cleanKey: String): String {
        if (lotesMap.containsKey(cleanKey)) return cleanKey
        val legacy = llaveLoteLegada(numero)
        if (lotesMap.containsKey(legacy)) return legacy
        val found = lotesMap.entries.firstOrNull { e ->
            val m = e.value as? Map<*, *>
            val num = (m?.get("numero") as? String) ?: e.key.toString()
            num.trim().equals(numero.trim(), ignoreCase = true)
        }
        return found?.key?.toString() ?: cleanKey
    }

    fun vencimientoMasCercano(lotesMap: Map<*, *>): String {
        return lotesMap.values.mapNotNull { it as? Map<*, *> }.filter {
                val cDisp = (it["cantidad"] as? Number)?.toDouble() ?: 0.0
                cDisp > 0 && (it["vencimiento"] as? String)?.isNotBlank() == true
            }.minByOrNull { diasHastaVencer(it["vencimiento"] as? String ?: "") ?: Int.MAX_VALUE }
            ?.get("vencimiento") as? String ?: ""
    }

    fun timestampDeVencimiento(vencimiento: String): Long {
        if (vencimiento.isBlank() || vencimiento == "—") return 0L
        return try {
            val v = vencimiento.trim()
            val yearMonth = when {
                v.contains("-") -> {
                    val parts = v.split("-")
                    when (parts.size) {
                        2 -> if (parts[0].length == 4) java.time.YearMonth.of(
                            parts[0].toInt(), parts[1].toInt()
                        ) else java.time.YearMonth.of(parts[1].toInt(), parts[0].toInt())

                        else -> return 0L
                    }
                }

                v.contains("/") -> {
                    val parts = v.split("/")
                    when (parts.size) {
                        2 -> {
                            val mes = parts[0].toInt()
                            val anioRaw = parts[1]
                            val anio =
                                if (anioRaw.length == 2) 2000 + anioRaw.toInt() else anioRaw.toInt()
                            java.time.YearMonth.of(anio, mes)
                        }

                        else -> return 0L
                    }
                }

                else -> return 0L
            }
            yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (e: Exception) {
            android.util.Log.w("FechaVencimiento", "timestamp vencimiento falló", e)
            0L
        }
    }
}
