package com.app.administradorfarmadon.analitica_reportes.datos

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.analitica_reportes.logica.AnaliticaCalculadora
import com.app.administradorfarmadon.analitica_reportes.modelo.SucursalInfo
import com.app.administradorfarmadon.facturacion.documentos.datos.FacturacionDocumentosRepository
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.inventario.compartido.datos.FacturaCompraRepository
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Repositorio exclusivo de lectura para el módulo Analítica (R1/R4/R8).
 *
 * R1: Consulta estrictamente dentro de farmaciaId (tenant) y sucursalId.
 * R8: Verdad vigente vía listeners (callbackFlow) para datos vivos (hoy, caja activa)
 *     y consultas puntuales (.get()) en rangos históricos.
 * R10/R11: Cero serverTimestamp en arrays, Firebase gestiona reconexión.
 */
class AnaliticaRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val ventasRepo: VentasRepository = VentasRepository(),
    private val cajaRepo: CajaRepository = CajaRepository(),
    private val factDocsRepo: FacturacionDocumentosRepository = FacturacionDocumentosRepository()
) {
    companion object {
        private const val TAG = "AnaliticaRepository"
    }

    /**
     * Escucha en vivo de las ventas del día de hoy para la sede seleccionada (R8).
     * Usa query de 1 solo campo ("diaClave").
     */
    fun observarVentasHoy(farmaciaId: String, sucursalId: String, diaHoy: String): Flow<List<Venta>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || diaHoy.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.ventas(db, farmaciaId, sucursalId)
            .whereEqualTo("diaClave", diaHoy)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando ventas hoy ($diaHoy): ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val ventas = snapshot.documents.mapNotNull { doc ->
                    ventasRepo.parseVenta(doc.id, doc.data)
                }
                trySend(ventas)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Helper para paginar consultas de Firestore en lotes de 500 hasta agotar resultados (R8/R13).
     * Previene recortes silenciosos de datos cuando una colección tiene miles de registros.
     */
    private suspend fun agotarConsultaPaginada(
        queryBase: Query,
        tamanioLote: Long = 500L
    ): List<DocumentSnapshot> {
        val docs = mutableListOf<DocumentSnapshot>()
        var ultimo: DocumentSnapshot? = null
        var continuar = true

        while (continuar) {
            var q = queryBase.limit(tamanioLote)
            if (ultimo != null) {
                q = q.startAfter(ultimo)
            }
            val snap = q.get().await()
            if (snap.isEmpty) {
                continuar = false
            } else {
                docs.addAll(snap.documents)
                if (snap.size() < tamanioLote) {
                    continuar = false
                } else {
                    ultimo = snap.documents.last()
                }
            }
        }
        return docs
    }

    /**
     * Consulta histórica de ventas para un período (R8/R13).
     * Itera sobre la lista de días con paginación exhaustiva y consolida en memoria.
     */
    suspend fun obtenerVentasPeriodo(
        farmaciaId: String,
        sucursalId: String,
        dias: List<String>
    ): List<Venta> {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || dias.isEmpty()) return emptyList()

        val todasVentas = mutableListOf<Venta>()
        val coleccion = FarmadonPaths.ventas(db, farmaciaId, sucursalId)

        for (dia in dias) {
            try {
                val docs = agotarConsultaPaginada(coleccion.whereEqualTo("diaClave", dia))
                val delDia = docs.mapNotNull { doc ->
                    ventasRepo.parseVenta(doc.id, doc.data)
                }
                todasVentas.addAll(delDia)
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando ventas del día $dia: ${e.message}", e)
                throw e
            }
        }
        return todasVentas
    }

    /**
     * Escucha en vivo de las devoluciones de hoy (R8).
     */
    fun observarDevolucionesHoy(farmaciaId: String, sucursalId: String, diaHoy: String): Flow<List<DevolucionVenta>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || diaHoy.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId)
            .whereEqualTo("diaClave", diaHoy)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando devoluciones hoy ($diaHoy): ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val devs = snapshot.documents.mapNotNull { doc ->
                    ventasRepo.parseDevolucion(doc.id, doc.data)
                }
                trySend(devs)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Consulta histórica de devoluciones en un período (R8).
     */
    suspend fun obtenerDevolucionesPeriodo(
        farmaciaId: String,
        sucursalId: String,
        dias: List<String>,
        inicioMs: Long,
        finMs: Long
    ): List<DevolucionVenta> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()

        val coleccion = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId)

        val todasDevs = mutableMapOf<String, DevolucionVenta>()

        // 1. Lectura por diaClave si hay días definidos (nuevas devoluciones F1)
        if (dias.isNotEmpty()) {
            for (dia in dias) {
                try {
                    val docs = agotarConsultaPaginada(coleccion.whereEqualTo("diaClave", dia))
                    for (doc in docs) {
                        ventasRepo.parseDevolucion(doc.id, doc.data)?.let {
                            todasDevs[it.id] = it
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en lectura diaClave de devoluciones ($dia): ${e.message}", e)
                    throw e
                }
            }
        }

        // 2. Consulta complementaria por rango fechaMs para no perder devoluciones históricas (sin diaClave)
        if (inicioMs > 0 && finMs >= inicioMs) {
            try {
                val qFecha = coleccion
                    .whereGreaterThanOrEqualTo("fechaMs", inicioMs)
                    .whereLessThanOrEqualTo("fechaMs", finMs)
                    .orderBy("fechaMs")
                val docs = agotarConsultaPaginada(qFecha)
                for (doc in docs) {
                    ventasRepo.parseDevolucion(doc.id, doc.data)?.let {
                        todasDevs[it.id] = it
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en lectura por rango fechaMs de devoluciones: ${e.message}", e)
                throw e
            }
        }

        return todasDevs.values.toList()
    }

    /**
     * Escucha en vivo de TODOS los punteros de turno vigentes de la sede (R1/R8/R13).
     *
     * Puntería corregida: el POS escribe el puntero del turno por cajero (`actual_<cajeroId>`,
     * o `actual` si el cajero no tiene ID). Leer el documento genérico `actual` dejaba a
     * Analítica ciega ante los turnos abiertos reales. Se escucha el rango de documentos
     * [actual .. actual\uFFFF] dentro de caja_sesiones (misma sede, R1) y se devuelven solo
     * los punteros ABIERTOS; el ViewModel los combina en un único estado de la sede.
     */
    fun observarEstadoCaja(farmaciaId: String, sucursalId: String): Flow<List<EstadoCaja>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), "actual")
            .whereLessThanOrEqualTo(FieldPath.documentId(), "actual\uffff")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando punteros de caja: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val punteros = snapshot?.documents
                ?.filter { it.id.startsWith("actual") }
                ?.mapNotNull { cajaRepo.parseEstadoCaja(it.data) }
                ?.filter { it.estado == CajaSesion.ESTADO_ABIERTA }
                ?: emptyList()

            trySend(punteros)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Consulta puntual de los punteros de turno vigentes de la sede, combinados en un único
     * EstadoCaja agregado (R1/R13). Lanza la excepción real para que el ViewModel la declare
     * como fuente fallida (R3/R9: nunca devolver "caja cerrada" fingiendo que fue leído).
     */
    suspend fun obtenerEstadoCajaPuntual(farmaciaId: String, sucursalId: String): EstadoCaja {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return EstadoCaja()
        return try {
            val snap = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId)
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), "actual")
                .whereLessThanOrEqualTo(FieldPath.documentId(), "actual\uffff")
                .get()
                .await()
            val punteros = snap.documents
                .filter { it.id.startsWith("actual") }
                .mapNotNull { cajaRepo.parseEstadoCaja(it.data) }
                .filter { it.estado == CajaSesion.ESTADO_ABIERTA }
            AnaliticaCalculadora.combinarEstadosCaja(punteros)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estado de caja puntual: ${e.message}", e)
            throw e
        }
    }

    /**
     * Escucha en vivo de los movimientos de la sesión de caja actual.
     * Query de 1 solo campo: "cajaSesionId".
     */
    fun observarCajaMovimientos(
        farmaciaId: String,
        sucursalId: String,
        sesionId: String
    ): Flow<List<MovimientoCaja>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || sesionId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId)
            .whereEqualTo("cajaSesionId", sesionId)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando caja_movimientos: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val movs = snapshot?.documents?.mapNotNull { doc ->
                cajaRepo.parseMovimiento(doc.id, doc.data)
            } ?: emptyList()

            trySend(movs)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Consulta histórica de movimientos de una sesión de caja.
     */
    suspend fun obtenerCajaMovimientos(
        farmaciaId: String,
        sucursalId: String,
        sesionId: String
    ): List<MovimientoCaja> {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || sesionId.isBlank()) return emptyList()
        return try {
            val snap = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId)
                .whereEqualTo("cajaSesionId", sesionId)
                .get()
                .await()
            snap.documents.mapNotNull { doc -> cajaRepo.parseMovimiento(doc.id, doc.data) }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo movimientos de caja $sesionId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Escucha en vivo el historial de turnos de caja para arqueos y auditoría financiera.
     */
    fun observarHistorialSesionesCaja(limite: Int = 50): Flow<List<CajaSesion>> {
        return cajaRepo.observarHistorialSesiones(limite)
    }

    /**
     * Obtiene el historial de turnos de caja para reportes y auditoría contable.
     * Consulta acotada en servidor por fecha descendente (R8/R13).
     */
    suspend fun obtenerHistorialSesionesCaja(farmaciaId: String, sucursalId: String, limite: Int = 50): List<CajaSesion> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val snap = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId)
                .orderBy("aperturaMs", Query.Direction.DESCENDING)
                .limit((limite + 1).toLong())
                .get().await()
            snap.documents.mapNotNull { doc ->
                // Excluir TODOS los punteros (doc 'actual' legacy y 'actual_<cajeroId>'):
                // el turno abierto entra una sola vez por su documento de sesión real;
                // el estado vivo del puntero se lee por la vía exclusiva de observarEstadoCaja (R13: cero doble conteo).
                if (doc.id.startsWith("actual")) null else cajaRepo.parseSesion(doc.id, doc.data)
            }.take(limite)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo historial de sesiones de caja: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene todas las sesiones de caja del período indicado para conciliación y estado financiero.
     * Utiliza paginación por lotes en Firestore para soportar miles de turnos sin trabas ni pérdida de datos (R8/R13).
     */
    suspend fun obtenerSesionesCajaPeriodo(
        farmaciaId: String,
        sucursalId: String,
        inicioMs: Long,
        finMs: Long
    ): List<CajaSesion> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val coleccion = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId)
            val docs = if (inicioMs > 0L) {
                // Margen de 24 horas para capturar turnos nocturnos abiertos el día anterior que cerraron en el período
                val margenApertura = maxOf(0L, inicioMs - 86_400_000L)
                val q = coleccion
                    .whereGreaterThanOrEqualTo("aperturaMs", margenApertura)
                    .whereLessThanOrEqualTo("aperturaMs", finMs)
                agotarConsultaPaginada(q)
            } else {
                agotarConsultaPaginada(coleccion)
            }
            docs.mapNotNull { doc ->
                // Excluir TODOS los punteros ('actual' y 'actual_<cajeroId>'): si entraran, el turno
                // abierto se contaría DOS veces (documento de sesión + puntero) en el historial y en
                // el puntero de conciliación. El estado vivo se lee solo por observarEstadoCaja (R13).
                if (doc.id.startsWith("actual")) null else cajaRepo.parseSesion(doc.id, doc.data)
            }.filter { s ->
                val tiempoCorte = if (s.cierreMs > 0L) s.cierreMs else s.aperturaMs
                tiempoCorte in inicioMs..finMs
            }.sortedByDescending { it.aperturaMs }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo sesiones de caja del período: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene los movimientos de caja (ingresos, retiros) dentro del período indicado.
     * Paginado en servidor por fecha (R8/R13).
     */
    suspend fun obtenerMovimientosCajaPeriodo(
        farmaciaId: String,
        sucursalId: String,
        inicioMs: Long,
        finMs: Long
    ): List<MovimientoCaja> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val coleccion = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId)
            val docs = if (inicioMs > 0L) {
                val q = coleccion
                    .whereGreaterThanOrEqualTo("fechaMs", inicioMs)
                    .whereLessThanOrEqualTo("fechaMs", finMs)
                agotarConsultaPaginada(q)
            } else {
                agotarConsultaPaginada(coleccion)
            }
            docs.mapNotNull { doc ->
                cajaRepo.parseMovimiento(doc.id, doc.data)
            }.filter { m ->
                m.fechaMs in inicioMs..finMs
            }.sortedByDescending { it.fechaMs }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo movimientos de caja del período: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Consulta de documentos fiscales emitidos en la farmacia con filtro en memoria por sucursal y fechas.
     */
    suspend fun obtenerFacturacionDocumentos(
        farmaciaId: String,
        sucursalId: String,
        inicioMs: Long,
        finMs: Long
    ): List<FacturacionDocumento> {
        if (farmaciaId.isBlank()) return emptyList()
        return try {
            val q = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
                .orderBy("fechaMs", Query.Direction.DESCENDING)
            val docs = agotarConsultaPaginada(q)

            docs.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                FacturacionDocumento(
                    id = doc.id,
                    tipo = d["tipo"] as? String ?: "BOLETA",
                    serie = d["serie"] as? String ?: "",
                    correlativo = (d["correlativo"] as? Number)?.toLong() ?: 0L,
                    numeroCompleto = d["numeroCompleto"] as? String ?: "",
                    clienteTipoDoc = d["clienteTipoDoc"] as? String ?: "NINGUNO",
                    clienteNumeroDoc = d["clienteNumeroDoc"] as? String ?: "",
                    clienteNombre = d["clienteNombre"] as? String ?: "Consumidor Final",
                    ventaId = d["ventaId"] as? String ?: "",
                    devolucionId = d["devolucionId"] as? String ?: "",
                    sucursalId = d["sucursalId"] as? String ?: "",
                    total = (d["total"] as? Number)?.toDouble() ?: 0.0,
                    estadoEnvio = d["estadoEnvio"] as? String ?: FacturacionDocumento.ESTADO_PENDIENTE,
                    fechaMs = (d["fechaMs"] as? Number)?.toLong() ?: 0L,
                    motivo = d["motivo"] as? String ?: "",
                    moduloOrigen = d["moduloOrigen"] as? String ?: "POS",
                    xmlUrl = d["xmlUrl"] as? String ?: "",
                    cdrUrl = d["cdrUrl"] as? String ?: "",
                    pdfUrl = d["pdfUrl"] as? String ?: ""
                )
            }.filter { doc ->
                (doc.sucursalId.isBlank() || doc.sucursalId == sucursalId) &&
                    doc.fechaMs in inicioMs..finMs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando facturacion_documentos: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fan-out seguro para leer documentos fiscales de múltiples sedes de la MISMA farmacia (R1).
     * Filtra en memoria por sucursal y rango, igual que la versión monosede.
     */
    suspend fun obtenerFacturacionDocumentosMultisede(
        farmaciaId: String,
        sedesIds: List<String>,
        inicioMs: Long,
        finMs: Long
    ): List<FacturacionDocumento> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyList()
        val acc = mutableMapOf<String, FacturacionDocumento>()
        for (sId in sedesIds) {
            val docs = obtenerFacturacionDocumentos(farmaciaId, sId, inicioMs, finMs)
            for (d in docs) acc[d.id] = d
        }
        return acc.values.toList()
    }

    /**
     * Busca ventas por número visible (ej: B001-000042) para drill-down.
     */
    suspend fun buscarVentaPorNumero(texto: String): Result<List<Venta>> {
        return ventasRepo.buscarVentaPorNumero(texto)
    }

    /**
     * Lee una venta puntual vinculada para drill-down.
     */
    suspend fun obtenerVentaVinculada(farmaciaId: String, sucursalId: String, ventaId: String): Venta? {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || ventaId.isBlank()) return null
        return try {
            val snap = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId).get().await()
            ventasRepo.parseVenta(snap.id, snap.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo venta vinculada $ventaId: ${e.message}", e)
            null
        }
    }

    /**
     * Lee una devolución puntual vinculada para drill-down.
     */
    suspend fun obtenerDevolucionVinculada(farmaciaId: String, sucursalId: String, devolucionId: String): DevolucionVenta? {
        if (farmaciaId.isBlank() || sucursalId.isBlank() || devolucionId.isBlank()) return null
        return try {
            val snap = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId).document(devolucionId).get().await()
            ventasRepo.parseDevolucion(snap.id, snap.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo devolucion vinculada $devolucionId: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene el catálogo de inventario de la sede para métricas de quiebres e inmovilizados (R1).
     */
    suspend fun obtenerInventarioSede(farmaciaId: String, sucursalId: String): List<MoldeProductos> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val docs = agotarConsultaPaginada(FarmadonPaths.inventario(db, farmaciaId, sucursalId))
            docs.mapNotNull { com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser.parseToMolde(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando inventario para analítica: ${e.message}", e)
            throw e
        }
    }

    /**
     * Obtiene las facturas de compra de la sede (R1) filtradas por el período solicitado.
     */
    private fun filtrarYMapearFactura(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        inicioMs: Long,
        finMs: Long,
        sdf1: java.text.SimpleDateFormat,
        sdf2: java.text.SimpleDateFormat
    ): FacturaCompra? {
        val id = doc.id
        val numFactura = doc.getString("numeroFactura") ?: doc.getString("numero") ?: id
        if (numFactura.startsWith("S/C") || numFactura.equals("AJUSTE", ignoreCase = true)) {
            return null
        }
        val estado = doc.getString("estadoPago") ?: "PENDIENTE"
        if (estado.equals("ANULADA", ignoreCase = true) || estado.equals("CANCELADA", ignoreCase = true)) {
            return null
        }

        // Filtrar estrictamente por fecha del período seleccionado (Fecha comercial de emisión o recepción)
        val fEmision = doc.getString("fechaEmision") ?: doc.getString("fecha") ?: ""
        val fReg = doc.getString("fechaRegistro") ?: doc.getString("fechaRecepcion") ?: ""
        val d1 = try { if (fEmision.isNotBlank()) (if (fEmision.contains("-")) sdf2.parse(fEmision) else sdf1.parse(fEmision))?.time else null } catch (_: Exception) { null }
        val d2 = try { if (fReg.isNotBlank()) (if (fReg.contains("-")) sdf2.parse(fReg) else sdf1.parse(fReg))?.time else null } catch (_: Exception) { null }
        val tsCreado = doc.getTimestamp("creadoEl")?.toDate()?.time
        val fechaDocMs: Long = d1 ?: d2 ?: tsCreado ?: 0L

        if (inicioMs > 0L && finMs >= inicioMs && fechaDocMs > 0L) {
            if (fechaDocMs !in inicioMs..finMs) {
                return null
            }
        }

        return FacturaCompraRepository.mapear(doc)
    }

    suspend fun obtenerComprasPeriodo(
        farmaciaId: String,
        sucursalId: String,
        inicioMs: Long = 0L,
        finMs: Long = 0L
    ): List<FacturaCompra> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val docs = agotarConsultaPaginada(FarmadonPaths.comprasFacturas(db, farmaciaId, sucursalId))
            val sdf1 = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") }
            val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") }

            docs.mapNotNull { doc ->
                filtrarYMapearFactura(doc, inicioMs, finMs, sdf1, sdf2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando compras para analítica: ${e.message}", e)
            throw e
        }
    }

    /**
     * Escucha viva en tiempo real de compras del período seleccionado (R8 Verdad Vigente).
     * Toda inserción, edición, abono o anulación en Firestore se emite de inmediato.
     */
    fun observarComprasPeriodo(
        farmaciaId: String,
        sucursalId: String,
        inicioMs: Long = 0L,
        finMs: Long = 0L
    ): Flow<List<FacturaCompra>> = callbackFlow {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val sdf1 = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") }
        val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") }

        val ref = FarmadonPaths.comprasFacturas(db, farmaciaId, sucursalId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando compras en tiempo real: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            val docs = snapshot?.documents ?: emptyList()
            val facturas = docs.mapNotNull { doc ->
                filtrarYMapearFactura(doc, inicioMs, finMs, sdf1, sdf2)
            }
            trySend(facturas)
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Fan-out reactivo en tiempo real para compras de múltiples sedes de la MISMA farmacia (R1/R8).
     */
    fun observarComprasMultisede(
        farmaciaId: String,
        sedesIds: List<String>,
        inicioMs: Long = 0L,
        finMs: Long = 0L
    ): Flow<List<FacturaCompra>> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return flowOf(emptyList())
        if (sedesIds.size == 1) return observarComprasPeriodo(farmaciaId, sedesIds.first(), inicioMs, finMs)
        val flows = sedesIds.map { sId -> observarComprasPeriodo(farmaciaId, sId, inicioMs, finMs) }
        return combine(flows) { arrays ->
            arrays.flatMap { it }.distinctBy { it.id }
        }
    }

    /**
     * Obtiene el listado de todas las sucursales de la farmacia (R1).
     */
    suspend fun obtenerSucursales(farmaciaId: String): List<SucursalInfo> {
        if (farmaciaId.isBlank()) return emptyList()
        return try {
            val snap = FarmadonPaths.sucursales(db, farmaciaId).get().await()
            snap.documents.mapNotNull { doc ->
                SucursalInfo(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "Sede",
                    activa = doc.getBoolean("activa") ?: true,
                    esPrincipal = doc.getBoolean("esPrincipal") ?: false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo sucursales para analítica: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fan-out seguro para leer ventas de múltiples sedes de la MISMA farmacia (R1).
     */
    suspend fun obtenerVentasMultisede(
        farmaciaId: String,
        sedesIds: List<String>,
        dias: List<String>
    ): Map<String, List<Venta>> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyMap()
        val res = mutableMapOf<String, List<Venta>>()
        for (sId in sedesIds) {
            res[sId] = obtenerVentasPeriodo(farmaciaId, sId, dias)
        }
        return res
    }

    /**
     * Fan-out seguro para leer devoluciones de múltiples sedes de la MISMA farmacia (R1).
     */
    suspend fun obtenerDevolucionesMultisede(
        farmaciaId: String,
        sedesIds: List<String>,
        dias: List<String>,
        inicioMs: Long,
        finMs: Long
    ): Map<String, List<DevolucionVenta>> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyMap()
        val res = mutableMapOf<String, List<DevolucionVenta>>()
        for (sId in sedesIds) {
            res[sId] = obtenerDevolucionesPeriodo(farmaciaId, sId, dias, inicioMs, finMs)
        }
        return res
    }

    /**
     * Fan-out seguro para leer inventario de múltiples sedes de la MISMA farmacia (R1).
     */
    suspend fun obtenerInventarioMultisede(
        farmaciaId: String,
        sedesIds: List<String>
    ): Map<String, List<MoldeProductos>> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyMap()
        val res = mutableMapOf<String, List<MoldeProductos>>()
        for (sId in sedesIds) {
            res[sId] = obtenerInventarioSede(farmaciaId, sId)
        }
        return res
    }

    /**
     * Fan-out seguro para leer compras de múltiples sedes de la MISMA farmacia (R1).
     */
    suspend fun obtenerComprasMultisede(
        farmaciaId: String,
        sedesIds: List<String>,
        inicioMs: Long = 0L,
        finMs: Long = 0L
    ): Map<String, List<FacturaCompra>> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyMap()
        val res = mutableMapOf<String, List<FacturaCompra>>()
        for (sId in sedesIds) {
            res[sId] = obtenerComprasPeriodo(farmaciaId, sId, inicioMs, finMs)
        }
        return res
    }

    /**
     * Obtiene el contrato activo de métodos de pago configurados para la sede (R1).
     * Documento: catalogos/metodosPago -> instancias: { id: { farmaciaId, sucursalId, tipoId, activa, datos } }
     */
    suspend fun obtenerMetodosPagoConfigurados(
        farmaciaId: String,
        sucursalId: String
    ): List<InstanciaPago> {
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return emptyList()
        return try {
            val docRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
                .collection("catalogos")
                .document("metodosPago")
            val snap = docRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val raw = (snap.get("instancias") as? Map<String, Any>) ?: emptyMap()
            raw.mapNotNull { (id, v) ->
                if (v is Map<*, *>) {
                    InstanciaPago(
                        id = id,
                        farmaciaId = (v["farmaciaId"] as? String) ?: farmaciaId,
                        sucursalId = (v["sucursalId"] as? String) ?: sucursalId,
                        tipoId = (v["tipoId"] as? String) ?: "",
                        activa = v["activa"] == true,
                        datos = (v["datos"] as? Map<*, *>)
                            ?.mapNotNull { (k, val_) -> (k as? String)?.let { it to (val_?.toString() ?: "") } }
                            ?.toMap() ?: emptyMap()
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo metodosPago de $sucursalId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fan-out para unir los contratos de métodos de pago de todas las sedes (R1).
     */
    suspend fun obtenerMetodosPagoMultisede(
        farmaciaId: String,
        sedesIds: List<String>
    ): List<InstanciaPago> {
        if (farmaciaId.isBlank() || sedesIds.isEmpty()) return emptyList()
        val todas = mutableListOf<InstanciaPago>()
        for (sId in sedesIds) {
            todas.addAll(obtenerMetodosPagoConfigurados(farmaciaId, sId))
        }
        return todas.distinctBy { "${it.sucursalId}_${it.tipoId}" }
    }
}
