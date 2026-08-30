package com.app.administradorfarmadon.compras.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.compartido.datos.FacturaCompraRepository
import com.app.administradorfarmadon.inventario.compartido.datos.IngresoMercaderiaRepository
import com.app.administradorfarmadon.inventario.compartido.datos.ProveedorRepository
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.datos.PedidoCompraRepository
import com.app.administradorfarmadon.compras.saldoafavor.datos.SaldoAFavorOperacionesRepository
import com.app.administradorfarmadon.configuracion.metodospago.datos.MetodosPagoRepository
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ComprasViewModel(
    private val proveedorRepository: ProveedorRepository = ProveedorRepository(),
    private val facturaRepository: FacturaCompraRepository = FacturaCompraRepository(),
    private val inventarioRepository: InventarioFirestoreRepository = InventarioFirestoreRepository(),
    private val pedidoCompraRepository: PedidoCompraRepository = PedidoCompraRepository(),
    private val ingresoRepository: IngresoMercaderiaRepository = IngresoMercaderiaRepository(),
    private val metodosPagoRepository: MetodosPagoRepository = MetodosPagoRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ComprasViewModel"

        // ──”€──”€ PERSISTENCIA EN MEMORIA DE SESIí“N (PERMANECE AL NAVEGAR ENTRE Mí“DULOS) ──”€──”€
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
    // Debe vivir ANTES del init —” el reinicio por cambio de sede lo limpia al instante
    private val ultimoValorEnviado = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private var ultimoAbonoIntentoId: String = ""
    private var huellaUltimoAbono: String = ""

    private fun huellaAbono(
        facturaId: String,
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        notas: String
    ): String = listOf(
        facturaId,
        monto,
        metodoPago,
        numeroOperacion.trim().uppercase(),
        notas.trim()
    ).joinToString("|")

    private var ultimoNotaIntentoId: String = ""
    private var huellaUltimoNota: String = ""

    private fun huellaNotaCredito(
        facturaId: String,
        numeroDocumento: String,
        monto: Double,
        motivo: String
    ): String = listOf(
        facturaId,
        numeroDocumento.trim().uppercase(),
        monto,
        motivo.trim()
    ).joinToString("|")

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
                metodosPago = emptyList(),
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
                    marcarErrorEscucha(e.message ?: e.toString())
                }
                .collect { mapa ->
                    // Poda del rastro local: lo que el servidor ya confirma deja de ser "pendiente"
                    ultimoValorEnviado.entries.removeAll { (clave, valor) ->
                        val proveedor = clave.substringBefore('\u0001')
                        val productoId = clave.substringAfter('\u0001')
                        mapa.totales[proveedor]?.get(productoId) == valor
                    }
                    _uiState.update {
                        it.copy(
                            pedidosPorProveedor = mapa.totales,
                            contribuidoresCarrito = mapa.contribuidores
                        )
                    }
                }
        }
        escuchasJobs.add(job)
    }

    /** La pantalla jamás confunde "no hay datos" con "se cayó la actualización en vivo". */
    private fun marcarErrorEscucha(motivo: String) {
        _uiState.update { it.copy(errorEscucha = motivo) }
    }

    fun reintentarEscuchas() {
        escuchasJobs.forEach { it.cancel() }
        escuchasJobs.clear()
        _uiState.update { it.copy(cargando = true, errorEscucha = null) }
        iniciarEscuchasTiempoReal()
        iniciarCarritoCompartido()
    }

    private fun iniciarEscuchasTiempoReal() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva

        // 1. Escuchar Proveedores en tiempo real
        escuchasJobs.add(viewModelScope.launch {
            proveedorRepository.observarProveedores(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error en flujo de proveedores: ${e.message}", e)
                    marcarErrorEscucha(e.message ?: e.toString())
                }
                .collect { lista ->
                    _uiState.update { it.copy(proveedores = lista, cargando = false) }
                }
        })

        // 2. Escuchar Facturas de Compra en tiempo real
        escuchasJobs.add(viewModelScope.launch {
            facturaRepository.observarFacturasRecientes(onErrorEscucha = ::marcarErrorEscucha)
                .catch { e ->
                    Log.e(TAG, "Error en flujo de facturas: ${e.message}", e)
                    marcarErrorEscucha(e.message ?: e.toString())
                }
                .collect { facturas ->
                    _uiState.update { it.copy(facturas = facturas) }
                }
        })

        // 3. Métodos de pago configurados para ESTA sucursal (el dinero real, no una lista fija)
        escuchasJobs.add(viewModelScope.launch {
            metodosPagoRepository.observarMetodosPago(sucursalId)
                .catch { e ->
                    Log.e(TAG, "Error escuchando métodos de pago: ${e.message}", e)
                    marcarErrorEscucha(e.message ?: e.toString())
                }
                .collect { lista ->
                    _uiState.update { it.copy(metodosPago = lista) }
                }
        })

        // 3. Escuchar Inventario / Catálogo de Productos Completo (Tiempo Real)
        if (farmaciaId.isNotBlank() && sucursalId.isNotBlank()) {
            escuchasJobs.add(viewModelScope.launch {
                inventarioRepository.observarInventario(farmaciaId)
                    .catch { e ->
                        Log.e(TAG, "Error en flujo de inventario para compras: ${e.message}", e)
                        marcarErrorEscucha(e.message ?: e.toString())
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
                    marcarErrorEscucha(e.message ?: e.toString())
                }
                .collect { pedidos ->
                    _uiState.update { it.copy(pedidosGuardados = pedidos) }
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

    // ──”€──”€ UNA SOLA PLUMA: la escucha en vivo dibuja el borrador; aquí solo escribimos a la base ──”€──”€
    private fun claveCarrito(proveedor: String, productoId: String) = "$proveedor\u0001$productoId"

    private fun valorBaseCarrito(proveedor: String, productoId: String): Int {
        val enPantalla = _uiState.value.pedidosPorProveedor[proveedor]?.get(productoId) ?: 0
        val enviadoPreviamente = ultimoValorEnviado[claveCarrito(proveedor, productoId)] ?: 0
        return maxOf(enPantalla, enviadoPreviamente)
    }

    private fun aplicarCambiosCarrito(proveedorNombre: String, cambios: Map<String, Int>, esDelta: Boolean = true) {
        if (cambios.isEmpty()) return
        // Captura sede al momento del toque —” evita que un cambio de sede mid-tap escriba en sede equivocada (R1)
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        val usuarioId = SessionManager.idCajera.ifBlank { "Sistema" }
        val usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Sistema" }
        viewModelScope.launch {
            val ok = pedidoCompraRepository.guardarProductosCarrito(
                proveedorNombre, cambios, farmaciaCapturada, sucursalCapturada,
                usuarioId = usuarioNombre, usuarioNombre = usuarioNombre,
                esDelta = esDelta
            )
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
        aplicarCambiosCarrito(proveedorNombre, cambios, esDelta = false)
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
            aplicarCambiosCarrito(proveedor, cambios, esDelta = false)
        }
    }

    private fun modificarCantidadPedirProveedor(proveedorNombre: String, productoId: String, delta: Int) {
        // El clic ES el incremento: se envía el +1/-1 tal cual. La transacción del
        // repositorio lo suma sobre la verdad vigente, de modo que dos usuarios que
        // tocan el mismo producto al mismo tiempo jamás pierden una unidad.
        if (delta == 0) return
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to delta))
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
                            mensajeExito = "──œ“ ¡Orden para ${pedido.proveedorNombre} guardada y marcada como ENVIADA!$notaMultiusuario"
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
        val ultimaFacturaNumero = pedido.recepciones.asReversed()
            .firstOrNull { it.numeroFactura.isNotBlank() }
            ?.numeroFactura
            ?.trim()
            ?.uppercase()
        val facturaViva = ultimaFacturaNumero?.let { numero ->
            _uiState.value.facturas.firstOrNull {
                it.numeroFactura.equals(numero, ignoreCase = true) &&
                        (pedido.proveedorId.isBlank() || it.proveedorId == pedido.proveedorId)
            }
        }
        _uiState.update {
            it.copy(
                pedidoParaRecepcionar = pedido,
                facturaRecepcionExistente = facturaViva,
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
        if (_uiState.value.procesandoRecepcion) return
        _uiState.update {
            it.copy(
                pedidoParaRecepcionar = null,
                facturaRecepcionExistente = null,
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
        montoPagado: Double,
        metodoPago: String = "",
        pagosRecepcion: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList(),
        saldoAFavorUsado: Double = 0.0,
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
                montoPagado = montoPagado,
                metodoPago = metodoPago,
                pagosRecepcion = pagosRecepcion,
                saldoAFavorUsado = saldoAFavorUsado,
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
                            mensajeExito = "──œ“ ¡Mercadería recibida y asentada con éxito! Stock y Factura $numeroFactura actualizados."
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
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to 0), esDelta = false)
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

    fun cobrarSaldoAFavor(proveedorId: String, monto: Double, documento: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = SaldoAFavorOperacionesRepository().registrarEgresoSaldo(
                proveedorId = proveedorId,
                monto = monto,
                tipo = SaldoAFavorOperacionesRepository.TIPO_COBRADO,
                documento = documento,
                motivo = "Cobro en efectivo del saldo a favor"
            )
            onComplete(res)
        }
    }

    fun declararSaldoPerdido(proveedorId: String, monto: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = SaldoAFavorOperacionesRepository().registrarEgresoSaldo(
                proveedorId = proveedorId,
                monto = monto,
                tipo = SaldoAFavorOperacionesRepository.TIPO_PERDIDO,
                documento = "",
                motivo = motivo
            )
            onComplete(res)
        }
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
                facturaParaAbonarId = factura.id,
                mensajeError = null
            )
        }
    }

    fun cerrarDialogoAbono() {
        _uiState.update {
            it.copy(
                mostrarDialogoAbono = false,
                facturaParaAbonarId = null,
                procesandoPago = false
            )
        }
    }

    fun registrarAbonoFactura(
        facturaId: String,
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        pagos: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList()
    ) {
        // El botón se desactiva en pantalla, pero este candado también protege
        // contra dos toques que lleguen antes de la siguiente recomposición.
        if (_uiState.value.procesandoPago) return

        val huellaActual = huellaAbono(facturaId, monto, metodoPago, numeroOperacion, "")
        val idIntento = if (ultimoAbonoIntentoId.isNotBlank() && huellaUltimoAbono == huellaActual) {
            ultimoAbonoIntentoId
        } else {
            java.util.UUID.randomUUID().toString().also {
                ultimoAbonoIntentoId = it
                huellaUltimoAbono = huellaActual
            }
        }

        // Se marca antes de lanzar la corrutina: una segunda llamada inmediata
        // ve el estado ocupado y no inicia otro pago.
        _uiState.update { it.copy(procesandoPago = true, mensajeError = null, mensajeExito = null) }
        viewModelScope.launch {
            val usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" }
            val usuarioEmail = SessionManager.email.ifBlank { "" }

            val res = facturaRepository.registrarAbono(
                facturaId = facturaId,
                monto = monto,
                metodoPago = metodoPago,
                numeroOperacion = numeroOperacion,
                pagos = pagos,
                usuarioNombre = usuarioNombre,
                usuarioEmail = usuarioEmail,
                notas = "",
                idempotenciaId = idIntento
            )

            res.fold(
                onSuccess = {
                    val montoStr = String.format(java.util.Locale.US, "%.2f", monto)
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mostrarDialogoAbono = false,
                            facturaParaAbonarId = null,
                            mensajeExito = "──œ“ Abono de ${SessionManager.monedaSimbolo.ifBlank { "S/" }} $montoStr registrado con éxito."
                        )
                    }
                    ultimoAbonoIntentoId = ""
                    huellaUltimoAbono = ""
                },
                onFailure = { e ->
                    val detalle = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
                    val esFallaDeConexion = detalle.contains("UNAVAILABLE", true) ||
                            detalle.contains("Network", true) ||
                            detalle.contains("timeout", true)
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mensajeError = if (esFallaDeConexion) {
                                "Firebase no confirmó el abono por un problema de conexión. Reintenta el mismo pago; no se duplicará. Detalle: $detalle"
                            } else {
                                "No se pudo registrar el abono. Revisa el detalle antes de reintentar: $detalle"
                            }
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
                facturaParaProrrogaId = factura.id,
                mensajeError = null
            )
        }
    }

    fun cerrarDialogoProrroga() {
        _uiState.update {
            it.copy(
                mostrarDialogoProrroga = false,
                facturaParaProrrogaId = null
            )
        }
    }

    // ── ANULAR FACTURA (plan Anular Factura: papel + producto + plata, cerrados juntos) ──

    /** Regla de negocio de dinero: solo dueño/administración cierra facturas con plata pagada. */
    val esUsuarioAutorizadoPlata: Boolean
        get() {
            val r = SessionManager.rol.trim()
            if (r.isBlank()) return true
            return listOf("dueño", "dueno", "administrador", "admin").any { r.equals(it, ignoreCase = true) }
        }

    fun abrirDialogoAnularFactura(factura: FacturaCompra) {
        _uiState.update {
            it.copy(
                mostrarDialogoAnulacion = true,
                facturaParaAnular = factura,
                procesandoAnulacion = false,
                cargandoLineasAnulacion = true,
                lineasAnulacion = emptyList(),
                mensajeError = null
            )
        }
        // Resumen EN VIVO: se lee el estante AHORA para mostrar entró/hoy/devuelve ANTES de confirmar.
        // La transacción final re-verifica atómicamente: si algo cambió mientras tanto, gana la tx.
        viewModelScope.launch {
            try {
                val lotesPorProducto = ingresoRepository.leerLotesDeProductos(factura.items.map { it.productoId })
                val lineas = factura.items.map { item ->
                    val lotes = lotesPorProducto[item.productoId]
                    val productoExiste = lotes != null
                    val loteRes = lotes?.let { FechaVencimientoHelper.resolverLote(it, item.loteNumero) }
                    val loteData = loteRes?.second as? Map<*, *>
                    val hoy = ((loteData?.get("cantidad") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
                    val devuelve = kotlin.math.min(item.cantidadTotal, hoy)
                    LineaAnulacionVista(
                        productoId = item.productoId,
                        productoNombre = item.productoNombre,
                        loteNumero = item.loteNumero,
                        vencimiento = loteData?.get("vencimiento") as? String ?: item.vencimiento,
                        entro = item.cantidadTotal,
                        hoy = hoy,
                        devuelve = devuelve,
                        noVuelve = (item.cantidadTotal - devuelve).coerceAtLeast(0.0),
                        loteExiste = loteRes != null,
                        productoExiste = productoExiste
                    )
                }
                _uiState.update { it.copy(cargandoLineasAnulacion = false, lineasAnulacion = lineas) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargandoLineasAnulacion = false,
                        mostrarDialogoAnulacion = false,
                        facturaParaAnular = null,
                        mensajeError = "No se pudo leer el inventario actual para comparar con la factura: ${e.message}"
                    )
                }
            }
        }
    }

    fun cerrarDialogoAnularFactura() {
        if (_uiState.value.procesandoAnulacion) return
        _uiState.update {
            it.copy(
                mostrarDialogoAnulacion = false,
                facturaParaAnular = null,
                lineasAnulacion = emptyList()
            )
        }
    }

    fun confirmarAnulacionFactura(
        motivo: String,
        respuestaPlata: String?,
        metodoDevolucion: String? = null,
        referenciaDevolucion: String? = null
    ) {
        val factura = _uiState.value.facturaParaAnular ?: return
        if (_uiState.value.procesandoAnulacion) return
        if (factura.esAnulada) {
            _uiState.update { it.copy(mensajeError = "Esta factura ya fue anulada por otro usuario. Nada cambió.") }
            return
        }
        val plata = factura.plataPagadaEnFactura
        if (plata > 0.01) {
            if (!esUsuarioAutorizadoPlata) {
                _uiState.update { it.copy(mensajeError = "Solo el dueño o administración puede anular una factura con dinero ya pagado. Pídele ayuda a un usuario de administración.") }
                return
            }
            if (respuestaPlata == null) {
                _uiState.update { it.copy(mensajeError = "Indica qué pasa con el dinero ya pagado: saldo a favor, devolución recibida o pérdida. La plata no puede quedar a medias.") }
                return
            }
            if (respuestaPlata == "DEVOLUCION_RECIBIDA" && metodoDevolucion.isNullOrBlank()) {
                _uiState.update { it.copy(mensajeError = "Indica en qué te devolvieron el dinero (efectivo, transferencia, cheque u otro).") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAnulacion = true, mensajeError = null) }
            val res = ingresoRepository.anularFactura(
                facturaId = factura.id,
                motivo = motivo,
                conDevolucion = true,
                usuarioEmail = SessionManager.email,
                usuarioNombre = SessionManager.nombreUsuario,
                respuestaPlata = respuestaPlata ?: "",
                metodoDevolucion = metodoDevolucion.orEmpty(),
                referenciaDevolucion = referenciaDevolucion.orEmpty()
            )
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            procesandoAnulacion = false,
                            mostrarDialogoAnulacion = false,
                            facturaParaAnular = null,
                            lineasAnulacion = emptyList(),
                            mensajeExito = "Factura ${factura.numeroFactura.ifBlank { "sin número" }} anulada. El papel, el estante y la plata quedaron escritos."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(procesandoAnulacion = false, mensajeError = e.message ?: "No se pudo anular la factura.")
                    }
                }
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
                            facturaParaProrrogaId = null,
                            mensajeExito = "──œ“ Fecha de vencimiento prorrogada a $nuevaFechaVencimiento."
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

    // ── NOTA DE CRÉDITO / AJUSTE DE FACTURA: el papel se reduce con documento, jamás en silencio ──
    fun abrirDialogoNotaCredito(factura: FacturaCompra) {
        _uiState.update {
            it.copy(
                mostrarDialogoNotaCredito = true,
                facturaParaNotaCreditoId = factura.id,
                mensajeError = null
            )
        }
    }

    fun cerrarDialogoNotaCredito() {
        _uiState.update {
            it.copy(
                mostrarDialogoNotaCredito = false,
                facturaParaNotaCreditoId = null,
                procesandoNotaCredito = false
            )
        }
    }

    fun registrarNotaCredito(facturaId: String, numeroDocumento: String, monto: Double, motivo: String) {
        if (_uiState.value.procesandoNotaCredito) return
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update {
                it.copy(mensajeError = "Solo el dueño o administración puede registrar notas de crédito. Pídele ayuda a un usuario de administración.")
            }
            return
        }

        // Idempotencia: un reintento tras corte de red jamás duplica la nota de crédito.
        val huellaActual = huellaNotaCredito(facturaId, numeroDocumento, monto, motivo)
        val idIntento = if (ultimoNotaIntentoId.isNotBlank() && huellaUltimoNota == huellaActual) {
            ultimoNotaIntentoId
        } else {
            java.util.UUID.randomUUID().toString().also {
                ultimoNotaIntentoId = it
                huellaUltimoNota = huellaActual
            }
        }

        _uiState.update { it.copy(procesandoNotaCredito = true, mensajeError = null, mensajeExito = null) }
        viewModelScope.launch {
            val usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" }
            val usuarioEmail = SessionManager.email
            val res = facturaRepository.registrarNotaCredito(
                facturaId = facturaId,
                numeroDocumento = numeroDocumento,
                monto = monto,
                motivo = motivo,
                usuarioNombre = usuarioNombre,
                usuarioEmail = usuarioEmail,
                idempotenciaId = idIntento
            )
            res.fold(
                onSuccess = {
                    val montoStr = String.format(java.util.Locale.US, "%.2f", monto)
                    _uiState.update {
                        it.copy(
                            procesandoNotaCredito = false,
                            mostrarDialogoNotaCredito = false,
                            facturaParaNotaCreditoId = null,
                            mensajeExito = "Nota de crédito $numeroDocumento por ${SessionManager.monedaSimbolo.ifBlank { "S/" }} $montoStr registrada."
                        )
                    }
                    ultimoNotaIntentoId = ""
                    huellaUltimoNota = ""
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoNotaCredito = false,
                            mensajeError = "No se pudo registrar la nota de crédito: ${e.message}"
                        )
                    }
                }
            )
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
                    _uiState.update { it.copy(mensajeExito = "──œ“ Teléfono de $proveedorNombre guardado en su ficha.") }
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
