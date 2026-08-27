package com.app.administradorfarmadon.autenticacion.datos

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas de base de datos exclusivas del dominio de Autenticación, Registro, Lista Negra y Validación de Clientes.
 */
object AuthPaths {
    fun solicitudes(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("solicitudes")

    fun documentos(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("documentos")

    fun historialTiempoSolicitudes(db: FirebaseFirestore) =
        db.collection("brixo").document("panel").collection("historialdetiemposolicitudes")

    fun clientes(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("clientes")

    fun usuariosFarmacia(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("usuarios_farmacia")

    fun listaNegra(db: FirebaseFirestore) =
        db.collection("brixo").document("clientes_restringidos").collection("lista_negra")

    fun existeRestringido(db: FirebaseFirestore) =
        db.collection("brixo").document("existencia").collection("restringidos")
}
