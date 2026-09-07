package com.app.administradorfarmadon.ventas.cierrecaja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.cierrecaja.logica.CierreCajaUiState
import com.app.administradorfarmadon.ventas.cierrecaja.logica.CierreCajaViewModel
import com.app.administradorfarmadon.ventas.compartido.logica.MontoFormateador
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * SECCIONES DEL MASTER-DETAIL DE CIERRE DE CAJA
 */
private enum class SeccionCierreCaja(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector
) {
    CONTEO_FISICO(
        "Conteo Físico de Arqueo",
        "Billetes y monedas en gaveta",
        Icons.Default.Payments
    ),
    VENTAS_METODO("Ventas por Método", "Efectivo, Yape, Plin, Tarjetas", Icons.Default.PointOfSale),
    MOVIMIENTOS(
        "Movimientos del Turno",
        "Ingresos y retiros manuales",
        Icons.AutoMirrored.Filled.CompareArrows
    ),
    RESUMEN_CIERRE(
        "Resumen y Cierre Definitivo",
        "Liquidación final y cierre de turno",
        Icons.Default.Lock
    )
}

/**
 * PANTALLA DE CONTROL Y CIERRE DE CAJA (R1/R3/R8/R12).
 * UI de Productividad Enterprise SaaS para Tablet - Patrón Master-Detail.
 */
