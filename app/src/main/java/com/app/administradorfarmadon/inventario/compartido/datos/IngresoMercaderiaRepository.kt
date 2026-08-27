package com.app.administradorfarmadon.inventario.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega
import com.app.administradorfarmadon.inventario.compartido.logica.CostoLoteCalculator
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Fuente única de verdad para TODO ingreso de mercadería (R1/R3/R8/R10).
 * Reemplaza la duplicación entre PedidoCompraRepository y StockEntryRepository.
 * Un solo runTransaction: producto+lote+stock + kardex + factura INMUTABLE + pedido (si viene pedidoId).
 * Sin arrayUnion en factura — una factura es un papel inmutable, no se edita.
 * Sin conciliar parche — si viene pedidoId es recepción, si no es ingreso suelto.
 */
class IngresoMercaderiaRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "IngresoMercaderiaRepository"
    }

    private fun obtenerIds(): Pair<String, String>? {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return null
        return Pair(f, s)
    }

    /**
     * Ingreso atómico. Si pedidoId != null → recepción de pedido (valida saldos, factura obligatoria, acta en pedido).
     * Si pedidoId == null → ingreso directo suelto (S/C permitido, no toca pedidos).
     */
    suspend fun ingresar(
        pedidoId: String? = null,
        numeroFactura: String = "",
        condicionPago: String = "Contado",
        fechaVencimientoPago: String = "",
        montoFactura: Double = 0.0,
        items: List<ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean = false,
        usuarioEmail: String = "",
        usuarioNombre: String = "",
        idempotenciaId: String = "",
        stockMinimoNuevoPorProducto: Map<String, Double> = emptyMap(),
        proveedorIdFactura: String = "",
        proveedorNombreFactura: String = "",
        rucProveedorFactura: String = ""
    ): Result<Unit> {
        val ids = obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val (farmaciaId, sucursalId) = ids

        val esRecepcionPedido = !pedidoId.isNullOrBlank()
        val numFacturaLimpio = numeroFactura.trim().uppercase()

        // Validaciones raíz — antes de tocar Firestore
        if (esRecepcionPedido && numFacturaLimpio.isBlank()) {
            return Result.failure(IllegalArgumentException("El número de factura del proveedor es obligatorio."))
        }
        val itemsConIngreso = items.filter { it.cantidadTotal > 0 }
        if (itemsConIngreso.isEmpty()) {
            return Result.failure(IllegalArgumentException("No llegaron unidades en esta recepción. Si nada llegó, CANCELA o DESCARTA la orden; no se puede asentar un ingreso vacío."))
        }
        if (esRecepcionPedido && itemsConIngreso.all { it.cantidadComprada == 0 }) {
            return Result.failure(IllegalArgumentException("Los regalos acompañan una compra: incluye al menos una unidad comprada en la recepción."))
        }
        for (item in itemsConIngreso) {
            if (item.loteNumero.trim().isBlank()) return Result.failure(IllegalArgumentException("El lote para '${item.productoNombre}' es obligatorio."))
            if (item.cantidadComprada > 0 && item.costoUnitarioReal <= 0.0) return Result.failure(IllegalArgumentException("El producto '${item.productoNombre}' necesita un costo unitario real mayor a 0. Mercadería sin costo no se asienta."))
            val vtoNorm = FechaVencimientoHelper.normalizar(item.vencimiento.trim()) ?: item.vencimiento.trim()
            if (vtoNorm.isBlank()) return Result.failure(IllegalArgumentException("La fecha de vencimiento para '${item.productoNombre}' es obligatoria (formato MM/AAAA)."))
            val dias = FechaVencimientoHelper.diasHastaVencer(vtoNorm)
                ?: return Result.failure(IllegalArgumentException("La fecha de vencimiento '${item.vencimiento}' para '${item.productoNombre}' no es válida."))
            if (dias <= 0) return Result.failure(IllegalArgumentException("La fecha de vencimiento '${item.vencimiento}' para '${item.productoNombre}' ya está vencida."))
        }

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
            val ahoraMs = HoraServidor.ahoraMs()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaLegible = sdf.format(Date(ahoraMs))
            val cleanFacturaNumero = numFacturaLimpio.replace("/", "-").replace(" ", "_")

            // Para factura inmutable: id = proveedorKey__numero. Si existe, no se edita — se rechaza.
            // Proveedor para factura viene del pedido (recepción) o del primer item (directo). Se resuelve dentro de tx.

            db.runTransaction { tx ->
                // ── Pedido (si es recepción) ──
                val pedidoRef = if (esRecepcionPedido) tiendaRef.collection("pedidos_compra").document(pedidoId!!) else null
                val pedidoSnap = pedidoRef?.let { tx.get(it) }
                var proveedorIdTx = ""
                var proveedorNombreTx = ""
                var proveedorRucTx = ""
                var currentItemsRaw: List<Map<String, Any>> = emptyList()
                var pedidoEstadoActual = ""
                var recepcionesPrevias: List<Map<String, Any>> = emptyList()
                if (esRecepcionPedido) {
                    if (pedidoSnap == null || !pedidoSnap.exists()) throw IllegalStateException("La orden de compra no existe en la sucursal actual.")
                    pedidoEstadoActual = pedidoSnap.getString("estado") ?: "ENVIADO"
                    when (pedidoEstadoActual) {
                        "CANCELADO" -> throw IllegalStateException("La orden fue CANCELADA por otro usuario mientras preparabas la recepción. No se asentó nada.")
                        "RECIBIDO", "COMPLETADA_AJUSTE" -> throw IllegalStateException("Esta orden ya fue cerrada (estado actual: $pedidoEstadoActual). No se puede recibir más mercadería en ella.")
                    }
                    @Suppress("UNCHECKED_CAST")
                    currentItemsRaw = (pedidoSnap.get("items") as? List<Map<String, Any>>) ?: emptyList()
                    val saldosFrescos = currentItemsRaw.associate { raw ->
                        val pedida = (raw["cantidad"] as? Number)?.toInt() ?: 0
                        val recibida = (raw["cantidadRecibida"] as? Number)?.toInt() ?: 0
                        (raw["productoId"] as? String ?: "") to (pedida - recibida)
                    }
                    for (item in itemsConIngreso) {
                        if (!saldosFrescos.containsKey(item.productoId)) throw IllegalArgumentException("'${item.productoNombre}' no pertenece a esta orden.")
                        val saldo = saldosFrescos[item.productoId] ?: 0
                        if (item.cantidadTotal > 0 && saldo <= 0 && !(item.cantidadComprada == 0 && item.bonificacionGratis > 0)) throw IllegalArgumentException("'${item.productoNombre}' ya está COMPLETO en esta orden. No puede recibir más unidades compradas (evita duplicar stock). Si es regalo, ponlo solo en REG.")
                        if (item.cantidadComprada > saldo) throw IllegalArgumentException("'${item.productoNombre}' solo quedan $saldo por recibir. Ingresaste ${item.cantidadComprada} compradas. Si ${item.cantidadComprada - saldo} son de regalo, ponlas en REG no en CANT.")
                    }
                    if (idempotenciaId.isNotBlank()) {
                        @Suppress("UNCHECKED_CAST")
                        recepcionesPrevias = (pedidoSnap.get("recepciones") as? List<Map<String, Any>>) ?: emptyList()
                        if (recepcionesPrevias.any { it["id"] == idempotenciaId }) return@runTransaction
                    }
                    proveedorIdTx = pedidoSnap.getString("proveedorId") ?: ""
                    proveedorNombreTx = pedidoSnap.getString("proveedorNombre") ?: "Proveedor"
                    proveedorRucTx = pedidoSnap.getString("proveedorRuc") ?: ""
                } else {
                    // Directo suelto — proveedor viene de los params de factura
                    proveedorIdTx = proveedorIdFactura
                    proveedorNombreTx = proveedorNombreFactura
                    proveedorRucTx = rucProveedorFactura
                }

                var facturaRef: com.google.firebase.firestore.DocumentReference? = null
                var facturaSnapExists = false
                var effectiveFacturaId: String? = null
                if (numFacturaLimpio.isNotBlank()) {
                    val provKey = when {
                        proveedorIdTx.isNotBlank() -> proveedorIdTx.trim()
                        proveedorNombreTx.isNotBlank() -> proveedorNombreTx.trim().uppercase().replace("/", "-").replace(" ", "_").replace(".", "__DOT__")
                        else -> ""
                    }
                    effectiveFacturaId = if (provKey.isNotBlank()) "${provKey}__${cleanFacturaNumero}" else cleanFacturaNumero
                    facturaRef = tiendaRef.collection("compras_facturas").document(effectiveFacturaId)
                    val snap = tx.get(facturaRef)
                    facturaSnapExists = snap.exists()
                    if (facturaSnapExists) {
                        val existentePedidoId = snap.getString("pedidoId") ?: ""
                        val esMismoPedido = esRecepcionPedido && existentePedidoId.isNotBlank() && existentePedidoId == pedidoId
                        if (esMismoPedido) {
                            // Misma factura para entrega parcial del mismo pedido — inmutable, no se reescribe, se reutiliza
                            facturaRef = null
                            facturaSnapExists = false
                            effectiveFacturaId = null
                        } else {
                            throw IllegalArgumentException("La factura $numFacturaLimpio ya fue registrada para ${if (provKey.isNotBlank()) proveedorNombreTx else "este proveedor"}. Usa un número nuevo.")
                        }
                    }
                    // Legacy fallback: si no existe con prefijo pero existe sin prefijo y mismo proveedor, también es duplicado
                    if (!facturaSnapExists && provKey.isNotBlank() && facturaRef != null) {
                        val legacyRef = tiendaRef.collection("compras_facturas").document(cleanFacturaNumero)
                        if (legacyRef != facturaRef) {
                            val legSnap = tx.get(legacyRef)
                            if (legSnap.exists()) {
                                val legProvId = legSnap.getString("proveedorId") ?: ""
                                val legProvNombre = legSnap.getString("proveedorNombre") ?: ""
                                val legPedidoId = legSnap.getString("pedidoId") ?: ""
                                val same = (legProvId.isNotBlank() && legProvId == proveedorIdTx) || legProvNombre.trim().equals(proveedorNombreTx.trim(), ignoreCase = true)
                                if (same) {
                                    val esMismoPedidoLegacy = esRecepcionPedido && legPedidoId.isNotBlank() && legPedidoId == pedidoId
                                    if (esMismoPedidoLegacy) {
                                        facturaRef = null
                                        effectiveFacturaId = null
                                    } else {
                                        throw IllegalArgumentException("La factura $numFacturaLimpio ya existe (registro legado) para $proveedorNombreTx. Usa número nuevo.")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 1. Actualizar cada producto + kardex ──
                val itemsFacturaList = mutableListOf<Map<String, Any>>()
                var totalCostoCalculado = 0.0

                for (item in itemsConIngreso) {
                    val productRef = tiendaRef.collection("inventario").document(item.productoId)
                    val productSnap = tx.get(productRef)
                    if (!productSnap.exists()) throw IllegalStateException("El producto '${item.productoNombre}' (ID: ${item.productoId}) no existe en inventario.")
                    val movimientoId = UUID.randomUUID().toString()
                    val movimientoRef = tiendaRef.collection("movimientos").document(movimientoId)
                    val cleanLoteKey = FechaVencimientoHelper.llaveLote(item.loteNumero)
                    @Suppress("UNCHECKED_CAST")
                    val currentLotes = (productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                    val resLote = FechaVencimientoHelper.resolverLote(currentLotes, item.loteNumero)
                    val keyLoteDestino = resLote?.first ?: cleanLoteKey
                    val loteActualData = resLote?.second as? Map<*, *>
                    val cantidadLoteAnterior = (loteActualData?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                    val nuevaCantidadLote = (cantidadLoteAnterior + item.cantidadTotal).coerceAtLeast(0.0)
                    val cantBloqueadaExistente = (loteActualData?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                    val motivoBloqueoExistente = loteActualData?.get("motivoBloqueo") as? String
                    val estadoSanitarioExistente = loteActualData?.get("estadoSanitario") as? String
                    val vencimientoOriginalRaw = (loteActualData?.get("vencimiento") as? String)?.takeIf { it.isNotBlank() }
                    val vencimientoOriginal = vencimientoOriginalRaw?.let { FechaVencimientoHelper.normalizar(it) } ?: vencimientoOriginalRaw
                    val vencimientoNormalizado = FechaVencimientoHelper.normalizar(item.vencimiento.trim()) ?: item.vencimiento.trim()
                    if (loteActualData != null && vencimientoOriginal != null && vencimientoOriginal.isNotBlank() && vencimientoNormalizado.isNotBlank() && vencimientoOriginal != vencimientoNormalizado) {
                        throw IllegalArgumentException("El lote ${item.loteNumero.trim().uppercase()} ya existe con vencimiento $vencimientoOriginal. Ingresaste $vencimientoNormalizado.")
                    }
                    val vencimientoFinalLote = vencimientoOriginal ?: vencimientoNormalizado
                    val costoTotalItem = if (item.costoTotalReal > 0) item.costoTotalReal else item.cantidadComprada * item.costoUnitarioReal
                    val costoUnitarioAnterior = (loteActualData?.get("costoUnitario") as? Number)?.toDouble() ?: 0.0
                    val resCosto = CostoLoteCalculator.calcular(
                        cantidadDisponibleAnterior = cantidadLoteAnterior,
                        cantidadBloqueadaAnterior = cantBloqueadaExistente,
                        costoUnitarioAnterior = costoUnitarioAnterior,
                        cantidadTotalNueva = item.cantidadTotal.toDouble(),
                        cantidadCompradaNueva = item.cantidadComprada.toDouble(),
                        costoTotalNuevo = costoTotalItem,
                        costoUnitarioNuevo = item.costoUnitarioReal
                    )
                    val costoUnitarioFinalLote = resCosto.costoUnitarioFinal
                    val costoTotalFinalLote = resCosto.costoTotalFinal
                    totalCostoCalculado += costoTotalItem
                    val proveedorOriginal = (loteActualData?.get("proveedor") as? String)?.takeIf { it.isNotBlank() }
                    val proveedorIdOriginal = (loteActualData?.get("proveedorId") as? String)?.takeIf { it.isNotBlank() }
                    val facturaOriginal = (loteActualData?.get("factura") as? String)?.takeIf { it.isNotBlank() }
                    @Suppress("UNCHECKED_CAST")
                    val historialEntradas = (loteActualData?.get("entradas") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                    val proveedorParaHistorial = if (esRecepcionPedido) proveedorNombreTx else item.productoNombre // fallback, pero caller directo debería pasar proveedor
                    historialEntradas.add(
                        mapOf(
                            "fechaLegible" to fechaLegible,
                            "fechaMs" to ahoraMs,
                            "proveedorNombre" to (if (esRecepcionPedido) proveedorNombreTx else proveedorIdTx.ifBlank { proveedorNombreTx }),
                            "proveedorId" to proveedorIdTx,
                            "factura" to numFacturaLimpio.ifBlank { "S/C" },
                            "cantidad" to item.cantidadTotal.toDouble(),
                            "costoUnitario" to item.costoUnitarioReal
                        )
                    )
                    val loteFinalData = mutableMapOf<String, Any>(
                        "numero" to item.loteNumero.trim().uppercase(),
                        "loteId" to FechaVencimientoHelper.llaveLote(item.loteNumero),
                        "vencimiento" to vencimientoFinalLote,
                        "cantidad" to nuevaCantidadLote,
                        "proveedor" to (proveedorOriginal ?: proveedorNombreTx),
                        "proveedorId" to (proveedorIdOriginal ?: proveedorIdTx),
                        "factura" to (facturaOriginal ?: numFacturaLimpio.ifBlank { "S/C" }),
                        "ultimaEntradaProveedor" to (if (esRecepcionPedido) proveedorNombreTx else proveedorNombreTx),
                        "entradas" to historialEntradas,
                        "costoCompra" to costoTotalFinalLote,
                        "costoUnitario" to costoUnitarioFinalLote,
                        "ultimaEntrada" to FieldValue.serverTimestamp()
                    )
                    if (cantBloqueadaExistente > 0) loteFinalData["cantidadBloqueada"] = cantBloqueadaExistente
                    if (!motivoBloqueoExistente.isNullOrBlank()) loteFinalData["motivoBloqueo"] = motivoBloqueoExistente
                    if (!estadoSanitarioExistente.isNullOrBlank()) loteFinalData["estadoSanitario"] = estadoSanitarioExistente
                    if (loteActualData == null) loteFinalData["fechaIngreso"] = FieldValue.serverTimestamp()
                    currentLotes[keyLoteDestino] = loteFinalData
                    val nuevoStockDisponible = currentLotes.values.sumOf { (it as? Map<*, *>)?.get("cantidad") as? Double ?: (it as? Map<*, *>)?.get("cantidad")?.let { n -> (n as? Number)?.toDouble() } ?: 0.0 }
                    val nuevoStockTotal = currentLotes.values.sumOf {
                        val d = it as? Map<*, *>
                        val cDisp = (d?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                        val cBloq = (d?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                        cDisp + cBloq
                    }
                    val vencimientoMasCercano = FechaVencimientoHelper.vencimientoMasCercano(currentLotes)
                    val productUpdates = mutableMapOf<String, Any>(
                        "lotes" to currentLotes,
                        "stock" to nuevoStockDisponible,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to vencimientoMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                    val lotesParaCosto = currentLotes.values.mapNotNull { it as? Map<*, *> }
                    val totalFisicoLotes = lotesParaCosto.sumOf { l ->
                        val disp = (l["cantidad"] as? Number)?.toDouble() ?: 0.0
                        val bloq = (l["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                        disp + bloq
                    }
                    val costoPonderadoLotes = lotesParaCosto.sumOf { l ->
                        val disp = (l["cantidad"] as? Number)?.toDouble() ?: 0.0
                        val bloq = (l["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                        val costUn = (l["costoUnitario"] as? Number)?.toDouble() ?: 0.0
                        (disp + bloq) * costUn
                    }
                    if (totalFisicoLotes > 0.0 && costoPonderadoLotes > 0.0) {
                        productUpdates["precioCompra"] = costoPonderadoLotes / totalFisicoLotes
                    } else if (item.costoUnitarioReal > 0.0) {
                        productUpdates["precioCompra"] = item.costoUnitarioReal
                    }
                    // Stock mínimo solo si se pidió y el servidor aún no tiene
                    val stockMinimoNuevo = stockMinimoNuevoPorProducto[item.productoId] ?: 0.0
                    if (stockMinimoNuevo >= 1.0) {
                        val minimoEnServidor = productSnap.getDouble("stockMinimo") ?: productSnap.getDouble("stockMinimoBase") ?: 0.0
                        if (minimoEnServidor <= 0.0) {
                            productUpdates["stockMinimo"] = stockMinimoNuevo
                            productUpdates["stockMinimoBase"] = stockMinimoNuevo
                        }
                    }
                    // Auto-vinculación profesional: si el producto nació sin droguería (vacío honesto R12),
                    // el primer ingreso que lo trae lo deja atado para el próximo pedido. Cero trabajo extra.
                    // Si ya tiene proveedor, se respeta (no pisa).
                    val provActual = productSnap.getString("proveedor") ?: productSnap.getString("proveedorNombre") ?: productSnap.getString("proveedorBaseNombre") ?: ""
                    val esVacio = provActual.isBlank()
                    if (esVacio && proveedorNombreTx.isNotBlank()) {
                        productUpdates["proveedor"] = proveedorNombreTx
                        productUpdates["proveedorNombre"] = proveedorNombreTx
                        if (proveedorIdTx.isNotBlank()) productUpdates["proveedorId"] = proveedorIdTx
                    }
                    tx.update(productRef, productUpdates)
                    // Kardex
                    val kardexProveedor = proveedorNombreTx
                    val kardexFactura = numFacturaLimpio.ifBlank { "S/C" }
                    val movimientoData = mapOf(
                        "id" to movimientoId,
                        "tipo" to if (esRecepcionPedido || numFacturaLimpio.isNotBlank()) "ENTRADA_COMPRA" else "INVENTARIO_INICIAL",
                        "productoId" to item.productoId,
                        "productoNombre" to item.productoNombre,
                        "empaque" to item.presentacion,
                        "cantidadTotal" to item.cantidadTotal.toDouble(),
                        "cantidadComprada" to item.cantidadComprada.toDouble(),
                        "bonificacionGratis" to item.bonificacionGratis.toDouble(),
                        "costoTotal" to costoTotalItem,
                        "costoUnitario" to item.costoUnitarioReal,
                        "loteNumero" to item.loteNumero.trim().uppercase(),
                        "vencimiento" to vencimientoFinalLote,
                        "esLoteExistente" to (loteActualData != null),
                        "proveedorNombre" to kardexProveedor,
                        "facturaNumero" to kardexFactura,
                        "condicionPago" to condicionPago,
                        "usuarioEmail" to usuarioEmail,
                        "usuarioNombre" to usuarioNombre,
                        "pedidoId" to (pedidoId ?: ""),
                        "origen" to if (esRecepcionPedido) "RECEPCION_PEDIDO" else "INGRESO_DIRECTO",
                        "fecha" to FieldValue.serverTimestamp()
                    )
                    tx.set(movimientoRef, movimientoData)
                    itemsFacturaList.add(
                        mapOf(
                            "productoId" to item.productoId,
                            "productoNombre" to item.productoNombre,
                            "empaque" to item.presentacion,
                            "loteNumero" to item.loteNumero.trim().uppercase(),
                            "vencimiento" to vencimientoFinalLote,
                            "cantidadTotal" to item.cantidadTotal.toDouble(),
                            "cantidadComprada" to item.cantidadComprada.toDouble(),
                            "bonificacionGratis" to item.bonificacionGratis.toDouble(),
                            "costoTotal" to costoTotalItem,
                            "costoUnitario" to item.costoUnitarioReal
                        )
                    )
                }

                // ── 2. Factura inmutable ──
                if (numFacturaLimpio.isNotBlank() && effectiveFacturaId != null && facturaRef != null && !facturaSnapExists) {
                    val estadoPago = if (condicionPago.contains("Contado", ignoreCase = true)) "PAGADO" else "PENDIENTE"
                    val montoTotalDoc = if (montoFactura > 0) montoFactura else totalCostoCalculado
                    val facturaData = mapOf(
                        "id" to effectiveFacturaId,
                        "numeroFactura" to numFacturaLimpio,
                        "proveedorId" to proveedorIdTx,
                        "proveedorNombre" to proveedorNombreTx,
                        "rucProveedor" to proveedorRucTx,
                        "pedidoId" to (pedidoId ?: ""),
                        "condicionPago" to condicionPago,
                        "fechaVencimientoPago" to fechaVencimientoPago,
                        "estadoPago" to estadoPago,
                        "montoTotal" to montoTotalDoc,
                        "montoAcumulado" to totalCostoCalculado,
                        "items" to itemsFacturaList,
                        "usuarioRegistroEmail" to usuarioEmail,
                        "creadoEl" to FieldValue.serverTimestamp(),
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                    tx.set(facturaRef, facturaData)
                } else if (numFacturaLimpio.isNotBlank() && facturaSnapExists) {
                    // Ya existe → no se edita (inmutable). El throw arriba ya habría abortado, pero si llegó aquí por carrera, no tocar.
                }

                // ── 3. Pedido (si es recepción) ──
                if (esRecepcionPedido && pedidoRef != null) {
                    val updatedItemsList = currentItemsRaw.map { rawMap ->
                        val prodId = rawMap["productoId"] as? String ?: ""
                        val itemRec = itemsConIngreso.find { it.productoId == prodId }
                        val prev = (rawMap["cantidadRecibida"] as? Number)?.toInt() ?: 0
                        val nuevo = prev + (itemRec?.cantidadTotal ?: 0)
                        rawMap.toMutableMap().apply { put("cantidadRecibida", nuevo) }
                    }
                    val entregaData = mapOf(
                        "id" to idempotenciaId.ifBlank { UUID.randomUUID().toString() },
                        "fechaLegible" to fechaLegible,
                        "fechaMs" to ahoraMs,
                        "usuarioNombre" to (usuarioNombre.ifBlank { SessionManager.nombreUsuario.ifBlank { "Administración" } }),
                        "usuarioEmail" to usuarioEmail,
                        "numeroFactura" to numFacturaLimpio,
                        "condicionPago" to condicionPago,
                        "fechaVencimientoPago" to fechaVencimientoPago,
                        "montoFactura" to (if (montoFactura > 0) montoFactura else totalCostoCalculado),
                        "cierreConAjuste" to cerrarConAjuste,
                        "origen" to "RECEPCION_PEDIDO",
                        "items" to itemsConIngreso.map { it2 ->
                            mapOf(
                                "productoId" to it2.productoId,
                                "productoNombre" to it2.productoNombre,
                                "presentacion" to it2.presentacion,
                                "loteNumero" to it2.loteNumero.trim().uppercase(),
                                "vencimiento" to it2.vencimiento.trim(),
                                "cantidadComprada" to it2.cantidadComprada,
                                "bonificacionGratis" to it2.bonificacionGratis,
                                "cantidadTotal" to it2.cantidadTotal,
                                "costoUnitarioReal" to it2.costoUnitarioReal,
                                "costoTotalReal" to it2.costoTotalReal
                            )
                        }
                    )
                    val todasCompletas = updatedItemsList.all {
                        val pedida = (it["cantidad"] as? Number)?.toInt() ?: 0
                        val recibida = (it["cantidadRecibida"] as? Number)?.toInt() ?: 0
                        recibida >= pedida
                    }
                    val nuevoEstado = when {
                        cerrarConAjuste -> "COMPLETADA_AJUSTE"
                        todasCompletas -> "RECIBIDO"
                        else -> "ENTREGA_PARCIAL"
                    }
                    val montoPrevio = pedidoSnap?.getDouble("montoFacturadoReal") ?: 0.0
                    tx.update(
                        pedidoRef,
                        mapOf(
                            "items" to updatedItemsList,
                            "recepciones" to FieldValue.arrayUnion(entregaData),
                            "montoFacturadoReal" to (montoPrevio + if (montoFactura > 0) montoFactura else totalCostoCalculado),
                            "estado" to nuevoEstado,
                            "fechaRecepcion" to fechaLegible,
                            "actualizadoEl" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error ingresando mercadería pedidoId=$pedidoId factura=$numeroFactura: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Compatibilidad para StockEntry simple (un solo producto) — delega al método principal con lista de 1
    suspend fun ingresarLoteSimple(
        productId: String,
        productoNombre: String,
        empaque: String,
        numeroLote: String,
        vencimiento: String,
        cantidadTotal: Double,
        cantidadComprada: Double,
        bonificacion: Double,
        costoTotal: Double,
        costoUnitario: Double,
        proveedorId: String,
        proveedorNombre: String,
        rucProveedor: String,
        numeroFactura: String,
        montoTotalFactura: Double,
        condicionPago: String,
        fechaVencimientoPago: String,
        stockMinimoNuevo: Double,
        usuarioEmail: String,
        pedidoId: String? = null,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val item = ItemRecepcionEntrega(
            productoId = productId,
            productoNombre = productoNombre,
            presentacion = empaque,
            loteNumero = numeroLote,
            vencimiento = vencimiento,
            cantidadComprada = cantidadComprada.toInt(),
            bonificacionGratis = bonificacion.toInt(),
            cantidadTotal = cantidadTotal.toInt(),
            costoUnitarioReal = costoUnitario,
            costoTotalReal = costoTotal
        )
        return ingresar(
            pedidoId = pedidoId,
            numeroFactura = numeroFactura,
            condicionPago = condicionPago,
            fechaVencimientoPago = fechaVencimientoPago,
            montoFactura = montoTotalFactura,
            items = listOf(item),
            cerrarConAjuste = false,
            usuarioEmail = usuarioEmail,
            usuarioNombre = "",
            idempotenciaId = idempotenciaId,
            stockMinimoNuevoPorProducto = if (stockMinimoNuevo > 0) mapOf(productId to stockMinimoNuevo) else emptyMap(),
            proveedorIdFactura = proveedorId,
            proveedorNombreFactura = proveedorNombre,
            rucProveedorFactura = rucProveedor
        )
    }

    /**
     * Anula factura inmutable de forma quirúrgica — 1 tx o nada.
     * Si conDevolucion=false → solo anula deuda (factura ANULADA + kardex ANULACION sin tocar stock)
     * Si conDevolucion=true → descuenta stock: si lote nació solo por esa factura se borra, si ya existía se descuenta.
     * Nunca deja fantasma 0u sin entradas.
     */
    suspend fun anularFactura(
        facturaId: String,
        motivo: String,
        conDevolucion: Boolean,
        usuarioEmail: String = "",
        usuarioNombre: String = ""
    ): Result<Unit> {
        if (motivo.trim().isBlank()) return Result.failure(IllegalArgumentException("El motivo de anulación es obligatorio."))
        val ids = obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val (farmaciaId, sucursalId) = ids
        if (facturaId.isBlank()) return Result.failure(IllegalArgumentException("ID de factura inválido."))
        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
            val facturaRef = tiendaRef.collection("compras_facturas").document(facturaId)
            val ahoraMs = HoraServidor.ahoraMs()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaLegible = sdf.format(Date(ahoraMs))
            db.runTransaction { tx ->
                val facturaSnap = tx.get(facturaRef)
                if (!facturaSnap.exists()) throw IllegalStateException("La factura no existe.")
                val estado = facturaSnap.getString("estadoPago") ?: ""
                if (estado.equals("ANULADA", true)) throw IllegalStateException("La factura ya está anulada.")
                if (motivo.trim().length < 5) throw IllegalArgumentException("El motivo debe tener al menos 5 caracteres.")
                val abonos = facturaSnap.get("abonos") as? List<*> ?: emptyList<Any>()
                val montoPagado = facturaSnap.getDouble("montoPagado") ?: 0.0
                val totalAbonado = if (abonos.isNotEmpty()) {
                    abonos.sumOf { (it as? Map<*, *>)?.let { m -> (m["monto"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0 }
                } else montoPagado
                if (abonos.isNotEmpty() || totalAbonado > 0.01) throw IllegalStateException("La factura tiene ${if (abonos.isNotEmpty()) abonos.size else 1} abono(s) por S/ ${String.format(java.util.Locale.US, "%.2f", totalAbonado)}. Anúlalos primero en Cuentas por Pagar.")
                val numeroFactura = facturaSnap.getString("numeroFactura") ?: facturaId
                val pedidoId = facturaSnap.getString("pedidoId") ?: ""
                @Suppress("UNCHECKED_CAST")
                val itemsRaw = facturaSnap.get("items") as? List<Map<String, Any>> ?: emptyList()
                if (itemsRaw.isEmpty() && conDevolucion) throw IllegalStateException("La factura no tiene productos para devolver.")

                // Si con devolución, validar y descontar stock por cada item
                if (conDevolucion) {
                    for (itemMap in itemsRaw) {
                        val productoId = itemMap["productoId"] as? String ?: ""
                        if (productoId.isBlank()) continue
                        val loteNumero = itemMap["loteNumero"] as? String ?: ""
                        val cantidadTotal = (itemMap["cantidadTotal"] as? Number)?.toDouble() ?: 0.0
                        if (cantidadTotal <= 0) continue
                        val productRef = tiendaRef.collection("inventario").document(productoId)
                        val productSnap = tx.get(productRef)
                        if (!productSnap.exists()) throw IllegalStateException("El producto $productoId de la factura ya no existe en inventario.")
                        @Suppress("UNCHECKED_CAST")
                        val currentLotes = (productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                        val res = FechaVencimientoHelper.resolverLote(currentLotes, loteNumero)
                            ?: throw IllegalStateException("El lote $loteNumero de $productoId no existe. Ya se vendió o fue borrado.")
                        val key = res.first
                        val loteData = res.second as? Map<*, *>
                        val cantidadActual = (loteData?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                        val cantBloq = (loteData?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                        if (cantidadActual < cantidadTotal - 0.001) throw IllegalStateException("Stock insuficiente en lote $loteNumero para $productoId: quedan ${cantidadActual.toInt()}u, necesitas ${cantidadTotal.toInt()}u. Vende menos o anula solo deuda sin devolver stock.")
                        @Suppress("UNCHECKED_CAST")
                        val entradas = (loteData?.get("entradas") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                        val esUnicoDeEstaFactura = entradas.size == 1 && (entradas[0]["factura"] as? String)?.equals(numeroFactura, true) == true
                        if (esUnicoDeEstaFactura && kotlin.math.abs(cantidadActual - cantidadTotal) < 0.001 && cantBloq < 0.001) {
                            // Lote nació solo por esta factura → se borra entero
                            currentLotes.remove(key)
                        } else {
                            // Lote ya existía → se descuenta y se quita esa entrada del historial
                            val nuevaCantidad = (cantidadActual - cantidadTotal).coerceAtLeast(0.0)
                            entradas.removeAll { (it["factura"] as? String)?.equals(numeroFactura, true) == true }
                            // Recalcular costo promedio con lo que queda
                            var costoRestante = 0.0
                            var cantidadRestante = 0.0
                            for (e in entradas) {
                                val c = (e["cantidad"] as? Number)?.toDouble() ?: 0.0
                                val cu = (e["costoUnitario"] as? Number)?.toDouble() ?: 0.0
                                costoRestante += c * cu
                                cantidadRestante += c
                            }
                            // Si quedan otras entradas, promedia; si no, usa costo del lote restante (0)
                            val nuevoCostoUnit = if (cantidadRestante > 0) costoRestante / cantidadRestante else 0.0
                            val loteNuevo = (loteData as Map<String, Any>).toMutableMap()
                            loteNuevo["cantidad"] = nuevaCantidad
                            loteNuevo["entradas"] = entradas
                            if (entradas.isEmpty()) loteNuevo.remove("entradas")
                            loteNuevo["costoUnitario"] = nuevoCostoUnit
                            loteNuevo["costoCompra"] = nuevoCostoUnit * (nuevaCantidad + cantBloq)
                            loteNuevo["ultimaEntrada"] = FieldValue.serverTimestamp()
                            if (kotlin.math.abs(nuevaCantidad) < 0.001 && cantBloq < 0.001 && entradas.isEmpty()) {
                                currentLotes.remove(key)
                            } else {
                                currentLotes[key] = loteNuevo
                            }
                        }
                        val nuevoStockDisp = currentLotes.values.sumOf { (it as? Map<*, *>)?.let { m -> (m["cantidad"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0 }
                        val nuevoStockTotal = currentLotes.values.sumOf {
                            val d = it as? Map<*, *>
                            val cd = (d?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                            val cb = (d?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                            cd + cb
                        }
                        val vencMasCercano = FechaVencimientoHelper.vencimientoMasCercano(currentLotes)
                        val lotesParaCosto = currentLotes.values.mapNotNull { it as? Map<*, *> }
                        val totalFis = lotesParaCosto.sumOf {
                            val cd = (it["cantidad"] as? Number)?.toDouble() ?: 0.0
                            val cb = (it["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                            cd + cb
                        }
                        val costoPond = lotesParaCosto.sumOf {
                            val cd = (it["cantidad"] as? Number)?.toDouble() ?: 0.0
                            val cb = (it["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                            val cu = (it["costoUnitario"] as? Number)?.toDouble() ?: 0.0
                            (cd + cb) * cu
                        }
                        val updates = mutableMapOf<String, Any>(
                            "lotes" to currentLotes,
                            "stock" to nuevoStockDisp,
                            "stockTotal" to nuevoStockTotal,
                            "vencimientoMasCercano" to vencMasCercano,
                            "actualizadoEl" to FieldValue.serverTimestamp()
                        )
                        if (totalFis > 0 && costoPond > 0) updates["precioCompra"] = costoPond / totalFis
                        tx.update(productRef, updates)
                        // Kardex egreso por anulación
                        val movRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
                        val movData = mapOf(
                            "id" to movRef.id,
                            "tipo" to "EGRESO_ANULACION_FACTURA",
                            "productoId" to productoId,
                            "productoNombre" to (itemMap["productoNombre"] as? String ?: ""),
                            "cantidadTotal" to -cantidadTotal,
                            "loteNumero" to loteNumero,
                            "facturaNumero" to numeroFactura,
                            "motivo" to motivo.trim(),
                            "usuarioEmail" to usuarioEmail,
                            "usuarioNombre" to usuarioNombre,
                            "fecha" to FieldValue.serverTimestamp()
                        )
                        tx.set(movRef, movData)
                    }
                } else {
                    // Sin devolución: solo kardex informativo por cada producto
                    for (itemMap in itemsRaw) {
                        val movRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
                        val movData = mapOf(
                            "id" to movRef.id,
                            "tipo" to "ANULACION_FACTURA_SIN_DEVOLUCION",
                            "productoId" to (itemMap["productoId"] as? String ?: ""),
                            "facturaNumero" to numeroFactura,
                            "motivo" to motivo.trim(),
                            "usuarioEmail" to usuarioEmail,
                            "usuarioNombre" to usuarioNombre,
                            "fecha" to FieldValue.serverTimestamp()
                        )
                        tx.set(movRef, movData)
                    }
                }

                // Si tenía pedido, revertir solo si conDevolucion (si solo anulas deuda, pedido queda como recibido)
                if (pedidoId.isNotBlank()) {
                    val pedidoRef = tiendaRef.collection("pedidos_compra").document(pedidoId)
                    val pedidoSnap = tx.get(pedidoRef)
                    if (pedidoSnap.exists()) {
                        val montoFactRaw = facturaSnap.getDouble("montoTotal") ?: facturaSnap.getDouble("montoAcumulado") ?: 0.0
                        val totalFact = if (montoFactRaw > 0) montoFactRaw else itemsRaw.sumOf { (it["costoTotal"] as? Number)?.toDouble() ?: (it["montoTotal"] as? Number)?.toDouble() ?: 0.0 }
                        // Monto siempre se revierte (deuda anulada) aunque no devuelvas stock
                        val montoPrevio = pedidoSnap.getDouble("montoFacturadoReal") ?: 0.0
                        val nuevoMonto = (montoPrevio - totalFact).coerceAtLeast(0.0)
                        if (conDevolucion) {
                            @Suppress("UNCHECKED_CAST")
                            val itemsPed = (pedidoSnap.get("items") as? List<Map<String, Any>>) ?: emptyList()
                            val updatedItems = itemsPed.map { raw ->
                                val pid = raw["productoId"] as? String ?: ""
                                val cantMap = itemsRaw.find { (it["productoId"] as? String) == pid }
                                val cantFact = (cantMap?.get("cantidadTotal") as? Number)?.toDouble()?.toInt() ?: 0
                                val prev = (raw["cantidadRecibida"] as? Number)?.toInt() ?: 0
                                raw.toMutableMap().apply { put("cantidadRecibida", (prev - cantFact).coerceAtLeast(0)) }
                            }
                            val nuevoEstado = when {
                                updatedItems.all { ((it["cantidadRecibida"] as? Number)?.toInt() ?: 0) == 0 } -> "ENVIADO"
                                updatedItems.all { val p = (it["cantidad"] as? Number)?.toInt() ?: 0; val r = (it["cantidadRecibida"] as? Number)?.toInt() ?: 0; r >= p } -> "RECIBIDO"
                                else -> "ENTREGA_PARCIAL"
                            }
                            // Añade acta de anulación para auditoría, no borra la recepción original
                            val actaAnulacion = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "fechaLegible" to fechaLegible,
                                "fechaMs" to ahoraMs,
                                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                                "usuarioEmail" to usuarioEmail,
                                "numeroFactura" to numeroFactura,
                                "tipo" to "ANULACION",
                                "motivo" to motivo.trim(),
                                "conDevolucion" to true
                            )
                            tx.update(pedidoRef, mapOf(
                                "items" to updatedItems,
                                "montoFacturadoReal" to nuevoMonto,
                                "recepciones" to FieldValue.arrayUnion(actaAnulacion),
                                "estado" to nuevoEstado,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            ))
                        } else {
                            // Solo deuda: no toca cantidadRecibida ni estado, solo monto y acta
                            val actaAnulacion = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "fechaLegible" to fechaLegible,
                                "fechaMs" to ahoraMs,
                                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                                "usuarioEmail" to usuarioEmail,
                                "numeroFactura" to numeroFactura,
                                "tipo" to "ANULACION_SIN_DEVOLUCION",
                                "motivo" to motivo.trim()
                            )
                            tx.update(pedidoRef, mapOf(
                                "montoFacturadoReal" to nuevoMonto,
                                "recepciones" to FieldValue.arrayUnion(actaAnulacion),
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            ))
                        }
                    }
                }

                // Factura a ANULADA (inmutable, no se borra)
                tx.update(facturaRef, mapOf(
                    "estadoPago" to "ANULADA",
                    "motivoAnulacion" to motivo.trim(),
                    "anuladoPorEmail" to usuarioEmail,
                    "anuladoPorNombre" to usuarioNombre,
                    "anuladoEl" to FieldValue.serverTimestamp(),
                    "actualizadoEl" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error anulando factura $facturaId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
