package com.app.administradorfarmadon.autenticacion.registro.paso2_plan.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos.Paso2UiState
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises
import kotlinx.coroutines.delay

@Composable
fun PasoSeleccionPlan(
    state: Paso2UiState,
    esCorreccion: Boolean,
    sugerirCambioPlan: Boolean,
    accionSugerida: String?,
    camposACorregir: List<String> = emptyList(),
    onPlanSelected: (PlanSuscripcion) -> Unit,
    onNextStep: () -> Unit,
    onRetryCargaPlanes: () -> Unit,
    onBackClick: () -> Unit,
    cargando: Boolean,
    resumenNombre: String,
    resumenRuc: String,
    resumenEmail: String,
    paisNombre: String = "",
    s: MedidaAdaptativa,
    incidenteVisible: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Paso 2: Selección de Plan",
            style = TokensFarmadon.tipografia.titulo1,
            color = FDColors.TextPrimary,
            modifier = Modifier.padding(bottom = s.gapXLarge)
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val alturaDisponible = maxHeight
            val compacto = alturaDisponible < 700.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                val avisoTexto = state.aviso
                val esAvisoPlan = avisoTexto?.contains("plan", ignoreCase = true) == true
                val esProblemaConexion = avisoTexto != null && (
                        avisoTexto.contains("internet", ignoreCase = true) ||
                                avisoTexto.contains("servidor", ignoreCase = true)
                        )
                val mostrarAviso = avisoTexto != null
                    && resumenRuc.trim().isNotBlank()
                    && (!esAvisoPlan || state.planSeleccionado?.id?.isNotBlank() == true)
                if (mostrarAviso) {
                    Surface(
                        color = TokensFarmadon.colores.alertaSutil,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TokensFarmadon.colores.estadoAlerta),
                        shape = TokensFarmadon.formas.mediana,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp).padding(bottom = s.gapMedium)
                    ) {
                        Row(
                            modifier = Modifier.padding(s.gapMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TokensFarmadon.colores.estadoAlerta,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        esAvisoPlan -> "Plan no disponible"
                                        esProblemaConexion -> "Problema de conexión"
                                        else -> "Atención con tus datos"
                                    },
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold),
                                    color = TokensFarmadon.colores.estadoAlerta
                                )
                                Text(
                                    text = checkNotNull(avisoTexto),
                                    style = TokensFarmadon.tipografia.cuerpoPequeno,
                                    color = FDColors.TextSecondary
                                )
                            }
                            TextButton(onClick = onBackClick) {
                                Text("CORREGIR", color = TokensFarmadon.colores.estadoAlerta, style = TokensFarmadon.tipografia.etiqueta)
                            }
                        }
                    }
                }

                // El aviso se enciende con la verdad del contrato: si BRIXO pidió
                // corregir el plan (camposACorregir incluye planId), el cliente debe
                // ver POR QUÉ está eligiendo plan. Los campos legacy sugerirCambioPlan/
                // accionSugerida se mantienen por compatibilidad.
                val pideCambioPlan = camposACorregir.any { it == "planId" || it == "plan" }
                if (esCorreccion && (pideCambioPlan || sugerirCambioPlan || accionSugerida == "cambiar_plan")) {
                    Surface(
                        color = TokensFarmadon.colores.alertaSutil,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TokensFarmadon.colores.estadoAlerta),
                        shape = TokensFarmadon.formas.mediana,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(s.gapMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TokensFarmadon.colores.estadoAlerta,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "BRIXO sugiere cambiar de plan",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold),
                                    color = TokensFarmadon.colores.estadoAlerta
                                )
                                Text(
                                    text = "Por favor selecciona un plan diferente al plan que fue observado.",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno,
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(s.gapMedium))
                }

                ResumenPaso1(resumenNombre, resumenRuc, resumenEmail, s)

                Spacer(modifier = Modifier.height(s.gapXXLarge))

                val errorPlanes = state.errorPlanes
                when {
                    // FALLO DE LECTURA: la causa real + camino de recuperación.
                    // Jamás se disfraza de "no hay planes publicados".
                    errorPlanes != null -> {
                        Column(
                            modifier = Modifier.padding(s.gapXXLarge).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = TokensFarmadon.colores.estadoAlerta,
                                modifier = Modifier.size(48.dp).alpha(0.6f)
                            )
                            Text(
                                "No pudimos leer el catálogo de planes",
                                style = TokensFarmadon.tipografia.titulo3,
                                color = FDColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                errorPlanes,
                                style = TokensFarmadon.tipografia.cuerpoPequeno,
                                color = FDColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRetryCargaPlanes,
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText
                                ),
                                shape = TokensFarmadon.formas.mediana
                            ) {
                                Text("REINTENTAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    !state.cargandoPlanes && state.planesDisponibles.isEmpty() -> {
                    Column(
                        modifier = Modifier.padding(s.gapXXLarge).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = TokensFarmadon.colores.estadoAlerta,
                            modifier = Modifier.size(48.dp).alpha(0.6f)
                        )
                        val paisEtiqueta = if (paisNombre.isBlank()) "tu país"
                        else CatalogoPaises.nombreLegible(paisNombre)
                        Text(
                            "Aún no hay planes publicados para $paisEtiqueta.\n" +
                                "La central de BRIXO los habilitará pronto. Vuelve a intentarlo más tarde.",
                            style = TokensFarmadon.tipografia.titulo3,
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    }
                    // LECTURA SANA: skeletons mientras carga, tarjetas cuando llegan.
                    else -> {
                    val planesMostrados = state.planesDisponibles.take(3)
                    val maxCardsWidth = if (planesMostrados.size == 1) 400.dp else 900.dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = maxCardsWidth),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (state.cargandoPlanes) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (compacto) 200.dp else 240.dp)
                                        .padding(horizontal = s.gapSmall)
                                        .background(FDColors.Glass, TokensFarmadon.formas.grande)
                                        .border(1.dp, FDColors.Border, TokensFarmadon.formas.grande),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                                }
                            }
                        } else {
                            planesMostrados.forEachIndexed { index, plan ->
                                val visible = remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(index * 150L)
                                    visible.value = true
                                }

                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = slideInVertically { 20 } + fadeIn(animationSpec = tween(600)),
                                    modifier = if (planesMostrados.size > 1) Modifier.weight(1f).padding(horizontal = s.gapSmall)
                                               else Modifier.widthIn(max = 400.dp)
                                ) {
                                    PlanCard(
                                        plan = plan,
                                        selected = state.planSeleccionado?.id == plan.id,
                                        onClick = {
                                            val planEnCampos = camposACorregir.any { it == "planId" || it == "plan" }
                                            if (!esCorreccion || planEnCampos) onPlanSelected(plan)
                                        },
                                        cargando = cargando,
                                        esCorreccion = esCorreccion,
                                        planSeleccionable = !esCorreccion || camposACorregir.any { it == "planId" || it == "plan" },
                                        s = s,
                                        modifier = Modifier.fillMaxWidth(),
                                        compacto = compacto
                                    )
                                }
                            }
                        }
                    }
                }
                }
                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        AnimatedVisibility(
            visible = !incidenteVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = s.gapLarge)
            ) {
                Button(
                    onClick = { onNextStep() },
                    enabled = state.planSeleccionado != null && !cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnLargeH)
                        .bounceClick(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText,
                        disabledContainerColor = FDColors.Primary.copy(alpha = 0.12f),
                        disabledContentColor = FDColors.TextDisabled
                    ),
                    shape = TokensFarmadon.formas.completa
                ) {
                    if (cargando) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FDColors.PrimaryText, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(s.gapSmall))
                            Text("Enviando solicitud...", style = TokensFarmadon.tipografia.titulo3, color = FDColors.PrimaryText)
                        }
                    } else {
                        // Etiqueta visible en AMBOS estados: era el botón vacío.
                        Text(
                            "ENVIAR SOLICITUD",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.PrimaryText
                        )
                    }
                }

                if (state.planSeleccionado == null) {
                    Text(
                        "Selecciona un plan para continuar",
                        style = TokensFarmadon.tipografia.leyenda,
                        color = FDColors.TextTertiary,
                        modifier = Modifier.padding(top = s.gapSmall)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(s.gapXXLarge))
    }
}