@Composable
fun SubmoduloCierreCaja(
    simboloMoneda: String = "S/",
    viewModel: CierreCajaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var modoConteoBilletes by remember { mutableStateOf(true) }
    var observacionesTexto by remember { mutableStateOf("") }
    var seccionSeleccionada by remember { mutableStateOf(SeccionCierreCaja.CONTEO_FISICO) }

    // Auto-limpieza de notificaciones tras 4 segundos
    LaunchedEffect(uiState.mensajeExito, uiState.error) {
        if (uiState.mensajeExito != null || uiState.error != null) {
            delay(4000)
            viewModel.limpiarMensajes()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Notificaciones en vivo de éxito o error
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

        if (uiState.cargando && !uiState.estaAbierta) {
            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 3.dp)
            }
        } else if (!uiState.estaAbierta) {
            // ───────────────────────────── ESTADO: CAJA CERRADA ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                color = FDColors.ErrorSubtle,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FDColors.Error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Caja Cerrada",
                                    style = FDType.Heading2.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.TextPrimary
                                )
                                Text(
                                    "Para iniciar las ventas del día, registra la apertura con el fondo inicial en gaveta.",
                                    style = FDType.Body.copy(
                                        fontSize = 12.5.sp,
                                        color = FDColors.TextSecondary
                                    )
                                )
                            }
                        }

                        FDBotonPrimario(
                            texto = "REALIZAR APERTURA DE CAJA",
                            onClick = { viewModel.abrirDialogoApertura() },
                            icono = Icons.Default.VpnKey,
                            habilitado = !uiState.procesandoAccion
                        )
                    }
                }

                // Historial de auditoría cuando la caja está cerrada
                TablaHistorialCajaSesiones(
                    sesiones = uiState.historialSesiones,
                    simboloMoneda = simboloMoneda,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // ───────────────────────────── ESTADO: CAJA ABIERTA (MASTER-DETAIL 30% / 70%) ─────────────────────────────
            val esTurnoVencido = uiState.estadoCaja.esTurnoVencido
            if (esTurnoVencido) {
                Surface(
                    color = FDColors.WarningSubtle,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.LockClock,
                            null,
                            tint = FDColors.Warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "BLOQUEO OPERATIVO · TURNO VENCIDO (${uiState.estadoCaja.fechaAperturaLegible()})",
                                style = FDType.Body.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                ),
                                color = FDColors.Warning
                            )
                            Text(
                                text = "Este turno pertenece a una jornada anterior y está vencido. Las ventas, el carrito y los movimientos manuales están bloqueados. Realice el conteo físico de gaveta para asentar el arqueo real y confirmar el cierre definitivo.",
                                style = FDType.Caption.copy(fontSize = 11.5.sp),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ───────────────────────────── PANEL IZQUIERDO (30%): MENÚ NAVEGABLE ─────────────────────────────
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PointOfSale,
                                null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Control de Caja",
                                style = FDType.Heading3.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.TextPrimary
                            )
                        }

                        Text(
                            text = "Selecciona una sección para auditar o cerrar el turno:",
                            style = FDType.Caption.copy(fontSize = 11.5.sp),
                            color = FDColors.TextSecondary
                        )

                        HorizontalDivider(
                            color = FDColors.Border.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )

                        SeccionCierreCaja.entries.forEach { seccion ->
                            val seleccionada = seccionSeleccionada == seccion
                            Surface(
                                onClick = { seccionSeleccionada = seccion },
                                color = if (seleccionada) FDColors.Primary.copy(alpha = 0.12f) else FDColors.InputBackground,
                                shape = FDShapes.Medium,
                                border = BorderStroke(
                                    if (seleccionada) 1.5.dp else 1.dp,
                                    if (seleccionada) FDColors.Primary else FDColors.Border.copy(
                                        alpha = 0.3f
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        seccion.icono,
                                        contentDescription = null,
                                        tint = if (seleccionada) FDColors.Primary else FDColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = seccion.titulo,
                                            style = FDType.Body.copy(
                                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = if (seleccionada) FDColors.Primary else FDColors.TextPrimary
                                        )
                                        Text(
                                            text = seccion.descripcion,
                                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                                            color = FDColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (seleccionada) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Footer informativo
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "INFORMACIÓN DEL TURNO",
                                    style = FDType.Label.copy(
                                        fontSize = 10.sp,
                                        color = FDColors.TextTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "Cajero: ${uiState.estadoCaja.abiertoPorNombre.ifBlank { "Personal activo" }}",
                                    style = FDType.Caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Apertura: ${uiState.estadoCaja.fechaAperturaLegible()}",
                                    style = FDType.Caption.copy(fontSize = 11.sp)
                                )
                            }
                        }

                        // Botón de Cierre Rápido siempre visible en la parte inferior del menú izquierdo
                        Button(
                            onClick = {
                                seccionSeleccionada = SeccionCierreCaja.RESUMEN_CIERRE
                                viewModel.abrirDialogoConfirmarCierre()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = FDShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                            enabled = !uiState.procesandoAccion
                        ) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = FDColors.PrimaryText)
                            Spacer(Modifier.width(6.dp))
                            Text("CERRAR TURNO DE CAJA", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = FDColors.PrimaryText))
                        }
                    }
                }

                // ───────────────────────────── PANEL DERECHO (70%): ÁREA ESPACIOSA DEDICADA ─────────────────────────────
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.70f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)) {
                        when (seccionSeleccionada) {
                            SeccionCierreCaja.CONTEO_FISICO -> DetalleConteoFisico(
                                uiState,
                                modoConteoBilletes,
                                simboloMoneda,
                                viewModel,
                                onCambiarModo = { modoConteoBilletes = it })

                            SeccionCierreCaja.VENTAS_METODO -> DetalleVentasMetodo(
                                uiState,
                                simboloMoneda
                            )

                            SeccionCierreCaja.MOVIMIENTOS -> DetalleMovimientosTurno(
                                uiState,
                                simboloMoneda,
                                viewModel
                            )

                            SeccionCierreCaja.RESUMEN_CIERRE -> DetalleResumenYCierre(
                                uiState,
                                simboloMoneda,
                                observacionesTexto,
                                viewModel,
                                onObservacionesChange = { observacionesTexto = it })
                        }
                    }
                }
            }
        }
    }

    // ───────────────────────────── DIÁLOGOS COMPACTOS ─────────────────────────────

    if (uiState.mostrarDialogoApertura) {
        DialogoApertura(
            simboloMoneda = simboloMoneda,
            procesando = uiState.procesandoAccion,
            error = uiState.error,
            onDismiss = { viewModel.cerrarDialogoApertura() },
            onConfirmar = { fondo -> viewModel.abrirCaja(fondo) }
        )
    }

    if (uiState.mostrarDialogoMovimiento) {
        DialogoMovimientoManual(
            tipo = uiState.tipoMovimientoManual,
            simboloMoneda = simboloMoneda,
            procesando = uiState.procesandoAccion,
            error = uiState.error,
            onDismiss = { viewModel.cerrarDialogoMovimiento() },
            onConfirmar = { monto, motivo -> viewModel.registrarMovimientoManual(monto, motivo) }
        )
    }

    if (uiState.mostrarDialogoConfirmarCierre) {
        DialogoConfirmarCierre(
            simboloMoneda = simboloMoneda,
            esperado = uiState.estadoCaja.efectivoEsperado,
            contado = uiState.totalContado,
            diferencia = uiState.diferenciaEfectivo,
            esCierreCiego = uiState.esCierreCiego,
            observaciones = observacionesTexto,
            procesando = uiState.procesandoAccion,
            error = uiState.error,
            ventasEnPausa = uiState.ventasEnPausa,
            onDismiss = { viewModel.cerrarDialogoConfirmarCierre() },
            onConfirmar = { viewModel.cerrarCaja(observacionesTexto) }
        )
    }

    uiState.sesionCerradaResultado?.let { sesionCerrada ->
        DialogoResultadoCierre(
            sesion = sesionCerrada,
            simboloMoneda = simboloMoneda,
            onAceptar = {
                viewModel.descartarResultadoCierre()
                observacionesTexto = ""
            }
        )
    }
}

