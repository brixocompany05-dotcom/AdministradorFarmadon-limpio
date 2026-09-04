package com.app.administradorfarmadon.inventario.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega
import com.app.administradorfarmadon.compras.saldoafavor.datos.SaldoAFavorFirestore
import com.app.administradorfarmadon.inventario.compartido.logica.CostoLoteCalculator
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

    private fun crearAbonoRecepcion(
        idRecepcion: String,
        monto: Double,
        metodoPago: String,
        pagos: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle>,
        usuarioNombre: String,
        usuarioEmail: String,
        ahoraMs: Long,
        fechaLegible: String
    ): Map<String, Any> = mapOf(
        "id" to "recepcion-$idRecepcion",
        "fechaLegible" to fechaLegible,
        "fechaMs" to ahoraMs,
        "monto" to monto,
        "metodoPago" to (pagos.firstOrNull()?.metodoPago ?: metodoPago),
        "numeroOperacion" to (pagos.firstOrNull()?.numeroOperacion ?: ""),
        "pagos" to pagos.map { p ->
            mapOf(
                "metodoPago" to p.metodoPago,
                "monto" to p.monto,
                "numeroOperacion" to p.numeroOperacion
            )
        },
        "usuarioNombre" to usuarioNombre.ifBlank { SessionManager.nombreUsuario },
        "usuarioEmail" to usuarioEmail,
        "notas" to if (pagos.isEmpty() && metodoPago.isBlank()) "Pago registrado junto con la recepción"
                else "Pago registrado junto con la recepción: " + pagos.joinToString(" + ") { p ->
                    "${p.metodoPago} ${String.format(java.util.Locale.US, "%.2f", p.monto)}"
                }
    )

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
        fechaEmisionPapel: String = "",
        montoFactura: Double = 0.0,
        montoPagadoEnRecepcion: Double = 0.0,
        metodoPago: String = "",
        pagosRecepcion: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList(),
        saldoAFavorUsado: Double = 0.0,
        items: List<ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean = false,
        usuarioEmail: String = "",
        usuarioNombre: String = "",
        idempotenciaId: String = "",
        stockMinimoNuevoPorProducto: Map<String, Double> = emptyMap(),
        proveedorIdFactura: String = "",
        proveedorNombreFactura: String = "",
        rucProveedorFactura: String = "",
        farmaciaIdParam: String? = null,
        sucursalIdParam: String? = null
    ): Result<Unit> {
        val ids = if (farmaciaIdParam != null && sucursalIdParam != null) {
            Pair(farmaciaIdParam, sucursalIdParam)
        } else {
            obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        }
        val (farmaciaId, sucursalId) = ids

        val esRecepcionPedido = !pedidoId.isNullOrBlank()
        val numFacturaLimpio = numeroFactura.trim().uppercase()

        // Validaciones raíz — antes de tocar Firestore
        if (numFacturaLimpio.isBlank()) {
            return Result.failure(IllegalArgumentException("El número de comprobante no puede estar vacío. Escribe el número de factura o 'S/C' si ingresa sin comprobante."))
        }
        if (fechaEmisionPapel.trim().isNotBlank()) {
            val fechaPapelLimpia = fechaEmisionPapel.trim()
            try {
                val sdfVerif = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("America/Lima")
                }
                val datePapel = sdfVerif.parse(fechaPapelLimpia)
                val msPapel = datePapel?.time ?: 0L
                val calHoyLima = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
                    timeInMillis = HoraServidor.ahoraMs()
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val finDeHoyMs = calHoyLima.timeInMillis
                if (msPapel > finDeHoyMs) {
                    return Result.failure(IllegalArgumentException("La fecha de emisión del documento ($fechaPapelLimpia) no puede ser posterior a la fecha de hoy."))
                }
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("La fecha de emisión '$fechaPapelLimpia' debe tener formato válido dd/MM/yyyy."))
            }
        }
        val itemsConIngreso = items.filter { it.cantidadTotal > 0 }
        if (itemsConIngreso.isEmpty()) {
            return Result.failure(IllegalArgumentException("No llegaron unidades en esta recepción. Si nada llegó, CANCELA o DESCARTA la orden; no se puede asentar un ingreso vacío."))
        }
        val hayDineroSinFactura = montoPagadoEnRecepcion > 0.01 || pagosRecepcion.isNotEmpty() || saldoAFavorUsado > 0.01
        if ((numFacturaLimpio == "S/C" || numFacturaLimpio.startsWith("S/C")) && hayDineroSinFactura) {
            return Result.failure(IllegalArgumentException(
                "Ingreso S/C sin comprobante no puede tener pagos registrados ni saldo a favor. Registra el número de factura para pagar o deja el pago en 0.00."
            ))
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
                val esFacturaFormal = numFacturaLimpio != "S/C" && !numFacturaLimpio.startsWith("S/C")

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
                    val huellaPagos = pagosRecepcion.joinToString("+") { "${it.metodoPago.trim().uppercase()}_${it.monto}_${it.numeroOperacion.trim()}" }
                    val huellaEntrega = "rec_${pedidoId ?: "suelto"}_${numFacturaLimpio}_${montoFactura}_${montoPagadoEnRecepcion}_${metodoPago.trim().uppercase()}_${huellaPagos}_${saldoAFavorUsado}_${condicionPago.trim().uppercase()}_${fechaVencimientoPago.trim()}_${itemsConIngreso.joinToString("_") { "${it.productoId}_${it.cantidadTotal}_${it.cantidadComprada}_${it.bonificacionGratis}_${it.costoUnitarioReal}_${it.costoTotalReal}_${it.loteNumero.trim().uppercase()}_${it.vencimiento.trim()}" }}"
                    val idEntregaFinal = if (idempotenciaId.isNotBlank()) idempotenciaId.trim() else "rec_" + Math.abs(huellaEntrega.hashCode()).toString()

                    @Suppress("UNCHECKED_CAST")
                    recepcionesPrevias = (pedidoSnap.get("recepciones") as? List<Map<String, Any>>) ?: emptyList()
                    if (recepcionesPrevias.any { it["id"] == idEntregaFinal || (idempotenciaId.isNotBlank() && it["id"] == idempotenciaId.trim()) }) return@runTransaction
                    proveedorIdTx = pedidoSnap.getString("proveedorId") ?: ""
                    proveedorNombreTx = pedidoSnap.getString("proveedorNombre") ?: ""
                    proveedorRucTx = pedidoSnap.getString("proveedorRuc") ?: ""
                } else {
                    // Directo suelto — proveedor viene de los params de factura
                    proveedorIdTx = proveedorIdFactura.trim()
                    proveedorNombreTx = proveedorNombreFactura.trim()
                    proveedorRucTx = rucProveedorFactura.trim()
                    if (proveedorNombreTx.isBlank() || listOf("SIN PROVEEDOR", "DROGUERÍA GENERAL", "DROGUERIA GENERAL", "SIN ASIGNAR").any { proveedorNombreTx.equals(it, ignoreCase = true) }) {
                        throw IllegalArgumentException("Selecciona un proveedor real con RUC de 11 dígitos para asentar el ingreso.")
                    }
                    if (esFacturaFormal && (proveedorRucTx.isBlank() || !proveedorRucTx.all { it.isDigit() } || proveedorRucTx.length != 11)) {
                        throw IllegalArgumentException("Un comprobante formal ($numFacturaLimpio) exige el RUC de 11 dígitos del proveedor.")
                    }
                }

                var facturaRef: com.google.firebase.firestore.DocumentReference? = null
                var facturaSnapExists = false
                var effectiveFacturaId: String? = null
                var esReusoFactura = false
                var facturaSnapReuso: com.google.firebase.firestore.DocumentSnapshot? = null
                var provKey = ""
                if (esFacturaFormal) {
                    provKey = when {
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
                            // Misma factura para entrega parcial del mismo pedido: el papel no se
                            // reescribe; los pagos de esta entrega se anexan como abonos (append-only).
                            esReusoFactura = true
                            facturaSnapExists = false
                            facturaSnapReuso = snap
                        } else {
                            throw IllegalArgumentException("La factura $numFacturaLimpio ya fue registrada para ${if (provKey.isNotBlank()) proveedorNombreTx else "este proveedor"}. Usa un número nuevo.")
                        }
                    }
                    // Legacy fallback: si no existe con prefijo pero existe sin prefijo y mismo proveedor, también es duplicado
                    if (!esReusoFactura && !facturaSnapExists && provKey.isNotBlank() && facturaRef != null) {
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
                                        // Factura legada (id sin prefijo) de ESTE pedido: es el mismo papel.
                                        // Se reutiliza para anexar pagos como abonos; si no, la plata de esta
                                        // entrega quedaría sin asiento (datos a medias).
                                        facturaRef = legacyRef
                                        effectiveFacturaId = legacyRef.id
                                        esReusoFactura = true
                                        facturaSnapExists = false
                                        facturaSnapReuso = legSnap
                                    } else {
                                        throw IllegalArgumentException("La factura $numFacturaLimpio ya existe (registro legado) para $proveedorNombreTx. Usa número nuevo.")
                                    }
                                }
                            }
                        }
                    }
                }

                // ══ FASE DE LECTURAS (Firestore: TODAS las lecturas antes de cualquier escritura) ══
                // Foto fresca de CADA producto de la recepción (con 2+ productos, leer dentro
                // del bucle de escrituras abortaría la transacción entera: "reads before writes").
                val productoSnaps = HashMap<String, com.google.firebase.firestore.DocumentSnapshot>(itemsConIngreso.size)
                for (item in itemsConIngreso) {
                    if (productoSnaps.containsKey(item.productoId)) continue
                    val snapProducto = tx.get(tiendaRef.collection("inventario").document(item.productoId))
                    if (!snapProducto.exists()) throw IllegalStateException("El producto '${item.productoNombre}' (ID: ${item.productoId}) no existe en inventario.")
                    productoSnaps[item.productoId] = snapProducto
                }
                // Foto fresca del saldo a favor SI se va a descontar en esta operación.
                val saldoSnap = if (saldoAFavorUsado > 0.01) {
                    if (proveedorIdTx.isBlank()) throw IllegalStateException("La factura no tiene proveedor vinculado; no se puede aplicar el saldo a favor.")
                    tx.get(SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorIdTx))
                } else null

                // ── 1. Actualizar cada producto + kardex (fase de escrituras) ──
                val itemsFacturaList = mutableListOf<Map<String, Any>>()
                var totalCostoCalculado = 0.0
                // ACUMULADOR POR PRODUCTO (R3, cero pérdida): una misma recepción puede
                // traer el MISMO producto en 2+ lotes distintos. Cada fila debe partir
                // del mapa YA mutado por la fila anterior — si cada una arrancara de la
                // foto original, la segunda fila BORRARÍA el lote de la primera al
                // sobrescribir el campo "lotes".
                val lotesAcumuladosPorProducto = HashMap<String, MutableMap<Any?, Any?>>()

                for (item in itemsConIngreso) {
                    val productRef = tiendaRef.collection("inventario").document(item.productoId)
                    val productSnap = productoSnaps[item.productoId]
                        ?: throw IllegalStateException("No se pudo leer el producto '${item.productoNombre}' dentro de la operación.")
                    val movimientoId = UUID.randomUUID().toString()
                    val movimientoRef = tiendaRef.collection("movimientos").document(movimientoId)
                    val cleanLoteKey = FechaVencimientoHelper.llaveLote(item.loteNumero)
                    val currentLotes = lotesAcumuladosPorProducto.getOrPut(item.productoId) {
                        @Suppress("UNCHECKED_CAST")
                        ((productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>())
                    }
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
                        "ultimaEntrada" to Timestamp(Date(ahoraMs))
                    )
                    if (cantBloqueadaExistente > 0) loteFinalData["cantidadBloqueada"] = cantBloqueadaExistente
                    if (!motivoBloqueoExistente.isNullOrBlank()) loteFinalData["motivoBloqueo"] = motivoBloqueoExistente
                    if (!estadoSanitarioExistente.isNullOrBlank()) loteFinalData["estadoSanitario"] = estadoSanitarioExistente
                    // Preservar la trazabilidad sanitaria y el nacimiento real del lote:
                    // si un lote ya vendió, esa verdad no puede perderse al recibir más unidades.
                    (loteActualData?.get("ventasRegistradas") as? Number)?.let { loteFinalData["ventasRegistradas"] = it.toDouble() }
                    loteActualData?.get("fechaIngreso")?.let { loteFinalData["fechaIngreso"] = it }
                    loteActualData?.get("createdAt")?.let { loteFinalData["createdAt"] = it }
                    if (loteActualData == null) loteFinalData["fechaIngreso"] = Timestamp(Date(ahoraMs))
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
                        productUpdates["precioCompra"] = redondear2(costoPonderadoLotes / totalFisicoLotes)
                    } else if (item.costoUnitarioReal > 0.0) {
                        productUpdates["precioCompra"] = redondear2(item.costoUnitarioReal)
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
                    val provEsPlaceholder = provActual.isBlank() || listOf("N/A", "NA", "Genérico", "Sin asignar", "Sin Asignar")
                        .any { it.equals(provActual.trim(), ignoreCase = true) }
                    val esVacio = provEsPlaceholder
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
                if (esFacturaFormal && effectiveFacturaId != null && facturaRef != null && !facturaSnapExists) {
                    val saldoUsado = saldoAFavorUsado.coerceAtLeast(0.0)
                    val hayPagoNuevo = montoPagadoEnRecepcion > 0.01 || saldoUsado > 0.01 || pagosRecepcion.isNotEmpty()

                    if (esReusoFactura) {
                        // Entrega parcial de la MISMA factura: el papel ya existe y no se reescribe.
                        // Los pagos reales de esta entrega se anexan como abonos (append-only),
                        // igual que en Cuentas por Pagar; el saldo a favor se descuenta del proveedor.
                        if (hayPagoNuevo) {
                            val snapFact = facturaSnapReuso
                                ?: throw IllegalStateException("La factura $numFacturaLimpio no se pudo leer dentro de la operación. No se asentó nada; intenta de nuevo.")
                            if (!snapFact.exists()) throw IllegalStateException("La factura $numFacturaLimpio ya no existe; no se pudo registrar el pago de esta entrega. Regístralo desde Cuentas por Pagar.")
                            if ((snapFact.getString("estadoPago") ?: "").equals("ANULADA", ignoreCase = true)) {
                                throw IllegalStateException("La factura $numFacturaLimpio fue ANULADA. No se puede recibir ni pagar sobre una factura anulada: usa un número de factura nuevo para esta entrega.")
                            }
                            @Suppress("UNCHECKED_CAST")
                            val abonosPrevios = (snapFact.get("abonos") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                            val montoTotalFactura = snapFact.getDouble("montoTotal") ?: snapFact.getDouble("montoAcumulado") ?: montoFactura
                            val montoPagadoPrevio = snapFact.getDouble("montoPagado") ?: abonosPrevios.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                            val pagosFinales = if (pagosRecepcion.isNotEmpty()) pagosRecepcion
                                else if (montoPagadoEnRecepcion > 0.0) listOf(
                                    com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle(
                                        metodoPago = metodoPago,
                                        monto = montoPagadoEnRecepcion
                                    )
                                ) else emptyList()
                            if (pagosFinales.isNotEmpty() && kotlin.math.abs(pagosFinales.sumOf { it.monto } - montoPagadoEnRecepcion) > 0.01) {
                                throw IllegalArgumentException("La distribución del pago no cuadra con el monto registrado.")
                            }
                            val abonosNuevos = mutableListOf<Map<String, Any>>()
                            if (montoPagadoEnRecepcion > 0.0) {
                                abonosNuevos.add(crearAbonoRecepcion(
                                    idRecepcion = idempotenciaId.ifBlank { UUID.randomUUID().toString() },
                                    monto = montoPagadoEnRecepcion,
                                    metodoPago = metodoPago,
                                    pagos = pagosFinales,
                                    usuarioNombre = usuarioNombre,
                                    usuarioEmail = usuarioEmail,
                                    ahoraMs = ahoraMs,
                                    fechaLegible = fechaLegible
                                ))
                            }
                            if (saldoUsado > 0.0) {
                                abonosNuevos.add(SaldoAFavorFirestore.crearAbonoSaldoAFavor(
                                    id = UUID.randomUUID().toString(),
                                    monto = saldoUsado,
                                    facturaNumero = numFacturaLimpio,
                                    motivo = "Saldo a favor aplicado en recepción de $numFacturaLimpio",
                                    usuarioNombre = usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                                    usuarioEmail = usuarioEmail,
                                    ahoraMs = ahoraMs,
                                    fechaLegible = fechaLegible
                                ))
                            }
                            val totalPagadoNuevo = montoPagadoPrevio + montoPagadoEnRecepcion.coerceAtLeast(0.0) + saldoUsado
                            if (totalPagadoNuevo > montoTotalFactura + 0.01) {
                                throw IllegalArgumentException("El pago supera el total de la factura $numFacturaLimpio.")
                            }
                            if (saldoUsado > 0.0) {
                                if (proveedorIdTx.isBlank()) throw IllegalStateException("La factura no tiene proveedor vinculado; no se puede aplicar el saldo a favor.")
                                SaldoAFavorFirestore.aplicarEnTransaccion(
                                    tx = tx,
                                    refSaldo = SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorIdTx),
                                    saldoAUsar = saldoUsado,
                                    facturaId = effectiveFacturaId,
                                    facturaNumero = numFacturaLimpio,
                                    motivo = "Saldo a favor aplicado en recepción de $numFacturaLimpio",
                                    usuarioNombre = usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                                    usuarioEmail = usuarioEmail,
                                    ahoraMs = ahoraMs,
                                    fechaLegible = fechaLegible,
                                    saldoSnapshot = saldoSnap
                                )
                            }
                            val nuevoEstado = when {
                                totalPagadoNuevo >= montoTotalFactura - 0.01 -> "PAGADA"
                                totalPagadoNuevo > 0.01 -> "ABONADO_PARCIAL"
                                else -> snapFact.getString("estadoPago") ?: "PENDIENTE"
                            }
                            tx.update(
                                facturaRef,
                                mapOf(
                                    "abonos" to abonosPrevios + abonosNuevos,
                                    "montoPagado" to totalPagadoNuevo,
                                    "estadoPago" to nuevoEstado,
                                    "actualizadoEl" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    } else {
                        // ── CREACIÓN: factura nueva (una sola vez, inmutable) ──
                        val montoTotalDoc = if (montoFactura > 0) montoFactura else redondear2(totalCostoCalculado)

                        // Pago mixto real de la recepción: porciones + saldo a favor.
                        val pagosFinales = if (pagosRecepcion.isNotEmpty()) pagosRecepcion
                            else if (montoPagadoEnRecepcion > 0.0) listOf(
                                com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle(
                                    metodoPago = metodoPago,
                                    monto = montoPagadoEnRecepcion
                                )
                            ) else emptyList()
                        if (pagosFinales.isNotEmpty() && kotlin.math.abs(pagosFinales.sumOf { it.monto } - montoPagadoEnRecepcion) > 0.01) {
                            throw IllegalArgumentException("La distribución del pago no cuadra con el monto registrado.")
                        }
                        val totalPagado = montoPagadoEnRecepcion.coerceAtLeast(0.0) + saldoUsado
                        if (totalPagado > montoTotalDoc + 0.01) {
                            throw IllegalArgumentException("El pago registrado supera el total de la factura.")
                        }
                        // Contado = se paga completo al recibir. Jamás una factura "PAGADA" sin plata registrada.
                        if (condicionPago.contains("Contado", ignoreCase = true) && totalPagado < montoTotalDoc - 0.01) {
                            throw IllegalArgumentException(
                                "Factura al contado: registra el pago completo al recibir (faltan " +
                                    String.format(java.util.Locale.US, "%.2f", (montoTotalDoc - totalPagado)) +
                                    "). Si se pagará después, elige Crédito."
                            )
                        }
                        val estadoPago = when {
                            totalPagado <= 0.01 -> "PENDIENTE"
                            totalPagado >= montoTotalDoc - 0.01 -> "PAGADA"
                            else -> "ABONADO_PARCIAL"
                        }

                        val abonosNuevos = mutableListOf<Map<String, Any>>()
                        if (montoPagadoEnRecepcion > 0.0) {
                            abonosNuevos.add(crearAbonoRecepcion(
                                idRecepcion = idempotenciaId.ifBlank { UUID.randomUUID().toString() },
                                monto = montoPagadoEnRecepcion,
                                metodoPago = metodoPago,
                                pagos = pagosFinales,
                                usuarioNombre = usuarioNombre,
                                usuarioEmail = usuarioEmail,
                                ahoraMs = ahoraMs,
                                fechaLegible = fechaLegible
                            ))
                        }
                        if (saldoUsado > 0.0) {
                            abonosNuevos.add(SaldoAFavorFirestore.crearAbonoSaldoAFavor(
                                id = UUID.randomUUID().toString(),
                                monto = saldoUsado,
                                facturaNumero = numFacturaLimpio,
                                motivo = "Saldo a favor aplicado en la recepción",
                                usuarioNombre = usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                                usuarioEmail = usuarioEmail,
                                ahoraMs = ahoraMs,
                                fechaLegible = fechaLegible
                            ))
                        }

                        // El saldo a favor SOLO se descuenta aquí, al guardar la recepción
                        // (nunca al tocar el interruptor). Si el proveedor ya no tiene saldo,
                        // la transacción aborta y no se asienta nada.
                        if (saldoUsado > 0.0) {
                            if (proveedorIdTx.isBlank()) throw IllegalStateException("La factura no tiene proveedor vinculado; no se puede aplicar el saldo a favor.")
                            SaldoAFavorFirestore.aplicarEnTransaccion(
                                tx = tx,
                                refSaldo = SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorIdTx),
                                saldoAUsar = saldoUsado,
                                facturaId = effectiveFacturaId,
                                facturaNumero = numFacturaLimpio,
                                motivo = "Saldo a favor aplicado en la recepción",
                                usuarioNombre = usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                                usuarioEmail = usuarioEmail,
                                ahoraMs = ahoraMs,
                                fechaLegible = fechaLegible,
                                saldoSnapshot = saldoSnap
                            )
                        }

                        val tipoDocCalculado = when {
                            numFacturaLimpio.startsWith("F") -> "FACTURA"
                            numFacturaLimpio.startsWith("B") -> "BOLETA"
                            numFacturaLimpio.startsWith("G") -> "GUIA"
                            else -> "FACTURA"
                        }
                        val serieCalculada = if (numFacturaLimpio.contains("-")) numFacturaLimpio.substringBefore("-") else ""
                        val correlativoCalculado = if (numFacturaLimpio.contains("-")) numFacturaLimpio.substringAfter("-") else numFacturaLimpio
                        val baseCalculada = if (tipoDocCalculado == "FACTURA") Math.round((montoTotalDoc / 1.18) * 100.0) / 100.0 else montoTotalDoc
                        val igvCalculado = if (tipoDocCalculado == "FACTURA") Math.round((montoTotalDoc - baseCalculada) * 100.0) / 100.0 else 0.0

                        val facturaData = mapOf(
                            "id" to effectiveFacturaId,
                            "numeroFactura" to numFacturaLimpio,
                            "tipoDoc" to tipoDocCalculado,
                            "serie" to serieCalculada,
                            "correlativo" to correlativoCalculado,
                            "montoBase" to baseCalculada,
                            "montoIgv" to igvCalculado,
                            "fechaEmision" to (if (fechaEmisionPapel.trim().isNotBlank()) fechaEmisionPapel.trim() else ""),
                            "fechaRecepcion" to fechaLegible,
                            "proveedorId" to proveedorIdTx,
                            "proveedorNombre" to proveedorNombreTx,
                            "rucProveedor" to proveedorRucTx,
                            "pedidoId" to (pedidoId ?: ""),
                            "condicionPago" to condicionPago,
                            "fechaVencimientoPago" to fechaVencimientoPago,
                            "estadoPago" to estadoPago,
                            "montoTotal" to montoTotalDoc,
                            "montoAcumulado" to redondear2(totalCostoCalculado),
                            "montoPagado" to totalPagado,
                            "abonos" to abonosNuevos,
                            "items" to itemsFacturaList,
                            "usuarioRegistroEmail" to usuarioEmail,
                            "creadoEl" to FieldValue.serverTimestamp(),
                            "actualizadoEl" to FieldValue.serverTimestamp()
                        )
                        tx.set(facturaRef, facturaData)
                    }
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
                        "usuarioNombre" to (usuarioNombre.ifBlank { SessionManager.nombreUsuario }),
                        "usuarioEmail" to usuarioEmail,
                        "numeroFactura" to numFacturaLimpio,
                        "fechaEmisionPapel" to (if (fechaEmisionPapel.trim().isNotBlank()) fechaEmisionPapel.trim() else ""),
                        "condicionPago" to condicionPago,
                        "fechaVencimientoPago" to fechaVencimientoPago,
                        "montoFactura" to (if (montoFactura > 0) montoFactura else redondear2(totalCostoCalculado)),
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
                    val montoEstaEntrega = if (montoFactura > 0) montoFactura else redondear2(totalCostoCalculado)
                    // S/C = ingreso sin papel contable ni deuda. No altera montoFacturadoReal.
                    // Factura ya existente (reuso en entrega parcial): el papel ya contabilizó su total.
                    val nuevoMontoFacturado = if (!esFacturaFormal) {
                        montoPrevio
                    } else if (esReusoFactura) {
                        if (montoPrevio > 0.0) montoPrevio else montoEstaEntrega
                    } else {
                        redondear2(montoPrevio + montoEstaEntrega)
                    }
                    tx.update(
                        pedidoRef,
                        mapOf(
                            "items" to updatedItemsList,
                            "recepciones" to FieldValue.arrayUnion(entregaData),
                            "montoFacturadoReal" to nuevoMontoFacturado,
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

    /**
     * Lotes ACTUALES de cada producto (foto del estante para la anulación):
     * productoId → { númeroLote → { cantidad, vencimiento, … } }.
     * Si falla la lectura se retorna vacío: la anulación continúa sin inventario comparado.
     */
    suspend fun leerLotesDeProductos(productoIds: List<String>): Map<String, Map<String, Any>>? {
        val ids = obtenerIds() ?: return null
        val (farmaciaId, sucursalId) = ids
        val resultado = mutableMapOf<String, Map<String, Any>>()
        return try {
            for (productoId in productoIds.distinct().filter { it.isNotBlank() }) {
                val snap = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
                    .collection("inventario").document(productoId).get().await()
                @Suppress("UNCHECKED_CAST")
                val lotes = snap.get("lotes") as? Map<String, Any> ?: continue
                resultado[productoId] = lotes
            }
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo lotes para anulación: ${e.message}", e)
            // R9: jamás convertir un error de lectura en "no existe". La pantalla debe
            // decir la verdad: no se pudo leer el estante, no que el producto falta.
            null
        }
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
        usuarioNombre: String = "",
        respuestaPlata: String = "",
        metodoDevolucion: String = "",
        referenciaDevolucion: String = "",
        farmaciaIdParam: String? = null,
        sucursalIdParam: String? = null
    ): Result<Unit> {
        if (motivo.trim().isBlank()) return Result.failure(IllegalArgumentException("El motivo de anulación es obligatorio."))
        val ids = if (farmaciaIdParam != null && sucursalIdParam != null) {
            Pair(farmaciaIdParam, sucursalIdParam)
        } else {
            obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        }
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
                val numeroFactura = facturaSnap.getString("numeroFactura") ?: facturaId
                val pedidoId = facturaSnap.getString("pedidoId") ?: ""
                val abonos = facturaSnap.get("abonos") as? List<*> ?: emptyList<Any>()
                val montoPagado = facturaSnap.getDouble("montoPagado") ?: 0.0
                val plataPagada = if (abonos.isNotEmpty()) {
                    // Misma regla que el modelo (totalAbonadoReal): solo abonos VIGENTES.
                    abonos.filterNot { (it as? Map<*, *>)?.get("anulado") == true }
                        .sumOf { (it as? Map<*, *>)?.let { m -> (m["monto"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0 }
                } else montoPagado
                // ══ FASE DE LECTURAS (Firestore: TODAS las lecturas antes de cualquier
                // escritura — si no, la transacción aborta y la anulación sería imposible) ══
                var proveedorIdSaldo: String? = null
                var saldoSnapAnulacion: com.google.firebase.firestore.DocumentSnapshot? = null
                if (plataPagada > 0.01) {
                    val respuesta = respuestaPlata.trim().uppercase()
                    when (respuesta) {
                        "SALDO_A_FAVOR" -> {
                            val proveedorId = facturaSnap.getString("proveedorId") ?: ""
                            if (proveedorId.isBlank()) {
                                throw IllegalStateException("La factura no tiene proveedor vinculado; no se puede registrar el saldo a favor.")
                            }
                            proveedorIdSaldo = proveedorId
                            saldoSnapAnulacion = tx.get(SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorId))
                            // La ficha del proveedor (donde vive el saldo a favor) puede haber sido
                            // eliminada mientras la factura seguía viva. Un tx.update sobre esa ficha
                            // fallaría con un error técnico y encallaría la anulación entera sin
                            // explicar nada. Aquí se corta con la verdad humana y las opciones reales.
                            if (saldoSnapAnulacion == null || !saldoSnapAnulacion.exists()) {
                                val nombreProv = facturaSnap.getString("proveedorNombre")?.takeIf { it.isNotBlank() }?.trim() ?: proveedorId
                                throw IllegalStateException(
                                    "El proveedor '$nombreProv' ya no existe (fue eliminado), así que no se puede " +
                                        "registrar el saldo a favor sin su ficha. Elige 'devolución recibida' del dinero o " +
                                        "declara la pérdida; o registra primero al proveedor de nuevo y reintenta la anulación."
                                )
                            }
                        }
                        "DEVOLUCION_RECIBIDA" -> {
                            if (metodoDevolucion.trim().isBlank()) {
                                throw IllegalStateException("Indica en qué medio te devolvieron el dinero (efectivo, transferencia, cheque u otro).")
                            }
                        }
                        "PERDIDA" -> { /* Pérdida declarada: queda registrada en la factura con quién, cuándo y por qué. */ }
                        else -> throw IllegalStateException("Indica qué pasa con el dinero ya pagado: saldo a favor, devolución recibida o pérdida.")
                    }
                }
                @Suppress("UNCHECKED_CAST")
                val itemsRaw = facturaSnap.get("items") as? List<Map<String, Any>> ?: emptyList()
                if (itemsRaw.isEmpty() && conDevolucion) throw IllegalStateException("La factura no tiene productos para devolver.")

                // Foto fresca de CADA producto a devolver (se lee TODO antes de escribir).
                val productoSnapsDev = HashMap<String, com.google.firebase.firestore.DocumentSnapshot>()
                if (conDevolucion) {
                    for (itemMap in itemsRaw) {
                        val productoId = itemMap["productoId"] as? String ?: ""
                        if (productoId.isBlank()) continue
                        val cantidadTotal = (itemMap["cantidadTotal"] as? Number)?.toDouble() ?: 0.0
                        if (cantidadTotal <= 0) continue
                        if (productoSnapsDev.containsKey(productoId)) continue
                        val snapProd = tx.get(tiendaRef.collection("inventario").document(productoId))
                        if (!snapProd.exists()) throw IllegalStateException("El producto $productoId de la factura ya no existe en inventario.")
                        productoSnapsDev[productoId] = snapProd
                    }
                }
                // El pedido asociado también se lee en la zona de lecturas.
                val pedidoRefAnulacion = if (pedidoId.isNotBlank()) tiendaRef.collection("pedidos_compra").document(pedidoId) else null
                val pedidoSnapAnulacion = pedidoRefAnulacion?.let { tx.get(it) }

                // ══ FASE DE ESCRITURAS ══
                if (proveedorIdSaldo != null) {
                    SaldoAFavorFirestore.registrarIngresoEnTransaccion(
                        tx = tx,
                        refSaldo = SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorIdSaldo!!),
                        monto = plataPagada,
                        tipo = "SALDO_A_FAVOR_ANULACION",
                        documento = numeroFactura,
                        motivo = "Saldo a favor por anulación de la factura $numeroFactura",
                        usuarioNombre = usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                        usuarioEmail = usuarioEmail,
                        ahoraMs = ahoraMs,
                        fechaLegible = fechaLegible,
                        saldoSnapshot = saldoSnapAnulacion
                    )
                }

                // Si con devolución, validar y descontar stock por cada item.
                // Misma regla anti-pérdida que en la recepción: la factura puede traer el
                // MISMO producto en 2+ lotes; cada fila parte del mapa ya mutado.
                val lotesAcumuladosAnulacion = HashMap<String, MutableMap<Any?, Any?>>()
                if (conDevolucion) {
                    for (itemMap in itemsRaw) {
                        val productoId = itemMap["productoId"] as? String ?: ""
                        if (productoId.isBlank()) continue
                        val loteNumero = itemMap["loteNumero"] as? String ?: ""
                        val cantidadTotal = (itemMap["cantidadTotal"] as? Number)?.toDouble() ?: 0.0
                        if (cantidadTotal <= 0) continue
                        val productRef = tiendaRef.collection("inventario").document(productoId)
                        val productSnap = productoSnapsDev[productoId]
                            ?: throw IllegalStateException("No se pudo leer el producto $productoId dentro de la anulación.")
                        val currentLotes = lotesAcumuladosAnulacion.getOrPut(productoId) {
                            @Suppress("UNCHECKED_CAST")
                            ((productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>())
                        }
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
                            loteNuevo["ultimaEntrada"] = Timestamp(Date(ahoraMs))
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
                // La foto del pedido se tomó en la fase de lecturas (jamás se lee tras escribir).
                if (pedidoRefAnulacion != null && pedidoSnapAnulacion != null) {
                    val pedidoRef = pedidoRefAnulacion
                    val pedidoSnap = pedidoSnapAnulacion
                    if (pedidoSnap.exists()) {
                        val montoFactRaw = facturaSnap.getDouble("montoTotal") ?: facturaSnap.getDouble("montoAcumulado") ?: 0.0
                        val totalFact = if (montoFactRaw > 0) montoFactRaw else itemsRaw.sumOf { (it["costoTotal"] as? Number)?.toDouble() ?: (it["montoTotal"] as? Number)?.toDouble() ?: 0.0 }
                        // Monto siempre se revierte (deuda anulada) aunque no devuelvas stock
                        val montoPrevio = pedidoSnap.getDouble("montoFacturadoReal") ?: 0.0
                        val nuevoMonto = redondear2((montoPrevio - totalFact).coerceAtLeast(0.0))
                        if (conDevolucion) {
                            @Suppress("UNCHECKED_CAST")
                            val itemsPed = (pedidoSnap.get("items") as? List<Map<String, Any>>) ?: emptyList()
                            val recibidoPorProducto = itemsRaw.groupBy { it["productoId"] as? String ?: "" }
                                .mapValues { (_, filas) ->
                                    filas.sumOf { (it["cantidadTotal"] as? Number)?.toDouble() ?: 0.0 }.toInt()
                                }
                            val updatedItems = itemsPed.map { raw ->
                                val pid = raw["productoId"] as? String ?: ""
                                val cantFact = recibidoPorProducto[pid] ?: 0
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
                                "usuarioNombre" to usuarioNombre.ifBlank { SessionManager.nombreUsuario },
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
                                "usuarioNombre" to usuarioNombre.ifBlank { SessionManager.nombreUsuario },
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
                val facturaAnulada = mutableMapOf<String, Any>(
                    "estadoPago" to "ANULADA",
                    "motivoAnulacion" to motivo.trim(),
                    "anuladoPorEmail" to usuarioEmail,
                    "anuladoPorNombre" to usuarioNombre,
                    "anuladoEl" to FieldValue.serverTimestamp(),
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                if (plataPagada > 0.01) {
                    facturaAnulada["anulacionPlata"] = mapOf(
                        "decision" to respuestaPlata.trim().uppercase(),
                        "monto" to plataPagada,
                        "metodoDevolucion" to metodoDevolucion.trim(),
                        "referenciaDevolucion" to referenciaDevolucion.trim(),
                        "fechaLegible" to fechaLegible,
                        "fechaMs" to ahoraMs,
                        "usuarioNombre" to usuarioNombre.ifBlank { SessionManager.nombreUsuario },
                        "usuarioEmail" to usuarioEmail
                    )
                }
                tx.update(facturaRef, facturaAnulada)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error anulando factura $facturaId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Redondeo a centavos exactos: los montos guardados jamás arrastran ruido flotante. */
    private fun redondear2(valor: Double): Double = Math.round(valor * 100.0) / 100.0
}
