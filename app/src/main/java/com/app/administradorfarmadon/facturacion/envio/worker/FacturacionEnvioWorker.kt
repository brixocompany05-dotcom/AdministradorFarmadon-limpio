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
        private const val WORK_NAME = "work_facturacion_contingencia"

        /**
         * Encola la sincronización de documentos pendientes con restricción obligatoria de red.
         * Si no hay internet, WorkManager espera automáticamente a que regrese la conexión.
         */
        fun encolarReintento(context: Context, farmaciaId: String) {
            if (farmaciaId.isBlank()) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FacturacionEnvioWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FARMACIA_ID to farmaciaId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Worker de contingencia fiscal encolado para farmacia $farmaciaId")
        }
    }

    override suspend fun doWork(): Result {
        val farmaciaId = inputData.getString(KEY_FARMACIA_ID).orEmpty()
        if (farmaciaId.isBlank()) {
            return Result.failure()
        }

        return try {
            val repository = FacturacionEnvioRepository()
            val reporte = repository.enviarLotePendientes(farmaciaId)
            val exitosos = reporte.exitosos.size
            val fallidos = reporte.requierenAtencion.size
            Log.i(TAG, "Sincronización en segundo plano terminada: $exitosos exitosos, $fallidos pendientes/fallidos")

            if (fallidos > 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando worker de contingencia: ${e.message}", e)
            Result.retry()
        }
    }
}