@Composable
fun PlanCard(
    plan: PlanSuscripcion,
    selected: Boolean,
    onClick: () -> Unit,
    cargando: Boolean,
    esCorreccion: Boolean,
    planSeleccionable: Boolean = true,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier,
    compacto: Boolean = false
) {
    val priceFinal = plan.precioMensual
    // La moneda NACE del país del plan (catálogo propio de BRIXO, O(1)).
    val monedaPlan = CatalogoPaises.monedaDe(plan.paisIso)
    val precioTexto = if (plan.esGratuito) "GRATIS" else "${monedaPlan.second} ${"%.2f".format(priceFinal)}"
    val precioUnidad = if (plan.esGratuito)
        "/ ${plan.diasGratis.coerceAtLeast(1)} días"
    else
        "/mes · ${monedaPlan.first}"
    val notaTrial = if (!plan.esGratuito && plan.diasPrueba > 0) "INCLUYE ${plan.diasPrueba} DÍAS DE PRUEBA" else null

    val isDestacado = plan.nombre.contains("Pro", ignoreCase = true) || plan.nombre.contains("Premium", ignoreCase = true)
    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .heightIn(min = if (compacto) 220.dp else 260.dp)
            .bounceClick()
            .clickable { onClick() }
            .then(
                if (selected) Modifier.border(1.5.dp, FDColors.Primary, RoundedCornerShape(20.dp))
                else if (isDestacado) Modifier.border(1.2.dp, FDColors.Success.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                else Modifier.border(1.dp, FDColors.Border, RoundedCornerShape(20.dp))
            ),
        color = when {
            selected -> FDColors.Primary
            isDestacado -> FDColors.SuccessSubtle
            else -> FDColors.SurfaceElevated
        },
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (selected || isDestacado) 8.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(s.gapXXLarge),
            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.nombre.uppercase(),
                    style = TokensFarmadon.tipografia.etiqueta,
                    color = if (selected) FDColors.PrimaryText else TokensFarmadon.colores.textoSecundario
                )
                if (selected) {
                    Icon(Icons.Default.CheckCircle, null, tint = FDColors.PrimaryText, modifier = Modifier.size(16.dp))
                }
            }

            if (plan.descripcion.isNotEmpty()) {
                Text(
                    text = plan.descripcion,
                    style = TokensFarmadon.tipografia.cuerpoPequeno,
                    color = if (selected) FDColors.PrimaryText.copy(alpha = 0.7f) else TokensFarmadon.colores.textoTerciario
                )
            }

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = precioTexto,
                        style = TokensFarmadon.tipografia.visual.copy(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light),
                        color = if (selected) FDColors.PrimaryText else FDColors.TextPrimary
                    )
                    Text(
                        text = precioUnidad,
                        style = TokensFarmadon.tipografia.leyenda.copy(fontSize = 11.sp),
                        color = if (selected) FDColors.PrimaryText.copy(alpha = 0.6f) else FDColors.TextTertiary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }
                if (notaTrial != null) {
                    Text(
                        text = notaTrial,
                        style = TokensFarmadon.tipografia.leyenda,
                        color = if (selected) FDColors.PrimaryText.copy(alpha = 0.6f) else TokensFarmadon.colores.textoTerciario,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = (if (selected) FDColors.PrimaryText else TokensFarmadon.colores.bordeSutil).copy(alpha = 0.12f))

            Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                // TODAS las herramientas del plan (jamás un recorte silencioso).
                // Se descartan vacías o repetidas idénticas a la descripción.
                val herramientas = plan.features
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it != plan.descripcion.trim() }
                herramientas.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = if (selected) FDColors.PrimaryText else FDColors.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = feature,
                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                            color = if (selected) FDColors.PrimaryText else TokensFarmadon.colores.textoSecundario
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(s.gapMedium))

            Button(
                onClick = onClick,
                enabled = !cargando && planSeleccionable,
                modifier = Modifier.fillMaxWidth().height(s.btnMediumH),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) FDColors.PrimaryText else FDColors.Primary.copy(alpha = 0.1f),
                    contentColor = if (selected) FDColors.Primary else FDColors.Primary
                ),
                shape = TokensFarmadon.formas.mediana
            ) {
                if (selected && cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FDColors.Primary, strokeWidth = 2.dp)
                } else {
                    Text(if (selected) "PLAN SELECCIONADO" else "ELEGIR PLAN", style = TokensFarmadon.tipografia.etiqueta, color = if (selected) FDColors.Primary else FDColors.Primary)
                }
            }
        }
    }
}

