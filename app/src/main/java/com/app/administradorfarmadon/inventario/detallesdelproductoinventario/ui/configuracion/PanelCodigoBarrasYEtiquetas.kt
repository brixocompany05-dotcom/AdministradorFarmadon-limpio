package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sub-módulo 5: Generador e Impresor de Códigos de Barra y Etiquetas Comerciales.
 * Directo, sin opciones falsas de almacén, enfocado 100% en etiquetas de venta al público.
 */
@Composable
fun PanelCodigoBarrasYEtiquetas(
    producto: MoldeProductos,
    onCodigoChange: (String) -> Unit,
    onGenerarCodigoUnico: suspend () -> String = { "" },
    onVerificarDuplicadoCodigo: suspend (String) -> String? = { null },
    onMarcarEtiquetaImpresa: () -> Unit = {},
    modifier: Modifier = Modifier,
    isGuardando: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val codigoActual = remember(producto.codigo) {
        producto.codigo.trim()
    }

    var inputCodigo by remember(codigoActual) { mutableStateOf(codigoActual) }
    var errorDuplicado by remember { mutableStateOf<String?>(null) }
    var isGenerandoCodigo by remember { mutableStateOf(false) }

    var formatoGondola by remember { mutableStateOf(true) }
    var cantidadCopias by remember { mutableIntStateOf(1) }

    // Presentaciones con precio real > 0
    val presentacionesValidas = remember(producto.presentaciones, FDColors.isDark) {
        producto.presentaciones.filter { it.precioventa > 0 }
    }

    var presentacionSeleccionada by remember(presentacionesValidas) {
        mutableStateOf(presentacionesValidas.firstOrNull())
    }

    var modoEdicionCodigo by remember { mutableStateOf(false) }

    LaunchedEffect(inputCodigo) {
        val limpio = inputCodigo.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (limpio.isNotBlank() && limpio != codigoActual) {
            delay(500)
            val conflicto = onVerificarDuplicadoCodigo(limpio)
            if (conflicto != null) {
                errorDuplicado = conflicto
            } else {
                errorDuplicado = null
                onCodigoChange(limpio)
            }
        } else if (limpio.isBlank() && codigoActual.isNotBlank()) {
            errorDuplicado = null
        } else {
            errorDuplicado = null
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. CÓDIGO DE BARRAS (UNA SOLA LÍNEA LIMPIA) ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, if (errorDuplicado != null) FDColors.Error else FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = null,
                            tint = if (codigoActual.isNotBlank()) FDColors.Primary else FDColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (codigoActual.isNotBlank()) "Código: $codigoActual" else "—",
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FDColors.TextPrimary)
                            )
                            if (errorDuplicado != null) {
                                Text(
                                    text = "En uso por: $errorDuplicado",
                                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.Error, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (codigoActual.isBlank()) {
                            Button(
                                onClick = {
                                    if (!isGenerandoCodigo) {
                                        isGenerandoCodigo = true
                                        coroutineScope.launch {
                                            val nuevo = onGenerarCodigoUnico()
                                            if (nuevo.isNotBlank()) {
                                                inputCodigo = nuevo
                                                onCodigoChange(nuevo)
                                            }
                                            isGenerandoCodigo = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (isGenerandoCodigo) "Generando..." else "Generar FMD",
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 11.sp,
                                            color = Color.Unspecified
                                        )
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { modoEdicionCodigo = !modoEdicionCodigo },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, FDColors.Border),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (modoEdicionCodigo) "Cerrar" else "Cambiar",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }

                // Campo desplegable solo si el usuario pide cambiar el código
                if (modoEdicionCodigo) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputCodigo,
                            onValueChange = { nuevo ->
                                inputCodigo = nuevo.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
                            },
                            placeholder = { Text("Escanear o escribir nuevo código", style = FDType.Caption) },
                            singleLine = true,
                            textStyle = FDType.Body.copy(fontSize = 12.5.sp),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FDColors.Primary,
                                unfocusedBorderColor = FDColors.Border
                            )
                        )

                        Button(
                            onClick = {
                                if (!isGenerandoCodigo) {
                                    isGenerandoCodigo = true
                                    coroutineScope.launch {
                                        val nuevo = onGenerarCodigoUnico()
                                        if (nuevo.isNotBlank()) {
                                            inputCodigo = nuevo
                                            onCodigoChange(nuevo)
                                        }
                                        isGenerandoCodigo = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Surface, contentColor = FDColors.TextPrimary),
                            border = BorderStroke(0.5.dp, FDColors.Border),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = FDColors.Primary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // ── 2. SELECCIÓN DE PRESENTACIÓN COMERCIAL ──
        if (presentacionesValidas.isEmpty()) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Este producto aún no tiene precio de venta. Asigna su precio en la pestaña \"Precios\" para poder imprimir etiquetas comerciales.",
                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PRESENTACIÓN A ETIQUETAR:",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presentacionesValidas.forEach { pres ->
                        val isPresSelected = presentacionSeleccionada == pres
                        Surface(
                            color = if (isPresSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isPresSelected) FDColors.Primary else FDColors.Border),
                            modifier = Modifier
                                .clickable { presentacionSeleccionada = pres }
                                .bounceClick()
                        ) {
                            Text(
                                text = "${pres.nombre.ifBlank { "Presentación" }} (${MonedaHelper.formatearSimple(pres.precioventa)})",
                                style = FDType.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isPresSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPresSelected) FDColors.Primary else FDColors.TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 3. TAMAÑO Y COPIAS EN UNA SOLA LÍNEA ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(true to "Góndola (50x30mm)", false to "Mini (30x20mm)").forEach { (isGondola, label) ->
                    val isSelected = formatoGondola == isGondola
                    Surface(
                        color = if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSelected) FDColors.Primary else FDColors.Border),
                        modifier = Modifier
                            .clickable { formatoGondola = isGondola }
                            .bounceClick()
                    ) {
                        Text(
                            text = label,
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Contador de copias compacto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { if (cantidadCopias > 1) cantidadCopias-- },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, null, tint = FDColors.TextPrimary, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "$cantidadCopias cop.",
                    style = FDType.Heading3.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                )
                IconButton(
                    onClick = { if (cantidadCopias < 50) cantidadCopias++ },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = FDColors.TextPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }

        // ── 4. TARJETA DE VISTA PREVIA ADAPTADA AL TEMA ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tituloEtiqueta = if (presentacionSeleccionada != null) {
                    "${producto.nombre.ifBlank { "Producto" }} (${presentacionSeleccionada?.nombre})"
                } else {
                    producto.nombre.ifBlank { "Producto" }
                }

                val maxCant = producto.presentaciones.maxByOrNull { it.cantidad }?.cantidad ?: 1
                val esFraccion = presentacionSeleccionada != null && (presentacionSeleccionada!!.cantidad < maxCant || producto.presentaciones.size > 1)
                val codLimpio = codigoActual.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
                val codigoParaEtiqueta = if (codLimpio.isBlank()) ""
                else if (presentacionSeleccionada != null && presentacionSeleccionada!!.codigoBarras.isNotBlank()) presentacionSeleccionada!!.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
                else if (esFraccion && presentacionSeleccionada != null && presentacionSeleccionada!!.cantidad > 1) "$codLimpio-B${presentacionSeleccionada!!.cantidad}"
                else if (esFraccion && presentacionSeleccionada != null && presentacionSeleccionada!!.cantidad == 1 && producto.presentaciones.any { it.cantidad > 1 }) "$codLimpio-U1"
                else codLimpio

                if (formatoGondola) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tituloEtiqueta,
                                style = FDType.Heading3.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary),
                                maxLines = 1
                            )
                            Text(
                                text = "${producto.concentracion.ifBlank { "Unidad" }} · Ubic: ${producto.ubicacion.ifBlank { "Mostrador" }}",
                                style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary),
                                maxLines = 1
                            )
                        }

                        if (presentacionSeleccionada != null && presentacionSeleccionada!!.precioventa > 0) {
                            Text(
                                text = MonedaHelper.formatearSimple(presentacionSeleccionada!!.precioventa),
                                style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Success)
                            )
                        } else {
                            Text(
                                text = "Sin precio",
                                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                            )
                        }
                    }

                    // Código de barras Canvas adaptado al tema
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (codigoParaEtiqueta.isNotBlank()) {
                            BarcodeCanvas(
                                codigo = codigoParaEtiqueta,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .fillMaxHeight(),
                                barColor = FDColors.TextPrimary
                            )
                        } else {
                            Text(
                                text = "[ Código no asignado ]",
                                style = FDType.Caption.copy(color = FDColors.TextTertiary, fontSize = 11.sp)
                            )
                        }
                    }

                    if (codigoParaEtiqueta.isNotBlank()) {
                        Text(
                            text = codigoParaEtiqueta,
                            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    // Mini Etiqueta
                    Text(
                        text = tituloEtiqueta,
                        style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary),
                        maxLines = 1
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (codigoParaEtiqueta.isNotBlank()) {
                            BarcodeCanvas(
                                codigo = codigoParaEtiqueta,
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .fillMaxHeight(),
                                barColor = FDColors.TextPrimary
                            )
                        }
                    }

                    if (codigoParaEtiqueta.isNotBlank()) {
                        Text(
                            text = codigoParaEtiqueta,
                            style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextPrimary),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // ── 5. BOTONES DE ACCIÓN (HABILITADOS SOLO SI HAY CÓDIGO Y PRECIO REAL) ──
        val puedeImprimir = codigoActual.isNotBlank() && errorDuplicado == null && presentacionSeleccionada != null && presentacionSeleccionada!!.precioventa > 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (puedeImprimir) {
                        val pdfFile = LabelPdfExporter.generarPdfEtiquetas(
                            context = context,
                            producto = producto,
                            copias = cantidadCopias,
                            formatoGondola = formatoGondola,
                            nombrePresentacion = presentacionSeleccionada!!.nombre,
                            precioVenta = presentacionSeleccionada!!.precioventa
                        )
                        LabelPdfExporter.imprimirPdfDirecto(
                            context = context,
                            pdfFile = pdfFile,
                            nombreTrabajo = "Etiqueta_${producto.nombre.take(15)}",
                            onImpresionConfirmada = { onMarcarEtiquetaImpresa() }
                        )
                    }
                },
                enabled = puedeImprimir,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FDColors.Primary,
                    contentColor = FDColors.PrimaryText,
                    disabledContainerColor = FDColors.SurfaceElevated,
                    disabledContentColor = FDColors.TextTertiary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .bounceClick()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                    Text("Imprimir ($cantidadCopias)", style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.Unspecified))
                }
            }

            OutlinedButton(
                onClick = {
                    if (puedeImprimir) {
                        val pdfFile = LabelPdfExporter.generarPdfEtiquetas(
                            context = context,
                            producto = producto,
                            copias = cantidadCopias,
                            formatoGondola = formatoGondola,
                            nombrePresentacion = presentacionSeleccionada!!.nombre,
                            precioVenta = presentacionSeleccionada!!.precioventa
                        )
                        LabelPdfExporter.compartirPdf(
                            context = context,
                            pdfFile = pdfFile,
                            titulo = "Etiqueta ${producto.nombre}"
                        )
                    }
                },
                enabled = puedeImprimir,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (puedeImprimir) FDColors.Primary else FDColors.Border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .bounceClick()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Share, null, tint = if (puedeImprimir) FDColors.Primary else FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                    Text("Compartir PDF", style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = if (puedeImprimir) FDColors.Primary else FDColors.TextTertiary))
                }
            }
        }
    }
}
