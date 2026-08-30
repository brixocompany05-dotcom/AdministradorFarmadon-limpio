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
    val montoMaximo: Double
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
    }

    fun actualizarMontoMaximo(nuevoMonto: Double) {
        montoMaximoActual = nuevoMonto
    }

    fun cambiarMonto(id: Int, texto: String) {
        filas.firstOrNull { it.id == id }?.montoTexto = texto.filter { c -> c.isDigit() || c == '.' || c == ',' }
    }

    /** Monto máximo que puede asignarse a una porción: jamás más que el total. */
    fun montoMaximoParaFila(id: Int): Double {
        val fila = filas.firstOrNull { it.id == id } ?: return 0.0
        val otras = filas.filter { it.id != id }.sumOf { it.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0 }
        return (montoMaximoActual - otras).coerceAtLeast(0.0)
    }

    fun cambiarOperacion(id: Int, operacion: String) {
        filas.firstOrNull { it.id == id }?.operacion = operacion
    }

    fun agregarFila() {
        val disponibles = opcionesMetodo.filter { opcion -> filas.none { it.metodo == opcion } }
        val metodoNuevo = disponibles.firstOrNull() ?: return
        // Al añadir otro método se abre vacío: la persona decide cuánto (abono en partes).
        filas.add(FilaPago(proximoId, metodoNuevo))
        proximoId++
    }

    /** Agrega un método concreto elegido por la persona (un solo paso, sin dropdown). */
    fun agregarMetodoEspecifico(metodo: String) {
        if (filas.any { it.metodo == metodo }) return
        // Se abre vacío: la persona escribe cuánto pagó con este método.
        filas.add(FilaPago(proximoId, metodo))
        proximoId++
    }

    /** Quita un método del reparto. */
    fun quitarPorMetodo(metodo: String) {
        if (filas.size <= 1) return
        filas.removeAll { it.metodo == metodo }
    }

    fun quitarFila(id: Int) {
        if (filas.size <= 1) return
        filas.removeAll { it.id == id }
    }

    val metodosDisponiblesParaAgregar: List<String>
        get() = opcionesMetodo.filter { opcion -> filas.none { it.metodo == opcion } }

    /** Suma real de las porciones que la persona escribió. */
    val sumaPorciones: Double
        get() = filas.sumOf { it.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0 }

    /** Ninguna porción puede pasarse del total (regla estricta). */
    val algunaPorcionExcede: Boolean
        get() = filas.any { fila ->
            val montoFila = fila.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
            montoFila > montoMaximoParaFila(fila.id) + 0.01
        }

    /** La suma de porciones jamás puede exceder el total. */
    val sumaExcedeTotal: Boolean
        get() = sumaPorciones > montoMaximoActual + 0.01

    /** El pago es válido cuando hay al menos un método con monto y nada se pasa del tope. */
    val cuadra: Boolean
        get() = filas.isNotEmpty() && sumaPorciones > 0.0 && !algunaPorcionExcede && !sumaExcedeTotal

    /** Lista REAL lista para guardar: solo porciones con monto mayor a cero. */
    val pagos: List<PagoDetalle>
        get() = filas.mapNotNull { fila ->
            val montoFila = fila.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (montoFila > 0.0) {
                PagoDetalle(
                    metodoPago = fila.metodo,
                    monto = if (montoFila > montoMaximoParaFila(fila.id)) montoMaximoParaFila(fila.id) else montoFila,
                    numeroOperacion = fila.operacion.trim().uppercase()
                )
            } else null
        }

    /** Mensaje honesto del estado de la distribución (para pintar en vivo). */
    val estadoVerificacion: String
        get() {
            if (montoMaximoActual <= 0.0) return ""
            if (filas.isEmpty()) return "Elige al menos un método de pago"
            if (algunaPorcionExcede) {
                val fila = filas.firstOrNull { it.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0 > montoMaximoParaFila(it.id) + 0.01 }
                if (fila != null) return "Este método no puede pagar más de " + formatear(montoMaximoParaFila(fila.id))
            }
            if (sumaExcedeTotal) return "El pago excede el saldo por " + formatear(sumaPorciones - montoMaximoActual)
            if (sumaPorciones <= 0.0) return "Falta escribir el monto de este método"
            return "Pago indicado: " + formatear(sumaPorciones) + " · Queda por pagar: " + formatear((montoMaximoActual - sumaPorciones).coerceAtLeast(0.0))
        }

    private fun formatear(valor: Double): String =
        String.format(Locale.US, "%.2f", valor)
}
