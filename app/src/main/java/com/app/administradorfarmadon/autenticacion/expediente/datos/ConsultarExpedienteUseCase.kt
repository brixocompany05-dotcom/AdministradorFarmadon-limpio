package com.app.administradorfarmadon.autenticacion.expediente.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class ResultadoExpediente {
    data class Encontrado(
        val ruc: String,
        val estado: String,
        val clienteId: String? = null,
        val camposACorregir: List<String> = emptyList(),
        val mensajeBrixo: String? = null,
        // Sello de corrección respondida: camposACorregir NO se borra al enviarse
        // la corrección (BrixoPanel lo usa para la tabla antes──†’después), así que
        // la UI necesita distinguir "hay observación pendiente" de "ya corregí".
        val correccionYaRespondida: Boolean = false
    ) : ResultadoExpediente()

    data object NoEncontrado : ResultadoExpediente()
    data class Error(val mensaje: String) : ResultadoExpediente()
}

interface ConsultarExpedienteUseCase {
    fun observar(ruc: String): Flow<ResultadoExpediente>
}

class ConsultarExpedienteUseCaseImpl : ConsultarExpedienteUseCase {
    override fun observar(ruc: String): Flow<ResultadoExpediente> = callbackFlow {
        val db = FarmadonFirestore.db
        val rucTrim = ruc.trim()
        if (rucTrim.isEmpty()) {
            trySend(ResultadoExpediente.NoEncontrado)
            close()
            return@callbackFlow
        }

        var farmaciaListener: ListenerRegistration? = null
        var restringidoListener: ListenerRegistration? = null

        val solListener = AuthPaths.solicitudes(db).document(rucTrim)
            .addSnapshotListener { solDoc, error ->
                if (error != null) {
                    trySend(ResultadoExpediente.Error(error.message ?: "Error al escuchar solicitud"))
                    return@addSnapshotListener
                }

                if (solDoc != null && solDoc.exists()) {
                    val campos = (solDoc.get("camposACorregir") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val mensaje = solDoc.getString("mensajeBrixo") ?: solDoc.getString("motivoRechazo") ?: solDoc.getString("motivoObservacion")
                    trySend(
                        ResultadoExpediente.Encontrado(
                            ruc = solDoc.getString("ruc") ?: rucTrim,
                            estado = solDoc.getString("estado") ?: "pendiente",
                            clienteId = solDoc.getString("clienteId"),
                            camposACorregir = campos,
                            mensajeBrixo = mensaje,
                            correccionYaRespondida = solDoc.get("correccionRecibidaAt") != null
                        )
                    )
                } else {
                    // Si la solicitud no existe o fue aprobada por Brixo
                    farmaciaListener?.remove()
                    farmaciaListener = FarmadonPaths.farmacia(db, rucTrim)
                        .addSnapshotListener { farmaciaDoc, _ ->
                            if (farmaciaDoc != null && farmaciaDoc.exists()) {
                                val estadoFarmacia = farmaciaDoc.getString("estado") ?: "activo"
                                if (estadoFarmacia == "suspendido") {
                                    trySend(
                                        ResultadoExpediente.Encontrado(
                                            ruc = rucTrim,
                                            estado = "suspendida",
                                            clienteId = rucTrim,
                                            mensajeBrixo = "La suscripción de la farmacia se encuentra suspendida. Contacta a soporte para reactivarla."
                                        )
                                    )
                                } else {
                                    trySend(
                                        ResultadoExpediente.Encontrado(
                                            ruc = rucTrim,
                                            estado = "aprobada",
                                            clienteId = rucTrim,
                                            mensajeBrixo = "¡Tu farmacia está aprobada y activa! Ya puedes iniciar sesión con tu correo y contraseña."
                                        )
                                    )
                                }
                            } else {
                                restringidoListener?.remove()
                                restringidoListener = AuthPaths.existeRestringido(db).document(rucTrim)
                                    .addSnapshotListener { restDoc, _ ->
                                        if (restDoc != null && restDoc.exists()) {
                                            trySend(
                                                ResultadoExpediente.Encontrado(
                                                    ruc = rucTrim,
                                                    estado = "rechazada",
                                                    mensajeBrixo = restDoc.getString("motivo")?.takeIf { it.isNotBlank() }
                                                        ?: "Tu solicitud se encuentra restringida. Contacta a soporte para más información."
                                                )
                                            )
                                        } else {
                                            trySend(ResultadoExpediente.NoEncontrado)
                                        }
                                    }
                            }
                        }
                }
            }

        awaitClose {
            solListener.remove()
            farmaciaListener?.remove()
            restringidoListener?.remove()
        }
    }
}