// ───────────────────────────── VISTAS DE DETALLE DEDICADAS (PANEL DERECHO) ─────────────────────────────

@Composable
private fun DetalleConteoFisico(
    uiState: CierreCajaUiState,
    modoConteoBilletes: Boolean,
    simboloMoneda: String,
    viewModel: CierreCajaViewModel,
    onCambiarModo: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Conteo Físico de Arqueo",
                    style = FDType.Heading2.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    "Ingresa la cantidad física de billetes y monedas en gaveta",
                    style = FDType.Caption.copy(fontSize = 11.5.sp),
                    color = FDColors.TextSecondary
                )
            }

            // Tabs Billetes / Monedas
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .background(FDColors.InputBackground, FDShapes.Small)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabMini(
                    "Billetes",
                    modoConteoBilletes,
                    modifier = Modifier.weight(1f)
                ) { onCambiarModo(true) }
                TabMini(
                    "Monedas",
                    !modoConteoBilletes,
                    modifier = Modifier.weight(1f)
                ) { onCambiarModo(false) }
            }
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Lista de Denominaciones amplia y cómoda
        val denoms =
            if (modoConteoBilletes) CierreCajaViewModel.DENOMINACIONES_BILLETES else CierreCajaViewModel.DENOMINACIONES_MONEDAS
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(denoms) { denom ->
                val cant = uiState.conteoDenominaciones[denom] ?: 0
                POSDenominationCounter(
                    denominacion = denom,
                    cantidad = cant,
                    onCantidadChange = { nuevaCant ->
                        viewModel.actualizarDenominacion(
                            denom,
                            nuevaCant
                        )
                    },
                    simbolo = simboloMoneda,
                    esMoneda = !modoConteoBilletes
                )
            }
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Barra de Totales
        Surface(
            color = FDColors.InputBackground,
            shape = FDShapes.Medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "TOTAL CONTADO EN GAVETA",
                        style = FDType.Label.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = FDColors.TextSecondary
                        )
                    )
                    Text(
                        formatearMoneda(simboloMoneda, uiState.totalContado),
                        style = FDType.Numeric.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = FDColors.TextPrimary
                        )
                    )
                }

                val dif = uiState.diferenciaEfectivo
                val (colorDif, textoDif) = when {
                    abs(dif) < 0.01 -> FDColors.Success to "CUADRADA"
                    dif < 0 -> FDColors.Error to "FALTANTE"
                    else -> FDColors.Warning to "SOBRANTE"
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "DIFERENCIA",
                            style = FDType.Label.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorDif
                            )
                        )
                        POSBadge(
                            texto = textoDif,
                            tipo = if (abs(dif) < 0.01) TipoEstadoFarmadon.EXITO else TipoEstadoFarmadon.PELIGRO
                        )
                    }
                    Text(
                        formatearMoneda(simboloMoneda, dif),
                        style = FDType.Numeric.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = colorDif
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetalleVentasMetodo(
    uiState: CierreCajaUiState,
    simboloMoneda: String
) {
    val ventasMap = uiState.estadoCaja.ventasPorMetodo
    val totalVentasTodas = ventasMap.values.sum()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                "Ventas por Método de Pago",
                style = FDType.Heading2.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextPrimary
            )
            Text(
                "Desglose de recaudación según forma de cobro en mostrador",
                style = FDType.Caption.copy(fontSize = 11.5.sp),
                color = FDColors.TextSecondary
            )
        }

        Surface(
            color = FDColors.InputBackground.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "MÉTODO DE PAGO",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextTertiary
                )
                Text(
                    "TOTAL COBRADO",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextTertiary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ventasMap.isEmpty()) {
                item {
                    POSEmptyState(
                        icono = Icons.Default.PointOfSale,
                        titulo = "Sin ventas registradas",
                        subtitulo = "Las ventas cobradas por efectivo, Yape, Plin o tarjetas aparecerán aquí."
                    )
                }
            } else {
                items(ventasMap.entries.toList()) { (metodo, total) ->
                    val nombreMetodo = when (metodo) {
                        "EFECTIVO" -> "Efectivo"
                        "YAPE" -> "Yape"
                        "PLIN" -> "Plin"
                        "TARJETA_POS" -> "Tarjeta POS"
                        "TRANSFERENCIA" -> "Transferencia"
                        "CHEQUE" -> "Cheque"
                        else -> metodo
                    }
                    Surface(
                        color = FDColors.Surface,
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                nombreMetodo,
                                style = FDType.Body.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                formatearMoneda(simboloMoneda, total),
                                style = FDType.Numeric.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

        Surface(
            color = FDColors.InputBackground,
            shape = FDShapes.Medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TOTAL RECAUDADO",
                    style = FDType.Label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = FDColors.TextSecondary
                    )
                )
                Text(
                    formatearMoneda(simboloMoneda, totalVentasTodas),
                    style = FDType.Numeric.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = FDColors.Primary
                    )
                )
            }
        }
    }
}

