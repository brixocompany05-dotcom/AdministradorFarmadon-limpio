package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.base_datos.AlertPersistenceManager
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.logica.InventarioAlertasLogic
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.FlowPreview::class)
class InventarioViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = InventarioFirestoreRepository()
    private val calculator = InventoryMetricsCalculator()

    private val _readAlerts =
        MutableStateFlow<Map<String, AlertPersistenceManager.AlertReadRecord>>(emptyMap())

    private val _uiState = MutableStateFlow(
        InventarioUiState(aliasMap = mapOf("acetaminofen" to "Paracetamol"))
    )
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    @Suppress("unused")
    val uiStateCompat: StateFlow<InventarioUIStateLegacy> get() = _uiStateCompat
    private val _uiStateCompat: StateFlow<InventarioUIStateLegacy> = _uiState.map { s ->
        InventarioUIStateLegacy(
            isLoading = s.isLoading,
            isNextPageLoading = s.isNextPageLoading,
            isLoadingMore = s.isLoadingMore,
            ultimoDoc = s.ultimoDoc,
            ultimoDocumento = s.ultimoDocumento,
            listaAcumulada = s.listaAcumulada,
            productsList = s.productsList,
            filteredProducts = s.filteredProducts,
            pagedProducts = s.pagedProducts,
            estadoCarga = s.estadoCarga,
            searchQuery = s.searchQuery,
            categories = s.categories,
            totalProductsCount = s.totalProductsCount,
            totalInventoryValue = s.totalInventoryValue,
            lowStockCount = s.lowStockCount,
            nearExpiryCount = s.nearExpiryCount,
            activeProductsCount = s.activeProductsCount,
            currentPage = s.currentPage,
            itemsPerPage = s.itemsPerPage,
            totalPages = s.totalPages,
            errorMessage = s.errorMessage,
            endOfListReached = s.endOfListReached,
            busquedaEstado = s.busquedaEstado,
            isEnBusqueda = s.isEnBusqueda,
            resultadosBusqueda = s.resultadosBusqueda,
            busquedaError = s.busquedaError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventarioUIStateLegacy())

    private val _filterState = MutableStateFlow(InventarioFilterState())
    val filterState: StateFlow<InventarioFilterState> = _filterState.asStateFlow()

    private val _estadoTab = MutableStateFlow("TODOS")
    val estadoTab: StateFlow<String> = _estadoTab.asStateFlow()

    // Cursor paginación silenciosa — último DocumentSnapshot para startAfter()
    private var ultimoDoc: DocumentSnapshot? = null
    private var finListaAlcanzado = false
    private var jobPagina: Job? = null
    private var jobCargarMas: Job? = null
    private var debounceMasJob: Job? = null

    // Búsqueda server-side silenciosa — cursor y estado honesto
    private var cursorBusqueda: DocumentSnapshot? = null
    private var finBusqueda = false
    private var jobBusqueda: Job? = null
    private var textoBusquedaActual: String = ""
    private var busquedaRequestId: Long = 0L
    private var cargaSucursalGeneration: Long = 0L
    private var conteoGlobalProductos: Int = 0
    private var conteoGlobalActivos: Int = 0

    // Filtrado base SIN búsqueda (búsqueda es 100% server, no depende de los 50 cargados)
    // Debounce 300ms para filtros locales suaves, pero búsqueda tiene su propio debounce 300 server
    private val _filtradoBase = combine(
        _uiState.map { it.productsList }.distinctUntilChanged(),
        _filterState,
        _estadoTab,
        _uiState.map { it.selectedSortOption }.distinctUntilChanged()
    ) { products, filter, tab, sortOption ->
        var base = InventarioFilterLogic.applyFilters(products, filter)
        base = when (tab) {
            "TODOS" -> base
            "POR_REPONER" -> base.filter { it.status == "Stock bajo" || it.status == "Agotado" || it.stock <= it.minStock }
            "POR_VENCER" -> base.filter { it.status == "Por vencer" || it.status == "Vencido" || (it.expiryTimestamp in 1L..com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs() + (90L * 24 * 60 * 60 * 1000) && it.expiryDate.isNotBlank()) }
            "PAUSADOS", "PAUSADO" -> base.filter { !it.activo }
            "ACTIVO" -> base.filter { it.activo }
            else -> base
        }
        when (sortOption) {
            SortOption.ALFABETICO_AZ -> base.sortedBy { it.name.lowercase() }
            SortOption.FECHA_ANTIGUOS -> base.sortedWith(
                compareBy(
                    { it.createdAtTimestamp },
                    { it.name.lowercase() })
            )

            else -> base.sortedWith(compareByDescending<PharmProduct> { it.createdAtTimestamp }.thenBy { it.name.lowercase() })
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mantener compatibilidad: filteredProducts expone lista visible (busqueda server si aplica, sino base filtrada)
    private val _filteredProducts = combine(
        _filtradoBase,
        _uiState.map { it.busquedaEstado }.distinctUntilChanged(),
        _uiState.map { it.isEnBusqueda }.distinctUntilChanged(),
        _uiState.map { it.resultadosBusqueda }.distinctUntilChanged()
    ) { base, busquedaEstado, enBusqueda, resultadosBusqueda ->
        if (enBusqueda) {
            when (busquedaEstado) {
                is InventarioBusquedaEstado.Exito -> {
                    // Para búsqueda, respetar tab/sort pero sin re-filtrar por nombre (ya filtró server)
                    var b = resultadosBusqueda
                    // Aplicar tab sobre resultados de búsqueda para coherencia, pero no filtro de texto local
                    val tabFiltered = when (_estadoTab.value) {
                        "TODOS" -> b
                        "POR_REPONER" -> b.filter { it.status == "Stock bajo" || it.status == "Agotado" || it.stock <= it.minStock }
                        "POR_VENCER" -> b.filter { it.status == "Por vencer" || it.status == "Vencido" || (it.expiryTimestamp in 1L..com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs() + (90L * 24 * 60 * 60 * 1000) && it.expiryDate.isNotBlank()) }
                        "PAUSADOS", "PAUSADO" -> b.filter { !it.activo }
                        "ACTIVO" -> b.filter { it.activo }
                        else -> b
                    }
                    // Aplicar filtros de panel (estadoStock, etc.) sobre resultados búsqueda para utilidad, pero sin filtro local de nombre
                    val panelFiltered =
                        InventarioFilterLogic.applyFilters(tabFiltered, _filterState.value)
                    when (_uiState.value.selectedSortOption) {
                        SortOption.ALFABETICO_AZ -> panelFiltered.sortedBy { it.name.lowercase() }
                        SortOption.FECHA_ANTIGUOS -> panelFiltered.sortedWith(
                            compareBy(
                                { it.createdAtTimestamp },
                                { it.name.lowercase() })
                        )

                        else -> panelFiltered.sortedWith(compareByDescending<PharmProduct> { it.createdAtTimestamp }.thenBy { it.name.lowercase() })
                    }
                }

                is InventarioBusquedaEstado.BusquedaVacia, is InventarioBusquedaEstado.BusquedaVacía -> emptyList()
                is InventarioBusquedaEstado.Cargando -> emptyList()
                is InventarioBusquedaEstado.Error -> emptyList()
                else -> base
            }
        } else {
            base
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<PharmProduct>> = _filteredProducts

    val alertas: StateFlow<List<InventarioAlertasLogic.AlertaProducto>> =
        _uiState.map { InventarioAlertasLogic.calcularAlertas(it.productsList) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertsCount: StateFlow<Int> = combine(alertas, _readAlerts) { a, r ->
        a.count { al -> val rec = r[AlertPersistenceManager.readKey(al.productId, al.tipo.name)]; rec == null || rec.lastMessage != al.mensaje }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadAlertIds: StateFlow<Set<String>> = combine(alertas, _readAlerts) { a, r ->
        a.filter { al -> val rec = r[AlertPersistenceManager.readKey(al.productId, al.tipo.name)]; rec == null || rec.lastMessage != al.mensaje }
            .map { AlertPersistenceManager.readKey(it.productId, it.tipo.name) }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val readInfoMap: StateFlow<Map<String, String>> = _readAlerts.map { m ->
        m.mapValues { (_, rec) -> formatReadTimestamp(rec.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        SessionManager.init(application)
        viewModelScope.launch {
            cargarPaginaInicial()
            loadReadAlerts()
            observarBusquedaServerSide()
        }
        // Sincroniza pagedProducts con lista visible infinita (sin cortes de página)
        _filteredProducts.onEach { visible ->
            // Solo actualiza pagedProducts si NO está en carga de búsqueda (evita parpadeo Cargando)
            val enBusqueda = _uiState.value.isEnBusqueda
            val busquedaCargando =
                _uiState.value.busquedaEstado is InventarioBusquedaEstado.Cargando
            if (!enBusqueda || !busquedaCargando) {
                _uiState.update {
                    it.copy(
                        pagedProducts = visible,
                        filteredProducts = visible,
                        totalProductsCount = visible.size
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Mide el desfase entre el reloj del dispositivo y el reloj de Firestore.
     * 3 intentos; si todos fallan, se continúa con reloj local (degradación
     * honesta documentada) — jamás inventa fecha.
     */
    private suspend fun sincronizarHoraServidor(): Boolean {
        repeat(3) { intento ->
            try {
                val docRef = FarmadonFirestore.db
                    .collection("_health").document("ping")
                    .collection("hora_fmd").document()
                val antes = System.currentTimeMillis()
                com.google.android.gms.tasks.Tasks.await(
                    docRef.set(mapOf("ts" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                )
                val srv = docRef.get().await().getTimestamp("ts")?.toDate()?.time ?: return@repeat
                com.app.administradorfarmadon.compartido.logica.HoraServidor.establecerOffset(srv - System.currentTimeMillis())
                android.util.Log.i(
                    "InventarioVM",
                    "HoraServidor offset=${srv - antes}ms (intento ${intento + 1})"
                )
                return true
            } catch (e: Exception) {
                android.util.Log.w(
                    "InventarioVM",
                    "Sincronización de hora falló (intento ${intento + 1}): ${e.message}"
                )
            }
        }
        return false
    }

    // ── Observador de búsqueda server-side con debounce 300ms y estados honestos ──
    private fun observarBusquedaServerSide() {
        _uiState.map { it.searchQuery }.distinctUntilChanged()
            .debounce(300.milliseconds)
            .onEach { queryRaw ->
                val query = queryRaw.trim()
                // Reset paginación búsqueda
                cursorBusqueda = null
                finBusqueda = false
                textoBusquedaActual = query
                jobBusqueda?.cancel()
                if (query.isEmpty()) {
                    // Salir de búsqueda: estados honestos Idle, volver a lista base infinita
                    _uiState.update {
                        it.copy(
                            isEnBusqueda = false,
                            endOfListReached = finListaAlcanzado,
                            busquedaEstado = InventarioBusquedaEstado.Idle,
                            resultadosBusqueda = emptyList(),
                            busquedaError = null,
                            isLoading = false,
                            isLoadingMore = false,
                            isNextPageLoading = false,
                            estadoCarga = if (it.productsList.isEmpty()) InventarioCargaEstado.Vacio else InventarioCargaEstado.Listo,
                            errorMessage = null
                        )
                    }
                    return@onEach
                }
                // Entrar en búsqueda: estado honesto Cargando
                _uiState.update {
                    it.copy(
                        isEnBusqueda = true,
                        busquedaEstado = InventarioBusquedaEstado.Cargando,
                        busquedaError = null,
                        isLoading = true,
                        isLoadingMore = false,
                        isNextPageLoading = false,
                        errorMessage = null,
                        estadoCarga = InventarioCargaEstado.Cargando
                    )
                }
                jobBusqueda?.cancel()
                val reqId = ++busquedaRequestId
                jobBusqueda = viewModelScope.launch {
                    try {
                        val farmaciaId = SessionManager.clienteIdGarantizado
                        val sucursalId = SessionManager.sucursalIdEfectiva
                        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
                            _uiState.update {
                                it.copy(
                                    busquedaEstado = InventarioBusquedaEstado.Error("Sesión no válida"),
                                    busquedaError = "Sesión no válida",
                                    isLoading = false,
                                    estadoCarga = InventarioCargaEstado.Error("Sesión no válida"),
                                    errorMessage = "Sesión no válida"
                                )
                            }
                            return@launch
                        }
                        // Llamada server-side: NO depende de los 50 ya cargados, Firestore filtra por prefijo
                        val pagina = repository.buscarInventarioPaginado(
                            farmaciaId = farmaciaId,
                            sucursalId = sucursalId,
                            texto = query,
                            limit = 50,
                            startAfterDoc = null
                        )
                        // Blindaje de correlación: Si ya se disparó otra búsqueda más reciente, descartar esta respuesta tardía
                        if (reqId != busquedaRequestId || _uiState.value.searchQuery.trim() != query.trim() || sucursalId != SessionManager.sucursalIdEfectiva) {
                            return@launch
                        }
                        cursorBusqueda = pagina.ultimoDocumento
                        finBusqueda = pagina.esUltimaPagina
                        val resultados = pagina.productos
                        if (resultados.isEmpty()) {
                            // Estado honesto BusquedaVacía / BusquedaVacia
                            _uiState.update {
                                it.copy(
                                    busquedaEstado = InventarioBusquedaEstado.BusquedaVacía,
                                    resultadosBusqueda = emptyList(),
                                    pagedProducts = emptyList(),
                                    filteredProducts = emptyList(),
                                    totalProductsCount = 0,
                                    isLoading = false,
                                    isLoadingMore = false,
                                    isNextPageLoading = false,
                                    estadoCarga = InventarioCargaEstado.Vacio,
                                    errorMessage = null
                                )
                            }
                        } else {
                            // Éxito: actualiza resultados acumulados (primera página)
                            _uiState.update {
                                it.copy(
                                    busquedaEstado = InventarioBusquedaEstado.Exito(resultados),
                                    resultadosBusqueda = resultados,
                                    // pagedProducts se actualizará vía _filteredProducts combine, pero también set directo para inmediatez
                                    pagedProducts = resultados,
                                    filteredProducts = resultados,
                                    totalProductsCount = resultados.size,
                                    isLoading = false,
                                    isLoadingMore = false,
                                    isNextPageLoading = false,
                                    estadoCarga = InventarioCargaEstado.Listo,
                                    endOfListReached = finBusqueda,
                                    errorMessage = null
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("InventarioViewModel", "Error búsqueda server-side: ${e.message}", e)
                        val msg = when {
                            e.message?.contains(
                                "offline",
                                ignoreCase = true
                            ) == true -> "Sin conexión. Verifica tu internet."

                            e.message?.contains(
                                "UNAVAILABLE",
                                ignoreCase = true
                            ) == true -> "Sin conexión con inventario. Verifica internet."

                            e.message?.contains(
                                "PERMISSION_DENIED",
                                ignoreCase = true
                            ) == true -> "No tienes permiso para buscar en este inventario."

                            else -> e.message ?: "Error buscando inventario"
                        }
                        _uiState.update {
                            it.copy(
                                busquedaEstado = InventarioBusquedaEstado.Error(msg),
                                busquedaError = msg,
                                isLoading = false,
                                isLoadingMore = false,
                                isNextPageLoading = false,
                                estadoCarga = InventarioCargaEstado.Error(msg),
                                errorMessage = msg
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    // ── Paginación silenciosa: primera página (limit 50) ──
    fun cargarPaginaInicial() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    estadoCarga = InventarioCargaEstado.Error("Sesión no iniciada"),
                    errorMessage = "Sesión no iniciada"
                )
            }
            return
        }
        val currentGen = ++cargaSucursalGeneration
        val sucursalEsperada = sucursalId
        finListaAlcanzado = false
        ultimoDoc = null
        _uiState.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                isNextPageLoading = false,
                endOfListReached = false,
                errorMessage = null,
                estadoCarga = InventarioCargaEstado.Cargando
            )
        }
        jobPagina?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val metricas = repository.obtenerMetricasGlobales(farmaciaId, sucursalId)
            if (metricas.totalProductos > 0) {
                conteoGlobalProductos = metricas.totalProductos
                conteoGlobalActivos = metricas.totalActivos
                _uiState.update { current ->
                    current.copy(
                        totalProductsCount = maxOf(current.totalProductsCount, metricas.totalProductos),
                        activeProductsCount = maxOf(current.activeProductsCount, metricas.totalActivos)
                    )
                }
            }
        }
        jobPagina = repository.observarInventarioPaginado(
            farmaciaId,
            sucursalId,
            limit = 50,
            startAfterDoc = null
        )
            .onEach { pagina ->
                onPagina(
                    pagina,
                    esInicial = true,
                    generation = currentGen,
                    sucursalEsperada = sucursalEsperada
                )
            }
            .catch { e ->
                Log.e("InventarioViewModel", "Error cargando página inicial: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isNextPageLoading = false,
                        estadoCarga = InventarioCargaEstado.Error("Sin conexión con inventario. Verifica internet."),
                        errorMessage = "Sin conexión con inventario. Verifica internet."
                    )
                }
            }.flowOn(Dispatchers.Default).launchIn(viewModelScope)
    }

    // ── Carga incremental silenciosa: debounce 300ms y soporte búsqueda paginable ──
    fun cargarMas() {
        // Si está en búsqueda server-side, paginar búsqueda silenciosamente
        if (_uiState.value.isEnBusqueda) {
            cargarMasBusqueda()
            return
        }
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || finListaAlcanzado) return
        debounceMasJob?.cancel()
        debounceMasJob = viewModelScope.launch {
            delay(300)
            ejecutarCargarMas()
        }
    }

    private fun cargarMasBusqueda() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || finBusqueda) return
        if (textoBusquedaActual.isBlank() || cursorBusqueda == null) return
        debounceMasJob?.cancel()
        debounceMasJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isLoadingMore = true, isNextPageLoading = true) }
            try {
                val farmaciaId = SessionManager.clienteIdGarantizado
                val sucursalId = SessionManager.sucursalIdEfectiva
                val pagina = repository.buscarInventarioPaginado(
                    farmaciaId = farmaciaId,
                    sucursalId = sucursalId,
                    texto = textoBusquedaActual,
                    limit = 50,
                    startAfterDoc = cursorBusqueda
                )
                cursorBusqueda = pagina.ultimoDocumento
                finBusqueda = pagina.esUltimaPagina
                val actuales = _uiState.value.resultadosBusqueda
                val mapa = actuales.associateBy { it.id }.toMutableMap()
                pagina.productos.forEach { mapa[it.id] = it }
                val acumulados = mapa.values.sortedBy { it.name.lowercase() }
                val estado: InventarioBusquedaEstado =
                    if (acumulados.isEmpty()) InventarioBusquedaEstado.BusquedaVacía else InventarioBusquedaEstado.Exito(
                        acumulados
                    )
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        isNextPageLoading = false,
                        endOfListReached = finBusqueda,
                        resultadosBusqueda = acumulados,
                        pagedProducts = acumulados,
                        filteredProducts = acumulados,
                        totalProductsCount = acumulados.size,
                        busquedaEstado = estado,
                        estadoCarga = if (acumulados.isEmpty()) InventarioCargaEstado.Vacio else InventarioCargaEstado.Listo
                    )
                }
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error paginando búsqueda: ${e.message}", e)
                val msg = e.message ?: "Error cargando más resultados"
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        isNextPageLoading = false,
                        busquedaEstado = InventarioBusquedaEstado.Error(msg),
                        busquedaError = msg
                    )
                }
            }
        }
    }

    private fun ejecutarCargarMas() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank() || ultimoDoc == null) return
        if (_uiState.value.isLoadingMore) return
        val currentGen = cargaSucursalGeneration
        val sucursalEsperada = sucursalId
        _uiState.update { it.copy(isLoadingMore = true, isNextPageLoading = true) }
        jobCargarMas?.cancel()
        jobCargarMas = repository.observarInventarioPaginado(
            farmaciaId,
            sucursalId,
            limit = 50,
            startAfterDoc = ultimoDoc
        )
            .onEach { pagina ->
                onPagina(
                    pagina,
                    esInicial = false,
                    generation = currentGen,
                    sucursalEsperada = sucursalEsperada
                )
            }
            .catch { e ->
                Log.e("InventarioViewModel", "Error cargando más: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        isNextPageLoading = false,
                        estadoCarga = InventarioCargaEstado.Error("Error cargando más productos.")
                    )
                }
            }.flowOn(Dispatchers.Default).launchIn(viewModelScope)
    }

    private fun onPagina(
        pagina: InventarioFirestoreRepository.PaginaInventario,
        esInicial: Boolean,
        generation: Long,
        sucursalEsperada: String
    ) {
        if (generation != cargaSucursalGeneration || sucursalEsperada != SessionManager.sucursalIdEfectiva) {
            // Descartar página de generación o sede obsoleta
            return
        }
        ultimoDoc = pagina.ultimoDocumento
        finListaAlcanzado = pagina.esUltimaPagina
        val actual = _uiState.value.productsList
        val combinada = if (esInicial) {
            pagina.productos
        } else {
            val mapa = actual.associateBy { it.id }.toMutableMap()
            pagina.productos.forEach { mapa[it.id] = it }
            mapa.values.sortedBy { it.name.lowercase() }
        }
        val estado: InventarioCargaEstado = when {
            combinada.isEmpty() -> InventarioCargaEstado.Vacio
            else -> InventarioCargaEstado.Listo
        }
        val cats = listOf("Todos") + combinada.flatMap {
            it.categories + it.category.split(",").map { c -> c.trim() }
        }.distinct().filter { it.isNotBlank() }.sorted()
        val catsWithCounts =
            combinada.flatMap { it.categories + it.category.split(",").map { c -> c.trim() } }
                .filter { it.isNotBlank() }.groupBy { it }.map { Pair(it.key, it.value.size) }
                .sortedByDescending { it.second }
        _uiState.update {
            it.copy(
                isLoading = false, isLoadingMore = false, isNextPageLoading = false,
                endOfListReached = finListaAlcanzado, lastLoadedKey = ultimoDoc?.id,
                ultimoDoc = ultimoDoc, ultimoDocumento = ultimoDoc,
                productsList = combinada, listaAcumulada = combinada,
                categories = cats, categoriesWithCounts = catsWithCounts,
                totalProductsCount = if (it.isEnBusqueda) it.totalProductsCount else maxOf(conteoGlobalProductos, combinada.size),
                totalInventoryValue = calculator.calculateTotalValue(combinada),
                lowStockCount = calculator.calculateLowStockCount(combinada),
                nearExpiryCount = calculator.calculateNearExpiryCount(combinada),
                activeProductsCount = if (it.isEnBusqueda) it.activeProductsCount else maxOf(conteoGlobalActivos, calculator.calculateActiveProductsCount(combinada)),
                estadoCarga = if (it.isEnBusqueda) it.estadoCarga else estado,
                errorMessage = null, isRealtimeConnected = true
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun seleccionarEstadoTab(tab: String) {
        _estadoTab.value = tab
    }

    fun toggleCategoryFilter(category: String) {
        if (category == "Todos") {
            _filterState.update { it.copy(categorias = emptySet()) }; _uiState.update {
                it.copy(
                    selectedCategories = emptySet()
                )
            }; return
        }
        _filterState.update { it.copy(categorias = setOf(category)) }
        _uiState.update { it.copy(selectedCategories = setOf(category)) }
    }

    fun toggleEstadoStockFilter(estado: String) {
        _filterState.update { c -> c.copy(estadoStock = if (c.estadoStock.contains(estado)) c.estadoStock - estado else c.estadoStock + estado) }
    }

    fun toggleStockBajoMetric() {
        _filterState.update { c ->
            val s =
                c.estadoStock.contains("Stock bajo"); c.copy(estadoStock = if (s) c.estadoStock - "Stock bajo" else c.estadoStock + "Stock bajo")
        }
    }

    fun togglePorVencerMetric() {
        _filterState.update { c ->
            val s =
                c.estadoStock.contains("Por vencer") || c.estadoStock.contains("Vencido"); c.copy(
            estadoStock = if (s) c.estadoStock - "Por vencer" - "Vencido" else c.estadoStock + "Por vencer" + "Vencido"
        )
        }
    }

    fun clearMetricFilters() {
        _filterState.update { it.copy(estadoStock = it.estadoStock - "Stock bajo" - "Por vencer" - "Vencido") }
    }

    fun toggleClasificacionFilter(c: String) {
        _filterState.update { it.copy(clasificacion = if (it.clasificacion.contains(c)) it.clasificacion - c else it.clasificacion + c) }
    }

    fun toggleUseCaseFilter(useCase: String) {
        _filterState.update { it.copy(clasificacion = if (it.clasificacion.contains(useCase)) it.clasificacion - useCase else it.clasificacion + useCase) }
        _uiState.update { it.copy(selectedUseCases = if (it.selectedUseCases.contains(useCase)) it.selectedUseCases - useCase else it.selectedUseCases + useCase) }
    }

    fun setFilterPanelOpen(open: Boolean) {
        _uiState.update { it.copy(isFilterPanelOpen = open) }
    }

    fun updateFilter(update: InventarioFilterState.() -> InventarioFilterState) {
        _filterState.update { it.update() }
    }

    fun clearAllFilters() {
        _filterState.value = InventarioFilterState()
        _uiState.update {
            it.copy(
                selectedCategories = emptySet(),
                selectedUseCases = emptySet(),
                selectedSortOption = SortOption.FECHA_CREACION,
                sortColumn = SortOption.FECHA_CREACION.column,
                sortDirection = SortOption.FECHA_CREACION.direction
            )
        }
    }

    fun getLaboratorios(): List<String> =
        InventarioFilterLogic.getLaboratorios(_uiState.value.productsList)

    fun getUbicaciones(): List<String> =
        InventarioFilterLogic.getUbicaciones(_uiState.value.productsList)

    fun getCategorias(): List<String> =
        InventarioFilterLogic.getCategorias(_uiState.value.productsList)

    fun getPrecioMax(): Float = InventarioFilterLogic.getPrecioMax(_uiState.value.productsList)

    fun toggleSort(column: InventarioSortColumn) {
        _uiState.update { c ->
            val (nc, nd) = when {
                c.sortColumn == column && c.sortDirection == InventarioSortDirection.ASC -> column to InventarioSortDirection.DESC
                c.sortColumn == column && c.sortDirection == InventarioSortDirection.DESC -> InventarioSortColumn.NONE to InventarioSortDirection.ASC
                else -> column to InventarioSortDirection.ASC
            }
            val opt = SortOption.entries.firstOrNull { it.column == nc && it.direction == nd }
            c.copy(
                sortColumn = nc,
                sortDirection = nd,
                selectedSortOption = opt ?: c.selectedSortOption
            )
        }
    }

    fun onSortOptionSelected(option: SortOption) {
        _uiState.update {
            it.copy(
                selectedSortOption = option,
                sortColumn = option.column,
                sortDirection = option.direction
            )
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun onItemsPerPageChanged(count: Int) {
        _uiState.update { c ->
            val tp = kotlin.math.ceil(c.totalProductsCount.toDouble() / count).toInt()
                .coerceAtLeast(1); c.copy(itemsPerPage = count, totalPages = tp, currentPage = 1)
        }
    }

    fun markAlertsAsRead() {
        val cur = alertas.value;
        val read = _readAlerts.value; if (cur.isEmpty()) return
        viewModelScope.launch {
            var ch = false
            cur.forEach { al ->
                val key = AlertPersistenceManager.readKey(al.productId, al.tipo.name)
                val r = read[key]; if (r == null || r.lastMessage != al.mensaje) {
                AlertPersistenceManager.markAsRead(
                    SessionManager.clienteIdGarantizado.ifBlank { SessionManager.idCajera },
                    al.productId,
                    al.tipo.name,
                    al.mensaje
                ); ch = true
            }
            }
            if (ch) loadReadAlerts()
        }
    }

    fun markAsRead(productId: String, tipo: String) {
        val al = alertas.value.find { it.productId == productId && it.tipo.name == tipo } ?: return
        val key = AlertPersistenceManager.readKey(productId, tipo)
        val rec = _readAlerts.value[key]
        if (rec == null || rec.lastMessage != al.mensaje) {
            viewModelScope.launch {
                AlertPersistenceManager.markAsRead(
                    SessionManager.clienteIdGarantizado.ifBlank { SessionManager.idCajera },
                    productId,
                    tipo,
                    al.mensaje
                )
                loadReadAlerts()
            }
        }
    }

    /**
     * Contrato de frescura del badge de alertas leídas (dato de baja criticidad:
     * tolera minutos de vejez). NO lleva listener permanente; se recarga:
     *   1. al entrar a la pantalla (init),
     *   2. tras marcar algo como visto,
     *   3. al volver a primer plano (InventarioScreen → refrescarAlertasLeidas).
     * Fallo de red: se registra con verdad y se conserva el último estado conocido.
     */
    fun refrescarAlertasLeidas() {
        viewModelScope.launch { loadReadAlerts() }
    }

    private suspend fun loadReadAlerts() {
        try {
            _readAlerts.value = AlertPersistenceManager.getReadAlerts(
                SessionManager.clienteIdGarantizado.ifBlank { SessionManager.idCajera }
            )
        } catch (e: Exception) {
            android.util.Log.e("InventarioVM", "No se pudo leer alertas vistas: ${e.message}", e)
        }
    }

    private fun formatReadTimestamp(ts: Long): String {
        if (ts == 0L) return ""; return try {
            val sdf = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.US
            ); sdf.format(java.util.Date(ts)).lowercase()
        } catch (e: Exception) {
            android.util.Log.w("InventarioVM", "format timestamp falló", e); ""
        }
    }

    fun reloadProductById(productId: String) {}
    fun resetAndReload() {
        cargarPaginaInicial()
    }

    fun refreshInventory() {
        cargarPaginaInicial()
    }
}
