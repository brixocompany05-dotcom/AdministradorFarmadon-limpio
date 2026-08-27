package com.app.administradorfarmadon.inventario.compartido.logica

import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await

/**
 * Fuente única de verdad para códigos de barras, aislada por SEDE (decisión A).
 * - Limpia y normaliza en un solo sitio (sin Regex duplicado).
 * - Centraliza la búsqueda de duplicados fuera de transacción (3 índices + sufijo fracción).
 * - Provee el índice atómico dentro de transacción para blindaje real sin carrera.
 *
 * Regla de negocio: un código limpio pertenece a un solo producto de la sede activa.
 * Los códigos derivados -B10 / -U1 se resuelven a su base para evitar que la caja cobre mal.
 */
object CodigoBarraHelper {

    private val REGEX_LIMPIEZA = Regex("[^a-zA-Z0-9_-]")
    private val REGEX_FRACCION = Regex("-(B|U)\\d+$", RegexOption.IGNORE_CASE)

    fun limpiar(raw: String): String =
        raw.replace(REGEX_LIMPIEZA, "").uppercase().trim()

    /**
     * ÚNICA forma de leer el código de barras de un documento de inventario
     * (mismo orden canónico que ProductoParser). Si el día de mañana cambia
     * el nombre del campo, se corrige aquí 1 vez y todas las pantallas
     * respiran igual. Prohibido copiar esta cadena fuera de este Helper.
     */
    fun leerCodigo(doc: com.google.firebase.firestore.DocumentSnapshot): String =
        doc.getString("codigoBarras") ?: doc.getString("codigo") ?: ""

    fun baseSinSufijo(codigoLimpio: String): String =
        codigoLimpio.replace(REGEX_FRACCION, "").trim()

    fun esValido(codigoLimpio: String): Boolean = codigoLimpio.isNotBlank()

    fun claveFicha(nombre: String, empaque: String, medida: String): String {
        fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")
        return "${norm(nombre)}|${norm(empaque)}|${norm(medida)}"
    }

    fun indiceFichaRef(db: FirebaseFirestore, clienteId: String, sucursalId: String, clave: String): com.google.firebase.firestore.DocumentReference =
        FarmadonPaths.indicesFichas(db, clienteId, sucursalId).document(clave.hashCode().toString() + "_" + clave.take(80).replace("/", "-").replace("|", "_"))

    fun verificarFichaUnicidadEnTransaccion(tx: Transaction, db: FirebaseFirestore, clienteId: String, claveFicha: String) {
        if (claveFicha.isBlank()) return
        val ref = indiceFichaRef(db, clienteId, SessionManager.sucursalIdEfectiva, claveFicha)
        val doc = tx.get(ref)
        if (doc.exists()) {
            throw IllegalArgumentException("Ya tienes registrado un producto con esa misma presentación (nombre + empaque + medida).")
        }
    }

