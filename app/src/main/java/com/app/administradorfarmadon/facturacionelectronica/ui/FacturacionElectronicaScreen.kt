package com.app.administradorfarmadon.facturacionelectronica.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.facturacion.configuracion.ui.ContenidoConfiguracionFiscal
import com.app.administradorfarmadon.facturacion.documentos.logica.FacturacionDocumentosUiState
import com.app.administradorfarmadon.facturacion.documentos.logica.FacturacionDocumentosViewModel
import com.app.administradorfarmadon.facturacion.envio.datos.ReporteEnvioLote
import com.app.administradorfarmadon.facturacion.envio.worker.FacturacionEnvioWorker
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FacturacionElectronicaScreen(
    onVolver: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: FacturacionDocumentosViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val isWide = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FDSpacing.xxl, vertical = FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            // ── 1. CABECERA ENTERPRISE ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                if (onVolver != null) {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(FDShapes.Medium)
                            .background(FDColors.Surface)
                            .border(1.dp, FDColors.Border, FDShapes.Medium)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FDColors.TextPrimary)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FACTURACIÓN ELECTRÓNICA SUNAT",
                        style = FDType.Label.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = FDColors.TextTertiary
                    )
                    Text(
                        text = "Bandeja Fiscal de Comprobantes Oficiales",
                        style = FDType.Heading1.copy(fontFamily = InterPremium),
                        color = FDColors.TextPrimary
                    )
                }

                // Botón superior de envío masivo de pendientes con confirmación (F5-B)
                if (state.totalPendientesEnCola > 0) {
                    FDBotonPrimario(
                        texto = if (state.enviandoLote) "Enviando..." else "Enviar pendientes (${state.totalPendientesEnCola})",
                        onClick = { viewModel.solicitarConfirmacionLote() },
                        cargando = state.enviandoLote,
                        icono = Icons.Default.CloudUpload,
                        modifier = Modifier.height(42.dp)
                    )
                }

                // Indicador de estado del emisor fiscal
                val emisorListo = state.emisor?.estaCompleta == true
                Surface(
                    color = if (emisorListo) FDColors.Success.copy(alpha = 0.1f) else FDColors.Warning.copy(alpha = 0.1f),
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, if (emisorListo) FDColors.Success else FDColors.Warning)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (emisorListo) FDColors.Success else FDColors.Warning)
                        )
                        Text(
                            text = if (emisorListo) "EMISOR ACTIVO" else "EMISOR PENDIENTE",
                            style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (emisorListo) FDColors.Success else FDColors.Warning
                        )
                    }
                }
            }

            // Notificación de estado o mensaje de error/éxito
            if (state.mensajeError != null) {
                Surface(
                    color = FDColors.Error.copy(alpha = 0.1f),
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FDSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = FDColors.Error)
                        Text(state.mensajeError ?: "", style = FDType.BodySmall, color = FDColors.Error, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.limpiarMensajes() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = FDColors.Error)
                        }
                    }
                }
            } else if (state.mensajeExito != null) {
                Surface(
                    color = FDColors.Success.copy(alpha = 0.1f),
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Success),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FDSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = FDColors.Success)
                        Text(state.mensajeExito ?: "", style = FDType.BodySmall, color = FDColors.Success, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.limpiarMensajes() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = FDColors.Success)
                        }
                    }
                }
            }

            // ── 2. PESTAÑAS ENTERPRISE (Underline Tabs) ──
            val pestanas = listOf(
                "Documentos (${state.totalDocumentos})",
                "Resumen",
                "Emisor Fiscal"
            )

            TabRow(
                selectedTabIndex = state.pestanaActual,
                containerColor = Color.Transparent,
                contentColor = FDColors.Primary,
                divider = { HorizontalDivider(color = FDColors.Border) }
            ) {
                pestanas.forEachIndexed { index, titulo ->
                    Tab(
                        selected = state.pestanaActual == index,
                        onClick = { viewModel.setPestana(index) },
                        text = {
                            Text(
                                text = titulo,
                                style = FDType.Body.copy(
                                    fontWeight = if (state.pestanaActual == index) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = InterPremium
                                ),
                                color = if (state.pestanaActual == index) FDColors.Primary else FDColors.TextSecondary
                            )
                        }
                    )
                }
            }

            // ── 3. CONTENIDO SEGÚN PESTAÑA ──
            when (state.pestanaActual) {
                0 -> PestanaDocumentos(state = state, viewModel = viewModel, isWide = isWide)
                1 -> PestanaResumen(state = state)
                2 -> PestanaEmisor(isWide = isWide)
            }
        }

        // ── DIÁLOGOS ENTERPRISE (F5-B / F5-C) ──
        val context = LocalContext.current

        // Diálogo de confirmación individual (F5-B)
        state.documentoAConfirmarEnvio?.let { docAEnviar ->
            DialogoConfirmarEnvioIndividual(
                doc = docAEnviar,
                enviando = state.enviandoDocId == docAEnviar.id,
                onConfirmar = {
                    viewModel.enviarDocumento(docAEnviar.id) {
                        val farmaciaId = SessionManager.clienteIdGarantizado
                        FacturacionEnvioWorker.encolarReintento(context, farmaciaId)
                    }
                },
                onCancelar = { viewModel.cancelarConfirmacionEnvio() }
            )
        }

        // Diálogo de confirmación masivo de lote (F5-B)
        if (state.mostrarDialogoConfirmarLote) {
            DialogoConfirmarEnvioLote(
                totalPendientes = state.totalPendientesEnCola,
                enviando = state.enviandoLote,
                onConfirmar = {
                    viewModel.enviarLotePendientes {
                        val farmaciaId = SessionManager.clienteIdGarantizado
                        FacturacionEnvioWorker.encolarReintento(context, farmaciaId)
                    }
                },
                onCancelar = { viewModel.cancelarConfirmacionLote() }
            )
        }

        // Diálogo con resultado individual de lote procesado (F5-C)
        state.resultadoLoteReciente?.let { reporte ->
            DialogoResultadoLote(
                reporte = reporte,
                onCerrar = { viewModel.cerrarReporteLote() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// PESTAÑA 1: BANDEJA DE DOCUMENTOS (F3 / F4)
// ─────────────────────────────────────────────

@Composable
private fun PestanaDocumentos(
    state: FacturacionDocumentosUiState,
    viewModel: FacturacionDocumentosViewModel,
    isWide: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
    ) {
        // Filtros y Buscador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.busquedaTexto,
                onValueChange = { viewModel.setBusquedaTexto(it) },
                placeholder = { Text("Buscar por número, cliente o serie...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextTertiary) },
                trailingIcon = {
                    if (state.busquedaTexto.isNotBlank()) {
                        IconButton(onClick = { viewModel.setBusquedaTexto("") }) {
                            Icon(Icons.Default.Close, null, tint = FDColors.TextTertiary)
                        }
                    }
                },
                singleLine = true,
                shape = FDShapes.Medium,
                modifier = Modifier.weight(1f)
            )

            // Selector por Tipo
            val tipos = listOf("TODOS", "BOLETA", "FACTURA", "NOTA_CREDITO", "COMUNICACION_BAJA")
            var expandirTipo by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expandirTipo = true },
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border)
                ) {
                    Text(
                        text = when (state.filtroTipo) {
                            "BOLETA" -> "Boletas"
                            "FACTURA" -> "Facturas"
                            "NOTA_CREDITO" -> "Notas de Crédito"
                            "COMUNICACION_BAJA" -> "Bajas"
                            else -> "Todos los tipos"
                        },
                        style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = expandirTipo,
                    onDismissRequest = { expandirTipo = false }
                ) {
                    tipos.forEach { t ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (t) {
                                        "BOLETA" -> "Boletas"
                                        "FACTURA" -> "Facturas"
                                        "NOTA_CREDITO" -> "Notas de Crédito"
                                        "COMUNICACION_BAJA" -> "Comunicaciones de Baja"
                                        else -> "Todos los tipos"
                                    }
                                )
                            },
                            onClick = {
                                viewModel.setFiltroTipo(t)
                                expandirTipo = false
                            }
                        )
                    }
                }
            }

            // Selector por Sede
            if (state.sedes.isNotEmpty()) {
                var expandirSede by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expandirSede = true },
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        val sedeNombre = state.sedes.firstOrNull { it.id == state.filtroSedeId }?.nombre ?: "Todas las sedes"
                        Text(sedeNombre, style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = expandirSede,
                        onDismissRequest = { expandirSede = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas las sedes") },
                            onClick = {
                                viewModel.setFiltroSede("TODAS")
                                expandirSede = false
                            }
                        )
                        state.sedes.forEach { sede ->
                            DropdownMenuItem(
                                text = { Text(sede.nombre) },
                                onClick = {
                                    viewModel.setFiltroSede(sede.id)
                                    expandirSede = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Chips de Filtro por Estado
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            val estados = listOf(
                "TODOS" to "Todos (${state.totalDocumentos})",
                "PENDIENTE" to "🟡 En cola (${state.totalPendientesEnCola})",
                "ATENCION" to "🔴 Requieren atención (${state.totalRequierenAtencion})",
                FacturacionDocumento.ESTADO_ACEPTADO to "🟢 Aceptadas (${state.totalAceptados})",
                FacturacionDocumento.ESTADO_ANULADO to "⚪ Anuladas (${state.totalAnulados})"
            )

            items(estados) { (est, label) ->
                val seleccionado = state.filtroEstado == est
                FilterChip(
                    selected = seleccionado,
                    onClick = { viewModel.setFiltroEstado(est) },
                    label = { Text(label, style = FDType.Label.copy(fontWeight = FontWeight.Bold)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FDColors.PrimarySubtle,
                        selectedLabelColor = FDColors.Primary
                    )
                )
            }
        }

        // Cuerpo: Lista (60%) + Detalle (40%) si pantalla ancha
        val listaFiltrada = state.documentosFiltrados
        if (listaFiltrada.isEmpty()) {
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (state.documentos.isEmpty()) "No hay documentos fiscales emitidos aún" else "No hay documentos con los filtros seleccionados",
                            style = FDType.Heading3,
                            color = FDColors.TextSecondary
                        )
                        Text(
                            text = if (state.documentos.isEmpty()) "Cada venta registrada en el POS aparecerá automáticamente en esta bandeja fiscal." else "Prueba cambiando o limpiando los filtros de búsqueda.",
                            style = FDType.BodySmall,
                            color = FDColors.TextTertiary
                        )
                    }
                }
            }
        } else if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.lg)
            ) {
                // Lista izquierda (60%)
                LazyColumn(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    items(listaFiltrada) { doc ->
                        TarjetaDocumentoItem(
                            doc = doc,
                            seleccionado = doc.id == state.documentoSeleccionado?.id,
                            onClick = { viewModel.seleccionarDocumento(doc) }
                        )
                    }
                }

                // Detalle derecho (40%)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    PanelDetalleDocumento(
                        doc = state.documentoSeleccionado,
                        state = state,
                        onEnviar = { docAEnviar ->
                            viewModel.solicitarConfirmacionEnvio(docAEnviar)
                        },
                        onCerrar = { viewModel.cerrarDetalle() }
                    )
                }
            }
        } else {
            // Pantalla compacta: lista vertical simple
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                items(listaFiltrada) { doc ->
                    TarjetaDocumentoItem(
                        doc = doc,
                        seleccionado = doc.id == state.documentoSeleccionado?.id,
                        onClick = { viewModel.seleccionarDocumento(doc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaDocumentoItem(
    doc: FacturacionDocumento,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (seleccionado) FDColors.Primary else FDColors.Border
    val bgColor = if (seleccionado) FDColors.PrimarySubtle.copy(alpha = 0.3f) else FDColors.Surface

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = FDShapes.Medium,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            // Icono por tipo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(FDShapes.Small)
                    .background(FDColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (doc.tipo) {
                        "FACTURA" -> Icons.Default.Receipt
                        "NOTA_CREDITO" -> Icons.Default.RemoveCircleOutline
                        "COMUNICACION_BAJA" -> Icons.Default.Cancel
                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                    },
                    contentDescription = null,
                    tint = FDColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Text(
                        text = doc.numeroCompleto.ifBlank { "${doc.serie}-${doc.correlativo}" },
                        style = FDType.Body.copy(fontWeight = FontWeight.Black, fontFamily = InterPremium),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = "· ${doc.tipo}",
                        style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = FDColors.TextSecondary
                    )
                }

                Text(
                    text = "${doc.clienteNombre} (${doc.clienteTipoDoc}: ${doc.clienteNumeroDoc.ifBlank { "Sin Doc" }})",
                    style = FDType.Caption,
                    color = FDColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (doc.fechaMs > 0L) {
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                    Text(
                        text = sdf.format(Date(doc.fechaMs)),
                        style = FDType.Caption.copy(fontSize = 10.sp),
                        color = FDColors.TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "S/ ${String.format(Locale.US, "%.2f", doc.total)}",
                    style = FDType.Body.copy(fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )

                BadgeEstadoDoc(estado = doc.estadoEnvio, numeroQuemado = doc.numeroQuemado)
            }
        }
    }
}

@Composable
private fun BadgeEstadoDoc(estado: String, numeroQuemado: Boolean = false) {
    val (color, label) = when {
        numeroQuemado || estado == FacturacionDocumento.ESTADO_RECHAZADO -> FDColors.Error to "🔴 REQUIERE ATENCIÓN"
        estado == FacturacionDocumento.ESTADO_ACEPTADO -> FDColors.Success to "🟢 ACEPTADO"
        estado == FacturacionDocumento.ESTADO_ENVIADO -> FDColors.Warning to "🟡 ENVIANDO"
        estado == FacturacionDocumento.ESTADO_ANULADO -> FDColors.TextTertiary to "⚪ ANULADO"
        else -> FDColors.Warning to "🟡 EN COLA"
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PanelDetalleDocumento(
    doc: FacturacionDocumento?,
    state: FacturacionDocumentosUiState,
    onEnviar: (FacturacionDocumento) -> Unit,
    onCerrar: () -> Unit
) {
    if (doc == null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(FDSpacing.xl)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                Icon(Icons.Default.Info, null, tint = FDColors.TextTertiary, modifier = Modifier.size(36.dp))
                Text("Selecciona un documento para ver el detalle legal y la venta vinculada", style = FDType.BodySmall, color = FDColors.TextTertiary)
            }
        }
        return
    }

    val context = LocalContext.current
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FDSpacing.xl)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = doc.numeroCompleto.ifBlank { "${doc.serie}-${doc.correlativo}" },
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
                Text(text = "Tipo: ${doc.tipo}", style = FDType.BodySmall, color = FDColors.TextSecondary)
            }
            IconButton(onClick = onCerrar) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        BadgeEstadoDoc(estado = doc.estadoEnvio, numeroQuemado = doc.numeroQuemado)

        // ── ACCIONES Y AVISOS DE LA MÁQUINA DE ESTADOS SUNAT (F4 / F5-E) ──
        if (doc.numeroQuemado || doc.estadoEnvio == FacturacionDocumento.ESTADO_RECHAZADO) {
            // F5-E: Lenguaje fiscal prudente, sin alarmismos
            Surface(
                color = FDColors.Error.copy(alpha = 0.1f),
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Cancel, null, tint = FDColors.Error, modifier = Modifier.size(18.dp))
                        Text("DOCUMENTO RECHAZADO", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.Error)
                    }
                    Text(
                        text = "Este comprobante no fue aceptado. Revisa el motivo.\nNo se reutilizará automáticamente este número.",
                        style = FDType.Caption,
                        color = FDColors.TextPrimary
                    )
                    val motivoReal = doc.motivo.ifBlank { doc.ultimoError }
                    if (motivoReal.isNotBlank()) {
                        Text("Motivo: $motivoReal", style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold), color = FDColors.Error)
                    }
                }
            }
        } else if (doc.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (doc.ultimoError.isNotBlank()) {
                    Surface(
                        color = FDColors.Warning.copy(alpha = 0.1f),
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Warning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FDSpacing.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("En cola para reintento: ${doc.ultimoError}", style = FDType.Caption, color = FDColors.Warning)
                        }
                    }
                }

                FDBotonPrimario(
                    texto = if (state.enviandoDocId == doc.id) "Enviando a SUNAT..." else "ENVIAR AHORA A SUNAT",
                    onClick = { onEnviar(doc) },
                    cargando = state.enviandoDocId == doc.id,
                    icono = Icons.Default.CloudUpload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (doc.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO) {
            Surface(
                color = FDColors.Success.copy(alpha = 0.1f),
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Success),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(18.dp))
                        Text("COMPROBANTE ACEPTADO POR SUNAT", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.Success)
                    }
                    if (doc.pdfUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.pdfUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = FDShapes.Medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar PDF Oficial Firmado")
                        }
                    }
                    if (doc.cdrUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.cdrUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = FDShapes.Medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar Constancia CDR (XML)")
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = FDColors.Border)

        // Datos del receptor
        Text("DATOS DEL RECEPTOR", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
        Surface(color = FDColors.Background, shape = FDShapes.Medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Razón / Nombre: ${doc.clienteNombre}", style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold))
                Text("Documento: ${doc.clienteTipoDoc} ${doc.clienteNumeroDoc.ifBlank { "(Sin documento)" }}", style = FDType.Caption, color = FDColors.TextSecondary)
            }
        }

        // Datos fiscales
        Text("DATOS FISCALES", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
        Surface(color = FDColors.Background, shape = FDShapes.Medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Serie: ${doc.serie}", style = FDType.Caption)
                Text("Correlativo: ${doc.correlativo}", style = FDType.Caption)
                Text("Monto Total: S/ ${String.format(Locale.US, "%.2f", doc.total)}", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                if (doc.documentIdProveedor.isNotBlank()) {
                    Text("APISUNAT ID: ${doc.documentIdProveedor}", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                }
                if (doc.responseTimeMs > 0L) {
                    Text("Tiempo de respuesta: ${doc.responseTimeMs} ms", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                }
            }
        }

        // Venta vinculada
        if (doc.ventaId.isNotBlank()) {
            Text("VENTA DE ORIGEN (POS)", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
            if (state.cargandoVentaVinculada) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (state.ventaVinculada != null) {
                val v = state.ventaVinculada
                Surface(color = FDColors.Background, shape = FDShapes.Medium, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Venta ID: ${v.id}", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        Text("Cajero: ${v.cajeroNombre}", style = FDType.Caption)
                        Text("Estado Fiscal en Venta: ${v.estadoFiscal}", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        Text("Ítems vendidos (${v.items.size}):", style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold))
                        v.items.forEach { item ->
                            Text("• ${item.cantidad}x ${item.nombreProducto} (S/ ${String.format(Locale.US, "%.2f", item.subtotal)})", style = FDType.Caption)
                        }
                    }
                }
            } else {
                Text("No se pudo cargar la venta vinculada.", style = FDType.Caption, color = FDColors.TextTertiary)
            }
        }

        // ── HISTORIAL Y TIMELINE DEL COMPROBANTE (F5-D) ──
        Text("HISTORIAL DEL DOCUMENTO", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
        Surface(color = FDColors.Background, shape = FDShapes.Medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(FDSpacing.md)) {
                // Evento 1: Venta registrada en POS
                val fechaVentaStr = if (doc.fechaMs > 0L) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date(doc.fechaMs))
                } else "Fecha no registrada"
                val cajeroNombre = state.ventaVinculada?.cajeroNombre?.ifBlank { "Personal de farmacia" } ?: "Personal de farmacia"

                TimelineItem(
                    icono = Icons.Default.ShoppingCart,
                    color = FDColors.Primary,
                    titulo = "Operación registrada en el POS",
                    subtitulo = "$fechaVentaStr • Cajero: $cajeroNombre"
                )

                // Evento 2: Stock y Caja (verdad física y contable según tipo de documento)
                val (tituloStockCaja, subtituloStockCaja) = when (doc.tipo.uppercase()) {
                    "NOTA_CREDITO" -> Pair(
                        "Stock reintegrado al inventario",
                        "Ingreso kardex por devolución y reembolso registrado"
                    )
                    "COMUNICACION_BAJA" -> Pair(
                        "Venta anulada en el sistema",
                        "Stock y cobro en caja revertidos"
                    )
                    else -> Pair(
                        "Stock descontado del inventario",
                        "Salida kardex confirmada y cobro registrado en caja"
                    )
                }

                TimelineItem(
                    icono = Icons.Default.Inventory2,
                    color = FDColors.Primary,
                    titulo = tituloStockCaja,
                    subtitulo = subtituloStockCaja
                )

                // Evento 3: Intentos de transmisión fiscal a SUNAT
                if (doc.historialIntentos.isNotEmpty()) {
                    doc.historialIntentos.forEachIndexed { idx, intento ->
                        val estado = intento["estado"] as? String ?: "PENDIENTE"
                        val error = intento["error"] as? String ?: ""
                        val motivo = intento["motivo"] as? String ?: ""
                        val duracionMs = (intento["duracionMs"] as? Number)?.toLong() ?: 0L
                        val fechaIntentoMs = (intento["fechaMs"] as? Number)?.toLong() ?: 0L
                        val horaStr = if (fechaIntentoMs > 0L) {
                            SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date(fechaIntentoMs))
                        } else "Intento ${idx + 1}"

                        val (colorIntento, labelIntento, iconoIntento) = when (estado) {
                            FacturacionDocumento.ESTADO_ACEPTADO -> Triple(FDColors.Success, "Aceptado por SUNAT", Icons.Default.CheckCircle)
                            FacturacionDocumento.ESTADO_RECHAZADO -> Triple(FDColors.Error, "Rechazado por SUNAT", Icons.Default.Cancel)
                            FacturacionDocumento.ESTADO_ENVIADO -> Triple(FDColors.Warning, "Enviado (en validación)", Icons.Default.HourglassEmpty)
                            else -> Triple(FDColors.Warning, "En cola para reintento", Icons.Default.Sync)
                        }

                        val detalleIntento = when {
                            motivo.isNotBlank() -> motivo
                            error.isNotBlank() -> error
                            duracionMs > 0L -> "Respuesta en ${duracionMs} ms"
                            else -> "Trámite fiscal ejecutado"
                        }

                        TimelineItem(
                            icono = iconoIntento,
                            color = colorIntento,
                            titulo = "$horaStr • $labelIntento",
                            subtitulo = if (duracionMs > 0L) "$detalleIntento (${duracionMs} ms)" else detalleIntento
                        )
                    }
                } else if (doc.estadoEnvio == FacturacionDocumento.ESTADO_PENDIENTE) {
                    TimelineItem(
                        icono = Icons.Default.HourglassEmpty,
                        color = FDColors.Warning,
                        titulo = "En cola de envío",
                        subtitulo = if (doc.ultimoError.isNotBlank()) "Último reporte: ${doc.ultimoError}" else "Se reintentará automáticamente al tener conexión con SUNAT"
                    )
                } else if (doc.estadoEnvio == FacturacionDocumento.ESTADO_ACEPTADO) {
                    TimelineItem(
                        icono = Icons.Default.CheckCircle,
                        color = FDColors.Success,
                        titulo = "Aceptado por SUNAT",
                        subtitulo = if (doc.responseTimeMs > 0L) "Respuesta en ${doc.responseTimeMs} ms con constancia CDR" else "Constancia CDR generada"
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PESTAÑA 2: RESUMEN DE COMPROBANTES (MÉTRICAS REALES)
// ─────────────────────────────────────────────

@Composable
private fun PestanaResumen(state: FacturacionDocumentosUiState) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
    ) {
        Text(
            text = "INDICADORES OPERATIVOS EN VIVO",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextTertiary
        )

        // Tarjetas métricas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            TarjetaMetrica(
                titulo = "TOTAL EMITIDOS",
                valor = state.totalDocumentos.toString(),
                icono = Icons.AutoMirrored.Filled.ReceiptLong,
                color = FDColors.Primary,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                titulo = "MONTO FACTURADO",
                valor = "S/ ${String.format(Locale.US, "%.2f", state.montoTotalFacturado)}",
                icono = Icons.Default.Payments,
                color = FDColors.Success,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                titulo = "PENDIENTES",
                valor = state.totalPendientes.toString(),
                icono = Icons.Default.HourglassEmpty,
                color = FDColors.Warning,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                titulo = "ACEPTADOS SUNAT",
                valor = state.totalAceptados.toString(),
                icono = Icons.Default.CheckCircle,
                color = FDColors.Success,
                modifier = Modifier.weight(1f)
            )
        }

        // Desglose por tipo de documento
        Text(
            text = "DESGLOSE POR TIPO DE COMPROBANTE",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextTertiary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            FilaConteoTipo("Boletas de Venta", state.totalBoletas, Icons.Default.Receipt, Modifier.weight(1f))
            FilaConteoTipo("Facturas Electrónicas", state.totalFacturas, Icons.Default.Business, Modifier.weight(1f))
            FilaConteoTipo("Notas de Crédito", state.totalNotasCredito, Icons.Default.RemoveCircleOutline, Modifier.weight(1f))
            FilaConteoTipo("Comunicaciones de Baja", state.totalBajas, Icons.Default.Cancel, Modifier.weight(1f))
        }

        // Explicación operativa honesta
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Medium,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(FDSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = FDColors.Primary)
                    Text("Acerca del ciclo de envío fiscal", style = FDType.Heading3)
                }
                Text(
                    text = "Los comprobantes generados en caja viajan a SUNAT a través del servicio oficial APISUNAT. Si no hay conexión a internet, los comprobantes quedan en cola de contingencia (PENDIENTE) y se reintentan automáticamente cuando regresa la conectividad.",
                    style = FDType.BodySmall,
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titulo, style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(valor, style = FDType.Heading1.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
        }
    }
}

