package com.app.administradorfarmadon.configuracion.sucursales.datos

import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas de base de datos exclusivas del dominio de Sucursales y Sedes.
 * Las tiendas y la auditoría de gestión viven en farmacias/{farmaciaId}.
 * clientes/{clienteId} solo se usa para resolver la identidad del dueño.
 */
object SucursalesPaths {
    fun farmacia(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId)

    fun sucursales(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.sucursales(db, farmaciaId)

    fun auditoriaLocal(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.farmacia(db, farmaciaId).collection("auditoria")

    fun usuariosFarmacia(db: FirebaseFirestore) =
        db.collection("farmaciapp").document("app").collection("usuarios_farmacia")

    fun usuarios(db: FirebaseFirestore, farmaciaId: String) =
        FarmadonPaths.usuarios(db, farmaciaId)
}
