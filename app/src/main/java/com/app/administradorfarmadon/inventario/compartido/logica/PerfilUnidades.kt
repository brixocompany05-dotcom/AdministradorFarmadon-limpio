package com.app.administradorfarmadon.inventario.compartido.logica

/**
 * FUENTE íšNICA de conversión matemática entre unidades de un mismo perfil físico.
 *
 * REGLA DE PRODUCTO (pedido del dueño): el usuario habla el idioma del producto
 * (litros, botellas, kg, cajas, tabletas); el sistema traduce TODO a la unidad base del
 * perfil para descontar stock con exactitud matemática, y le devuelve al usuario el
 * resultado en su idioma (nunca le muestra ml sueltos ni lo obliga a convertir).
 *
 * CUBRE TODAS LAS FAMILIAS de CatalogoEmpaques:
 *   Líquido (base = ml):  1 L = 1000 ml, 1 Galón ──‰ˆ 3785 ml, 1 cc = 1 ml
 *   Peso   (base = g):    1 kg = 1000 g, 1 g = 1000 mg, 1 mg = 1000 mcg, 1 lb ──‰ˆ 453.6 g, 1 oz ──‰ˆ 28.35 g
 *   Sólido / contados (base = unidad): Tab, Cáp, Sob, Und, Par, Dosis, Pch, UI = 1,
 *     y empaques usados como unidad de conteo (Caja, Blíster, Sobre, Tira, Paquete, Bulto) = 1
 *     dentro de su propia familia de conteo.
 *
 * Cualquier producto, sin importar si el usuario escribió "1.5 L", "1500 ml", "1 kg" o "1000 g",
 * el sistema lo normaliza a la unidad base y el consumo coincide física y matemáticamente.
 */
object PerfilUnidades {

    /** Factor para llevar la unidad a su unidad base del perfil (base = 1.0). */
    private val FACTOR_A_BASE = mapOf(
        // Líquido → ml
        "ml" to 1.0, "cc" to 1.0, "l" to 1000.0, "lt" to 1000.0, "litro" to 1000.0,
        "litros" to 1000.0, "galon" to 3785.0, "galón" to 3785.0, "got" to 0.05,
        "gotas" to 0.05,
        // Peso → g
        "g" to 1.0, "gr" to 1.0, "gramo" to 1.0, "gramos" to 1.0, "kg" to 1000.0,
        "kilogramo" to 1000.0, "kilogramos" to 1000.0, "mg" to 0.001,
        "miligramo" to 0.001, "miligramos" to 0.001, "mcg" to 0.000001,
        "microgramo" to 0.000001, "microgramos" to 0.000001,
        "lb" to 453.592, "libra" to 453.592, "libras" to 453.592,
        "oz" to 28.3495, "onza" to 28.3495, "onzas" to 28.3495,
        // Sólido / dosificados / contados → unidad (base 1.0)
        "tab" to 1.0, "tableta" to 1.0, "tabletas" to 1.0, "comprimido" to 1.0,
        "comprimidos" to 1.0, "pastilla" to 1.0, "pastillas" to 1.0,
        "cap" to 1.0, "cáp" to 1.0, "capsula" to 1.0, "cápsula" to 1.0,
        "cápsulas" to 1.0, "capsulas" to 1.0,
        "sob" to 1.0, "sobre" to 1.0, "sobres" to 1.0,
        "und" to 1.0, "unidad" to 1.0, "unidades" to 1.0, "u" to 1.0,
        "par" to 1.0, "pares" to 1.0,
        "docena" to 12.0, "docenas" to 12.0,
        "kit" to 1.0, "dosis" to 1.0, "pch" to 1.0, "parch" to 1.0, "parche" to 1.0,
        "ui" to 1.0, "iu" to 1.0,
        // Empaques usados como unidad de conteo (base 1.0 dentro de su familia de conteo)
        "caja" to 1.0, "blister" to 1.0, "blíster" to 1.0, "tira" to 1.0,
        "paquete" to 1.0, "bolsa" to 1.0, "bulto" to 1.0, "pote" to 1.0,
        "frasco" to 1.0, "botella" to 1.0, "tubo" to 1.0, "sachet" to 1.0,
        "inhalador" to 1.0, "ampolla" to 1.0, "spray" to 1.0, "lata" to 1.0,
        "bloque" to 1.0, "porción" to 1.0, "porcion" to 1.0, "tajada" to 1.0,
        "bomba" to 1.0
    )

    /** Devuelve el factor de la unidad a su base del perfil (1.0 si no se reconoce). */
    fun factorAUnidadBase(unidad: String): Double {
        val clave = unidad.trim().lowercase()
            .replace("ó", "o").replace("á", "a").replace("é", "e").replace("í", "i").replace("ú", "u")
        return FACTOR_A_BASE[clave] ?: 1.0
    }

    /**
     * Normaliza [cantidad] en [unidadOrigen] a la unidad [unidadDestino] del mismo perfil.
     * Si alguna unidad no se reconoce, devuelve la cantidad sin cambios (no inventa conversión).
     */
    fun normalizarA(cantidad: Double, unidadOrigen: String, unidadDestino: String): Double {
        if (unidadOrigen.isBlank() || unidadDestino.isBlank()) return cantidad
        if (unidadOrigen.equals(unidadDestino, ignoreCase = true)) return cantidad
        val baseOrigen = factorAUnidadBase(unidadOrigen)
        val baseDestino = factorAUnidadBase(unidadDestino)
        if (baseOrigen <= 0.0 || baseDestino <= 0.0) return cantidad
        // cantidad(en base origen) / base destino = cantidad en unidad destino
        return (cantidad * baseOrigen) / baseDestino
    }

    /**
     * Cantidad ya expresada en la unidad base del perfil (para cálculo interno exacto).
     * Ej: 100 ml → 100 (base ml); 1.5 L → 1500 (base ml); 1 kg → 1000 (base g).
     */
    fun aUnidadBase(cantidad: Double, unidad: String): Double {
        return cantidad * factorAUnidadBase(unidad)
    }
}
