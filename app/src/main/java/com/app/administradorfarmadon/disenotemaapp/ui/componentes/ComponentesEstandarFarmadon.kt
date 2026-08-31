package com.app.administradorfarmadon.disenotemaapp.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

/**
 * 
 * COMPONENTES ESTÁNDAR UNIVERSALES DE FARMADON (ENTERPRISE TABLET SAAS 2026)
 * 
 * Garantizan 100% de coherencia matemática, geométrica y cromática en toda la app:
 * - Mismas medidas y radios (10.dp en inputs/botones, 16.dp/18.dp en modales/cards).
 * - Mismas alturas (52.dp en inputs, 48.dp en botones principales).
 * - Mismos colores institucionales (Éxito, Alerta, Peligro, Neutral).
 * - Misma escala tipográfica sin variaciones arbitrarias.
 */

/**
 * Campo de texto Enterprise estandarizado con etiqueta superior, altura geométrica uniforme
 * y manejo coherente de teclados y estados.
 */
@Composable
fun FDCampoTexto(
    valor: String,
    onValorCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    iconoInicio: ImageVector? = null,
    iconoInicioColor: Color? = null,
    iconoFin: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    habilitado: Boolean = true,
    esObligatorio: Boolean = false,
    textoAyuda: String? = null,
    textoError: String? = null
) {
    val colores = TokensFarmadon.colores

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Etiqueta Superior Externa
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (esObligatorio) "$etiqueta *" else etiqueta,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = if (textoError != null) colores.estadoPeligro else colores.textoSecundario
            )
        }

        // Input Box con Medidas Estandarizadas
        OutlinedTextField(
            value = valor,
            onValueChange = onValorCambio,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.5.sp,
                        color = colores.textoTerciario
                    )
                }
            },
            leadingIcon = iconoInicio?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = iconoInicioColor ?: if (valor.isNotBlank()) colores.botonPrimarioFondo else colores.textoTerciario,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = iconoFin,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            maxLines = 1,
            enabled = habilitado,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (textoError != null) colores.estadoPeligro else colores.bordeEnfoque,
                unfocusedBorderColor = if (textoError != null) colores.estadoPeligro else colores.inputBorde,
                focusedContainerColor = colores.inputFondo,
                unfocusedContainerColor = colores.inputFondo,
                focusedTextColor = colores.textoPrincipal,
                unfocusedTextColor = colores.textoPrincipal,
                focusedPlaceholderColor = colores.inputPlaceholder,
                unfocusedPlaceholderColor = colores.inputPlaceholder
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        // Texto de Error o de Ayuda
        if (textoError != null) {
            Text(
                text = textoError,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colores.estadoPeligro
            )
        } else if (textoAyuda != null) {
            Text(
                text = textoAyuda,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontSize = 10.sp,
                    lineHeight = 13.5.sp
                ),
                color = colores.textoTerciario
            )
        }
    }
}

/**
 * Botón Primario Enterprise con altura estándar (48.dp) y estados de carga y bloqueo.
 */
@Composable
fun FDBotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colorFondo: Color = TokensFarmadon.colores.botonPrimarioFondo,
    colorTexto: Color = TokensFarmadon.colores.botonPrimarioTexto
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor = colorTexto,
            disabledContainerColor = colorFondo.copy(alpha = 0.4f),
            disabledContentColor = colorTexto.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(48.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colorTexto,
                strokeWidth = 2.2.dp
            )
        } else {
            if (icono != null) {
                Icon(icono, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = texto,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

/**
 * Botón Secundario / Cancelar con altura estándar (48.dp) y borde sobrio.
 */
@Composable
fun FDBotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true
) {
    val colores = TokensFarmadon.colores
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colores.cardBorde),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colores.textoSecundario
        ),
        modifier = modifier.height(48.dp)
    ) {
        if (icono != null) {
            Icon(icono, null, modifier = Modifier.size(18.dp), tint = colores.textoSecundario)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = texto,
            style = TokensFarmadon.tipografia.etiqueta.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = colores.textoSecundario
        )
    }
}

/**
 * Contenedor Maestro para Diálogos y Modales en Tablet Horizontal.
 * Garantiza proporciones amplias (780dp - 920dp), borde y esquinas redondeadas uniformes.
 */
@Composable
fun FDDialogoContenedor(
    titulo: String,
    subtitulo: String,
    onDismiss: () -> Unit,
    iconoCabecera: ImageVector,
    modifier: Modifier = Modifier,
    anchoMaximo: Dp = 920.dp,
    bloqueado: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val colores = TokensFarmadon.colores

    Dialog(
        onDismissRequest = { if (!bloqueado) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .widthIn(min = 760.dp, max = anchoMaximo)
                .fillMaxWidth(0.90f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            color = colores.fondoModal,
            border = BorderStroke(1.dp, colores.cardBorde)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp)
            ) {
                // Cabecera Estandarizada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colores.botonPrimarioFondo.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconoCabecera,
                                contentDescription = null,
                                tint = colores.botonPrimarioFondo,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = titulo,
                                style = TokensFarmadon.tipografia.titulo1.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = colores.textoPrincipal,
                                maxLines = 1
                            )
                            Text(
                                text = subtitulo,
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 12.5.sp),
                                color = colores.textoTerciario,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !bloqueado,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = colores.textoTerciario
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = colores.divisor.copy(alpha = 0.5f))
                Spacer(Modifier.height(20.dp))

                // Contenido del Diálogo
                contenido()
            }
        }
    }
}

/**
 * Chip / Badge de Estado Institucional Unificado.
 */
enum class TipoEstadoFarmadon {
    EXITO,
    ALERTA,
    PELIGRO,
    NEUTRO
}

@Composable
fun FDBadgeEstado(
    texto: String,
    tipo: TipoEstadoFarmadon,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    val colores = TokensFarmadon.colores
    val (colorFondo, colorTexto) = when (tipo) {
        TipoEstadoFarmadon.EXITO -> colores.exitoSutil to colores.estadoExito
        TipoEstadoFarmadon.ALERTA -> colores.alertaSutil to colores.estadoAlerta
        TipoEstadoFarmadon.PELIGRO -> colores.peligroSutil to colores.estadoPeligro
        TipoEstadoFarmadon.NEUTRO -> colores.neutroSutil to colores.estadoNeutral
    }

    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, colorTexto.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icono != null) {
                Icon(icono, null, tint = colorTexto, modifier = Modifier.size(12.dp))
            }
            Text(
                text = texto,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colorTexto
            )
        }
    }
}
