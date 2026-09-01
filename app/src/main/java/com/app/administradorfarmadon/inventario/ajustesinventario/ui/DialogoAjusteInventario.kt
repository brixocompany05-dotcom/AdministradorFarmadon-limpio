package com.app.administradorfarmadon.inventario.ajustesinventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback
import com.app.administradorfarmadon.inventario.compartido.ui.SelectorVencimiento
import com.app.administradorfarmadon.inventario.compartido.ui.StepperCantidad
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico

private data class OpcionAjuste(
    val tipo: String,
    val etiqueta: String,
    val descripcion: String,
    val icono: ImageVector,
    val esEntrada: Boolean
)

private val OPCIONES_AJUSTE = listOf(
    OpcionAjuste("MUESTRA", "Muestra del laboratorio", "Llegó una muestra gratis para probar o promocionar", Icons.Default.Info, true),
    OpcionAjuste("DONACION", "Regalo o donación", "Nos regalaron o donaron mercadería sin costo", Icons.Default.Star, true),
    OpcionAjuste("SOBRANTE", "Sobró del proveedor", "El proveedor dejó unidades de más sin cobrar", Icons.Default.Inventory2, true),
    OpcionAjuste("INVENTARIO_INICIAL", "Primera carga de stock", "Estamos cargando el stock que ya había en la farmacia", Icons.Default.Add, true),
    OpcionAjuste("MERMA", "Se dañó o se venció", "Mercadería dañada, rota o vencida que hay que sacar", Icons.Default.Warning, false),
    OpcionAjuste("PERDIDA", "Se perdió", "Se perdió o se extravió y ya no está en el estante", Icons.Default.Search, false),
    OpcionAjuste("ERROR_INVENTARIO", "Me equivoqué al contar", "El sistema dice que hay más de lo que realmente hay", Icons.Default.Edit, false),
    OpcionAjuste("DEVOLUCION", "Devolver al proveedor", "Devolución formal con guía, nota o canje por vencimiento", Icons.Default.LocalShipping, false)
)

/**
 * Ajuste de inventario en lenguaje humano: el usuario elige QUÉ PASÓ, CUÁNTO y POR QUÉ.
 * No es una compra: no crea factura ni toca pedidos. Todo queda trazado en el kardex.
 */
