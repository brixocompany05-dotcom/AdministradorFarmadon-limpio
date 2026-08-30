package com.app.administradorfarmadon.disenotemaapp.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

/**
 * Control de Selección Profesional (Estética Tablet SaaS).
 * Rompe el patrón de "input de texto" para ofrecer una experiencia de configuración premium.
 *
 * Diseño Horizontal: [Icono] Etiqueta ——— [Valor Seleccionado] > */
@Composable
fun FDSelectorProfesional(
    etiqueta: String,
    valor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    iconoInicio: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contenidoValor: @Composable (RowScope.() -> Unit)? = null,
    error: String? = null,
    advertencia: String? = null,
    esObligatorio: Boolean = false,) {
    val colores = TokensFarmadon.colores

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = habilitado,
            shape = RoundedCornerShape(12.dp),
            color = if (habilitado) colores.inputFondo else colores.superficieDefecto.copy(alpha = 0.5f),
            border = BorderStroke(
                width = 0.5.dp,
                color = when {
                    error != null -> colores.estadoPeligro
                    advertencia != null -> colores.estadoAlerta
                    else -> colores.inputBorde.copy(alpha = 0.5f)                }
            ),
            tonalElevation = if (habilitado) 1.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .bounceClick()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lado Izquierdo: Icono + Etiqueta
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (iconoInicio != null) {
                        Icon(
                            imageVector = iconoInicio,
                            contentDescription = null,
                            tint = if (habilitado) colores.botonPrimarioFondo else colores.textoTerciario,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Text(
                        text = if (esObligatorio) "$etiqueta *" else etiqueta,
                        style = TokensFarmadon.tipografia.cuerpo.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = if (habilitado) colores.textoPrincipal else colores.textoTerciario
                    )

                    if (!habilitado) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = colores.textoTerciario,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Lado Derecho: Valor Seleccionado + Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (contenidoValor != null) {
                        contenidoValor()
                    } else {
                        Text(
                            text = valor,
                            style = TokensFarmadon.tipografia.cuerpo.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (habilitado && valor.isNotBlank()) colores.botonPrimarioFondo else colores.textoTerciario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colores.textoTerciario.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Feedback de Error o Advertencia
        if (error != null) {
            Text(
                text = error,
                style = TokensFarmadon.tipografia.leyenda,
                color = colores.estadoPeligro,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else if (advertencia != null) {
            Text(
                text = advertencia,
                style = TokensFarmadon.tipografia.leyenda,
                color = colores.estadoAlerta,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
