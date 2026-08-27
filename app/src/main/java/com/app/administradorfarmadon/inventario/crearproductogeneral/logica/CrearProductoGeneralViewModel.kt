package com.app.administradorfarmadon.inventario.crearproductogeneral.logica

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.ClasificadorProductoIaRepository
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CrearProductoRepository
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.ProductoInventario
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrearProductoGeneralViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: CrearProductoRepository = CrearProductoRepository()
    private val _uiState = MutableStateFlow(CrearProductoUiState())
    val uiState: StateFlow<CrearProductoUiState> = _uiState.asStateFlow()

    private var verificarCodigoJob: Job? = null

    init {
        SessionManager.init(application)
        val cid = SessionManager.clienteIdGarantizado
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        _uiState.update { it.copy(clienteId = cid, usuarioActualUid = uid) }
    }

    /**
     * El usuario escribe tranquilamente sin que la IA compita ni dispare llamadas en segundo plano.
     */
    fun onNombreChanged(nuevoNombre: String) {
        val valorSanitizado = nuevoNombre.take(120)
        val cid = _uiState.value.clienteId.ifBlank { SessionManager.clienteIdGarantizado }

        // DETECCIÓN INTELIGENTE: Si el usuario pistoleó un código de barras en el campo de Nombre
        val esCodigoNumerico = valorSanitizado.trim().length in 8..20 && valorSanitizado.trim().all { it.isDigit() }

        if (esCodigoNumerico && !_uiState.value.formularioDesplegado) {
            viewModelScope.launch {
                val sucursalId = SessionManager.sucursalIdEfectiva
                val existente = repository.buscarProductoPorCodigoBarras(cid, valorSanitizado.trim(), sucursalId)
                if (existente != null) {
                    _uiState.update {
                        it.copy(
                            productoExistenteDuplicado = existente,
                            nombre = "",
                            errores = it.errores + ("nombre" to "El código ${valorSanitizado.trim()} ya pertenece a '${existente.second}'.")
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            codigoBarras = valorSanitizado.trim(),
                            nombre = "",
                            formularioDesplegado = true,
                            productoExistenteDuplicado = null
                        )
                    }
                }
            }
            return
        }

        _uiState.update {
            val errores = it.errores.toMutableMap().apply { remove("nombre") }
            it.copy(
                nombre = valorSanitizado,
                nombreSugeridoCorregido = "",
                errores = errores
            )
        }
    }

    fun ejecutarAnalisisIa() {
        val s = _uiState.value
        val nombreLimpio = s.nombre.trim()

        if (nombreLimpio.length < 2) {
            _uiState.update {
                it.copy(errores = it.errores + ("nombre" to "Ingresa el nombre del producto primero."))
            }
            return
        }

        // RESPUESTA INMEDIATA: Despliega la pantalla a 60/40 en 0 milisegundos con animación fluida
        _uiState.update {
            it.copy(
                formularioDesplegado = true,
                clasificandoIa = true,
                mensajeErrorGuardado = null
            )
        }

        viewModelScope.launch {
            val sugerencia = ClasificadorProductoIaRepository.clasificarProducto(nombreLimpio)
            val cantFinal = sugerencia.cantidadContenido.ifBlank {
                val (c, _) = CatalogoEmpaques.separarContenidoYUnidad(sugerencia.medidaConcentracion)
                c
            }
            val unidadFinal = sugerencia.unidadMedida.ifBlank {
                val (_, u) = CatalogoEmpaques.separarContenidoYUnidad(sugerencia.medidaConcentracion)
                u
            }

            _uiState.update { state ->
                state.copy(
                    clasificandoIa = false,
                    sugerenciaIaAplicada = sugerencia.exitoIa,
                    esModoManual = !sugerencia.exitoIa,
                    nombreSugeridoCorregido = if (sugerencia.nombreCorregido.isNotBlank() && !sugerencia.nombreCorregido.equals(nombreLimpio, ignoreCase = true)) sugerencia.nombreCorregido else "",
                    tipoProducto = sugerencia.tipoProducto,
                    categoriaNombre = sugerencia.categoriaNombre,
                    principioActivo = sugerencia.principioActivo,
                    laboratorio = sugerencia.laboratorio,
                    empaque = CatalogoEmpaques.normalizarEmpaque(sugerencia.empaque),
                    cantidadContenido = cantFinal,
                    unidadMedida = unidadFinal,
                    variantesSugeridas = sugerencia.variantesSugeridas,
                    varianteSeleccionada = "",
                    requiereReceta = sugerencia.requiereReceta,
                    esRefrigerado = sugerencia.esRefrigerado,
                    permiteFraccionar = sugerencia.permiteFraccionar,
                    clasificacionControl = sugerencia.clasificacionControl
                )
            }
        }
    }

    fun onAplicarNombreCorregido() {
        val corregido = _uiState.value.nombreSugeridoCorregido
        if (corregido.isNotBlank()) {
            _uiState.update {
                it.copy(
                    nombre = corregido,
                    nombreSugeridoCorregido = ""
                )
            }
        }
    }

    fun onAbrirCamaraScanner(abrir: Boolean) {
        _uiState.update { it.copy(mostrarCamaraScanner = abrir) }
    }

    fun onCodigoBarrasDetectadoPorCamara(codigo: String) {
        onCodigoBarrasChanged(codigo)
        _uiState.update { it.copy(mostrarCamaraScanner = false) }
    }

    fun onDesplegarFormularioManual() {
        _uiState.update {
            it.copy(
                formularioDesplegado = true,
                esModoManual = true,
                usuarioModificoManualmente = true,
                sugerenciaIaAplicada = false,
                clasificandoIa = false,
                nombre = "",
                categoriaNombre = "",
                laboratorio = "",
                principioActivo = "",
                empaque = "",
                cantidadContenido = "",
                unidadMedida = "",
                codigoBarras = "",
                variantesSugeridas = emptyList(),
                requiereReceta = false,
                esRefrigerado = false,
                permiteFraccionar = false,
                nombreSugeridoCorregido = "",
                tipoProducto = "MEDICAMENTO"
            )
        }
    }

    fun onCantidadContenidoChanged(valor: String) {
        val soloDigitosYPunto = valor.replace(',', '.').filter { it.isDigit() || it == '.' }
        val partes = soloDigitosYPunto.split('.')
        val limpio = if (partes.size > 2) {
            "${partes[0]}.${partes.subList(1, partes.size).joinToString("")}"
        } else {
            soloDigitosYPunto
        }.take(15)
        _uiState.update {
            it.copy(
                cantidadContenido = limpio,
                varianteSeleccionada = "",
                usuarioModificoManualmente = true
            )
        }
    }

    fun onUnidadMedidaSelected(unidad: String) {
        val unitValida = CatalogoEmpaques.normalizarUnidad(unidad)
        val empCompatibles = CatalogoEmpaques.obtenerEmpaquesPorUnidad(unitValida)
        val empActual = _uiState.value.empaque
        val empSigueValido = empActual.isNotBlank() && empCompatibles.any { it.equals(empActual, ignoreCase = true) }
        _uiState.update {
            it.copy(
                unidadMedida = unitValida,
                empaque = if (empSigueValido) empActual else "",
                empaquesDisponibles = empCompatibles,
                unidadesDisponibles = CatalogoEmpaques.UNIDADES_MEDIDA_VALIDAS,
                varianteSeleccionada = "",
                usuarioModificoManualmente = true
            )
        }
    }

    fun onCategoriaSelected(categoria: String) {
        val catValida = CatalogoEmpaques.normalizarCategoria(categoria)
        _uiState.update { it.copy(categoriaNombre = catValida, usuarioModificoManualmente = true) }
    }

    fun onEmpaqueSelected(empaque: String) {
        val empValido = CatalogoEmpaques.normalizarEmpaque(empaque)
        val uniCompatibles = CatalogoEmpaques.obtenerUnidadesPorEmpaque(empValido)
        val uniActual = _uiState.value.unidadMedida
        val uniSigueValida = uniActual.isNotBlank() && uniCompatibles.any { it.equals(uniActual, ignoreCase = true) }
        _uiState.update {
            it.copy(
                empaque = empValido,
                unidadMedida = if (uniSigueValida) uniActual else "",
                empaquesDisponibles = CatalogoEmpaques.EMPAQUES_VALIDOS,
                unidadesDisponibles = uniCompatibles,
                usuarioModificoManualmente = true
            )
        }
    }

    fun onLaboratorioChanged(valor: String) {
        _uiState.update { it.copy(laboratorio = valor.take(80), usuarioModificoManualmente = true) }
    }

    fun onPrincipioActivoChanged(valor: String) {
        _uiState.update { it.copy(principioActivo = valor.take(100), usuarioModificoManualmente = true) }
    }

    fun onCodigoBarrasChanged(valor: String) {
        val limpio = valor.filter { it.isLetterOrDigit() || it == '-' }.take(40)
        val cid = _uiState.value.clienteId.ifBlank { SessionManager.clienteIdGarantizado }

        _uiState.update {
            val errores = it.errores.toMutableMap().apply { remove("codigoBarras") }
            it.copy(
                codigoBarras = limpio,
                productoExistenteDuplicado = null,
                errores = errores
            )
        }

        verificarCodigoJob?.cancel()
        if (limpio.length >= 6) {
            verificarCodigoJob = viewModelScope.launch {
                delay(300)
                val sucursalId = SessionManager.sucursalIdEfectiva
                val existente = repository.buscarProductoPorCodigoBarras(cid, limpio, sucursalId)
                _uiState.update { state ->
                    if (existente != null) {
                        val errores = state.errores.toMutableMap().apply {
                            put("codigoBarras", "Este código ya pertenece a '${existente.second}'.")
                        }
                        state.copy(
                            productoExistenteDuplicado = existente,
                            errores = errores
                        )
                    } else {
                        val errores = state.errores.toMutableMap().apply { remove("codigoBarras") }
                        state.copy(
                            productoExistenteDuplicado = null,
                            errores = errores
                        )
                    }
                }
            }
        }
    }

    fun onVarianteSeleccionada(variante: String) {
        val (cant, unidad) = CatalogoEmpaques.separarContenidoYUnidad(variante)
        val empaqueEnVariante = CatalogoEmpaques.EMPAQUES_VALIDOS.firstOrNull {
            variante.contains(it, ignoreCase = true)
        }
        _uiState.update {
            it.copy(
                varianteSeleccionada = variante,
                cantidadContenido = cant,
                unidadMedida = if (unidad.isNotBlank()) unidad else it.unidadMedida,
                empaque = empaqueEnVariante ?: it.empaque,
                usuarioModificoManualmente = true
            )
        }
    }

    fun onRequiereRecetaChanged(requiere: Boolean) {
        _uiState.update { it.copy(requiereReceta = requiere, usuarioModificoManualmente = true) }
    }

    fun onPermiteFraccionarChanged(permite: Boolean) {
        _uiState.update { it.copy(permiteFraccionar = permite, usuarioModificoManualmente = true) }
    }

    fun onEsRefrigeradoChanged(refrigerado: Boolean) {
        _uiState.update { it.copy(esRefrigerado = refrigerado, usuarioModificoManualmente = true) }
    }

    fun descartarProductoDuplicadoDetectado() {
        _uiState.update { it.copy(productoExistenteDuplicado = null) }
    }

    fun onRegistrarOtroProducto() {
        val cid = _uiState.value.clienteId.ifBlank { SessionManager.clienteIdGarantizado }
        val uid = _uiState.value.usuarioActualUid
        _uiState.update {
            CrearProductoUiState(
                clienteId = cid,
                usuarioActualUid = uid
            )
        }
    }

    fun onReintentarGuardado() {
        guardarProducto()
    }

    /**
     * Guarda el producto. Si ocurre un error, muestra el estado de error en el panel derecho sin perder datos.
     */
    fun guardarProducto() {
        val s = _uiState.value
        if (s.estadoGuardado == EstadoGuardadoProducto.GUARDANDO) return

        val errores = mutableMapOf<String, String>()
        val nombreTrim = s.nombre.trim()
        if (nombreTrim.isBlank()) {
            errores["nombre"] = "El nombre del producto es obligatorio"
        } else if (nombreTrim.length < 3) {
            errores["nombre"] = "El nombre debe tener al menos 3 caracteres"
        } else if (StructuralProductValidator.detectarBasuraEvidente(nombreTrim)) {
            errores["nombre"] = "Nombre no válido — verifica el nombre del producto"
        }

        if (s.productoExistenteDuplicado != null) {
            errores["codigoBarras"] = "El código ya pertenece a '${s.productoExistenteDuplicado.second}'"
        }

        // Validación de coherencia Envase + Unidad + Cantidad (desde la ficha, no solo en Precios)
        val empTrim = s.empaque.trim()
        val uniTrim = s.unidadMedida.trim()
        val cantTrim = s.cantidadContenido.trim()
        if (empTrim.isBlank()) {
            errores["empaque"] = "Elige el envase (Caja, Frasco, Blíster...)"
        }
        if (cantTrim.isBlank()) {
            errores["cantidadContenido"] = "Indica cuántas unidades trae el envase"
        } else {
            val cantNum = cantTrim.replace(',', '.').toDoubleOrNull()
            if (cantNum == null || cantNum <= 0) {
                errores["cantidadContenido"] = "La cantidad debe ser mayor a 0"
            }
        }
        if (uniTrim.isBlank()) {
            errores["unidadMedida"] = "Elige la unidad (Tab, ml, g...)"
        }
        if (empTrim.isNotBlank() && uniTrim.isNotBlank()) {
            val familia = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.detectarFamiliaFisica(empTrim, uniTrim)
            val empOk = familia.empaquesCompatibles.any { it.equals(empTrim, ignoreCase = true) }
            val uniOk = familia.unidadesCompatibles.any { it.equals(uniTrim, ignoreCase = true) }
            if (!empOk || !uniOk) {
                errores["empaque"] = "Envase y unidad no combinan. Para $uniTrim usa: ${familia.empaquesCompatibles.take(3).joinToString(", ")}"
            }
        }

        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores, estadoGuardado = EstadoGuardadoProducto.IDLE) }
            return
        }

        val nombreSanitizado = nombreTrim.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { p ->
                p.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        val empaqueFinal = if (s.empaque.isBlank()) "Caja" else CatalogoEmpaques.normalizarEmpaque(s.empaque)
        val categoriaFinal = if (s.categoriaNombre.isBlank()) "General" else CatalogoEmpaques.normalizarCategoria(s.categoriaNombre)
        val medidaFinal = s.medidaConcentracion
        val tipoFinal = if (s.tipoProducto.isBlank()) {
            if (s.requiereReceta || s.esRefrigerado || s.principioActivo.isNotBlank()) "MEDICAMENTO" else "GENERAL"
        } else s.tipoProducto

        val cidFinal = s.clienteId.ifBlank { SessionManager.clienteIdGarantizado }
        val sucursalIdFinal = SessionManager.sucursalIdEfectiva
        val producto = ProductoInventario(
            clienteId = cidFinal,
            nombre = nombreSanitizado,
            principioActivo = s.principioActivo.trim(),
            codigoBarras = s.codigoBarras.trim(),
            tipoProducto = tipoFinal,
            categoriaNombre = categoriaFinal,
            laboratorio = s.laboratorio.trim(),
            empaque = empaqueFinal,
            contenido = s.cantidadContenido.trim(),
            contenidoUnidad = s.unidadMedida.trim(),
            medidaConcentracion = medidaFinal,
            requiereReceta = if (s.esMedicamento) s.requiereReceta else false,
            esRefrigerado = if (s.esMedicamento) s.esRefrigerado else false,
            clasificacionControl = if (s.esMedicamento) s.clasificacionControl else "VENTA_LIBRE"
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoProducto.GUARDANDO,
                    mensajeErrorGuardado = null
                )
            }
            try {
                val nuevoId = repository.guardarProducto(producto, sucursalIdFinal)
                _uiState.update {
                    it.copy(
                        estadoGuardado = EstadoGuardadoProducto.EXITO,
                        productoGuardadoId = nuevoId,
                        productoGuardadoNombre = nombreSanitizado
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        estadoGuardado = EstadoGuardadoProducto.ERROR,
                        mensajeErrorGuardado = e.message ?: "No se pudo conectar con el servidor. Revisa tu conexión."
                    )
                }
            }
        }
    }
}
