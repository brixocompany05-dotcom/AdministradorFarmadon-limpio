package com.app.administradorfarmadon.configuracion.sucursales.logica

import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal

data class SucursalesUiState(
    val sucursales: List<Sucursal> = emptyList(),
    val sucursalSeleccionada: Sucursal? = null,
    val esModoCreacion: Boolean = false,
    val filtroEstado: String = "TODAS", // TODAS, OPERANDO, MANTENIMIENTO

    val clienteId: String = "",
    val planNombre: String = "Plan Estándar",
    val maxSucursales: Int = 1,

    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mostrarDialogoLimite: Boolean = false,
    val mostrarDialogoEliminar: Boolean = false,
    val mostrarDialogoDescartar: Boolean = false,
    val colaboradoresAsignadosNombres: List<String> = emptyList(),
    val mensajeExito: String? = null,
    val mensajeError: String? = null,

    // Formulario reactivo
    val formNombre: String = "",
    val formDireccion: String = "",
    val formTelefono: String = "",
    val formResponsable: String = "",
    val formCodigoInterno: String = "",
    val formLatitud: Double? = null,
    val formLongitud: Double? = null,
    val formActiva: Boolean = true,
    val formErrores: Map<String, String> = emptyMap()
) {
    val totalSucursales: Int get() = sucursales.size

    val sucursalesFiltradas: List<Sucursal> get() {
        return when (filtroEstado) {
            "OPERANDO" -> sucursales.filter { it.activa }
            "MANTENIMIENTO" -> sucursales.filter { !it.activa }
            else -> sucursales
        }
    }

    val puedeCrearMas: Boolean get() = totalSucursales < maxSucursales
    val porcentajeOcupado: Float get() = if (maxSucursales > 0) (totalSucursales.toFloat() / maxSucursales.toFloat()).coerceIn(0f, 1f) else 1f

    val hayCambiosSinGuardar: Boolean
        get() {
            return if (esModoCreacion) {
                formNombre.isNotBlank() || formDireccion.isNotBlank() || formTelefono.isNotBlank() || formResponsable.isNotBlank()
            } else {
                val s = sucursalSeleccionada ?: return false
                formNombre.trim() != s.nombre.trim() ||
                formDireccion.trim() != s.direccion.trim() ||
                formTelefono.trim() != s.telefono.trim() ||
                formResponsable.trim() != s.responsable.trim() ||
                formActiva != s.activa ||
                formLatitud != s.latitud ||
                formLongitud != s.longitud
            }
        }
}
