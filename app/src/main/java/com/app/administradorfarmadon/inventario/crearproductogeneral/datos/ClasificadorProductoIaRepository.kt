package com.app.administradorfarmadon.inventario.crearproductogeneral.datos

import android.util.Log
import com.app.administradorfarmadon.BuildConfig
import com.app.administradorfarmadon.compartido.ia.DeepSeekApi
import com.app.administradorfarmadon.compartido.ia.DeepSeekChatRequest
import com.app.administradorfarmadon.compartido.ia.DeepSeekMessage
import com.app.administradorfarmadon.compartido.ia.DeepSeekResponseFormat
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Sugerencia estructurada devuelta por la IA sin inventos ni heurísticas locales falsas.
 * Cubre medicamentos y todo el catálogo de mostrador y retail de una farmacia real.
 * Emplea nomenclatura canónica abreviada (ml, L, mg, g, kg, Tab, Cáp, Und) para evitar saltos de línea feos.
 */
data class ProductoIaSugerencia(
    val nombreCorregido: String = "",
    val tipoProducto: String = "MEDICAMENTO", // "MEDICAMENTO" | "GENERAL"
    val categoriaNombre: String = "General",
    val principioActivo: String = "",
    val laboratorio: String = "",
    val empaque: String = "Caja",
    val cantidadContenido: String = "", // "500", "1.5", "400", "100", etc.
    val unidadMedida: String = "",       // "ml", "L", "mg", "g", "kg", "Cáp", "Tab", etc.
    val variantesSugeridas: List<String> = emptyList(),
    val requiereReceta: Boolean = false,
    val esRefrigerado: Boolean = false,
    val permiteFraccionar: Boolean = false,
    val clasificacionControl: String = "VENTA_LIBRE", // "PSICOTROPICO" | "CONTROLADO" | "ANTIBIOTICO" | "VENTA_LIBRE"
    val exitoIa: Boolean = true
) {
    val medidaConcentracion: String
        get() = if (cantidadContenido.isBlank()) "" else if (unidadMedida.isBlank()) cantidadContenido else "$cantidadContenido $unidadMedida".trim()
}

private data class AiProductJsonResponse(
    @field:Json(name = "nombreCorregido") val nombreCorregido: String = "",
    @field:Json(name = "tipoProducto") val tipoProducto: String = "MEDICAMENTO",
    @field:Json(name = "categoriaNombre") val categoriaNombre: String = "General",
    @field:Json(name = "principioActivo") val principioActivo: String = "",
    @field:Json(name = "laboratorio") val laboratorio: String = "",
    @field:Json(name = "empaque") val empaque: String = "Caja",
    @field:Json(name = "cantidadContenido") val cantidadContenido: String = "",
    @field:Json(name = "unidadMedida") val unidadMedida: String = "",
    @field:Json(name = "variantesSugeridas") val variantesSugeridas: List<String> = emptyList(),
    @field:Json(name = "requiereReceta") val requiereReceta: Boolean = false,
    @field:Json(name = "esRefrigerado") val esRefrigerado: Boolean = false,
    @field:Json(name = "permiteFraccionar") val permiteFraccionar: Boolean = false,
    @field:Json(name = "clasificacionControl") val clasificacionControl: String = "VENTA_LIBRE"
)

object ClasificadorProductoIaRepository {

    private const val TAG = "ClasificadorProductoIA"
    private const val DEEPSEEK_MODEL = "deepseek-chat"
    private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: DeepSeekApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(3500, TimeUnit.MILLISECONDS)
            .readTimeout(4000, TimeUnit.MILLISECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(DEEPSEEK_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeepSeekApi::class.java)
    }

