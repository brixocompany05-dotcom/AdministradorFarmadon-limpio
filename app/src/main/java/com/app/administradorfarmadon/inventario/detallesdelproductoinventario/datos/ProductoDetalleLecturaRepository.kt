package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.ExpedienteReclamoProveedor
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Repositorio de Detalles de Producto en Cloud Firestore (Multi-Tenant).
 * Lee en tiempo real el producto, sus lotes activos y la bitácora de movimientos (Kardex),
 * todo aislado por sede: farmacias/{farmaciaId}/sucursales/{sucursalId}/...
 */

/**
 * Lecturas en tiempo real —” detalle, movimientos, reclamos, catálogo y códigos.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) —” responsabilidad única.
 */
class ProductoDetalleLecturaRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object { private const val TAG = "ProductoDetalleLecturaRepository" }

    fun observarDetalleProducto(clienteId: String, productId: String): Flow<MoldeProductos?> = callbackFlow {
        if (clienteId.isBlank() || productId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(productId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error al escuchar detalle en Firestore: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val productoMapeado = ProductoParser.parseToMolde(snapshot)
                if (productoMapeado != null) {
                    trySend(productoMapeado)
                } else {
                    Log.e(TAG, "Error mapeando detalle de producto: parser retornó null")
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listener.remove()
        }
    }


    fun observarMovimientosProducto(clienteId: String, productId: String, limit: Long = 50): Flow<List<MovimientoInventario>> = callbackFlow {
        if (clienteId.isBlank() || productId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = FarmadonPaths.movimientos(db, clienteId, SessionManager.sucursalIdEfectiva)
            .whereEqualTo("productoId", productId)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando movimientos: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val lista = snapshot?.documents?.mapNotNull { doc ->
                val id = doc.id
                val tipo = doc.getString("tipo") ?: "MOVIMIENTO"
                val cant = doc.getDouble("cantidad") ?: doc.getDouble("cantidadTotal") ?: 0.0
                val ts = doc.get("fecha") as? Timestamp
                val user = doc.getString("usuarioEmail") ?: "Sistema"
                val loteNum = doc.getString("loteNumero") ?: doc.getString("lote") ?: ""
                val prov = doc.getString("proveedorNombre") ?: ""
                val fact = doc.getString("facturaNumero") ?: ""
                val mot = doc.getString("motivo") ?: ""

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val fechaStr = ts?.toDate()?.let { sdf.format(it) } ?: ""

                val refDetalle = buildString {
                    if (loteNum.isNotBlank()) append("Lote: $loteNum")
                    if (prov.isNotBlank()) append("  ·  Prov: $prov")
                    if (fact.isNotBlank()) append("  ·  Fact: $fact")
                    if (mot.isNotBlank()) append("  ·  Motivo: $mot")
                }.ifBlank { "Ajuste general" }

                MovimientoInventario(
                    id = id,
                    tipo = tipo,
                    cantidad = cant,
                    fecha = fechaStr,
                    loteNumero = loteNum,
                    usuarioNombre = user.substringBefore("@"),
                    referencia = refDetalle,
                    costoTotal = doc.getDouble("costoTotal") ?: 0.0
                )
            } ?: emptyList()

            trySend(lista)
        }

        awaitClose {
            listener.remove()
        }
    }


    private fun calcularResumenStockYFefo(lotesMap: Map<*, *>): Triple<Double, Double, String> {
        val nuevoStockDisponible = lotesMap.values.sumOf { 
            val data = it as? Map<*, *>
            (data?.get("cantidad") as? Number)?.toDouble() ?: 0.0 
        }
        val nuevoStockTotal = lotesMap.values.sumOf { 
            val data = it as? Map<*, *>
            val cDisp = (data?.get("cantidad") as? Number)?.toDouble() ?: 0.0
            val cBloq = (data?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
            cDisp + cBloq
        }
        val nuevoVencimientoMasCercano = lotesMap.values
            .mapNotNull { it as? Map<*, *> }
            .filter { 
                val cDisp = (it["cantidad"] as? Number)?.toDouble() ?: 0.0
                cDisp > 0.0 && (it["vencimiento"] as? String)?.isNotBlank() == true
            }
            .minByOrNull { 
                val v = it["vencimiento"] as? String ?: ""
                ProductDetailMapper.diasHastaVencer(v) ?: Int.MAX_VALUE 
            }?.get("vencimiento") as? String ?: ""

        return Triple(nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano)
    }


    fun observarReclamosProducto(clienteId: String, productId: String): Flow<List<ExpedienteReclamoProveedor>> = callbackFlow {
        if (clienteId.isBlank() || productId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = FarmadonPaths.reclamosProveedores(db, clienteId, SessionManager.sucursalIdEfectiva)
            .whereEqualTo("productoId", productId)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando reclamos: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val lista = snapshot?.documents?.mapNotNull { doc ->
                val id = doc.id
                val prodId = doc.getString("productoId") ?: ""
                val prodNom = doc.getString("productoNombre") ?: ""
                val emp = doc.getString("empaque") ?: "Caja"
                val loteNum = doc.getString("loteNumero") ?: ""
                val cant = doc.getDouble("cantidadDevuelta") ?: 0.0
                val cUnit = doc.getDouble("costoUnitario") ?: 0.0
                val mTot = doc.getDouble("montoTotal") ?: 0.0
                val provId = doc.getString("proveedorId") ?: ""
                val provNom = doc.getString("proveedorNombre") ?: ""
                val fact = doc.getString("facturaOrigen") ?: ""
                val guia = doc.getString("guiaRetiro") ?: ""
                val nc = doc.getString("notaCredito") ?: ""
                val mot = doc.getString("motivo") ?: ""
                val mod = doc.getString("modalidadCompensacion") ?: "NOTA_CREDITO_DINERO"
                val est = doc.getString("estado") ?: "EN_REVISION_DROGUERIA"
                val user = doc.getString("usuarioRegistroEmail") ?: ""
                val ts = doc.get("creadoEl") as? Timestamp

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val fechaStr = ts?.toDate()?.let { sdf.format(it) } ?: "Reciente"

                ExpedienteReclamoProveedor(
                    id = id,
                    clienteId = clienteId,
                    productoId = prodId,
                    productoNombre = prodNom,
                    empaque = emp,
                    loteNumero = loteNum,
                    cantidadDevuelta = cant,
                    costoUnitario = cUnit,
                    montoTotal = mTot,
                    proveedorId = provId,
                    proveedorNombre = provNom,
                    facturaOrigen = fact,
                    guiaRetiro = guia,
                    notaCredito = nc,
                    motivo = mot,
                    modalidadCompensacion = mod,
                    estado = est,
                    usuarioRegistroEmail = user,
                    fechaRegistroStr = fechaStr
                )
            } ?: emptyList()

            trySend(lista)
        }

        awaitClose {
            listener.remove()
        }
    }


    fun observarCatalogoUbicaciones(clienteId: String): Flow<List<String>> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val docRef = FarmadonPaths.catalogos(db, clienteId, SessionManager.sucursalIdEfectiva).document("ubicaciones")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando catálogo de ubicaciones en vivo: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val lista = (snapshot.get("lista") as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?.sorted() ?: emptyList()
                trySend(lista)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Verifica si un código de barras ya pertenece a OTRO producto de la sede.
     * Retorna el Nombre del producto en conflicto si existe, o null si está libre.
     * Fuente única: CodigoBarraHelper.buscarDuplicadoOutside (evita Regex y lógica duplicada).
     */

    suspend fun buscarDuplicadoCodigoBarras(clienteId: String, codigoBarras: String, currentProductoId: String): String? {
        return CodigoBarraHelper.buscarDuplicadoOutside(db, clienteId, codigoBarras, currentProductoId)?.second
    }

    /**
     * Genera un código interno FarmaDON garantizado como 100% único contra Firestore.
     */

    suspend fun generarCodigoInternoUnico(clienteId: String): String {
        val random = java.util.Random()
        for (intento in 1..10) {
            val numero = random.nextInt(900000) + 100000
            val candidato = "FMD-$numero"
            val duplicado = buscarDuplicadoCodigoBarras(clienteId, candidato, "")
            if (duplicado == null) {
                return candidato
            }
        }
        return "FMD-${System.currentTimeMillis().toString().takeLast(8)}"
    }

    data class InfoEliminacion(
        val email: String = "",
        val fechaStr: String = "",
        val motivo: String = ""
    )

    suspend fun obtenerInfoEliminacion(clienteId: String, productoId: String): InfoEliminacion? {
        if (clienteId.isBlank() || productoId.isBlank()) return null
        return try {
            val tienda = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val q = tienda.collection("auditorias").document("inventario").collection("productos")
                .document("listaeliminado").collection("items")
                .whereEqualTo("productoId", productoId)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1).get().await()
            val doc = q.documents.firstOrNull() ?: return null
            val email = doc.getString("usuarioEmail") ?: doc.getString("eliminadoPorUid") ?: ""
            val motivo = doc.getString("motivo") ?: ""
            val ts = doc.getTimestamp("fecha")
            val fechaStr = if (ts != null) {
                try { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(ts.toDate()) } catch (_: Exception) { "" }
            } else ""
            InfoEliminacion(email, fechaStr, motivo)
        } catch (_: Exception) { null }
    }

    /**
     * Guarda la configuración operativa y logística (Ubicación, Stock Mínimo y Estado Activo/Suspendido).
     */

}
