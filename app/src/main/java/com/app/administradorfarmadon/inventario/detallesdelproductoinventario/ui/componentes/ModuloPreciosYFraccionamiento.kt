package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.animation.*
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.PreciosYFraccionamientoValidator
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import java.util.UUID
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.R
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback
import kotlinx.coroutines.delay

enum class EstadoGuardadoPrecios {
    INACTIVO,
    GUARDANDO,
    EXITO,
    ERROR
}

/**
 * Módulo 2: Configuración Enterprise de Precios, Presentaciones y Fraccionamiento.
 */
@Composable
fun ModuloPreciosYFraccionamiento(
    product: MoldeProductos,
    precioCosto: Double,
    isPrivileged: Boolean,
    onGuardarPrecios: (unidadBase: String, presentaciones: List<PresentacionProducto>, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    val empaqueBase = product.empaque.ifBlank { "Caja" }
    val (cantFromContenido, unitFromContenido) = CatalogoEmpaques.separarContenidoYUnidad(product.contenido)
    val cantMaster = cantFromContenido.toIntOrNull() ?: 1
    val unitMaster = product.contenidoUnidad.ifBlank { unitFromContenido }.ifBlank { product.unidadBase }.ifBlank { empaqueBase }

    // El borrador se siembra UNA vez por producto (clave = id), NO en cada emisión del listener.
    // Así, una actualización en vivo del servidor (stock, lotes, otro campo) jamás borra a
    // medias lo que el usuario está escribiendo aquí. Al guardar, la verdad es este borrador.
    var presentacionesState by remember(product.indice) {
        mutableStateOf(
            if (product.presentaciones.isNotEmpty()) {
                product.presentaciones
            } else {
                listOf(
                    PresentacionProducto(
                        presentacionId = UUID.randomUUID().toString(),
                        nombre = product.nombre.ifBlank { if (cantMaster > 1) "$empaqueBase x $cantMaster" else empaqueBase },
                        empaque = empaqueBase,
                        cantidad = cantMaster,
                        unidadMedida = unitMaster,
                        codigoBarras = product.codigo,
                        precioventa = 0.0
                    )
                )
            }
        )
    }

    // Línea base guardada en base de datos (Firestore) para este producto
    var presentacionesGuardadas by remember(product.indice) {
        mutableStateOf<List<PresentacionProducto>>(product.presentaciones)
    }

    LaunchedEffect(product.presentaciones) {
        if (product.presentaciones.isNotEmpty()) {
            presentacionesGuardadas = product.presentaciones
        }
    }

    var revisionFormulario by remember { mutableIntStateOf(0) }

    val hayCambios = remember(presentacionesState, presentacionesGuardadas) {
        if (presentacionesGuardadas.isNotEmpty()) {
            !sonListasPresentacionesIdenticas(presentacionesState, presentacionesGuardadas)
        } else {
            // Producto nuevo sin política guardada previamente en base de datos:
            // Hay cambios accionables cuando el usuario definió un precio o agregó presentaciones
            presentacionesState.any { it.precioventa > 0.0 } || presentacionesState.size > 1
        }
    }

    var estadoGuardado by remember { mutableStateOf(EstadoGuardadoPrecios.INACTIVO) }
    var mensajeErrorGuardado by remember { mutableStateOf("") }

    val ejecutarGuardado = {
        estadoGuardado = EstadoGuardadoPrecios.GUARDANDO
        mensajeErrorGuardado = ""
        onGuardarPrecios(unitMaster, presentacionesState) { result ->
            result.fold(
                onSuccess = {
                    estadoGuardado = EstadoGuardadoPrecios.EXITO
                    presentacionesGuardadas = presentacionesState.map { it.copy() }
                    revisionFormulario++
                },
                onFailure = { error ->
                    estadoGuardado = EstadoGuardadoPrecios.ERROR
                    mensajeErrorGuardado = error.message ?: "No se pudo sincronizar la política de precios."
                }
            )
        }
    }

    LaunchedEffect(estadoGuardado) {
        if (estadoGuardado == EstadoGuardadoPrecios.EXITO) {
            delay(1800)
            estadoGuardado = EstadoGuardadoPrecios.INACTIVO
        }
    }

    var mostrarAlertaConfirmacionPerdida by remember { mutableStateOf(false) }

    // Misma lengua que la pestaña Lotes: DISPONIBLE (sin cuarentena), en unidades físicas.
    val totalStockUnidades = remember(product.lotes) { product.stockDisponibleFisico }

    val costoUnitarioBase = if (precioCosto > 0) precioCosto
    else if (product.precioCompra > 0) product.precioCompra
    else 0.0

    val lotesOrdenadosFEFO = remember(product.lotes) {
        product.lotes.values
            .filter { it.cantidad > 0 }
            .sortedBy { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE }
    }
    val lotePrioritario = lotesOrdenadosFEFO.firstOrNull()

    // Blindaje: límite es UNIDADES POR EMPAQUE (contenido), no mg de concentración
    val limiteContenido = cantMaster.coerceAtLeast(1)

    // ── VALIDACIÓN PURA EN TIEMPO REAL ──
    val validacion = remember(presentacionesState, costoUnitarioBase, limiteContenido, product.permiteFraccionar) {
        PreciosYFraccionamientoValidator.validar(
            presentaciones = presentacionesState,
            costoBaseUnitario = costoUnitarioBase,
            permiteFraccionar = product.permiteFraccionar,
            limiteContenidoMaestro = limiteContenido
        )
    }

    if (mostrarAlertaConfirmacionPerdida) {
        AlertDialog(
            onDismissRequest = { if (estadoGuardado != EstadoGuardadoPrecios.GUARDANDO) mostrarAlertaConfirmacionPerdida = false },
            title = { Text("Advertencia Comercial", style = FDType.Heading2.copy(color = FDColors.Warning)) },
            text = { Text("Se detectó venta a pérdida en una o más presentaciones (precio inferior al costo de compra). ¿Deseas autorizar esta política de precios?", style = FDType.Body) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarAlertaConfirmacionPerdida = false
                        ejecutarGuardado()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Warning, contentColor = FDColors.PrimaryText)
                ) {
                    Text("Autorizar y Guardar")
                }
            },
            dismissButton = { 
                OutlinedButton(
                    onClick = { mostrarAlertaConfirmacionPerdida = false }
                ) { 
                    Text("Revisar Precios") 
                } 
            }
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    if (selectedIndex >= presentacionesState.size) {
        selectedIndex = (presentacionesState.size - 1).coerceAtLeast(0)
    }
    val selectedPres = presentacionesState.getOrNull(selectedIndex)

    val empaquesDisponibles = remember(product.empaque, product.contenidoUnidad) {
        val list = product.sugerenciasEnvase.ifEmpty {
            CatalogoEmpaques.obtenerEmpaquesCompatibles(product.empaque, product.contenidoUnidad.ifBlank { product.unidadBase })
        }
        if (list.contains(product.empaque)) list else listOf(product.empaque) + list
    }
    val unidadesDisponibles = remember(product.empaque, product.contenidoUnidad) {
        val list = product.sugerenciasPerfil.ifEmpty {
            CatalogoEmpaques.obtenerUnidadesCompatibles(product.empaque, product.contenidoUnidad.ifBlank { product.unidadBase })
        }
        val u = product.contenidoUnidad.ifBlank { product.unidadBase }
        if (u.isNotBlank() && !list.contains(u)) listOf(u) + list else list
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1.1f).fillMaxHeight(),
            color = FDColors.Background,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PRESENTACIONES (${presentacionesState.size})",
                                style = FDType.Heading3.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Costo base: ${MonedaHelper.formatearSimple(costoUnitarioBase)}",
                                style = FDType.Caption.copy(color = FDColors.TextSecondary)
                            )
                        }
                    }

                    HorizontalDivider(color = FDColors.Border)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        presentacionesState.forEachIndexed { index, pres ->
                            val isSelected = index == selectedIndex
                            val pNum = pres.precioventa
                            val cNum = pres.cantidad
                            val costoUnitarioFisico = if (limiteContenido > 1) costoUnitarioBase / limiteContenido else costoUnitarioBase
                            val costo = costoUnitarioFisico * cNum
                            val ganancia = pNum - costo
                            val margen = if (pNum > 0 && ganancia > 0) (ganancia / pNum) * 100 else 0.0
                            val erroresFila = validacion.erroresPorId[pres.presentacionId] ?: emptyList()

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIndex = index },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) FDColors.SurfaceElevated else FDColors.Surface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (erroresFila.any { it.esBloqueante }) FDColors.Error 
                                            else if (isSelected) FDColors.Primary 
                                            else FDColors.Border
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val esEnvaseCompleto = index == 0 || (pres.cantidad >= cantMaster && cantMaster > 1)
                                        val badgeTexto = when {
                                            esEnvaseCompleto -> "ENVASE COMPLETO"
                                            pres.cantidad == 1 -> "UNIDAD"
                                            else -> "FRACCIÓN (x${pres.cantidad})"
                                        }
                                        Surface(
                                            color = if (esEnvaseCompleto) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Border.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = badgeTexto,
                                                style = FDType.Caption.copy(
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (esEnvaseCompleto) FDColors.Primary else FDColors.TextSecondary
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = if (pNum > 0) MonedaHelper.formatearSimple(pNum) else "$ 0.00",
                                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary)
                                        )
                                    }

                                    Text(
                                        text = pres.nombre.ifBlank { if (index == 0) "$empaqueBase Master" else "Presentación #${index + 1}" },
                                        style = FDType.BodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = FDColors.TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pres.cantidad} ${pres.unidadMedida.ifBlank { unitMaster }} · ${pres.empaque.ifBlank { empaqueBase }}",
                                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                                        )

                                        if (pNum > 0) {
                                            Text(
                                                text = "${String.format("%.1f", margen)}% mrg",
                                                style = FDType.Caption.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ganancia < 0) FDColors.Error else FDColors.Success
                                                )
                                            )
                                        }
                                    }

                                    if (erroresFila.isNotEmpty()) {
                                        Text(
                                            text = erroresFila.first().mensaje,
                                            style = FDType.Caption.copy(
                                                fontSize = 10.sp,
                                                color = if (erroresFila.first().esBloqueante) FDColors.Error else FDColors.Warning
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isPrivileged) {
                        val esSellado = !product.permiteFraccionar
                        OutlinedButton(
                            onClick = {
                                if (esSellado) return@OutlinedButton
                                val sugerenciaEmpaque = empaquesDisponibles.firstOrNull { it != empaqueBase } ?: "Fracción"
                                val sugerenciaUnidad = unidadesDisponibles.firstOrNull() ?: unitMaster
                                val nueva = PresentacionProducto(
                                    presentacionId = UUID.randomUUID().toString(),
                                    nombre = "",
                                    empaque = sugerenciaEmpaque,
                                    cantidad = 1,
                                    unidadMedida = sugerenciaUnidad,
                                    precioventa = 0.0
                                )
                                val lista = (presentacionesState + nueva).toMutableList()
                                presentacionesState = lista
                                selectedIndex = lista.size - 1
                            },
                            enabled = !esSellado,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, if (esSellado) FDColors.Border.copy(alpha=0.5f) else FDColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (esSellado) FDColors.TextTertiary else FDColors.TextPrimary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = if (esSellado) FDColors.TextTertiary else FDColors.TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(if (esSellado) "PRODUCTO SELLADO —” NO SE FRACCIONA" else "AÑADIR OTRA PRESENTACIÓN", style = FDType.Label.copy(fontSize = 11.sp, color = if (esSellado) FDColors.TextTertiary else FDColors.TextPrimary))
                        }
                    }
                }

                Surface(
                    color = FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, FDColors.Border),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("STOCK DISPONIBLE", style = FDType.Caption.copy(fontSize = 9.5.sp))
                            Text("${totalStockUnidades.toInt()} $empaqueBase", style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("VALOR VENTA ESTIMADO", style = FDType.Caption.copy(fontSize = 9.5.sp))
                            // R3: usar el precio de la presentación de MAYOR contenido (envase completo),
                            // no la primera fila (que puede ser una fracción barata y dar un número engañoso).
                            val presPrincipalEstimado = presentacionesState.maxByOrNull { it.cantidad } ?: presentacionesState.firstOrNull()
                            val precioBase = presPrincipalEstimado?.precioventa ?: 0.0
                            Text(MonedaHelper.formatearSimple(totalStockUnidades * precioBase), style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary))
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            if (selectedPres != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedIndex == 0) "FORMATO BASE (#1)" else "FRACCIÓN (#${selectedIndex + 1})",
                                style = FDType.Heading3.copy(fontWeight = FontWeight.Bold)
                            )

                            val isBase = if (product.presentacionPrincipalId.isNotBlank()) {
                                selectedPres.presentacionId == product.presentacionPrincipalId
                            } else {
                                selectedIndex == 0
                            }
                            if (!isBase && isPrivileged && presentacionesState.size > 1) {
                                OutlinedButton(
                                    onClick = {
                                        val lista = presentacionesState.toMutableList()
                                        lista.removeAt(selectedIndex)
                                        presentacionesState = lista
                                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Error),
                                    border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Eliminar", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        HorizontalDivider(color = FDColors.Border)

                        key(selectedPres.presentacionId, revisionFormulario) {
                            TarjetaEditorFormulario(
                                pres = selectedPres,
                                empaqueBase = empaqueBase,
                                unitMaster = unitMaster,
                                editable = isPrivileged,
                                empaquesDisponibles = empaquesDisponibles,
                                unidadesDisponibles = unidadesDisponibles,
                                onUpdate = { nuevoNombre, nuevoEmpaque, nuevaCant, nuevaUnidad, nuevoPrecio ->
                                    val lista = presentacionesState.toMutableList()
                                    lista[selectedIndex] = selectedPres.copy(
                                        nombre = nuevoNombre,
                                        empaque = nuevoEmpaque,
                                        cantidad = nuevaCant,
                                        unidadMedida = nuevaUnidad,
                                        precioventa = nuevoPrecio
                                    )
                                    presentacionesState = lista
                                }
                            )
                        }

                        val pNum = selectedPres.precioventa
                        val cNum = selectedPres.cantidad
                        val costoUnitarioFisico = if (limiteContenido > 1) costoUnitarioBase / limiteContenido else costoUnitarioBase
                        val costo = costoUnitarioFisico * cNum
                        val ganancia = pNum - costo
                        val margen = if (pNum > 0 && ganancia > 0) (ganancia / pNum) * 100 else 0.0

                        Surface(
                            color = FDColors.Background,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, FDColors.Border)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "LIQUIDACIÓN EN VIVO",
                                    style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("COSTO PROPORCIONAL", style = FDType.Caption.copy(fontSize = 10.sp))
                                        Text(MonedaHelper.formatearSimple(costo), style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("UTILIDAD POR VENTA", style = FDType.Caption.copy(fontSize = 10.sp))
                                        Text(
                                            text = MonedaHelper.formatearSimple(ganancia),
                                            style = FDType.BodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (ganancia < 0) FDColors.Error else FDColors.Success
                                            )
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("MARGEN COMERCIAL", style = FDType.Caption.copy(fontSize = 10.sp))
                                        Surface(
                                            color = if (ganancia < 0) FDColors.Error.copy(alpha = 0.15f) else FDColors.Success.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${String.format("%.1f", margen)}%",
                                                style = FDType.Caption.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ganancia < 0) FDColors.Error else FDColors.Success
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = FDColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    val unitTxt = selectedPres.unidadMedida.ifBlank { unitMaster }
                                    val tipoConsumoTexto = when {
                                        cNum >= cantMaster && cantMaster > 1 -> " (Equivale a 1 $empaqueBase completo)"
                                        cNum == 1 -> " (Venta suelta al menudeo)"
                                        else -> " (Fracción de $cNum $unitTxt)"
                                    }
                                    Text(
                                        text = "Consumo de Lote: Cada venta descuenta $cNum $unitTxt físicas del stock disponible$tipoConsumoTexto.",
                                        style = FDType.Caption.copy(
                                            fontSize = 11.sp,
                                            color = FDColors.TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        // ERRORES O ALERTAS
                        val erroresFila = validacion.erroresPorId[selectedPres.presentacionId] ?: emptyList()
                        erroresFila.forEach { err ->
                            Surface(
                                color = (if (err.esBloqueante) FDColors.Error else FDColors.Warning).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, if (err.esBloqueante) FDColors.Error else FDColors.Warning)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (err.esBloqueante) Icons.Outlined.ErrorOutline else Icons.Outlined.WarningAmber,
                                        null,
                                        tint = if (err.esBloqueante) FDColors.Error else FDColors.Warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = err.mensaje,
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (err.esBloqueante) FDColors.Error else FDColors.Warning
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // BOTÓN PRIMARIO FIJO AL FONDO
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (validacion.hayVentaAPerdida) {
                            Surface(
                                color = FDColors.Warning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, FDColors.Warning.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.WarningAmber, null, tint = FDColors.Warning, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Atención: Tienes presentaciones vendiéndose a pérdida.",
                                        style = FDType.Caption.copy(color = FDColors.Warning, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else if (costoUnitarioBase <= 0.0) {
                            Surface(
                                color = FDColors.Warning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, FDColors.Warning.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.WarningAmber, null, tint = FDColors.Warning, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Sin costo de compra cargado: el margen no es real. Registra el costo en 'Agregar stock' para ver la utilidad verdadera.",
                                        style = FDType.Caption.copy(color = FDColors.Warning, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                        val textoBoton = if (presentacionesGuardadas.isNotEmpty()) {
                            "GUARDAR CAMBIOS"
                        } else {
                            "GUARDAR POLÍTICA DE PRECIOS"
                        }

                        AnimatedContent(
                            targetState = (hayCambios || estadoGuardado == EstadoGuardadoPrecios.GUARDANDO),
                            label = "TransicionAccionPrecios"
                        ) { conAccionPendiente ->
                            if (conAccionPendiente) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (presentacionesGuardadas.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                presentacionesState = presentacionesGuardadas.map { it.copy() }
                                                revisionFormulario++
                                            },
                                            enabled = estadoGuardado == EstadoGuardadoPrecios.INACTIVO,
                                            modifier = Modifier.weight(0.35f).height(48.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextSecondary)
                                        ) {
                                            Text(
                                                text = "DESCARTAR",
                                                style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (validacion.hayVentaAPerdida) {
                                                mostrarAlertaConfirmacionPerdida = true
                                            } else {
                                                ejecutarGuardado()
                                            }
                                        },
                                        enabled = isPrivileged && estadoGuardado == EstadoGuardadoPrecios.INACTIVO && validacion.esValidoParaGuardar,
                                        modifier = Modifier
                                            .weight(if (presentacionesGuardadas.isNotEmpty()) 0.65f else 1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FDColors.Primary,
                                            contentColor = FDColors.PrimaryText
                                        )
                                    ) {
                                        if (estadoGuardado == EstadoGuardadoPrecios.GUARDANDO) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = FDColors.PrimaryText
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "GUARDANDO...",
                                                style = FDType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            )
                                        } else {
                                            Text(
                                                text = textoBoton,
                                                style = FDType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    color = if (presentacionesGuardadas.isNotEmpty()) FDColors.Success.copy(alpha = 0.08f) else FDColors.SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (presentacionesGuardadas.isNotEmpty()) FDColors.Success.copy(alpha = 0.3f) else FDColors.Border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            if (presentacionesGuardadas.isNotEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = if (presentacionesGuardadas.isNotEmpty()) FDColors.Success else FDColors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (presentacionesGuardadas.isNotEmpty()) {
                                                "Política de precios vigente · Sin cambios pendientes"
                                            } else {
                                                "Ingresa un precio de venta para activar la política"
                                            },
                                            style = FDType.BodySmall.copy(
                                                fontWeight = if (presentacionesGuardadas.isNotEmpty()) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (presentacionesGuardadas.isNotEmpty()) FDColors.Success else FDColors.TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    PreciosGuardadoDialog(
        estadoGuardado = estadoGuardado,
        mensajeError = mensajeErrorGuardado,
        productoNombre = product.nombre,
        presentacionesCount = presentacionesState.size,
        onDismiss = { estadoGuardado = EstadoGuardadoPrecios.INACTIVO },
        onReintentar = { ejecutarGuardado() },
        onVerFresco = {
            // Verdad vigente: recarga borrador con lo fresco del servidor sin perder tu trabajo previo del todo — 1 toque
            presentacionesState = if (product.presentaciones.isNotEmpty()) product.presentaciones else presentacionesState
            presentacionesGuardadas = product.presentaciones
            revisionFormulario++
            estadoGuardado = EstadoGuardadoPrecios.INACTIVO
            mensajeErrorGuardado = ""
        }
    )
}

/**
 * Validador de equivalencia entre la política en memoria y la guardada en base de datos.
 */
private fun sonListasPresentacionesIdenticas(
    actual: List<PresentacionProducto>,
    guardada: List<PresentacionProducto>
): Boolean {
    if (actual.size != guardada.size) return false
    for (i in actual.indices) {
        val a = actual[i]
        val g = guardada[i]
        if (a.nombre.trim() != g.nombre.trim()) return false
        if (a.empaque.trim() != g.empaque.trim()) return false
        if (a.cantidad != g.cantidad) return false
        if (a.unidadMedida.trim() != g.unidadMedida.trim()) return false
        if (kotlin.math.abs(a.precioventa - g.precioventa) > 0.0001) return false
        if (a.codigoBarras.trim() != g.codigoBarras.trim()) return false
    }
    return true
}
