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
import com.app.administradorfarmadon.soporte.modelo.MensajeSoporte
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import java.util.UUID
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
    /** Mensajes nuevos (subcolección) del caso seleccionado, en vivo. */
    val mensajesEnVivo: List<MensajeSoporte> = emptyList(),
    /** Enviados que aún no confirma el servidor (Enviando…/error con reintento). */
    val mensajesOptimistas: List<MensajeSoporte> = emptyList(),
    /** Primera foto del servidor ya llegó: antes, nada a medias. */
    val mensajesListos: Boolean = false,
    val mostrarModalNuevoTicket: Boolean = false,
    val creandoTicket: Boolean = false,
    val canalesOficiales: CanalesSoporteOficial = CanalesSoporteOficial(),
    val diagnosticos: List<DiagnosticoServicio> = emptyList(),
    val mensajeExito: String? = null,
    val error: String? = null,
    /** El caso abierto dejó de existir: se dice, no se esconde. */
    val casoNoDisponible: Boolean = false,
    /** Marca de tiempo del último reporte bien enviado (cierra su diálogo). */
    val ultimoReporteOkMs: Long = 0L
) {
    /** Historial oficial: legacy + vivo + optimistas, sin duplicados, ordenado. */
    val mensajesCombinados: List<MensajeSoporte>
        get() {
            val ticket = ticketSeleccionado
                ?: return com.app.administradorfarmadon.soporte.modelo.combinarHistorialSoporte(
                    emptyList(), mensajesEnVivo, "", 0L, mensajesOptimistas
                )
            return com.app.administradorfarmadon.soporte.modelo.combinarHistorialSoporte(
                ticket.obtenerTodosLosMensajes(), mensajesEnVivo,
                ticket.descripcion, ticket.fechaMs, mensajesOptimistas
            )
        }
}

