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

    data class MetricasFiscales(
        val totalDocumentos: Int = 0,
        val totalAceptados: Int = 0,
        val totalPendientes: Int = 0,
        val totalEnviados: Int = 0,
        val totalRequierenAtencion: Int = 0,
        val totalAnulados: Int = 0,
        val montoTotalFacturado: Double = 0.0,
        val baseGravadaTotal: Double = 0.0,
        val igvTotal: Double = 0.0,
        val totalBoletas: Int = 0,
        val totalFacturas: Int = 0,
        val totalNotasCredito: Int = 0,
        val totalBajas: Int = 0
    )

    /**
     * Consulta atómica agregada en Google Cloud Firestore:
     * Obtiene los totales reales de comprobantes emitidos en el servidor (sin descargar miles de documentos).
     * Garantiza la verdad absoluta (R12) de las métricas aunque la vista solo renderice los 50 más recientes.
     */
    suspend fun obtenerMetricasFiscales(farmaciaId: String): MetricasFiscales {
        if (farmaciaId.isBlank()) return MetricasFiscales()
        return try {
            val ref = FarmadonPaths.facturacionDocumentos(db, farmaciaId)

            // Conteo aggregate en el servidor de Firestore (escalar, ultra rápido y veraz)
            val total = ref.count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()
            if (total == 0) return MetricasFiscales()

            val totalAceptados = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ACEPTADO)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalPendientes = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_PENDIENTE)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalEnviados = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ENVIADO)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalRechazados = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_RECHAZADO)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalAnulados = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ANULADO)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalBoletas = ref.whereEqualTo("tipo", "BOLETA")
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalFacturas = ref.whereEqualTo("tipo", "FACTURA")
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalNotasCredito = ref.whereEqualTo("tipo", "NOTA_CREDITO")
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            val totalBajas = ref.whereEqualTo("tipo", "COMUNICACION_BAJA")
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()

            // Suma del monto total facturado de los comprobantes aceptados
            var montoFacturado = 0.0
            try {
                val sumQuery = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ACEPTADO)
                    .aggregate(com.google.firebase.firestore.AggregateField.sum("total"))
                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                montoFacturado = (sumQuery.get(com.google.firebase.firestore.AggregateField.sum("total")) as? Number)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                Log.w(TAG, "Fallback calculando suma de facturado: ${e.message}")
                val snap = ref.whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ACEPTADO).get().await()
                montoFacturado = snap.documents.filter {
                    val tipo = it.getString("tipo") ?: ""
                    tipo != "NOTA_CREDITO" && tipo != "COMUNICACION_BAJA"
                }.sumOf { (it.get("total") as? Number)?.toDouble() ?: 0.0 }
            }

            val montoRedondeado = kotlin.math.round(montoFacturado * 100.0) / 100.0
            val baseGravada = kotlin.math.round((montoRedondeado / 1.18) * 100.0) / 100.0
            val igv = kotlin.math.round((montoRedondeado - baseGravada) * 100.0) / 100.0

            MetricasFiscales(
                totalDocumentos = total,
                totalAceptados = totalAceptados,
                totalPendientes = totalPendientes,
                totalEnviados = totalEnviados,
                totalRequierenAtencion = totalRechazados,
                totalAnulados = totalAnulados,
                montoTotalFacturado = montoRedondeado,
                baseGravadaTotal = baseGravada,
                igvTotal = igv,
                totalBoletas = totalBoletas,
                totalFacturas = totalFacturas,
                totalNotasCredito = totalNotasCredito,
                totalBajas = totalBajas
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando métricas fiscales agregadas: ${e.message}", e)
            MetricasFiscales()
        }
    }

    /**
     * Escucha viva paginada de los documentos fiscales emitidos por la farmacia (Regla R8 - Verdad Vigente).
     * Lee un lote inicial de 50 documentos para que la interfaz cargue al instante y sea 100% fluida,
     * sin congelamientos ni jaloneo por descargar miles de registros en la memoria del dispositivo.
     */
    fun observarDocumentos(farmaciaId: String, limite: Long = 50): Flow<List<FacturacionDocumento>> = callbackFlow {
        if (farmaciaId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .orderBy("fechaMs", Query.Direction.DESCENDING)
            .limit(limite)

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
     * Carga el siguiente lote de comprobantes fiscales usando un cursor cronológico en Firestore.
     */
    suspend fun cargarSiguientePagina(
        farmaciaId: String,
        ultimoDocMs: Long,
        limite: Long = 50
    ): List<FacturacionDocumento> {
        if (farmaciaId.isBlank() || ultimoDocMs <= 0L) return emptyList()
        return try {
            val snap = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
                .orderBy("fechaMs", Query.Direction.DESCENDING)
                .startAfter(ultimoDocMs)
                .limit(limite)
                .get().await()

            snap.documents.mapNotNull { parseDocumento(it.id, it.data) }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando siguiente página de documentos fiscales: ${e.message}", e)
            emptyList()
        }
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
