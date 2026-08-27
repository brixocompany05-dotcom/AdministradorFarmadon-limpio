package com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos

import com.app.administradorfarmadon.base_datos.PlanSuscripcion

data class Paso2UiState(
    val planSeleccionado: PlanSuscripcion? = null,
    val planesDisponibles: List<PlanSuscripcion> = emptyList(),
    val cargandoPlanes: Boolean = true,
    // Causa REAL del fallo de lectura (red/servidor/permisos). null = la lectura
    // no falló: una lista vacía entonces significa "aún no hay planes publicados".
    val errorPlanes: String? = null,
    val aviso: String? = null
)
