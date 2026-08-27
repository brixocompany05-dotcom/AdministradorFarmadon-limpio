package com.app.administradorfarmadon.inventario.crearproductogeneral.datos

/**
 * Catálogo canónico de empaques comerciales y unidades de medida estándar en Farmadon (Enterprise SaaS).
 * Fuente única de verdad física y comercial para Crear Producto, Editar Producto y Precios/Presentaciones.
 */
object CatalogoEmpaques {

    enum class FamiliaFisica(
        val etiqueta: String,
        val empaquesCompatibles: List<String>,
        val unidadesCompatibles: List<String>,
        val unidadKardexSugerida: String
    ) {
        SOLIDO_ORAL(
            etiqueta = "Sólidos Orales / Dosificados",
            empaquesCompatibles = listOf("Caja", "Blíster", "Sobre", "Tira", "Frasco", "Unidad"),
            unidadesCompatibles = listOf("Tab", "Cáp", "Sob", "mg", "g", "Und"),
            unidadKardexSugerida = "Tab"
        ),
        LIQUIDO_VOLUMEN(
            etiqueta = "Líquidos / Volumen",
            empaquesCompatibles = listOf("Frasco", "Botella", "Gotero", "Ampolla", "Spray", "Caja", "Sachet", "Bolsa"),
            unidadesCompatibles = listOf("ml", "L", "Got"),
            unidadKardexSugerida = "ml"
        ),
        TOPICOS_CREMAS(
            etiqueta = "Tópicos / Semisólidos",
            empaquesCompatibles = listOf("Tubo", "Pote", "Sachet", "Frasco", "Caja"),
            unidadesCompatibles = listOf("g", "ml", "mg"),
            unidadKardexSugerida = "g"
        ),
        AEROSOLES(
            etiqueta = "Aerosoles / Inhaladores",
            empaquesCompatibles = listOf("Inhalador", "Spray", "Lata", "Frasco", "Caja"),
            unidadesCompatibles = listOf("Dosis", "ml"),
            unidadKardexSugerida = "Dosis"
        ),
        PESO_MASA(
            etiqueta = "Peso / Granel / Polvos",
            empaquesCompatibles = listOf("Pote", "Lata", "Bolsa", "Caja", "Paquete"),
            unidadesCompatibles = listOf("g", "kg", "mg", "mcg"),
            unidadKardexSugerida = "g"
        ),
        INSUMOS_PIEZAS(
            etiqueta = "Insumos / Conteo Unitario",
            empaquesCompatibles = listOf("Caja", "Paquete", "Bolsa", "Tira", "Unidad"),
            unidadesCompatibles = listOf("Und", "Par"),
            unidadKardexSugerida = "Und"
        )
    }

    // 17 Empaques Comerciales Oficiales de Farmacia y Retail (Sin términos extraños)
    val EMPAQUES_VALIDOS: List<String> = listOf(
        "Caja",
        "Frasco",
        "Botella",
        "Tubo",
        "Blíster",
        "Ampolla",
        "Sobre",
        "Gotero",
        "Spray",
        "Lata",
        "Bolsa",
        "Pote",
        "Inhalador",
        "Sachet",
        "Tira",
        "Paquete",
        "Unidad"
    )

    val CATEGORIAS_VALIDAS: List<String> = listOf(
        "Analgésicos & Antipiréticos",
        "Antibióticos & Antimicrobianos",
        "Antiinflamatorios",
        "Antigripales & Respiratorio",
        "Gastroenterología & Digestivo",
        "Cardiología & Hipertensión",
        "Dermatología & Piel",
        "Vitaminas & Suplementos",
        "Cuidado Personal & Belleza",
        "Bebés & Maternidad",
        "Bebidas & Snacks",
        "Primeros Auxilios & Botiquín",
        "General"
    )

    // Unidades de Medida Comerciales Oficiales
    val UNIDADES_MEDIDA_VALIDAS: List<String> = listOf(
        "Tab",
        "Cáp",
        "Sob",
        "Und",
        "Par",
        "ml",
        "L",
        "Got",
        "mg",
        "g",
        "kg",
        "mcg",
        "UI",
        "Dosis"
    )

    fun normalizarEmpaque(raw: String): String {
        val limpio = raw.trim().lowercase()
        return EMPAQUES_VALIDOS.firstOrNull { it.lowercase() == limpio } ?: "Caja"
    }

    fun normalizarCategoria(raw: String): String {
        val limpio = raw.trim().lowercase()
        return CATEGORIAS_VALIDAS.firstOrNull { it.lowercase() == limpio } ?: "General"
    }

