package com.app.administradorfarmadon.soporte.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.soporte.modelo.CanalesSoporteOficial
import com.app.administradorfarmadon.soporte.modelo.MensajeSoporte
import com.app.administradorfarmadon.soporte.modelo.SoporteAsignacion
import com.app.administradorfarmadon.soporte.modelo.SoporteEstado
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import com.app.administradorfarmadon.soporte.modelo.SoporteTipoMensaje
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

/**
 * Soporte — acceso a datos con blindaje real (no solo UI).
 *
 * Contrato oficial (compartido con BrixoPanel):
 * - farmaciapp/app/farmacias/{farmaciaId}/soporte_tickets/{ticketId}
 * - .../mensajes/{clientMessageId}  (ID = clientMessageId → idempotencia real)
 * - .../auditoria/{auditId}         (append-only)
 *
 * Reglas que este repositorio SÍ hace cumplir en cada escritura:
 * - R1: todo sale de SessionManager.clienteIdGarantizado, nunca de la pantalla.
 * - Cierre absoluto: si estado es CLOSED o RESUELTO, se rechaza escribir/adjuntar.
 * - Idempotencia: reintentar con el mismo clientMessageId no duplica.
 * - Orden: fechaMs viene de HoraServidor (hora corregida por servidor).
 * - R10: jamás serverTimestamp dentro de arreglos; los mensajes son documentos.
 *
 * Lo que NO puede hacer solo el cliente (requiere aprobación del dueño):
 * cambiar firestore.rules. Ver SOPORTE_REGLAS_PROPUESTA.md.
 */
class SoporteRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "SoporteRepository"
        const val MAX_ADJUNTO_BYTES = 10L * 1024L * 1024L // 10 MB
        val MIMES_PERMITIDOS = setOf(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf", "text/plain"
        )
        const val ERROR_CONVERSACION_CERRADA =
            "Esta conversación ya fue cerrada por soporte y quedó como historial de solo lectura. Crea un caso nuevo si necesitas ayuda."

        /** Validación pura (sin Firebase) para poder probarla sin Android. */
        fun validarAdjuntoPuro(nombre: String, tamanoBytes: Long, mime: String): Result<String> {
            if (nombre.isBlank()) return Result.failure(IllegalArgumentException("El archivo no tiene nombre."))
            if (tamanoBytes <= 0) return Result.failure(IllegalArgumentException("El archivo está vacío."))
            if (tamanoBytes > MAX_ADJUNTO_BYTES) {
                return Result.failure(IllegalArgumentException("El archivo supera 10 MB. Envía uno más liviano."))
            }
            val mimeLimpio = mime.trim().lowercase(java.util.Locale.US)
            if (mimeLimpio.isBlank() || mimeLimpio !in MIMES_PERMITIDOS) {
                return Result.failure(IllegalArgumentException("Tipo de archivo no permitido. Solo imágenes, PDF o texto."))
            }
            val nombreSeguro = nombre.trim().takeLast(120).replace(Regex("[^A-Za-z0-9._-]"), "_")
            if (nombreSeguro.isBlank()) return Result.failure(IllegalArgumentException("Nombre de archivo inválido."))
            return Result.success(nombreSeguro)
        }
    }

    private fun farmaciaId(): String? = SessionManager.clienteIdGarantizado.takeIf { it.isNotBlank() }

    // ── LECTURA EN VIVO (R8: verdad vigente, se cancela al salir) ──

    fun observarTickets(): Flow<List<SoporteTicket>> = callbackFlow {
        val fId = farmaciaId() ?: run {
            close(IllegalStateException("No hay sesión activa de farmacia."))
            return@callbackFlow
        }
        val ref = FarmadonPaths.soporteTickets(db, fId)
            .orderBy("actualizadoMs", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando tickets de soporte: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val tickets = snap?.documents?.mapNotNull { doc ->
                parseTicket(doc.id, doc.data)
            } ?: emptyList()
            trySend(tickets)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Mensajes nuevos (subcolección) en vivo, ordenados de forma determinista.
     * Se combina en la UI con los mensajes legacy (array) del documento padre.
     * Sin orderBy en servidor para no exigir índices nuevos: se ordena en memoria.
     */
    fun observarMensajes(ticketId: String): Flow<List<MensajeSoporte>> = callbackFlow {
        val fId = farmaciaId() ?: run {
            close(IllegalStateException("No hay sesión activa de farmacia."))
            return@callbackFlow
        }
        if (ticketId.isBlank()) {
            close(IllegalArgumentException("Conversación inválida."))
            return@callbackFlow
        }
        val ref = FarmadonPaths.soporteMensajes(db, fId, ticketId)
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando mensajes $ticketId: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { d ->
                parseMensaje(ticketId, d.id, d.data)
            }?.sortedWith(compareBy({ it.fechaMs }, { it.id })) ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    // ── CREAR CONVERSACIÓN (idempotente) ──

    suspend fun crearTicket(
        asunto: String,
        categoria: String,
        prioridad: String,
        descripcion: String
    ): Result<SoporteTicket> = crearCasoEstructurado(
        asunto = asunto,
        categoria = categoria,
        prioridad = prioridad,
        descripcion = descripcion,
        contextoProblema = "",
        contextoAccion = "",
        contextoDesdeCuando = "",
        clientConversationId = ""
    )

    /**
     * Crea el caso con formulario estructurado (§6) + contexto de error (§9).
     * [clientConversationId]: clave de idempotencia generada ANTES de llamar.
     * Si se reintenta con la misma clave, se devuelve el caso ya creado (sin duplicar).
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun crearCasoEstructurado(
        asunto: String,
        categoria: String,
        prioridad: String,
        descripcion: String,
        contextoProblema: String = "",
        contextoAccion: String = "",
        contextoDesdeCuando: String = "",
        relatedConversationId: String = "",
        errorCodigo: String = "",
        errorModulo: String = "",
        errorPantalla: String = "",
        errorAccion: String = "",
        appVersion: String = "",
        deviceInfo: String = "",
        osVersion: String = "",
        clientConversationId: String = ""
    ): Result<SoporteTicket> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión activa de farmacia."))
        val asuntoLimpio = asunto.trim()
        val descLimpia = descripcion.trim()
        if (asuntoLimpio.isBlank()) return Result.failure(IllegalArgumentException("El asunto de la consulta es obligatorio."))
        if (descLimpia.isBlank()) return Result.failure(IllegalArgumentException("Escribe el mensaje de tu consulta."))
        if (asuntoLimpio.length > 140) return Result.failure(IllegalArgumentException("El asunto no puede superar 140 caracteres."))
        if (descLimpia.length > 4000) return Result.failure(IllegalArgumentException("La descripción no puede superar 4000 caracteres."))

        val ahoraMs = HoraServidor.ahoraMs()
        val claveIdem = clientConversationId.trim().ifBlank { "tk_${UUID.randomUUID().toString().take(8)}" }
        val ticketId = claveIdem
        val numTicket = String.format(Locale.US, "SUP-%06d", (ahoraMs % 1_000_000).toInt())
        val msgInitClientId = "msg_${ahoraMs}_init_${UUID.randomUUID().toString().take(6)}"

        return try {
            val ticket = db.runTransaction { tx ->
                val docRef = FarmadonPaths.soporteTickets(db, fId).document(ticketId)
                val existente = tx.get(docRef)
                if (existente.exists()) {
                    // Reintento con la misma clave: devolver lo ya guardado, sin duplicar.
                    return@runTransaction parseTicket(existente.id, existente.data)
                        ?: throw IllegalStateException("El caso ya existe pero no se pudo leer. Vuelve a entrar a Soporte.")
                }
                val data = mapOf(
                    "id" to ticketId,
                    "numeroTicket" to numTicket,
                    "asunto" to asuntoLimpio,
                    "categoria" to categoria.trim().ifBlank { "OTRO" },
                    "prioridad" to prioridad.trim().ifBlank { "MEDIA" },
                    "descripcion" to descLimpia,
                    "contextoProblema" to contextoProblema.trim(),
                    "contextoAccion" to contextoAccion.trim(),
                    "contextoDesdeCuando" to contextoDesdeCuando.trim(),
                    "estado" to SoporteEstado.OPEN,
                    "farmaciaId" to fId,
                    "sucursalId" to SessionManager.sucursalIdEfectiva,
                    "sucursalNombre" to SessionManager.sucursalNombre,
                    "usuarioId" to SessionManager.idCajera,
                    "usuarioNombre" to SessionManager.nombreUsuario,
                    "usuarioEmail" to SessionManager.email,
                    "responsableAgenteId" to "",
                    "responsableAgenteNombre" to "",
                    "assignmentState" to SoporteAsignacion.UNASSIGNED,
                    "assignedAtMs" to 0L,
                    "primeraRespuestaMs" to 0L,
                    "clientConversationId" to claveIdem,
                    "relatedConversationId" to relatedConversationId.trim(),
                    "errorCodigo" to errorCodigo.trim(),
                    "errorModulo" to errorModulo.trim(),
                    "errorPantalla" to errorPantalla.trim(),
                    "errorAccion" to errorAccion.trim(),
                    "appVersion" to appVersion.trim(),
                    "deviceInfo" to deviceInfo.trim(),
                    "osVersion" to osVersion.trim(),
                    "ultimoMensaje" to descLimpia.take(140),
                    "mensajesSinLeerFarmacia" to 0,
                    "cerradoMs" to 0L,
                    "cerradoPor" to "",
                    "resumenCierre" to "",
                    "respuestaBrixo" to "",
                    "respondidoPor" to "",
                    "fechaMs" to ahoraMs,
                    "actualizadoMs" to ahoraMs,
                    "creadoEl" to FieldValue.serverTimestamp()
                )
                tx.set(docRef, data)
                // Primer mensaje como documento (ID = clientMessageId).
                val msgRef = FarmadonPaths.soporteMensajes(db, fId, ticketId).document(msgInitClientId)
                tx.set(
                    msgRef, mapOf(
                        "id" to msgInitClientId,
                        "clientMessageId" to msgInitClientId,
                        "conversationId" to ticketId,
                        "emisor" to "FARMACIA",
                        "autorNombre" to SessionManager.nombreUsuario,
                        "usuarioEmail" to SessionManager.email,
                        "sucursalId" to SessionManager.sucursalIdEfectiva,
                        "sucursalNombre" to SessionManager.sucursalNombre,
                        "clienteId" to fId,
                        "texto" to descLimpia,
                        "tipo" to if (errorCodigo.isNotBlank()) SoporteTipoMensaje.ERROR_REPORT else SoporteTipoMensaje.TEXT,
                        "archivoNombre" to "",
                        "archivoTamanoBytes" to 0L,
                        "archivoMime" to "",
                        "errorCodigo" to errorCodigo.trim(),
                        "fechaMs" to ahoraMs,
                        "leido" to false,
                        "estadoLectura" to "ENTREGADO",
                        "creadoEl" to FieldValue.serverTimestamp()
                    )
                )
                parseTicket(
                    ticketId, data + ("mensajes" to emptyList<Map<String, Any>>())
                ) ?: SoporteTicket(
                    id = ticketId, numeroTicket = numTicket, asunto = asuntoLimpio,
                    categoria = categoria, prioridad = prioridad, descripcion = descLimpia,
                    estado = SoporteEstado.OPEN, farmaciaId = fId,
                    sucursalId = SessionManager.sucursalIdEfectiva,
                    sucursalNombre = SessionManager.sucursalNombre,
                    usuarioNombre = SessionManager.nombreUsuario,
                    usuarioEmail = SessionManager.email,
                    clientConversationId = claveIdem,
                    fechaMs = ahoraMs, actualizadoMs = ahoraMs
                )
            }.await()
            Result.success(ticket)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando ticket de soporte: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── ENVIAR MENSAJE (transacción + cierre + idempotencia) ──

    suspend fun agregarMensajeATicket(ticketId: String, textoMensaje: String): Result<Unit> =
        enviarMensaje(ticketId = ticketId, texto = textoMensaje, clientMessageId = "")

    /**
     * Envía un mensaje verificando en el SERVIDOR (transacción):
     * tenant correcto → caso existe → NO está CLOSED/RESUELTO → mensaje no duplicado.
     * Si el cliente pulsa Enviar 3 veces o la red reintenta, solo queda 1 mensaje.
     */
    suspend fun enviarMensaje(
        ticketId: String,
        texto: String,
        clientMessageId: String,
        tipo: String = SoporteTipoMensaje.TEXT,
        archivoNombre: String = "",
        archivoTamanoBytes: Long = 0L,
        archivoMime: String = ""
    ): Result<Unit> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión activa de farmacia."))
        val textoLimpio = texto.trim()
        if (ticketId.isBlank()) return Result.failure(IllegalArgumentException("Conversación inválida."))
        if (textoLimpio.isBlank()) return Result.failure(IllegalArgumentException("El mensaje no puede estar vacío."))
        if (textoLimpio.length > 4000) return Result.failure(IllegalArgumentException("El mensaje no puede superar 4000 caracteres."))
        if (tipo == SoporteTipoMensaje.FILE || tipo == SoporteTipoMensaje.IMAGE) {
            val v = validarAdjunto(archivoNombre, archivoTamanoBytes, archivoMime)
            if (v.isFailure) return Result.failure(v.exceptionOrNull()!!)
        }
        val claveMsg = clientMessageId.trim().ifBlank { "msg_${HoraServidor.ahoraMs()}_${UUID.randomUUID().toString().take(6)}" }

        return try {
            db.runTransaction { tx ->
                val docRef = FarmadonPaths.soporteTickets(db, fId).document(ticketId)
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalArgumentException("Esta conversación ya no existe. Crea un caso nuevo.")
                val estadoActual = SoporteEstado.normalizar(snap.getString("estado"))
                if (estadoActual == SoporteEstado.CLOSED || estadoActual == SoporteEstado.RESOLVED) {
                    throw IllegalStateException(ERROR_CONVERSACION_CERRADA)
                }
                val msgRef = FarmadonPaths.soporteMensajes(db, fId, ticketId).document(claveMsg)
                if (tx.get(msgRef).exists()) {
                    // Reintento / doble clic: ya quedó guardado, no duplicar.
                    return@runTransaction Unit
                }
                val ahoraMs = HoraServidor.ahoraMs()
                tx.set(
                    msgRef, mapOf(
                        "id" to claveMsg,
                        "clientMessageId" to claveMsg,
                        "conversationId" to ticketId,
                        "emisor" to "FARMACIA",
                        "autorNombre" to SessionManager.nombreUsuario,
                        "usuarioEmail" to SessionManager.email,
                        "sucursalId" to SessionManager.sucursalIdEfectiva,
                        "sucursalNombre" to SessionManager.sucursalNombre,
                        "clienteId" to fId,
                        "texto" to textoLimpio,
                        "tipo" to tipo,
                        "archivoNombre" to archivoNombre.trim(),
                        "archivoTamanoBytes" to archivoTamanoBytes,
                        "archivoMime" to archivoMime.trim(),
                        "errorCodigo" to "",
                        "fechaMs" to ahoraMs,
                        "leido" to false,
                        "estadoLectura" to "ENTREGADO",
                        "creadoEl" to FieldValue.serverTimestamp()
                    )
                )
                // La farmacia respondió: si Brixo estaba esperando, el caso vuelve a Brixo.
                val siguienteEstado = when (estadoActual) {
                    SoporteEstado.OPEN, SoporteEstado.IN_PROGRESS, SoporteEstado.WAITING_CUSTOMER -> SoporteEstado.WAITING_BRIXO
                    else -> estadoActual
                }
                val updates = mutableMapOf<String, Any>(
                    "ultimoMensaje" to textoLimpio.take(140),
                    "actualizadoMs" to ahoraMs,
                    "estado" to siguienteEstado
                )
                tx.update(docRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error agregando mensaje al ticket $ticketId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Valida un adjunto antes de subirlo: tamaño, tipo real, sin confiar solo en la extensión. */
    fun validarAdjunto(nombre: String, tamanoBytes: Long, mime: String): Result<String> {
        val base = validarAdjuntoPuro(nombre, tamanoBytes, mime)
        if (base.isFailure) return base
        val attachmentId = "att_${HoraServidor.ahoraMs()}_${UUID.randomUUID().toString().take(6)}"
        return Result.success("${attachmentId}_${base.getOrNull()}")
    }

    /**
     * Marca los mensajes como leídos (recibo de lectura). No toca el historial,
     * solo el contador. Si falla, se reintenta al volver a abrir (log, sin
     * romper el chat: es sincronización de fondo, no una acción del usuario).
     */
    suspend fun marcarConversacionLeida(ticketId: String) {
        val fId = farmaciaId() ?: return
        if (ticketId.isBlank()) return
        try {
            FarmadonPaths.soporteTickets(db, fId).document(ticketId)
                .update("mensajesSinLeerFarmacia", 0).await()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo marcar leído $ticketId: ${e.message}", e)
        }
    }

    /**
     * Obtiene los canales oficiales de contacto de BRIXO desde la central.
     */
    suspend fun obtenerCanalesOficiales(): CanalesSoporteOficial {
        return try {
            val doc = db.collection("brixo_configuracion").document("empresa").get().await()
            if (doc.exists()) {
                // Solo lo publicado por la central. Vacío = no publicado (estado vacío honesto).
                val wa = (doc.getString("whatsappCobranzas") ?: doc.getString("whatsapp") ?: "").trim()
                val email = (doc.getString("emailCobranzas") ?: doc.getString("email") ?: "").trim()
                val horario = (doc.getString("horarioAtencion") ?: "").trim()
                val central = (doc.getString("telefonoCentral") ?: "").trim()
                CanalesSoporteOficial(whatsapp = wa, email = email, horarioAtencion = horario, telefonoCentral = central)
            } else {
                CanalesSoporteOficial()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo canales oficiales: ${e.message}", e)
            CanalesSoporteOficial()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTicket(id: String, data: Map<String, Any>?): SoporteTicket? {
        if (data == null) return null
        return try {
            // Legacy: casos viejos sin assignmentState pero con responsable escrito
            // se tratan como ASSIGNED en lectura (no se reescribe nada).
            val estadoLeido = SoporteEstado.normalizar(data["estado"] as? String)
            val respId = data["responsableAgenteId"] as? String ?: ""
            val asignLeido = SoporteAsignacion.normalizar(data["assignmentState"] as? String)
            val asignFinal = if (asignLeido == SoporteAsignacion.UNASSIGNED && respId.isNotBlank() &&
                estadoLeido != SoporteEstado.CLOSED && estadoLeido != SoporteEstado.RESOLVED
            ) SoporteAsignacion.ASSIGNED else asignLeido
            val mListRaw = data["mensajes"] as? List<Map<String, Any>> ?: emptyList()
            val mListParsed = mListRaw.mapNotNull { mData ->
                try {
                    MensajeSoporte(
                        id = mData["id"] as? String ?: "msg_${UUID.randomUUID()}",
                        clientMessageId = mData["clientMessageId"] as? String ?: (mData["id"] as? String ?: ""),
                        conversationId = mData["conversationId"] as? String ?: id,
                        emisor = mData["emisor"] as? String ?: "FARMACIA",
                        autorNombre = mData["autorNombre"] as? String ?: "",
                        usuarioEmail = mData["usuarioEmail"] as? String ?: "",
                        sucursalId = mData["sucursalId"] as? String ?: "",
                        sucursalNombre = mData["sucursalNombre"] as? String ?: "",
                        clienteId = mData["clienteId"] as? String ?: "",
                        texto = mData["texto"] as? String ?: "",
                        tipo = mData["tipo"] as? String ?: SoporteTipoMensaje.TEXT,
                        archivoNombre = mData["archivoNombre"] as? String ?: "",
                        archivoTamanoBytes = (mData["archivoTamanoBytes"] as? Number)?.toLong() ?: 0L,
                        archivoMime = mData["archivoMime"] as? String ?: "",
                        errorCodigo = mData["errorCodigo"] as? String ?: "",
                        evento = mData["evento"] as? String ?: "",
                        actorId = mData["actorId"] as? String ?: "",
                        fechaMs = (mData["fechaMs"] as? Number)?.toLong() ?: 0L,
                        leido = mData["leido"] as? Boolean ?: false,
                        estadoLectura = mData["estadoLectura"] as? String ?: "ENTREGADO"
                    )
                } catch (_: Exception) { null }
            }

            SoporteTicket(
                id = id,
                numeroTicket = data["numeroTicket"] as? String ?: id,
                asunto = data["asunto"] as? String ?: "",
                categoria = data["categoria"] as? String ?: "OTRO",
                prioridad = data["prioridad"] as? String ?: "MEDIA",
                descripcion = data["descripcion"] as? String ?: "",
                contextoProblema = data["contextoProblema"] as? String ?: "",
                contextoAccion = data["contextoAccion"] as? String ?: "",
                contextoDesdeCuando = data["contextoDesdeCuando"] as? String ?: "",
                estado = estadoLeido,
                farmaciaId = data["farmaciaId"] as? String ?: (data["clienteId"] as? String ?: ""),
                sucursalId = data["sucursalId"] as? String ?: "",
                sucursalNombre = data["sucursalNombre"] as? String ?: "",
                usuarioId = data["usuarioId"] as? String ?: "",
                usuarioNombre = data["usuarioNombre"] as? String ?: "",
                usuarioEmail = data["usuarioEmail"] as? String ?: "",
                responsableAgenteId = data["responsableAgenteId"] as? String ?: "",
                responsableAgenteNombre = data["responsableAgenteNombre"] as? String ?: (data["respondidoPor"] as? String ?: ""),
                assignmentState = asignFinal,
                assignedAtMs = (data["assignedAtMs"] as? Number)?.toLong() ?: 0L,
                primeraRespuestaMs = (data["primeraRespuestaMs"] as? Number)?.toLong() ?: 0L,
                clientConversationId = data["clientConversationId"] as? String ?: "",
                relatedConversationId = data["relatedConversationId"] as? String ?: "",
                errorCodigo = data["errorCodigo"] as? String ?: "",
                errorModulo = data["errorModulo"] as? String ?: "",
                errorPantalla = data["errorPantalla"] as? String ?: "",
                errorAccion = data["errorAccion"] as? String ?: "",
                appVersion = data["appVersion"] as? String ?: "",
                deviceInfo = data["deviceInfo"] as? String ?: "",
                osVersion = data["osVersion"] as? String ?: "",
                ultimoMensaje = data["ultimoMensaje"] as? String ?: "",
                mensajesSinLeerFarmacia = (data["mensajesSinLeerFarmacia"] as? Number)?.toInt() ?: 0,
                cerradoMs = (data["cerradoMs"] as? Number)?.toLong() ?: 0L,
                cerradoPor = data["cerradoPor"] as? String ?: "",
                resumenCierre = data["resumenCierre"] as? String ?: "",
                respuestaBrixo = data["respuestaBrixo"] as? String ?: "",
                respondidoPor = data["respondidoPor"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
                actualizadoMs = (data["actualizadoMs"] as? Number)?.toLong() ?: 0L,
                mensajes = mListParsed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando ticket $id: ${e.message}", e)
            null
        }
    }

    private fun parseMensaje(conversationId: String, docId: String, data: Map<String, Any>?): MensajeSoporte? {
        if (data == null) return null
        return try {
            MensajeSoporte(
                id = data["id"] as? String ?: docId,
                clientMessageId = data["clientMessageId"] as? String ?: docId,
                conversationId = data["conversationId"] as? String ?: conversationId,
                emisor = data["emisor"] as? String ?: "FARMACIA",
                autorNombre = data["autorNombre"] as? String ?: "",
                usuarioEmail = data["usuarioEmail"] as? String ?: "",
                sucursalId = data["sucursalId"] as? String ?: "",
                sucursalNombre = data["sucursalNombre"] as? String ?: "",
                clienteId = data["clienteId"] as? String ?: "",
                texto = data["texto"] as? String ?: "",
                tipo = data["tipo"] as? String ?: SoporteTipoMensaje.TEXT,
                archivoNombre = data["archivoNombre"] as? String ?: "",
                archivoTamanoBytes = (data["archivoTamanoBytes"] as? Number)?.toLong() ?: 0L,
                archivoMime = data["archivoMime"] as? String ?: "",
                errorCodigo = data["errorCodigo"] as? String ?: "",
                evento = data["evento"] as? String ?: "",
                actorId = data["actorId"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
                leido = data["leido"] as? Boolean ?: false,
                estadoLectura = data["estadoLectura"] as? String ?: "ENTREGADO"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando mensaje $docId: ${e.message}", e)
            null
        }
    }
}
