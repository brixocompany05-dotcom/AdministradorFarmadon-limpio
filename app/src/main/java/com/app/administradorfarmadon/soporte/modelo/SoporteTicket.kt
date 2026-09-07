package com.app.administradorfarmadon.soporte.modelo

/**
 * Mensaje individual dentro de una conversación de soporte.
 * 100% Datos reales: Emisor real, Identificación de usuario y Sede.
 * Incluye gestión de estado de entrega y errores para reintento.
 */
/**
 * Estados oficiales de la conversación (máquina explícita).
 * OPEN -> IN_PROGRESS -> WAITING_CUSTOMER <-> WAITING_BRIXO -> RESOLVED -> CLOSED
 * CLOSED es definitivo e irreversible: solo lectura para ambas partes.
 */
object SoporteEstado {
    const val OPEN = "OPEN"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val WAITING_CUSTOMER = "WAITING_CUSTOMER"
    const val WAITING_BRIXO = "WAITING_BRIXO"
    const val RESOLVED = "RESOLVED"
    const val CLOSED = "CLOSED"

    // Compatibilidad con tickets antiguos.
    const val LEGACY_ABIERTO = "ABIERTO"
    const val LEGACY_EN_PROCESO = "EN_PROCESO"
    const val LEGACY_RESUELTO = "RESUELTO"

    fun normalizar(raw: String?): String {
        return when (raw?.uppercase()) {
            OPEN, IN_PROGRESS, WAITING_CUSTOMER, WAITING_BRIXO, RESOLVED, CLOSED -> raw.uppercase()
            LEGACY_ABIERTO -> OPEN
            LEGACY_EN_PROCESO -> IN_PROGRESS
            LEGACY_RESUELTO -> RESOLVED
            else -> OPEN
        }
    }

    fun esCerrado(estado: String?): Boolean =
        normalizar(estado) == CLOSED || normalizar(estado) == RESOLVED

    fun esCierreDefinitivo(estado: String?): Boolean = normalizar(estado) == CLOSED
}

/** Tipos de mensaje soportados en el historial oficial. */
object SoporteTipoMensaje {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val FILE = "FILE"
    const val SYSTEM_EVENT = "SYSTEM_EVENT"
    const val ACTION_EVENT = "ACTION_EVENT"
    const val ERROR_REPORT = "ERROR_REPORT"
    const val STATUS_CHANGE = "STATUS_CHANGE"
}

/**
 * Asignación (dimensión SEPARADA del estado de la conversación y de la
 * presencia del agente — SOPORTE 2026):
 * UNASSIGNED = en cola esperando agente · ASSIGNED = 1:1 con un agente ·
 * RELEASED = liberada a cola (el historial se conserva, otro agente la toma).
 * Desconectar/cerrar la app NO libera: solo una acción explícita cambia esto.
 */
object SoporteAsignacion {
    const val UNASSIGNED = "UNASSIGNED"
    const val ASSIGNED = "ASSIGNED"
    const val RELEASED = "RELEASED"

    fun normalizar(raw: String?): String = when (raw?.uppercase()) {
        ASSIGNED -> ASSIGNED
        RELEASED -> RELEASED
        else -> UNASSIGNED
    }
}

/** Motivos oficiales para liberar una solicitud (quedan auditados). */
object MotivoLiberacion {
    const val REQUIERE_ESPECIALISTA = "Requiere especialista"
    const val NO_CORRESPONDE = "No corresponde a mi área"
    const val ESCALACION_TECNICA = "Escalación técnica"
    const val CAMBIO_TURNO = "Cambio de turno"
    const val OTRO = "Otro"

    val TODOS = listOf(REQUIERE_ESPECIALISTA, NO_CORRESPONDE, ESCALACION_TECNICA, CAMBIO_TURNO, OTRO)
}

/** Categorías oficiales para crear un caso (spec §6). */
object SoporteCategorias {
    const val POS = "POS"
    const val CAJA = "CAJA"
    const val INVENTARIO = "INVENTARIO"
    const val VENTAS = "VENTAS"
    const val FACTURACION = "FACTURACION_ELECTRONICA"
    const val SUNAT = "SUNAT"
    const val USUARIOS = "USUARIOS"
    const val CONFIGURACION = "CONFIGURACION"
    const val REPORTES = "REPORTES"
    const val RENDIMIENTO = "RENDIMIENTO"
    const val ERROR_TECNICO = "ERROR_TECNICO"
    const val OTRO = "OTRO"

