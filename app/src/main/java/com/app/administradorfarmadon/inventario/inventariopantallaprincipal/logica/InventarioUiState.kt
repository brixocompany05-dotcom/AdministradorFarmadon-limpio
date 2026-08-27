package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Estado inmutable de la pantalla de inventario con paginación silenciosa.
 *
 * Reglas:
 * - Un único estado honesto [estadoCarga] con 4 variantes: Cargando / Listo / Vacio / Error.
 * - La UI nunca ve "50" ni el cursor; solo ve lista infinita y pie "Cargando más...".
 * - Lista acumulada [productsList] crece lote a lote (limit 50 interno en Repository).
 * - Cursor [ultimoDoc] = último DocumentSnapshot de Firestore para startAfter().
 * - [isLoadingMore] = true mientras se trae la siguiente página (debounce 300ms en ViewModel).
 * - Búsqueda con debounce 300ms se aplica dentro del ViewModel sobre [searchQuery].
 */
sealed interface InventarioCargaEstado {
    data object Cargando : InventarioCargaEstado
    data object Listo : InventarioCargaEstado
    data object Vacio : InventarioCargaEstado
    data class Error(val mensaje: String) : InventarioCargaEstado
}

/**
 * Estados honestos para búsqueda server-side — 100% en ViewModel.
 * Nunca filtra local; Repo hace whereGreaterThanOrEqualTo...limit(50).
 * Cargando / BusquedaVacia (alias BusquedaVacía) / Error
 */
sealed interface InventarioBusquedaEstado {
    data object Idle : InventarioBusquedaEstado
    data object Cargando : InventarioBusquedaEstado
    data object BusquedaVacia : InventarioBusquedaEstado
    data object BusquedaVacía : InventarioBusquedaEstado
    data class Error(val mensaje: String) : InventarioBusquedaEstado
    data class Exito(val resultados: List<PharmProduct>) : InventarioBusquedaEstado
}

data class InventarioUiState(
    // Estado honesto central — gobierna toda la pantalla
    val estadoCarga: InventarioCargaEstado = InventarioCargaEstado.Cargando,
    // Alias compatibilidad con código que lee isLoading / errorMessage
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // Lista acumulada silenciosa — crece con cada página de 50, UI ve infinita
    val productsList: List<PharmProduct> = emptyList(),
    val listaAcumulada: List<PharmProduct> = emptyList(),
    // Cursor para paginación Firestore: último DocumentSnapshot de la página anterior
    val ultimoDoc: DocumentSnapshot? = null,
    val ultimoDocumento: DocumentSnapshot? = null,
    // Flag silencioso de carga incremental — dispara pie "Cargando más..."
    val isLoadingMore: Boolean = false,
    val isNextPageLoading: Boolean = false,
    // Búsqueda con debounce 300ms (gestionado en ViewModel, reflejado aquí)
    val searchQuery: String = "",
    val debounceMs: Long = 300L,
    // Banderas paginación silenciosa
    val endOfListReached: Boolean = false,
    val lastLoadedKey: String? = null,
    // Filtros / categorías / métricas
    val selectedCategory: String = "Todos",
    val filteredProducts: List<PharmProduct> = emptyList(),
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
    val activeProductsCount: Int = 0,
    // Compatibilidad paginación visible deprecada — no usada por UI infinita
    val currentPage: Int = 1,
    val itemsPerPage: Int = 10,
    val totalPages: Int = 1,
    // Búsqueda server-side silenciosa — estados honestos 300ms debounce
    val busquedaEstado: InventarioBusquedaEstado = InventarioBusquedaEstado.Idle,
    val isEnBusqueda: Boolean = false,
    val resultadosBusqueda: List<PharmProduct> = emptyList(),
    val busquedaError: String? = null,
    val metricsTrends: Map<String, String> = emptyMap()
)
