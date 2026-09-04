package com.app.administradorfarmadon.configuracion.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDCampoTexto
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.configuracion.pos.logica.PosConfigViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PosConfigScreen(
    onBack: () -> Unit,
    viewModel: PosConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val isWide = maxWidth >= 880.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FDSpacing.xl, vertical = FDSpacing.lg)
        ) {
            // Header Enterprise con botón de retorno y selector de sucursal
            HeaderPosConfig(
                sucursales = uiState.sucursales,
                sucursalSeleccionadaId = uiState.sucursalSeleccionadaId,
                onSeleccionarSucursal = { viewModel.seleccionarSucursal(it) },
                onBack = onBack
            )

            Spacer(Modifier.height(FDSpacing.md))

            // Banners de éxito / error si están presentes
            AnimatedVisibility(visible = uiState.mensajeExito != null) {
                uiState.mensajeExito?.let {
                    BannerMensaje(
                        mensaje = it,
                        esError = false,
                        onCerrar = { viewModel.limpiarMensajes() }
                    )
                }
            }
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let {
                    BannerMensaje(
                        mensaje = it,
                        esError = true,
                        onCerrar = { viewModel.limpiarMensajes() }
                    )
                }
            }

            Spacer(Modifier.height(FDSpacing.sm))

            if (uiState.cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FDColors.Primary)
                }
            } else {
                if (isWide) {
                    // Distribución Tablet Enterprise: 60% Formulario continuo, 40% Resumen en vivo
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl)
                    ) {
                        // Columna Izquierda: Formulario Continuo (60%)
                        Column(
                            modifier = Modifier
                                .weight(0.62f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(end = FDSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                        ) {
                            SeccionDescuentos(uiState, viewModel)
                            SeccionCaja(uiState, viewModel)
                            SeccionTicketYReceta(uiState, viewModel)
                            Spacer(Modifier.height(FDSpacing.xxl))
                        }

                        // Columna Derecha: Panel Ejecutivo de Liquidación y Guardado (40%)
                        Column(
                            modifier = Modifier
                                .weight(0.38f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                        ) {
                            PanelResumenEjecutivo(
                                uiState = uiState,
                                onGuardar = { viewModel.guardarConfiguracion() },
                                onAplicarATodas = { viewModel.abrirDialogoAplicarTodas() }
                            )
                        }
                    }
                } else {
                    // Vista vertical para pantallas más compactas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                    ) {
                        SeccionDescuentos(uiState, viewModel)
                        SeccionCaja(uiState, viewModel)
                        SeccionTicketYReceta(uiState, viewModel)
                        PanelResumenEjecutivo(
                            uiState = uiState,
                            onGuardar = { viewModel.guardarConfiguracion() },
                            onAplicarATodas = { viewModel.abrirDialogoAplicarTodas() }
                        )
                        Spacer(Modifier.height(FDSpacing.xxl))
                    }
                }

                if (uiState.mostrarDialogoAplicarTodas) {
                    AlertDialog(
                        onDismissRequest = { viewModel.cerrarDialogoAplicarTodas() },
                        title = { Text("¿Aplicar reglas a todas las sedes?", style = FDType.Heading2.copy(fontWeight = FontWeight.Black)) },
                        text = {
                            Text(
                                "Estas políticas de descuentos, supervisión, retiros de caja y tickets se guardarán como VIGENTES en todas las sedes de la farmacia. ¿Deseas continuar?",
                                style = FDType.Body,
                                color = FDColors.TextSecondary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.aplicarATodasLasSedes() },
                                shape = FDShapes.Small,
                                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                            ) {
                                Text("APLICAR A TODAS", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { viewModel.cerrarDialogoAplicarTodas() }, shape = FDShapes.Small) {
                                Text("Cancelar", color = FDColors.TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderPosConfig(
    sucursales: List<com.app.administradorfarmadon.configuracion.metodospago.datos.SucursalCatalogo>,
    sucursalSeleccionadaId: String,
    onSeleccionarSucursal: (String) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = FDColors.Surface,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FDColors.TextPrimary)
                }
            }

            Column {
                Text(
                    "Reglas de Venta y Caja (POS)",
                    style = FDType.Heading2.copy(fontFamily = InterPremium, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
                Text(
                    "Límites de descuentos, topes de efectivo y autorización por rol",
                    style = FDType.Caption.copy(fontFamily = InterPremium),
                    color = FDColors.TextSecondary
                )
            }
        }

        // Selector de Sede si la farmacia tiene múltiples sucursales
        if (sucursales.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                Text("Sede:", style = FDType.Label, color = FDColors.TextSecondary)
                sucursales.forEach { sede ->
                    val esSel = sede.id == sucursalSeleccionadaId
                    Surface(
                        onClick = { onSeleccionarSucursal(sede.id) },
                        shape = FDShapes.Small,
                        color = if (esSel) FDColors.Primary else FDColors.Surface,
                        border = BorderStroke(1.dp, if (esSel) FDColors.Primary else FDColors.Border)
                    ) {
                        Text(
                            text = sede.nombre,
                            style = FDType.Label.copy(
                                fontWeight = if (esSel) FontWeight.Black else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (esSel) FDColors.PrimaryText else FDColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.xs)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionDescuentos(
    state: com.app.administradorfarmadon.configuracion.pos.logica.PosConfigUiState,
    viewModel: PosConfigViewModel
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            TituloSeccion(
                icono = Icons.Default.Percent,
                titulo = "DESCUENTOS EN MOSTRADOR",
                descripcion = "Topes comerciales para cajeros y condición para exigir visto bueno de jefatura."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                FDCampoTexto(
                    valor = state.maxPctStr,
                    onValorCambio = { viewModel.onMaxPctChange(it) },
                    etiqueta = "Descuento máximo (%)",
                    placeholder = "10",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textoAyuda = "Tope porcentual por venta"
                )

                FDCampoTexto(
                    valor = state.maxMontoStr,
                    onValorCambio = { viewModel.onMaxMontoChange(it) },
                    etiqueta = "Monto máximo (S/)",
                    placeholder = "50.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    textoAyuda = "Tope en soles por venta"
                )
            }
        }
    }
}

@Composable
private fun SeccionCaja(
    state: com.app.administradorfarmadon.configuracion.pos.logica.PosConfigUiState,
    viewModel: PosConfigViewModel
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            TituloSeccion(
                icono = Icons.Default.PointOfSale,
                titulo = "CONTROL DE EFECTIVO Y ARQUEO",
                descripcion = "Límites en caja física y modalidades de cierre de turno."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                FDCampoTexto(
                    valor = state.retiroMaxStr,
                    onValorCambio = { viewModel.onRetiroMaxChange(it) },
                    etiqueta = "Retiro máximo individual (S/)",
                    placeholder = "500.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    textoAyuda = "Tope por cada salida manual"
                )

                FDCampoTexto(
                    valor = state.vueltoMaxStr,
                    onValorCambio = { viewModel.onVueltoMaxChange(it) },
                    etiqueta = "Vuelto máximo permitido (S/)",
                    placeholder = "200.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    textoAyuda = "Tope de cambio en efectivo"
                )
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

            // Switch Entrega Ciega Turno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Entrega a ciegas al cerrar turno (Arqueo ciego)", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                    Text("El cajero declara su conteo físico sin conocer el saldo teórico calculado.", style = FDType.Caption, color = FDColors.TextSecondary)
                }
                Switch(
                    checked = state.entregaCiegaTurno,
                    onCheckedChange = { viewModel.onEntregaCiegaTurnoChange(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = FDColors.Primary, checkedTrackColor = FDColors.Primary.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
private fun SeccionTicketYReceta(
    state: com.app.administradorfarmadon.configuracion.pos.logica.PosConfigUiState,
    viewModel: PosConfigViewModel
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            TituloSeccion(
                icono = Icons.Default.ReceiptLong,
                titulo = "COMPROBANTES Y RECETAS MÉDICAS",
                descripcion = "Configuración de impresión del ticket y seguridad farmacológica."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                FDCampoTexto(
                    valor = state.copiasTicketStr,
                    onValorCambio = { viewModel.onCopiasTicketChange(it) },
                    etiqueta = "Número de copias ticket",
                    placeholder = "1",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(160.dp),
                    textoAyuda = "Entre 1 y 5 copias"
                )

                FDCampoTexto(
                    valor = state.pieTicket,
                    onValorCambio = { viewModel.onPieTicketChange(it) },
                    etiqueta = "Mensaje al pie del ticket",
                    placeholder = "Gracias por su compra",
                    modifier = Modifier.weight(1f),
                    textoAyuda = "Aparece impreso al final del comprobante"
                )
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Exigir confirmación obligatoria para recetas", style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                    Text("Bloquea el botón de cobrar si el carrito tiene medicamentos bajo receta sin validar.", style = FDType.Caption, color = FDColors.TextSecondary)
                }
                Switch(
                    checked = state.exigirConfirmacionReceta,
                    onCheckedChange = { viewModel.onExigirConfirmacionRecetaChange(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = FDColors.Primary, checkedTrackColor = FDColors.Primary.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
private fun PanelResumenEjecutivo(
    uiState: com.app.administradorfarmadon.configuracion.pos.logica.PosConfigUiState,
    onGuardar: () -> Unit,
    onAplicarATodas: (() -> Unit)? = null
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, FDColors.BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = FDColors.Primary, modifier = Modifier.size(22.dp))
                Text("RESUMEN DE REGLAS ACTIVAS", style = FDType.Label.copy(fontWeight = FontWeight.Black))
            }

            Surface(
                color = FDColors.InputBackground,
                shape = FDShapes.Medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilaResumen("Sede aplicada:", uiState.sucursalSeleccionadaNombre)
                    FilaResumen("Tope descuento máx:", "${uiState.maxPctStr}% / S/ ${uiState.maxMontoStr}")
                    FilaResumen("Retiro máx efectivo:", "S/ ${uiState.retiroMaxStr}")
                    FilaResumen("Vuelto máx efectivo:", "S/ ${uiState.vueltoMaxStr}")
                    FilaResumen("Arqueo ciego:", if (uiState.entregaCiegaTurno) "ACTIVADO" else "DESACTIVADO")
                    FilaResumen("Control de receta:", if (uiState.exigirConfirmacionReceta) "OBLIGATORIO" else "OPCIONAL")
                }
            }

            if (uiState.existeEnServidor && !uiState.esBorrador) {
                Surface(
                    color = FDColors.Success.copy(alpha = 0.08f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FDColors.Success, modifier = Modifier.size(18.dp))
                        Column {
                            val fecha = if (uiState.actualizadoEnMs > 0L) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(uiState.actualizadoEnMs)) else "Reciente"
                            Text("REGLAS VIGENTES EN NUBE", style = FDType.Caption.copy(fontWeight = FontWeight.Black), color = FDColors.Success)
                            Text("Guardado por ${uiState.actualizadoPorNombre} ($fecha)", style = FDType.Caption, color = FDColors.TextSecondary)
                        }
                    }
                }
            } else {
                Surface(
                    color = FDColors.Warning.copy(alpha = 0.1f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("BORRADOR PENDIENTE DE ALTA", style = FDType.Caption.copy(fontWeight = FontWeight.Black), color = FDColors.Warning)
                            Text(
                                "Esta sede no tiene reglas guardadas en la nube. Revisa los límites y presiona 'GUARDAR REGLAS SEDE' para autorizar el cobro en mostrador.",
                                style = FDType.Caption,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(FDSpacing.sm))

            FDBotonPrimario(
                texto = if (uiState.guardando) "GUARDANDO..." else "GUARDAR REGLAS SEDE",
                icono = Icons.Default.Save,
                onClick = onGuardar,
                habilitado = !uiState.guardando && !uiState.aplicandoATodas,
                modifier = Modifier.fillMaxWidth()
            )

            if (onAplicarATodas != null && uiState.sucursales.size > 1) {
                OutlinedButton(
                    onClick = onAplicarATodas,
                    enabled = !uiState.guardando && !uiState.aplicandoATodas,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (uiState.aplicandoATodas) "APLICANDO..." else "APLICAR A TODAS LAS SEDES",
                        style = FDType.Label.copy(fontWeight = FontWeight.Bold),
                        color = FDColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(icono: ImageVector, titulo: String, descripcion: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FDColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(titulo, style = FDType.Heading3.copy(fontWeight = FontWeight.Black, fontSize = 14.sp), color = FDColors.TextPrimary)
            Text(descripcion, style = FDType.Caption, color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, style = FDType.Caption, color = FDColors.TextSecondary)
        Text(valor, style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
    }
}

@Composable
private fun BannerMensaje(mensaje: String, esError: Boolean, onCerrar: () -> Unit) {
    val fondo = if (esError) FDColors.Error.copy(alpha = 0.12f) else FDColors.Success.copy(alpha = 0.12f)
    val colorTexto = if (esError) FDColors.Error else FDColors.Success
    val icono = if (esError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline

    Surface(
        color = fondo,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, colorTexto.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(20.dp))
                Text(mensaje, style = FDType.Body.copy(fontSize = 13.sp, color = colorTexto))
            }
            IconButton(onClick = onCerrar, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = colorTexto, modifier = Modifier.size(16.dp))
            }
        }
    }
}
