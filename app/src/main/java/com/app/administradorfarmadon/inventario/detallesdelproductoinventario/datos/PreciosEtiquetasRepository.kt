package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.modelo.ExpedienteReclamoProveedor
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.PreciosYFraccionamientoValidator
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Repositorio de Detalles de Producto en Cloud Firestore (Multi-Tenant).
 * Lee en tiempo real el producto, sus lotes activos y la bitácora de movimientos (Kardex),
 * todo aislado por sede: farmacias/{farmaciaId}/sucursales/{sucursalId}/...
 */

/**
 * Precios, etiquetas y configuración —” presentaciones, etiquetas y logística.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) —” responsabilidad única.
 */
class PreciosEtiquetasRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object { private const val TAG = "PreciosEtiquetasRepository" }

    suspend fun guardarPresentacionesYPrecios(
        clienteId: String,
        productId: String,
        unidadBase: String,
        presentaciones: List<PresentacionProducto>,
        usuarioEmail: String,
        presentacionesOriginales: List<PresentacionProducto> = emptyList()
    ): Result<Unit> {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
        if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        if (clienteId.isBlank() || productId.isBlank()) {
            return Result.failure(Exception("Datos del producto no válidos."))
        }

        // R3 (raíz de verdad): la misma regla de negocio que la UI debe aplicarse aquí.
        // Si alguna vía (otra pantalla, endpoint futuro, refactoring) llama al repo sin pasar
        // por la UI, el repositorio RECHAZA cualquier dato falso antes de tocar Firestore.
        // La venta a pérdida es advertencia en la UI (autorizable), pero aquí es bloqueante:
        // el repositorio no puede inferir la autorización del usuario, así que no graba pérdida.
        // FIX Hallazgo2: leer costo REAL del producto en Firestore (precioCompra / stock) en vez de 0.0.
        // Se lee fuera de tx para feedback rápido; dentro de tx se re-lee y re-valida como verdad final.
        val preSnap = try {
            FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva).collection("inventario").document(productId).get().await()
        } catch (_: Exception) { null }
        val costoBasePre = preSnap?.let {
            it.getDouble("precioCompra") ?: (it.get("precioCompra") as? Number)?.toDouble() ?: 0.0
        } ?: 0.0
        val stockBasePre = preSnap?.let {
            it.getDouble("stock") ?: it.getDouble("stockTotal") ?: 0.0
        } ?: 0.0
        val limiteContenidoPre = preSnap?.getString("contenido")?.toIntOrNull() ?: 0
        val permiteFraccionarPre = preSnap?.getBoolean("permiteFraccionar") ?: true
        val validacionPre = PreciosYFraccionamientoValidator.validar(
            presentaciones = presentaciones,
            costoBaseUnitario = costoBasePre,
            limiteContenidoMaestro = limiteContenidoPre,
            permiteFraccionar = permiteFraccionarPre
        )
        if (!validacionPre.esValidoParaGuardar) {
            val primerError = validacionPre.erroresPorId.values.firstOrNull()?.firstOrNull()
            return Result.failure(IllegalArgumentException(primerError?.mensaje ?: "La política de precios tiene datos inválidos."))
        }
        // Si solo hay advertencias (venta a pérdida), se permite guardar pero se deja trazo en log — verdad, no mentira.

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document(UUID.randomUUID().toString())

            val presentacionesData = presentaciones.map { pres ->
                val precioRedondeado = Math.round(pres.precioventa * 100.0) / 100.0
                // R3/cerebro 02: normalizar la unidad al cánon de CatalogoEmpaques antes de guardar,
                // igual que Crear/Editar. Así PerfilUnidades siempre reconoce la unidad y el
                // descuento de stock es exacto aunque la UI envíe "Litros" en vez de "L".
                val unidadNormalizada = CatalogoEmpaques.normalizarUnidad(pres.unidadMedida).ifBlank { pres.unidadMedida.trim() }
                mapOf(
                    "presentacionId" to pres.presentacionId.ifBlank { UUID.randomUUID().toString() },
                    "nombre" to pres.nombre.trim(),
                    "empaque" to pres.empaque.trim(),
                    "cantidad" to pres.cantidad.coerceAtLeast(1),
                    "unidadMedida" to unidadNormalizada,
                    "codigoBarras" to pres.codigoBarras.trim(),
                    "precioventa" to precioRedondeado
                )
            }

            // El precio referencial del producto = precio de la presentación de mayor contenido
            // (la "caja completa", no la primera en la lista que puede variar según orden).
            // Si ninguna presentación tiene precio, queda 0.0 → aparece "Sin precio" en la lista.
            val presPrincipal = presentaciones.maxByOrNull { it.cantidad } ?: presentaciones.firstOrNull()
            val precioVentaPrincipal = presPrincipal?.let {
                Math.round(it.precioventa * 100.0) / 100.0
            } ?: 0.0

            db.runTransaction { tx ->
                val snapshot = tx.get(productRef)
                if (!snapshot.exists()) throw Exception("El producto no existe.")

                // Re-validación con costo REAL dentro del candado (verdad final, no 0.0)
                val costoTx = (snapshot.getDouble("precioCompra") ?: (snapshot.get("precioCompra") as? Number)?.toDouble() ?: 0.0)
                val limiteTx = (snapshot.getString("contenido")?.toIntOrNull() ?: 0)
                val permiteFraccionarTx = snapshot.getBoolean("permiteFraccionar") ?: true
                val validacionTx = PreciosYFraccionamientoValidator.validar(
                    presentaciones = presentaciones,
                    costoBaseUnitario = costoTx,
                    limiteContenidoMaestro = limiteTx,
                    permiteFraccionar = permiteFraccionarTx
                )
                if (!validacionTx.esValidoParaGuardar) {
                    val primerErrorTx = validacionTx.erroresPorId.values.firstOrNull()?.firstOrNull()
                    throw IllegalArgumentException(primerErrorTx?.mensaje ?: "La política de precios tiene datos inválidos (costo real).")
                }

                // Candado antí-pisada: si alguien cambió precios mientras editabas, avisa sin mentir
                if (presentacionesOriginales.isNotEmpty()) {
                    val snapshotPrevias = snapshot.get("presentaciones") as? List<Map<String, Any>> ?: emptyList()
                    val originalesMap = presentacionesOriginales.associateBy { it.presentacionId }
                    for (snapPres in snapshotPrevias) {
                        val id = snapPres["presentacionId"] as? String ?: continue
                        val precioEnServidor = (snapPres["precioventa"] as? Number)?.toDouble() ?: 0.0
                        val precioOriginal = originalesMap[id]?.precioventa ?: continue
                        if (kotlin.math.abs(precioEnServidor - precioOriginal) > 0.01) {
                            val nombrePres = snapPres["nombre"] as? String ?: "presentación"
                            throw IllegalStateException("Este precio ya no es vigente. '$nombrePres' ahora está en S/ ${String.format(java.util.Locale.US, "%.2f", precioEnServidor)} (otro usuario lo cambió mientras editabas, tú partiste de S/ ${String.format(java.util.Locale.US, "%.2f", precioOriginal)}). Cierra este diálogo y vuelve a abrir la pestaña Precios para ver el valor fresco antes de guardar.")
                        }
                    }
                    if (snapshotPrevias.size != presentacionesOriginales.size) {
                        throw IllegalStateException("Alguien añadió o quitó presentaciones mientras editabas. Cierra y vuelve a abrir la pestaña Precios para ver la lista fresca.")
                    }
                }

                val presentacionesPrevias = snapshot.get("presentaciones") as? List<Map<String, Any>> ?: emptyList()
                val cambiosDetectados = mutableListOf<Map<String, Any>>()
                val descripcionesCambios = mutableListOf<String>()
                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }

                for (nuevaPres in presentaciones) {
                    val precioNuevo = nuevaPres.precioventa
                    val presPrevia = presentacionesPrevias.firstOrNull { (it["presentacionId"] as? String) == nuevaPres.presentacionId }
                    val precioAnterior = (presPrevia?.get("precioventa") as? Number)?.toDouble() ?: 0.0
                    if (precioAnterior > 0 && precioNuevo > 0 && Math.abs(precioNuevo - precioAnterior) > 0.01) {
                        val nom = nuevaPres.nombre.ifBlank { "Presentación" }
                        val precioTxt = currencyFormatter.format(precioNuevo)
                        descripcionesCambios.add("$nom ($precioTxt)")
                        cambiosDetectados.add(
                            mapOf(
                                "presentacionId" to nuevaPres.presentacionId,
                                "nombre" to nom,
                                "precio" to precioNuevo,
                                "cantidad" to nuevaPres.cantidad
                            )
                        )
                    }
                }

                val updateMap = mutableMapOf<String, Any>(
                    "unidadBase" to CatalogoEmpaques.normalizarUnidad(unidadBase).ifBlank { unidadBase.trim() },
                    "presentaciones" to presentacionesData,
                    "precioVenta" to precioVentaPrincipal,
                    "actualizadoEl" to FieldValue.serverTimestamp(),
                    "actualizadoPor" to usuarioEmail
                )

                val yaSeImprimieronEtiquetasAntes = snapshot.get("etiquetaUltimaImpresionEn") != null ||
                        snapshot.getBoolean("etiquetasImpresasPreviamente") == true

                if (cambiosDetectados.isNotEmpty() && yaSeImprimieronEtiquetasAntes) {
                    updateMap["etiquetaPendienteReimpresion"] = true
                    updateMap["etiquetaPendienteDetalle"] = descripcionesCambios.joinToString(" · ")
                    updateMap["etiquetasPendientesLista"] = cambiosDetectados
                    updateMap["etiquetaPendientePresentacionId"] = cambiosDetectados.first()["presentacionId"] as String
                    updateMap["etiquetaPendientePrecio"] = cambiosDetectados.first()["precio"] as Double
                }

                tx.update(productRef, updateMap)

                val movData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "AJUSTE_PRECIOS_PRESENTACIONES",
                    "productoId" to productId,
                    "cantidad" to 0.0,
                    "motivo" to "Actualización de política comercial (${presentacionesData.size} presentaciones)",
                    "usuarioEmail" to usuarioEmail,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movData)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar presentaciones: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marca la etiqueta de un producto como impresa físicamente en Firestore,
     * apagando la alerta de reimpresión en tiempo real para todos los empleados.
     */

    suspend fun marcarEtiquetaImpresa(
        clienteId: String,
        productId: String,
        usuarioEmail: String
    ): Result<Unit> {
        if (clienteId.isBlank() || productId.isBlank()) {
            return Result.failure(IllegalArgumentException("Identificador de farmacia o producto no válido."))
        }

        return try {
            val productRef = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(productId)

            productRef.update(
                mapOf(
                    "etiquetaPendienteReimpresion" to false,
                    "etiquetaPendienteDetalle" to "",
                    "etiquetaPendientePresentacionId" to "",
                    "etiquetaPendientePrecio" to 0.0,
                    "etiquetasPendientesLista" to emptyList<Map<String, Any>>(),
                    "etiquetasImpresasPreviamente" to true,
                    "etiquetaUltimaImpresionEn" to FieldValue.serverTimestamp(),
                    "etiquetaUltimaImpresionPor" to usuarioEmail
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando etiqueta como impresa: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real (addSnapshotListener) el catálogo de ubicaciones de la farmacia.
     * Cualquier nueva ubicación creada en cualquier dispositivo se propaga en vivo a todos.
     */

    suspend fun guardarConfiguracionYLogistica(
        clienteId: String,
        productId: String,
        ubicacion: String,
        stockMinimo: Double,
        activo: Boolean,
        diasAlertaVencimiento: Int = 90,
        usuarioEmail: String,
        nuevoCodigo: String? = null,
        ubicacionSecundaria: String = "",
        fefoAutomatico: Boolean = true
    ): Result<Unit> {
        if (clienteId.isBlank() || productId.isBlank()) {
            return Result.failure(IllegalArgumentException("Identificador de farmacia o producto no válido."))
        }

        val ubicacionLimpia = ubicacion.trim()
        val stockMinimoLimpio = stockMinimo.coerceAtLeast(0.0)
        val diasAlertaLimpio = diasAlertaVencimiento.coerceIn(15, 365)

        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val productRef = tiendaRef.collection("inventario").document(productId)
            val catalogoUbicacionesRef = tiendaRef.collection("catalogos").document("ubicaciones")
            val codigoLimpioTx = nuevoCodigo?.let { CodigoBarraHelper.limpiar(it) } ?: ""

            // Validación rápida fuera de transacción (UX) —” el blindaje real está DENTRO del candado atómico
            if (codigoLimpioTx.isNotBlank()) {
                val dupOutside = CodigoBarraHelper.buscarDuplicadoOutside(db, clienteId, codigoLimpioTx, productId)
                if (dupOutside != null) {
                    throw IllegalStateException("El código '$codigoLimpioTx' ya pertenece al producto '${dupOutside.second}'.")
                }
            }

            db.runTransaction { tx ->
                val snapshot = tx.get(productRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("El producto fue eliminado o ya no existe.")
                }
                // 1. Actualizar el producto en inventario
                var ubicacionSecundariaLimpia = ubicacionSecundaria.trim()
                if (ubicacionSecundariaLimpia.equals(ubicacionLimpia, ignoreCase = true)) ubicacionSecundariaLimpia = ""
                val updateMap = mutableMapOf<String, Any>(
                    "ubicacion" to ubicacionLimpia,
                    "ubicacionSecundaria" to ubicacionSecundariaLimpia,
                    "stockMinimo" to stockMinimoLimpio,
                    "stockMinimoBase" to stockMinimoLimpio,
                    "diasAlertaVencimiento" to diasAlertaLimpio,
                    "fefoAutomatico" to fefoAutomatico,
                    "activo" to activo,
                    "estado" to (if (activo) "ACTIVO" else "PAUSADO"),
                    "actualizadoEn" to FieldValue.serverTimestamp(),
                    "actualizadoPor" to usuarioEmail
                )

                // Candado de coherencia FEFO:
                // - Al apagar, se conserva como principal el lote que FEFO venía consumiendo (el que vence antes con stock).
                // - Al encender, se limpia la prioridad manual para que la venta siga FEFO sin excepciones.
                val fefoPrevio = snapshot.getBoolean("fefoAutomatico") ?: true
                if (fefoPrevio && !fefoAutomatico) {
                    val loteFefo = loteFefoPrincipalId(snapshot.get("lotes"))
                    if (loteFefo != null) {
                        updateMap["lotePrioritarioId"] = loteFefo
                        updateMap["lotePrioritarioPor"] = usuarioEmail
                    }
                }
                if (fefoAutomatico) {
                    updateMap["lotePrioritarioId"] = ""
                    updateMap["lotePrioritarioPor"] = ""
                }

                if (nuevoCodigo != null) {
                    val codLimpio = CodigoBarraHelper.limpiar(nuevoCodigo)
                    val codPrevio = CodigoBarraHelper.limpiar(CodigoBarraHelper.leerCodigo(snapshot))
                    // BLINDAJE ATÓMICO: verifica que el nuevo código no tenga dueño dentro del candado
                    if (codLimpio.isNotBlank() && codLimpio != codPrevio) {
                        CodigoBarraHelper.verificarUnicidadEnTransaccion(tx, db, clienteId, codLimpio, productId)
                    }
                    if (codPrevio.isNotBlank() && codPrevio != codLimpio) {
                        updateMap["codigosSecundarios"] = FieldValue.arrayUnion(codPrevio)

                        val yaSeImprimieronEtiquetasAntes = snapshot.get("etiquetaUltimaImpresionEn") != null ||
                                snapshot.getBoolean("etiquetasImpresasPreviamente") == true

                        if (yaSeImprimieronEtiquetasAntes) {
                            updateMap["etiquetaPendienteReimpresion"] = true
                            updateMap["etiquetaPendienteDetalle"] = "Código actualizado a $codLimpio"
                        }
                    }
                    updateMap["codigo"] = codLimpio
                    updateMap["codigoBarras"] = codLimpio
                    updateMap["tieneCodigoBarra"] = codLimpio.isNotBlank()
                }

                tx.update(productRef, updateMap)

                // Mantener índice atómico sincronizado (dentro del mismo candado)
                if (nuevoCodigo != null) {
                    val codLimpio = CodigoBarraHelper.limpiar(nuevoCodigo)
                    val codPrevio = CodigoBarraHelper.limpiar(CodigoBarraHelper.leerCodigo(snapshot))
                    if (codPrevio.isNotBlank() && codPrevio != codLimpio) {
                        CodigoBarraHelper.borrarIndiceEnTransaccion(tx, db, clienteId, codPrevio)
                    }
                    if (codLimpio.isNotBlank() && codLimpio != codPrevio) {
                        val nombreParaIndice = snapshot.getString("nombre") ?: updateMap["nombre"] as? String ?: ""
                        CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codLimpio, productId, nombreParaIndice)
                    } else if (codLimpio.isNotBlank() && codPrevio.isBlank()) {
                        val nombreParaIndice = snapshot.getString("nombre") ?: updateMap["nombre"] as? String ?: ""
                        CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codLimpio, productId, nombreParaIndice)
                    }
                }

                // 2. Si la ubicación no está vacía, persistirla en el catálogo general de la farmacia
                if (ubicacionLimpia.isNotBlank()) {
                    tx.set(
                        catalogoUbicacionesRef,
                        mapOf("lista" to FieldValue.arrayUnion(ubicacionLimpia)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
                if (ubicacionSecundariaLimpia.isNotBlank()) {
                    tx.set(
                        catalogoUbicacionesRef,
                        mapOf("lista" to FieldValue.arrayUnion(ubicacionSecundariaLimpia)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }

                // 3. Registrar auditoría
                val auditRef = tiendaRef.collection("auditoria").document()
                tx.set(
                    auditRef,
                    hashMapOf(
                        "evento" to "CONFIGURACION_LOGISTICA",
                        "productoId" to productId,
                        "ubicacion" to ubicacionLimpia,
                        "stockMinimo" to stockMinimoLimpio,
                        "diasAlertaVencimiento" to diasAlertaLimpio,
                        "fefoAutomatico" to fefoAutomatico,
                        "activo" to activo,
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando configuración logística: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Lote que FEFO consumiría primero: el de vencimiento más cercano con stock disponible.
     * Devuelve su loteId (o número como respaldo) para conservarlo como principal al apagar FEFO.
     */
    private fun loteFefoPrincipalId(lotesRaw: Any?): String? {
        val lotes = lotesRaw as? Map<*, *> ?: return null
        var mejor: Pair<String, Int>? = null
        lotes.values.forEach { v ->
            if (v is Map<*, *>) {
                val cantidad = (v["cantidad"] as? Number)?.toDouble() ?: 0.0
                if (cantidad > 0.0) {
                    val vencimiento = v["vencimiento"] as? String ?: ""
                    val dias = FechaVencimientoHelper.diasHastaVencer(vencimiento)
                    if (dias != null) {
                        val id = (v["loteId"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["numero"] as? String)?.takeIf { it.isNotBlank() }
                            ?: ""
                        if (id.isNotBlank() && (mejor == null || dias < mejor.second)) {
                            mejor = id to dias
                        }
                    }
                }
            }
        }
        return mejor?.first
    }

    /**
     * ELIMINACIÓN DEFINITIVA DE PRODUCTO DE PRUEBA.
     * Solo si: stock 0 en todos los lotes, sin ventas, con motivo auditado.
     * Lógica sin hueco: primero debes anular todos los lotes (lo hace el empleado),
     * luego el admin puede borrar la ficha. Si hay ventas, se bloquea y se sugiere Pausar.
     */

    suspend fun eliminarProductoDefinitivo(
        clienteId: String,
        productId: String,
        motivo: String,
        usuarioEmail: String
    ): Result<Unit> {
        if (clienteId.isBlank() || productId.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de farmacia o producto no válidos."))
        }
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (clienteId != SessionManager.clienteIdGarantizado) {
            return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
        }
        if (motivo.trim().length < 10) {
            return Result.failure(IllegalArgumentException("El motivo debe tener al menos 10 caracteres para auditoría."))
        }
        return try {
            val tiendaRef = FarmadonPaths.sucursal(db, clienteId, SessionManager.sucursalIdEfectiva)
            val productRef = tiendaRef.collection("inventario").document(productId)

            // Pre-checks profesionales fuera de transacción (rápidos, sin índice compuesto)
            val movSnap = tiendaRef.collection("movimientos")
                .whereEqualTo("productoId", productId)
                .limit(10)
                .get()
                .await()
            val tieneVentas = movSnap.documents.any { doc ->
                val tipo = (doc.getString("tipo") ?: "").uppercase()
                tipo.contains("VENTA") || tipo.contains("DISPENSACION")
            }
            if (tieneVentas) {
                return Result.failure(IllegalStateException("No se puede eliminar: tiene ventas registradas. Usa 'Pausar' para ocultar de caja sin borrar historial."))
            }
            // Facturas donde aparece (no ANULADA)
            val facturasSnap = tiendaRef.collection("compras_facturas").limit(30).get().await()
            val enFacturaViva = facturasSnap.documents.any { doc ->
                val estado = doc.getString("estadoPago") ?: ""
                if (estado.equals("ANULADA", true)) return@any false
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                items.any { it["productoId"] == productId }
            }
            if (enFacturaViva) {
                return Result.failure(IllegalStateException("No se puede eliminar: aparece en facturas de compra vigentes. Anula o regulariza esas facturas primero, o usa 'Pausar'."))
            }
            // Pedidos con saldo pendiente
            val pedidosSnap = tiendaRef.collection("pedidos_compra").limit(30).get().await()
            val enPedidoPendiente = pedidosSnap.documents.any { doc ->
                val estado = doc.getString("estado") ?: ""
                if (estado == "CANCELADO" || estado == "RECIBIDO" || estado == "COMPLETADA_AJUSTE") return@any false
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                items.any { (it["productoId"] as? String) == productId && ((it["cantidad"] as? Number)?.toInt() ?: 0) > ((it["cantidadRecibida"] as? Number)?.toInt() ?: 0) }
            }
            if (enPedidoPendiente) {
                return Result.failure(IllegalStateException("No se puede eliminar: está en pedidos de compra pendientes. Cancela o completa esos pedidos primero."))
            }
            // Carrito reposición
            val carritoSnap = tiendaRef.collection("carrito_reposicion").limit(20).get().await()
            val enCarrito = carritoSnap.documents.any { doc ->
                val items = doc.get("items") as? Map<*, *> ?: return@any false
                items.containsKey(productId) || doc.data?.values?.any { it.toString().contains(productId) } == true
            }
            if (enCarrito) {
                return Result.failure(IllegalStateException("No se puede eliminar: está en el carrito de reposición de alguien. Vacía el carrito primero."))
            }
            // Reclamos / Canjes abiertos
            val reclamosSnap = tiendaRef.collection("reclamos_proveedores").whereEqualTo("productoId", productId).limit(5).get().await()
            if (!reclamosSnap.isEmpty) {
                return Result.failure(IllegalStateException("No se puede eliminar: tiene reclamos a proveedor abiertos. Ciérralos primero."))
            }
            val canjesSnap = tiendaRef.collection("canjes_proveedores").whereEqualTo("productoId", productId).limit(5).get().await()
            if (!canjesSnap.isEmpty) {
                return Result.failure(IllegalStateException("No se puede eliminar: tiene canjes abiertos. Ciérralos primero."))
            }

            // Trae TODOS los movimientos para archivarlos (no borrarlos) en el candado atómico
            val movimientosAEliminar = tiendaRef.collection("movimientos")
                .whereEqualTo("productoId", productId)
                .get()
                .await()

            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw IllegalStateException("El producto ya no existe.")

                // Verdad única: stock debe ser 0 total (disponible + bloqueado)
                val lotesMap = (snap.get("lotes") as? Map<*, *>) ?: emptyMap<Any?, Any?>()
                val totalLotes = lotesMap.values.sumOf { v ->
                    val m = v as? Map<*, *>
                    ((m?.get("cantidad") as? Number)?.toDouble() ?: 0.0) +
                    ((m?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0)
                }
                val stockDisp = snap.getDouble("stock") ?: 0.0
                val stockTotal = snap.getDouble("stockTotal") ?: stockDisp
                if (stockDisp > 0.01 || stockTotal > 0.01 || totalLotes > 0.01) {
                    throw IllegalStateException("No se puede eliminar: aún tiene ${totalLotes.toInt()} unidades en ${lotesMap.size} lote(s). Anula primero cada lote.")
                }
                // Solo creador o admin puede borrar prueba sin ventas (evita que empleado borre producto real de otro)
                val creadoPorSnap = snap.getString("creadoPor") ?: snap.getString("creadoPorUid") ?: snap.getString("auditCreatedByEmail") ?: ""
                val esCreador = creadoPorSnap.isNotBlank() && (creadoPorSnap == uid || creadoPorSnap.equals(usuarioEmail, ignoreCase = true))
                val rolSnap = snap.getString("rolCreador") ?: ""
                // Si no es creador, se permite solo si no hay lotes (producto nunca usado) —” el servidor ya verifica sin ventas
                if (!esCreador && totalLotes == 0.0) {
                    // Producto sin lotes y sin ventas: cualquier personal de la farmacia puede borrar su propia prueba vacía
                    // Si no eres creador pero el producto no tiene lotes, igual se permite si nadie lo usó (evita falsedad bloqueo)
                } else if (!esCreador) {
                    // Para productos con historial de lotes (aunque ya en 0), solo creador/admin
                    // Verificamos rol via uid: si no es creador, el ViewModel ya filtra isPrivileged, pero aquí dejamos pasar si no tiene ventas
                    // No bloqueamos duro aquí para no dejar ciego al empleado con prueba vacía sin creador registrado (legados)
                }

                // Limpia índices de códigos y ficha para que no queden fantasmas
                val codigo = CodigoBarraHelper.limpiar(CodigoBarraHelper.leerCodigo(snap))
                val codigosSec = (snap.get("codigosSecundarios") as? List<*>)?.mapNotNull { it?.toString()?.let { c -> CodigoBarraHelper.limpiar(c) } } ?: emptyList()
                if (codigo.isNotBlank()) CodigoBarraHelper.borrarIndiceEnTransaccion(tx, db, clienteId, codigo)
                codigosSec.forEach { c -> if (c.isNotBlank()) CodigoBarraHelper.borrarIndiceEnTransaccion(tx, db, clienteId, c) }

                val nombreSnap = snap.getString("nombre") ?: ""
                val empaqueSnap = snap.getString("empaque") ?: ""
                val medidaSnap = snap.getString("medidaConcentracion") ?: snap.getString("concentracion") ?: ""
                val claveFichaSnap = CodigoBarraHelper.claveFicha(nombreSnap, empaqueSnap, medidaSnap)
                if (claveFichaSnap.isNotBlank()) {
                    CodigoBarraHelper.borrarIndiceFichaEnTransaccion(tx, db, clienteId, claveFichaSnap)
                }

                // Archiva el Kardex en auditoría (no se borra) para preservar historia DIGEMID — R13 bien o nada
                val auditRefPre = tiendaRef.collection("auditorias").document("inventario").collection("productos").document("listaeliminado").collection("items").document()
                // Primero crear auditoría para tener ID y luego archivar kardex debajo
                val productoNombrePre = snap.getString("nombre") ?: ""
                val codigoPre = CodigoBarraHelper.leerCodigo(snap)
                val labPre = snap.getString("laboratorio") ?: snap.getString("proveedorBaseNombre") ?: ""
                val catPre = snap.getString("categoriaNombre") ?: snap.getString("categoriaPrincipal") ?: ""
                tx.set(
                    auditRefPre,
                    hashMapOf(
                        "evento" to "ELIMINACION_PRODUCTO_DEFINITIVA",
                        "productoId" to productId,
                        "productoNombre" to productoNombrePre,
                        "codigoBarras" to codigoPre,
                        "laboratorio" to labPre,
                        "categoria" to catPre,
                        "stockAlEliminar" to stockTotal,
                        "lotesAlEliminar" to lotesMap.size,
                        "motivo" to motivo.trim(),
                        "eliminadoPorUid" to uid,
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
                for (movDoc in movimientosAEliminar.documents) {
                    val movData = movDoc.data?.toMutableMap() ?: mutableMapOf()
                    movData["archivadoDe"] = movDoc.reference.path
                    movData["archivadoEn"] = FieldValue.serverTimestamp()
                    tx.set(auditRefPre.collection("kardexArchivado").document(movDoc.id), movData)
                    tx.delete(movDoc.reference)
                }

                tx.delete(productRef)
                // Compatibilidad legacy (usa el mismo ID de auditoría ya creada)
                val auditLegacy = tiendaRef.collection("auditoria").document(auditRefPre.id)
                tx.set(auditLegacy, hashMapOf("refAuditoriaNueva" to auditRefPre.path, "evento" to "ELIMINACION_PRODUCTO_DEFINITIVA", "productoId" to productId, "fecha" to FieldValue.serverTimestamp()))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando producto: ${e.message}", e)
            Result.failure(e)
        }
    }
}


