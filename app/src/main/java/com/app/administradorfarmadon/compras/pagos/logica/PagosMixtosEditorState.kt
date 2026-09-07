package com.app.administradorfarmadon.compras.pagos.logica

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle
import java.util.Locale

/**
 * Estado y reglas del PAGO MIXTO. CERO UI: la pantalla solo dibuja lo que esto dice.
 *
 * Responsabilidades:
 * - Mantener las porciones (método + monto + operación) que la persona va editando.
 * - NINGÚN método preseleccionado: la persona elige el primero y puede añadir más.
 * - Abono PARCIAL: la persona paga lo que quiera (puede ser en partes), pero ninguna
 *   porción ni la suma pueden pasarse del saldo pendiente de la factura.
 * - Exponer [pagos] (lista real lista para guardar) y [estadoVerificacion].
 */
class PagosMixtosEditorState(
    val opcionesMetodo: List<String>,
    pagosIniciales: List<PagoDetalle>,
    /** Tope máximo: el saldo pendiente de la factura. El pago puede ser menor. */
    val montoMaximo: Double,
    /** Si es true, al seleccionar un solo método se le asigna automáticamente el 100% sin pedir escribir el monto. */
    val autoCompletarTotalUnicoMetodo: Boolean = false
) {

    /** Una fila editable: un método con su monto (y operación opcional). */
    data class FilaPago(
        val id: Int,
        val metodo: String
    ) {
        // Campos observables: cada tecla dispara recomposición (jamás campos "bloqueados").
        var montoTexto by mutableStateOf("")
        var operacion by mutableStateOf("")
    }

    val filas = mutableStateListOf<FilaPago>()
    var montoMaximoActual by mutableStateOf(montoMaximo)
        private set

    private var proximoId: Int

    init {
        val iniciales = if (pagosIniciales.isNotEmpty()) {
            pagosIniciales.mapIndexed { i, p ->
                FilaPago(i, p.metodoPago).apply {
                    montoTexto = String.format(Locale.US, "%.2f", p.monto)
                    operacion = p.numeroOperacion
                }
            }
        } else {
            // Sin método preseleccionado: la persona elige el primero (los abonos pueden ser parciales).
            emptyList()
        }
        filas.addAll(iniciales)
        proximoId = (iniciales.maxOfOrNull { it.id } ?: -1) + 1
        if (autoCompletarTotalUnicoMetodo && filas.size == 1 && filas[0].montoTexto.isBlank()) {
            filas[0].montoTexto = if (montoMaximoActual > 0) String.format(Locale.US, "%.2f", montoMaximoActual) else ""
        }
    }

    fun limpiar() {
        filas.clear()
        proximoId = 0
    }

    fun actualizarMontoMaximo(nuevoMonto: Double) {
        montoMaximoActual = nuevoMonto
        if (autoCompletarTotalUnicoMetodo && filas.size == 1) {
            filas[0].montoTexto = if (nuevoMonto > 0) String.format(Locale.US, "%.2f", nuevoMonto) else ""
        }
    }

    fun cambiarMonto(id: Int, texto: String) {
        filas.firstOrNull { it.id == id }?.montoTexto = texto.filter { c -> c.isDigit() || c == '.' || c == ',' }
    }

    /** Monto máximo que puede asignarse a una porción: el total de la factura. */
    fun montoMaximoParaFila(id: Int): Double = montoMaximoActual

    fun cambiarOperacion(id: Int, operacion: String) {
        filas.firstOrNull { it.id == id }?.operacion = operacion
    }

    fun agregarFila() {
        val disponibles = opcionesMetodo.filter { opcion -> filas.none { it.metodo == opcion } }
        val metodoNuevo = disponibles.firstOrNull() ?: return
        filas.add(FilaPago(proximoId, metodoNuevo))
        proximoId++
    }

    /** Agrega un método concreto elegido por la persona (un solo paso, sin dropdown). */
    fun agregarMetodoEspecifico(metodo: String) {
        if (filas.any { it.metodo == metodo }) return
        val nuevaFila = FilaPago(proximoId, metodo)
        proximoId++
        filas.add(nuevaFila)
        if (autoCompletarTotalUnicoMetodo) {
            if (filas.size == 1) {
                nuevaFila.montoTexto = if (montoMaximoActual > 0) String.format(Locale.US, "%.2f", montoMaximoActual) else ""
            } else if (filas.size == 2 && filas[0].montoTexto == String.format(Locale.US, "%.2f", montoMaximoActual)) {
                // Pasó de método único (100% automático) a pago repartido:
                // Se limpian los montos para que el usuario escriba la distribución limpia de cada método
                // sin falsas alarmas de sobrepago.
                filas.forEach { it.montoTexto = "" }
            }
        }
    }

    /**
     * Quita un método del reparto, INCLUSO si es el último: quedar sin métodos es un
     * estado honesto ("elige al menos un método"), jamás una trampa silenciosa.
     */
    fun quitarPorMetodo(metodo: String) {
        filas.removeAll { it.metodo == metodo }
        if (autoCompletarTotalUnicoMetodo && filas.size == 1) {
            filas[0].montoTexto = if (montoMaximoActual > 0) String.format(Locale.US, "%.2f", montoMaximoActual) else ""
        }
    }

    fun quitarFila(id: Int) {
        filas.removeAll { it.id == id }
        if (autoCompletarTotalUnicoMetodo && filas.size == 1) {
            filas[0].montoTexto = if (montoMaximoActual > 0) String.format(Locale.US, "%.2f", montoMaximoActual) else ""
        }
    }

    val metodosDisponiblesParaAgregar: List<String>
        get() = opcionesMetodo.filter { opcion -> filas.none { it.metodo == opcion } }

    /** Suma real de las porciones que la persona escribió. */
    val sumaPorciones: Double
        get() = filas.sumOf { it.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0 }

    /** Ninguna porción individual puede superar el total de la factura. */
    val algunaPorcionExcede: Boolean
        get() = filas.any { fila ->
            val montoFila = fila.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
            montoFila > montoMaximoActual + 0.01
        }

    /** La suma de porciones jamás puede exceder el total. */
    val sumaExcedeTotal: Boolean
        get() = sumaPorciones > montoMaximoActual + 0.01

    /** El pago es válido cuando hay al menos un método con monto y nada se pasa del tope. */
    val cuadra: Boolean
        get() = filas.isNotEmpty() && sumaPorciones > 0.0 && !algunaPorcionExcede && !sumaExcedeTotal

    /**
     * Lista REAL lista para guardar: solo porciones con monto mayor a cero y EXACTAMENTE
     * lo que la persona escribió (jamás un monto recortado en silencio; si se pasa del
     * tope, [cuadra] bloquea el guardado con mensaje visible).
     */
    val pagos: List<PagoDetalle>
        get() = filas.mapNotNull { fila ->
            val montoFila = fila.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (montoFila > 0.0) {
                PagoDetalle(
                    metodoPago = fila.metodo,
                    monto = montoFila,
                    numeroOperacion = fila.operacion.trim().uppercase()
                )
            } else null
        }

    /** Mensaje honesto del estado de la distribución (para pintar en vivo). */
    val estadoVerificacion: String
        get() {
            if (montoMaximoActual <= 0.0) return ""
            if (filas.isEmpty()) return "Elige al menos un método de pago"
            if (filas.size == 1 && autoCompletarTotalUnicoMetodo) {
                return "✓ Pago total al contado con ${filas[0].metodo}: " + formatear(montoMaximoActual)
            }
            if (algunaPorcionExcede) {
                val fila = filas.firstOrNull { (it.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0) > montoMaximoActual + 0.01 }
                if (fila != null) return "El monto en ${fila.metodo} supera el total (${formatear(montoMaximoActual)})"
            }
            if (sumaExcedeTotal) return "El pago excede el total por " + formatear(sumaPorciones - montoMaximoActual)
            if (sumaPorciones <= 0.0) return "Escribe el monto para cada método elegido"
            val restante = montoMaximoActual - sumaPorciones
            if (restante > 0.01) {
                return "Distribuido: " + formatear(sumaPorciones) + " · Falta asignar: " + formatear(restante)
            }
            return "✓ Total distribuido correctamente (" + formatear(sumaPorciones) + ")"
        }

    private fun formatear(valor: Double): String =
        String.format(Locale.US, "%.2f", valor)
}
