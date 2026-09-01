package com.app.administradorfarmadon.inventario.ajustesinventario.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.ajustesinventario.datos.AjusteInventarioRepository
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Ajuste de inventario NO comercial (muestras, donaciones, mermas, etc.).
 * Proceso separado de la compra: no crea factura, no toca pedidos ni cuentas por pagar.
 */
class AjusteInventarioViewModel(
    private val repository: AjusteInventarioRepository = AjusteInventarioRepository()
) : ViewModel() {

    private data class OperacionPendiente(val idem: String, val clave: String)

    private var entradaPendiente: OperacionPendiente? = null
    private var salidaPendiente: OperacionPendiente? = null
    private var reintentoEntrada: (() -> Unit)? = null
    private var reintentoSalida: (() -> Unit)? = null

    data class AjusteInventarioUiState(
        val producto: MoldeProductos? = null,
        val mostrandoDialogo: Boolean = false,
        val procesando: Boolean = false,
        val mensajeError: String? = null,
        val exito: Boolean = false
    )

    private val _state = MutableStateFlow(AjusteInventarioUiState())
    val state: StateFlow<AjusteInventarioUiState> = _state.asStateFlow()

    fun abrir(producto: MoldeProductos) {
        entradaPendiente = null
        salidaPendiente = null
        reintentoEntrada = null
        reintentoSalida = null
        _state.update {
            it.copy(
                producto = producto,
                mostrandoDialogo = true,
                procesando = false,
                mensajeError = null,
                exito = false
            )
        }
    }

    fun cerrar() {
        if (_state.value.procesando) return
        _state.update { it.copy(mostrandoDialogo = false) }
    }

    /** Al salir del workspace de entrada se reinicia TODO: la próxima vez arranca desde la decisión. */
    fun reiniciar() {
        entradaPendiente = null
        salidaPendiente = null
        reintentoEntrada = null
        reintentoSalida = null
        _state.update { AjusteInventarioUiState() }
    }

    private fun idemPara(clave: String, pendiente: OperacionPendiente?): OperacionPendiente =
        if (pendiente?.clave == clave) pendiente else OperacionPendiente(UUID.randomUUID().toString(), clave)

    fun registrarEntrada(
        loteNumero: String,
        vencimiento: String,
        cantidad: Double,
        tipo: String,
        motivo: String,
        onSuccess: () -> Unit = {}
    ) {
        val p = _state.value.producto ?: return
        if (_state.value.procesando) return
        val op = idemPara("E|$loteNumero|$vencimiento|$cantidad|$tipo|$motivo", entradaPendiente)
        entradaPendiente = op
        reintentoEntrada = { registrarEntrada(loteNumero, vencimiento, cantidad, tipo, motivo) }
        _state.update { it.copy(procesando = true, mensajeError = null, exito = false) }
        viewModelScope.launch {
            val res = repository.registrarEntradaNoCompra(
                productId = p.indice,
                productoNombre = p.nombre,
                empaque = p.empaque,
                loteNumero = loteNumero,
                vencimiento = vencimiento,
                cantidad = cantidad,
                tipo = tipo,
                motivo = motivo,
                usuarioEmail = SessionManager.email,
                usuarioNombre = SessionManager.nombreUsuario,
                idempotenciaId = op.idem
            )
            res.fold(
                onSuccess = {
                    entradaPendiente = null
                    _state.update { it.copy(procesando = false, mensajeError = null, exito = true) }
                    viewModelScope.launch {
                        delay(1800)
                        _state.update { it.copy(exito = false, mostrandoDialogo = false) }
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update { it.copy(procesando = false, exito = false, mensajeError = e.message ?: "No se pudo registrar la entrada. Verifica tu conexión e inténtalo de nuevo.") }
                }
            )
        }
    }

    fun registrarSalida(
        loteNumero: String,
        cantidad: Double,
        tipo: String,
        motivo: String,
        onSuccess: () -> Unit = {}
    ) {
        val p = _state.value.producto ?: return
        if (_state.value.procesando) return
        val op = idemPara("S|$loteNumero|$cantidad|$tipo|$motivo", salidaPendiente)
        salidaPendiente = op
        reintentoSalida = { registrarSalida(loteNumero, cantidad, tipo, motivo) }
        _state.update { it.copy(procesando = true, mensajeError = null, exito = false) }
        viewModelScope.launch {
            val res = repository.registrarSalidaAjuste(
                productId = p.indice,
                productoNombre = p.nombre,
                loteNumero = loteNumero,
                cantidad = cantidad,
                tipo = tipo,
                motivo = motivo,
                usuarioEmail = SessionManager.email,
                usuarioNombre = SessionManager.nombreUsuario,
                idempotenciaId = op.idem
            )
            res.fold(
                onSuccess = {
                    salidaPendiente = null
                    _state.update { it.copy(procesando = false, mensajeError = null, exito = true) }
                    viewModelScope.launch {
                        delay(1800)
                        _state.update { it.copy(exito = false, mostrandoDialogo = false) }
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update { it.copy(procesando = false, exito = false, mensajeError = e.message ?: "No se pudo registrar la salida. Verifica tu conexión e inténtalo de nuevo.") }
                }
            )
        }
    }

    /** Reintenta la última operación fallida con la misma clave (la idempotencia evita duplicar). */
    fun reintentar() {
        when {
            reintentoEntrada != null -> reintentoEntrada!!.invoke()
            reintentoSalida != null -> reintentoSalida!!.invoke()
        }
    }
}