class SoporteViewModel(
    private val soporteRepository: SoporteRepository = SoporteRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "SoporteViewModel"
    }

    private val _uiState = MutableStateFlow(SoporteUiState())
    val uiState: StateFlow<SoporteUiState> = _uiState.asStateFlow()

    private var observadorMensajesJob: kotlinx.coroutines.Job? = null
    private var observadorTicketsJob: kotlinx.coroutines.Job? = null
    /** Origen para "nueva solicitud relacionada": jamás reabre la cerrada. */
    private var origenRelacionadoId: String? = null
    // Anti doble-toque: mismo texto dos veces seguidas se ignora.
    private var ultimoEnvioTexto: String? = null
    private var ultimoEnvioMs: Long = 0L
    private var seleccionInicialHecha: Boolean = false

    init {
        iniciarObservadorTickets()
        cargarCanalesOficiales()
        ejecutarDiagnostico()
    }

    private fun iniciarObservadorTickets() {
        // Un solo dueño: reintentar nunca duplica escuchas.
        observadorTicketsJob?.cancel()
        observadorTicketsJob = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            soporteRepository.observarTickets()
                .catch { err ->
                    Log.e(TAG, "Error escuchando tickets: ${err.message}", err)
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "No se pudieron cargar tus solicitudes. Revisa tu conexión.") }
                }
                .collect { lista ->
                    // Orden estricto: Más reciente primero (Hora Servidor)
                    val listaOrdenada = lista.sortedByDescending { maxOf(it.actualizadoMs, it.fechaMs) }
                    _uiState.update { estado ->
                        val selPrevia = estado.ticketSeleccionado
                        val selActualizada = selPrevia?.let { sel ->
                            listaOrdenada.firstOrNull { it.id == sel.id }
                        }
                        // Al entrar no se abre ningún chat solo: la persona elige.
                        // El servidor actualiza datos pero jamás mueve la pantalla.
                        // El caso abierto ya no existe: se avisa con salida, no se esconde.
                        val desaparecio = selPrevia != null && selActualizada == null &&
                            !estado.cargando && estado.ticketSeleccionado != null
                        estado.copy(
                            tickets = listaOrdenada,
                            cargando = false,
                            ticketSeleccionado = selActualizada,
                            mensajesEnVivo = if (desaparecio) emptyList() else estado.mensajesEnVivo,
                            mensajesOptimistas = if (desaparecio) emptyList() else estado.mensajesOptimistas,
                            casoNoDisponible = desaparecio
                        )
                    }
                }
        }
    }

    /** Reintento manual cuando la bandeja falló (el error explica y guía). */
    fun reintentarCarga() {
        iniciarObservadorTickets()
    }

    /** Vuelve a suscribir el chat abierto (si tardó o vino vacío, sin cerrar nada). */
    fun recargarChatAbierto() {
        observarMensajesDelSeleccionado(_uiState.value.ticketSeleccionado?.id)
    }

    private fun cargarCanalesOficiales() {
        viewModelScope.launch {
            val canales = soporteRepository.obtenerCanalesOficiales()
            _uiState.update { it.copy(canalesOficiales = canales) }
        }
    }

    /**
     * Solo datos reales de la sesión (nada de "conexión en vivo" inventada:
     * la frescura la da cada listener con su propio estado carga/error).
     */
    fun ejecutarDiagnostico() {
        val lista = listOf(
            DiagnosticoServicio(
                nombre = "Sesión de farmacia",
                estadoOk = SessionManager.clienteIdGarantizado.isNotBlank(),
                detalle = if (SessionManager.clienteIdGarantizado.isNotBlank()) {
                    "Rol: ${SessionManager.rol.ifBlank { "Operador" }} · ${SessionManager.nombreUsuario.ifBlank { "Usuario" }}"
                } else {
                    "Sin sesión activa. Vuelve a ingresar."
                }
            ),
            DiagnosticoServicio(
                nombre = "Sede operativa",
                estadoOk = SessionManager.sucursalIdEfectiva.isNotBlank(),
                detalle = SessionManager.sucursalNombre.ifBlank { SessionManager.sucursalIdEfectiva.ifBlank { "Sin sede" } }
            )
        )
        _uiState.update { it.copy(diagnosticos = lista) }
    }

    fun seleccionarTicket(ticket: SoporteTicket?) {
        observadorMensajesJob?.cancel()
        _uiState.update {
            it.copy(
                ticketSeleccionado = ticket,
                mensajesEnVivo = emptyList(),
                mensajesOptimistas = emptyList(),
                mensajesListos = false,
                casoNoDisponible = false
            )
        }
        observarMensajesDelSeleccionado(ticket?.id)
    }

    /**
     * Marca leído SOLO lo que la persona realmente está viendo (al final del
     * chat). Abierto pero arriba = no leído, como WhatsApp.
     */
    fun marcarLeidoAhora() {
        val sel = _uiState.value.ticketSeleccionado ?: return
        if (sel.mensajesSinLeerFarmacia <= 0) return
        viewModelScope.launch { soporteRepository.marcarConversacionLeida(sel.id) }
    }

    /** Nueva solicitud relacionada con una cerrada (no la reabre). */
    fun prepararNuevaConsultaDesde(origenId: String) {
        observadorMensajesJob?.cancel()
        origenRelacionadoId = origenId.ifBlank { null }
        _uiState.update { it.copy(ticketSeleccionado = null, mensajesEnVivo = emptyList(), mensajesOptimistas = emptyList(), mensajesListos = false, error = null) }
    }

    private fun observarMensajesDelSeleccionado(ticketId: String?) {
        observadorMensajesJob?.cancel()
        if (ticketId.isNullOrBlank()) return
        val esperado = ticketId
        observadorMensajesJob = viewModelScope.launch {
            soporteRepository.observarMensajes(esperado)
                .catch { err ->
                    Log.e(TAG, "Error escuchando mensajes: ${err.message}", err)
                    // Error también cierra la espera: la pantalla muestra
                    // salida (reintentar), jamás un vacío mentiroso.
                    if (_uiState.value.ticketSeleccionado?.id == esperado) {
                        _uiState.update { it.copy(mensajesListos = true) }
                    }
                }
                .collect { vivos ->
                    // Respuesta tardía de otro chat: jamás pinta la pantalla nueva.
                    if (_uiState.value.ticketSeleccionado?.id != esperado) return@collect
                    // Primera foto del servidor: recién ahí se pinta completo.
                    _uiState.update { it.copy(mensajesEnVivo = vivos, mensajesListos = true) }
                }
        }
    }

    fun abrirModalNuevoTicket() {
        _uiState.update { it.copy(mostrarModalNuevoTicket = true, error = null) }
    }

    fun cerrarModalNuevoTicket() {
        _uiState.update { it.copy(mostrarModalNuevoTicket = false) }
    }

    /**
     * Envío optimista estilo WhatsApp: el mensaje aparece AL INSTANTE como
     * "Enviando…" y el servidor lo confirma después (misma clave: si ya
     * existe, no se duplica). El botón jamás queda trabado.
     */
    fun enviarMensajeEnChat(texto: String) {
        val msgLimpio = texto.trim()
        if (msgLimpio.isBlank()) return

        val ticketSel = _uiState.value.ticketSeleccionado
        if (ticketSel != null) {
            if (!ticketSel.puedeEscribir) {
                _uiState.update {
                    it.copy(error = SoporteRepository.ERROR_CONVERSACION_CERRADA)
                }
                return
            }
            val ahora = HoraServidor.ahoraMs()
            if (com.app.administradorfarmadon.soporte.modelo.debeIgnorarDobleEnvio(
                    ultimoEnvioTexto, ultimoEnvioMs, msgLimpio, ahora
                )
            ) return
            ultimoEnvioTexto = msgLimpio
            ultimoEnvioMs = ahora
            // Clave generada ANTES del envío + mensaje visible al instante.
            val clientId = "msg_${ahora}_${UUID.randomUUID().toString().take(6)}"
            val optimista = MensajeSoporte(
                id = clientId,
                clientMessageId = clientId,
                conversationId = ticketSel.id,
                emisor = "FARMACIA",
                autorNombre = SessionManager.nombreUsuario,
                usuarioEmail = SessionManager.email,
                sucursalId = SessionManager.sucursalIdEfectiva,
                sucursalNombre = SessionManager.sucursalNombre,
                clienteId = SessionManager.clienteIdGarantizado,
                texto = msgLimpio,
                fechaMs = ahora,
                leido = false,
                estadoLectura = "ENVIANDO"
            )
            _uiState.update { st ->
                st.copy(error = null, mensajesOptimistas = st.mensajesOptimistas + optimista)
            }
            viewModelScope.launch {
                val res = soporteRepository.enviarMensaje(ticketSel.id, msgLimpio, clientId)
                res.onSuccess {
                    // El eco del servidor (misma clave) lo reemplaza solo.
                    _uiState.update { st ->
                        st.copy(mensajesOptimistas = st.mensajesOptimistas.filterNot {
                            it.clientMessageId == clientId
                        })
                    }
                }.onFailure {
                    _uiState.update { st ->
                        st.copy(
                            error = "No pudimos enviar un mensaje. Revisa tu conexión y toca Reintentar en el mensaje.",
                            mensajesOptimistas = st.mensajesOptimistas.map {
                                if (it.clientMessageId == clientId) it.copy(estadoLectura = "ERROR") else it
                            }
                        )
                    }
                }
            }
        } else {
            // Sin caso abierto no hay dónde escribir: se abre el diálogo único.
            abrirModalNuevoTicket()
        }
    }

    /**
     * Reintenta con LA MISMA clave: si el servidor ya lo guardó (la primera
     * vez sí llegó), no crea un duplicado, solo lo confirma.
     */
    fun reintentarEnvioMensaje(ticketId: String, clientMessageId: String) {
        val sel = _uiState.value.ticketSeleccionado
        if (sel != null && sel.id == ticketId && !sel.puedeEscribir) {
            _uiState.update { it.copy(error = SoporteRepository.ERROR_CONVERSACION_CERRADA) }
            return
        }
        val pendiente = _uiState.value.mensajesOptimistas.firstOrNull {
            it.clientMessageId == clientMessageId && it.estadoLectura == "ERROR"
        } ?: return
        viewModelScope.launch {
            _uiState.update { st ->
                st.copy(
                    error = null,
                    mensajesOptimistas = st.mensajesOptimistas.map {
                        if (it.clientMessageId == clientMessageId) it.copy(estadoLectura = "ENVIANDO") else it
                    }
                )
            }
            val res = soporteRepository.enviarMensaje(ticketId, pendiente.texto, clientMessageId)
            res.onSuccess {
                _uiState.update { st ->
                    st.copy(mensajesOptimistas = st.mensajesOptimistas.filterNot {
                        it.clientMessageId == clientMessageId
                    })
                }
            }.onFailure {
                _uiState.update { st ->
                    st.copy(
                        error = "Sigue sin poder enviarse. Revisa tu conexión y vuelve a intentarlo.",
                        mensajesOptimistas = st.mensajesOptimistas.map {
                            if (it.clientMessageId == clientMessageId) it.copy(estadoLectura = "ERROR") else it
                        }
                    )
                }
            }
        }
    }

    /**
     * Nueva solicitud con cero fricción: la persona elige 1 opción y cuenta
     * con sus palabras. El asunto y la prioridad los deduce el sistema;
     * lo técnico (sede, usuario, equipo) viaja solo en segundo plano.
     */
    fun crearSolicitudSimple(categoria: String, descripcion: String) {
        val descLimpia = descripcion.trim()
        if (descLimpia.length < 8) {
            _uiState.update { it.copy(error = "Cuéntanos con al menos 8 caracteres qué está pasando.") }
            return
        }
        if (_uiState.value.creandoTicket) return
        val titular = generarTitularConversacion(descLimpia)
        val prioridad = if (categoria == "ERROR_TECNICO") "ALTA" else "MEDIA"
        viewModelScope.launch {
            _uiState.update { it.copy(creandoTicket = true, error = null) }
            val clientConversationId = "tk_${UUID.randomUUID().toString().take(8)}"
            val relacionado = origenRelacionadoId
            val res = soporteRepository.crearCasoEstructurado(
                asunto = titular,
                categoria = categoria,
                prioridad = prioridad,
                descripcion = descLimpia,
                relatedConversationId = relacionado ?: "",
                clientConversationId = clientConversationId
            )
            res.onSuccess { ticket ->
                origenRelacionadoId = null
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        mostrarModalNuevoTicket = false,
                        ticketSeleccionado = ticket,
                        mensajesEnVivo = emptyList(),
                        mensajesOptimistas = emptyList(),
                        mensajesListos = true,
                        mensajeExito = "Solicitud recibida. Un agente la atenderá."
                    )
                }
                observarMensajesDelSeleccionado(ticket.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        error = err.message ?: "No se pudo enviar tu solicitud. Vuelve a intentarlo."
                    )
                }
            }
        }
    }

    /**
     * Reporta un error real con contexto automático (§8-§10):
     * el personal elige en simple, BrixoPanel recibe lo técnico.
     */
    fun reportarErrorConContexto(
        tipoReporte: String,
        descripcion: String,
        errorCodigo: String = "",
        modulo: String = "",
        pantalla: String = "",
        accion: String = ""
    ) {
        if (_uiState.value.creandoTicket) return
        val descLimpia = descripcion.trim()
        if (descLimpia.length < 8) {
            _uiState.update { it.copy(error = "Describe lo ocurrido con al menos 8 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(creandoTicket = true, error = null) }
            // Contexto real del dispositivo (sin inventar versión: se deja vacío
            // si la UI no la aporta; BrixoPanel igual recibe módulo/pantalla/usuario).
            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
            val os = "Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
            val asunto = if (errorCodigo.isNotBlank()) "Error $errorCodigo en $pantalla" else "Reporte: $tipoReporte"
            val res = soporteRepository.crearCasoEstructurado(
                asunto = asunto.take(140),
                categoria = "ERROR_TECNICO",
                prioridad = "ALTA",
                descripcion = "[$tipoReporte] $descLimpia",
                errorCodigo = errorCodigo,
                errorModulo = modulo.ifBlank { pantalla },
                errorPantalla = pantalla,
                errorAccion = accion,
                appVersion = "",
                deviceInfo = device,
                osVersion = os,
                clientConversationId = "tk_${UUID.randomUUID().toString().take(8)}"
            )
            res.onSuccess { ticket ->
                _uiState.update {
                    it.copy(
                        creandoTicket = false,
                        mostrarModalNuevoTicket = false,
                        ticketSeleccionado = ticket,
                        mensajesEnVivo = emptyList(),
                        mensajesOptimistas = emptyList(),
                        mensajesListos = true,
                        mensajeExito = "Reporte enviado a soporte con el contexto del error.",
                        ultimoReporteOkMs = HoraServidor.ahoraMs()
                    )
                }
                observarMensajesDelSeleccionado(ticket.id)
            }.onFailure { err ->
                _uiState.update { it.copy(creandoTicket = false, error = err.message ?: "No se pudo enviar el reporte. Tu texto sigue aquí.") }
            }
        }
    }

    fun contactarWhatsApp(context: Context, motivo: String = "Consulta operativa") {
        val wa = _uiState.value.canalesOficiales.whatsapp.trim()
        if (wa.isBlank()) {
            _uiState.update { it.copy(error = "La central aún no publicó un WhatsApp oficial. Escríbenos por el chat de aquí.") }
            return
        }
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
        val email = _uiState.value.canalesOficiales.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "La central aún no publicó un correo oficial. Escríbenos por el chat de aquí.") }
            return
        }
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

    private fun generarTitularConversacion(texto: String): String {
        val palabras = texto.trim().split("\\s+".toRegex())
        if (palabras.size <= 6) return texto.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val primerTexto = palabras.take(6).joinToString(" ")
        return primerTexto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + "..."
    }
}
