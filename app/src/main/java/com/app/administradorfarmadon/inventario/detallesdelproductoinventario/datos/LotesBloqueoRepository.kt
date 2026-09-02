package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
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
 * Operaciones de lotes —” bloqueo, devolución, canje, anulación y merma. Transacciones atómicas todo-o-nada.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) —” responsabilidad única.
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
                        throw Exception("Este lote se acaba de actualizar por otro usuario. Intentaste guardar $cantOperar pero ahora solo hay $cantDisponible disponibles. Actualicé el saldo en tu pantalla — revisa y vuelve a intentar con $cantDisponible o menos.")
                    }
                    val nuevaDisp = (cantDisponible - cantOperar).coerceAtLeast(0.0)
                    val nuevaBloq = (cantBloqueada + cantOperar).coerceAtLeast(0.0)
                    loteData["cantidad"] = nuevaDisp
                    loteData["cantidadBloqueada"] = nuevaBloq
                    loteData["estadoSanitario"] =
                        if (nuevaDisp == 0.0) "CUARENTENA_TOTAL" else "CUARENTENA_PARCIAL"
                } else {
                    if (cantOperar > cantBloqueada) {
                        throw Exception("Este lote se acaba de actualizar. Intentaste liberar $cantOperar pero ahora solo hay $cantBloqueada en cuarentena. Actualicé el saldo — intenta con $cantBloqueada o menos.")
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

                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesMap)

                val updatesProducto = mutableMapOf<String, Any>(
                    "lotes" to lotesMap,
                    "stock" to nuevoStockDisponible,
                    "stockTotal" to nuevoStockTotal,
                    "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                // Si el lote principal queda totalmente en cuarentena, ya no es vendible: se limpia solo.
                if (((loteData["cantidad"] as? Number)?.toDouble() ?: 0.0) <= 0.0) {
                    val principalActual = snap.getString("lotePrioritarioId") ?: ""
                    if (principalActual.isNotBlank() &&
                        (principalActual.equals(cleanKey, true) || principalActual.equals(lote.numero, true))
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

        // Reclamos abiertos del lote: anular dejaría el reclamo huérfano.
        val reclamosAbiertos = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            .collection("reclamos_proveedores")
            .whereEqualTo("productoId", productId)
            .whereEqualTo("loteNumero", lote.numero.trim().uppercase())
            .whereIn("estado", listOf("EN_REVISION_DROGUERIA", "EN_REVISION"))
            .get().await()
        if (reclamosAbiertos.documents.isNotEmpty()) {
            return Result.failure(
                Exception(
                    "No puedes anular el lote ${lote.numero}: tiene ${reclamosAbiertos.documents.size} reclamo(s) abierto(s) al proveedor. Ciérralos o resuélvelos primero."
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

                val lotesMap =
                    (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val resAnular = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero)
                    ?: throw Exception("El lote no se encuentra en el inventario.")
                val (cleanKeyRealAnular, loteData) = resAnular
                val cantDisponible = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                val cantBloqueada = (loteData["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                val totalLote = cantDisponible + cantBloqueada
                if (totalLote <= 0) throw Exception("El lote ${lote.numero} ya está en 0, no hay saldo que anular.")

                // BLINDAJE SANITARIO AUTORITATIVO (dentro de la transacción):
                // Firestore no permite consultas adentro de runTransaction, por eso la
                // pregunta "¿este lote ya vendió?" se resuelve con la bandera desnormalizada
                // del lote (ventasRegistradas), que la caja enciende al vender, en la MISMA
                // transacción de la venta. Así la carrera con una venta en el mismo instante
                // es imposible: o la venta ya la puso (y abortamos) o esta tx gana y la
                // venta siguiente la verá. Nunca se borra un lote con trazabilidad viva.
                val ventasRegistradas = (loteData["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                if (ventasRegistradas > 0) {
                    throw Exception(
                        "No puedes anular el lote ${lote.numero}: ya tiene ${ventasRegistradas.toLong()} venta(s) registrada(s). " +
                                "La trazabilidad sanitaria debe preservarse. Usa MERMA o DEVOLUCIÓN para las unidades restantes."
                    )
                }
                // Candado anti-borrado ajeno: si el lote junta mercadería de MÁS DE UN
                // ingreso (dos facturas, o ajuste + compra), anularlo borraría también lo
                // que no nació de esta recepción. Se bloquea con la salida correcta.
                @Suppress("UNCHECKED_CAST")
                val entradasDelLote = (loteData["entradas"] as? List<Map<String, Any>>) ?: emptyList()
                val origenesDistintos = entradasDelLote
                    .mapNotNull { (it["factura"] as? String)?.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (origenesDistintos.size > 1) {
                    throw Exception(
                        "El lote ${lote.numero} junta mercadería de ${origenesDistintos.size} ingresos distintos (${origenesDistintos.take(3).joinToString(" · ")}). " +
                                "Anularlo borraría también lo que no nació de esta compra. Corrige la cantidad con Ajuste de salida (error de conteo) o anula la factura desde Cuentas por Pagar."
                    )
                }
                val cleanKey = cleanKeyRealAnular

                lotesMap.remove(cleanKeyRealAnular)
                val (nuevoStockDisponible, nuevoStockTotal, nuevoVencimientoMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesMap)

                val updatesProducto = mutableMapOf<String, Any>(
                    "lotes" to lotesMap,
                    "stock" to nuevoStockDisponible,
                    "stockTotal" to nuevoStockTotal,
                    "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                // El lote se eliminó: si era el principal de consumo, se limpia solo.
                val principalActual = snap.getString("lotePrioritarioId") ?: ""
                if (principalActual.isNotBlank() &&
                    (principalActual.equals(cleanKeyRealAnular, true) || principalActual.equals(lote.numero, true))
                ) {
                    updatesProducto["lotePrioritarioId"] = ""
                    updatesProducto["lotePrioritarioPor"] = ""
                    updatesProducto["lotePrioritarioPorRol"] = ""
                }
                // ══ FASE DE LECTURAS COMPLETA (Firestore: TODAS las lecturas antes de
                // cualquier escritura; un get posterior abortaría toda la anulación y
                // antes era tragado en silencio por un try/catch — datos a medias) ══
                val facturaNumeroRaw = (loteData["factura"] as? String)?.trim() ?: ""
                val proveedorNombreRaw = (loteData["proveedor"] as? String)?.trim() ?: ""
                val esFacturaFormal =
                    facturaNumeroRaw.isNotBlank() && facturaNumeroRaw != "S/C (Sin Comprobante)" && proveedorNombreRaw.isNotBlank() && proveedorNombreRaw != "Almacén General"
                var facturaSnap: com.google.firebase.firestore.DocumentSnapshot? = null
                var effectiveRef: com.google.firebase.firestore.DocumentReference? = null
                var pedidoRefFactura: com.google.firebase.firestore.DocumentReference? = null
                var pedidoSnapFactura: com.google.firebase.firestore.DocumentSnapshot? = null
                if (esFacturaFormal) {
                    val cleanNumero =
                        facturaNumeroRaw.uppercase().replace("/", "-").replace(" ", "_")
                    val provIdLote = (loteData["proveedorId"] as? String)?.trim().orEmpty()
                    val provKeyNombre = proveedorNombreRaw.uppercase().replace("/", "-").replace(" ", "_")
                        .replace(".", "__DOT__")
                    // La factura pudo nacer con llave proveedorId__numero O nombre__numero
                    // (llegadas por pedido o por ingreso suelto). Se prueban todas las llaves
                    // reales: si ninguna existe, se ABORTA con la verdad (jamás se anula el
                    // lote dejando la factura viva con su deuda intacta en silencio).
                    val candidatas = buildList {
                        if (provIdLote.isNotBlank()) add(tiendaRef.collection("compras_facturas").document("${provIdLote}__${cleanNumero}"))
                        if (provKeyNombre.isNotBlank()) add(tiendaRef.collection("compras_facturas").document("${provKeyNombre}__${cleanNumero}"))
                        add(tiendaRef.collection("compras_facturas").document(cleanNumero))
                    }.distinctBy { it.id }
                    var snapLeida: com.google.firebase.firestore.DocumentSnapshot? = null
                    var refEfectiva: com.google.firebase.firestore.DocumentReference? = null
                    for (refC in candidatas) {
                        val c = tx.get(refC)
                        if (!c.exists()) continue
                        // La llave plana de OTRO proveedor no es esta factura.
                        if (refC.id == cleanNumero && candidatas.size > 1) {
                            val provDeLaFactura = (c.getString("proveedorNombre") ?: "").trim()
                            val idProvDeLaFactura = (c.getString("proveedorId") ?: "").trim()
                            val mismo = (provIdLote.isNotBlank() && idProvDeLaFactura == provIdLote) ||
                                provDeLaFactura.equals(proveedorNombreRaw, ignoreCase = true)
                            if (!mismo) continue
                        }
                        snapLeida = c
                        refEfectiva = refC
                        break
                    }
                    if (snapLeida == null) {
                        throw IllegalStateException(
                            "La factura $facturaNumeroRaw de este lote no aparece en Cuentas por Pagar; anular el lote sin tocarla dejaría la deuda y el stock partidos. No se anuló nada: revisa la factura primero."
                        )
                    }
                    facturaSnap = snapLeida
                    effectiveRef = refEfectiva
                    // El pedido asociado también se lee AHORA, en la zona de lecturas.
                    val pedidoIdLectura = snapLeida.getString("pedidoId") ?: ""
                    if (pedidoIdLectura.isNotBlank()) {
                        pedidoRefFactura = tiendaRef.collection("pedidos_compra").document(pedidoIdLectura)
                        pedidoSnapFactura = tx.get(pedidoRefFactura!!)
                    }
                }

                tx.update(productRef, updatesProducto)

                if (facturaSnap != null && effectiveRef != null) {
                        val itemsRaw = facturaSnap.get("items") as? List<*>
                        val itemsMatch = itemsRaw?.filterIsInstance<Map<*, *>>()?.filter {
                            it["productoId"] == productId && (it["loteNumero"] as? String)?.trim()
                                ?.equals(lote.numero.trim(), ignoreCase = true) == true
                        } ?: emptyList()
                        val totalADescontar = if (itemsMatch.isNotEmpty()) itemsMatch.sumOf {
                            (it["costoTotal"] as? Number)?.toDouble() ?: 0.0
                        } else {
                            val costoCompraLote = (loteData["costoCompra"] as? Number)?.toDouble() ?: 0.0
                            if (costoCompraLote > 0.0) costoCompraLote
                            else CostoRealLote.costoUnitario(loteData) * totalLote
                        }
                        if (totalADescontar > 0) {
                            val nuevoAcumulado = ((facturaSnap.getDouble("montoAcumulado")
                                ?: 0.0) - totalADescontar).coerceAtLeast(0.0)
                            val itemsRestantes = itemsRaw?.filterNot { m ->
                                m is Map<*, *> && m["productoId"] == productId && (m["loteNumero"] as? String)?.trim()
                                    ?.equals(lote.numero.trim(), ignoreCase = true) == true
                            } ?: emptyList<Any>()
                            // Si la factura queda sin contenido tras la anulación, se marca ANULADA
                            // registrando QUIÉN la anuló, CUíNDO y POR QUÉ —” coherencia multiusuario.
                            val updatesFactura = mutableMapOf<String, Any>(
                                "montoAcumulado" to nuevoAcumulado,
                                "items" to itemsRestantes,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            )
                            if (itemsRestantes.isEmpty()) {
                                updatesFactura["estadoPago"] = "ANULADA"
                                // Campos canónicos de anulación (misma verdad que el motor de facturas)
                                updatesFactura["motivoAnulacion"] = motivo.trim()
                                updatesFactura["anuladoPorEmail"] = usuarioEmail
                                updatesFactura["anuladoEl"] = FieldValue.serverTimestamp()
                            }
                            tx.update(effectiveRef, updatesFactura)
                        }

                        // Compras debe decir la verdad: si la entrada anulada pertenecía a un pedido,
                        // se descuenta de lo recibido y se recalcula el estado del pedido.
                        // La foto del pedido se tomó en la fase de lecturas (jamás leer tras escribir).
                        if (pedidoRefFactura != null) {
                            val pedidoRef = pedidoRefFactura
                            val pedidoSnap = pedidoSnapFactura
                            if (pedidoSnap != null && pedidoSnap.exists()) {
                                val estadoPedido = pedidoSnap.getString("estado") ?: "ENVIADO"
                                if (estadoPedido in listOf("RECIBIDO", "ENTREGA_PARCIAL", "ENVIADO")) {
                                    @Suppress("UNCHECKED_CAST")
                                    val itemsPedido = (pedidoSnap.get("items") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                                    val cantidadAnuladaItem = itemsMatch.firstOrNull()?.let { m ->
                                        (m["cantidadTotal"] as? Number)?.toDouble()
                                            ?: (m["cantidad"] as? Number)?.toDouble()
                                            ?: totalLote
                                    } ?: totalLote
                                    var huboCambio = false
                                    val itemsNuevos = itemsPedido.map { raw ->
                                        val m = raw.toMutableMap()
                                        if ((m["productoId"] as? String) == productId) {
                                            val recibida = (m["cantidadRecibida"] as? Number)?.toInt() ?: 0
                                            val aRestar = cantidadAnuladaItem.toInt().coerceIn(0, recibida)
                                            if (aRestar > 0) {
                                                m["cantidadRecibida"] = (recibida - aRestar).coerceAtLeast(0)
                                                huboCambio = true
                                            }
                                        }
                                        m
                                    }
                                    if (huboCambio) {
                                        val todasCompletas = itemsNuevos.all {
                                            val rec = (it["cantidadRecibida"] as? Number)?.toInt() ?: 0
                                            val ped = (it["cantidad"] as? Number)?.toInt() ?: 0
                                            rec >= ped
                                        }
                                        val hayRecibido = itemsNuevos.any {
                                            (it["cantidadRecibida"] as? Number)?.toInt() ?: 0 > 0
                                        }
                                        val nuevoEstadoPedido = when {
                                            todasCompletas -> "RECIBIDO"
                                            hayRecibido -> "ENTREGA_PARCIAL"
                                            else -> "ENVIADO"
                                        }
                                        tx.update(
                                            pedidoRef,
                                            mapOf(
                                                "items" to itemsNuevos,
                                                "estado" to nuevoEstadoPedido,
                                                "actualizadoEl" to FieldValue.serverTimestamp()
                                            )
                                        )
                                    }
                                }
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
                // Auditoría lote ordenada: auditorias/inventario/lotes/listaeliminado/{id} —” solo valiosa
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
                        "eliminadoPorUid" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""),
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

    /**
     * Corrección de vencimiento mal tipeado al recibir. Sin esto, un "12/2026" que era
     * "12/2027" mentiría en las alertas para siempre (y la única salida era anular el
     * lote entero — demasiado destructivo). Es una transacción atómica que NO toca
     * cantidades: solo corrige la fecha, recalcula el FEFO del producto y deja asiento
     * en el kardex con el antes → después, quién y por qué.
     * La fecha puede quedar en el pasado a propósito (la mercadería realmente está
     * vencida hoy): corregir hacia la verdad jamás se bloquea.
     */
    suspend fun corregirVencimientoLote(
        clienteId: String,
        productId: String,
        lote: LoteProducto,
        nuevoVencimiento: String,
        motivo: String,
        usuarioEmail: String
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(
            SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia.")
        )
        if (clienteId.isBlank() || productId.isBlank() || lote.numero.isBlank()) {
            return Result.failure(Exception("Datos insuficientes para corregir el vencimiento."))
        }
        val vtoNorm = FechaVencimientoHelper.normalizar(nuevoVencimiento.trim())
            ?: return Result.failure(IllegalArgumentException("La fecha nueva no es válida (usa formato mes/año)."))
        if (motivo.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Escribe el motivo real de la corrección (mínimo 5 letras)."))
        }

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")

                val lotesMap = (snap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val res = FechaVencimientoHelper.resolverLote(lotesMap, lote.numero)
                    ?: throw Exception("El lote ya no existe en inventario.")
                val (cleanKeyReal, loteData) = res
                val vencActual = (loteData["vencimiento"] as? String)?.trim().orEmpty()
                val vencActualNorm = FechaVencimientoHelper.normalizar(vencActual) ?: vencActual
                if (vencActualNorm == vtoNorm) {
                    throw IllegalArgumentException("El lote ${lote.numero} ya tiene el vencimiento $vtoNorm; no hay nada que corregir.")
                }

                loteData["vencimiento"] = vtoNorm
                lotesMap[cleanKeyReal] = loteData

                // Solo recalcula la fecha guía del producto; el stock no se toca.
                val (_, _, nuevoVencimientoMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesMap)
                tx.update(
                    productRef,
                    mapOf(
                        "lotes" to lotesMap,
                        "vencimientoMasCercano" to nuevoVencimientoMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                )

                // Asiento de kardex: qué cambió (antes → después), quién y por qué.
                tx.set(
                    movimientoRef,
                    mapOf(
                        "id" to movimientoRef.id,
                        "tipo" to "CORRECCION_VENCIMIENTO",
                        "productoId" to productId,
                        "productoNombre" to (snap.getString("nombre") ?: ""),
                        "loteNumero" to lote.numero,
                        "cantidad" to 0.0,
                        "motivo" to "Vencimiento corregido: ${vencActual.ifBlank { "sin fecha" }} → $vtoNorm. ${motivo.trim()}",
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error corrigiendo vencimiento del lote ${lote.numero}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
