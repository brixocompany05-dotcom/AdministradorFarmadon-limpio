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
import androidx.compose.foundation.lazy.LazyListState
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
    todosLosProductos: List<PharmProduct> = emptyList(),
    onSeleccionarSubTab: (String) -> Unit,
    onSeleccionarProveedor: (String) -> Unit,
    onCrearProveedor: () -> Unit,
    onEditarProveedor: (Proveedor) -> Unit,
    onEliminarProveedor: (Proveedor) -> Unit = {},
    onVincularProducto: (PharmProduct, Proveedor) -> Unit = { _, _ -> },
    onDesvincularProducto: (PharmProduct) -> Unit = {},
    // Operaciones de DINERO: sin valor por defecto — el botón jamás queda
    // "REGISTRANDO..." infinito por un llamante olvidadizo; el compilador lo exige.
    onCobrarSaldoAFavor: (Double, String, (Result<Unit>) -> Unit) -> Unit,
    onDeclararSaldoPerdido: (Double, String, (Result<Unit>) -> Unit) -> Unit,
    listaState: LazyListState = LazyListState(),
    cargando: Boolean = false,
    errorEscucha: String? = null,
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
    var mostrarDialogoEliminar by remember(proveedorSeleccionado?.id) { mutableStateOf(false) }
    var mostrarDialogoAsignar by remember { mutableStateOf(false) }
    var productoParaCambiar by remember { mutableStateOf<PharmProduct?>(null) }
    var busquedaProveedor by remember { mutableStateOf("") }
    var filtroRapido by remember { mutableStateOf("TODOS") } // "TODOS" | "CON_SALDO"

    val totalProveedores = proveedores.size
    val cantConSaldo = remember(proveedores) {
        proveedores.count { it.saldoAFavor > 0.01 }
    }

    val proveedoresFiltrados = remember(proveedores, busquedaProveedor, filtroRapido) {
        val q = busquedaProveedor.trim().lowercase()
        proveedores.filter { prov ->
            val coincideFiltro = when (filtroRapido) {
                "CON_SALDO" -> prov.saldoAFavor > 0.01
                else -> true
            }
            if (!coincideFiltro) return@filter false
            if (q.isBlank()) return@filter true
            prov.nombre.contains(q, ignoreCase = true) ||
                prov.idFiscal.contains(q, ignoreCase = true) ||
                prov.contacto.contains(q, ignoreCase = true) ||
                prov.telefono.contains(q, ignoreCase = true)
        }
    }

    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleProv -> { keyboardControllerProv?.hide(); focusManagerProv.clearFocus(force = true) }
            mostrarDialogoEliminar -> { mostrarDialogoEliminar = false }
            else -> { }
        }
    }

    // Verdad de la lista: "vacío" solo se declara cuando la carga terminó SIN error.
    // Jamás se confunde "aún no llega la data" ni "falló la actualización" con vacío.
    if (proveedores.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(s.xxl),
            contentAlignment = Alignment.Center
        ) {
            if (cargando || errorEscucha != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(s.lg)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(color = FDColors.Primary, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        Text(
                            text = "Cargando directorio de proveedores…",
                            style = FDType.Body,
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "No se pudo cargar el directorio",
                            style = FDType.Heading2,
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Motivo real: ${errorEscucha ?: ""}. Usa el botón REINTENTAR de arriba.",
                            style = FDType.Body,
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
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

                // ── BUSCADOR DE PROVEEDOR CÓMODO Y ESPACIOSO ──
                CampoBuscadorModerno(
                    busqueda = busquedaProveedor,
                    onBusquedaChange = { busquedaProveedor = it },
                    placeholder = "Buscar droguería, RUC o contacto...",
                    altura = s.inputMinH
                )

                // ── FILTROS RÁPIDOS Y CONTADOR EN VIVO ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip TODOS
                    val selTodos = filtroRapido == "TODOS"
                    Surface(
                        onClick = { filtroRapido = "TODOS" },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selTodos) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selTodos) FDColors.Primary else FDColors.Border.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp)
                        ) {
                            Text(
                                text = "TODOS",
                                style = FDType.Caption.copy(fontWeight = if (selTodos) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp),
                                color = if (selTodos) FDColors.Primary else FDColors.TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (selTodos) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "$totalProveedores",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = if (selTodos) Color.White else FDColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }

                    // Chip CON SALDO
                    val selSaldo = filtroRapido == "CON_SALDO"
                    Surface(
                        onClick = { filtroRapido = "CON_SALDO" },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selSaldo) FDColors.Success.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selSaldo) FDColors.Success else FDColors.Border.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp)
                        ) {
                            Text(
                                text = "CON SALDO",
                                style = FDType.Caption.copy(fontWeight = if (selSaldo) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp),
                                color = if (selSaldo) FDColors.Success else FDColors.TextSecondary
                            )
                            if (cantConSaldo > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selSaldo) FDColors.Success else FDColors.Success.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "$cantConSaldo",
                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = if (selSaldo) Color.White else FDColors.Success,
                                        modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "${proveedoresFiltrados.size} de $totalProveedores",
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = FDColors.TextTertiary
                    )
                }

                if (proveedoresFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(s.gapSmall),
                            modifier = Modifier.padding(s.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Sin resultados",
                                style = FDType.Heading3.copy(fontSize = 14.sp),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                text = if (busquedaProveedor.isNotBlank()) "No se encontró '$busquedaProveedor'" else "No hay proveedores con el filtro seleccionado",
                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (busquedaProveedor.isNotBlank() || filtroRapido != "TODOS") {
                                Spacer(Modifier.height(4.dp))
                                FDBotonSecundario(
                                    texto = "LIMPIAR BÚSQUEDA",
                                    onClick = {
                                        busquedaProveedor = ""
                                        filtroRapido = "TODOS"
                                    },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listaState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                    items(proveedoresFiltrados, key = { it.id }) { prov ->
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
                    // ── CABECERA: TARJETA DE PRESENTACIÓN EMPRESARIAL ──
                    Surface(
                        color = FDColors.SurfaceElevated.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(s.padCardLarge),
                            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                            ) {
                                // Avatar Gigante de Identidad
                                val iniciales = proveedorSeleccionado.nombre.take(2).uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(FDColors.Primary.copy(alpha = 0.15f), FDColors.Primary.copy(alpha = 0.05f))
                                            )
                                        )
                                        .border(2.dp, FDColors.Primary.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = iniciales,
                                        style = FDType.Numeric.copy(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ),
                                        color = FDColors.Primary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proveedorSeleccionado.nombre.uppercase(),
                                        style = FDType.Heading2.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = FDColors.TextPrimary
                                    )
                                    var errorContacto by remember(proveedorSeleccionado.id) { mutableStateOf<String?>(null) }
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = FDColors.TextPrimary.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "RUC: ${proveedorSeleccionado.idFiscal.ifBlank { "NO REGISTRADO" }}",
                                                style = FDType.Numeric.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                ),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (proveedorSeleccionado.telefono.isNotBlank()) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Chat,
                                                null,
                                                tint = FDColors.Success,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        try {
                                                            val uri = Uri.parse("https://wa.me/${proveedorSeleccionado.telefono.replace(" ", "").replace("+", "").replace("-", "")}")
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                            errorContacto = null
                                                        } catch (e: Exception) {
                                                            // Fallo real visible: la persona necesita la verdad y el siguiente paso.
                                                            errorContacto = "No se pudo abrir WhatsApp (${e.message ?: "sin detalle del sistema"}). Llama al ${proveedorSeleccionado.telefono}."
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                    errorContacto?.let { err ->
                                        Text(
                                            text = err,
                                            style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                            color = FDColors.Error
                                        )
                                    }
                                }

                                // Botones de Acción Operativa: Editar y Eliminar Proveedor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FDBotonSecundario(
                                        texto = "EDITAR",
                                        icono = Icons.Default.Edit,
                                        onClick = { onEditarProveedor(proveedorSeleccionado) },
                                        modifier = Modifier.height(s.btnSmallH)
                                    )
                                    IconButton(
                                        onClick = { mostrarDialogoEliminar = true },
                                        modifier = Modifier
                                            .size(s.btnSmallH)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FDColors.Error.copy(alpha = 0.10f))
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar proveedor",
                                            tint = FDColors.Error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (mostrarDialogoEliminar) {
                        AlertDialog(
                            onDismissRequest = { mostrarDialogoEliminar = false },
                            title = {
                                Text(
                                    text = "Eliminar Proveedor",
                                    style = FDType.Heading3,
                                    color = FDColors.TextPrimary
                                )
                            },
                            text = {
                                when {
                                    facturasPendientesCount > 0 -> {
                                        Text(
                                            text = "No se puede eliminar a '${proveedorSeleccionado.nombre}' porque tiene $facturasPendientesCount factura(s) pendiente(s) de pago por un total de $simboloMoneda ${String.format(Locale.US, "%.2f", deudaPendiente)}. Liquida o anula esas facturas primero.",
                                            style = FDType.Body,
                                            color = FDColors.Error
                                        )
                                    }
                                    proveedorSeleccionado.saldoAFavor > 0.01 -> {
                                        Text(
                                            text = "No se puede eliminar a '${proveedorSeleccionado.nombre}' porque tiene un saldo a favor de $simboloMoneda ${String.format(Locale.US, "%.2f", proveedorSeleccionado.saldoAFavor)}. Aplica o cobra ese saldo primero.",
                                            style = FDType.Body,
                                            color = FDColors.Error
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Se borrarán los datos comerciales de '${proveedorSeleccionado.nombre}' (contacto, teléfono, correo, dirección y pedido mínimo). Esta acción no se puede deshacer. Las facturas y el historial de pagos NO se borran.",
                                            style = FDType.Body,
                                            color = FDColors.TextSecondary
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                if (facturasPendientesCount == 0 && proveedorSeleccionado.saldoAFavor <= 0.01) {
                                    Button(
                                        onClick = {
                                            mostrarDialogoEliminar = false
                                            onEliminarProveedor(proveedorSeleccionado)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FDColors.Error,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("ELIMINAR")
                                    }
                                } else {
                                    TextButton(onClick = { mostrarDialogoEliminar = false }) {
                                        Text("ENTENDIDO", color = FDColors.Primary)
                                    }
                                }
                            },
                            dismissButton = {
                                if (facturasPendientesCount == 0 && proveedorSeleccionado.saldoAFavor <= 0.01) {
                                    TextButton(onClick = { mostrarDialogoEliminar = false }) {
                                        Text("CANCELAR", color = FDColors.TextSecondary)
                                    }
                                }
                            },
                            containerColor = FDColors.SurfaceElevated,
                            shape = RoundedCornerShape(14.dp)
                        )
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
                                                text = if (errorEscucha != null) "SIN DATOS" else if (deudaPendiente > 0.01) "$simboloMoneda " + String.format(Locale.US, "%.2f", deudaPendiente) else "AL DÍA",
                                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                                                color = if (errorEscucha != null) FDColors.Warning else if (deudaPendiente > 0.01) FDColors.Error else FDColors.Success
                                            )
                                        }
                                        VerticalDivider(modifier = Modifier.height(14.dp), color = FDColors.Border)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.LocalShipping, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconTiny * 1.0f))
                                            Text("MÍNIMO DESPACHO:", style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                                            Text(
                                                text = if (proveedorSeleccionado.montoMinimoPedido > 0) "$simboloMoneda " + String.format(Locale.US, "%.2f", proveedorSeleccionado.montoMinimoPedido) else "LIBRE",
                                                style = FDType.Body.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                                                color = FDColors.TextPrimary
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = s.separatorH)

                                    // Información de Contacto (Business Card Style)
                                    Surface(
                                        color = FDColors.SurfaceElevated.copy(alpha = 0.2f),
                                        shape = FDShapes.Medium,
                                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(s.padCard),
                                            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
                                        ) {
                                            Text(
                                                text = "CANALES DE CONTACTO Y LOGÍSTICA",
                                                style = FDType.Label.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.5.sp,
                                                    letterSpacing = 1.2.sp
                                                ),
                                                color = FDColors.TextTertiary
                                            )

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapXLarge)) {
                                                DatoRegistro(
                                                    icono = Icons.Default.Person,
                                                    etiqueta = "Asesor Comercial",
                                                    valor = proveedorSeleccionado.contacto.ifBlank { "No asignado" },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                DatoRegistro(
                                                    icono = Icons.Default.Phone,
                                                    etiqueta = "Teléfono Directo",
                                                    valor = proveedorSeleccionado.telefono.ifBlank { "No registrado" },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapXLarge)) {
                                                DatoRegistro(
                                                    icono = Icons.Default.Email,
                                                    etiqueta = "Correo Corporativo",
                                                    valor = proveedorSeleccionado.email.ifBlank { "Sin correo registrado" },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                DatoRegistro(
                                                    icono = Icons.Default.LocationOn,
                                                    etiqueta = "Dirección Operativa",
                                                    valor = proveedorSeleccionado.direccion.ifBlank { "No registrada en sistema" },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = s.separatorH)

                                    // Saldo a Favor (Ficha Financiera) — el negativo jamás se aplana: es corrupción y se muestra.
                                    val saldoCrudo = proveedorSeleccionado.saldoAFavor
                                    val saldoAFavor = saldoCrudo.coerceAtLeast(0.0)
                                    val saldoCorrupto = saldoCrudo < -0.01
                                    var modoSaldo by remember(proveedorSeleccionado.id) { mutableStateOf<String?>(null) }
                                    var montoSaldo by remember(proveedorSeleccionado.id) { mutableStateOf("") }
                                    var detalleSaldo by remember(proveedorSeleccionado.id) { mutableStateOf("") }
                                    var procesandoSaldo by remember(proveedorSeleccionado.id) { mutableStateOf(false) }
                                    var mensajeSaldo by remember(proveedorSeleccionado.id) { mutableStateOf<String?>(null) }
                                    var mensajeSaldoEsError by remember(proveedorSeleccionado.id) { mutableStateOf(false) }

                                    Surface(
                                        color = if (saldoAFavor > 0.01) FDColors.Success.copy(alpha = 0.03f) else FDColors.SurfaceElevated.copy(alpha = 0.1f),
                                        shape = FDShapes.Medium,
                                        border = BorderStroke(1.dp, if (saldoAFavor > 0.01) FDColors.Success.copy(alpha = 0.25f) else FDColors.Border.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(s.padCard),
                                            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(
                                                        text = "SALDO A FAVOR DEL PROVEEDOR",
                                                        style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                                                        color = FDColors.TextTertiary
                                                    )
                                                    Text(
                                                        text = if (saldoCorrupto) "Saldo negativo en servidor: avisa a soporte, no se oculta." else if (saldoAFavor > 0.01) "Este proveedor tiene dinero pendiente a tu favor." else "Sin saldos pendientes a tu favor.",
                                                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                                        color = if (saldoCorrupto) FDColors.Error else FDColors.TextSecondary
                                                    )
                                                }
                                                Text(
                                                    text = "$simboloMoneda " + String.format(Locale.US, "%.2f", saldoCrudo),
                                                    style = FDType.Numeric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
                                                    color = if (saldoCorrupto) FDColors.Error else if (saldoAFavor > 0.01) FDColors.Success else FDColors.TextTertiary
                                                )
                                            }

                                            if (saldoAFavor > 0.01) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                                                ) {
                                                    FDBotonSecundario(
                                                        texto = "COBRAR SALDO",
                                                        onClick = { modoSaldo = if (modoSaldo == "COBRAR") null else "COBRAR"; detalleSaldo = ""; montoSaldo = ""; mensajeSaldo = null },
                                                        habilitado = !procesandoSaldo,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    FDBotonSecundario(
                                                        texto = "DECLARAR PERDIDO",
                                                        onClick = { modoSaldo = if (modoSaldo == "PERDIDO") null else "PERDIDO"; detalleSaldo = ""; montoSaldo = ""; mensajeSaldo = null },
                                                        habilitado = !procesandoSaldo,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                if (modoSaldo != null) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(FDColors.SurfaceElevated.copy(alpha = 0.5f), FDShapes.Small)
                                                            .padding(s.padCard * 0.7f),
                                                        verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                                                    ) {
                                                        Text(
                                                            text = if (modoSaldo == "COBRAR") "COBRAR SALDO A FAVOR" else "DECLARAR SALDO PERDIDO",
                                                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                                            color = FDColors.TextPrimary
                                                        )
                                                        OutlinedTextField(
                                                            value = montoSaldo,
                                                            onValueChange = { nuevo ->
                                                                val limpio = nuevo.filter { it.isDigit() || it == '.' || it == ',' }
                                                                val separadores = limpio.count { it == '.' || it == ',' }
                                                                if (separadores <= 1) montoSaldo = limpio
                                                            },
                                                            enabled = !procesandoSaldo,
                                                            label = { Text("Monto a registrar") },
                                                            singleLine = true,
                                                            shape = FDShapes.Small,
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedContainerColor = FDColors.InputBackground,
                                                                unfocusedContainerColor = FDColors.InputBackground,
                                                                focusedBorderColor = FDColors.BorderFocus,
                                                                unfocusedBorderColor = FDColors.InputBorder,
                                                                focusedTextColor = FDColors.InputText,
                                                                unfocusedTextColor = FDColors.InputText
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(s.inputMinH)
                                                        )
                                                        OutlinedTextField(
                                                            value = detalleSaldo,
                                                            onValueChange = { detalleSaldo = it },
                                                            enabled = !procesandoSaldo,
                                                            label = { Text(if (modoSaldo == "COBRAR") "Documento o comprobante del cobro" else "Motivo de la pérdida (mínimo 5 letras)") },
                                                            singleLine = true,
                                                            shape = FDShapes.Small,
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedContainerColor = FDColors.InputBackground,
                                                                unfocusedContainerColor = FDColors.InputBackground,
                                                                focusedBorderColor = FDColors.BorderFocus,
                                                                unfocusedBorderColor = FDColors.InputBorder,
                                                                focusedTextColor = FDColors.InputText,
                                                                unfocusedTextColor = FDColors.InputText
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(s.inputMinH)
                                                        )
                                                        FDBotonPrimario(
                                                            texto = if (procesandoSaldo) "REGISTRANDO..." else "CONFIRMAR",
                                                            onClick = {
                                                                val monto = montoSaldo.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                                val detalle = detalleSaldo.trim()
                                                                when {
                                                                    monto <= 0.0 -> { mensajeSaldo = "Escribe un monto mayor a cero."; mensajeSaldoEsError = true }
                                                                    monto > saldoAFavor + 0.01 -> { mensajeSaldo = "El monto no puede superar el saldo a favor de $simboloMoneda ${String.format(Locale.US, "%.2f", saldoAFavor)}."; mensajeSaldoEsError = true }
                                                                    modoSaldo == "COBRAR" && detalle.length < 3 -> { mensajeSaldo = "Indica el documento o comprobante del cobro."; mensajeSaldoEsError = true }
                                                                    modoSaldo == "PERDIDO" && detalle.length < 5 -> { mensajeSaldo = "El motivo de la pérdida debe tener al menos 5 letras."; mensajeSaldoEsError = true }
                                                                    else -> {
                                                                        mensajeSaldo = null
                                                                        procesandoSaldo = true
                                                                        if (modoSaldo == "COBRAR") {
                                                                            onCobrarSaldoAFavor(monto, detalle) { res ->
                                                                                procesandoSaldo = false
                                                                                if (res.isSuccess) {
                                                                                    // El panel QUEDA ABIERTO con el éxito visible: cerrarlo
                                                                                    // escondía la confirmación de un movimiento de dinero.
                                                                                    mensajeSaldo = "Cobro registrado. El saldo a favor bajó."
                                                                                    mensajeSaldoEsError = false
                                                                                    montoSaldo = ""; detalleSaldo = ""
                                                                                } else {
                                                                                    mensajeSaldo = "No se pudo registrar el cobro: ${res.exceptionOrNull()?.message ?: "sin detalle del servidor"}"
                                                                                    mensajeSaldoEsError = true
                                                                                }
                                                                            }
                                                                        } else {
                                                                            onDeclararSaldoPerdido(monto, detalle) { res ->
                                                                                procesandoSaldo = false
                                                                                if (res.isSuccess) {
                                                                                    // El panel QUEDA ABIERTO con el éxito visible (R3).
                                                                                    mensajeSaldo = "Pérdida declarada y registrada con justificación."
                                                                                    mensajeSaldoEsError = false
                                                                                    montoSaldo = ""; detalleSaldo = ""
                                                                                } else {
                                                                                    mensajeSaldo = "No se pudo declarar la pérdida: ${res.exceptionOrNull()?.message ?: "sin detalle del servidor"}"
                                                                                    mensajeSaldoEsError = true
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            habilitado = !procesandoSaldo,
                                                            cargando = procesandoSaldo,
                                                            icono = Icons.Default.Check,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(s.btnMediumH)
                                                        )
                                                        mensajeSaldo?.let { msg ->
                                                            Surface(
                                                                color = if (mensajeSaldoEsError) FDColors.Error.copy(alpha = 0.08f) else FDColors.Success.copy(alpha = 0.08f),
                                                                shape = FDShapes.XSmall,
                                                                border = BorderStroke(
                                                                    s.borderWidth * 0.6f,
                                                                    if (mensajeSaldoEsError) FDColors.Error.copy(alpha = 0.35f) else FDColors.Success.copy(alpha = 0.35f)
                                                                ),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = msg,
                                                                    style = FDType.BodySmall.copy(fontSize = 11.sp),
                                                                    color = if (mensajeSaldoEsError) FDColors.Error else FDColors.Success,
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // HISTORIAL DEL SALDO A FAVOR
                                            val historialSaldo = proveedorSeleccionado.historialSaldoAFavor.sortedByDescending { it.fechaMs }
                                            if (historialSaldo.isNotEmpty()) {
                                                var verHistorialSaldo by remember(proveedorSeleccionado.id) { mutableStateOf(false) }
                                                FDBotonSecundario(
                                                    texto = if (verHistorialSaldo) "OCULTAR MOVIMIENTOS" else "VER MOVIMIENTOS (${historialSaldo.size})",
                                                    onClick = { verHistorialSaldo = !verHistorialSaldo },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (verHistorialSaldo) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                                                    ) {
                                                        historialSaldo.forEach { mov ->
                                                            val esIngreso = mov.tipo == "SALDO_A_FAVOR_ANULACION"
                                                            Surface(
                                                                color = FDColors.Surface,
                                                                shape = FDShapes.XSmall,
                                                                border = BorderStroke(s.borderWidth * 0.6f, FDColors.Border),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Text(
                                                                            text = etiquetaMovimientoSaldo(mov.tipo),
                                                                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                                                            color = if (esIngreso) FDColors.Success else FDColors.Warning,
                                                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                                        )
                                                                        Text(
                                                                            text = (if (esIngreso) "+" else "-") + "$simboloMoneda " + String.format(Locale.US, "%.2f", kotlin.math.abs(mov.monto)),
                                                                            style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                                                                            color = FDColors.TextPrimary
                                                                        )
                                                                    }
                                                                    Text(
                                                                        text = (mov.motivo.ifBlank { mov.documento }).ifBlank { "Movimiento registrado" },
                                                                        style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                                                        color = FDColors.TextSecondary
                                                                    )
                                                                    Text(
                                                                        text = (mov.fechaLegible.ifBlank { "—" }) + (if (mov.usuarioNombre.isNotBlank()) " · ${mov.usuarioNombre}" else ""),
                                                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                                                        color = FDColors.TextTertiary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "PRODUCTOS SUMINISTRADOS",
                                                    style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextTertiary
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = FDColors.Primary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "${productosDelProveedor.size}",
                                                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                                                        color = FDColors.Primary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }

                                            FDBotonPrimario(
                                                texto = "VINCULAR PRODUCTOS",
                                                icono = Icons.Default.Add,
                                                onClick = { mostrarDialogoAsignar = true },
                                                modifier = Modifier.height(30.dp)
                                            )
                                        }
                                        HorizontalDivider(color = FDColors.Border, thickness = s.separatorH)
                                    }
                                    if (productosDelProveedor.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Este proveedor aún no tiene productos vinculados.",
                                                        style = FDType.BodySmall.copy(fontSize = 12.sp),
                                                        color = FDColors.TextSecondary
                                                    )
                                                    FDBotonSecundario(
                                                        texto = "VINCULAR PRIMER PRODUCTO",
                                                        icono = Icons.Default.Add,
                                                        onClick = { mostrarDialogoAsignar = true },
                                                        modifier = Modifier.height(30.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(productosDelProveedor, key = { it.id }) { prod ->
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
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
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.name,
                                                        style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                                        color = FDColors.TextPrimary
                                                    )
                                                    val especificacion = listOfNotNull(
                                                        prod.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                                                        prod.empaque.takeIf { it.isNotBlank() },
                                                        "${prod.content} ${prod.contentUnit}".takeIf { prod.content.isNotBlank() }
                                                    ).joinToString(" · ")
                                                    if (especificacion.isNotBlank()) {
                                                        Text(
                                                            text = especificacion.uppercase(),
                                                            style = FDType.Label.copy(fontSize = 9.sp, color = FDColors.TextTertiary)
                                                        )
                                                    }
                                                }

                                                // Stock, precio y acción de cambio
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "Stock: ${prod.stock}u",
                                                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                            color = if (prod.stock <= prod.minStock && prod.minStock > 0) FDColors.Warning else FDColors.TextPrimary
                                                        )
                                                        if (prod.purchasePrice > 0) {
                                                            Text(
                                                                text = "$simboloMoneda ${String.format(Locale.US, "%.2f", prod.purchasePrice)}",
                                                                style = FDType.Caption.copy(fontSize = 10.5.sp),
                                                                color = FDColors.TextTertiary
                                                            )
                                                        }
                                                    }

                                                    FDBotonSecundario(
                                                        texto = "CAMBIAR",
                                                        icono = Icons.Default.SwapHoriz,
                                                        onClick = { productoParaCambiar = prod },
                                                        modifier = Modifier.height(28.dp)
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
                                onClick = { mostrarDialogoEliminar = true },
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

    if (mostrarDialogoAsignar && proveedorSeleccionado != null) {
        DialogoAsignarProductosAProveedor(
            proveedor = proveedorSeleccionado,
            todosLosProductos = todosLosProductos,
            onVincularProducto = { prod, prov ->
                onVincularProducto(prod, prov)
            },
            onDismiss = { mostrarDialogoAsignar = false }
        )
    }

    productoParaCambiar?.let { prod ->
        DialogoCambiarProveedor(
            producto = prod,
            proveedores = proveedores,
            onGuardar = { nuevoProv ->
                onVincularProducto(prod, nuevoProv)
                productoParaCambiar = null
            },
            onDesvincular = {
                onDesvincularProducto(prod)
                productoParaCambiar = null
            },
            onDismiss = { productoParaCambiar = null }
        )
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

/** Traducción humana de cada tipo de movimiento del saldo a favor (R12: nada falso). */
private fun etiquetaMovimientoSaldo(tipo: String): String = when (tipo) {
    "SALDO_A_FAVOR_ANULACION" -> "Saldo a favor por anulación"
    "SALDO_USADO_RECEPCION" -> "Usado en recepción"
    "SALDO_COBRADO_EFECTIVO" -> "Cobrado en efectivo"
    "SALDO_DECLARADO_PERDIDO" -> "Declarado perdido"
    else -> tipo.ifBlank { "Movimiento" }
}
