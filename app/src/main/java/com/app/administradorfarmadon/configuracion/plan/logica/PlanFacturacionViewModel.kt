package com.app.administradorfarmadon.configuracion.plan.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.configuracion.plan.datos.BrixoCanalesPagoInfo
import com.app.administradorfarmadon.configuracion.plan.datos.HistorialPagoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanCatalogoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PlanFacturacionViewModel(
    private val repository: PlanFacturacionRepository = PlanFacturacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanFacturacionUiState())
    val uiState: StateFlow<PlanFacturacionUiState> = _uiState.asStateFlow()

    init {
        iniciarEscuchaTiempoReal()
    }

    fun iniciarEscuchaTiempoReal() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            try {
                val clienteId = repository.resolverClienteId()
                if (clienteId.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        error = "No se pudo identificar la farmacia en sesión."
                    )
                    return@launch
                }

                // Catálogo y canales son secciones secundarias (el upgrade va por
                // WhatsApp a BRIXO, no son datos de la suscripción del cliente).
                // Si fallan, NO tumban la pantalla: se guarda el error en su campo
                // propio y se emite valor vacío para que el combine no se trabe.
                val flujoCatalogo = repository.observarCatalogoPlanes()
                    .catch { e -> emit(emptyList<PlanCatalogoItem>()); _uiState.value = _uiState.value.copy(catalogoError = e.message ?: "No se pudo cargar el catálogo de BRIXO") }
                val flujoCanales = repository.observarCanalesPagoBrixo()
                    .catch { e -> emit(BrixoCanalesPagoInfo()); _uiState.value = _uiState.value.copy(canalesError = e.message ?: "No se pudo cargar los canales de pago") }

                combine(
                    repository.observarPlanInfo(clienteId),
                    repository.observarHistorialPagos(clienteId),
                    flujoCanales,
                    flujoCatalogo
                ) { planInfo, historial, canales, catalogo ->
                    _uiState.value.copy(
                        cargando = false,
                        error = null,
                        planInfo = planInfo,
                        historialPagos = historial,
                        canalesPago = canales,
                        catalogoPlanes = catalogo
                    )
                }.catch { e ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        error = e.message ?: "Error al sincronizar datos de facturación"
                    )
                }.collect { nuevoEstado ->
                    _uiState.value = nuevoEstado
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    error = e.message ?: "Fallo de conexión al cargar plan"
                )
            }
        }
    }

    fun abrirModalPlanes() {
        _uiState.value = _uiState.value.copy(mostrarModalPlanes = true)
    }

    fun cerrarModalPlanes() {
        _uiState.value = _uiState.value.copy(mostrarModalPlanes = false)
    }

    fun seleccionarPagoParaDetalle(pago: HistorialPagoItem?) {
        _uiState.value = _uiState.value.copy(pagoSeleccionado = pago)
    }

    /**
     * Lógica de Asentamiento de Pago Bancario (Notificación a BRIXO)
     * RAíZ: nunca fingir éxito. Antes era delay(1500) falso que decía "notificado"
     * sin subir nada a BRIXO. Ahora informa veraz y deriva al flujo real
     * ReportarPagoDialog (Storage + solicitudes_pago) que sí tiene verdad.
     */
    fun notificarAbono(
        monto: Double,
        banco: String,
        operacion: String,
        uriImagen: android.net.Uri?,
        nota: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            // No fingir guardado (R3). Este canal está en construcción: el flujo real
            // es ReportarPagoDialog que sube voucher a Storage y crea solicitudes_pago.
            _uiState.value = _uiState.value.copy(
                cargando = false,
                error = "Notificación directa aún no disponible —” usa el botón 'Reportar Pago' con comprobante. Si ves este mensaje, avisa a soporte BRIXO."
            )
        }
    }
}
