package com.app.administradorfarmadon.configuracion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.preferencias_sistema.ui.PreferenciasSistemaPanel
import com.app.administradorfarmadon.disenotemaapp.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

@Composable
fun ConfiguracionScreen(
    onNavigateToSucursales: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToUsuarios: () -> Unit,
    onNavigateToMetodosPago: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val rolActual = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.rol
    val esAdmin = rolActual.equals("Administrador", true) || rolActual.equals("Dueño", true) || rolActual.equals("Dueno", true)
    val esSedePrincipal = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalIdEfectiva.equals("principal", true)
    val puedeGestionarSucursales = esAdmin && esSedePrincipal
    val msgAdmin = "Solo administración puede abrir este módulo."
    val nombreSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.nombreUsuario.ifBlank { "Operador" }
    val sucursalSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre.ifBlank { "Sede Principal" }
    val emailSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.email
    
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
                .padding(horizontal = FDSpacing.xxl, vertical = FDSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = FDSizes.contentMaxWidth)) {
                // Header Enterprise —” Limpio, sin ruido
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                        Text(
                            "Configuración",
                            style = FDType.Display.copy(fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "Centro de control y gestión de la farmacia",
                            style = FDType.Body.copy(color = FDColors.TextSecondary, fontFamily = InterPremium)
                        )
                    }

                    // Botón de salida discreto pero visible
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
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = FDColors.Error, modifier = Modifier.size(FDSizes.iconSm))
                            Text("CERRAR SESIÓN", style = FDType.Label.copy(color = FDColors.Error, fontFamily = InterPremium))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // Banner de Farmacia —” Enterprise Quiet
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.BorderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(FDSpacing.xl).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl)) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(FDShapes.Medium)
                                    .background(FDColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, null, tint = FDColors.PrimaryText, modifier = Modifier.size(FDSizes.iconMd))
                            }
                            Column {
                                Text(
                                    sucursalSesion.uppercase(),
                                    style = FDType.Label.copy(fontFamily = InterPremium)
                                )
                                Text(
                                    nombreSesion,
                                    style = FDType.Heading2.copy(fontFamily = InterPremium)
                                )
                                if (emailSesion.isNotBlank()) {
                                    Text(
                                        emailSesion,
                                        style = FDType.BodySmall.copy(fontFamily = InterPremium)
                                    )
                                }
                            }
                        }

                        // Status Tags
                        Row(horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                            StatusTag(label = "BRIXO VERIFIED", icon = Icons.Default.Verified, color = FDColors.Primary)
                            StatusTag(
                                label = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo,
                                icon = Icons.Default.Payments,
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // Secciones de Configuración
                if (isWide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FDSpacing.xxxl)) {
                        // Columna 1: Gestión Operativa
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FDSpacing.xxl)) {
                            ConfigGestionItems(puedeGestionarSucursales, esAdmin, onNavigateToSucursales, onNavigateToUsuarios, context, msgAdmin)
                            
                            Spacer(Modifier.height(FDSpacing.md))
                            ConfigSectionHeader("PREFERENCIAS DEL SISTEMA")
                            ConfigItemRow(
                                titulo = "Preferencias del Sistema",
                                desc = "Configuración avanzada de la terminal.",
                                icon = Icons.Default.SettingsSuggest,
                                habilitado = true,
                                onClick = { showSistemaPanel = true }
                            )
                        }

                        // Columna 2: Administración
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FDSpacing.xxl)) {
                            ConfigAdminItems(esAdmin, onNavigateToPlan, onNavigateToMetodosPago, context, msgAdmin)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FDSpacing.xxl)) {
                        ConfigGestionItems(puedeGestionarSucursales, esAdmin, onNavigateToSucursales, onNavigateToUsuarios, context, msgAdmin)
                        ConfigAdminItems(esAdmin, onNavigateToPlan, onNavigateToMetodosPago, context, msgAdmin)
                        
                        ConfigSectionHeader("PREFERENCIAS DEL SISTEMA")
                        ConfigItemRow(
                            titulo = "Preferencias del Sistema",
                            desc = "Configuración avanzada de la terminal.",
                            icon = Icons.Default.SettingsSuggest,
                            habilitado = true,
                            onClick = { showSistemaPanel = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FDSpacing.xxxl))

                // Footer
                HorizontalDivider(color = FDColors.Border)
                Spacer(modifier = Modifier.height(FDSpacing.xl))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BRIXO © 2026 · Farmadon Administrador",
                        style = FDType.Caption.copy(fontFamily = InterPremium)
                    )
                    Text(
                        "Versión 2.0.4 - Enterprise Stable",
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
private fun ConfigGestionItems(
    puedeGestionarSucursales: Boolean,
    esAdmin: Boolean,
    onNavigateToSucursales: () -> Unit,
    onNavigateToUsuarios: () -> Unit,
    context: android.content.Context,
    msgAdmin: String
) {
    ConfigSectionHeader("GESTIÓN OPERATIVA")

    ConfigItemRow(
        titulo = "Sucursales y Sedes",
        desc = "Administra tus locales físicos y almacenes.",
        icon = Icons.Default.Storefront,
        habilitado = puedeGestionarSucursales,
        badge = if (!puedeGestionarSucursales) "Sede Principal" else null,
        onClick = onNavigateToSucursales
    )

    ConfigItemRow(
        titulo = "Personal y Roles",
        desc = "Control de acceso, permisos y turnos.",
        icon = Icons.Default.Group,
        habilitado = esAdmin,
        badge = if (!esAdmin) "Solo Admin" else null,
        onClick = { if (esAdmin) onNavigateToUsuarios() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }
    )
}

@Composable
private fun ConfigAdminItems(
    esAdmin: Boolean,
    onNavigateToPlan: () -> Unit,
    onNavigateToMetodosPago: () -> Unit,
    context: android.content.Context,
    msgAdmin: String
) {
    ConfigSectionHeader("ADMINISTRACIÓN Y FACTURACIÓN")

    ConfigItemRow(
        titulo = "Plan y Suscripción",
        desc = "Estado de cuenta, vigencia y límites del plan.",
        icon = Icons.Default.Diamond,
        habilitado = esAdmin,
        onClick = { if (esAdmin) onNavigateToPlan() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }
    )

    ConfigItemRow(
        titulo = "Métodos de Pago",
        desc = "Configura cómo cobran tus cajas.",
        icon = Icons.Default.Payments,
        habilitado = esAdmin,
        onClick = { if (esAdmin) onNavigateToMetodosPago() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }
    )
}

@Composable
private fun ConfigSectionHeader(titulo: String) {
    Text(
        titulo,
        style = FDType.Label.copy(letterSpacing = 1.2.sp, fontFamily = InterPremium),
        color = FDColors.TextTertiary,
        modifier = Modifier.padding(bottom = FDSpacing.xs)
    )
}

@Composable
private fun ConfigItemRow(
    titulo: String,
    desc: String,
    icon: ImageVector,
    habilitado: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (habilitado) onClick() },
        color = if (habilitado) FDColors.Surface else FDColors.Surface.copy(alpha = 0.5f),
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, if (habilitado) FDColors.Border else FDColors.Border.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(FDSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(FDShapes.Small)
                    .background(if (habilitado) FDColors.PrimarySubtle else FDColors.Border),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (habilitado) FDColors.Primary else FDColors.TextDisabled,
                    modifier = Modifier.size(FDSizes.iconMd)
                )
            }
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                    Text(
                        titulo,
                        style = FDType.Heading3.copy(fontFamily = InterPremium),
                        color = if (habilitado) FDColors.TextPrimary else FDColors.TextDisabled
                    )
                    if (badge != null) {
                        Surface(
                            color = FDColors.Border,
                            shape = FDShapes.Full
                        ) {
                            Text(
                                badge.uppercase(),
                                modifier = Modifier.padding(horizontal = FDSpacing.sm, vertical = 2.dp),
                                style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium)
                            )
                        }
                    }
                }
                Text(
                    desc,
                    style = FDType.BodySmall.copy(fontFamily = InterPremium),
                    color = FDColors.TextTertiary
                )
            }
            
            Icon(
                if (habilitado) Icons.Default.ChevronRight else Icons.Default.Lock,
                null,
                tint = FDColors.TextDisabled,
                modifier = Modifier.size(FDSizes.iconSm)
            )
        }
    }
}

@Composable
private fun StatusTag(label: String, icon: ImageVector, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = FDShapes.Full,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = FDSpacing.md, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Text(
                label,
                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = InterPremium)
            )
        }
    }
}

