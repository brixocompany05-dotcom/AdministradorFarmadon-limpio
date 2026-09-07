package com.app.administradorfarmadon.configuracion.metodospago.modelo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tipos de pago FIJOS del sistema. La farmacia NO crea ni renombra tipos:
 * solo activa/desactiva, rellena sus propios datos y usa referencia libre.
 * Así se evitan duplicados, mala escritura y ediciones que rompen el historial.
 */
data class CampoTipoPago(
    val id: String,
    val etiqueta: String,
    val placeholder: String,
    val esNumerico: Boolean = false
)

data class TipoPagoFijo(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val icono: ImageVector,
    val colorMarca: Color,
    val campos: List<CampoTipoPago>,
    /**
     * Tipos ÚNICOS (Efectivo, POS): no manejan cuentas ni datos.
     * Solo se encienden o apagan; no tiene sentido pedir "cuenta" para el
     * dinero en caja ni para el punto de venta.
     */
    val unica: Boolean = false,
    /**
     * Coherencia de negocio: ¿este método sirve para PAGAR A UN PROVEEDOR?
     * - Efectivo, Yape, Plin, Transferencia, Cheque → sí.
     * - Tarjeta (POS) → NO: el POS cobra a clientes; un proveedor no se paga
     *   pasando la tarjeta por nuestro punto de venta.
     */
    val paraPagoProveedores: Boolean = true,
    /**
     * Realidad de cada pago: ¿este método tiene número de operación?
     * - Efectivo NO: el dinero en mano no genera operación.
     * - Yape, Plin, Transferencia, Cheque SÍ: dejan constancia con número.
     */
    val requiereOperacion: Boolean = false
) {
    val permiteSoloReferencia: Boolean get() = true
}

/**
 * Una cuenta/canal concreto de un tipo fijo (ej: Transferencia → BCP cuenta 0011…,
 * Yape → número 987…). Cada instancia se activa, edita o elimina por separado,
 * y cada movimiento guarda cuál se usó: el dinero no se mezcla en una sola bolsa.
 */
data class InstanciaPago(
    val id: String = "",
    val farmaciaId: String = "",
    val sucursalId: String = "",
    val tipoId: String = "",
    val activa: Boolean = true,
    val datos: Map<String, String> = emptyMap()
) {
    fun nombreLegible(tipo: TipoPagoFijo): String {
        val valores = tipo.campos.mapNotNull { c -> datos[c.id]?.takeIf { it.isNotBlank() } }
        return if (valores.isEmpty()) "Sin datos · solo referencia" else valores.joinToString(" · ")
    }
}

private val verdeEfectivo = Color(0xFF2E7D32)
private val violetaYape = Color(0xFF7B2FA0)
private val azulPlin = Color(0xFF0B7AC9)
private val azulTransferencia = Color(0xFF1E6FD9)
private val indigoTarjeta = Color(0xFF5C6BC0)
private val cafeCheque = Color(0xFF8D6E63)

val TIPOS_PAGO_FIJOS: List<TipoPagoFijo> = listOf(
    TipoPagoFijo(
        id = "EFECTIVO",
        nombre = "Efectivo",
        descripcion = "Dinero en caja. Solo referencia si lo deseas.",
        icono = Icons.Outlined.Payments,
        colorMarca = verdeEfectivo,
        campos = emptyList(),
        unica = true
    ),
    TipoPagoFijo(
        id = "YAPE",
        nombre = "Yape",
        descripcion = "Billetera móvil del proveedor o cliente.",
        icono = Icons.Outlined.Smartphone,
        colorMarca = violetaYape,
        campos = listOf(
            CampoTipoPago("numero", "N° de Yape", "987 654 321", esNumerico = true),
            CampoTipoPago("titular", "Titular", "Nombre del titular")
        )
    ),
    TipoPagoFijo(
        id = "PLIN",
        nombre = "Plin",
        descripcion = "Billetera móvil (Plin).",
        icono = Icons.Outlined.AccountBalanceWallet,
        colorMarca = azulPlin,
        campos = listOf(
            CampoTipoPago("numero", "N° de Plin", "987 654 321", esNumerico = true),
            CampoTipoPago("titular", "Titular", "Nombre del titular")
        )
    ),
    TipoPagoFijo(
        id = "TRANSFERENCIA",
        nombre = "Transferencia",
        descripcion = "Transferencia bancaria (BCP, BBVA, Interbank…).",
        icono = Icons.Outlined.AccountBalance,
        colorMarca = azulTransferencia,
        campos = listOf(
            CampoTipoPago("banco", "Banco", "BCP, BBVA, Interbank…"),
            CampoTipoPago("cuenta", "N° de cuenta", "Número de cuenta o interbancario", esNumerico = true),
            CampoTipoPago("titular", "Titular", "Nombre del titular")
        )
    ),
    TipoPagoFijo(
        id = "TARJETA_POS",
        nombre = "Tarjeta (POS)",
        descripcion = "Pago con tarjeta en el punto de venta.",
        icono = Icons.Outlined.CreditCard,
        colorMarca = indigoTarjeta,
        campos = emptyList(),
        unica = true,
        paraPagoProveedores = false
    ),
    TipoPagoFijo(
        id = "CHEQUE",
        nombre = "Cheque",
        descripcion = "Cheque bancario.",
        icono = Icons.Outlined.ReceiptLong,
        colorMarca = cafeCheque,
        campos = listOf(
            CampoTipoPago("banco", "Banco", "Banco emisor"),
            CampoTipoPago("numeroCheque", "N° de cheque", "Número del cheque", esNumerico = true),
            CampoTipoPago("titular", "Titular", "Nombre del titular")
        ),
        // El cheque sí deja constancia: su número va en el campo de operación.
        requiereOperacion = true
    ),
)
