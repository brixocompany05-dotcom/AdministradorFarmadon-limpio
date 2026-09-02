package com.app.administradorfarmadon.clientes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.clientes.logica.ClientesUiState
import com.app.administradorfarmadon.clientes.logica.ClientesViewModel
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA PRINCIPAL DEL DIRECTORIO DE CLIENTES (R1/R3/R8/R12).
 * Arquitectura Enterprise: Lista de Clientes (55%) + Ficha & Historial de Compras (45%).
 */
@Composable
fun ClientesScreen(
    onVolver: () -> Unit = {},
    viewModel: ClientesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    var mostrarConfirmarEliminar by remember { mutableStateOf<ClienteFarmacia?>(null) }

    // Auto-limpieza de mensajes temporales
    LaunchedEffect(uiState.mensajeExito, uiState.error) {
        if (uiState.mensajeExito != null || uiState.error != null) {
            delay(4000)
            viewModel.limpiarMensajes()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── CABECERA PRINCIPAL ──
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Medium,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FDColors.TextPrimary)
                    }
                    Column {
                        Text("Directorio de Clientes", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp))
                        Text("Fichas de clientes y registro de compras unificado de la farmacia", style = FDType.Caption, color = FDColors.TextSecondary)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FDSearchField(
                        busqueda = uiState.filtroTexto,
                        onBusquedaChange = { viewModel.setFiltroTexto(it) },
                        placeholder = "Buscar por nombre, DNI, RUC o teléfono...",
                        modifier = Modifier.width(340.dp)
                    )

                    Button(
                        onClick = { viewModel.abrirDialogoCrear() },
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                        shape = FDShapes.Small,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("NUEVO CLIENTE", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }

        // Notificaciones en vivo
        if (uiState.mensajeExito != null) {
            POSNotificationBar(
                mensaje = uiState.mensajeExito ?: "",
                tipo = TipoEstadoFarmadon.EXITO,
                icono = Icons.Default.CheckCircle
            )
        }
        if (uiState.error != null) {
            POSNotificationBar(
                mensaje = uiState.error ?: "",
                tipo = TipoEstadoFarmadon.PELIGRO,
                icono = Icons.Default.ErrorOutline
            )
        }

        // ── SPLIT VIEW: DIRECTORIO (55%) | FICHA & HISTORIAL (45%) ──
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ───────────────────────────── PANEL IZQUIERDO: TABLA DE CLIENTES (55%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Clientes Registrados (${uiState.clientesFiltrados.size})",
                modifier = Modifier.weight(0.55f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.cargando && uiState.clientes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                        }
                    } else if (uiState.clientesFiltrados.isEmpty()) {
                        POSEmptyState(
                            icono = Icons.Default.PeopleOutline,
                            titulo = if (uiState.clientes.isEmpty()) "Directorio Vacío" else "Sin coincidencias",
                            subtitulo = if (uiState.clientes.isEmpty()) "Registra tu primer cliente usando el botón 'NUEVO CLIENTE'." else "Prueba con otro término de búsqueda."
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Encabezados
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("DOCUMENTO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                    Text("NOMBRE / RAZÓN SOCIAL", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.8f))
                                    Text("TELÉFONO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(uiState.clientesFiltrados) { cliente ->
                                    val isSel = uiState.clienteSeleccionado?.id == cliente.id

                                    Surface(
                                        onClick = { viewModel.seleccionarCliente(cliente) },
                                        color = if (isSel) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
                                        shape = FDShapes.Small,
                                        border = BorderStroke(1.dp, if (isSel) FDColors.Primary else FDColors.Border.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                POSBadge(
                                                    texto = cliente.tipoDocumento,
                                                    tipo = if (cliente.esDni) TipoEstadoFarmadon.EXITO else TipoEstadoFarmadon.ALERTA
                                                )
                                                Text(
                                                    cliente.numeroDocumento,
                                                    style = FDType.Numeric.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                    color = FDColors.TextPrimary
                                                )
                                            }

                                            Text(
                                                cliente.nombre,
                                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1.8f)
                                            )

                                            Text(
                                                cliente.telefono.ifBlank { "---" },
                                                style = FDType.Caption.copy(fontSize = 11.5.sp),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ───────────────────────────── PANEL DERECHO: FICHA & HISTORIAL (45%) ─────────────────────────────
            POSSurfacePanel(
                titulo = "Ficha del Cliente e Historial",
                modifier = Modifier.weight(0.45f)
            ) {
                val cliente = uiState.clienteSeleccionado
                if (cliente == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ContactPage,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = FDColors.TextTertiary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Selecciona un Cliente", style = FDType.Heading3.copy(fontWeight = FontWeight.Black), color = FDColors.TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("Elige un cliente de la lista para ver su ficha completa, datos de contacto e historial de compras.", style = FDType.Body, color = FDColors.TextTertiary, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tarjeta Principal de Ficha
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cliente.nombre, style = FDType.Heading3.copy(fontWeight = FontWeight.Black, fontSize = 16.sp), color = FDColors.TextPrimary)
                                        Text("${cliente.tipoDocumento}: ${cliente.numeroDocumento}", style = FDType.Caption.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { viewModel.abrirDialogoEditar(cliente) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, "Editar", tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { mostrarConfirmarEliminar = cliente }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, "Eliminar", tint = FDColors.Error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                if (cliente.telefono.isNotBlank() || cliente.direccion.isNotBlank()) {
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                                    if (cliente.telefono.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                                            Text(cliente.telefono, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                                        }
                                    }
                                    if (cliente.direccion.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                                            Text(cliente.direccion, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Métricas del Cliente
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = FDColors.Surface,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TOTAL COMPRADO", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                    Text("$simboloMoneda ${String.format(Locale.US, "%.2f", uiState.totalComprasCliente)}", style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, color = FDColors.Primary))
                                }
                            }

                            Surface(
                                color = FDColors.Surface,
                                shape = FDShapes.Small,
                                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("COMPROBANTES", style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                                    Text("${uiState.totalOperacionesCliente} compras", style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black))
                                }
                            }
                        }

                        // Historial de Compras en Vivo
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("HISTORIAL DE COMPRAS EN ESTA SUCURSAL", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)

                            if (uiState.cargandoHistorial) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp)
                                }
                            } else if (uiState.historialVentas.isEmpty()) {
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.3f),
                                    shape = FDShapes.Small,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                        Text("Este cliente aún no registra compras en esta sucursal.", style = FDType.Caption, color = FDColors.TextTertiary, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                val fmtFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.historialVentas) { venta ->
                                        val fecha = if (venta.fechaHoraMs > 0) fmtFecha.format(Date(venta.fechaHoraMs)) else "--"
                                        val (tipoBadge, textoBadge) = when (venta.estado) {
                                            Venta.ESTADO_COMPLETADA -> TipoEstadoFarmadon.EXITO to "COMPLETADA"
                                            Venta.ESTADO_DEVOLUCION_PARCIAL -> TipoEstadoFarmadon.ALERTA to "DEV. PARCIAL"
                                            Venta.ESTADO_DEVOLUCION_TOTAL -> TipoEstadoFarmadon.PELIGRO to "DEV. TOTAL"
                                            else -> TipoEstadoFarmadon.NEUTRO to venta.estado
                                        }

                                        Surface(
                                            color = FDColors.Surface,
                                            shape = FDShapes.Small,
                                            border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(venta.numeroCompleto, style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = FDColors.TextPrimary)
                                                    Text(fecha, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        "$simboloMoneda ${String.format(Locale.US, "%.2f", venta.total)}",
                                                        style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                                                    )
                                                    POSBadge(texto = textoBadge, tipo = tipoBadge)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── DIÁLOGO CREAR / EDITAR CLIENTE ──
        if (uiState.mostrarDialogoCrearEditar) {
            DialogoCrearEditarCliente(
                clienteInicial = uiState.clienteEnEdicion,
                consultando = uiState.consultandoDoc,
                onDismiss = { viewModel.cerrarDialogoCrearEditar() },
                onConsultar = { tipo, num, onRes -> viewModel.consultarDocumentoOficial(tipo, num, onRes) },
                onGuardar = { viewModel.guardarCliente(it) }
            )
        }

        // ── DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN ──
        mostrarConfirmarEliminar?.let { c ->
            Dialog(onDismissRequest = { mostrarConfirmarEliminar = null }) {
                Surface(
                    shape = FDShapes.Medium,
                    color = FDColors.Surface,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.width(400.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Eliminar Cliente", style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp), color = FDColors.Error)
                        Text(
                            "¿Estás seguro de eliminar al cliente '${c.nombre}' (${c.tipoDocumento} ${c.numeroDocumento}) del directorio? El historial de compras ya emitidas no se borrará.",
                            style = FDType.Body,
                            color = FDColors.TextSecondary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { mostrarConfirmarEliminar = null }, modifier = Modifier.weight(1f).height(42.dp), shape = FDShapes.Small) {
                                Text("Cancelar", color = FDColors.TextSecondary)
                            }
                            Button(
                                onClick = {
                                    viewModel.eliminarCliente(c.id)
                                    mostrarConfirmarEliminar = null
                                },
                                modifier = Modifier.weight(1.2f).height(42.dp),
                                shape = FDShapes.Small,
                                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error)
                            ) {
                                Text("ELIMINAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── DIÁLOGO CREAR / EDITAR CLIENTE ─────────────────────────────

@Composable
private fun DialogoCrearEditarCliente(
    clienteInicial: ClienteFarmacia?,
    consultando: Boolean,
    onDismiss: () -> Unit,
    onConsultar: (tipo: String, num: String, onRes: (nombre: String, direccion: String) -> Unit) -> Unit,
    onGuardar: (ClienteFarmacia) -> Unit
) {
    val esEdicion = clienteInicial != null
    var tipoDoc by remember { mutableStateOf(clienteInicial?.tipoDocumento ?: "DNI") }
    var numDoc by remember { mutableStateOf(clienteInicial?.numeroDocumento ?: "") }
    var nombre by remember { mutableStateOf(clienteInicial?.nombre ?: "") }
    var telefono by remember { mutableStateOf(clienteInicial?.telefono ?: "") }
    var direccion by remember { mutableStateOf(clienteInicial?.direccion ?: "") }
    var notas by remember { mutableStateOf(clienteInicial?.notas ?: "") }

    val docValido = if (tipoDoc == "DNI") Regex("^\\d{8}$").matches(numDoc.trim()) else Regex("^\\d{11}$").matches(numDoc.trim())
    val guardarHabilitado = docValido && nombre.trim().isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (esEdicion) "Editar Ficha de Cliente" else "Nuevo Cliente",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp)
                )

                // Selector Tipo de Documento
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DNI", "RUC").forEach { t ->
                        val sel = tipoDoc == t
                        Surface(
                            onClick = { if (!esEdicion) tipoDoc = t },
                            color = if (sel) FDColors.Primary else FDColors.InputBackground,
                            shape = FDShapes.Small,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    t,
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (sel) FontWeight.Black else FontWeight.Bold
                                    ),
                                    color = if (sel) FDColors.PrimaryText else FDColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Número de Documento + Botón de Consulta Oficial RENIEC/SUNAT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FDTextField(
                        value = numDoc,
                        onValueChange = { if (!esEdicion) numDoc = it },
                        label = "N° DE $tipoDoc *",
                        placeholder = if (tipoDoc == "DNI") "8 dígitos" else "11 dígitos",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            onConsultar(tipoDoc, numDoc) { nom, dir ->
                                nombre = nom
                                if (dir.isNotBlank()) direccion = dir
                            }
                        },
                        enabled = !consultando && numDoc.trim().isNotBlank(),
                        shape = FDShapes.Small,
                        modifier = Modifier.padding(top = 18.dp).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        if (consultando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RENIEC/SUNAT", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                FDTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = if (tipoDoc == "RUC") "RAZÓN SOCIAL *" else "NOMBRES Y APELLIDOS *",
                    placeholder = "Nombre completo del cliente"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FDTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = "TELÉFONO (OPCIONAL)",
                        placeholder = "987 654 321",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )

                    FDTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = "DIRECCIÓN (OPCIONAL)",
                        placeholder = "Av. Principal 123",
                        modifier = Modifier.weight(1.3f)
                    )
                }

                FDTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = "NOTAS (OPCIONAL)",
                    placeholder = "Alergias, preferencias o referencias",
                    singleLine = false,
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp), shape = FDShapes.Small) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val c = ClienteFarmacia(
                                id = numDoc.trim(),
                                tipoDocumento = tipoDoc,
                                numeroDocumento = numDoc.trim(),
                                nombre = nombre.trim(),
                                telefono = telefono.trim(),
                                direccion = direccion.trim(),
                                notas = notas.trim(),
                                fechaMs = clienteInicial?.fechaMs ?: 0L
                            )
                            onGuardar(c)
                        },
                        enabled = guardarHabilitado,
                        modifier = Modifier.weight(1.3f).height(44.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text("GUARDAR CLIENTE", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}