@Composable
fun ResumenPaso1(nombre: String, ruc: String, email: String, s: MedidaAdaptativa) {
    Surface(
        color = FDColors.Glass,
        shape = TokensFarmadon.formas.completa,
        border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.padding(bottom = s.gapLarge)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = s.gapXXLarge, vertical = s.gapSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
        ) {
            Icon(Icons.Default.Info, null, tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp))
            Text(
                text = "$nombre • $ruc • $email",
                style = TokensFarmadon.tipografia.leyenda,
                color = FDColors.TextSecondary
            )
        }
    }
}

@Composable
fun ExitoRegistroScreen(
    onNavigateToLogin: (String) -> Unit,
    email: String,
    s: MedidaAdaptativa,
    esCorreccion: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Fondo Adaptativo — tokens (sin Color fijo)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = if (FDColors.isDark) {
                            listOf(FDColors.Surface, FDColors.Background)
                        } else {
                            listOf(FDColors.SurfaceElevated, FDColors.Background)
                        },
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.maxDimension * 0.8f
                    )
                )

                // 2. Foco de Luz Atmosférico — adaptativo
                drawRect(
                    brush = Brush.linearGradient(
                        colors = if (FDColors.isDark) {
                            listOf(FDColors.TextPrimary.copy(alpha = 0.08f), FDColors.Primary.copy(alpha = 0.03f), Color.Transparent)
                        } else {
                            listOf(FDColors.TextPrimary.copy(alpha = 0.03f), FDColors.Primary.copy(alpha = 0.01f), Color.Transparent)
                        },
                        start = Offset(size.width, size.height * 0.5f),
                        end = Offset(size.width * 0.3f, size.height * 0.5f)
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .padding(s.padScreenH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.gapXXLarge)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FDColors.Success,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(s.gapLarge))
                Text(
                    if (esCorreccion) "¡CORRECCIÓN ENVIADA!" else "¡SOLICITUD RECIBIDA!",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        color = FDColors.Success,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    if (esCorreccion) "EXPEDIENTE ACTUALIZADO" else "BIENVENIDO A VIORA",
                    style = TokensFarmadon.tipografia.visual.copy(fontSize = 36.sp),
                    color = FDColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = if (esCorreccion)
                    "Tus correcciones fueron enviadas con éxito a Brixo. La central revisará los cambios y te notificaremos cuando tu cuenta esté aprobada."
                else
                    "Tu solicitud fue enviada correctamente. BRIXO la revisará y te avisaremos cuando esté aprobada.",
                style = TokensFarmadon.tipografia.cuerpo,
                color = FDColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { onNavigateToLogin(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(s.inputMinH),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FDColors.Primary,
                    contentColor = FDColors.PrimaryText
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "ENTENDIDO, VOLVER AL LOGIN", 
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun MinimalProgressLine(pasoActual: Int, s: MedidaAdaptativa) {
    Row(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .padding(vertical = s.gapLarge),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(2) { index ->
            val step = index + 1
            val isActive = step <= pasoActual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        color = if (isActive) FDColors.Primary else FDColors.Border,
                        shape = CircleShape
                    )
            )
        }
    }
}
