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
class LotesBloqueoRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "LotesBloqueoRepository"
    }

    suspend fun cambiarBloqueoLote(
        clienteId: String,
        productId: String,
        lote: LoteProducto,
        ponerEnCuarentena: Boolean,
        cantidadAfectada: Double,
        motivo: String,
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(
            SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia.")
        )
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank()) {
            return Result.failure(Exception("Parámetros insuficientes."))
        }
        if (motivo.trim()
                .isBlank()
        ) return Result.failure(Exception("El motivo de cuarentena es obligatorio para auditoría."))
        if (cantidadAfectada < 0) return Result.failure(Exception("La cantidad no puede ser negativa."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef =
                tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")

                val lotesMap =
                    (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val res = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero)
                    ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyReal, loteData) = res
                val cleanKey = cleanKeyReal

                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0

                val cantOperar =
                    if (cantidadAfectada > 0) cantidadAfectada else if (ponerEnCuarentena) cantDisponible else cantBloqueada

                if (ponerEnCuarentena) {
                    if (cantOperar > cantDisponible) {
                        throw Exception("La cantidad a poner en cuarentena ($cantOperar) supera el stock disponible ($cantDisponible).")
                    }
                    val nuevaDisp = (cantDisponible - cantOperar).coerceAtLeast(0.0)
                    val nuevaBloq = (cantBloqueada + cantOperar).coerceAtLeast(0.0)
                    loteData["cantidad"] = nuevaDisp
                    loteData["cantidadBloqueada"] = nuevaBloq
                    loteData["estadoSanitario"] =
                        if (nuevaDisp == 0.0) "CUARENTENA_TOTAL" else "CUARENTENA_PARCIAL"
                } else {
                    if (cantOperar > cantBloqueada) {
                        throw Exception("La cantidad a desbloquear ($cantOperar) supera las unidades en cuarentena ($cantBloqueada).")
                    }
                    val nuevaDisp = (cantDisponible + cantOperar).coerceAtLeast(0.0)
                    val nuevaBloq = (cantBloqueada - cantOperar).coerceAtLeast(0.0)
                    loteData["cantidad"] = nuevaDisp
                    loteData["cantidadBloqueada"] = nuevaBloq
                    loteData["estadoSanitario"] =
                        if (nuevaBloq == 0.0) "ACTIVO_DISPONIBLE" else "CUARENTENA_PARCIAL"
                }
                loteData["ultimaModificacionEstado"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyReal] = loteData

                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(
                    lotesMap
                )

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

                val movData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to if (ponerEnCuarentena) "BLOQUEO_CUARENTENA_LOTE" else "DESBLOQUEO_LOTE",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to lote.numero,
                    "cantidad" to if (ponerEnCuarentena) -cantOperar else cantOperar,
                    "motivo" to "$motivo (Saldo: ${loteData["cantidad"]} disp / ${loteData["cantidadBloqueada"]} bloq)",
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movData)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando estado de bloqueo de lote: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun anularIngresoLote(
        clienteId: String,
        productId: String,
        lote: LoteProducto,
        motivo: String,
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(
            SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia.")
        )
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank()) {
            return Result.failure(Exception("Parámetros insuficientes para anular lote."))
        }
        if (motivo.trim().length < 10) return Result.failure(Exception("Anulación requiere motivo de al menos 10 caracteres para auditoría."))

        // ── BLINDAJE SANITARIO ──
        // Si este lote ya tuvo ventas, NO se puede anular silenciosamente.
        // Anular borraria la trazabilidad de qué lote recibieron los pacientes.
        val movimientosLoteQuery =
            FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
                .collection("movimientos")
                .whereEqualTo("productoId", productId)
                .whereEqualTo("loteNumero", lote.numero.trim().uppercase())
                .get().await()
        val ventasPrevias = movimientosLoteQuery.documents.count { m ->
            val t = m.getString("tipo")?.uppercase() ?: ""
            t.contains("VENTA") || t.contains("DISPENSACION")
        }
        if (ventasPrevias > 0) {
            return Result.failure(
                Exception(
                    "No puedes anular el lote ${lote.numero}: tiene $ventasPrevias venta(s) previa(s). " +
                            "La trazabilidad sanitaria debe preservarse. Usa MERMA o DEVOLUCIÓN para las unidades restantes."
                )
            )
        }

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef =
                tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val cleanKey = FechaVencimientoHelper.llaveLote(lote.numero)

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")

                val currentStock = snap.getDouble("stock") ?: snap.getDouble("stockTotal") ?: 0.0
                val lotesMap =
                    (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val resAnular = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero)
                    ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyRealAnular, loteData) = resAnular
                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalLote = cantDisponible + cantBloqueada
                if (totalLote <= 0) throw Exception("El lote ${lote.numero} ya está en 0, no hay saldo que anular.")
                val cleanKey = cleanKeyRealAnular

                lotesMap.remove(cleanKeyRealAnular)
                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(
                    lotesMap
                )

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

                val facturaNumeroRaw = (loteData["factura"] as? String)?.trim() ?: ""
                val proveedorNombreRaw = (loteData["proveedor"] as? String)?.trim() ?: ""
                val esFacturaFormal =
                    facturaNumeroRaw.isNotBlank() && facturaNumeroRaw != "S/C (Sin Comprobante)" && proveedorNombreRaw.isNotBlank() && proveedorNombreRaw != "Almacén General"
                if (esFacturaFormal) {
                    val cleanNumero =
                        facturaNumeroRaw.uppercase().replace("/", "-").replace(" ", "_")
                    val provKey = proveedorNombreRaw.uppercase().replace("/", "-").replace(" ", "_")
                        .replace(".", "__DOT__")
                    val compositeId = "${provKey}__${cleanNumero}"
                    val facturaRef = tiendaRef.collection("compras_facturas").document(compositeId)
                    val legacyRef = tiendaRef.collection("compras_facturas").document(cleanNumero)
                    var facturaSnap = try {
                        tx.get(facturaRef)
                    } catch (e: Exception) {
                        android.util.Log.w("LotesOps", "get facturaRef falló", e); null
                    }
                    var effectiveRef = facturaRef
                    if (facturaSnap == null || !facturaSnap.exists()) {
                        val legacySnap = try {
                            tx.get(legacyRef)
                        } catch (e: Exception) {
                            android.util.Log.w("LotesOps", "get legacyRef falló", e); null
                        }
                        if (legacySnap != null && legacySnap.exists()) {
                            val legProv = legacySnap.getString("proveedorNombre") ?: ""
                            if (legProv.trim().equals(proveedorNombreRaw, ignoreCase = true)) {
                                facturaSnap = legacySnap
                                effectiveRef = legacyRef
                            }
                        }
                    }
                    if (facturaSnap != null && facturaSnap.exists()) {
                        val itemsRaw = facturaSnap.get("items") as? List<*>
                        val itemsMatch = itemsRaw?.filterIsInstance<Map<*, *>>()?.filter {
                            it["productoId"] == productId && (it["loteNumero"] as? String)?.trim()
                                ?.equals(lote.numero.trim(), ignoreCase = true) == true
                        } ?: emptyList()
                        val totalADescontar = if (itemsMatch.isNotEmpty()) itemsMatch.sumOf {
                            (it["costoTotal"] as? Number)?.toDouble() ?: 0.0
                        } else (loteData["costoCompra"] as? Number)?.toDouble() ?: 0.0
                        if (totalADescontar > 0) {
                            val nuevoAcumulado = ((facturaSnap.getDouble("montoAcumulado")
                                ?: 0.0) - totalADescontar).coerceAtLeast(0.0)
                            val itemsRestantes = itemsRaw?.filterNot { m ->
                                m is Map<*, *> && m["productoId"] == productId && (m["loteNumero"] as? String)?.trim()
                                    ?.equals(lote.numero.trim(), ignoreCase = true) == true
                            } ?: emptyList<Any>()
                            // Si la factura queda sin contenido tras la anulación, se marca ANULADA
                            // registrando QUIÉN la anuló, CUÁNDO y POR QUÉ — coherencia multiusuario.
                            val updatesFactura = mutableMapOf<String, Any>(
                                "montoAcumulado" to nuevoAcumulado,
                                "items" to itemsRestantes,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            )
                            if (itemsRestantes.isEmpty()) {
                                updatesFactura["estadoPago"] = "ANULADA"
                                updatesFactura["anuladaMotivo"] = motivo.trim()
                                updatesFactura["anuladaPor"] = usuarioEmail
                                updatesFactura["anuladaEn"] = FieldValue.serverTimestamp()
                            }
                            tx.update(effectiveRef, updatesFactura)
                        }
                    }
                }

                val movData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "ANULACION_ENTRADA",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to lote.numero,
                    "cantidad" to -totalLote,
                    "motivo" to motivo,
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movData)
                // Auditoría lote ordenada: auditorias/inventario/lotes/listaeliminado/{id} — solo valiosa
                val loteAuditRef =
                    tiendaRef.collection("auditorias").document("inventario").collection("lotes")
                        .document("listaeliminado").collection("items").document()
                tx.set(
                    loteAuditRef, hashMapOf(
                        "evento" to "ANULACION_LOTE",
                        "productoId" to productId,
                        "loteNumero" to lote.numero,
                        "cantidadAnulada" to totalLote,
                        "motivo" to motivo,
                        "eliminadoPorUid" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error anulando lote: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun registrarMerma(
        clienteId: String,
        productId: String,
        lote: LoteProducto,
        cantidadMerma: Double,
        motivo: String,
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(
            SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia.")
        )
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank() || cantidadMerma <= 0) {
            return Result.failure(Exception("Datos inválidos para registrar merma."))
        }
        if (motivo.trim()
                .isBlank()
        ) return Result.failure(Exception("El motivo de merma es obligatorio."))

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)

            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef =
                tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())
            val cleanKey = FechaVencimientoHelper.llaveLote(lote.numero)

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")

                val lotesMap =
                    (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val resMerma = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero)
                    ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyRealMerma, loteData) = resMerma
                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalLote = cantDisponible + cantBloqueada
                val cleanKey = cleanKeyRealMerma

                if (cantidadMerma > totalLote) {
                    throw Exception("La cantidad de merma ($cantidadMerma) supera el saldo total del lote ($totalLote).")
                }

                // La merma deduce primero de disponible; si no alcanza, continúa con cuarentena.
                // Un medicamento dañado/vencido en cuarentena va DIRECTO a su destino
                // sin volver a estar "disponible" para venta ni un segundo.
                val desdeDisponible = minOf(cantidadMerma, cantDisponible)
                val desdeBloqueada = cantidadMerma - desdeDisponible
                loteData["cantidad"] = (cantDisponible - desdeDisponible).coerceAtLeast(0.0)
                loteData["cantidadBloqueada"] = (cantBloqueada - desdeBloqueada).coerceAtLeast(0.0)

                loteData["ultimaMerma"] = FieldValue.serverTimestamp()
                lotesMap[cleanKeyRealMerma] = loteData

                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = calcularResumenStockYFefo(
                    lotesMap
                )

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

                val movData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "MERMA_DESCARTE",
                    "productoId" to productId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "loteNumero" to lote.numero,
                    "cantidad" to -cantidadMerma,
                    "motivo" to motivo,
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movData)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando merma: ${e.message}", e)
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
