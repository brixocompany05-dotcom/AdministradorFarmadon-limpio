package com.app.administradorfarmadon.inventario.compartido.modelo

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Proveedor(
    val id: String = "",
    val nombre: String = "",
    val idFiscal: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val montoMinimoPedido: Double = 0.0
)
