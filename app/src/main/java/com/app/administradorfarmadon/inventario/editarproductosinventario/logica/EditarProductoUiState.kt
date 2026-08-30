package com.app.administradorfarmadon.inventario.editarproductosinventario.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques

/**
 * Estados del ciclo de guardado de edición de producto en vivo.
 */
enum class EstadoGuardadoEdicion {
    IDLE,
    GUARDANDO,
    EXITO,
    ERROR
}

/**
 * Estado inmutable de la pantalla de Edición de Producto Enterprise.
 * 100% Simétrico y reflejo exacto de la pantalla de Creación de Producto.
 */
data class EditarProductoUiState(
    val clienteId: String = "",
    val productoId: String = "",
    val isLoading: Boolean = true,
    val estadoGuardado: EstadoGuardadoEdicion = EstadoGuardadoEdicion.IDLE,
    val mensajeErrorGuardado: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    
    // Producto Original para comparación exacta de cambios
    val originalProduct: MoldeProductos? = null,

    // Datos de la Ficha Técnica (Identidad exacta de Crear Producto)
    val nombre: String = "",
    val tipoProducto: String = "MEDICAMENTO", // "MEDICAMENTO" | "GENERAL"
    val principioActivo: String = "",
    val categoriaNombre: String = "General",
    val laboratorio: String = "",
    val empaque: String = "Caja",
    
    // Contenido (numérico) y Unidad de Medida (selector canónico)
    val cantidadContenido: String = "",
    val unidadMedida: String = "",
    
    val codigoBarras: String = "",
    val productoExistenteDuplicado: Pair<String, String>? = null,
    val mostrarCamaraScanner: Boolean = false,

    // Condiciones Sanitarias (solo para medicamentos)
    val requiereReceta: Boolean = false,
    val esRefrigerado: Boolean = false,
    val permiteFraccionar: Boolean = false,

    // Catálogos válidos canónicos abreviados
    val categoriasDisponibles: List<String> = CatalogoEmpaques.CATEGORIAS_VALIDAS,
    val empaquesDisponibles: List<String> = CatalogoEmpaques.EMPAQUES_VALIDOS,
    val unidadesDisponibles: List<String> = CatalogoEmpaques.UNIDADES_MEDIDA_VALIDAS
) {
    val medidaConcentracion: String
        get() = if (cantidadContenido.isBlank()) "" else if (unidadMedida.isBlank()) cantidadContenido.trim() else "${cantidadContenido.trim()} $unidadMedida".trim()

    val esMedicamento: Boolean
        get() = tipoProducto.equals("MEDICAMENTO", ignoreCase = true) ||
                CatalogoEmpaques.esCategoriaMedica(categoriaNombre) ||
                requiereReceta ||
                esRefrigerado ||
                principioActivo.isNotBlank()

    val isAnyFieldChanged: Boolean
        get() {
            val orig = originalProduct ?: return false
            val origRefrig = orig.temperaturaAlmacenamiento.contains("REFRIG", ignoreCase = true)
            val origTipo = if (orig.requiereReceta || origRefrig || orig.principioActivo.isNotBlank()) "MEDICAMENTO" else "GENERAL"
            
            return nombre.trim().lowercase() != orig.nombre.trim().lowercase() ||
                    (esMedicamento && principioActivo.trim().lowercase() != orig.principioActivo.trim().lowercase()) ||
                    tipoProducto.trim().lowercase() != origTipo.lowercase() ||
                    categoriaNombre.trim().lowercase() != orig.categoriaNombre.trim().lowercase() ||
                    laboratorio.trim().lowercase() != orig.proveedorBaseNombre.trim().lowercase() ||
                    empaque.trim().lowercase() != orig.empaque.trim().lowercase() ||
                    cantidadContenido.trim() != orig.contenido.trim() ||
                    unidadMedida.trim().lowercase() != orig.contenidoUnidad.trim().lowercase() ||
                    codigoBarras.trim() != orig.codigo.trim() ||
                    (esMedicamento && requiereReceta != orig.requiereReceta) ||
                    (esMedicamento && esRefrigerado != origRefrig) ||
                    permiteFraccionar != orig.permiteFraccionar
        }
}
