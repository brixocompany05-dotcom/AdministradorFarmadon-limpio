package com.app.administradorfarmadon.clientes.modelo

/**
 * MODELO DEL DIRECTORIO DE CLIENTES DE LA FARMACIA (R1/R3/R12: cero campos ficticios).
 *
 * El documento vive en: farmacias/{farmaciaId}/clientes/{numeroDocumento}
 * El ID del documento es el mismo número de documento (DNI de 8 dígitos o RUC de 11 dígitos),
 * garantizando unicidad física a nivel de base de datos sin duplicados entre terminales.
 */
data class ClienteFarmacia(
    val id: String = "",
    val tipoDocumento: String = "DNI", // DNI | RUC
    val numeroDocumento: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val creadoPor: String = "",
    val fechaMs: Long = 0L
) {
    val esDni: Boolean get() = tipoDocumento == "DNI"
    val esRuc: Boolean get() = tipoDocumento == "RUC"
}
