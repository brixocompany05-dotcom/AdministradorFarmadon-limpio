package com.app.administradorfarmadon.configuracion.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.datos.UxSettingsManager
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.datos.TecladoPrefs
import com.app.administradorfarmadon.disenotemaapp.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

@Composable
fun SistemaTiendaPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UxSettingsManager.init(context)
        TecladoPrefs.init(context)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .background(FDColors.Background),
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
                // Header del Panel
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Sistema de la Tienda",
                            style = FDType.Heading2.copy(fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "UX, periféricos y comportamiento",
                            style = FDType.BodySmall.copy(fontFamily = InterPremium),
                            color = FDColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = FDColors.TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // Sección: Dispositivos
                ConfigGroupHeader("DISPOSITIVOS Y PERIFÉRICOS")
                
                SwitchPreference(
                    titulo = "Modo Escáner Externo",
                    desc = "Optimiza para pistolas USB/BT. Activa la misma regla de Bloquear Teclado: ningún campo abrirá el teclado del dispositivo.",
                    icon = Icons.Default.KeyboardHide,
                    checked = TecladoPrefs.bloquearTeclado,
                    onCheckedChange = { TecladoPrefs.bloquearTeclado = it }
                )

                SwitchPreference(
                    titulo = "Impresión Automática",
                    desc = "Envía el ticket a la impresora de inmediato al finalizar una venta exitosa.",
                    icon = Icons.Default.Print,
                    checked = UxSettingsManager.impresionAutomatica,
                    onCheckedChange = { UxSettingsManager.impresionAutomatica = it }
                )

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // Sección: Experiencia de Uso
                ConfigGroupHeader("EXPERIENCIA DE USO (UX)")

                SwitchPreference(
                    titulo = "Modo de Lista Compacta",
                    desc = "Aumenta la densidad de información en inventario y ventas (expertos).",
                    icon = Icons.Default.ViewStream,
                    checked = UxSettingsManager.modoListaCompacta,
                    onCheckedChange = { UxSettingsManager.modoListaCompacta = it }
                )

                SwitchPreference(
                    titulo = "Sonidos de Operación",
                    desc = "Feedback auditivo al escanear, cobrar o detectar errores.",
                    icon = Icons.Default.VolumeUp,
                    checked = UxSettingsManager.sonidosOperacion,
                    onCheckedChange = { UxSettingsManager.sonidosOperacion = it }
                )

                SwitchPreference(
                    titulo = "Confirmar Salida en Ventas",
                    desc = "Muestra una alerta si intentas abandonar una venta con productos cargados.",
                    icon = Icons.Default.Warning,
                    checked = UxSettingsManager.confirmarSalidaVentas,
                    onCheckedChange = { UxSettingsManager.confirmarSalidaVentas = it }
                )

                SwitchPreference(
                    titulo = "Búsqueda Continua",
                    desc = "Mantiene el foco en el buscador tras agregar un producto al carrito.",
                    icon = Icons.Default.Search,
                    checked = UxSettingsManager.busquedaContinua,
                    onCheckedChange = { UxSettingsManager.busquedaContinua = it }
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(FDSpacing.xxl))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(FDSizes.buttonHeight),
                    shape = FDShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                ) {
                    Text("GUARDAR Y CERRAR", style = FDType.Label.copy(color = FDColors.PrimaryText, fontFamily = InterPremium))
                }
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
