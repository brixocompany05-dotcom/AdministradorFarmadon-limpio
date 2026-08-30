package com.app.administradorfarmadon.compras.pagos.logica

import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS

/**
 * Etiqueta legible de una cuenta de pago configurada, para mostrarla en
 * cualquier pantalla (abono, recepción…): "Yape · 987-111", "Transferencia · BCP 0011".
 * Una sola verdad: nadie vuelve a inventar el formato en cada pantalla.
 */
object EtiquetaMetodoPago {

    /**
     * ¿Este método deja constancia con número de operación?
     * El efectivo NO; Yape, Plin, Transferencia y Cheque SÍ.
     */
    fun requiereOperacion(etiquetaMetodo: String): Boolean {
        return TIPOS_PAGO_FIJOS.any { tipo ->
            tipo.requiereOperacion && etiquetaMetodo.startsWith(tipo.nombre)
        }
    }

    /** ¿Esta cuenta configurada sirve para PAGAR A UN PROVEEDOR en esta sucursal? */
    fun esValidaParaProveedor(instancia: InstanciaPago): Boolean {
        if (!instancia.activa) return false
        return TIPOS_PAGO_FIJOS.firstOrNull { it.id == instancia.tipoId }?.paraPagoProveedores == true
    }

    fun deInstancia(instancia: InstanciaPago): String {
        val tipo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == instancia.tipoId }
        val base = tipo?.nombre ?: instancia.tipoId
        val detalle = if (tipo != null) instancia.nombreLegible(tipo) else ""
        return if (detalle.isBlank() || detalle == "Sin datos · solo referencia") base else "$base · $detalle"
    }

    /** Opciones base del sistema (solo métodos que sirven para pagar a un proveedor). */
    val baseParaProveedores: List<String>
        get() = TIPOS_PAGO_FIJOS.filter { it.paraPagoProveedores }.map { it.nombre }
}
