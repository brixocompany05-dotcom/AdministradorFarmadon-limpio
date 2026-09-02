package com.app.administradorfarmadon.ventas.cierrecaja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.ventas.cierrecaja.logica.CierreCajaUiState
import com.app.administradorfarmadon.ventas.cierrecaja.logica.CierreCajaViewModel
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA DE CONTROL Y CIERRE DE CAJA (R1/R3/R8/R12).
 * UI de Productividad Enterprise para Tablet / Pantalla Grande.
 */
@Composable
fun SubmoduloCierreCaja(
    simboloMoneda: String = "S/",
    viewModel: CierreCajaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var modoConteoBilletes by remember { mutableStateOf(true) }
    var observacionesTexto by remember { mutableStateOf("") }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 3.dp)
            }
        } else if (!uiState.estaAbierta) {
            // ───────────────────────────── ESTADO: CAJA CERRADA ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                POSEmptyState(
                    icono = Icons.Default.Lock,
                    titulo = "Caja Cerrada",
                    subtitulo = "La caja actual se encuentra cerrada. Realiza la apertura con el fondo inicial para comenzar a operar."
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    FDBotonPrimario(
                        texto = "REALIZAR APERTURA DE CAJA",
                        onClick = { viewModel.abrirDialogoApertura() },
                        icono = Icons.Default.VpnKey,
                        habilitado = !uiState.procesandoAccion
                    )
                }
            }
        } else {
            // ───────────────────────────── ESTADO: CAJA ABIERTA ─────────────────────────────

            // FILA SUPERIOR: DASHBOARD DE CIERRE (3 COLUMNAS)
            Row(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // COLUMNA A: Resumen en Vivo del Puntero de Caja
                POSSurfacePanel(
                    titulo = "Resumen del Turno",
                    modifier = Modifier.weight(1f)
                ) {
                    val p = uiState.estadoCaja
                    val ventasEfectivo = p.ventasPorMetodo["EFECTIVO"] ?: 0.0

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemResumen("Fondo Inicial", formatearMoneda(simboloMoneda, p.fondoInicial))
                        ItemResumen("Ventas Efectivo (+)", formatearMoneda(simboloMoneda, ventasEfectivo))
                        ItemResumen("Ingresos (+)", formatearMoneda(simboloMoneda, p.ingresos))
                        ItemResumen("Retiros (-)", formatearMoneda(simboloMoneda, p.retiros))
                        ItemResumen("Devoluciones Efectivo (-)", formatearMoneda(simboloMoneda, p.devolucionesEfectivo))
                        ItemResumen("Ventas Registradas", "${p.cantidadVentas} oper.")

                        Spacer(Modifier.weight(1f))
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ESPERADO EN CAJÓN",
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FDColors.Primary
                                    )
                                )
                                Text(
                                    "Efectivo físico en gaveta",
                                    style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                                )
                            }
                            Text(
                                formatearMoneda(simboloMoneda, p.efectivoEsperado),
                                style = FDType.Numeric.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.Primary
                                )
                            )
                        }
                    }
                }

                // COLUMNA B: Conteo Físico por Denominación
                POSSurfacePanel(
                    titulo = "Conteo Físico",
                    modifier = Modifier.weight(1.3f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FDColors.InputBackground, FDShapes.Small)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabMini("Billetes", modoConteoBilletes, modifier = Modifier.weight(1f)) {
                                modoConteoBilletes = true
                            }
                            TabMini("Monedas", !modoConteoBilletes, modifier = Modifier.weight(1f)) {
                                modoConteoBilletes = false
                            }
                        }

                        // Lista de Denominaciones
                        val denoms = if (modoConteoBilletes) CierreCajaViewModel.DENOMINACIONES_BILLETES else CierreCajaViewModel.DENOMINACIONES_MONEDAS
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(denoms) { denom ->
                                val cant = uiState.conteoDenominaciones[denom] ?: 0
                                POSDenominationCounter(
                                    denominacion = denom,
                                    cantidad = cant,
                                    onCantidadChange = { nuevaCant ->
                                        viewModel.actualizarDenominacion(denom, nuevaCant)
                                    },
                                    simbolo = simboloMoneda,
                                    esMoneda = !modoConteoBilletes
                                )
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        // Totales del Conteo y Diferencia
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL CONTADO",
                                style = FDType.Label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.TextPrimary
                                )
                            )
                            Text(
                                formatearMoneda(simboloMoneda, uiState.totalContado),
                                style = FDType.Numeric.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.TextPrimary
                                )
                            )
                        }

                        val dif = uiState.diferenciaEfectivo
                        val (colorDif, textoDif) = when {
                            kotlin.math.abs(dif) < 0.01 -> FDColors.Success to "CUADRADA"
                            dif < 0 -> FDColors.Error to "FALTANTE"
                            else -> FDColors.Warning to "SOBRANTE"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "DIFERENCIA",
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colorDif
                                    )
                                )
                                POSBadge(texto = textoDif, tipo = if (kotlin.math.abs(dif) < 0.01) TipoEstadoFarmadon.EXITO else TipoEstadoFarmadon.PELIGRO)
                            }
                            Text(
                                formatearMoneda(simboloMoneda, dif),
                                style = FDType.Numeric.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorDif
                                )
                            )
                        }
                    }
                }

                // COLUMNA C: Resumen por Métodos de Pago
                POSSurfacePanel(
                    titulo = "Ventas por Método",
                    modifier = Modifier.weight(1.1f)
                ) {
                    val ventasMap = uiState.estadoCaja.ventasPorMetodo
                    val totalVentasTodas = ventasMap.values.sum()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Método", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                            Text("Total Cobrado", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (ventasMap.isEmpty()) {
                                item {
                                    Text(
                                        "No hay ventas registradas aún",
                                        style = FDType.Caption,
                                        color = FDColors.TextTertiary,
                                        modifier = Modifier.padding(vertical = 12.dp)
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(nombreMetodo, style = FDType.Body.copy(fontSize = 13.sp), color = FDColors.TextPrimary)
                                        Text(
                                            formatearMoneda(simboloMoneda, total),
                                            style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL RECAUDADO",
                                style = FDType.Label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.TextSecondary
                                )
                            )
                            Text(
                                formatearMoneda(simboloMoneda, totalVentasTodas),
                                style = FDType.Numeric.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FDColors.TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // FILA INFERIOR: MOVIMIENTOS Y ACCIONES DE CIERRE
            Row(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Movimientos de Caja en Vivo
                POSSurfacePanel(
                    titulo = "Movimientos del Turno",
                    modifier = Modifier.weight(0.65f)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.movimientos.size} movimientos registrados",
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = FDColors.TextTertiary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.abrirDialogoMovimiento(MovimientoCaja.TIPO_INGRESO) },
                                    shape = FDShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = FDColors.Success)
                                    Spacer(Modifier.width(4.dp))
                                    Text("+ Ingreso", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.Success)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.abrirDialogoMovimiento(MovimientoCaja.TIPO_RETIRO) },
                                    shape = FDShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = FDColors.Error)
                                    Spacer(Modifier.width(4.dp))
                                    Text("- Retiro", style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.Error)
                                }
                            }
                        }

                        if (uiState.movimientos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                POSEmptyState(
                                    icono = Icons.AutoMirrored.Filled.CompareArrows,
                                    titulo = "Sin movimientos",
                                    subtitulo = "Las ventas, ingresos y retiros de este turno aparecerán aquí automáticamente."
                                )
                            }
                        } else {
                            // Cabecera de la tabla
                            Surface(
                                color = FDColors.InputBackground.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("HORA", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.8f))
                                    Text("TIPO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f))
                                    Text("MOTIVO / REF", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(2f))
                                    Text("MONTO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.movimientos) { mov ->
                                    val (colorTipo, tipoBadge) = when (mov.tipo) {
                                        MovimientoCaja.TIPO_VENTA -> FDColors.Primary to TipoEstadoFarmadon.NEUTRO
                                        MovimientoCaja.TIPO_INGRESO -> FDColors.Success to TipoEstadoFarmadon.EXITO
                                        MovimientoCaja.TIPO_RETIRO -> FDColors.Error to TipoEstadoFarmadon.PELIGRO
                                        MovimientoCaja.TIPO_DEVOLUCION -> FDColors.Warning to TipoEstadoFarmadon.ALERTA
                                        else -> FDColors.TextSecondary to TipoEstadoFarmadon.NEUTRO
                                    }

                                    val horaTexto = if (mov.fechaMs > 0L) fmtHora.format(Date(mov.fechaMs)) else "--:--"

                                    Surface(
                                        color = FDColors.Surface,
                                        shape = FDShapes.Small,
                                        border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                horaTexto,
                                                style = FDType.Caption.copy(fontSize = 11.sp),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.weight(0.8f)
                                            )
                                            Box(modifier = Modifier.weight(1f)) {
                                                POSBadge(texto = mov.tipo, tipo = tipoBadge)
                                            }
                                            Text(
                                                mov.motivo.ifBlank { mov.referenciaNumero.ifBlank { "Operación de caja" } },
                                                style = FDType.Body.copy(fontSize = 12.sp),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(2f)
                                            )
                                            Text(
                                                formatearMoneda(simboloMoneda, mov.monto),
                                                style = FDType.Numeric.copy(
                                                    fontSize = 13.sp,
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

                // Observaciones y Cierre Definitivo
                POSSurfacePanel(
                    titulo = "Cierre Definitivo",
                    modifier = Modifier.weight(0.35f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FDTextField(
                            value = observacionesTexto,
                            onValueChange = { observacionesTexto = it },
                            label = "OBSERVACIONES DEL TURNO",
                            placeholder = "Escriba notas relevantes del cierre (opcional)...",
                            singleLine = false,
                            minLines = 3,
                            modifier = Modifier.weight(1f)
                        )

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
                                CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CERRAR TURNO DE CAJA",
                                    style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ───────────────────────────── DIÁLOGOS ─────────────────────────────

    // 1. Diálogo de Apertura de Caja
    if (uiState.mostrarDialogoApertura) {
        DialogoApertura(
            simboloMoneda = simboloMoneda,
            procesando = uiState.procesandoAccion,
            error = uiState.error,
            onDismiss = { viewModel.cerrarDialogoApertura() },
            onConfirmar = { fondo -> viewModel.abrirCaja(fondo) }
        )
    }

    // 2. Diálogo de Movimiento Manual (Ingreso / Retiro)
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

    // 3. Diálogo de Confirmación de Cierre
    if (uiState.mostrarDialogoConfirmarCierre) {
        DialogoConfirmarCierre(
            simboloMoneda = simboloMoneda,
            esperado = uiState.estadoCaja.efectivoEsperado,
            contado = uiState.totalContado,
            diferencia = uiState.diferenciaEfectivo,
            observaciones = observacionesTexto,
            procesando = uiState.procesandoAccion,
            error = uiState.error,
            onDismiss = { viewModel.cerrarDialogoConfirmarCierre() },
            onConfirmar = { viewModel.cerrarCaja(observacionesTexto) }
        )
    }

    // 4. Diálogo de Resumen de Turno Cerrado
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

// ───────────────────────────── COMPONENTES AUXILIARES Y DIÁLOGOS ─────────────────────────────

@Composable
private fun ItemResumen(label: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FDType.Caption.copy(fontSize = 12.sp), color = FDColors.TextSecondary)
        Text(valor, style = FDType.Numeric.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
    }
}

@Composable
private fun TabMini(label: String, activo: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (activo) FDColors.Primary else Color.Transparent,
        shape = FDShapes.Small,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = FDType.Label.copy(fontSize = 12.sp, fontWeight = if (activo) FontWeight.Black else FontWeight.Bold),
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
    val fondoValido = fondoTexto.toDoubleOrNull()?.let { it >= 0.0 } == true

    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(420.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Apertura de Caja",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 20.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    "Ingresa el fondo inicial en efectivo con el que se inicia la atención en mostrador.",
                    style = FDType.Body,
                    color = FDColors.TextSecondary
                )

                if (error != null) {
                    Text(error, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold))
                }

                FDTextField(
                    value = fondoTexto,
                    onValueChange = { fondoTexto = it },
                    label = "FONDO INICIAL ($simboloMoneda)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = FDShapes.Small
                    ) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val fondo = fondoTexto.toDoubleOrNull() ?: 0.0
                            onConfirmar(fondo)
                        },
                        enabled = !procesando && fondoValido,
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("ABRIR CAJA", style = FDType.Label.copy(fontWeight = FontWeight.Black))
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

    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(440.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    titulo,
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                    color = if (esIngreso) FDColors.Success else FDColors.Error
                )

                if (error != null) {
                    Text(error, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold))
                }

                FDTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it },
                    label = "MONTO ($simboloMoneda)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                FDTextField(
                    value = motivoTexto,
                    onValueChange = { motivoTexto = it },
                    label = "MOTIVO (MÍNIMO 4 CARACTERES)",
                    placeholder = if (esIngreso) "Ej: Sencillo adicional para caja" else "Ej: Pago a repartidor / Pago de servicios",
                    singleLine = false,
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = FDShapes.Small
                    ) {
                        Text("Cancelar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val monto = montoTexto.toDoubleOrNull() ?: 0.0
                            onConfirmar(monto, motivoTexto)
                        },
                        enabled = !procesando && (montoTexto.toDoubleOrNull() ?: 0.0) > 0.0 && motivoTexto.trim().length >= 4,
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = if (esIngreso) FDColors.Success else FDColors.Error)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("GUARDAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
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
    observaciones: String,
    procesando: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    Dialog(onDismissRequest = { if (!procesando) onDismiss() }) {
        Surface(
            shape = FDShapes.Medium,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Confirmar Cierre de Caja",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 20.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    "Revisa los montos finales antes de cerrar definitivamente el turno de atención:",
                    style = FDType.Body,
                    color = FDColors.TextSecondary
                )

                if (error != null) {
                    Text(error, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold))
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Esperado:", style = FDType.Body, color = FDColors.TextSecondary)
                            Text(formatearMoneda(simboloMoneda, esperado), style = FDType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Contado:", style = FDType.Body, color = FDColors.TextSecondary)
                            Text(formatearMoneda(simboloMoneda, contado), style = FDType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                        val colorDif = if (kotlin.math.abs(diferencia) < 0.01) FDColors.Success else FDColors.Error
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diferencia:", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = colorDif)
                            Text(formatearMoneda(simboloMoneda, diferencia), style = FDType.Numeric.copy(fontWeight = FontWeight.Black, color = colorDif))
                        }
                    }
                }

                if (observaciones.isNotBlank()) {
                    Text("Nota: \"$observaciones\"", style = FDType.Caption.copy(color = FDColors.TextTertiary))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = FDShapes.Small
                    ) {
                        Text("Revisar", color = FDColors.TextSecondary)
                    }
                    Button(
                        onClick = onConfirmar,
                        enabled = !procesando,
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = FDShapes.Small,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("CONFIRMAR CIERRE", style = FDType.Label.copy(fontWeight = FontWeight.Black))
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
            modifier = Modifier.width(460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(28.dp))
                    Text(
                        "Turno Cerrado Exitosamente",
                        style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                        color = FDColors.TextPrimary
                    )
                }

                Surface(
                    color = FDColors.InputBackground,
                    shape = FDShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cajero:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(sesion.cerradoPorNombre, style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fecha y Hora:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(sesion.cierreLegible, style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Esperado:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(formatearMoneda(simboloMoneda, sesion.efectivoEsperado), style = FDType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Contado:", style = FDType.Caption, color = FDColors.TextSecondary)
                            Text(formatearMoneda(simboloMoneda, sesion.efectivoContado), style = FDType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diferencia:", style = FDType.Caption.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                            Text(formatearMoneda(simboloMoneda, sesion.diferenciaEfectivo), style = FDType.Numeric.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }

                Button(
                    onClick = onAceptar,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = FDShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    Text("ACEPTAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}

private fun formatearMoneda(simbolo: String, monto: Double): String {
    return String.format(Locale.US, "%s %.2f", simbolo, monto)
}
