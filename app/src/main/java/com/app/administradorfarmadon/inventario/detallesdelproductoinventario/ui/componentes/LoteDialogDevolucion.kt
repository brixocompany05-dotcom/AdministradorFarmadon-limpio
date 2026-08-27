package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

@Composable
internal fun DialogoDevolucionDrogueria(
    product: MoldeProductos,
    lote: LoteProducto,
    onDismiss: () -> Unit,
    onConfirmDevolucion: (cantidad: Double, guiaRetiro: String, notaCredito: String, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onConfirmCanje: (cantidad: Double, nuevoLote: String, nuevoVencimiento: String, guiaCanje: String, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    val totalDisponible = lote.cantidad
    val costoUnitario = remember(lote) { calcularCostoUnitario(lote) }

    var form by remember {
        mutableStateOf(DevolucionFormState(cantidadStr = totalDisponible.toInt().toString()))
    }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val cantNum = calcularCantidad(form, totalDisponible)
    val totalReclamo = cantNum * costoUnitario
    val formularioValido = esCanjeValido(form, cantNum, totalDisponible) ||
        esDevolucionValida(form, cantNum, totalDisponible)

    EnterpriseModalShell(
        title = "Devolución — Lote ${lote.numero}",
        icon = Icons.AutoMirrored.Outlined.AssignmentReturn,
        iconColor = FDColors.Primary,
        onDismiss = onDismiss,
        isProcesando = isProcesando,
        mensajeError = mensajeError,
        leftContent = {
            // NUEVO: Underline Tabs (pro) + formulario continuo 52dp, sin cajas anidadas
            var tabCanje by remember { mutableStateOf(form.esCanjesFisico) }
            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = if (tabCanje) 0 else 1,
                modifier = Modifier.height(48.dp),
                containerColor = FDColors.SurfaceElevated,
                contentColor = FDColors.Primary,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (tabCanje) 0 else 1]),
                        color = FDColors.Primary,
                        height = 2.dp
                    )
                }
            ) {
                androidx.compose.material3.Tab(
                    selected = tabCanje,
                    onClick = { tabCanje = true; form = form.copy(modalidadIdx = 0, motivoSeleccionado = "") },
                    text = { Text("Canje", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = if (tabCanje) FontWeight.Bold else FontWeight.Medium, color = if (tabCanje) FDColors.Primary else FDColors.TextSecondary)) }
                )
                androidx.compose.material3.Tab(
                    selected = !tabCanje,
                    onClick = { tabCanje = false; form = form.copy(modalidadIdx = 1, motivoSeleccionado = "") },
                    text = { Text("Nota de crédito", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = if (!tabCanje) FontWeight.Bold else FontWeight.Medium, color = if (!tabCanje) FDColors.Primary else FDColors.TextSecondary)) }
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            // Cantidad 52dp + guía 52dp en 2 columnas (tablet)
            SeccionDatosEntrada(
                form = form,
                totalDisponible = totalDisponible,
                empaque = product.empaque,
                cantNum = cantNum,
                loteOriginal = lote,
                enabled = !isProcesando,
                onCambio = { ns: DevolucionFormState -> form = ns; tabCanje = ns.esCanjesFisico }
            )
            SeccionMotivo(
                form = form,
                enabled = !isProcesando,
                onCambio = { ns: DevolucionFormState -> form = ns }
            )
        },
        rightContent = {
            PanelResumenDerecho(
                lote = lote,
                product = product,
                form = form,
                cantNum = cantNum,
                costoUnitario = costoUnitario,
                totalReclamo = totalReclamo,
                isProcesando = isProcesando,
                formularioValido = formularioValido,
                onConfirmar = {
                    if (isProcesando) return@PanelResumenDerecho
                    isProcesando = true
                    mensajeError = null
                    if (form.esCanjesFisico) {
                        onConfirmCanje(
                            cantNum,
                            loteEfectivo(form, lote.numero),
                            vencimientoEfectivo(form, lote.vencimiento),
                            form.guiaCanjeStr,
                            form.motivoSeleccionado
                        ) { result ->
                            isProcesando = false
                            if (result.isFailure) {
                                mensajeError = result.exceptionOrNull()?.message ?: "Error al procesar el canje."
                            }
                        }
                    } else {
                        onConfirmDevolucion(
                            cantNum,
                            form.guiaRetiroStr,
                            form.notaCreditoStr,
                            form.motivoSeleccionado
                        ) { result ->
                            isProcesando = false
                            if (result.isFailure) {
                                mensajeError = result.exceptionOrNull()?.message ?: "Error al procesar la devolución."
                            }
                        }
                    }
                }
            )
        },
        confirmButton = null,
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcesando,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.height(46.dp)
            ) {
                Text("CANCELAR", style = FDType.Label.copy(color = FDColors.TextSecondary, fontWeight = FontWeight.SemiBold))
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SECCIONES DEL FORMULARIO — CADA UNA CON UNA SOLA RESPONSABILIDAD
// ─────────────────────────────────────────────────────────────────────────────

/** Sección 1: Modalidad de resolución */
@Composable
private fun SeccionModalidad(
    form: DevolucionFormState,
    enabled: Boolean,
    onCambio: (DevolucionFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SeccionLabel("¿Cómo lo resuelves?")
        EnterpriseSegmentedControl(
            options = listOf("CANJE POR PRODUCTO", "NOTA DE CRÉDITO"),
            selectedIndex = form.modalidadIdx,
            onSelect = { idx ->
                val motivoInicial = ""
                onCambio(form.copy(modalidadIdx = idx, motivoSeleccionado = motivoInicial))
            },
            enabled = enabled,
            activeColor = FDColors.Primary
        )
    }
}

/** Sección 2: Cantidad y Datos de Resolución (Formulario Directo) */
@Composable
private fun SeccionDatosEntrada(
    form: DevolucionFormState,
    totalDisponible: Double,
    empaque: String,
    cantNum: Double,
    loteOriginal: LoteProducto,
    enabled: Boolean,
    onCambio: (DevolucionFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val hayErrorCant = cantNum > totalDisponible || (form.cantidadStr.isNotBlank() && cantNum <= 0)
        EnterpriseInputField(
            value = form.cantidadStr,
            onValueChange = { nuevo ->
                onCambio(form.copy(cantidadStr = nuevo.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            enabled = enabled,
            label = "CANTIDAD A PROCESAR",
            placeholder = "Máximo: ${totalDisponible.toInt()} $empaque",
            isError = hayErrorCant,
            errorMessage = if (cantNum > totalDisponible) "Supera el saldo (${totalDisponible.toInt()})" else "Mínimo 1"
        )

        if (form.esCanjesFisico) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EnterpriseInputField(
                    value = form.nuevoLoteNumero,
                    onValueChange = { onCambio(form.copy(nuevoLoteNumero = it.uppercase())) },
                    enabled = enabled,
                    label = "N° LOTE DE REPOSICIÓN",
                    placeholder = "Lote de entrega",
                    modifier = Modifier.weight(1.2f)
                )
                EnterpriseInputField(
                    value = form.nuevoVencimiento,
                    onValueChange = { onCambio(form.copy(nuevoVencimiento = it)) },
                    enabled = enabled,
                    label = "VENCIMIENTO",
                    placeholder = "MM/AAAA",
                    isError = form.nuevoVencimiento.isNotBlank() && !esFechaVencimientoValida(form.nuevoVencimiento),
                    modifier = Modifier.weight(0.8f)
                )
            }
            EnterpriseInputField(
                value = form.guiaCanjeStr,
                onValueChange = { onCambio(form.copy(guiaCanjeStr = it.uppercase())) },
                enabled = enabled,
                label = "N° GUÍA DE ENTREGA / CANJE",
                placeholder = "Comprobante"
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EnterpriseInputField(
                    value = form.guiaRetiroStr,
                    onValueChange = { onCambio(form.copy(guiaRetiroStr = it.uppercase())) },
                    enabled = enabled,
                    label = "N° GUÍA DE RETIRO",
                    placeholder = "Ticket",
                    modifier = Modifier.weight(1f)
                )
                EnterpriseInputField(
                    value = form.notaCreditoStr,
                    onValueChange = { onCambio(form.copy(notaCreditoStr = it.uppercase())) },
                    enabled = enabled,
                    label = "N° NOTA DE CRÉDITO",
                    placeholder = "Opcional",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Sección 3: Motivo de la resolución */
@Composable
private fun SeccionMotivo(
    form: DevolucionFormState,
    enabled: Boolean,
    onCambio: (DevolucionFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SeccionLabel("¿Por qué?")
        EnterpriseReasonList(
            reasons = motivosPorModalidad(form.esCanjesFisico),
            selectedReason = form.motivoSeleccionado,
            onSelectReason = { onCambio(form.copy(motivoSeleccionado = it)) },
            enabled = enabled
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PANEL DERECHO — EXPEDIENTE DEL LOTE + RESUMEN + BOTÓN DE ACCIÓN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PanelResumenDerecho(
    lote: LoteProducto,
    product: MoldeProductos,
    form: DevolucionFormState,
    cantNum: Double,
    costoUnitario: Double,
    totalReclamo: Double,
    isProcesando: Boolean,
    formularioValido: Boolean,
    onConfirmar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SeccionLabel("LOTE OBSERVADO")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EnterpriseInfoRow("Proveedor", lote.proveedorNombre.ifBlank { "Droguería no registrada" })
            EnterpriseInfoRow("Saldo del lote", "${lote.cantidad.toInt()} ${product.empaque}")
            EnterpriseInfoRow("Vencimiento actual", lote.vencimiento)
            EnterpriseInfoRow("Costo unitario", "$ ${String.format(Locale.US, "%.2f", costoUnitario)}", isMonospace = true)
        }
    }

    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val tituloResumen = if (form.esCanjesFisico) "RESULTADO DEL CANJE" else "IMPACTO FINANCIERO"
        SeccionLabel(tituloResumen, color = FDColors.TextPrimary)
        Text(
            text = textoResumen(form, cantNum, product.empaque, totalReclamo),
            style = FDType.Body.copy(color = FDColors.TextPrimary, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
        )
    }

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = onConfirmar,
        enabled = formularioValido && !isProcesando,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)
    ) {
        if (isProcesando) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText)
            Spacer(Modifier.width(10.dp))
            Text("PROCESANDO...", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
        } else {
            val labelBoton = if (form.esCanjesFisico) "PROCESAR CANJE" else "GENERAR NOTA DE CRÉDITO"
            Text(labelBoton, style = FDType.Label.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
