package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioFilterLogic
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioFilterState
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSidePanel(
    filterState: InventarioFilterState,
    products: List<PharmProduct>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onToggleEstadoStock: (String) -> Unit,
    onToggleClasificacion: (String) -> Unit,
    onToggleCategoria: (String) -> Unit,
    onLaboratorioChanged: (String) -> Unit,
    onVencimientoChanged: (String) -> Unit,
    onPrecioMaxChanged: (Float) -> Unit,
    onUbicacionChanged: (String) -> Unit,
    onToggleSoloConLotes: () -> Unit,
    onToggleRequiereReceta: () -> Unit,
    onToggleSoloRefrigerados: () -> Unit,
    onToggleStockBajoMinimo: () -> Unit,
    onClearAll: () -> Unit,
    onApply: () -> Unit,
    s: MedidaAdaptativa,
    laboratorios: List<String> = emptyList(),
    ubicaciones: List<String> = emptyList(),
    categorias: List<String> = emptyList(),
    precioMaxDisponible: Float = 0f,
    selectedSortOption: com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption = com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption.FECHA_CREACION,
    onSortSelected: (com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption) -> Unit = {}
) {
    val panelBg = FDColors.Background
    val surfaceColor = FDColors.SurfaceElevated
    val borderColor = FDColors.Border
    val textPrimary = FDColors.TextPrimary
    val textSecondary = FDColors.TextSecondary

    val filteredCount = remember(products, filterState) {
        InventarioFilterLogic.applyFilters(products, filterState).size
    }

    Column(
        modifier = modifier
            .background(panelBg)
            .border(width = 0.5.dp, color = borderColor, shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
    ) {
        // ── HEADER — simétrico ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILTROS AVANZADOS",
                        style = FDType.Heading2.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary, letterSpacing = 0.5.sp)
                    )
                    Text(
                        text = "$filteredCount productos · filtra al tocar",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = textSecondary)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .background(surfaceColor, CircleShape)
                        .border(0.5.dp, borderColor, CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // ── CONTENT — herramienta real, sin duplicar métricas ni chips ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. POR LABORATORIO — para pedidos a proveedor (agrupa y llamas una vez)
            FilterDropdownSection(
                title = "LABORATORIO (para pedir)",
                hint = "Todos los laboratorios",
                options = laboratorios,
                selected = filterState.laboratorio,
                onSelected = onLaboratorioChanged
            )

            // 2. POR UBICACIÓN — para encontrar físico / conteo
            FilterDropdownSection(
                title = "UBICACIÓN EN TIENDA",
                hint = "Todas las ubicaciones",
                options = ubicaciones,
                selected = filterState.ubicacion,
                onSelected = onUbicacionChanged
            )

            HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 3. CONTROL DE VENTA — solo lo que existe (sin mentira decorativa)
            val controlCounts = remember(products) {
                mapOf(
                    "venta_libre" to products.count { it.clasificacionControl == "VENTA_LIBRE" },
                    "receta" to products.count { it.clasificacionControl in listOf("RX", "CONTROLADO", "RX_RETENCION") || it.controlReceta },
                    "controlado" to products.count { it.clasificacionControl in listOf("CONTROLADO", "RX_RETENCION") },
                    "refrigerado" to products.count { it.requiereRefrigeracion }
                )
            }
            // Solo muestra la sección si hay al menos 1 con control (si solo Coca Cola venta libre, no muestra Controlado)
            if (controlCounts.values.any { it > 0 }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("CONTROL DE VENTA")
                    Text("Solo lo que existe en tu inventario.", style = FDType.Caption.copy(fontSize = 11.sp, color = textSecondary))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if ((controlCounts["venta_libre"] ?: 0) > 0) FilterChipReal(label = "Venta libre", selected = "venta_libre" in filterState.clasificacion, onClick = { onToggleClasificacion("venta_libre") })
                        if ((controlCounts["receta"] ?: 0) > 0) FilterChipReal(label = "Con receta", selected = "receta" in filterState.clasificacion, onClick = { onToggleClasificacion("receta") })
                        if ((controlCounts["controlado"] ?: 0) > 0) FilterChipReal(label = "Controlado", selected = "controlado" in filterState.clasificacion, onClick = { onToggleClasificacion("controlado") })
                        if ((controlCounts["refrigerado"] ?: 0) > 0) FilterChipReal(label = "Refrigerado", selected = "refrigerado" in filterState.clasificacion, onClick = { onToggleClasificacion("refrigerado") })
                    }
                }
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 4. VENCIMIENTO — solo rangos con producto real (sin decoración)
            val vencCounts = remember(products) {
                val hoy = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
                mapOf(
                    "Próx. 30 días" to products.count { it.expiryTimestamp in hoy..(hoy + 30L*86400000) },
                    "Próx. 90 días" to products.count { it.expiryTimestamp in hoy..(hoy + 90L*86400000) },
                    "Próx. 6 meses" to products.count { it.expiryTimestamp in hoy..(hoy + 180L*86400000) },
                    "Ya vencido" to products.count { it.expiryTimestamp in 1L until hoy }
                )
            }
            val hasVenc = vencCounts.values.any { it > 0 }
            if (hasVenc) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("VENCIMIENTO (planificación)")
                    Text("Solo rangos que existen en tu inventario.", style = FDType.Caption.copy(fontSize = 11.sp, color = textSecondary))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Próx. 30 días", "Próx. 90 días", "Próx. 6 meses", "Ya vencido").forEach { opt ->
                            if ((vencCounts[opt] ?: 0) > 0) {
                                FilterChipReal(label = opt, selected = filterState.vencimiento == opt, onClick = { onVencimientoChanged(if (filterState.vencimiento == opt) "" else opt) })
                            }
                        }
                    }
                }
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            // 5. ORDENAR — selector único (dropdown), no chips sueltos
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("ORDENAR")
                Text("Recién agregados es el orden por defecto.", style = FDType.Caption.copy(fontSize = 11.sp, color = textSecondary))
                var ordenExpanded by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { ordenExpanded = true },
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, FDColors.Border),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(selectedSortOption.label, style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextPrimary, fontWeight = FontWeight.Medium), maxLines = 1)
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = FDColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = ordenExpanded, onDismissRequest = { ordenExpanded = false }, modifier = Modifier.background(FDColors.SurfaceElevated).border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))) {
                        val opts = listOf(
                            com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption.FECHA_CREACION,
                            com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption.FECHA_ANTIGUOS,
                            com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.SortOption.ALFABETICO_AZ
                        )
                        opts.forEach { opt ->
                            val isSel = selectedSortOption == opt
                            DropdownMenuItem(
                                text = { Text(opt.label, style = FDType.Body.copy(fontSize = 12.sp, color = if (isSel) FDColors.Primary else FDColors.TextPrimary, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)) },
                                onClick = { onSortSelected(opt); ordenExpanded = false },
                                leadingIcon = if (isSel) { { Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(14.dp)) } } else null
                            )
                        }
                    }
                }
            }

            // Mensaje honesto si no hay datos para filtrar
            if (laboratorios.isEmpty() && ubicaciones.isEmpty()) {
                Surface(color = FDColors.SurfaceElevated, shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, borderColor)) {
                    Text(
                        text = "Aún no hay laboratorios ni ubicaciones cargadas. Crea productos con laboratorio y ubicación y aquí aparecerán para filtrar.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = textSecondary),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // ── FOOTER — solo Limpiar cuando hay >1 filtro (filtra al tocar, sin botón Ver) ──
        // Si hay un solo filtro, se quita tocando el chip/dropdown mismo (uno por uno). Limpiar limpia todo de verdad.
        val activeCount = remember(filterState) {
            var c = 0
            if (filterState.laboratorio.isNotBlank()) c++
            if (filterState.ubicacion.isNotBlank()) c++
            if (filterState.clasificacion.isNotEmpty()) c += filterState.clasificacion.size
            if (filterState.vencimiento.isNotBlank()) c++
            // categorías no están en panel, pero por si acaso
            if (filterState.categorias.isNotEmpty()) c += filterState.categorias.size
            c
        }
        if (activeCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        // Limpia realmente todo (verificado: clearAllFilters resetea laboratorio, ubicacion, clasificacion, vencimiento, categorias)
                        onClearAll()
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Limpiar filtros ($activeCount)", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
                }
            }
        } else {
            // Sin footer cuando 0-1 filtro: el chip/dropdown se deselecciona tocándolo de nuevo, sin necesidad de botón
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = FDColors.TextTertiary,
        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun FilterDropdownSection(title: String, hint: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(title)
        Box {
            Surface(
                onClick = { if (options.isNotEmpty()) expanded = true },
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, if (selected.isNotBlank()) FDColors.Primary else FDColors.Border),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (selected.isBlank()) hint else selected,
                        style = FDType.Body.copy(fontSize = 12.5.sp, color = if (selected.isBlank()) FDColors.TextTertiary else FDColors.TextPrimary, fontWeight = if (selected.isBlank()) FontWeight.Normal else FontWeight.Medium),
                        maxLines = 1
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = if (selected.isNotBlank()) FDColors.Primary else FDColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FDColors.SurfaceElevated).border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(text = { Text(hint, style = FDType.Body.copy(fontSize = 12.sp, color = FDColors.TextSecondary)) }, onClick = { onSelected(""); expanded = false })
                options.forEach { opt ->
                    val isSel = selected == opt
                    DropdownMenuItem(
                        text = { Text(opt, style = FDType.Body.copy(fontSize = 12.sp, color = if (isSel) FDColors.Primary else FDColors.TextPrimary, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)) },
                        onClick = { onSelected(opt); expanded = false },
                        leadingIcon = if (isSel) { { Icon(Icons.Default.Check, null, tint = FDColors.Primary, modifier = Modifier.size(14.dp)) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipReal(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, if (selected) FDColors.Primary else FDColors.Border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = FDType.Label.copy(color = if (selected) FDColors.TextPrimary else FDColors.TextSecondary, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        )
    }
}
