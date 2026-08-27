package com.app.administradorfarmadon.inventario.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.ItemFacturaCompra
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio Oficial de Facturas de Compra (Cuentas por Pagar Multi-Tenant).
 * Colección: farmaciapp/app/farmacias/{farmaciaId}/sucursales/{sucursalId}/compras_facturas
 */
class FacturaCompraRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FacturaCompraRepository"
    }

    private fun getClienteId(): String {
        return SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    suspend fun buscarFacturaPorNumero(numeroFactura: String): FacturaCompra? {
        val clienteId = getClienteId()
        val numLimpio = numeroFactura.trim().uppercase()
        if (clienteId.isBlank() || numLimpio.isBlank()) return null

        return try {
            val col = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva)

            val q = col.whereEqualTo("numeroFactura", numLimpio).limit(1).get().await()
            if (!q.isEmpty) {
                mapearFactura(q.documents[0])
            } else {
                val docIdLegado = numLimpio.replace("/", "-").replace(" ", "_")
                val snap = col.document(docIdLegado).get().await()
                if (snap.exists()) mapearFactura(snap) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando factura $numLimpio: ${e.message}", e)
            null
        }
    }

    suspend fun buscarFacturaPorNumeroYProveedor(numeroFactura: String, proveedorId: String): FacturaCompra? {
        val clienteId = getClienteId()
        val numLimpio = numeroFactura.trim().uppercase()
        if (clienteId.isBlank() || numLimpio.isBlank()) return null
        return try {
            val col = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva)
            if (proveedorId.isNotBlank()) {
                val cleanNumero = numLimpio.replace("/", "-").replace(" ", "_")
                val compositeId = "${proveedorId}__${cleanNumero}"
                val snap = col.document(compositeId).get().await()
                if (snap.exists()) return mapearFactura(snap)
            }
            val q = col.whereEqualTo("numeroFactura", numLimpio).whereEqualTo("proveedorId", proveedorId).limit(1).get().await()
            if (!q.isEmpty) mapearFactura(q.documents[0]) else buscarFacturaPorNumero(numLimpio)
        } catch (e: Exception) {
            null
        }
    }

    fun observarFacturasRecientes(onErrorEscucha: ((String) -> Unit)? = null): Flow<List<FacturaCompra>> = callbackFlow {
        val clienteId = getClienteId()
        if (clienteId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva)
            .orderBy("creadoEl", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando facturas: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: "Sin detalle del servidor")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val facturas = snapshot?.documents?.mapNotNull { doc ->
                mapearFactura(doc)
            } ?: emptyList()

            trySend(facturas)
        }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun actualizarEstadoPagoFactura(facturaId: String, nuevoEstado: String): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank()) {
            return Result.failure(IllegalStateException("No hay sesión activa o ID de factura no válido"))
        }

        return try {
            val col = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva)
            col.document(facturaId).update(
                mapOf(
                    "estadoPago" to nuevoEstado.uppercase(),
                    "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado de factura $facturaId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapearFactura(doc: com.google.firebase.firestore.DocumentSnapshot): FacturaCompra? {
        return try {
            val id = doc.id
            val num = doc.getString("numeroFactura") ?: id
            val provId = doc.getString("proveedorId") ?: ""
            val provNombre = doc.getString("proveedorNombre") ?: ""
            val ruc = doc.getString("rucProveedor") ?: ""
            val condicion = doc.getString("condicionPago") ?: "Contado"
            val vencPago = doc.getString("fechaVencimientoPago") ?: ""
            val estado = doc.getString("estadoPago") ?: "PENDIENTE"
            val totalDoc = doc.getDouble("montoTotal")
                ?: doc.getDouble("total")
                ?: doc.getDouble("monto")
                ?: doc.getDouble("montoFactura")
                ?: 0.0
            val montoAcumulado = doc.getDouble("montoAcumulado") ?: 0.0
            val usuario = doc.getString("usuarioRegistroEmail") ?: ""
            val notas = doc.getString("notas") ?: ""

            val creadoTimestamp = doc.getTimestamp("creadoEl")
            val fechaRegistroStr = if (creadoTimestamp != null) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(creadoTimestamp.toDate())
            } else {
                doc.getString("fechaEmision") ?: doc.getString("fecha") ?: ""
            }

            val itemsRaw = doc.get("items") as? List<*>
            val itemsList = itemsRaw?.mapNotNull { itemMap ->
                if (itemMap is Map<*, *>) {
                    val pNombre = (itemMap["productoNombre"] as? String)
                        ?: (itemMap["nombre"] as? String)
                        ?: (itemMap["descripcion"] as? String)
                        ?: "Producto"
                    val cantComp = (itemMap["cantidadComprada"] as? Number)?.toDouble()
                        ?: (itemMap["cantidad"] as? Number)?.toDouble()
                        ?: (itemMap["unidades"] as? Number)?.toDouble()
                        ?: 0.0
                    val cantTot = (itemMap["cantidadTotal"] as? Number)?.toDouble() ?: cantComp
                    val cTot = (itemMap["costoTotal"] as? Number)?.toDouble()
                        ?: (itemMap["subtotal"] as? Number)?.toDouble()
                        ?: (itemMap["monto"] as? Number)?.toDouble()
                        ?: (itemMap["total"] as? Number)?.toDouble()
                        ?: 0.0
                    val cUnit = (itemMap["costoUnitario"] as? Number)?.toDouble()
                        ?: (itemMap["precioUnitario"] as? Number)?.toDouble()
                        ?: (itemMap["precioCompra"] as? Number)?.toDouble()
                        ?: (itemMap["costo"] as? Number)?.toDouble()
                        ?: 0.0

                    val finalCostoUnit = if (cUnit > 0) cUnit else if (cantComp > 0 && cTot > 0) cTot / cantComp else 0.0
                    val finalCostoTot = if (cTot > 0) cTot else if (cUnit > 0 && cantComp > 0) cUnit * cantComp else 0.0

                    ItemFacturaCompra(
                        productoId = itemMap["productoId"] as? String ?: (itemMap["id"] as? String ?: ""),
                        productoNombre = pNombre,
                        empaque = itemMap["empaque"] as? String ?: (itemMap["presentacion"] as? String ?: "Caja"),
                        loteNumero = itemMap["loteNumero"] as? String ?: (itemMap["lote"] as? String ?: ""),
                        vencimiento = itemMap["vencimiento"] as? String ?: (itemMap["fechaVencimiento"] as? String ?: ""),
                        cantidadTotal = cantTot,
                        cantidadComprada = cantComp,
                        bonificacionGratis = (itemMap["bonificacionGratis"] as? Number)?.toDouble() ?: 0.0,
                        costoTotal = finalCostoTot,
                        costoUnitario = finalCostoUnit
                    )
                } else null
            } ?: emptyList()

            val montoPagado = doc.getDouble("montoPagado") ?: 0.0

            val abonosRaw = doc.get("abonos") as? List<*>
            val abonosList = abonosRaw?.mapNotNull { abMap ->
                if (abMap is Map<*, *>) {
                    com.app.administradorfarmadon.inventario.compartido.modelo.AbonoFactura(
                        id = abMap["id"] as? String ?: "",
                        fechaLegible = abMap["fechaLegible"] as? String ?: "",
                        fechaMs = (abMap["fechaMs"] as? Number)?.toLong() ?: 0L,
                        monto = (abMap["monto"] as? Number)?.toDouble() ?: 0.0,
                        metodoPago = abMap["metodoPago"] as? String ?: "Transferencia",
                        numeroOperacion = abMap["numeroOperacion"] as? String ?: "",
                        usuarioNombre = abMap["usuarioNombre"] as? String ?: "",
                        usuarioEmail = abMap["usuarioEmail"] as? String ?: "",
                        notas = abMap["notas"] as? String ?: ""
                    )
                } else null
            } ?: emptyList()

            val totalFinal = if (itemsList.isNotEmpty()) itemsList.sumOf { it.costoTotal } else totalDoc

            FacturaCompra(
                id = id,
                numeroFactura = num,
                proveedorId = provId,
                proveedorNombre = provNombre,
                rucProveedor = ruc,
                condicionPago = condicion,
                fechaVencimientoPago = vencPago,
                estadoPago = estado,
                montoTotal = totalFinal,
                montoAcumulado = montoAcumulado,
                montoPagado = montoPagado,
                abonos = abonosList,
                items = itemsList,
                usuarioRegistroEmail = usuario,
                notas = notas,
                fechaRegistro = fechaRegistroStr
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapeando factura ${doc.id}: ${e.message}")
            null
        }
    }

    suspend fun registrarAbono(
        facturaId: String,
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        usuarioNombre: String,
        usuarioEmail: String,
        notas: String
    ): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión no válida o ID de factura ausente."))
        }
        if (monto <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto a abonar debe ser mayor a 0."))
        }

        return try {
            val docRef = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva).document(facturaId)
            // Hora del servidor: la fecha del abono jamás depende del reloj del celular.
            val ahoraMs = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val fechaLegible = sdf.format(java.util.Date(ahoraMs))

            val nuevoAbonoMap = mapOf(
                "id" to java.util.UUID.randomUUID().toString(),
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "monto" to monto,
                "metodoPago" to metodoPago,
                "numeroOperacion" to numeroOperacion.trim().uppercase(),
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail,
                "notas" to notas.trim()
            )

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La factura no existe.")

                val fact = mapearFactura(snap) ?: throw IllegalStateException("No se pudo leer la factura.")
                val saldoActual = fact.saldoPendienteReal

                if (monto > (saldoActual + 0.01)) {
                    throw IllegalArgumentException("El abono de S/ $monto supera el saldo pendiente de S/ ${String.format(java.util.Locale.US, "%.2f", saldoActual)}.")
                }

                val nuevoMontoPagado = fact.totalAbonadoReal + monto
                val saldoRestante = (fact.totalEfectivo - nuevoMontoPagado).coerceAtLeast(0.0)
                val nuevoEstado = if (saldoRestante <= 0.01) "PAGADA" else "ABONADO_PARCIAL"

                tx.update(
                    docRef,
                    mapOf(
                        "montoPagado" to nuevoMontoPagado,
                        "abonos" to com.google.firebase.firestore.FieldValue.arrayUnion(nuevoAbonoMap),
                        "estadoPago" to nuevoEstado,
                        "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando abono en factura $facturaId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun anularAbono(facturaId: String, abonoId: String): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank() || abonoId.isBlank()) {
            return Result.failure(IllegalStateException("Parámetros no válidos para anular abono."))
        }

        return try {
            val docRef = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva).document(facturaId)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La factura no existe.")

                val fact = mapearFactura(snap) ?: throw IllegalStateException("No se pudo leer la factura.")
                val abonosActualizados = fact.abonos.filterNot { it.id == abonoId }

                if (abonosActualizados.size == fact.abonos.size) {
                    throw IllegalStateException("El abono no fue encontrado en la factura.")
                }

                val nuevoTotalAbonado = abonosActualizados.sumOf { it.monto }
                val saldoRestante = (fact.totalEfectivo - nuevoTotalAbonado).coerceAtLeast(0.0)

                val nuevoEstado = when {
                    saldoRestante <= 0.01 -> "PAGADA"
                    nuevoTotalAbonado > 0 -> "ABONADO_PARCIAL"
                    else -> "PENDIENTE"
                }

                val abonosMapList = abonosActualizados.map { ab ->
                    mapOf(
                        "id" to ab.id,
                        "fechaLegible" to ab.fechaLegible,
                        "fechaMs" to ab.fechaMs,
                        "monto" to ab.monto,
                        "metodoPago" to ab.metodoPago,
                        "numeroOperacion" to ab.numeroOperacion,
                        "usuarioNombre" to ab.usuarioNombre,
                        "usuarioEmail" to ab.usuarioEmail,
                        "notas" to ab.notas
                    )
                }

                tx.update(
                    docRef,
                    mapOf(
                        "abonos" to abonosMapList,
                        "montoPagado" to nuevoTotalAbonado,
                        "estadoPago" to nuevoEstado,
                        "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error anulando abono $abonoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun prorrogarVencimiento(facturaId: String, nuevaFechaVencimiento: String): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank()) {
            return Result.failure(IllegalStateException("Parámetros no válidos para prorrogar."))
        }

        return try {
            val docRef = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva).document(facturaId)
            docRef.update(
                mapOf(
                    "fechaVencimientoPago" to nuevaFechaVencimiento.trim(),
                    "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error prorrogando vencimiento: ${e.message}", e)
            Result.failure(e)
        }
    }
}
