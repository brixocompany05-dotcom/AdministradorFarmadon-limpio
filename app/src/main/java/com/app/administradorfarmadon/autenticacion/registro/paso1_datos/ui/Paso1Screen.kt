package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos.Paso1UiState
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises

@Composable
fun PasoDatosNegocioForm(
    state: Paso1UiState,
    esCorreccion: Boolean,
    motivoRechazo: String?,
    camposACorregir: List<String>,
    onFieldChanged: (String, String) -> Unit,
    onPaisSeleccionado: (String, String, String) -> Unit,
    onNextStep: () -> Unit,
    s: MedidaAdaptativa,
    onMapClick: (String) -> Unit,
    isProgrammaticScroll: MutableState<Boolean>,
    onFocus: () -> Unit,
) {
    val fNombre = remember { FocusRequester() }
    val fDueno = remember { FocusRequester() }
    val fEmail = remember { FocusRequester() }
    val fTel = remember { FocusRequester() }
    val fRuc = remember { FocusRequester() }
    val fPass = remember { FocusRequester() }
    val fPassConfirm = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Cero auto-foco ni auto-apertura de teclado al entrar:
    // Permite que la persona lea la pantalla cómodamente primero. El teclado solo
    // se abre cuando la persona toca explícitamente un campo de texto.

    // REGLA DE PRODUCTO: el país se elige PRIMERO; hasta entonces todo el resto
    // del formulario permanece bloqueado (moneda, prefijo y documento dependen de él).
    val paisElegido = state.paisIso.isNotBlank()
    val cfgPais = CatalogoPaises.porIso(state.paisIso)

    val esEditable: (String) -> Boolean = { campo ->
        if (!esCorreccion) true
        else when (campo) {
            "email" -> false
            "ruc" -> false
            "nombreFarmacia" -> camposACorregir.isEmpty() || camposACorregir.any {
                it.equals(
                    "nombreFarmacia",
                    ignoreCase = true
                ) || it.equals("nombre", ignoreCase = true)
            }

            "dueno" -> camposACorregir.isEmpty() || camposACorregir.any {
                it.equals(
                    "dueno",
                    ignoreCase = true
                ) || it.equals("titular", ignoreCase = true)
            }

            "telefono" -> camposACorregir.isEmpty() || camposACorregir.any {
                it.equals(
                    "telefono",
                    ignoreCase = true
                ) || it.equals("celular", ignoreCase = true)
            }

            "direccion" -> camposACorregir.isEmpty() || camposACorregir.any {
                it.equals(
                    "direccion",
                    ignoreCase = true
                ) || it.equals("ubicacion", ignoreCase = true) || it.equals(
                    "ubicacionGeo",
                    ignoreCase = true
                )
            }

            else -> camposACorregir.isEmpty() || camposACorregir.any {
                it.equals(
                    campo,
                    ignoreCase = true
                )
            }
        }
    }

    val tieneAdvertencia: (String) -> Boolean = { campo ->
        if (camposACorregir.isEmpty()) false
        else when (campo) {
            "nombreFarmacia" -> camposACorregir.any {
                it.equals(
                    "nombreFarmacia",
                    ignoreCase = true
                ) || it.equals("nombre", ignoreCase = true)
            }

            "dueno" -> camposACorregir.any {
                it.equals(
                    "dueno",
                    ignoreCase = true
                ) || it.equals("titular", ignoreCase = true)
            }

            "email" -> camposACorregir.any {
                it.equals(
                    "email",
                    ignoreCase = true
                ) || it.equals("correo", ignoreCase = true)
            }

            "telefono" -> camposACorregir.any {
                it.equals(
                    "telefono",
                    ignoreCase = true
                ) || it.equals("celular", ignoreCase = true)
            }

            "ruc" -> camposACorregir.any {
                it.equals("ruc", ignoreCase = true) || it.equals(
                    "dni",
                    ignoreCase = true
                ) || it.equals("documento", ignoreCase = true)
            }

            "direccion" -> camposACorregir.any {
                it.equals(
                    "direccion",
                    ignoreCase = true
                ) || it.equals("ubicacion", ignoreCase = true) || it.equals(
                    "ubicacionGeo",
                    ignoreCase = true
                )
            }

            else -> camposACorregir.any { it.equals(campo, ignoreCase = true) }
        }
    }

    val jumpToNextEmpty: () -> Unit = {
        val targets = listOf(
            Triple(state.nombreFarmacia, fNombre, "nombreFarmacia"),
            Triple(state.dueno, fDueno, "dueno"),
            Triple(state.email, fEmail, "email"),
            Triple(state.telefono, fTel, "telefono"),
            Triple(state.ruc, fRuc, "ruc"),
            Triple(state.contrasena, fPass, "contrasena"),
            Triple(state.confirmarContrasena, fPassConfirm, "confirmarContrasena"),
        )

        val firstTarget = if (esCorreccion && camposACorregir.isNotEmpty()) {
            targets.firstOrNull { (_, _, key) -> (esEditable(key) && (key != "email")) }
        } else {
            targets.firstOrNull { it.first.trim().isEmpty() }
        }

        if (firstTarget != null) {
            firstTarget.second.requestFocus()
        } else {
            focusManager.clearFocus()
            onNextStep()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(s.gapXLarge)
    ) {
        if (esCorreccion && !motivoRechazo.isNullOrBlank()) {
            Surface(
                color = FDColors.WarningSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Warning),
                shape = FDShapes.Medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(s.gapMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = FDColors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Solicitud observada por la central",
                            style = FDType.Label.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.Warning
                        )
                        Text(
                            text = motivoRechazo,
                            style = FDType.BodySmall,
                            color = FDColors.TextSecondary
                        )
                        if (camposACorregir.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Solo los campos indicados están habilitados para edición.",
                                style = FDType.Label,
                                color = FDColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }

        RegistroSection(title = "IDENTIFICACIÓN Y CONTACTO", s = s) {
            // ── PAÍS DE OPERACIÓN: PRIMERO, sin excepciones ──
            // La moneda y el prefijo nacen del país y viajan internos.
            // MIENTRAS NO HAYA PAÍS el selector vive SIEMPRE: elegirlo es
            // prerrequisito, no una "corrección" más (cero deadlocks en
            // correcciones legadas cuyo país nunca existió).
            com.app.administradorfarmadon.organizacion.ui.componentes.SelectorPaisProfesional(
                paisIso = state.paisIso,
                onPaisSeleccionado = { pais ->
                    onPaisSeleccionado(
                        pais.iso,
                        pais.monedaIso,
                        pais.simboloMoneda
                    )
                },
                habilitado = esEditable("pais") || !paisElegido,
                esObligatorio = true,
                error = state.erroresCampos["pais"],
                advertencia = if (tieneAdvertencia("pais")) "La central solicitó corregir este campo" else null
            )
            if (!paisElegido) {
                Text(
                    text = "Elige tu país para habilitar el formulario: define tu documento, teléfono y moneda.",
                    style = FDType.Label,
                    color = FDColors.TextTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                val warnNombre = tieneAdvertencia("nombreFarmacia")
                val editNombre = esEditable("nombreFarmacia")
                RegistroTextField(
                    label = "Nombre de la Farmacia *",
                    value = state.nombreFarmacia,
                    onValueChange = { if (editNombre && paisElegido) onFieldChanged("nombreFarmacia", it) },
                    onFocus = onFocus,
                    readOnly = !editNombre || !paisElegido,
                    error = state.erroresCampos["nombreFarmacia"],
                    isWarning = warnNombre,
                    warningMessage = if (warnNombre) "La central solicitó corregir este campo" else null,
                    focusRequester = fNombre,
                    keyboardType = KeyboardType.Text,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s, modifier = Modifier.weight(1f)
                )

                val warnDueno = tieneAdvertencia("dueno")
                val editDueno = esEditable("dueno")
                RegistroTextField(
                    label = "Nombre del Dueño *",
                    value = state.dueno,
                    onValueChange = { if (editDueno && paisElegido) onFieldChanged("dueno", it) },
                    onFocus = onFocus,
                    readOnly = !editDueno || !paisElegido,
                    error = state.erroresCampos["dueno"],
                    isWarning = warnDueno,
                    warningMessage = if (warnDueno) "La central solicitó corregir este campo" else null,
                    focusRequester = fDueno,
                    keyboardType = KeyboardType.Text,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s, modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                val warnEmail = tieneAdvertencia("email")
                RegistroTextField(
                    label = "Correo Electrónico *",
                    value = state.email,
                    onValueChange = { if (paisElegido) onFieldChanged("email", it) },
                    onFocus = onFocus,
                    readOnly = esCorreccion || !paisElegido,
                    error = if (esCorreccion) null else state.erroresCampos["email"],
                    isWarning = warnEmail,
                    warningMessage = if (warnEmail) "La central solicitó corregir este campo" else null,
                    keyboardType = KeyboardType.Email,
                    focusRequester = fEmail,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s, modifier = Modifier.weight(1f)
                )

                val warnTel = tieneAdvertencia("telefono")
                val editTel = esEditable("telefono")
                RegistroTextField(
                    label = "Teléfono *",
                    value = state.telefono,
                    onValueChange = { if (editTel && paisElegido) onFieldChanged("telefono", it) },
                    readOnly = !editTel || !paisElegido,
                    error = state.erroresCampos["telefono"],
                    isWarning = warnTel,
                    warningMessage = if (warnTel) "La central solicitó corregir este campo" else null,
                    keyboardType = KeyboardType.Phone,
                    visualTransformation = PhoneNumberVisualTransformation(),
                    prefixText = CatalogoPaises.prefijoTel(state.paisIso).ifBlank { null },
                    onFocus = onFocus,
                    focusRequester = fTel,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s, modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                val warnRuc = tieneAdvertencia("ruc")
                val editRuc = esEditable("ruc")
                val hintDocumento = when {
                    cfgPais == null -> "Elige el país para definir tu documento"
                    else -> "${cfgPais.documentoNombre}: entre ${cfgPais.docMin} y ${cfgPais.docMax} ${if (cfgPais.docSoloNumeros) "dígitos" else "caracteres"}"
                }
                RegistroTextField(
                    label = "${cfgPais?.documentoNombre ?: "Documento"} *",
                    value = state.ruc,
                    onValueChange = { if (editRuc && paisElegido) onFieldChanged("ruc", it) },
                    onFocus = onFocus,
                    readOnly = !editRuc || !paisElegido,
                    error = state.erroresCampos["ruc"],
                    isWarning = warnRuc,
                    warningMessage = if (warnRuc) "La central solicitó corregir este campo" else null,
                    keyboardType = if (cfgPais?.docSoloNumeros != false) KeyboardType.Number else KeyboardType.Text,
                    placeholder = hintDocumento,
                    focusRequester = fRuc,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s, modifier = Modifier.fillMaxWidth()
                )
            }

            val warnDir = tieneAdvertencia("direccion")
            val editDir = esEditable("direccion")
            FDTarjetaDireccionProfesional(
                direccion = state.direccion,
                onClick = { if (editDir && paisElegido) onMapClick("direccion") },
                habilitado = editDir && paisElegido,
                error = state.erroresCampos["direccion"],
                advertencia = if (warnDir) "La central solicitó corregir este campo" else null,
                s = s
            )
        }

        if (!esCorreccion) {
            RegistroSection(title = "SEGURIDAD DE LA CUENTA", s = s) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        RegistroTextField(
                            label = "Contraseña *",
                            value = state.contrasena,
                            onValueChange = { if (paisElegido) onFieldChanged("contrasena", it) },
                            onFocus = onFocus,
                            readOnly = !paisElegido,
                            error = state.erroresCampos["contrasena"],
                            keyboardType = KeyboardType.Password,
                            showPasswordToggle = true,
                            focusRequester = fPass,
                            onImeAction = jumpToNextEmpty,
                            isProgrammaticScroll = isProgrammaticScroll,
                            s = s
                        )
                        ChecklistContrasena(state.contrasena, s)
                    }
                    RegistroTextField(
                        label = "Confirmar Contraseña *",
                        value = state.confirmarContrasena,
                        onValueChange = { if (paisElegido) onFieldChanged("confirmarContrasena", it) },
                        onFocus = onFocus,
                        readOnly = !paisElegido,
                        error = state.erroresCampos["confirmarContrasena"],
                        keyboardType = KeyboardType.Password,
                        showPasswordToggle = true,
                        imeAction = ImeAction.Done,
                        focusRequester = fPassConfirm,
                        onImeAction = jumpToNextEmpty,
                        isProgrammaticScroll = isProgrammaticScroll,
                        s = s, modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            RegistroSection(title = "ACLARACIÓN O DESCARGO PARA EL AUDITOR DE BRIXO", s = s) {
                RegistroTextField(
                    label = "Mensaje de Aclaración a BRIXO (Opcional)",
                    value = state.notaAclaratoria,
                    onValueChange = { if (paisElegido) onFieldChanged("notaAclaratoria", it) },
                    onFocus = onFocus,
                    readOnly = !paisElegido,
                    placeholder = "Explica cualquier detalle o justificación sobre los datos corregidos...",
                    keyboardType = KeyboardType.Text,
                    onImeAction = jumpToNextEmpty,
                    isProgrammaticScroll = isProgrammaticScroll,
                    s = s,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun RegistroSection(
    title: String,
    s: MedidaAdaptativa,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
        Text(
            text = title,
            style = FDType.Label.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
            color = FDColors.TextTertiary
        )
        Surface(
            color = FDColors.SurfaceElevated,
            shape = FDShapes.Large,
            border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Border),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                content()
            }
        }
    }
}
