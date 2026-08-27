package com.app.administradorfarmadon.navegacion.sidebar

import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas de base de datos exclusivas del Sidebar y Verificación de Licencia/Suscripción.
 * El negocio (nombre, plan, estado, suscripción) vive en farmacias/{farmaciaId}.
 * La identidad del cliente (clientes/{clienteId}) solo se usa como respaldo al buscar por email.
 */
object SidebarPaths {
    fun usuario(db: FirebaseFirestore, uid: String) =
        db.collection("farmaciapp").document("app").collection("usuarios_farmacia").document(uid)

    fun farmacia(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId)

    fun suscripciones(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.suscripciones(db, farmaciaId)
}