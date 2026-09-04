package com.app.administradorfarmadon.facturacion.documentos.logica

import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class FacturacionDocumentosUiState(
    val cargando: Boolean = true,
    val cargandoMas: Boolean = false,
    val finDeLista: Boolean = false,
    val documentos: List<FacturacionDocumento> = emptyList(),
    val metricasServidor: FacturacionDocumentosRepository.MetricasFiscales? = null,
    val emisor: EmisorFiscal? = null,
    val sedes: List<Sucursal> = emptyList(),
    val pestanaActual: Int = 0, // 0: Documentos, 1: Resumen, 2: Emisor
    val filtroEstado: String = "TODOS", // TODOS, PENDIENTE, ENVIADO, ATENCION, ACEPTADO, ANULADO
    val filtroTipo: String = "TODOS", // TODOS, BOLETA, FACTURA, NOTA_CREDITO, COMUNICACION_BAJA
    val filtroSedeId: String = "TODAS",
    val filtroFechaMs: Long? = null,
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
    // ── Métricas 100% computadas de la verdad real del servidor (Regla R12) ──
    // Si ya cargaron las métricas escalares de Firestore, reflejan la totalidad de la farmacia (ej: 200, 1500).
    // Si aún están cargando, computan sobre los documentos actualmente visibles para jamás mostrar ceros o blanks.
    val totalDocumentos: Int
        get() = metricasServidor?.totalDocumentos ?: documentos.size

    val totalSoloPendientes: Int
        get() = metricasServidor?.totalPendientes ?: documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE && !it.numeroQuemado }

    val totalEnviados: Int
        get() = metricasServidor?.totalEnviados ?: documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_ENVIADO && !it.numeroQuemado }

    val totalPendientesEnCola: Int
        get() = totalSoloPendientes

    val totalRequierenAtencion: Int
        get() = metricasServidor?.totalRequierenAtencion ?: documentos.count {
            it.numeroQuemado || it.estadoEnvio == FacturacionDocumento.ESTADO_RECHAZADO
        }

    val totalAceptados: Int
        get() = metricasServidor?.totalAceptados ?: documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO }

    val totalAnulados: Int
        get() = metricasServidor?.totalAnulados ?: documentos.count { it.estadoEnvio == FacturacionDocumento.ESTADO_ANULADO }

    val totalPendientes: Int get() = totalSoloPendientes
    val totalRechazados: Int get() = totalRequierenAtencion

    /**
     * R12/SUNAT: Monto facturado computa ÚNICAMENTE comprobantes ACEPTADOS con constancia.
     * Documentos en cola, rechazados o anulados no suman ingresos fiscales.
     */
    val montoTotalFacturado: Double
        get() = metricasServidor?.montoTotalFacturado ?: kotlin.math.round(
            documentos.filter {
                it.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO &&
                        it.tipo != "NOTA_CREDITO" &&
                        it.tipo != "COMUNICACION_BAJA"
            }.sumOf { it.total } * 100.0
        ) / 100.0

    val baseGravadaTotal: Double
        get() = metricasServidor?.baseGravadaTotal ?: (kotlin.math.round((montoTotalFacturado / 1.18) * 100.0) / 100.0)

    val igvTotal: Double
        get() = metricasServidor?.igvTotal ?: (kotlin.math.round((montoTotalFacturado - baseGravadaTotal) * 100.0) / 100.0)

    val totalBoletas: Int
        get() = metricasServidor?.totalBoletas ?: documentos.count { it.tipo.equals("BOLETA", ignoreCase = true) }

    val totalFacturas: Int
        get() = metricasServidor?.totalFacturas ?: documentos.count { it.tipo.equals("FACTURA", ignoreCase = true) }

    val totalNotasCredito: Int
        get() = metricasServidor?.totalNotasCredito ?: documentos.count { it.tipo.equals("NOTA_CREDITO", ignoreCase = true) }

    val totalBajas: Int
        get() = metricasServidor?.totalBajas ?: documentos.count { it.tipo.equals("COMUNICACION_BAJA", ignoreCase = true) }

    val documentosFiltrados: List<FacturacionDocumento>
        get() {
            val q = busquedaTexto.trim().lowercase()
            return documentos.filter { doc ->
                val coincideEstado = when (filtroEstado) {
                    "TODOS" -> true
                    "PENDIENTE" -> doc.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE && !doc.numeroQuemado
                    FacturacionDocumento.ESTADO_ENVIADO -> doc.estadoEnvio == FacturacionDocumento.ESTADO_ENVIADO && !doc.numeroQuemado
                    "ATENCION", "RECHAZADO" -> doc.numeroQuemado || doc.estadoEnvio == FacturacionDocumento.ESTADO_RECHAZADO
                    FacturacionDocumento.ESTADO_ACEPTADO -> doc.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO
                    FacturacionDocumento.ESTADO_ANULADO -> doc.estadoEnvio == FacturacionDocumento.ESTADO_ANULADO
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

                val coincideFecha = if (filtroFechaMs == null || filtroFechaMs <= 0L) {
                    true
                } else {
                    val calDoc = Calendar.getInstance().apply { timeInMillis = doc.fechaMs }
                    val calFiltro = Calendar.getInstance().apply { timeInMillis = filtroFechaMs }
                    calDoc.get(Calendar.YEAR) == calFiltro.get(Calendar.YEAR) &&
                            calDoc.get(Calendar.DAY_OF_YEAR) == calFiltro.get(Calendar.DAY_OF_YEAR)
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

                coincideEstado && coincideTipo && coincideSede && coincideFecha && coincideBusqueda
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

    private var sucursalObserverJob: Job? = null

    init {
        recargarMetricasServidor()
        iniciarEscuchaDocumentos()
        cargarDatosComplementarios()
        observarCambiosDeSucursal()
    }

    fun recargarMetricasServidor() {
        val fId = farmaciaId
        if (fId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val metricas = docsRepository.obtenerMetricasFiscales(fId)
                _uiState.update { it.copy(metricasServidor = metricas) }
            } catch (e: Exception) {
                Log.e("FacturacionDocsVM", "Error al obtener métricas fiscales agregadas del servidor: ${e.message}", e)
            }
        }
    }

    fun cargarMas() {
        val fId = farmaciaId
        if (fId.isBlank() || _uiState.value.cargandoMas || _uiState.value.finDeLista) return
        val docsActuales = _uiState.value.documentos
        val ultimoDoc = docsActuales.lastOrNull() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(cargandoMas = true) }
            try {
                val siguientes = docsRepository.cargarSiguientePagina(fId, ultimoDoc.fechaMs, 50)
                _uiState.update { current ->
                    if (siguientes.isEmpty()) {
                        current.copy(cargandoMas = false, finDeLista = true)
                    } else {
                        val idsExistentes = current.documentos.map { it.id }.toSet()
                        val nuevos = siguientes.filter { it.id !in idsExistentes }
                        current.copy(
                            cargandoMas = false,
                            documentos = current.documentos + nuevos,
                            finDeLista = siguientes.size < 50
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FacturacionDocsVM", "Error cargando más documentos: ${e.message}", e)
                _uiState.update { it.copy(cargandoMas = false) }
            }
        }
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            SessionManager.sucursalFlow.collect { sucursal ->
                if (sucursal != ultimaSucursal) {
                    ultimaSucursal = sucursal
                    _uiState.update { current ->
                        if (current.filtroSedeId != "TODAS") {
                            current.copy(filtroSedeId = sucursal)
                        } else current
                    }
                }
            }
        }
    }

    private fun iniciarEscuchaDocumentos() {
        if (farmaciaId.isBlank()) {
            _uiState.update { it.copy(cargando = false, error = "No se encontró sesión de farmacia activa.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            docsRepository.observarDocumentos(farmaciaId, limite = 50)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = "Error al conectar con la bandeja de facturación: ${e.message}"
                        )
                    }
                }
                .collect { lista ->
                    _uiState.update { current ->
                        val docSeleccionadoActualizado = if (current.documentoSeleccionado != null) {
                            lista.firstOrNull { it.id == current.documentoSeleccionado.id } ?: current.documentoSeleccionado
                        } else null

                        current.copy(
                            cargando = false,
                            documentos = lista,
                            documentoSeleccionado = docSeleccionadoActualizado,
                            error = null
                        )
                    }
                    // Mantener las métricas globales del servidor sincronizadas al recibir nuevos comprobantes
                    recargarMetricasServidor()
                }
        }
    }

    private fun cargarDatosComplementarios() {
        if (farmaciaId.isBlank()) return

        viewModelScope.launch {
            configRepository.observarEmisor(farmaciaId)
                .catch { /* Falla tolerada en complementario */ }
                .collect { emisor ->
                    _uiState.update { it.copy(emisor = emisor) }
                }
        }

        viewModelScope.launch {
            sucursalesRepository.observarSucursales(farmaciaId)
                .catch { /* Falla tolerada en complementario */ }
                .collect { sedes ->
                    _uiState.update { it.copy(sedes = sedes) }
                }
        }
    }

    fun setPestanaActual(index: Int) {
        _uiState.update { it.copy(pestanaActual = index) }
    }

    fun setPestana(index: Int) = setPestanaActual(index)
    fun cerrarReporteLote() = cerrarDialogoResultadoLote()
    fun cerrarDetalle() = seleccionarDocumento(null)

    fun setFiltroEstado(estado: String) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun setFiltroTipo(tipo: String) {
        _uiState.update { it.copy(filtroTipo = tipo) }
    }

    fun setFiltroSede(sedeId: String) {
        _uiState.update { it.copy(filtroSedeId = sedeId) }
    }

    fun setFiltroFecha(fechaMs: Long?) {
        _uiState.update { it.copy(filtroFechaMs = fechaMs) }
    }

    fun setBusquedaTexto(texto: String) {
        _uiState.update { it.copy(busquedaTexto = texto) }
    }

    fun seleccionarDocumento(doc: FacturacionDocumento?) {
        _uiState.update {
            it.copy(
                documentoSeleccionado = doc,
                ventaVinculada = null,
                cargandoVentaVinculada = doc != null && doc.ventaId.isNotBlank()
            )
        }
        if (doc != null && doc.ventaId.isNotBlank()) {
            cargarVentaVinculada(doc)
        }
    }

    private fun cargarVentaVinculada(doc: FacturacionDocumento) {
        viewModelScope.launch {
            val venta = docsRepository.obtenerVentaVinculada(farmaciaId, doc.sucursalId, doc.ventaId)
            _uiState.update {
                it.copy(
                    ventaVinculada = venta,
                    cargandoVentaVinculada = false
                )
            }
        }
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

    fun cerrarDialogoResultadoLote() {
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
                            mensajeExito = "Comprobante aceptado por SUNAT con CDR firmado."
                        )
                    }
                }
                is ResultadoEnvioFiscal.Rechazado -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Comprobante rechazado por SUNAT (número quemado): ${res.motivo}"
                        )
                    }
                }
                is ResultadoEnvioFiscal.Excepcion -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Excepción técnica en SUNAT: ${res.motivo}. La numeración no se quemó."
                        )
                    }
                }
                is ResultadoEnvioFiscal.Enviado -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeExito = "Comprobante en trámite ante SUNAT (ID: ${res.docIdProveedor}). Esperando CDR."
                        )
                    }
                }
                is ResultadoEnvioFiscal.FallaRed -> {
                    _uiState.update {
                        it.copy(
                            enviandoDocId = null,
                            mensajeError = "Sin conexión: el comprobante quedó en cola de contingencia para reintento automático."
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
            val enTramite = reporte.enTramite.size
            val fallidos = reporte.requierenAtencion.size

            _uiState.update {
                it.copy(
                    enviandoLote = false,
                    resultadoLoteReciente = reporte,
                    mensajeExito = if (exitosos > 0) "Envío completado: $exitosos aceptados por SUNAT con CDR." else if (enTramite > 0) "$enTramite comprobantes quedaron en trámite." else null,
                    mensajeError = if (exitosos == 0 && fallidos > 0) "$fallidos comprobantes requieren atención." else null
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
