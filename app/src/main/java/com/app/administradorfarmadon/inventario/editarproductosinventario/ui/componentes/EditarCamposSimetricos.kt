package com.app.administradorfarmadon.inventario.editarproductosinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Componentes de entrada con simetría geométrica estricta (Skill 06) y escalado de fuente dinámico.
 * Altura estándar de 52.dp, radio de 8.dp y etiquetas alineadas a 18.dp.
 */

@Composable
fun EditarTextFieldSimetrico(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String = "",
    isModified: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val dynamicFontSize = when {
        value.length > 35 -> 11.5.sp
        value.length > 22 -> 12.5.sp
        else -> 13.5.sp
    }

    Column(modifier = modifier) {
        // Label con altura reservada uniforme (18.dp + 6.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isError) FDColors.Error else if (isFocused) FDColors.Primary else FDColors.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isModified) {
                Text(
                    text = "EDITADO",
                    style = FDType.Caption.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.Warning
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Contenedor geométrico estándar 52.dp para Input de Texto Editable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isError -> FDColors.Error.copy(alpha = 0.02f)
                        isFocused -> FDColors.Primary.copy(alpha = 0.04f)
                        else -> FDColors.Surface
                    }
                )
                .border(
                    width = if (isFocused || isError) 1.5.dp else 0.8.dp,
                    color = when {
                        isError -> FDColors.Error
                        isFocused -> FDColors.Primary
                        else -> FDColors.Border
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

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = FDType.Body.copy(
                        color = FDColors.TextPrimary,
                        fontSize = dynamicFontSize,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FDColors.Primary),
                    decorationBox = { innerTextField ->
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
                        innerTextField()
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
            }
        }

        if (isError && errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = FDType.Caption.copy(color = FDColors.Error, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun EditarDropdownSimetrico(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isModified: Boolean = false,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    val dropdownFontSize = when {
        value.length > 25 -> 11.5.sp
        value.length > 18 -> 12.5.sp
        else -> 13.5.sp
    }

    Column(modifier = modifier) {
        // Label con altura reservada uniforme (18.dp + 6.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expanded) FDColors.Primary else FDColors.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isModified) {
                Text(
                    text = "EDITADO",
                    style = FDType.Caption.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.Warning
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Contenedor geométrico estándar 52.dp para Selector Desplegable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (expanded) FDColors.Primary.copy(alpha = 0.04f) else FDColors.Surface)
                .border(
                    width = if (expanded || isError) 1.5.dp else 0.8.dp,
                    color = when {
                        isError -> FDColors.Error
                        expanded -> FDColors.Primary
                        else -> FDColors.Border
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

                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = if (expanded) FDColors.Primary else FDColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 420.dp)
                    // Selector FIJO y con scroll interno: nunca ocupa toda la pantalla.
                    .heightIn(max = 280.dp)
                    .background(FDColors.SurfaceElevated)
                    .border(1.dp, FDColors.Border, RoundedCornerShape(8.dp))
            ) {
                options.forEach { opt ->
                    val isSelected = opt.equals(value, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                style = FDType.Body.copy(
                                    fontSize = 13.sp,
                                    color = if (isSelected) FDColors.Primary else FDColors.TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        onClick = {
                            onValueChange(opt)
                            expanded = false
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Seleccionado",
                                    tint = FDColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
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

@Composable
fun EditarSwitchSimetrico(
    label: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isModified: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = FDType.Label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FDColors.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isModified) {
                Text(
                    text = "EDITADO",
                    style = FDType.Caption.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.Warning
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = if (checked) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = if (checked) 1.5.dp else 1.dp,
                color = if (checked) FDColors.Primary else FDColors.Border
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onCheckedChange(!checked) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (checked) FDColors.Primary else FDColors.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = FDType.Body.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FDColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = FDType.Caption.copy(
                                fontSize = 10.5.sp,
                                color = FDColors.TextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FDColors.Primary,
                        checkedTrackColor = FDColors.Primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = FDColors.TextTertiary,
                        uncheckedTrackColor = FDColors.SurfaceElevated
                    )
                )
            }
        }
    }
}
