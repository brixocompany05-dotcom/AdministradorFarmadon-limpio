package com.app.administradorfarmadon.compras.logica

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
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

    // Scroll de cada pestaña vive en el ViewModel (uno solo por actividad): al navegar
    // entre módulos o cambiar de pestaña, la lista NO salta al inicio.
    val listaReposicion = LazyListState()
    val listaProveedores = LazyListState()
    val listaCuentas = LazyListState()

    // Debe vivir ANTES del init —” el reinicio por cambio de sede lo limpia al instante
    private val ultimoValorEnviado = java.util.concurrent.ConcurrentHashMap<String, Int>()
    /** Objetivos locales del carrito: el número que la pantalla muestra mientras la red confirma. */
    private val optimismoObjetivo = java.util.concurrent.ConcurrentHashMap<String, Int>()
    /** Claves con una escritura real aún en tránsito (el optimismo solo aplica mientras exista). */
    private val escriturasCarritoEnCurso = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    /** Id de orden por proveedor: el reintento reutiliza el mismo documento (anti duplicación). */
    private val intentoEnvioIds = java.util.concurrent.ConcurrentHashMap<String, String>()
    /** Última foto viva confirmada por el servidor (para revertir sin pisar la verdad). */
    private var ultimoServidorCarrito: Map<String, Map<String, Int>> = emptyMap()
    private var ultimoAbonoIntentoId: String = ""
    private var huellaUltimoAbono: String = ""

    /** Candado síncrono de acciones de UNA orden (cancelar, cerrar ajuste, descartar):
     * la clave "op:pedidoId[:productoId]" vive solo mientras la operación está en vuelo;
     * un segundo toque de la MISMA acción se ignora hasta tener respuesta del servidor. */
    private val accionesOrdenEnCurso = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun intentarAccionOrden(clave: String): Boolean = accionesOrdenEnCurso.add(clave)
    private fun liberarAccionOrden(clave: String) { accionesOrdenEnCurso.remove(clave) }

    private fun huellaAbono(
        facturaId: String,
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        notas: String,
        pagos: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList()
    ): String = listOf(
        facturaId,
        monto,
        metodoPago,
        numeroOperacion.trim().uppercase(),
        notas.trim(),
        pagos.joinToString("|") { p -> "${p.metodoPago}:${p.monto}:${p.numeroOperacion.trim().uppercase()}" }
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
        optimismoObjetivo.clear()
        escriturasCarritoEnCurso.clear()
        intentoEnvioIds.clear()
        ultimoServidorCarrito = emptyMap()
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
        viewModelScope.launch {
            listaReposicion.scrollToItem(0)
            listaProveedores.scrollToItem(0)
            listaCuentas.scrollToItem(0)
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
                    ultimoServidorCarrito = mapa.totales
                    _uiState.update {
                        it.copy(
                            pedidosPorProveedor = construirCarritoMostrado(mapa.totales),
                            contribuidoresCarrito = mapa.contribuidores
                        )
                    }
                }
        }
        escuchasJobs.add(job)
    }

    /**
     * El número que la pantalla debe mostrar: la verdad del servidor, pero sin dejar
     * que un snapshot intermedio baje lo que el usuario acaba de tocar. Cuando el
     * servidor alcanza (o supera) el objetivo local, el objetivo se libera y se
     * muestra la verdad vigente.
     */
    private fun construirCarritoMostrado(servidor: Map<String, Map<String, Int>>): Map<String, Map<String, Int>> {
        val mostrado = servidor.mapValues { (_, carro) -> carro.toMutableMap() }.toMutableMap()
        optimismoObjetivo.entries.toList().forEach { (clave, objetivo) ->
            val proveedor = clave.substringBefore('\u0001')
            val productoId = clave.substringAfter('\u0001')
            val servidorValor = servidor[proveedor]?.get(productoId) ?: 0
            if (objetivo > servidorValor && escriturasCarritoEnCurso.contains(clave)) {
                mostrado.getOrPut(proveedor) { mutableMapOf() }[productoId] = objetivo
            } else {
                optimismoObjetivo.remove(clave)
            }
        }
        return mostrado
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
        intentoEnvioIds.clear()
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

    private fun aplicarCambiosCarrito(proveedorNombre: String, cambios: Map<String, Int>, esDelta: Boolean = true) {
        if (cambios.isEmpty()) return
        // Captura sede al momento del toque —” evita que un cambio de sede mid-tap escriba en sede equivocada (R1)
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        val usuarioId = SessionManager.idCajera.ifBlank { "Sistema" }
        val usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Sistema" }

        // Optimismo por objetivo: la pantalla responde al toque y ningún snapshot
        // intermedio (más viejo) puede bajar el número. Se suelta al confirmar.
        cambios.forEach { (productoId, valor) ->
            val clave = claveCarrito(proveedorNombre, productoId)
            val servidor = _uiState.value.pedidosPorProveedor[proveedorNombre]?.get(productoId) ?: 0
            val base = optimismoObjetivo[clave] ?: servidor
            val objetivo = if (esDelta) base + valor else valor
            if (objetivo <= 0) optimismoObjetivo.remove(clave) else optimismoObjetivo[clave] = objetivo
            escriturasCarritoEnCurso.add(clave)
        }
        _uiState.update { current ->
            val mapa = current.pedidosPorProveedor.toMutableMap()
            val prov = (mapa[proveedorNombre] ?: emptyMap()).toMutableMap()
            cambios.forEach { (productoId, valor) ->
                val objetivo = optimismoObjetivo[claveCarrito(proveedorNombre, productoId)]
                val nuevo = objetivo ?: 0
                if (nuevo <= 0) prov.remove(productoId) else prov[productoId] = nuevo
            }
            if (prov.isNotEmpty()) mapa[proveedorNombre] = prov else mapa.remove(proveedorNombre)
            current.copy(pedidosPorProveedor = mapa)
        }

        viewModelScope.launch {
            val resultado = pedidoCompraRepository.guardarProductosCarrito(
                proveedorNombre, cambios, farmaciaCapturada, sucursalCapturada,
                usuarioId = usuarioId, usuarioNombre = usuarioNombre,
                esDelta = esDelta
            )
            cambios.keys.forEach { productoId ->
                escriturasCarritoEnCurso.remove(claveCarrito(proveedorNombre, productoId))
            }
            if (resultado.isFailure) {
                val causa = resultado.exceptionOrNull()?.message ?: "falló la conexión"
                // Revertir el optimismo: se quita el objetivo fallido y la pantalla
                // vuelve a la última verdad confirmada por el servidor.
                cambios.keys.forEach { productoId ->
                    val clave = claveCarrito(proveedorNombre, productoId)
                    optimismoObjetivo.remove(clave)
                    ultimoValorEnviado.remove(clave)
                }
                _uiState.update {
                    it.copy(
                        pedidosPorProveedor = construirCarritoMostrado(ultimoServidorCarrito),
                        mensajeError = "No se pudo actualizar el pedido de $proveedorNombre: $causa. El pedido sigue como estaba; inténtalo de nuevo."
                    )
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

    private fun modificarCantidadPedirProveedor(proveedorNombre: String, productoId: String, delta: Int) {
        // El clic ES el incremento: se envía el +1/-1 tal cual. La transacción del
        // repositorio lo suma sobre la verdad vigente, de modo que dos usuarios que
        // tocan el mismo producto al mismo tiempo jamás pierden una unidad.
        if (delta == 0) return
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to delta))
    }

    fun seleccionarSubTabPedidosDerecha(subTab: String) {
        _uiState.update { it.copy(subTabPedidosDerecha = subTab) }
    }

    fun confirmarPedidoEnviado(pedido: PedidoProveedor) {
        if (_uiState.value.enviandoPedido) return

        // Validación: no enviar una orden vacía
        if (pedido.itemsValidos.isEmpty()) {
            _uiState.update { it.copy(mensajeError = "El pedido no tiene productos con cantidad mayor a 0.") }
            return
        }

        // Captura de sede al momento del toque (R1): la orden se consume, guarda y
        // restaura SIEMPRE en la sede donde el usuario la armó, jamás en otra.
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva

        // Idempotencia anti duplicación: si el envío falla de forma ambigua y el usuario
        // reintenta, se reutiliza el MISMO id de orden y se sobrescribe el mismo documento.
        // Jamás nacen dos órdenes para un mismo intento de pedido.
        val idEnvio = intentoEnvioIds.getOrPut(pedido.proveedorNombre) {
            java.util.UUID.randomUUID().toString()
        }

        // El candado se marca SÍNCRONO (antes del launch): dos toques en el mismo
        // instante jamás inician dos envíos (el segundo ve el candado y se detiene).
        _uiState.update { it.copy(enviandoPedido = true) }
        viewModelScope.launch {

            // Reserva atómica: distingue "otro usuario lo tomó primero" (éxito vacío)
            // de "falló la conexión" (failure con la causa real). Jamás se confunden.
            val resultadoConsumo = pedidoCompraRepository.consumirCarritoParaEnvio(
                pedido.proveedorNombre,
                farmaciaCapturada,
                sucursalCapturada
            )
            val borradorVigente = resultadoConsumo.getOrNull()

            if (borradorVigente == null) {
                _uiState.update { it.copy(enviandoPedido = false) }
                val causa = resultadoConsumo.exceptionOrNull()?.message ?: "Sin detalle del servidor"
                _uiState.update {
                    it.copy(
                        mensajeError = "No se pudo registrar la orden: $causa. El pedido sigue intacto en el borrador; intenta nuevamente."
                    )
                }
                return@launch
            }

            if (borradorVigente.isEmpty()) {
                _uiState.update { it.copy(enviandoPedido = false) }
                _uiState.update {
                    it.copy(
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
                    precioCompra = Math.round(prod.purchasePrice * 100.0) / 100.0,
                    cantidad = cantidad
                )
            }

            if (itemsVigentes.isEmpty()) {
                _uiState.update { it.copy(enviandoPedido = false) }
                val restaurado = pedidoCompraRepository.guardarProductosCarrito(
                    pedido.proveedorNombre,
                    borradorVigente,
                    farmaciaCapturada,
                    sucursalCapturada
                ).isSuccess
                _uiState.update {
                    it.copy(
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
                id = idEnvio,
                proveedorId = pedido.proveedorId,
                proveedorNombre = pedido.proveedorNombre,
                proveedorRuc = pedido.proveedorRuc,
                proveedorTelefono = pedido.proveedorTelefono,
                items = itemsVigentes,
                estado = "ENVIADO"
            )

            val resultado = pedidoCompraRepository.guardarPedidoEnviado(
                nuevoPedido,
                farmaciaCapturada,
                sucursalCapturada
            )
            _uiState.update { it.copy(enviandoPedido = false) }
            resultado.fold(
                onSuccess = {
                    intentoEnvioIds.remove(pedido.proveedorNombre)
                    limpiarPedidoProveedor(pedido.proveedorNombre)
                    _uiState.update {
                        it.copy(
                            subTabPedidosDerecha = "REALIZADOS",
                            envioExitosoProveedor = pedido.proveedorNombre,
                            mensajeExito = " ¡Orden para ${pedido.proveedorNombre} guardada y marcada como ENVIADA!$notaMultiusuario"
                        )
                    }
                },
                onFailure = { e ->
                    // La restauración del borrador se ESPERA y se VERIFICA antes de decir nada:
                    // prohibido prometer "restaurado" sin comprobarlo.
                    val restaurado = pedidoCompraRepository.guardarProductosCarrito(
                        pedido.proveedorNombre,
                        borradorVigente,
                        farmaciaCapturada,
                        sucursalCapturada
                    ).isSuccess
                    _uiState.update {
                        it.copy(
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
        if (!intentarAccionOrden("cancelar:$pedidoId")) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            try {
                pedidoCompraRepository.cancelarPedido(pedidoId, farmaciaCapturada, sucursalCapturada).fold(
                    onSuccess = {
                        _uiState.update { it.copy(mensajeExito = "Pedido cancelado correctamente.") }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(mensajeError = "Error al cancelar pedido: ${e.message}") }
                    }
                )
            } finally {
                liberarAccionOrden("cancelar:$pedidoId")
            }
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
        // Captura de sede al momento del toque (R1): toda la recepción (stock,
        // kardex, factura, pedido) se asienta en la sede donde se abrió la orden.
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoRecepcion = true, mensajeError = null) }

            // Sello idempotente del ACTO de entrega: reintentar con los mismos datos
            // jamás duplica (mismo sello); cambiar cantidad, lote, vencimiento o costo
            // genera un acto nuevo (nueva entrega parcial). Sobrevive a cerrar y reabrir.
            val selloEntrega = "$pedidoId|$numeroFactura|${
                itemsRecepcion.joinToString(";") { it ->
                    "${it.productoId}:${it.cantidadTotal}:${it.loteNumero.trim().uppercase()}:${it.vencimiento.trim()}:${it.costoUnitarioReal}"
                }
            }"
            val idIntento = java.util.UUID.nameUUIDFromBytes(selloEntrega.toByteArray()).toString()

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
                idempotenciaId = idIntento,
                farmaciaIdParam = farmaciaCapturada,
                sucursalIdParam = sucursalCapturada
            )

            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            procesandoRecepcion = false,
                            mostrarDialogoRecepcion = false,
                            pedidoParaRecepcionar = null,
                            cargandoIndiceRecepcion = false,
                            indiceLotesOrden = emptyMap(),
                            mensajeExito = " ¡Mercadería recibida y asentada con éxito! Stock y Factura $numeroFactura actualizados."
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
        if (!intentarAccionOrden("ajuste:$pedidoId")) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            try {
                pedidoCompraRepository.cerrarOrdenConAjuste(pedidoId, motivo, farmaciaCapturada, sucursalCapturada).fold(
                    onSuccess = {
                        _uiState.update { it.copy(mensajeExito = "Orden cerrada con ajuste correctamente.") }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(mensajeError = "Error al cerrar orden: ${e.message}") }
                    }
                )
            } finally {
                liberarAccionOrden("ajuste:$pedidoId")
            }
        }
    }

    fun descartarProductoDePedido(pedidoId: String, productoId: String) {
        if (!intentarAccionOrden("descartar:$pedidoId:$productoId")) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            try {
                pedidoCompraRepository.descartarProductoDePedido(pedidoId, productoId, farmaciaCapturada, sucursalCapturada).fold(
                    onSuccess = {
                        _uiState.update { it.copy(mensajeExito = "Producto descartado del pedido por quiebre de stock.") }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(mensajeError = "No se pudo descartar el producto: ${e.message}") }
                    }
                )
            } finally {
                liberarAccionOrden("descartar:$pedidoId:$productoId")
            }
        }
    }

    fun limpiarPedidoProveedor(proveedorNombre: String) {
        // Optimismo inmediato: el panel vacía al instante y la red lo confirma.
        escriturasCarritoEnCurso.removeIf { it.startsWith("$proveedorNombre\u0001") }
        intentoEnvioIds.remove(proveedorNombre)
        val objetivosQuitados = optimismoObjetivo
            .filterKeys { it.startsWith("$proveedorNombre\u0001") }
            .toMutableMap()
        objetivosQuitados.keys.forEach { optimismoObjetivo.remove(it) }
        _uiState.update { current ->
            val mapa = current.pedidosPorProveedor.toMutableMap()
            mapa.remove(proveedorNombre)
            current.copy(pedidosPorProveedor = mapa)
        }
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            val resultado = pedidoCompraRepository.eliminarCarritoProveedor(proveedorNombre, farmaciaCapturada, sucursalCapturada)
            if (resultado.isFailure) {
                val causa = resultado.exceptionOrNull()?.message ?: "falló la conexión"
                objetivosQuitados.forEach { (clave, objetivo) -> optimismoObjetivo[clave] = objetivo }
                _uiState.update {
                    it.copy(
                        pedidosPorProveedor = construirCarritoMostrado(ultimoServidorCarrito),
                        mensajeError = "No se pudo vaciar el pedido de $proveedorNombre: $causa. El pedido sigue como estaba; inténtalo de nuevo."
                    )
                }
            }
        }
        ultimoValorEnviado.keys.removeAll { it.startsWith("$proveedorNombre\u0001") }
    }

    /**
     * Edición de un pedido REALIZADO con auditoría: guarda los cambios y registra
     * en la bitácora qué cambió, quién y cuándo. Solo se edita mientras el pedido
     * sigue sin mercadería recibida; una vez recibido, vive en RECIBIR.
     */
    fun editarPedidoRealizado(pedidoId: String, itemsNuevos: List<ItemPedidoCompra>) {
        if (_uiState.value.procesandoEdicionPedido) return
        val pedidoActual = _uiState.value.pedidosGuardados.find { it.id == pedidoId }
        if (pedidoActual == null) {
            _uiState.update { it.copy(mensajeError = "El pedido ya no está disponible para editar.") }
            return
        }
        if (pedidoActual.estado != "ENVIADO" || pedidoActual.recepciones.isNotEmpty() || pedidoActual.totalUnidadesRecibidas > 0) {
            _uiState.update {
                it.copy(mensajeError = "Este pedido ya recibió mercadería o cambió de estado; no se puede editar aquí. Usa la pestaña RECIBIR.")
            }
            return
        }
        if (itemsNuevos.any { it.cantidad < it.cantidadRecibida }) {
            _uiState.update { it.copy(mensajeError = "La cantidad no puede ser menor a lo ya recibido.") }
            return
        }
        if (itemsNuevos.isEmpty() || itemsNuevos.all { it.cantidad <= 0 }) {
            _uiState.update { it.copy(mensajeError = "El pedido debe tener al menos un producto con cantidad.") }
            return
        }
        val detalle = construirDetalleCambio(pedidoActual.items, itemsNuevos)
        if (detalle.isBlank()) {
            _uiState.update { it.copy(mensajeError = "No hay cambios para guardar.") }
            return
        }

        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        val usuario = SessionManager.nombreUsuario.ifBlank { "Administración" }
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoEdicionPedido = true) }
            val resultado = pedidoCompraRepository.actualizarPedidoRealizado(
                pedidoId,
                itemsNuevos,
                detalle,
                usuario,
                farmaciaCapturada,
                sucursalCapturada
            )
            _uiState.update { it.copy(procesandoEdicionPedido = false) }
            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Pedido actualizado. El cambio quedó registrado en la bitácora.") }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            mensajeError = "No se pudo actualizar el pedido: ${e.message ?: "falló la conexión"}. Ningún cambio se guardó."
                        )
                    }
                }
            )
        }
    }

    /** Borra un pedido realizado por completo, incluida su bitácora (el pedido está muerto). */
    fun eliminarPedidoRealizado(pedidoId: String) {
        if (_uiState.value.procesandoEliminacionPedido) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoEliminacionPedido = true) }
            val resultado = pedidoCompraRepository.eliminarPedidoRealizado(pedidoId, farmaciaCapturada, sucursalCapturada)
            _uiState.update { it.copy(procesandoEliminacionPedido = false) }
            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Pedido eliminado por completo, incluyendo su rastro.") }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(mensajeError = "No se pudo eliminar el pedido: ${e.message ?: "falló la conexión"}.")
                    }
                }
            )
        }
    }

    /** Texto de auditoría: qué cambió entre la versión anterior y la nueva del pedido. */
    private fun construirDetalleCambio(antes: List<ItemPedidoCompra>, despues: List<ItemPedidoCompra>): String {
        val cambios = mutableListOf<String>()
        val despuesPorId = despues.associateBy { it.productoId }
        antes.forEach { item ->
            val nuevo = despuesPorId[item.productoId]
            when {
                nuevo == null -> cambios += "Se quitó ${item.productoNombre}"
                nuevo.cantidad != item.cantidad -> cambios += "${item.productoNombre}: ${item.cantidad} → ${nuevo.cantidad}"
            }
        }
        val antesIds = antes.map { it.productoId }.toSet()
        despues.filter { it.productoId !in antesIds }.forEach { cambios += "Se agregó ${it.productoNombre} (${it.cantidad})" }
        return cambios.joinToString("; ")
    }

    fun seleccionarProveedor(proveedorId: String) {
        proveedorSeleccionadoIdGuardado = proveedorId
        _uiState.update { it.copy(proveedorSeleccionadoId = proveedorId) }
    }

    fun vincularProductoAProveedor(productoId: String, proveedor: Proveedor) {
        viewModelScope.launch {
            proveedorRepository.vincularProducto(productoId, proveedor.id, proveedor.nombre).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensajeExito = "Producto afiliado a ${proveedor.nombre}.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "No se pudo afiliar el producto: ${e.message}") }
                }
            )
        }
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
                    _uiState.update { it.copy(mensajeExito = "Proveedor eliminado correctamente.") }
                    onResult(true, "Proveedor eliminado correctamente.")
                }.onFailure { e ->
                    val msg = e.message ?: "Error al eliminar proveedor."
                    _uiState.update { it.copy(mensajeError = msg) }
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Error inesperado al eliminar proveedor."
                _uiState.update { it.copy(mensajeError = msg) }
                onResult(false, msg)
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

        // Captura de sede al momento del toque (R1): el abono se registra siempre
        // en la sucursal donde el usuario lo decidió.
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva

        val huellaActual = huellaAbono(facturaId, monto, metodoPago, numeroOperacion, "", pagos)
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
                idempotenciaId = idIntento,
                farmaciaIdParam = farmaciaCapturada,
                sucursalIdParam = sucursalCapturada
            )

            res.fold(
                onSuccess = {
                    val montoStr = String.format(java.util.Locale.US, "%.2f", monto)
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            mostrarDialogoAbono = false,
                            facturaParaAbonarId = null,
                            mensajeExito = " Abono de ${SessionManager.monedaSimbolo.ifBlank { "S/" }} $montoStr registrado con éxito."
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
        // Si hay una escritura de prórroga en vuelo, el diálogo no se cierra a medias.
        if (_uiState.value.procesandoProrroga) return
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
                    ?: throw IllegalStateException("No se pudo leer el estante actual para comparar con la factura. Reintenta.")
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
        // Captura de sede al momento del toque (R1): la anulación escribe en la sede donde se decidió.
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
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
                referenciaDevolucion = referenciaDevolucion.orEmpty(),
                farmaciaIdParam = farmaciaCapturada,
                sucursalIdParam = sucursalCapturada
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
                    val detalle = e.message ?: "No se pudo anular la factura."
                    if (detalle.contains("ya está anulada", true)) {
                        // La primera operación sí llegó a Firebase y la respuesta se perdió:
                        // "ya está anulada" significa éxito, no error. Se cierra sin duplicar nada.
                        _uiState.update {
                            it.copy(
                                procesandoAnulacion = false,
                                mostrarDialogoAnulacion = false,
                                facturaParaAnular = null,
                                lineasAnulacion = emptyList(),
                                mensajeExito = "La factura ya había sido anulada (por esta operación o por otro usuario). Nada cambió."
                            )
                        }
                    } else {
                        _uiState.update { it.copy(procesandoAnulacion = false, mensajeError = detalle) }
                    }
                }
            )
        }
    }

    fun prorrogarVencimientoFactura(facturaId: String, nuevaFechaVencimiento: String) {
        // Candado síncrono: dos toques jamás lanzan dos escrituras de fecha.
        if (_uiState.value.procesandoProrroga) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        _uiState.update { it.copy(procesandoProrroga = true, mensajeError = null) }
        viewModelScope.launch {
            val res = facturaRepository.prorrogarVencimiento(facturaId, nuevaFechaVencimiento, farmaciaCapturada, sucursalCapturada)
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            procesandoProrroga = false,
                            mostrarDialogoProrroga = false,
                            facturaParaProrrogaId = null,
                            mensajeExito = " Fecha de vencimiento prorrogada a $nuevaFechaVencimiento."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoProrroga = false,
                            mensajeError = "Error al prorrogar vencimiento: ${e.message}. La fecha anterior sigue intacta."
                        )
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
        // Captura de sede al momento del toque (R1).
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
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
                idempotenciaId = idIntento,
                farmaciaIdParam = farmaciaCapturada,
                sucursalIdParam = sucursalCapturada
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
            // El diálogo no edita "contacto": al editar, se conserva el existente (jamás se pierde).
            contacto = if (contacto.trim().isBlank()) (editando?.contacto ?: "") else contacto.trim(),
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

    /** La pantalla ya cerró el detalle del proveedor enviado: se consume el evento. */
    fun consumirEnvioExitoso() {
        _uiState.update { it.copy(envioExitosoProveedor = null) }
    }

    /** Cada mensaje se consume SOLO después de mostrarse: jamás se pierde uno en silencio. */
    fun consumirMensajeExito() {
        _uiState.update { it.copy(mensajeExito = null) }
    }

    fun consumirMensajeError() {
        _uiState.update { it.copy(mensajeError = null) }
    }
}
