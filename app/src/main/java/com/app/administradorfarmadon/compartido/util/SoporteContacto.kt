package com.app.administradorfarmadon.compartido.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Canal íšNICO de contacto con soporte BRIXO (fuente única de verdad).
 * El número y el correo viven SOLO aquí: cambiar el contacto toca este
 * archivo y nada más (login y registro lo consumen igual).
 *
 * Cadena honesta de fallback: WhatsApp ──†’ correo ──†’ aviso visible si no hay
 * ninguna app disponible (jamás un fallo silencioso).
 */
object SoporteContacto {

    private const val WHATSAPP_PHONE = "51900000000"
    private const val EMAIL = "soporte@brixo.pe"
    private const val TAG = "FARMADON_UI"

    /**
     * @param origen          De dónde viene el pedido: "Login" o "Registro".
     * @param tipoIncidente   Nombre técnico del incidente para el detalle.
     * @param tituloIncidente Título legible del incidente (asunto del correo).
     * @param mensaje         Mensaje mostrado al usuario en pantalla.
     * @param usuario         Correo del usuario que escribe.
     */
    fun abrir(
        context: Context,
        origen: String,
        tipoIncidente: String,
        tituloIncidente: String,
        mensaje: String,
        usuario: String
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val cuerpo = "Hola Viora, necesito ayuda con Farmadon ($origen).\n\n" +
                "Detalles:\n" +
                "- Incidente: $tipoIncidente\n" +
                "- Usuario: $usuario\n" +
                "- Fecha: $timestamp\n" +
                "- Error: $mensaje"

        val whatsappUrl = "https://api.whatsapp.com/send?phone=$WHATSAPP_PHONE&text=${Uri.encode(cuerpo)}"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
        } catch (e: Exception) {
            Log.w(TAG, "WhatsApp no disponible, fallback email: ${e.message}", e)
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "Incidente de $origen: $tituloIncidente")
                putExtra(Intent.EXTRA_TEXT, cuerpo)
            }
            try {
                context.startActivity(Intent.createChooser(emailIntent, "Contactar soporte vía..."))
            } catch (fallbackError: Exception) {
                Log.w(TAG, "Fallo con Toast visible: ${fallbackError.message}", fallbackError)
                Toast.makeText(context, "No se encontró una aplicación para contactar soporte", Toast.LENGTH_LONG).show()
            }
        }
    }
}
