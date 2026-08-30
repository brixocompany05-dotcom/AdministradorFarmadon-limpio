package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.ExpedienteReclamoProveedor
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.app.administradorfarmadon.inventario.compartido.logica.CostoRealLote
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repositorio de Detalles de Producto en Cloud Firestore (Multi-Tenant).
 * Lee en tiempo real el producto, sus lotes activos y la bitácora de movimientos (Kardex),
 * todo aislado por sede: farmacias/{farmaciaId}/sucursales/{sucursalId}/...
 */

/**
 * Operaciones de lotes —” bloqueo, devolución, canje, anulación y merma. Transacciones atómicas todo-o-nada.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) —” responsabilidad única.
 */
class LotesDevolucionCanjeRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object { private const val TAG = "LotesDevolucionCanjeRepository" }

    suspend fun registrarDevolucionProveedor(
        clienteId: String,
        productId: String,
        lote: LoteProducto,
        cantidadDevuelta: Double,
        guiaRetiro: String,
        notaCredito: String,
        motivo: String,
        modalidadCompensacion: String,
        usuarioEmail: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank() || cantidadDevuelta <= 0) {
            return Result.failure(Exception("Datos inválidos para registrar devolución a droguería."))
        }
        if (motivo.trim().isBlank()) return Result.failure(Exception("El motivo de devolución es obligatorio."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val idemFinal = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }
            val ahoraMs = HoraServidor.ahoraMs()
            val fechaLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ahoraMs))

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document("idem_$idemFinal")
            val reclamoRef = tiendaRef.collection("reclamos_proveedores").document(UUID.randomUUID().toString())
            val cleanKey = FechaVencimientoHelper.llaveLote(lote.numero)

            val cleanFacturaId = if (lote.nroFactura.isNotBlank()) {
                lote.nroFactura.trim().uppercase().replace("/", "-").replace(" ", "_")
            } else null
            val facturaRef = cleanFacturaId?.let { tiendaRef.collection("compras_facturas").document(it) }

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")
                if (tx.get(movimientoRef).exists()) return@runTransaction

                val lotesMap = (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val res2 = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero) ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyReal, loteData) = res2
                val cleanKey = cleanKeyReal
                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalLote = cantDisponible + cantBloqueada

                if (cantidadDevuelta > totalLote) {
                    throw Exception("Este lote se acaba de actualizar. Intentaste devolver $cantidadDevuelta pero ahora solo hay $totalLote en total. Actualicé el saldo — revisa e intenta con $totalLote o menos.")
                }

                // 1. Costo unitario oficial: la MISMA fuente que ve la pantalla (CostoRealLote).
                //    El monto se redondea a 2 decimales: lo que se mostró es lo que se guarda.
                val costoUnitarioLote = CostoRealLote.costoUnitario(loteData)
                val montoTotalReclamo = CostoRealLote.monto(cantidadDevuelta, costoUnitarioLote)

                // R3: devolución solo sobre disponible; cuarentena requiere desbloqueo previo
                if (cantidadDevuelta > cantDisponible) {
                    throw Exception("Este lote se acaba de actualizar. Intentaste devolver $cantidadDevuelta pero ahora solo hay $cantDisponible disponibles ($cantBloqueada en cuarentena). Actualicé el saldo — si quieres devolver lo de cuarentena, desbloquea primero o ajusta a $cantDisponible.")
                }
                loteData["cantidad"] = (cantDisponible - cantidadDevuelta).coerceAtLeast(0.0)
                loteData["ultimaDevolucion"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyReal] = loteData

                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(lotesMap)

                val updatesProducto = mutableMapOf<String, Any>(
                    "lotes" to lotesMap,
                    "stock" to nuevoStockDisponible,
                    "stockTotal" to nuevoStockTotal,
                    "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                // Si el lote principal de consumo queda en 0, se limpia solo.
                if (((loteData["cantidad"] as? Number)?.toDouble() ?: 0.0) <= 0.0) {
                    val principalActual = snap.getString("lotePrioritarioId") ?: ""
                    if (principalActual.isNotBlank() &&
                        (principalActual.equals(cleanKeyReal, true) || principalActual.equals(lote.numero, true))
                    ) {
                        updatesProducto["lotePrioritarioId"] = ""
                        updatesProducto["lotePrioritarioPor"] = ""
                        updatesProducto["lotePrioritarioPorRol"] = ""
                    }
                }
                tx.update(
                    productRef,
                    updatesProducto
                )

                // 3. Asiento inmutable en Kardex
                val movData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "SALIDA_DEVOLUCION_PROVEEDOR",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to lote.numero,
                    "cantidad" to -cantidadDevuelta,
                    "costoTotal" to montoTotalReclamo,
                    "costoUnitario" to costoUnitarioLote,
                    "proveedorNombre" to lote.proveedorNombre,
                    "facturaNumero" to lote.nroFactura,
                    "guiaRetiro" to guiaRetiro.trim().uppercase(),
                    "notaCredito" to notaCredito.trim().uppercase(),
                    "motivo" to motivo,
                    "modalidadCompensacion" to modalidadCompensacion,
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movData)

                // 4. Creación del Expediente Oficial de Reclamo
                val reclamoData = mapOf(
                    "id" to reclamoRef.id,
                    "clienteId" to clienteId,
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "empaque" to (snap.getString("empaque") ?: "Caja"),
                    "loteNumero" to lote.numero,
                    "cantidadDevuelta" to cantidadDevuelta,
                    "costoUnitario" to costoUnitarioLote,
                    "montoTotal" to montoTotalReclamo,
                    "proveedorId" to lote.proveedorId,
                    "proveedorNombre" to lote.proveedorNombre,
                    "facturaOrigen" to lote.nroFactura,
                    "guiaRetiro" to guiaRetiro.trim().uppercase(),
                    "notaCredito" to notaCredito.trim().uppercase(),
                    "motivo" to motivo,
                    "modalidadCompensacion" to modalidadCompensacion,
                    "estado" to "EN_REVISION_DROGUERIA",
                    "usuarioRegistroEmail" to usuarioEmail,
                    "creadoEl" to FieldValue.serverTimestamp()
                )
                tx.set(reclamoRef, reclamoData)

                // 5. La nota de crédito es información CONTABLE: reduce la deuda real de la factura
                //    (ajustesFactura, que Cuentas por Pagar lee) y suma el "saldo a favor" del proveedor
                //    (plata que la droguería nos debe). Todo en la misma transacción, jamás en silencio.
                if (facturaRef != null) {
                    val snapFactura = tx.get(facturaRef)
                    if (snapFactura.exists()) {
                        val estadoFacturaOrigen = snapFactura.getString("estadoPago") ?: ""
                        if (estadoFacturaOrigen.equals("ANULADA", ignoreCase = true)) {
                            throw IllegalStateException("La factura de origen está ANULADA; no se puede registrar una nota de crédito sobre ella. Resuelve primero el estado de la factura.")
                        }
                        @Suppress("UNCHECKED_CAST")
                        val ajustesExistentes = (snapFactura.get("ajustesFactura") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                        val totalAjustesActual = ajustesExistentes.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                        val itemsRaw = snapFactura.get("items") as? List<*>
                        val totalItems = itemsRaw?.mapNotNull { (it as? Map<*, *>)?.let { m -> (m["costoTotal"] as? Number)?.toDouble() ?: 0.0 } }?.sum() ?: 0.0
                        val totalPapel = (snapFactura.getDouble("montoTotal") ?: snapFactura.getDouble("montoAcumulado")) ?: totalItems
                        val maximoAjustable = (totalPapel - totalAjustesActual).coerceAtLeast(0.0)
                        if (montoTotalReclamo > maximoAjustable + 0.01) {
                            throw IllegalStateException(
                                "La nota de crédito de S/ ${String.format(Locale.US, "%.2f", montoTotalReclamo)} supera el saldo ajustable de la factura (S/ ${String.format(Locale.US, "%.2f", maximoAjustable)}). Revisa el costo del lote."
                            )
                        }
                        ajustesExistentes.add(
                            mapOf(
                                "id" to reclamoRef.id,
                                "tipo" to "NOTA_CREDITO",
                                "numeroDocumento" to notaCredito.trim().uppercase(),
                                "monto" to montoTotalReclamo,
                                "motivo" to motivo,
                                "fechaLegible" to fechaLegible,
                                "fechaMs" to ahoraMs,
                                "usuarioNombre" to "Administración",
                                "usuarioEmail" to usuarioEmail
                            )
                        )
                        val nuevoTotalEfectivo = (totalPapel - totalAjustesActual - montoTotalReclamo).coerceAtLeast(0.0)
                        @Suppress("UNCHECKED_CAST")
                        val totalAbonado = (snapFactura.get("abonos") as? List<Map<String, Any>>)?.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                            ?: (snapFactura.getDouble("montoPagado") ?: 0.0)
                        val saldoRestante = (nuevoTotalEfectivo - totalAbonado).coerceAtLeast(0.0)
                        val nuevoEstadoPago = when {
                            saldoRestante <= 0.01 -> "PAGADA"
                            totalAbonado > 0.01 -> "ABONADO_PARCIAL"
                            else -> "PENDIENTE"
                        }
                        tx.update(
                            facturaRef,
                            mapOf(
                                "ajustesFactura" to ajustesExistentes,
                                "estadoPago" to nuevoEstadoPago,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            )
                        )

                        // Proveedor: SOLO si ya pagamos más de lo que la factura ahora vale,
                        // esa diferencia es plata que la droguería nos debe (saldo a favor).
                        // Si la factura aún no estaba pagada, la nota solo reduce lo que debemos:
                        // no se inventa un saldo a favor.
                        val excesoPagado = (totalAbonado - nuevoTotalEfectivo).coerceAtLeast(0.0)
                        if (lote.proveedorId.isNotBlank() && excesoPagado > 0.0) {
                            val provRef = tiendaRef.collection("proveedores").document(lote.proveedorId)
                            val provSnap = tx.get(provRef)
                            val entradaSaldo = mapOf(
                                "id" to reclamoRef.id,
                                "tipo" to "SALDO_A_FAVOR_NOTA_CREDITO",
                                "monto" to excesoPagado,
                                "facturaId" to facturaRef.id,
                                "facturaNumero" to lote.nroFactura,
                                "motivo" to motivo,
                                "fechaLegible" to fechaLegible,
                                "fechaMs" to ahoraMs,
                                "usuarioNombre" to "Administración",
                                "usuarioEmail" to usuarioEmail
                            )
                            if (provSnap.exists()) {
                                val saldoActual = provSnap.getDouble("saldoAFavor") ?: 0.0
                                @Suppress("UNCHECKED_CAST")
                                val historial = (provSnap.get("historialSaldoAFavor") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                                historial.add(entradaSaldo)
                                tx.update(
                                    provRef,
                                    mapOf(
                                        "saldoAFavor" to saldoActual + excesoPagado,
                                        "historialSaldoAFavor" to historial,
                                        "actualizadoEl" to FieldValue.serverTimestamp()
                                    )
                                )
                            } else {
                                tx.set(
                                    provRef,
                                    mapOf(
                                        "id" to lote.proveedorId,
                                        "nombre" to (snapFactura.getString("proveedorNombre") ?: lote.proveedorNombre),
                                        "saldoAFavor" to excesoPagado,
                                        "historialSaldoAFavor" to listOf<Map<String, Any>>(entradaSaldo),
                                        "actualizadoEl" to FieldValue.serverTimestamp()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                            }
                        }
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando devolución a proveedor: ${e.message}", e)
            Result.failure(e)
        }
    }



    suspend fun registrarCanjeProducto(
        clienteId: String,
        productId: String,
        loteOrigen: LoteProducto,
        cantidadCanjeada: Double,
        nuevoLoteNumero: String,
        nuevoVencimiento: String,
        guiaCanje: String,
        motivo: String,
        usuarioEmail: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        if (clienteId.isBlank() || productId.isBlank() || loteOrigen.numero.isBlank() || cantidadCanjeada <= 0 || nuevoLoteNumero.isBlank() || nuevoVencimiento.isBlank()) {
            return Result.failure(Exception("Datos insuficientes para procesar el canje de producto."))
        }
        if (motivo.trim().isBlank()) return Result.failure(Exception("El motivo de canje es obligatorio."))
        val diasNuevo = FechaVencimientoHelper.diasHastaVencer(nuevoVencimiento.trim())
        if (diasNuevo != null && diasNuevo <= 0) return Result.failure(Exception("El vencimiento nuevo $nuevoVencimiento está vencido o es hoy."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val idemFinal = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoSalidaRef = tiendaRef.collection("movimientos").document("idem_$idemFinal")
            val movimientoEntradaRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val canjeRef = tiendaRef.collection("canjes_proveedores").document(UUID.randomUUID().toString())

            val cleanKeyOrigen = FechaVencimientoHelper.llaveLote(loteOrigen.numero)
            val cleanKeyNuevo = FechaVencimientoHelper.llaveLote(nuevoLoteNumero)

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe en inventario.")
                if (tx.get(movimientoSalidaRef).exists()) return@runTransaction

                val lotesMap = (snap.get("lotes") as? Map<String, Any?>)?.toMutableMap() as MutableMap<Any?, Any?>
                val res3 = FechaVencimientoHelper.resolverLote(lotesMap, loteOrigen.numero) ?: throw Exception("El lote origen no se encuentra en el inventario.")
                val (cleanKeyOrigenReal, loteDataOrigen) = res3
                val cleanKeyOrigen = cleanKeyOrigenReal
                val cantDisponible = (loteDataOrigen["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteDataOrigen["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalOrigen = cantDisponible + cantBloqueada

                if (cantidadCanjeada > totalOrigen) {
                    throw Exception("La cantidad a canjear ($cantidadCanjeada) supera el saldo total del lote origen ($totalOrigen).")
                }

                // El canje opera sobre el SALDO TOTAL del lote (disponible + cuarentena).
                // Un medicamento sospechoso jamás debe volver a estar "disponible" para poder canjearlo.
                val totalOperable = cantDisponible + cantBloqueada
                if (cantidadCanjeada > totalOperable) {
                    throw Exception("La cantidad a canjear ($cantidadCanjeada) supera el saldo total del lote ($totalOperable).")
                }

                val desdeDisponible = minOf(cantidadCanjeada, cantDisponible)
                val desdeBloqueada = cantidadCanjeada - desdeDisponible
                loteDataOrigen["cantidad"] = (cantDisponible - desdeDisponible).coerceAtLeast(0.0)
                loteDataOrigen["cantidadBloqueada"] = (cantBloqueada - desdeBloqueada).coerceAtLeast(0.0)
                loteDataOrigen["ultimoCanje"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyOrigenReal] = loteDataOrigen

                // 2. Acreditar en el lote nuevo
                val costoUnitarioHistorico = CostoRealLote.costoUnitario(loteDataOrigen)

                val loteDataNuevo = (lotesMap[cleanKeyNuevo] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf<String, Any?>()
                val cantNuevoActual = (loteDataNuevo["cantidad"] as? Number)?.toDouble() ?: 0.0
                val vtoExistenteNuevo = (loteDataNuevo["vencimiento"] as? String)?.takeIf { it.isNotBlank() }?.let { FechaVencimientoHelper.normalizar(it) }
                val vtoNuevoNorm = FechaVencimientoHelper.normalizar(nuevoVencimiento.trim()) ?: nuevoVencimiento.trim()
                if (vtoExistenteNuevo != null && vtoExistenteNuevo.isNotBlank() && vtoNuevoNorm.isNotBlank() && vtoExistenteNuevo != vtoNuevoNorm) {
                    throw IllegalArgumentException("El lote $nuevoLoteNumero ya vive con vencimiento $vtoExistenteNuevo. Ingresaste $vtoNuevoNorm —” si es el mismo lote, usa $vtoExistenteNuevo.")
                }
                loteDataNuevo["numero"] = nuevoLoteNumero.trim().uppercase()
                loteDataNuevo["loteId"] = FechaVencimientoHelper.llaveLote(nuevoLoteNumero)
                loteDataNuevo["vencimiento"] = vtoExistenteNuevo ?: vtoNuevoNorm
                loteDataNuevo["cantidad"] = (cantNuevoActual + cantidadCanjeada).coerceAtLeast(0.0)
                loteDataNuevo["costoCompraUnitario"] = costoUnitarioHistorico
                loteDataNuevo["costoUnitario"] = costoUnitarioHistorico
                loteDataNuevo["proveedor"] = loteOrigen.proveedorNombre
                loteDataNuevo["proveedorId"] = loteOrigen.proveedorId
                loteDataNuevo["factura"] = loteOrigen.nroFactura
                // Compatibilidad: también deja llaves lectoras antiguas por si cliente viejo lee nroFactura
                loteDataNuevo["proveedorNombre"] = loteOrigen.proveedorNombre
                loteDataNuevo["nroFactura"] = loteOrigen.nroFactura
                loteDataNuevo["esCanje"] = true
                loteDataNuevo["actualizadoEl"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyNuevo] = loteDataNuevo

                // Si el lote origen quedó en 0 (y la reposición NO es el mismo lote),
                // desaparece del stock activo; su historial queda en el kardex.
                val cantidadOrigenFinal = (loteDataOrigen["cantidad"] as? Number)?.toDouble() ?: 0.0
                val bloqueadaOrigenFinal = (loteDataOrigen["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val esMismoLote = cleanKeyNuevo.equals(cleanKeyOrigen, true)
                if (!esMismoLote && cantidadOrigenFinal <= 0.0 && bloqueadaOrigenFinal <= 0.0) {
                    lotesMap.remove(cleanKeyOrigen)
                }

                // 3. Sincronizar stock disponible, stock total y vencimiento FEFO tras el canje
                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(lotesMap)

                val updatesProducto = mutableMapOf<String, Any>(
                    "lotes" to lotesMap,
                    "stock" to nuevoStockDisponible,
                    "stockTotal" to nuevoStockTotal,
                    "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                // Si el lote origen (principal de consumo) queda en 0, se limpia solo.
                if (((loteDataOrigen["cantidad"] as? Number)?.toDouble() ?: 0.0) <= 0.0) {
                    val principalActual = snap.getString("lotePrioritarioId") ?: ""
                    if (principalActual.isNotBlank() &&
                        (principalActual.equals(cleanKeyOrigen, true) || principalActual.equals(loteOrigen.numero, true))
                    ) {
                        updatesProducto["lotePrioritarioId"] = ""
                        updatesProducto["lotePrioritarioPor"] = ""
                        updatesProducto["lotePrioritarioPorRol"] = ""
                    }
                }
                tx.update(
                    productRef,
                    updatesProducto
                )

                // 4. Asiento de SALIDA de mercadería dañada
                val movSalida = mapOf(
                    "id" to movimientoSalidaRef.id,
                    "tipo" to "SALIDA_CANJE_PROVEEDOR",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to loteOrigen.numero,
                    "cantidad" to -cantidadCanjeada,
                    "costoTotal" to (cantidadCanjeada * costoUnitarioHistorico),
                    "proveedorNombre" to loteOrigen.proveedorNombre,
                    "facturaNumero" to loteOrigen.nroFactura,
                    "guiaCanje" to guiaCanje.trim().uppercase(),
                    "motivo" to motivo,
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoSalidaRef, movSalida)

                // 5. Asiento de ENTRADA de mercadería sana —” costo heredado para no perder valorizado
                val movEntrada = mapOf(
                    "id" to movimientoEntradaRef.id,
                    "tipo" to "ENTRADA_CANJE_PROVEEDOR",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to nuevoLoteNumero.trim().uppercase(),
                    "loteVencimiento" to nuevoVencimiento.trim(),
                    "cantidad" to cantidadCanjeada,
                    "costoTotal" to (cantidadCanjeada * costoUnitarioHistorico),
                    "costoUnitario" to costoUnitarioHistorico,
                    "proveedorNombre" to loteOrigen.proveedorNombre,
                    "guiaCanje" to guiaCanje.trim().uppercase(),
                    "motivo" to "Reposición física por canje ($motivo)",
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoEntradaRef, movEntrada)

                // 6. Expediente de Canje
                val canjeData = mapOf(
                    "id" to canjeRef.id,
                    "clienteId" to clienteId,
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteOrigen" to loteOrigen.numero,
                    "loteNuevo" to nuevoLoteNumero.trim().uppercase(),
                    "vencimientoNuevo" to nuevoVencimiento.trim(),
                    "cantidad" to cantidadCanjeada,
                    "proveedorNombre" to loteOrigen.proveedorNombre,
                    "guiaCanje" to guiaCanje.trim().uppercase(),
                    "motivo" to motivo,
                    "estado" to "CANJE_COMPLETADO_MANO_A_MANO",
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(canjeRef, canjeData)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando canje directo: ${e.message}", e)
            Result.failure(e)
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
}
