package com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PlanesRepository(private val db: FirebaseFirestore = FarmadonFirestore.db) {
    fun getPlanesActivos(): Flow<List<PlanSuscripcion>> = callbackFlow {
        val registration = EcosistemaPaths.planes(db)
            .whereEqualTo("activo", true)
            .whereEqualTo("eliminado", false)
            .orderBy("precioMensual", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val planes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PlanSuscripcion::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(planes)
            }
        awaitClose { registration.remove() }
    }
}