    fun normalizarUnidad(raw: String): String {
        val limpio = raw.trim().lowercase()
        return when {
            limpio == "ml" || limpio.startsWith("mili") || limpio == "cc" -> "ml"
            limpio == "l" || limpio == "lt" || limpio.startsWith("litro") -> "L"
            limpio == "mg" || limpio.startsWith("miligr") -> "mg"
            limpio == "g" || limpio == "gr" || limpio.startsWith("gram") -> "g"
            limpio == "kg" || limpio.startsWith("kilo") -> "kg"
            limpio == "mcg" || limpio == "ug" || limpio.startsWith("micro") -> "mcg"
            limpio == "ui" || limpio.contains("internacional") -> "UI"
            limpio.contains("tab") || limpio.contains("comprim") || limpio.contains("past") -> "Tab"
            limpio.contains("cap") || limpio.contains("cáp") -> "Cáp"
            limpio.contains("sob") -> "Sob"
            limpio.contains("par") -> "Par"
            limpio.contains("dosis") || limpio.contains("puff") -> "Dosis"
            limpio.contains("und") || limpio.contains("unid") || limpio == "u" -> "Und"
            limpio.contains("got") -> "Got"
            else -> UNIDADES_MEDIDA_VALIDAS.firstOrNull { it.lowercase() == limpio } ?: ""
        }
    }

    fun detectarFamiliaFisica(empaque: String, unidad: String): FamiliaFisica {
        val u = normalizarUnidad(unidad).lowercase()
        val e = normalizarEmpaque(empaque).lowercase()

        return when {
            e in listOf("tubo", "sachet", "pote") && u in listOf("g", "mg", "ml") -> FamiliaFisica.TOPICOS_CREMAS
            e in listOf("inhalador", "spray") || u == "dosis" -> FamiliaFisica.AEROSOLES
            u in listOf("tab", "cáp", "sob") || e in listOf("blíster", "sobre") -> FamiliaFisica.SOLIDO_ORAL
            u in listOf("ml", "l", "got") || e in listOf("botella", "frasco", "gotero", "ampolla") -> FamiliaFisica.LIQUIDO_VOLUMEN
            u in listOf("g", "kg", "mcg") -> FamiliaFisica.PESO_MASA
            u in listOf("par", "und") || e in listOf("paquete", "bolsa", "tira", "unidad") -> FamiliaFisica.INSUMOS_PIEZAS
            else -> FamiliaFisica.SOLIDO_ORAL
        }
    }

    fun obtenerEmpaquesPorUnidad(unidad: String): List<String> {
        val u = normalizarUnidad(unidad)
        if (u.isBlank()) return EMPAQUES_VALIDOS
        val familia = FamiliaFisica.entries.firstOrNull { fam -> fam.unidadesCompatibles.any { it.equals(u, ignoreCase = true) } }
        return familia?.empaquesCompatibles ?: EMPAQUES_VALIDOS
    }

    fun obtenerUnidadesPorEmpaque(empaque: String): List<String> {
        val e = normalizarEmpaque(empaque)
        if (e.isBlank()) return UNIDADES_MEDIDA_VALIDAS
        val familia = FamiliaFisica.entries.firstOrNull { fam -> fam.empaquesCompatibles.any { it.equals(e, ignoreCase = true) } }
        return familia?.unidadesCompatibles ?: UNIDADES_MEDIDA_VALIDAS
    }

    fun obtenerEmpaquesCompatibles(empaque: String, unidad: String): List<String> {
        val familia = detectarFamiliaFisica(empaque, unidad)
        return familia.empaquesCompatibles
    }

    fun obtenerUnidadesCompatibles(empaque: String, unidad: String): List<String> {
        val familia = detectarFamiliaFisica(empaque, unidad)
        return familia.unidadesCompatibles
    }

    fun separarContenidoYUnidad(raw: String): Pair<String, String> {
        if (raw.isBlank()) return Pair("", "")
        val limpio = raw.trim()
        val regex = Regex("""^(\d+(?:[.,]\d+)?)\s*(.*)$""")
        val match = regex.find(limpio)
        if (match != null) {
            val cant = match.groupValues[1]
            val unitRaw = match.groupValues[2].trim()
            if (unitRaw.isBlank()) return Pair(cant, "")

            val unitNormalizada = normalizarUnidad(unitRaw)
            return Pair(cant, unitNormalizada)
        }
        return Pair(limpio, "")
    }

    fun esCategoriaMedica(categoria: String): Boolean {
        return when (normalizarCategoria(categoria)) {
            "Cuidado Personal & Belleza", "Bebés & Maternidad", "Bebidas & Snacks", "General" -> false
            else -> true
        }
    }
}
