package com.app.administradorfarmadon.configuracion.sucursales.logica

import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal

data class ColaboradorItem(
    val id: String = "",
    val nombre: String = "",
    val rol: String = ""
)

data class SucursalesUiState(
    val sucursales: List<Sucursal> = emptyList(),
    val sucursalSeleccionada: Sucursal? = null,
    val esModoCreacion: Boolean = false,
    val filtroEstado: String = "TODAS", // TODAS, OPERANDO, MANTENIMIENTO

    val clienteId: String = "",
    val planNombre: String = "Plan Estándar",
    val maxSucursales: Int = 1,
    // Verdad honesta: BRIXO no escribió el límite de sedes en la suscripción.
    val limiteNoConfigurado: Boolean = false,

    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mostrarDialogoLimite: Boolean = false,
    val mostrarDialogoEliminar: Boolean = false,
    val mostrarDialogoDescartar: Boolean = false,
    val colaboradoresAsignados: List<ColaboradorItem> = emptyList(),
    val colaboradoresAsignadosNombres: List<String> = emptyList(),

    // Pasos secuenciales para eliminar sede con personal (Cero competencia visual)
    // 0: Cerrado, 1: Elección Macro ("REUBICAR" / "ELIMINAR_TODOS"), 2: Configuración o Advertencia
    val pasoEliminarSede: Int = 0,
    val opcionMacroEliminarPersonal: String = "", // "REUBICAR", "ELIMINAR_TODOS"
    val subOpcionReubicar: String = "TODOS_IGUAL", // "TODOS_IGUAL", "INDIVIDUAL"
    val sedeDestinoTodosId: String = "",
    val mapaDestinoIndividual: Map<String, String> = emptyMap(),

    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val accesoRestringido: Boolean = false,

    // Formulario reactivo
    val formNombre: String = "",
    val formDireccion: String = "",
    val formTelefono: String = "",
    val formResponsable: String = "",
    val formCodigoInterno: String = "",
    val formLatitud: Double? = null,
    val formLongitud: Double? = null,
    val formActiva: Boolean = true,
    // Métodos de pago de la sucursal nueva. Todos vienen marcados por defecto;
    // el administrador solo desmarca los que esta sede no debe manejar.
    val formPagosSeleccionados: Set<String> = emptySet(),
    // Tipos de pago que la Sede Principal tiene configurados (con cuenta activa).
    // Se usan para avisar qué marcado no se podrá copiar al nacer.
    val principalPagosDisponibles: Set<String> = emptySet(),
    val formErrores: Map<String, String> = emptyMap(),
    val pasoActual: Int = 1
) {
    val totalSucursales: Int get() = sucursales.size

    val sucursalesFiltradas: List<Sucursal> get() {
        return when (filtroEstado) {
            "OPERANDO" -> sucursales.filter { it.activa }
            "MANTENIMIENTO" -> sucursales.filter { !it.activa }
            else -> sucursales
        }
    }

    val puedeCrearMas: Boolean get() = !limiteNoConfigurado && totalSucursales < maxSucursales
    val porcentajeOcupado: Float get() = if (maxSucursales > 0) (totalSucursales.toFloat() / maxSucursales.toFloat()).coerceIn(0f, 1f) else 1f

    /** Métodos marcados que NO podrán copiarse porque la principal no los tiene configurados. */
    val pagosMarcadosSinDisponibilidad: Set<String>
        get() = formPagosSeleccionados - principalPagosDisponibles

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
