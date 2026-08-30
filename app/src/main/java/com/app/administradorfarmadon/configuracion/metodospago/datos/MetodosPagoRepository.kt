package com.app.administradorfarmadon.configuracion.metodospago.datos

import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesPaths
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Catálogo de métodos de pago FIJOS, AISLADO POR SUCURSAL (R1).
 *
 * Cada sucursal tiene su propio documento: farmacias/{f}/sucursales/{s}/catalogos/metodosPago.
 * El administrador general puede elegir qué sucursal está configurando (selector en pantalla),
 * y cada cambio se guarda SOLO en la sucursal elegida. El historial de pagos ya hechos no se
 * toca jamás: activar o desactivar solo cambia lo que se puede elegir de aquí en adelante.
 *
 * Documento: catalogos/metodosPago → instancias: { id: { farmaciaId, sucursalId, tipoId, activa, datos } }
 */
class MetodosPagoRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {

    private fun refMetodos(sucursalId: String): DocumentReference {
        val f = SessionManager.clienteIdGarantizado
        val s = sucursalId.ifBlank { "principal" }
        return FarmadonPaths.sucursal(db, f, s).collection("catalogos").document("metodosPago")
    }

    /** Lista de sucursales de la farmacia para el selector del administrador. */
    fun observarSucursales(): Flow<List<SucursalCatalogo>> = callbackFlow {
        val f = SessionManager.clienteIdGarantizado
        if (f.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val reg = SucursalesPaths.sucursales(db, f).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { doc ->
                SucursalCatalogo(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "Sede sin nombre"
                )
            } ?: emptyList()
            trySend(lista)
        }
        awaitClose { reg.remove() }
    }

    fun observarMetodosPago(sucursalId: String): Flow<List<InstanciaPago>> = callbackFlow {
        val reg = refMetodos(sucursalId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val raw = (snap?.get("instancias") as? Map<String, Any>) ?: emptyMap()
            val lista = raw.mapNotNull { (id, v) ->
                if (v is Map<*, *>) {
                    InstanciaPago(
                        id = id,
                        farmaciaId = (v["farmaciaId"] as? String) ?: SessionManager.clienteIdGarantizado,
                        sucursalId = (v["sucursalId"] as? String) ?: sucursalId,
                        tipoId = (v["tipoId"] as? String) ?: "",
                        activa = v["activa"] == true,
                        datos = (v["datos"] as? Map<*, *>)
                            ?.mapNotNull { (k, val_) -> (k as? String)?.let { it to (val_?.toString() ?: "") } }
                            ?.toMap()
                            ?: emptyMap()
                    )
                } else null
            }
            trySend(lista)
        }
        awaitClose { reg.remove() }
    }

    suspend fun agregarInstancia(sucursalId: String, tipoId: String, datos: Map<String, String>): Result<Unit> {
        val id = UUID.randomUUID().toString()
        val limpios = datos.mapValues { (_, v) -> v.trim() }
        val f = SessionManager.clienteIdGarantizado
        return try {
            val docRef = refMetodos(sucursalId)
            // Regla ATÓMICA: una sola cuenta por tipo de pago en cada sucursal.
            // Se lee dentro de la transacción para que dos dispositivos no puedan
            // crear dos cuentas del mismo tipo al mismo tiempo.
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                @Suppress("UNCHECKED_CAST")
                val instanciasActuales = (snap.get("instancias") as? Map<String, Any>) ?: emptyMap()
                val yaExisteTipo = instanciasActuales.values.any { v ->
                    (v as? Map<*, *>)?.get("tipoId") == tipoId
                }
                if (yaExisteTipo) {
                    throw IllegalStateException("Este tipo de pago ya tiene una cuenta registrada en esta sucursal. Solo se permite una: edítala.")
                }
                tx.set(
                    docRef,
                    mapOf(
                        "instancias" to mapOf(
                            id to mapOf(
                                "farmaciaId" to f,
                                "sucursalId" to sucursalId,
                                "tipoId" to tipoId,
                                "activa" to true,
                                "datos" to limpios
                            )
                        )
                    ),
                    SetOptions.merge()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setInstanciaActiva(sucursalId: String, id: String, activa: Boolean): Result<Unit> = try {
        refMetodos(sucursalId).set(
            mapOf("instancias" to mapOf(id to mapOf("activa" to activa))),
            SetOptions.merge()
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun guardarDatosInstancia(sucursalId: String, id: String, datos: Map<String, String>): Result<Unit> {
        val limpios = datos.mapValues { (_, v) -> v.trim() }
        return try {
            refMetodos(sucursalId).set(
                mapOf("instancias" to mapOf(id to mapOf("datos" to limpios))),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarInstancia(sucursalId: String, id: String): Result<Unit> = try {
        refMetodos(sucursalId).update(mapOf("instancias.$id" to FieldValue.delete())).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** Resumen de una sucursal para el selector de configuración de pagos. */
data class SucursalCatalogo(
    val id: String,
    val nombre: String
)
