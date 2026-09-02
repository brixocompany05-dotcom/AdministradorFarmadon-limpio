package com.app.administradorfarmadon.inventario.compartido.logica

import android.util.Log
import com.app.administradorfarmadon.inventario.compartido.modelo.EtiquetaPendienteItem
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Fuente única de verdad para traducir un DocumentSnapshot de inventario.
 * Antes había 3 traductores copiados (Lista, Detalles, Ingreso) con defaults distintos.
 * Ahora 1 solo lee el documento y los 3 lo usan. Menos código, mismo resultado, sin drift.
 *
 * No crea capas nuevas: solo centraliza la lectura. Si un campo cambia en Firestore,
 * se cambia aquí 1 vez y las 3 pantallas ven lo mismo.
 */
object ProductoParser {

    // ---------- Helpers robustos (toleran String/Long/Double/nulo) ----------
    private fun docString(doc: DocumentSnapshot, vararg keys: String, fallback: String = ""): String {
        for (k in keys) {
            val v = doc.getString(k)
            if (!v.isNullOrBlank()) return v
            // fallback si está como otro tipo (ej: contenido como Double)
            val raw = doc.get(k)
            if (raw is String && raw.isNotBlank()) return raw
        }
        return fallback
    }

    private fun docDouble(doc: DocumentSnapshot, vararg keys: String, fallback: Double = 0.0): Double {
        for (k in keys) {
            val d = doc.getDouble(k)
            if (d != null) return d
            val raw = doc.get(k) as? Number
            if (raw != null) return raw.toDouble()
        }
        return fallback
    }

    private fun docBoolean(doc: DocumentSnapshot, key: String, fallback: Boolean = false): Boolean {
        return doc.getBoolean(key) ?: fallback
    }

