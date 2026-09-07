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
            val precioCompra = docDouble(doc, "precioCompra", "costoCompra", "costoUnitario", "costo")
            val precioVenta = docDouble(doc, "precioVenta", "precio")
            val ubicacion = docString(doc, "ubicacion")
            val ubicacionSecundaria = docString(doc, "ubicacionSecundaria", "ubicacion2")
            val stockMinimo = docDouble(doc, "stockMinimo", "stockMinimoBase")
            val diasAlerta = (doc.getLong("diasAlertaVencimiento")?.toInt() ?: (doc.get("diasAlertaVencimiento") as? Number)?.toInt() ?: 90)
            val activo = docBoolean(doc, "activo", true)
            val permiteFraccionar = docBoolean(doc, "permiteFraccionar", false) || docBoolean(doc, "esFraccionable", false)
            val creadoPorUid = docString(doc, "creadoPor", "creadoPorUid", "auditCreatedByUid")
            val creadoEnMillis = try {
                doc.getTimestamp("creadoEn")?.toDate()?.time
                    ?: doc.getTimestamp("creadoEl")?.toDate()?.time
                    ?: (doc.getLong("creadoEnMillis") ?: (doc.get("creadoEnMillis") as? Number)?.toLong() ?: 0L)
            } catch (e: Exception) {
                0L
            }
            val unidadBase = docString(doc, "unidadBase", "empaque")
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
                    val costoComp = (v["costoCompra"] as? Number)?.toDouble()
                        ?: (v["costoTotal"] as? Number)?.toDouble()
                        ?: 0.0
                    val costoUnitRaw = (v["costoUnitario"] as? Number)?.toDouble()
                        ?: (v["costoCompraUnitario"] as? Number)?.toDouble()
                        ?: (v["costoUnitarioReal"] as? Number)?.toDouble()
                        ?: (v["costo"] as? Number)?.toDouble()
                        ?: 0.0
                    val costoUnit = if (costoUnitRaw > 0.0) {
                        costoUnitRaw
                    } else if (cant > 0.0 && costoComp > 0.0) {
                        costoComp / cant
                    } else if (precioCompra > 0.0) {
                        precioCompra
                    } else {
                        0.0
                    }
                    val costoCompFinal = if (costoComp > 0.0) costoComp else if (costoUnit > 0.0 && cant > 0.0) costoUnit * cant else 0.0
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
                        proveedorNombre = prov, proveedorId = provId, nroFactura = fact, costoUltimoIngreso = costoCompFinal, costoCompraUnitario = costoUnit,
                        fecha = fechaStr, createdAt = createdAtStr, loteId = loteIdVal,
                        noValorizado = v["noValorizado"] == true,
                        ventasRegistradas = (v["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                    )
                }
            }

            // Fallback de verdad: si no tiene mapa de lotes pero el documento registra stock físico > 0,
            // se sintetiza un lote base para no invisibilizar existencias ni distorsionar valor contable
            if (lotesMap.isEmpty()) {
                val docStock = docDouble(doc, "stock", "stockTotal", "totalStock", "stockDisponible", "stockFisico")
                if (docStock > 0.0) {
                    val vtoDoc = docString(doc, "vencimientoMasCercano", "vencimiento")
                    lotesMap["LOTE_BASE"] = LoteProducto(
                        numero = "LOTE-BASE",
                        loteId = "LOTE_BASE",
                        vencimiento = vtoDoc,
                        cantidad = docStock,
                        costoCompraUnitario = precioCompra,
                        costoUltimoIngreso = precioCompra * docStock
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

            if (presentacionesList.isEmpty()) {
                val presNombre = empaque.ifBlank { "Unidad" }
                val precioPres = if (precioVenta > 0.0) precioVenta else docDouble(doc, "precio", "precioUnitario")
                presentacionesList.add(
                    PresentacionProducto(
                        presentacionId = "PRES_PRINCIPAL",
                        nombre = presNombre,
                        empaque = empaque.ifBlank { "Unidad" },
                        cantidad = 1,
                        unidadMedida = unidadBase.ifBlank { "unidad" },
                        precioventa = precioPres,
                        codigoBarras = codigoBarras
                    )
                )
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

    // ---------- Parser canónico a PharmProduct (usado por la lista principal) ----------
    fun parseToPharm(doc: DocumentSnapshot): PharmProduct? {
        if (!doc.exists()) return null
        return try {
            val id = doc.id
            val nombre = docString(doc, "nombre")
            val principioActivo = docString(doc, "principioActivo")
            val codigoBarras = docString(doc, "codigoBarras", "codigo")
            val categoria = docString(doc, "categoriaNombre", "categoriaPrincipal")
            val laboratorio = docString(doc, "laboratorio")
            val empaque = docString(doc, "empaque")
            val medida = docString(doc, "medidaConcentracion", "concentracion")
            val requiereReceta = docBoolean(doc, "requiereReceta", false)
            val esRefrigerado = docBoolean(doc, "esRefrigerado", false) || (docString(doc, "temperaturaAlmacenamiento").contains("REFRIG", ignoreCase = true))
            val precioCompra = docDouble(doc, "precioCompra", "costoCompra", "costoUnitario", "costo")
            val ubicacion = docString(doc, "ubicacion")
            val activoPharm = docBoolean(doc, "activo", true)
            val permiteFraccionarPharm = docBoolean(doc, "permiteFraccionar", false) || docBoolean(doc, "esFraccionable", false)
            val estadoPharm = docString(doc, "estado").ifBlank { if (activoPharm) "ACTIVO" else "INACTIVO" }

            val lotesMap = mutableMapOf<String, LoteProducto>()
            val lotesRaw = doc.get("lotes") as? Map<*, *>
            lotesRaw?.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    val num = v["numero"] as? String ?: k.toString()
                    val venc = v["vencimiento"] as? String ?: ""
                    val cant = (v["cantidad"] as? Number)?.toDouble() ?: 0.0
                    val cantBloq = (v["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                    val loteIdVal = v["loteId"] as? String ?: FechaVencimientoHelper.llaveLote(num)
                    lotesMap[k.toString()] = LoteProducto(
                        numero = num, vencimiento = venc, cantidad = cant, cantidadBloqueada = cantBloq,
                        loteId = loteIdVal,
                        noValorizado = v["noValorizado"] == true
                    )
                }
            }

            if (lotesMap.isEmpty()) {
                val docStock = docDouble(doc, "stock", "stockTotal", "totalStock", "stockDisponible", "stockFisico")
                if (docStock > 0.0) {
                    val vtoDoc = docString(doc, "vencimientoMasCercano", "vencimiento")
                    lotesMap["LOTE_BASE"] = LoteProducto(
                        numero = "LOTE-BASE",
                        loteId = "LOTE_BASE",
                        vencimiento = vtoDoc,
                        cantidad = docStock
                    )
                }
            }

            val lotesList = lotesMap.values.toList()
            val stockDisponible = lotesList.sumOf { it.cantidad.coerceAtLeast(0.0) }
            val stockBloqueado = lotesList.sumOf { it.cantidadBloqueada.coerceAtLeast(0.0) }
            val totalFisico = stockDisponible + stockBloqueado

            val loteMasProximo = lotesList.filter { it.cantidad > 0.0 }
                .minByOrNull { FechaVencimientoHelper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE }
            val vencimientoCalculado = loteMasProximo?.vencimiento?.ifBlank { "—" } ?: docString(doc, "vencimientoMasCercano", "vencimiento").ifBlank { "—" }

            // Blindaje R3: si hay texto de vencimiento pero el parser da 0L/null, no apagues alertas — fuerza vencido
            val expiryTimestampRaw = FechaVencimientoHelper.timestampDeVencimiento(vencimientoCalculado)
            val expiryTimestamp = if (vencimientoCalculado != "—" && vencimientoCalculado.isNotBlank() && expiryTimestampRaw == 0L) 1L else expiryTimestampRaw
            val diasHastaVencerRaw = FechaVencimientoHelper.diasHastaVencer(vencimientoCalculado)
            val diasHastaVencer = diasHastaVencerRaw ?: if (vencimientoCalculado != "—" && vencimientoCalculado.isNotBlank()) -1 else null
            val stockMinimo = docDouble(doc, "stockMinimo", "stockMinimoBase")
            val stockFisico = stockDisponible.coerceAtLeast(0.0)
            val empaqueDisplay = empaque.ifBlank { "unidades" }
            val contentFactor = doc.get("contenido")?.toString()?.toDoubleOrNull()?.takeIf { it > 1.0 } ?: 1.0
            val contenidoUnidadDisp = docString(doc, "contenidoUnidad").ifBlank { null }
            val tieneStockFraccional = stockFisico > 0.0 && (stockFisico % 1.0) > 0.001 && contentFactor > 1.0 && contenidoUnidadDisp != null
            val stockInt = Math.round(stockFisico).toInt().coerceAtLeast(0)

            val stockHumanReadable: String = if (tieneStockFraccional) {
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
                proveedor = docString(doc, "proveedor", "proveedorNombre", "proveedorBaseNombre", "distribuidor"),
                proveedorId = docString(doc, "proveedorId")
            )
        } catch (e: Exception) {
            Log.e("ProductoParser", "Error mapeando Pharm ${doc.id}: ${e.message}")
            null
        }
    }
}
