package com.app.administradorfarmadon.soporte.modelo

/**
 * Mensaje individual dentro de una conversación de soporte.
 * 100% Datos reales: Emisor real, Identificación de usuario y Sede.
 * Incluye gestión de estado de entrega y errores para reintento.
 */
data class MensajeSoporte(
    val id: String = "",
    val emisor: String = "FARMACIA", // "FARMACIA" o "BRIXO"
    val autorNombre: String = "",
    val usuarioEmail: String = "",
    val sucursalId: String = "",
    val sucursalNombre: String = "",
    val clienteId: String = "",
    val texto: String = "",
    val fechaMs: Long = 0L,
    val leido: Boolean = false,
    val estadoLectura: String = "LEIDO" // "ENVIADO", "ENTREGADO", "LEIDO", "ERROR"
) {
    val esDeFarmacia: Boolean get() = emisor.equals("FARMACIA", ignoreCase = true)
    val tieneError: Boolean get() = estadoLectura.equals("ERROR", ignoreCase = true)
}

/**
 * Modelo de Ticket de Soporte e Incidencia (Enterprise SaaS 2026).
 *
 * R1: Cada farmacia ve y gestiona ÚNICAMENTE sus propios tickets.
 * R3: Cero fallos silenciosos, cada solicitud queda registrada con fecha exacta.
 * R12: Cero datos de relleno, cero respuestas simuladas o maquillaje.
 */
data class SoporteTicket(
    val id: String = "",
    val numeroTicket: String = "",
    val asunto: String = "",
    val categoria: String = "CONSULTA_GENERAL",
    val prioridad: String = "MEDIA",
    val descripcion: String = "",
    val estado: String = "ABIERTO", // ABIERTO, EN_PROCESO, RESUELTO
    val sucursalId: String = "",
    val sucursalNombre: String = "",
    val usuarioNombre: String = "",
    val usuarioEmail: String = "",
    val respuestaBrixo: String = "",
    val respondidoPor: String = "",
    val fechaMs: Long = 0L,
    val actualizadoMs: Long = 0L,
    val mensajes: List<MensajeSoporte> = emptyList()
) {
    val esUrgente: Boolean get() = prioridad.equals("URGENTE", ignoreCase = true) || prioridad.equals("ALTA", ignoreCase = true)
    val estaResuelto: Boolean get() = estado.equals("RESUELTO", ignoreCase = true)

    /**
     * Retorna únicamente los mensajes REALES ordenados cronológicamente sin duplicados.
     */
    fun obtenerTodosLosMensajes(): List<MensajeSoporte> {
        val listaResultante = mutableListOf<MensajeSoporte>()
        
        if (descripcion.isNotBlank()) {
            listaResultante.add(
                MensajeSoporte(
                    id = "msg_init_$id",
                    emisor = "FARMACIA",
                    autorNombre = usuarioNombre,
                    usuarioEmail = usuarioEmail,
                    sucursalId = sucursalId,
                    sucursalNombre = sucursalNombre,
                    texto = descripcion,
                    fechaMs = fechaMs,
                    leido = true,
                    estadoLectura = if (respuestaBrixo.isNotBlank() || mensajes.any { !it.esDeFarmacia }) "LEIDO" else "ENTREGADO"
                )
            )
        }

        listaResultante.addAll(mensajes)

        if (respuestaBrixo.isNotBlank() && mensajes.none { it.texto.trim() == respuestaBrixo.trim() }) {
            listaResultante.add(
                MensajeSoporte(
                    id = "msg_brixo_resp_$id",
                    emisor = "BRIXO",
                    autorNombre = respondidoPor.ifBlank { "BRIXO Soporte Técnico" },
                    texto = respuestaBrixo,
                    fechaMs = if (actualizadoMs > fechaMs) actualizadoMs else fechaMs + 60000L,
                    leido = true,
                    estadoLectura = "LEIDO"
                )
            )
        }

        // Deduplicación estricta por ID y orden cronológico por fecha de servidor
        return listaResultante.distinctBy { it.id }.sortedBy { it.fechaMs }
    }
}

data class CanalesSoporteOficial(
    val whatsapp: String = "51900000000",
    val email: String = "soporte@brixo.pe",
    val horarioAtencion: String = "Lunes a Sábado: 8:00 AM - 10:00 PM · Emergencias 24/7",
    val telefonoCentral: String = "(01) 700-BRIXO"
)
