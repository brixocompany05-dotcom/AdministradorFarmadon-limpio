package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.buscador

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioFilterState
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioViewModel

@Composable
fun FilterTagsRow(
    filterState: InventarioFilterState,
    selectedCategories: Set<String>,
    selectedUseCases: Set<String>,
    viewModel: InventarioViewModel,
    s: MedidaAdaptativa
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Filtros:",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = s.textLabel,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        
        filterState.estadoStock.forEach { e -> 
            FilterTag(e, when (e) { "Disponible" -> FDColors.Success; "Stock bajo" -> FDColors.Warning; "Por vencer" -> FDColors.Error; else -> FDColors.Primary }) { 
                viewModel.updateFilter { copy(estadoStock = estadoStock - e) }
                viewModel.resetAndReload()
            } 
        }
        filterState.clasificacion.forEach { c -> 
            FilterTag(c, FDColors.Primary) { 
                viewModel.updateFilter { copy(clasificacion = clasificacion - c) }
                viewModel.resetAndReload()
            } 
        }
        selectedCategories.forEach { c -> 
            FilterTag(c, FDColors.Primary) { 
                viewModel.toggleCategoryFilter(c)
                viewModel.resetAndReload()
            } 
        }
        selectedUseCases.forEach { u -> 
            FilterTag(u, FDColors.Primary) { 
                viewModel.toggleUseCaseFilter(u)
                viewModel.resetAndReload()
            } 
        }
        if (filterState.laboratorio.isNotBlank()) FilterTag(filterState.laboratorio, FDColors.Primary) { viewModel.updateFilter { copy(laboratorio = "") }; viewModel.resetAndReload() }
        if (filterState.vencimiento.isNotBlank()) FilterTag(filterState.vencimiento, FDColors.Warning) { viewModel.updateFilter { copy(vencimiento = "") }; viewModel.resetAndReload() }
        if (filterState.precioMax < Float.MAX_VALUE) FilterTag("Precio < $${filterState.precioMax.toInt()}", FDColors.Primary) { viewModel.updateFilter { copy(precioMax = Float.MAX_VALUE) }; viewModel.resetAndReload() }
        if (filterState.ubicacion.isNotBlank()) FilterTag(filterState.ubicacion, FDColors.Primary) { viewModel.updateFilter { copy(ubicacion = "") }; viewModel.resetAndReload() }
        if (filterState.soloConLotes) FilterTag("Con lotes", FDColors.Primary) { viewModel.updateFilter { copy(soloConLotes = false) }; viewModel.resetAndReload() }
        if (filterState.requiereReceta) FilterTag("Receta", FDColors.Error) { viewModel.updateFilter { copy(requiereReceta = false) }; viewModel.resetAndReload() }
        if (filterState.soloRefrigerados) FilterTag("Refrigerado", FDColors.Primary) { viewModel.updateFilter { copy(soloRefrigerados = false) }; viewModel.resetAndReload() }
        if (filterState.stockBajoMinimo) FilterTag("Bajo mínimo", FDColors.Warning) { viewModel.updateFilter { copy(stockBajoMinimo = false) }; viewModel.resetAndReload() }
    }
}