@Composable
private fun DetalleMovimientosTurno(
    uiState: CierreCajaUiState,
    simboloMoneda: String,
    viewModel: CierreCajaViewModel
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Movimientos del Turno",
                    style = FDType.Heading2.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    "${uiState.movimientos.size} operaciones de caja registradas",
                    style = FDType.Caption.copy(fontSize = 11.5.sp),
                    color = FDColors.TextSecondary
                )
            }

            val esTurnoVencido = uiState.estadoCaja.esTurnoVencido
            if (esTurnoVencido) {
                Surface(
                    color = FDColors.WarningSubtle,
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LockClock,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = FDColors.Warning
                        )
                        Text(
                            "Turno vencido: No se permiten ingresos ni retiros manuales. Solo lectura y cierre.",
                            style = FDType.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FDColors.Warning
                            )
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.abrirDialogoMovimiento(MovimientoCaja.TIPO_INGRESO) },
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Success,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "+ Registrar Ingreso",
                            style = FDType.Label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Button(
                        onClick = { viewModel.abrirDialogoMovimiento(MovimientoCaja.TIPO_RETIRO) },
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Error,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            Icons.Default.RemoveCircle,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "- Registrar Retiro",
                            style = FDType.Label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (uiState.movimientos.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f), contentAlignment = Alignment.Center) {
                POSEmptyState(
                    icono = Icons.AutoMirrored.Filled.CompareArrows,
                    titulo = "Sin movimientos registrados",
                    subtitulo = "Las ventas, ingresos y retiros de este turno aparecerán aquí en vivo."
                )
            }
        } else {
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "HORA",
                        style = FDType.Label.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        "TIPO",
                        style = FDType.Label.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "MOTIVO / REFERENCIA",
                        style = FDType.Label.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        "MONTO",
                        style = FDType.Label.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.movimientos) { mov ->
                    val tipoBadge = when (mov.tipo) {
                        MovimientoCaja.TIPO_VENTA -> TipoEstadoFarmadon.NEUTRO
                        MovimientoCaja.TIPO_INGRESO -> TipoEstadoFarmadon.EXITO
                        MovimientoCaja.TIPO_RETIRO -> TipoEstadoFarmadon.PELIGRO
                        MovimientoCaja.TIPO_DEVOLUCION -> TipoEstadoFarmadon.ALERTA
                        else -> TipoEstadoFarmadon.NEUTRO
                    }

                    val horaTexto =
                        if (mov.fechaMs > 0L) fmtHora.format(Date(mov.fechaMs)) else "--:--"

                    Surface(
                        color = FDColors.Surface,
                        shape = FDShapes.Small,
                        border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                horaTexto,
                                style = FDType.Caption.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(0.8f)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                POSBadge(
                                    texto = mov.tipo,
                                    tipo = tipoBadge
                                )
                            }
                            Text(
                                mov.motivo.ifBlank { mov.referenciaNumero.ifBlank { "Operación de caja" } },
                                style = FDType.Body.copy(fontSize = 12.5.sp),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                formatearMoneda(simboloMoneda, mov.monto),
                                style = FDType.Numeric.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mov.monto >= 0) FDColors.TextPrimary else FDColors.Error
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalleResumenYCierre(
    uiState: CierreCajaUiState,
    simboloMoneda: String,
    observacionesTexto: String,
    viewModel: CierreCajaViewModel,
    onObservacionesChange: (String) -> Unit
) {
    val p = uiState.estadoCaja
    val ventasEfectivoNeto = p.ventasPorMetodo["EFECTIVO"] ?: 0.0
    val ventasEfectivoBruto = round((ventasEfectivoNeto + p.devolucionesEfectivo) * 100.0) / 100.0

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Resumen Financiero y Cierre Definitivo",
            style = FDType.Heading2.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextPrimary
        )

        Surface(
            color = FDColors.InputBackground.copy(alpha = 0.4f),
            shape = FDShapes.Medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemResumen("Cajero a cargo", p.abiertoPorNombre.ifBlank { "Personal activo" })
                ItemResumen("Apertura", p.fechaAperturaLegible())
                ItemResumen("Fondo Inicial", formatearMoneda(simboloMoneda, p.fondoInicial))
                ItemResumen(
                    "Ventas Efectivo bruto (+)",
                    formatearMoneda(simboloMoneda, ventasEfectivoBruto)
                )
                ItemResumen("Ingresos manuales (+)", formatearMoneda(simboloMoneda, p.ingresos))
                ItemResumen("Retiros (−)", formatearMoneda(simboloMoneda, p.retiros))
                ItemResumen(
                    "Devoluciones Efectivo (−)",
                    formatearMoneda(simboloMoneda, p.devolucionesEfectivo)
                )
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                if (uiState.esCierreCiego) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "ESPERADO EN CAJÓN",
                                style = FDType.Label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.Primary
                                )
                            )
                            Text(
                                "Modalidad Entrega Ciega Activa",
                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                            )
                        }
                        Text(
                            "*** (Ciego)",
                            style = FDType.Numeric.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = FDColors.TextSecondary
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ESPERADO EN CAJÓN",
                            style = FDType.Label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = FDColors.Primary
                            )
                        )
                        Text(
                            formatearMoneda(simboloMoneda, p.efectivoEsperado),
                            style = FDType.Numeric.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = FDColors.Primary
                            )
                        )
                    }
                }
            }
        }

        FDTextField(
            value = observacionesTexto,
            onValueChange = onObservacionesChange,
            label = "OBSERVACIONES Y NOTAS DEL TURNO",
            placeholder = "Escriba notas relevantes del cierre (opcional)...",
            singleLine = false,
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.abrirDialogoConfirmarCierre() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = FDShapes.Small,
            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
            enabled = !uiState.procesandoAccion
        ) {
            if (uiState.procesandoAccion) {
                CircularProgressIndicator(
                    color = FDColors.PrimaryText,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "CERRAR TURNO DE CAJA DEFINITIVAMENTE",
                    style = FDType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Black)
                )
            }
        }
    }
}

