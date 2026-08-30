package com.app.administradorfarmadon.autenticacion.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.logica.SolicitudEstadoUi
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun AccesoRestringidoContent(
    s: MedidaAdaptativa,
    message: String,
    onVolverClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(s.gapXLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Warning,
            null,
            tint = FDColors.Warning,
            modifier = Modifier.size(s.iconLarge * 2)
        )
        Text(
            text = "Acceso Restringido",
            color = FDColors.TextPrimary,
            style = TokensFarmadon.tipografia.titulo1
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = TokensFarmadon.formas.grande,
            color = FDColors.Glass,
            border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Border)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(s.padModule),
                style = TokensFarmadon.tipografia.cuerpo,
                color = TokensFarmadon.colores.textoSecundario,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onVolverClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(s.btnLargeH)
                .bounceClick(),
            colors = ButtonDefaults.buttonColors(
                containerColor = FDColors.Primary,
                contentColor = FDColors.PrimaryText
            ),
            shape = TokensFarmadon.formas.completa
        ) {
            Text(
                text = "VOLVER",
                style = TokensFarmadon.tipografia.titulo3,
                color = FDColors.PrimaryText
            )
        }
    }
}

@Composable
internal fun LoadingOverlay(s: MedidaAdaptativa) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Overlay),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = TokensFarmadon.formas.grande,
            color = FDColors.SurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Border)
        ) {
            Column(
                modifier = Modifier.padding(s.padModule * 2),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = FDColors.Primary, strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(s.gapLarge))
                Text(
                    "Conectando con el servidor...",
                    style = TokensFarmadon.tipografia.titulo2,
                    color = FDColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(s.gapSmall))
                Text(
                    "Consultando el estado de tu cuenta...",
                    style = TokensFarmadon.tipografia.cuerpo,
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}

@Composable
internal fun SolicitudEstadoOverlay(
    estado: SolicitudEstadoUi,
    onCerrar: () -> Unit,
    onCorregir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val colorEstado = when {
        estado.estado == "en_revision" -> FDColors.Warning
        estado.correccionSolicitada && estado.fechaCorreccion.isBlank() -> FDColors.Warning
        estado.estado == "rechazada" -> FDColors.Error
        else -> FDColors.Success
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Overlay)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = FDColors.Border,
                    shape = RoundedCornerShape(24.dp)
                ),
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 12.dp,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colorEstado))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "ESTADO DE TU SOLICITUD",
                        style = FDType.Label.copy(letterSpacing = 1.sp),
                        color = FDColors.TextTertiary
                    )
                }

                Text(
                    estado.etiqueta,
                    style = FDType.Heading3.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = colorEstado
                )

                if (estado.mensajeBrixo.isNotBlank()) {
                    Text(
                        estado.mensajeBrixo,
                        style = FDType.BodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = FDColors.TextPrimary
                    )
                } else {
                    Text(
                        when {
                            estado.estado == "en_revision" -> "La central Brixo está revisando tu solicitud. Te avisaremos en cuanto haya novedades."
                            estado.correccionSolicitada && estado.fechaCorreccion.isBlank() -> "Se solicitó corregir algunos datos declarados. Revisa los detalles arriba."
                            estado.estado == "rechazada" -> "Tu solicitud fue rechazada. Contacta a soporte para conocer los detalles."
                            else -> "Tu solicitud fue recibida. La central Brixo la revisará pronto."
                        },
                        style = FDType.BodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = FDColors.TextSecondary
                    )
                }

                if (estado.correccionSolicitada && estado.fechaCorreccion.isBlank()) {
                    Button(
                        onClick = onCorregir,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Warning,
                            contentColor = FDColors.SurfaceElevated
                        ),
                        shape = RoundedCornerShape(s.radiusInput),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.inputMinH)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.EditNote, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "CORREGIR MI SOLICITUD",
                            style = FDType.Label.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCerrar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "CERRAR",
                            style = FDType.Label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
