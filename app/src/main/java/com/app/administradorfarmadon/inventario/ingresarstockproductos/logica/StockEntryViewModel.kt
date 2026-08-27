package com.app.administradorfarmadon.inventario.ingresarstockproductos.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.inventario.compartido.datos.FacturaCompraRepository
import com.app.administradorfarmadon.inventario.compartido.datos.ProveedorRepository
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.inventario.compartido.modelo.precioVenta
import com.app.administradorfarmadon.inventario.ingresarstockproductos.datos.StockEntryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel Enterprise para la recepciÃ³n de stock, bonificaciones y facturaciÃ³n.
 * CÃ³digo limpio, sin parches y con responsabilidades claras (1+1=2).
 */
class StockEntryViewModel(
    private val repository: StockEntryRepository = StockEntryRepository(),
    private val proveedorRepository: ProveedorRepository = ProveedorRepository(),
    private val facturaCompraRepository: FacturaCompraRepository = FacturaCompraRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StockEntryState())
    val state: StateFlow<StockEntryState> = _state.asStateFlow()

    private var initializedProductId: String? = null
    private var searchFacturaJob: Job? = null

    init {
        viewModelScope.launch {
            proveedorRepository.observarProveedores().collect { lista ->
                _state.update { it.copy(proveedoresDisponibles = lista) }
            }
        }
        // Sugerencia en vivo de la Ãºltima factura de la farmacia (R8). Se ofrece, jamÃ¡s se impone.
        viewModelScope.launch {
            facturaCompraRepository.observarFacturasRecientes().collect { lista ->
                val ultima = lista.firstOrNull()
                _state.update {
                    it.copy(
                        ultimaFacturaSugerida = ultima,
                        mostrarSugerenciaUltimaFactura = ultima != null &&
                                _state.value.numeroFactura.isBlank() &&
                                _state.value.facturaDetectadaInfo == null
                    )
                }
            }
        }
    }

    /** Aplica la factura sugerida — la marca como factura existente para bloquear tope y proveedor. */
    fun onUsarSugerenciaUltimaFactura() {
        val f = _state.value.ultimaFacturaSugerida ?: return
        _state.update {
            it.copy(
                numeroFactura = f.numeroFactura,
                montoTotalFactura = if (f.montoTotal > 0) String.format(java.util.Locale.US, "%.2f", f.montoTotal) else "",
                proveedorNombre = f.proveedorNombre,
                proveedorIdSeleccionado = f.proveedorId,
                rucProveedorSeleccionado = f.rucProveedor,
                facturaDetectadaInfo = f,
                mostrarSugerenciaUltimaFactura = false,
                mensajeError = null
            )
        }
    }

    fun inicializar(productId: String) {
        if (initializedProductId == productId && _state.value.productoId.isNotBlank()) return
        initializedProductId = productId

        _state.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            val producto = repository.obtenerProducto(productId)
            if (producto != null) {
                val stockActual = producto.stockDisponibleFisico

                val contenido = producto.contenido.toIntOrNull()?.coerceAtLeast(1) ?: 1

                _state.update {
                    it.copy(
                        productoId = producto.indice,
                        productoNombre = producto.nombre,
                        categoria = producto.categoriaPrincipal,
                        laboratorio = producto.proveedorBaseNombre,
                        empaque = producto.empaque,
                        codigoBarras = producto.codigo,
                        stockActual = stockActual,
                        lotesExistentes = producto.lotes,
                        contenidoPorUnidad = contenido,
                        unidadContenido = producto.contenidoUnidad,
                        unidadesPorBulto = "",
                        mesVencimiento = 0,
                        anoVencimiento = 0,
                        modoIngreso = ModoIngresoStock.UNIDADES_DIRECTAS,
                        precioVentaActual = producto.precioVenta,
                        ultimoPrecioCompraRegistrado = producto.precioCompra,
                        stockMinimoActual = producto.stockMinimoBase,
                        esRefrigerado = producto.temperaturaAlmacenamiento == "REFRIGERACION",
                        principioActivo = producto.principioActivo,
                        clasificacionControl = producto.clasificacionControl,
                        requiereReceta = producto.requiereReceta,
                        estaCargando = false
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        estaCargando = false,
                        mensajeError = "No se pudo cargar la informaciÃ³n del producto."
                    )
                }
            }
        }
    }

    fun inicializarConLote(productId: String, lote: LoteProducto) {
        inicializarConLoteNumero(productId, lote.numero)
    }

    fun inicializarConLoteNumero(productId: String, loteNumero: String) {
        initializedProductId = productId
        _state.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            val producto = repository.obtenerProducto(productId)
            if (producto != null) {
                val stockActual = producto.stockDisponibleFisico
                val cleanKey = FechaVencimientoHelper.llaveLote(loteNumero)
                val lote = producto.lotes[cleanKey] 
                    ?: producto.lotes[FechaVencimientoHelper.llaveLoteLegada(loteNumero)]
                    ?: producto.lotes[loteNumero.trim().uppercase()]
                    ?: producto.lotes.values.firstOrNull { it.numero.trim().equals(loteNumero.trim(), ignoreCase = true) }

                val venc = lote?.vencimiento ?: ""
                val partes = venc.split("/")
                // R3: nunca inventar vencimiento. Si el lote guardado no trae fecha,
                // se deja en blanco (mes=0, aÃ±o=0) y el usuario debe escribirla.
                val mes = if (partes.size == 2) partes[0] else ""
                val ano = if (partes.size == 2) partes[1] else ""

                // Cuando se preselecciona lote, se bloquea y se trae vencimiento/proveedor oficial
                val contenido = producto.contenido.toIntOrNull()?.coerceAtLeast(1) ?: 1

                _state.update {
                    it.copy(
                        productoId = producto.indice,
                        productoNombre = producto.nombre,
                        categoria = producto.categoriaPrincipal,
                        laboratorio = producto.proveedorBaseNombre,
                        empaque = producto.empaque,
                        codigoBarras = producto.codigo,
                        stockActual = stockActual,
                        lotesExistentes = producto.lotes,
                        contenidoPorUnidad = contenido,
                        unidadContenido = producto.contenidoUnidad,
                        unidadesPorBulto = "",
                        modoIngreso = ModoIngresoStock.UNIDADES_DIRECTAS,
                        precioVentaActual = producto.precioVenta,
                        ultimoPrecioCompraRegistrado = producto.precioCompra,
                        stockMinimoActual = producto.stockMinimoBase,
                        principioActivo = producto.principioActivo,
                        clasificacionControl = producto.clasificacionControl,
                        requiereReceta = producto.requiereReceta,
                        numeroLote = loteNumero.uppercase(),
                        mesVencimiento = mes.toIntOrNull() ?: 0,
                        anoVencimiento = ano.toIntOrNull() ?: 0,
                        proveedorNombre = lote?.proveedorNombre ?: "",
                        numeroFactura = "",
                        camposBloqueadosPorLoteExistente = true,
                        estaCargando = false
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        estaCargando = false,
                        mensajeError = "No se pudo cargar la informaciÃ³n del producto."
                    )
                }
            }
        }
    }

    // â”€â”€ 1. GESTIÃ“N DEL LOTE Y VENCIMIENTO â”€â”€
    fun onModoIngresoChanged(modo: ModoIngresoStock) {
        _state.update { it.copy(modoIngreso = modo, mensajeError = null) }
    }

    fun onLoteChanged(lote: String) {
        val clean = lote.trim().uppercase()
        // No auto-rellena â€” usuario debe tocar Agregar a este lote para traer datos existentes
        _state.update { it.copy(numeroLote = clean, mensajeError = null) }
    }

    fun onCodigoGs1Escaneado(codigo: String) {
        val parsed = GS1DataMatrixParser.parse(codigo)
        if (parsed.lote != null) {
            onLoteChanged(parsed.lote)
        }
        if (parsed.mesVencimiento != null && parsed.anoVencimiento != null) {
            if (_state.value.camposBloqueadosPorLoteExistente) return
            // R3: si el DataMatrix trae una fecha ya vencida, avisar EN VIVO (no esperar a guardar)
            val fechaEscaneada = String.format("%02d/%d", parsed.mesVencimiento, parsed.anoVencimiento)
            val dias = FechaVencimientoHelper.diasHastaVencer(fechaEscaneada)
            if (dias != null && dias <= 0) {
                _state.update {
                    it.copy(
                        mesVencimiento = parsed.mesVencimiento,
                        anoVencimiento = parsed.anoVencimiento,
                        mensajeError = "El cÃ³digo escaneado trae vencimiento $fechaEscaneada (ya vencido). CorrÃ­gelo antes de guardar."
                    )
                }
            } else {
                _state.update {
                    it.copy(mesVencimiento = parsed.mesVencimiento, anoVencimiento = parsed.anoVencimiento)
                }
            }
        }
    }

    fun onAplicarDatosLoteExistente(lote: LoteProducto) {
        val partes = lote.vencimiento.split("/")
        val mes = partes.getOrNull(0)?.toIntOrNull() ?: _state.value.mesVencimiento
        val ano = partes.getOrNull(1)?.toIntOrNull() ?: _state.value.anoVencimiento
        _state.update {
            it.copy(
                numeroLote = lote.numero,
                mesVencimiento = mes,
                anoVencimiento = ano,
                proveedorNombre = if (lote.proveedorNombre.isNotBlank()) lote.proveedorNombre else it.proveedorNombre,
                camposBloqueadosPorLoteExistente = true,
                mensajeError = null
            )
        }
    }

    fun onMesVencimientoChanged(mes: Int) {
        _state.update { it.copy(mesVencimiento = mes, mensajeError = null) }
    }

    fun onAnoVencimientoChanged(ano: Int) {
        _state.update { it.copy(anoVencimiento = ano, mensajeError = null) }
    }

    fun onDesbloquearCamposLote() {
        _state.update { it.copy(camposBloqueadosPorLoteExistente = false) }
    }

    // â”€â”€ 2. GESTIÃ“N DE CANTIDADES Y BONIFICACIÃ“N â”€â”€
    fun onCantidadBultosChanged(bultos: String) {
        val clean = bultos.filter { it.isDigit() }
        _state.update { it.copy(cantidadBultos = clean, mensajeError = null) }
    }

    fun onUnidadesPorBultoChanged(unidades: String) {
        val clean = unidades.filter { it.isDigit() }
        _state.update { it.copy(unidadesPorBulto = clean, mensajeError = null) }
    }

    fun onCantidadDirectaChanged(cantidad: String) {
        val clean = cantidad.filter { it.isDigit() }
        _state.update { it.copy(cantidadDirecta = clean, mensajeError = null) }
    }

    fun onToggleBonificacion(activar: Boolean) {
        _state.update {
            it.copy(
                activarBonificacion = activar,
                cantidadBonificacion = if (activar) it.cantidadBonificacion else "",
                mensajeError = null
            )
        }
    }

    fun onCantidadBonificacionChanged(cant: String) {
        val clean = cant.filter { it.isDigit() }
        _state.update { it.copy(cantidadBonificacion = clean, mensajeError = null) }
    }

    fun onCostoCompraLoteChanged(costo: String) {
        val clean = costo.replace(',', '.').filter { it.isDigit() || it == '.' }
        val parts = clean.split('.')
        val finalClean = if (parts.size > 2) "${parts[0]}.${parts.drop(1).joinToString("")}" else clean
        _state.update { it.copy(costoCompraLote = finalClean, mensajeError = null) }
    }

    // â”€â”€ AVISO DE REPOSICIÃ“N: propuesta visible + ediciÃ³n discreta (sin campos obligatorios) â”€â”€
    fun onEditarStockMinimoPropuesto() {
        _state.update {
            it.copy(
                stockMinimoEditando = true,
                stockMinimoPersonalizado = it.stockMinimoAplicar.toString()
            )
        }
    }

    fun onStockMinimoPersonalizadoChanged(valor: String) {
        _state.update { it.copy(stockMinimoPersonalizado = valor.filter { c -> c.isDigit() }.take(4)) }
    }

    fun onConfirmarEdicionStockMinimo() {
        _state.update { it.copy(stockMinimoEditando = false) }
    }

    fun onCancelarEdicionStockMinimo() {
        _state.update { it.copy(stockMinimoEditando = false, stockMinimoPersonalizado = "") }
    }

    // â”€â”€ 3. GESTIÃ“N DE FACTURA Y CONDICIÃ“N DE PAGO â”€â”€
    fun onFacturaChanged(factura: String) {
        val clean = factura.trim().uppercase()
        // No auto-rellena â€” solo avisa que existe, usuario debe tocar Aplicar para traer datos vinculados
        _state.update { it.copy(numeroFactura = clean, mensajeError = null, facturaDetectadaInfo = null) }
        searchFacturaJob?.cancel()
        if (clean.isNotBlank()) {
            searchFacturaJob = viewModelScope.launch {
                delay(400)
                val provId = _state.value.proveedorIdSeleccionado
                val facturaExistente = if (provId.isNotBlank()) {
                    facturaCompraRepository.buscarFacturaPorNumeroYProveedor(clean, provId)
                } else {
                    facturaCompraRepository.buscarFacturaPorNumero(clean)
                }
                _state.update { it.copy(facturaDetectadaInfo = facturaExistente) }
            }
        }
    }
    
    fun onAplicarFacturaExistente() {
        val info = _state.value.facturaDetectadaInfo ?: return
        _state.update {
            it.copy(
                proveedorNombre = info.proveedorNombre,
                rucProveedorSeleccionado = info.rucProveedor,
                proveedorIdSeleccionado = info.proveedorId,
                montoTotalFactura = if (info.montoTotal > 0) info.montoTotal.toString() else it.montoTotalFactura
            )
        }
    }

    fun onMontoTotalFacturaChanged(monto: String) {
        val clean = monto.replace(',', '.').filter { it.isDigit() || it == '.' }
        val parts = clean.split('.')
        val finalClean = if (parts.size > 2) "${parts[0]}.${parts.drop(1).joinToString("")}" else clean
        _state.update { it.copy(montoTotalFactura = finalClean, mensajeError = null) }
    }

    fun onTipoCondicionPagoChanged(tipo: TipoCondicionPago) {
        _state.update { it.copy(tipoCondicionPago = tipo, mensajeError = null) }
    }

    fun onDiasCreditoPersonalizadoChanged(dias: String) {
        val clean = dias.filter { it.isDigit() }
        _state.update { it.copy(diasCreditoPersonalizado = clean, mensajeError = null) }
    }

    // â”€â”€ 4. GESTIÃ“N DE PROVEEDOR â”€â”€
    fun onAbrirDialogoProveedores() {
        _state.update {
            it.copy(
                mostrarDialogoProveedores = true,
                busquedaProveedorDialogo = "",
                mostrarFormularioNuevoProveedor = false
            )
        }
    }

    fun onCerrarDialogoProveedores() {
        _state.update { it.copy(mostrarDialogoProveedores = false) }
    }

    fun onBusquedaProveedorDialogoChanged(q: String) {
        _state.update { it.copy(busquedaProveedorDialogo = q) }
    }

    fun onSeleccionarProveedor(prov: Proveedor) {
        val prevFactura = _state.value.facturaDetectadaInfo
        val debeLimpiarFactura = prevFactura != null && (
            (prevFactura.proveedorId.isNotBlank() && prov.id.isNotBlank() && prevFactura.proveedorId != prov.id) ||
            !prevFactura.proveedorNombre.trim().equals(prov.nombre.trim(), ignoreCase = true)
        )
        _state.update {
            it.copy(
                proveedorNombre = prov.nombre,
                proveedorIdSeleccionado = prov.id,
                rucProveedorSeleccionado = prov.idFiscal,
                mostrarDialogoProveedores = false,
                mensajeError = null,
                facturaDetectadaInfo = if (debeLimpiarFactura) null else it.facturaDetectadaInfo
            )
        }
    }

    fun onLimpiarProveedor() {
        val cur = _state.value
        val facturaEsDeEsteProveedor = cur.facturaDetectadaInfo != null &&
            (cur.facturaDetectadaInfo.proveedorId == cur.proveedorIdSeleccionado || cur.facturaDetectadaInfo.proveedorNombre.equals(cur.proveedorNombre, ignoreCase = true))
        val facturaEsSugerida = cur.ultimaFacturaSugerida != null &&
            cur.numeroFactura.equals(cur.ultimaFacturaSugerida.numeroFactura, ignoreCase = true) &&
            cur.proveedorNombre.equals(cur.ultimaFacturaSugerida.proveedorNombre, ignoreCase = true)
        _state.update {
            it.copy(
                proveedorNombre = "",
                proveedorIdSeleccionado = "",
                rucProveedorSeleccionado = "",
                facturaDetectadaInfo = null,
                // Si borras proveedor y la factura nació de ese proveedor, borra también factura y tope — no dejar F0032 suelto
                numeroFactura = if (facturaEsDeEsteProveedor || facturaEsSugerida) "" else it.numeroFactura,
                montoTotalFactura = if (facturaEsDeEsteProveedor || facturaEsSugerida) "" else it.montoTotalFactura
            )
        }
    }

    fun onToggleFormularioNuevoProveedor(mostrar: Boolean) {
        _state.update {
            it.copy(
                mostrarFormularioNuevoProveedor = mostrar,
                nuevoProveedorNombre = if (mostrar) it.busquedaProveedorDialogo else "",
                nuevoProveedorRuc = "",
                nuevoProveedorTelefono = "",
                nuevoProveedorContacto = "",
                nuevoProveedorEmail = "",
                nuevoProveedorDireccion = ""
            )
        }
    }

    fun onNuevoProveedorNombreChanged(nombre: String) {
        _state.update { it.copy(nuevoProveedorNombre = nombre) }
    }

    fun onNuevoProveedorRucChanged(ruc: String) {
        _state.update { it.copy(nuevoProveedorRuc = ruc) }
    }

    fun onNuevoProveedorTelefonoChanged(tel: String) {
        _state.update { it.copy(nuevoProveedorTelefono = tel) }
    }

    fun onNuevoProveedorContactoChanged(contacto: String) {
        _state.update { it.copy(nuevoProveedorContacto = contacto) }
    }

    fun onNuevoProveedorEmailChanged(email: String) {
        _state.update { it.copy(nuevoProveedorEmail = email) }
    }

    fun onNuevoProveedorDireccionChanged(dir: String) {
        _state.update { it.copy(nuevoProveedorDireccion = dir) }
    }

    fun onGuardarNuevoProveedor() {
        val current = _state.value
        onGuardarNuevoProveedorConDatos(
            nombre = current.nuevoProveedorNombre,
            idFiscal = current.nuevoProveedorRuc,
            contacto = current.nuevoProveedorContacto,
            telefono = current.nuevoProveedorTelefono,
            email = current.nuevoProveedorEmail,
            direccion = current.nuevoProveedorDireccion,
            montoMinimo = 0.0
        )
    }

    fun onGuardarNuevoProveedorConDatos(
        nombre: String,
        idFiscal: String,
        contacto: String,
        telefono: String,
        email: String,
        direccion: String,
        montoMinimo: Double
    ) {
        val nombreTrim = nombre.trim()
        if (nombreTrim.isBlank()) return
        if (_state.value.estaGuardando) return

        val montoLimpio = if (montoMinimo < 0.0) 0.0 else montoMinimo
        val nuevo = Proveedor(
            nombre = nombreTrim,
            idFiscal = idFiscal.trim(),
            contacto = contacto.trim(),
            telefono = telefono.trim(),
            email = email.trim(),
            direccion = direccion.trim(),
            montoMinimoPedido = montoLimpio
        )

        viewModelScope.launch {
            val result = proveedorRepository.registrarOActualizarProveedor(nuevo)
            result.onSuccess { provId ->
                _state.update {
                    it.copy(
                        proveedorNombre = nuevo.nombre,
                        proveedorIdSeleccionado = provId,
                        rucProveedorSeleccionado = nuevo.idFiscal,
                        mostrarDialogoProveedores = false,
                        mostrarFormularioNuevoProveedor = false
                    )
                }
            }
        }
    }

    // â”€â”€ 5. ASENTAMIENTO ATÃ“MICO EN INVENTARIO â”€â”€
    fun guardarIngreso(onSuccess: () -> Unit) {
        val current = _state.value
        // Candado interior: aunque la manija de afuera (botÃ³n) falle,
        // adentro nadie vuelve a entrar mientras ya se estÃ¡ guardando.
        if (current.estaGuardando) return
        if (!current.esFormularioValido) {
            _state.update {
                it.copy(
                    mensajeError = when {
                        current.tieneVencimientoNoCoincideConLoteExistente -> "El lote ${current.loteExistenteDetectado?.numero} ya existe con vencimiento ${current.loteExistenteDetectado?.vencimiento}. Cambia la fecha a ${current.loteExistenteDetectado?.vencimiento} o toca Agregar a este lote."
                        current.tieneUnidadesPorCajaInvalida -> "Indica cuÃ¡ntas unidades trae cada caja (ej: 24). No puede ser 0 ni vacÃ­o."
                        current.faltaMontoTotalEnFacturaNueva -> "Para la factura nueva ${current.numeroFactura} debes indicar el total que dice el papel (ej: 1200.00)."
                        current.tieneFaltaComprobanteEnControlado -> "Este medicamento necesita factura y proveedor por control sanitario."
                        current.tieneInconsistenciaFacturaSinProveedor -> "Si escribes factura ${current.numeroFactura} debes elegir el proveedor."
                        current.esVencidoOInvalido -> "La fecha de vencimiento no puede ser anterior al mes actual."
                        current.excedeMontoFactura -> "Ya supera lo que dice la factura (${current.montoTotalFacturaDouble}). Revisa el costo."
                        current.faltaCostoDeCompra -> "Digita el costo total de compra real del lote. MercaderÃ­a sin costo no se guarda."
                        else -> "Completa lote y cantidad mayor a 0."
                    }
                )
            }
            return
        }

        val usuarioEmail = FirebaseAuth.getInstance().currentUser?.email ?: "administrador@farmacia.com"

        when (NetworkHealthMonitor.status.value) {
            NetworkStatus.DESCONECTADO, NetworkStatus.SIN_SALIDA -> {
                _state.update { it.copy(estaGuardando = false, mensajeError = "Sin conexiÃ³n real (sin datos/wifi). No se guardÃ³ nada. Activa datos y reintenta.") }
                return
            }
            NetworkStatus.ESTADO_DEGRADADO -> {
                _state.update { it.copy(estaGuardando = false, mensajeError = "ConexiÃ³n bloqueada hacia el servidor (falso internet). Verifica firewall/captive portal y reintenta.") }
                return
            }
            NetworkStatus.CONEXION_LENTA -> {
                _state.update { it.copy(estaGuardando = true, mensajeError = null) }
            }
            else -> {}
        }

        _state.update { it.copy(estaGuardando = true, mensajeError = null) }

        // El mínimo propuesto SOLO se aplica si el producto sigue sin mínimo (nunca pisa uno real).
        val aplicarStockMinimo = if (current.stockMinimoActual <= 0) current.stockMinimoAplicar.toDouble() else 0.0

        viewModelScope.launch {
            val result = repository.registrarIngresoLote(
                productId = current.productoId,
                productoNombre = current.productoNombre,
                empaque = current.empaque,
                numeroLote = current.numeroLote,
                fechaVencimiento = current.fechaVencimiento,
                cantidadTotal = current.cantidadTotalIngresada,
                cantidadComprada = current.cantidadCompradaBase,
                bonificacionGratis = current.bonificacionUnidadesTotales,
                costoTotal = current.costoTotalIngreso,
                costoUnitario = current.costoUnitarioCalculado,
                proveedorId = current.proveedorIdSeleccionado,
                proveedorNombre = current.proveedorNombre,
                rucProveedor = current.rucProveedorSeleccionado,
                numeroFactura = current.numeroFactura,
                montoTotalFactura = current.montoTotalFacturaDouble,
                condicionPago = current.condicionPagoResumen,
                fechaVencimientoPago = current.fechaVencimientoPagoCalculada,
                stockMinimoNuevo = aplicarStockMinimo,
                usuarioEmail = usuarioEmail
            )

            result.fold(
                onSuccess = {
                    _state.update { it.copy(estaGuardando = false, guardadoExitoso = true) }
                    onSuccess()
                },
                onFailure = { error ->
                    val net = NetworkHealthMonitor.status.value
                    val msg = when {
                        net == NetworkStatus.CONEXION_LENTA -> "Conexión lenta: tardó pero falló. Revisa señal y reintenta — nada se guardó a medias."
                        error.message?.contains("UNAVAILABLE", true) == true || error.message?.contains("Network", true) == true -> "Sin conexión estable al servidor. Nada se guardó a medias. Reintenta con mejor señal."
                        else -> error.message ?: "Ocurrió un error al registrar el ingreso del lote."
                    }
                    _state.update {
                        it.copy(
                            estaGuardando = false,
                            mensajeError = msg
                        )
                    }
                }
            )
        }
    }

    fun onDismissVincularPedido() {
        _state.update { it.copy(mostrarDialogoVincularPedido = false) }
    }

    fun onConfirmarVincularPedido(onSuccess: () -> Unit) {
        _state.update { it.copy(mostrarDialogoVincularPedido = false) }
        guardarIngreso(onSuccess)
    }

    fun onConfirmarIngresoSuelto(onSuccess: () -> Unit) {
        _state.update { it.copy(mostrarDialogoVincularPedido = false, pedidoPendienteVinculable = null) }
        guardarIngreso(onSuccess)
    }
}
