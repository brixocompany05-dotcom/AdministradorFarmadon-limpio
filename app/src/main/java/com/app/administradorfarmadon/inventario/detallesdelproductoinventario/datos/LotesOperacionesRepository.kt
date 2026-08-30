package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.datos

import kotlinx.coroutines.tasks.await

/**
 * Operaciones de lotes —” bloqueo, devolución, canje, anulación y merma. Transacciones atómicas todo-o-nada.
 * Fachada compatibilidad: delega a LotesBloqueoRepository y LotesDevolucionCanjeRepository.
 * Extraído de ProductDetailFirestoreRepository (1.268 líneas) y God Lotes 673 ──†’ 2 repos.
 */
class LotesOperacionesRepository(
    private val db: com.google.firebase.firestore.FirebaseFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
) {
    private val bloqueo = LotesBloqueoRepository(db)
    private val devol = LotesDevolucionCanjeRepository(db)

    suspend fun cambiarBloqueoLote(clienteId: String, productId: String, lote: com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto, ponerEnCuarentena: Boolean, cantidadAfectada: Double, motivo: String, usuarioEmail: String) = bloqueo.cambiarBloqueoLote(clienteId, productId, lote, ponerEnCuarentena, cantidadAfectada, motivo, usuarioEmail)
    suspend fun anularIngresoLote(clienteId: String, productId: String, lote: com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto, motivo: String, usuarioEmail: String) = bloqueo.anularIngresoLote(clienteId, productId, lote, motivo, usuarioEmail)
    suspend fun registrarDevolucionProveedor(clienteId: String, productId: String, lote: com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto, cantidadDevuelta: Double, guiaRetiro: String, notaCredito: String, motivo: String, modalidadCompensacion: String, usuarioEmail: String, idempotenciaId: String = "") = devol.registrarDevolucionProveedor(clienteId, productId, lote, cantidadDevuelta, guiaRetiro, notaCredito, motivo, modalidadCompensacion, usuarioEmail, idempotenciaId)
    suspend fun registrarCanjeProducto(clienteId: String, productId: String, loteOrigen: com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto, cantidadCanjeada: Double, nuevoLoteNumero: String, nuevoVencimiento: String, guiaCanje: String, motivo: String, usuarioEmail: String, idempotenciaId: String = "") = devol.registrarCanjeProducto(clienteId, productId, loteOrigen, cantidadCanjeada, nuevoLoteNumero, nuevoVencimiento, guiaCanje, motivo, usuarioEmail, idempotenciaId)

    /**
     * PRIORIDAD DE VENTA: define qué lote se consume primero. loteId = null/vacío vuelve a FEFO.
     * AUDITORíA OBLIGATORIA: dentro de la misma transacción se registra QUIí‰N (email), CON QUí‰
     * ROL, qué lote anterior quedaba y cuál queda ahora —” la decisión nunca es anónima.
     */
    suspend fun definirLotePrioritario(clienteId: String, productId: String, loteId: String?, usuarioRol: String, usuarioEmail: String): Result<Unit> {
        if (clienteId.isBlank() || productId.isBlank()) return Result.failure(Exception("Sesión no válida."))
        return try {
            val sucursalId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalIdEfectiva
            val tiendaRef = com.app.administradorfarmadon.compartido.datos.FarmadonPaths.sucursal(db, clienteId, sucursalId)
            val productRef = tiendaRef.collection("inventario").document(productId)
            val loteFinal = loteId?.trim().orEmpty()
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) throw Exception("El producto no existe.")
                if (snap.getBoolean("fefoAutomatico") ?: true) {
                    throw Exception("El FEFO automático está activo. Apágalo en Configuración del producto para elegir manualmente.")
                }
                if (loteFinal.isNotBlank()) {
                    val lotes = snap.get("lotes") as? Map<*, *> ?: emptyMap<Any?, Any?>()
                    val existe = lotes.keys.any { it.toString().equals(loteFinal, ignoreCase = true) } ||
                        lotes.values.mapNotNull { it as? Map<*, *> }.any {
                            (it["loteId"] as? String)?.equals(loteFinal, ignoreCase = true) == true ||
                            (it["numero"] as? String)?.equals(loteFinal, ignoreCase = true) == true
                        }
                    if (!existe) throw Exception("El lote indicado ya no existe en este producto.")
                }
                val loteAnterior = snap.getString("lotePrioritarioId") ?: ""

                tx.update(productRef, mapOf(
                    "lotePrioritarioId" to loteFinal,
                    "lotePrioritarioPor" to usuarioEmail,
                    "lotePrioritarioPorRol" to usuarioRol,
                    "actualizadoEl" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "actualizadoPor" to usuarioEmail
                ))

                // Asiento de auditoría atómico: quién, con qué rol, antes ──†’ ahora.
                tx.set(
                    tiendaRef.collection("auditoria").document(),
                    mapOf(
                        "evento" to "PRIORIDAD_CONSUMO_LOTE",
                        "productoId" to productId,
                        "loteAnterior" to loteAnterior,
                        "loteNuevo" to loteFinal,
                        "rol" to usuarioRol,
                        "usuarioEmail" to usuarioEmail,
                        "fecha" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("LotesOperacionesRepo", "Error definiendo lote prioritario: ${e.message}", e)
            Result.failure(e)
        }
    }
}
