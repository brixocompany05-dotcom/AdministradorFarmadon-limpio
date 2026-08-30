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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
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
    val s = recordarMedidaAdaptativa()
    val colores = TokensFarmadon.colores
    val context = androidx.compose.ui.platform.LocalContext.current

    val rolActual = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.rol
    val esAdmin = rolActual.equals("Administrador", true) || rolActual.equals("Dueño", true) || rolActual.equals("Dueno", true)
    val msgAdmin = "Solo administración puede abrir este módulo."
    val nombreSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.nombreUsuario.ifBlank { "Operador" }
    val sucursalSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre.ifBlank { "Sede Principal" }
    val emailSesion = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.email
    val iniciales = nombreSesion.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "OP" }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colores.fondoBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val isWide = maxWidth >= 900.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = s.padScreenH, vertical = s.padScreenV).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
        ) {
            // Header premium —” 1 línea, sin laberinto
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text("Configuración", style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp, fontFamily = InterPremium), color = colores.textoPrincipal)
                    Text("Centro de control de tu farmacia", style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.95f, fontFamily = InterPremium), color = colores.textoSecundario)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                    Surface(color = colores.cardBase, shape = RoundedCornerShape(s.radiusButton), border = BorderStroke(s.borderWidth, colores.cardBorde)) {
                        Row(Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.8f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                            Box(Modifier.size(s.iconMedium + 6.dp).clip(CircleShape).background(colores.textoPrincipal), contentAlignment = Alignment.Center) {
                                Text(iniciales, style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.textoInvertido)
                            }
                            Column {
                                Text(nombreSesion, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp * 0.92f, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = colores.textoPrincipal, maxLines = 1)
                                Text("$rolActual · $sucursalSesion", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.80f, fontFamily = InterPremium), color = colores.textoTerciario, maxLines = 1)
                            }
                        }
                    }
                    Surface(onClick = onLogout, color = colores.cardBase, shape = RoundedCornerShape(s.radiusButton), border = BorderStroke(s.borderWidth, colores.cardBorde)) {
                        Row(Modifier.padding(horizontal = s.sm, vertical = s.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = colores.estadoPeligro, modifier = Modifier.size(s.iconTiny))
                            Text("SALIR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.estadoPeligro)
                        }
                    }
                }
            }

            // Hero farmacia —” compacto premium con métricas —” sin caja anidada gruesa
            Surface(
                color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard), border = BorderStroke(s.borderWidth, colores.cardBorde), shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(s.padCard).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                        Box(Modifier.size(s.iconLarge + 22.dp).clip(RoundedCornerShape(s.radiusChip + 2.dp)).background(colores.textoPrincipal), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, null, tint = colores.textoInvertido, modifier = Modifier.size(s.iconMedium))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("FARMACIA OPERATIVA", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.76f, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, fontFamily = InterPremium), color = colores.textoTerciario)
                            Text(nombreSesion, style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textSubtitle.value.sp * 0.96f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.textoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (emailSesion.isNotBlank()) Text(emailSesion, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.80f, fontFamily = InterPremium), color = colores.textoTerciario, maxLines = 1)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(s.xs), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = colores.fondoBase, shape = RoundedCornerShape(100.dp), border = BorderStroke(s.borderWidth, colores.cardBorde)) {
                            Row(Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.6f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                                Icon(Icons.Default.Payments, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                Text("${com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo} ${com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo}", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.88f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.textoPrincipal)
                            }
                        }
                        Surface(color = colores.textoPrincipal, shape = RoundedCornerShape(100.dp)) {
                            Row(Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.6f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                                Icon(Icons.Default.Verified, null, tint = colores.textoInvertido, modifier = Modifier.size(s.iconTiny))
                                Text("BRIXO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.textoInvertido)
                            }
                        }
                    }
                }
            }

            Text("CONTROL TOTAL", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterPremium), color = colores.textoTerciario, modifier = Modifier.padding(start = s.xs * 0.45f, top = s.xs))

            if (isWide) {
                Column(verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                        Box(Modifier.weight(1f)) {
                            BentoCard(
                                numero = "01", titulo = "Sucursales", subtitulo = "Tus locales físicos", desc = "Direcciones, mapas y límites del plan",
                                icono = Icons.Default.Storefront, accent = colores.estadoExito, habilitado = esAdmin, badge = if (esAdmin) null else "Admin",
                                onClick = { if (esAdmin) onNavigateToSucursales() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            BentoCard(
                                numero = "02", titulo = "Plan y pagos", subtitulo = "Suscripción BRIXO", desc = "Vigencia, pagos y comprobantes",
                                icono = Icons.Default.Diamond, accent = colores.textoPrincipal, habilitado = esAdmin, badge = if (esAdmin) null else "Admin",
                                onClick = { if (esAdmin) onNavigateToPlan() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                        Box(Modifier.weight(1f)) {
                            BentoCard(
                                numero = "03", titulo = "Personal", subtitulo = "Quién atiende", desc = "Roles, sedes y permisos",
                                icono = Icons.Default.Group, accent = colores.estadoAlerta, habilitado = esAdmin, badge = if (esAdmin) null else "Admin",
                                onClick = { if (esAdmin) onNavigateToUsuarios() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            BentoCard(
                                numero = "04", titulo = "Métodos de pago", subtitulo = "Cómo paga tu empresa", desc = "Efectivo, Yape, transferencias…",
                                icono = Icons.Default.Payments, accent = colores.estadoExito, habilitado = esAdmin, badge = if (esAdmin) null else "Admin",
                                onClick = { if (esAdmin) onNavigateToMetodosPago() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                    BentoCard(numero = "01", titulo = "Sucursales", subtitulo = "Tus locales físicos", desc = "Direcciones, mapas y límites", icono = Icons.Default.Storefront, accent = colores.estadoExito, habilitado = esAdmin, badge = if (esAdmin) null else "Admin", onClick = { if (esAdmin) onNavigateToSucursales() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s)
                    BentoCard(numero = "02", titulo = "Plan y pagos", subtitulo = "Suscripción BRIXO", desc = "Vigencia, pagos y comprobantes", icono = Icons.Default.Diamond, accent = colores.textoPrincipal, habilitado = esAdmin, badge = if (esAdmin) null else "Admin", onClick = { if (esAdmin) onNavigateToPlan() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s)
                    BentoCard(numero = "03", titulo = "Personal", subtitulo = "Quién atiende", desc = "Roles, sedes y permisos", icono = Icons.Default.Group, accent = colores.estadoAlerta, habilitado = esAdmin, badge = if (esAdmin) null else "Admin", onClick = { if (esAdmin) onNavigateToUsuarios() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s)
                    BentoCard(numero = "04", titulo = "Métodos de pago", subtitulo = "Cómo paga tu empresa", desc = "Efectivo, Yape, transferencias…", icono = Icons.Default.Payments, accent = colores.estadoExito, habilitado = esAdmin, badge = if (esAdmin) null else "Admin", onClick = { if (esAdmin) onNavigateToMetodosPago() else android.widget.Toast.makeText(context, msgAdmin, android.widget.Toast.LENGTH_SHORT).show() }, s = s)
                }
            }

            Surface(color = colores.cardBase.copy(alpha = 0.55f), shape = RoundedCornerShape(s.radiusChip), border = BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = s.padCard, vertical = s.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                        Icon(Icons.Default.Shield, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                        Text("BRIXO · Central de control", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = colores.textoTerciario)
                    }
                    Text("v2026.05", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.72f, fontFamily = InterPremium), color = colores.textoTerciario.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun BentoCard(
    numero: String,
    titulo: String,
    subtitulo: String,
    desc: String,
    icono: ImageVector,
    accent: Color,
    habilitado: Boolean,
    badge: String?,
    onClick: () -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    val colores = TokensFarmadon.colores
    // Quiet premium: altura geométrica s.btnLargeH*2.65 (~143dp base) clamp 124-164 —” nunca 148.dp fijo
    val cardH = (s.btnLargeH * 2.65f).coerceIn(124.dp, 164.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().height(cardH).clip(RoundedCornerShape(s.radiusCard)).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusCard)).clickable { onClick() },
        shape = RoundedCornerShape(s.radiusCard),
        color = if (habilitado) colores.cardBase else colores.cardBase.copy(alpha = 0.58f),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxSize().padding(s.padCard), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = 0.11f), shape = RoundedCornerShape(s.radiusChip), border = BorderStroke(s.borderWidth * 0.7f, accent.copy(alpha = 0.18f))) {
                    Text(numero, modifier = Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.4f), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = accent)
                }
                Box(Modifier.size(s.iconLarge + 8.dp).clip(RoundedCornerShape(s.radiusChip)).background(accent.copy(alpha = 0.10f)).border(s.borderWidth * 0.6f, accent.copy(alpha = 0.16f), RoundedCornerShape(s.radiusChip)), contentAlignment = Alignment.Center) {
                    Icon(icono, null, tint = if (habilitado) accent else colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                    Text(titulo, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.92f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = if (habilitado) colores.textoPrincipal else colores.textoSecundario)
                    if (!badge.isNullOrBlank()) {
                        Surface(color = colores.cardElevada, shape = RoundedCornerShape(100.dp), border = BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)) {
                            Text(badge.uppercase(), modifier = Modifier.padding(horizontal = s.xs, vertical = 2.dp), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.68f, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = colores.textoTerciario)
                        }
                    }
                }
                Text(subtitulo, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp * 0.88f, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = colores.textoPrincipal)
                Text(desc, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.82f, fontFamily = InterPremium), color = colores.textoTerciario, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(s.xs * 0.4f)) {
                    repeat(3) { Box(Modifier.size(s.xs * 0.65f).clip(CircleShape).background(accent.copy(alpha = 0.18f))) }
                }
                Box(Modifier.size(s.iconSmall + 8.dp).clip(CircleShape).background(FDColors.Surface).border(s.borderWidth * 0.7f, colores.cardBorde, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (habilitado) Icons.Default.ChevronRight else Icons.Default.Lock, null, tint = if (habilitado) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.5f), modifier = Modifier.size(s.iconTiny))
                }
            }
        }
    }
}