    suspend fun clasificarProducto(nombreRaw: String): ProductoIaSugerencia {
        val q = nombreRaw.trim()
        if (q.isBlank()) return ProductoIaSugerencia(exitoIa = false)

        val apiKey = BuildConfig.DEEPSEEK_API_KEY
        if (apiKey.isNotBlank()) {
            try {
                val systemPrompt = """
                    Eres el asistente experto de inventario y catálogo para Farmacias comerciales (2026).
                    Las farmacias venden MEDICAMENTOS y PRODUCTOS DE MOSTRADOR / RETAIL (bebidas, snacks, champús, cremas, pañales, cuidado dental, fórmulas infantiles, primeros auxilios, aseo personal).
                    Tu misión es clasificar el producto ingresado con PRECISIÓN QUIRíšRGICA Y NOMENCLATURA CANÓNICA ABREVIADA.
                    
                    REGLAS OBLIGATORIAS:
                    1. 'nombreCorregido': Corrige íšNICAMENTE las faltas ortográficas, acentuación o tipeo del texto exacto que el usuario escribió (ej: "sprite" -> "Sprite", "amoxisilina 500mg" -> "Amoxicilina 500mg", "champu pantene" -> "Shampoo Pantene", "coca cola" -> "Coca Cola"). PROHIBIDO agregar presentaciones o palabras no escritas al nombre.
                    2. 'tipoProducto': "MEDICAMENTO" (fármacos, jarabes medicinales, gotas oftálmicas, inyectables, antibióticos, analgésicos) o "GENERAL" (gaseosas, aguas, snacks, champús, jabones, pastas dentales, pañales, bloqueadores, desodorantes, fórmulas infantiles).
                    3. 'categoriaNombre': Debe ser exactamente una de estas categorías oficiales:
                       - "Analgésicos & Antipiréticos"
                       - "Antibióticos & Antimicrobianos"
                       - "Antiinflamatorios"
                       - "Antigripales & Respiratorio"
                       - "Gastroenterología & Digestivo"
                       - "Cardiología & Hipertensión"
                       - "Dermatología & Piel"
                       - "Vitaminas & Suplementos"
                       - "Cuidado Personal & Belleza"
                       - "Bebés & Maternidad"
                       - "Bebidas & Snacks"
                       - "Primeros Auxilios & Botiquín"
                       - "General"
                    4. 'laboratorio': Fabricante o marca real conocida:
                       - Para Sprite / Coca Cola -> "The Coca-Cola Company"
                       - Para Pepsi / Gatorade / Doritos / Lays -> "PepsiCo"
                       - Para Pantene / Head & Shoulders / Gillette / Pampers / Oral-B -> "Procter & Gamble (P&G)"
                       - Para Sedal / Dove / Rexona / Axe -> "Unilever"
                       - Para Colgate / Protex / Palmolive -> "Colgate-Palmolive"
                       - Para Nivea / Eucerin -> "Beiersdorf"
                       - Para medicamentos -> Fabricante farmacéutico real (ej: "Genfar", "Bagó", "Pfizer", "Bayer", "GSK", "Sanofi", "Roemmers", "Genérico").
                    5. 'empaque': Debe ser el envase físico real comercial, exactamente uno de los 17 oficiales:
                       - "Caja" (pastillas en caja, cremas con caja, pañales en caja, curitas)
                       - "Frasco" (cápsulas en frasco, vitaminas, suspensiones, jarabes)
                       - "Botella" (gaseosas, aguas, champús, enjuagues, alcohol, lociones)
                       - "Tubo" (pastas dentales, cremas, pomadas, geles)
                       - "Blíster" (láminas de pastillas o cápsulas)
                       - "Ampolla" (inyectables líquidos, ampollas bebibles)
                       - "Sobre" (polvos efervescentes, sales de rehidratación, granulados)
                       - "Gotero" (gotas oftálmicas, nasales, pediátricas)
                       - "Spray" (desodorantes aerosol, sprays nasales, antisépticos)
                       - "Lata" (gaseosas en lata, energizantes, fórmulas infantiles en lata)
                       - "Bolsa" (pañales en paquete bolsa, toallas higiénicas, algodón)
                       - "Pote" (cremas en pote, vaselina, mascarillas capilares)
                       - "Inhalador" (inhaladores de dosis respiratoria)
                       - "Sachet" (sachets individuales de crema, shampoo monodosis)
                       - "Tira" (tiras reactivas, apósitos continuos)
                       - "Paquete" (pañales, toallitas húmedas, jabones en pack)
                       - "Unidad" (cepillos dentales, termómetros, jeringas, dispositivos)
                    6. 'cantidadContenido' y 'unidadMedida':
                       - ¡PROHIBIDO poner "1" o conteos genéricos como contenido cuando el producto tiene volumen, peso o dosis!
                       - Usa SIEMPRE las unidades canónicas abreviadas oficiales: "ml", "L", "mg", "g", "kg", "mcg", "Tab", "Cáp", "Sob", "Und", "Par", "Dosis", "Got".
                       - Coloca el valor numérico y la unidad abreviada de la presentación comercial más común en farmacias si el usuario no especificó tamaño:
                         * Bebidas (gaseosas, aguas): "500" con "ml" (o "1.5" con "L")
                         * Champús, acondicionadores, enjuagues: "400" con "ml"
                         * Desodorantes en barra / cremas: "50" con "g" o "150" con "ml" (spray)
                         * Pastas dentales: "75" con "ml" o "90" con "g"
                         * Jabones en barra: "120" con "g"
                         * Fórmulas infantiles en lata: "800" con "g"
                         * Pañales / Toallitas húmedas: "36" con "Und" (o "80" con "Und")
                         * Medicamentos sólidos (pastillas): dosis "500" con "mg", "400" con "mg", "1" con "g"
                         * Jarabes / Gotas: "120" con "ml", "15" con "ml"
                         * Alcohol / Antisépticos: "500" con "ml", "1" con "L"
                    7. 'variantesSugeridas': Lista de 3 a 5 presentaciones comerciales reales del mercado (con unidades abreviadas):
                       - Ejemplos Sprite: ["500 ml (Botella)", "1.5 L (Botella)", "355 ml (Lata)", "2 L (Botella)", "3 L (Botella)"]
                       - Ejemplos Shampoo Pantene: ["400 ml", "200 ml", "750 ml", "1 L"]
                       - Ejemplos Ibuprofeno: ["400 mg (Caja x 100)", "600 mg (Caja x 50)", "800 mg (Caja x 20)", "100 mg / 5ml (Jarabe 120ml)"]
                       - Ejemplos Nan 1: ["800 g (Lata)", "400 g (Lata)", "1.2 kg (Caja)"]
                       - Ejemplos Pampers: ["Talla G (x 36 Und)", "Talla M (x 40 Und)", "Talla XG (x 32 Und)"]
                       - Ejemplos Alcohol 70°: ["500 ml", "1 L", "250 ml", "1 Galón"]
                    8. 'principioActivo': Solo si es MEDICAMENTO (ej. "Ibuprofeno", "Amoxicilina"). Vacío ("") si es GENERAL.
                    9. 'requiereReceta': true solo si es medicamento ético con receta obligatoria o antibiótico/controlado. false si es venta libre (OTC) o GENERAL.
                    10. 'esRefrigerado': true solo si requiere cadena de frío (2°C-8°C). false para todo lo demás.
                    11. 'clasificacionControl': "VENTA_LIBRE", "ANTIBIOTICO", "PSICOTROPICO", o "CONTROLADO".
                    12. 'permiteFraccionar': true si el producto es a granel o se vende suelto (vino casero, queso, aceite a granel, pan), false si es sellado y no se fracciona (Coca Cola 300ml, lata, caja sellada). Por defecto false.

                    Responde íšNICAMENTE en JSON válido con este formato:
                    {
                      "nombreCorregido": "Sprite",
                      "tipoProducto": "GENERAL",
                      "categoriaNombre": "Bebidas & Snacks",
                      "principioActivo": "",
                      "laboratorio": "The Coca-Cola Company",
                      "empaque": "Botella",
                      "cantidadContenido": "500",
                      "unidadMedida": "ml",
                      "variantesSugeridas": ["500 ml (Botella)", "1.5 L (Botella)", "355 ml (Lata)", "2 L (Botella)", "3 L (Botella)"],
                      "requiereReceta": false,
                      "esRefrigerado": false,
                      "permiteFraccionar": false,
                      "clasificacionControl": "VENTA_LIBRE"
                    }
                """.trimIndent()

                val request = DeepSeekChatRequest(
                    model = DEEPSEEK_MODEL,
                    messages = listOf(
                        DeepSeekMessage("system", systemPrompt),
                        DeepSeekMessage("user", "Clasifica este producto para el inventario: \"$q\"")
                    ),
                    temperature = 0.1,
                    responseFormat = DeepSeekResponseFormat("json_object")
                )

                val response = api.createChatCompletion("Bearer $apiKey", request)
                val rawContent = response.body()?.choices?.firstOrNull()?.message?.content ?: ""

                val firstBrace = rawContent.indexOf('{')
                val lastBrace = rawContent.lastIndexOf('}')
                val cleanedJson = if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
                    rawContent.substring(firstBrace, lastBrace + 1).trim()
                } else {
                    rawContent
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                }

                if (cleanedJson.isNotBlank()) {
                    val parsed = moshi.adapter(AiProductJsonResponse::class.java).fromJson(cleanedJson)
                    if (parsed != null) {
                        val empaqueValido = CatalogoEmpaques.normalizarEmpaque(parsed.empaque)
                        val categoriaValida = CatalogoEmpaques.normalizarCategoria(parsed.categoriaNombre)
                        val unidadValida = CatalogoEmpaques.normalizarUnidad(parsed.unidadMedida)
                        val esControl = parsed.clasificacionControl.uppercase() in listOf("PSICOTROPICO", "CONTROLADO", "ESTUPEFACIENTE", "ANTIBIOTICO")

                        return ProductoIaSugerencia(
                            nombreCorregido = parsed.nombreCorregido.trim(),
                            tipoProducto = if (parsed.tipoProducto.uppercase() == "GENERAL") "GENERAL" else "MEDICAMENTO",
                            categoriaNombre = categoriaValida,
                            principioActivo = parsed.principioActivo.trim(),
                            laboratorio = parsed.laboratorio.trim(),
                            empaque = empaqueValido,
                            cantidadContenido = parsed.cantidadContenido.trim(),
                            unidadMedida = unidadValida,
                            variantesSugeridas = parsed.variantesSugeridas.filter { it.isNotBlank() },
                            requiereReceta = parsed.requiereReceta || esControl,
                            esRefrigerado = parsed.esRefrigerado,
                            permiteFraccionar = parsed.permiteFraccionar,
                            clasificacionControl = parsed.clasificacionControl.uppercase(),
                            exitoIa = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Clasificación IA no disponible o timeout (${e.message}). Activando modo manual limpio sin bloquear al usuario.")
            }
        }

        // Respaldo honesto en modo manual
        return ProductoIaSugerencia(
            nombreCorregido = q,
            tipoProducto = "MEDICAMENTO",
            categoriaNombre = "General",
            principioActivo = "",
            laboratorio = "",
            empaque = "Caja",
            cantidadContenido = "",
            unidadMedida = "",
            variantesSugeridas = emptyList(),
            requiereReceta = false,
            esRefrigerado = false,
            permiteFraccionar = false,
            clasificacionControl = "VENTA_LIBRE",
            exitoIa = false
        )
    }
}