@Composable
private fun FilaConteoTipo(titulo: String, cantidad: Int, icono: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            Icon(icono, null, tint = FDColors.Primary, modifier = Modifier.size(24.dp))
            Column {
                Text(titulo, style = FDType.Caption, color = FDColors.TextSecondary)
                Text(cantidad.toString(), style = FDType.Heading2.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            }
        }
    }
}

// ─────────────────────────────────────────────
// PESTAÑA 3: EMISOR FISCAL (CONFIGURACIÓN INTEGRADA COMPLETA)
// ─────────────────────────────────────────────

@Composable
private fun PestanaEmisor(isWide: Boolean) {
    ContenidoConfiguracionFiscal(isWide = isWide)
}

// ─────────────────────────────────────────────
// COMPONENTES AUXILIARES ENTERPRISE (F5)
// ─────────────────────────────────────────────

@Composable
private fun TimelineItem(
    icono: ImageVector,
    color: Color,
    titulo: String,
    subtitulo: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(titulo, style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text(subtitulo, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun DialogoConfirmarEnvioIndividual(
    doc: FacturacionDocumento,
    enviando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!enviando) onCancelar() },
        title = {
            Text(
                text = "Enviar ${doc.numeroCompleto.ifBlank { "${doc.serie}-${doc.correlativo}" }}",
                style = FDType.Heading2.copy(fontWeight = FontWeight.Bold),
                color = FDColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                HorizontalDivider(color = FDColors.Border)
                Text(
                    text = "Esto intentará subir el comprobante a SUNAT.",
                    style = FDType.Body.copy(fontWeight = FontWeight.SemiBold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "• NO crea otra venta.\n• NO toca stock.\n• NO repite números.",
                    style = FDType.BodySmall,
                    color = FDColors.TextSecondary
                )
                if (doc.total > 0.0) {
                    Surface(
                        color = FDColors.PrimarySubtle.copy(alpha = 0.4f),
                        shape = FDShapes.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total: S/ ${String.format(Locale.US, "%.2f", doc.total)} | Cliente: ${doc.clienteNombre}",
                            style = FDType.Caption.copy(fontWeight = FontWeight.Medium),
                            color = FDColors.Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FDBotonPrimario(
                texto = if (enviando) "Transmitiendo..." else "Enviar comprobante",
                onClick = onConfirmar,
                cargando = enviando,
                icono = Icons.Default.CloudUpload
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                enabled = !enviando,
                shape = FDShapes.Medium
            ) {
                Text("Cancelar", style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
        },
        containerColor = FDColors.Surface,
        shape = FDShapes.Large
    )
}

@Composable
private fun DialogoConfirmarEnvioLote(
    totalPendientes: Int,
    enviando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!enviando) onCancelar() },
        title = {
            Text(
                text = "Enviar $totalPendientes comprobantes en cola",
                style = FDType.Heading2.copy(fontWeight = FontWeight.Bold),
                color = FDColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                HorizontalDivider(color = FDColors.Border)
                Text(
                    text = "Se procesará la transmisión secuencial a SUNAT de los comprobantes pendientes.",
                    style = FDType.Body.copy(fontWeight = FontWeight.SemiBold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "• NO modifica las ventas registradas en el POS.\n• NO repite numeración fiscal.\n• Los comprobantes con falla de red permanecerán en cola para reintento automático.",
                    style = FDType.BodySmall,
                    color = FDColors.TextSecondary
                )
            }
        },
        confirmButton = {
            FDBotonPrimario(
                texto = if (enviando) "Procesando lote..." else "Iniciar envío",
                onClick = onConfirmar,
                cargando = enviando,
                icono = Icons.Default.CloudUpload
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                enabled = !enviando,
                shape = FDShapes.Medium
            ) {
                Text("Cancelar", style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
        },
        containerColor = FDColors.Surface,
        shape = FDShapes.Large
    )
}

@Composable
private fun DialogoResultadoLote(
    reporte: ReporteEnvioLote,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (reporte.requierenAtencion.isEmpty()) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (reporte.requierenAtencion.isEmpty()) FDColors.Success else FDColors.Warning
                )
                Text(
                    text = "Resultado del envío a SUNAT",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                HorizontalDivider(color = FDColors.Border)

                // Resumen superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Surface(
                        color = FDColors.Success.copy(alpha = 0.12f),
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Success),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(FDSpacing.sm)) {
                            Text("✓ Aceptados", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.Success)
                            Text("${reporte.exitosos.size}", style = FDType.Heading1.copy(fontWeight = FontWeight.Black), color = FDColors.Success)
                        }
                    }

                    if (reporte.requierenAtencion.isNotEmpty()) {
                        Surface(
                            color = FDColors.Warning.copy(alpha = 0.12f),
                            shape = FDShapes.Medium,
                            border = BorderStroke(1.dp, FDColors.Warning),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(FDSpacing.sm)) {
                                Text("⚠ Requieren atención", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.Warning)
                                Text("${reporte.requierenAtencion.size}", style = FDType.Heading1.copy(fontWeight = FontWeight.Black), color = FDColors.Warning)
                            }
                        }
                    }
                }

                // Lista detallada
                Text("DETALLE POR COMPROBANTE:", style = FDType.Label.copy(fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                ) {
                    items(reporte.exitosos) { item ->
                        Surface(
                            color = FDColors.Background,
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
                                    Text(item.numeroCompleto, style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Text("Aceptado por SUNAT", style = FDType.Caption, color = FDColors.Success)
                            }
                        }
                    }

                    items(reporte.requierenAtencion) { item ->
                        val esErrorRed = item.estado == "FALLA_RED"
                        val colorItem = if (esErrorRed) FDColors.Warning else FDColors.Error
                        Surface(
                            color = colorItem.copy(alpha = 0.08f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, colorItem.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (esErrorRed) Icons.Default.HourglassEmpty else Icons.Default.WarningAmber,
                                            null,
                                            tint = colorItem,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(item.numeroCompleto, style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Text(
                                        text = if (esErrorRed) "En cola (reintento auto)" else "Requiere atención",
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                                        color = colorItem
                                    )
                                }
                                Text(
                                    text = item.motivo,
                                    style = FDType.Caption.copy(fontSize = 11.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FDBotonPrimario(
                texto = "Entendido",
                onClick = onCerrar
            )
        },
        containerColor = FDColors.Surface,
        shape = FDShapes.Large
    )
}
