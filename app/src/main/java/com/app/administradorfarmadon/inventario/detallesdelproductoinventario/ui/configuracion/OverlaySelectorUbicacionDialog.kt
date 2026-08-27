package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

private val PREFIJOS_RAPIDOS = listOf("Pasillo ", "Vitrina ", "Estante ", "Nevera ")

/**
 * Diálogo Overlay Enterprise para Buscar, Seleccionar o Crear Ubicaciones en Farmacia.
 * Totalmente optimizado para Tablet Horizontal con manejo de teclado e IME.
 */
@Composable
fun OverlaySelectorUbicacionDialog(
    ubicacionActual: String,
    ubicacionesDisponibles: List<String>,
    onUbicacionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var modoCreacion by remember { mutableStateOf(false) }
    var nuevaUbicacionInput by remember { mutableStateOf("") }

    val queryLimpia = searchQuery.trim().replace(Regex("\\s+"), " ")

    val opcionesFiltradas = remember(ubicacionesDisponibles, queryLimpia) {
        if (queryLimpia.isBlank()) ubicacionesDisponibles
        else ubicacionesDisponibles.filter { it.contains(queryLimpia, ignoreCase = true) }
    }

    val existeCoincidenciaExacta = remember(queryLimpia, ubicacionesDisponibles) {
        if (queryLimpia.isBlank()) null
        else ubicacionesDisponibles.firstOrNull { it.equals(queryLimpia, ignoreCase = true) }
    }

    val textoNuevaLimpia = nuevaUbicacionInput.trim().replace(Regex("\\s+"), " ")
    val esNuevaValida = textoNuevaLimpia.length >= 3

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FDColors.Background.copy(alpha = 0.6f))
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 560.dp)
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // ── 1. CABECERA SUPERIOR ──
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = FDColors.Primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = if (modoCreacion) "CREAR NUEVA UBICACIÓN" else "UBICACIÓN EN FARMACIA",
                                        style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                                    )
                                    Text(
                                        text = if (modoCreacion) "Registra un nuevo estante para el catálogo" else "Selecciona la posición física del producto",
                                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (modoCreacion) modoCreacion = false
                                    else onDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FDColors.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                        // ── 2. MODO BUSCADOR O MODO CREACIÓN ──
                        if (!modoCreacion) {
                            // Barra de búsqueda con icono y auto-focus
                            val searchInteraction = remember { MutableInteractionSource() }
                            val isFocused by searchInteraction.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) FDColors.SurfaceElevated else FDColors.Surface)
                                    .border(
                                        width = if (isFocused) 1.5.dp else 1.dp,
                                        color = if (isFocused) FDColors.Primary else FDColors.Border,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = if (isFocused) FDColors.Primary else FDColors.TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        interactionSource = searchInteraction,
                                        textStyle = FDType.Body.copy(color = FDColors.TextPrimary, fontSize = 13.5.sp),
                                        cursorBrush = SolidColor(FDColors.Primary),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { inner ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Escribe para filtrar (ej. Pasillo, Vitrina)...",
                                                    style = FDType.Body.copy(color = FDColors.TextTertiary, fontSize = 12.5.sp)
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = FDColors.TextTertiary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Formulario de creación rápida
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PREFIJOS_RAPIDOS.forEach { prefijo ->
                                        Surface(
                                            color = FDColors.Surface,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border),
                                            modifier = Modifier
                                                .clickable {
                                                    if (!nuevaUbicacionInput.startsWith(prefijo)) {
                                                        nuevaUbicacionInput = prefijo + nuevaUbicacionInput.trimStart()
                                                    }
                                                }
                                                .bounceClick()
                                        ) {
                                            Text(
                                                text = "+ $prefijo",
                                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                ConfiguracionTextField(
                                    label = "Nombre de Ubicación *",
                                    value = nuevaUbicacionInput,
                                    onValueChange = { if (it.length <= 40) nuevaUbicacionInput = it },
                                    placeholder = "Ej. Pasillo 3 - Estante Superior...",
                                    leadingIcon = Icons.Outlined.AddLocationAlt
                                )
                            }
                        }
                    }

                    // ── 3. LISTA DE RESULTADOS (CUERPO CENTRAL) ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        if (!modoCreacion) {
                            if (opcionesFiltradas.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.Place, null, tint = FDColors.TextTertiary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (queryLimpia.isBlank())
                                            "No tienes ubicaciones registradas aún en tu farmacia."
                                        else
                                            "No existe ninguna ubicación con \"$queryLimpia\".",
                                        style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            nuevaUbicacionInput = queryLimpia
                                            modoCreacion = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FDColors.Primary,
                                            contentColor = FDColors.PrimaryText
                                        ),
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (queryLimpia.isNotBlank()) "Crear \"$queryLimpia\"" else "Crear Primera Ubicación",
                                            style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(opcionesFiltradas) { item ->
                                        val isSelected = item.equals(ubicacionActual, ignoreCase = true)
                                        Surface(
                                            color = if (isSelected) FDColors.Primary.copy(alpha = 0.12f) else FDColors.Surface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) FDColors.Primary else FDColors.Border
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onUbicacionSelected(item)
                                                    onDismiss()
                                                }
                                                .bounceClick()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Place,
                                                        contentDescription = null,
                                                        tint = if (isSelected) FDColors.Primary else FDColors.TextTertiary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = item,
                                                        style = FDType.Body.copy(
                                                            fontSize = 13.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) FDColors.Primary else FDColors.TextPrimary
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (isSelected) {
                                                    Surface(
                                                        color = FDColors.Primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.Check, null, tint = FDColors.PrimaryText, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Espacio informativo en modo creación
                            Surface(
                                color = FDColors.Surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, FDColors.Border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 Esta nueva ubicación se guardará en el catálogo general de tu farmacia y podrá ser elegida por cualquier otro producto.",
                                    style = FDType.Caption.copy(fontSize = 12.sp, color = FDColors.TextSecondary),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    // ── 4. BOTONES INFERIORES DE ACCIÓN ──
                    HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!modoCreacion) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cerrar", style = FDType.Label.copy(fontSize = 12.sp))
                            }

                            Button(
                                onClick = {
                                    nuevaUbicacionInput = queryLimpia
                                    modoCreacion = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .bounceClick()
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crear Nueva Ubicación", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    modoCreacion = false
                                    nuevaUbicacionInput = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FDColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Volver a Lista", style = FDType.Label.copy(fontSize = 12.sp))
                            }

                            Button(
                                onClick = {
                                    if (esNuevaValida) {
                                        onUbicacionSelected(textoNuevaLimpia)
                                        onDismiss()
                                    }
                                },
                                enabled = esNuevaValida,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText,
                                    disabledContainerColor = FDColors.Surface,
                                    disabledContentColor = FDColors.TextDisabled
                                ),
                                border = if (!esNuevaValida) BorderStroke(0.5.dp, FDColors.Border) else null,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .let { if (esNuevaValida) it.bounceClick() else it }
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Asignar y Guardar", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
