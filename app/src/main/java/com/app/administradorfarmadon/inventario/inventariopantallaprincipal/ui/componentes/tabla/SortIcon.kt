package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioSortColumn
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioSortDirection
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.SaaSPrimary

@Composable
fun SortIcon(
    column: InventarioSortColumn,
    currentSort: InventarioSortColumn,
    direction: InventarioSortDirection
) {
    if (column == currentSort) {
        Icon(
            imageVector = if (direction == InventarioSortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = SaaSPrimary,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(12.dp)
        )
    }
}
