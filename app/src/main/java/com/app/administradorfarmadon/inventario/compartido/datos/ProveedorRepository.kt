package com.app.administradorfarmadon.inventario.compartido.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.MovimientoSaldoProveedor
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repositorio de Proveedores / Distribuidores en Cloud Firestore (Multi-Tenant).
 * Permite listar, observar en tiempo real y registrar proveedores reutilizables por sede.
 */
class ProveedorRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "ProveedorRepository"
    }

    private fun getClienteId(): String {
        // Solo la farmacia real de la sesión es válida como contenedor (R1): nunca
        // el uid de un usuario, que no es una farmacia y mezclaría datos.
        return SessionManager.clienteIdGarantizado
    }

    private fun cleanKey(name: String): String {
        return name.trim().lowercase().replace(" ", "_").replace("/", "-")
    }

    fun observarProveedores(onErrorEscucha: ((String) -> Unit)? = null): Flow<List<Proveedor>> = callbackFlow {
        val clienteId = getClienteId()
        if (clienteId.isBlank()) {
            close(IllegalStateException("No hay una farmacia activa para cargar proveedores."))
            return@callbackFlow
        }

        val ref = FarmadonPaths.proveedores(db, clienteId, SessionManager.sucursalIdEfectiva)
            .orderBy("nombre", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando proveedores: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: error.toString())
                close(error)
                return@addSnapshotListener
            }

            val lista = snapshot?.documents?.mapNotNull { doc ->
                val id = doc.id
                val nombre = doc.getString("nombre") ?: ""
                val idFiscal = doc.getString("idFiscal") ?: ""
                val contacto = doc.getString("contacto") ?: ""
                val telefono = doc.getString("telefono") ?: ""
                val email = doc.getString("email") ?: ""
                val direccion = doc.getString("direccion") ?: ""
                val montoMinimoPedido = doc.getDouble("montoMinimoPedido") ?: (doc.get("montoMinimoPedido") as? Number)?.toDouble() ?: 0.0
                val saldoAFavor = doc.getDouble("saldoAFavor") ?: 0.0
                val historialRaw = doc.get("historialSaldoAFavor") as? List<*>
                val historialSaldo = historialRaw?.mapNotNull { m ->
                    if (m is Map<*, *>) MovimientoSaldoProveedor(
                        id = m["id"] as? String ?: "",
                        tipo = m["tipo"] as? String ?: "",
                        monto = (m["monto"] as? Number)?.toDouble() ?: 0.0,
                        facturaId = m["facturaId"] as? String ?: "",
                        facturaNumero = m["facturaNumero"] as? String ?: "",
                        motivo = m["motivo"] as? String ?: "",
                        documento = m["documento"] as? String ?: "",
                        fechaLegible = m["fechaLegible"] as? String ?: "",
                        fechaMs = (m["fechaMs"] as? Number)?.toLong() ?: 0L,
                        usuarioNombre = m["usuarioNombre"] as? String ?: "",
                        usuarioEmail = m["usuarioEmail"] as? String ?: ""
                    ) else null
                } ?: emptyList()

                if (nombre.isNotBlank()) {
                    Proveedor(
                        id = id,
                        nombre = nombre,
                        idFiscal = idFiscal,
                        contacto = contacto,
                        telefono = telefono,
                        email = email,
                        direccion = direccion,
                        montoMinimoPedido = montoMinimoPedido,
                        saldoAFavor = saldoAFavor,
                        historialSaldoAFavor = historialSaldo
                    )
                } else null
            } ?: emptyList()

            trySend(lista)
        }

        awaitClose { listener.remove() }
    }

    suspend fun registrarOActualizarProveedor(proveedor: Proveedor): Result<String> {
        val clienteId = getClienteId()
        if (clienteId.isBlank()) return Result.failure(Exception("No se encontró sesión activa de farmacia."))
        val nomLimpio = proveedor.nombre.trim()
        val rucLimpio = proveedor.idFiscal.trim()
        if (nomLimpio.isBlank()) return Result.failure(Exception("El nombre del proveedor es obligatorio."))
        if (listOf("SIN PROVEEDOR", "DROGUERIA GENERAL", "SIN ASIGNAR", "N/A").any { nomLimpio.equals(it, ignoreCase = true) }) {
            return Result.failure(Exception("Ingresa el nombre comercial o razón social real de la droguería/proveedor."))
        }
        if (rucLimpio.isNotBlank() && (!rucLimpio.all { it.isDigit() } || rucLimpio.length != 11)) {
            return Result.failure(Exception("El RUC del proveedor debe tener exactamente 11 dígitos numéricos."))
        }

        return try {
            val colRef = FarmadonPaths.proveedores(db, clienteId, SessionManager.sucursalIdEfectiva)
            // Un RUC = una ficha: antes de crear, buscar si ya existe otra ficha con el mismo RUC.
            // Sin esto la misma factura entra una vez por cada ficha y la deuda se duplica.
            if (proveedor.id.isBlank() && rucLimpio.length == 11) {
                val dupRuc = colRef.whereEqualTo("idFiscal", rucLimpio).limit(1).get().await()
                val dupDoc = dupRuc.documents.firstOrNull()
                if (dupDoc != null) {
                    val dupNombre = dupDoc.getString("nombre") ?: "registrado"
                    return Result.failure(
                        Exception("Ya existe el proveedor '$dupNombre' con el RUC $rucLimpio. Úsalo de la lista en vez de crear otro.")
                    )
                }
            }
            val cleanKey = cleanKey(nomLimpio)
            val provId = proveedor.id.ifBlank {
                if (rucLimpio.length == 11) "prov_$rucLimpio"
                else if (cleanKey.isNotBlank()) "prov_$cleanKey"
                else UUID.randomUUID().toString()
            }

            val docRef = colRef.document(provId)

            val data = mapOf(
                "id" to provId,
                "nombre" to nomLimpio,
                "idFiscal" to rucLimpio,
                "contacto" to proveedor.contacto.trim(),
                "telefono" to proveedor.telefono.trim(),
                "email" to proveedor.email.trim(),
                "direccion" to proveedor.direccion.trim(),
                "montoMinimoPedido" to proveedor.montoMinimoPedido,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )

            if (proveedor.id.isBlank()) {
                // Creación con transacción: dos personas registrando el mismo nombre
                // a la vez jamás se pisan; la segunda recibe un error claro.
                db.runTransaction { tx ->
                    val snap = tx.get(docRef)
                    if (snap.exists()) {
                        throw IllegalStateException("Ya existe un proveedor con este RUC o identificador ('${nomLimpio}'). Revisa la lista antes de registrarlo.")
                    }
                    tx.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }.await()
            } else {
                // Edición con transacción: si otra persona ELIMINÓ el proveedor mientras
                // se editaba, el guardado NO lo resucita.
                db.runTransaction { tx ->
                    val snap = tx.get(docRef)
                    if (!snap.exists()) {
                        throw IllegalStateException(
                            "El proveedor fue eliminado mientras lo editabas; no se guardó. " +
                                "Cierra el formulario y revisa la lista de proveedores."
                        )
                    }
                    tx.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }.await()
            }
            Result.success(provId)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando proveedor en Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un proveedor por su ID determinístico.
     * Bloqueado si tiene facturas pendientes de pago (trampa preventiva).
     * La factura es documento legal separado —” no se borra, solo se bloquea la eliminación del contacto.
     */
    suspend fun eliminarProveedor(
        clienteId: String,
        sucursalId: String,
        proveedorId: String,
        proveedorNombre: String,
        facturasPendientesCount: Int
    ): Result<Unit> {
        if (proveedorNombre.isBlank()) return Result.failure(Exception("Nombre de proveedor inválido."))
        return try {
            val cleanKey = cleanKey(proveedorNombre)
            val provId = proveedorId.ifBlank { "prov_$cleanKey" }
            if (provId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar el documento del proveedor a eliminar."))
            }

            // Verificación fresca de facturas pendientes en el servidor (no confiar en la UI que puede estar desactualizada)
            val facturasSnap = FarmadonPaths.comprasFacturas(db, clienteId, sucursalId)
                .whereEqualTo("proveedorId", provId)
                .get().await()
            val facturasPendientesReales = facturasSnap.documents.count { doc ->
                val estado = doc.getString("estadoPago") ?: "PENDIENTE"
                estado != "PAGADA" && estado != "PAGADO" && estado != "ANULADA"
            }
            if (facturasPendientesReales > 0) {
                return Result.failure(Exception(
                    "No puedes eliminar este proveedor: tiene $facturasPendientesReales factura(s) pendiente(s) de pago. Liquidar primero."
                ))
            }

            val docRef = FarmadonPaths.proveedores(db, clienteId, sucursalId).document(provId)

            // Candado preventivo: un proveedor con pedidos EN CAMINO no se puede borrar —
            // su factura y su recepción necesitan su ficha viva. Se bloquea ANTES con la
            // verdad completa, jamás a medias.
            val pedidosVivos = FarmadonPaths.pedidosCompra(db, clienteId, sucursalId)
                .whereIn("estado", listOf("ENVIADO", "ENTREGA_PARCIAL"))
                .get().await()
            val pedidosDeEsteProveedor = pedidosVivos.documents.count { d ->
                (d.getString("proveedorId") ?: "") == provId ||
                    (d.getString("proveedorNombre") ?: "").equals(proveedorNombre.trim(), ignoreCase = true)
            }
            if (pedidosDeEsteProveedor > 0) {
                return Result.failure(Exception(
                    "No puedes eliminar este proveedor: tiene $pedidosDeEsteProveedor pedido(s) esperando mercadería. Recíbelos o cancélalos primero."
                ))
            }
            // Igual para productos afiliados: borrar la ficha dejaría productos apuntando
            // a un fantasma. El cambio de proveedor se hace producto por producto.
            val productosAfiliados = FarmadonPaths.inventario(db, clienteId, sucursalId)
                .whereEqualTo("proveedorId", provId)
                .limit(1)
                .get().await()
            if (!productosAfiliados.isEmpty) {
                return Result.failure(Exception(
                    "No puedes eliminar este proveedor: todavía tiene productos afiliados. Primero cambia esos productos a otro proveedor desde su ficha."
                ))
            }
            // Legado: productos viejos solo guardan el nombre (campo "proveedor").
            // Sin este candado el borrado pasa y esos productos quedan huérfanos.
            val productosLegado = FarmadonPaths.inventario(db, clienteId, sucursalId)
                .whereEqualTo("proveedor", proveedorNombre.trim())
                .limit(1)
                .get().await()
            if (!productosLegado.isEmpty) {
                return Result.failure(Exception(
                    "No puedes eliminar este proveedor: todavía tiene productos afiliados por nombre (fichas antiguas). Primero cambia esos productos a otro proveedor desde su ficha."
                ))
            }
            // Reclamos abiertos al proveedor: eliminarlo dejaría el expediente sin dueño.
            val reclamosAbiertos = FarmadonPaths.reclamosProveedores(db, clienteId, sucursalId)
                .whereEqualTo("proveedorId", provId)
                .whereIn("estado", listOf("EN_REVISION_DROGUERIA", "EN_REVISION"))
                .limit(1)
                .get().await()
            if (!reclamosAbiertos.isEmpty) {
                return Result.failure(Exception(
                    "No puedes eliminar este proveedor: tiene un reclamo abierto en revisión. Ciérralo o resuélvelo primero."
                ))
            }
            // Transacción: el saldo se relee y se borra en el mismo acto (un cambio de
            // saldo a favor concurrente jamás queda huérfano ni se elimina por error).
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val saldoAFavor = snap.getDouble("saldoAFavor") ?: 0.0
                if (kotlin.math.abs(saldoAFavor) > 0.01) {
                    throw IllegalStateException(
                        "No puedes eliminar este proveedor: tiene un saldo a favor de ${String.format(java.util.Locale.US, "%.2f", saldoAFavor)} pendiente de recuperar. Primero úsalo en una compra o decláralo perdido."
                    )
                }
                tx.delete(docRef)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando proveedor: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Afiliación manual de un producto a un proveedor (desde Reposición → "Sin proveedor").
     * Escribe el proveedor en el documento del producto; la escucha en vivo lo refleja al instante.
     */
    suspend fun vincularProducto(
        productoId: String,
        proveedorId: String,
        proveedorNombre: String
    ): Result<Unit> {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (productoId.isBlank()) return Result.failure(IllegalArgumentException("El producto es obligatorio."))
        if (proveedorId.isBlank() || proveedorNombre.isBlank()) {
            return Result.failure(IllegalArgumentException("Selecciona un proveedor real para afiliar el producto."))
        }
        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, f, s)
            val productRef = tiendaRef.collection("inventario").document(productoId)
            val auditRef = tiendaRef.collection("auditorias").document("inventario")
                .collection("productos").document()
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw IllegalStateException("El producto ya no existe en inventario.")

                // Regla del historial: los lotes, facturas y pedidos YA ocurridos guardan
                // al proveedor de su momento (inmutable — eso jamás se reescribe). Lo que
                // cambia es el proveedor de LAS PRÓXIMAS compras. Se conserva el anterior
                // en la ficha para que mañana nadie pregunte "¿desde cuándo cambió?".
                val anteriorNombre = (snap.getString("proveedorNombre") ?: snap.getString("proveedor") ?: "").trim()
                val anteriorId = (snap.getString("proveedorId") ?: "").trim()
                val actorEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

                @Suppress("UNCHECKED_CAST")
                val historial = (snap.get("historialProveedores") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                if (anteriorId.isNotBlank() && anteriorId != proveedorId) {
                    historial.add(
                        mapOf(
                            "proveedorId" to anteriorId,
                            "proveedorNombre" to anteriorNombre,
                            "hastaEl" to com.google.firebase.Timestamp.now(),
                            "cambiadoPorEmail" to actorEmail
                        )
                    )
                }
                val updates = mutableMapOf<String, Any>(
                    "proveedor" to proveedorNombre.trim(),
                    "proveedorNombre" to proveedorNombre.trim(),
                    "proveedorId" to proveedorId,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                if (historial.isNotEmpty()) updates["historialProveedores"] = historial
                tx.update(productRef, updates)

                // Auditoría atómica junto al cambio (mismo acto, jamás un cambio sin nombre).
                tx.set(auditRef, mapOf(
                    "evento" to "CAMBIO_PROVEEDOR_PRODUCTO",
                    "productoId" to productoId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "proveedorAnteriorId" to anteriorId,
                    "proveedorAnteriorNombre" to anteriorNombre,
                    "proveedorNuevoId" to proveedorId,
                    "proveedorNuevoNombre" to proveedorNombre.trim(),
                    "usuarioEmail" to actorEmail,
                    "fecha" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error vinculando producto $productoId a $proveedorNombre: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Desvinculación de un producto: limpia el proveedor asignado para que vuelva a "Sin proveedor".
     * Preserva el historial anterior en la ficha del producto y registra el evento de auditoría.
     */
    suspend fun desvincularProducto(productoId: String): Result<Unit> {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (productoId.isBlank()) return Result.failure(IllegalArgumentException("El producto es obligatorio."))
        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, f, s)
            val productRef = tiendaRef.collection("inventario").document(productoId)
            val auditRef = tiendaRef.collection("auditorias").document("inventario")
                .collection("productos").document()
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw IllegalStateException("El producto ya no existe en inventario.")

                val anteriorNombre = (snap.getString("proveedorNombre") ?: snap.getString("proveedor") ?: "").trim()
                val anteriorId = (snap.getString("proveedorId") ?: "").trim()
                val actorEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

                @Suppress("UNCHECKED_CAST")
                val historial = (snap.get("historialProveedores") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                if (anteriorId.isNotBlank() || anteriorNombre.isNotBlank()) {
                    historial.add(
                        mapOf(
                            "proveedorId" to anteriorId,
                            "proveedorNombre" to anteriorNombre,
                            "hastaEl" to com.google.firebase.Timestamp.now(),
                            "cambiadoPorEmail" to actorEmail
                        )
                    )
                }
                val updates = mutableMapOf<String, Any>(
                    "proveedor" to "",
                    "proveedorNombre" to "",
                    "proveedorId" to "",
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                if (historial.isNotEmpty()) updates["historialProveedores"] = historial
                tx.update(productRef, updates)

                tx.set(auditRef, mapOf(
                    "evento" to "DESVINCULAR_PROVEEDOR_PRODUCTO",
                    "productoId" to productoId,
                    "productoNombre" to (snap.getString("nombre") ?: ""),
                    "proveedorAnteriorId" to anteriorId,
                    "proveedorAnteriorNombre" to anteriorNombre,
                    "usuarioEmail" to actorEmail,
                    "fecha" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error desvinculando producto $productoId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
