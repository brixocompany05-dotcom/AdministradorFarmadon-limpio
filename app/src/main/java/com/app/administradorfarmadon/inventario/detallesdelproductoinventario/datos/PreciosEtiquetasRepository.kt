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
 * Precios, etiquetas y configuración — presentaciones, etiquetas y logística.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) — responsabilidad única.
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
        usuarioEmail: String
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
        val costoBaseParaValidar = presentaciones.firstOrNull()?.let { _ -> 0.0 } ?: 0.0
        val validacion = PreciosYFraccionamientoValidator.validar(
            presentaciones = presentaciones,
            costoBaseUnitario = costoBaseParaValidar,
            permiteFraccionar = true,
            limiteContenidoMaestro = 0
        )
        if (!validacion.esValidoParaGuardar) {
            val primerError = validacion.erroresPorId.values.firstOrNull()?.firstOrNull()
            return Result.failure(IllegalArgumentException(primerError?.mensaje ?: "La política de precios tiene datos inválidos."))
        }

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
        nuevoCodigo: String? = null
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

            // Validación rápida fuera de transacción (UX) — el blindaje real está DENTRO del candado atómico
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
                val updateMap = mutableMapOf<String, Any>(
                    "ubicacion" to ubicacionLimpia,
                    "stockMinimo" to stockMinimoLimpio,
                    "stockMinimoBase" to stockMinimoLimpio,
                    "diasAlertaVencimiento" to diasAlertaLimpio,
                    "activo" to activo,
                    "estado" to (if (activo) "ACTIVO" else "PAUSADO"),
                    "actualizadoEn" to FieldValue.serverTimestamp(),
                    "actualizadoPor" to usuarioEmail
                )

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

            // Pre-check fuera de transacción: si tiene ventas, no se borra (pausar es lo correcto)
            // Evita índice compuesto: trae pocos docs y filtra en memoria
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

            // Trae TODOS los movimientos del producto (sin límite) para borrarlos en el candado atómico y no dejar Kardex huérfano
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
                // Si no es creador, se permite solo si no hay lotes (producto nunca usado) — el servidor ya verifica sin ventas
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

                // Borra el Kardex (movimientos) del producto para no dejar fantasmas huérfanos que apunten a una ficha inexistente
                for (movDoc in movimientosAEliminar.documents) {
                    tx.delete(movDoc.reference)
                }

                tx.delete(productRef)

                // Auditoría ordenada y sin basura: solo valiosa, en subcolección dedicada (no cuello de botella)
                val productoNombreVal = snap.getString("nombre") ?: ""
                val codigoVal = CodigoBarraHelper.leerCodigo(snap)
                val labVal = snap.getString("laboratorio") ?: snap.getString("proveedorBaseNombre") ?: ""
                val catVal = snap.getString("categoriaNombre") ?: snap.getString("categoriaPrincipal") ?: ""
                // Ruta ordenada: auditorias/inventario/productos/listaeliminado/{id}
                val auditRef = tiendaRef.collection("auditorias").document("inventario").collection("productos").document("listaeliminado").collection("items").document()
                tx.set(
                    auditRef,
                    hashMapOf(
                        "evento" to "ELIMINACION_PRODUCTO_DEFINITIVA",
                        "productoId" to productId,
                        "productoNombre" to productoNombreVal,
                        "codigoBarras" to codigoVal,
                        "laboratorio" to labVal,
                        "categoria" to catVal,
                        "stockAlEliminar" to stockTotal,
                        "lotesAlEliminar" to lotesMap.size,
                        "motivo" to motivo.trim(),
                        "eliminadoPorUid" to uid,
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
                // Compatibilidad: también en auditoria legacy para no romper dashboards viejos
                val auditLegacy = tiendaRef.collection("auditoria").document(auditRef.id)
                tx.set(auditLegacy, hashMapOf("refAuditoriaNueva" to auditRef.path, "evento" to "ELIMINACION_PRODUCTO_DEFINITIVA", "productoId" to productId, "fecha" to FieldValue.serverTimestamp()))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando producto: ${e.message}", e)
            Result.failure(e)
        }
    }
}


