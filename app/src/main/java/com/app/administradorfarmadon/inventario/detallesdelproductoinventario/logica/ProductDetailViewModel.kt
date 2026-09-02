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
import java.util.UUID

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

    private data class OperacionPendiente(val idem: String, val clave: String)
    private var devolucionPendiente: OperacionPendiente? = null
    private var canjePendiente: OperacionPendiente? = null

    private fun idemPara(clave: String, pendiente: OperacionPendiente?): OperacionPendiente =
        if (pendiente?.clave == clave) pendiente else OperacionPendiente(UUID.randomUUID().toString(), clave)

    var uiState by mutableStateOf<ProductDetailState>(ProductDetailState.Loading)
        private set
    var infoEliminacion by mutableStateOf<ProductoDetalleLecturaRepository.InfoEliminacion?>(null)
        private set
    var kardexError by mutableStateOf<String?>(null)
        private set

    fun loadProduct(productId: String) {
        val userRole = SessionManager.rol.lowercase()
        val isPrivileged = userRole in listOf("administrador", "supervisor")
        val clienteId = SessionManager.clienteIdGarantizado

        if (clienteId.isBlank()) {
            uiState = ProductDetailState.Error("No hay una farmacia activa. Vuelve a iniciar sesión.")
            return
        }

        productJob?.cancel()
        movementsJob?.cancel()
        kardexError = null

        // 1. Si el estado anterior es de otro producto (incluido borrado), limpiarlo de inmediato —” evita parpadeo de "ELIMINADO" en producto ajeno
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
                        // Solo marcar borrado si el productId solicitado sigue siendo el actual
                        if (productId == PerfTracker.cachedDetailProduct?.indice || uiState is ProductDetailState.Loading || (uiState is ProductDetailState.Success && (uiState as ProductDetailState.Success).product.indice == productId)) {
                            when (val current = uiState) {
                                is ProductDetailState.Success -> {
                                    if (current.product.indice == productId) {
                                        uiState = current.copy(isDeleted = true)
                                        // Cargar info real de quién lo borró (simple, sin complicar)
                                        viewModelScope.launch {
                                            infoEliminacion = lecturaRepo.obtenerInfoEliminacion(clienteId, productId)
                                        }
                                    } else uiState = ProductDetailState.Error("Producto no encontrado o eliminado del inventario.")
                                }
                                else -> {
                                    uiState = ProductDetailState.Error("Producto no encontrado o eliminado del inventario.")
                                    viewModelScope.launch {
                                        infoEliminacion = lecturaRepo.obtenerInfoEliminacion(clienteId, productId)
                                    }
                                }
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
                // Kardex sin conexión: mantiene producto visible y muestra el bloqueo real.
                kardexError = "No se pudo cargar el historial del producto. Verifica internet y vuelve a intentar."
            }
        }
    }

    private var bloqueoLoteEnCurso = false

    fun cambiarBloqueoLote(productId: String, lote: LoteProducto, ponerEnCuarentena: Boolean, cantidadAfectada: Double = 0.0, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        if (bloqueoLoteEnCurso) return
        bloqueoLoteEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            bloqueoLoteEnCurso = false
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            try {
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
            } finally {
                bloqueoLoteEnCurso = false
            }
        }
    }

    private var definirPrioritarioEnCurso = false
    fun definirLotePrioritario(productId: String, loteId: String?, onComplete: (Result<Unit>) -> Unit) {
        if (definirPrioritarioEnCurso) return
        definirPrioritarioEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            definirPrioritarioEnCurso = false
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        val userRol = SessionManager.rol
        viewModelScope.launch {
            try {
                val result = lotesRepo.definirLotePrioritario(
                    clienteId = clienteId,
                    productId = productId,
                    loteId = loteId,
                    usuarioRol = userRol,
                    usuarioEmail = userEmail
                )
                onComplete(result)
            } finally {
                definirPrioritarioEnCurso = false
            }
        }
    }

    private var devolucionEnCurso = false
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
        if (devolucionEnCurso) return
        devolucionEnCurso = true
        val op = idemPara(
            "D|$productId|${lote.numero}|$cantidadDevuelta|$guiaRetiro|$notaCredito|$motivo|$modalidadCompensacion",
            devolucionPendiente
        )
        devolucionPendiente = op
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            devolucionEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            try {
                val result = lotesRepo.registrarDevolucionProveedor(
                    clienteId = clienteId,
                    productId = productId,
                    lote = lote,
                    cantidadDevuelta = cantidadDevuelta,
                    guiaRetiro = guiaRetiro,
                    notaCredito = notaCredito,
                    motivo = motivo,
                    modalidadCompensacion = modalidadCompensacion,
                    usuarioEmail = userEmail,
                    idempotenciaId = op.idem
                )
                if (result.isSuccess) devolucionPendiente = null
                onComplete(result)
            } finally {
                devolucionEnCurso = false
            }
        }
    }

    private var canjeEnCurso = false
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
        if (canjeEnCurso) return
        canjeEnCurso = true
        val op = idemPara(
            "C|$productId|${loteOrigen.numero}|$cantidadCanjeada|$nuevoLoteNumero|$nuevoVencimiento|$guiaCanje|$motivo",
            canjePendiente
        )
        canjePendiente = op
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            canjeEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            try {
                val result = lotesRepo.registrarCanjeProducto(
                    clienteId = clienteId,
                    productId = productId,
                    loteOrigen = loteOrigen,
                    cantidadCanjeada = cantidadCanjeada,
                    nuevoLoteNumero = nuevoLoteNumero,
                    nuevoVencimiento = nuevoVencimiento,
                    guiaCanje = guiaCanje,
                    motivo = motivo,
                    usuarioEmail = userEmail,
                    idempotenciaId = op.idem
                )
                if (result.isSuccess) canjePendiente = null
                onComplete(result)
            } finally {
                canjeEnCurso = false
            }
        }
    }

    private var correccionVencimientoEnCurso = false
    fun corregirVencimientoLote(productId: String, lote: LoteProducto, nuevoVencimiento: String, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        if (correccionVencimientoEnCurso) return
        correccionVencimientoEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            correccionVencimientoEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        viewModelScope.launch {
            try {
                val result = lotesRepo.corregirVencimientoLote(clienteId, productId, lote, nuevoVencimiento, motivo, userEmail)
                onComplete(result)
            } finally {
                correccionVencimientoEnCurso = false
            }
        }
    }

    private var anularEnCurso = false
    fun anularIngreso(productId: String, lote: LoteProducto, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        if (anularEnCurso) return
        anularEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            anularEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            try {
                val result = lotesRepo.anularIngresoLote(
                    clienteId = clienteId,
                    productId = productId,
                    lote = lote,
                    motivo = motivo,
                    usuarioEmail = userEmail
                )
                onComplete(result)
            } finally {
                anularEnCurso = false
            }
        }
    }

    private var guardandoPreciosEnCurso = false
    fun guardarPresentacionesYPrecios(
        productId: String,
        unidadBase: String,
        presentaciones: List<PresentacionProducto>,
        presentacionesOriginales: List<PresentacionProducto> = emptyList(),
        onComplete: (Result<Unit>) -> Unit
    ) {
        if (guardandoPreciosEnCurso) return
        guardandoPreciosEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            guardandoPreciosEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            try {
                val result = preciosRepo.guardarPresentacionesYPrecios(
                    clienteId = clienteId,
                    productId = productId,
                    unidadBase = unidadBase,
                    presentaciones = presentaciones,
                    usuarioEmail = userEmail,
                    presentacionesOriginales = presentacionesOriginales
                )
                onComplete(result)
            } finally {
                guardandoPreciosEnCurso = false
            }
        }
    }

    private var ubicacionesJob: Job? = null

    var ubicacionesDisponibles by mutableStateOf<List<String>>(emptyList())
        private set

    fun cargarCatalogoUbicaciones() {
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) return
        ubicacionesJob?.cancel()
        ubicacionesJob = viewModelScope.launch {
            lecturaRepo.observarCatalogoUbicaciones(clienteId).collect { lista ->
                ubicacionesDisponibles = lista
            }
        }
    }

    suspend fun generarCodigoInternoUnico(): String {
        val clienteId = SessionManager.clienteIdGarantizado
        return lecturaRepo.generarCodigoInternoUnico(clienteId)
    }

    suspend fun buscarDuplicadoCodigo(codigo: String, currentProductId: String): String? {
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) return null
        return lecturaRepo.buscarDuplicadoCodigoBarras(clienteId, codigo, currentProductId)
    }

    private var guardandoConfigEnCurso = false
    fun guardarConfiguracionYLogistica(
        productId: String,
        ubicacion: String,
        stockMinimo: Double,
        activo: Boolean,
        diasAlertaVencimiento: Int = 90,
        nuevoCodigo: String? = null,
        ubicacionSecundaria: String = "",
        fefoAutomatico: Boolean = true,
        onComplete: (Result<Unit>) -> Unit
    ) {
        if (guardandoConfigEnCurso) return
        guardandoConfigEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            guardandoConfigEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        // Optimización en vivo inmediata
        if (ubicacion.isNotBlank()) {
            ubicacionesDisponibles = (ubicacionesDisponibles + ubicacion.trim()).filter { it.isNotBlank() }.distinct().sorted()
        }
        if (ubicacionSecundaria.isNotBlank()) {
            ubicacionesDisponibles = (ubicacionesDisponibles + ubicacionSecundaria.trim()).filter { it.isNotBlank() }.distinct().sorted()
        }

        viewModelScope.launch {
            try {
                val result = preciosRepo.guardarConfiguracionYLogistica(
                    clienteId = clienteId,
                    productId = productId,
                    ubicacion = ubicacion,
                    stockMinimo = stockMinimo,
                    activo = activo,
                    diasAlertaVencimiento = diasAlertaVencimiento,
                    usuarioEmail = userEmail,
                    nuevoCodigo = nuevoCodigo,
                    ubicacionSecundaria = ubicacionSecundaria,
                    fefoAutomatico = fefoAutomatico
                )
                if (result.isSuccess) {
                    cargarCatalogoUbicaciones()
                }
                onComplete(result)
            } finally {
                guardandoConfigEnCurso = false
            }
        }
    }

    private var eliminandoEnCurso = false
    fun eliminarProductoDefinitivo(
        productId: String,
        motivo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        if (eliminandoEnCurso) return
        if (motivo.trim().length < 10) {
            onComplete(Result.failure(IllegalArgumentException("El motivo debe tener al menos 10 caracteres.")))
            return
        }
        eliminandoEnCurso = true
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            eliminandoEnCurso = false
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        viewModelScope.launch {
            try {
                val result = preciosRepo.eliminarProductoDefinitivo(
                    clienteId = clienteId,
                    productId = productId,
                    motivo = motivo,
                    usuarioEmail = userEmail
                )
                onComplete(result)
            } finally {
                eliminandoEnCurso = false
            }
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
        val clienteId = SessionManager.clienteIdGarantizado
        if (clienteId.isBlank()) {
            onComplete(Result.failure(IllegalStateException("No hay una farmacia activa. Vuelve a iniciar sesión.")))
            return
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        viewModelScope.launch {
            val result = preciosRepo.marcarEtiquetaImpresa(clienteId, productId, userEmail)
            onComplete(result)
        }
    }
}
