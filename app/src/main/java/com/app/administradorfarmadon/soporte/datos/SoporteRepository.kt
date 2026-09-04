package com.app.administradorfarmadon.soporte.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.soporte.modelo.CanalesSoporteOficial
import com.app.administradorfarmadon.soporte.modelo.MensajeSoporte
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class SoporteRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "SoporteRepository"
    }

    private fun farmaciaId(): String? = SessionManager.clienteIdGarantizado.takeIf { it.isNotBlank() }

    /**
     * Observa en tiempo real los tickets de soporte enviados por esta farmacia (R1/R8).
     */
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
     * Agrega un nuevo mensaje con ID único a la conversación activa existente.
     */
    suspend fun agregarMensajeATicket(ticketId: String, textoMensaje: String): Result<Unit> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión activa de farmacia."))
        val textoLimpio = textoMensaje.trim()
        if (textoLimpio.isBlank()) {
            return Result.failure(IllegalArgumentException("El mensaje no puede estar vacío."))
        }

        val ahoraMs = HoraServidor.ahoraMs()
        val msgId = "msg_${ahoraMs}_${UUID.randomUUID().toString().take(6)}"

        val nuevoMensaje = mapOf(
            "id" to msgId,
            "emisor" to "FARMACIA",
            "autorNombre" to SessionManager.nombreUsuario,
            "usuarioEmail" to SessionManager.email,
            "sucursalId" to SessionManager.sucursalIdEfectiva,
            "sucursalNombre" to SessionManager.sucursalNombre,
            "clienteId" to fId,
            "texto" to textoLimpio,
            "fechaMs" to ahoraMs,
            "leido" to false,
            "estadoLectura" to "ENTREGADO"
        )

        return try {
            val docRef = FarmadonPaths.soporteTickets(db, fId).document(ticketId)
            docRef.update(
                mapOf(
                    "mensajes" to FieldValue.arrayUnion(nuevoMensaje),
                    "actualizadoMs" to ahoraMs
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error agregando mensaje al ticket $ticketId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Registra un nuevo ticket de soporte e incidencia.
     */
    suspend fun crearTicket(
        asunto: String,
        categoria: String,
        prioridad: String,
        descripcion: String
    ): Result<SoporteTicket> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión activa de farmacia."))
        val asuntoLimpio = asunto.trim()
        val descLimpia = descripcion.trim()

        if (asuntoLimpio.isBlank()) {
            return Result.failure(IllegalArgumentException("El asunto de la consulta es obligatorio."))
        }
        if (descLimpia.isBlank()) {
            return Result.failure(IllegalArgumentException("Escribe el mensaje de tu consulta."))
        }

        val ahoraMs = HoraServidor.ahoraMs()
        val ticketId = "tk_${UUID.randomUUID().toString().take(8)}"
        val numTicket = String.format(Locale.US, "TK-%04d", (ahoraMs % 10000).toInt())
        val msgInitId = "msg_${ahoraMs}_init"

        val primerMensajeMap = mapOf(
            "id" to msgInitId,
            "emisor" to "FARMACIA",
            "autorNombre" to SessionManager.nombreUsuario,
            "usuarioEmail" to SessionManager.email,
            "sucursalId" to SessionManager.sucursalIdEfectiva,
            "sucursalNombre" to SessionManager.sucursalNombre,
            "clienteId" to fId,
            "texto" to descLimpia,
            "fechaMs" to ahoraMs,
            "leido" to false,
            "estadoLectura" to "ENTREGADO"
        )

        val ticket = SoporteTicket(
            id = ticketId,
            numeroTicket = numTicket,
            asunto = asuntoLimpio,
            categoria = categoria,
            prioridad = prioridad,
            descripcion = descLimpia,
            estado = "ABIERTO",
            sucursalId = SessionManager.sucursalIdEfectiva,
            sucursalNombre = SessionManager.sucursalNombre,
            usuarioNombre = SessionManager.nombreUsuario,
            usuarioEmail = SessionManager.email,
            fechaMs = ahoraMs,
            actualizadoMs = ahoraMs
        )

        return try {
            val docRef = FarmadonPaths.soporteTickets(db, fId).document(ticketId)
            val data = mapOf(
                "id" to ticket.id,
                "numeroTicket" to ticket.numeroTicket,
                "asunto" to ticket.asunto,
                "categoria" to ticket.categoria,
                "prioridad" to ticket.prioridad,
                "descripcion" to ticket.descripcion,
                "estado" to ticket.estado,
                "sucursalId" to ticket.sucursalId,
                "sucursalNombre" to ticket.sucursalNombre,
                "usuarioNombre" to ticket.usuarioNombre,
                "usuarioEmail" to ticket.usuarioEmail,
                "respuestaBrixo" to "",
                "respondidoPor" to "",
                "fechaMs" to ticket.fechaMs,
                "actualizadoMs" to ticket.actualizadoMs,
                "mensajes" to listOf(primerMensajeMap),
                "creadoEl" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(ticket)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando ticket de soporte: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los canales oficiales de contacto de BRIXO desde la central.
     */
    suspend fun obtenerCanalesOficiales(): CanalesSoporteOficial {
        return try {
            val doc = db.collection("brixo_configuracion").document("empresa").get().await()
            if (doc.exists()) {
                val wa = doc.getString("whatsappCobranzas") ?: doc.getString("whatsapp") ?: "51900000000"
                val email = doc.getString("emailCobranzas") ?: doc.getString("email") ?: "soporte@brixo.pe"
                CanalesSoporteOficial(
                    whatsapp = wa.trim(),
                    email = email.trim()
                )
            } else {
                CanalesSoporteOficial()
            }
        } catch (e: Exception) {
            CanalesSoporteOficial()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTicket(id: String, data: Map<String, Any>?): SoporteTicket? {
        if (data == null) return null
        return try {
            val mListRaw = data["mensajes"] as? List<Map<String, Any>> ?: emptyList()
            val mListParsed = mListRaw.mapNotNull { mData ->
                try {
                    MensajeSoporte(
                        id = mData["id"] as? String ?: "msg_${UUID.randomUUID()}",
                        emisor = mData["emisor"] as? String ?: "FARMACIA",
                        autorNombre = mData["autorNombre"] as? String ?: "",
                        usuarioEmail = mData["usuarioEmail"] as? String ?: "",
                        sucursalId = mData["sucursalId"] as? String ?: "",
                        sucursalNombre = mData["sucursalNombre"] as? String ?: "",
                        clienteId = mData["clienteId"] as? String ?: "",
                        texto = mData["texto"] as? String ?: "",
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
                categoria = data["categoria"] as? String ?: "CONSULTA_GENERAL",
                prioridad = data["prioridad"] as? String ?: "MEDIA",
                descripcion = data["descripcion"] as? String ?: "",
                estado = data["estado"] as? String ?: "ABIERTO",
                sucursalId = data["sucursalId"] as? String ?: "",
                sucursalNombre = data["sucursalNombre"] as? String ?: "",
                usuarioNombre = data["usuarioNombre"] as? String ?: "",
                usuarioEmail = data["usuarioEmail"] as? String ?: "",
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
}
