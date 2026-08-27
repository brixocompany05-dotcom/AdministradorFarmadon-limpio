package com.app.administradorfarmadon.autenticacion.login.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteAccion
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteTipo
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteUi
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

@Composable
fun LoginIncidenteBubble(
    incidente: LoginIncidenteUi,
    onAccionPrincipal: (LoginIncidenteAccion) -> Unit,
    onAccionSecundaria: (LoginIncidenteAccion) -> Unit,
    modifier: Modifier = Modifier
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
        tonalElevation = 12.dp,
        shadowElevation = 20.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FDColors.Glass),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = incidente.icono,
                    contentDescription = null,
                    tint = when (incidente.tipo) {
                        LoginIncidenteTipo.SIN_INTERNET -> FDColors.Warning
                        else -> FDColors.Error
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incidente.titulo,
                        style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )
                    
                    // Target táctil mínimo 48x48dp: es la acción de escape de un panel bloqueante.
                    IconButton(
                        onClick = { onAccionSecundaria(LoginIncidenteAccion.Descartar) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = incidente.mensaje,
                    style = FDType.BodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = FDColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    incidente.accionSecundaria?.let { accion ->
                        TextButton(
                            onClick = { onAccionSecundaria(accion) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            val label = when (accion) {
                                LoginIncidenteAccion.Descartar -> "Descartar"
                                LoginIncidenteAccion.LimpiarCampos -> "Limpiar"
                                LoginIncidenteAccion.IrARegistro -> "Registro"
                                LoginIncidenteAccion.ContactarSoporte -> "Soporte"
                                else -> "Cerrar"
                            }
                            Text(
                                text = label,
                                style = FDType.Label.copy(fontSize = 12.sp),
                                color = FDColors.TextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = { onAccionPrincipal(incidente.accionPrincipal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        val label = when (incidente.accionPrincipal) {
                            LoginIncidenteAccion.Reintentar -> "REINTENTAR"
                            LoginIncidenteAccion.IrARegistro -> "IR A REGISTRO"
                            LoginIncidenteAccion.ContactarSoporte -> "SOPORTE"
                            LoginIncidenteAccion.CorregirSolicitud -> "CORREGIR"
                            else -> "ACEPTAR"
                        }
                        Text(
                            text = label,
                            style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