    // Claves legacy que siguen leyéndose sin romperse.
    const val LEGACY_FACTURACION_SUNAT = "FACTURACION_SUNAT"
    const val LEGACY_CAJA_VENTAS = "CAJA_VENTAS"
    const val LEGACY_INVENTARIO_STOCK = "INVENTARIO_STOCK"
    const val LEGACY_HARDWARE = "HARDWARE_IMPRESORA"
    const val LEGACY_GENERAL = "CONSULTA_GENERAL"

    fun etiqueta(categoria: String?): String = when (categoria?.uppercase()) {
        POS -> "POS"
        CAJA, LEGACY_CAJA_VENTAS -> "Caja"
        INVENTARIO, LEGACY_INVENTARIO_STOCK -> "Inventario"
        VENTAS -> "Ventas"
        FACTURACION, LEGACY_FACTURACION_SUNAT -> "Facturación electrónica"
        SUNAT -> "SUNAT"
        USUARIOS -> "Usuarios"
        CONFIGURACION -> "Configuración"
        REPORTES -> "Reportes"
        RENDIMIENTO -> "Rendimiento"
        ERROR_TECNICO -> "Error técnico"
        LEGACY_HARDWARE -> "Impresora"
        else -> "Otro"
    }
}

data class MensajeSoporte(
    val id: String = "",
    val clientMessageId: String = "",
    val conversationId: String = "",
    val emisor: String = "FARMACIA", // "FARMACIA" o "BRIXO" o "SISTEMA"
    val autorNombre: String = "",
    val usuarioEmail: String = "",
    val sucursalId: String = "",
    val sucursalNombre: String = "",
    val clienteId: String = "",
    val texto: String = "",
    val tipo: String = SoporteTipoMensaje.TEXT,
    val archivoNombre: String = "",
    val archivoTamanoBytes: Long = 0L,
    val archivoMime: String = "",
    val errorCodigo: String = "",
    /** Evento estructurado (TAKE/RELEASE/CLOSE/DIAGNOSTIC) + actor, para voz por lado. */
    val evento: String = "",
    val actorId: String = "",
    val fechaMs: Long = 0L,
    val leido: Boolean = false,
    val estadoLectura: String = "LEIDO" // "ENVIADO", "ENTREGADO", "LEIDO", "ERROR"
) {
    val esDeFarmacia: Boolean get() = emisor.equals("FARMACIA", ignoreCase = true)
    val esSistema: Boolean get() = emisor.equals("SISTEMA", ignoreCase = true) ||
        tipo == SoporteTipoMensaje.SYSTEM_EVENT ||
        tipo == SoporteTipoMensaje.STATUS_CHANGE ||
        tipo == SoporteTipoMensaje.ACTION_EVENT
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
    val categoria: String = "OTRO",
    val prioridad: String = "MEDIA",
    val descripcion: String = "",
    val contextoProblema: String = "",
    val contextoAccion: String = "",
    val contextoDesdeCuando: String = "",
    val estado: String = SoporteEstado.OPEN,
    val farmaciaId: String = "",
    val sucursalId: String = "",
    val sucursalNombre: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val usuarioEmail: String = "",
    val responsableAgenteId: String = "",
    val responsableAgenteNombre: String = "",
    val assignmentState: String = SoporteAsignacion.UNASSIGNED,
    val assignedAtMs: Long = 0L,
    val primeraRespuestaMs: Long = 0L,
    val clientConversationId: String = "",
    val relatedConversationId: String = "",
    val errorCodigo: String = "",
    val errorModulo: String = "",
    val errorPantalla: String = "",
    val errorAccion: String = "",
    val appVersion: String = "",
    val deviceInfo: String = "",
    val osVersion: String = "",
    val ultimoMensaje: String = "",
    val mensajesSinLeerFarmacia: Int = 0,
    val cerradoMs: Long = 0L,
    val cerradoPor: String = "",
    val resumenCierre: String = "",
    val respuestaBrixo: String = "",
    val respondidoPor: String = "",
    val fechaMs: Long = 0L,
    val actualizadoMs: Long = 0L,
    val mensajes: List<MensajeSoporte> = emptyList()
) {
    val estadoNormalizado: String get() = SoporteEstado.normalizar(estado)
    val esUrgente: Boolean get() = prioridad.equals("URGENTE", ignoreCase = true) || prioridad.equals("ALTA", ignoreCase = true)
    val estaResuelto: Boolean get() = SoporteEstado.esCerrado(estado)
    val estaCerradoDefinitivo: Boolean get() = SoporteEstado.esCierreDefinitivo(estado)
    /** Regla absoluta de cierre: CLOSED/RESUELTO = solo lectura, sin escribir ni adjuntar. */
    val puedeEscribir: Boolean get() = !estaResuelto
    val asignacionNormalizada: String get() = SoporteAsignacion.normalizar(assignmentState)
    /** true solo cuando hay un agente 1:1 atendiéndola ahora mismo. */
    val tieneAgenteAsignado: Boolean get() =
        asignacionNormalizada == SoporteAsignacion.ASSIGNED && responsableAgenteId.isNotBlank()
    /** Texto honesto y corto para la tarjeta: el quién ya vive en el chat. */
    val textoAtencion: String get() = when {
        estaCerradoDefinitivo -> "Cerrada"
        estaResuelto -> "Resuelta"
        tieneAgenteAsignado -> "En atención"
        else -> "Esperando atención · en cola"
    }

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

        // Deduplicación estricta por ID (incluye clientMessageId) y orden
        // determinista por fecha de servidor + id. Nunca depende del reloj local.
        return listaResultante
            .distinctBy { it.clientMessageId.ifBlank { it.id } }
            .sortedWith(compareBy({ it.fechaMs }, { it.id }))
    }
}

