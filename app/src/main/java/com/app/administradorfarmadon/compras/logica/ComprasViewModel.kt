package com.app.administradorfarmadon.compras.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.compartido.datos.FacturaCompraRepository
import com.app.administradorfarmadon.inventario.compartido.datos.ProveedorRepository
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.datos.PedidoCompraRepository
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ComprasViewModel(
    private val proveedorRepository: ProveedorRepository = ProveedorRepository(),
    private val facturaRepository: FacturaCompraRepository = FacturaCompraRepository(),
    private val inventarioRepository: InventarioFirestoreRepository = InventarioFirestoreRepository(),
    private val pedidoCompraRepository: PedidoCompraRepository = PedidoCompraRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ComprasViewModel"

        // ── PERSISTENCIA EN MEMORIA DE SESIÓN (PERMANECE AL NAVEGAR ENTRE MÓDULOS) ──
        private var tabSesionGuardada: String = "REPOSICION"
        private var subTabProveedorGuardada: String = "RESUMEN"
        private var filtroEstadoFacturaGuardada: String = "TODAS"
        private var proveedorSeleccionadoIdGuardado: String? = null
        private var facturaSeleccionadaIdGuardada: String? = null
    }

    private val _uiState = MutableStateFlow(
        ComprasUiState(
            tabSeleccionada = tabSesionGuardada,
            subTabProveedor = subTabProveedorGuardada,
            filtroEstadoFactura = filtroEstadoFacturaGuardada,
            proveedorSeleccionadoId = proveedorSeleccionadoIdGuardado,
            facturaSeleccionadaId = facturaSeleccionadaIdGuardada
        )
    )
    val uiState: StateFlow<ComprasUiState> = _uiState.asStateFlow()

    private val escuchasJobs = mutableListOf<kotlinx.coroutines.Job>()
    private var sucursalObserverJob: kotlinx.coroutines.Job? = null
    // Debe vivir ANTES del init — el reinicio por cambio de sede lo limpia al instante
    private val ultimoValorEnviado = java.util.concurrent.ConcurrentHashMap<String, Int>()

    init {
        observarCambiosDeSucursal()
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            SessionManager.sucursalFlow.collect { sucursal ->
                if (sucursal != ultimaSucursal) {
                    ultimaSucursal = sucursal
                    reiniciarEscuchasPorCambioDeSucursal()
                }
            }
        }
    }

    private fun reiniciarEscuchasPorCambioDeSucursal() {
        // Política D: al cambiar de sede, los datos de la sede anterior se eliminan inmediato
        escuchasJobs.forEach { it.cancel() }
        escuchasJobs.clear()
        ultimoValorEnviado.clear()
        _uiState.update {
            it.copy(
                proveedores = emptyList(),
                facturas = emptyList(),
                pedidosGuardados = emptyList(),
                pedidosPorProveedor = emptyMap(),
                todosLosProductos = emptyList(),
                cargando = true,
                errorEscucha = null
            )
        }
        iniciarEscuchasTiempoReal()
        iniciarCarritoCompartido()
    }

    private fun iniciarCarritoCompartido() {
        val job = viewModelScope.launch {
            pedidoCompraRepository.observarCarritosReposicion(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error escuchando carrito compartido: ${e.message}", e)
                }
                .collect { mapa ->
                    // Poda del rastro local: lo que el servidor ya confirma deja de ser "pendiente"
                    ultimoValorEnviado.entries.removeAll { (clave, valor) ->
                        val proveedor = clave.substringBefore('\u0001')
                        val productoId = clave.substringAfter('\u0001')
                        mapa[proveedor]?.get(productoId) == valor
                    }
                    _uiState.update { it.copy(pedidosPorProveedor = mapa, errorEscucha = null) }
                }
        }
        escuchasJobs.add(job)
    }

    /** La pantalla jamás confunde "no hay datos" con "se cayó la actualización en vivo". */
    private fun marcarErrorEscucha(motivo: String) {
        _uiState.update { it.copy(errorEscucha = motivo) }
    }

    private fun iniciarEscuchasTiempoReal() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva

        // 1. Escuchar Proveedores en tiempo real
        escuchasJobs.add(viewModelScope.launch {
            proveedorRepository.observarProveedores(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error en flujo de proveedores: ${e.message}", e)
                    emit(emptyList())
                }
                .collect { lista ->
                    _uiState.update { it.copy(proveedores = lista, cargando = false, errorEscucha = null) }
                }
        })

        // 2. Escuchar Facturas de Compra en tiempo real
        escuchasJobs.add(viewModelScope.launch {
            facturaRepository.observarFacturasRecientes(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error en flujo de facturas: ${e.message}", e)
                    emit(emptyList())
                }
                .collect { facturas ->
                    _uiState.update { it.copy(facturas = facturas, errorEscucha = null) }
                }
        })

        // 3. Escuchar Inventario / Catálogo de Productos Completo (Tiempo Real)
        if (farmaciaId.isNotBlank() && sucursalId.isNotBlank()) {
            escuchasJobs.add(viewModelScope.launch {
                inventarioRepository.observarInventario(farmaciaId)
                    .catch { e ->
                        Log.e(TAG, "Error en flujo de inventario para compras: ${e.message}", e)
                        emit(emptyList())
                    }
                    .collect { productos ->
                        _uiState.update { it.copy(todosLosProductos = productos) }
                    }
            })
        }

        // 4. Escuchar Pedidos de Compra Enviados (Tiempo Real)
        escuchasJobs.add(viewModelScope.launch {
            pedidoCompraRepository.observarPedidosRecientes(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error en flujo de pedidos_compra: ${e.message}", e)
                    emit(emptyList())
                }
                .collect { pedidos ->
                    _uiState.update { it.copy(pedidosGuardados = pedidos, errorEscucha = null) }
                }
        })
    }

    override fun onCleared() {
        super.onCleared()
        sucursalObserverJob?.cancel()
        escuchasJobs.forEach { it.cancel() }
        escuchasJobs.clear()
    }

    fun seleccionarTab(tab: String) {
        tabSesionGuardada = tab
        _uiState.update {
            it.copy(
                tabSeleccionada = tab,
                busquedaQuery = ""
            )
        }
    }

    fun seleccionarSubTabProveedor(subTab: String) {
        subTabProveedorGuardada = subTab
        _uiState.update { it.copy(subTabProveedor = subTab) }
    }

    fun toggleProveedorExpandido(proveedorNombre: String) {
        _uiState.update { current ->
            val set = current.proveedoresExpandidos.toMutableSet()
            if (set.contains(proveedorNombre)) set.remove(proveedorNombre) else set.add(
                proveedorNombre
            )
            current.copy(proveedoresExpandidos = set)
        }
    }

    fun modificarCantidadProducto(producto: PharmProduct, delta: Int, ignorarAviso: Boolean = false) {
        val provNombre = _uiState.value.resolverProveedorProducto(producto)

        // Escudo anti doble pedido: si ya viene en una orden ENVIADO/PARCIAL, orientar antes de sumar
        val yaEnCamino = _uiState.value.enCaminoDe(producto)
        val yaEnCarrito = _uiState.value.pedidosPorProveedor[provNombre]?.get(producto.id) ?: 0
        if (!ignorarAviso && delta > 0 && yaEnCamino > 0 && yaEnCarrito == 0) {
            _uiState.update {
                it.copy(productoPendienteConfirmar = producto, cantidadExtraPropuesta = delta)
            }
            return
        }

        modificarCantidadPedirProveedor(provNombre, producto.id, delta)
    }

    fun confirmarAdicionExtra() {
        val producto = _uiState.value.productoPendienteConfirmar ?: return
        val delta = _uiState.value.cantidadExtraPropuesta
        _uiState.update { it.copy(productoPendienteConfirmar = null, cantidadExtraPropuesta = 0) }
        modificarCantidadProducto(producto, delta, ignorarAviso = true)
    }

    fun descartarAdicionExtra() {
        _uiState.update { it.copy(productoPendienteConfirmar = null, cantidadExtraPropuesta = 0) }
    }

    // ── UNA SOLA PLUMA: la escucha en vivo dibuja el borrador; aquí solo escribimos a la base ──
    private fun claveCarrito(proveedor: String, productoId: String) = "$proveedor\u0001$productoId"

    private fun valorBaseCarrito(proveedor: String, productoId: String): Int {
        val enPantalla = _uiState.value.pedidosPorProveedor[proveedor]?.get(productoId) ?: 0
        val enviadoPreviamente = ultimoValorEnviado[claveCarrito(proveedor, productoId)] ?: 0
        return maxOf(enPantalla, enviadoPreviamente)
    }

    private fun aplicarCambiosCarrito(proveedorNombre: String, cambios: Map<String, Int>) {
        if (cambios.isEmpty()) return
        // Captura sede al momento del toque — evita que un cambio de sede mid-tap escriba en sede equivocada (R1)
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            val ok = pedidoCompraRepository.guardarProductosCarrito(proveedorNombre, cambios, farmaciaCapturada, sucursalCapturada)
            if (!ok) {
                // Retroceso del optimismo local: si la base NO confirmó, esos valores
                // dejan de ser base para el próximo toque (+/-). Así el siguiente ajuste
                // parte del número real que ve la pantalla, jamás de unidades fantasma.
                cambios.keys.forEach { productoId ->
                    ultimoValorEnviado.remove(claveCarrito(proveedorNombre, productoId))
                }
                _uiState.update {
                    it.copy(mensajeError = "No se pudo actualizar el pedido de $proveedorNombre. Revisa la conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    fun reponerSugeridosDeProveedor(proveedorNombre: String) {
        val estado = _uiState.value
        val carroActual = estado.pedidosPorProveedor[proveedorNombre] ?: emptyMap()
        val cambios = mutableMapOf<String, Int>()
        estado.todosLosProductos.forEach { prod ->
            if (estado.resolverProveedorProducto(prod) != proveedorNombre) return@forEach
            val sugerido = estado.calcularCantidadSugerida(prod)
            // Respetar decisión del usuario: solo llenar lo que aún está en 0
            if (sugerido > 0 && valorBaseCarrito(proveedorNombre, prod.id) == 0) {
                cambios[prod.id] = sugerido
                ultimoValorEnviado[claveCarrito(proveedorNombre, prod.id)] = sugerido
            }
        }
        aplicarCambiosCarrito(proveedorNombre, cambios)
    }

    fun reponerTodosLosSugeridosGlobal() {
        val estado = _uiState.value
        val cambiosPorProveedor = mutableMapOf<String, MutableMap<String, Int>>()
        estado.todosLosProductos.forEach { prod ->
            val proveedor = estado.resolverProveedorProducto(prod)
            val sugerido = estado.calcularCantidadSugerida(prod)
            if (sugerido > 0 && valorBaseCarrito(proveedor, prod.id) == 0) {
                cambiosPorProveedor.getOrPut(proveedor) { mutableMapOf() }[prod.id] = sugerido
                ultimoValorEnviado[claveCarrito(proveedor, prod.id)] = sugerido
            }
        }
        cambiosPorProveedor.forEach { (proveedor, cambios) ->
            aplicarCambiosCarrito(proveedor, cambios)
        }
    }

    private fun modificarCantidadPedirProveedor(proveedorNombre: String, productoId: String, delta: Int) {
        val base = valorBaseCarrito(proveedorNombre, productoId)
        val nueva = (base + delta).coerceAtLeast(0)
        if (nueva == base) return

        val clave = claveCarrito(proveedorNombre, productoId)
        if (nueva == 0) ultimoValorEnviado.remove(clave) else ultimoValorEnviado[clave] = nueva
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to nueva))
    }

    fun abrirRevisionPedido(pedido: PedidoProveedor) {
        _uiState.update { it.copy(pedidoEnRevision = pedido, mostrarModalRevisionPedido = true) }
    }

    fun cerrarRevisionPedido() {
        _uiState.update { it.copy(pedidoEnRevision = null, mostrarModalRevisionPedido = false) }
    }

    fun seleccionarSubTabPedidosDerecha(subTab: String) {
        _uiState.update { it.copy(subTabPedidosDerecha = subTab) }
    }

    fun prepararConfirmacionEnvio(pedido: PedidoProveedor) {
        _uiState.update { it.copy(pedidoParaConfirmarEnvio = pedido, mostrarModalConfirmacionEnvio = true, mostrarModalRevisionPedido = false) }
    }

    fun cerrarConfirmacionEnvio() {
        _uiState.update { it.copy(pedidoParaConfirmarEnvio = null, mostrarModalConfirmacionEnvio = false) }
    }

    fun confirmarPedidoEnviado(pedido: PedidoProveedor) {
        if (_uiState.value.enviandoPedido) return

        // Validación: no enviar una orden vacía
        if (pedido.itemsValidos.isEmpty()) {
            _uiState.update { it.copy(mensajeError = "El pedido no tiene productos con cantidad mayor a 0.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(enviandoPedido = true) }

            // Reserva atómica: distingue "otro usuario lo tomó primero" (éxito vacío)
            // de "falló la conexión" (failure con la causa real). Jamás se confunden.
            val resultadoConsumo = pedidoCompraRepository.consumirCarritoParaEnvio(pedido.proveedorNombre)
            val borradorVigente = resultadoConsumo.getOrNull()

            if (borradorVigente == null) {
                _uiState.update { it.copy(enviandoPedido = false) }
                val causa = resultadoConsumo.exceptionOrNull()?.message ?: "Sin detalle del servidor"
                _uiState.update {
                    it.copy(
                        mostrarModalConfirmacionEnvio = false,
                        pedidoParaConfirmarEnvio = null,
                        mensajeError = "No se pudo registrar la orden: $causa. El pedido sigue intacto en el borrador; intenta nuevamente."
                    )
                }
                return@launch
            }

            if (borradorVigente.isEmpty()) {
                _uiState.update { it.copy(enviandoPedido = false) }
                _uiState.update {
                    it.copy(
                        mostrarModalConfirmacionEnvio = false,
                        pedidoParaConfirmarEnvio = null,
                        mensajeError = "Otro usuario ya envió o vació este pedido de ${pedido.proveedorNombre}. Revisa la pestaña 'Pedidos Enviados'."
                    )
                }
                return@launch
            }

            // La orden se construye desde el borrador VIGENTE, nunca desde la foto del modal.
            val productosPorId = _uiState.value.todosLosProductos.associateBy { it.id }
            val itemsVigentes = borradorVigente.mapNotNull { (productoId, cantidad) ->
                val prod = productosPorId[productoId] ?: return@mapNotNull null
                ItemPedidoCompra(
                    productoId = prod.id,
                    productoNombre = prod.name,
                    presentacion = prod.empaque.ifBlank { "Und" },
                    categoria = prod.category,
                    codigo = prod.code,
                    precioCompra = prod.purchasePrice,
                    cantidad = cantidad
                )
            }

            if (itemsVigentes.isEmpty()) {
                _uiState.update { it.copy(enviandoPedido = false) }
                val restaurado = pedidoCompraRepository.guardarProductosCarrito(pedido.proveedorNombre, borradorVigente, SessionManager.clienteIdGarantizado, SessionManager.sucursalIdEfectiva)
                _uiState.update {
                    it.copy(
                        mostrarModalConfirmacionEnvio = false,
                        pedidoParaConfirmarEnvio = null,
                        mensajeError = if (restaurado) {
                            "El borrador contiene productos que ya no existen en el inventario. Fue restaurado para revisarlo."
                        } else {
                            "El borrador tenía productos que ya no existen en el inventario y NO se pudo restaurar automáticamente. Vuelve a armar el pedido desde el catálogo."
                        }
                    )
                }
                return@launch
            }

            val notaMultiusuario = if (itemsVigentes.size != pedido.itemsValidos.size) {
                " (incluye ajustes hechos por otro usuario)"
            } else ""

            val nuevoPedido = com.app.administradorfarmadon.compras.datos.PedidoCompra(
                proveedorId = pedido.proveedorId,
                proveedorNombre = pedido.proveedorNombre,
                proveedorRuc = pedido.proveedorRuc,
                proveedorTelefono = pedido.proveedorTelefono,
                items = itemsVigentes,
                estado = "ENVIADO"
            )

            val resultado = pedidoCompraRepository.guardarPedidoEnviado(nuevoPedido)
            _uiState.update { it.copy(enviandoPedido = false) }
            resultado.fold(
                onSuccess = {
                    limpiarPedidoProveedor(pedido.proveedorNombre)
                    _uiState.update {
                        it.copy(
                            mostrarModalConfirmacionEnvio = false,
                            pedidoParaConfirmarEnvio = null,
                            subTabPedidosDerecha = "ENVIADOS",
                            mensajeExito = "✓ ¡Orden para ${pedido.proveedorNombre} guardada y marcada como ENVIADA!$notaMultiusuario"
                        )
                    }
                },
                onFailure = { e ->
                    // La restauración del borrador se ESPERA y se VERIFICA antes de decir nada:
                    // prohibido prometer "restaurado" sin comprobarlo.
                    val restaurado = pedidoCompraRepository.guardarProductosCarrito(pedido.proveedorNombre, borradorVigente, SessionManager.clienteIdGarantizado, SessionManager.sucursalIdEfectiva)
                    _uiState.update {
                        it.copy(
                            mostrarModalConfirmacionEnvio = false,
                            pedidoParaConfirmarEnvio = null,
                            mensajeError = if (restaurado) {
                                "No se pudo registrar la orden: ${e.message}. El borrador fue restaurado; intenta nuevamente."
                            } else {
                                "No se pudo registrar la orden: ${e.message}. Además falló la reconstrucción del borrador: vuelve a armar el pedido desde el catálogo."
                            }
                        )
                    }
                }
            )
        }
    }

    fun cancelarPedidoEnviado(pedidoId: String) {
        viewModelScope.launch {
            pedidoCompraRepository.cancelarPedido(pedidoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Pedido cancelado correctamente.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "Error al cancelar pedido: ${e.message}") }
                }
            )
        }
    }

    fun abrirDialogoRecepcion(pedido: PedidoCompra) {
        _uiState.update {
            it.copy(
                pedidoParaRecepcionar = pedido,
                mostrarDialogoRecepcion = true,
                cargandoIndiceRecepcion = true,
                indiceLotesOrden = emptyMap(),
                mensajeError = null
            )
        }
        // El índice de lotes se carga ANTES de dibujar el panel: la recepción abre
        // con la verdad completa y nada de lo que el usuario escriba se pierde.
        viewModelScope.launch {
            val indice = pedidoCompraRepository.cargarIndiceLotesDeProductos(pedido.items.map { it.productoId })
            _uiState.update { s ->
                // Escudo anti carrera: si el usuario ya cerró o abrió OTRA orden,
                // este resultado tardío se descarta (jamás contamina la recepción actual).
                if (s.pedidoParaRecepcionar?.id == pedido.id) {
                    s.copy(indiceLotesOrden = indice, cargandoIndiceRecepcion = false)
                } else s
            }
        }
    }

    fun cerrarDialogoRecepcion() {
        _uiState.update {
            it.copy(
                pedidoParaRecepcionar = null,
                mostrarDialogoRecepcion = false,
                procesandoRecepcion = false,
                recepcionIntentoId = "",
                cargandoIndiceRecepcion = false,
                indiceLotesOrden = emptyMap()
            )
        }
    }

    fun asentarRecepcionPedido(
        pedidoId: String,
        numeroFactura: String,
        condicionPago: String,
        fechaVencimientoPago: String,
        montoFactura: Double,
        itemsRecepcion: List<com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoRecepcion = true, mensajeError = null) }

            // Sello de intento: sobrevive a reintentos dentro del diálogo para que un
            // reintento tras corte de red jamás duplique la mercadería ya asentada.
            val idIntento = _uiState.value.recepcionIntentoId.ifBlank {
                java.util.UUID.randomUUID().toString()
                    .also { nuevo -> _uiState.update { s -> s.copy(recepcionIntentoId = nuevo) } }
            }

            // Verdad de auditoría: el KARDEX guarda correo real en usuarioEmail y
            // nombre real en usuarioNombre (jamás un nombre haciéndose pasar por correo).
            val usuarioEmail = SessionManager.email
            val res = pedidoCompraRepository.asentarRecepcionDirecta(
                pedidoId = pedidoId,
                numeroFactura = numeroFactura,
                condicionPago = condicionPago,
                fechaVencimientoPago = fechaVencimientoPago,
                montoFactura = montoFactura,
                itemsRecepcion = itemsRecepcion,
                cerrarConAjuste = cerrarConAjuste,
                usuarioEmail = usuarioEmail,
                usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" },
                idempotenciaId = idIntento
            )

            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            procesandoRecepcion = false,
                            mostrarDialogoRecepcion = false,
                            pedidoParaRecepcionar = null,
                            mensajeExito = "✓ ¡Mercadería recibida y asentada con éxito! Stock y Factura $numeroFactura actualizados."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoRecepcion = false,
                            mensajeError = "No se pudo asentar la recepción: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun cerrarOrdenConAjuste(pedidoId: String, motivo: String = "Quiebre de stock en proveedor") {
        viewModelScope.launch {
            pedidoCompraRepository.cerrarOrdenConAjuste(pedidoId, motivo).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Orden cerrada con ajuste correctamente.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "Error al cerrar orden: ${e.message}") }
                }
            )
        }
    }

    fun descartarProductoDePedido(pedidoId: String, productoId: String) {
        viewModelScope.launch {
            pedidoCompraRepository.descartarProductoDePedido(pedidoId, productoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Producto descartado del pedido por quiebre de stock.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "No se pudo descartar el producto: ${e.message}") }
                }
            )
        }
    }

    fun removerProductoDePedido(proveedorNombre: String, productoId: String) {
        val clave = claveCarrito(proveedorNombre, productoId)
        ultimoValorEnviado.remove(clave)
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to 0))
    }

    fun limpiarPedidoProveedor(proveedorNombre: String) {
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            val ok = pedidoCompraRepository.eliminarCarritoProveedor(proveedorNombre, farmaciaCapturada, sucursalCapturada)
            if (!ok) {
                _uiState.update {
                    it.copy(mensajeError = "No se pudo vaciar el pedido de $proveedorNombre. Revisa la conexión e inténtalo de nuevo.")
                }
            }
        }
        ultimoValorEnviado.keys.removeAll { it.startsWith("$proveedorNombre\u0001") }
    }

    fun seleccionarProveedor(proveedorId: String) {
        proveedorSeleccionadoIdGuardado = proveedorId
        _uiState.update { it.copy(proveedorSeleccionadoId = proveedorId) }
    }

    fun eliminarProveedor(
        proveedor: Proveedor,
        facturasPendientes: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = proveedorRepository.eliminarProveedor(
                    clienteId = SessionManager.clienteIdGarantizado,
                    sucursalId = SessionManager.sucursalIdEfectiva,
                    proveedorId = proveedor.id,
                    proveedorNombre = proveedor.nombre,
                    facturasPendientesCount = facturasPendientes
                )
                result.onSuccess {
                    onResult(true, "Proveedor eliminado correctamente.")
                }.onFailure { e ->
                    onResult(false, e.message ?: "Error al eliminar proveedor.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error inesperado al eliminar proveedor.")
            }
        }
    }

    fun seleccionarFactura(facturaId: String) {
        facturaSeleccionadaIdGuardada = facturaId
        _uiState.update { it.copy(facturaSeleccionadaId = facturaId) }
    }

    fun setFiltroEstadoFactura(filtro: String) {
        filtroEstadoFacturaGuardada = filtro
        _uiState.update { it.copy(filtroEstadoFactura = filtro) }
    }

    fun abrirDialogoAbono(factura: FacturaCompra) {
        _uiState.update {
            it.copy(
                mostrarDialogoAbono = true,
                facturaParaAbonar = factura,
                mensajeError = null
            )
        }
    }

    fun cerrarDialogoAbono() {
        _uiState.update {
            it.copy(
                mostrarDialogoAbono = false,
                facturaParaAbonar = null,
                procesandoPago = false
            )
        }
    }

    fun registrarAbonoFactura(
        facturaId: String,
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        notas: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoPago = true, mensajeError = null, mensajeExito = null) }
            val usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" }
            val usuarioEmail = SessionManager.email.ifBlank { "" }

            val res = facturaRepository.registrarAbono(
                facturaId = facturaId,
                monto = monto,
                metodoPago = metodoPago,
                numeroOperacion = numeroOperacion,
                usuarioNombre = usuarioNombre,
                usuarioEmail = usuarioEmail,
                notas = notas
            )

            res.fold(
                onSuccess = {
                    val montoStr = String.format(java.util.Locale.US, "%.2f", monto)
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mostrarDialogoAbono = false,
                            facturaParaAbonar = null,
                            mensajeExito = "✓ Abono de ${SessionManager.monedaSimbolo.ifBlank { "S/" }} $montoStr registrado con éxito."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mensajeError = "No se pudo registrar el abono: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun anularAbonoFactura(facturaId: String, abonoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoPago = true, mensajeError = null, mensajeExito = null) }
            val res = facturaRepository.anularAbono(facturaId, abonoId)
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mensajeExito = "✓ Abono anulado y saldo de factura restaurado correctamente."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mensajeError = "Error al anular abono: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun abrirDialogoProrroga(factura: FacturaCompra) {
        _uiState.update {
            it.copy(
                mostrarDialogoProrroga = true,
                facturaParaProrroga = factura,
                mensajeError = null
            )
        }
    }

    fun cerrarDialogoProrroga() {
        _uiState.update {
            it.copy(
                mostrarDialogoProrroga = false,
                facturaParaProrroga = null
            )
        }
    }

    fun prorrogarVencimientoFactura(facturaId: String, nuevaFechaVencimiento: String) {
        viewModelScope.launch {
            val res = facturaRepository.prorrogarVencimiento(facturaId, nuevaFechaVencimiento)
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            mostrarDialogoProrroga = false,
                            facturaParaProrroga = null,
                            mensajeExito = "✓ Fecha de vencimiento prorrogada a $nuevaFechaVencimiento."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(mensajeError = "Error al prorrogar vencimiento: ${e.message}")
                    }
                }
            )
        }
    }

    fun revertirPagoFactura(facturaId: String, numeroFactura: String = "", proveedor: String = "") {
        viewModelScope.launch {
            val factura = _uiState.value.facturas.find { it.id == facturaId }
            if (factura != null && factura.totalAbonadoReal > 0) {
                val simbolo = SessionManager.monedaSimbolo.ifBlank { "S/" }
                _uiState.update {
                    it.copy(
                        mensajeError = "Esta factura tiene ${factura.abonos.size} abono(s) registrado(s) por $simbolo " +
                                String.format(java.util.Locale.US, "%.2f", factura.totalAbonadoReal) +
                                ". Anula primero los abonos desde el historial de la factura; la reversión no borra pagos reales."
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    procesandoPago = true,
                    mensajeError = null,
                    mensajeExito = null
                )
            }
            val res = facturaRepository.actualizarEstadoPagoFactura(facturaId, "PENDIENTE")
            if (res.isSuccess) {
                val detalle = if (numeroFactura.isNotBlank()) "Factura $numeroFactura" else "La factura"
                _uiState.update {
                    it.copy(
                        procesandoPago = false,
                        mensajeExito = "↺ $detalle devuelta a estado PENDIENTE DE PAGO."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        procesandoPago = false,
                        mensajeError = res.exceptionOrNull()?.message
                            ?: "Error al revertir el estado de la factura."
                    )
                }
            }
        }
    }

    fun abrirDialogoCrearProveedor() {
        _uiState.update { it.copy(mostrarDialogoProveedor = true, proveedorEditando = null) }
    }

    fun abrirDialogoEditarProveedor(proveedor: Proveedor) {
        _uiState.update { it.copy(mostrarDialogoProveedor = true, proveedorEditando = proveedor) }
    }

    fun cerrarDialogoProveedor() {
        _uiState.update { it.copy(mostrarDialogoProveedor = false, proveedorEditando = null) }
    }

    fun guardarProveedor(
        nombre: String,
        idFiscal: String,
        contacto: String,
        telefono: String,
        email: String,
        direccion: String,
        montoMinimo: Double = 0.0
    ) {
        val nombreTrim = nombre.trim()
        if (nombreTrim.isBlank()) {
            _uiState.update { it.copy(mensajeError = "El nombre o razón social del proveedor es obligatorio.") }
            return
        }

        if (_uiState.value.guardandoProveedor) return

        val editando = _uiState.value.proveedorEditando
        val montoLimpio = if (montoMinimo < 0.0) 0.0 else montoMinimo
        val proveedorAGuardar = Proveedor(
            id = editando?.id ?: "",
            nombre = nombreTrim,
            idFiscal = idFiscal.trim(),
            contacto = contacto.trim(),
            telefono = telefono.trim(),
            email = email.trim(),
            direccion = direccion.trim(),
            montoMinimoPedido = montoLimpio
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    guardandoProveedor = true,
                    mensajeError = null,
                    mensajeExito = null
                )
            }
            val res = proveedorRepository.registrarOActualizarProveedor(proveedorAGuardar)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        guardandoProveedor = false,
                        mostrarDialogoProveedor = false,
                        proveedorEditando = null,
                        mensajeExito = "Proveedor \"${proveedorAGuardar.nombre}\" guardado correctamente."
                    )
                }
            } else {
                val errorMsg = res.exceptionOrNull()?.message ?: "Error al guardar el proveedor."
                _uiState.update {
                    it.copy(
                        guardandoProveedor = false,
                        mensajeError = errorMsg
                    )
                }
            }
        }
    }

    fun actualizarTelefonoProveedor(proveedorNombre: String, nuevoTelefono: String) {
        val telTrim = nuevoTelefono.trim()
        if (telTrim.isBlank()) {
            _uiState.update { it.copy(mensajeError = "No se guardó el teléfono: el número está vacío.") }
            return
        }
        val prov = _uiState.value.proveedores.find { it.nombre.equals(proveedorNombre, ignoreCase = true) }
        if (prov == null) {
            _uiState.update { it.copy(mensajeError = "No se pudo guardar el teléfono: no se encontró la ficha de \"$proveedorNombre\".") }
            return
        }
        val provActualizado = prov.copy(telefono = telTrim)
        viewModelScope.launch {
            proveedorRepository.registrarOActualizarProveedor(provActualizado).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "✓ Teléfono de $proveedorNombre guardado en su ficha.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "No se pudo guardar el teléfono: ${e.message}") }
                }
            )
        }
    }

    /** Cada mensaje se consume SOLO después de mostrarse: jamás se pierde uno en silencio. */
    fun consumirMensajeExito() {
        _uiState.update { it.copy(mensajeExito = null) }
    }

    fun consumirMensajeError() {
        _uiState.update { it.copy(mensajeError = null) }
    }
}
