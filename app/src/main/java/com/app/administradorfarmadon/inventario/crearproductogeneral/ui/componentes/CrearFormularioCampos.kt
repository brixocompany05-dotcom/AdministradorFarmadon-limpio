package com.app.administradorfarmadon.inventario.crearproductogeneral.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.CrearProductoUiState

/**
 * Panel Izquierdo (60%): Formulario Continuo Enterprise para Crear Producto.
 * Soporta corrección ortográfica estricta sin palabras extra y lector de cámara instantáneo.
 */
@Composable
internal fun FormSectionHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = FDType.Label.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = FDColors.Primary,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = description,
            style = FDType.Caption.copy(
                fontSize = 11.sp,
                color = FDColors.TextTertiary
            )
        )
    }
}

// ── COMPONENTES SIMÉTRICOS CON BORDES NÍTIDOS (52.dp) ──


@Composable
internal fun CrearTextFieldSimetrico(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier) {
        if (label != null) {
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
                        color = if (isError) FDColors.Error else if (isFocused) FDColors.Primary else FDColors.TextSecondary
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FDColors.InputBackground)
                .border(
                    width = if (isFocused || isError) 1.5.dp else 1.dp,
                    color = when {
                        isError -> FDColors.Error
                        isFocused -> FDColors.BorderFocus
                        else -> FDColors.InputBorder
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused || value.isNotBlank()) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                val dynamicFontSize = when {
                    value.length > 35 -> 11.5.sp
                    value.length > 22 -> 12.5.sp
                    else -> 13.5.sp
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onSearch = { onImeAction?.invoke() },
                        onDone = { onImeAction?.invoke() }
                    ),
                    textStyle = FDType.Body.copy(
                        color = FDColors.TextPrimary,
                        fontSize = dynamicFontSize,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FDColors.Primary),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = FDType.Body.copy(
                                    color = FDColors.TextTertiary,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                )

                if (trailingIcon != null) {
                    IconButton(
                        onClick = { onTrailingIconClick?.invoke() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = "Acción",
                            tint = FDColors.Primary,
                            modifier = Modifier.size(19.dp)
                        )
            }
        }
        if (isError && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

        if (isError && errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = errorMessage, style = FDType.Caption.copy(color = FDColors.Error, fontSize = 11.sp))
        }
    }
}


@Composable
internal fun CrearDropdownSimetrico(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String = ""
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
                .background(FDColors.InputBackground)
                .border(
                    width = if (expanded || isError) 1.5.dp else 1.dp,
                    color = when {
                        isError -> FDColors.Error
                        expanded -> FDColors.BorderFocus
                        else -> FDColors.InputBorder
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (expanded || value.isNotBlank()) FDColors.Primary else FDColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    val dropdownFontSize = when {
                        value.length > 25 -> 11.5.sp
                        value.length > 18 -> 12.5.sp
                        else -> 13.5.sp
                    }

                    Text(
                        text = value.ifBlank { placeholder },
                        style = FDType.Body.copy(
                            color = if (value.isBlank()) FDColors.TextTertiary else FDColors.TextPrimary,
                            fontSize = dropdownFontSize,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = if (expanded) FDColors.Primary.copy(alpha = 0.15f) else FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar opciones",
                            tint = if (expanded) FDColors.Primary else FDColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .background(FDColors.SurfaceElevated)
                    .border(1.dp, FDColors.Border, RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    val isSelected = option.equals(value, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = FDType.Body.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) FDColors.Primary else FDColors.TextPrimary
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = FDColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun CrearSwitchSimetrico(
    label: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    color = FDColors.TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onCheckedChange(!checked) },
            shape = RoundedCornerShape(8.dp),
            color = if (checked) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (checked) FDColors.Primary.copy(alpha = 0.4f) else FDColors.Border
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = FDType.Label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checked) FDColors.Primary else FDColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = FDType.Caption.copy(
                            fontSize = 10.sp,
                            color = FDColors.TextTertiary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FDColors.PrimaryText,
                        checkedTrackColor = FDColors.Primary,
                        uncheckedThumbColor = FDColors.TextTertiary,
                        uncheckedTrackColor = FDColors.Background
                    )
                )
            }
        }
    }
}
