package com.app.administradorfarmadon.notificaciones.suscripcion.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.notificaciones.suscripcion.datos.AlertaSuscripcionItem
import com.app.administradorfarmadon.notificaciones.suscripcion.datos.AlertaSuscripcionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertaSuscripcionViewModel(
    private val repository: AlertaSuscripcionRepository = AlertaSuscripcionRepository()
) : ViewModel() {

    private val _alertaVisible = MutableStateFlow<AlertaSuscripcionItem?>(null)
    val alertaVisible: StateFlow<AlertaSuscripcionItem?> = _alertaVisible.asStateFlow()

    private var autoHideJob: Job? = null
    private var escuchaJob: Job? = null

    init {
        iniciarEscuchaAlertas()
    }

    fun iniciarEscuchaAlertas() {
        escuchaJob?.cancel()
        val clienteId = SessionManager.clienteIdGarantizado
        val usuarioUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val rol = SessionManager.rol.lowercase()
        val esAdminODueno = rol == "dueño" || rol == "dueno" || rol == "administrador" || rol == "admin" || rol.isBlank()

        if (clienteId.isBlank() || usuarioUid.isBlank()) {
            _alertaVisible.value = null
            return
        }

        escuchaJob = viewModelScope.launch {
            repository.escucharAlertaPendiente(
                clienteId = clienteId,
                usuarioUid = usuarioUid,
                esAdminODueno = esAdminODueno
            ).collect { nuevaAlerta ->
                _alertaVisible.value = nuevaAlerta

                // Si la alerta tiene temporizador de auto-hide, programar su desvanecimiento y lectura
                autoHideJob?.cancel()
                if (nuevaAlerta != null && !nuevaAlerta.esPersistente && nuevaAlerta.autoHideSegundos > 0) {
                    autoHideJob = viewModelScope.launch {
                        delay(nuevaAlerta.autoHideSegundos * 1000L)
                        if (_alertaVisible.value?.id == nuevaAlerta.id) {
                            _alertaVisible.value = null
                            onAlertaRenderizadaSilenciosa(nuevaAlerta.id)
                        }
                    }
                }
            }
        }
    }

    fun onAlertaRenderizadaSilenciosa(alertaId: String) {
        val clienteId = SessionManager.clienteIdGarantizado
        val usuarioUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (clienteId.isNotBlank() && usuarioUid.isNotBlank() && alertaId.isNotBlank()) {
            repository.marcarAlertaLeidaSilenciosa(clienteId, usuarioUid, alertaId)
        }
    }

    fun descartarAlertaManual(alertaId: String) {
        autoHideJob?.cancel()
        _alertaVisible.value = null
        onAlertaRenderizadaSilenciosa(alertaId)
    }

    override fun onCleared() {
        super.onCleared()
        autoHideJob?.cancel()
        escuchaJob?.cancel()
    }
}
