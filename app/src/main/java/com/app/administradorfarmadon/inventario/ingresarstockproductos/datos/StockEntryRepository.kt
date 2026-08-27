package com.app.administradorfarmadon.inventario.ingresarstockproductos.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repositorio Enterprise de Recepción de Lotes y Facturas de Compra.
 * Asienta de forma atómica:
 * 1. Existencias físicas, Lote y recálculo FEFO.
 * 2. Factura General de Compra con conciliación acumulada.
 * 3. Asiento inmutable en Kardex con costos de adquisición.
 */
class StockEntryRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "StockEntryRepository"
    }

    private fun getClienteId(): String {
        return SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    suspend fun obtenerProducto(productId: String): MoldeProductos? {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || productId.isBlank()) return null
        val ref = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(productId)
        return try {
            // Cache primero → apertura instantánea 0ms (offline-first). Luego servidor corrige si hace falta.
            val cacheSnap = try { ref.get(com.google.firebase.firestore.Source.CACHE).await() } catch (_: Exception) { null }
            if (cacheSnap != null && cacheSnap.exists()) {
                val parsed = ProductoParser.parseToMolde(cacheSnap)
                // Revalida en segundo plano sin bloquear UI
                try { ref.get(com.google.firebase.firestore.Source.SERVER).await().let { if (it.exists()) ProductoParser.parseToMolde(it) ?: parsed else parsed } } catch (_: Exception) { parsed }
                return parsed
            }
            val doc = ref.get(com.google.firebase.firestore.Source.SERVER).await()
            return ProductoParser.parseToMolde(doc)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo producto para ingreso de stock: ${e.message}", e)
            null
        }
    }

    suspend fun registrarIngresoLote(
        productId: String,
        productoNombre: String,
        empaque: String,
        numeroLote: String,
        fechaVencimiento: String,
        cantidadTotal: Double,
        cantidadComprada: Double,
        bonificacionGratis: Double,
        costoTotal: Double,
        costoUnitario: Double,
        proveedorId: String,
        proveedorNombre: String,
        rucProveedor: String,
        numeroFactura: String,
        montoTotalFactura: Double,
        condicionPago: String,
        fechaVencimientoPago: String,
        stockMinimoNuevo: Double = 0.0,
        usuarioEmail: String
    ): Result<Unit> {
        // Fuente única — delega al repositorio unificado (1 tx para lote+stock+kardex+factura inmutable)
        // Si hay pedido pendiente, no se autodespacha a escondidas — el ViewModel ofrece vincular
        return com.app.administradorfarmadon.inventario.compartido.datos.IngresoMercaderiaRepository(db).ingresarLoteSimple(
            productId = productId,
            productoNombre = productoNombre,
            empaque = empaque,
            numeroLote = numeroLote,
            vencimiento = fechaVencimiento,
            cantidadTotal = cantidadTotal,
            cantidadComprada = cantidadComprada,
            bonificacion = bonificacionGratis,
            costoTotal = costoTotal,
            costoUnitario = costoUnitario,
            proveedorId = proveedorId,
            proveedorNombre = proveedorNombre,
            rucProveedor = rucProveedor,
            numeroFactura = numeroFactura,
            montoTotalFactura = montoTotalFactura,
            condicionPago = condicionPago,
            fechaVencimientoPago = fechaVencimientoPago,
            stockMinimoNuevo = stockMinimoNuevo,
            usuarioEmail = usuarioEmail
        )
    }

}