/**
 * Canales oficiales publicados por la central (100% reales).
 * Vacío = la central aún no los publicó: la UI muestra estado vacío honesto,
 * jamás un número/correo inventado.
 */
/**
 * Mezcla pura del historial (testeable sin Android):
 * legacy + subcolección + optimistas, sin el primer mensaje duplicado y sin
 * duplicados por reintento; un optimista desaparece cuando el servidor lo
 * confirma (misma clave). Orden determinista por fecha + id.
 */
fun combinarHistorialSoporte(
    base: List<MensajeSoporte>,
    vivos: List<MensajeSoporte>,
    descripcion: String,
    fechaCasoMs: Long,
    optimistas: List<MensajeSoporte> = emptyList()
): List<MensajeSoporte> {
    val descLimpia = descripcion.trim()
    val baseFiltrada = if (vivos.isNotEmpty() && descLimpia.isNotBlank() && vivos.any {
            it.texto.trim() == descLimpia && it.esDeFarmacia &&
                kotlin.math.abs(it.fechaMs - fechaCasoMs) < 60_000L
        }
    ) {
        base.filterNot { it.id.startsWith("msg_init_") }
    } else base
    if (optimistas.isEmpty()) {
        return (baseFiltrada + vivos)
            .distinctBy { it.clientMessageId.ifBlank { it.id } }
            .sortedWith(compareBy({ it.fechaMs }, { it.id }))
    }
    val confirmadas = (baseFiltrada + vivos)
        .map { it.clientMessageId.ifBlank { it.id } }.toSet()
    val pendientes = optimistas.filterNot { it.clientMessageId.ifBlank { it.id } in confirmadas }
    return (baseFiltrada + vivos + pendientes)
        .distinctBy { it.clientMessageId.ifBlank { it.id } }
        .sortedWith(compareBy({ it.fechaMs }, { it.id }))
}

/** Anti doble-toque puro: mismo texto dos veces en menos de 1.5 s se ignora. */
fun debeIgnorarDobleEnvio(ultimoTexto: String?, ultimoMs: Long, nuevoTexto: String, ahoraMs: Long): Boolean {
    if (ultimoTexto == null) return false
    return nuevoTexto.trim() == ultimoTexto.trim() && (ahoraMs - ultimoMs) < 1500L
}

data class CanalesSoporteOficial(
    val whatsapp: String = "",
    val email: String = "",
    val horarioAtencion: String = "",
    val telefonoCentral: String = ""
) {
    val tieneWhatsapp: Boolean get() = whatsapp.trim().isNotBlank()
    val tieneEmail: Boolean get() = email.trim().isNotBlank()
}
