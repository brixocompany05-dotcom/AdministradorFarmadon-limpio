package com.app.administradorfarmadon.compras.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.inventario.compartido.datos.IngresoMercaderiaRepository
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ItemRecepcionEntrega(
    val productoId: String = "",
    val productoNombre: String = "",
    val presentacion: String = "Und",
    val loteNumero: String = "",
    val vencimiento: String = "",
    val cantidadComprada: Int = 0,
    val bonificacionGratis: Int = 0,
    val cantidadTotal: Int = 0,
    val costoUnitarioReal: Double = 0.0,
    val costoTotalReal: Double = 0.0
)

data class RecepcionEntrega(
    val id: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val usuarioNombre: String = "",
    val usuarioEmail: String = "",
    val numeroFactura: String = "",
    val condicionPago: String = "Contado",
    val fechaVencimientoPago: String = "",
    val montoFactura: Double = 0.0,
    val items: List<ItemRecepcionEntrega> = emptyList(),
    val notas: String = "",
    val cierreConAjuste: Boolean = false
)

data class PedidoCompra(
    val id: String = "",
    val numeroOrden: String = "",
    val farmaciaId: String = "",
    val sucursalId: String = "",
    val proveedorId: String = "",
    val proveedorNombre: String = "",
    val proveedorRuc: String = "",
    val proveedorTelefono: String = "",
    val items: List<ItemPedidoCompra> = emptyList(),
    val recepciones: List<RecepcionEntrega> = emptyList(),
    val totalProductos: Int = 0,
    val totalUnidades: Int = 0,
    val totalInversion: Double = 0.0,
    val montoFacturadoReal: Double = 0.0,
    val estado: String = "ENVIADO", // "ENVIADO", "ENTREGA_PARCIAL", "RECIBIDO", "COMPLETADA_AJUSTE", "CANCELADO"
    val fechaEmision: String = "",
    val fechaEmisionMs: Long = 0L,
    val fechaRecepcion: String = "",
    val notas: String = "",
    val usuarioEmisor: String = ""
) {
    val totalUnidadesRecibidas: Int get() = items.sumOf { it.cantidadRecibida }
    val totalUnidadesPendientes: Int get() = items.sumOf { (it.cantidad - it.cantidadRecibida).coerceAtLeast(0) }
    val estaCompletamenteRecibido: Boolean get() = items.isNotEmpty() && items.all { it.cantidadRecibida >= it.cantidad }
}

/** Vista de un lote YA EXISTENTE en inventario, para inteligencia en vivo de recepción. */
data class LoteExistenteVista(
    val productoId: String,
    val productoNombre: String,
    val numero: String,
    val cantidad: Double,
    val vencimiento: String
)

