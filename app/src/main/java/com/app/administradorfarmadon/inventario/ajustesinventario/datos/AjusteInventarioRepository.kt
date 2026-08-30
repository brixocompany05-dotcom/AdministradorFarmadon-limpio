package com.app.administradorfarmadon.inventario.ajustesinventario.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Ajustes de inventario NO comerciales (muestras, donaciones, sobrantes, inventario inicial,
 * mermas, pérdidas, errores). Proceso separado de la compra: sin factura, sin cuentas por pagar,
 * sin pedidos. Siempre con motivo y quién, trazado en kardex (R1/R3/R12).
 */
class AjusteInventarioRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "AjusteInventarioRepository"
    }

    private fun obtenerIds(): Pair<String, String>? {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return null
        return Pair(f, s)
    }

    /**
     * Entrada que NO es compra (muestra, donación, sobrante, inventario inicial).
     * Requiere lote y vencimiento reales; costo 0 (no toca precioCompra).
     */
    suspend fun registrarEntradaNoCompra(
        productId: String,
        productoNombre: String,
        empaque: String,
        loteNumero: String,
        vencimiento: String,
        cantidad: Double,
        tipo: String, // MUESTRA | DONACION | SOBRANTE | INVENTARIO_INICIAL
        motivo: String,
        usuarioEmail: String,
        usuarioNombre: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val ids = obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val (farmaciaId, sucursalId) = ids
        val loteLimpio = loteNumero.trim().uppercase()
        if (loteLimpio.isBlank()) return Result.failure(IllegalArgumentException("El lote es obligatorio para la entrada."))
        if (cantidad <= 0.0) return Result.failure(IllegalArgumentException("La cantidad debe ser mayor a 0."))
        if (motivo.trim().length < 5) return Result.failure(IllegalArgumentException("El motivo debe tener al menos 5 caracteres."))
        val vtoNorm = FechaVencimientoHelper.normalizar(vencimiento.trim())
            ?: return Result.failure(IllegalArgumentException("La fecha de vencimiento no es válida (formato MM/AAAA)."))
        val dias = FechaVencimientoHelper.diasHastaVencer(vtoNorm)
            ?: return Result.failure(IllegalArgumentException("La fecha de vencimiento no es válida."))
        if (dias <= 0) return Result.failure(IllegalArgumentException("La fecha de vencimiento ya está vencida."))

        return try {
            val idemFinal = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }
            val tiendaRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
            val ahoraMs = HoraServidor.ahoraMs()
            val fechaLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ahoraMs))
            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document("idem_$idemFinal")

            db.runTransaction { tx ->
                val productSnap = tx.get(productRef)
                if (!productSnap.exists()) throw IllegalStateException("El producto '$productoNombre' no existe en inventario.")
                if (tx.get(movimientoRef).exists()) return@runTransaction
                @Suppress("UNCHECKED_CAST")
                val currentLotes = (productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val resLote = FechaVencimientoHelper.resolverLote(currentLotes, loteLimpio)
                val keyLoteDestino = resLote?.first ?: FechaVencimientoHelper.llaveLote(loteLimpio)
                val loteActual = resLote?.second as? Map<*, *>
                val vencimientoOriginal = (loteActual?.get("vencimiento") as? String)?.takeIf { it.isNotBlank() }
                if (loteActual != null && vencimientoOriginal != null && vencimientoOriginal != vtoNorm) {
                    throw IllegalArgumentException("El lote $loteLimpio ya existe con vencimiento $vencimientoOriginal. Si es el mismo lote, usa esa fecha.")
                }
                val vencimientoFinal = vencimientoOriginal ?: vtoNorm
                val cantidadAnterior = (loteActual?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                val nuevaCantidad = cantidadAnterior + cantidad
                val proveedorOriginal = (loteActual?.get("proveedor") as? String)?.takeIf { it.isNotBlank() }
                val proveedorIdOriginal = (loteActual?.get("proveedorId") as? String)?.takeIf { it.isNotBlank() }
                val facturaOriginal = (loteActual?.get("factura") as? String)?.takeIf { it.isNotBlank() }
                val costoCompraOriginal = (loteActual?.get("costoCompra") as? Number)?.toDouble() ?: 0.0
                val costoUnitarioOriginal = (loteActual?.get("costoUnitario") as? Number)?.toDouble() ?: 0.0
                @Suppress("UNCHECKED_CAST")
                val entradas = (loteActual?.get("entradas") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                entradas.add(
                    mapOf(
                        "fechaLegible" to fechaLegible,
                        "fechaMs" to ahoraMs,
                        "tipo" to "AJUSTE_$tipo",
                        "motivo" to motivo.trim(),
                        "cantidad" to cantidad,
                        "costoUnitario" to 0.0,
                        "proveedorNombre" to "",
                        "factura" to "AJUSTE"
                    )
                )
                val loteFinalData = mutableMapOf<String, Any>(
                    "numero" to loteLimpio,
                    "loteId" to FechaVencimientoHelper.llaveLote(loteLimpio),
                    "vencimiento" to vencimientoFinal,
                    "cantidad" to nuevaCantidad,
                    "proveedor" to (proveedorOriginal ?: ""),
                    "proveedorId" to (proveedorIdOriginal ?: ""),
                    "factura" to (facturaOriginal ?: "AJUSTE"),
                    "entradas" to entradas,
                    "costoCompra" to costoCompraOriginal,
                    "costoUnitario" to costoUnitarioOriginal,
                    "ultimaEntrada" to FieldValue.serverTimestamp()
                )
                if (loteActual == null) loteFinalData["fechaIngreso"] = FieldValue.serverTimestamp()
                currentLotes[keyLoteDestino] = loteFinalData

                val nuevoStockDisp = currentLotes.values.sumOf { (it as? Map<*, *>)?.let { m -> (m["cantidad"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0 }
                val nuevoStockTotal = currentLotes.values.sumOf {
                    val d = it as? Map<*, *>
                    val cDisp = (d?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                    val cBloq = (d?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                    cDisp + cBloq
                }
                tx.update(
                    productRef,
                    mapOf(
                        "lotes" to currentLotes,
                        "stock" to nuevoStockDisp,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to FechaVencimientoHelper.vencimientoMasCercano(currentLotes),
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                )

                val movimientoData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "ENTRADA_AJUSTE_$tipo",
                    "productoId" to productId,
                    "productoNombre" to productoNombre,
                    "empaque" to empaque,
                    "cantidadTotal" to cantidad,
                    "cantidadComprada" to 0.0,
                    "bonificacionGratis" to 0.0,
                    "costoTotal" to 0.0,
                    "costoUnitario" to 0.0,
                    "loteNumero" to loteLimpio,
                    "vencimiento" to vencimientoFinal,
                    "esLoteExistente" to (loteActual != null),
                    "proveedorNombre" to "",
                    "facturaNumero" to "AJUSTE",
                    "condicionPago" to "",
                    "usuarioEmail" to usuarioEmail,
                    "usuarioNombre" to usuarioNombre,
                    "pedidoId" to "",
                    "origen" to "AJUSTE_INVENTARIO",
                    "notas" to motivo.trim(),
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movimientoData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando entrada de ajuste ($productoNombre): ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Salida de inventario NO comercial (merma, pérdida, error de inventario).
     * Descarta de un lote existente; nunca inventa ni deja stock negativo.
     */
    suspend fun registrarSalidaAjuste(
        productId: String,
        productoNombre: String,
        loteNumero: String,
        cantidad: Double,
        tipo: String, // MERMA | PERDIDA | ERROR_INVENTARIO
        motivo: String,
        usuarioEmail: String,
        usuarioNombre: String,
        idempotenciaId: String = ""
    ): Result<Unit> {
        val ids = obtenerIds() ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val (farmaciaId, sucursalId) = ids
        val loteLimpio = loteNumero.trim().uppercase()
        if (loteLimpio.isBlank()) return Result.failure(IllegalArgumentException("Elige el lote del que sale la mercadería."))
        if (cantidad <= 0.0) return Result.failure(IllegalArgumentException("La cantidad debe ser mayor a 0."))
        if (motivo.trim().length < 5) return Result.failure(IllegalArgumentException("El motivo debe tener al menos 5 caracteres."))

        return try {
            val idemFinal = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }
            val tiendaRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
            val ahoraMs = HoraServidor.ahoraMs()
            val fechaLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ahoraMs))
            val productRef = tiendaRef.collection("inventario").document(productId)
            val movimientoRef = tiendaRef.collection("movimientos").document("idem_$idemFinal")

            db.runTransaction { tx ->
                val productSnap = tx.get(productRef)
                if (!productSnap.exists()) throw IllegalStateException("El producto '$productoNombre' no existe en inventario.")
                if (tx.get(movimientoRef).exists()) return@runTransaction
                @Suppress("UNCHECKED_CAST")
                val currentLotes = (productSnap.get("lotes") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                val resLote = FechaVencimientoHelper.resolverLote(currentLotes, loteLimpio)
                    ?: throw IllegalArgumentException("El lote $loteLimpio ya no existe en inventario.")
                val key = resLote.first
                val loteData = resLote.second
                val cantidadDisponible = ((loteData["cantidad"] as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
                if (cantidad > cantidadDisponible + 0.001) {
                    throw IllegalArgumentException("Solo hay $cantidadDisponible unidades disponibles en el lote $loteLimpio.")
                }
                val nuevaCantidad = (cantidadDisponible - cantidad).coerceAtLeast(0.0)
                val loteEliminado = nuevaCantidad < 0.001
                if (loteEliminado) {
                    currentLotes.remove(key)
                } else {
                    loteData["cantidad"] = nuevaCantidad
                    currentLotes[key] = loteData
                }
                val nuevoStockDisp = currentLotes.values.sumOf { (it as? Map<*, *>)?.let { m -> (m["cantidad"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0 }
                val nuevoStockTotal = currentLotes.values.sumOf {
                    val d = it as? Map<*, *>
                    val cDisp = (d?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                    val cBloq = (d?.get("cantidadBloqueada") as? Number)?.toDouble() ?: 0.0
                    cDisp + cBloq
                }
                val updatesProducto = mutableMapOf<String, Any>(
                    "lotes" to currentLotes,
                    "stock" to nuevoStockDisp,
                    "stockTotal" to nuevoStockTotal,
                    "vencimientoMasCercano" to FechaVencimientoHelper.vencimientoMasCercano(currentLotes),
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                // Si el lote principal de consumo se agota, se limpia solo (nada de lotes fantasma).
                if (loteEliminado) {
                    val principalActual = productSnap.getString("lotePrioritarioId") ?: ""
                    if (principalActual.isNotBlank() &&
                        (principalActual.equals(FechaVencimientoHelper.llaveLote(loteLimpio), true) || principalActual.equals(loteLimpio, true))
                    ) {
                        updatesProducto["lotePrioritarioId"] = ""
                        updatesProducto["lotePrioritarioPor"] = ""
                        updatesProducto["lotePrioritarioPorRol"] = ""
                    }
                }
                tx.update(productRef, updatesProducto)

                val movimientoData = mapOf(
                    "id" to movimientoRef.id,
                    "tipo" to "SALIDA_AJUSTE_$tipo",
                    "productoId" to productId,
                    "productoNombre" to productoNombre,
                    "cantidadTotal" to -cantidad,
                    "costoTotal" to 0.0,
                    "loteNumero" to loteLimpio,
                    "vencimiento" to (loteData["vencimiento"] as? String ?: ""),
                    "usuarioEmail" to usuarioEmail,
                    "usuarioNombre" to usuarioNombre,
                    "pedidoId" to "",
                    "origen" to "AJUSTE_INVENTARIO",
                    "notas" to motivo.trim(),
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movimientoRef, movimientoData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando salida de ajuste ($productoNombre): ${e.message}", e)
            Result.failure(e)
        }
    }
}
