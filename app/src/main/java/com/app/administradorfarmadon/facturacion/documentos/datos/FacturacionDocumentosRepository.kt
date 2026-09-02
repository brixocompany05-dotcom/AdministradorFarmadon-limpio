package com.app.administradorfarmadon.facturacion.documentos.datos

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FacturacionDocumentosRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val ventasRepo: VentasRepository = VentasRepository()
) {
    companion object {
        private const val TAG = "FacturacionDocsRepo"
    }

    /**
     * Escucha viva de los documentos fiscales emitidos por la farmacia (Regla R8 - Verdad Vigente).
     * Lee directamente de `farmacias/{farmaciaId}/facturacion_documentos` ordenado cronológicamente.
     */
    fun observarDocumentos(farmaciaId: String): Flow<List<FacturacionDocumento>> = callbackFlow {
        if (farmaciaId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .orderBy("fechaMs", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando facturacion_documentos: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val documentos = snapshot.documents.mapNotNull { doc ->
                    parseDocumento(doc.id, doc.data)
                }
                trySend(documentos)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene el detalle de una venta vinculada a un comprobante fiscal.
     */
    suspend fun obtenerVentaVinculada(farmaciaId: String, sucursalId: String, ventaId: String): Venta? {
        if (farmaciaId.isBlank() || ventaId.isBlank()) return null
        return try {
            if (sucursalId.isNotBlank()) {
                val snap = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId).get().await()
                if (snap.exists()) {
                    return ventasRepo.parseVenta(snap.id, snap.data)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando venta vinculada $ventaId: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene el detalle de una devolución vinculada a una Nota de Crédito.
     */
    suspend fun obtenerDevolucionVinculada(farmaciaId: String, sucursalId: String, devolucionId: String): DevolucionVenta? {
        if (farmaciaId.isBlank() || devolucionId.isBlank() || sucursalId.isBlank()) return null
        return try {
            val snap = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId).document(devolucionId).get().await()
            if (!snap.exists()) return null
            val data = snap.data ?: return null

            @Suppress("UNCHECKED_CAST")
            val rawItems = data["items"] as? List<Map<String, Any?>> ?: emptyList()
            val parsedItems = rawItems.map { itm ->
                ItemDevolucion(
                    productoId = itm["productoId"] as? String ?: "",
                    nombreProducto = itm["nombreProducto"] as? String ?: "",
                    presentacionNombre = itm["presentacionNombre"] as? String ?: "",
                    cantidad = (itm["cantidad"] as? Number)?.toInt() ?: 1,
                    precioUnitario = (itm["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                    monto = (itm["monto"] as? Number)?.toDouble() ?: 0.0
                )
            }

            DevolucionVenta(
                id = snap.id,
                ventaId = data["ventaId"] as? String ?: "",
                numeroVenta = data["numeroVenta"] as? String ?: "",
                tipoDocumento = data["tipoDocumento"] as? String ?: "NOTA_CREDITO",
                serie = data["serie"] as? String ?: "",
                correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L,
                numeroCompleto = data["numeroCompleto"] as? String ?: "",
                estadoFiscal = data["estadoFiscal"] as? String ?: "PENDIENTE",
                items = parsedItems,
                montoReembolso = (data["montoReembolso"] as? Number)?.toDouble() ?: 0.0,
                metodoReembolso = data["metodoReembolso"] as? String ?: "EFECTIVO",
                motivo = data["motivo"] as? String ?: "",
                usuarioId = data["usuarioId"] as? String ?: "",
                usuarioNombre = data["usuarioNombre"] as? String ?: "",
                cajaSesionId = data["cajaSesionId"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando devolucion vinculada $devolucionId: ${e.message}", e)
            null
        }
    }

    private fun parseDocumento(id: String, data: Map<String, Any?>?): FacturacionDocumento? {
        if (data == null) return null
        return try {
            FacturacionDocumento(
                id = id,
                tipo = data["tipo"] as? String ?: "BOLETA",
                serie = data["serie"] as? String ?: "",
                correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L,
                numeroCompleto = data["numeroCompleto"] as? String ?: "",
                clienteTipoDoc = data["clienteTipoDoc"] as? String ?: "NINGUNO",
                clienteNumeroDoc = data["clienteNumeroDoc"] as? String ?: "",
                clienteNombre = data["clienteNombre"] as? String ?: "Consumidor Final",
                ventaId = data["ventaId"] as? String ?: "",
                devolucionId = data["devolucionId"] as? String ?: "",
                sucursalId = data["sucursalId"] as? String ?: "",
                total = (data["total"] as? Number)?.toDouble() ?: 0.0,
                estadoEnvio = data["estadoEnvio"] as? String ?: FacturacionDocumento.ESTADO_PENDIENTE,
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
                motivo = data["motivo"] as? String ?: "",
                moduloOrigen = data["moduloOrigen"] as? String ?: "POS",
                documentIdProveedor = data["documentIdProveedor"] as? String ?: "",
                xmlUrl = data["xmlUrl"] as? String ?: "",
                cdrUrl = data["cdrUrl"] as? String ?: "",
                pdfUrl = data["pdfUrl"] as? String ?: "",
                numeroQuemado = data["numeroQuemado"] as? Boolean ?: false,
                ultimoError = data["ultimoError"] as? String ?: "",
                responseTimeMs = (data["responseTimeMs"] as? Number)?.toLong() ?: 0L,
                reintentos = (data["reintentos"] as? Number)?.toInt() ?: 0,
                historialIntentos = @Suppress("UNCHECKED_CAST") (data["historialIntentos"] as? List<Map<String, Any?>> ?: emptyList())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando FacturacionDocumento $id: ${e.message}", e)
            null
        }
    }
}
