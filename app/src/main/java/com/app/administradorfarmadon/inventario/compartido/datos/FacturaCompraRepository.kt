package com.app.administradorfarmadon.inventario.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.ItemFacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.RespuestaPlataAnulacion
import com.app.administradorfarmadon.inventario.compartido.modelo.AjusteFactura
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
            // Con proveedor seleccionado, una ausencia es ausencia para ese proveedor.
            // Nunca se cae a una factura del mismo número perteneciente a otra droguería.
            if (!q.isEmpty) mapearFactura(q.documents[0]) else null
        } catch (e: Exception) {
            null
        }
    }

    fun observarFacturasRecientes(onErrorEscucha: ((String) -> Unit)? = null): Flow<List<FacturaCompra>> = callbackFlow {
        val clienteId = getClienteId()
        if (clienteId.isBlank()) {
            close(IllegalStateException("No hay una farmacia activa para cargar facturas."))
            return@callbackFlow
        }

        val ref = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva)
            .orderBy("creadoEl", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando facturas: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: error.toString())
                close(error)
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
                    @Suppress("UNCHECKED_CAST")
                    val pagosRaw = abMap["pagos"] as? List<Map<String, Any>>
                    val pagosList = pagosRaw?.mapNotNull { p ->
                        if (p is Map<*, *>) {
                            com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle(
                                metodoPago = p["metodoPago"] as? String ?: "",
                                monto = (p["monto"] as? Number)?.toDouble() ?: 0.0,
                                numeroOperacion = p["numeroOperacion"] as? String ?: ""
                            )
                        } else null
                    } ?: emptyList()
                    val metodoLegacy = abMap["metodoPago"] as? String ?: ""
                    val opLegacy = abMap["numeroOperacion"] as? String ?: ""
                    com.app.administradorfarmadon.inventario.compartido.modelo.AbonoFactura(
                        id = abMap["id"] as? String ?: "",
                        fechaLegible = abMap["fechaLegible"] as? String ?: "",
                        fechaMs = (abMap["fechaMs"] as? Number)?.toLong() ?: 0L,
                        monto = (abMap["monto"] as? Number)?.toDouble() ?: 0.0,
                        metodoPago = metodoLegacy,
                        numeroOperacion = opLegacy,
                        pagos = if (pagosList.isNotEmpty()) pagosList
                                else if (metodoLegacy.isNotBlank()) listOf(
                                    com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle(
                                        metodoPago = metodoLegacy,
                                        monto = (abMap["monto"] as? Number)?.toDouble() ?: 0.0,
                                        numeroOperacion = opLegacy
                                    )
                                ) else emptyList(),
                        usuarioNombre = abMap["usuarioNombre"] as? String ?: "",
                        usuarioEmail = abMap["usuarioEmail"] as? String ?: "",
                        notas = abMap["notas"] as? String ?: "",
                        anulado = abMap["anulado"] == true,
                        anuladoPorNombre = abMap["anuladoPorNombre"] as? String ?: "",
                        anuladoPorEmail = abMap["anuladoPorEmail"] as? String ?: "",
                        anuladoElLegible = abMap["anuladoElLegible"] as? String ?: "",
                        motivoAnulacion = abMap["motivoAnulacion"] as? String ?: ""
                    )
                } else null
            } ?: emptyList()

            val ajustesRaw = doc.get("ajustesFactura") as? List<*>
            val ajustesList = ajustesRaw?.mapNotNull { aMap ->
                if (aMap is Map<*, *>) {
                    AjusteFactura(
                        id = aMap["id"] as? String ?: "",
                        tipo = aMap["tipo"] as? String ?: "NOTA_CREDITO",
                        numeroDocumento = aMap["numeroDocumento"] as? String ?: "",
                        monto = (aMap["monto"] as? Number)?.toDouble() ?: 0.0,
                        motivo = aMap["motivo"] as? String ?: "",
                        fechaLegible = aMap["fechaLegible"] as? String ?: "",
                        fechaMs = (aMap["fechaMs"] as? Number)?.toLong() ?: 0L,
                        usuarioNombre = aMap["usuarioNombre"] as? String ?: "",
                        usuarioEmail = aMap["usuarioEmail"] as? String ?: ""
                    )
                } else null
            } ?: emptyList()

            // El total del papel manda. Los items solo representan lo recibido hasta ahora.
            // Una factura puede llegar parcialmente y no por eso cambia su total comercial.
            val totalFinal = totalDoc.takeIf { it > 0.0 } ?: itemsList.sumOf { it.costoTotal }

            // ── Campos de anulación (el papel anulado se lee igual que el vivo) ──
            val motivoAnulacion = doc.getString("motivoAnulacion") ?: ""
            val anuladoPorEmail = doc.getString("anuladoPorEmail") ?: ""
            val anuladoPorNombre = doc.getString("anuladoPorNombre") ?: ""
            val anuladoElLegible = doc.getTimestamp("anuladoEl")?.let { ts ->
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                sdf.format(ts.toDate())
            } ?: ""
            val anulacionPlataMap = doc.get("anulacionPlata") as? Map<*, *>
            val anulacionPlata = if (anulacionPlataMap != null) RespuestaPlataAnulacion(
                decision = anulacionPlataMap["decision"] as? String ?: "",
                monto = (anulacionPlataMap["monto"] as? Number)?.toDouble() ?: 0.0,
                metodoDevolucion = anulacionPlataMap["metodoDevolucion"] as? String ?: "",
                referenciaDevolucion = anulacionPlataMap["referenciaDevolucion"] as? String ?: "",
                fechaLegible = anulacionPlataMap["fechaLegible"] as? String ?: "",
                fechaMs = (anulacionPlataMap["fechaMs"] as? Number)?.toLong() ?: 0L,
                usuarioNombre = anulacionPlataMap["usuarioNombre"] as? String ?: "",
                usuarioEmail = anulacionPlataMap["usuarioEmail"] as? String ?: ""
            ) else null

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
                fechaRegistro = fechaRegistroStr,
                ajustesFactura = ajustesList,
                motivoAnulacion = motivoAnulacion,
                anuladoPorEmail = anuladoPorEmail,
                anuladoPorNombre = anuladoPorNombre,
                anuladoElLegible = anuladoElLegible,
                anulacionPlata = anulacionPlata
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
        pagos: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList(),
        usuarioNombre: String,
        usuarioEmail: String,
        notas: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión no válida o ID de factura ausente."))
        }
        if (monto <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto a abonar debe ser mayor a 0."))
        }
        val pagosFinales = if (pagos.isNotEmpty()) pagos else listOf(
            com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle(
                metodoPago = metodoPago,
                monto = monto,
                numeroOperacion = numeroOperacion.trim().uppercase()
            )
        )
        val sumaPagos = pagosFinales.sumOf { it.monto }
        if (kotlin.math.abs(sumaPagos - monto) > 0.01) {
            return Result.failure(IllegalArgumentException("La suma de los pagos ($sumaPagos) no coincide con el monto del abono ($monto)."))
        }

        return try {
            val docRef = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva).document(facturaId)
            // Hora del servidor: la fecha del abono jamás depende del reloj del celular.
            val ahoraMs = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val fechaLegible = sdf.format(java.util.Date(ahoraMs))

            val idAbono = idempotenciaId.trim().ifBlank { java.util.UUID.randomUUID().toString() }
            val nuevoAbonoMap = mapOf(
                "id" to idAbono,
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "monto" to monto,
                "metodoPago" to pagosFinales.first().metodoPago,
                "numeroOperacion" to pagosFinales.first().numeroOperacion,
                "pagos" to pagosFinales.map { p ->
                    mapOf(
                        "metodoPago" to p.metodoPago,
                        "monto" to p.monto,
                        "numeroOperacion" to p.numeroOperacion
                    )
                },
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail,
                "notas" to notas.trim(),
                "anulado" to false,
                "anuladoPorNombre" to "",
                "anuladoPorEmail" to "",
                "anuladoElLegible" to "",
                "motivoAnulacion" to ""
            )

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La factura no existe.")

                val fact = mapearFactura(snap) ?: throw IllegalStateException("No se pudo leer la factura.")
                if (fact.esAnulada) throw IllegalStateException("No se puede registrar el abono: esta factura fue anulada en otra sesión.")

                // Si la respuesta de Firebase se perdió, el mismo intento puede
                // volver a llegar. El abono ya escrito significa éxito: no se suma
                // otra vez ni se altera el saldo.
                if (idempotenciaId.isNotBlank() && fact.abonos.any { it.id == idAbono }) {
                    return@runTransaction
                }

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

    /**
     * Nota de crédito: reduce el total del PAPEL con documento real del proveedor (R3/R12).
     * El papel original se conserva (totalPapel); la deuda viva baja y se recalcula el estado.
     * Historial append-only, con idempotencia para que un reintento de red no duplique el ajuste.
     */
    suspend fun registrarNotaCredito(
        facturaId: String,
        numeroDocumento: String,
        monto: Double,
        motivo: String,
        usuarioNombre: String,
        usuarioEmail: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val clienteId = getClienteId()
        if (clienteId.isBlank() || facturaId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión no válida o ID de factura ausente."))
        }
        val numDoc = numeroDocumento.trim().uppercase()
        if (numDoc.isBlank()) {
            return Result.failure(IllegalArgumentException("El número de la nota de crédito es obligatorio."))
        }
        if (monto <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto de la nota de crédito debe ser mayor a 0."))
        }
        if (motivo.trim().length < 5) {
            return Result.failure(IllegalArgumentException("El motivo debe tener al menos 5 caracteres."))
        }

        return try {
            val docRef = FarmadonPaths.comprasFacturas(db, clienteId, SessionManager.sucursalIdEfectiva).document(facturaId)
            val ahoraMs = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val fechaLegible = sdf.format(java.util.Date(ahoraMs))

            val idAjuste = idempotenciaId.trim().ifBlank { java.util.UUID.randomUUID().toString() }
            val nuevoAjusteMap = mapOf(
                "id" to idAjuste,
                "tipo" to "NOTA_CREDITO",
                "numeroDocumento" to numDoc,
                "monto" to monto,
                "motivo" to motivo.trim(),
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail
            )

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La factura no existe.")
                val fact = mapearFactura(snap) ?: throw IllegalStateException("No se pudo leer la factura.")
                if (fact.esAnulada) throw IllegalStateException("No se puede ajustar una factura anulada.")

                // Reintento de red: el ajuste ya escrito significa éxito, no se suma otra vez.
                if (idempotenciaId.isNotBlank() && fact.ajustesFactura.any { it.id == idAjuste }) {
                    return@runTransaction
                }

                val totalAjustesActual = fact.totalAjustes
                val maximoAjustable = (fact.totalPapel - totalAjustesActual).coerceAtLeast(0.0)
                if (monto > maximoAjustable + 0.01) {
                    throw IllegalArgumentException(
                        "La nota de crédito de " + String.format(java.util.Locale.US, "%.2f", monto) +
                            " supera el total ajustable de " + String.format(java.util.Locale.US, "%.2f", maximoAjustable) + "."
                    )
                }

                val ajustesActualizados = fact.ajustesFactura.map { a ->
                    mapOf(
                        "id" to a.id,
                        "tipo" to a.tipo,
                        "numeroDocumento" to a.numeroDocumento,
                        "monto" to a.monto,
                        "motivo" to a.motivo,
                        "fechaLegible" to a.fechaLegible,
                        "fechaMs" to a.fechaMs,
                        "usuarioNombre" to a.usuarioNombre,
                        "usuarioEmail" to a.usuarioEmail
                    )
                }.toMutableList()
                ajustesActualizados.add(nuevoAjusteMap)

                val nuevoTotalEfectivo = (fact.totalPapel - totalAjustesActual - monto).coerceAtLeast(0.0)
                val totalAbonado = fact.totalAbonadoReal
                val saldoRestante = (nuevoTotalEfectivo - totalAbonado).coerceAtLeast(0.0)
                val nuevoEstado = when {
                    saldoRestante <= 0.01 -> "PAGADA"
                    totalAbonado > 0.01 -> "ABONADO_PARCIAL"
                    else -> "PENDIENTE"
                }

                tx.update(
                    docRef,
                    mapOf(
                        "ajustesFactura" to ajustesActualizados,
                        "estadoPago" to nuevoEstado,
                        "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando nota de crédito en factura $facturaId: ${e.message}", e)
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
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La factura no existe.")

                val fact = mapearFactura(snap) ?: throw IllegalStateException("No se pudo leer la factura.")
                if (fact.esAnulada) throw IllegalStateException("No se puede prorrogar: esta factura fue anulada en otra sesión.")

                tx.update(
                    docRef,
                    mapOf(
                        "fechaVencimientoPago" to nuevaFechaVencimiento.trim(),
                        "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error prorrogando vencimiento: ${e.message}", e)
            Result.failure(e)
        }
    }
}
