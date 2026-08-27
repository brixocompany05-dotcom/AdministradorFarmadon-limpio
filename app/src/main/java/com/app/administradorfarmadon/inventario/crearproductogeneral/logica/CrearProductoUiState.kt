package com.app.administradorfarmadon.inventario.crearproductogeneral.logica

import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques

/**
 * Estados del ciclo de vida de guardado en el panel derecho (40%).
 */
enum class EstadoGuardadoProducto {
    IDLE,       // Ficha lista para interactuar y guardar
    GUARDANDO,  // Subiendo a la base de datos (animación de carga)
    EXITO,      // Guardado confirmado (animación de éxito + registrar otro / salir)
    ERROR       // Falla de red o servidor (animación de error + botón de reintento)
}

/**
 * Estado inmutable de la pantalla de Creación de Producto.
 * Inicia 100% limpio sin datos falsos ni valores pre-rellenados por defecto.
 */
data class CrearProductoUiState(
    val clienteId: String = "",
    val usuarioActualUid: String = "",
    val nombre: String = "",
    val nombreSugeridoCorregido: String = "", // Sugerencia ortográfica estricta devuelta por IA
    val principioActivo: String = "",
    val codigoBarras: String = "",
    val tipoProducto: String = "", // "" | "MEDICAMENTO" | "GENERAL"
    val categoriaId: String = "",
    val categoriaNombre: String = "", // 100% limpio al iniciar
    val laboratorio: String = "",
    val empaque: String = "", // 100% limpio al iniciar
    
    // Separación clara: Cantidad y Unidad
    val cantidadContenido: String = "",
    val unidadMedida: String = "", // 100% limpio al iniciar
    
    val variantesSugeridas: List<String> = emptyList(),
    val varianteSeleccionada: String = "", // Guarda la variante exacta seleccionada
    val requiereReceta: Boolean = false,
    val esRefrigerado: Boolean = false,
    val permiteFraccionar: Boolean = false,
    val clasificacionControl: String = "VENTA_LIBRE",
    
    // Control de flujo progresivo y cámara
    val formularioDesplegado: Boolean = false,
    val clasificandoIa: Boolean = false,
    val sugerenciaIaAplicada: Boolean = false,
    val esModoManual: Boolean = false,
    val usuarioModificoManualmente: Boolean = false,
    val mostrarCamaraScanner: Boolean = false,
    val productoExistenteDuplicado: Pair<String, String>? = null, // (productoId, nombreProducto) si el código ya existe
    
    // Ciclo de guardado en el panel derecho (40%)
    val estadoGuardado: EstadoGuardadoProducto = EstadoGuardadoProducto.IDLE,
    val productoGuardadoNombre: String = "",
    val productoGuardadoId: String? = null,
    val mensajeErrorGuardado: String? = null,
    val errores: Map<String, String> = emptyMap(),
    
    // Catálogos canónicos
    val categoriasDisponibles: List<String> = CatalogoEmpaques.CATEGORIAS_VALIDAS,
    val empaquesDisponibles: List<String> = CatalogoEmpaques.EMPAQUES_VALIDOS,
    val unidadesDisponibles: List<String> = CatalogoEmpaques.UNIDADES_MEDIDA_VALIDAS
) {
    /**
     * Medida combinada para la base de datos (ej: "500 mg", "1.5 Litros").
     */
    val medidaConcentracion: String
        get() {
            if (cantidadContenido.isBlank()) return ""
            return if (unidadMedida.isBlank()) cantidadContenido.trim() else "${cantidadContenido.trim()} $unidadMedida".trim()
        }

    /**
     * Determina si el producto es de naturaleza médica o tiene atributos clínicos activos.
     */
    val esMedicamento: Boolean
        get() = tipoProducto.equals("MEDICAMENTO", ignoreCase = true) ||
                requiereReceta ||
                esRefrigerado ||
                principioActivo.isNotBlank()
}
