package com.app.administradorfarmadon.compartido.datos

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas canónicas del catálogo global compartido gestionado por BRIXO.
 * La aplicación Farmadon únicamente realiza operaciones de lectura sobre estos catálogos.
 */
object EcosistemaPaths {
    fun planes(db: FirebaseFirestore) =
        db.collection("compartido").document("ecosistema").collection("planes")

    fun rolesFarmacia(db: FirebaseFirestore) =
        db.collection("compartido").document("ecosistema").collection("roles_farmacia")

    fun herramientasPlan(db: FirebaseFirestore) =
        db.collection("compartido").document("ecosistema").collection("herramientas_plan")

    fun auditoriaClientes(db: FirebaseFirestore) =
        db.collection("compartido").document("ecosistema").collection("auditoria_clientes")
}
