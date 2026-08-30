package com.app.administradorfarmadon.suscripcion

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComprobanteCompartidoData(
    val imageUri: Uri? = null,
    val textContent: String = "",
    val mimeType: String = "",
    val esDesdeIntentExterno: Boolean = false
)

/**
 * Gestor global para capturar comprobantes bancarios compartidos desde afuera (Yape, BCP, BBVA, etc.)
 * y presentarlos directamente al usuario en Farmadon sin fricción ni rodeos.
 */
object ReportarPagoManager {
    private const val TAG = "ReportarPagoManager"

    private val _comprobanteCompartido = MutableStateFlow<ComprobanteCompartidoData?>(null)
    val comprobanteCompartido: StateFlow<ComprobanteCompartidoData?> = _comprobanteCompartido.asStateFlow()

    private val _mostrarDialogoManual = MutableStateFlow(false)
    val mostrarDialogoManual: StateFlow<Boolean> = _mostrarDialogoManual.asStateFlow()

    fun abrirDialogoManual() {
        _mostrarDialogoManual.value = true
    }

    fun cerrarDialogo() {
        _comprobanteCompartido.value = null
        _mostrarDialogoManual.value = false
    }

    fun capturarIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val mimeType = intent.type ?: ""
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        }

        Log.d(TAG, "[INTENT] Comprobante recibido - MimeType: $mimeType, Uri: $streamUri, Text: $extraText")

        if (streamUri != null || extraText.isNotBlank()) {
            _comprobanteCompartido.value = ComprobanteCompartidoData(
                imageUri = streamUri,
                textContent = extraText,
                mimeType = mimeType,
                esDesdeIntentExterno = true
            )
        }
    }
}
