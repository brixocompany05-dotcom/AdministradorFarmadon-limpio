package com.app.administradorfarmadon.ventas.nuevaventa.logica

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.ApiDocumentosPeru
import com.app.administradorfarmadon.compartido.datos.ResultadoConsultaDoc
import com.app.administradorfarmadon.configuracion.metodospago.datos.MetodosPagoRepository
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.ResolvedProductPresentation
import com.app.administradorfarmadon.inventario.compartido.modelo.resolverPresentacionPorCodigo
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.datos.TicketComprobantePdf
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

import com.app.administradorfarmadon.clientes.datos.ClientesRepository
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia

/**
 * Estado UI completo para Nueva Venta / POS (R1/R3/R8/R10).
 */
data class NuevaVentaUiState(
    val carrito: List<ItemVenta> = emptyList(),
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val directorioClientes: List<ClienteFarmacia> = emptyList(),
    val descuento: Double = 0.0,
    val confirmoReceta: Boolean = false,
    // Búsqueda y Resultados
    val busquedaTexto: String = "",
    val buscando: Boolean = false,
    val resultadosBusqueda: List<MoldeProductos> = emptyList(),
    // Estado de Caja y Métodos
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val metodosPagoDisponibles: List<InstanciaPago> = emptyList(),
    val lineasPago: List<PagoVenta> = emptyList(),
    // Ventas Suspendidas
    val ventasSuspendidas: List<VentaSuspendida> = emptyList(),
    val mostrarSheetSuspendidas: Boolean = false,
    val mostrarDialogoSuspender: Boolean = false,
    // Diálogo y Flujo de Cobro
    val mostrarOverlayCobro: Boolean = false,
    val procesandoCobro: Boolean = false,
    val ventaExitosa: Venta? = null,
    val idempotenciaIdActual: String = "",
    // Consulta DNI/RUC
    val consultandoDoc: Boolean = false,
    val mostrarDialogoCliente: Boolean = false,
    // Notificaciones
    val error: String? = null,
    val mensajeExito: String? = null
) {
    val subtotal: Double
        get() = kotlin.math.round(carrito.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0

    val total: Double
        get() = kotlin.math.round((subtotal - descuento).coerceAtLeast(0.0) * 100.0) / 100.0

    val totalItems: Int
        get() = carrito.sumOf { it.cantidad }

    val requiereReceta: Boolean
        get() = carrito.any { it.requiereReceta }

    val cajaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA

    val sumaPagos: Double
        get() = kotlin.math.round(lineasPago.sumOf { it.monto } * 100.0) / 100.0

    val pagosNoEfectivo: Double
        get() = kotlin.math.round(lineasPago.filter { it.tipoId != "EFECTIVO" }.sumOf { it.monto } * 100.0) / 100.0

    val vuelto: Double
        get() = kotlin.math.round((sumaPagos - total).coerceAtLeast(0.0) * 100.0) / 100.0

    val montoFaltante: Double
        get() = kotlin.math.round((total - sumaPagos).coerceAtLeast(0.0) * 100.0) / 100.0

    /**
     * Valida que todo método que requiera N° de operación lo tenga lleno (CRÍTICO 2).
     */
    val operacionesCompletas: Boolean
        get() {
            val tiposRequierenOp = TIPOS_PAGO_FIJOS.filter { it.requiereOperacion }.map { it.id }.toSet()
            return lineasPago.all { p ->
                if (p.tipoId in tiposRequierenOp) p.numeroOperacion.isNotBlank() else true
            }
        }

    val puedeCobrar: Boolean
        get() = cajaAbierta &&
            carrito.isNotEmpty() &&
            (!requiereReceta || confirmoReceta) &&
            sumaPagos >= total - 0.009 &&
            pagosNoEfectivo <= total + 0.009 &&
            operacionesCompletas &&
            lineasPago.isNotEmpty()
}

/**
 * ViewModel principal del Punto de Venta (POS).
 */
class NuevaVentaViewModel(
    private val ventasRepository: VentasRepository = VentasRepository(),
    private val inventarioRepository: InventarioFirestoreRepository = InventarioFirestoreRepository(),
    private val cajaRepository: CajaRepository = CajaRepository(),
    private val metodosPagoRepository: MetodosPagoRepository = MetodosPagoRepository(),
    private val clientesRepository: ClientesRepository = ClientesRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "NuevaVentaViewModel"
    }

    private val _uiState = MutableStateFlow(NuevaVentaUiState())
    val uiState: StateFlow<NuevaVentaUiState> = _uiState.asStateFlow()

    private var jobBusqueda: Job? = null

    init {
        iniciarObservadores()
    }

    private fun iniciarObservadores() {
        val sucursalId = SessionManager.sucursalIdEfectiva

        // 1. Escuchar Estado de Caja
        viewModelScope.launch {
            cajaRepository.observarEstadoCaja()
                .catch { Log.e(TAG, "Error escuchando estado de caja: ${it.message}", it) }
                .collect { nuevoEstado ->
                    _uiState.update { it.copy(estadoCaja = nuevoEstado) }
                }
        }

        // 2. Escuchar Métodos de Pago Activos de la Sucursal (CRÍTICO 1)
        if (sucursalId.isNotBlank()) {
            viewModelScope.launch {
                metodosPagoRepository.observarMetodosPago(sucursalId)
                    .catch { Log.e(TAG, "Error escuchando métodos de pago: ${it.message}", it) }
                    .collect { lista ->
                        val activas = lista.filter { m -> m.activa }
                        _uiState.update { it.copy(metodosPagoDisponibles = activas) }
                    }
            }
        }

        // 3. Escuchar Ventas Suspendidas
        viewModelScope.launch {
            ventasRepository.observarSuspendidas()
                .catch { Log.e(TAG, "Error escuchando suspendidas: ${it.message}", it) }
                .collect { suspList ->
                    _uiState.update { it.copy(ventasSuspendidas = suspList) }
                }
        }

        // 4. Escuchar Directorio de Clientes de la Farmacia (FASE 6)
        viewModelScope.launch {
            clientesRepository.observarClientes()
                .catch { Log.e(TAG, "Error escuchando directorio de clientes: ${it.message}", it) }
                .collect { clientes ->
                    _uiState.update { it.copy(directorioClientes = clientes) }
                }
        }
    }

    // ───────────────────────────── BÚSQUEDA DE PRODUCTOS (Debounce 300ms) ─────────────────────────────

    fun onBusquedaChange(texto: String) {
        _uiState.update { it.copy(busquedaTexto = texto) }
        jobBusqueda?.cancel()

        if (texto.trim().isBlank()) {
            _uiState.update { it.copy(buscando = false, resultadosBusqueda = emptyList()) }
            return
        }

        jobBusqueda = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(buscando = true) }
            val farmaciaId = SessionManager.clienteIdGarantizado
            val sucursalId = SessionManager.sucursalIdEfectiva
            val pagina = inventarioRepository.buscarInventarioPaginado(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                texto = texto,
                limit = 20
            )

            // CRÍTICO 4 & 5: Auto-agregar SOLO si es coincidencia exacta de CÓDIGO (código de barras, código base, secundario o fracción -B/-U).
            // NUNCA auto-agregar por coincidencia de texto de nombre.
            if (pagina.productosMolde.size == 1 && esCoincidenciaExactaCodigo(pagina.productosMolde.first(), texto)) {
                val unicoProd = pagina.productosMolde.first()
                val presResuelta = unicoProd.resolverPresentacionPorCodigo(texto)
                agregarItemResuelto(unicoProd, presResuelta)
                _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList()) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    buscando = false,
                    resultadosBusqueda = pagina.productosMolde
                )
            }
        }
    }

    /**
     * Verifica si el texto de búsqueda es un código exacto del producto (barcode, código base, alias o fracción).
     */
    private fun esCoincidenciaExactaCodigo(prod: MoldeProductos, texto: String): Boolean {
        val codLimpio = texto.trim().replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (codLimpio.isBlank()) return false

        val codProdLimpio = prod.codigo.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (codProdLimpio == codLimpio) return true

        if (prod.codigosSecundarios.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio }) return true

        if (prod.presentaciones.any { pres ->
            (pres.codigoBarras.isNotBlank() && pres.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio) ||
                pres.codigosAnteriores.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio }
        }) return true

        val matchFraccion = Regex("-(B|U)\\d+$", RegexOption.IGNORE_CASE).find(codLimpio)
        if (matchFraccion != null) {
            val baseCodigo = codLimpio.substring(0, matchFraccion.range.first)
            if (baseCodigo == codProdLimpio || prod.codigosSecundarios.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == baseCodigo }) {
                return true
            }
        }
        return false
    }

    fun limpiarBusqueda() {
        jobBusqueda?.cancel()
        _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList()) }
    }

    // ───────────────────────────── CARRITO DE VENTAS ─────────────────────────────

    fun agregarAlCarrito(producto: MoldeProductos, presentacion: PresentacionProducto, cantidadInicial: Int = 1) {
        // CRÍTICO 6: Bloquear productos con precio <= 0.00
        if (presentacion.precioventa <= 0.0) {
            _uiState.update { it.copy(error = "La presentación '${presentacion.nombre}' tiene precio S/ 0.00 y no puede ser agregada.") }
            return
        }

        val carritoActual = _uiState.value.carrito.toMutableList()
        val indexExistente = carritoActual.indexOfFirst {
            it.productoId == producto.indice && it.presentacionId == presentacion.presentacionId
        }

        if (indexExistente >= 0) {
            val itemExistente = carritoActual[indexExistente]
            val nuevaCant = itemExistente.cantidad + cantidadInicial
            carritoActual[indexExistente] = itemExistente.copy(
                cantidad = nuevaCant,
                subtotal = kotlin.math.round(itemExistente.precioUnitario * nuevaCant * 100.0) / 100.0
            )
        } else {
            val nuevoItem = ItemVenta(
                productoId = producto.indice,
                nombreProducto = producto.nombre,
                empaque = producto.empaque,
                presentacionId = presentacion.presentacionId,
                presentacionNombre = presentacion.nombre,
                cantidad = cantidadInicial,
                precioUnitario = presentacion.precioventa,
                subtotal = kotlin.math.round(presentacion.precioventa * cantidadInicial * 100.0) / 100.0,
                requiereReceta = producto.requiereReceta,
                cantidadDevuelta = 0
            )
            carritoActual.add(nuevoItem)
        }

        _uiState.update { it.copy(carrito = carritoActual, error = null) }
    }

    private fun agregarItemResuelto(producto: MoldeProductos, res: ResolvedProductPresentation, cantidadInicial: Int = 1) {
        // CRÍTICO 6: Bloquear productos resueltos con precio <= 0.00
        if (res.precioVenta <= 0.0) {
            _uiState.update { it.copy(error = "La presentación '${res.nombrePresentacion}' tiene precio S/ 0.00 y no puede ser agregada.") }
            return
        }

        val carritoActual = _uiState.value.carrito.toMutableList()
        val indexExistente = carritoActual.indexOfFirst {
            it.productoId == producto.indice && it.presentacionId == res.presentacionId
        }

        if (indexExistente >= 0) {
            val itemExistente = carritoActual[indexExistente]
            val nuevaCant = itemExistente.cantidad + cantidadInicial
            carritoActual[indexExistente] = itemExistente.copy(
                cantidad = nuevaCant,
                subtotal = kotlin.math.round(itemExistente.precioUnitario * nuevaCant * 100.0) / 100.0
            )
        } else {
            val nuevoItem = ItemVenta(
                productoId = producto.indice,
                nombreProducto = producto.nombre,
                empaque = producto.empaque,
                presentacionId = res.presentacionId,
                presentacionNombre = res.nombrePresentacion,
                cantidad = cantidadInicial,
                precioUnitario = res.precioVenta,
                subtotal = kotlin.math.round(res.precioVenta * cantidadInicial * 100.0) / 100.0,
                requiereReceta = producto.requiereReceta,
                cantidadDevuelta = 0
            )
            carritoActual.add(nuevoItem)
        }

        _uiState.update { it.copy(carrito = carritoActual, error = null) }
    }

    fun cambiarCantidadItem(productoId: String, presentacionId: String, delta: Int) {
        val carritoActual = _uiState.value.carrito.toMutableList()
        val index = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionId }
        if (index >= 0) {
            val item = carritoActual[index]
            val nuevaCant = item.cantidad + delta
            if (nuevaCant <= 0) {
                carritoActual.removeAt(index)
            } else {
                carritoActual[index] = item.copy(
                    cantidad = nuevaCant,
                    subtotal = kotlin.math.round(item.precioUnitario * nuevaCant * 100.0) / 100.0
                )
            }
            _uiState.update { it.copy(carrito = carritoActual) }
        }
    }

    fun eliminarItemCarrito(productoId: String, presentacionId: String) {
        val carritoActual = _uiState.value.carrito.filterNot {
            it.productoId == productoId && it.presentacionId == presentacionId
        }
        _uiState.update { it.copy(carrito = carritoActual) }
    }

    fun vaciarCarrito() {
        _uiState.update {
            it.copy(
                carrito = emptyList(),
                descuento = 0.0,
                confirmoReceta = false,
                lineasPago = emptyList(),
                error = null
            )
        }
    }

    fun setDescuento(monto: Double) {
        val d = kotlin.math.round(monto.coerceAtLeast(0.0) * 100.0) / 100.0
        _uiState.update { it.copy(descuento = d) }
    }

    fun setConfirmoReceta(confirmo: Boolean) {
        _uiState.update { it.copy(confirmoReceta = confirmo) }
    }

    // ───────────────────────────── CLIENTE Y CONSULTAS ─────────────────────────────

    fun abrirDialogoCliente() {
        _uiState.update { it.copy(mostrarDialogoCliente = true, error = null) }
    }

    fun cerrarDialogoCliente() {
        _uiState.update { it.copy(mostrarDialogoCliente = false) }
    }

    fun establecerCliente(cliente: ClienteDeVenta) {
        val idEnlazado = if (cliente.tipoDocumento != "NINGUNO" && cliente.numeroDocumento.isNotBlank()) {
            _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento == cliente.numeroDocumento.trim() }?.id
                ?: cliente.numeroDocumento.trim()
        } else ""

        _uiState.update { it.copy(cliente = cliente.copy(clienteId = idEnlazado), mostrarDialogoCliente = false) }
    }

    fun guardarClienteYEstablecer(cliente: ClienteDeVenta, guardarEnDirectorio: Boolean) {
        viewModelScope.launch {
            if (guardarEnDirectorio && cliente.tipoDocumento != "NINGUNO" && cliente.numeroDocumento.isNotBlank()) {
                val ficha = ClienteFarmacia(
                    id = cliente.numeroDocumento.trim(),
                    tipoDocumento = cliente.tipoDocumento,
                    numeroDocumento = cliente.numeroDocumento.trim(),
                    nombre = cliente.nombre.trim(),
                    creadoPor = SessionManager.nombreUsuario
                )
                val res = clientesRepository.guardarCliente(ficha)
                res.onFailure { err ->
                    _uiState.update { it.copy(error = "Venta enlazada, pero la ficha no se guardó en el directorio: ${err.message}") }
                }
            }
            val idEnlazado = if (cliente.tipoDocumento != "NINGUNO" && cliente.numeroDocumento.isNotBlank()) {
                _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento == cliente.numeroDocumento.trim() }?.id
                    ?: cliente.numeroDocumento.trim()
            } else ""

            _uiState.update { it.copy(cliente = cliente.copy(clienteId = idEnlazado), mostrarDialogoCliente = false) }
        }
    }

    fun consultarDocumentoCliente(tipo: String, numero: String) {
        val numLimpio = numero.trim()
        if (numLimpio.isBlank()) return

        // 1. Verificar si ya existe en el directorio local de la farmacia
        val existente = _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento == numLimpio }
        if (existente != null) {
            val clienteEncontrado = ClienteDeVenta(
                tipoDocumento = existente.tipoDocumento,
                numeroDocumento = existente.numeroDocumento,
                nombre = existente.nombre,
                clienteId = existente.id
            )
            _uiState.update {
                it.copy(
                    cliente = clienteEncontrado,
                    consultandoDoc = false,
                    mostrarDialogoCliente = false,
                    mensajeExito = "Cliente del directorio: ${existente.nombre}"
                )
            }
            return
        }

        // 2. Si no existe en el directorio, consultar API oficial (RENIEC / SUNAT)
        viewModelScope.launch {
            _uiState.update { it.copy(consultandoDoc = true, error = null) }
            when (val res = ApiDocumentosPeru.consultar(tipo, numLimpio)) {
                is ResultadoConsultaDoc.Encontrado -> {
                    val nuevoCliente = ClienteDeVenta(
                        tipoDocumento = res.tipo,
                        numeroDocumento = res.numero,
                        nombre = res.nombreCompleto,
                        clienteId = res.numero
                    )
                    _uiState.update {
                        it.copy(
                            cliente = nuevoCliente,
                            consultandoDoc = false,
                            mostrarDialogoCliente = false,
                            mensajeExito = "Cliente identificado: ${res.nombreCompleto}"
                        )
                    }
                }
                is ResultadoConsultaDoc.NoEncontrado -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = "No se encontraron datos oficiales para el $tipo $numLimpio."
                        )
                    }
                }
                is ResultadoConsultaDoc.SinToken -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = res.mensaje
                        )
                    }
                }
                is ResultadoConsultaDoc.Error -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = res.mensaje
                        )
                    }
                }
            }
        }
    }

    // ───────────────────────────── FLUJO DE COBRO E IDEMPOTENCIA ─────────────────────────────

    fun abrirOverlayCobro() {
        val total = _uiState.value.total
        val idIdem = _uiState.value.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }

        // CRÍTICO 1: Si hay EFECTIVO activo en metodosPagoDisponibles, precargarlo por defecto; si no, dejar vacío
        val metodoEfectivoActivo = _uiState.value.metodosPagoDisponibles.firstOrNull { it.tipoId == "EFECTIVO" }
        val lineaDefecto = if (metodoEfectivoActivo != null) {
            listOf(
                PagoVenta(
                    tipoId = "EFECTIVO",
                    instanciaId = metodoEfectivoActivo.id,
                    nombreMetodo = "Efectivo",
                    monto = total,
                    numeroOperacion = ""
                )
            )
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                mostrarOverlayCobro = true,
                idempotenciaIdActual = idIdem,
                lineasPago = lineaDefecto,
                error = null
            )
        }
    }

    fun cerrarOverlayCobro() {
        _uiState.update { it.copy(mostrarOverlayCobro = false) }
    }

    fun actualizarLineasPago(nuevasLineas: List<PagoVenta>) {
        _uiState.update { it.copy(lineasPago = nuevasLineas) }
    }

    fun confirmarVenta() {
        val estado = _uiState.value
        if (estado.procesandoCobro) return
        if (!estado.cajaAbierta) {
            _uiState.update { it.copy(error = "La caja está cerrada. Ábrela en 'Cierre de Caja' para cobrar.") }
            return
        }
        if (estado.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito no tiene productos.") }
            return
        }
        if (estado.requiereReceta && !estado.confirmoReceta) {
            _uiState.update { it.copy(error = "Debes confirmar la receta médica para los medicamentos que la requieren.") }
            return
        }
        if (!estado.operacionesCompletas) {
            _uiState.update { it.copy(error = "Debe ingresar el N° de operación para los pagos que lo requieren (Yape, Plin, Transferencia o Cheque).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoCobro = true, error = null) }

            val res = ventasRepository.registrarVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                pagos = estado.lineasPago,
                descuento = estado.descuento,
                confirmoReceta = estado.confirmoReceta,
                idempotenciaId = estado.idempotenciaIdActual
            )

            res.onSuccess { ventaCompletada ->
                _uiState.update {
                    it.copy(
                        procesandoCobro = false,
                        mostrarOverlayCobro = false,
                        ventaExitosa = ventaCompletada,
                        carrito = emptyList(),
                        descuento = 0.0,
                        confirmoReceta = false,
                        lineasPago = emptyList(),
                        idempotenciaIdActual = "", // Se resetea solo tras el éxito
                        cliente = ClienteDeVenta(),
                        mensajeExito = "¡Venta ${ventaCompletada.numeroCompleto} registrada exitosamente!"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoCobro = false,
                        error = err.message ?: "No se pudo registrar la venta."
                    )
                }
            }
        }
    }

    fun finalizarVentaExitosa() {
        _uiState.update { it.copy(ventaExitosa = null, error = null, mensajeExito = null) }
    }

    // MENOR M2 & M3: Sin datos suplentes y manejo de error de impresión honesto
    fun imprimirComprobante(context: Context) {
        val venta = _uiState.value.ventaExitosa ?: return
        viewModelScope.launch {
            val emisorRes = ventasRepository.obtenerEmisor()
            val emisor = emisorRes.getOrNull()
            if (emisor == null) {
                _uiState.update { it.copy(error = "No se pudieron obtener los datos de la farmacia para imprimir el comprobante: ${emisorRes.exceptionOrNull()?.message}") }
                return@launch
            }

            val resImpresion = TicketComprobantePdf.imprimirTicket(context, venta, emisor)
            resImpresion.onFailure { err ->
                _uiState.update { it.copy(error = "No se pudo imprimir el comprobante: ${err.message}") }
            }
        }
    }

    // ───────────────────────────── SUSPENDER / REANUDAR VENTAS ─────────────────────────────

    fun abrirDialogoSuspender() {
        if (_uiState.value.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito está vacío, no hay nada que suspender.") }
            return
        }
        _uiState.update { it.copy(mostrarDialogoSuspender = true, error = null) }
    }

    fun cerrarDialogoSuspender() {
        _uiState.update { it.copy(mostrarDialogoSuspender = false) }
    }

    fun suspenderVenta(nota: String) {
        val estado = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(mostrarDialogoSuspender = false, error = null) }
            val res = ventasRepository.suspenderVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                nota = nota
            )
            res.onSuccess {
                vaciarCarrito()
                _uiState.update {
                    it.copy(mensajeExito = "Venta suspendida correctamente.")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "No se pudo suspender la venta.") }
            }
        }
    }

    fun abrirSheetSuspendidas() {
        _uiState.update { it.copy(mostrarSheetSuspendidas = true, error = null) }
    }

    fun cerrarSheetSuspendidas() {
        _uiState.update { it.copy(mostrarSheetSuspendidas = false) }
    }

    // MENOR M1: Solo cargar al carrito si la eliminación en Firestore tuvo éxito
    fun reanudarVentaSuspendida(v: VentaSuspendida) {
        viewModelScope.launch {
            val res = ventasRepository.eliminarSuspendida(v.id)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        carrito = v.items,
                        cliente = v.cliente,
                        mostrarSheetSuspendidas = false,
                        mensajeExito = "Venta reanudada en el carrito."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(error = "No se pudo reanudar la venta suspendida: ${err.message}")
                }
            }
        }
    }

    fun descartarVentaSuspendida(id: String) {
        viewModelScope.launch {
            val res = ventasRepository.eliminarSuspendida(id)
            res.onFailure { err ->
                _uiState.update { it.copy(error = "No se pudo eliminar la venta suspendida: ${err.message}") }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
