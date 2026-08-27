package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos.Paso1UiState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

interface EnviarCorreccionUseCase {
    suspend fun ejecutar(
        uid: String,
        state: Paso1UiState,
        requestId: String,
        versionOriginal: Long,
        originalRuc: String,
        nuevoPlanId: String = "",
        nuevoPlanNombre: String = ""
    )
}

class EnviarCorreccionUseCaseImpl(private val db: FirebaseFirestore = FarmadonFirestore.db) : EnviarCorreccionUseCase {
    override suspend fun ejecutar(
        uid: String,
        state: Paso1UiState,
        requestId: String,
        versionOriginal: Long,
        originalRuc: String,
        nuevoPlanId: String,
        nuevoPlanNombre: String
    ) {
        db.runTransaction { tx ->
            val rucLimpio = state.ruc.trim()
            val targetRuc = if (originalRuc.isNotBlank()) originalRuc else uid
            val solicitudesCol = AuthPaths.solicitudes(db)
            val solicitudRef = solicitudesCol.document(targetRuc)

            // 1. Lectura y validación directa del expediente
            var doc = tx.get(solicitudRef)
            var actualRef = solicitudRef

            if (!doc.exists() && uid.isNotBlank() && uid != targetRuc) {
                val altRef = solicitudesCol.document(uid)
                val altDoc = tx.get(altRef)
                if (altDoc.exists()) {
                    actualRef = altRef
                    doc = altDoc
                }
            }

            if (!doc.exists()) {
                throw Exception("No se encontró el expediente de solicitud para la farmacia.")
            }

            val versionActual = doc.getLong("version") ?: 1L
            if (versionOriginal > 0 && versionActual != versionOriginal) {
                throw com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.ConflictoVersionException(
                    "El expediente fue modificado por BRIXO mientras editabas. Recarga para ver los cambios."
                )
            }
            val savedUid = doc.getString("uid") ?: "cliente"

            // 2. Preparar actualización limpia de los campos corregidos
            val now = Date()
            val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(now)

            val detalleNota = if (state.notaAclaratoria.isNotBlank()) {
                state.notaAclaratoria.trim()
            } else {
                "El cliente envió la corrección solicitada directamente."
            }

            val rucInmutable = if (originalRuc.isNotBlank()) originalRuc else targetRuc
            val updates = mutableMapOf<String, Any>(
                "nombreFarmacia" to state.nombreFarmacia.trim(),
                "dueno" to state.dueno.trim(),
                "ruc" to rucInmutable,
                "telefono" to state.telefono.trim(),
                "direccion" to state.direccion.trim(),
                "estado" to "pendiente",
                "correccionSolicitada" to false,
                "fechaSolicitud" to FieldValue.serverTimestamp(),
                "version" to versionActual + 1,
                "correccionRecibidaAt" to FieldValue.serverTimestamp(),
                "fechaCorreccion" to iso8601,
                "fechaCorreccion_ts" to FieldValue.serverTimestamp(),
                "lineaTiempo" to FieldValue.arrayUnion(mapOf(
                    "tipo" to "CORRECCION_ENVIADA_CLIENTE",
                    "agente" to savedUid,
                    "ts" to com.google.firebase.Timestamp(now),
                    "detalle" to detalleNota
                ))
            )

            if (state.notaAclaratoria.isNotBlank()) {
                updates["notaAclaratoriaCliente"] = state.notaAclaratoria.trim()
            }

            if (state.latitud != null && state.longitud != null) {
                updates["ubicacionGeo"] = GeoPoint(state.latitud, state.longitud)
            }

            // Persistir cambio de plan si el cliente eligió uno distinto al observado.
            // Sin esto, la corrección de plan se perdía silenciosamente: el doc
            // seguía con el planId viejo y BrixoPanel aprobaría el plan equivocado.
            val planNuevo = nuevoPlanId.trim()
            val planOriginalDoc = doc.getString("planId") ?: ""
            if (planNuevo.isNotBlank() && planNuevo != planOriginalDoc) {
                updates["planId"] = planNuevo
                // Solo sobrescribir el nombre si llegó uno real: un "" borraría
                // el dato existente (fallo silencioso).
                if (nuevoPlanNombre.isNotBlank()) {
                    updates["planNombre"] = nuevoPlanNombre
                }
            }

            // Persistir país elegido/corregido + sus monedas derivadas del catálogo.
            // Sin esto, la elección de país durante una corrección moría al enviar:
            // el expediente quedaba huérfano de país y la aprobación inventaría uno.
            val paisEnDoc = (doc.getString("pais") ?: "").trim().uppercase()
            val paisCorregido = state.paisIso.trim().uppercase()
            if (paisCorregido.isNotBlank() && paisCorregido != paisEnDoc) {
                val monedaPais = com.app.administradorfarmadon.organizacion.datos.CatalogoPaises.monedaDe(paisCorregido)
                if (monedaPais.first.isNotBlank()) {
                    updates["pais"] = paisCorregido
                    updates["monedaOperativa"] = monedaPais.first
                    updates["simboloMoneda"] = monedaPais.second
                }
            }


            tx.update(actualRef, updates)
        }.await()
    }
}

