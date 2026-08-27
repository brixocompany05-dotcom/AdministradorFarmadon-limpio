package com.app.administradorfarmadon.compras.recepcion.logica

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega
import com.app.administradorfarmadon.compras.datos.LoteExistenteVista
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Estado y lógica de la recepción física de mercadería.
 * ZERO UI — solo datos, cálculos y reglas. La pantalla solo dibuja lo que esto dice.
 */
class RecepcionMercaderiaEstado(
    pedido: PedidoCompra,
    val indiceLotes: Map<String, List<LoteExistenteVista>> = emptyMap()
) {
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var numeroFactura by mutableStateOf("")
        private set
    var condicionPago by mutableStateOf("Contado")
        private set
    var diasCredito by mutableStateOf<Int?>(null)
        private set
    var montoFacturaManual by mutableStateOf("")
        private set
    var decisionFaltante by mutableStateOf("PARCIAL")
        private set
    var errorGeneral by mutableStateOf<String?>(null)
        private set

    val items = pedido.items.map { item ->
        ItemEdicionFila(
            productoId = item.productoId,
            productoNombre = item.productoNombre,
            presentacion = item.presentacion,
            cantidadPedida = item.cantidad,
            cantidadPrevia = item.cantidadRecibida,
            saldoPendiente = item.saldoPendiente,
            precioCompraInicial = item.precioCompra
        )
    }

    fun onFacturaChanged(valor: String) {
        numeroFactura = valor.uppercase()
        errorGeneral = null
    }

    fun onCondicionPagoChanged(condicion: String) { condicionPago = condicion }
    fun onDiasCreditoChanged(dias: Int) { diasCredito = dias }
    fun onMontoFacturaChanged(valor: String) { montoFacturaManual = valor }
    fun onDecisionFaltanteChanged(decision: String) { decisionFaltante = decision }
    fun onErrorMostrado() { errorGeneral = null }
    fun setError(msg: String) { errorGeneral = msg }

    // ── CÁLCULOS EN VIVO ──
    val unidadesCompradas: Int get() = items.sumOf { it.cantidadRecibir.toIntOrNull() ?: 0 }
    val unidadesRegalo: Int get() = items.sumOf { it.bonificacionGratis.toIntOrNull() ?: 0 }
    val totalCostoCalculado: Double get() = items.sumOf {
        (it.cantidadRecibir.toIntOrNull() ?: 0) * (it.costoUnitario.toDoubleOrNull() ?: 0.0)
    }
    val totalFacturaFinal: Double
        get() = montoFacturaManual.toDoubleOrNull()?.takeIf { it > 0 } ?: totalCostoCalculado

    val hayFaltantes: Boolean get() = items.any { it.tieneFaltante }
    val algoPorRecibir: Boolean get() = items.any { it.totalHoy > 0 }
    val productosConSobrante: Int get() = items.count { it.esSobrante }
    val lotesRepetidosNuevos: List<String>
        get() = items.mapNotNull { it.loteNumero.trim().uppercase().takeIf { s -> s.isNotBlank() && !indiceLotes.containsKey(s) } }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList()

    // ── PREVENCIÓN ACTIVA: el botón se bloquea solo y dice qué falta ──
    val puedeAsentar: Boolean
        get() = algoPorRecibir && numeroFactura.isNotBlank() && !faltaPlazoCredito
    val razonesBloqueo: List<String>
        get() = buildList {
            if (!algoPorRecibir) add("Escribe las unidades que están llegando hoy")
            if (numeroFactura.isBlank()) add("Falta el N° de factura del proveedor")
            if (faltaPlazoCredito) add("Elige los días de crédito")
        }
    val faltaPlazoCredito: Boolean
        get() = condicionPago == "Crédito" && (diasCredito ?: 0) <= 0

    fun fechaPagoCredito(): String? {
        if (condicionPago != "Crédito") return null
        val plazo = diasCredito ?: return null
        // Hora del servidor: la fecha de vencimiento jamás depende del reloj del celular.
        val cal = Calendar.getInstance().apply {
            timeInMillis = HoraServidor.ahoraMs()
            add(Calendar.DAY_OF_YEAR, plazo)
        }
        return sdf.format(cal.time)
    }

    /** Construye la lista final para asentar. Retorna null si alguna regla falla. */
    fun construirItemsAAsentar(): List<ItemRecepcionEntrega>? {
        val resultado = mutableListOf<ItemRecepcionEntrega>()
        for (fila in items) {
            if (fila.saldoPendiente <= 0) continue
            val cant = fila.cantidadRecibir.toIntOrNull() ?: 0
            val bonif = fila.bonificacionGratis.toIntOrNull() ?: 0
            val tot = cant + bonif
            if (tot <= 0) continue

            if (fila.loteNumero.trim().isBlank()) {
                setError("Ingresa el lote físico para '${fila.productoNombre}'.")
                return null
            }
            val vto = FechaVencimientoHelper.normalizar(fila.vencimiento.trim()) ?: fila.vencimiento.trim()
            val diasVto = if (vto.isBlank()) null else FechaVencimientoHelper.diasHastaVencer(vto)
            if (diasVto == null) {
                setError("La fecha de vencimiento para '${fila.productoNombre}' no es válida.")
                return null
            }
            if (diasVto <= 0) {
                setError("La fecha de vencimiento '${fila.vencimiento}' de '${fila.productoNombre}' ya está vencida.")
                return null
            }
            val costUn = fila.costoUnitario.toDoubleOrNull() ?: 0.0
            if (cant > 0 && costUn <= 0.0) {
                setError("Digita el costo unitario real de '${fila.productoNombre}'. Mercadería sin costo no se puede asentar.")
                return null
            }
            resultado.add(ItemRecepcionEntrega(
                productoId = fila.productoId,
                productoNombre = fila.productoNombre,
                presentacion = fila.presentacion,
                loteNumero = fila.loteNumero.trim().uppercase(),
                vencimiento = vto,
                cantidadComprada = cant,
                bonificacionGratis = bonif,
                cantidadTotal = tot,
                costoUnitarioReal = costUn,
                costoTotalReal = cant * costUn
            ))
        }
        if (resultado.isEmpty() && decisionFaltante != "AJUSTE") {
            setError("Debes ingresar al menos 1 producto con cantidad mayor a cero.")
            return null
        }
        if (resultado.all { it.cantidadComprada == 0 }) {
            setError("Los regalos acompañan una compra: incluye al menos una unidad comprada.")
            return null
        }
        return resultado.toList()
    }

    /** Fila editable de un producto en la tabla de recepción. */
    class ItemEdicionFila(
        val productoId: String,
        val productoNombre: String,
        val presentacion: String,
        val cantidadPedida: Int,
        val cantidadPrevia: Int,
        val saldoPendiente: Int,
        precioCompraInicial: Double
    ) {
        var cantidadRecibir by mutableStateOf(if (saldoPendiente > 0) saldoPendiente.toString() else "")
        var loteNumero by mutableStateOf("")
        var vencimiento by mutableStateOf("")
        var costoUnitario by mutableStateOf(if (precioCompraInicial > 0) String.format(Locale.US, "%.2f", precioCompraInicial) else "")
        var bonificacionGratis by mutableStateOf("")

        val esCompleta: Boolean get() = saldoPendiente <= 0
        val tieneFaltante: Boolean get() = saldoPendiente > 0
        val esSobrante: Boolean get() = (cantidadPrevia + (cantidadRecibir.toIntOrNull() ?: 0)) > cantidadPedida
        val totalHoy: Int get() = (cantidadRecibir.toIntOrNull() ?: 0) + (bonificacionGratis.toIntOrNull() ?: 0)

        // Inteligencia de lote contra inventario
        fun coincidenciasLote(indice: Map<String, List<LoteExistenteVista>>): List<LoteExistenteVista> {
            val key = loteNumero.trim().uppercase()
            return if (key.isBlank()) emptyList() else indice[key].orEmpty()
        }
    }
}
