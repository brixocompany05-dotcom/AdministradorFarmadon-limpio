package com.app.administradorfarmadon.inventario.editarproductosinventario.logica

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.editarproductosinventario.datos.EditarProductoRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel Enterprise que gestiona la edición de productos basándose estrictamente en los datos reales.
 * Paridad exacta 1 a 1 con Crear Producto.
 */
class EditarProductoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: EditarProductoRepository = EditarProductoRepository()
    private val _uiState = MutableStateFlow(EditarProductoUiState())
    val uiState: StateFlow<EditarProductoUiState> = _uiState.asStateFlow()

    private var verificarCodigoJob: Job? = null
    private var cargarProductoJob: Job? = null

    init {
        SessionManager.init(application)
    }

    fun init(productoId: String) {
        if (_uiState.value.productoId == productoId && !_uiState.value.isLoading) return

        val clienteId = SessionManager.clienteIdGarantizado.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }

        _uiState.update {
            it.copy(
                clienteId = clienteId,
                productoId = productoId,
                isLoading = true,
                estadoGuardado = EstadoGuardadoEdicion.IDLE,
                mensajeErrorGuardado = null,
                fieldErrors = emptyMap()
            )
        }

        cargarProducto(clienteId, productoId)
    }

    private fun cargarProducto(clienteId: String, productoId: String) {
        cargarProductoJob?.cancel()
        cargarProductoJob = viewModelScope.launch {
            repository.cargarProducto(clienteId, productoId)
                .onSuccess { p ->
                    // Blindaje contra contaminación de contexto: Si el usuario cambió a otro producto mientras cargaba, descartar
                    if (_uiState.value.productoId != productoId) return@launch
                    val esRefrig = p.temperaturaAlmacenamiento.contains("REFRIG", ignoreCase = true)
                    val catNombre = p.categoriaPrincipal.ifBlank { p.categoriaNombre }.ifBlank { "General" }
                    val tipoProd = if (p.requiereReceta || esRefrig || p.principioActivo.isNotBlank()) "MEDICAMENTO" else "GENERAL"
                    
                    val (cantFromContenido, unitFromContenido) = CatalogoEmpaques.separarContenidoYUnidad(p.contenido)
                    val cant = if (cantFromContenido.isNotBlank()) cantFromContenido else {
                        val (c, _) = CatalogoEmpaques.separarContenidoYUnidad(p.concentracion)
                        c
                    }
                    val unidad = if (p.contenidoUnidad.isNotBlank()) CatalogoEmpaques.normalizarUnidad(p.contenidoUnidad) else if (unitFromContenido.isNotBlank()) unitFromContenido else {
                        val (_, u) = CatalogoEmpaques.separarContenidoYUnidad(p.concentracion)
                        u
                    }
                    val empFinal = p.empaque.ifBlank { "Caja" }
                    val empCompatibles = CatalogoEmpaques.EMPAQUES_VALIDOS
                    val uniCompatibles = CatalogoEmpaques.obtenerUnidadesPorEmpaque(empFinal)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalProduct = p,
                            nombre = p.nombre,
                            tipoProducto = tipoProd,
                            principioActivo = p.principioActivo,
                            categoriaNombre = catNombre,
                            laboratorio = p.proveedorBaseNombre,
                            empaque = empFinal,
                            cantidadContenido = cant,
                            unidadMedida = unidad,
                            empaquesDisponibles = empCompatibles,
                            unidadesDisponibles = uniCompatibles,
                            codigoBarras = p.codigo,
                            requiereReceta = p.requiereReceta,
                            esRefrigerado = esRefrig,
                            permiteFraccionar = p.permiteFraccionar,
                            estadoGuardado = EstadoGuardadoEdicion.IDLE,
                            mensajeErrorGuardado = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            estadoGuardado = EstadoGuardadoEdicion.ERROR,
                            mensajeErrorGuardado = e.message ?: "No se pudo cargar la información del producto."
                        )
                    }
                }
        }
    }

    fun onNombreChanged(nuevo: String) {
        _uiState.update {
            val errors = it.fieldErrors.toMutableMap().apply { remove("nombre") }
            it.copy(nombre = nuevo.take(120), fieldErrors = errors)
        }
    }

    fun onPrincipioActivoChanged(nuevo: String) {
        _uiState.update { it.copy(principioActivo = nuevo.take(100)) }
    }

    fun onTipoProductoChanged(nuevo: String) {
        val tipoLimpio = if (nuevo.equals("GENERAL", ignoreCase = true)) "GENERAL" else "MEDICAMENTO"
        _uiState.update {
            it.copy(
                tipoProducto = tipoLimpio,
                principioActivo = if (tipoLimpio == "GENERAL") "" else it.principioActivo,
                requiereReceta = if (tipoLimpio == "GENERAL") false else it.requiereReceta,
                esRefrigerado = if (tipoLimpio == "GENERAL") false else it.esRefrigerado
            )
        }
    }

    fun onCategoriaChanged(nueva: String) {
        val catNormalizada = CatalogoEmpaques.normalizarCategoria(nueva)
        _uiState.update {
            it.copy(categoriaNombre = catNormalizada)
        }
    }

    fun onLaboratorioChanged(nuevo: String) {
        _uiState.update { it.copy(laboratorio = nuevo.take(80)) }
    }

    fun onEmpaqueChanged(nuevo: String) {
        val empNormalizado = CatalogoEmpaques.normalizarEmpaque(nuevo)
        val uniCompatibles = CatalogoEmpaques.obtenerUnidadesPorEmpaque(empNormalizado)
        val uniActual = _uiState.value.unidadMedida
        val uniSigueValida = uniActual.isNotBlank() && uniCompatibles.any { it.equals(uniActual, ignoreCase = true) }
        _uiState.update {
            it.copy(
                empaque = empNormalizado,
                unidadMedida = if (uniSigueValida) uniActual else "",
                empaquesDisponibles = CatalogoEmpaques.EMPAQUES_VALIDOS,
                unidadesDisponibles = uniCompatibles
            )
        }
    }

    fun onCantidadContenidoChanged(nuevo: String) {
        val soloDigitosYPunto = nuevo.replace(',', '.').filter { it.isDigit() || it == '.' }
        val partes = soloDigitosYPunto.split('.')
        val limpio = if (partes.size > 2) {
            "${partes[0]}.${partes.subList(1, partes.size).joinToString("")}"
        } else {
            soloDigitosYPunto
        }.take(15)
        _uiState.update { it.copy(cantidadContenido = limpio) }
    }

    fun onUnidadMedidaChanged(nueva: String) {
        val unitNormalizada = CatalogoEmpaques.normalizarUnidad(nueva)
        val empCompatibles = CatalogoEmpaques.obtenerEmpaquesPorUnidad(unitNormalizada)
        val empActual = _uiState.value.empaque
        val empSigueValido = empActual.isNotBlank() && empCompatibles.any { it.equals(empActual, ignoreCase = true) }
        _uiState.update {
            it.copy(
                unidadMedida = unitNormalizada,
                empaque = if (empSigueValido) empActual else "",
                empaquesDisponibles = empCompatibles,
                unidadesDisponibles = CatalogoEmpaques.UNIDADES_MEDIDA_VALIDAS
            )
        }
    }

    fun onCodigoBarrasChanged(nuevo: String) {
        val limpio = nuevo.filter { it.isLetterOrDigit() || it == '-' }.take(40)
        val s = _uiState.value
        val cid = s.clienteId.ifBlank { SessionManager.clienteIdGarantizado }

        _uiState.update {
            val errors = it.fieldErrors.toMutableMap().apply { remove("codigoBarras") }
            it.copy(
                codigoBarras = limpio,
                productoExistenteDuplicado = null,
                fieldErrors = errors
            )
        }

        verificarCodigoJob?.cancel()
        if (limpio.length >= 6) {
            verificarCodigoJob = viewModelScope.launch {
                delay(300)
                val existente = repository.buscarProductoPorCodigoBarras(cid, limpio, s.productoId)
                _uiState.update { state ->
                    if (existente != null) {
                        val errors = state.fieldErrors.toMutableMap().apply {
                            put("codigoBarras", "Este código ya pertenece a '${existente.second}'.")
                        }
                        state.copy(
                            productoExistenteDuplicado = existente,
                            fieldErrors = errors
                        )
                    } else {
                        val errors = state.fieldErrors.toMutableMap().apply { remove("codigoBarras") }
                        state.copy(
                            productoExistenteDuplicado = null,
                            fieldErrors = errors
                        )
                    }
                }
            }
        }
    }

    fun onToggleCamaraScanner(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarCamaraScanner = mostrar) }
    }

    fun onBarcodeScanned(codigo: String) {
        onToggleCamaraScanner(false)
        onCodigoBarrasChanged(codigo)
    }

    fun onRequiereRecetaChanged(nuevo: Boolean) {
        _uiState.update { it.copy(requiereReceta = nuevo) }
    }

    fun onPermiteFraccionarChanged(nuevo: Boolean) {
        _uiState.update { it.copy(permiteFraccionar = nuevo) }
    }

    fun onEsRefrigeradoChanged(nuevo: Boolean) {
        _uiState.update { it.copy(esRefrigerado = nuevo) }
    }

    fun reintentarGuardado() {
        guardarCambios()
    }

    fun guardarCambios() {
        val s = _uiState.value
        if (s.nombre.trim().isBlank()) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors + ("nombre" to "El nombre comercial es obligatorio."),
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Por favor completa el nombre comercial del producto."
                )
            }
            return
        }
        if (com.app.administradorfarmadon.inventario.crearproductogeneral.logica.StructuralProductValidator.detectarBasuraEvidente(s.nombre.trim())) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors + ("nombre" to "Nombre no válido —” verifica el nombre del producto."),
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Nombre no válido —” verifica el nombre del producto."
                )
            }
            return
        }

        // La categoría es OBLIGATORIA: editar nunca puede dejar un producto sin categoría.
        if (s.categoriaNombre.trim().isBlank()) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors + ("categoria" to "Elige la categoría del producto (es obligatoria)."),
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Elige la categoría del producto (es obligatoria)."
                )
            }
            return
        }

        if (s.productoExistenteDuplicado != null) {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "El código de barras ya pertenece a '${s.productoExistenteDuplicado.second}'."
                )
            }
            return
        }

        // Coherencia Envase + Unidad + Cantidad (igual que al crear)
        val empTrimE = s.empaque.trim()
        val uniTrimE = s.unidadMedida.trim()
        val cantTrimE = s.cantidadContenido.trim()
        if (empTrimE.isBlank() || uniTrimE.isBlank() || cantTrimE.isBlank()) {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Completa envase, cantidad y unidad con valores lógicos."
                )
            }
            return
        }
        val cantNumE = cantTrimE.replace(',', '.').toDoubleOrNull()
        if (cantNumE == null || cantNumE <= 0) {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "La cantidad debe ser mayor a 0."
                )
            }
            return
        }
        val famE = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.detectarFamiliaFisica(empTrimE, uniTrimE)
        val empOkE = famE.empaquesCompatibles.any { it.equals(empTrimE, ignoreCase = true) }
        val uniOkE = famE.unidadesCompatibles.any { it.equals(uniTrimE, ignoreCase = true) }
        if (!empOkE || !uniOkE) {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Envase y unidad no combinan. Para $uniTrimE usa: ${famE.empaquesCompatibles.take(3).joinToString(", ")}"
                )
            }
            return
        }

        val cid = s.clienteId.ifBlank {
            SessionManager.clienteIdGarantizado.ifBlank {
                FirebaseAuth.getInstance().currentUser?.uid ?: ""
            }
        }

        if (cid.isBlank()) {
            _uiState.update {
                it.copy(
                    estadoGuardado = EstadoGuardadoEdicion.ERROR,
                    mensajeErrorGuardado = "Sesión no válida. Inicia sesión en Farmadon."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                estadoGuardado = EstadoGuardadoEdicion.GUARDANDO,
                mensajeErrorGuardado = null
            )
        }

        val tipoFinal = if (s.requiereReceta || s.esRefrigerado || s.principioActivo.isNotBlank()) "MEDICAMENTO" else s.tipoProducto

        viewModelScope.launch {
            repository.guardarEdicionProducto(
                clienteId = cid,
                productoId = s.productoId,
                nombre = s.nombre.trim(),
                principioActivo = s.principioActivo.trim(),
                tipoProducto = tipoFinal,
                categoriaNombre = s.categoriaNombre.trim(),
                laboratorio = s.laboratorio.trim(),
                empaque = s.empaque.trim(),
                contenido = s.cantidadContenido.trim(),
                contenidoUnidad = s.unidadMedida.trim(),
                medidaConcentracion = s.medidaConcentracion.trim(),
                codigoBarras = s.codigoBarras.trim(),
                requiereReceta = s.requiereReceta,
                esRefrigerado = s.esRefrigerado,
                permiteFraccionar = s.permiteFraccionar
            ).onSuccess {
                _uiState.update { it.copy(estadoGuardado = EstadoGuardadoEdicion.EXITO) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        estadoGuardado = EstadoGuardadoEdicion.ERROR,
                        mensajeErrorGuardado = e.message ?: "Ocurrió un error al guardar los cambios en la ficha."
                    )
                }
            }
        }
    }
}