class PedidoCompraRepository(
    private val firestore: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "PedidoCompraRepository"
    }

    private fun obtenerFarmaciaYSucursal(): Pair<String, String>? {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return null
        return Pair(farmaciaId, sucursalId)
    }

    // ══ CARRITO DE REPOSICIÓN COMPARTIDO EN FIRESTORE ══
    // Un documento por proveedor. Todos los usuarios de la sucursal ven el mismo carrito.
    // Estructura: {items: {productoId: cantidad}, actualizadoPor: email}

    fun observarCarritosReposicion(onErrorEscucha: ((String) -> Unit)? = null): Flow<Map<String, Map<String, Int>>> = callbackFlow {
        val ids = obtenerFarmaciaYSucursal()
        if (ids == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }
        val (farmaciaId, sucursalId) = ids

        val listener = FarmadonPaths.carritoReposicion(firestore, farmaciaId, sucursalId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando carrito reposición: ${error.message}", error)
                    onErrorEscucha?.invoke(error.message ?: "Sin detalle del servidor")
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val mapa = mutableMapOf<String, MutableMap<String, Int>>()
                snapshot?.documents?.forEach { doc ->
                    // La clave para la pantalla es el NOMBRE REAL guardado en el campo,
                    // jamás el ID del documento (que va saneado y puede diferir).
                    val provNombre = doc.getString("proveedorNombre")?.takeIf { it.isNotBlank() } ?: doc.id
                    @Suppress("UNCHECKED_CAST")
                    val itemsRaw = doc.get("items") as? Map<*, *> ?: return@forEach
                    val carroProv = mutableMapOf<String, Int>()
                    itemsRaw.forEach { (clave, valor) ->
                        val c = (valor as? Number)?.toInt() ?: 0
                        if (c > 0 && clave != null) carroProv[clave.toString()] = c
                    }
                    if (carroProv.isNotEmpty()) mapa[provNombre] = carroProv
                }
                trySend(mapa)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Clave segura de documento para el borrador: los nombres con "/" son ILEGALES
     * como ID en Firestore (ej. "Droguería General / Sin Asignar") y rompían el carrito.
     * El nombre real de pantalla viaja en el campo "proveedorNombre".
     */
    private fun claveCarritoDoc(proveedorNombre: String): String =
        proveedorNombre.trim().replace("/", "-")

    /**
     * Escritura POR PRODUCTO del carrito compartido (multiusuario seguro):
     * cada producto escribe su propio casillero y Firestore fusiona — dos usuarios
     * editando productos distintos jamás se borran mutuamente.
     * cambios: productoId -> nueva cantidad (cantidad <= 0 quita el producto).
     */
    suspend fun guardarProductosCarrito(proveedorNombre: String, cambios: Map<String, Int>, farmaciaIdParam: String? = null, sucursalIdParam: String? = null): Boolean {
        val ids = if (farmaciaIdParam != null && sucursalIdParam != null) Pair(farmaciaIdParam, sucursalIdParam) else obtenerFarmaciaYSucursal() ?: return false
        val (farmaciaId, sucursalId) = ids
        if (proveedorNombre.isBlank() || cambios.isEmpty()) return true
        val docRef = FarmadonPaths.carritoReposicion(firestore, farmaciaId, sucursalId).document(claveCarritoDoc(proveedorNombre))
        var todoOk = true
        for ((productoId, cantidad) in cambios) {
            try {
                if (cantidad > 0) {
                    // merge escribe SOLO el casillero de este producto (y crea el documento
                    // con su nombre real si aún no existe) sin pisar lo de otros usuarios.
                    docRef.set(
                        mapOf(
                            "items" to mapOf(productoId to cantidad),
                            "proveedorNombre" to proveedorNombre.trim()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                } else {
                    try {
                        docRef.update("items.$productoId", FieldValue.delete()).await()
                    } catch (e: FirebaseFirestoreException) {
                        if (e.code != FirebaseFirestoreException.Code.NOT_FOUND) throw e
                    }
                }
            } catch (e: Exception) {
                todoOk = false
                Log.e(TAG, "Error sincronizando carrito ($proveedorNombre / $productoId = $cantidad): ${e.message}", e)
            }
        }
        return todoOk
    }

    suspend fun eliminarCarritoProveedor(proveedorNombre: String, farmaciaIdParam: String? = null, sucursalIdParam: String? = null): Boolean {
        val ids = if (farmaciaIdParam != null && sucursalIdParam != null) Pair(farmaciaIdParam, sucursalIdParam) else obtenerFarmaciaYSucursal() ?: return false
        val (farmaciaId, sucursalId) = ids
        return try {
            FarmadonPaths.carritoReposicion(firestore, farmaciaId, sucursalId).document(claveCarritoDoc(proveedorNombre)).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando carrito ($proveedorNombre): ${e.message}", e)
            false
        }
    }

    /**
     * Reserva atómica del borrador compartido para envío:
     * retorna la versión VIGENTE de los items (con cambios de otros usuarios) y retira el borrador.
     * Éxito con mapa vacío = el borrador ya no existe (otro usuario lo envió o lo vació).
     * Failure = falla real de red/servidor (jamás se confunde con "otro usuario").
     */
    suspend fun consumirCarritoParaEnvio(proveedorNombre: String): Result<Map<String, Int>> {
        val ids = obtenerFarmaciaYSucursal()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia o sucursal activa."))
        val (farmaciaId, sucursalId) = ids
        if (proveedorNombre.isBlank()) return Result.success(emptyMap())
        val docRef = FarmadonPaths.carritoReposicion(firestore, farmaciaId, sucursalId).document(claveCarritoDoc(proveedorNombre))
        return try {
            val carroVigente = firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                val itemsRaw = snap.get("items") as? Map<*, *> ?: emptyMap<Any?, Any?>()
                if (!snap.exists() || itemsRaw.isEmpty()) return@runTransaction emptyMap<String, Int>()

                val carro = mutableMapOf<String, Int>()
                itemsRaw.forEach { (clave, valor) ->
                    val cantidad = (valor as? Number)?.toInt() ?: 0
                    if (cantidad > 0 && clave != null) carro[clave.toString()] = cantidad
                }
                if (carro.isNotEmpty()) tx.delete(docRef)
                carro
            }.await() ?: emptyMap()
            Result.success(carroVigente)
        } catch (e: Exception) {
            Log.e(TAG, "Error consumiendo carrito para envío ($proveedorNombre): ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa los pedidos de compra de la sucursal activa en tiempo real.
     */
    fun observarPedidosRecientes(onErrorEscucha: ((String) -> Unit)? = null): Flow<List<PedidoCompra>> = callbackFlow {
        val ids = obtenerFarmaciaYSucursal()
        if (ids == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val (farmaciaId, sucursalId) = ids

        val query = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId)
            .orderBy("fechaEmisionMs", Query.Direction.DESCENDING)
            .limit(100)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando pedidos_compra: ${error.message}", error)
                onErrorEscucha?.invoke(error.message ?: "Sin detalle del servidor")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val pedidos = snapshot?.documents?.mapNotNull { doc ->
                mapearPedido(doc)
            } ?: emptyList()

            trySend(pedidos)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Guarda una nueva orden de compra emitida a un proveedor.
     */
    suspend fun guardarPedidoEnviado(pedido: PedidoCompra): Result<String> {
        // Blindaje raíz: jamás crear una orden sin productos reales
        if (pedido.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("La orden no tiene productos. Agrega al menos un producto con cantidad mayor a 0."))
        }
        if (pedido.items.all { it.cantidad <= 0 }) {
            return Result.failure(IllegalArgumentException("Todas las cantidades están en 0. La orden debe tener al menos un producto con cantidad."))
        }
        return try {
            val ids = obtenerFarmaciaYSucursal()
                ?: return Result.failure(IllegalStateException("No hay sesión de farmacia o sucursal activa."))
            val (farmaciaId, sucursalId) = ids

            val coleccion = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId)
            val docRef = if (pedido.id.isNotBlank()) {
                coleccion.document(pedido.id)
            } else {
                coleccion.document()
            }

            val ahoraMs = HoraServidor.ahoraMs()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaLegible = sdf.format(Date(ahoraMs))
            // Últimos 8 dígitos del ms del servidor: identidad única por ~115 días,
            // jamás el módulo de 1e6 que repetía numeración cada ~17 minutos.
            val numeroGenerado = if (pedido.numeroOrden.isNotBlank()) pedido.numeroOrden else "ORD-${ahoraMs.toString().takeLast(8)}"

            val itemsMap = pedido.items.map { item ->
                mapOf(
                    "productoId" to item.productoId,
                    "productoNombre" to item.productoNombre,
                    "presentacion" to item.presentacion,
                    "categoria" to item.categoria,
                    "codigo" to item.codigo,
                    "precioCompra" to item.precioCompra,
                    "cantidad" to item.cantidad,
                    "cantidadRecibida" to item.cantidadRecibida
                )
            }

            val data = hashMapOf(
                "id" to docRef.id,
                "numeroOrden" to numeroGenerado,
                "farmaciaId" to farmaciaId,
                "sucursalId" to sucursalId,
                "proveedorId" to pedido.proveedorId,
                "proveedorNombre" to pedido.proveedorNombre,
                "proveedorRuc" to pedido.proveedorRuc,
                "proveedorTelefono" to pedido.proveedorTelefono,
                "items" to itemsMap,
                "recepciones" to emptyList<Map<String, Any>>(),
                "totalProductos" to pedido.items.size,
                "totalUnidades" to pedido.items.sumOf { it.cantidad },
                "totalInversion" to pedido.items.sumOf { it.cantidad * it.precioCompra },
                "montoFacturadoReal" to 0.0,
                "estado" to (pedido.estado.ifBlank { "ENVIADO" }),
                "fechaEmision" to (pedido.fechaEmision.ifBlank { fechaLegible }),
                "fechaEmisionMs" to (if (pedido.fechaEmisionMs > 0) pedido.fechaEmisionMs else ahoraMs),
                "fechaRecepcion" to pedido.fechaRecepcion,
                "notas" to pedido.notas,
                "usuarioEmisor" to (pedido.usuarioEmisor.ifBlank { SessionManager.nombreUsuario.ifBlank { "Administración" } }),
                "creadoEl" to FieldValue.serverTimestamp(),
                "actualizadoEl" to FieldValue.serverTimestamp()
            )

            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando pedido: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Asienta de forma atómica y completa la recepción de mercadería física de una orden:
     * 1. Actualiza stock y lote FEFO de cada producto en inventario.
     * 2. Registra los movimientos en Kardex (ENTRADA_COMPRA).
     * 3. Crea o actualiza la factura en Cuentas por Pagar (compras_facturas).
     * 4. Registra el acta de entrega en la orden y actualiza su estado (RECIBIDO / ENTREGA_PARCIAL / COMPLETADA_AJUSTE).
     */
    suspend fun asentarRecepcionDirecta(
        pedidoId: String,
        numeroFactura: String,
        condicionPago: String,
        fechaVencimientoPago: String,
        montoFactura: Double,
        itemsRecepcion: List<ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean = false,
        usuarioEmail: String = "",
        usuarioNombre: String = "",
        idempotenciaId: String = ""
    ): Result<Unit> {
        // Fuente única — delega al repositorio unificado (1 tx para lote+stock+kardex+factura inmutable+pedido)
        return IngresoMercaderiaRepository(firestore).ingresar(
            pedidoId = pedidoId,
            numeroFactura = numeroFactura,
            condicionPago = condicionPago,
            fechaVencimientoPago = fechaVencimientoPago,
            montoFactura = montoFactura,
            items = itemsRecepcion,
            cerrarConAjuste = cerrarConAjuste,
            usuarioEmail = usuarioEmail,
            usuarioNombre = usuarioNombre,
            idempotenciaId = idempotenciaId
        )
    }

    suspend fun conciliarIngresoDeStockIndividual(
        productoId: String,
        proveedorId: String,
        cantidadIngresada: Int,
        numeroFactura: String
    ): Result<Unit> {
        // Blindaje de vida real: si el ingreso no tiene proveedor identificado
        // (devolución, ajuste, donación), NO se asigna a ninguna orden pendiente.
        // Conciliar sin origen produce órdenes mentirosas y droguerías confundidas.
        if (proveedorId.isBlank()) return Result.success(Unit)
        val ids = obtenerFarmaciaYSucursal() ?: return Result.failure(IllegalStateException("Sin sesión activa."))
        val (farmaciaId, sucursalId) = ids
        if (productoId.isBlank() || cantidadIngresada <= 0) return Result.success(Unit)

        return try {
            val pedidosPendientesSnap = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId)
                .whereIn("estado", listOf("ENVIADO", "ENTREGA_PARCIAL"))
                .get()
                .await()

            val pedidos = pedidosPendientesSnap.documents.mapNotNull { mapearPedido(it) }

            // Conciliación ESTRICTA por proveedor: la mercadería de una droguería jamás
            // descuenta la orden de OTRA droguería. Si el proveedor del ingreso no tiene
            // orden pendiente para este producto, no se concilia nada: la orden verdadera
            // se resuelve por su propio camino (cancelar / cerrar con ajuste / descartar faltante).
            val pedidoMatch = pedidos.firstOrNull { p ->
                p.proveedorId == proveedorId &&
                        p.items.any { it.productoId == productoId && it.saldoPendiente > 0 }
            }

            if (pedidoMatch != null) {
                val docRef = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId).document(pedidoMatch.id)
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val ahoraLegible = sdf.format(Date(HoraServidor.ahoraMs()))

                firestore.runTransaction { tx ->
                    val snapFresh = tx.get(docRef)
                    val pedidoFresco = mapearPedido(snapFresh)
                        ?: throw IllegalStateException("La orden conciliada ya no existe.")
                    if (pedidoFresco.estado !in listOf("ENVIADO", "ENTREGA_PARCIAL")) {
                        return@runTransaction
                    }

                    var restanteIngreso = cantidadIngresada
                    val updatedItems = pedidoFresco.items.map { item ->
                        if (item.productoId == productoId && restanteIngreso > 0 && item.saldoPendiente > 0) {
                            val tomar = minOf(item.saldoPendiente, restanteIngreso)
                            restanteIngreso -= tomar
                            item.copy(cantidadRecibida = item.cantidadRecibida + tomar)
                        } else {
                            item
                        }
                    }

                    val todasCompletas = updatedItems.all { it.cantidadRecibida >= it.cantidad }
                    val nuevoEstado = if (todasCompletas) "RECIBIDO" else "ENTREGA_PARCIAL"

                    val itemsMap = updatedItems.map { item ->
                        mapOf(
                            "productoId" to item.productoId,
                            "productoNombre" to item.productoNombre,
                            "presentacion" to item.presentacion,
                            "categoria" to item.categoria,
                            "codigo" to item.codigo,
                            "precioCompra" to item.precioCompra,
                            "cantidad" to item.cantidad,
                            "cantidadRecibida" to item.cantidadRecibida
                        )
                    }

                    tx.update(
                        docRef,
                        mapOf(
                            "items" to itemsMap,
                            "estado" to nuevoEstado,
                            "fechaRecepcion" to ahoraLegible,
                            "actualizadoEl" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error conciliando ingreso de stock individual: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cierra una orden con ajuste (cuando la droguería quebró stock y no traerá el saldo).
     * Transaccional: relee el estado real antes de escribir para no pisar cambios concurrentes.
     */
    suspend fun cerrarOrdenConAjuste(pedidoId: String, motivo: String = "Quiebre de stock en proveedor"): Result<Unit> {
        val ids = obtenerFarmaciaYSucursal()
            ?: return Result.failure(IllegalStateException("Sin sesión activa."))
        val (farmaciaId, sucursalId) = ids

        return try {
            val docRef = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId).document(pedidoId)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val ahoraLegible = sdf.format(Date(HoraServidor.ahoraMs()))

            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La orden no existe.")
                val estado = snap.getString("estado") ?: "ENVIADO"
                if (estado !in listOf("ENVIADO", "ENTREGA_PARCIAL")) {
                    throw IllegalStateException("No se puede cerrar con ajuste una orden en estado $estado.")
                }
                tx.update(
                    docRef,
                    mapOf(
                        "estado" to "COMPLETADA_AJUSTE",
                        "fechaRecepcion" to ahoraLegible,
                        "notas" to motivo,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando orden con ajuste $pedidoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Descarta quirúrgicamente un producto de un pedido enviado (por ejemplo si la droguería quebró stock).
     * Transaccional: dos descartes simultáneos se aplican ambos sin perderse.
     */
    suspend fun descartarProductoDePedido(pedidoId: String, productoId: String): Result<Unit> {
        val ids = obtenerFarmaciaYSucursal() ?: return Result.failure(IllegalStateException("Sin sesión activa."))
        val (farmaciaId, sucursalId) = ids

        return try {
            val docRef = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId).document(pedidoId)

            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La orden no existe.")
                val estado = snap.getString("estado") ?: ""
                if (estado !in listOf("ENVIADO", "ENTREGA_PARCIAL")) {
                    throw IllegalStateException("No se puede descartar productos de una orden en estado $estado.")
                }
                val pedido = mapearPedido(snap) ?: throw IllegalStateException("Pedido no encontrado.")

                val itemsActualizados = pedido.items.map { item ->
                    if (item.productoId == productoId) {
                        item.copy(cantidad = item.cantidadRecibida)
                    } else item
                }

                if (itemsActualizados == pedido.items) {
                    throw IllegalStateException("El producto no pertenece a esta orden.")
                }

                val todasCompletas = itemsActualizados.all { it.cantidadRecibida >= it.cantidad }
                val nuevoEstado = if (todasCompletas) "COMPLETADA_AJUSTE" else "ENTREGA_PARCIAL"

                val itemsMap = itemsActualizados.map { item ->
                    mapOf(
                        "productoId" to item.productoId,
                        "productoNombre" to item.productoNombre,
                        "presentacion" to item.presentacion,
                        "categoria" to item.categoria,
                        "codigo" to item.codigo,
                        "precioCompra" to item.precioCompra,
                        "cantidad" to item.cantidad,
                        "cantidadRecibida" to item.cantidadRecibida
                    )
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val updates = mutableMapOf<String, Any>(
                    "items" to itemsMap,
                    "estado" to nuevoEstado,
                    "totalUnidades" to itemsActualizados.sumOf { it.cantidad },
                    "totalInversion" to itemsActualizados.sumOf { it.cantidad * it.precioCompra },
                    "actualizadoEl" to FieldValue.serverTimestamp()
                )
                if (todasCompletas) {
                    updates["fechaRecepcion"] = sdf.format(Date(HoraServidor.ahoraMs()))
                }
                tx.update(docRef, updates)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error descartando producto $productoId de pedido $pedidoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cancela una orden que aún espera entrega. Transaccional: si otro usuario
     * ya recibió mercadería o cerró la orden, se rechaza con la verdad real.
     */
    /**
     * Índice de lotes EXISTENTES para los productos de una orden (lectura puntual al abrir
     * la recepción). Llave = número de lote en mayúsculas. Si falla la lectura retorna vacío:
     * el flujo de recepción continúa normal, solo sin avisos inteligentes.
     */
    suspend fun cargarIndiceLotesDeProductos(productoIds: List<String>): Map<String, List<LoteExistenteVista>> {
        val ids = obtenerFarmaciaYSucursal() ?: return emptyMap()
        val (farmaciaId, sucursalId) = ids
        val indice = mutableMapOf<String, MutableList<LoteExistenteVista>>()
        return try {
            for (productoId in productoIds.distinct().filter { it.isNotBlank() }) {
                val snap = FarmadonPaths.sucursal(firestore, farmaciaId, sucursalId)
                    .collection("inventario").document(productoId).get().await()
                @Suppress("UNCHECKED_CAST")
                val lotesRaw = snap.get("lotes") as? Map<String, Any> ?: continue
                lotesRaw.forEach { (_, data) ->
                    val m = data as? Map<*, *> ?: return@forEach
                    val numero = (m["numero"] as? String)?.trim()?.uppercase() ?: ""
                    if (numero.isBlank()) return@forEach
                    val disp = (m["cantidad"] as? Number)?.toDouble() ?: 0.0
                    val bloq = (m["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0
                    val vista = LoteExistenteVista(
                        productoId = productoId,
                        productoNombre = snap.getString("name") ?: "",
                        numero = numero,
                        cantidad = disp + bloq,
                        vencimiento = (m["vencimiento"] as? String) ?: ""
                    )
                    indice.getOrPut(numero) { mutableListOf() }.add(vista)
                }
            }
            indice
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando índice de lotes: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun cancelarPedido(pedidoId: String): Result<Unit> {
        val ids = obtenerFarmaciaYSucursal()
            ?: return Result.failure(IllegalStateException("Sin sesión activa."))
        val (farmaciaId, sucursalId) = ids

        return try {
            val docRef = FarmadonPaths.pedidosCompra(firestore, farmaciaId, sucursalId).document(pedidoId)

            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La orden no existe.")
                val estado = snap.getString("estado") ?: "ENVIADO"
                if (estado != "ENVIADO") {
                    throw IllegalStateException("Solo se puede cancelar una orden esperando entrega (estado actual: $estado).")
                }
                tx.update(
                    docRef,
                    mapOf(
                        "estado" to "CANCELADO",
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelando pedido $pedidoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapearPedido(doc: com.google.firebase.firestore.DocumentSnapshot): PedidoCompra? {
        return try {
            val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
            val items = itemsRaw.map { m ->
                ItemPedidoCompra(
                    productoId = m["productoId"] as? String ?: "",
                    productoNombre = m["productoNombre"] as? String ?: "",
                    presentacion = m["presentacion"] as? String ?: "Und",
                    categoria = m["categoria"] as? String ?: "",
                    codigo = m["codigo"] as? String ?: "",
                    precioCompra = (m["precioCompra"] as? Number)?.toDouble() ?: 0.0,
                    cantidad = (m["cantidad"] as? Number)?.toInt() ?: 0,
                    cantidadRecibida = (m["cantidadRecibida"] as? Number)?.toInt() ?: 0
                )
            }

            val recepcionesRaw = doc.get("recepciones") as? List<Map<String, Any>> ?: emptyList()
            val recepciones = recepcionesRaw.map { r ->
                val rItemsRaw = r["items"] as? List<Map<String, Any>> ?: emptyList()
                val rItems = rItemsRaw.map { ri ->
                    ItemRecepcionEntrega(
                        productoId = ri["productoId"] as? String ?: "",
                        productoNombre = ri["productoNombre"] as? String ?: "",
                        presentacion = ri["presentacion"] as? String ?: "Und",
                        loteNumero = ri["loteNumero"] as? String ?: "",
                        vencimiento = ri["vencimiento"] as? String ?: "",
                        cantidadComprada = (ri["cantidadComprada"] as? Number)?.toInt() ?: 0,
                        bonificacionGratis = (ri["bonificacionGratis"] as? Number)?.toInt() ?: 0,
                        cantidadTotal = (ri["cantidadTotal"] as? Number)?.toInt() ?: 0,
                        costoUnitarioReal = (ri["costoUnitarioReal"] as? Number)?.toDouble() ?: 0.0,
                        costoTotalReal = (ri["costoTotalReal"] as? Number)?.toDouble() ?: 0.0
                    )
                }

                RecepcionEntrega(
                    id = r["id"] as? String ?: "",
                    fechaLegible = r["fechaLegible"] as? String ?: "",
                    fechaMs = (r["fechaMs"] as? Number)?.toLong() ?: 0L,
                    usuarioNombre = r["usuarioNombre"] as? String ?: "",
                    usuarioEmail = r["usuarioEmail"] as? String ?: "",
                    numeroFactura = r["numeroFactura"] as? String ?: "",
                    condicionPago = r["condicionPago"] as? String ?: "Contado",
                    fechaVencimientoPago = r["fechaVencimientoPago"] as? String ?: "",
                    montoFactura = (r["montoFactura"] as? Number)?.toDouble() ?: 0.0,
                    items = rItems,
                    notas = r["notas"] as? String ?: "",
                    cierreConAjuste = r["cierreConAjuste"] as? Boolean ?: false
                )
            }

            PedidoCompra(
                id = doc.getString("id") ?: doc.id,
                numeroOrden = doc.getString("numeroOrden") ?: "",
                farmaciaId = doc.getString("farmaciaId") ?: "",
                sucursalId = doc.getString("sucursalId") ?: "",
                proveedorId = doc.getString("proveedorId") ?: "",
                proveedorNombre = doc.getString("proveedorNombre") ?: "",
                proveedorRuc = doc.getString("proveedorRuc") ?: "",
                proveedorTelefono = doc.getString("proveedorTelefono") ?: "",
                items = items,
                recepciones = recepciones,
                totalProductos = doc.getLong("totalProductos")?.toInt() ?: items.size,
                totalUnidades = doc.getLong("totalUnidades")?.toInt() ?: items.sumOf { it.cantidad },
                totalInversion = doc.getDouble("totalInversion") ?: items.sumOf { it.cantidad * it.precioCompra },
                montoFacturadoReal = doc.getDouble("montoFacturadoReal") ?: 0.0,
                estado = doc.getString("estado") ?: "ENVIADO",
                fechaEmision = doc.getString("fechaEmision") ?: "",
                fechaEmisionMs = doc.getLong("fechaEmisionMs") ?: 0L,
                fechaRecepcion = doc.getString("fechaRecepcion") ?: "",
                notas = doc.getString("notas") ?: "",
                usuarioEmisor = doc.getString("usuarioEmisor") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapeando pedido ${doc.id}: ${e.message}", e)
            null
        }
    }
}
