package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.logica.LineaAnulacionVista
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

private val MOTIVOS_ANULACION_FACTURA = listOf(
    "La escribí mal o está duplicada",
    "La mercadería nunca llegó o llegó incompleta",
    "El producto o el lote es equivocado",
    "Devolución al proveedor (parcial o total)",
    "Conflicto con el proveedor"
)

private fun textoRespuestaPlata(decision: String?): String = when (decision) {
    "SALDO_A_FAVOR" -> "saldo a favor del proveedor"
    "DEVOLUCION_RECIBIDA" -> "devolución ya recibida del proveedor"
    "PERDIDA" -> "pérdida declarada por el dueño"
    else -> ""
}

@Composable
fun DialogoAnularFactura(
    factura: FacturaCompra,
    lineas: List<LineaAnulacionVista>,
    cargandoLineas: Boolean,
    procesando: Boolean,
    autorizadoPlata: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (motivo: String, respuestaPlata: String?, metodoDevolucion: String?, referenciaDevolucion: String?) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    val plataPagada = factura.plataPagadaEnFactura

    val esContado = factura.esContado
    // "Contado" describe la condición pactada; solo el pago real pone dinero en juego.
    val hayDineroEnJuego = plataPagada > 0.01

    var motivoSeleccionado by remember { mutableStateOf<String?>(null) }
    var respuestaPlata by remember { mutableStateOf<String?>(null) }
    var metodoDevolucion by remember { mutableStateOf<String?>(null) }
    var referenciaDevolucion by remember { mutableStateOf("") }

    // La anulación devuelve TODO el lote de la factura o no se hace (nunca a medias).
    // Si falta stock, un lote o el producto, se bloquea el botón con la razón real.
    val sinLineas = !cargandoLineas && lineas.isEmpty()
    val bloqueoDevolucion = lineas.firstOrNull { it.noVuelve > 0.01 || !it.loteExiste || !it.productoExiste }
    val puedeConfirmar = !cargandoLineas && motivoSeleccionado != null &&
        (!hayDineroEnJuego || (respuestaPlata != null && (respuestaPlata != "DEVOLUCION_RECIBIDA" || metodoDevolucion != null))) &&
        !sinLineas && bloqueoDevolucion == null

    val motivoBloqueo = when {
        sinLineas -> "Esta factura no tiene productos para devolver. No se puede anular con devolución de stock."
        bloqueoDevolucion == null -> null
        !bloqueoDevolucion.productoExiste -> "El producto '${bloqueoDevolucion.productoNombre}' ya no existe en inventario. No se puede anular devolviendo stock."
        !bloqueoDevolucion.loteExiste -> "El lote '${bloqueoDevolucion.loteNumero}' de '${bloqueoDevolucion.productoNombre}' ya no existe (se vendió o se borró)."
        else -> "Falta stock para devolver el lote '${bloqueoDevolucion.loteNumero}' de '${bloqueoDevolucion.productoNombre}': la factura dice ${bloqueoDevolucion.entro.toInt()}u y hoy hay ${bloqueoDevolucion.hoy.toInt()}u."
    }

    BackHandler(enabled = true) { if (!procesando) onDismiss() }

    // ═══ PANEL COMPLETO (no diálogo) ═══
    Surface(
        color = FDColors.Background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── BARRA SUPERIOR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.Surface)
                    .padding(horizontal = s.padCardLarge, vertical = s.padCard),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    IconButton(onClick = { if (!procesando) onDismiss() }) {
                        Icon(Icons.Default.Close, "Cerrar", tint = FDColors.TextSecondary)
                    }
                    Surface(
                        color = FDColors.Error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(s.radiusChip),
                        modifier = Modifier.size(s.iconLarge + s.xs)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Cancel, null, tint = FDColors.Error, modifier = Modifier.size(s.iconMedium))
                        }
                    }
                    Column {
                        Text(
                            "Anular Factura ${factura.numeroFactura.ifBlank { "sin número" }}",
                            style = FDType.Heading2.copy(fontSize = s.textTitle.value.sp, fontWeight = FontWeight.Black),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "${factura.proveedorNombre} · ${factura.fechaRegistro} · ${if (factura.esContado) "Contado" else "Crédito"}",
                            style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.9f),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border, thickness = s.separatorH)

            // ── CONTENIDO: DOS PANELES ──
            Row(
                modifier = Modifier.fillMaxSize().padding(s.padCardLarge),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {

                // ═══════════════════════════════════════════════════════
                // PANEL IZQUIERDO (55%): TABLA DE PRODUCTOS
                // ═══════════════════════════════════════════════════════
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.55f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Cabecera de la tabla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FDColors.SurfaceElevated)
                                .padding(horizontal = s.padCard, vertical = s.gapSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                        ) {
                            Icon(Icons.Default.Inventory2, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall))
                            Text(
                                "PRODUCTOS EN LA FACTURA",
                                style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                color = FDColors.TextSecondary
                            )
                            Spacer(Modifier.weight(1f))
                            if (!cargandoLineas) {
                                Text(
                                    "${lineas.size} producto${if (lineas.size != 1) "s" else ""}",
                                    style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.9f),
                                    color = FDColors.TextTertiary
                                )
                            }
                        }

                        if (cargandoLineas) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                                    CircularProgressIndicator(modifier = Modifier.size(s.iconMedium), color = FDColors.Primary, strokeWidth = 2.dp)
                                    Text("Contando el estante…", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                }
                            }
                        } else {
                            // Encabezados de columnas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FDColors.Background)
                                    .padding(horizontal = s.padCard, vertical = s.xs * 1.3f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRODUCTO / LOTE", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = FDColors.TextTertiary, modifier = Modifier.weight(1.8f))
                                Text("ENTRÓ", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text("HOY", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text("DEVUELVE", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.Success, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                Text("NO VUELVE", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.Warning, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            }

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                            // Filas de productos
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(lineas, key = { _, it -> it.productoId + it.loteNumero }) { index, linea ->
                                    FilaProducto(linea, esPar = index % 2 == 0)
                                    if (index < lineas.lastIndex) {
                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = s.padCard))
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════
                // PANEL DERECHO (45%): INFO + OPERACIÓN + ACCIÓN
                // ═══════════════════════════════════════════════════════
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Medium,
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.weight(0.45f).fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(s.padCardLarge),
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {

                        // ── INFO DE LA FACTURA ──
                        Text(
                            "INFORMACIÓN DE LA FACTURA",
                            style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                            color = FDColors.TextTertiary
                        )
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = FDShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(s.padCard),
                                verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                            ) {
                                FilaInfo("Proveedor", factura.proveedorNombre)
                                FilaInfo("N° Factura", factura.numeroFactura.ifBlank { "Sin número" })
                                FilaInfo("Fecha emisión", factura.fechaRegistro)
                                FilaInfo("Tipo", if (factura.esContado) "Contado" else "Crédito")
                                if (plataPagada > 0.01) {
                                    FilaInfo("Pagado", "$simboloMoneda ${String.format(Locale.US, "%,.2f", plataPagada)}", esDestacado = true)
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = s.separatorH)

                        // ── MOTIVO ──
                        Text(
                            "MOTIVO DE ANULACIÓN",
                            style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                            color = FDColors.TextTertiary
                        )
                        SelectorOpcion(
                            opcionSeleccionada = motivoSeleccionado,
                            opciones = MOTIVOS_ANULACION_FACTURA,
                            habilitado = !procesando,
                            onSeleccionar = { motivoSeleccionado = it },
                            placeholder = "Elige el motivo…"
                        )

                        // ── PLATA (solo si existe un pago real) ──
                        if (hayDineroEnJuego) {
                            val montoEnJuego = plataPagada
                            Text(
                                "¿QUÉ PASA CON $simboloMoneda ${String.format(Locale.US, "%,.2f", montoEnJuego)}?",
                                style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                color = FDColors.TextTertiary
                            )
                            if (autorizadoPlata) {
                                SelectorOpcion(
                                    opcionSeleccionada = when (respuestaPlata) {
                                        "SALDO_A_FAVOR" -> "El proveedor me lo debe (queda a mi favor)"
                                        "DEVOLUCION_RECIBIDA" -> "El proveedor ya me lo devolvió"
                                        "PERDIDA" -> "Ya no se recupera (pérdida)"
                                        else -> null
                                    },
                                    opciones = listOf(
                                        "El proveedor me lo debe (queda a mi favor)",
                                        "El proveedor ya me lo devolvió",
                                        "Ya no se recupera (pérdida)"
                                    ),
                                    habilitado = !procesando,
                                    onSeleccionar = { elegido ->
                                        respuestaPlata = when (elegido) {
                                            "El proveedor me lo debe (queda a mi favor)" -> "SALDO_A_FAVOR"
                                            "El proveedor ya me lo devolvió" -> "DEVOLUCION_RECIBIDA"
                                            else -> "PERDIDA"
                                        }
                                        // Al cambiar de decisión, lo de la opción anterior
                                        // se limpia completo: jamás se guarda un dato
                                        // "fantasma" que la pantalla ya no muestra.
                                        metodoDevolucion = null
                                        referenciaDevolucion = ""
                                    },
                                    placeholder = "Decide qué pasa con el dinero…"
                                )
                                if (respuestaPlata == "DEVOLUCION_RECIBIDA") {
                                    Text(
                                        "¿CÓMO TE LO DEVOLVIERON? *",
                                        style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                        color = FDColors.TextTertiary
                                    )
                                    SelectorOpcion(
                                        opcionSeleccionada = metodoDevolucion,
                                        // Coherencia: solo métodos reales que existen en el catálogo.
                                        opciones = listOf("Efectivo", "Transferencia", "Cheque"),
                                        habilitado = !procesando,
                                        onSeleccionar = { metodoDevolucion = it },
                                        placeholder = "Elige el método…"
                                    )
                                    OutlinedTextField(
                                        value = referenciaDevolucion,
                                        onValueChange = { referenciaDevolucion = it },
                                        label = { Text("REFERENCIA (OPCIONAL)", fontSize = s.textLabel.value.sp) },
                                        placeholder = { Text("N° de operación, cheque…", fontSize = s.textLabel.value.sp) },
                                        singleLine = true,
                                        enabled = !procesando,
                                        textStyle = FDType.Body.copy(fontSize = s.textInput.value.sp, color = FDColors.TextPrimary),
                                        shape = RoundedCornerShape(s.radiusInput),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FDColors.Primary,
                                            unfocusedBorderColor = FDColors.Border,
                                            focusedContainerColor = FDColors.SurfaceElevated,
                                            unfocusedContainerColor = FDColors.SurfaceElevated,
                                            focusedTextColor = FDColors.TextPrimary,
                                            unfocusedTextColor = FDColors.TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = s.inputMinH)
                                    )
                                    Text(
                                        "Queda registrado cómo y con qué referencia se devolvió la plata.",
                                        style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.92f),
                                        color = FDColors.TextTertiary
                                    )
                                }
                            } else {
                                Surface(
                                    color = FDColors.WarningSubtle,
                                    shape = FDShapes.Small,
                                    border = BorderStroke(s.borderWidth * 0.8f, FDColors.Warning.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(s.padCard * 0.7f),
                                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Lock, null, tint = FDColors.Warning, modifier = Modifier.size(s.iconSmall))
                                        Text(
                                            "Solo el dueño o administración puede decidir qué pasa con $simboloMoneda ${String.format(Locale.US, "%,.2f", montoEnJuego)}.",
                                            style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f),
                                            color = FDColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = s.separatorH)

                        // ── RESUMEN: QUEDARÁ ESCRITO ──
                        Text(
                            "QUEDARÁ ESCRITO",
                            style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                            color = FDColors.TextTertiary
                        )
                        val totalDevuelve = lineas.sumOf { it.devuelve }.toInt()
                        val totalNoVuelve = lineas.sumOf { it.noVuelve }.toInt()
                        ResumenFila(
                            Icons.Default.Receipt, "LA FACTURA",
                            "Factura ${factura.numeroFactura.ifBlank { "sin número" }} queda ANULADA",
                            FDColors.Error
                        )
                        ResumenFila(
                            Icons.Default.Inventory2, "INVENTARIO",
                            if (cargandoLineas) "Contando…"
                            else {
                                val textoDevuelve = "$totalDevuelve producto${if (totalDevuelve != 1) "s" else ""} se ${if (totalDevuelve == 1) "devuelve" else "devuelven"} al inventario"
                                if (totalNoVuelve > 0) "$textoDevuelve · $totalNoVuelve sin stock para devolver: la anulación queda bloqueada"
                                else textoDevuelve
                            },
                            FDColors.Primary
                        )
                        ResumenFila(
                            Icons.Default.Payments, "EL PAGO",
                            when {
                                plataPagada > 0.01 -> "$simboloMoneda ${String.format(Locale.US, "%,.2f", plataPagada)} ya abonados quedan como ${textoRespuestaPlata(respuestaPlata).ifBlank { "falta decidir" }}"
                                factura.esContado -> "Contado, pero todavía no hay un pago registrado"
                                else -> "Crédito sin abonos — nada que decidir"
                            },
                            if (plataPagada > 0.01) FDColors.Primary else FDColors.TextTertiary
                        )

                        if (motivoBloqueo != null) {
                            Surface(
                                color = FDColors.Error.copy(alpha = 0.08f),
                                shape = FDShapes.Small,
                                border = BorderStroke(s.borderWidth * 0.8f, FDColors.Error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(s.padCard * 0.7f),
                                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.WarningAmber, null, tint = FDColors.Error, modifier = Modifier.size(s.iconSmall))
                                    Text(
                                        motivoBloqueo,
                                        style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f, fontWeight = FontWeight.SemiBold),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // ── BOTONES DE ACCIÓN ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                        ) {
                            OutlinedButton(
                                onClick = { if (!procesando) onDismiss() },
                                enabled = !procesando,
                                shape = RoundedCornerShape(s.radiusButton),
                                border = BorderStroke(s.borderWidth, FDColors.Border),
                                modifier = Modifier.weight(1f).height(s.btnMediumH)
                            ) {
                                Text("CANCELAR", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                            }
                            Button(
                                onClick = { onConfirmar(motivoSeleccionado ?: "", respuestaPlata, metodoDevolucion, referenciaDevolucion) },
                                enabled = !procesando && puedeConfirmar,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Error,
                                    contentColor = FDColors.Surface,
                                    disabledContainerColor = FDColors.Error.copy(alpha = 0.3f),
                                    disabledContentColor = FDColors.Surface.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(s.radiusButton),
                                modifier = Modifier.weight(1.4f).height(s.btnMediumH)
                            ) {
                                if (procesando) {
                                    CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = FDColors.Surface, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(s.xs))
                                    Text("ANULANDO…", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Black))
                                } else {
                                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(s.iconSmall))
                                    Spacer(Modifier.width(s.xs * 0.7f))
                                    Text("ANULAR Y CERRAR", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Black))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// COMPONENTES
// ═══════════════════════════════════════════════════════════

@Composable
private fun FilaProducto(linea: LineaAnulacionVista, esPar: Boolean) {
    val s = recordarMedidaAdaptativa()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (esPar) Color.Transparent else FDColors.SurfaceElevated.copy(alpha = 0.4f))
            .padding(horizontal = s.padCard, vertical = s.xs * 1.2f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.8f)) {
            Text(
                linea.productoNombre.ifBlank { "Producto" },
                style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Medium),
                color = FDColors.TextPrimary,
                maxLines = 1
            )
            val notaLote = when {
                !linea.productoExiste -> "Ya no está en inventario"
                !linea.loteExiste -> "Lote no encontrado"
                else -> "Lote ${linea.loteNumero.ifBlank { "—" }}"
            }
            Text(notaLote, style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.9f), color = FDColors.TextTertiary, maxLines = 1)
        }
        Text("${linea.entro.toInt()}", style = FDType.Body.copy(fontSize = s.textBody.value.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
        Text("${linea.hoy.toInt()}", style = FDType.Body.copy(fontSize = s.textBody.value.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
        Text("${linea.devuelve.toInt()}", style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Success, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
        Text("${linea.noVuelve.toInt()}", style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold), color = if (linea.noVuelve > 0) FDColors.Warning else FDColors.TextTertiary, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
    }
}

@Composable
private fun FilaInfo(etiqueta: String, valor: String, esDestacado: Boolean = false) {
    val s = recordarMedidaAdaptativa()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f), color = FDColors.TextTertiary)
        Text(
            valor,
            style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = if (esDestacado) FontWeight.Bold else FontWeight.Medium),
            color = if (esDestacado) FDColors.Warning else FDColors.TextPrimary
        )
    }
}

@Composable
private fun SelectorOpcion(
    opcionSeleccionada: String?,
    opciones: List<String>,
    habilitado: Boolean,
    onSeleccionar: (String) -> Unit,
    placeholder: String
) {
    val s = recordarMedidaAdaptativa()
    var abierto by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { if (habilitado) abierto = true },
            color = if (opcionSeleccionada != null) FDColors.Primary.copy(alpha = 0.04f) else FDColors.SurfaceElevated,
            shape = FDShapes.Small,
            border = BorderStroke(
                s.borderWidth,
                if (opcionSeleccionada != null) FDColors.Primary.copy(alpha = 0.4f) else FDColors.Border
            ),
            modifier = Modifier.fillMaxWidth().height(s.btnMediumH)
        ) {
            Row(modifier = Modifier.padding(horizontal = s.padCard * 0.7f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    opcionSeleccionada ?: placeholder,
                    style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = if (opcionSeleccionada != null) FontWeight.Medium else FontWeight.Normal),
                    color = if (opcionSeleccionada != null) FDColors.TextPrimary else FDColors.TextTertiary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Icon(
                    if (abierto) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    null,
                    tint = if (opcionSeleccionada != null) FDColors.Primary else FDColors.TextTertiary,
                    modifier = Modifier.size(s.iconSmall)
                )
            }
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier.background(FDColors.SurfaceElevated)
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opcion,
                            style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = if (opcion == opcionSeleccionada) FontWeight.Medium else FontWeight.Normal),
                            color = if (opcion == opcionSeleccionada) FDColors.Primary else FDColors.TextPrimary
                        )
                    },
                    leadingIcon = if (opcion == opcionSeleccionada) {
                        { Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall)) }
                    } else null,
                    onClick = {
                        onSeleccionar(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ResumenFila(icono: ImageVector, titulo: String, detalle: String, color: Color) {
    val s = recordarMedidaAdaptativa()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(titulo, style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp), color = color)
            Text(detalle, style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Medium), color = FDColors.TextPrimary)
        }
    }
}
