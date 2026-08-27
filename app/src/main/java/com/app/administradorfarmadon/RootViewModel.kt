package com.app.administradorfarmadon

import androidx.lifecycle.ViewModel
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel de nivel raíz para la PantallaPrincipal.
 * Gestiona estados globales que trascienden módulos, como el arranque (Splash)
 * y errores críticos de conexión inicial.
 */
class RootViewModel : ViewModel() {

    private val _incidenteSplash = MutableStateFlow<RegistroIncidenteUi?>(null)
    val incidenteSplash = _incidenteSplash.asStateFlow()

    private val _cargandoSplash = MutableStateFlow(true)
    val cargandoSplash = _cargandoSplash.asStateFlow()

    fun reportarIncidenteSplash(incidente: RegistroIncidenteUi?) {
        _incidenteSplash.value = incidente
    }

    fun setCargandoSplash(cargando: Boolean) {
        _cargandoSplash.value = cargando
    }

    fun reintentarArranque(onReintento: () -> Unit) {
        _incidenteSplash.value = null
        _cargandoSplash.value = true
        onReintento()
    }
}
