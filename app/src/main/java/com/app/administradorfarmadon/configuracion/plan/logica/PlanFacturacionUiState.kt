package com.app.administradorfarmadon.configuracion.plan.logica

import com.app.administradorfarmadon.configuracion.plan.datos.BrixoCanalesPagoInfo
import com.app.administradorfarmadon.configuracion.plan.datos.HistorialPagoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanCatalogoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo

data class PlanFacturacionUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val planInfo: PlanFacturacionInfo = PlanFacturacionInfo(),
    val historialPagos: List<HistorialPagoItem> = emptyList(),
    val canalesPago: BrixoCanalesPagoInfo = BrixoCanalesPagoInfo(),
    val catalogoPlanes: List<PlanCatalogoItem> = emptyList(),
    // Errores aislados por sección (no tumban toda la pantalla, pero se muestran con verdad).
    val catalogoError: String? = null,
    val canalesError: String? = null,
    val mostrarModalPlanes: Boolean = false,
    val pagoSeleccionado: HistorialPagoItem? = null
)
