package com.app.administradorfarmadon.compras.ui.componentes

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import java.util.*

@Composable
fun PestanaProveedores(
    proveedores: List<Proveedor>,
    proveedorSeleccionado: Proveedor?,
    deudaPendiente: Double = 0.0,
    facturasPendientesCount: Int = 0,
    subTabActual: String,
    productosDelProveedor: List<PharmProduct>,
    onSeleccionarSubTab: (String) -> Unit,
    onSeleccionarProveedor: (String) -> Unit,
    onCrearProveedor: () -> Unit,
    onEditarProveedor: (Proveedor) -> Unit,
    onEliminarProveedor: (Proveedor) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val context = LocalContext.current
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    // BackHandler quiet — teclado primero
    val focusManagerProv = LocalFocusManager.current
    val keyboardControllerProv = LocalSoftwareKeyboardController.current
    val densityProv = LocalDensity.current
    val isKeyboardVisibleProv = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(densityProv) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleProv -> { keyboardControllerProv?.hide(); focusManagerProv.clearFocus(force = true) }
            else -> { }
        }
    }

    if (proveedores.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(s.xxl),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Large,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier.widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier.padding(s.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(s.lg)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FDColors.TextPrimary.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = FDColors.TextSecondary,
                            modifier = Modifier.size(s.iconMedium)
                        )
                    }
                    Text(
                        text = "Directorio de Proveedores Vacío",
                        style = FDType.Heading2,
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = "Registra a tus droguerías, distribuidores y contactos comerciales para organizar tus pedidos de compra y facturas.",
                        style = FDType.Body,
                        color = FDColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    FDBotonPrimario(
                        texto = "REGISTRAR PRIMER PROVEEDOR",
                        onClick = onCrearProveedor,
                        icono = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = s.xl, vertical = s.lg),
        horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // ── PANEL IZQUIERDO: DIRECTORIO COMERCIAL (340 DP) ──
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Large,
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DIRECTORIO COMERCIAL",
                            style = FDType.Label.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = FDColors.TextTertiary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Droguerías y Aliados",
                            style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                    }

                    FDBotonSecundario(
                        texto = "NUEVO",
                        onClick = onCrearProveedor,
                        icono = Icons.Default.Add,
                        modifier = Modifier.height(s.btnSmallH)
                    )
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = s.separatorH)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(s.sm)
                ) {
                    items(proveedores, key = { it.id }) { prov ->
                        val isSelected = prov.id == (proveedorSeleccionado?.id ?: "")
                        
                        // Barra de Selección Animada
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 4.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "Indicator"
                        )

                        Surface(
                            color = if (isSelected) FDColors.SurfaceElevated else Color.Transparent,
                            shape = FDShapes.Medium,
                            border = BorderStroke(
                                if (isSelected) 1.dp else 0.dp,
                                if (isSelected) FDColors.Primary.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeleccionarProveedor(prov.id) }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Indicador Vertical Izquierdo
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(indicatorWidth)
                                        .fillMaxHeight()
                                        .background(FDColors.Primary, FDShapes.Full)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                                ) {
                                    // Avatar de Negocio (Iniciales con Gradiente)
                                    val iniciales = prov.nombre.take(2).uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(s.iconLarge * 1.5f)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(FDColors.TextPrimary.copy(alpha = 0.08f), FDColors.TextPrimary.copy(alpha = 0.03f))
                                                )
                                            )
                                            .border(1.dp, FDColors.Border.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = iniciales,
                                            style = FDType.Numeric.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = FDColors.TextSecondary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prov.nombre,
                                            style = FDType.Body.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ),
                                            color = if (isSelected) FDColors.Primary else FDColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (prov.idFiscal.isNotBlank()) "RUC: ${prov.idFiscal}" else "Sin RUC",
                                                style = FDType.BodySmall.copy(fontSize = 11.sp),
                                                color = FDColors.TextTertiary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(s.iconSmall)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── PANEL DERECHO: EXPEDIENTE DE NEGOCIO (65%) ──
        Surface(
            color = FDColors.Surface,
            shape = FDShapes.Large,
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (proveedorSeleccionado == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Selecciona un proveedor para gestionar su ficha comercial",
                        color = FDColors.TextTertiary,
                        style = FDType.Body.copy(fontSize = 13.5.sp)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. CABECERA DE ALTA DENSIDAD (Fija al Top)
                    Surface(
                        color = FDColors.SurfaceElevated.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = s.padCard, vertical = s.padCard * 0.85f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(s.iconLarge * 1.55f)
                                        .clip(FDShapes.Small)
                                        .background(FDColors.TextPrimary.copy(alpha = 0.05f))
                                        .border(1.dp, FDColors.Border, FDShapes.Small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Business, null, tint = FDColors.TextSecondary, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(
                                        text = proveedorSeleccionado.nombre.uppercase(),
                                        style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                                        color = FDColors.TextPrimary
                                    )
                                    Text(
                                        text = "ID FISCAL: ${proveedorSeleccionado.idFiscal.ifBlank { "NO REGISTRADO" }}",
                                        style = FDType.Label.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                                        color = FDColors.TextTertiary
                                    )
                                }
                            }

                            // ACCIÓN RÁPIDA: WHATSAPP FIJO EN EL TOP-BAR (ESQUINA DERECHA)
                            if (proveedorSeleccionado.telefono.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val uri = Uri.parse("https://wa.me/${proveedorSeleccionado.telefono.replace(" ", "").replace("+", "").replace("-", "")}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.message ?: "sin detalle"}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(s.iconLarge * 1.55f)
                                        .clip(CircleShape)
                                        .background(FDColors.Success.copy(alpha = 0.1f))
                                        .border(1.dp, FDColors.Success.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, "WhatsApp", tint = FDColors.Success, modifier = Modifier.size(s.iconSmall * 1.1f))
                                }
                            }
                        }
                    }

                    // 2. NAVEGACIÓN DE PESTAÑAS (Fija al Top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = s.padCard),
                        horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                    ) {
                        listOf(
                            "RESUMEN" to "EXPEDIENTE COMERCIAL",
                            "PRODUCTOS" to "PRODUCTOS SUMINISTRADOS"
                        ).forEach { (clave, etiqueta) ->
                            val isSelected = subTabActual == clave
                            Column(
                                modifier = Modifier
                                    .clickable { onSeleccionarSubTab(clave) }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = etiqueta,
                                    style = FDType.Label.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                AnimatedVisibility(visible = isSelected) {
                                    Box(modifier = Modifier.height(3.dp).width(24.dp).clip(FDShapes.Full).background(FDColors.Primary))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = s.separatorH)

                    // 3. CUERPO DE DATOS (SCROLLEABLE)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (subTabActual) {
                            "RESUMEN" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(s.padCard),
                                    verticalArrangement = Arrangement.spacedBy(s.gapLarge)
                                ) {
                                    // Matriz de Métricas Industrial
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(s.gapLarge),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.AccountBalanceWallet, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconTiny * 1.0f))
                                            Text("DEUDA:", style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                                            Text(
                                                text = if (deudaPendiente > 0) "$simboloMoneda " + String.format(Locale.US, "%,.2f", deudaPendiente) else "AL DÍA",
                                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                                                color = if (deudaPendiente > 0) FDColors.Error else FDColors.Success
                                            )
                                        }
                                        VerticalDivider(modifier = Modifier.height(14.dp), color = FDColors.Border)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.LocalShipping, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconTiny * 1.0f))
                                            Text("MÍNIMO DESPACHO:", style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                                            Text(
                                                text = if (proveedorSeleccionado.montoMinimoPedido > 0) "$simboloMoneda " + String.format(Locale.US, "%,.2f", proveedorSeleccionado.montoMinimoPedido) else "LIBRE",
                                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                                                color = FDColors.TextPrimary
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = s.separatorH)

                                    // Matriz de Contacto (Estilo Terminal)
                                    Column(verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                                        Text("DATOS DE CONTACTO Y LOGÍSTICA", style = FDType.Label.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapXLarge)) {
                                            DatoRegistro(
                                                icono = Icons.Default.Person,
                                                etiqueta = "Asesor Comercial",
                                                valor = proveedorSeleccionado.contacto.ifBlank { "No asignado" },
                                                modifier = Modifier.weight(1f)
                                            )
                                            DatoRegistro(
                                                icono = Icons.Default.Phone,
                                                etiqueta = "Teléfono / WhatsApp",
                                                valor = proveedorSeleccionado.telefono.ifBlank { "No registrado" },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapXLarge)) {
                                            DatoRegistro(
                                                icono = Icons.Default.Email,
                                                etiqueta = "Correo Electrónico",
                                                valor = proveedorSeleccionado.email.ifBlank { "Sin correo registrado" },
                                                modifier = Modifier.weight(1f)
                                            )
                                            DatoRegistro(
                                                icono = Icons.Default.LocationOn,
                                                etiqueta = "Dirección Fiscal / Almacén",
                                                valor = proveedorSeleccionado.direccion.ifBlank { "No registrada en sistema" },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            "PRODUCTOS" -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = s.padCard),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "LISTADO DE PRODUCTOS SUMINISTRADOS",
                                            style = FDType.Label.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                                            color = FDColors.TextTertiary,
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                        )
                                        HorizontalDivider(color = FDColors.Border, thickness = s.separatorH)
                                    }
                                    items(productosDelProveedor, key = { it.id }) { prod ->
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(FDShapes.XSmall)
                                                        .background(FDColors.TextPrimary.copy(alpha = 0.04f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Inventory2, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconSmall * 0.85f))
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.name,
                                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                        color = FDColors.TextPrimary
                                                    )
                                                    Text(
                                                        text = prod.category.uppercase(),
                                                        style = FDType.Label.copy(fontSize = 9.sp, color = FDColors.TextTertiary)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = s.separatorH)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. ZÓCALO DE ACCIONES CRÍTICAS (Fijo al Bottom)
                    Surface(
                        color = FDColors.SurfaceElevated.copy(alpha = 0.5f),
                        border = BorderStroke(s.borderWidth, FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // ELIMINAR: Fijo, visible y segregado a la izquierda
                            IconButton(
                                onClick = { onEliminarProveedor(proveedorSeleccionado) },
                                modifier = Modifier
                                    .size(s.iconLarge * 1.55f)
                                    .clip(FDShapes.Small)
                                    .background(FDColors.Error.copy(alpha = 0.08f)),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = FDColors.Error)
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall * 1.1f))
                            }

                            // EDITAR: Acción principal fija a la derecha
                            FDBotonSecundario(
                                texto = "EDITAR INFORMACIÓN COMERCIAL",
                                onClick = { onEditarProveedor(proveedorSeleccionado) },
                                icono = Icons.Default.Edit,
                                modifier = Modifier.height(s.btnMediumH * 0.92f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatoRegistro(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    accion: (() -> Unit)? = null
) {
    val s = recordarMedidaAdaptativa()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icono, null, tint = FDColors.TextTertiary, modifier = Modifier.size(13.dp))
            Text(etiqueta.uppercase(), style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.then(if (accion != null) Modifier.clickable { accion() } else Modifier)
        ) {
            Text(
                text = valor,
                style = FDType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                color = if (accion != null) FDColors.Primary else FDColors.TextPrimary
            )
            if (accion != null) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.OpenInNew, null, tint = FDColors.Primary.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            }
        }
    }
}
