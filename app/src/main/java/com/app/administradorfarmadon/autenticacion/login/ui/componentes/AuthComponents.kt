package com.app.administradorfarmadon.autenticacion.login.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ExecutiveInput(
    s: MedidaAdaptativa,
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String,
    helpText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    imeAction: ImeAction = ImeAction.Next,
    isProgrammaticScroll: MutableState<Boolean>? = null,
    onImeAction: () -> Unit = {},
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val colores = TokensFarmadon.colores
    val hasError = !errorText.isNullOrBlank()
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            snapshotFlow { ime.getBottom(density) }
                .distinctUntilChanged()
                .collectLatest { bottomInset ->
                    if (bottomInset > 0) {
                        isProgrammaticScroll?.value = true
                        val offset = with(density) { 90.dp.toPx() }
                        bringIntoViewRequester.bringIntoView(
                            Rect(0f, 0f, fieldSize.width.toFloat(), fieldSize.height.toFloat() + offset)
                        )
                        isProgrammaticScroll?.value = false
                    }
                }
        }
    }

    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { fieldSize = it.size },
        verticalArrangement = Arrangement.spacedBy(s.xs * 0.6f)
    ) {
        Text(
            text = label.uppercase(),
            color = if (hasError) colores.estadoPeligro else if (isFocused) colores.textoPrincipal else colores.textoTerciario,
            style = TokensFarmadon.tipografia.etiqueta.copy(
                fontFamily = InterPremium,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                fontSize = s.textLabel.value.sp * 0.92f
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(s.inputMinH)
                .clip(RoundedCornerShape(s.radiusInput))
                .background(
                    color = if (isFocused) colores.inputFondoFoco else colores.inputFondo,
                    shape = RoundedCornerShape(s.radiusInput)
                )
                .border(
                    width = if (hasError) s.borderWidth * 1.2f else if (isFocused) s.borderWidth * 1.2f else s.borderWidth * 0.6f,
                    color = if (hasError) colores.estadoPeligro else if (isFocused) colores.textoPrincipal.copy(alpha = 0.5f) else colores.inputBorde.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(s.radiusInput)
                )
                .padding(horizontal = s.padInputH * 0.78f),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Icon(
                    icon, null,
                    tint = if (hasError) colores.estadoPeligro else if (isFocused) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.6f),
                    modifier = Modifier.size(s.iconSmall)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colores.textoTerciario.copy(alpha = 0.55f),
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontFamily = InterPremium, fontSize = s.textInput.value.sp)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        readOnly = readOnly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused = it.isFocused
                            }
                            .semantics {
                                contentDescription = label
                            },
                        textStyle = TokensFarmadon.tipografia.cuerpo.copy(
                            fontFamily = InterPremium,
                            fontSize = s.textInput.value.sp,
                            color = colores.textoPrincipal,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPassword) KeyboardType.Text else keyboardType,
                            capitalization = if (isPassword) KeyboardCapitalization.None else if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                            imeAction = imeAction,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { onImeAction() },
                            onDone = { onImeAction() },
                            onGo = { onImeAction() },
                            onSend = { onImeAction() }
                        ),
                        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        cursorBrush = SolidColor(colores.textoPrincipal)
                    )
                }
                if (isPassword) {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(s.btnSmallH)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = colores.textoTerciario,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }
                }
                trailing?.invoke()
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = colores.estadoPeligro,
                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = s.xs * 0.5f)
            )
        } else if (!helpText.isNullOrBlank()) {
            Text(
                text = helpText,
                color = colores.textoTerciario,
                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f),
                modifier = Modifier.padding(horizontal = s.xs * 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(s.xs * 0.3f))
    }
}
