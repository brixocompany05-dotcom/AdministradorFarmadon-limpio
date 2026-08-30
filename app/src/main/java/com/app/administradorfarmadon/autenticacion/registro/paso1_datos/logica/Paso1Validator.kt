package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica

import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos.Paso1UiState
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises

object Paso1Validator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    fun validar(state: Paso1UiState, esCorreccion: Boolean): Map<String, String> {
        val errores = mutableMapOf<String, String>()

        val pass = state.contrasena
        val passConfirm = state.confirmarContrasena

        if (!esCorreccion || pass.isNotEmpty() || passConfirm.isNotEmpty()) {
            if (pass.length < 8) errores["contrasena"] = "Mínimo 8 caracteres"
            else if (!pass.any { it.isUpperCase() }) errores["contrasena"] = "Debe tener 1 mayúscula"
            else if (!pass.any { it.isDigit() }) errores["contrasena"] = "Debe tener 1 número"

            if (pass != passConfirm) {
                errores["confirmarContrasena"] = "Las contraseñas no coinciden"
            }
        }

        // País primero: sin país no hay reglas de documento ni teléfono.
        val pais = CatalogoPaises.porIso(state.paisIso)
        if (pais == null) {
            errores["pais"] = "Elige el país de operación"
        }

        val nombre = state.nombreFarmacia.trim()
        val dueno = state.dueno.trim()
        val email = state.email.trim().lowercase()
        val dir = state.direccion.trim()

        if (nombre.length < 3) errores["nombreFarmacia"] = "Mínimo 3 caracteres"
        if (dueno.length < 3) errores["dueno"] = "Mínimo 3 caracteres"

        if (email.isEmpty() || !EMAIL_REGEX.matches(email)) {
            errores["email"] = "Correo electrónico inválido"
        }

        // Teléfono: dígitos locales SIN el prefijo del país (prefijo es visual).
        // Guarda universal E.164 (7—“15 dígitos); la fuerte es del agente BRIXO.
        val telLimpio = state.telefono.filter { it.isDigit() }
        if (telLimpio.length !in 7..15) {
            errores["telefono"] = "Teléfono inválido (solo números, sin prefijo)"
        }

        // Documento según la regla del país: se quitan espacios/puntos/guiones ──†’
        // longitud en rango + tipo correcto (números o alfanumérico).
        val docLimpio = state.ruc.trim().filter { it.isLetterOrDigit() }
        if (pais != null && (
                    docLimpio.isEmpty() ||
                            docLimpio.length !in pais.docMin..pais.docMax ||
                            (pais.docSoloNumeros && !docLimpio.all { it.isDigit() })
                    )
        ) {
            errores["ruc"] = "${pais.documentoNombre}: entre ${pais.docMin} y ${pais.docMax} ${if (pais.docSoloNumeros) "dígitos" else "caracteres"}"
        }

        if (dir.isEmpty()) errores["direccion"] = "La dirección es requerida"

        return errores
    }
}
