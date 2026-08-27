package com.app.administradorfarmadon.inventario.compartido.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
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
        return SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    private fun cleanKey(name: String): String {
        return name.trim().lowercase().replace(" ", "_").replace("/", "-")
    }

    fun observarProveedores(onErrorEscucha: ((String) -> Unit)? = null): Flow<List<Proveedor>> = callbackFlow {
        val clienteId = getClienteId()
        if (clienteId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = FarmadonPaths.proveedores(db, clienteId, SessionManager.sucursalIdEfectiva)
            .orderBy("nombre", Query.Direction.ASCENDING).limit(80)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando proveedores: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: "Sin detalle del servidor")
                trySend(emptyList())
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

                if (nombre.isNotBlank()) {
                    Proveedor(
                        id = id,
                        nombre = nombre,
                        idFiscal = idFiscal,
                        contacto = contacto,
                        telefono = telefono,
                        email = email,
                        direccion = direccion,
                        montoMinimoPedido = montoMinimoPedido
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
        if (proveedor.nombre.isBlank()) return Result.failure(Exception("El nombre del proveedor es obligatorio."))

        return try {
            val cleanKey = cleanKey(proveedor.nombre)
            val provId = proveedor.id.ifBlank {
                if (cleanKey.isNotBlank()) "prov_$cleanKey" else UUID.randomUUID().toString()
            }

            val docRef = FarmadonPaths.proveedores(db, clienteId, SessionManager.sucursalIdEfectiva).document(provId)

            val data = mapOf(
                "id" to provId,
                "nombre" to proveedor.nombre.trim(),
                "idFiscal" to proveedor.idFiscal.trim(),
                "contacto" to proveedor.contacto.trim(),
                "telefono" to proveedor.telefono.trim(),
                "email" to proveedor.email.trim(),
                "direccion" to proveedor.direccion.trim(),
                "montoMinimoPedido" to proveedor.montoMinimoPedido,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )

            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(provId)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando proveedor en Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un proveedor por su ID determinístico.
     * Bloqueado si tiene facturas pendientes de pago (trampa preventiva).
     * La factura es documento legal separado — no se borra, solo se bloquea la eliminación del contacto.
     */
    suspend fun eliminarProveedor(
        clienteId: String,
        sucursalId: String,
        proveedorId: String,
        proveedorNombre: String,
        facturasPendientesCount: Int
    ): Result<Unit> {
        if (proveedorNombre.isBlank()) return Result.failure(Exception("Nombre de proveedor inválido."))
        if (facturasPendientesCount > 0) {
            return Result.failure(Exception(
                "No puedes eliminar este proveedor: tiene $facturasPendientesCount factura(s) pendiente(s) de pago. Liquidar primero."
            ))
        }

        return try {
            val cleanKey = cleanKey(proveedorNombre)
            val provId = proveedorId.ifBlank { "prov_$cleanKey" }
            if (provId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar el documento del proveedor a eliminar."))
            }
            val docRef = FarmadonPaths.proveedores(db, clienteId, sucursalId).document(provId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando proveedor: ${e.message}", e)
            Result.failure(e)
        }
    }
}
