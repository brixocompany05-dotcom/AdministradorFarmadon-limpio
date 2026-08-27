package com.app.administradorfarmadon.autenticacion.login.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.compartido.sha256
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class ErrorVerificacionListaNegra(cause: Throwable) : Exception("Error al verificar lista negra", cause)

object ListaNegraRepository {

    private const val TAG = "ListaNegraRepository"

    /**
     * Consulta si un RUC o un Email están en la lista negra de BrixoPanel.
     * Devuelve el documento (con el motivo del baneo) si lo encuentra.
     */
    suspend fun consultar(db: FirebaseFirestore, ruc: String, email: String): DocumentSnapshot? {
        return try {
            // 1. Chequeo por Email (hasheado)
            if (email.isNotBlank()) {
                val emailLimpio = email.trim().lowercase()
                val emailHash = emailLimpio.sha256()
                val emailDoc = AuthPaths.listaNegra(db).document(emailHash).get(Source.SERVER).await()
                if (emailDoc.exists()) return emailDoc
            }

            // 2. Chequeo por RUC
            if (ruc.isNotBlank()) {
                val rucDoc = AuthPaths.listaNegra(db).document(ruc.trim()).get(Source.SERVER).await()
                if (rucDoc.exists()) return rucDoc
            }

            null
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                    Log.w(TAG, "Verificación de lista negra no disponible (${e.code}). Se continúa; el bloqueo real es server-side.")
                    null
                }
                else -> throw ErrorVerificacionListaNegra(e)
            }
        } catch (e: Throwable) {
            throw ErrorVerificacionListaNegra(e)
        }
    }
}
