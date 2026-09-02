package com.app.administradorfarmadon.facturacion.envio.datos

/**
 * Fuente única de configuración y URLs para la integración oficial con APISUNAT.
 * Conforme a la documentación oficial: todo el tráfico de API corre bajo https://back.apisunat.com.
 */
object ApisunatConfig {
    const val BASE_URL = "https://back.apisunat.com/"
    const val LAST_DOCUMENT_URL = "https://back.apisunat.com/personas/lastDocument"
    const val TIMEOUT_SECONDS = 15L
}