@Composable
fun DialogoAjusteInventario(
    producto: MoldeProductos,
    procesando: Boolean = false,
    mensajeError: String? = null,
    direccionInicial: String? = null,
    tipoInicial: String? = null,
    loteSalidaInicial: LoteProducto? = null,
    loteEntradaInicial: LoteProducto? = null,
    exito: Boolean = false,
    onReintentar: () -> Unit = {},
    onDismiss: () -> Unit,
    onRegistrarEntrada: (lote: String, vencimiento: String, cantidad: Double, tipo: String, motivo: String) -> Unit,
    onRegistrarSalida: (lote: String, cantidad: Double, tipo: String, motivo: String) -> Unit,
    onMotivoEspecialClick: (String, LoteProducto?) -> Unit = { _, _ -> }
) {
    val s = recordarMedidaAdaptativa()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    @OptIn(ExperimentalLayoutApi::class)
    val tecladoAbierto = WindowInsets.isImeVisible

    var opcionSeleccionada by remember {
        mutableStateOf(tipoInicial?.let { t -> OPCIONES_AJUSTE.firstOrNull { it.tipo == t } })
    }
    // Sincronizamos con direccionInicial para que responda al navegador lateral
    var direccion by remember(direccionInicial) { mutableStateOf(direccionInicial) }
    
    var loteNumero by remember { mutableStateOf(loteEntradaInicial?.numero ?: "") }
    var vencimiento by remember { mutableStateOf(loteEntradaInicial?.vencimiento ?: "") }
    var cantidadTexto by remember { mutableStateOf("") }
    var notasAdicionales by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    
    // Al cambiar la dirección desde afuera (navegador), reseteamos la opción
    LaunchedEffect(direccionInicial) {
        opcionSeleccionada = tipoInicial?.let { t -> OPCIONES_AJUSTE.firstOrNull { it.tipo == t } }
        errorLocal = null
    }

    var loteSalidaSeleccionado by remember { mutableStateOf(loteSalidaInicial?.numero) }

    val opcion = opcionSeleccionada
    val esEntrada = opcion?.esEntrada == true
    
    // Optimizamos cálculos derivados para evitar lag en recomposición
    val cantidad by remember(cantidadTexto) {
        derivedStateOf { cantidadTexto.replace(',', '.').toDoubleOrNull() ?: 0.0 }
    }
    
    val lotesDisponibles by remember(producto.lotes) {
        derivedStateOf {
            producto.lotes.values
                .filter { it.cantidad > 0 }
                .sortedByDescending { it.cantidad }
        }
    }
    
    val loteSalidaActual by remember(lotesDisponibles, loteSalidaSeleccionado) {
        derivedStateOf { lotesDisponibles.firstOrNull { it.numero.equals(loteSalidaSeleccionado, ignoreCase = true) } }
    }
    
    val stockActual = producto.stockDisponibleFisico
    val nuevoStock by remember(esEntrada, stockActual, cantidad) {
        derivedStateOf { if (esEntrada) stockActual + cantidad else stockActual - cantidad }
    }

    val loteValido by remember(opcion, esEntrada, loteNumero, loteSalidaSeleccionado) {
        derivedStateOf { opcion == null || (if (esEntrada) loteNumero.trim().isNotBlank() else loteSalidaSeleccionado != null) }
    }
    
    val vencimientoValido by remember(opcion, esEntrada, vencimiento) {
        derivedStateOf {
            opcion == null || !esEntrada || run {
                val v = FechaVencimientoHelper.normalizar(vencimiento.trim())
                v != null && (FechaVencimientoHelper.diasHastaVencer(v) ?: 0) > 0
            }
        }
    }
    
    val disponibleLote = loteSalidaActual?.cantidad ?: 0.0
    val cantidadValida by remember(opcion, cantidad, esEntrada, disponibleLote) {
        derivedStateOf { opcion == null || (cantidad > 0.0 && (esEntrada || cantidad <= disponibleLote + 0.001)) }
    }
    
    val motivoValido = true // Siempre válido porque el motivo base es la opción seleccionada

    val puedeGuardar by remember(opcion, loteValido, vencimientoValido, cantidadValida, motivoValido, procesando, exito) {
        derivedStateOf { 
            opcion != null && 
            opcion.tipo != "DEVOLUCION" && // No se guarda desde aquí si es devolución formal
            loteValido && vencimientoValido && cantidadValida && motivoValido && !procesando && !exito
        }
    }

    fun intentarGuardar() {
        val op = opcion ?: return
        when {
            !loteValido -> errorLocal = if (esEntrada) "Cuéntanos qué lote tiene la mercadería." else "Elige de qué lote sale."
            !vencimientoValido -> errorLocal = "La fecha de vencimiento no es válida o ya está vencida."
            !cantidadValida -> errorLocal = if (esEntrada) "Ingresa una cantidad mayor a 0." else "Esa cantidad es más de lo que hay en el lote."
            else -> {
                keyboardController?.hide(); focusManager.clearFocus()
                // La justificación es el Motivo (etiqueta) + notas si existen
                val justificacionFinal = if (notasAdicionales.isNotBlank()) "${op.etiqueta}: ${notasAdicionales.trim()}" else op.etiqueta
                
                if (op.esEntrada) {
                    onRegistrarEntrada(loteNumero.trim().uppercase(), vencimiento.trim(), cantidad, op.tipo, justificacionFinal)
                } else {
                    onRegistrarSalida(loteSalidaSeleccionado ?: "", cantidad, op.tipo, justificacionFinal)
                }
            }
        }
    }

    fun seleccionarOpcion(op: OpcionAjuste) {
        if (op.tipo == "DEVOLUCION") {
            val lote = loteSalidaSeleccionado?.let { n ->
                lotesDisponibles.firstOrNull { it.numero.equals(n, true) }
            } ?: lotesDisponibles.firstOrNull()
            onMotivoEspecialClick("DEVOLUCION", lote)
            return
        }
        opcionSeleccionada = op
        direccion = if (op.esEntrada) "ENTRADA" else "SALIDA"
        errorLocal = null
        if (!op.esEntrada && lotesDisponibles.size == 1) {
            loteSalidaSeleccionado = lotesDisponibles.first().numero
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        val esAncho = maxWidth >= 720.dp
        if (esAncho) {
            Row(Modifier.fillMaxSize()) {
                PanelIzquierdoAjuste(
                    opciones = OPCIONES_AJUSTE,
                    direccion = direccion,
                    opcionSeleccionada = opcion,
                    onSeleccionarOpcion = ::seleccionarOpcion,
                    loteNumero = loteNumero,
                    onLoteNumeroChange = { loteNumero = it.uppercase(); errorLocal = null },
                    vencimiento = vencimiento,
                    onVencimientoChange = { vencimiento = it; errorLocal = null },
                    cantidadTexto = cantidadTexto,
                    onCantidadChange = { cantidadTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; errorLocal = null },
                    esEntrada = esEntrada,
                    lotesDisponibles = lotesDisponibles,
                    loteSalidaActual = loteSalidaActual,
                    onSeleccionarLoteSalida = { loteSalidaSeleccionado = it; errorLocal = null },
                    vencimientoValido = vencimientoValido,
                    errorLocal = errorLocal,
                    s = s,
                    modifier = Modifier.weight(1.3f).fillMaxHeight().imePadding()
                )
                VerticalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = s.separatorH)
                PanelResumenAjuste(
                    producto = producto,
                    opcion = opcion,
                    cantidad = cantidad,
                    stockActual = stockActual,
                    nuevoStock = nuevoStock,
                    loteSalidaActual = loteSalidaActual,
                    notasAdicionales = notasAdicionales,
                    onNotasChange = { notasAdicionales = it },
                    puedeGuardar = puedeGuardar,
                    procesando = procesando,
                    mensajeError = mensajeError,
                    onGuardar = ::intentarGuardar,
                    exito = exito,
                    onReintentar = onReintentar,
                    tecladoAbierto = tecladoAbierto,
                    s = s,
                    modifier = Modifier.weight(0.7f).fillMaxHeight()
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(s.padCardLarge),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    SeccionQuePaso(
                        esEntrada = direccion == "ENTRADA",
                        opciones = OPCIONES_AJUSTE.filter { it.esEntrada == (direccion == "ENTRADA") },
                        seleccionada = opcion,
                        onSeleccionarOpcion = ::seleccionarOpcion,
                        s = s
                    )
                    if (opcion != null) {
                        CamposAjuste(
                            esEntrada = esEntrada,
                            loteNumero = loteNumero,
                            onLoteNumeroChange = { loteNumero = it.uppercase(); errorLocal = null },
                            vencimiento = vencimiento,
                            onVencimientoChange = { vencimiento = it; errorLocal = null },
                            vencimientoValido = vencimientoValido,
                            cantidadTexto = cantidadTexto,
                            onCantidadChange = { cantidadTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; errorLocal = null },
                            lotesDisponibles = lotesDisponibles,
                            loteSalidaActual = loteSalidaActual,
                            onSeleccionarLoteSalida = { loteSalidaSeleccionado = it; errorLocal = null },
                            opcion = opcion,
                            s = s
                        )
                        errorLocal?.let {
                            Text(it, style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error, modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Elige un motivo arriba para completar los datos.",
                                style = FDType.BodySmall,
                                color = FDColors.TextTertiary
                            )
                        }
                    }
                }
                
                // Encapsulamos el Footer en un componente local para que solo él se recomponga con el teclado
                FooterAjuste(
                    visible = !tecladoAbierto,
                    puedeGuardar = puedeGuardar,
                    procesando = procesando,
                    mensajeError = mensajeError,
                    onGuardar = ::intentarGuardar,
                    onReintentar = onReintentar,
                    s = s
                )
            }
        }

        if (exito) {
            Box(
                modifier = Modifier.fillMaxSize().background(FDColors.Background.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FDLottieFeedback(resId = com.app.administradorfarmadon.R.raw.anim_exito, isLoop = false, size = 110.dp)
                    Text("Ajuste guardado", style = FDType.Heading3.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                    Text("El inventario se actualizó en tiempo real.", style = FDType.Caption.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PanelIzquierdoAjuste(
    opciones: List<OpcionAjuste>,
    direccion: String?,
    opcionSeleccionada: OpcionAjuste?,
    onSeleccionarOpcion: (OpcionAjuste) -> Unit,
    loteNumero: String,
    onLoteNumeroChange: (String) -> Unit,
    vencimiento: String,
    onVencimientoChange: (String) -> Unit,
    cantidadTexto: String,
    onCantidadChange: (String) -> Unit,
    esEntrada: Boolean,
    lotesDisponibles: List<LoteProducto>,
    loteSalidaActual: LoteProducto?,
    onSeleccionarLoteSalida: (String) -> Unit,
    vencimientoValido: Boolean,
    errorLocal: String?,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(s.padCardLarge),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        SeccionQuePaso(
            esEntrada = direccion == "ENTRADA",
            opciones = opciones.filter { it.esEntrada == (direccion == "ENTRADA") },
            seleccionada = opcionSeleccionada,
            onSeleccionarOpcion = onSeleccionarOpcion,
            s = s
        )
        if (opcionSeleccionada != null) {
            CamposAjuste(
                esEntrada = esEntrada,
                loteNumero = loteNumero,
                onLoteNumeroChange = onLoteNumeroChange,
                vencimiento = vencimiento,
                onVencimientoChange = onVencimientoChange,
                vencimientoValido = vencimientoValido,
                cantidadTexto = cantidadTexto,
                onCantidadChange = onCantidadChange,
                lotesDisponibles = lotesDisponibles,
                loteSalidaActual = loteSalidaActual,
                onSeleccionarLoteSalida = onSeleccionarLoteSalida,
                opcion = opcionSeleccionada,
                s = s
            )
            errorLocal?.let {
                Text(it, style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Selecciona el motivo del ajuste para continuar.",
                    style = FDType.BodySmall,
                    color = FDColors.TextTertiary
                )
            }
        }
    }
}

/**
 * Selector simplificado: muestra los motivos en una grilla (FlowRow) para evitar
 * el scroll lateral y mejorar la visibilidad SaaS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionQuePaso(
    esEntrada: Boolean,
    opciones: List<OpcionAjuste>,
    seleccionada: OpcionAjuste?,
    onSeleccionarOpcion: (OpcionAjuste) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
        Text(
            if (esEntrada) "ELIGE EL MOTIVO DE ENTRADA" else "ELIGE EL MOTIVO DE SALIDA",
            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
            color = FDColors.TextTertiary
        )
        Text(
            if (esEntrada)
                "Mercadería que entra sin compra: muestra, regalo, sobrante o primera carga."
            else
                "Merma, pérdida, error de conteo o devolución al proveedor.",
            style = FDType.Caption.copy(fontSize = 10.5.sp),
            color = FDColors.TextTertiary
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall),
            maxItemsInEachRow = 3
        ) {
            opciones.forEach { op ->
                TarjetaOpcion(
                    opcion = op,
                    seleccionada = seleccionada?.tipo == op.tipo,
                    onSeleccionar = onSeleccionarOpcion,
                    modifier = Modifier.weight(1f).widthIn(min = 180.dp),
                    s = s
                )
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(
    opcion: OpcionAjuste,
    seleccionada: Boolean,
    onSeleccionar: (OpcionAjuste) -> Unit,
    modifier: Modifier,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Surface(
        onClick = { onSeleccionar(opcion) },
        color = if (seleccionada) FDColors.Primary.copy(alpha = 0.05f) else FDColors.SurfaceElevated.copy(alpha = 0.4f),
        shape = RoundedCornerShape(s.radiusInput),
        border = BorderStroke(
            if (seleccionada) 2.dp else 1.dp,
            if (seleccionada) FDColors.Primary else FDColors.Border.copy(alpha = 0.6f)
        ),
        modifier = modifier.height(96.dp) // Espacio para 2 líneas sin cortar texto
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(s.radiusChip))
                    .background(if (seleccionada) FDColors.Primary.copy(alpha = 0.12f) else FDColors.TextPrimary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    opcion.icono,
                    null,
                    tint = if (seleccionada) FDColors.Primary else FDColors.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    opcion.etiqueta,
                    style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = if (seleccionada) FontWeight.Black else FontWeight.Bold),
                    color = if (seleccionada) FDColors.Primary else FDColors.TextPrimary,
                    maxLines = 2
                )
                Text(
                    opcion.descripcion,
                    style = FDType.Caption.copy(fontSize = 9.5.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 2
                )
            }
            if (seleccionada) {
                Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun TablaLotes(
    lotes: List<LoteProducto>,
    seleccionadoNumero: String?,
    onSeleccionar: (String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FDShapes.Small)
            .background(FDColors.SurfaceElevated),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FDColors.TextPrimary.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOTE", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.2f))
            Text("VENCE", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.8f))
            Text("HAY", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
            Spacer(Modifier.width(26.dp))
        }
        lotes.forEachIndexed { index, lote ->
            val seleccionado = lote.numero.equals(seleccionadoNumero, ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            seleccionado -> FDColors.Primary.copy(alpha = 0.08f)
                            index % 2 == 0 -> Color.Transparent
                            else -> FDColors.TextPrimary.copy(alpha = 0.03f)
                        }
                    )
                    .clickable { onSeleccionar(lote.numero) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lote.numero,
                    style = FDType.Body.copy(fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Medium),
                    color = if (seleccionado) FDColors.Primary else FDColors.TextPrimary,
                    modifier = Modifier.weight(1.2f),
                    maxLines = 1
                )
                Text(
                    lote.vencimiento.ifBlank { "—" },
                    style = FDType.BodySmall.copy(fontSize = 11.sp),
                    color = FDColors.TextSecondary,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    "${lote.cantidad.toInt()}",
                    style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.End
                )
                if (seleccionado) {
                    Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                } else {
                    Spacer(Modifier.width(18.dp))
                }
            }
            if (index < lotes.lastIndex) {
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
internal fun TablaLotesAnimada(
    lotes: List<LoteProducto>,
    seleccionadoNumero: String?,
    onSeleccionar: (String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FDShapes.Small)
            .background(FDColors.SurfaceElevated),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FDColors.TextPrimary.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOTE", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(1.2f))
            Text("VENCE", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.8f))
            Text("HAY", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
            Spacer(Modifier.width(26.dp))
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(
                lotes,
                key = { _, lote -> "lote|${lote.loteId}|${lote.numero}|${lote.vencimiento}" }
            ) { index, lote ->
                val seleccionado = lote.numero.equals(seleccionadoNumero, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .background(
                            when {
                                seleccionado -> FDColors.Primary.copy(alpha = 0.08f)
                                index % 2 == 0 -> Color.Transparent
                                else -> FDColors.TextPrimary.copy(alpha = 0.03f)
                            }
                        )
                        .clickable { onSeleccionar(lote.numero) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        lote.numero,
                        style = FDType.Body.copy(fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Medium),
                        color = if (seleccionado) FDColors.Primary else FDColors.TextPrimary,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        lote.vencimiento.ifBlank { "—" },
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        "${lote.cantidad.toInt()}",
                        style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.End
                    )
                    if (seleccionado) {
                        Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                    } else {
                        Spacer(Modifier.width(18.dp))
                    }
                }
                if (index < lotes.lastIndex) {
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun CamposAjuste(
    esEntrada: Boolean,
    loteNumero: String,
    onLoteNumeroChange: (String) -> Unit,
    vencimiento: String,
    onVencimientoChange: (String) -> Unit,
    vencimientoValido: Boolean,
    cantidadTexto: String,
    onCantidadChange: (String) -> Unit,
    lotesDisponibles: List<LoteProducto>,
    loteSalidaActual: LoteProducto?,
    onSeleccionarLoteSalida: (String) -> Unit,
    opcion: OpcionAjuste?,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Surface(
        color = FDColors.SurfaceElevated.copy(alpha = 0.5f),
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            Text(
                "DATOS TÉCNICOS DEL AJUSTE",
                style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = FDColors.TextTertiary
            )
            
            if (esEntrada) {
                Text(
                    "Entra mercadería sin compra. No crea factura ni toca pedidos al proveedor.",
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = FDColors.TextSecondary
                )
                StepperCantidad(
                    cantidad = cantidadTexto.replace(',', '.').toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 1,
                    onCantidadChange = { onCantidadChange(it.toString()) },
                    minimo = 1,
                    maximo = Int.MAX_VALUE
                )

                // Fila 2: Lote y Vencimiento en grilla 2 col
                if (loteNumero.isBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FDSlimTextField(
                            value = loteNumero,
                            onValueChange = onLoteNumeroChange,
                            label = "Número de Lote",
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            s = s
                        )
                        SelectorVencimiento(
                            vencimiento = vencimiento,
                            onVencimientoChange = onVencimientoChange,
                            label = "Vencimiento",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "Indica en qué lote entra la mercadería y cuándo vence.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextSecondary
                    )
                } else {
                    Text(
                        "Entrando al lote ${loteNumero.uppercase()} · Vence ${vencimiento.ifBlank { "—" }}",
                        style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.Primary
                    )
                }
            } else {
                if (lotesDisponibles.isEmpty()) {
                    Text(
                        "Este producto no tiene stock que ajustar.",
                        style = FDType.BodySmall.copy(fontSize = s.textLabel.value.sp),
                        color = FDColors.Warning
                    )
                } else if (loteSalidaActual == null) {
                    Text(
                        "1 · ELIGE EL LOTE DEL QUE SALE",
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = FDColors.Primary
                    )
                    TablaLotes(
                        lotes = lotesDisponibles,
                        seleccionadoNumero = null,
                        onSeleccionar = onSeleccionarLoteSalida,
                        s = s
                    )
                    Text(
                        "Toca el lote del que sale la mercadería para continuar.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary
                    )
                } else {
                    SeccionCantidadPorLote(
                        lote = loteSalidaActual,
                        cantidadTexto = cantidadTexto,
                        onCantidadChange = onCantidadChange,
                        s = s
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionCantidadPorLote(
    lote: LoteProducto,
    cantidadTexto: String,
    onCantidadChange: (String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    val cantNum = cantidadTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
    val disponible = lote.cantidad.coerceAtLeast(0.0)
    val quedan = (disponible - cantNum).coerceAtLeast(0.0)
    val hayError = cantidadTexto.isNotBlank() && (cantNum <= 0 || cantNum > disponible)

    Surface(
        color = FDColors.Primary.copy(alpha = 0.05f),
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Inventory2, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                Text(
                    "LOTE ${lote.numero.ifBlank { "Sin número" }} · Vence ${lote.vencimiento.ifBlank { "Sin fecha" }}",
                    style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Disponible: ${disponible.toInt()}",
                    style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextSecondary
                )
            }

            StepperCantidad(
                cantidad = cantNum.toInt().coerceAtLeast(1),
                onCantidadChange = { onCantidadChange(it.toString()) },
                minimo = 1,
                maximo = disponible.toInt().coerceAtLeast(1)
            )

            Text(
                text = when {
                    cantidadTexto.isBlank() -> "Escribe cuántas unidades salen de este lote."
                    cantNum <= 0 -> "La cantidad debe ser mayor a 0."
                    cantNum > disponible -> "Solo hay ${disponible.toInt()} unidades en este lote."
                    else -> "Salen ${numeroLegible(cantNum)} → quedan ${numeroLegible(quedan)} en el lote ${lote.numero}."
                },
                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = if (hayError) FontWeight.Bold else FontWeight.Medium),
                color = when {
                    cantidadTexto.isBlank() -> FDColors.TextSecondary
                    hayError -> FDColors.Error
                    else -> FDColors.Success
                }
            )

        }
    }
}

private fun numeroLegible(v: Double): String =
    if (kotlin.math.abs(v - v.toInt()) < 0.001) v.toInt().toString() else String.format("%.2f", v)

/**
 * Campo de texto optimizado para Enterprise SaaS: elimina padding excesivo
 * y permite una densidad de información mayor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FDSlimTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = FDType.Body.copy(fontSize = 14.sp),
    modifier: Modifier = Modifier,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(color = FDColors.TextPrimary),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(FDColors.Primary),
        interactionSource = interactionSource
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text(label, fontSize = 12.sp) },
            placeholder = { Text(placeholder, fontSize = 14.sp, color = FDColors.TextTertiary) },
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FDColors.Background,
                unfocusedContainerColor = FDColors.Background,
                focusedBorderColor = FDColors.Primary,
                unfocusedBorderColor = FDColors.Border,
                errorBorderColor = FDColors.Error,
                focusedLabelColor = FDColors.Primary,
                unfocusedLabelColor = FDColors.TextTertiary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = true,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FDColors.Background,
                        unfocusedContainerColor = FDColors.Background,
                        focusedBorderColor = FDColors.Primary,
                        unfocusedBorderColor = FDColors.Border,
                        errorBorderColor = FDColors.Error
                    ),
                    shape = RoundedCornerShape(s.radiusInput)
                )
            }
        )
    }
}

@Composable
private fun FooterAjuste(
    visible: Boolean,
    puedeGuardar: Boolean,
    procesando: Boolean,
    mensajeError: String?,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit = {},
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    if (!visible) return
    
    Surface(
        color = FDColors.Surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f))
    ) {
        Box(Modifier.padding(s.padCardLarge)) {
            PieBotonesAjuste(
                puedeGuardar = puedeGuardar,
                procesando = procesando,
                mensajeError = mensajeError,
                onGuardar = onGuardar,
                onReintentar = onReintentar,
                s = s
            )
        }
    }
}

@Composable
private fun PanelResumenAjuste(
    producto: MoldeProductos,
    opcion: OpcionAjuste?,
    cantidad: Double,
    stockActual: Double,
    nuevoStock: Double,
    loteSalidaActual: LoteProducto?,
    notasAdicionales: String,
    onNotasChange: (String) -> Unit,
    puedeGuardar: Boolean,
    procesando: Boolean,
    mensajeError: String?,
    onGuardar: () -> Unit,
    exito: Boolean = false,
    onReintentar: () -> Unit = {},
    tecladoAbierto: Boolean,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(s.padCardLarge),
            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            Text("RESUMEN DEL IMPACTO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp), color = FDColors.TextTertiary)
            Surface(
                color = FDColors.SurfaceElevated,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                    FilaResumenAjuste("Producto", producto.nombre)
                    FilaResumenAjuste("Stock actual", "${stockActual.toInt()} und")
                    opcion?.let {
                        FilaResumenAjuste("Motivo base", it.etiqueta, color = FDColors.Primary)
                    }
                    if (cantidad > 0.0) {
                        FilaResumenAjuste(
                            if (opcion?.esEntrada == true) "Entran" else "Saldrán",
                            "${cantidad.toInt()} und",
                            color = if (opcion?.esEntrada == true) FDColors.Success else FDColors.Warning
                        )
                    }
                    if (opcion?.esEntrada == false && loteSalidaActual != null) {
                        FilaResumenAjuste("Del lote", "${loteSalidaActual.numero} (hay ${loteSalidaActual.cantidad.toInt()})")
                    }
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = s.separatorH)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("QUEDARÁ", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
                        Text(
                            if (nuevoStock < 0.0) "NO PERMITIDO" else "${nuevoStock.toInt()} UND",
                            style = FDType.Numeric.copy(fontSize = 26.sp, fontWeight = FontWeight.Black),
                            color = if (nuevoStock < 0.0) FDColors.Error else FDColors.Primary
                        )
                    }
                }
            }

            // Campo de Notas Adicionales (Opcional)
            Surface(
                color = FDColors.SurfaceElevated.copy(alpha = 0.6f),
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "NOTAS O DETALLES ADICIONALES (OPCIONAL)",
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                        color = FDColors.TextTertiary
                    )
                    BasicTextField(
                        value = notasAdicionales,
                        onValueChange = onNotasChange,
                        textStyle = FDType.Body.copy(color = FDColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (notasAdicionales.isEmpty()) {
                                Text("Ej: Dejado por visitador médico...", style = FDType.BodySmall, color = FDColors.TextTertiary)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Text(
                "Esto NO es una compra: no se crea factura ni se tocan pedidos. Queda registrado quién lo hizo y el motivo.",
                style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                color = FDColors.TextSecondary
            )
            mensajeError?.let { msg ->
                Surface(
                    color = FDColors.Error.copy(alpha = 0.1f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(msg, style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                        TextButton(onClick = onReintentar, enabled = !procesando && !exito) {
                            Text("REINTENTAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), color = FDColors.Primary)
                        }
                    }
                }
            }
            if (procesando) {
                Surface(
                    color = FDColors.Primary.copy(alpha = 0.08f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Box(Modifier.size(8.dp).clip(FDShapes.Full).background(FDColors.Primary))
                        Text(
                            "Guardando ajuste…",
                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextPrimary
                        )
                    }
                }
            }
        }
        
        // Encapsulamos el footer lateral para rendimiento
        FooterAjuste(
            visible = !tecladoAbierto,
            puedeGuardar = puedeGuardar,
            procesando = procesando,
            mensajeError = null,
            onGuardar = onGuardar,
            s = s
        )
    }
}

@Composable
private fun PieBotonesAjuste(
    puedeGuardar: Boolean,
    procesando: Boolean,
    mensajeError: String?,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit = {},
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    mensajeError?.let { msg ->
        Surface(
            color = FDColors.Error.copy(alpha = 0.1f),
            shape = FDShapes.Small,
            border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(msg, style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                TextButton(onClick = onReintentar, enabled = !procesando) {
                    Text("REINTENTAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), color = FDColors.Primary)
                }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onGuardar,
            enabled = puedeGuardar,
            colors = ButtonDefaults.buttonColors(
                containerColor = FDColors.Primary,
                contentColor = FDColors.PrimaryText,
                disabledContainerColor = FDColors.Primary.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(s.radiusButton),
            modifier = Modifier.width(320.dp).height(56.dp)
        ) {
            if (procesando) {
                Text("GUARDANDO AJUSTE...", style = FDType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, color = FDColors.PrimaryText))
            } else {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = FDColors.PrimaryText)
                Spacer(Modifier.width(12.dp))
                Text("GUARDAR AJUSTE", style = FDType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, color = FDColors.PrimaryText))
            }
        }
    }
}

@Composable
private fun FilaResumenAjuste(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color = FDColors.TextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = FDType.BodySmall.copy(fontSize = 11.5.sp), color = FDColors.TextTertiary)
        Text(valor, style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = color)
    }
}
