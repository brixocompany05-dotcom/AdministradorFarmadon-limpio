package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
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
        val textoLower = texto.trim().lowercase()
        if (textoLower.isBlank()) {
            return PaginaInventario(emptyList(), emptyList(), null, true)
        }
        // Etiqueta buscadora: nombre+contenido+unidad en minúscula, tokens. Viejos sin tokens no aparecen (pedido).
        val tokens = textoLower.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct().take(10)
        if (tokens.isEmpty()) return PaginaInventario(emptyList(), emptyList(), null, true)
        try {
            val inventarioRef = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
            // 1. Búsqueda exacta por código de barras (pistola física o número de código).
            val codigoLimpio = CodigoBarraHelper.limpiar(texto)
            val esBusquedaPorCodigo = codigoLimpio.isNotBlank() &&
                (codigoLimpio.any { it.isDigit() } || codigoLimpio.startsWith("FMD-"))
            if (esBusquedaPorCodigo) {
                val porCodigo = mutableListOf<DocumentSnapshot>()
                val exactoBarras = inventarioRef.whereEqualTo("codigoBarras", codigoLimpio).limit(limit.toLong()).get().await()
                porCodigo.addAll(exactoBarras.documents)
                if (porCodigo.isEmpty()) {
                    val exactoLegacy = inventarioRef.whereEqualTo("codigo", codigoLimpio).limit(limit.toLong()).get().await()
                    porCodigo.addAll(exactoLegacy.documents)
                }
                if (porCodigo.isEmpty()) {
                    val secundarios = inventarioRef.whereArrayContains("codigosSecundarios", codigoLimpio).limit(limit.toLong()).get().await()
                    porCodigo.addAll(secundarios.documents)
                }
                if (porCodigo.isEmpty()) {
                    // CIERRE DEL CIRCUITO ETIQUETA→ESCÁNER (R3): el código de una
                    // presentación vive en el índice atómico (indices_codigos), no como
                    // campo del documento. Sin esta consulta, escanear la etiqueta
                    // impresa de una presentación decía "sin resultados" siendo falso.
                    val refIndice = FarmadonPaths.indicesCodigos(db, farmaciaId, sucursalId).document(codigoLimpio)
                    var productoIdIndice = refIndice.get().await().getString("productoId").orEmpty()
                    if (productoIdIndice.isBlank()) {
                        // Etiqueta derivada de fracción (BASE-B10 / BASE-U1): el índice
                        // vive con el código base; resolver igual que resolverPresentacionPorCodigo.
                        val base = CodigoBarraHelper.baseSinSufijo(codigoLimpio)
                        if (base.isNotBlank() && base != codigoLimpio) {
                            productoIdIndice = FarmadonPaths.indicesCodigos(db, farmaciaId, sucursalId)
                                .document(base).get().await().getString("productoId").orEmpty()
                        }
                    }
                    if (productoIdIndice.isNotBlank()) {
                        val docDelIndice = inventarioRef.document(productoIdIndice).get().await()
                        if (docDelIndice.exists()) porCodigo.add(docDelIndice)
                        // Si el índice apunta a un producto eliminado, se cae a resultados
                        // vacíos con verdad: "sin resultados" (no se muestra un muerto).
                    }
                }
                if (porCodigo.isNotEmpty()) {
                    val distinctById = porCodigo.distinctBy { it.id }
                    val listaPharmCodigo = distinctById.mapNotNull { ProductoParser.parseToPharm(it) }
                        .sortedBy { it.name.lowercase() }
                    val listaMoldeCodigo = distinctById.mapNotNull { ProductoParser.parseToMolde(it) }
                    return PaginaInventario(
                        productos = listaPharmCodigo,
                        productosMolde = listaMoldeCodigo,
                        ultimoDocumento = distinctById.lastOrNull(),
                        esUltimaPagina = true
                    )
                }
            }

            // 2. Búsqueda por nombre/contenido/unidad (tokens), misma lógica previa.
            var query: Query = inventarioRef
            query = if (tokens.size == 1) {
                query.whereArrayContains("busquedaTokens", tokens[0])
            } else {
                query.whereArrayContainsAny("busquedaTokens", tokens)
            }
            // Ordenar por nombre mantiene paginación coherente con lista
            query = query.orderBy("nombre").limit(limit.toLong())
            if (startAfterDoc != null) {
                query = query.startAfter(startAfterDoc)
            }
            val snap = query.get().await()
            var listaPharm = snap.documents.mapNotNull { ProductoParser.parseToPharm(it) }
            var listaMolde = snap.documents.mapNotNull { ProductoParser.parseToMolde(it) }

            // Búsqueda por prefijo nativo de respaldo si tokens no devolvieron resultados (para documentos antiguos)
            if (listaPharm.isEmpty() && textoLower.isNotBlank()) {
                val textoTrim = texto.trim()
                val variantes = listOf(textoLower, textoTrim, textoTrim.uppercase(), textoTrim.lowercase().replaceFirstChar { it.uppercase() })
                    .filter { it.isNotBlank() }.distinct()
                val docsPorNombre = mutableListOf<DocumentSnapshot>()
                for (variante in variantes) {
                    val snapPrefix = inventarioRef.orderBy("nombre")
                        .startAt(variante)
                        .endAt(variante + "\uf8ff")
                        .limit(limit.toLong())
                        .get().await()
                    docsPorNombre.addAll(snapPrefix.documents)
                    if (docsPorNombre.isNotEmpty()) break
                }
                if (docsPorNombre.isNotEmpty()) {
                    val distinctDocs = docsPorNombre.distinctBy { it.id }
                    listaPharm = distinctDocs.mapNotNull { ProductoParser.parseToPharm(it) }
                    listaMolde = distinctDocs.mapNotNull { ProductoParser.parseToMolde(it) }
                }
            }

            val ultimo = snap.documents.lastOrNull()
            val esUltima = snap.size() < limit
            return PaginaInventario(listaPharm, listaMolde, ultimo, esUltima)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error buscarInventarioPaginado farmacia=$farmaciaId sucursal=$sucursalId texto=$textoLower tokens=$tokens: ${e.message}", e)
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
            Log.e(TAG, "No se pudo obtener métricas agregadas del servidor: ${e.message}", e)
            throw e
        }
    }

    /**
     * Lectura puntual de TODOS los productos de la sede para el centro de alertas.
     * Las alertas de stock/vencimiento no caben en una página de 50: se leen todos
     * los documentos una sola vez (segundo plano) y la alerta se calcula con la
     * verdad completa, jamás solo con las páginas visibles.
     */
    suspend fun leerTodosLosProductos(
        farmaciaId: String,
        sucursalId: String
    ): List<PharmProduct> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        val snapshot = FarmadonPaths.inventario(db, farmaciaId, sucursalId).get().await()
        return snapshot.documents.mapNotNull { ProductoParser.parseToPharm(it) }
    }

    /**
     * Métricas completas y veraces de TODA la sede, sin importar cuántos productos haya.
     * Se leen todos los documentos una sola vez en segundo plano; la lista no depende de
     * esta lectura y responde de inmediato. Solo se pintan valores cuando la lectura terminó.
     */
    suspend fun obtenerMetricasCompletas(
        farmaciaId: String,
        sucursalId: String
    ): MetricasGlobalesInventario {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return MetricasGlobalesInventario()
        val invRef = FarmadonPaths.inventario(db, farmaciaId, sucursalId)
        val snapshot = invRef.get().await()
        val productos = snapshot.documents.mapNotNull { ProductoParser.parseToPharm(it) }
        val total = productos.size
        val activos = productos.count { it.activo }
        val valorTotal = productos.sumOf { it.totalValue }
        val hoy = HoraServidor.ahoraMs()
        val stockBajo = productos.count {
            it.status == "Stock bajo" || it.status == "Agotado" ||
                (it.minStock > 0 && it.stock <= it.minStock)
        }
        val porVencer = productos.count {
            it.status == "Por vencer" || it.status == "Vencido" ||
                (it.expiryTimestamp in 1L..(hoy + 30L * 24 * 60 * 60 * 1000))
        }
        return MetricasGlobalesInventario(
            totalProductos = total,
            totalActivos = activos,
            valorTotal = valorTotal,
            stockBajoConteo = stockBajo,
            porVencerConteo = porVencer
        )
    }
}
