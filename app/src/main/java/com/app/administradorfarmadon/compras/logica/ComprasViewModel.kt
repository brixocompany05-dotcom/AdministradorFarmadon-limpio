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
    /** Toques aún sin confirmar por el servidor ((proveedor, producto) → suma de +1/-1 en vuelo).
     *  La pantalla SIEMPRE muestra servidor + pendiente: por más rápido que se marque,
     *  el número jamás salta hacia atrás ni se aloca, solo avanza con cada toque. */
    private val ajustePendiente = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, Int>()
    /** Toques acumulados aún sin enviar ((proveedor, producto) → delta). El escritor
     *  los junta: 10 toques rápidos salen en 1 sola petición, no en 10 que se
     *  estorban y repintan la pantalla a cada rato. */
    private val porEnviar = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, Int>()
    /** Un escritor por proveedor: envía por turnos, sin encimarse. */
    private val escritores = mutableMapOf<String, kotlinx.coroutines.Job>()
    /** Id de orden por proveedor: el reintento reutiliza el mismo documento (anti duplicación). */
    private val intentoEnvioIds = java.util.concurrent.ConcurrentHashMap<String, String>()
    /** Última foto viva confirmada por el servidor (para revertir sin pisar la verdad). */
    private var ultimoServidorCarrito: Map<String, Map<String, Int>> = emptyMap()
    /** La purga de huérfanos corre una vez por sede: si falla (sin red) reintenta
     *  con la próxima foto viva en vez de dejar restos guardados. */
    private var purgaFantasmasHecha: Boolean = false
    private var ultimoAbonoIntentoId: String = ""
    private var huellaUltimoAbono: String = ""
    // Candado + huella del egreso de saldo a favor (cobro/pérdida): dos toques rápidos
    // jamás registran dos egresos, y el reintento tras un fallo ambiguo reutiliza el
    // mismo id de idempotencia (el servidor rechaza el duplicado con la verdad real).
    private var procesandoSaldoAFavor: Boolean = false
    private var ultimoEgresoSaldoIntentoId: String = ""
    private var huellaUltimoEgresoSaldo: String = ""

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
        ajustePendiente.clear()
        porEnviar.clear()
        escritores.values.toList().forEach { it.cancel() }
        escritores.clear()
        intentoEnvioIds.clear()
        ultimoServidorCarrito = emptyMap()
        purgaFantasmasHecha = false
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
                    ultimoServidorCarrito = mapa.totales
                    intentarPurgarHuerfanos()
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
     * El número que la pantalla debe mostrar: verdad del servidor MÁS toques aún
     * sin confirmar. Cada escritura resta su propio toque al terminar, así por más
     * rápido que se marque (+/-/-/-), el número solo avanza con cada toque y jamás
     * salta hacia atrás con fotos viejas del servidor.
     */
    private fun construirCarritoMostrado(servidor: Map<String, Map<String, Int>>): Map<String, Map<String, Int>> {
        val mostrado = servidor.mapValues { (_, carro) -> carro.toMutableMap() }.toMutableMap()
        ajustePendiente.entries.toList().forEach { (clave, ajuste) ->
            if (ajuste == 0) {
                ajustePendiente.remove(clave)
            } else {
                val (proveedor, productoId) = clave
                val final = ((servidor[proveedor]?.get(productoId) ?: 0) + ajuste).coerceAtLeast(0)
                if (final <= 0) {
                    mostrado[proveedor]?.remove(productoId)
                } else {
                    mostrado.getOrPut(proveedor) { mutableMapOf() }[productoId] = final
                }
            }
        }
        mostrado.entries.removeIf { it.value.isEmpty() }
        return mostrado
    }

    /**
     * Limpieza de restos: borra del servidor los borradores de proveedores que ya
     * no existen y sus rastros en memoria local. Corre una vez por sede; si no hay
     * red, queda pendiente hasta la próxima foto viva. Jamás toca carritos vigentes
     * (el repositorio verifica contra el catálogo fresco antes de borrar).
     */
    private fun intentarPurgarHuerfanos() {
        if (purgaFantasmasHecha) return
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return
        viewModelScope.launch {
            pedidoCompraRepository.purgarCarritosHuerfanos(farmaciaId, sucursalId)
                .onSuccess { purgados ->
                    purgaFantasmasHecha = true
                    purgados.forEach { clave ->
                        ajustePendiente.keys.removeIf { it.first == clave }
                        intentoEnvioIds.remove(clave)
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Purga de huérfanos pendiente (reintenta sola): ${e.message}")
                }
        }
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
                    _uiState.update { s ->
                        val pedidoRecepcionandoId = s.pedidoParaRecepcionar?.id
                        val ordenActualizada = if (pedidoRecepcionandoId != null) pedidos.find { it.id == pedidoRecepcionandoId } else null
                        val conflictoConcurrente = ordenActualizada != null && !MaquinaEstadosPedido.puedeRecibir(ordenActualizada.estado)
                        val eliminadaPorOtro = pedidoRecepcionandoId != null && ordenActualizada == null

                        val cerrarDialogo = conflictoConcurrente || eliminadaPorOtro
                        val aviso = if (conflictoConcurrente) {
                            "La orden fue modificada a '${ordenActualizada!!.estado}' en otra terminal. El panel se cerró para proteger el inventario."
                        } else if (eliminadaPorOtro) {
                            "La orden fue eliminada en otra terminal. El panel se cerró."
                        } else null

                        s.copy(
                            pedidosGuardados = pedidos,
                            mostrarDialogoRecepcion = if (cerrarDialogo) false else s.mostrarDialogoRecepcion,
                            pedidoParaRecepcionar = if (cerrarDialogo) null else s.pedidoParaRecepcionar,
                            facturaRecepcionExistente = if (cerrarDialogo) null else s.facturaRecepcionExistente,
                            cargandoIndiceRecepcion = if (cerrarDialogo) false else s.cargandoIndiceRecepcion,
                            mensajeError = aviso ?: s.mensajeError
                        )
                    }
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
    private fun aplicarCambiosCarrito(proveedorNombre: String, cambios: Map<String, Int>) {
        val efectivos = cambios.filterValues { it != 0 }
        if (efectivos.isEmpty()) return
        // El toque se anota al instante (la pantalla responde ya) y se acumula para
        // el escritor: si llegan más toques antes de enviar, viajan juntos.
        efectivos.forEach { (productoId, delta) ->
            val clave = proveedorNombre to productoId
            ajustePendiente[clave] = (ajustePendiente[clave] ?: 0) + delta
            porEnviar[clave] = (porEnviar[clave] ?: 0) + delta
        }
        _uiState.update { current ->
            current.copy(pedidosPorProveedor = construirCarritoMostrado(ultimoServidorCarrito))
        }
        asegurarEscritor(proveedorNombre)
    }

    /**
     * Escritor por proveedor: manda lo acumulado de a un lote por vez. Si mientras
     * envía llegan más toques, el siguiente lote los lleva juntos. Un toque solo
     * sale al instante; diez toques rápidos salen en uno o dos viajes, jamás en diez
     * peticiones que se estorban y repintan la pantalla.
     */
    private fun asegurarEscritor(proveedorNombre: String) {
        if (escritores[proveedorNombre]?.isActive == true) return
        escritores[proveedorNombre] = viewModelScope.launch {
            try {
                while (true) {
                    // Sede al momento del envío (R1): jamás escribe en otra sede. Sin
                    // sede no se toma nada de la cola (los toques esperan al próximo).
                    val farmaciaId = SessionManager.clienteIdGarantizado
                    val sucursalId = SessionManager.sucursalIdEfectiva
                    if (farmaciaId.isBlank() || sucursalId.isBlank()) break
                    val lote = mutableMapOf<String, Int>()
                    porEnviar.entries.toList().forEach { (clave, delta) ->
                        if (clave.first == proveedorNombre && delta != 0) {
                            lote[clave.second] = delta
                            porEnviar.remove(clave)
                        }
                    }
                    if (lote.isEmpty()) break
                    val resultado = pedidoCompraRepository.guardarProductosCarrito(
                        proveedorNombre, lote, farmaciaId, sucursalId,
                        usuarioId = SessionManager.idCajera.ifBlank { "Sistema" },
                        usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Sistema" }
                    )
                    // Verdad fresca post-escritura: ya incluye este lote (y lo que el
                    // compañero haya sumado). Se registra ANTES de descontar, así el
                    // número jamás baja y vuelve mientras llega la foto viva.
                    try {
                        pedidoCompraRepository.leerCarritoProveedor(proveedorNombre, farmaciaId, sucursalId)
                            .onSuccess { frescos ->
                                val actual = ultimoServidorCarrito.toMutableMap()
                                if (frescos.isEmpty()) actual.remove(proveedorNombre)
                                else actual[proveedorNombre] = frescos
                                ultimoServidorCarrito = actual
                            }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Sin lectura fresca se sigue igual: la foto viva corrige al llegar.
                    }
                    // Cada lote resta lo suyo al terminar (éxito o fallo): la cuenta
                    // local jamás se descuadra con lo que el servidor ya tiene.
                    lote.forEach { (productoId, delta) ->
                        val clave = proveedorNombre to productoId
                        val queda = (ajustePendiente[clave] ?: 0) - delta
                        if (queda == 0) ajustePendiente.remove(clave) else ajustePendiente[clave] = queda
                    }
                    _uiState.update { current ->
                        current.copy(
                            pedidosPorProveedor = construirCarritoMostrado(ultimoServidorCarrito),
                            mensajeError = if (resultado.isFailure) {
                                val causa = resultado.exceptionOrNull()?.message ?: "falló la conexión"
                                "No se pudo actualizar el pedido de $proveedorNombre: $causa. Lo no guardado se quitó de la pantalla; revisa y vuelve a marcarlo."
                            } else current.mensajeError
                        )
                    }
                }
            } finally {
                escritores.remove(proveedorNombre)
            }
        }
    }

    fun reponerSugeridosDeProveedor(proveedorNombre: String) {
        val estado = _uiState.value
        val cambios = mutableMapOf<String, Int>()
        estado.todosLosProductos.forEach { prod ->
            if (estado.resolverProveedorProducto(prod) != proveedorNombre) return@forEach
            val sugerido = estado.calcularCantidadSugerida(prod)
            // Respetar decisión del usuario: solo llenar lo que aún está en 0
            // (la pantalla ya incluye lo recién tocado).
            val enPantalla = estado.pedidosPorProveedor[proveedorNombre]?.get(prod.id) ?: 0
            if (sugerido > 0 && enPantalla == 0) cambios[prod.id] = sugerido
        }
        aplicarCambiosCarrito(proveedorNombre, cambios)
    }

    private fun modificarCantidadPedirProveedor(proveedorNombre: String, productoId: String, delta: Int) {
        // El clic ES el incremento: se envía el +1/-1 tal cual. La transacción del
        // repositorio lo suma sobre la verdad vigente, de modo que dos usuarios que
        // tocan el mismo producto al mismo tiempo jamás pierden una unidad.
        if (delta == 0) return
        aplicarCambiosCarrito(proveedorNombre, mapOf(productoId to delta))
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

            // Sede cambiada a mitad del envío: nada se arma con datos mezclados. El
            // borrador (ya retirado) se devuelve a la sede donde nació y se avisa.
            if (SessionManager.clienteIdGarantizado != farmaciaCapturada ||
                SessionManager.sucursalIdEfectiva != sucursalCapturada
            ) {
                _uiState.update { it.copy(enviandoPedido = false) }
                val restaurado = if (borradorVigente.isNotEmpty()) {
                    pedidoCompraRepository.guardarProductosCarrito(
                        pedido.proveedorNombre,
                        borradorVigente,
                        farmaciaCapturada,
                        sucursalCapturada
                    ).isSuccess
                } else true
                _uiState.update {
                    it.copy(
                        mensajeError = if (restaurado) {
                            "Cambiaste de sede mientras se enviaba. El borrador volvió intacto a la sede anterior; entra a esa sede para enviarlo."
                        } else {
                            "Cambiaste de sede mientras se enviaba y no se pudo devolver el borrador. Vuelve a la sede anterior y rearma el pedido."
                        }
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

            // REGLA DE ORO: el agregado pertenece al mismo compromiso mientras la orden siga
            // viva (en camino: ENVIADO o ENTREGA_PARCIAL). Solo sin orden viva nace una nueva.
            val ordenEnCaminoExistente = _uiState.value.pedidosGuardados.firstOrNull {
                it.proveedorNombre.equals(pedido.proveedorNombre.trim(), ignoreCase = true) &&
                MaquinaEstadosPedido.perteneceAEnCamino(it.estado)
            }

            val resultado = if (ordenEnCaminoExistente != null) {
                pedidoCompraRepository.sumarProductosAOrdenEnviada(
                    pedidoId = ordenEnCaminoExistente.id,
                    nuevosItems = itemsVigentes,
                    usuarioNombre = SessionManager.nombreUsuario,
                    farmaciaIdParam = farmaciaCapturada,
                    sucursalIdParam = sucursalCapturada
                ).map { it.id }
            } else {
                val nuevoPedido = com.app.administradorfarmadon.compras.datos.PedidoCompra(
                    id = idEnvio,
                    proveedorId = pedido.proveedorId,
                    proveedorNombre = pedido.proveedorNombre,
                    proveedorRuc = pedido.proveedorRuc,
                    proveedorTelefono = pedido.proveedorTelefono,
                    items = itemsVigentes,
                    estado = "ENVIADO"
                )
                pedidoCompraRepository.guardarPedidoEnviado(
                    pedido = nuevoPedido,
                    idempotenciaId = idEnvio,
                    farmaciaIdParam = farmaciaCapturada,
                    sucursalIdParam = sucursalCapturada
                )
            }

            _uiState.update { it.copy(enviandoPedido = false) }
            resultado.fold(
                onSuccess = {
                    intentoEnvioIds.remove(pedido.proveedorNombre)
                    // El borrador ya fue retirado por el consumo atómico: aquí solo se
                    // limpia la pantalla. Lo que se marcó durante el envío sobrevive.
                    limpiarPedidoProveedor(pedido.proveedorNombre, borrarServidor = false)
                    val textoExito = if (ordenEnCaminoExistente != null) {
                        "✓ Se sumaron ${itemsVigentes.sumOf { it.cantidad }} unidades a la orden en camino ${ordenEnCaminoExistente.numeroOrden} de ${pedido.proveedorNombre}.$notaMultiusuario"
                    } else {
                        "✓ ¡Orden para ${pedido.proveedorNombre} guardada y marcada como ENVIADA!$notaMultiusuario"
                    }
                    _uiState.update {
                        it.copy(
                            envioExitosoProveedor = pedido.proveedorNombre,
                            mensajeExito = textoExito
                        )
                    }
                },
                onFailure = { e ->
                    // Si el doc ya existe con OTROS productos, la clave quedó quemada:
                    // se libera para que el próximo toque use clave nueva en vez de fallar en bucle.
                    if ((e.message ?: "").contains("ya fue guardada", ignoreCase = true)) {
                        intentoEnvioIds.remove(pedido.proveedorNombre)
                    }
                    // La restauración del borrador se ESPERA y se VERIFICA antes de decir nada:
                    // prohibido prometer "restaurado" sin comprobarlo.
                    val restaurado = pedidoCompraRepository.guardarProductosCarrito(
                        pedido.proveedorNombre,
                        borradorVigente,
                        farmaciaCapturada,
                        sucursalCapturada
                    ).isSuccess
                    val operacion = if (ordenEnCaminoExistente != null) "consolidar con la orden ${ordenEnCaminoExistente.numeroOrden}" else "registrar la orden"
                    _uiState.update {
                        it.copy(
                            mensajeError = if (restaurado) {
                                "No se pudo $operacion: ${e.message}. El borrador fue restaurado; intenta nuevamente."
                            } else {
                                "No se pudo $operacion: ${e.message}. Además falló la reconstrucción del borrador: vuelve a armar el pedido desde el catálogo."
                            }
                        )
                    }
                }
            )
        }
    }

    fun cancelarPedidoEnviado(pedidoId: String) {
        val orden = _uiState.value.pedidosGuardados.find { it.id == pedidoId }
        if (orden != null && !MaquinaEstadosPedido.puedeCancelar(orden.estado, orden.recepciones.size)) {
            _uiState.update { it.copy(mensajeError = "No se puede cancelar: la orden está en estado '${orden.estado}' o ya tiene entregas parciales.") }
            return
        }
        if (!intentarAccionOrden("cancelar:$pedidoId")) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            try {
                pedidoCompraRepository.cancelarPedido(pedidoId, farmaciaCapturada, sucursalCapturada, SessionManager.nombreUsuario).fold(
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
        if (!MaquinaEstadosPedido.puedeRecibir(pedido.estado)) {
            _uiState.update {
                it.copy(mensajeError = "La orden ${pedido.numeroOrden} está en estado '${pedido.estado}' y no permite recibir mercadería.")
            }
            return
        }
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
        fechaEmisionPapel: String = "",
        montoFactura: Double,
        montoPagado: Double,
        metodoPago: String = "",
        pagosRecepcion: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList(),
        saldoAFavorUsado: Double = 0.0,
        itemsRecepcion: List<com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean = false
    ) {
        // Candado síncrono: dos toques en CONFIRMAR jamás inician dos recepciones.
        if (_uiState.value.procesandoRecepcion) return

        // Captura de sede al momento del toque (R1): toda la recepción (stock,
        // kardex, factura, pedido) se asienta en la sede donde se abrió la orden.
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoRecepcion = true, mensajeError = null) }

            val selloEntrega = "$pedidoId|$numeroFactura|$fechaEmisionPapel|$montoFactura|$montoPagado|$saldoAFavorUsado|" +
                pagosRecepcion.joinToString(";") { pago ->
                    "${pago.metodoPago}:${pago.monto}:${pago.numeroOperacion}"
                } + "|" +
                itemsRecepcion.joinToString(";") { it ->
                    "${it.productoId}:${it.cantidadTotal}:${it.loteNumero.trim().uppercase()}:${it.vencimiento.trim()}:${it.costoUnitarioReal}"
                }
            val idIntento = java.util.UUID.nameUUIDFromBytes(selloEntrega.toByteArray()).toString()

            val usuarioEmail = SessionManager.email
            val res = pedidoCompraRepository.asentarRecepcionDirecta(
                pedidoId = pedidoId,
                numeroFactura = numeroFactura,
                condicionPago = condicionPago,
                fechaVencimientoPago = fechaVencimientoPago,
                fechaEmisionPapel = fechaEmisionPapel,
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
                    val textoExito = if (cerrarConAjuste) {
                        " ¡Mercadería recibida con ajuste! La orden fue cerrada y trasladada al Historial. Stock y Factura $numeroFactura actualizados."
                    } else {
                        " ¡Mercadería recibida y asentada con éxito! Stock y Factura $numeroFactura actualizados."
                    }
                    _uiState.update {
                        it.copy(
                            procesandoRecepcion = false,
                            mostrarDialogoRecepcion = false,
                            pedidoParaRecepcionar = null,
                            facturaRecepcionExistente = null,
                            cargandoIndiceRecepcion = false,
                            indiceLotesOrden = emptyMap(),
                            mensajeExito = textoExito
                        )
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: "Error desconocido al procesar la recepción"
                    val esConflictoConcurrente = msg.contains("CANCELADA", ignoreCase = true) ||
                            msg.contains("ya fue cerrada", ignoreCase = true) ||
                            msg.contains("ya fue RECIBIDA", ignoreCase = true) ||
                            msg.contains("no existe", ignoreCase = true) ||
                            msg.contains("no permite recibir", ignoreCase = true)

                    _uiState.update {
                        it.copy(
                            procesandoRecepcion = false,
                            mostrarDialogoRecepcion = if (esConflictoConcurrente) false else it.mostrarDialogoRecepcion,
                            pedidoParaRecepcionar = if (esConflictoConcurrente) null else it.pedidoParaRecepcionar,
                            facturaRecepcionExistente = if (esConflictoConcurrente) null else it.facturaRecepcionExistente,
                            cargandoIndiceRecepcion = if (esConflictoConcurrente) false else it.cargandoIndiceRecepcion,
                            indiceLotesOrden = if (esConflictoConcurrente) emptyMap() else it.indiceLotesOrden,
                            mensajeError = "No se pudo asentar la recepción: $msg"
                        )
                    }
                }
            )
        }
    }

    fun cerrarOrdenConAjuste(pedidoId: String, motivo: String = "Quiebre de stock en proveedor") {
        val orden = _uiState.value.pedidosGuardados.find { it.id == pedidoId }
        if (orden != null && !MaquinaEstadosPedido.puedeCerrarConAjuste(orden.estado, orden.recepciones.size)) {
            _uiState.update { it.copy(mensajeError = "No se puede cerrar con ajuste: la orden está en estado '${orden.estado}'.") }
            return
        }
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

    /**
     * Anexa el papel tardío a una orden que recibió S/C. Nace la deuda en Cuentas;
     * el stock no se mueve (ya entró). La pantalla se entera por las escuchas vivas.
     */
    fun anexarFacturaAOrden(
        pedidoId: String,
        numeroFactura: String,
        montoTotal: Double,
        condicionPago: String,
        fechaVencimientoPago: String = "",
        fechaEmisionPapel: String = ""
    ) {
        if (!intentarAccionOrden("anexar:$pedidoId")) return
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            _uiState.update { it.copy(anexandoFactura = true) }
            try {
                ingresoRepository.anexarFacturaAOrden(
                    pedidoId = pedidoId,
                    numeroFactura = numeroFactura,
                    montoTotal = montoTotal,
                    condicionPago = condicionPago,
                    fechaVencimientoPago = fechaVencimientoPago,
                    fechaEmisionPapel = fechaEmisionPapel,
                    usuarioEmail = SessionManager.email,
                    usuarioNombre = SessionManager.nombreUsuario,
                    farmaciaIdParam = farmaciaCapturada,
                    sucursalIdParam = sucursalCapturada
                ).fold(
                    onSuccess = {
                        _uiState.update { it.copy(mensajeExito = "Factura $numeroFactura anexada. Ya aparece en Cuentas por Pagar.") }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(mensajeError = "No se pudo anexar la factura: ${e.message}") }
                    }
                )
            } finally {
                _uiState.update { it.copy(anexandoFactura = false) }
                liberarAccionOrden("anexar:$pedidoId")
            }
        }
    }

    fun limpiarPedidoProveedor(proveedorNombre: String, borrarServidor: Boolean = true) {
        // Optimismo inmediato: el panel vacía al instante y la red lo confirma.
        // borrarServidor=false: solo limpia la pantalla. Se usa tras un envío
        // exitoso, porque el consumo atómico YA retiró el borrador; volver a borrar
        // aquí se llevaría lo que alguien marcó durante el envío. Los toques en
        // vuelo se descuentan solos al terminar (jamás se tocan aquí).
        intentoEnvioIds.remove(proveedorNombre)
        _uiState.update { current ->
            val mapa = current.pedidosPorProveedor.toMutableMap()
            mapa.remove(proveedorNombre)
            current.copy(pedidosPorProveedor = mapa)
        }
        if (!borrarServidor) return
        // Vaciar manda: se cancelan los envíos en curso de este proveedor y se
        // descartan sus toques en cola (lo cancelado jamás llegó al servidor).
        escritores.remove(proveedorNombre)?.cancel()
        ajustePendiente.keys.removeIf { it.first == proveedorNombre }
        porEnviar.keys.removeIf { it.first == proveedorNombre }
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva
        viewModelScope.launch {
            val resultado = pedidoCompraRepository.eliminarCarritoProveedor(proveedorNombre, farmaciaCapturada, sucursalCapturada)
            if (resultado.isFailure) {
                val causa = resultado.exceptionOrNull()?.message ?: "falló la conexión"
                _uiState.update {
                    it.copy(
                        pedidosPorProveedor = construirCarritoMostrado(ultimoServidorCarrito),
                        mensajeError = "No se pudo vaciar el pedido de $proveedorNombre: $causa. El pedido sigue como estaba; inténtalo de nuevo."
                    )
                }
            }
        }
    }

    /**
     * Edición de un pedido REALIZADO con auditoría: guarda los cambios y registra
     * en la bitácora qué cambió, quién y cuándo. Solo se edita mientras el pedido
     * sigue sin mercadería recibida; una vez recibido, su historial vive en RECIBIDOS.
     */
    fun seleccionarProveedor(proveedorId: String) {
        proveedorSeleccionadoIdGuardado = proveedorId
        _uiState.update { it.copy(proveedorSeleccionadoId = proveedorId) }
    }

    fun vincularProductoAProveedor(productoId: String, proveedor: Proveedor) {
        viewModelScope.launch {
            proveedorRepository.vincularProducto(productoId, proveedor.id, proveedor.nombre).fold(
                onSuccess = {
                    limpiarProductoDeOtrosCarritos(productoId, proveedor.nombre)
                    _uiState.update { it.copy(mensajeExito = "Producto afiliado a ${proveedor.nombre}.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "No se pudo afiliar el producto: ${e.message}") }
                }
            )
        }
    }

    fun desvincularProductoDeProveedor(productoId: String) {
        viewModelScope.launch {
            proveedorRepository.desvincularProducto(productoId).fold(
                onSuccess = {
                    limpiarProductoDeOtrosCarritos(productoId, null)
                    _uiState.update { it.copy(mensajeExito = "Producto desvinculado del proveedor.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(mensajeError = "No se pudo desvincular el producto: ${e.message}") }
                }
            )
        }
    }

    private fun limpiarProductoDeOtrosCarritos(productoId: String, proveedorExcluido: String?) {
        val carritos = _uiState.value.pedidosPorProveedor
        val farmaciaCapturada = SessionManager.clienteIdGarantizado
        val sucursalCapturada = SessionManager.sucursalIdEfectiva

        carritos.forEach { (provNombre, items) ->
            if (proveedorExcluido != null && provNombre.equals(proveedorExcluido, ignoreCase = true)) return@forEach
            if (items.containsKey(productoId) && (items[productoId] ?: 0) > 0) {
                viewModelScope.launch {
                    pedidoCompraRepository.quitarProductoDeCarrito(
                        proveedorNombre = provNombre,
                        productoId = productoId,
                        farmaciaIdParam = farmaciaCapturada,
                        sucursalIdParam = sucursalCapturada
                    )
                }
            }
        }
    }

    fun cobrarSaldoAFavor(proveedorId: String, monto: Double, documento: String, onComplete: (Result<Unit>) -> Unit) {
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update { it.copy(mensajeError = "Solo el dueño o administración puede registrar egresos o cobros de saldo a favor.") }
            onComplete(Result.failure(IllegalStateException("Sin autorización de dinero/administración.")))
            return
        }
        registrarEgresoSaldoAFavor(
            proveedorId = proveedorId,
            monto = monto,
            tipo = SaldoAFavorOperacionesRepository.TIPO_COBRADO,
            documento = documento,
            motivo = "Cobro en efectivo del saldo a favor",
            onComplete = onComplete
        )
    }

    fun declararSaldoPerdido(proveedorId: String, monto: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) {
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update { it.copy(mensajeError = "Solo el dueño o administración puede declarar un saldo como perdido.") }
            onComplete(Result.failure(IllegalStateException("Sin autorización de dinero/administración.")))
            return
        }
        registrarEgresoSaldoAFavor(
            proveedorId = proveedorId,
            monto = monto,
            tipo = SaldoAFavorOperacionesRepository.TIPO_PERDIDO,
            documento = "",
            motivo = motivo,
            onComplete = onComplete
        )
    }

    /**
     * Único camino de egreso de saldo a favor (cobro o pérdida). Candado síncrono
     * ANTES del launch (dos toques jamás inician dos transacciones) + huella de
     * idempotencia: si el resultado se pierde y la persona reintenta con los mismos
     * datos, el servidor reconoce el egreso ya registrado y no lo duplica.
     */
    private fun registrarEgresoSaldoAFavor(
        proveedorId: String,
        monto: Double,
        tipo: String,
        documento: String,
        motivo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        if (procesandoSaldoAFavor) return

        val huella = "$proveedorId|$tipo|$monto|$documento|$motivo"
        val idIntento = if (ultimoEgresoSaldoIntentoId.isNotBlank() && huellaUltimoEgresoSaldo == huella) {
            ultimoEgresoSaldoIntentoId
        } else {
            java.util.UUID.randomUUID().toString().also {
                ultimoEgresoSaldoIntentoId = it
                huellaUltimoEgresoSaldo = huella
            }
        }

        procesandoSaldoAFavor = true
        viewModelScope.launch {
            val res = SaldoAFavorOperacionesRepository().registrarEgresoSaldo(
                proveedorId = proveedorId,
                monto = monto,
                tipo = tipo,
                documento = documento,
                motivo = motivo,
                idempotenciaId = idIntento
            )
            procesandoSaldoAFavor = false
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
                    if (proveedorSeleccionadoIdGuardado == proveedor.id) {
                        proveedorSeleccionadoIdGuardado = null
                    }
                    _uiState.update {
                        it.copy(
                            proveedorSeleccionadoId = if (it.proveedorSeleccionadoId == proveedor.id) null else it.proveedorSeleccionadoId,
                            mensajeExito = "Proveedor eliminado correctamente."
                        )
                    }
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
        if (_uiState.value.procesandoPago) return
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update { it.copy(mensajeError = "Solo el dueño o administración puede registrar abonos y pagos de facturas.") }
            return
        }

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

    /** Regla de negocio de dinero (fail-closed): solo dueño/administración ejecuta movimientos de dinero. */
    val esUsuarioAutorizadoPlata: Boolean
        get() {
            val r = SessionManager.rol.trim().lowercase()
            if (r.isBlank()) return false
            return listOf("dueño", "dueno", "administrador", "admin").contains(r)
        }

    fun abrirDialogoAnularFactura(factura: FacturaCompra) {
        // Anular es acto terminal sobre dinero y stock: solo dueño/administración,
        // haya o no plata pagada. Un papel impago anulado por cualquiera también miente.
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update {
                it.copy(mensajeError = "Solo el dueño o administración puede anular facturas. Pídele ayuda a un usuario de administración.")
            }
            return
        }
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
        referenciaDevolucion: String? = null,
        conDevolucion: Boolean = true
    ) {
        val factura = _uiState.value.facturaParaAnular ?: return
        if (_uiState.value.procesandoAnulacion) return
        // Doble candado: aunque el diálogo se abriera por otro camino, sin dueño no pasa.
        if (!esUsuarioAutorizadoPlata) {
            _uiState.update { it.copy(mensajeError = "Solo el dueño o administración puede anular facturas.") }
            return
        }
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
                conDevolucion = conDevolucion,
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
                    val recordatorioCaja = if (respuestaPlata == "DEVOLUCION_RECIBIDA" &&
                        (metodoDevolucion ?: "").equals("Efectivo", ignoreCase = true)
                    ) {
                        " Registra el INGRESO en Caja para que el arqueo cuadre."
                    } else ""
                    _uiState.update {
                        it.copy(
                            procesandoAnulacion = false,
                            mostrarDialogoAnulacion = false,
                            facturaParaAnular = null,
                            lineasAnulacion = emptyList(),
                            mensajeExito = "Factura ${factura.numeroFactura.ifBlank { "sin número" }} anulada. El papel, el estante y la plata quedaron escritos.$recordatorioCaja"
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

    fun registrarNotaCredito(
        facturaId: String,
        numeroDocumento: String,
        monto: Double,
        motivo: String,
        salidaProductoId: String = "",
        salidaProductoNombre: String = "",
        salidaLote: String = "",
        salidaCantidad: Double = 0.0
    ) {
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
                sucursalIdParam = sucursalCapturada,
                salidaProductoId = salidaProductoId,
                salidaProductoNombre = salidaProductoNombre,
                salidaLote = salidaLote,
                salidaCantidad = salidaCantidad
            )
            res.fold(
                onSuccess = {
                    val montoStr = String.format(java.util.Locale.US, "%.2f", monto)
                    val extraStock = if (salidaCantidad > 0.0) " Salieron $salidaCantidad und. del lote $salidaLote." else ""
                    _uiState.update {
                        it.copy(
                            procesandoNotaCredito = false,
                            mostrarDialogoNotaCredito = false,
                            facturaParaNotaCreditoId = null,
                            mensajeExito = "Nota de crédito $numeroDocumento por ${SessionManager.monedaSimbolo.ifBlank { "S/" }} $montoStr registrada.$extraStock"
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
        _uiState.update {
            it.copy(
                mostrarDialogoProveedor = false,
                proveedorEditando = null,
                errorGuardadoProveedor = null
            )
        }
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
        val esEdicion = editando != null
        val proveedorAGuardar = Proveedor(
            id = editando?.id ?: "",
            nombre = nombreTrim,
            idFiscal = idFiscal.trim(),
            // Al editar, un campo en blanco conserva el existente: jamás se borra en silencio.
            contacto = if (esEdicion && contacto.trim().isBlank()) (editando?.contacto ?: "") else contacto.trim(),
            telefono = if (esEdicion && telefono.trim().isBlank()) (editando?.telefono ?: "") else telefono.trim(),
            email = if (esEdicion && email.trim().isBlank()) (editando?.email ?: "") else email.trim(),
            direccion = if (esEdicion && direccion.trim().isBlank()) (editando?.direccion ?: "") else direccion.trim(),
            montoMinimoPedido = montoLimpio
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    guardandoProveedor = true,
                    errorGuardadoProveedor = null,
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
                        // El error se ve DENTRO del diálogo, en el lugar del bloqueo (R3):
                        // el snackbar quedaría tapado por el diálogo y el fallo sería silencioso.
                        errorGuardadoProveedor = errorMsg,
                        mensajeError = null
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
