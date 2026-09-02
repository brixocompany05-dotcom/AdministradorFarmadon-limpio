package com.app.administradorfarmadon.facturacion.documentos.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesRepository
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.facturacion.configuracion.datos.FacturacionConfigRepository
import com.app.administradorfarmadon.facturacion.documentos.datos.FacturacionDocumentosRepository
import com.app.administradorfarmadon.facturacion.envio.datos.FacturacionEnvioRepository
import com.app.administradorfarmadon.facturacion.envio.datos.ResultadoEnvioFiscal
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacturacionDocumentosUiState(
    val cargando: Boolean = true,
    val documentos: List<FacturacionDocumento> = emptyList(),
    val emisor: EmisorFiscal? = null,
    val sedes: List<Sucursal> = emptyList(),
    val pestanaActual: Int = 0, // 0: Documentos, 1: Resumen, 2: Emisor
    val filtroEstado: String = "TODOS", // TODOS, PENDIENTE, ATENCION, ACEPTADO, ANULADO
    val filtroTipo: String = "TODOS", // TODOS, BOLETA, FACTURA, NOTA_CREDITO, COMUNICACION_BAJA
    val filtroSedeId: String = "TODAS",
    val busquedaTexto: String = "",
    val documentoSeleccionado: FacturacionDocumento? = null,
    val ventaVinculada: Venta? = null,
    val cargandoVentaVinculada: Boolean = false,
    val enviandoDocId: String? = null,
    val enviandoLote: Boolean = false,
    val documentoAConfirmarEnvio: FacturacionDocumento? = null,
    val mostrarDialogoConfirmarLote: Boolean = false,
    val resultadoLoteReciente: com.app.administradorfarmadon.facturacion.envio.datos.ReporteEnvioLote? = null,
    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val error: String? = null
) {
    // ── Métricas 100% computadas de la verdad real (Regla R12) ──
    val totalDocumentos: Int get() = documentos.size

    // F5-A: Dos familias visuales para el usuario
    val totalPendientesEnCola: Int
        get() = documentos.count {
            (it.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE || it.estadoEnvio == FacturacionDocumento.ESTADO_ENVIADO) && !it.numeroQuemado
        }

    val totalRequierenAtencion: Int
        get() = documentos.count {
            it.numeroQuemado || it.estadoEnvio == FacturacionDocumento.ESTADO_RECHAZADO
        }

    val totalAceptados: Int get() = documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO }
    val totalAnulados: Int get() = documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_ANULADO }
    val totalPendientes: Int get() = totalPendientesEnCola
    val totalRechazados: Int get() = totalRequierenAtencion

    val montoTotalFacturado: Double
        get() = kotlin.math.round(
            documentos.filter {
                it.estadoEnvio != FacturacionDocumento.ESTADO_ANULADO &&
                        it.tipo != "NOTA_CREDITO" &&
                        it.tipo != "COMUNICACION_BAJA"
            }.sumOf { it.total } * 100.0
        ) / 100.0

    val totalBoletas: Int get() = documentos.count { it.tipo.equals("BOLETA", ignoreCase = true) }
    val totalFacturas: Int get() = documentos.count { it.tipo.equals("FACTURA", ignoreCase = true) }
    val totalNotasCredito: Int get() = documentos.count { it.tipo.equals("NOTA_CREDITO", ignoreCase = true) }
    val totalBajas: Int get() = documentos.count { it.tipo.equals("COMUNICACION_BAJA", ignoreCase = true) }

    val documentosFiltrados: List<FacturacionDocumento>
        get() {
            val q = busquedaTexto.trim().lowercase()
            return documentos.filter { doc ->
                // F5-A: Mapeo de estados a las familias comprensibles
                val coincideEstado = when (filtroEstado) {
                    "TODOS" -> true
                    "PENDIENTE" -> (doc.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE || doc.estadoEnvio == FacturacionDocumento.ESTADO_ENVIADO) && !doc.numeroQuemado
                    "ATENCION", "RECHAZADO" -> doc.numeroQuemado || doc.estadoEnvio == FacturacionDocumento.ESTADO_RECHAZADO
                    "ACEPTADO" -> doc.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO
                    "ANULADO" -> doc.estadoEnvio == FacturacionDocumento.ESTADO_ANULADO
                    else -> doc.estadoEnvio.equals(filtroEstado, ignoreCase = true)
                }

                val coincideTipo = when (filtroTipo) {
                    "TODOS" -> true
                    else -> doc.tipo.equals(filtroTipo, ignoreCase = true)
                }

                val coincideSede = when (filtroSedeId) {
                    "TODAS" -> true
                    else -> doc.sucursalId == filtroSedeId
                }

                val coincideBusqueda = if (q.isBlank()) {
                    true
                } else {
                    doc.numeroCompleto.lowercase().contains(q) ||
                            doc.clienteNombre.lowercase().contains(q) ||
                            doc.clienteNumeroDoc.contains(q) ||
                            doc.serie.lowercase().contains(q) ||
                            doc.motivo.lowercase().contains(q)
                }

                coincideEstado && coincideTipo && coincideSede && coincideBusqueda
            }
        }
}

