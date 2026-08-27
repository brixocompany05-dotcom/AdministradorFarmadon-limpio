package com.app.administradorfarmadon.organizacion.datos

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Rutas canónicas del esquema Organization-first (arranque limpio).
 *
 * Jerarquía:
 *   orgs/{orgId}                          ← EL TENIENTE ES LA EMPRESA
 *     miembros/{uid}                      ← personas con rol + alcance
 *     farmacias/{farmaciaId}              ← cada local (país define su moneda)
 *       equipo/{uid}                      ← rol local opcional
 *       impuestos/pais                    ← configuración fiscal
 *       suscripciones                     ← quien paga es la ORG
 *       sucursales/{sede}/...             ← operativo actual INTACTO debajo
 *
 * Convivencia: FarmadonPaths sigue sirviendo al operativo heredado.
 * La migración del puntero operativo hacia este objeto se hará en un solo punto.
 */
object OrgPaths {

    private const val RAIZ = "orgs"

    fun raiz(db: FirebaseFirestore) =
        db.collection(RAIZ)

    fun org(db: FirebaseFirestore, orgId: String) =
        raiz(db).document(orgId)

    // ── PERSONAS DE LA ORGANIZACIÓN ──
    fun miembros(db: FirebaseFirestore, orgId: String) =
        org(db, orgId).collection("miembros")

    fun miembro(db: FirebaseFirestore, orgId: String, uid: String) =
        miembros(db, orgId).document(uid)

    // ── FARMACIAS DE LA ORGANIZACIÓN ──
    fun farmacias(db: FirebaseFirestore, orgId: String) =
        org(db, orgId).collection("farmacias")

    fun farmacia(db: FirebaseFirestore, orgId: String, farmaciaId: String) =
        farmacias(db, orgId).document(farmaciaId)

    /** Rol local opcional por persona dentro de una farmacia concreta. */
    fun equipoFarmacia(db: FirebaseFirestore, orgId: String, farmaciaId: String) =
        farmacia(db, orgId, farmaciaId).collection("equipo")

    /** Configuración fiscal vigente (por país) de esta farmacia. */
    fun impuestosFarmacia(db: FirebaseFirestore, orgId: String, farmaciaId: String) =
        farmacia(db, orgId, farmaciaId).collection("impuestos")

    // ── SUSCRIPCIÓN Y COBROS (quien paga es la organización) ──
    fun suscripciones(db: FirebaseFirestore, orgId: String) =
        org(db, orgId).collection("suscripciones")

    // ── OPERATIVO: delega la forma exacta a FarmadonPaths cuando la sesión
    //    exponga orgId; por ahora solo fija el ancla documental de la farmacia.
    fun sucursales(db: FirebaseFirestore, orgId: String, farmaciaId: String) =
        farmacia(db, orgId, farmaciaId).collection("sucursales")
}
