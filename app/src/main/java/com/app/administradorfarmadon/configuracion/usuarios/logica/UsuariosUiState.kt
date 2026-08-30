package com.app.administradorfarmadon.configuracion.usuarios.logica

import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.usuarios.datos.RolFarmacia
import com.app.administradorfarmadon.configuracion.usuarios.datos.UsuarioFarmacia
import com.app.administradorfarmadon.modulos.domain.ModuloResuelto

data class UsuariosUiState(
    val usuarios: List<UsuarioFarmacia> = emptyList(),
    val roles: List<RolFarmacia> = emptyList(),
    val sucursales: List<Sucursal> = emptyList(),
    val usuarioSeleccionado: UsuarioFarmacia? = null,

    val filtroRol: String? = null,
    val busquedaQuery: String = "",
    val esModoCreacion: Boolean = false,
    val verDadasDeBaja: Boolean = false,

    val clienteId: String = "",
    val usuarioActualUid: String = "",

    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mostrarDialogoSuspender: Boolean = false,
    val mostrarDialogoEliminar: Boolean = false,
    val mostrarDialogoDescartar: Boolean = false,
    val mensajeExito: String? = null,
    val mensajeError: String? = null,

    // Formulario reactivo
    val formNombre: String = "",
    val formDni: String = "",
    val formTelefono: String = "",
    val formEmail: String = "",
    val formPassword: String = "",
    val formRolId: String = "",
    val formRolNombre: String = "",
    val formSucursalId: String = "todas",
    val formSucursalNombre: String = "Todas las Sedes",
    val formAcceso: Boolean = true,
    val formErrores: Map<String, String> = emptyMap(),
    // Herramientas del plan contratado (resueltas vivo)
    val herramientasPlan: List<ModuloResuelto> = emptyList(),
    val planNombre: String = "",
    val cargandoHerramientas: Boolean = true,
    // Selector por usuario: qué módulos opera este usuario (solo los del plan)
    val formPermisosModulos: Map<String, Boolean> = emptyMap()
) {
    val totalUsuarios: Int get() = usuarios.count { !it.dadoDeBaja }
    val totalDadasDeBaja: Int get() = usuarios.count { it.dadoDeBaja }

    val usuariosFiltrados: List<UsuarioFarmacia>
        get() {
            // Pestaña DADOS DE BAJA: solo sellados. Resto: la lista activa de siempre.
            val base = if (verDadasDeBaja) {
                usuarios.filter { it.dadoDeBaja }
            } else {
                usuarios.filter { !it.dadoDeBaja }
            }
            return base.filter { u ->
                val coincideRol = verDadasDeBaja || filtroRol == null ||
                        u.rolNombre.equals(filtroRol, ignoreCase = true) || u.rolId == filtroRol
                val coincideTexto = busquedaQuery.isBlank() ||
                        u.nombre.contains(busquedaQuery, ignoreCase = true) ||
                        u.dni.contains(busquedaQuery) ||
                        u.sucursalNombre.contains(busquedaQuery, ignoreCase = true)
                coincideRol && coincideTexto
            }
        }

    val hayCambiosSinGuardar: Boolean
        get() {
            return if (esModoCreacion) {
                formNombre.isNotBlank() || formDni.isNotBlank() || formEmail.isNotBlank() || formPassword.isNotBlank() || formPermisosModulos.isNotEmpty()
            } else {
                val u = usuarioSeleccionado ?: return false
                formNombre.trim() != u.nombre.trim() ||
                formDni.trim() != u.dni.trim() ||
                formTelefono.trim() != u.telefono.trim() ||
                formEmail.trim().lowercase() != u.email.trim().lowercase() ||
                formPassword.isNotBlank() ||
                formRolId != u.rolId ||
                formSucursalId != u.sucursalId ||
                formAcceso != u.acceso ||
                formPermisosModulos != u.permisosModulos
            }
        }

    // Herramientas visibles para el selector (solo las del plan, ordenadas)
    val herramientasVisibles: List<ModuloResuelto> get() = herramientasPlan
    val totalHerramientasPlan: Int get() = herramientasPlan.size
    val totalHerramientasHabilitadas: Int get() = if (formPermisosModulos.isEmpty()) {
        // Si no hay override, hereda todo lo del plan (según rol base, pero UI muestra todo habilitado por defecto)
        herramientasPlan.size
    } else {
        formPermisosModulos.count { it.value == true }
    }
}
