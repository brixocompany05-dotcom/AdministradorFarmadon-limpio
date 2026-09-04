package com.app.administradorfarmadon.configuracion.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.preferencias_sistema.ui.PreferenciasSistemaPanel
import com.app.administradorfarmadon.disenotemaapp.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

@Composable
fun ConfiguracionScreen(
    onNavigateToSucursales: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToUsuarios: () -> Unit,
    onNavigateToMetodosPago: () -> Unit,
    onNavigateToPosConfig: () -> Unit = {},
    onNavigateToFacturacion: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val rolActual = SessionManager.rol
    val esAdmin = rolActual.equals("Administrador", true) || rolActual.equals("Dueño", true) || rolActual.equals("Dueno", true)
    val esSedePrincipal = SessionManager.sucursalIdEfectiva.equals("principal", true)
    val puedeGestionarSucursales = esAdmin && esSedePrincipal
    val msgAdmin = "Solo la administración puede acceder a este módulo."
    
    val nombreSesion = SessionManager.nombreUsuario
    val sucursalSesion = SessionManager.sucursalNombre
    val emailSesion = SessionManager.email
    val moneda = SessionManager.monedaCodigo

    var showSistemaPanel by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val isWide = maxWidth >= 840.dp

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FDSpacing.xxl, vertical = FDSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = FDSizes.contentMaxWidth)) {
                
                // 1. ENCABEZADO PRINCIPAL
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                        Text(
                            text = "Configuración",
                            style = FDType.Display.copy(fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Centro de control y gestión de la farmacia",
                            style = FDType.Body.copy(color = FDColors.TextSecondary, fontFamily = InterPremium)
                        )
                    }

                    Surface(
                        onClick = onLogout,
                        color = FDColors.Surface,
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        Row(
                            Modifier.padding(horizontal = FDSpacing.md, vertical = FDSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = FDColors.Error,
                                modifier = Modifier.size(FDSizes.iconSm)
                            )
                            Text(
                                text = "CERRAR SESIÓN",
                                style = FDType.Label.copy(color = FDColors.Error, fontFamily = InterPremium)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xl))

                // 2. BANNER DE INFORMACIÓN REAL DE SEDE Y SESIÓN (Cero Datos Falsos)
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.BorderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .padding(FDSpacing.xl)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(FDShapes.Medium)
                                    .background(FDColors.PrimarySubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏬",
                                    fontSize = 26.sp
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (sucursalSesion.isNotBlank()) {
                                    Text(
                                        text = sucursalSesion.uppercase(),
                                        style = FDType.Caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FDColors.Primary,
                                            fontFamily = InterPremium
                                        )
                                    )
                                }
                                if (nombreSesion.isNotBlank()) {
                                    Text(
                                        text = nombreSesion,
                                        style = FDType.Heading2.copy(fontFamily = InterPremium)
                                    )
                                }
                                if (emailSesion.isNotBlank()) {
                                    Text(
                                        text = emailSesion,
                                        style = FDType.BodySmall.copy(fontFamily = InterPremium)
                                    )
                                }
                            }
                        }

                        // Status Tags 100% Reales
                        Row(horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                            if (rolActual.isNotBlank()) {
                                StatusTag(label = rolActual.uppercase(), emoji = "👤", color = FDColors.Primary)
                            }
                            if (moneda.isNotBlank()) {
                                StatusTag(label = moneda, emoji = "💰", color = FDColors.TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxl))

                // 3. SECCIONES DE CONFIGURACIÓN (CON EMOJIS, SIN ICONOS DEL SIDEBAR, CERO DATOS FALSOS)
                if (isWide) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl)
                    ) {
                        // Columna Izquierda
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
                        ) {
                            // Grupo 1: Organización y Equipo
                            ConfigGroupCard(
                                tituloGrupo = "ORGANIZACIÓN Y EQUIPO",
                                items = {
                                    ConfigEmojiRow(
                                        emoji = "🏢",
                                        titulo = "Sucursales y Sedes",
                                        desc = "Administra tus locales físicos y almacenes.",
                                        habilitado = puedeGestionarSucursales,
                                        badgeText = if (!puedeGestionarSucursales) "Solo Sede Principal" else null,
                                        onClick = onNavigateToSucursales
                                    )
                                    HorizontalDivider(color = FDColors.Border)
                                    ConfigEmojiRow(
                                        emoji = "🛡️",
                                        titulo = "Personal y Roles",
                                        desc = "Control de acceso, permisos y turnos.",
                                        habilitado = esAdmin,
                                        badgeText = if (!esAdmin) "Solo Admin" else null,
                                        onClick = {
                                            if (esAdmin) onNavigateToUsuarios()
                                            else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )

                            // Grupo 3: Cobros y Suscripción
                            ConfigGroupCard(
                                tituloGrupo = "COBROS Y SUSCRIPCIÓN",
                                items = {
                                    ConfigEmojiRow(
                                        emoji = "💳",
                                        titulo = "Métodos de Pago",
                                        desc = "Configura cómo cobran tus cajas.",
                                        habilitado = esAdmin,
                                        badgeText = if (!esAdmin) "Solo Admin" else null,
                                        onClick = {
                                            if (esAdmin) onNavigateToMetodosPago()
                                            else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    HorizontalDivider(color = FDColors.Border)
                                    ConfigEmojiRow(
                                        emoji = "⭐",
                                        titulo = "Plan y Suscripción BRIXO",
                                        desc = "Estado de cuenta, vigencia y límites del plan.",
                                        habilitado = esAdmin,
                                        badgeText = if (!esAdmin) "Solo Admin" else null,
                                        onClick = {
                                            if (esAdmin) onNavigateToPlan()
                                            else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }

                        // Columna Derecha
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
                        ) {
                            // Grupo 2: Reglas de Venta y Caja
                            ConfigGroupCard(
                                tituloGrupo = "REGLAS DE VENTA Y CAJA",
                                items = {
                                    ConfigEmojiRow(
                                        emoji = "🎛️",
                                        titulo = "Reglas del Punto de Venta (POS)",
                                        desc = "Límites de descuentos, topes de caja y arqueo ciego.",
                                        habilitado = esAdmin,
                                        badgeText = if (!esAdmin) "Solo Admin" else null,
                                        onClick = {
                                            if (esAdmin) onNavigateToPosConfig()
                                            else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )

                            // Grupo 4: Terminal y Sistema
                            ConfigGroupCard(
                                tituloGrupo = "TERMINAL Y SISTEMA",
                                items = {
                                    ConfigEmojiRow(
                                        emoji = "🖥️",
                                        titulo = "Preferencias del Sistema",
                                        desc = "Configuración avanzada de la terminal e impresora.",
                                        habilitado = true,
                                        badgeText = null,
                                        onClick = { showSistemaPanel = true }
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Vista Angosta (1 Columna)
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
                    ) {
                        ConfigGroupCard(
                            tituloGrupo = "ORGANIZACIÓN Y EQUIPO",
                            items = {
                                ConfigEmojiRow(
                                    emoji = "🏢",
                                    titulo = "Sucursales y Sedes",
                                    desc = "Administra tus locales físicos y almacenes.",
                                    habilitado = puedeGestionarSucursales,
                                    badgeText = if (!puedeGestionarSucursales) "Solo Sede Principal" else null,
                                    onClick = onNavigateToSucursales
                                )
                                HorizontalDivider(color = FDColors.Border)
                                ConfigEmojiRow(
                                    emoji = "🛡️",
                                    titulo = "Personal y Roles",
                                    desc = "Control de acceso, permisos y turnos.",
                                    habilitado = esAdmin,
                                    badgeText = if (!esAdmin) "Solo Admin" else null,
                                    onClick = {
                                        if (esAdmin) onNavigateToUsuarios()
                                        else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )

                        ConfigGroupCard(
                            tituloGrupo = "REGLAS DE VENTA Y CAJA",
                            items = {
                                ConfigEmojiRow(
                                    emoji = "🎛️",
                                    titulo = "Reglas del Punto de Venta (POS)",
                                    desc = "Límites de descuentos, topes de caja y arqueo ciego.",
                                    habilitado = esAdmin,
                                    badgeText = if (!esAdmin) "Solo Admin" else null,
                                    onClick = {
                                        if (esAdmin) onNavigateToPosConfig()
                                        else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )

                        ConfigGroupCard(
                            tituloGrupo = "COBROS Y SUSCRIPCIÓN",
                            items = {
                                ConfigEmojiRow(
                                    emoji = "💳",
                                    titulo = "Métodos de Pago",
                                    desc = "Configura cómo cobran tus cajas.",
                                    habilitado = esAdmin,
                                    badgeText = if (!esAdmin) "Solo Admin" else null,
                                    onClick = {
                                        if (esAdmin) onNavigateToMetodosPago()
                                        else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                    }
                                )
                                HorizontalDivider(color = FDColors.Border)
                                ConfigEmojiRow(
                                    emoji = "⭐",
                                    titulo = "Plan y Suscripción BRIXO",
                                    desc = "Estado de cuenta, vigencia y límites del plan.",
                                    habilitado = esAdmin,
                                    badgeText = if (!esAdmin) "Solo Admin" else null,
                                    onClick = {
                                        if (esAdmin) onNavigateToPlan()
                                        else Toast.makeText(context, msgAdmin, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )

                        ConfigGroupCard(
                            tituloGrupo = "TERMINAL Y SISTEMA",
                            items = {
                                ConfigEmojiRow(
                                    emoji = "🖥️",
                                    titulo = "Preferencias del Sistema",
                                    desc = "Configuración avanzada de la terminal e impresora.",
                                    habilitado = true,
                                    badgeText = null,
                                    onClick = { showSistemaPanel = true }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxl))

                // 4. PIE DE PÁGINA
                HorizontalDivider(color = FDColors.Border)
                Spacer(modifier = Modifier.height(FDSpacing.md))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRIXO © 2026 · Farmadon Administrador",
                        style = FDType.Caption.copy(fontFamily = InterPremium)
                    )
                    Text(
                        text = "Versión 2.0.4 - Enterprise Stable",
                        style = FDType.Caption.copy(fontFamily = InterPremium)
                    )
                }
            }
        }

        // Panel Lateral de Preferencias
        PreferenciasSistemaPanel(
            isVisible = showSistemaPanel,
            onDismiss = { showSistemaPanel = false }
        )
    }
}

@Composable
private fun ConfigGroupCard(
    tituloGrupo: String,
    items: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
    ) {
        Text(
            text = tituloGrupo,
            style = FDType.Label.copy(
                letterSpacing = 1.2.sp,
                fontFamily = InterPremium,
                fontWeight = FontWeight.Bold
            ),
            color = FDColors.TextTertiary,
            modifier = Modifier.padding(start = FDSpacing.xs, bottom = FDSpacing.xs)
        )
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Medium,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                items()
            }
        }
    }
}

@Composable
private fun ConfigEmojiRow(
    emoji: String,
    titulo: String,
    desc: String,
    habilitado: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (habilitado) onClick() },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = FDSpacing.xl, vertical = FDSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.lg)
        ) {
            // Contenedor de Emoji
            Box(
                Modifier
                    .size(44.dp)
                    .clip(FDShapes.Small)
                    .background(if (habilitado) FDColors.PrimarySubtle else FDColors.Border.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (habilitado) emoji else "🔒",
                    fontSize = 20.sp
                )
            }

            // Título y Descripción
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Text(
                        text = titulo,
                        style = FDType.Heading3.copy(fontFamily = InterPremium),
                        color = if (habilitado) FDColors.TextPrimary else FDColors.TextDisabled
                    )
                    if (badgeText != null) {
                        Surface(
                            color = if (habilitado) FDColors.PrimarySubtle else FDColors.Border.copy(alpha = 0.5f),
                            shape = FDShapes.Full
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                modifier = Modifier.padding(horizontal = FDSpacing.sm, vertical = 2.dp),
                                style = FDType.Caption.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (habilitado) FDColors.Primary else FDColors.TextDisabled,
                                    fontFamily = InterPremium
                                )
                            )
                        }
                    }
                }
                Text(
                    text = desc,
                    style = FDType.BodySmall.copy(fontFamily = InterPremium),
                    color = FDColors.TextTertiary
                )
            }

            // Flecha indicadora
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (habilitado) FDColors.TextTertiary else FDColors.TextDisabled,
                modifier = Modifier.size(FDSizes.iconSm)
            )
        }
    }
}

@Composable
private fun StatusTag(label: String, emoji: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = FDShapes.Full,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = FDSpacing.md, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
        ) {
            Text(text = emoji, fontSize = 11.sp)
            Text(
                text = label,
                style = FDType.Caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = InterPremium
                )
            )
        }
    }
}
