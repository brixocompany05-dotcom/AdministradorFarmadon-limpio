package com.app.administradorfarmadon.compras.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Campo de búsqueda moderno, espacioso y sin recortes de texto.
 * Elimina los paddings internos excesivos de OutlinedTextField que cortaban la visibilidad
 * del texto escrito por el usuario en diálogos y paneles compactos.
 */
@Composable
fun CampoBuscadorModerno(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    altura: Dp = 48.dp,
    iconoInicio: ImageVector = Icons.Default.Search,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var estaEnfocado by remember { mutableStateOf(false) }

    Surface(
        color = FDColors.InputBackground,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (estaEnfocado) FDColors.BorderFocus else FDColors.InputBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(altura)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = iconoInicio,
                contentDescription = null,
                tint = if (estaEnfocado) FDColors.Primary else FDColors.TextTertiary,
                modifier = Modifier.size(19.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (busqueda.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = FDType.Body.copy(
                            color = FDColors.InputPlaceholder,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = busqueda,
                    onValueChange = onBusquedaChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { estaEnfocado = it.isFocused },
                    textStyle = FDType.Body.copy(
                        color = FDColors.InputText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FDColors.Primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = keyboardActions
                )
            }

            if (busqueda.isNotEmpty()) {
                IconButton(
                    onClick = { onBusquedaChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = FDColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
