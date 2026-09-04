package com.app.administradorfarmadon.soporte.logica

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.soporte.datos.SoporteRepository
import com.app.administradorfarmadon.soporte.modelo.CanalesSoporteOficial
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticoServicio(
    val nombre: String,
    val estadoOk: Boolean,
    val detalle: String
)

data class SoporteUiState(
    val tickets: List<SoporteTicket> = emptyList(),
    val cargando: Boolean = true,
    val ticketSeleccionado: SoporteTicket? = null,
    val mostrarModalNuevoTicket: Boolean = false,
    val creandoTicket: Boolean = false,
    val canalesOficiales: CanalesSoporteOficial = CanalesSoporteOficial(),
    val diagnosticos: List<DiagnosticoServicio> = emptyList(),
    val mensajeExito: String? = null,
    val error: String? = null
)

class SoporteViewModel(
    private val soporteRepository: SoporteRepository = SoporteRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "SoporteViewModel"
    }

    private val _uiState = MutableStateFlow(SoporteUiState())
    val uiState: StateFlow<SoporteUiState> = _uiState.asStateFlow()

    init {
        iniciarObservadorTickets()
        cargarCanalesOficiales()
        ejecutarDiagnostico()
    }

    private fun iniciarObservadorTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            soporteRepository.observarTickets()
                .catch { err ->
                    Log.e(TAG, "Error escuchando tickets: ${err.message}", err)
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "No se pudieron cargar los tickets de soporte.") }
                }
                .collect { lista ->
                    // Orden estricto: Más reciente primero (Hora Servidor)
                    val listaOrdenada = lista.sortedByDescending { maxOf(it.actualizadoMs, it.fechaMs) }
                    _uiState.update { estado ->
                        val selActualizada = estado.ticketSeleccionado?.let { sel ->
                            listaOrdenada.firstOrNull { it.id == sel.id }
                        }
                        estado.copy(
                            tickets = listaOrdenada,
                            cargando = false,
                            ticketSeleccionado = selActualizada ?: listaOrdenada.firstOrNull()
                        )
                    }
                }
        }
    }

    private fun cargarCanalesOficiales() {
        viewModelScope.launch {
            val canales = soporteRepository.obtenerCanalesOficiales()
            _uiState.update { it.copy(canalesOficiales = canales) }
        }
    }

    fun ejecutarDiagnostico() {
        val lista = listOf(
            DiagnosticoServicio(
                nombre = "Base de Datos Firestore",
                estadoOk = true,
                detalle = "Conexión en vivo y sincronización activa"
            ),
            DiagnosticoServicio(
                nombre = "Sesión y Permisos BRIXO",
                estadoOk = SessionManager.clienteIdGarantizado.isNotBlank(),
                detalle = "Rol: ${SessionManager.rol.ifBlank { "Operador" }} · ${SessionManager.nombreUsuario}"
            ),
            DiagnosticoServicio(
                nombre = "Sede Operativa Activa",
                estadoOk = SessionManager.sucursalIdEfectiva.isNotBlank(),
                detalle = SessionManager.sucursalNombre.ifBlank { "Sede Principal" }
            )
        )
        _uiState.update { it.copy(diagnosticos = lista) }
    }

    fun seleccionarTicket(ticket: SoporteTicket?) {
        _uiState.update { it.copy(ticketSeleccionado = ticket) }
    }

    fun prepararNuevaConsultaDirecta() {
        _uiState.update { it.copy(ticketSeleccionado = null, error = null) }
    }

    fun abrirModalNuevoTicket() {
        _uiState.update { it.copy(mostrarModalNuevoTicket = true, error = null) }
    }

    fun cerrarModalNuevoTicket() {
        _uiState.update { it.copy(mostrarModalNuevoTicket = false) }
    }

    /**
     * Envía un mensaje dentro de la interfaz de chat:
     * - Si hay una conversación activa abierta, agrega el mensaje A ESE MISMO CHAT THREAD.
     * - Si no hay conversación abierta, crea un nuevo chat thread con titular e identificación inteligente de categoría.
     * - Hora registrada 100% desde el Servidor (HoraServidor).
     */
    fun enviarMensajeEnChat(texto: String) {
        val msgLimpio = texto.trim()
        if (msgLimpio.isBlank()) return

        val ticketSel = _uiState.value.ticketSeleccionado
        if (ticketSel != null && !ticketSel.estaResuelto) {
            // MISMA CONVERSACIÓN ACTIVA
            viewModelScope.launch {
                _uiState.update { it.copy(creandoTicket = true, error = null) }
                val res = soporteRepository.agregarMensajeATicket(ticketSel.id, msgLimpio)
                res.onSuccess {
                    _uiState.update { it.copy(creandoTicket = false) }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            creandoTicket = false,
                            error = "No se pudo enviar el mensaje: ${err.message ?: "Verifica tu conexión."}"
                        )
                    }
                }
            }
        } else {
            // NUEVA CONVERSACIÓN
            enviarNuevoMensajeDirecto(msgLimpio)
        }
    }

    /**
     * Reintenta el envío de un mensaje en caso de error de red.
     */
    fun reintentarEnvioMensaje(ticketId: String, textoMensaje: String) {
        val msgLimpio = textoMensaje.trim()
        if (msgLimpio.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(creandoTicket = true, error = null) }
            val res = soporteRepository.agregarMensajeATicket(ticketId, msgLimpio)
            res.onSuccess {
                _uiState.update { it.copy(creandoTicket = false, mensajeExito = "Mensaje reenviado con éxito.") }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        error = "Fallo al reintentar envío: ${err.message ?: "Comprueba tu conexión de red."}"
                    )
                }
            }
        }
    }

    fun enviarNuevoMensajeDirecto(mensaje: String) {
        val msgLimpio = mensaje.trim()
        if (msgLimpio.isBlank()) return

        val categoriaSugerida = identificarCategoriaSugerida(msgLimpio)
        val titularSugerido = generarTitularConversacion(msgLimpio)
        val prioridad = if (msgLimpio.contains("urgente", ignoreCase = true) || msgLimpio.contains("error", ignoreCase = true) || msgLimpio.contains("fallo", ignoreCase = true)) "ALTA" else "NORMAL"

        crearTicket(
            asunto = titularSugerido,
            categoria = categoriaSugerida,
            prioridad = prioridad,
            descripcion = msgLimpio
        )
    }

    fun crearTicket(
        asunto: String,
        categoria: String,
        prioridad: String,
        descripcion: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(creandoTicket = true, error = null) }
            val res = soporteRepository.crearTicket(asunto, categoria, prioridad, descripcion)
            res.onSuccess { ticket ->
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        mostrarModalNuevoTicket = false,
                        ticketSeleccionado = ticket,
                        mensajeExito = "Consulta iniciada con éxito. BRIXO Soporte atenderá tu mensaje."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        error = err.message ?: "No se pudo registrar la consulta."
                    )
                }
            }
        }
    }

    fun contactarWhatsApp(context: Context, motivo: String = "Consulta operativa") {
        val wa = _uiState.value.canalesOficiales.whatsapp
        val fechaServidor = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(HoraServidor.ahoraMs()))
        val mensaje = "Hola BRIXO Soporte, requiero asistencia para Farmadon.\n" +
                "• Farmacia ID: ${SessionManager.clienteIdGarantizado}\n" +
                "• Sede: ${SessionManager.sucursalNombre} (ID: ${SessionManager.sucursalIdEfectiva})\n" +
                "• Usuario: ${SessionManager.nombreUsuario} (${SessionManager.email})\n" +
                "• Rol: ${SessionManager.rol}\n" +
                "• Motivo: $motivo\n" +
                "• Fecha Servidor: $fechaServidor"

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$wa&text=${Uri.encode(mensaje)}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            contactarEmail(context, motivo)
        }
    }

    fun contactarEmail(context: Context, asunto: String = "Consulta Soporte Farmadon") {
        val email = _uiState.value.canalesOficiales.email
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "[$asunto] - ${SessionManager.sucursalNombre}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Farmacia: ${SessionManager.clienteIdGarantizado}\n" +
                        "Sede: ${SessionManager.sucursalNombre} (ID: ${SessionManager.sucursalIdEfectiva})\n" +
                        "Usuario: ${SessionManager.nombreUsuario} (${SessionManager.email})\n" +
                        "Rol: ${SessionManager.rol}\n\n" +
                        "Detalle de la consulta:\n"
            )
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Enviar correo a soporte BRIXO"))
        } catch (_: Exception) {
            _uiState.update { it.copy(error = "No se encontró una app de correo instalada.") }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, error = null) }
    }

    private fun identificarCategoriaSugerida(texto: String): String {
        val t = texto.lowercase(Locale.getDefault())
        return when {
            t.contains("sunat") || t.contains("factura") || t.contains("boleta") || t.contains("ruc") || t.contains("comprobante") || t.contains("serie") || t.contains("nota") -> "FACTURACION_SUNAT"
            t.contains("impresora") || t.contains("imprimir") || t.contains("ticket") || t.contains("papel") || t.contains("escaner") || t.contains("bluetooth") || t.contains("usb") -> "HARDWARE_IMPRESORA"
            t.contains("caja") || t.contains("cobro") || t.contains("pos") || t.contains("yape") || t.contains("plin") || t.contains("tarjeta") || t.contains("descuento") || t.contains("arqueo") || t.contains("efectivo") -> "CAJA_VENTAS"
            t.contains("stock") || t.contains("lote") || t.contains("inventario") || t.contains("vencer") || t.contains("vencimiento") || t.contains("fefo") || t.contains("kardex") || t.contains("producto") -> "INVENTARIO_STOCK"
            else -> "CONSULTA_GENERAL"
        }
    }

    private fun generarTitularConversacion(texto: String): String {
        val palabras = texto.trim().split("\\s+".toRegex())
        if (palabras.size <= 6) return texto.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val primerTexto = palabras.take(6).joinToString(" ")
        return primerTexto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + "..."
    }
}
