package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos.ProductoDetalleLecturaRepository
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos.LotesOperacionesRepository
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos.PreciosEtiquetasRepository
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.PerfTracker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class ProductDetailState {
    object Loading : ProductDetailState()
    data class Success(
        val product: MoldeProductos,
        val movements: List<MovimientoInventario> = emptyList(),
        val isPrivileged: Boolean = true,
        val isDeleted: Boolean = false
    ) : ProductDetailState()
    data class Error(val message: String) : ProductDetailState()
}

class ProductDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val lecturaRepo = ProductoDetalleLecturaRepository()
    private val lotesRepo = LotesOperacionesRepository()
    private val preciosRepo = PreciosEtiquetasRepository()
    private var productJob: Job? = null
    private var movementsJob: Job? = null
    private var currentMovements: List<MovimientoInventario> = emptyList()
    private var movimientosLimit: Long = 50

    var uiState by mutableStateOf<ProductDetailState>(ProductDetailState.Loading)
        private set

    fun loadProduct(productId: String) {
        val userRole = SessionManager.rol.lowercase()
        val isPrivileged = userRole in listOf("administrador", "supervisor")
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }

        productJob?.cancel()
        movementsJob?.cancel()

        // 1. Si el estado anterior es de otro producto (incluido borrado), limpiarlo de inmediato — evita parpadeo de "ELIMINADO" en producto ajeno
        val prev = uiState
        if (prev is ProductDetailState.Success && prev.product.indice != productId) {
            uiState = ProductDetailState.Loading
            // Limpiar caché stale del producto anterior
            if (PerfTracker.cachedDetailProduct?.indice != productId) {
                PerfTracker.cachedDetailProduct = null
            }
        }
        // 2. Si tenemos el producto precargado en memoria (0ms de latencia), mostrarlo de inmediato
        val cached = PerfTracker.cachedDetailProduct
        if (cached != null && cached.indice == productId) {
            uiState = ProductDetailState.Success(
                product = cached,
                movements = currentMovements,
                isPrivileged = isPrivileged
            )
        } else {
            val current = uiState
            if (current !is ProductDetailState.Success || current.product.indice != productId) {
                uiState = ProductDetailState.Loading
            }
        }

        productJob = viewModelScope.launch {
            try {
                lecturaRepo.observarDetalleProducto(clienteId, productId).collect { fetchedProduct ->
                    if (fetchedProduct != null) {
                        uiState = ProductDetailState.Success(
                            product = fetchedProduct,
                            movements = currentMovements,
                            isPrivileged = isPrivileged
                        )
                    } else {
                        // Solo marcar borrado si el productId solicitado sigue siendo el actual (evita que un null tardío de producto viejo marque borrado en uno nuevo)
                        if (productId == PerfTracker.cachedDetailProduct?.indice || uiState is ProductDetailState.Loading || (uiState is ProductDetailState.Success && (uiState as ProductDetailState.Success).product.indice == productId)) {
                            when (val current = uiState) {
                                is ProductDetailState.Success -> {
                                    // Solo si el success actual es del mismo productId
                                    if (current.product.indice == productId) uiState = current.copy(isDeleted = true)
                                    else uiState = ProductDetailState.Error("Producto no encontrado o eliminado del inventario.")
                                }
                                else -> uiState = ProductDetailState.Error("Producto no encontrado o eliminado del inventario.")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                uiState = ProductDetailState.Error("Sin conexión con inventario. Verifica internet y toca Reintentar.")
            }
        }

        movementsJob = viewModelScope.launch {
            try {
                lecturaRepo.observarMovimientosProducto(clienteId, productId, movimientosLimit).collect { fetchedMovements ->
                    currentMovements = fetchedMovements
                    val current = uiState
                    if (current is ProductDetailState.Success) {
                        uiState = current.copy(movements = fetchedMovements)
                    }
                }
            } catch (e: Exception) {
                // Kardex sin conexión: mantiene producto visible, solo log
            }
        }
    }

    fun cambiarBloqueoLote(productId: String, lote: LoteProducto, ponerEnCuarentena: Boolean, cantidadAfectada: Double = 0.0, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = lotesRepo.cambiarBloqueoLote(
                clienteId = clienteId,
                productId = productId,
                lote = lote,
                ponerEnCuarentena = ponerEnCuarentena,
                cantidadAfectada = cantidadAfectada,
                motivo = motivo,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun definirLotePrioritario(productId: String, loteId: String?, onComplete: (Result<Unit>) -> Unit) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"
        val userRol = SessionManager.rol.ifBlank { "Administrador" }
        viewModelScope.launch {
            val result = lotesRepo.definirLotePrioritario(
                clienteId = clienteId,
                productId = productId,
                loteId = loteId,
                usuarioRol = userRol,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun registrarDevolucionProveedor(
        productId: String,
        lote: LoteProducto,
        cantidadDevuelta: Double,
        guiaRetiro: String,
        notaCredito: String,
        motivo: String,
        modalidadCompensacion: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = lotesRepo.registrarDevolucionProveedor(
                clienteId = clienteId,
                productId = productId,
                lote = lote,
                cantidadDevuelta = cantidadDevuelta,
                guiaRetiro = guiaRetiro,
                notaCredito = notaCredito,
                motivo = motivo,
                modalidadCompensacion = modalidadCompensacion,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun registrarCanjeProducto(
        productId: String,
        loteOrigen: LoteProducto,
        cantidadCanjeada: Double,
        nuevoLoteNumero: String,
        nuevoVencimiento: String,
        guiaCanje: String,
        motivo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = lotesRepo.registrarCanjeProducto(
                clienteId = clienteId,
                productId = productId,
                loteOrigen = loteOrigen,
                cantidadCanjeada = cantidadCanjeada,
                nuevoLoteNumero = nuevoLoteNumero,
                nuevoVencimiento = nuevoVencimiento,
                guiaCanje = guiaCanje,
                motivo = motivo,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun anularIngreso(productId: String, lote: LoteProducto, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = lotesRepo.anularIngresoLote(
                clienteId = clienteId,
                productId = productId,
                lote = lote,
                motivo = motivo,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun registrarMerma(productId: String, lote: LoteProducto, cantidadMerma: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = lotesRepo.registrarMerma(
                clienteId = clienteId,
                productId = productId,
                lote = lote,
                cantidadMerma = cantidadMerma,
                motivo = motivo,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun guardarPresentacionesYPrecios(
        productId: String,
        unidadBase: String,
        presentaciones: List<PresentacionProducto>,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = preciosRepo.guardarPresentacionesYPrecios(
                clienteId = clienteId,
                productId = productId,
                unidadBase = unidadBase,
                presentaciones = presentaciones,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    private var ubicacionesJob: Job? = null

    var ubicacionesDisponibles by mutableStateOf<List<String>>(emptyList())
        private set

    fun cargarCatalogoUbicaciones() {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        ubicacionesJob?.cancel()
        ubicacionesJob = viewModelScope.launch {
            lecturaRepo.observarCatalogoUbicaciones(clienteId).collect { lista ->
                ubicacionesDisponibles = lista
            }
        }
    }

    suspend fun generarCodigoInternoUnico(): String {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        return lecturaRepo.generarCodigoInternoUnico(clienteId)
    }

    suspend fun buscarDuplicadoCodigo(codigo: String, currentProductId: String): String? {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        return lecturaRepo.buscarDuplicadoCodigoBarras(clienteId, codigo, currentProductId)
    }

    fun guardarConfiguracionYLogistica(
        productId: String,
        ubicacion: String,
        stockMinimo: Double,
        activo: Boolean,
        diasAlertaVencimiento: Int = 90,
        nuevoCodigo: String? = null,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        // Optimización en vivo inmediata
        if (ubicacion.isNotBlank()) {
            ubicacionesDisponibles = (ubicacionesDisponibles + ubicacion.trim()).filter { it.isNotBlank() }.distinct().sorted()
        }

        viewModelScope.launch {
            val result = preciosRepo.guardarConfiguracionYLogistica(
                clienteId = clienteId,
                productId = productId,
                ubicacion = ubicacion,
                stockMinimo = stockMinimo,
                activo = activo,
                diasAlertaVencimiento = diasAlertaVencimiento,
                usuarioEmail = userEmail,
                nuevoCodigo = nuevoCodigo
            )
            if (result.isSuccess) {
                cargarCatalogoUbicaciones()
            }
            onComplete(result)
        }
    }

    fun eliminarProductoDefinitivo(
        productId: String,
        motivo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"
        if (motivo.trim().length < 10) {
            onComplete(Result.failure(IllegalArgumentException("El motivo debe tener al menos 10 caracteres.")))
            return
        }
        viewModelScope.launch {
            val result = preciosRepo.eliminarProductoDefinitivo(
                clienteId = clienteId,
                productId = productId,
                motivo = motivo,
                usuarioEmail = userEmail
            )
            onComplete(result)
        }
    }

    fun cargarMasMovimientos() {
        if (uiState is ProductDetailState.Success) {
            val pid = (uiState as ProductDetailState.Success).product.indice
            movimientosLimit += 50
            loadProduct(pid)
        }
    }

    fun marcarEtiquetaImpresa(
        productId: String,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        viewModelScope.launch {
            val result = preciosRepo.marcarEtiquetaImpresa(clienteId, productId, userEmail)
            onComplete(result)
        }
    }
}
