package com.app.administradorfarmadon.clientes.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * REPOSITORIO DEL DIRECTORIO DE CLIENTES (R1/R3/R8).
 *
 * El directorio pertenece a la farmacia (todas las sucursales comparten la ficha del cliente).
 * La clave del documento es su número de documento oficial (DNI o RUC), imposibilitando
 * duplicaciones transaccionales desde múltiples terminales.
 */
class ClientesRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val ventasRepository: VentasRepository = VentasRepository()
) {
    companion object {
        private const val TAG = "ClientesRepository"
    }

    private fun farmaciaId(): String? = SessionManager.clienteIdGarantizado.takeIf { it.isNotBlank() }

    /**
     * Observa en vivo la lista completa de clientes del directorio de la farmacia.
     */
    fun observarClientes(): Flow<List<ClienteFarmacia>> = callbackFlow {
        val fId = farmaciaId() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        val ref = FarmadonPaths.clientesDirectorio(db, fId)
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando clientes: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { doc ->
                parseCliente(doc.id, doc.data)
            }?.sortedBy { it.nombre } ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Busca un cliente directamente por su DNI o RUC (búsqueda puntual por llave).
     */
    suspend fun buscarClientePorDocumento(numeroDocumento: String): Result<ClienteFarmacia?> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val docLimpio = numeroDocumento.trim()
        if (docLimpio.isBlank()) return Result.success(null)

        return try {
            val snap = FarmadonPaths.clientesDirectorio(db, fId).document(docLimpio).get().await()
            if (snap.exists()) {
                Result.success(parseCliente(snap.id, snap.data))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando cliente por documento: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda o edita un cliente con validación estricta y clave unívoca anti-duplicado.
     */
    suspend fun guardarCliente(cliente: ClienteFarmacia): Result<ClienteFarmacia> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))

        val tipo = cliente.tipoDocumento.trim().uppercase()
        val num = cliente.numeroDocumento.trim()
        val nombre = cliente.nombre.trim()

        if (nombre.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre o razón social es obligatorio."))
        }

        if (tipo == "DNI") {
            if (!Regex("^\\d{8}$").matches(num)) {
                return Result.failure(IllegalArgumentException("El DNI debe contener exactamente 8 dígitos numéricos."))
            }
        } else if (tipo == "RUC") {
            if (!Regex("^\\d{11}$").matches(num)) {
                return Result.failure(IllegalArgumentException("El RUC debe contener exactamente 11 dígitos numéricos."))
            }
        } else {
            return Result.failure(IllegalArgumentException("Tipo de documento inválido: debe ser DNI o RUC."))
        }

        val docId = num
        val ahoraMs = HoraServidor.ahoraMs()

        return try {
            val docRef = FarmadonPaths.clientesDirectorio(db, fId).document(docId)
            val data = mapOf(
                "id" to docId,
                "tipoDocumento" to tipo,
                "numeroDocumento" to num,
                "nombre" to nombre,
                "telefono" to cliente.telefono.trim(),
                "direccion" to cliente.direccion.trim(),
                "notas" to cliente.notas.trim(),
                "creadoPor" to (cliente.creadoPor.ifBlank { SessionManager.nombreUsuario }),
                "fechaMs" to if (cliente.fechaMs > 0) cliente.fechaMs else ahoraMs,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )
            docRef.set(data, SetOptions.merge()).await()

            Result.success(
                cliente.copy(
                    id = docId,
                    tipoDocumento = tipo,
                    numeroDocumento = num,
                    nombre = nombre,
                    fechaMs = if (cliente.fechaMs > 0) cliente.fechaMs else ahoraMs
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando cliente: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un cliente del directorio.
     */
    suspend fun eliminarCliente(clienteId: String): Result<Unit> {
        val fId = farmaciaId() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (clienteId.isBlank()) return Result.failure(IllegalArgumentException("ID de cliente no válido."))

        return try {
            FarmadonPaths.clientesDirectorio(db, fId).document(clienteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando cliente: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa el historial de compras efectuadas por el cliente en la sucursal actual.
     */
    fun observarHistorialVentas(clienteId: String): Flow<List<Venta>> {
        return ventasRepository.observarHistorialCliente(clienteId)
    }

    private fun parseCliente(id: String, data: Map<String, Any>?): ClienteFarmacia? {
        if (data == null) return null
        return try {
            ClienteFarmacia(
                id = id,
                tipoDocumento = data["tipoDocumento"] as? String ?: "DNI",
                numeroDocumento = data["numeroDocumento"] as? String ?: id,
                nombre = data["nombre"] as? String ?: "",
                telefono = data["telefono"] as? String ?: "",
                direccion = data["direccion"] as? String ?: "",
                notas = data["notas"] as? String ?: "",
                creadoPor = data["creadoPor"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando cliente $id: ${e.message}", e)
            null
        }
    }
}
