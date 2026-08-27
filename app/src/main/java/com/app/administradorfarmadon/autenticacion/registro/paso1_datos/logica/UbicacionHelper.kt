package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import java.util.*

object UbicacionHelper {
    fun obtenerDireccion(
        context: Context,
        latitud: Double,
        longitud: Double,
        onResultado: (String) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitud, longitud, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        onResultado(addresses[0].getAddressLine(0))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitud, longitud, 1)
                if (!addresses.isNullOrEmpty()) {
                    onResultado(addresses[0].getAddressLine(0))
                }
            }
        } catch (e: Exception) {
            Log.e("FARMADON_REGISTRO", "Error en reverse geocoding: ${e.message}")
        }
    }
}
