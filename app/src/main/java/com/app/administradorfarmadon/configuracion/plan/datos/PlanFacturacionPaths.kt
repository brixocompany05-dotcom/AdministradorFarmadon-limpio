package com.app.administradorfarmadon.configuracion.plan.datos

import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore

object PlanFacturacionPaths {
    fun farmacia(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId)

    fun suscripciones(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId).collection("suscripciones")

    fun auditoriaSuscripcion(db: FirebaseFirestore, farmaciaId: String, suscripcionId: String) =
        FarmadonPaths.farmacia(db, farmaciaId).collection("suscripciones").document(suscripcionId).collection("auditoria")

    fun planEcosistema(db: FirebaseFirestore, planId: String) =
        EcosistemaPaths.planes(db).document(planId)

    fun sucursales(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId).collection("sucursales")

    fun usuariosFarmacia(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("usuarios_farmacia")
}
