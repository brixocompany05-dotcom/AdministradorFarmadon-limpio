package com.app.administradorfarmadon.compartido

import kotlinx.coroutines.delay

/**
 * Utilidades para operaciones de persistencia resilientes.
 */
object PersistenceResilience {

    /**
     * Ejecuta una operacion suspendida con reintentos exponenciales.
     * Ideal para combatir fallos de red momentaneos en Firebase.
     *
     * @param isRecoverable Predicado configurable para determinar si un error es recuperable.
     *                      Permite a diferentes callers inyectar lógica de recuperación específica.
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        isRecoverable: (Throwable) -> Boolean = ::defaultIsRecoverable,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentAttempt = 0
        var currentDelay = initialDelay

        while (currentAttempt < maxAttempts) {
            val result = block()
            if (result.isSuccess) return result
            
            val error = result.exceptionOrNull()

            // 🛡️ BUG FIX #4: Predicado configurable — cubre conflictos de transacción por defecto.
            if (error == null || !isRecoverable(error)) {
                return result
            }

            currentAttempt++
            if (currentAttempt < maxAttempts) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        
        return Result.failure(Exception("Operacion fallida por inestabilidad de red tras $maxAttempts intentos."))
    }

    /**
     * Predicado de recuperación por defecto.
     * Cubre fallos de red, timeouts, y conflictos de transacción de Firebase.
     */
    private fun defaultIsRecoverable(error: Throwable): Boolean {
        val msg = error.message?.lowercase() ?: ""
        return msg.contains("network") ||
               msg.contains("timeout") ||
               msg.contains("unavailable") ||
               msg.contains("deadline") ||
               msg.contains("conflict") ||
               msg.contains("transaction") ||
               msg.contains("overridden") ||
               msg.contains("operation_failed") ||
               error.javaClass.simpleName.contains("TransaccionConflict", ignoreCase = true)
    }
}