    fun crearIndiceFichaEnTransaccion(tx: Transaction, db: FirebaseFirestore, clienteId: String, claveFicha: String, productoId: String) {
        if (claveFicha.isBlank()) return
        val ref = indiceFichaRef(db, clienteId, SessionManager.sucursalIdEfectiva, claveFicha)
        tx.set(ref, mapOf("productoId" to productoId, "clave" to claveFicha, "creadoEn" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
    }

    fun indiceRef(db: FirebaseFirestore, clienteId: String, sucursalId: String, codigoLimpio: String): DocumentReference =
        FarmadonPaths.indicesCodigos(db, clienteId, sucursalId).document(codigoLimpio)

    /**
     * Búsqueda fuera de transacción para feedback rápido en UI (debounce).
     * Revisa codigoBarras, codigo legacy, codigosSecundarios y base de fracción.
     * Retorna Pair(productoId, productoNombre) si hay dueño distinto a excludeId.
     */
    suspend fun buscarDuplicadoOutside(
        db: FirebaseFirestore,
        clienteId: String,
        codigoRaw: String,
        excludeId: String = "",
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ): Pair<String, String>? {
        val codLimpio = limpiar(codigoRaw)
        if (codLimpio.isBlank() || clienteId.isBlank()) return null

        val inv = FarmadonPaths.inventario(db, clienteId, sucursalId)

        val snap1 = inv.whereEqualTo("codigoBarras", codLimpio).limit(5).get().await()
        snap1.documents.firstOrNull { it.id != excludeId }?.let {
            return Pair(it.id, it.getString("nombre") ?: "Producto existente")
        }

        val snap2 = inv.whereEqualTo("codigo", codLimpio).limit(5).get().await()
        snap2.documents.firstOrNull { it.id != excludeId }?.let {
            return Pair(it.id, it.getString("nombre") ?: "Producto existente")
        }

        val snap3 = inv.whereArrayContains("codigosSecundarios", codLimpio).limit(5).get().await()
        snap3.documents.firstOrNull { it.id != excludeId }?.let {
            return Pair(it.id, it.getString("nombre") ?: "Producto existente")
        }

        val base = baseSinSufijo(codLimpio)
        if (base.isNotBlank() && base != codLimpio) {
            val base1 = inv.whereEqualTo("codigoBarras", base).limit(5).get().await()
                .documents.firstOrNull { it.id != excludeId }
                ?: inv.whereEqualTo("codigo", base).limit(5).get().await()
                    .documents.firstOrNull { it.id != excludeId }
            if (base1 != null) {
                val nombre = base1.getString("nombre") ?: "Producto existente"
                return Pair(base1.id, "$nombre (Fracción de $base)")
            }
        }
        return null
    }

    /**
     * Verificación atómica DENTRO de transaction usando el índice de la sede activa.
     * Lanza IllegalArgumentException si el código ya tiene dueño distinto.
     * Debe llamarse dentro de db.runTransaction { tx -> ... }.
     */
    fun verificarUnicidadEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        codigoLimpio: String,
        productoIdActual: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (codigoLimpio.isBlank()) return

        val ref = indiceRef(db, clienteId, sucursalId, codigoLimpio)
        val snap = tx.get(ref)
        if (snap.exists()) {
            val dueno = snap.getString("productoId") ?: ""
            if (dueno.isNotBlank() && dueno != productoIdActual) {
                val nombre = snap.getString("productoNombre") ?: "otro producto"
                throw IllegalArgumentException("El código '$codigoLimpio' ya pertenece a '$nombre'.")
            }
            if (dueno.isBlank()) {
                throw IllegalArgumentException("El código '$codigoLimpio' ya está asignado a otro producto.")
            }
        }

        val base = baseSinSufijo(codigoLimpio)
        if (base.isNotBlank() && base != codigoLimpio) {
            val baseRef = indiceRef(db, clienteId, sucursalId, base)
            val baseSnap = tx.get(baseRef)
            if (baseSnap.exists()) {
                val duenoBase = baseSnap.getString("productoId") ?: ""
                if (duenoBase.isNotBlank() && duenoBase != productoIdActual) {
                    val nombreBase = baseSnap.getString("productoNombre") ?: "otro producto"
                    throw IllegalArgumentException("El código base '$base' ya pertenece a '$nombreBase' (fracción $codigoLimpio).")
                }
            }
        }
    }

    fun crearIndiceEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        codigoLimpio: String,
        productoId: String,
        productoNombre: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (codigoLimpio.isBlank()) return
        val ref = indiceRef(db, clienteId, sucursalId, codigoLimpio)
        tx.set(ref, mapOf(
            "productoId" to productoId,
            "codigo" to codigoLimpio,
            "productoNombre" to productoNombre,
            "actualizadoEn" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        ))
    }

    fun borrarIndiceEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        codigoLimpio: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (codigoLimpio.isBlank()) return
        val ref = indiceRef(db, clienteId, sucursalId, codigoLimpio)
        tx.delete(ref)
    }
}