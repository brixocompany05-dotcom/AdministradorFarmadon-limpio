package com.app.administradorfarmadon.configuracion.preferencias_sistema.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.datos.TecladoPrefs
import com.app.administradorfarmadon.configuracion.preferencias_sistema.impresion.datos.ImpresionPrefs
import com.app.administradorfarmadon.configuracion.preferencias_sistema.ux.datos.UxPrefs
import com.app.administradorfarmadon.disenotemaapp.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

@Composable
fun PreferenciasSistemaPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { 
        TecladoPrefs.init(context)
        ImpresionPrefs.init(context)
        UxPrefs.init(context)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp),
            color = FDColors.Background,
            border = BorderStroke(1.dp, FDColors.Border),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FDSpacing.xxl)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Preferencias del Sistema",
                            style = FDType.Heading2.copy(fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "Configuración avanzada de la terminal",
                            style = FDType.BodySmall.copy(fontFamily = InterPremium),
                            color = FDColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = FDColors.TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // SUBMÓDULO: TECLADO Y ENTRADA
                ConfigGroupHeader("TECLADO Y ENTRADA")
                
                SwitchPreference(
                    titulo = "Bloquear Teclado",
                    desc = "Con esta opción activada, el teclado del dispositivo NUNCA se abrirá en ningún campo de la app, ni siquiera en ventanas emergentes. Ideal para pistolas de escaneo.",
                    icon = Icons.Default.KeyboardHide,
                    checked = TecladoPrefs.bloquearTeclado,
                    onCheckedChange = { TecladoPrefs.bloquearTeclado = it }
                )

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // SUBMÓDULO: IMPRESIÓN
                ConfigGroupHeader("IMPRESIÓN")
                
                SwitchPreference(
                    titulo = "Impresión Automática",
                    desc = "Genera el comprobante físico de inmediato al cobrar.",
                    icon = Icons.Default.Print,
                    checked = ImpresionPrefs.impresionAutomatica,
                    onCheckedChange = { ImpresionPrefs.impresionAutomatica = it }
                )

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // SUBMÓDULO: EXPERIENCIA (UX)
                ConfigGroupHeader("EXPERIENCIA DE USO (UX)")

                SwitchPreference(
                    titulo = "Modo de Lista Compacta",
                    desc = "Mayor densidad de datos para flujos rápidos.",
                    icon = Icons.Default.ViewStream,
                    checked = UxPrefs.modoListaCompacta,
                    onCheckedChange = { UxPrefs.modoListaCompacta = it }
                )

                SwitchPreference(
                    titulo = "Sonidos de Operación",
                    desc = "Alertas auditivas en procesos críticos.",
                    icon = Icons.Default.VolumeUp,
                    checked = UxPrefs.sonidosOperacion,
                    onCheckedChange = { UxPrefs.sonidosOperacion = it }
                )

                SwitchPreference(
                    titulo = "Confirmar Salida en Ventas",
                    desc = "Evita cierres accidentales en el POS.",
                    icon = Icons.Default.Warning,
                    checked = UxPrefs.confirmarSalidaVentas,
                    onCheckedChange = { UxPrefs.confirmarSalidaVentas = it }
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(FDSpacing.xxl))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(FDSizes.buttonHeight),
                    shape = FDShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    Text("CERRAR PANEL", style = FDType.Label.copy(color = FDColors.PrimaryText, fontFamily = InterPremium))
                }
                
                Spacer(modifier = Modifier.height(FDSpacing.md))
                Text(
                    "Los cambios se guardan automáticamente.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = FDType.Caption.copy(fontSize = 10.sp, fontFamily = InterPremium),
                    color = FDColors.TextTertiary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ConfigGroupHeader(titulo: String) {
    Text(
        titulo,
        style = FDType.Label.copy(letterSpacing = 1.2.sp, fontFamily = InterPremium, fontSize = 10.sp),
        color = FDColors.TextTertiary,
        modifier = Modifier.padding(bottom = FDSpacing.md)
    )
}

@Composable
private fun SwitchPreference(
    titulo: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isChecked by remember(checked) { mutableStateOf(checked) }

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = FDSpacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(FDShapes.Small)
                    .background(FDColors.PrimarySubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = FDColors.Primary, modifier = Modifier.size(FDSizes.iconSm))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = FDType.Heading3.copy(fontFamily = InterPremium, fontSize = 14.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    desc,
                    style = FDType.BodySmall.copy(fontFamily = InterPremium, lineHeight = 16.sp),
                    color = FDColors.TextSecondary
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = { 
                    isChecked = it
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FDColors.PrimaryText,
                    checkedTrackColor = FDColors.Primary,
                    uncheckedThumbColor = FDColors.TextDisabled,
                    uncheckedTrackColor = FDColors.Border
                )
            )
        }
    }
}
