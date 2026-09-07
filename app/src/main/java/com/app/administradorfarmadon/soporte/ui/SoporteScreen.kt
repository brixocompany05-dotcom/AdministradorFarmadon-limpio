package com.app.administradorfarmadon.soporte.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.soporte.logica.SoporteViewModel
import com.app.administradorfarmadon.soporte.modelo.MensajeSoporte
import com.app.administradorfarmadon.soporte.modelo.SoporteTicket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Soporte al Cliente — CHAT REAL 100% PRODUCCIÓN (Estilo WhatsApp 2026)
 * - Mismo chat thread para mensajes continuos de la misma conversación.
 * - ID único por mensaje, ordenamiento cronológico estricto sin brincos ni incoherencias.
 * - Estados de lectura honestos en palabras (Enviando… / Enviado / Leído).
 * - Identificación real de Sede y Usuario sin mentiras ni decoraciones falsas.
 * - Bloqueo de escritura en consultas resueltas (Modo Historial inmutable).
 * - Reintento de mensaje en caso de falla de red (Cero pérdida de trabajo).
 */
@Composable
fun SoporteScreen(
    onVolver: () -> Unit = {},
    viewModel: SoporteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var textoMensajeInput by remember { mutableStateOf("") }
    var mostrarDialogoReporte by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.mensajeExito, uiState.error) {
        if (uiState.mensajeExito != null || uiState.error != null) {
            delay(4500)
            viewModel.limpiarMensajes()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val isWide = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FDSpacing.xl, vertical = FDSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            
            // ── 1. CABECERA SUPERIOR REAL Y DIRECTA ──
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Large,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                    ) {
                        // Avatar sobrio con inicial (sin presencia inventada).
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(FDColors.TextPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                style = FDType.Heading2.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.Background
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Soporte BRIXO Central",
                                style = FDType.Heading2.copy(
                                    fontFamily = InterPremium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            val sedeInfo = SessionManager.sucursalNombre.ifBlank { SessionManager.sucursalIdEfectiva.ifBlank { "Sin sede" } }
                            val userInfo = SessionManager.nombreUsuario.ifBlank { "Usuario" }
                            val abiertos = uiState.tickets.count { it.puedeEscribir }
                            Text(
                                text = if (uiState.cargando) {
                                    "Sede: $sedeInfo · Usuario: $userInfo"
                                } else if (abiertos > 0) {
                                    "Sede: $sedeInfo · $abiertos caso(s) abierto(s)"
                                } else {
                                    "Sede: $sedeInfo · Sin casos abiertos"
                                },
                                style = FDType.BodySmall.copy(color = FDColors.TextSecondary, fontFamily = InterPremium)
                            )
                        }
                    }

                    // Canales reales publicados por la central (si no hay, no se muestran).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        if (uiState.canalesOficiales.tieneWhatsapp) {
                            Surface(
                                onClick = { viewModel.contactarWhatsApp(context) },
                                color = FDColors.SurfaceHover,
                                shape = FDShapes.Full,
                                border = BorderStroke(1.dp, FDColors.Border)
                            ) {
                                Text(
                                    text = "WhatsApp",
                                    style = FDType.Label.copy(color = FDColors.TextSecondary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.sm)
                                )
                            }
                        }
                        if (uiState.canalesOficiales.tieneEmail) {
                            Surface(
                                onClick = { viewModel.contactarEmail(context) },
                                color = FDColors.SurfaceHover,
                                shape = FDShapes.Full,
                                border = BorderStroke(1.dp, FDColors.Border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = FDColors.TextSecondary, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "Correo",
                                        style = FDType.Label.copy(color = FDColors.TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── BANNERS DE FEEDBACK ──
            AnimatedVisibility(
                visible = uiState.mensajeExito != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.mensajeExito?.let { msg ->
                    Surface(
                        color = FDColors.Success.copy(alpha = 0.10f),
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FDSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                        ) {
                            Text(msg, style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.Success))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.error?.let { err ->
                    Surface(
                        color = FDColors.Error.copy(alpha = 0.10f),
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FDSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                        ) {
                            Text(err, style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.Error))
                        }
                    }
                }
            }

            // ── 2. PANELS DE CHAT: CONVERSACIONES (IZQUIERDA) + VISTA DE CHAT (DERECHA) ──
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                ) {
                    // Columna 1: Mis solicitudes
                    Box(modifier = Modifier.weight(0.38f).fillMaxHeight()) {
                        PanelConversaciones(
                            tickets = uiState.tickets,
                            seleccionado = uiState.ticketSeleccionado,
                            cargando = uiState.cargando,
                            onSeleccionar = { viewModel.seleccionarTicket(it) },
                            onNuevaConsulta = { viewModel.abrirModalNuevoTicket() },
                            errorCarga = if (uiState.tickets.isEmpty()) uiState.error else null,
                            onReintentarCarga = { viewModel.reintentarCarga() }
                        )
                    }

                    // Columna 2: Chat Activo estilo WhatsApp
                    Box(modifier = Modifier.weight(0.62f).fillMaxHeight()) {
                        PanelVistaChat(
                            ticket = uiState.ticketSeleccionado,
                            mensajes = uiState.mensajesCombinados,
                            textoInput = textoMensajeInput,
                            cargandoEnvio = uiState.creandoTicket,
                            onTextoInputChange = { textoMensajeInput = it },
                            onNuevaConsulta = { viewModel.abrirModalNuevoTicket() },
                            onNuevaRelacionada = { viewModel.prepararNuevaConsultaDesde(it) },
                            onAbrirReporte = { mostrarDialogoReporte = true },
                            onEnviarMensajeDirecto = {
                                if (textoMensajeInput.trim().isNotBlank()) {
                                    val msg = textoMensajeInput.trim()
                                    textoMensajeInput = ""
                                    viewModel.enviarMensajeEnChat(msg)
                                }
                            },
                            onReintentarMensaje = { ticketId, clientMessageId ->
                                viewModel.reintentarEnvioMensaje(ticketId, clientMessageId)
                            },
                            onRecargarChat = { viewModel.recargarChatAbierto() },
                            casoNoDisponible = uiState.casoNoDisponible,
                            onVolverSolicitudes = { viewModel.seleccionarTicket(null) },
                            cargandoLista = uiState.cargando,
                            onMarcarLeido = { viewModel.marcarLeidoAhora() },
                            mostrarVolver = !isWide,
                            onVolver = { viewModel.seleccionarTicket(null) },
                            mensajesListos = uiState.mensajesListos,
                            onContactarWhatsApp = { viewModel.contactarWhatsApp(context, it) }
                        )
                    }
                }
            } else {
                // Vista Móvil / Angosta
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                ) {
                    if (uiState.ticketSeleccionado != null || uiState.casoNoDisponible) {
                        PanelVistaChat(
                            ticket = uiState.ticketSeleccionado,
                            mensajes = uiState.mensajesCombinados,
                            textoInput = textoMensajeInput,
                            cargandoEnvio = uiState.creandoTicket,
                            onTextoInputChange = { textoMensajeInput = it },
                            onNuevaConsulta = { viewModel.abrirModalNuevoTicket() },
                            onNuevaRelacionada = { viewModel.prepararNuevaConsultaDesde(it) },
                            onAbrirReporte = { mostrarDialogoReporte = true },
                            onEnviarMensajeDirecto = {
                                if (textoMensajeInput.trim().isNotBlank()) {
                                    val msg = textoMensajeInput.trim()
                                    textoMensajeInput = ""
                                    viewModel.enviarMensajeEnChat(msg)
                                }
                            },
                            onReintentarMensaje = { ticketId, clientMessageId ->
                                viewModel.reintentarEnvioMensaje(ticketId, clientMessageId)
                            },
                            onRecargarChat = { viewModel.recargarChatAbierto() },
                            casoNoDisponible = uiState.casoNoDisponible,
                            onVolverSolicitudes = { viewModel.seleccionarTicket(null) },
                            cargandoLista = uiState.cargando,
                            onMarcarLeido = { viewModel.marcarLeidoAhora() },
                            mostrarVolver = !isWide,
                            onVolver = { viewModel.seleccionarTicket(null) },
                            mensajesListos = uiState.mensajesListos,
                            onContactarWhatsApp = { viewModel.contactarWhatsApp(context, it) }
                        )
                    } else {
                        PanelConversaciones(
                            tickets = uiState.tickets,
                            seleccionado = uiState.ticketSeleccionado,
                            cargando = uiState.cargando,
                            onSeleccionar = { viewModel.seleccionarTicket(it) },
                            onNuevaConsulta = { viewModel.abrirModalNuevoTicket() },
                            errorCarga = if (uiState.tickets.isEmpty()) uiState.error else null,
                            onReintentarCarga = { viewModel.reintentarCarga() }
                        )
                    }
                }
            }
        }

        // Nueva solicitud con cero fricción.
        if (uiState.mostrarModalNuevoTicket) {
            DialogoNuevaConsultaChat(
                cargando = uiState.creandoTicket,
                onDismiss = { viewModel.cerrarModalNuevoTicket() },
                onConfirmar = { cat, desc ->
                    viewModel.crearSolicitudSimple(cat, desc)
                }
            )
        }



        // Reportar un error real con lenguaje simple (§8).
        // El diálogo solo se cierra cuando el reporte sí llegó (si falla,
        // el texto sigue ahí para reintentar).
        LaunchedEffect(uiState.ultimoReporteOkMs) {
            if (uiState.ultimoReporteOkMs > 0 && mostrarDialogoReporte) {
                mostrarDialogoReporte = false
            }
        }
        if (mostrarDialogoReporte) {
            DialogoReportarProblema(
                cargando = uiState.creandoTicket,
                onDismiss = { mostrarDialogoReporte = false },
                onEnviar = { tipo, desc ->
                    viewModel.reportarErrorConContexto(
                        tipoReporte = tipo,
                        descripcion = desc,
                        pantalla = uiState.ticketSeleccionado?.asunto ?: "Soporte"
                    )
                }
            )
        }
    }
}

