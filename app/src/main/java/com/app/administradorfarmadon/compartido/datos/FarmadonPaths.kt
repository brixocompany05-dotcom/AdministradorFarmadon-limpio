package com.app.administradorfarmadon.compartido.datos

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas canónicas de Farmadon (base nueva, sin migración).
 *
 * Modelo limpio:
 * - clientes/{clienteId}          ──†’ SOLO identidad del cliente (dueño): dueno, nombre, email, dni.
 * - farmacias/{farmaciaId}        ──†’ LA FARMACIA (farmaciaId == clienteId, vínculo 1:1 por uid del dueño,
 *                                    con su campo clienteId adentro): nombreFarmacia, ruc, plan, estado.
 *   - farmacias/{f}/sucursales/{sucursalId} ──†’ LAS TIENDAS, y debajo TODO el operativo de esa sede:
 *     inventario, movimientos, alertasInventario, compras_facturas, proveedores, indices_codigos,
 *     reclamos_proveedores, canjes_proveedores, catalogos, auditoria, auditorias.
 *
 * R1: cada farmacia ve y toca SOLO sus datos; la regla del servidor aísla por farmaciaId.
 */
object FarmadonPaths {

    fun farmacias(db: FirebaseFirestore): CollectionReference =
        db.collection("farmaciapp").document("app").collection("farmacias")

    /** La farmacia. farmaciaId == clienteId (el uid del dueño es el id de su farmacia). */
    fun farmacia(db: FirebaseFirestore, farmaciaId: String): DocumentReference =
        farmacias(db).document(farmaciaId)

    fun suscripciones(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("suscripciones")

    fun usuarios(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("usuarios")

    fun sucursales(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("sucursales")

    fun sucursal(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): DocumentReference =
        sucursales(db, farmaciaId).document(sucursalId)

    fun inventario(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("inventario")

    fun movimientos(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("movimientos")

    fun alertasInventario(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("alertasInventario")

    fun comprasFacturas(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("compras_facturas")

    fun proveedores(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("proveedores")

    fun indicesCodigos(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("indices_codigos")

    fun indicesFichas(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("indices_fichas")

    fun reclamosProveedores(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("reclamos_proveedores")

    fun canjesProveedores(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("canjes_proveedores")

    fun catalogos(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("catalogos")

    fun auditoria(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("auditoria")

    fun auditorias(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("auditorias")

    fun pedidosCompra(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("pedidos_compra")

    fun carritoReposicion(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("carrito_reposicion")
}
