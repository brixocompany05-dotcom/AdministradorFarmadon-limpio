package com.app.administradorfarmadon.facturacion.envio.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.app.administradorfarmadon.facturacion.envio.datos.FacturacionEnvioRepository

class FacturacionEnvioWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "FacturacionEnvioWorker"
        const val KEY_FARMACIA_ID = "farmacia_id"

        /**
         * Encola la sincronización de contingencia con restricción obligatoria de red.
         * WORK_NAME único por farmacia para no pisar ejecuciones entre tenants (R1).
         */
        fun encolarReintento(context: Context, farmaciaId: String) {
            val idLimpio = farmaciaId.trim()
            if (idLimpio.isBlank()) return

            val workName = "work_facturacion_contingencia_$idLimpio"
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FacturacionEnvioWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FARMACIA_ID to idLimpio))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Worker de contingencia fiscal encolado para farmacia $idLimpio con nombre $workName")
        }
    }

    override suspend fun doWork(): Result {
        val farmaciaId = inputData.getString(KEY_FARMACIA_ID).orEmpty().trim()
        if (farmaciaId.isBlank()) {
            return Result.failure()
        }

        return try {
            val repository = FacturacionEnvioRepository()
            val reporte = repository.enviarLotePendientes(farmaciaId)
            val exitosos = reporte.exitosos.size
            val enTramite = reporte.enTramite.size
            val atencion = reporte.requierenAtencion.size
            Log.i(TAG, "Worker fiscal farmacia $farmaciaId: $exitosos aceptados, $enTramite en trámite, $atencion requieren atención")

            // Regla: Solo reintentar si hubo fallas transitorias de red o excepciones que ameriten reintento en segundo plano.
            // Si los fallos son definitivos (RECHAZADO quemado, INVÁLIDO), no reintentar en bucle infinito.
            val tieneFallasRed = reporte.requierenAtencion.any { it.estado == "FALLA_RED" || it.estado == "EXCEPCIÓN" }
            if (tieneFallasRed || enTramite > 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red/servidor ejecutando worker de contingencia: ${e.message}", e)
            Result.retry()
        }
    }
}
