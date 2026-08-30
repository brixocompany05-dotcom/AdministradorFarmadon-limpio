package com.app.administradorfarmadon.autenticacion.expediente.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.expediente.datos.ConsultarExpedienteUseCase
import com.app.administradorfarmadon.autenticacion.expediente.datos.ConsultarExpedienteUseCaseImpl
import com.app.administradorfarmadon.autenticacion.expediente.datos.ResultadoExpediente
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultarExpedienteUiState(
    val ruc: String = "",
    val cargando: Boolean = false,
    val resultado: ResultadoExpediente? = null,
    val errorValidacion: String? = null
)

class ConsultarExpedienteViewModel(
    private val useCase: ConsultarExpedienteUseCase = ConsultarExpedienteUseCaseImpl()
) : ViewModel() {

    private val _state = MutableStateFlow(ConsultarExpedienteUiState())
    val state = _state.asStateFlow()

    private var escuchaJob: Job? = null

    fun onRucChanged(ruc: String) {
        escuchaJob?.cancel()
        escuchaJob = null
        _state.update { it.copy(ruc = ruc.filter { char -> char.isDigit() }, resultado = null, errorValidacion = null) }
    }

    fun consultar() {
        val ruc = _state.value.ruc.trim()
        if (ruc.length < 8) {
            _state.update { it.copy(errorValidacion = "Ingresa tu RUC o DNI válido") }
            return
        }
        _state.update { it.copy(cargando = true, resultado = null, errorValidacion = null) }
        escuchaJob?.cancel()
        escuchaJob = viewModelScope.launch {
            useCase.observar(ruc).collect { res ->
                _state.update { it.copy(cargando = false, resultado = res) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        escuchaJob?.cancel()
    }
}