    // ---------- Parser canónico a MoldeProductos (usado por Detalles, Ingreso, Editar) ----------
    fun parseToMolde(doc: DocumentSnapshot): MoldeProductos? {
        if (!doc.exists()) return null
        return try {
            val id = doc.id
            val nombre = docString(doc, "nombre")
            val principioActivo = docString(doc, "principioActivo")
            val codigoBarras = docString(doc, "codigoBarras", "codigo")
            val categoria = docString(doc, "categoriaNombre", "categoriaPrincipal")
            // R12: el laboratorio JAMÁS se rellena con el proveedor; vacío honesto si no existe.
            val laboratorio = docString(doc, "laboratorio")
            val empaque = docString(doc, "empaque")
            val medida = docString(doc, "medidaConcentracion", "concentracion")
            val requiereReceta = docBoolean(doc, "requiereReceta", false)
            val precioCompra = docDouble(doc, "precioCompra")
            val precioVenta = docDouble(doc, "precioVenta", "precio")
            val ubicacion = docString(doc, "ubicacion")
            val ubicacionSecundaria = docString(doc, "ubicacionSecundaria", "ubicacion2")
            val stockMinimo = docDouble(doc, "stockMinimo", "stockMinimoBase")
            val diasAlerta = (doc.getLong("diasAlertaVencimiento")?.toInt() ?: (doc.get("diasAlertaVencimiento") as? Number)?.toInt() ?: 90)
            val activo = docBoolean(doc, "activo", true)
            val permiteFraccionar = docBoolean(doc, "permiteFraccionar", false) || docBoolean(doc, "esFraccionable", false)
            val creadoPorUid = docString(doc, "creadoPor", "creadoPorUid", "auditCreatedByUid")
            val creadoEnMillis = try { (doc.getTimestamp("creadoEn")?.toDate()?.time ?: 0L) } catch (e: Exception) { android.util.Log.w("ProductoParser", "creadoEn parse falló", e); 0L }
            val unidadBase = docString(doc, "unidadBase", "empaque")

            // Lotes —” tolera Map y List
            val lotesMap = mutableMapOf<String, LoteProducto>()
            val lotesRaw = doc.get("lotes") as? Map<*, *>
            lotesRaw?.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    val num = v["numero"] as? String ?: k.toString()
                    val venc = v["vencimiento"] as? String ?: ""
                    val cant = (v["cantidad"] as? Number)?.toDouble() ?: 0.0
                    val cantBloq = (v["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                    val prov = (v["proveedor"] as? String) ?: (v["proveedorNombre"] as? String) ?: ""
                    val provId = v["proveedorId"] as? String ?: ""
                    val fact = (v["factura"] as? String) ?: (v["nroFactura"] as? String) ?: ""
                    val costoComp = (v["costoCompra"] as? Number)?.toDouble() ?: 0.0
                    val costoUnit = (v["costoUnitario"] as? Number)?.toDouble() ?: 0.0
                    // Fecha de nacimiento real del lote (sin mentir): lee Timestamp o String
                    val fechaRaw = v["fechaIngreso"] ?: v["fecha"] ?: v["createdAt"] ?: v["ultimaEntrada"]
                    val fechaStr = when (fechaRaw) {
                        is Timestamp -> try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaRaw.toDate()) } catch (e: Exception) { android.util.Log.w("ProductoParser", "fechaRaw parse falló", e); "" }
                        is String -> fechaRaw.trim()
                        else -> ""
                    }
                    val createdAtStr = when (val ca = v["createdAt"]) {
                        is Timestamp -> try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ca.toDate()) } catch (e: Exception) { android.util.Log.w("ProductoParser", "createdAt parse falló", e); "" }
                        is String -> ca.trim()
                        else -> ""
                    }
                    val loteIdVal = v["loteId"] as? String ?: FechaVencimientoHelper.llaveLote(num)
                    lotesMap[k.toString()] = LoteProducto(
                        numero = num, vencimiento = venc, cantidad = cant, cantidadBloqueada = cantBloq,
                        proveedorNombre = prov, proveedorId = provId, nroFactura = fact, costoUltimoIngreso = costoComp, costoCompraUnitario = costoUnit,
                        fecha = fechaStr, createdAt = createdAtStr, loteId = loteIdVal,
                        ventasRegistradas = (v["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                    )
                }
            }

            val registroSanitario = docString(doc, "registroSanitario")
            val esRefrigerado = docBoolean(doc, "esRefrigerado", false) || (docString(doc, "temperaturaAlmacenamiento").contains("REFRIG", ignoreCase = true))
            val temperaturaAlmacenamiento = docString(doc, "temperaturaAlmacenamiento")
            val temperatura = if (temperaturaAlmacenamiento.isNotBlank()) temperaturaAlmacenamiento
                else if (esRefrigerado) "REFRIGERACION" else "AMBIENTE"
            val clasificacion = docString(doc, "clasificacionControl").ifBlank {
                if (requiereReceta) "CONTROLADO" else "VENTA_LIBRE"
            }

            // Presentaciones
            val presentacionesList = mutableListOf<PresentacionProducto>()
            val presRaw = doc.get("presentaciones") as? List<*>
            presRaw?.forEach { item ->
                if (item is Map<*, *>) {
                    val pId = item["presentacionId"] as? String ?: ""
                    val pNom = item["nombre"] as? String ?: ""
                    val pEmp = item["empaque"] as? String ?: ""
                    val pCant = (item["cantidad"] as? Number)?.toInt() ?: 1
                    val pUni = item["unidadMedida"] as? String ?: ""
                    val pPre = (item["precioventa"] as? Number)?.toDouble() ?: 0.0
                    val pCod = item["codigoBarras"] as? String ?: ""
                    val pCodigosAnteriores = (item["codigosAnteriores"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    presentacionesList.add(PresentacionProducto(pId, pNom, pEmp, pCant, pUni, pPre, pCod, pCodigosAnteriores))
                }
            }

            val codigosSecundarios = (doc.get("codigosSecundarios") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val etiquetaPendiente = docBoolean(doc, "etiquetaPendienteReimpresion", false)
            val etiquetaDetalle = docString(doc, "etiquetaPendienteDetalle")
            val etiquetaPresId = docString(doc, "etiquetaPendientePresentacionId")
            val etiquetaPrecio = docDouble(doc, "etiquetaPendientePrecio")
            val pendientesRaw = doc.get("etiquetasPendientesLista") as? List<*>
            val etiquetasPendientesLista = pendientesRaw?.mapNotNull { item ->
                if (item is Map<*, *>) EtiquetaPendienteItem(
                    presentacionId = item["presentacionId"] as? String ?: "",
                    nombre = item["nombre"] as? String ?: "",
                    precio = (item["precio"] as? Number)?.toDouble() ?: 0.0,
                    cantidad = (item["cantidad"] as? Number)?.toInt() ?: 1
                ) else null
            } ?: emptyList()
            val impresasPreviamente = docBoolean(doc, "etiquetasImpresasPreviamente", false) || doc.get("etiquetaUltimaImpresionEn") != null

            val contenido = doc.get("contenido")?.toString() ?: ""
            val contenidoUnidad = docString(doc, "contenidoUnidad")
            val concentracionUnidad = docString(doc, "concentracionUnidad")
            val sugerenciasEnvase = (doc.get("sugerenciasEnvase") as? List<*>)?.mapNotNull { it?.toString() }
                ?: CatalogoEmpaques.obtenerEmpaquesCompatibles(empaque, contenidoUnidad.ifBlank { unidadBase })
            val sugerenciasPerfil = (doc.get("sugerenciasPerfil") as? List<*>)?.mapNotNull { it?.toString() }
                ?: CatalogoEmpaques.obtenerUnidadesCompatibles(empaque, contenidoUnidad.ifBlank { unidadBase })

            MoldeProductos(
                indice = id, nombre = nombre, principioActivo = principioActivo, codigo = codigoBarras,
                categoriaPrincipal = categoria, categoriaNombre = categoria,
                laboratorio = laboratorio,
                proveedorBaseNombre = docString(doc, "proveedorBaseNombre"),
                empaque = empaque, contenido = contenido, contenidoUnidad = contenidoUnidad,
                concentracion = medida, concentracionUnidad = concentracionUnidad,
                sugerenciasEnvase = sugerenciasEnvase, sugerenciasPerfil = sugerenciasPerfil,
                requiereReceta = requiereReceta, precioCompra = precioCompra,
                ubicacion = ubicacion, ubicacionSecundaria = ubicacionSecundaria, stockMinimoBase = stockMinimo,
                registroSanitario = registroSanitario, temperaturaAlmacenamiento = temperatura,
                clasificacionControl = clasificacion, presentaciones = presentacionesList, lotes = lotesMap,
                presentacionPrincipalId = docString(doc, "presentacionPrincipalId"),
                activo = activo, diasAlertaVencimiento = diasAlerta, unidadBase = unidadBase,
                permiteFraccionar = permiteFraccionar, creadoPorUid = creadoPorUid, creadoEnMillis = creadoEnMillis,
                codigosSecundarios = codigosSecundarios, etiquetaPendienteReimpresion = etiquetaPendiente,
                etiquetaPendienteDetalle = etiquetaDetalle, etiquetaPendientePresentacionId = etiquetaPresId,
                etiquetaPendientePrecio = etiquetaPrecio, etiquetasPendientesLista = etiquetasPendientesLista,
                etiquetasImpresasPreviamente = impresasPreviamente
            ).apply {
                // Prioridad de venta (contrato del futuro POS) + auditoría de quién decidió.
                lotePrioritarioId = docString(doc, "lotePrioritarioId")
                lotePrioritarioPor = docString(doc, "lotePrioritarioPor")
                lotePrioritarioPorRol = docString(doc, "lotePrioritarioPorRol")
                fefoAutomatico = docBoolean(doc, "fefoAutomatico", true)
            }
        } catch (e: Exception) {
            Log.e("ProductoParser", "Error mapeando Molde ${doc.id}: ${e.message}")
            null
        }
    }

    // ---------- Parser a PharmProduct (Lista) —” mismo origen, misma lectura ----------
    fun parseToPharm(doc: DocumentSnapshot): PharmProduct? {
        if (!doc.exists()) return null
        return try {
            val id = doc.id
            val nombre = docString(doc, "nombre")
            val principioActivo = docString(doc, "principioActivo")
            val codigoBarras = docString(doc, "codigoBarras", "codigo")
            val categoria = docString(doc, "categoriaNombre", "categoriaPrincipal")
            // R12: el laboratorio JAMÁS se rellena con el proveedor; vacío honesto si no existe.
            val laboratorio = docString(doc, "laboratorio")
            val empaque = docString(doc, "empaque")
            val medida = docString(doc, "medidaConcentracion", "concentracion")
            val requiereReceta = docBoolean(doc, "requiereReceta", false)
            val esRefrigerado = docBoolean(doc, "esRefrigerado", false)
            val activoPharm = docBoolean(doc, "activo", true)
            val permiteFraccionarPharm = docBoolean(doc, "permiteFraccionar", false) || docBoolean(doc, "esFraccionable", false)
            val estadoPharm = docString(doc, "estado", fallback = if (activoPharm) "ACTIVO" else "PAUSADO")
            val precioCompra = docDouble(doc, "precioCompra")
            val ubicacion = docString(doc, "ubicacion")

            // R1/R3 —” La lista debe mostrar lo VENDIBLE, no el total físico.
            // Disponible = suma de la cantidad disponible de cada lote (excluye cuarentena/bloqueado).
            val lotesRaw = doc.get("lotes") as? Map<*, *>
            val stockDisponibleCalculado: Double? = lotesRaw?.values?.mapNotNull { it as? Map<*, *> }
                ?.sumOf { (it["cantidad"] as? Number)?.toDouble() ?: 0.0 }
            val stockBloqueadoCalculado: Double? = lotesRaw?.values?.mapNotNull { it as? Map<*, *> }
                ?.sumOf { (it["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0 }

            val stockDisponible = stockDisponibleCalculado ?: docDouble(doc, "stock")
            val stockBloqueado = stockBloqueadoCalculado ?: 0.0
            val totalFisico = stockDisponible + stockBloqueado

            // El vencimiento que importa para la lista es el del lote VENDIBLE (cantidad > 0),
            // nunca el de un lote en cuarentena que no se puede despachar. Se captura también
            // el NíšMERO del lote: el vencimiento pertenece a un lote, y las alertas deben decirlo.
            data class LoteProximoPharm(val numero: String, val vencimiento: String, val dias: Int)
            val loteMasProximo = lotesRaw?.values?.mapNotNull { m ->
                if (m is Map<*, *>) {
                    val cant = (m["cantidad"] as? Number)?.toDouble() ?: 0.0
                    val venc = m["vencimiento"] as? String ?: ""
                    val num = m["numero"] as? String ?: ""
                    val dias = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.diasHastaVencer(venc) ?: Int.MAX_VALUE
                    if (cant > 0 && venc.isNotBlank()) LoteProximoPharm(num, venc, dias) else null
                } else null
            }?.minByOrNull { it.dias }
            val vencimientoDesdeLotes = loteMasProximo?.vencimiento
            val vencimientoCalculado = if (lotesRaw != null) {
                vencimientoDesdeLotes ?: "—”"
            } else {
                docString(doc, "vencimientoMasCercano").ifBlank { "—”" }
            }

            // Blindaje R3: si hay texto de vencimiento pero el parser da 0L/null, no apagues alertas —” fuerza vencido
            val expiryTimestampRaw = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.timestampDeVencimiento(vencimientoCalculado)
            val expiryTimestamp = if (vencimientoCalculado != "—”" && vencimientoCalculado.isNotBlank() && expiryTimestampRaw == 0L) 1L else expiryTimestampRaw
            val diasHastaVencerRaw = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.diasHastaVencer(vencimientoCalculado)
            val diasHastaVencer = diasHastaVencerRaw ?: if (vencimientoCalculado != "—”" && vencimientoCalculado.isNotBlank()) -1 else null

            val stockMinimo = docDouble(doc, "stockMinimo", "stockMinimoBase")
            val stockFisico = stockDisponible.coerceAtLeast(0.0)
            val empaqueDisplay = empaque.ifBlank { "unidades" }
            val contentFactor = doc.get("contenido")?.toString()?.toDoubleOrNull()?.takeIf { it > 1.0 } ?: 1.0
            val contenidoUnidadDisp = docString(doc, "contenidoUnidad").ifBlank { null }
            val tieneStockFraccional = stockFisico > 0.0 && (stockFisico % 1.0) > 0.001 && contentFactor > 1.0 && contenidoUnidadDisp != null
            // ── UNIDAD íšNICA DEL NíšMERO LÓGICO (R3): SIEMPRE unidades físicas (Cajas). ──
            // REDONDEO al entero más cercano: 2.994 cajas se MUESTRA y se COMPARA como 3,
            // igual a lo que el humano cuenta en el estante. Un solo número en todas las
            // pantallas; el texto rico ("2 Cajas + 149 Tab") vive solo en stockHumanReadable.
            val stockInt = Math.round(stockFisico).toInt().coerceAtLeast(0)

            val stockHumanReadable: String = if (tieneStockFraccional) {
                // Lógica humana: lo que el cajero ve en el estante = N envases enteros + lo que queda
                // de un envase abierto, expresado en la unidad de contenido (ej: "4 Bot + 1.4 L").
                // NUNCA un número abstracto de ml sueltos ("7400 ml") ni decimales de envase ("4.933").
                val enteras = stockFisico.toInt().coerceAtLeast(0)
                val restoEnvase = (stockFisico - enteras).coerceAtLeast(0.0)
                val restoContenido = restoEnvase * contentFactor
                val restoTexto = if (restoContenido >= 1000.0) {
                    "${String.format(java.util.Locale.US, "%.1f", restoContenido / 1000.0)} L"
                } else {
                    "${restoContenido.toLong().coerceAtLeast(0)} $contenidoUnidadDisp"
                }
                "$enteras $empaqueDisplay + $restoTexto"
            } else {
                "$stockInt $empaqueDisplay"
            }

            val stockBloqueadoInt = stockBloqueado.toInt().coerceAtLeast(0)
            val stockMinimoInt = stockMinimo.toInt()
            val hayCuarentena = stockBloqueadoInt > 0

            val precioPrincipal = docDouble(doc, "precioVenta", "precio")
            val status = when {
                // Si no hay nada vendible y todo está bloqueado → cuarentena.
                stockInt <= 0 && hayCuarentena -> "En cuarentena"
                stockInt <= 0 -> "Agotado"
                diasHastaVencer != null && diasHastaVencer < 0 -> "Vencido"
                diasHastaVencer != null && diasHastaVencer <= 30 -> "Por vencer"
                // Producto con stock pero sin precio configurado: el cajero no puede venderlo.
                precioPrincipal <= 0.0 -> "Sin precio"
                stockMinimo > 0 && stockFisico <= stockMinimo -> "Stock bajo"
                else -> "Disponible"
            }

            val presentacionFormateada = when {
                empaque.isNotBlank() && medida.isNotBlank() -> "$empaque · $medida"
                empaque.isNotBlank() -> empaque
                medida.isNotBlank() -> medida
                else -> "—”"
            }
            val codigosSecundarios = (doc.get("codigosSecundarios") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val unidadReal = docString(doc, "contenidoUnidad", "unidadBase").ifBlank { "unidades" }
            val unidadSufijo = " $unidadReal"

            val creadoEnMillisPharm = try {
                (doc.getTimestamp("creadoEn") ?: doc.getTimestamp("actualizadoEn"))?.toDate()?.time ?: 0L
            } catch (e: Exception) {
                android.util.Log.w("ProductoParser", "creadoEn parse falló en Pharm", e); 0L
            }

            PharmProduct(
                id = id, name = nombre, presentation = presentacionFormateada,
                inventoryVisualSummary = listOfNotNull(
                    principioActivo.ifBlank { null },
                    empaque.ifBlank { null },
                    if (hayCuarentena) "$stockBloqueadoInt en cuarentena" else null
                ).joinToString("  ·  "),
                stockHumanReadable = stockHumanReadable, stockBaseReadable = "$stockInt",
                minStockHumanReadable = "$stockMinimoInt ${empaqueDisplay.let { if (stockMinimoInt == 1) it else if (it.endsWith("s", ignoreCase = true)) it else "${it}s" }}",
                nearestLoteNumero = loteMasProximo?.numero ?: "",
                code = codigoBarras, laboratory = laboratorio,
                category = categoria, categories = listOf(categoria), empaque = empaque,
                stock = stockInt, stockBloqueado = stockBloqueadoInt, minStock = stockMinimoInt,
                expiryDate = vencimientoCalculado,
                expiryTimestamp = expiryTimestamp,
                status = status, purchasePrice = precioCompra, salePrice = precioPrincipal, totalValue = totalFisico * precioCompra,
                clasificacionControl = docString(doc, "clasificacionControl").ifBlank {
                    if (requiereReceta) "RECETA_MEDICA" else "VENTA_LIBRE"
                },
                requiereRefrigeracion = esRefrigerado, controlReceta = requiereReceta,
                ubicacion = ubicacion, concentration = medida,
                content = doc.get("contenido")?.toString()?.ifBlank { null } ?: medida,
                contentUnit = docString(doc, "contenidoUnidad").ifBlank { unidadReal },
                createdAtTimestamp = creadoEnMillisPharm,
                secondaryCodes = codigosSecundarios,
                activo = activoPharm,
                permiteFraccionar = permiteFraccionarPharm,
                estado = estadoPharm,
                proveedor = docString(doc, "proveedor", "proveedorNombre", "proveedorBaseNombre", "distribuidor")
            )
        } catch (e: Exception) {
            Log.e("ProductoParser", "Error mapeando Pharm ${doc.id}: ${e.message}")
            null
        }
    }
}
