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
 * Operaciones de lotes — bloqueo, devolución, canje, anulación y merma. Transacciones atómicas todo-o-nada.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) — responsabilidad única.
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
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank() || cantidadDevuelta <= 0) {
            return Result.failure(Exception("Datos inválidos para registrar devolución a droguería."))
        }
        if (motivo.trim().isBlank()) return Result.failure(Exception("El motivo de devolución es obligatorio."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val reclamoRef = tiendaRef.collection("reclamos_proveedores").document(UUID.randomUUID().toString())
            val cleanKey = FechaVencimientoHelper.llaveLote(lote.numero)

            val cleanFacturaId = if (lote.nroFactura.isNotBlank()) {
                lote.nroFactura.trim().uppercase().replace("/", "-").replace(" ", "_")
            } else null
            val facturaRef = cleanFacturaId?.let { tiendaRef.collection("compras_facturas").document(it) }

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")

                val currentStock = snap.getDouble("stock") ?: snap.getDouble("stockTotal") ?: 0.0
                val lotesMap = (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val res2 = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero) ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyReal, loteData) = res2
                val cleanKey = cleanKeyReal
                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalLote = cantDisponible + cantBloqueada

                if (cantidadDevuelta > totalLote) {
                    throw Exception("La cantidad a devolver ($cantidadDevuelta) supera el saldo total del lote ($totalLote).")
                }

                // 1. Detección automática del costo unitario oficial de compra
                val costoUnitarioLote = (loteData["costoUnitario"] as? Number)?.toDouble()
                    ?: (loteData["costoCompraUnitario"] as? Number)?.toDouble()
                    ?: (loteData["costoUltimoIngresoUnitario"] as? Number)?.toDouble()
                    ?: if (totalLote > 0) ((loteData["costoCompra"] as? Number)?.toDouble() ?: 0.0) / totalLote else 0.0
                val montoTotalReclamo = cantidadDevuelta * costoUnitarioLote

                // R3: devolución solo sobre disponible; cuarentena requiere desbloqueo previo
                if (cantidadDevuelta > cantDisponible) {
                    throw Exception("No puedes devolver $cantidadDevuelta unidades: solo hay $cantDisponible disponibles ($cantBloqueada en cuarentena, desbloquea primero)." )
                }
                loteData["cantidad"] = (cantDisponible - cantidadDevuelta).coerceAtLeast(0.0)
                loteData["ultimaDevolucion"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyReal] = loteData

                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(lotesMap)

                tx.update(
                    productRef,
                    mapOf(
                        "lotes" to lotesMap,
                        "stock" to nuevoStockDisponible,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
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

                // 5. Si la factura de origen está registrada en sistema, enlazar el débito
                if (facturaRef != null) {
                    val snapFactura = tx.get(facturaRef)
                    if (snapFactura.exists()) {
                        val notasCreditoExistentes = (snapFactura.get("notasCredito") as? List<*>)?.toMutableList() ?: mutableListOf<Any?>()
                        notasCreditoExistentes.add(
                            mapOf(
                                "reclamoId" to reclamoRef.id,
                                "productoId" to productId,
                                "lote" to lote.numero,
                                "monto" to montoTotalReclamo,
                                "notaCreditoNumero" to notaCredito.trim().uppercase(),
                                "fecha" to FieldValue.serverTimestamp()
                            )
                        )
                        tx.update(facturaRef, "notasCredito", notasCreditoExistentes)
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
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        if (clienteId.isBlank() || productId.isBlank() || loteOrigen.numero.isBlank() || cantidadCanjeada <= 0 || nuevoLoteNumero.isBlank() || nuevoVencimiento.isBlank()) {
            return Result.failure(Exception("Datos insuficientes para procesar el canje de producto."))
        }
        if (motivo.trim().isBlank()) return Result.failure(Exception("El motivo de canje es obligatorio."))
        if (nuevoLoteNumero.trim().equals(loteOrigen.numero.trim(), ignoreCase = true)) {
            return Result.failure(Exception("El lote de reposición no puede ser el mismo que el lote origen (${loteOrigen.numero}). Usa un lote nuevo."))
        }
        val diasNuevo = FechaVencimientoHelper.diasHastaVencer(nuevoVencimiento.trim())
        if (diasNuevo != null && diasNuevo <= 0) return Result.failure(Exception("El vencimiento nuevo $nuevoVencimiento está vencido o es hoy."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoSalidaRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val movimientoEntradaRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val canjeRef = tiendaRef.collection("canjes_proveedores").document(UUID.randomUUID().toString())

            val cleanKeyOrigen = FechaVencimientoHelper.llaveLote(loteOrigen.numero)
            val cleanKeyNuevo = FechaVencimientoHelper.llaveLote(nuevoLoteNumero)

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe en inventario.")

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
                val costoUnitarioHistorico = (loteDataOrigen["costoUnitario"] as? Number)?.toDouble()
                    ?: (loteDataOrigen["costoCompraUnitario"] as? Number)?.toDouble()
                    ?: 0.0

                val loteDataNuevo = (lotesMap[cleanKeyNuevo] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf<String, Any?>()
                val cantNuevoActual = (loteDataNuevo["cantidad"] as? Number)?.toDouble() ?: 0.0
                val vtoExistenteNuevo = (loteDataNuevo["vencimiento"] as? String)?.takeIf { it.isNotBlank() }?.let { FechaVencimientoHelper.normalizar(it) }
                val vtoNuevoNorm = FechaVencimientoHelper.normalizar(nuevoVencimiento.trim()) ?: nuevoVencimiento.trim()
                if (vtoExistenteNuevo != null && vtoExistenteNuevo.isNotBlank() && vtoNuevoNorm.isNotBlank() && vtoExistenteNuevo != vtoNuevoNorm) {
                    throw IllegalArgumentException("El lote $nuevoLoteNumero ya existe con vencimiento $vtoExistenteNuevo. Ingresaste $vtoNuevoNorm.")
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

                // 3. Sincronizar stock disponible, stock total y vencimiento FEFO tras el canje
                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(lotesMap)

                tx.update(
                    productRef,
                    mapOf(
                        "lotes" to lotesMap,
                        "stock" to nuevoStockDisponible,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
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

                // 5. Asiento de ENTRADA de mercadería sana — costo heredado para no perder valorizado
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
