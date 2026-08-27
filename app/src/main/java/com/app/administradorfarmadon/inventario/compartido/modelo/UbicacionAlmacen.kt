package com.app.administradorfarmadon.inventario.compartido.modelo

import java.io.Serializable

/**
 * Modelo de catálogo maestro para Ubicaciones de Almacén (v2026).
 * Almacenado en Firestore clientes/{clienteId}/catalogos/ubicaciones (única verdad).
 */
data class UbicacionAlmacen(
    val id: String = "",
    val nombre: String = "",      // Identificador legible ("Pasillo 2, Estante B")
    val zona: String = "",        // Agrupación opcional ("Zona Fría", "Zona Ambiente")
    val descripcion: String = "", // Detalle u observaciones
    val activo: Boolean = true
) : Serializable
