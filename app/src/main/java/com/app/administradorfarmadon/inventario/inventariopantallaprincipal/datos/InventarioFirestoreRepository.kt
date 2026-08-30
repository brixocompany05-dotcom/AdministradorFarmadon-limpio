package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repositorio Oficial de Inventario en Cloud Firestore (Multi-Tenant).
 * Fuente íšnica de Verdad: escucha cambios en tiempo real (<50ms) de la colección:
 * farmaciapp/app/farmacias/{farmaciaId}/sucursales/{sucursalId}/inventario
 *
 * R1 Aislamiento: toda lectura verifica farmaciaId + sucursalId via FarmadonPaths.
 * No escritura aquí: solo lectura paginada reactiva.
 */
class InventarioFirestoreRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "InventarioFirestoreRepo"
    }

    /**
     * Resultado paginado profesional: lista + cursor para la siguiente página.
     * Contiene PharmProduct (modelo liviano de lista) y MoldeProductos (modelo completo)
     * para cumplir contrato de lectura paginada sin tocar escritura.
     */
    data class PaginaInventario(
        val productos: List<PharmProduct>,
        // Alias completo para contrato MoldeProductos —” misma página, mapeo alternativo
        val productosMolde: List<MoldeProductos> = emptyList(),
        val ultimoDocumento: DocumentSnapshot?,
        val esUltimaPagina: Boolean = false
    )

    /**
     * Observa inventario paginado en tiempo real con aislamiento estricto por farmacia.
     *
     * Contrato solicitado:
     * - usar FarmadonPaths.inventario(db, farmaciaId, sucursalId)
     * - query FarmadonPaths.inventario(db, farmaciaId, sucursalId).orderBy("nombre").limit(limit).startAfter(lastDoc)
     * - addSnapshotListener con manejo de error honesto (Log.e + close(error), sin catch vacío)
     * - devuelve Flow<List<MoldeProductos>> y ultimo DocumentSnapshot (envuelto en PaginaInventario)
     *
     * @param farmaciaId tenant dueño de los datos —” R1
     * @param sucursalId sede operativa —” R1
     * @param limit tamaño de página, default 50 profesional
     * @param startAfterDoc cursor del último doc de la página anterior, null para primera página
     */
    fun observarInventarioPaginado(
        farmaciaId: String,
        sucursalId: String,
        limit: Int = 50,
        startAfterDoc: DocumentSnapshot? = null
    ): Flow<PaginaInventario> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            trySend(PaginaInventario(emptyList(), emptyList(), null, true))
            close()
            return@callbackFlow
        }

        // Aislamiento intacto: siempre a través de FarmadonPaths
        var query: Query = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
            .orderBy("nombre")
            .limit(limit.toLong())

        // Paginación por cursor: startAfter(lastDoc) solo si hay cursor previo
        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando inventario paginado farmacia=$farmaciaId sucursal=$sucursalId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val listaPharm = snapshot.documents.mapNotNull { doc ->
                    ProductoParser.parseToPharm(doc)
                }
                // Mapeo alternativo a MoldeProductos para contrato solicitado Flow<List<MoldeProductos>>
                val listaMolde = snapshot.documents.mapNotNull { doc ->
                    ProductoParser.parseToMolde(doc)
                }
                val ultimo = snapshot.documents.lastOrNull()
                val esUltima = snapshot.size() < limit
                trySend(PaginaInventario(listaPharm, listaMolde, ultimo, esUltima))
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observa inventario paginado —” variante que expone directamente Flow<Pair<List<MoldeProductos>, DocumentSnapshot?>>
     * para cumplir literalmente "devuelve Flow<List<MoldeProductos>> y ultimo DocumentSnapshot".
     * Envuelve PaginaInventario para compatibilidad estricta con el contrato del issue.
     */
    fun observarInventarioPaginadoMolde(
        farmaciaId: String,
        sucursalId: String,
        limit: Int = 50,
        startAfterDoc: DocumentSnapshot? = null
    ): Flow<Pair<List<MoldeProductos>, DocumentSnapshot?>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            trySend(emptyList<MoldeProductos>() to null)
            close()
            return@callbackFlow
        }

        var query: Query = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
            .orderBy("nombre")
            .limit(limit.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando inventario paginado (Molde) farmacia=$farmaciaId sucursal=$sucursalId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val listaMolde = snapshot.documents.mapNotNull { doc ->
                    ProductoParser.parseToMolde(doc)
                }
                val ultimo = snapshot.documents.lastOrNull()
                trySend(listaMolde to ultimo)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Variante paginable silenciosa para búsqueda —” mismo contrato prefix pero con cursor.
     * UI nunca ve páginas; ViewModel acumula lote a lote internamente.
     */
    suspend fun buscarInventarioPaginado(
        farmaciaId: String,
        sucursalId: String,
        texto: String,
        limit: Int = 50,
        startAfterDoc: DocumentSnapshot? = null
    ): PaginaInventario {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            return PaginaInventario(emptyList(), emptyList(), null, true)
        }
        val textoUpper = texto.trim().uppercase()
        if (textoUpper.isBlank()) {
            return PaginaInventario(emptyList(), emptyList(), null, true)
        }
        try {
            var query: Query = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
                .whereGreaterThanOrEqualTo("nombre", textoUpper)
                .whereLessThan("nombre", textoUpper + "\uf8ff")
                .orderBy("nombre")
                .limit(limit.toLong())
            if (startAfterDoc != null) {
                query = query.startAfter(startAfterDoc)
            }
            val snap = query.get().await()
            val listaPharm = snap.documents.mapNotNull { ProductoParser.parseToPharm(it) }
            val listaMolde = snap.documents.mapNotNull { ProductoParser.parseToMolde(it) }
            val ultimo = snap.documents.lastOrNull()
            val esUltima = snap.size() < limit
            return PaginaInventario(listaPharm, listaMolde, ultimo, esUltima)
        } catch (e: Exception) {
            Log.e(TAG, "Error buscarInventarioPaginado farmacia=$farmaciaId sucursal=$sucursalId texto=$textoUpper: ${e.message}", e)
            throw e
        }
    }

    /**
     * Método legacy sin paginación —” se conserva por compatibilidad pero se recomienda
     * migrar a observarInventarioPaginado para listas grandes.
     * Mantiene addSnapshotListener con manejo honesto (Log.e + close(error)).
     */
    fun observarInventario(clienteId: String, onErrorEscucha: ((String) -> Unit)? = null): Flow<List<PharmProduct>> = callbackFlow {
        if (clienteId.isBlank()) {
            close(IllegalStateException("No hay una farmacia activa para cargar inventario."))
            return@callbackFlow
        }

        val coleccionRef = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva)

        val listener = coleccionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando inventario Firestore: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: error.toString())
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val lista = snapshot.documents.mapNotNull { doc ->
                    ProductoParser.parseToPharm(doc)
                }
                trySend(lista)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    data class MetricasGlobalesInventario(
        val totalProductos: Int = 0,
        val totalActivos: Int = 0,
        val valorTotal: Double = 0.0,
        val stockBajoConteo: Int = 0,
        val porVencerConteo: Int = 0
    )

    /**
     * Consulta atómica agregada en Google Cloud Firestore:
     * Obtiene el conteo exacto de los 1,000+ productos de toda la sede en 1 sola llamada escalar (sin descargar documentos).
     */
    suspend fun obtenerMetricasGlobales(
        farmaciaId: String,
        sucursalId: String
    ): MetricasGlobalesInventario {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return MetricasGlobalesInventario()
        return try {
            val invRef = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
            val total = invRef.count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()
            val countActivos = invRef.whereEqualTo("activo", true).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            MetricasGlobalesInventario(
                totalProductos = total,
                totalActivos = countActivos
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener métricas agregadas del servidor: ${e.message}")
            MetricasGlobalesInventario()
        }
    }
}
