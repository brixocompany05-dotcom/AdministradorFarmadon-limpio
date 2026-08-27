package com.app.administradorfarmadon.autenticacion.registro.contenedor.logica

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteUi
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteAccion

internal fun mapearIncidente(
    tipo: RegistroIncidenteTipo,
    customMensaje: String? = null
): RegistroIncidenteUi {
    return when (tipo) {
        RegistroIncidenteTipo.CORREO_EXISTENTE -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Correo registrado",
            mensaje = "Este correo ya está registrado en Farmadon. Por favor, contacta a soporte si crees que es un error o necesitas ayuda para entrar.",
            icono = Icons.Default.Email,
            accionPrincipal = RegistroIncidenteAccion.ContactarSoporte
        )
        RegistroIncidenteTipo.FALLO_INTEGRIDAD -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Fallo de integridad",
            mensaje = customMensaje ?: "Hubo un error al crear tu cuenta. El sistema detectó una inconsistencia que requiere atención de soporte. Por favor, contáctanos para resolverlo.",
            icono = Icons.Default.Error,
            accionPrincipal = RegistroIncidenteAccion.ContactarSoporte
        )
        RegistroIncidenteTipo.RUC_EXISTENTE -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "RUC registrado",
            mensaje = "Este RUC ya está registrado. Si eres el dueño, inicia sesión o contacta a soporte.",
            icono = Icons.Default.Assignment,
            accionPrincipal = RegistroIncidenteAccion.VolverYCorregir
        )
        RegistroIncidenteTipo.SIN_INTERNET -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Sin conexión",
            mensaje = "No pudimos conectarnos al servidor. Revisa tu internet e inténtalo de nuevo.",
            icono = Icons.Default.WifiOff,
            accionPrincipal = RegistroIncidenteAccion.Reintentar
        )
        RegistroIncidenteTipo.PLAN_NO_DISPONIBLE -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Plan no disponible",
            mensaje = "El plan que seleccionaste ya no está disponible. Por favor, elige otro.",
            icono = Icons.Default.Warning,
            accionPrincipal = RegistroIncidenteAccion.ElegirOtroPlan
        )
        RegistroIncidenteTipo.USUARIO_BANEADO -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Acceso denegado",
            mensaje = customMensaje ?: "Tu acceso está restringido. Contacta a soporte para más información.",
            icono = Icons.Default.GppBad,
            accionPrincipal = RegistroIncidenteAccion.ContactarSoporte
        )
        RegistroIncidenteTipo.RUC_DUPLICADO_EN_COLA -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Solicitud en proceso",
            mensaje = "Este RUC ya tiene una solicitud de registro pendiente de aprobación. Por favor, espera a que sea procesada o contacta a soporte.",
            icono = Icons.Default.Warning,
            accionPrincipal = RegistroIncidenteAccion.ContactarSoporte
        )
        RegistroIncidenteTipo.ERROR_BASE_DATOS -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Error de base de datos",
            mensaje = customMensaje?.takeIf { it.isNotBlank() } ?: "Hubo un problema al conectar con el servidor. Por favor, inténtalo de nuevo.",
            icono = Icons.Default.Error,
            accionPrincipal = RegistroIncidenteAccion.Reintentar
        )
        RegistroIncidenteTipo.ERROR_DESCONOCIDO -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Error al procesar solicitud",
            mensaje = customMensaje?.takeIf { it.isNotBlank() } ?: "Ocurrió un problema durante el registro. Inténtalo de nuevo en unos minutos.",
            icono = Icons.Default.Error,
            accionPrincipal = RegistroIncidenteAccion.Reintentar
        )
        RegistroIncidenteTipo.DATOS_IDENTICOS -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Sin cambios detectados",
            mensaje = "Debes corregir o cambiar al menos un dato antes de reenviar.",
            icono = Icons.Default.Warning,
            accionPrincipal = RegistroIncidenteAccion.Descartar
        )
        RegistroIncidenteTipo.CORRECCION_CONFLICTO -> RegistroIncidenteUi(
            tipo = tipo,
            titulo = "Conflicto de edición",
            mensaje = "Este registro fue modificado por otro usuario. Recarga antes de continuar.",
            icono = Icons.Default.Warning,
            accionPrincipal = RegistroIncidenteAccion.RecargarSolicitud,
            accionSecundaria = RegistroIncidenteAccion.IrALogin
        )
    }
}
