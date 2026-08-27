package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentSnapshot

@Immutable
data class PharmProduct(
    val id: String,
    val name: String,
    val presentation: String,
    val inventoryVisualSummary: String = "",
    val stockHumanReadable: String = "",
    val stockBaseReadable: String = "",
    val minStockHumanReadable: String = "",
    val code: String,
    val laboratory: String,
    val category: String,
    val categories: List<String> = emptyList(),
    val empaque: String = "",
    val stock: Int,
    val stockBloqueado: Int = 0, // unidades en cuarentena/bloqueo (NO vendibles)
    val minStock: Int,
    val expiryDate: String,
    val status: String, // "Disponible", "Stock bajo", "Por vencer"
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0, // Precio de venta vigente de la presentación mayor (espejo "precioVenta")
    val nearestLoteNumero: String = "", // Lote vendible más próximo a vencer (el vencimiento es DEL lote)
    val totalValue: Double = 0.0,
    val useCases: List<String> = emptyList(),
    // Campos para filtrado avanzado
    val clasificacionControl: String = "VENTA_LIBRE",
    val requiereRefrigeracion: Boolean = false,
    val controlReceta: Boolean = false,
    val ubicacion: String = "",
    val expiryTimestamp: Long = 0L,
    val createdAtTimestamp: Long = 0L,
    // Campos de Especificacion
    val concentration: String = "",
    val content: String = "",
    val contentUnit: String = "",
    val imageUrl: String? = null,
    val secondaryCodes: List<String> = emptyList(),
    val activo: Boolean = true,
    val estado: String = "ACTIVO",
    val permiteFraccionar: Boolean = false,
    val proveedor: String = "" // Proveedor comercial / Droguería a quien se le compra (distinto del laboratorio fabricante)
)

enum class InventarioSortColumn {
    NONE, PRODUCTO, STOCK, VENCE, ESTADO
}

enum class InventarioSortDirection {
    ASC, DESC
}

enum class SortOption(val label: String, val column: InventarioSortColumn, val direction: InventarioSortDirection) {
    FECHA_CREACION("Recien agregados", InventarioSortColumn.NONE, InventarioSortDirection.DESC),
    FECHA_ANTIGUOS("Mas antiguos", InventarioSortColumn.NONE, InventarioSortDirection.ASC),
    ALFABETICO_AZ("Nombre (A-Z)", InventarioSortColumn.PRODUCTO, InventarioSortDirection.ASC)
}

// InventarioCargaEstado / InventarioBusquedaEstado viven en InventarioUiState.kt — fuente única.
// Estados honestos: Cargando / Listo / Vacio / Error y Cargando / BusquedaVacía / Error (ver InventarioUiState.kt)

data class InventarioUIStateLegacy(
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val ultimoDoc: DocumentSnapshot? = null,
    val ultimoDocumento: DocumentSnapshot? = null,
    val listaAcumulada: List<PharmProduct> = emptyList(),
    val debounceMs: Long = 300L,
    val endOfListReached: Boolean = false,
    val lastLoadedKey: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    // Lista acumulada silenciosa — crece lote a lote (limit 50 interno), UI ve lista infinita
    val productsList: List<PharmProduct> = emptyList(),
    val filteredProducts: List<PharmProduct> = emptyList(),
    // pagedProducts se mantiene por compatibilidad pero ya no gobierna la UI; la lista infinita es productsList filtrada
    val pagedProducts: List<PharmProduct> = emptyList(),
    val categories: List<String> = listOf("Todos"),
    val totalProductsCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val nearExpiryCount: Int = 0,
    val searchSuggestion: String? = null,
    val aliasMap: Map<String, String> = emptyMap(),
    val selectedCategories: Set<String> = emptySet(),
    val selectedUseCases: Set<String> = emptySet(),
    val allUseCasesList: List<String> = emptyList(),
    val isFilterPanelOpen: Boolean = false,
    val categoriesWithCounts: List<Pair<String, Int>> = emptyList(),
    val useCasesWithCounts: List<Pair<String, Int>> = emptyList(),
    val sortColumn: InventarioSortColumn = InventarioSortColumn.NONE,
    val sortDirection: InventarioSortDirection = InventarioSortDirection.DESC,
    val selectedSortOption: SortOption = SortOption.FECHA_CREACION,
    val readAlertIds: Set<String> = emptySet(),
    val pendingNewProductIds: Set<String> = emptySet(),
    val isRealtimeConnected: Boolean = false,
    
    // Rediseño 2026
    val activeProductsCount: Int = 0,
    // Paginacion visible DEPRECADA — mantenida solo por compatibilidad, UI no muestra "Pagina X de Y"
    val currentPage: Int = 1,
    val itemsPerPage: Int = 10,
    val totalPages: Int = 1,
    val busquedaEstado: InventarioBusquedaEstado = InventarioBusquedaEstado.Idle,
    val isEnBusqueda: Boolean = false,
    val resultadosBusqueda: List<PharmProduct> = emptyList(),
    val busquedaError: String? = null,
    val errorMessage: String? = null,
    val metricsTrends: Map<String, String> = emptyMap(),
    // Estado honesto central para paginacion silenciosa
    val estadoCarga: InventarioCargaEstado = InventarioCargaEstado.Cargando
)

@Deprecated("Usar InventarioUiState — fuente única")
typealias InventarioUIState = InventarioUIStateLegacy
