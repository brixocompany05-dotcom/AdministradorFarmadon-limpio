package com.app.administradorfarmadon.compras.saldoafavor.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Operaciones del saldo a favor que NO pasan por la recepción:
 * cobrar en efectivo (el proveedor devuelve la plata) y declarar perdido.
 * Cada una es una transacción todo-o-nada con historial de justificación.
 */
class SaldoAFavorOperacionesRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "SaldoAFavorOperaciones"
        const val TIPO_COBRADO = "SALDO_COBRADO_EFECTIVO"
        const val TIPO_PERDIDO = "SALDO_DECLARADO_PERDIDO"
    }

    suspend fun registrarEgresoSaldo(
        proveedorId: String,
        monto: Double,
        tipo: String,
        documento: String,
        motivo: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        }
        if (proveedorId.isBlank()) return Result.failure(IllegalArgumentException("El proveedor es obligatorio."))
        if (monto <= 0.0) return Result.failure(IllegalArgumentException("El monto debe ser mayor a 0."))
        if (motivo.trim().length < 5) {
            return Result.failure(IllegalArgumentException("El motivo debe tener al menos 5 caracteres para auditoría."))
        }
        if (tipo == TIPO_COBRADO && documento.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Indica el documento o comprobante del cobro."))
        }

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val fechaLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ahoraMs))
            val ref = SaldoAFavorFirestore.refSaldo(db, farmaciaId, sucursalId, proveedorId)
            db.runTransaction { tx ->
                SaldoAFavorFirestore.registrarEgresoEnTransaccion(
                    tx = tx,
                    refSaldo = ref,
                    monto = monto,
                    tipo = tipo,
                    documento = documento,
                    motivo = motivo,
                    usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" },
                    usuarioEmail = SessionManager.email,
                    ahoraMs = ahoraMs,
                    fechaLegible = fechaLegible,
                    idempotenciaId = idempotenciaId
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando egreso de saldo a favor: ${e.message}", e)
            Result.failure(e)
        }
    }
}