// ───────────────────────────── COMPONENTES AUXILIARES Y DIÁLOGOS ─────────────────────────────

@Composable
private fun ItemResumen(label: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FDType.Caption.copy(fontSize = 12.sp), color = FDColors.TextSecondary)
        Text(
            valor,
            style = FDType.Numeric.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextPrimary
        )
    }
}

@Composable
private fun TabMini(
    label: String,
    activo: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (activo) FDColors.Primary else Color.Transparent,
        shape = FDShapes.Small,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = FDType.Label.copy(
                    fontSize = 12.sp,
                    fontWeight = if (activo) FontWeight.Black else FontWeight.Bold
                ),
                color = if (activo) FDColors.PrimaryText else FDColors.TextTertiary
            )
        }
    }
}

@Composable
private fun DialogoApertura(
    simboloMoneda: String,
    procesando: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirmar: (Double) -> Unit
) {
    var fondoTexto by remember { mutableStateOf("0.00") }
    val fondoParsed = MontoFormateador.normalizarMontoPositivo(fondoTexto)
    val fondoValido = fondoParsed != null

    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(380.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.VpnKey,
                        null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Apertura de Caja",
                        style = FDType.Heading2.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                }

                Text(
                    "Ingresa el fondo inicial en efectivo con el que se inicia la atención en mostrador.",
                    style = FDType.Body.copy(fontSize = 12.5.sp),
                    color = FDColors.TextSecondary
                )

                if (error != null) {
                    Text(
                        error,
                        style = FDType.Caption.copy(
                            color = FDColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                FDTextField(
                    value = fondoTexto,
                    onValueChange = { fondoTexto = it },
                    label = "FONDO INICIAL ($simboloMoneda)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = FDShapes.Small
                    ) {
                        Text(
                            "Cancelar",
                            color = FDColors.TextSecondary,
                            style = FDType.Label.copy(fontSize = 12.sp)
                        )
                    }
                    Button(
                        onClick = {
                            val fondo = MontoFormateador.normalizarMontoPositivo(fondoTexto) ?: 0.0
                            onConfirmar(fondo)
                        },
                        enabled = !procesando && fondoValido,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                color = FDColors.PrimaryText,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "ABRIR CAJA",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoMovimientoManual(
    tipo: String,
    simboloMoneda: String,
    procesando: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirmar: (Double, String) -> Unit
) {
    var montoTexto by remember { mutableStateOf("") }
    var motivoTexto by remember { mutableStateOf("") }
    val esIngreso = tipo == MovimientoCaja.TIPO_INGRESO
    val titulo = if (esIngreso) "Registrar Ingreso de Efectivo" else "Registrar Retiro de Efectivo"
    val montoParsed = MontoFormateador.normalizarMontoEstricto(montoTexto)
    val montoValido = montoParsed != null

    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (esIngreso) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = if (esIngreso) FDColors.Success else FDColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        titulo,
                        style = FDType.Heading2.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                }

                if (error != null) {
                    Text(
                        error,
                        style = FDType.Caption.copy(
                            color = FDColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                FDTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it },
                    label = "MONTO ($simboloMoneda)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                FDTextField(
                    value = motivoTexto,
                    onValueChange = { motivoTexto = it },
                    label = "MOTIVO (MÍNIMO 4 CARACTERES)",
                    placeholder = if (esIngreso) "Ej: Sencillo adicional para caja" else "Ej: Pago de servicios / Suministros",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = FDShapes.Small
                    ) {
                        Text(
                            "Cancelar",
                            color = FDColors.TextSecondary,
                            style = FDType.Label.copy(fontSize = 12.sp)
                        )
                    }
                    Button(
                        onClick = {
                            val monto = MontoFormateador.normalizarMontoEstricto(montoTexto) ?: 0.0
                            onConfirmar(monto, motivoTexto)
                        },
                        enabled = !procesando && montoValido && motivoTexto.trim().length >= 4,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esIngreso) FDColors.Success else FDColors.Error,
                            contentColor = Color.White
                        )
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (esIngreso) "+ GUARDAR INGRESO" else "- GUARDAR RETIRO",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoConfirmarCierre(
    simboloMoneda: String,
    esperado: Double,
    contado: Double,
    diferencia: Double,
    esCierreCiego: Boolean = false,
    observaciones: String,
    procesando: Boolean,
    error: String?,
    ventasEnPausa: Int = 0,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = FDColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Confirmar Cierre de Caja",
                        style = FDType.Heading2.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                }

                Text(
                    if (esCierreCiego) "Confirma el conteo físico de gaveta para liquidar el turno a ciegas:"
                    else "Revisa los montos finales antes de cerrar definitivamente el turno de atención:",
                    style = FDType.Body.copy(fontSize = 12.sp),
                    color = FDColors.TextSecondary
                )

                if (error != null) {
                    Text(
                        error,
                        style = FDType.Caption.copy(
                            color = FDColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!esCierreCiego) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Efectivo Esperado:",
                                    style = FDType.Body.copy(fontSize = 12.5.sp),
                                    color = FDColors.TextSecondary
                                )
                                Text(
                                    formatearMoneda(simboloMoneda, esperado),
                                    style = FDType.Numeric.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Efectivo Contado:",
                                style = FDType.Body.copy(fontSize = 12.5.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                formatearMoneda(simboloMoneda, contado),
                                style = FDType.Numeric.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        if (!esCierreCiego) {
                            HorizontalDivider(
                                color = FDColors.Border.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )

                            val colorDif =
                                if (abs(diferencia) < 0.01) FDColors.Success else FDColors.Error
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Diferencia:",
                                    style = FDType.Body.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = colorDif
                                )
                                Text(
                                    formatearMoneda(simboloMoneda, diferencia),
                                    style = FDType.Numeric.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = colorDif
                                    )
                                )
                            }
                        } else {
                            Text(
                                "Modalidad Entrega Ciega: la diferencia teórica se revelará en el reporte al cerrar.",
                                style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.TextTertiary)
                            )
                        }
                    }
                }

                if (observaciones.isNotBlank()) {
                    Text(
                        "Nota: \"$observaciones\"",
                        style = FDType.Caption.copy(color = FDColors.TextTertiary)
                    )
                }

                if (ventasEnPausa > 0) {
                    Surface(
                        color = FDColors.Warning.copy(alpha = 0.1f),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Tienes $ventasEnPausa venta${if (ventasEnPausa == 1) "" else "s"} en pausa. Resuélvela${if (ventasEnPausa == 1) "" else "s"} en Nueva Venta antes de cerrar.",
                            style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.Warning,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = FDShapes.Small
                    ) {
                        Text(
                            "Revisar",
                            color = FDColors.TextSecondary,
                            style = FDType.Label.copy(fontSize = 12.sp)
                        )
                    }
                    Button(
                        onClick = onConfirmar,
                        enabled = !procesando && ventasEnPausa == 0,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                color = FDColors.PrimaryText,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "CONFIRMAR CIERRE",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoResultadoCierre(
    sesion: CajaSesion,
    simboloMoneda: String,
    onAceptar: () -> Unit
) {
    Dialog(onDismissRequest = onAceptar) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = FDColors.Success,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Turno Cerrado Exitosamente",
                        style = FDType.Heading2.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Cajero:",
                                style = FDType.Caption.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                sesion.cerradoPorNombre,
                                style = FDType.Body.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Fecha y Hora:",
                                style = FDType.Caption.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                sesion.cierreLegible,
                                style = FDType.Body.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Efectivo Esperado:",
                                style = FDType.Caption.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                formatearMoneda(simboloMoneda, sesion.efectivoEsperado),
                                style = FDType.Numeric.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Efectivo Contado:",
                                style = FDType.Caption.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                            Text(
                                formatearMoneda(simboloMoneda, sesion.efectivoContado),
                                style = FDType.Numeric.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                        HorizontalDivider(
                            color = FDColors.Border.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Diferencia:",
                                style = FDType.Caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                formatearMoneda(simboloMoneda, sesion.diferenciaEfectivo),
                                style = FDType.Numeric.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = onAceptar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = FDShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    Text(
                        "ACEPTAR",
                        style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

private fun formatearMoneda(simbolo: String, monto: Double): String {
    return String.format(Locale.US, "%s %.2f", simbolo, monto)
}
