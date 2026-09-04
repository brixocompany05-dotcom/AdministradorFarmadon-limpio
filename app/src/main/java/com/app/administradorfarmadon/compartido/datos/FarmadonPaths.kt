package com.app.administradorfarmadon.compartido.datos

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas canónicas de Farmadon (base nueva, sin migración).
 *
 * Modelo limpio:
 * - clientes/{clienteId}          → SOLO identidad del cliente (dueño): dueno, nombre, email, dni.
 * - farmacias/{farmaciaId}        → LA FARMACIA (farmaciaId == clienteId, vínculo 1:1 por uid del dueño,
 *                                    con su campo clienteId adentro): nombreFarmacia, ruc, plan, estado.
 *   - farmacias/{f}/sucursales/{sucursalId} → LAS TIENDAS, y debajo TODO el operativo de esa sede:
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

    // ── MÓDULO POS / VENTAS (todo aislado por farmacia + sucursal, R1) ──

    /** Ventas emitidas por la caja de la sucursal. */
    fun ventas(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("ventas")

    /** Ventas parqueadas (suspendidas) en vivo; se recuperan desde cualquier terminal de la sede. */
    fun ventasSuspendidas(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("ventas_suspendidas")

    /** Turnos de caja (aperturas/cierres). Un solo turno ABIERTO por sucursal,
     *  garantizado por el puntero atómico [estadoCaja]. */
    fun cajaSesiones(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("caja_sesiones")

    /**
     * Puntero atómico del turno de caja vigente (doc único `actual` en `caja_sesiones`).
     * Toda venta/devolución/movimiento lo lee DENTRO de su transacción:
     * solo hay una caja abierta por sede y el dinero esperado siempre cuadra.
     */
    fun estadoCaja(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): DocumentReference =
        sucursal(db, farmaciaId, sucursalId).collection("caja_sesiones").document("actual")

    /** Entradas/salidas de dinero de la caja (ventas, devoluciones, ingresos, retiros). */
    fun cajaMovimientos(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("caja_movimientos")

    /** Contadores atómicos de series de comprobantes (doc `ventas`: ultimaBoleta/ultimaFactura). */
    fun contadores(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("contadores")

    /** Devoluciones de ventas (notas de crédito internas). */
    fun devoluciones(db: FirebaseFirestore, farmaciaId: String, sucursalId: String): CollectionReference =
        sucursal(db, farmaciaId, sucursalId).collection("devoluciones")

    /** Directorio de clientes de la farmacia (a nivel farmacia: compran en cualquier sede). */
    fun clientesDirectorio(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("clientes")

    /** Bandeja e índice fiscal de comprobantes electrónicos (FASE 12). */
    fun facturacionDocumentos(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("facturacion_documentos")

    /** Configuración fiscal a nivel farmacia (emisor, series, credenciales). */
    fun facturacionConfig(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("facturacion_config")

    /** Documento asignador de índices secuenciales de series de sucursales. */
    fun facturacionSeries(db: FirebaseFirestore, farmaciaId: String): DocumentReference =
        facturacionConfig(db, farmaciaId).document("series")

    /** Documento único del emisor fiscal APISUNAT de la farmacia. */
    fun facturacionEmisor(db: FirebaseFirestore, farmaciaId: String): DocumentReference =
        facturacionConfig(db, farmaciaId).document("emisor")

    /** Historial append-only de cambios del emisor (auditoría: qué cambió, quién, cuándo). */
    fun facturacionEmisorHistorial(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        facturacionConfig(db, farmaciaId).document("emisor").collection("historial")

    /** Tickets de soporte e incidencias de la farmacia dirigidos a BRIXO Central (R1). */
    fun soporteTickets(db: FirebaseFirestore, farmaciaId: String): CollectionReference =
        farmacia(db, farmaciaId).collection("soporte_tickets")
}
