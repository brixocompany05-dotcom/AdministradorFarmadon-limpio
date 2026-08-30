package com.app.administradorfarmadon.organizacion.datos

/**
 * LA VERDAD íšNICA de países operativos BRIXO —” tabla idéntica en ambos repos
 * (Panel: com.brixo.panel.planes.modelo.CatalogoPaises).
 *
 * REGLA DE PRODUCTO:
 * - La moneda y el prefijo telefónico NACEN del país. Jamás se editan.
 * - Documento: se quitan espacios/puntos/guiones ──†’ longitud en [docMin..docMax]
 *   y tipo correcto (solo números o alfanumérico). La verificación fuerte del
 *   documento real sigue siendo el agente BRIXO.
 * - País nuevo = UNA fila nueva aquí Y una en el Panel. Nunca antes.
 */
data class PaisInfo(
    val iso: String,
    val nombre: String,
    val banderaEmoji: String,
    val monedaIso: String,
    val simboloMoneda: String,
    val prefijoTel: String,
    val documentoNombre: String,
    val docMin: Int,
    val docMax: Int,
    val docSoloNumeros: Boolean
)

object CatalogoPaises {

    val TODOS = listOf(
        PaisInfo("AR", "Argentina", "\uD83C\uDDE6\uD83C\uDDF7", "ARS", "$", "+54", "DNI", 7, 8, true),
        PaisInfo("CL", "Chile", "\uD83C\uDDE8\uD83C\uDDF1", "CLP", "$", "+56", "RUT", 8, 9, false),
        PaisInfo("CO", "Colombia", "\uD83C\uDDE8\uD83C\uDDF4", "COP", "$", "+57", "Cédula / NIT", 6, 11, true),
        PaisInfo("EC", "Ecuador", "\uD83C\uDDEA\uD83C\uDDE8", "USD", "$", "+593", "Cédula", 10, 10, true),
        PaisInfo("ES", "España", "\uD83C\uDDEA\uD83C\uDDF8", "EUR", "──‚¬", "+34", "DNI / NIE", 9, 10, false),
        PaisInfo("PE", "Perú", "\uD83C\uDDF5\uD83C\uDDEA", "PEN", "S/", "+51", "DNI / RUC", 8, 11, true),
        PaisInfo("VE", "Venezuela", "\uD83C\uDDFB\uD83C\uDDEA", "VES", "Bs.", "+58", "RIF", 9, 10, false)
    )

    fun todos(): List<PaisInfo> = TODOS

    fun porIso(iso: String): PaisInfo? = TODOS.find { it.iso == iso.uppercase().trim() }

    fun existe(iso: String): Boolean = porIso(iso) != null

    /** Nombre legible para selectores y avisos (fallback: el ISO tal cual). */
    fun nombreLegible(iso: String): String = porIso(iso)?.nombre ?: iso

    /** La moneda NACE del país. Desconocido ──†’ vacío honesto, nunca inventar. */
    fun monedaDe(iso: String): Pair<String, String> =
        porIso(iso)?.let { it.monedaIso to it.simboloMoneda } ?: ("" to "")

    /** Prefijo telefónico del país (texto fijo del campo teléfono). */
    fun prefijoTel(iso: String): String = porIso(iso)?.prefijoTel ?: ""

    /**
     * Normaliza teléfonos legados que guardaban el prefijo pegado ("+51987—¦"):
     * el prefijo ahora es visual y fijo, el valor debe ser solo número local.
     */
    fun telefonoSinPrefijo(iso: String, telefono: String): String {
        var t = telefono.trim().filter { it.isDigit() || it == '+' }
        val prefijo = prefijoTel(iso)
        if (prefijo.isNotBlank() && t.startsWith(prefijo)) t = t.removePrefix(prefijo)
        return t.removePrefix("+").trim()
    }
}
