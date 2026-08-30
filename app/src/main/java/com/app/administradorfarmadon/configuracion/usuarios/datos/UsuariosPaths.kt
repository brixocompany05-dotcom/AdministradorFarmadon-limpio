package com.app.administradorfarmadon.configuracion.usuarios.datos

import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas de base de datos exclusivas del dominio de Personal y Usuarios de la Farmacia.
 * El personal de la farmacia y su auditoría viven en farmacias/{farmaciaId}.
 * clientes/{clienteId} solo se usa para resolver la identidad del dueño.
 */
object UsuariosPaths {
    fun farmacia(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId)

    fun usuariosSubcoleccion(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.usuarios(db, farmaciaId)

    fun sucursales(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.sucursales(db, farmaciaId)

    fun auditoriaLocal(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId).collection("auditoria")

    fun usuariosGlobal(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("usuarios_farmacia")
}
