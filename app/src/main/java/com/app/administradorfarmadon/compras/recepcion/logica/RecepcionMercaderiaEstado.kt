package com.app.administradorfarmadon.compras.recepcion.logica

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega
import com.app.administradorfarmadon.compras.datos.LoteExistenteVista
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.pagos.logica.EtiquetaMetodoPago
import com.app.administradorfarmadon.compras.pagos.logica.PagosMixtosEditorState
import com.app.administradorfarmadon.compras.saldoafavor.SaldoAFavorCalculo
import com.app.administradorfarmadon.compras.saldoafavor.SaldoAFavorLiquidacion
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Estado y lógica de la recepción física de mercadería.
 * ZERO UI —” solo datos, cálculos y reglas. La pantalla solo dibuja lo que esto dice.
 */
class RecepcionMercaderiaEstado(
    private val pedido: PedidoCompra,
    val indiceLotes: Map<String, List<LoteExistenteVista>> = emptyMap(),
    private val facturaExistente: FacturaCompra? = null,
    saldoAFavorDisponible: Double = 0.0,
    metodosPagoConfigurados: List<InstanciaPago> = emptyList()
) {
    val saldoAFavorDisponible: Double = saldoAFavorDisponible.coerceAtLeast(0.0)
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val ultimaRecepcionConFactura = pedido.recepciones.asReversed()
        .firstOrNull { it.numeroFactura.isNotBlank() }
    private val fechaVencimientoExistente = facturaExistente?.fechaVencimientoPago?.takeIf { it.isNotBlank() }
        ?: ultimaRecepcionConFactura?.fechaVencimientoPago?.takeIf { it.isNotBlank() }

    val facturaContinua: Boolean get() = facturaExistente != null || ultimaRecepcionConFactura != null
    val fechaVencimientoPagoVisible: String? get() = fechaVencimientoExistente

    var numeroFactura by mutableStateOf(
        facturaExistente?.numeroFactura?.trim()?.uppercase()
            ?: ultimaRecepcionConFactura?.numeroFactura?.trim()?.uppercase().orEmpty()
    )
        private set
    var condicionPago by mutableStateOf(facturaExistente?.condicionPago ?: ultimaRecepcionConFactura?.condicionPago ?: "Contado")
        private set
    var diasCredito by mutableStateOf<Int?>(null)
        private set
    var montoFacturaManual by mutableStateOf(
        facturaExistente?.montoTotal?.takeIf { it > 0 }
            ?.let { String.format(Locale.US, "%.2f", it) }
            ?: ultimaRecepcionConFactura?.montoFactura?.takeIf { it > 0 }
                ?.let { String.format(Locale.US, "%.2f", it) }
                .orEmpty()
    )
        private set
    var montoPagadoManual by mutableStateOf("0.00")
        private set
    var usarSaldoAFavor by mutableStateOf(false)
        private set
    var decisionFaltante by mutableStateOf("PARCIAL")
        private set
    var errorGeneral by mutableStateOf<String?>(null)
        private set

    /** Métodos REALES de esta sucursal para pagar al proveedor (sin POS: el POS
     * cobra a clientes, no paga proveedores). */
    val opcionesMetodoPago: List<InstanciaPago> = metodosPagoConfigurados
        .filter { EtiquetaMetodoPago.esValidaParaProveedor(it) }

    /** Dueño de la distribución del pago (lógica pura, sin UI). */
    val editorPagos = PagosMixtosEditorState(
        opcionesMetodo = if (opcionesMetodoPago.isNotEmpty()) {
            opcionesMetodoPago.map { EtiquetaMetodoPago.deInstancia(it) }
        } else {
            EtiquetaMetodoPago.baseParaProveedores
        },
        pagosIniciales = emptyList(),
        montoMaximo = 0.0
    )

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
    fun onMontoFacturaChanged(valor: String) { montoFacturaManual = valor; errorGeneral = null }
    fun onMontoPagadoChanged(valor: String) {
        montoPagadoManual = valor
        editorPagos.actualizarMontoMaximo(valor.replace(',', '.').toDoubleOrNull() ?: 0.0)
        errorGeneral = null
    }
    fun onUsarSaldoAFavorChanged(usar: Boolean) { usarSaldoAFavor = usar; errorGeneral = null }
    fun onDecisionFaltanteChanged(decision: String) { decisionFaltante = decision }
    fun onErrorMostrado() { errorGeneral = null }
    fun setError(msg: String) { errorGeneral = msg }

    // ──”€──”€ CÁLCULOS EN VIVO ──”€──”€
    val unidadesCompradas: Int get() = items.sumOf { it.cantidadRecibir.toIntOrNull() ?: 0 }
    val unidadesRegalo: Int get() = items.sumOf { it.bonificacionGratis.toIntOrNull() ?: 0 }
    val unidadesRecibidasAntes: Int get() = items.sumOf { it.cantidadPrevia }
    val unidadesEstaEntrega: Int get() = items.sumOf { it.totalHoy }
    val unidadesRecibidasDespues: Int get() = unidadesRecibidasAntes + unidadesEstaEntrega
    val unidadesPendientesDespues: Int get() = items.sumOf {
        (it.saldoPendiente - it.totalHoy).coerceAtLeast(0)
    }
    val totalCostoCalculado: Double get() = items.sumOf {
        (it.cantidadRecibir.toIntOrNull() ?: 0) * (it.costoUnitario.toDoubleOrNull() ?: 0.0)
    }
    val totalFacturaFinal: Double
        get() = montoFacturaManual.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    val montoPagadoFinal: Double
        get() = montoPagadoManual.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: -1.0
    val liquidacionSaldoAFavor: SaldoAFavorLiquidacion
        get() = SaldoAFavorCalculo.liquidacion(saldoAFavorDisponible, usarSaldoAFavor, totalFacturaFinal)
    val saldoAFavorAplicado: Double
        get() = liquidacionSaldoAFavor.aplicado
    val montoPagadoAntes: Double
        get() = facturaExistente?.totalAbonadoReal
            ?: pedido.recepciones.filter { it.numeroFactura.trim().equals(numeroFactura.trim(), ignoreCase = true) }.sumOf { it.montoPagado }
    val saldoFacturaAntesDePago: Double
        get() = ((facturaExistente?.montoTotal ?: 0.0) - (facturaExistente?.totalAbonadoReal ?: 0.0)).coerceAtLeast(0.0)

    val hayFaltantes: Boolean get() = items.any { it.tieneFaltante }
    val algoPorRecibir: Boolean get() = items.any { it.totalHoy > 0 }
    val productosConSobrante: Int get() = items.count { it.esSobrante }
    val lotesRepetidosNuevos: List<String>
        get() = items.mapNotNull { it.loteNumero.trim().uppercase().takeIf { s -> s.isNotBlank() && !indiceLotes.containsKey(s) } }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList()

    // ──”€──”€ PREVENCIí“N ACTIVA: el botón se bloquea solo y dice qué falta ──”€──”€
    val puedeAsentar: Boolean
        get() = algoPorRecibir && numeroFactura.isNotBlank() && totalFacturaFinal > 0.0 && montoPagadoFinal >= 0.0 &&
            montoPagadoFinal <= (saldoFacturaAntesDePago + liquidacionSaldoAFavor.netoAPagar + 0.01) &&
            !faltaPlazoCredito &&
            (montoPagadoFinal <= 0.0 || editorPagos.cuadra)
    val razonesBloqueo: List<String>
        get() = buildList {
            if (!algoPorRecibir) add("Escribe las unidades que están llegando hoy")
            if (numeroFactura.isBlank()) add("Falta el N° de factura del proveedor")
            if (totalFacturaFinal <= 0.0) add("Escribe el total que aparece en la factura")
            if (montoPagadoFinal < 0.0) add("El pago registrado no es válido")
            if (montoPagadoFinal > (saldoFacturaAntesDePago + liquidacionSaldoAFavor.netoAPagar + 0.01)) add("El pago supera el saldo de la factura (después del descuento)")
            if (montoPagadoFinal > 0.0 && !editorPagos.cuadra) add("La distribución del pago no cuadra: reparte el monto entre los métodos")
            if (faltaPlazoCredito) add("Elige los días de crédito")
        }
    val faltaPlazoCredito: Boolean
        get() = condicionPago == "Crédito" && (diasCredito ?: 0) <= 0 && fechaVencimientoExistente.isNullOrBlank()

    fun fechaPagoCredito(): String? {
        if (condicionPago != "Crédito") return null
        fechaVencimientoExistente?.let { return it }
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
