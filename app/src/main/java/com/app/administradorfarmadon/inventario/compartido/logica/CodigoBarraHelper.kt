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
 * Regla de negocio: un código limpio pertenece a un solo producto de la SEDE ACTIVA.
 * Los códigos derivados -B10 / -U1 se resuelven a su base para evitar que la caja cobre mal.
 *
 * 
 * CONTRATO DE AISLAMIENTO DE CÓDIGOS (LEER ANTES DE TOCAR ESTE ARCHIVO)
 * 
 * 1. La unicidad de códigos es POR SUCURSAL, no por farmacia. Cada tienda tiene
 *    su propia libreta (índice) y el mismo código puede existir en otra tienda de
 *    la misma dueña APUNTANDO AL MISMO PRODUCTO. ESO ES CORRECTO Y ESPERADO.
 *
 * 2. TODA búsqueda/escritura de código debe usar SIEMPRE la sucursal activa
 *    (SessionManager.sucursalIdEfectiva, valor por defecto de este helper).
 *    NUNCA hacer una consulta "de toda la farmacia" para resolver un código:
 *    la caja escanea en su propia tienda y debe descontar el stock de su tienda.
 *
 * 3. PROHIBIDO cambiar esto a "único para toda la farmacia". Hacerlo impediría
 *    registrar el mismo producto (mismo código) en una segunda tienda, rompiendo
 *    el flujo real de una cadena de farmacias. El diseño actual es el correcto.
 *
 * 4. Si algún día existe el módulo de VENTAS/CAJA, su búsqueda por código de barras
 *    DEBE pasar por este helper (o usar FarmadonPaths.indicesCodigos con la
 *    sucursalId activa). Bajo ningún concepto consultar el índice sin sucursalId.
 * 
 */
object CodigoBarraHelper {

    private val REGEX_LIMPIEZA = Regex("[^a-zA-Z0-9_-]")
    private val REGEX_FRACCION = Regex("-(B|U)\\d+$", RegexOption.IGNORE_CASE)

    fun limpiar(raw: String): String =
        raw.replace(REGEX_LIMPIEZA, "").uppercase().trim()

    /**
     * íšNICA forma de leer el código de barras de un documento de inventario
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

    fun verificarFichaUnicidadEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        claveFicha: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (claveFicha.isBlank()) return
        val ref = indiceFichaRef(db, clienteId, sucursalId, claveFicha)
        val doc = tx.get(ref)
        if (doc.exists()) {
            throw IllegalArgumentException("Ya tienes registrado un producto con esa misma presentación (nombre + empaque + medida).")
        }
    }

    fun crearIndiceFichaEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        claveFicha: String,
        productoId: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (claveFicha.isBlank()) return
        val ref = indiceFichaRef(db, clienteId, sucursalId, claveFicha)
        tx.set(ref, mapOf("productoId" to productoId, "clave" to claveFicha, "creadoEn" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
    }

    fun borrarIndiceFichaEnTransaccion(
        tx: Transaction,
        db: FirebaseFirestore,
        clienteId: String,
        claveFicha: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ) {
        if (claveFicha.isBlank()) return
        val ref = indiceFichaRef(db, clienteId, sucursalId, claveFicha)
        tx.delete(ref)
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
            return Pair(it.id, it.getString("nombre") ?: "otro producto")
        }

        val snap2 = inv.whereEqualTo("codigo", codLimpio).limit(5).get().await()
        snap2.documents.firstOrNull { it.id != excludeId }?.let {
            return Pair(it.id, it.getString("nombre") ?: "otro producto")
        }

        val snap3 = inv.whereArrayContains("codigosSecundarios", codLimpio).limit(5).get().await()
        snap3.documents.firstOrNull { it.id != excludeId }?.let {
            return Pair(it.id, it.getString("nombre") ?: "otro producto")
        }

        val base = baseSinSufijo(codLimpio)
        if (base.isNotBlank() && base != codLimpio) {
            val base1 = inv.whereEqualTo("codigoBarras", base).limit(5).get().await()
                .documents.firstOrNull { it.id != excludeId }
                ?: inv.whereEqualTo("codigo", base).limit(5).get().await()
                    .documents.firstOrNull { it.id != excludeId }
            if (base1 != null) {
                val nombre = base1.getString("nombre") ?: "otro producto"
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

    /** Código interno único legible (FMD-XXXXXX), verificado contra el índice antes de usarlo. */
    suspend fun generarCodigoInternoUnico(db: FirebaseFirestore, clienteId: String): String {
        val random = java.util.Random()
        for (intento in 1..10) {
            val numero = random.nextInt(900000) + 100000
            val candidato = "FMD-$numero"
            if (buscarDuplicadoOutside(db, clienteId, candidato) == null) {
                return candidato
            }
        }
        return "FMD-${System.currentTimeMillis().toString().takeLast(8)}"
    }

    /**
     * Genera un código interno único para una presentación de producto verificándolo
     * contra el índice real de códigos de la sede (no contra el inventario a secas).
     * Productos y presentaciones comparten el mismo índice, por lo que jamás se repite
     * ni se cruza un código entre dos productos o entre dos presentaciones.
     */
    suspend fun generarCodigoPresentacionUnico(
        db: FirebaseFirestore,
        clienteId: String,
        sucursalId: String = SessionManager.sucursalIdEfectiva
    ): String {
        val random = java.util.Random()
        repeat(50) {
            val numero = random.nextInt(900000) + 100000
            val candidato = "FMD-$numero"
            val ref = indiceRef(db, clienteId, sucursalId, candidato)
            if (!ref.get().await().exists() &&
                buscarDuplicadoOutside(db, clienteId, candidato, "", sucursalId) == null
            ) {
                return candidato
            }
        }
        return "FMD-${System.currentTimeMillis().toString().takeLast(8)}"
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
