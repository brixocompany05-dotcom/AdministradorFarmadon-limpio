package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos

data class Paso1UiState(
    val nombreFarmacia: String = "",
    val dueno: String = "",
    val email: String = "",
    val contrasena: String = "",
    val confirmarContrasena: String = "",
    val telefono: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val notaAclaratoria: String = "",
    // País de operación: NACE VACíO —” el aspirante ELIGE primero y el resto
    // del formulario permanece bloqueado hasta elegir. La moneda se deriva.
    val paisIso: String = "",
    val monedaIso: String = "",
    val monedaSimbolo: String = "",
    val erroresCampos: Map<String, String> = emptyMap()
)