class FacturacionDocumentosViewModel(
    private val docsRepository: FacturacionDocumentosRepository = FacturacionDocumentosRepository(),
    private val configRepository: FacturacionConfigRepository = FacturacionConfigRepository(),
    private val sucursalesRepository: SucursalesRepository = SucursalesRepository(),
    private val envioRepository: FacturacionEnvioRepository = FacturacionEnvioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacturacionDocumentosUiState())
    val uiState: StateFlow<FacturacionDocumentosUiState> = _uiState.asStateFlow()

    private val farmaciaId: String
        get() = SessionManager.clienteIdGarantizado

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        if (farmaciaId.isBlank()) {
            _uiState.update { it.copy(cargando = false, error = "Sesión no identificada") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            // 1. Escucha viva de documentos fiscales
            launch {
                docsRepository.observarDocumentos(farmaciaId)
                    .catch { e ->
                        _uiState.update { it.copy(cargando = false, error = "Error al sincronizar documentos: ${e.message}") }
                    }
                    .collect { lista ->
                        _uiState.update { currentState ->
                            // Mantener la referencia actualizada del documento seleccionado
                            val docActualizado = currentState.documentoSeleccionado?.let { sel ->
                                lista.find { it.id == sel.id }
                            }
                            currentState.copy(
                                cargando = false,
                                documentos = lista,
                                documentoSeleccionado = docActualizado ?: currentState.documentoSeleccionado,
                                error = null
                            )
                        }
                    }
            }

            // 2. Escucha del emisor fiscal
            launch {
                configRepository.observarEmisor(farmaciaId)
                    .catch { /* Falla tolerada en emisor no bloquea la bandeja */ }
                    .collect { emisor ->
                        _uiState.update { it.copy(emisor = emisor) }
                    }
            }

            // 3. Escucha de sucursales para el filtro por sede
            launch {
                sucursalesRepository.observarSucursales(farmaciaId)
                    .catch { /* Falla tolerada en sucursales */ }
                    .collect { sedes ->
                        _uiState.update { it.copy(sedes = sedes) }
                    }
            }
        }
    }

    fun setPestana(index: Int) {
        _uiState.update { it.copy(pestanaActual = index) }
    }

    fun setFiltroEstado(estado: String) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun setFiltroTipo(tipo: String) {
        _uiState.update { it.copy(filtroTipo = tipo) }
    }

    fun setFiltroSede(sedeId: String) {
        _uiState.update { it.copy(filtroSedeId = sedeId) }
    }

    fun setBusquedaTexto(texto: String) {
        _uiState.update { it.copy(busquedaTexto = texto) }
    }

    fun seleccionarDocumento(doc: FacturacionDocumento?) {
        if (doc == null) {
            _uiState.update { it.copy(documentoSeleccionado = null, ventaVinculada = null) }
            return
        }

        _uiState.update {
            it.copy(
                documentoSeleccionado = doc,
                ventaVinculada = null,
                cargandoVentaVinculada = doc.ventaId.isNotBlank()
            )
        }

        if (doc.ventaId.isNotBlank()) {
            viewModelScope.launch {
                val venta = docsRepository.obtenerVentaVinculada(farmaciaId, doc.sucursalId, doc.ventaId)
                _uiState.update { it.copy(ventaVinculada = venta, cargandoVentaVinculada = false) }
            }
        }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(documentoSeleccionado = null, ventaVinculada = null) }
    }

    fun solicitarConfirmacionEnvio(doc: FacturacionDocumento) {
        _uiState.update { it.copy(documentoAConfirmarEnvio = doc) }
    }

    fun cancelarConfirmacionEnvio() {
        _uiState.update { it.copy(documentoAConfirmarEnvio = null) }
    }

    fun solicitarConfirmacionLote() {
        _uiState.update { it.copy(mostrarDialogoConfirmarLote = true) }
    }

    fun cancelarConfirmacionLote() {
        _uiState.update { it.copy(mostrarDialogoConfirmarLote = false) }
    }

    fun cerrarReporteLote() {
        _uiState.update { it.copy(resultadoLoteReciente = null) }
    }

    fun enviarDocumento(docId: String, onFallaRed: (() -> Unit)? = null) {
        if (docId.isBlank() || farmaciaId.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    enviandoDocId = docId,
                    documentoAConfirmarEnvio = null,
                    mensajeError = null,
                    mensajeExito = null
                )
            }
            val res = envioRepository.enviarDocumento(farmaciaId, docId)
            when (res) {
                is ResultadoEnvioFiscal.Aceptado -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeExito = "Comprobante aceptado por SUNAT exitosamente."
                        )
                    }
                }
                is ResultadoEnvioFiscal.Rechazado -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Comprobante no aceptado por SUNAT: ${res.motivo}"
                        )
                    }
                }
                is ResultadoEnvioFiscal.Excepcion -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Excepción temporal en SUNAT: ${res.motivo}. La numeración no se quemó."
                        )
                    }
                }
                is ResultadoEnvioFiscal.Enviado -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeExito = "Comprobante enviado a SUNAT (en proceso de validación)."
                        )
                    }
                }
                is ResultadoEnvioFiscal.FallaRed -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Sin conexión: el comprobante quedó en cola para reintento automático."
                        )
                    }
                    onFallaRed?.invoke()
                }
                is ResultadoEnvioFiscal.Invalido -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = res.motivo
                        )
                    }
                }
            }
        }
    }

    fun enviarLotePendientes(onFallaRed: (() -> Unit)? = null) {
        if (farmaciaId.isBlank() || _uiState.value.enviandoLote) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    enviandoLote = true,
                    mostrarDialogoConfirmarLote = false,
                    mensajeError = null,
                    mensajeExito = null
                )
            }
            val reporte = envioRepository.enviarLotePendientes(farmaciaId)
            val exitosos = reporte.exitosos.size
            val fallidos = reporte.requierenAtencion.size

            _uiState.update {
                it.copy(
                    enviandoLote = false,
                    resultadoLoteReciente = reporte,
                    mensajeExito = if (exitosos > 0) "Envío completado: $exitosos aceptados por SUNAT." else null,
                    mensajeError = if (exitosos == 0 && fallidos > 0) "$fallidos comprobantes requieren atención o quedaron en cola." else null
                )
            }
            if (fallidos > 0) {
                onFallaRed?.invoke()
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null, error = null) }
    }
}
