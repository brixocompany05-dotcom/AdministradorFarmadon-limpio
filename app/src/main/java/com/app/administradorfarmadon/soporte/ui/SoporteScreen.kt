package com.app.administradorfarmadon.soporte.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Soporte al Cliente — CHAT REAL 100% PRODUCCIÓN (Estilo WhatsApp 2026)
 * - Mismo chat thread para mensajes continuos de la misma conversación.
 * - ID único por mensaje, ordenamiento cronológico estricto sin brincos ni incoherencias.
 * - Checks de lectura reales estilo WhatsApp (✓ / ✓✓ / ✓✓ Azul cuando es leído).
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
                        // Avatar con indicador verde "En Línea"
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(FDColors.PrimarySubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎧", fontSize = 24.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                                    .border(2.dp, FDColors.Surface, CircleShape)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                            ) {
                                Text(
                                    text = "Soporte BRIXO Central",
                                    style = FDType.Heading2.copy(
                                        fontFamily = InterPremium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                )
                                Text(
                                    text = "🟢 En línea",
                                    style = FDType.Caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF22C55E)
                                    )
                                )
                            }
                            val sedeInfo = SessionManager.sucursalNombre.ifBlank { "Sede Operativa" }
                            val userInfo = SessionManager.nombreUsuario.ifBlank { "Operador" }
                            Text(
                                text = "Sede: $sedeInfo · Usuario: $userInfo",
                                style = FDType.BodySmall.copy(color = FDColors.TextSecondary, fontFamily = InterPremium)
                            )
                        }
                    }

                    // Botones de acción directa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        Surface(
                            onClick = { viewModel.contactarWhatsApp(context) },
                            color = Color(0xFF25D366),
                            shape = FDShapes.Full
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                            ) {
                                Text("💬", fontSize = 14.sp)
                                Text(
                                    text = "WhatsApp Directo",
                                    style = FDType.Label.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

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
                                    text = "Correo Oficial",
                                    style = FDType.Label.copy(color = FDColors.TextSecondary)
                                )
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
                            Text("✔️", fontSize = 16.sp)
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
                            Text("⚠️", fontSize = 16.sp)
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
                    // Columna 1: Conversaciones / Consultas
                    Box(modifier = Modifier.weight(0.38f).fillMaxHeight()) {
                        PanelConversaciones(
                            tickets = uiState.tickets,
                            seleccionado = uiState.ticketSeleccionado,
                            cargando = uiState.cargando,
                            onSeleccionar = { viewModel.seleccionarTicket(it) },
                            onNuevaConsulta = { viewModel.prepararNuevaConsultaDirecta() }
                        )
                    }

                    // Columna 2: Chat Activo estilo WhatsApp
                    Box(modifier = Modifier.weight(0.62f).fillMaxHeight()) {
                        PanelVistaChat(
                            ticket = uiState.ticketSeleccionado,
                            textoInput = textoMensajeInput,
                            cargandoEnvio = uiState.creandoTicket,
                            onTextoInputChange = { textoMensajeInput = it },
                            onNuevaConsulta = { viewModel.prepararNuevaConsultaDirecta() },
                            onEnviarMensajeDirecto = {
                                if (textoMensajeInput.trim().isNotBlank()) {
                                    val msg = textoMensajeInput.trim()
                                    textoMensajeInput = ""
                                    viewModel.enviarMensajeEnChat(msg)
                                }
                            },
                            onReintentarMensaje = { ticketId, msgTexto ->
                                viewModel.reintentarEnvioMensaje(ticketId, msgTexto)
                            },
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
                    if (uiState.ticketSeleccionado != null) {
                        PanelVistaChat(
                            ticket = uiState.ticketSeleccionado,
                            textoInput = textoMensajeInput,
                            cargandoEnvio = uiState.creandoTicket,
                            onTextoInputChange = { textoMensajeInput = it },
                            onNuevaConsulta = { viewModel.prepararNuevaConsultaDirecta() },
                            onEnviarMensajeDirecto = {
                                if (textoMensajeInput.trim().isNotBlank()) {
                                    val msg = textoMensajeInput.trim()
                                    textoMensajeInput = ""
                                    viewModel.enviarMensajeEnChat(msg)
                                }
                            },
                            onReintentarMensaje = { ticketId, msgTexto ->
                                viewModel.reintentarEnvioMensaje(ticketId, msgTexto)
                            },
                            onContactarWhatsApp = { viewModel.contactarWhatsApp(context, it) }
                        )
                    } else {
                        PanelConversaciones(
                            tickets = uiState.tickets,
                            seleccionado = uiState.ticketSeleccionado,
                            cargando = uiState.cargando,
                            onSeleccionar = { viewModel.seleccionarTicket(it) },
                            onNuevaConsulta = { viewModel.prepararNuevaConsultaDirecta() }
                        )
                    }
                }
            }
        }

        // Modal Opcional de Nueva Consulta
        if (uiState.mostrarModalNuevoTicket) {
            DialogoNuevaConsultaChat(
                cargando = uiState.creandoTicket,
                onDismiss = { viewModel.cerrarModalNuevoTicket() },
                onConfirmar = { asunto, cat, prio, desc ->
                    viewModel.crearTicket(asunto, cat, prio, desc)
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
    onNuevaConsulta: () -> Unit
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
            // Título + Botón Nueva Consulta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Conversaciones",
                        style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                    )
                    Text(
                        text = if (tickets.isEmpty()) "Sin mensajes" else "${tickets.size} consultas",
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
                        Text("➕", fontSize = 12.sp)
                        Text(
                            text = "Nueva Consulta",
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
                        Text("💬", fontSize = 36.sp)
                        Text(
                            text = "No tienes mensajes pendientes",
                            style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                        )
                        Text(
                            text = "Toca 'Nueva Consulta' arriba para iniciar una conversación directa con el equipo técnico.",
                            style = FDType.Caption.copy(color = FDColors.TextSecondary),
                            modifier = Modifier.padding(horizontal = FDSpacing.md)
                        )
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
    onClick: () -> Unit
) {
    val fondoColor = if (isSelected) FDColors.PrimarySubtle else FDColors.SurfaceHover.copy(alpha = 0.4f)
    val bordeColor = if (isSelected) FDColors.Primary.copy(alpha = 0.5f) else FDColors.Border.copy(alpha = 0.4f)

    val emojiCat = when (ticket.categoria.uppercase()) {
        "FACTURACION_SUNAT" -> "📄"
        "CAJA_VENTAS" -> "🎛️"
        "INVENTARIO_STOCK" -> "📦"
        "HARDWARE_IMPRESORA" -> "🖨️"
        else -> "💬"
    }

    val timestampReferencia = maxOf(ticket.actualizadoMs, ticket.fechaMs)
    val fechaFormateada = remember(timestampReferencia) {
        if (timestampReferencia > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampReferencia))
        } else ""
    }

    val ultimoMensajeTexto = remember(ticket) {
        val msgs = ticket.obtenerTodosLosMensajes()
        msgs.lastOrNull()?.texto ?: ticket.descripcion
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
            // Icono de Categoría
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Border.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emojiCat, fontSize = 18.sp)
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
                    Text(
                        text = fechaFormateada,
                        style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                    )
                }

                Text(
                    text = ultimoMensajeTexto,
                    style = FDType.BodySmall.copy(
                        fontSize = 11.5.sp,
                        color = FDColors.TextSecondary,
                        fontFamily = InterPremium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Status Tag Sutil
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeEstadoChat(ticket.estado)
                    Text(
                        text = ticket.numeroTicket,
                        style = FDType.Caption.copy(fontSize = 9.5.sp, color = FDColors.TextTertiary)
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
    textoInput: String,
    cargandoEnvio: Boolean,
    onTextoInputChange: (String) -> Unit,
    onNuevaConsulta: () -> Unit,
    onEnviarMensajeDirecto: () -> Unit,
    onReintentarMensaje: (ticketId: String, texto: String) -> Unit,
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
                    Row(
                        modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FDColors.PrimarySubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎧", fontSize = 20.sp)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = ticket.asunto,
                                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                                ) {
                                    val sedeNombreReal = ticket.sucursalNombre.ifBlank { SessionManager.sucursalNombre }
                                    Text(
                                        text = "${ticket.numeroTicket} · $sedeNombreReal",
                                        style = FDType.Caption.copy(color = FDColors.TextSecondary)
                                    )
                                    Text("•", style = FDType.Caption.copy(color = FDColors.TextTertiary))
                                    BadgeEstadoChat(ticket.estado)
                                }
                            }
                        }

                        // WhatsApp Directo sobre este ticket
                        Surface(
                            onClick = { onContactarWhatsApp("Consulta sobre ${ticket.numeroTicket}: ${ticket.asunto}") },
                            color = Color(0xFF25D366).copy(alpha = 0.12f),
                            shape = FDShapes.Full,
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💬", fontSize = 12.sp)
                                Text(
                                    text = "Abrir en WhatsApp",
                                    style = FDType.Caption.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        fontFamily = InterPremium
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. CUERPO SCROLLABLE DEL CHAT (BURBUJAS MÚLTIPLES SINCRO REAL SIN BRINCOS)
                val listState = rememberLazyListState()
                val todosLosMensajes = remember(ticket) { ticket.obtenerTodosLosMensajes() }

                // Scroll suave e instantáneo al último mensaje al actualizar la lista
                LaunchedEffect(todosLosMensajes.size) {
                    if (todosLosMensajes.isNotEmpty()) {
                        listState.animateScrollToItem(todosLosMensajes.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(FDColors.Background.copy(alpha = 0.4f))
                        .padding(horizontal = FDSpacing.xl, vertical = FDSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                ) {
                    items(todosLosMensajes, key = { it.id }) { msg ->
                        val horaStr = remember(msg.fechaMs) {
                            if (msg.fechaMs > 0) SimpleDateFormat("HH:mm a", Locale.getDefault()).format(Date(msg.fechaMs)) else ""
                        }

                        if (msg.esDeFarmacia) {
                            BurbujaMensajeFarmacia(
                                usuario = msg.autorNombre.ifBlank { SessionManager.nombreUsuario.ifBlank { "Farmacia" } },
                                mensaje = msg.texto,
                                fechaStr = horaStr,
                                estadoLectura = msg.estadoLectura,
                                tieneError = msg.tieneError,
                                onReintentar = { onReintentarMensaje(ticket.id, msg.texto) }
                            )
                        } else {
                            BurbujaRespuestaBrixo(
                                respondidoPor = msg.autorNombre.ifBlank { "BRIXO Soporte Técnico" },
                                respuesta = msg.texto,
                                fechaStr = horaStr
                            )
                        }
                    }
                }

                // 3. BARRA INFERIOR DE ENTRADA DE TEXTO O BANNER DE HISTORIAL SI ESTÁ RESUELTO
                if (ticket.estaResuelto) {
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
                            Text("🔒", fontSize = 18.sp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Consulta Resuelta (Modo Historial Inmutable)",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                                )
                                Text(
                                    text = "Esta conversación ha sido finalizada y queda guardada como historial. Para una nueva inquietud, inicia una nueva consulta.",
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
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
                                    Text("➕", fontSize = 11.sp)
                                    Text(
                                        text = "Nueva Consulta",
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    BarraChatInput(
                        textoInput = textoInput,
                        cargandoEnvio = cargandoEnvio,
                        placeholderText = "Escribe tu respuesta o mensaje...",
                        onTextoInputChange = onTextoInputChange,
                        onEnviar = onEnviarMensajeDirecto
                    )
                }
            }
        } else {
            // VISTA CHAT DE NUEVA CONSULTA INMEDIATA
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Nueva Consulta
                Surface(
                    color = FDColors.SurfaceHover.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FDColors.PrimarySubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💬", fontSize = 20.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Nueva Consulta Directa a BRIXO Soporte",
                                style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                            )
                            Text(
                                text = "🟢 En línea · Asistencia inmediata por chat",
                                style = FDType.Caption.copy(color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Cuerpo Iniciar Chat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(FDColors.Background.copy(alpha = 0.4f))
                        .padding(horizontal = FDSpacing.xxl, vertical = FDSpacing.xl),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎧", fontSize = 48.sp)
                    Spacer(Modifier.height(FDSpacing.xs))
                    Text(
                        text = "¿En qué podemos ayudarte hoy?",
                        style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                    )
                    Text(
                        text = "Escribe tu consulta abajo. BRIXO Soporte identificará el tema y creará la conversación automáticamente.",
                        style = FDType.Body.copy(color = FDColors.TextSecondary),
                        modifier = Modifier.padding(horizontal = FDSpacing.xl)
                    )

                    Spacer(Modifier.height(FDSpacing.xl))

                    // Chips sugeridos para autorellenar con 1 toque
                    Text(
                        text = "Temas Frecuentes (toca para escribir):",
                        style = FDType.Caption.copy(color = FDColors.TextTertiary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(FDSpacing.xs))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChipSugerencia(
                            label = "📄 Facturación / SUNAT",
                            onClick = { onTextoInputChange("Consulta sobre Facturación SUNAT: ") }
                        )
                        ChipSugerencia(
                            label = "🎛️ POS y Caja",
                            onClick = { onTextoInputChange("Consulta sobre Punto de Venta y Caja: ") }
                        )
                        ChipSugerencia(
                            label = "🖨️ Impresora de Tickets",
                            onClick = { onTextoInputChange("Consulta sobre Impresora de tickets: ") }
                        )
                        ChipSugerencia(
                            label = "📦 Inventario y Lotes",
                            onClick = { onTextoInputChange("Consulta sobre Inventario y Lotes: ") }
                        )
                    }
                }

                // Barra Chat Input Nueva Consulta
                BarraChatInput(
                    textoInput = textoInput,
                    cargandoEnvio = cargandoEnvio,
                    placeholderText = "Escribe tu mensaje o consulta directa para BRIXO...",
                    onTextoInputChange = onTextoInputChange,
                    onEnviar = onEnviarMensajeDirecto
                )
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
        modifier = Modifier.fillMaxWidth()
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
                color = if (textoInput.trim().isNotBlank() && !cargandoEnvio) FDColors.Primary else FDColors.Border,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (cargandoEnvio) {
                        CircularProgressIndicator(
                            color = FDColors.PrimaryText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (textoInput.trim().isNotBlank()) FDColors.PrimaryText else FDColors.TextDisabled,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipSugerencia(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = FDColors.InputBackground,
        shape = FDShapes.Full,
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary),
            modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = 6.dp)
        )
    }
}

// ───────────────────────────── BURBUJAS DE CHAT CON CHECKS DE LECTURA REALES Y REINTENTO ─────────────────────────────

@Composable
private fun CheckLecturaWhatsApp(estadoLectura: String) {
    val (ticks, color) = when (estadoLectura.uppercase()) {
        "LEIDO" -> "✓✓" to Color(0xFF34B7F1) // Doble check azul estilo WhatsApp
        "ENTREGADO" -> "✓✓" to FDColors.TextTertiary // Doble check gris
        else -> "✓" to FDColors.TextTertiary // Un check gris
    }
    Text(
        text = ticks,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = color
    )
}

@Composable
private fun BurbujaMensajeFarmacia(
    usuario: String,
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
            color = if (tieneError) FDColors.Error.copy(alpha = 0.12f) else FDColors.PrimarySubtle,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            border = BorderStroke(1.dp, if (tieneError) FDColors.Error.copy(alpha = 0.4f) else FDColors.Primary.copy(alpha = 0.25f)),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = FDSpacing.lg, vertical = FDSpacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = usuario,
                        style = FDType.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (tieneError) FDColors.Error else FDColors.Primary,
                            fontFamily = InterPremium
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = fechaStr,
                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                        )
                        if (!tieneError) {
                            CheckLecturaWhatsApp(estadoLectura)
                        }
                    }
                }
                Text(
                    text = mensaje,
                    style = FDType.Body.copy(
                        fontSize = 13.5.sp,
                        color = FDColors.TextPrimary,
                        fontFamily = InterPremium
                    )
                )

                if (tieneError && onReintentar != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onReintentar,
                            color = FDColors.Error,
                            shape = FDShapes.Full
                        ) {
                            Text(
                                text = "🔄 Reintentar envío",
                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FDColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎧", fontSize = 16.sp)
                }

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
private fun BadgeEstadoChat(estado: String) {
    val (color, label, emoji) = when (estado.uppercase()) {
        "RESUELTO" -> Triple(Color(0xFF22C55E), "Resuelto", "✔️")
        "EN_PROCESO" -> Triple(Color(0xFFEAB308), "En atención", "⏳")
        else -> Triple(FDColors.Primary, "Enviado", "🟢")
    }
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = FDShapes.Full,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 9.sp)
            Text(
                text = label,
                style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = color)
            )
        }
    }
}

// ───────────────────────────── DIÁLOGO NUEVA CONSULTA ESTILO CHAT DELICADO ─────────────────────────────

@Composable
private fun DialogoNuevaConsultaChat(
    cargando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (asunto: String, categoria: String, prioridad: String, descripcion: String) -> Unit
) {
    var asunto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("CONSULTA_GENERAL") }
    var descripcion by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    val opcionesCategorias = listOf(
        Triple("FACTURACION_SUNAT", "📄 Facturación y SUNAT", "Series, comprobantes y APISUNAT"),
        Triple("CAJA_VENTAS", "🎛️ Punto de Venta y Caja", "Ventas, arqueos y cobros"),
        Triple("INVENTARIO_STOCK", "📦 Inventario y Lotes", "Stock, FEFO y productos"),
        Triple("HARDWARE_IMPRESORA", "🖨️ Impresoras y Escáner", "Impresión de tickets y dispositivos"),
        Triple("CONSULTA_GENERAL", "💬 Consulta General", "Dudas generales y cuenta")
    )

    Dialog(onDismissRequest = { if (!cargando) onDismiss() }) {
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.XLarge,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(FDSpacing.xxl),
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
                        Text("💬", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Nueva Consulta a BRIXO",
                                style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium)
                            )
                            Text(
                                text = "Escribe tu consulta y un especialista te responderá.",
                                style = FDType.Caption,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border)

                // Asunto corto
                OutlinedTextField(
                    value = asunto,
                    onValueChange = { asunto = it },
                    label = { Text("Tema o asunto de tu consulta") },
                    placeholder = { Text("Ej: Consulta sobre impresión de ticket") },
                    singleLine = true,
                    shape = FDShapes.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Categoría
                Text("Categoría:", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                    opcionesCategorias.forEach { (catKey, catLabel, catSub) ->
                        val esSel = categoria == catKey
                        Surface(
                            onClick = { categoria = catKey },
                            color = if (esSel) FDColors.PrimarySubtle else FDColors.InputBackground,
                            shape = FDShapes.Medium,
                            border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(catLabel, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text(catSub, style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary))
                                }
                                if (esSel) {
                                    Text("✔️", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Detalle del mensaje
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Mensaje o detalle") },
                    placeholder = { Text("Explica brevemente tu consulta o lo que necesitas...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = FDShapes.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                errorLocal?.let {
                    Text(it, style = FDType.Caption.copy(color = FDColors.Error))
                }

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !cargando) {
                        Text("Cancelar", style = FDType.Body.copy(color = FDColors.TextSecondary))
                    }
                    Spacer(Modifier.width(FDSpacing.sm))
                    Surface(
                        onClick = {
                            if (asunto.trim().length < 4) {
                                errorLocal = "Ingresa un tema o asunto breve."
                            } else if (descripcion.trim().length < 8) {
                                errorLocal = "Escribe un mensaje de al menos 8 caracteres."
                            } else {
                                errorLocal = null
                                onConfirmar(asunto, categoria, "NORMAL", descripcion)
                            }
                        },
                        color = FDColors.Primary,
                        shape = FDShapes.Full
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = FDColors.PrimaryText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text("📩", fontSize = 14.sp)
                                Text("Enviar Consulta", style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.PrimaryText))
                            }
                        }
                    }
                }
            }
        }
    }
}
