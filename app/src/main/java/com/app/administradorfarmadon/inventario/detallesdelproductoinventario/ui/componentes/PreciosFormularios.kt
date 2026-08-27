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
import com.app.administradorfarmadon.inventario.compartido.modelo.stockFisicoTotalUnidades
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


/**
 * Módulo 2: Configuración Enterprise de Precios, Presentaciones y Fraccionamiento.
 */
@Composable
internal fun TarjetaEditorFormulario(
    pres: PresentacionProducto,
    empaqueBase: String,
    unitMaster: String,
    editable: Boolean,
    empaquesDisponibles: List<String>,
    unidadesDisponibles: List<String>,
    onUpdate: (nombre: String, empaque: String, cantidad: Int, unidadMedida: String, precioventa: Double) -> Unit
) {
    var empaqueState by remember(pres.presentacionId, pres.empaque) { mutableStateOf(pres.empaque.ifBlank { empaqueBase }) }
    var nombreState by remember(pres.presentacionId, pres.nombre) { mutableStateOf(pres.nombre) }
    var cantStr by remember(pres.presentacionId, pres.cantidad) { mutableStateOf(if (pres.cantidad > 0) pres.cantidad.toString() else "") }
    var unidadState by remember(pres.presentacionId, pres.unidadMedida) { mutableStateOf(pres.unidadMedida.ifBlank { unitMaster }) }
    var precioStr by remember(pres.presentacionId, pres.precioventa) { mutableStateOf(if (pres.precioventa > 0) String.format("%.2f", pres.precioventa) else "") }

    val cNum = cantStr.toIntOrNull() ?: 0
    val pNum = precioStr.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // FILA 1: ENVASE Y NOMBRE (50% / 50% - 52dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DropdownFormato(
                label = "Envase *",
                value = empaqueState,
                onValueChange = {
                    empaqueState = it
                    onUpdate(nombreState, empaqueState, cNum, unidadState, pNum)
                },
                options = empaquesDisponibles,
                enabled = editable,
                modifier = Modifier.weight(1f)
            )

            TextFieldFormato(
                label = "Nombre Comercial *",
                value = nombreState,
                onValueChange = {
                    nombreState = it
                    onUpdate(nombreState, empaqueState, cNum, unidadState, pNum)
                },
                placeholder = "Ej: Blíster x 10",
                enabled = editable,
                modifier = Modifier.weight(1f)
            )
        }

        // FILA 2: CONTENIDO Y UNIDAD (50% / 50% - 52dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextFieldFormato(
                label = "Contenido (Solo Núm.) *",
                value = cantStr,
                onValueChange = {
                    val f = it.filter { c -> c.isDigit() }
                    cantStr = f
                    val cantVal = f.toIntOrNull() ?: 0
                    onUpdate(nombreState, empaqueState, cantVal, unidadState, pNum)
                },
                placeholder = "10",
                isNumeric = true,
                keyboardType = KeyboardType.Number,
                textAlign = TextAlign.Center,
                enabled = editable,
                modifier = Modifier.weight(1f)
            )

            DropdownFormato(
                label = "Unidad de Medida *",
                value = unidadState,
                onValueChange = {
                    unidadState = it
                    onUpdate(nombreState, empaqueState, cNum, unidadState, pNum)
                },
                options = unidadesDisponibles,
                enabled = editable,
                modifier = Modifier.weight(1f)
            )
        }

        // FILA 3: PRECIO DE VENTA (100% - 52dp)
        TextFieldFormato(
            label = "Precio de Venta ($) *",
            value = precioStr,
            onValueChange = {
                val f = it.filter { c -> c.isDigit() || c == '.' }
                val parts = f.split(".")
                val clean = if (parts.size > 2) parts[0] + "." + parts.subList(1, parts.size).joinToString("") else f
                precioStr = clean
                val preVal = clean.toDoubleOrNull() ?: 0.0
                onUpdate(nombreState, empaqueState, cNum, unidadState, preVal)
            },
            placeholder = "0.00",
            isNumeric = true,
            keyboardType = KeyboardType.Decimal,
            textAlign = TextAlign.End,
            enabled = editable,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
internal fun DropdownFormato(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expanded) FDColors.Primary else FDColors.TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (expanded) FDColors.SurfaceElevated else FDColors.Surface)
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) FDColors.Primary else FDColors.Border,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { expanded = !expanded }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value.ifBlank { "Seleccionar" },
                    style = FDType.BodySmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (value.isNotBlank()) FDColors.TextPrimary else FDColors.TextTertiary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (expanded) FDColors.Primary else FDColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FDColors.SurfaceElevated)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                style = FDType.BodySmall.copy(
                                    fontWeight = if (opt == value) FontWeight.Bold else FontWeight.Normal,
                                    color = if (opt == value) FDColors.Primary else FDColors.TextPrimary
                                )
                            )
                        },
                        onClick = {
                            onValueChange(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun TextFieldFormato(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    isNumeric: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) FDColors.Primary else FDColors.TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(if (enabled) FDColors.InputBackground else FDColors.InputBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) FDColors.BorderFocus else FDColors.InputBorder,
                    shape = RoundedCornerShape(8.dp)
                ),
            textStyle = FDType.BodySmall.copy(
                fontSize = 13.5.sp,
                color = if (enabled) FDColors.TextPrimary else FDColors.TextTertiary,
                textAlign = textAlign,
                fontWeight = FontWeight.Medium,
                fontFamily = if (isNumeric) FontFamily.Monospace else FontFamily.Default
            ),
            singleLine = true,
            cursorBrush = SolidColor(FDColors.Primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentAlignment = when (textAlign) {
                        TextAlign.End -> Alignment.CenterEnd
                        TextAlign.Center -> Alignment.Center
                        else -> Alignment.CenterStart
                    }
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = FDType.BodySmall.copy(
                                fontSize = 13.5.sp,
                                color = FDColors.TextTertiary, 
                                textAlign = textAlign
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