// ───────────────────────────── PANEL IZQUIERDO: CONVERSACIONES ─────────────────────────────

@Composable
private fun PanelConversaciones(
    tickets: List<SoporteTicket>,
    seleccionado: SoporteTicket?,
    cargando: Boolean,
    onSeleccionar: (SoporteTicket) -> Unit,
    onNuevaConsulta: () -> Unit,
    errorCarga: String? = null,
    onReintentarCarga: () -> Unit = {}
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FDSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            // Título + botón Nueva solicitud
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis solicitudes",
                        style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                    )
                    Text(
                        text = if (tickets.isEmpty()) "Sin solicitudes" else "${tickets.size} solicitudes",
                        style = FDType.Caption,
                        color = FDColors.TextSecondary
                    )
                }

                Surface(
                    onClick = onNuevaConsulta,
                    color = FDColors.Primary,
                    shape = FDShapes.Full
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "+ Nueva solicitud",
                            style = FDType.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = FDColors.PrimaryText,
                                fontFamily = InterPremium
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border)

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                }
            } else if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(FDSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        if (errorCarga != null) {
                            Text(
                                text = "No pudimos cargar tus solicitudes",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                            Text(
                                text = "Revisa tu conexión. Nada se perdió.",
                                style = FDType.Caption.copy(color = FDColors.TextSecondary)
                            )
                            Spacer(Modifier.height(FDSpacing.xs))
                            Surface(
                                onClick = onReintentarCarga,
                                color = FDColors.Primary,
                                shape = FDShapes.Full
                            ) {
                                Text(
                                    text = "↻ Reintentar",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText),
                                    modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.sm)
                                )
                            }
                        } else {
                            Text(
                                text = "No tienes solicitudes",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                            Text(
                                text = "Toca 'Nueva solicitud' arriba para contarle tu problema al equipo de soporte.",
                                style = FDType.Caption.copy(color = FDColors.TextSecondary),
                                modifier = Modifier.padding(horizontal = FDSpacing.md)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tickets, key = { it.id }) { ticket ->
                        val esSeleccionado = ticket.id == seleccionado?.id
                        ItemConversacionChat(
                            ticket = ticket,
                            isSelected = esSeleccionado,
                            onClick = { onSeleccionar(ticket) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemConversacionChat(
    ticket: SoporteTicket,
    isSelected: Boolean,
    onClick: () -> Unit,
    esAbierto: Boolean = isSelected
) {
    val fondoColor = if (isSelected) FDColors.PrimarySubtle else FDColors.SurfaceHover.copy(alpha = 0.4f)
    val bordeColor = if (isSelected) FDColors.Primary.copy(alpha = 0.5f) else FDColors.Border.copy(alpha = 0.4f)

    // Inicial sobria del asunto (sin iconos decorativos).
    val inicialCat = ticket.asunto.trim().take(1).uppercase().ifBlank { "S" }

    val timestampReferencia = maxOf(ticket.actualizadoMs, ticket.fechaMs)
    // Fecha humana: hoy con hora, ayer, o día/mes. Nada técnico.
    val fechaHumana = remember(timestampReferencia) {
        if (timestampReferencia <= 0) ""
        else {
            val cal = java.util.Calendar.getInstance()
            val hoy = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val anioHoy = cal.get(java.util.Calendar.YEAR)
            cal.timeInMillis = timestampReferencia
            val dia = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val anio = cal.get(java.util.Calendar.YEAR)
            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampReferencia))
            when {
                anio == anioHoy && dia == hoy -> "Hoy $hora"
                anio == anioHoy && dia == hoy - 1 -> "Ayer $hora"
                else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestampReferencia))
            }
        }
    }

    val ultimoMensajeTexto = remember(ticket) {
        // Lo último real: el resumen del servidor, si no el historial local.
        ticket.ultimoMensaje.ifBlank {
            val msgs = ticket.obtenerTodosLosMensajes()
            msgs.lastOrNull()?.texto ?: ticket.descripcion
        }.take(90)
    }

    Surface(
        onClick = onClick,
        color = fondoColor,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, bordeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            // Inicial sobria
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) FDColors.TextPrimary else FDColors.SurfaceHover),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inicialCat,
                    style = FDType.Body.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) FDColors.Background else FDColors.TextSecondary
                    )
                )
            }

            // Asunto y Vista previa
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticket.asunto,
                        style = FDType.Heading3.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            fontFamily = InterPremium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Chat abierto = lo estás leyendo: no se marca nada nuevo.
                        if (ticket.mensajesSinLeerFarmacia > 0 && !esAbierto) {
                            Surface(
                                color = FDColors.Primary,
                                shape = FDShapes.Full
                            ) {
                                Text(
                                    text = if (ticket.mensajesSinLeerFarmacia > 9) "9+ nuevos" else "${ticket.mensajesSinLeerFarmacia} nuevos",
                                    style = FDType.Caption.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FDColors.PrimaryText
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = fechaHumana,
                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                        )
                    }
                }

                Text(
                    text = ultimoMensajeTexto.ifBlank { ticket.descripcion.take(90) }.ifBlank { "(sin mensajes)" },
                    style = FDType.BodySmall.copy(
                        fontSize = 11.5.sp,
                        color = FDColors.TextSecondary,
                        fontFamily = InterPremium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Estado + quién atiende (jamás nombra un agente que aún no la tomó).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeEstadoChat(ticket.estado)
                    Text(
                        text = ticket.numeroTicket,
                        style = FDType.Caption.copy(fontSize = 9.5.sp, color = FDColors.TextTertiary)
                    )
                    Text("•", style = FDType.Caption.copy(fontSize = 9.5.sp, color = FDColors.TextTertiary))
                    Text(
                        text = ticket.textoAtencion,
                        style = FDType.Caption.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = FDColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ───────────────────────────── PANEL DERECHO: VISTA CHAT ESTILO WHATSAPP ─────────────────────────────

@Composable
private fun PanelVistaChat(
    ticket: SoporteTicket?,
    mensajes: List<MensajeSoporte> = ticket?.obtenerTodosLosMensajes().orEmpty(),
    textoInput: String,
    cargandoEnvio: Boolean,
    onTextoInputChange: (String) -> Unit,
    onNuevaConsulta: () -> Unit,
    onNuevaRelacionada: (String) -> Unit = { _ -> onNuevaConsulta() },
    onAbrirReporte: () -> Unit = {},
    onEnviarMensajeDirecto: () -> Unit,
    onReintentarMensaje: (ticketId: String, clientMessageId: String) -> Unit,
    onRecargarChat: () -> Unit = {},
    casoNoDisponible: Boolean = false,
    onVolverSolicitudes: () -> Unit = onNuevaConsulta,
    cargandoLista: Boolean = false,
    onMarcarLeido: () -> Unit = {},
    mostrarVolver: Boolean = false,
    onVolver: () -> Unit = {},
    tieneWhatsapp: Boolean = false,
    mensajesListos: Boolean = true,
    onContactarWhatsApp: (String) -> Unit
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxSize()
    ) {
        if (ticket != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. ENCABEZADO CHAT ACTIVO
                Surface(
                    color = FDColors.SurfaceHover.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        if (mostrarVolver) {
                            IconButton(onClick = onVolver) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Volver a mis solicitudes",
                                    tint = FDColors.TextSecondary
                                )
                            }
                        }
                        // Un solo avatar: inicial del caso.
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FDColors.TextPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ticket.asunto.trim().take(1).uppercase().ifBlank { "S" },
                                style = FDType.Body.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.Background
                                )
                            )
                        }

                        // Título + id. Nada más: limpio y legible.
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = ticket.asunto.ifBlank { "Solicitud" },
                                style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ticket.numeroTicket,
                                style = FDType.Caption.copy(color = FDColors.TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Selector de acciones: reportar y WhatsApp viven aquí.
                        var menuAbierto by remember(ticket.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuAbierto = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Acciones de la solicitud",
                                    tint = FDColors.TextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = menuAbierto,
                                onDismissRequest = { menuAbierto = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reportar problema") },
                                    onClick = { menuAbierto = false; onAbrirReporte() }
                                )
                                if (tieneWhatsapp) {
                                    DropdownMenuItem(
                                        text = { Text("WhatsApp") },
                                        onClick = {
                                            menuAbierto = false
                                            onContactarWhatsApp("Consulta sobre ${ticket.numeroTicket}: ${ticket.asunto}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Una línea honesta solo si aún espera agente.
                    if (!ticket.tieneAgenteAsignado && !ticket.estaResuelto) {
                        Text(
                            text = "En cola · te avisaremos aquí cuando un agente la tome.",
                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary),
                            modifier = Modifier.padding(
                                start = FDSpacing.lg, end = FDSpacing.lg, bottom = FDSpacing.sm
                            )
                        )
                    }
                    }
                }

                // 2. CHAT estilo WhatsApp: si lees arriba y llega algo nuevo,
                // NO te jalonea; aparece el pill flotante y se borra SOLO al
                // llegar al final. Lo que tú envías siempre baja al instante.
                val listState = rememberLazyListState()
                val todosLosMensajes = remember(mensajes) { mensajes }
                var nuevosSinVer by remember(ticket.id) { mutableStateOf(0) }
                val alcanceChat = rememberCoroutineScope()

                LaunchedEffect(todosLosMensajes.size) {
                    if (todosLosMensajes.isEmpty()) return@LaunchedEffect
                    val ultimo = todosLosMensajes.last()
                    if (ultimo.esDeFarmacia) {
                        // Mi mensaje: bajo a verlo y limpio el pill.
                        nuevosSinVer = 0
                        listState.animateScrollToItem(todosLosMensajes.size - 1)
                        return@LaunchedEffect
                    }
                    val info = listState.layoutInfo
                    val ultimoVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    val alFinal = ultimoVisible == -1 || ultimoVisible >= todosLosMensajes.size - 2
                    if (alFinal) {
                        nuevosSinVer = 0
                        listState.animateScrollToItem(todosLosMensajes.size - 1)
                    } else {
                        val vistos = ultimoVisible + 1
                        nuevosSinVer = (todosLosMensajes.size - vistos).coerceAtLeast(1)
                    }
                }

                // Al llegar al final por tu cuenta, el aviso desaparece solo y
                // recién ahí se marca leído: abierto pero arriba = no leído.
                // (Se lee layoutInfo, que sí es estado observable.)
                val alFinalLista by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val total = info.totalItemsCount
                        if (total == 0) true
                        else {
                            val ultimoVisible =
                                info.visibleItemsInfo.lastOrNull()?.index ?: -1
                            ultimoVisible >= total - 1
                        }
                    }
                }
                LaunchedEffect(alFinalLista) {
                    if (alFinalLista && nuevosSinVer != 0) nuevosSinVer = 0
                }
                val noLeidosServidor = ticket.mensajesSinLeerFarmacia
                LaunchedEffect(alFinalLista, noLeidosServidor) {
                    if (alFinalLista && noLeidosServidor > 0) onMarcarLeido()
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Tres verdades: cargando con salida, vacío real con guía,
                    // o el historial completo. Hasta la primera foto del
                    // servidor no se declara nada (ni vacío ni parcial).
                    val contenidoEsperado =
                        ticket.descripcion.isNotBlank() || ticket.ultimoMensaje.isNotBlank()
                    if (!mensajesListos || (todosLosMensajes.isEmpty() && contenidoEsperado)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(FDSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                            Spacer(Modifier.height(FDSpacing.sm))
                            Text(
                                text = "Cargando conversación…",
                                style = FDType.Body.copy(color = FDColors.TextSecondary)
                            )
                            Spacer(Modifier.height(FDSpacing.xs))
                            Text(
                                text = "Si tarda, tu conexión puede estar lenta. Nada se perdió.",
                                style = FDType.Caption.copy(color = FDColors.TextTertiary)
                            )
                            Spacer(Modifier.height(FDSpacing.sm))
                            Surface(
                                onClick = onRecargarChat,
                                color = FDColors.SurfaceHover,
                                shape = FDShapes.Full,
                                border = BorderStroke(1.dp, FDColors.Border)
                            ) {
                                Text(
                                    text = "Reintentar",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextSecondary),
                                    modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.sm)
                                )
                            }
                        }
                    } else if (todosLosMensajes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(FDSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Aún no hay mensajes",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                            Spacer(Modifier.height(FDSpacing.xs))
                            Text(
                                text = "Escribe el primero abajo y te responderemos aquí.",
                                style = FDType.Caption.copy(color = FDColors.TextSecondary)
                            )
                        }
                    } else {
                    val diaFmt = remember(ticket.id) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FDColors.Background.copy(alpha = 0.4f))
                            .padding(horizontal = FDSpacing.xl, vertical = FDSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        itemsIndexed(
                            todosLosMensajes,
                            key = { _, it -> it.clientMessageId.ifBlank { it.id } },
                            contentType = { _, it -> if (it.esSistema) "sys" else if (it.esDeFarmacia) "yo" else "soporte" }
                        ) { idx, msg ->
                        val horaStr = remember(msg.fechaMs) {
                            if (msg.fechaMs > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.fechaMs)) else ""
                        }
                        val dia = if (msg.fechaMs > 0) diaFmt.format(Date(msg.fechaMs)) else ""
                        val diaAnt = todosLosMensajes.getOrNull(idx - 1)?.let { a ->
                            if (a.fechaMs > 0) diaFmt.format(Date(a.fechaMs)) else ""
                        } ?: ""
                        if (dia.isNotBlank() && dia != diaAnt) {
                            Text(
                                text = dia.uppercase(),
                                style = FDType.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.TextTertiary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }

                        if (msg.esSistema) {
                            BurbujaEventoSistema(texto = msg.texto, fechaStr = horaStr)
                        } else if (msg.esDeFarmacia) {
                            BurbujaMensajeFarmacia(
                                mensaje = if (msg.tipo == "ERROR_REPORT" && msg.errorCodigo.isNotBlank()) {
                                    "Código ${msg.errorCodigo}\n${msg.texto}"
                                } else if ((msg.tipo == "FILE" || msg.tipo == "IMAGE") && msg.archivoNombre.isNotBlank()) {
                                    "Archivo: ${msg.archivoNombre}\n${msg.texto}"
                                } else msg.texto,
                                fechaStr = horaStr,
                                estadoLectura = msg.estadoLectura,
                                tieneError = msg.tieneError,
                                onReintentar = { onReintentarMensaje(ticket.id, msg.clientMessageId.ifBlank { msg.id }) }
                            )
                        } else {
                            BurbujaRespuestaBrixo(
                                respondidoPor = "${msg.autorNombre.ifBlank { "Soporte Brixo" }} · Soporte Brixo",
                                respuesta = if ((msg.tipo == "FILE" || msg.tipo == "IMAGE") && msg.archivoNombre.isNotBlank()) {
                                    "Archivo: ${msg.archivoNombre}\n${msg.texto}"
                                } else msg.texto,
                                fechaStr = horaStr
                            )
                        }
                        }
                    }
                    // Flecha WhatsApp: solo arriba. Con contador si hay nuevos.
                    // Al llegar abajo desaparece sola (la maneja alFinalLista).
                    if (!alFinalLista) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = FDSpacing.lg, bottom = FDSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = {
                                    nuevosSinVer = 0
                                    alcanceChat.launch {
                                        if (todosLosMensajes.isNotEmpty()) {
                                            listState.animateScrollToItem(todosLosMensajes.size - 1)
                                        }
                                    }
                                },
                                color = FDColors.TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Bajar a los mensajes nuevos",
                                        tint = FDColors.Background,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (nuevosSinVer > 0) {
                                Surface(
                                    color = FDColors.Background,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, FDColors.TextPrimary),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = if (nuevosSinVer > 99) "99+" else "$nuevosSinVer",
                                        style = FDType.Caption.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FDColors.TextPrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // 3. BARRA INFERIOR DE ENTRADA DE TEXTO O BANNER DE HISTORIAL SI ESTÁ RESUELTO/CERRADO
                if (ticket.estaResuelto) {
                    val tituloCierre = if (ticket.estaCerradoDefinitivo) {
                        "Solicitud cerrada"
                    } else {
                        "Solicitud resuelta (historial)"
                    }
                    val tituloColor = FDColors.TextPrimary
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FDSpacing.lg, vertical = FDSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = tituloCierre,
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = tituloColor)
                                )
                                Text(
                                    text = if (ticket.estaCerradoDefinitivo) {
                                        "Esta conversación ya no admite nuevos mensajes. Si tienes un problema nuevo, puedes crear una nueva solicitud."
                                    } else {
                                        "Esta conversación quedó guardada como historial. Si tienes un problema nuevo, crea una nueva solicitud."
                                    },
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                                )
                            }
                            Surface(
                                onClick = { onNuevaRelacionada(ticket.id) },
                                color = FDColors.Primary,
                                shape = FDShapes.Full
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+ Nueva solicitud",
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // El siguiente paso siempre visible: si te necesitan, te lo dice aquí.
                    if (ticket.estadoNormalizado == "WAITING_CUSTOMER") {
                        Text(
                            text = "Necesitamos una respuesta tuya para continuar. Cuando respondas, el agente seguirá atendiéndote.",
                            style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FDColors.SurfaceHover)
                                .padding(horizontal = FDSpacing.lg, vertical = FDSpacing.sm)
                        )
                    }
                    BarraChatInput(
                        textoInput = textoInput,
                        cargandoEnvio = cargandoEnvio,
                        placeholderText = "Escribe tu respuesta o mensaje...",
                        onTextoInputChange = onTextoInputChange,
                        onEnviar = onEnviarMensajeDirecto
                    )
                }
            }
        } else if (casoNoDisponible) {
            // El caso abierto ya no existe: se dice con salida, no se esconde.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FDSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Esta solicitud ya no está disponible",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                )
                Spacer(Modifier.height(FDSpacing.xs))
                Text(
                    text = "Pudo ser eliminada o movida. Tus demás solicitudes están intactas.",
                    style = FDType.Body.copy(color = FDColors.TextSecondary)
                )
                Spacer(Modifier.height(FDSpacing.lg))
                Surface(
                    onClick = onVolverSolicitudes,
                    color = FDColors.TextPrimary,
                    shape = FDShapes.Full
                ) {
                    Text(
                        text = "Volver a mis solicitudes",
                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.Background),
                        modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md)
                    )
                }
            }
        } else {
            // Sin caso abierto: un solo camino (el diálogo), sin chats paralelos.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FDSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (cargandoLista) {
                    CircularProgressIndicator(color = FDColors.TextPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(FDSpacing.sm))
                    Text(
                        text = "Cargando tus solicitudes…",
                        style = FDType.Body.copy(color = FDColors.TextSecondary)
                    )
                } else {
                    Text(
                        text = "¿En qué podemos ayudarte hoy?",
                        style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                    )
                    Spacer(Modifier.height(FDSpacing.xs))
                    Text(
                        text = "Cuenta tu problema y un agente te atenderá aquí mismo.",
                        style = FDType.Body.copy(color = FDColors.TextSecondary)
                    )
                    Spacer(Modifier.height(FDSpacing.lg))
                    Surface(
                        onClick = onNuevaConsulta,
                        color = FDColors.TextPrimary,
                        shape = FDShapes.Full
                    ) {
                        Text(
                            text = "+ Nueva solicitud",
                            style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.Background),
                            modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraChatInput(
    textoInput: String,
    cargandoEnvio: Boolean,
    placeholderText: String,
    onTextoInputChange: (String) -> Unit,
    onEnviar: () -> Unit
) {
    Surface(
        color = FDColors.Surface,
        border = BorderStroke(1.dp, FDColors.Border),
        // El teclado jamás tapa el campo donde se escribe.
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FDSpacing.lg, vertical = FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            OutlinedTextField(
                value = textoInput,
                onValueChange = onTextoInputChange,
                placeholder = {
                    Text(
                        placeholderText,
                        style = FDType.Body.copy(color = FDColors.TextTertiary, fontSize = 13.sp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onEnviar() }),
                shape = FDShapes.Full,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FDColors.Primary,
                    unfocusedBorderColor = FDColors.Border,
                    focusedContainerColor = FDColors.InputBackground,
                    unfocusedContainerColor = FDColors.InputBackground
                ),
                modifier = Modifier.weight(1f)
            )

            Surface(
                onClick = { if (!cargandoEnvio) onEnviar() },
                // Sólido serio a juego con tus burbujas. Sin progreso trabado en el chat.
                color = if (textoInput.trim().isNotBlank() && !cargandoEnvio) FDColors.TextPrimary else FDColors.Border,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (cargandoEnvio) {
                        CircularProgressIndicator(
                            color = FDColors.Background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (textoInput.trim().isNotBlank()) FDColors.Background else FDColors.TextDisabled,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── BURBUJAS DE CHAT CON CHECKS DE LECTURA REALES Y REINTENTO ─────────────────────────────

@Composable
private fun EstadoEnvioTexto(estadoLectura: String, claro: Boolean) {
    // Palabras honestas, sin colores semáforo: qué pasó con mi mensaje.
    val texto = when (estadoLectura.uppercase()) {
        "ENVIANDO" -> "Enviando…"
        "LEIDO" -> "Leído"
        "ERROR" -> null
        else -> "Enviado"
    } ?: return
    Text(
        text = texto,
        fontSize = 10.sp,
        color = if (claro) FDColors.Background.copy(alpha = 0.75f) else FDColors.TextTertiary
    )
}

@Composable
private fun BurbujaMensajeFarmacia(
    mensaje: String,
    fechaStr: String,
    estadoLectura: String = "LEIDO",
    tieneError: Boolean = false,
    onReintentar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            // Sólido serio: burbuja oscura en claro, clara en oscuro. Sin verde/azul.
            color = FDColors.TextPrimary,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "TÚ",
                    style = FDType.Caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.Background.copy(alpha = 0.7f),
                        fontFamily = InterPremium
                    )
                )
                Text(
                    text = mensaje,
                    style = FDType.Body.copy(
                        fontSize = 13.5.sp,
                        color = FDColors.Background,
                        fontFamily = InterPremium
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fechaStr,
                        style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Background.copy(alpha = 0.7f))
                    )
                    Spacer(Modifier.width(6.dp))
                    EstadoEnvioTexto(estadoLectura, claro = true)
                }

                if (tieneError && onReintentar != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No se pudo enviar",
                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Background.copy(alpha = 0.85f))
                        )
                        Text(
                            text = "Reintentar",
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FDColors.Background
                            ),
                            modifier = Modifier
                                .clip(FDShapes.Full)
                                .clickable { onReintentar() }
                                .border(1.dp, FDColors.Background.copy(alpha = 0.6f), FDShapes.Full)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BurbujaRespuestaBrixo(
    respondidoPor: String,
    respuesta: String,
    fechaStr: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = FDColors.SurfaceHover,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = respondidoPor,
                            style = FDType.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = FDColors.TextPrimary,
                                fontFamily = InterPremium
                            )
                        )
                        if (fechaStr.isNotBlank()) {
                            Text(
                                text = fechaStr,
                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                            )
                        }
                    }
                    Text(
                        text = respuesta,
                        style = FDType.Body.copy(
                            fontSize = 13.5.sp,
                            color = FDColors.TextPrimary,
                            fontFamily = InterPremium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BurbujaEventoSistema(texto: String, fechaStr: String) {
    // Evento del sistema: texto centrado sobrio, claramente distinto del chat.
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = texto,
            style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary),
        )
        if (fechaStr.isNotBlank()) {
            Text(
                text = fechaStr,
                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
            )
        }
    }
}

@Composable
private fun DialogoReportarProblema(
    cargando: Boolean,
    onDismiss: () -> Unit,
    onEnviar: (tipo: String, descripcion: String) -> Unit
) {
    var tipo by remember { mutableStateOf("Algo no funciona") }
    var descripcion by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    val opciones = listOf(
        "Algo no funciona",
        "Apareció un error",
        "El sistema está lento",
        "Los datos parecen incorrectos",
        "No puedo realizar una operación"
    )
    Dialog(onDismissRequest = { if (!cargando) onDismiss() }) {
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.XLarge,
            border = BorderStroke(1.dp, FDColors.Border),
            // El teclado jamás tapa los campos: se desplaza y respeta el teclado.
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(FDSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                Text(
                    text = "Reportar problema",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                )
                Text(
                    text = "Cuéntalo en simple. Soporte recibe el contexto técnico automáticamente.",
                    style = FDType.Caption, color = FDColors.TextSecondary
                )
                HorizontalDivider(color = FDColors.Border)
                Text("¿Qué problema encontraste?", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                opciones.forEach { op ->
                    val sel = tipo == op
                    Surface(
                        onClick = { tipo = op },
                        color = if (sel) FDColors.PrimarySubtle else FDColors.InputBackground,
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, if (sel) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(op, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium))
                        }
                    }
                }
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Describe lo ocurrido") },
                    placeholder = { Text("Ej: Al cerrar caja dice que sigue abierta...") },
                    minLines = 3, maxLines = 5,
                    shape = FDShapes.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                errorLocal?.let { Text(it, style = FDType.Caption.copy(color = FDColors.Error)) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !cargando) {
                        Text("Cancelar", style = FDType.Body.copy(color = FDColors.TextSecondary))
                    }
                    Spacer(Modifier.width(FDSpacing.sm))
                    Surface(
                        onClick = {
                            if (descripcion.trim().length < 8) {
                                errorLocal = "Describe lo ocurrido con al menos 8 caracteres."
                            } else {
                                errorLocal = null
                                onEnviar(tipo, descripcion.trim())
                            }
                        },
                        color = FDColors.Primary, shape = FDShapes.Full
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = FDColors.PrimaryText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Enviar reporte", style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeEstadoChat(estado: String) {
    // Lenguaje humano en texto sobrio: sin píldoras de colores ni iconos.
    val label = when (estado.uppercase()) {
        "CLOSED" -> "Cerrada"
        "RESUELTO", "RESOLVED" -> "Resuelta"
        "IN_PROGRESS", "EN_PROCESO" -> "En atención"
        "WAITING_CUSTOMER" -> "Te necesitamos"
        "WAITING_BRIXO" -> "En atención"
        else -> "En cola"
    }
    Text(
        text = label,
        style = FDType.Caption.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = FDColors.TextSecondary
        )
    )
}

// ───────────────────────────── DIÁLOGO NUEVA CONSULTA ESTILO CHAT DELICADO ─────────────────────────────

@Composable
private fun DialogoNuevaConsultaChat(
    cargando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (categoria: String, descripcion: String) -> Unit
) {
    var categoria by remember { mutableStateOf<String?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    // 8 opciones grandes y claras. Nada técnico que rellenar.
    val opciones = listOf(
        "FACTURACION_ELECTRONICA" to "Facturación",
        "CAJA" to "Caja",
        "INVENTARIO" to "Inventario",
        "VENTAS" to "Ventas",
        "USUARIOS" to "Usuarios",
        "CONFIGURACION" to "Configuración",
        "ERROR_TECNICO" to "Reportar un error",
        "OTRO" to "Otro problema"
    )

    Dialog(onDismissRequest = { if (!cargando) onDismiss() }) {
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.XLarge,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(FDSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        Column {
                            Text(
                                text = "¿En qué podemos ayudarte?",
                                style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                            )
                            Text(
                                text = "Elige un tema y cuéntanos. Un agente te atenderá.",
                                style = FDType.Caption,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border)

                // 1. ¿En qué podemos ayudarte? (una sola elección, grande y clara)
                Text("¿En qué podemos ayudarte?", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                    opciones.chunked(2).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                        ) {
                            fila.forEach { (catKey, catLabel) ->
                                val esSel = categoria == catKey
                                Surface(
                                    onClick = { categoria = catKey },
                                    color = if (esSel) FDColors.PrimarySubtle else FDColors.InputBackground,
                                    shape = FDShapes.Medium,
                                    border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = FDSpacing.sm, vertical = FDSpacing.md),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            catLabel,
                                            style = FDType.Body.copy(
                                                fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // 2. Cuéntanos qué ocurre (un solo campo grande).
                Text("Cuéntanos qué ocurre", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    placeholder = { Text("Escribe aquí qué problema estás teniendo...") },
                    minLines = 4,
                    maxLines = 6,
                    shape = FDShapes.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Lo técnico (sede, usuario, equipo) lo juntamos nosotros. Tú solo cuéntalo con tus palabras.",
                    style = FDType.Caption.copy(color = FDColors.TextSecondary)
                )

                errorLocal?.let {
                    Text(it, style = FDType.Caption.copy(color = FDColors.Error))
                }

                // 3. Una sola acción principal.
                Surface(
                    onClick = {
                        when {
                            categoria == null -> errorLocal = "Elige primero en qué podemos ayudarte."
                            descripcion.trim().length < 8 -> errorLocal = "Cuéntanos con al menos 8 caracteres qué está pasando."
                            else -> {
                                errorLocal = null
                                onConfirmar(categoria!!, descripcion.trim())
                            }
                        }
                    },
                    color = if (categoria != null && descripcion.trim().length >= 8) FDColors.Primary else FDColors.Border,
                    shape = FDShapes.Full,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = FDSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Enviar solicitud", style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
                TextButton(onClick = onDismiss, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
                    Text("Ahora no", style = FDType.Body.copy(color = FDColors.TextSecondary))
                }
            }
        }
    }
}
