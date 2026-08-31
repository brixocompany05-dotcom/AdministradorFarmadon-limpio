package com.app.administradorfarmadon.configuracion.usuarios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.ExecutiveInput
import com.app.administradorfarmadon.configuracion.usuarios.logica.UsuariosUiState
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

/**
 * Panel Gestión Personal — Geometría Física 2026
 * Documento 58% + Liquidación 42%, simetría viewport-proporcional.
 * Cero Color.White fijo: todo token adaptativo claro/oscuro.
 * Todo tamaño deriva de MedidaAdaptativa s (física 1280 ^0.55) + viewport proporcional.
 * Sin scroll como parche: el contenido cabe en vista mediante gaps/card/padding escalados;
 * scroll solo fallback suave si herramientas >6 en pantallas muy bajas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioFormularioPanel(
    state: UsuariosUiState,
    onFieldChanged: (String, String) -> Unit,
    onRolSelected: (String, String) -> Unit,
    onSucursalSelected: (String, String) -> Unit,
    onAccesoChanged: (Boolean) -> Unit,
    onPermisoModuloChanged: (String, Boolean) -> Unit = { _, _ -> },
    onToggleTodosPermisos: (Boolean) -> Unit = {},
    onReintentarHerramientas: () -> Unit = {},
    onGuardar: () -> Unit,
    onSolicitarSuspender: () -> Unit,
    onSolicitarEliminar: () -> Unit,
    onEnviarRestablecimiento: () -> Unit,
    onDominioSeleccionado: (String) -> Unit,
    onCerrarPanel: () -> Unit,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    var menuSedesExpandido by remember { mutableStateOf(false) }
    var pestanaActiva by remember { mutableIntStateOf(0) }
    val pestanas = listOf("IDENTIDAD" to "Datos personales", "ACCESO" to "Rol y herramientas", "SEGURIDAD" to "Clave y baja")
    val scrollState = rememberScrollState()

    val nombreMostrar = if (state.esModoCreacion) state.formNombre.ifBlank { "Nuevo Colaborador" } else state.formNombre
    val esRecontratacion = !state.esModoCreacion && state.usuarioSeleccionado?.dadoDeBaja == true
    val partes = nombreMostrar.trim().split(" ").filter { it.isNotBlank() }
    val iniciales = when {
        partes.size >= 2 -> "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        partes.isNotEmpty() -> partes[0].take(2).uppercase()
        else -> "US"
    }
    val tieneErroresActivos = state.formErrores.any { it.value.isNotBlank() }
    val puedeGuardar = if (state.esModoCreacion) {
        !state.guardando &&
            state.formNombre.isNotBlank() &&
            state.formDni.isNotBlank() &&
            state.formTelefono.isNotBlank() &&
            state.formEmail.isNotBlank() &&
            state.formPassword.isNotBlank() &&
            !tieneErroresActivos
    } else {
        !state.guardando && (state.hayCambiosSinGuardar || esRecontratacion) && !tieneErroresActivos
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(s.radiusCard * 1.25f),
        color = colores.cardBase,
        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── IZQUIERDA 58% — Documento continuo (gaps y paddings geométricos s.* ) ──
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(horizontal = s.padCardLarge, vertical = s.padCard)
            ) {
                // Cabecera — avatar 48dp base escalado via s.iconLarge*1.7
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                        Box(
                            modifier = Modifier.size(s.iconLarge * 1.7f).clip(RoundedCornerShape(s.radiusChip))
                                .background(if (esRecontratacion) colores.estadoExito.copy(alpha=0.12f) else colores.fondoBase)
                                .border(s.borderWidth, if (esRecontratacion) colores.estadoExito.copy(alpha=0.3f) else colores.cardBorde, RoundedCornerShape(s.radiusChip)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iniciales, style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Black), color = if (esRecontratacion) colores.estadoExito else colores.textoPrincipal)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (state.esModoCreacion) "Registrar colaborador" else nombreMostrar,
                                style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp * 0.95f, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                                color = colores.textoPrincipal
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                                Box(Modifier.size(s.xs * 0.9f).clip(CircleShape).background(if (state.formAcceso) colores.estadoExito else colores.estadoPeligro))
                                Text(if (state.formAcceso) "HABILITADO" else "SUSPENDIDO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = if (state.formAcceso) colores.estadoExito else colores.estadoPeligro)
                                if (state.formRolNombre.isNotBlank()) {
                                    Text("·", color = colores.textoTerciario, style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp))
                                    Text(state.formRolNombre.uppercase(), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Medium), color = colores.textoTerciario)
                                }
                            }
                        }
                    }
                    IconButton(onClick = onCerrarPanel, modifier = Modifier.size(s.btnSmallH).clip(RoundedCornerShape(s.radiusChip)).background(colores.fondoBase).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip))) {
                        Icon(Icons.Default.Close, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                    }
                }

                Spacer(Modifier.height(s.sm))

                // Tabs underline — gaps s.sm
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.lg)) {
                    pestanas.forEachIndexed { index, (titulo, subtitulo) ->
                        val activa = pestanaActiva == index
                        Column(
                            modifier = Modifier.clickable { pestanaActiva = index }.padding(bottom = s.xs * 0.8f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(titulo, style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = if (activa) FontWeight.Black else FontWeight.Medium, fontSize = s.textLabel.value.sp, letterSpacing = 0.6.sp), color = if (activa) colores.textoPrincipal else colores.textoTerciario)
                            Text(subtitulo, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.9f), color = if (activa) colores.textoSecundario else colores.textoTerciario.copy(alpha=0.7f))
                            Box(Modifier.height(2.5.dp).width(if (activa) s.lg else 0.dp).clip(CircleShape).background(FDColors.Primary))
                        }
                    }
                }
                HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.35f), thickness = s.separatorH)
                Spacer(Modifier.height(s.sm))

                // Contenido — simetría: gap adaptativo s.md, imePadding teclado primero
                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).imePadding(), verticalArrangement = Arrangement.spacedBy(s.md)) {
                    when (pestanaActiva) {
                        0 -> {
                            Surface(color = colores.fondoBase, shape = RoundedCornerShape(s.radiusChip), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha=0.4f))) {
                                Row(modifier = Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                                    Icon(Icons.Default.Badge, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                                    Text("Verificamos identidad con DNI y correo único en todo el ecosistema.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario, modifier = Modifier.weight(1f))
                                }
                            }
                            ExecutiveInput(s = s, label = "Nombre completo", value = state.formNombre, icon = Icons.Default.Person, placeholder = "Nombre y apellidos", errorText = state.formErrores["nombre"], onValueChange = { onFieldChanged("nombre", it) })
                            Row(horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveInput(s = s, label = "DNI / Documento", value = state.formDni, icon = Icons.Default.Badge, placeholder = "8 a 12 dígitos", keyboardType = KeyboardType.Number, errorText = state.formErrores["dni"], onValueChange = { onFieldChanged("dni", it) })
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveInput(s = s, label = "Teléfono móvil", value = state.formTelefono, icon = Icons.Default.Phone, placeholder = "9 dígitos", keyboardType = KeyboardType.Phone, errorText = state.formErrores["telefono"], onValueChange = { onFieldChanged("telefono", it) })
                                }
                            }
                            var menuDominioExpandido by remember { mutableStateOf(false) }
                            val dominios = listOf("@gmail.com", "@outlook.com", "@hotmail.com", "@yahoo.com")
                            ExecutiveInput(
                                s = s, label = "Correo electrónico (único para acceso)", value = state.formEmail, icon = Icons.Default.Email,
                                placeholder = "correo del colaborador", keyboardType = KeyboardType.Email, errorText = state.formErrores["email"], readOnly = !state.esModoCreacion,
                                onValueChange = { onFieldChanged("email", it) },
                                trailing = {
                                    Box {
                                        TextButton(onClick = { menuDominioExpandido = true }, enabled = state.esModoCreacion, contentPadding = PaddingValues(horizontal = s.xs)) {
                                            Text("@", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                        }
                                        DropdownMenu(expanded = menuDominioExpandido, onDismissRequest = { menuDominioExpandido = false }) {
                                            dominios.forEach { dom ->
                                                DropdownMenuItem(text = { Text(dom, style = TokensFarmadon.tipografia.cuerpoPequeno) }, onClick = { onDominioSeleccionado(dom); menuDominioExpandido = false })
                                            }
                                        }
                                    }
                                }
                            )
                            if (!state.esModoCreacion) {
                                Text(
                                    "El correo de acceso no se edita aquí: es la identidad con la que el colaborador entra al sistema. Para recuperar la clave usa «Restablecer acceso» en la pestaña SEGURIDAD.",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.92f),
                                    color = colores.textoTerciario
                                )
                            }
                        }
                        1 -> {
                            // Banner plan — tokens, cero Color.White
                            Surface(color = colores.textoPrincipal.copy(alpha = 0.04f), shape = RoundedCornerShape(s.radiusCard * 0.85f), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.textoPrincipal.copy(alpha=0.08f))) {
                                Row(modifier = Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                    Box(Modifier.size(s.iconLarge * 1.4f).clip(RoundedCornerShape(s.radiusChip)).background(colores.cardElevada).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WorkspacePremium, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("PLAN CONTRATADO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp), color = colores.textoTerciario)
                                        Text(if (state.cargandoHerramientas) "Sincronizando…" else state.planNombre.ifBlank { "Plan de la farmacia" }, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                        Text(if (state.cargandoHerramientas) "—" else "${state.totalHerramientasPlan} herramientas incluidas", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoSecundario)
                                    }
                                    if (!state.cargandoHerramientas) {
                                        Surface(color = colores.textoPrincipal, shape = RoundedCornerShape(s.radiusChip)) { Text("${state.totalHerramientasPlan}", modifier = Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.7f), style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black), color = colores.textoInvertido) }
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                                Text("ROL EN LA FARMACIA", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp), color = colores.textoTerciario)
                                Row(modifier = Modifier.fillMaxWidth().height(s.inputMinH).clip(RoundedCornerShape(s.radiusInput)).background(colores.fondoBase).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    state.roles.forEach { rol ->
                                        val sel = state.formRolId == rol.id
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(s.radiusChip))
                                                .background(if (sel) colores.cardElevada else colores.cardBase.copy(alpha=0.0f))
                                                .border(if (sel) s.borderWidth else 0.dp, if (sel) colores.cardBorde else colores.cardBase.copy(alpha=0f), RoundedCornerShape(s.radiusChip))
                                                .clickable { onRolSelected(rol.id, rol.nombre) }
                                                .bounceClick(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(rol.nombre, style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = if (sel) FontWeight.Black else FontWeight.Medium, fontSize = s.textLabel.value.sp), color = if (sel) colores.textoPrincipal else colores.textoTerciario)
                                        }
                                    }
                                }
                                if (state.formErrores["rol"] != null) Text(state.formErrores["rol"]!!, color = colores.estadoPeligro, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("QUÉ PODRÁ OPERAR ESTE USUARIO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp), color = colores.textoTerciario)
                                        Text("${state.totalHerramientasHabilitadas} de ${state.totalHerramientasPlan} activas", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoSecundario)
                                    }
                                    if (state.herramientasPlan.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                                            TextButton(onClick = { onToggleTodosPermisos(true) }, contentPadding = PaddingValues(horizontal = s.xs)) { Text("Todas", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Primary) }
                                            Text("·", color = colores.cardBorde)
                                            TextButton(onClick = { onToggleTodosPermisos(false) }, contentPadding = PaddingValues(horizontal = s.xs)) { Text("Ninguna", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), color = colores.textoTerciario) }
                                        }
                                    }
                                }
                                if (state.cargandoHerramientas) {
                                    Box(Modifier.fillMaxWidth().height(s.inputMinH * 1.4f).clip(RoundedCornerShape(s.radiusInput)).background(colores.fondoBase), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(s.iconSmall), color = colores.textoTerciario, strokeWidth = 2.dp) }
                                } else if (state.herramientasPlan.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(s.radiusInput)).background(colores.fondoBase).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput)).padding(s.sm)) {
                                        Text("Tu plan no tiene herramientas activas. Contacta a BRIXO.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.clip(RoundedCornerShape(s.radiusInput)).background(colores.fondoBase).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput))) {
                                        state.herramientasPlan.forEachIndexed { idx, modulo ->
                                            val on = state.formPermisosModulos[modulo.modulo] ?: true
                                            Row(modifier = Modifier.fillMaxWidth().clickable { onPermisoModuloChanged(modulo.modulo, !on) }.padding(horizontal = s.sm, vertical = s.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm), modifier = Modifier.weight(1f)) {
                                                    Box(Modifier.size(s.iconMedium * 1.25f).clip(RoundedCornerShape(s.radiusChip * 0.8f)).background(if (on) FDColors.Primary.copy(alpha=0.12f) else colores.cardElevada).border(s.borderWidth, if (on) FDColors.Primary.copy(alpha=0.2f) else colores.cardBorde, RoundedCornerShape(s.radiusChip * 0.8f)), contentAlignment = Alignment.Center) {
                                                        Icon(if (on) Icons.Default.Check else Icons.Default.Block, null, tint = if (on) FDColors.Primary else colores.textoTerciario, modifier = Modifier.size(s.iconSmall * 0.9f))
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                        Text(modulo.nombre, style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.SemiBold, fontSize = s.textBody.value.sp), color = if (on) colores.textoPrincipal else colores.textoSecundario)
                                                        Text("${modulo.categoria} · ${modulo.modulo}", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f), color = colores.textoTerciario)
                                                    }
                                                }
                                                Switch(checked = on, onCheckedChange = { onPermisoModuloChanged(modulo.modulo, it) }, colors = SwitchDefaults.colors(checkedThumbColor = colores.cardBase, checkedTrackColor = FDColors.Primary, uncheckedThumbColor = colores.cardBase, uncheckedTrackColor = colores.cardBorde))
                                            }
                                            if (idx < state.herramientasPlan.lastIndex) HorizontalDivider(color = colores.cardBorde.copy(alpha=0.5f), thickness = s.separatorH)
                                        }
                                    }
                                }
                            }
                            // Sede + acceso — alturas s.inputMinH *0.88 , ningún 46.dp fijo
                            Row(horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                                    Text("SEDE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                                    Box(modifier = Modifier.fillMaxWidth().height(s.inputMinH * 0.88f).clip(RoundedCornerShape(s.radiusInput)).background(colores.inputFondo).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput)).clickable { menuSedesExpandido = true }.padding(horizontal = s.sm), contentAlignment = Alignment.CenterStart) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                                            Icon(Icons.Default.Storefront, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                                            Text(state.formSucursalNombre, style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp), color = colores.textoPrincipal, maxLines = 1)
                                        }
                                    }
                                    DropdownMenu(expanded = menuSedesExpandido, onDismissRequest = { menuSedesExpandido = false }) {
                                        state.sucursales.forEach { suc ->
                                            DropdownMenuItem(text = { Text(suc.nombre, style = TokensFarmadon.tipografia.cuerpo) }, onClick = { onSucursalSelected(suc.id, suc.nombre); menuSedesExpandido = false })
                                        }
                                        DropdownMenuItem(text = { Text("Todas las Sedes (Itinerante)", style = TokensFarmadon.tipografia.cuerpo) }, onClick = { onSucursalSelected("todas", "Todas las Sedes"); menuSedesExpandido = false })
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                                    Text("ACCESO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                                    val esPropio = !state.esModoCreacion && state.usuarioSeleccionado?.id == state.usuarioActualUid
                                    Row(modifier = Modifier.height(s.inputMinH * 0.88f).clip(RoundedCornerShape(s.radiusInput)).background(colores.fondoBase).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput)).padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        listOf(true to "On", false to "Off").forEach { (v, l) ->
                                            val sel = state.formAcceso == v
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(s.radiusChip)).background(if (sel) colores.cardElevada else colores.cardBase.copy(alpha=0f)).border(if (sel) s.borderWidth else 0.dp, if (sel) colores.cardBorde else colores.cardBase.copy(alpha=0f), RoundedCornerShape(s.radiusChip)).clickable(enabled = v || !esPropio) { onAccesoChanged(v) }, contentAlignment = Alignment.Center) {
                                                Text(l, style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = s.textLabel.value.sp), color = if (sel) colores.textoPrincipal else colores.textoTerciario)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (state.esModoCreacion) {
                                ExecutiveInput(s = s, label = "Contraseña inicial", value = state.formPassword, icon = Icons.Default.Lock, isPassword = true, placeholder = "Mínimo 6 caracteres", errorText = state.formErrores["password"], onValueChange = { onFieldChanged("password", it) })
                                Surface(color = FDColors.Primary.copy(alpha=0.06f), shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, FDColors.Primary.copy(alpha=0.15f))) {
                                    Row(modifier = Modifier.padding(s.sm), horizontalArrangement = Arrangement.spacedBy(s.xs), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall))
                                        Text("Se creará el acceso con esta clave. El usuario podrá cambiarla luego.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoSecundario, modifier = Modifier.weight(1f))
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                                    Surface(color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard * 0.85f), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                                        Row(modifier = Modifier.padding(s.padCard), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("Restablecer contraseña", style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.Bold, fontSize = s.textBody.value.sp), color = colores.textoPrincipal)
                                                Text("Enviamos un correo para que cree una nueva clave.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoTerciario)
                                            }
                                            FilledTonalButton(onClick = onEnviarRestablecimiento, shape = RoundedCornerShape(s.radiusChip)) { Text("ENVIAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold)) }
                                        }
                                    }
                                    HorizontalDivider(color = colores.cardBorde.copy(alpha=0.4f), thickness = s.separatorH)
                                    Text("ZONA SENSIBLE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp), color = colores.estadoPeligro)
                                    OutlinedButton(onClick = onSolicitarEliminar, modifier = Modifier.fillMaxWidth().height(s.btnMediumH), shape = RoundedCornerShape(s.radiusInput), colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.estadoPeligro), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha=0.25f))) {
                                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall))
                                        Spacer(Modifier.width(s.xs))
                                        Text("Dar de baja a este colaborador", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
                                    }
                                    Text("Se conserva el historial y podrá recontratarse. No borra el acceso de forma irreversible.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoTerciario)
                                }
                            }
                        }
                    }
                }
            }

            // Divisor vertical — 1dp token
            Box(modifier = Modifier.width(s.separatorH).fillMaxHeight().background(colores.cardBorde.copy(alpha=0.5f)))

            // ── DERECHA 42% — Liquidación viva (paddings y radios s.*) ──
            Column(
                modifier = Modifier.weight(0.42f).fillMaxHeight().background(colores.fondoBase.copy(alpha=0.55f)).padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                // Preview — colores.cardElevada, nunca Color.White
                Surface(color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde), shadowElevation = 0.dp) {
                    Column(modifier = Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(s.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                            Box(Modifier.size(s.iconLarge * 1.85f).clip(CircleShape).background(colores.textoPrincipal).padding(1.dp).clip(CircleShape).background(colores.cardElevada).padding(2.dp).clip(CircleShape).background(colores.textoPrincipal), contentAlignment = Alignment.Center) {
                                Text(iniciales, color = colores.textoInvertido, style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Black))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(nombreMostrar.ifBlank { "—" }, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Bold), color = colores.textoPrincipal, maxLines = 1)
                                Text(state.formEmail.ifBlank { "sin correo" }, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoTerciario, maxLines = 1)
                            }
                            Box(Modifier.size(s.xs * 1.25f).clip(CircleShape).background(if (state.formAcceso) colores.estadoExito else colores.estadoPeligro).border(2.dp, colores.cardElevada, CircleShape))
                        }
                        HorizontalDivider(color = colores.cardBorde.copy(alpha=0.4f), thickness = s.separatorH)
                        Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(s.xs * 0.9f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Work, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                Text(state.formRolNombre.ifBlank { "—" }, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontWeight = FontWeight.Medium, fontSize = s.textBody.value.sp * 0.92f), color = colores.textoPrincipal)
                                Text("·", color = colores.cardBorde)
                                Icon(Icons.Default.Storefront, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                Text(state.formSucursalNombre, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(s.xs * 0.9f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                Text(state.formDni.ifBlank { "—" }, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario)
                                if (state.formTelefono.isNotBlank()) {
                                    Text("·", color = colores.cardBorde)
                                    Icon(Icons.Default.Phone, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                    Text(state.formTelefono, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario)
                                }
                            }
                        }
                    }
                }

                Surface(color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard * 0.85f), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                    Column(modifier = Modifier.padding(s.sm), verticalArrangement = Arrangement.spacedBy(s.xs)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ACCESOS", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp), color = colores.textoTerciario)
                            Surface(color = if (state.totalHerramientasHabilitadas == state.totalHerramientasPlan) FDColors.Primary else colores.textoTerciario, shape = RoundedCornerShape(s.radiusChip * 0.8f)) {
                                Text("${state.totalHerramientasHabilitadas}/${state.totalHerramientasPlan}", modifier = Modifier.padding(horizontal = s.xs, vertical = 3.dp), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.Black), color = colores.textoInvertido)
                            }
                        }
                        if (state.herramientasPlan.isEmpty()) {
                            if (state.cargandoHerramientas) {
                                Text("Cargando…", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                            } else if (state.errorHerramientas) {
                                Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                                    Text(
                                        "No se pudo cargar el plan contratado (revisa tu conexión).",
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp),
                                        color = colores.estadoPeligro
                                    )
                                    TextButton(onClick = onReintentarHerramientas, contentPadding = PaddingValues(0.dp)) {
                                        Text("Reintentar", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                    }
                                }
                            } else {
                                Text("Sin herramientas por plan", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)) {
                                state.herramientasPlan.take(6).forEach { m ->
                                    val on = state.formPermisosModulos[m.modulo] ?: true
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.9f)) {
                                        Icon(if (on) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (on) colores.estadoExito else colores.cardBorde, modifier = Modifier.size(s.iconSmall))
                                        Text(m.nombre, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal), color = if (on) colores.textoPrincipal else colores.textoTerciario, modifier = Modifier.weight(1f))
                                    }
                                }
                                if (state.herramientasPlan.size > 6) Text("+${state.herramientasPlan.size - 6} más", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), color = colores.textoTerciario)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onGuardar,
                    enabled = puedeGuardar,
                    modifier = Modifier.fillMaxWidth().height(s.btnLargeH).bounceClick(),
                    shape = RoundedCornerShape(s.radiusButton),
                    colors = ButtonDefaults.buttonColors(containerColor = if (esRecontratacion) colores.estadoExito else FDColors.Primary, contentColor = colores.textoInvertido, disabledContainerColor = colores.cardBorde, disabledContentColor = colores.textoTerciario),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (state.guardando) {
                        CircularProgressIndicator(Modifier.size(s.iconSmall), color = colores.textoInvertido, strokeWidth = 2.dp)
                    } else {
                        Icon(if (esRecontratacion) Icons.Default.RestartAlt else Icons.Default.Check, null, modifier = Modifier.size(s.iconSmall))
                        Spacer(Modifier.width(s.xs))
                        Text(
                            when {
                                state.esModoCreacion -> "Crear y habilitar"
                                esRecontratacion -> "Recontratar"
                                else -> "Guardar cambios"
                            },
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, letterSpacing = 0.7.sp, fontSize = s.textLabel.value.sp)
                        )
                    }
                }
                Text(
                    if (state.esModoCreacion) "Se creará el acceso y la ficha en esta farmacia. El correo debe ser único."
                    else "Los cambios se guardan en esta farmacia y su acceso global.",
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.92f), color = colores.textoTerciario, modifier = Modifier.padding(horizontal = s.xs * 0.5f)
                )
            }
        }
    }
}
