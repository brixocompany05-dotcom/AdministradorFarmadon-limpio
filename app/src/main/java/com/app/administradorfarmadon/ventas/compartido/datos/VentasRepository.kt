package com.app.administradorfarmadon.ventas.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.inventario.compartido.logica.CostoRealLote
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EmisorComprobante
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucionParam
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.LoteConsumido
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import com.app.administradorfarmadon.configuracion.pos.datos.PosConfigRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * REPOSITORIO CENTRAL DE VENTAS Y FACTURACIÓN POS (R1/R3/R8/R10).
 *
 * Cumple todas las reglas técnicas infranqueables:
 * 1. Transacciones Firestore: TODAS las lecturas antes de cualquier escritura.
 * 2. Cero serverTimestamp() dentro de listas o mapas (R10) — solo campos de primer nivel.
 * 3. Aislamiento estricto por farmaciaId + sucursalId (R1).
 * 4. Idempotencia anti doble-tap (v_{uuid}).
 * 5. Revalidación de precio vivo en tiempo real (aborto si difiere > 0.005).
 * 6. FEFO multi-línea con reconstrucción de stock en memoria de trabajo.
 * 7. Resolución robusta de lote (llave directa -> campo loteId -> resolverLote).
 * 8. Lote en 0 conserva ventasRegistradas y limpia lotePrioritarioId si quedó en 0.
 * 9. Cero índices compuestos: consultas por igualdad de 1 solo campo, orden en memoria.
 * 10. Errores veraces con e.message.
 */
class VentasRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    private val posConfigRepo = PosConfigRepository(db)

    companion object {
        private const val TAG = "VentasRepository"
        val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")
        fun formatoDiaClave(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TIMEZONE_LIMA }

        private fun redondear2(valor: Double): Double =
            kotlin.math.round(valor * 100.0) / 100.0
    }

    private fun ids(): Pair<String, String>? {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return null
        return f to s
    }

    // ───────────────────────────── FUNCIÓN 1: REGISTRAR VENTA ─────────────────────────────

    /**
     * OPERACIÓN ATÓMICA DE VENTA COMPLETA (R1/R3/R8/R10).
     *
     * Valida caja abierta, correlativo, precio vivo, stock FEFO, actualiza productos,
     * escribe comprobante, kardex, movimientos de caja y actualiza el estado de caja.
     */
    suspend fun registrarVenta(
        items: List<ItemVenta>,
        cliente: ClienteDeVenta = ClienteDeVenta(),
        pagos: List<PagoVenta>,
        descuento: Double = 0.0,
        confirmoReceta: Boolean = false,
        idempotenciaId: String = "",
        autorizadoPorId: String = "",
        autorizadoPorNombre: String = "",
        autorizadoPorRol: String = ""
    ): Result<Venta> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))

        // Validaciones previas a la transacción
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("El carrito no tiene productos para cobrar."))
        }
        if (items.any { it.cantidad <= 0 }) {
            return Result.failure(IllegalArgumentException("Cada producto debe tener una cantidad mayor a 0."))
        }
        if (items.any { it.precioUnitario <= 0.0 }) {
            val itemCero = items.first { it.precioUnitario <= 0.0 }
            return Result.failure(IllegalArgumentException("El producto '${itemCero.nombreProducto}' (${itemCero.presentacionNombre}) tiene precio S/ 0.00 y no puede ser vendido."))
        }
        val subtotalCalculado = redondear2(items.sumOf { it.precioUnitario * it.cantidad })
        if (descuento < 0.0 || (subtotalCalculado > 0.0 && descuento >= subtotalCalculado)) {
            return Result.failure(IllegalArgumentException("El descuento no puede ser negativo ni igual o mayor al subtotal."))
        }
        val descuentoRedondeado = redondear2(descuento)
        val totalCalculado = redondear2((subtotalCalculado - descuentoRedondeado).coerceAtLeast(0.0))

        // Validaciones de reglas de negocio POS de la sede (R1/R3)
        val posConfig = posConfigRepo.obtener(sucursalId)
        if (!posConfig.estaVigente) {
            return Result.failure(
                IllegalStateException("APERTURA PENDIENTE: La sede no tiene reglas de POS guardadas en Firestore. Debe configurarse y guardarse en Configuración > Ventas/POS antes de operar.")
            )
        }
        if (descuentoRedondeado > 0.0) {
            val pctDescuento = if (subtotalCalculado > 0.0) (descuentoRedondeado / subtotalCalculado) * 100.0 else 0.0
            if (posConfig.excedeLimitesDescuento(pctDescuento, descuentoRedondeado)) {
                val maxMontoStr = String.format(Locale.US, "%.2f", posConfig.descuento.maxMonto)
                val descStr = String.format(Locale.US, "%.2f", descuentoRedondeado)
                val pctStr = String.format(Locale.US, "%.1f", pctDescuento)
                return Result.failure(
                    IllegalArgumentException(
                        "El descuento (S/ $descStr / $pctStr%) supera el límite comercial permitido de la sede (${posConfig.descuento.maxPct}% / S/ $maxMontoStr)."
                    )
                )
            }
        }

        if (posConfig.receta.exigirConfirmacion && items.any { it.requiereReceta } && !confirmoReceta) {
            return Result.failure(
                IllegalArgumentException("Esta venta contiene medicamentos bajo receta médica obligatoria.")
            )
        }

        if (pagos.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Debe ingresar al menos un método de pago para registrar la venta.")
            )
        }

        // Regla de cuadre de caja: el vuelto solo puede nacer del sobrepago en efectivo
        val pagosNoEfectivo = redondear2(pagos.filter { it.tipoId != "EFECTIVO" }.sumOf { it.monto })
        if (pagosNoEfectivo > totalCalculado + 0.009) {
            val totalStr = String.format(Locale.US, "%.2f", totalCalculado)
            val noEfectivoStr = String.format(Locale.US, "%.2f", pagosNoEfectivo)
            return Result.failure(
                IllegalArgumentException(
                    "El pago sin efectivo (S/ $noEfectivoStr) supera el total de la venta (S/ $totalStr). " +
                        "El exceso no puede devolverse por ese canal: cobra lo exacto o combina con efectivo."
                )
            )
        }

        val sumaPagos = redondear2(pagos.sumOf { it.monto })
        if (sumaPagos < totalCalculado - 0.01) {
            val totalStr = String.format(Locale.US, "%.2f", totalCalculado)
            val pagoStr = String.format(Locale.US, "%.2f", sumaPagos)
            return Result.failure(IllegalArgumentException("El monto recibido (S/ $pagoStr) no cubre el total de la venta (S/ $totalStr)."))
        }

        val montoRecibido = sumaPagos
        val vuelto = redondear2((montoRecibido - totalCalculado).coerceAtLeast(0.0))

        val rawIdem = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }
        val ventaId = if (rawIdem.startsWith("v_")) rawIdem else "v_$rawIdem"

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val diaClave = formatoDiaClave().format(Date(ahoraMs))

            val ventaRef = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId)
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
            val contadorRef = FarmadonPaths.contadores(db, farmaciaId, sucursalId).document("ventas")

            val distinctProductIds = items.map { it.productoId }.distinct()

            var ventaResult: Venta? = null

            db.runTransaction { tx ->
                // ── 1. TODAS LAS LECTURAS ANTES DE CUALQUIER ESCRITURA (Regla 1) ──
                val ventaSnap = tx.get(ventaRef)
                if (ventaSnap.exists()) {
                    // Idempotencia anti doble-tap: ya existía la venta
                    val existente = parseVenta(ventaSnap.id, ventaSnap.data)
                    if (existente != null) {
                        ventaResult = existente
                        return@runTransaction
                    }
                }

                val punteroSnap = tx.get(pointerRef)
                val contadorSnap = tx.get(contadorRef)
                val sucursalRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
                val sucursalSnap = tx.get(sucursalRef)
                val emisorRef = FarmadonPaths.facturacionEmisor(db, farmaciaId)
                val emisorSnap = tx.get(emisorRef)

                val metodosDocRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId).collection("catalogos").document("metodosPago")
                val metodosSnap = tx.get(metodosDocRef)

                val productSnaps = distinctProductIds.associateWith { prodId ->
                    tx.get(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId))
                }

                // ── 2. VALIDAR CAJA ABIERTA, CONTRATO DE PAGO Y FACTURACIÓN VERIFICADA (FASE F2) ──
                @Suppress("UNCHECKED_CAST")
                val instanciasMap = (metodosSnap.get("instancias") as? Map<String, Any?>) ?: emptyMap()
                val instanciasActivas = instanciasMap.values.filterIsInstance<Map<String, Any?>>().filter {
                    it["activa"] == true
                }
                if (instanciasActivas.isEmpty()) {
                    throw IllegalStateException("APERTURA PENDIENTE: La sede no tiene métodos de pago activos configurados. Activa al menos un método en Configuración > Métodos de Pago.")
                }
                for (pago in pagos) {
                    val instanciaValida = instanciasActivas.firstOrNull { inst ->
                        val instId = inst["id"] as? String ?: ""
                        val instTipo = inst["tipoId"] as? String ?: ""
                        (pago.instanciaId.isNotBlank() && instId == pago.instanciaId) || (instTipo == pago.tipoId)
                    }
                    if (instanciaValida == null) {
                        throw IllegalArgumentException("El método de pago '${pago.nombreMetodo.ifBlank { pago.tipoId }}' no está activo para esta sede.")
                    }
                    val tipoIdLimpio = (instanciaValida["tipoId"] as? String) ?: pago.tipoId
                    val tipoInfo = com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS.firstOrNull { it.id == tipoIdLimpio }
                    if (tipoInfo?.requiereOperacion == true && pago.numeroOperacion.isBlank()) {
                        throw IllegalArgumentException("El método de pago '${pago.nombreMetodo.ifBlank { tipoIdLimpio }}' requiere número de operación obligatorio.")
                    }
                }
                val emisorVerificado = emisorSnap.getBoolean("verificadoOk") == true
                if (!emisorVerificado) {
                    throw IllegalStateException("FACTURACIÓN ELECTRÓNICA PENDIENTE: El administrador debe completar y verificar el emisor en Configuración → Facturación Electrónica antes de cobrar.")
                }

                val estadoCajaStr = punteroSnap.getString("estado") ?: CajaSesion.ESTADO_CERRADA
                if (estadoCajaStr != CajaSesion.ESTADO_ABIERTA) {
                    throw IllegalStateException("La caja está cerrada. Ábrela para cobrar.")
                }
                val sesionId = punteroSnap.getString("sesionId").orEmpty()
                if (sesionId.isBlank()) {
                    throw IllegalStateException("No hay un turno de caja válido asociado a la caja abierta.")
                }
                val aperturaCajaMs = (punteroSnap.get("aperturaMs") as? Number)?.toLong() ?: 0L
                if (aperturaCajaMs > 0L) {
                    val tzLima = TimeZone.getTimeZone("America/Lima")
                    val calApertura =
                        Calendar.getInstance(tzLima).apply { timeInMillis = aperturaCajaMs }
                    val calHoy = Calendar.getInstance(tzLima).apply { timeInMillis = ahoraMs }
                    val esDiaAnterior = calApertura.get(Calendar.YEAR) < calHoy.get(Calendar.YEAR) ||
                            (calApertura.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                             calApertura.get(Calendar.DAY_OF_YEAR) < calHoy.get(Calendar.DAY_OF_YEAR))
                    if (esDiaAnterior) {
                        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tzLima }
                        throw IllegalStateException(
                            "Existe una caja abierta pendiente de cierre del ${fmt.format(Date(aperturaCajaMs))}. " +
                            "Por seguridad contable y aislamiento de jornadas, debe cerrarla en Cierre de Caja antes de registrar ventas hoy."
                        )
                    }
                }

                // ── 3. CORRELATIVO Y COMPROBANTE (Series por sucursal - FASE F0) ──
                val tipoComprobante = if (cliente.tipoDocumento == "RUC" && cliente.numeroDocumento.length == 11) "FACTURA" else "BOLETA"
                val serieBoleta = sucursalSnap.getString("serieBoleta")?.trim().orEmpty()
                val serieFactura = sucursalSnap.getString("serieFactura")?.trim().orEmpty()
                val serieNCBoleta = sucursalSnap.getString("serieNotaCreditoBoleta")?.trim().orEmpty()
                val serieNCFactura = sucursalSnap.getString("serieNotaCreditoFactura")?.trim().orEmpty()

                if (serieBoleta.isBlank() || serieFactura.isBlank() || serieNCBoleta.isBlank() || serieNCFactura.isBlank()) {
                    throw IllegalStateException("APERTURA PENDIENTE: La sede no tiene las 4 series fiscales completas (B, F, BC, FC). Configura las series en Sedes.")
                }

                val serie = if (tipoComprobante == "FACTURA") serieFactura else serieBoleta
                val campoContador = if (tipoComprobante == "FACTURA") "ultimaFactura" else "ultimaBoleta"

                val ultimoCorrelativo = if (contadorSnap.exists()) {
                    (contadorSnap.get(campoContador) as? Number)?.toLong() ?: 0L
                } else 0L
                val nuevoCorrelativo = ultimoCorrelativo + 1L
                val numeroCompleto = String.format(Locale.US, "%s-%06d", serie, nuevoCorrelativo)

                // ── 4. EVALUAR CADA PRODUCTO Y LÍNEA DE VENTA ──
                val itemsFinalesDeVenta = mutableListOf<ItemVenta>()
                val productosUpdates = mutableMapOf<String, Map<String, Any>>()
                val kardexPorProducto = mutableMapOf<String, MutableList<LoteConsumido>>()

                // Agrupamos las líneas de venta por producto para aplicar mutaciones acumulativas
                val itemsPorProducto = items.groupBy { it.productoId }

                for ((prodId, lineasCarrito) in itemsPorProducto) {
                    val prodSnap = productSnaps[prodId]
                        ?: throw IllegalStateException("No se pudo leer el producto $prodId en inventario.")
                    if (!prodSnap.exists()) {
                        throw IllegalStateException("El producto con ID '$prodId' no existe en el inventario.")
                    }

                    val molde = ProductoParser.parseToMolde(prodSnap)
                        ?: throw IllegalStateException("No se pudo procesar el producto con ID '$prodId'.")
                    if (!molde.activo) {
                        throw IllegalStateException("El producto '${molde.nombre}' está inactivo y no se puede vender.")
                    }

                    // Mapa de trabajo mutable para los lotes de este producto
                    @Suppress("UNCHECKED_CAST")
                    val lotesDeTrabajo = (prodSnap.get("lotes") as? Map<*, *>)?.mapNotNull { (k, v) ->
                        if (v is Map<*, *>) k.toString() to (v as Map<String, Any>).toMutableMap() else null
                    }?.toMap(mutableMapOf()) ?: mutableMapOf<String, MutableMap<String, Any>>()

                    val lotesConsumidosTotalesProducto = mutableListOf<LoteConsumido>()

                    for (item in lineasCarrito) {
                        // Resolución de presentación (con cálculo de precio vivo y unidad coherente si es descontinuada)
                        val pres = if (item.presentacionId.startsWith("descontinuada_")) {
                            val cant = item.presentacionId.removePrefix("descontinuada_").toIntOrNull() ?: 1
                            val precioBaseUnitario = (molde.presentaciones.firstOrNull { it.cantidad == 1 } ?: molde.presentaciones.firstOrNull())?.let {
                                if (it.cantidad > 0) it.precioventa / it.cantidad else it.precioventa
                            } ?: 0.0
                            val precioVivoDescontinuada = redondear2(precioBaseUnitario * cant)
                            PresentacionProducto(
                                presentacionId = item.presentacionId,
                                nombre = item.presentacionNombre,
                                cantidad = cant,
                                unidadMedida = molde.contenidoUnidad.ifBlank { molde.empaque.ifBlank { "unidad" } },
                                precioventa = precioVivoDescontinuada
                            )
                        } else {
                            molde.presentaciones.firstOrNull { it.presentacionId == item.presentacionId }
                                ?: throw IllegalStateException("La presentación '${item.presentacionNombre}' de '${molde.nombre}' ya no existe.")
                        }

                        // Revalidación de precio vivo (Regla 5: tolerancia 0.005)
                        val precioVivo = pres.precioventa
                        if (kotlin.math.abs(precioVivo - item.precioUnitario) > 0.005) {
                            throw IllegalStateException("El precio de ${molde.nombre} cambió, revisa el carrito.")
                        }

                        // Revalidación de receta médica
                        if (molde.requiereReceta && !confirmoReceta) {
                            throw IllegalStateException("El producto '${molde.nombre}' requiere receta médica.")
                        }

                        // Reconstrucción de molde de trabajo con el estado vivo de lotes (Regla 6)
                        val lotesMoldeTrabajo = lotesDeTrabajo.mapValues { (k, v) ->
                            LoteProducto(
                                numero = v["numero"] as? String ?: k,
                                vencimiento = v["vencimiento"] as? String ?: "",
                                cantidad = ((v["cantidad"] as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0),
                                cantidadBloqueada = (v["cantidadBloqueada"] as? Number)?.toDouble() ?: 0.0,
                                proveedorNombre = (v["proveedor"] as? String) ?: (v["proveedorNombre"] as? String) ?: "",
                                proveedorId = v["proveedorId"] as? String ?: "",
                                nroFactura = (v["factura"] as? String) ?: (v["nroFactura"] as? String) ?: "",
                                costoUltimoIngreso = (v["costoCompra"] as? Number)?.toDouble() ?: 0.0,
                                costoCompraUnitario = (v["costoUnitario"] as? Number)?.toDouble() ?: 0.0,
                                fecha = v["fecha"] as? String ?: "",
                                loteId = v["loteId"] as? String ?: FechaVencimientoHelper.llaveLote(v["numero"] as? String ?: k),
                                ventasRegistradas = (v["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                        val moldeTrabajo = molde.copy(
                            precioStock = molde.precioStock.copy(),
                            productoBase = molde.productoBase.copy(),
                            loteInfo = molde.loteInfo.copy(lotes = lotesMoldeTrabajo)
                        )

                        // Escalamiento de presentación para FEFO
                        val presEscalada = pres.copy(cantidad = pres.cantidad.coerceAtLeast(1) * item.cantidad)
                        val descuentoResult = UnidadVentaHelper.calcularDescuentoFEFO(moldeTrabajo, presEscalada)
                        if (descuentoResult.isFailure) {
                            throw IllegalStateException(descuentoResult.exceptionOrNull()?.message ?: "Stock insuficiente para '${molde.nombre}'.")
                        }

                        val descuentos = descuentoResult.getOrThrow()
                        val lotesConsumidosLinea = mutableListOf<LoteConsumido>()

                        for (d in descuentos) {
                            val entradaLote = buscarEntradaLote(lotesDeTrabajo, d.loteId, d.loteNumero)
                                ?: throw IllegalStateException("Lote '${d.loteNumero}' no encontrado en '${molde.nombre}'.")

                            val (loteKey, loteData) = entradaLote
                            val cantActual = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                            val nuevaCant = (cantActual - d.cantidadADescontar).coerceAtLeast(0.0)
                            loteData["cantidad"] = nuevaCant

                            val ventasRegActual = (loteData["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                            loteData["ventasRegistradas"] = ventasRegActual + d.cantidadADescontar

                            lotesDeTrabajo[loteKey] = loteData

                            val costoUnitarioLote = redondear2(CostoRealLote.costoUnitario(loteData))
                            if (costoUnitarioLote <= 0.0 || loteData["noValorizado"] == true) {
                                throw IllegalStateException("El lote '${d.loteNumero}' de '${molde.nombre}' no tiene costo registrado (S/ 0.00). Muestras o productos sin compra no se pueden vender por POS.")
                            }
                            val lc = LoteConsumido(
                                loteId = d.loteId,
                                loteNumero = d.loteNumero,
                                vencimiento = loteData["vencimiento"] as? String ?: "",
                                cantidadFisica = d.cantidadADescontar,
                                costoUnitarioReal = costoUnitarioLote
                            )
                            lotesConsumidosLinea.add(lc)
                            lotesConsumidosTotalesProducto.add(lc)
                        }

                        val subtotalLinea = redondear2(item.precioUnitario * item.cantidad)
                        val costoTotalRealLinea = redondear2(lotesConsumidosLinea.sumOf { it.cantidadFisica * it.costoUnitarioReal })
                        itemsFinalesDeVenta.add(
                            item.copy(
                                lotesConsumidos = lotesConsumidosLinea,
                                subtotal = subtotalLinea,
                                cantidadDevuelta = 0,
                                costoTotalReal = costoTotalRealLinea
                            )
                        )
                    }

                    kardexPorProducto[prodId] = lotesConsumidosTotalesProducto

                    // Resumen de stock tras mutación de lotes
                    val (nuevoStockDisp, nuevoStockTotal, vencMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesDeTrabajo)
                    val updatesProd = mutableMapOf<String, Any>(
                        "lotes" to lotesDeTrabajo,
                        "stock" to nuevoStockDisp,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to vencMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )

                    // Regla 8: Si lote prioritario quedó en < 0.001, limpiar prioridad
                    val prioId = molde.lotePrioritarioId.trim()
                    if (prioId.isNotBlank()) {
                        val prioEntrada = buscarEntradaLote(lotesDeTrabajo, prioId, prioId)
                        val cantPrio = (prioEntrada?.second?.get("cantidad") as? Number)?.toDouble() ?: 0.0
                        if (cantPrio < 0.001) {
                            updatesProd["lotePrioritarioId"] = ""
                            updatesProd["lotePrioritarioPor"] = ""
                            updatesProd["lotePrioritarioPorRol"] = ""
                        }
                    }

                    productosUpdates[prodId] = updatesProd
                }

                // ── 5. TODAS LAS ESCRITURAS (Regla 1) ──

                // 5.1 Actualizar productos en inventario
                for ((prodId, updates) in productosUpdates) {
                    tx.update(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId), updates)
                }

                // 5.2 Escribir Kardex por cada producto vendido (con motivo y notas consistentes)
                for ((prodId, lotesCons) in kardexPorProducto) {
                    val kardexRef = FarmadonPaths.movimientos(db, farmaciaId, sucursalId).document("venta_${ventaId}_${prodId}")
                    val totalDescontado = lotesCons.sumOf { it.cantidadFisica }
                    val lotesResumen = lotesCons.joinToString(", ") { "${it.loteNumero} (${it.cantidadFisica})" }
                    val primerNombre = itemsFinalesDeVenta.firstOrNull { it.productoId == prodId }?.nombreProducto ?: ""
                    val primerEmpaque = itemsFinalesDeVenta.firstOrNull { it.productoId == prodId }?.empaque ?: ""

                    val kardexData = mapOf(
                        "id" to kardexRef.id,
                        "tipo" to "SALIDA_VENTA",
                        "productoId" to prodId,
                        "productoNombre" to primerNombre,
                        "empaque" to primerEmpaque,
                        "cantidadTotal" to -totalDescontado,
                        "cantidad" to -totalDescontado,
                        "loteNumero" to lotesResumen,
                        "referenciaNumero" to numeroCompleto,
                        "referenciaId" to ventaId,
                        "cajaSesionId" to sesionId,
                        "usuarioId" to SessionManager.idCajera,
                        "usuarioNombre" to SessionManager.nombreUsuario,
                        "usuarioEmail" to SessionManager.email,
                        "origen" to "VENTA_POS",
                        "motivo" to "Venta $numeroCompleto",
                        "notas" to "Venta $numeroCompleto",
                        "fecha" to FieldValue.serverTimestamp(),
                        "fechaMs" to ahoraMs
                    )
                    tx.set(kardexRef, kardexData)
                }

                // 5.3 Incrementar correlativo de comprobante
                tx.set(contadorRef, mapOf(campoContador to nuevoCorrelativo), SetOptions.merge())

                // 5.4 Escribir documento de Venta completo
                val cId = cliente.clienteId.ifBlank { cliente.numeroDocumento.trim() }
                val ventaData = mapOf(
                    "id" to ventaId,
                    "numeroCompleto" to numeroCompleto,
                    "tipoComprobante" to tipoComprobante,
                    "serie" to serie,
                    "correlativo" to nuevoCorrelativo,
                    "clienteId" to cId,
                    "cliente" to mapOf(
                        "tipoDocumento" to cliente.tipoDocumento,
                        "numeroDocumento" to cliente.numeroDocumento,
                        "nombre" to cliente.nombre,
                        "clienteId" to cId
                    ),
                    "items" to itemsFinalesDeVenta.map { item ->
                        mapOf(
                            "productoId" to item.productoId,
                            "nombreProducto" to item.nombreProducto,
                            "empaque" to item.empaque,
                            "presentacionId" to item.presentacionId,
                            "presentacionNombre" to item.presentacionNombre,
                            "cantidad" to item.cantidad,
                            "precioUnitario" to redondear2(item.precioUnitario),
                            "subtotal" to redondear2(item.subtotal),
                            "requiereReceta" to item.requiereReceta,
                            "cantidadDevuelta" to 0,
                            "costoTotalReal" to redondear2(item.costoTotalReal),
                            "lotesConsumidos" to item.lotesConsumidos.map { lc ->
                                mapOf(
                                    "loteId" to lc.loteId,
                                    "loteNumero" to lc.loteNumero,
                                    "vencimiento" to lc.vencimiento,
                                    "cantidadFisica" to lc.cantidadFisica,
                                    "costoUnitarioReal" to redondear2(lc.costoUnitarioReal)
                                )
                            }
                        )
                    },
                    "totalItems" to itemsFinalesDeVenta.sumOf { it.cantidad },
                    "costoTotalReal" to redondear2(itemsFinalesDeVenta.sumOf { it.costoTotalReal }),
                    "subtotal" to subtotalCalculado,
                    "descuento" to descuentoRedondeado,
                    "total" to totalCalculado,
                    "pagos" to pagos.map { p ->
                        mapOf(
                            "tipoId" to p.tipoId,
                            "instanciaId" to p.instanciaId,
                            "nombreMetodo" to p.nombreMetodo,
                            "monto" to redondear2(p.monto),
                            "numeroOperacion" to p.numeroOperacion
                        )
                    },
                    "montoRecibido" to montoRecibido,
                    "vuelto" to vuelto,
                    "estado" to Venta.ESTADO_COMPLETADA,
                    "cajaSesionId" to sesionId,
                    "cajeroId" to SessionManager.idCajera,
                    "cajeroNombre" to SessionManager.nombreUsuario,
                    "fechaHoraMs" to ahoraMs,
                    "diaClave" to diaClave,
                    "estadoFiscal" to "PENDIENTE",
                    "moduloOrigen" to "POS",
                    "autorizadoPorId" to autorizadoPorId.trim(),
                    "autorizadoPorNombre" to autorizadoPorNombre.trim(),
                    "autorizadoPorRol" to autorizadoPorRol.trim(),
                    "creadoEl" to FieldValue.serverTimestamp()
                )
                tx.set(ventaRef, ventaData)

                // 5.5 Registrar documento en la bandeja fiscal (FASE 12)
                val factDocRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(ventaId)
                val factDocData = mapOf(
                    "id" to ventaId,
                    "tipo" to tipoComprobante,
                    "serie" to serie,
                    "correlativo" to nuevoCorrelativo,
                    "numeroCompleto" to numeroCompleto,
                    "clienteTipoDoc" to cliente.tipoDocumento,
                    "clienteNumeroDoc" to cliente.numeroDocumento,
                    "clienteNombre" to cliente.nombre,
                    "ventaId" to ventaId,
                    "devolucionId" to "",
                    "sucursalId" to sucursalId,
                    "total" to totalCalculado,
                    "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                    "fechaMs" to ahoraMs,
                    "motivo" to "",
                    "moduloOrigen" to "POS"
                )
                tx.set(factDocRef, factDocData)

                // 5.6 Actualizar puntero de caja (ventasPorMetodo y cantidadVentas)
                @Suppress("UNCHECKED_CAST")
                val ventasPorMetodo = (punteroSnap.get("ventasPorMetodo") as? Map<String, Any>)
                    ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 }
                    ?.toMutableMap() ?: mutableMapOf()

                var vueltoRestantePuntero = vuelto
                for (p in pagos) {
                    val montoNetoMetodo = if (p.tipoId == "EFECTIVO") {
                        val asignado = minOf(vueltoRestantePuntero, p.monto)
                        vueltoRestantePuntero = redondear2(vueltoRestantePuntero - asignado)
                        redondear2(p.monto - asignado)
                    } else redondear2(p.monto)
                    val actual = ventasPorMetodo[p.tipoId] ?: 0.0
                    ventasPorMetodo[p.tipoId] = redondear2(actual + montoNetoMetodo)
                }
                val cantVentasActual = (punteroSnap.get("cantidadVentas") as? Number)?.toInt() ?: 0
                tx.update(
                    pointerRef,
                    mapOf(
                        "ventasPorMetodo" to ventasPorMetodo,
                        "cantidadVentas" to cantVentasActual + 1
                    )
                )

                // 5.7 Registrar movimientos de caja individuales por cada método de pago (monto neto ingresado al cajón/cuenta)
                var vueltoRestanteMovs = vuelto
                pagos.forEachIndexed { index, p ->
                    val montoNetoMov = if (p.tipoId == "EFECTIVO") {
                        val asignado = minOf(vueltoRestanteMovs, p.monto)
                        vueltoRestanteMovs = redondear2(vueltoRestanteMovs - asignado)
                        redondear2(p.monto - asignado)
                    } else redondear2(p.monto)
                    if (montoNetoMov > 0.0) {
                        val movRef = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId).document("mov_${ventaId}_${p.tipoId}_${index}")
                        val movData = mapOf(
                            "id" to movRef.id,
                            "farmaciaId" to farmaciaId,
                            "sucursalId" to sucursalId,
                            "tipo" to MovimientoCaja.TIPO_VENTA,
                            "metodoTipo" to p.tipoId,
                            "metodoNombre" to p.nombreMetodo,
                            "monto" to montoNetoMov,
                            "motivo" to "Venta $numeroCompleto",
                            "referenciaId" to ventaId,
                            "referenciaNumero" to numeroCompleto,
                            "cajaSesionId" to sesionId,
                            "usuarioId" to SessionManager.idCajera,
                            "usuarioNombre" to SessionManager.nombreUsuario,
                            "fechaMs" to ahoraMs,
                            "fecha" to FieldValue.serverTimestamp()
                        )
                        tx.set(movRef, movData)
                    }
                }

                ventaResult = Venta(
                    id = ventaId,
                    numeroCompleto = numeroCompleto,
                    tipoComprobante = tipoComprobante,
                    serie = serie,
                    correlativo = nuevoCorrelativo,
                    cliente = cliente,
                    items = itemsFinalesDeVenta,
                    totalItems = itemsFinalesDeVenta.sumOf { it.cantidad },
                    subtotal = subtotalCalculado,
                    descuento = descuentoRedondeado,
                    total = totalCalculado,
                    pagos = pagos,
                    montoRecibido = montoRecibido,
                    vuelto = vuelto,
                    estado = Venta.ESTADO_COMPLETADA,
                    cajaSesionId = sesionId,
                    cajeroId = SessionManager.idCajera,
                    cajeroNombre = SessionManager.nombreUsuario,
                    fechaHoraMs = ahoraMs,
                    diaClave = diaClave,
                    estadoFiscal = "PENDIENTE",
                    moduloOrigen = "POS",
                    autorizadoPorId = autorizadoPorId.trim(),
                    autorizadoPorNombre = autorizadoPorNombre.trim(),
                    autorizadoPorRol = autorizadoPorRol.trim()
                )
            }.await()

            val resultadoFinal = ventaResult
                ?: return Result.failure(IllegalStateException("No se pudo completar la transacción de venta."))
            Result.success(resultadoFinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando venta: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Consulta si una venta ya fue grabada previamente por su ID de idempotencia (FASE 11 H1).
     */
    suspend fun consultarVentaPorIdempotencia(rawIdem: String): Venta? {
        val (farmaciaId, sucursalId) = ids() ?: return null
        if (rawIdem.isBlank()) return null
        val ventaId = if (rawIdem.startsWith("v_")) rawIdem else "v_$rawIdem"
        return try {
            val snap = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId).get().await()
            if (snap.exists()) {
                parseVenta(snap.id, snap.data)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando venta previa por idempotencia: ${e.message}", e)
            null
        }
    }

    // ───────────────────────────── FUNCIÓN 2: REGISTRAR DEVOLUCIÓN ─────────────────────────────

    /**
     * Registra una devolución total o parcial de venta:
     * Restituye el stock a los lotes originales proporcionales a lo consumido,
     * descuenta las ventasRegistradas, actualiza kardex, actualiza el puntero de caja,
     * emite el movimiento de caja de egreso y actualiza el estado de la venta.
     */
    suspend fun registrarDevolucion(
        ventaId: String,
        itemsADevolver: List<ItemDevolucionParam>,
        motivo: String,
        metodoReembolso: String = "EFECTIVO",
        idempotenciaId: String = ""
    ): Result<DevolucionVenta> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))

        if (ventaId.isBlank()) return Result.failure(IllegalArgumentException("ID de venta no válido."))
        if (itemsADevolver.isEmpty()) return Result.failure(IllegalArgumentException("Selecciona al menos un producto a devolver."))
        if (itemsADevolver.any { it.cantidad <= 0 }) return Result.failure(IllegalArgumentException("La cantidad a devolver debe ser mayor a 0."))
        if (motivo.trim().length < 5) return Result.failure(IllegalArgumentException("El motivo de devolución debe tener al menos 5 caracteres."))
        if (metodoReembolso.isBlank()) return Result.failure(IllegalArgumentException("Elige el método de reembolso."))

        val rawIdem = idempotenciaId.trim().ifBlank { UUID.randomUUID().toString() }
        val devId = if (rawIdem.startsWith("dev_")) rawIdem else "dev_$rawIdem"

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val devRef = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId).document(devId)
            val ventaRef = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId)
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
            val contadorRef = FarmadonPaths.contadores(db, farmaciaId, sucursalId).document("ventas")

            val distinctProductIds = itemsADevolver.map { it.productoId }.distinct()

            var devolucionResult: DevolucionVenta? = null

            db.runTransaction { tx ->
                // ── 1. TODAS LAS LECTURAS ANTES DE CUALQUIER ESCRITURA (Regla 1) ──
                val devSnap = tx.get(devRef)
                if (devSnap.exists()) {
                    val existente = parseDevolucion(devSnap.id, devSnap.data)
                    if (existente != null) {
                        devolucionResult = existente
                        return@runTransaction
                    }
                }

                val ventaSnap = tx.get(ventaRef)
                if (!ventaSnap.exists()) {
                    throw IllegalStateException("La venta con ID '$ventaId' no existe.")
                }

                val punteroSnap = tx.get(pointerRef)
                val estadoCajaStr = punteroSnap.getString("estado") ?: CajaSesion.ESTADO_CERRADA
                if (estadoCajaStr != CajaSesion.ESTADO_ABIERTA) {
                    throw IllegalStateException("La caja está cerrada. Ábrela para registrar devoluciones.")
                }
                val sesionId = punteroSnap.getString("sesionId").orEmpty()

                val contadorSnap = tx.get(contadorRef)
                val sucursalRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
                val sucursalSnap = tx.get(sucursalRef)

                val metodosDocRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId).collection("catalogos").document("metodosPago")
                val metodosSnap = tx.get(metodosDocRef)

                val productSnaps = distinctProductIds.associateWith { prodId ->
                    tx.get(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId))
                }

                // ── 2. VALIDAR ÍTEMS DEVOLVIBLES Y MÉTODO DE REEMBOLSO ──
                @Suppress("UNCHECKED_CAST")
                val instanciasMap = (metodosSnap.get("instancias") as? Map<String, Any?>) ?: emptyMap()
                val instanciasActivas = instanciasMap.values.filterIsInstance<Map<String, Any?>>().filter {
                    it["activa"] == true
                }
                if (instanciasActivas.isNotEmpty()) {
                    val metodoValido = instanciasActivas.any { inst ->
                        val instId = inst["id"] as? String ?: ""
                        val instTipo = inst["tipoId"] as? String ?: ""
                        instId == metodoReembolso || instTipo == metodoReembolso
                    }
                    if (!metodoValido) {
                        throw IllegalArgumentException("El método de reembolso '$metodoReembolso' no está activo para esta sede.")
                    }
                }

                val venta = parseVenta(ventaSnap.id, ventaSnap.data)
                    ?: throw IllegalStateException("Error al leer el documento de la venta '$ventaId'.")

                if (venta.estado == Venta.ESTADO_ANULADA) {
                    throw IllegalStateException("No se pueden registrar devoluciones sobre una venta anulada.")
                }

                val factorDescuento = if (venta.subtotal > 0.0) (venta.total / venta.subtotal) else 1.0

                val itemsDevolucionDataList = mutableListOf<ItemDevolucion>()
                var montoReembolsoTotal = 0.0

                val updatedVentaItems = venta.items.map { it.copy() }.toMutableList()
                val productosUpdates = mutableMapOf<String, Map<String, Any>>()
                val kardexPorProducto = mutableMapOf<String, Double>()

                for (itemDevParam in itemsADevolver) {
                    val index = updatedVentaItems.indexOfFirst {
                        it.productoId == itemDevParam.productoId && it.presentacionId == itemDevParam.presentacionId
                    }
                    if (index == -1) {
                        throw IllegalStateException("El producto a devolver no pertenece a la venta original.")
                    }
                    val itemVenta = updatedVentaItems[index]
                    if (itemDevParam.cantidad > itemVenta.cantidadDevolvible) {
                        throw IllegalStateException("No se pueden devolver ${itemDevParam.cantidad} unidades de '${itemVenta.nombreProducto}'. Solo quedan ${itemVenta.cantidadDevolvible} disponibles para devolución.")
                    }

                    // Monto por ítem con descuento prorrateado justo y redondeado
                    val montoItem = redondear2(itemDevParam.cantidad * itemVenta.precioUnitario * factorDescuento)
                    montoReembolsoTotal = redondear2(montoReembolsoTotal + montoItem)

                    // Proporción devuelta respecto al total vendido de este ítem (F1)
                    val proporcionItem = if (itemVenta.cantidad > 0) {
                        itemDevParam.cantidad.toDouble() / itemVenta.cantidad.toDouble()
                    } else 1.0
                    // Costo heredado directamente de los lotes consumidos en la venta original (F1)
                    val montoCostoItem = redondear2(
                        itemVenta.lotesConsumidos.sumOf { lc ->
                            (lc.cantidadFisica * proporcionItem) * lc.costoUnitarioReal
                        }
                    )
                    val costoUnitarioItem = if (itemDevParam.cantidad > 0) {
                        redondear2(montoCostoItem / itemDevParam.cantidad.toDouble())
                    } else 0.0

                    itemsDevolucionDataList.add(
                        ItemDevolucion(
                            productoId = itemVenta.productoId,
                            nombreProducto = itemVenta.nombreProducto,
                            presentacionNombre = itemVenta.presentacionNombre,
                            cantidad = itemDevParam.cantidad,
                            precioUnitario = redondear2(itemVenta.precioUnitario),
                            monto = montoItem,
                            costoUnitarioReal = costoUnitarioItem,
                            montoCosto = montoCostoItem
                        )
                    )

                    // Actualizar cantidad devuelta en la línea de venta
                    updatedVentaItems[index] = itemVenta.copy(
                        cantidadDevuelta = itemVenta.cantidadDevuelta + itemDevParam.cantidad
                    )
                }

                // Correlativo atómico de Nota de Crédito (Series por sucursal - FASE F0)
                // SUNAT UBL 2.1: Boleta -> serieNotaCreditoBoleta (BCxx) / Factura -> serieNotaCreditoFactura (FCxx)
                val esFactura = venta.tipoComprobante.equals("FACTURA", ignoreCase = true) || venta.serie.startsWith("F", ignoreCase = true)
                val serieNC = if (esFactura) {
                    sucursalSnap.getString("serieNotaCreditoFactura")?.trim().orEmpty()
                } else {
                    sucursalSnap.getString("serieNotaCreditoBoleta")?.trim().orEmpty()
                }
                if (serieNC.isBlank()) {
                    throw IllegalStateException("La sede no tiene serie de Nota de Crédito asignada. Configura Facturación Electrónica desde la Sede Principal.")
                }

                val campoContadorNC = if (esFactura) "ultimaNcFactura" else "ultimaNcBoleta"
                val ultimoCorrelativoNC = if (contadorSnap.exists()) {
                    (contadorSnap.get(campoContadorNC) as? Number)?.toLong()
                        ?: (contadorSnap.get("ultimaNotaCredito") as? Number)?.toLong()
                        ?: 0L
                } else 0L
                val nuevoCorrelativoNC = ultimoCorrelativoNC + 1L
                val numeroNC = String.format(Locale.US, "%s-%06d", serieNC, nuevoCorrelativoNC)

                // ── 3. RESTITUCIÓN DE STOCK A LOS LOTES ORIGINALES ──
                val itemsDevueltosPorProducto = itemsADevolver.groupBy { it.productoId }

                for ((prodId, paramsProducto) in itemsDevueltosPorProducto) {
                    val prodSnap = productSnaps[prodId]
                        ?: throw IllegalStateException("No se pudo leer el producto $prodId en inventario.")
                    if (!prodSnap.exists()) {
                        throw IllegalStateException("El producto con ID '$prodId' no existe en inventario.")
                    }

                    @Suppress("UNCHECKED_CAST")
                    val lotesDeTrabajo = (prodSnap.get("lotes") as? Map<*, *>)?.mapNotNull { (k, v) ->
                        if (v is Map<*, *>) k.toString() to (v as Map<String, Any>).toMutableMap() else null
                    }?.toMap(mutableMapOf()) ?: mutableMapOf<String, MutableMap<String, Any>>()

                    var totalFisicoDevueltoProd = 0.0

                    for (param in paramsProducto) {
                        val itemVentaOriginal = venta.items.first {
                            it.productoId == param.productoId && it.presentacionId == param.presentacionId
                        }
                        val proporcion = if (itemVentaOriginal.cantidad > 0) {
                            param.cantidad.toDouble() / itemVentaOriginal.cantidad.toDouble()
                        } else 1.0

                        for (lc in itemVentaOriginal.lotesConsumidos) {
                            val cantARestituir = lc.cantidadFisica * proporcion
                            if (cantARestituir <= 0.0) continue
                            totalFisicoDevueltoProd += cantARestituir

                            val entrada = buscarEntradaLote(lotesDeTrabajo, lc.loteId, lc.loteNumero)
                            if (entrada != null) {
                                val (loteKey, loteData) = entrada
                                val cantActual = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                                loteData["cantidad"] = cantActual + cantARestituir

                                val vReg = (loteData["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                                loteData["ventasRegistradas"] = (vReg - cantARestituir).coerceAtLeast(0.0)

                                lotesDeTrabajo[loteKey] = loteData
                            } else {
                                // Recreación mínima del lote original
                                val nuevaKey = lc.loteId.ifBlank { FechaVencimientoHelper.llaveLote(lc.loteNumero) }
                                lotesDeTrabajo[nuevaKey] = mutableMapOf(
                                    "numero" to lc.loteNumero,
                                    "loteId" to nuevaKey,
                                    "vencimiento" to lc.vencimiento,
                                    "cantidad" to cantARestituir,
                                    "ventasRegistradas" to 0.0,
                                    "fecha" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ahoraMs)),
                                    "fechaMs" to ahoraMs
                                )
                            }
                        }
                    }

                    kardexPorProducto[prodId] = totalFisicoDevueltoProd

                    val (nuevoStockDisp, nuevoStockTotal, vencMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesDeTrabajo)
                    productosUpdates[prodId] = mapOf(
                        "lotes" to lotesDeTrabajo,
                        "stock" to nuevoStockDisp,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to vencMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                }

                // ── 4. TODAS LAS ESCRITURAS (Regla 1) ──

                // 4.1 Actualizar stock de productos
                for ((prodId, updates) in productosUpdates) {
                    tx.update(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId), updates)
                }

                // 4.2 Escribir Kardex de entrada por devolución (con motivo y notas consistentes)
                for ((prodId, cantFisica) in kardexPorProducto) {
                    val kardexRef = FarmadonPaths.movimientos(db, farmaciaId, sucursalId).document("dev_${devId}_${prodId}")
                    val nomProd = itemsDevolucionDataList.firstOrNull { it.productoId == prodId }?.nombreProducto ?: ""
                    val kardexData = mapOf(
                        "id" to kardexRef.id,
                        "tipo" to "ENTRADA_DEVOLUCION",
                        "productoId" to prodId,
                        "productoNombre" to nomProd,
                        "cantidadTotal" to cantFisica,
                        "cantidad" to cantFisica,
                        "referenciaNumero" to venta.numeroCompleto,
                        "referenciaId" to devId,
                        "cajaSesionId" to sesionId,
                        "usuarioId" to SessionManager.idCajera,
                        "usuarioNombre" to SessionManager.nombreUsuario,
                        "usuarioEmail" to SessionManager.email,
                        "origen" to "DEVOLUCION_POS",
                        "motivo" to "Devolución $motivo (${venta.numeroCompleto})",
                        "notas" to motivo.trim(),
                        "fecha" to FieldValue.serverTimestamp(),
                        "fechaMs" to ahoraMs
                    )
                    tx.set(kardexRef, kardexData)
                }

                // 4.3 Incrementar correlativo de Nota de Crédito según tipo
                tx.set(contadorRef, mapOf(campoContadorNC to nuevoCorrelativoNC), SetOptions.merge())

                // 4.4 Escribir documento de Devolución
                val diaClave = formatoDiaClave().format(Date(ahoraMs))
                val costoTotalDevuelto = redondear2(itemsDevolucionDataList.sumOf { it.montoCosto })
                val devData = mapOf(
                    "id" to devId,
                    "ventaId" to venta.id,
                    "numeroVenta" to venta.numeroCompleto,
                    "tipoDocumento" to "NOTA_CREDITO",
                    "serie" to serieNC,
                    "correlativo" to nuevoCorrelativoNC,
                    "numeroCompleto" to numeroNC,
                    "estadoFiscal" to "PENDIENTE",
                    "items" to itemsDevolucionDataList.map {
                        mapOf(
                            "productoId" to it.productoId,
                            "nombreProducto" to it.nombreProducto,
                            "presentacionNombre" to it.presentacionNombre,
                            "cantidad" to it.cantidad,
                            "precioUnitario" to it.precioUnitario,
                            "monto" to it.monto,
                            "costoUnitarioReal" to redondear2(it.costoUnitarioReal),
                            "montoCosto" to redondear2(it.montoCosto)
                        )
                    },
                    "montoReembolso" to montoReembolsoTotal,
                    "costoTotalDevuelto" to costoTotalDevuelto,
                    "metodoReembolso" to metodoReembolso,
                    "motivo" to motivo.trim(),
                    "usuarioId" to SessionManager.idCajera,
                    "usuarioNombre" to SessionManager.nombreUsuario,
                    "cajaSesionId" to sesionId,
                    "fechaMs" to ahoraMs,
                    "diaClave" to diaClave,
                    "creadoEl" to FieldValue.serverTimestamp()
                )
                tx.set(devRef, devData)

                // 4.5 Registrar en la bandeja fiscal (FASE 12)
                val factNCId = "nc_$devId"
                val factNCRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(factNCId)
                val factNCData = mapOf(
                    "id" to factNCId,
                    "tipo" to "NOTA_CREDITO",
                    "serie" to serieNC,
                    "correlativo" to nuevoCorrelativoNC,
                    "numeroCompleto" to numeroNC,
                    "clienteTipoDoc" to venta.cliente.tipoDocumento,
                    "clienteNumeroDoc" to venta.cliente.numeroDocumento,
                    "clienteNombre" to venta.cliente.nombre,
                    "ventaId" to venta.id,
                    "devolucionId" to devId,
                    "sucursalId" to sucursalId,
                    "total" to montoReembolsoTotal,
                    "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                    "fechaMs" to ahoraMs,
                    "motivo" to motivo.trim(),
                    "moduloOrigen" to "POS"
                )
                tx.set(factNCRef, factNCData)

                // 4.6 Actualizar la Venta con las nuevas cantidades devueltas y estado
                val nuevoEstadoVenta = if (updatedVentaItems.all { it.cantidadDevolvible == 0 }) {
                    Venta.ESTADO_DEVOLUCION_TOTAL
                } else {
                    Venta.ESTADO_DEVOLUCION_PARCIAL
                }

                val updatedItemsData = updatedVentaItems.map { item ->
                    mapOf(
                        "productoId" to item.productoId,
                        "nombreProducto" to item.nombreProducto,
                        "empaque" to item.empaque,
                        "presentacionId" to item.presentacionId,
                        "presentacionNombre" to item.presentacionNombre,
                        "cantidad" to item.cantidad,
                        "precioUnitario" to redondear2(item.precioUnitario),
                        "subtotal" to redondear2(item.subtotal),
                        "requiereReceta" to item.requiereReceta,
                        "cantidadDevuelta" to item.cantidadDevuelta,
                        "costoTotalReal" to redondear2(item.costoTotalReal),
                        "lotesConsumidos" to item.lotesConsumidos.map { lc ->
                            mapOf(
                                "loteId" to lc.loteId,
                                "loteNumero" to lc.loteNumero,
                                "vencimiento" to lc.vencimiento,
                                "cantidadFisica" to lc.cantidadFisica,
                                "costoUnitarioReal" to redondear2(lc.costoUnitarioReal)
                            )
                        }
                    )
                }
                tx.update(
                    ventaRef,
                    mapOf(
                        "items" to updatedItemsData,
                        "estado" to nuevoEstadoVenta,
                        "devolucionMotivo" to motivo.trim(),
                        "devolucionPorId" to SessionManager.idCajera,
                        "devolucionPorNombre" to SessionManager.nombreUsuario,
                        "devolucionEnMs" to ahoraMs,
                        "devolucionNumeroNotaCredito" to numeroNC,
                        "devolucionMetodoReembolso" to metodoReembolso,
                        "devolucionTotalMonto" to montoReembolsoTotal,
                        "devolucionCostoTotal" to costoTotalDevuelto,
                        "devolucionId" to devId,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                )

                // 4.7 Actualizar puntero de caja (ventasPorMetodo y devolucionesEfectivo)
                @Suppress("UNCHECKED_CAST")
                val ventasPorMetodo = (punteroSnap.get("ventasPorMetodo") as? Map<String, Any>)
                    ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 }
                    ?.toMutableMap() ?: mutableMapOf()

                val actualMetodo = ventasPorMetodo[metodoReembolso] ?: 0.0
                ventasPorMetodo[metodoReembolso] = redondear2(actualMetodo - montoReembolsoTotal)

                val devEfectivoActual = (punteroSnap.get("devolucionesEfectivo") as? Number)?.toDouble() ?: 0.0
                val nuevaDevEfectivo = if (metodoReembolso == "EFECTIVO") {
                    redondear2(devEfectivoActual + montoReembolsoTotal)
                } else devEfectivoActual

                val cantDevsActual = (punteroSnap.get("cantidadDevoluciones") as? Number)?.toInt() ?: 0
                tx.update(
                    pointerRef,
                    mapOf(
                        "ventasPorMetodo" to ventasPorMetodo,
                        "devolucionesEfectivo" to nuevaDevEfectivo,
                        "cantidadDevoluciones" to cantDevsActual + 1
                    )
                )

                // 4.8 Registrar movimiento de caja por devolución (monto negativo)
                val nombreMetodoReembolso = when (metodoReembolso) {
                    "EFECTIVO" -> "Efectivo"
                    "YAPE" -> "Yape"
                    "PLIN" -> "Plin"
                    "TRANSFERENCIA" -> "Transferencia"
                    "TARJETA_POS" -> "Tarjeta"
                    "CHEQUE" -> "Cheque"
                    else -> metodoReembolso
                }

                val movRef = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId).document("devolucion_${devId}")
                val movData = mapOf(
                    "id" to movRef.id,
                    "farmaciaId" to farmaciaId,
                    "sucursalId" to sucursalId,
                    "tipo" to MovimientoCaja.TIPO_DEVOLUCION,
                    "metodoTipo" to metodoReembolso,
                    "metodoNombre" to nombreMetodoReembolso,
                    "monto" to redondear2(-montoReembolsoTotal),
                    "motivo" to "Devolución $motivo (${venta.numeroCompleto})",
                    "referenciaId" to devId,
                    "referenciaNumero" to venta.numeroCompleto,
                    "cajaSesionId" to sesionId,
                    "usuarioId" to SessionManager.idCajera,
                    "usuarioNombre" to SessionManager.nombreUsuario,
                    "fechaMs" to ahoraMs,
                    "fecha" to FieldValue.serverTimestamp()
                )
                tx.set(movRef, movData)

                devolucionResult = DevolucionVenta(
                    id = devId,
                    ventaId = venta.id,
                    numeroVenta = venta.numeroCompleto,
                    tipoDocumento = "NOTA_CREDITO",
                    serie = serieNC,
                    correlativo = nuevoCorrelativoNC,
                    numeroCompleto = numeroNC,
                    estadoFiscal = "PENDIENTE",
                    items = itemsDevolucionDataList,
                    montoReembolso = montoReembolsoTotal,
                    metodoReembolso = metodoReembolso,
                    motivo = motivo.trim(),
                    usuarioId = SessionManager.idCajera,
                    usuarioNombre = SessionManager.nombreUsuario,
                    cajaSesionId = sesionId,
                    fechaMs = ahoraMs,
                    diaClave = diaClave,
                    costoTotalDevuelto = costoTotalDevuelto
                )
            }.await()

            val resultadoFinal = devolucionResult
                ?: return Result.failure(IllegalStateException("No se pudo completar la transacción de devolución."))
            Result.success(resultadoFinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando devolución: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── FUNCIÓN 2.1: ANULAR VENTA COMPLETA ─────────────────────────────

    /**
     * Anula una venta completada en una transacción atómica (FASE 12):
     * Restituye todo el stock a los lotes originales, descuenta ventasRegistradas,
     * escribe kardex ANULACION_VENTA, descuenta el puntero de caja aplicando regla de vuelto-neto,
     * registra movimiento de egreso ANULACION en caja_movimientos,
     * marca la venta como ANULADA con auditoría completa (quién, motivo, fecha),
     * y genera el documento fiscal pendiente (NOTA_CREDITO si era FACTURA, COMUNICACION_BAJA si era BOLETA).
     */
    suspend fun anularVenta(
        ventaId: String,
        motivo: String,
        idempotenciaId: String = "",
        autorizadoPorId: String = "",
        autorizadoPorNombre: String = "",
        autorizadoPorRol: String = ""
    ): Result<Venta> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))

        val motivoLimpio = motivo.trim()
        if (ventaId.isBlank()) return Result.failure(IllegalArgumentException("ID de venta no válido."))
        if (motivoLimpio.length < 5) return Result.failure(IllegalArgumentException("Debe ingresar un motivo de anulación de al menos 5 caracteres."))

        // Regla de vigencia por sede
        val posConfig = posConfigRepo.obtener(sucursalId)
        if (!posConfig.estaVigente) {
            return Result.failure(
                IllegalStateException("APERTURA PENDIENTE: La sede no tiene reglas de POS guardadas en Firestore. Debe configurarse y guardarse en Configuración > Ventas/POS antes de operar.")
            )
        }

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val ventaRef = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId)
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
            val contadorRef = FarmadonPaths.contadores(db, farmaciaId, sucursalId).document("ventas")

            var ventaAnuladaResult: Venta? = null

            db.runTransaction { tx ->
                // ── 1. TODAS LAS LECTURAS ANTES DE CUALQUIER ESCRITURA (Regla 1) ──
                val ventaSnap = tx.get(ventaRef)
                if (!ventaSnap.exists()) {
                    throw IllegalStateException("La venta con ID '$ventaId' no existe.")
                }

                val venta = parseVenta(ventaSnap.id, ventaSnap.data)
                    ?: throw IllegalStateException("Error al leer el documento de la venta '$ventaId'.")

                if (venta.estado == Venta.ESTADO_ANULADA) {
                    // Idempotencia: ya estaba anulada
                    ventaAnuladaResult = venta
                    return@runTransaction
                }

                if (venta.estado != Venta.ESTADO_COMPLETADA) {
                    throw IllegalStateException("Esa venta ya tiene devoluciones registradas (${venta.estado}). No se puede anular.")
                }

                val punteroSnap = tx.get(pointerRef)
                val estadoCajaStr = punteroSnap.getString("estado") ?: CajaSesion.ESTADO_CERRADA
                if (estadoCajaStr != CajaSesion.ESTADO_ABIERTA) {
                    throw IllegalStateException("La caja está cerrada. Ábrela para registrar anulaciones de venta.")
                }
                val sesionId = punteroSnap.getString("sesionId").orEmpty()

                val contadorSnap = tx.get(contadorRef)
                val sucursalRef = FarmadonPaths.sucursal(db, farmaciaId, sucursalId)
                val sucursalSnap = tx.get(sucursalRef)

                val distinctProductIds = venta.items.map { it.productoId }.distinct()
                val productSnaps = distinctProductIds.associateWith { prodId ->
                    tx.get(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId))
                }

                // ── 2. PREPARAR NOTA DE CRÉDITO (SI FACTURA) O COMUNICACIÓN DE BAJA (SI BOLETA) ──
                val esFactura = venta.tipoComprobante == "FACTURA" || venta.serie.startsWith("F", ignoreCase = true)
                val serieNCFactura = if (esFactura) {
                    val s = sucursalSnap.getString("serieNotaCreditoFactura")?.trim().orEmpty()
                    if (s.isBlank()) {
                        throw IllegalStateException("La sede no tiene serie de Nota de Crédito asignada. Configura Facturación Electrónica desde la Sede Principal.")
                    }
                    s
                } else ""

                val ultimoCorrelativoNC = if (contadorSnap.exists()) {
                    (contadorSnap.get("ultimaNcFactura") as? Number)?.toLong()
                        ?: (contadorSnap.get("ultimaNotaCredito") as? Number)?.toLong()
                        ?: 0L
                } else 0L
                val nuevoCorrelativoNC = if (esFactura) ultimoCorrelativoNC + 1L else ultimoCorrelativoNC
                val numeroNC = if (esFactura) String.format(Locale.US, "%s-%06d", serieNCFactura, nuevoCorrelativoNC) else ""

                // ── 3. RESTITUCIÓN DE STOCK A LOS LOTES ORIGINALES ──
                val productosUpdates = mutableMapOf<String, Map<String, Any>>()
                val kardexPorProducto = mutableMapOf<String, Double>()

                for (item in venta.items) {
                    val prodId = item.productoId
                    val prodSnap = productSnaps[prodId]
                        ?: throw IllegalStateException("No se pudo leer el producto $prodId en inventario.")
                    if (!prodSnap.exists()) {
                        throw IllegalStateException("El producto '${item.nombreProducto}' no existe en inventario.")
                    }

                    @Suppress("UNCHECKED_CAST")
                    val lotesDeTrabajo = (prodSnap.get("lotes") as? Map<*, *>)?.mapNotNull { (k, v) ->
                        if (v is Map<*, *>) k.toString() to (v as Map<String, Any>).toMutableMap() else null
                    }?.toMap(mutableMapOf()) ?: mutableMapOf<String, MutableMap<String, Any>>()

                    var totalFisicoItem = 0.0

                    for (lc in item.lotesConsumidos) {
                        val cantARestituir = lc.cantidadFisica
                        if (cantARestituir <= 0.0) continue
                        totalFisicoItem += cantARestituir

                        val entrada = buscarEntradaLote(lotesDeTrabajo, lc.loteId, lc.loteNumero)
                        if (entrada != null) {
                            val (loteKey, loteData) = entrada
                            val cantActual = (loteData["cantidad"] as? Number)?.toDouble() ?: 0.0
                            loteData["cantidad"] = cantActual + cantARestituir

                            val vReg = (loteData["ventasRegistradas"] as? Number)?.toDouble() ?: 0.0
                            loteData["ventasRegistradas"] = (vReg - cantARestituir).coerceAtLeast(0.0)

                            lotesDeTrabajo[loteKey] = loteData
                        } else {
                            val nuevaKey = lc.loteId.ifBlank { FechaVencimientoHelper.llaveLote(lc.loteNumero) }
                            lotesDeTrabajo[nuevaKey] = mutableMapOf(
                                "numero" to lc.loteNumero,
                                "loteId" to nuevaKey,
                                "vencimiento" to lc.vencimiento,
                                "cantidad" to cantARestituir,
                                "ventasRegistradas" to 0.0,
                                "fecha" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ahoraMs)),
                                "fechaMs" to ahoraMs
                            )
                        }
                    }

                    kardexPorProducto[prodId] = (kardexPorProducto[prodId] ?: 0.0) + totalFisicoItem

                    val (nuevoStockDisp, nuevoStockTotal, vencMasCercano) = FechaVencimientoHelper.resumenStockYFefo(lotesDeTrabajo)
                    productosUpdates[prodId] = mapOf(
                        "lotes" to lotesDeTrabajo,
                        "stock" to nuevoStockDisp,
                        "stockTotal" to nuevoStockTotal,
                        "vencimientoMasCercano" to vencMasCercano,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    )
                }

                // ── 4. TODAS LAS ESCRITURAS (Regla 1) ──

                // 4.1 Actualizar productos
                for ((prodId, updates) in productosUpdates) {
                    tx.update(FarmadonPaths.inventario(db, farmaciaId, sucursalId).document(prodId), updates)
                }

                // 4.2 Kardex de entrada por anulación de venta
                for ((prodId, cantFisica) in kardexPorProducto) {
                    val kardexRef = FarmadonPaths.movimientos(db, farmaciaId, sucursalId).document("anulacion_${ventaId}_${prodId}")
                    val nomProd = venta.items.firstOrNull { it.productoId == prodId }?.nombreProducto ?: ""
                    val kardexData = mapOf(
                        "id" to kardexRef.id,
                        "tipo" to "ANULACION_VENTA",
                        "productoId" to prodId,
                        "productoNombre" to nomProd,
                        "cantidadTotal" to cantFisica,
                        "cantidad" to cantFisica,
                        "referenciaNumero" to venta.numeroCompleto,
                        "referenciaId" to ventaId,
                        "cajaSesionId" to sesionId,
                        "usuarioId" to SessionManager.idCajera,
                        "usuarioNombre" to SessionManager.nombreUsuario,
                        "usuarioEmail" to SessionManager.email,
                        "origen" to "ANULACION_POS",
                        "motivo" to "Anulación $motivoLimpio (${venta.numeroCompleto})",
                        "notas" to motivoLimpio,
                        "fecha" to FieldValue.serverTimestamp(),
                        "fechaMs" to ahoraMs
                    )
                    tx.set(kardexRef, kardexData)
                }

                // 4.3 Si era Factura, incrementar correlativo de Nota de Crédito
                if (esFactura) {
                    tx.set(contadorRef, mapOf("ultimaNcFactura" to nuevoCorrelativoNC), SetOptions.merge())
                }

                // 4.4 Actualizar el documento de Venta a ANULADA (operativo y fiscal anulados; la baja/NC vive en su propio doc fiscal)
                tx.update(
                    ventaRef,
                    mapOf(
                        "estado" to Venta.ESTADO_ANULADA,
                        "estadoFiscal" to FacturacionDocumento.ESTADO_ANULADO,
                        "anuladaPorId" to SessionManager.idCajera,
                        "anuladaPorNombre" to SessionManager.nombreUsuario,
                        "anulacionMotivo" to motivoLimpio,
                        "anuladaEnMs" to ahoraMs,
                        "numeroNotaCredito" to numeroNC,
                        "autorizadoPorId" to autorizadoPorId.trim(),
                        "autorizadoPorNombre" to autorizadoPorNombre.trim(),
                        "autorizadoPorRol" to autorizadoPorRol.trim(),
                        "anuladoEl" to FieldValue.serverTimestamp()
                    )
                )

                // 4.5 Actualizar puntero de caja (ventasPorMetodo y cantidadVentas)
                @Suppress("UNCHECKED_CAST")
                val ventasPorMetodo = (punteroSnap.get("ventasPorMetodo") as? Map<String, Any>)
                    ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 }
                    ?.toMutableMap() ?: mutableMapOf()

                var vueltoRestante = venta.vuelto
                for (p in venta.pagos) {
                    val montoNetoMetodo = if (p.tipoId == "EFECTIVO") {
                        val asignado = minOf(vueltoRestante, p.monto)
                        vueltoRestante = redondear2(vueltoRestante - asignado)
                        redondear2(p.monto - asignado)
                    } else redondear2(p.monto)
                    val actual = ventasPorMetodo[p.tipoId] ?: 0.0
                    ventasPorMetodo[p.tipoId] = redondear2(actual - montoNetoMetodo)
                }

                val cantVentasActual = (punteroSnap.get("cantidadVentas") as? Number)?.toInt() ?: 0
                tx.update(
                    pointerRef,
                    mapOf(
                        "ventasPorMetodo" to ventasPorMetodo,
                        "cantidadVentas" to (cantVentasActual - 1).coerceAtLeast(0)
                    )
                )

                // 4.6 Registrar movimientos de egreso por anulación individuales por método en caja_movimientos
                var vueltoRestanteMovs = venta.vuelto
                venta.pagos.forEachIndexed { index, p ->
                    val montoNetoMov = if (p.tipoId == "EFECTIVO") {
                        val asignado = minOf(vueltoRestanteMovs, p.monto)
                        vueltoRestanteMovs = redondear2(vueltoRestanteMovs - asignado)
                        redondear2(p.monto - asignado)
                    } else redondear2(p.monto)
                    if (montoNetoMov > 0.0) {
                        val movRef = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId).document("anulacion_${venta.id}_${p.tipoId}_${index}")
                        val movData = mapOf(
                            "id" to movRef.id,
                            "farmaciaId" to farmaciaId,
                            "sucursalId" to sucursalId,
                            "tipo" to MovimientoCaja.TIPO_ANULACION,
                            "metodoTipo" to p.tipoId,
                            "metodoNombre" to p.nombreMetodo,
                            "monto" to redondear2(-montoNetoMov),
                            "motivo" to "Anulación de ${venta.numeroCompleto}: $motivoLimpio",
                            "referenciaId" to venta.id,
                            "referenciaNumero" to venta.numeroCompleto,
                            "cajaSesionId" to sesionId,
                            "usuarioId" to SessionManager.idCajera,
                            "usuarioNombre" to SessionManager.nombreUsuario,
                            "fechaMs" to ahoraMs,
                            "fecha" to FieldValue.serverTimestamp()
                        )
                        tx.set(movRef, movData)
                    }
                }

                // 4.7 Registrar documento pendiente en facturacion_documentos
                val factAnulId = "anul_${venta.id}"
                val factAnulRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(factAnulId)
                val tipoDocFiscal = if (esFactura) "NOTA_CREDITO" else "COMUNICACION_BAJA"
                val numDocFiscal = if (esFactura) numeroNC else "AN-${venta.numeroCompleto}"
                val factDocData = mapOf(
                    "id" to factAnulId,
                    "tipo" to tipoDocFiscal,
                    "serie" to if (esFactura) serieNCFactura else venta.serie,
                    "correlativo" to if (esFactura) nuevoCorrelativoNC else venta.correlativo,
                    "numeroCompleto" to numDocFiscal,
                    "clienteTipoDoc" to venta.cliente.tipoDocumento,
                    "clienteNumeroDoc" to venta.cliente.numeroDocumento,
                    "clienteNombre" to venta.cliente.nombre,
                    "ventaId" to venta.id,
                    "devolucionId" to "",
                    "sucursalId" to sucursalId,
                    "total" to venta.total,
                    "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                    "fechaMs" to ahoraMs,
                    "motivo" to motivoLimpio,
                    "moduloOrigen" to "POS"
                )
                tx.set(factAnulRef, factDocData)

                ventaAnuladaResult = venta.copy(
                    estado = Venta.ESTADO_ANULADA,
                    estadoFiscal = FacturacionDocumento.ESTADO_ANULADO,
                    anuladaPorId = SessionManager.idCajera,
                    anuladaPorNombre = SessionManager.nombreUsuario,
                    anulacionMotivo = motivoLimpio,
                    anuladaEnMs = ahoraMs,
                    numeroNotaCredito = numeroNC
                )
            }.await()

            val res = ventaAnuladaResult
                ?: return Result.failure(IllegalStateException("No se pudo completar la transacción de anulación."))
            Result.success(res)
        } catch (e: Exception) {
            Log.e(TAG, "Error anulando venta: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── FUNCIONES 3-6: CONSULTAS Y SUSPENDIDAS ─────────────────────────────

    /**
     * Observa en vivo las ventas del día actual (R8/R9).
     * Consulta simple de 1 solo campo (diaClave) y orden en memoria (Regla 9).
     */
    fun observarVentasDelDia(): Flow<List<Venta>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(HoraServidor.ahoraMs()))
        val ref = FarmadonPaths.ventas(db, farmaciaId, sucursalId)
            .whereEqualTo("diaClave", hoy)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando ventas del día: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { parseVenta(it.id, it.data) }
                ?.sortedByDescending { it.fechaHoraMs }
                ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Observa en vivo las devoluciones (notas de crédito) del día actual (R8/R9).
     * Consulta simple de 1 solo campo (diaClave) y orden en memoria.
     * Permite que Ventas del Día cuadre el neto del día con la caja:
     * neto del día = ventas brutas de hoy − reembolsos de hoy (aunque sean de ventas de ayer).
     */
    fun observarDevolucionesDelDia(): Flow<List<DevolucionVenta>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(HoraServidor.ahoraMs()))
        val ref = FarmadonPaths.devoluciones(db, farmaciaId, sucursalId)
            .whereEqualTo("diaClave", hoy)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando devoluciones del día: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { parseDevolucion(it.id, it.data) }
                ?.sortedByDescending { it.fechaMs }
                ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Observa el historial de compras de un cliente en la sucursal (R8/R1).
     * Consulta de 1 solo campo top-level `clienteId` (cero índices compuestos).
     */
    fun observarHistorialCliente(clienteId: String): Flow<List<Venta>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        if (clienteId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val ref = FarmadonPaths.ventas(db, farmaciaId, sucursalId)
            .whereEqualTo("clienteId", clienteId)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando historial del cliente: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { parseVenta(it.id, it.data) }
                ?.sortedByDescending { it.fechaHoraMs }
                ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Busca ventas por número completo (ej: "B001-000042") o por correlativo si son solo dígitos.
     */
    suspend fun buscarVentaPorNumero(texto: String): Result<List<Venta>> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        val queryLimpia = texto.trim().uppercase()
        if (queryLimpia.isBlank()) return Result.success(emptyList())

        return try {
            val col = FarmadonPaths.ventas(db, farmaciaId, sucursalId)
            val resultados = mutableMapOf<String, Venta>()

            // 1. Búsqueda exacta por numeroCompleto
            val porNumeroCompleto = col.whereEqualTo("numeroCompleto", queryLimpia).get().await()
            for (doc in porNumeroCompleto.documents) {
                parseVenta(doc.id, doc.data)?.let { resultados[it.id] = it }
            }

            // 2. Si son solo dígitos, buscar también por correlativo
            val correlativoLong = queryLimpia.toLongOrNull()
            if (correlativoLong != null) {
                val porCorrelativo = col.whereEqualTo("correlativo", correlativoLong).get().await()
                for (doc in porCorrelativo.documents) {
                    parseVenta(doc.id, doc.data)?.let { resultados[it.id] = it }
                }
            }

            val lista = resultados.values.sortedByDescending { it.fechaHoraMs }
            Result.success(lista)
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando venta: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa en vivo las ventas suspendidas (parqueadas) de la sucursal.
     */
    fun observarSuspendidas(): Flow<List<VentaSuspendida>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        val ref = FarmadonPaths.ventasSuspendidas(db, farmaciaId, sucursalId)
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando ventas suspendidas: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { parseVentaSuspendida(it.id, it.data) }
                ?.sortedByDescending { it.fechaMs }
                ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Guarda una venta en pausa en la nube para retomarla luego desde cualquier caja.
     * Preserva los ítems, el cliente y el descuento pactado (R3/R8).
     */
    suspend fun suspenderVenta(
        items: List<ItemVenta>,
        cliente: ClienteDeVenta = ClienteDeVenta(),
        descuento: Double = 0.0,
        nota: String = ""
    ): Result<VentaSuspendida> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("No hay productos para suspender."))

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val docRef = FarmadonPaths.ventasSuspendidas(db, farmaciaId, sucursalId).document()
            val subtotal = redondear2(items.sumOf { it.precioUnitario * it.cantidad })
            val descuentoRedondeado = redondear2(descuento.coerceAtLeast(0.0))
            val total = redondear2((subtotal - descuentoRedondeado).coerceAtLeast(0.0))
            val suspData = mapOf(
                "id" to docRef.id,
                "items" to items.map { item ->
                    mapOf(
                        "productoId" to item.productoId,
                        "nombreProducto" to item.nombreProducto,
                        "empaque" to item.empaque,
                        "presentacionId" to item.presentacionId,
                        "presentacionNombre" to item.presentacionNombre,
                        "cantidad" to item.cantidad,
                        "precioUnitario" to redondear2(item.precioUnitario),
                        "subtotal" to redondear2(item.subtotal),
                        "requiereReceta" to item.requiereReceta,
                        "cantidadDevuelta" to 0,
                        "lotesConsumidos" to emptyList<Map<String, Any>>()
                    )
                },
                "cliente" to mapOf(
                    "tipoDocumento" to cliente.tipoDocumento,
                    "numeroDocumento" to cliente.numeroDocumento,
                    "nombre" to cliente.nombre,
                    "clienteId" to cliente.clienteId
                ),
                "subtotal" to subtotal,
                "descuento" to descuentoRedondeado,
                "total" to total,
                "nota" to nota.trim(),
                "creadoPorId" to SessionManager.idCajera,
                "creadoPorNombre" to SessionManager.nombreUsuario,
                "fechaMs" to ahoraMs,
                "creadoEl" to FieldValue.serverTimestamp()
            )
            docRef.set(suspData).await()
            Result.success(
                VentaSuspendida(
                    id = docRef.id,
                    items = items,
                    cliente = cliente,
                    subtotal = subtotal,
                    descuento = descuentoRedondeado,
                    total = total,
                    nota = nota.trim(),
                    creadoPorId = SessionManager.idCajera,
                    creadoPorNombre = SessionManager.nombreUsuario,
                    fechaMs = ahoraMs
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error suspendiendo venta: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reanuda atómicamente una venta suspendida mediante transacción:
     * verifica que aún exista en Firestore, la elimina para que otra caja no la retome,
     * y retorna la venta recuperada (ALTO 3).
     */
    suspend fun reanudarVentaSuspendida(id: String): Result<VentaSuspendida> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (id.isBlank()) return Result.failure(IllegalArgumentException("ID de venta suspendida no válido."))

        return try {
            val suspRef = FarmadonPaths.ventasSuspendidas(db, farmaciaId, sucursalId).document(id)
            val susp = db.runTransaction { tx ->
                val snap = tx.get(suspRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Esta venta suspendida ya fue recuperada o descartada desde otra terminal.")
                }
                val parsed = parseVentaSuspendida(snap.id, snap.data)
                    ?: throw IllegalStateException("Los datos de la venta suspendida están corruptos.")
                tx.delete(suspRef)
                parsed
            }.await()

            Result.success(susp)
        } catch (e: Exception) {
            Log.e(TAG, "Error reanudando venta suspendida: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina una venta suspendida tras haber sido descartada.
     */
    suspend fun eliminarSuspendida(id: String): Result<Unit> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (id.isBlank()) return Result.failure(IllegalArgumentException("ID de venta suspendida no válido."))
        return try {
            FarmadonPaths.ventasSuspendidas(db, farmaciaId, sucursalId).document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando venta suspendida: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los datos del emisor para la impresión del ticket.
     * Lee directamente el documento de la farmacia con fallback a SessionManager.
     */
    suspend fun obtenerEmisor(): Result<EmisorComprobante> {
        val (farmaciaId, _) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        return try {
            val farmaciaDoc = FarmadonPaths.farmacia(db, farmaciaId).get().await()
            val nombre = farmaciaDoc.getString("nombreFarmacia")
                ?: farmaciaDoc.getString("nombreComercial")
                ?: SessionManager.nombreUsuario.ifBlank { "Farmacia" }
            val ruc = farmaciaDoc.getString("ruc") ?: SessionManager.dni
            val direccion = farmaciaDoc.getString("direccion") ?: ""
            val sucursal = SessionManager.sucursalNombre.ifBlank { "Sede Principal" }
            Result.success(
                EmisorComprobante(
                    nombreFarmacia = nombre,
                    ruc = ruc,
                    direccion = direccion,
                    sucursalNombre = sucursal
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo datos del emisor: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── HELPERS Y PARSERS ─────────────────────────────

    /**
     * Busca la entrada de un lote en el mapa mutable de lotes (Regla 7):
     * 1. Llave directa en el mapa.
     * 2. Match por campo interno loteId.
     * 3. FechaVencimientoHelper.resolverLote(numero).
     */
    private fun buscarEntradaLote(
        lotesMap: MutableMap<String, MutableMap<String, Any>>,
        loteId: String,
        loteNumero: String
    ): Pair<String, MutableMap<String, Any>>? {
        // 1. Llave directa
        val directa = lotesMap[loteId]
        if (directa != null) return loteId to directa

        // 2. Match por campo interno loteId
        val porCampo = lotesMap.entries.firstOrNull { (_, v) ->
            (v["loteId"] as? String)?.equals(loteId, ignoreCase = true) == true
        }
        if (porCampo != null) return porCampo.key to porCampo.value

        // 3. FechaVencimientoHelper.resolverLote(numero)
        val res = FechaVencimientoHelper.resolverLote(lotesMap, loteNumero)
        if (res != null) {
            val entry = lotesMap[res.first]
            if (entry != null) return res.first to entry
            return res.first to res.second
        }
        return null
    }

    fun parseVenta(id: String, data: Map<String, Any>?): Venta? {
        if (data == null) return null
        return try {
            val cMap = data["cliente"] as? Map<*, *>
            val clienteIdTop = data["clienteId"] as? String ?: ""
            val cIdSub = cMap?.get("clienteId") as? String ?: ""
            val cNum = cMap?.get("numeroDocumento") as? String ?: ""
            val cliente = ClienteDeVenta(
                tipoDocumento = cMap?.get("tipoDocumento") as? String ?: "NINGUNO",
                numeroDocumento = cNum,
                nombre = cMap?.get("nombre") as? String ?: "Consumidor Final",
                clienteId = cIdSub.ifBlank { clienteIdTop }.ifBlank { cNum }
            )

            val itemsRaw = data["items"] as? List<*>
            val items = itemsRaw?.mapNotNull { itemMap ->
                if (itemMap !is Map<*, *>) return@mapNotNull null
                val lotesConsRaw = itemMap["lotesConsumidos"] as? List<*>
                val lotesCons = lotesConsRaw?.mapNotNull { lcMap ->
                    if (lcMap !is Map<*, *>) return@mapNotNull null
                    LoteConsumido(
                        loteId = lcMap["loteId"] as? String ?: "",
                        loteNumero = lcMap["loteNumero"] as? String ?: "",
                        vencimiento = lcMap["vencimiento"] as? String ?: "",
                        cantidadFisica = (lcMap["cantidadFisica"] as? Number)?.toDouble() ?: 0.0,
                        costoUnitarioReal = (lcMap["costoUnitarioReal"] as? Number)?.toDouble() ?: 0.0
                    )
                } ?: emptyList()

                ItemVenta(
                    productoId = itemMap["productoId"] as? String ?: "",
                    nombreProducto = itemMap["nombreProducto"] as? String ?: "",
                    empaque = itemMap["empaque"] as? String ?: "",
                    presentacionId = itemMap["presentacionId"] as? String ?: "",
                    presentacionNombre = itemMap["presentacionNombre"] as? String ?: "",
                    cantidad = (itemMap["cantidad"] as? Number)?.toInt() ?: 0,
                    precioUnitario = (itemMap["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                    subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    requiereReceta = itemMap["requiereReceta"] as? Boolean ?: false,
                    lotesConsumidos = lotesCons,
                    cantidadDevuelta = (itemMap["cantidadDevuelta"] as? Number)?.toInt() ?: 0,
                    costoTotalReal = (itemMap["costoTotalReal"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList()

            val pagosRaw = data["pagos"] as? List<*>
            val pagos = pagosRaw?.mapNotNull { pMap ->
                if (pMap !is Map<*, *>) return@mapNotNull null
                PagoVenta(
                    tipoId = pMap["tipoId"] as? String ?: "",
                    instanciaId = pMap["instanciaId"] as? String ?: "",
                    nombreMetodo = pMap["nombreMetodo"] as? String ?: "",
                    monto = (pMap["monto"] as? Number)?.toDouble() ?: 0.0,
                    numeroOperacion = pMap["numeroOperacion"] as? String ?: ""
                )
            } ?: emptyList()

            Venta(
                id = id,
                numeroCompleto = data["numeroCompleto"] as? String ?: "",
                tipoComprobante = data["tipoComprobante"] as? String ?: "BOLETA",
                serie = data["serie"] as? String ?: "B001",
                correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L,
                cliente = cliente,
                items = items,
                totalItems = (data["totalItems"] as? Number)?.toInt() ?: items.sumOf { it.cantidad },
                subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                descuento = (data["descuento"] as? Number)?.toDouble() ?: 0.0,
                total = (data["total"] as? Number)?.toDouble() ?: 0.0,
                pagos = pagos,
                montoRecibido = (data["montoRecibido"] as? Number)?.toDouble() ?: 0.0,
                vuelto = (data["vuelto"] as? Number)?.toDouble() ?: 0.0,
                estado = data["estado"] as? String ?: Venta.ESTADO_COMPLETADA,
                cajaSesionId = data["cajaSesionId"] as? String ?: "",
                cajeroId = data["cajeroId"] as? String ?: "",
                cajeroNombre = data["cajeroNombre"] as? String ?: "",
                fechaHoraMs = (data["fechaHoraMs"] as? Number)?.toLong() ?: 0L,
                diaClave = data["diaClave"] as? String ?: "",
                estadoFiscal = data["estadoFiscal"] as? String ?: "PENDIENTE",
                moduloOrigen = data["moduloOrigen"] as? String ?: "POS",
                anuladaPorId = data["anuladaPorId"] as? String ?: "",
                anuladaPorNombre = data["anuladaPorNombre"] as? String ?: "",
                anulacionMotivo = data["anulacionMotivo"] as? String ?: "",
                anuladaEnMs = (data["anuladaEnMs"] as? Number)?.toLong() ?: 0L,
                numeroNotaCredito = data["numeroNotaCredito"] as? String ?: "",
                costoTotalReal = (data["costoTotalReal"] as? Number)?.toDouble() ?: 0.0,
                devolucionMotivo = data["devolucionMotivo"] as? String ?: "",
                devolucionPorId = data["devolucionPorId"] as? String ?: "",
                devolucionPorNombre = data["devolucionPorNombre"] as? String ?: "",
                devolucionEnMs = (data["devolucionEnMs"] as? Number)?.toLong() ?: 0L,
                devolucionNumeroNotaCredito = data["devolucionNumeroNotaCredito"] as? String ?: "",
                devolucionMetodoReembolso = data["devolucionMetodoReembolso"] as? String ?: "",
                autorizadoPorId = data["autorizadoPorId"] as? String ?: "",
                autorizadoPorNombre = data["autorizadoPorNombre"] as? String ?: "",
                autorizadoPorRol = data["autorizadoPorRol"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando Venta $id: ${e.message}", e)
            null
        }
    }

    fun parseDevolucion(id: String, data: Map<String, Any>?): DevolucionVenta? {
        if (data == null) return null
        return try {
            val itemsRaw = data["items"] as? List<*>
            val items = itemsRaw?.mapNotNull { itemMap ->
                if (itemMap !is Map<*, *>) return@mapNotNull null
                ItemDevolucion(
                    productoId = itemMap["productoId"] as? String ?: "",
                    nombreProducto = itemMap["nombreProducto"] as? String ?: "",
                    presentacionNombre = itemMap["presentacionNombre"] as? String ?: "",
                    cantidad = (itemMap["cantidad"] as? Number)?.toInt() ?: 0,
                    precioUnitario = (itemMap["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                    monto = (itemMap["monto"] as? Number)?.toDouble() ?: 0.0,
                    costoUnitarioReal = (itemMap["costoUnitarioReal"] as? Number)?.toDouble() ?: 0.0,
                    montoCosto = (itemMap["montoCosto"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList()

            DevolucionVenta(
                id = id,
                ventaId = data["ventaId"] as? String ?: "",
                numeroVenta = data["numeroVenta"] as? String ?: "",
                tipoDocumento = data["tipoDocumento"] as? String ?: "NOTA_CREDITO",
                serie = data["serie"] as? String ?: "NC01",
                correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L,
                numeroCompleto = data["numeroCompleto"] as? String ?: "",
                estadoFiscal = data["estadoFiscal"] as? String ?: "PENDIENTE",
                items = items,
                montoReembolso = (data["montoReembolso"] as? Number)?.toDouble() ?: 0.0,
                metodoReembolso = data["metodoReembolso"] as? String ?: "EFECTIVO",
                motivo = data["motivo"] as? String ?: "",
                usuarioId = data["usuarioId"] as? String ?: "",
                usuarioNombre = data["usuarioNombre"] as? String ?: "",
                cajaSesionId = data["cajaSesionId"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
                diaClave = data["diaClave"] as? String ?: "",
                costoTotalDevuelto = (data["costoTotalDevuelto"] as? Number)?.toDouble() ?: 0.0,
                autorizadoPorId = data["autorizadoPorId"] as? String ?: "",
                autorizadoPorNombre = data["autorizadoPorNombre"] as? String ?: "",
                autorizadoPorRol = data["autorizadoPorRol"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando DevolucionVenta $id: ${e.message}", e)
            null
        }
    }

    fun parseVentaSuspendida(id: String, data: Map<String, Any>?): VentaSuspendida? {
        if (data == null) return null
        return try {
            val cMap = data["cliente"] as? Map<*, *>
            val cliente = ClienteDeVenta(
                tipoDocumento = cMap?.get("tipoDocumento") as? String ?: "NINGUNO",
                numeroDocumento = cMap?.get("numeroDocumento") as? String ?: "",
                nombre = cMap?.get("nombre") as? String ?: "Consumidor Final",
                clienteId = cMap?.get("clienteId") as? String ?: ""
            )

            val itemsRaw = data["items"] as? List<*>
            val items = itemsRaw?.mapNotNull { itemMap ->
                if (itemMap !is Map<*, *>) return@mapNotNull null
                ItemVenta(
                    productoId = itemMap["productoId"] as? String ?: "",
                    nombreProducto = itemMap["nombreProducto"] as? String ?: "",
                    empaque = itemMap["empaque"] as? String ?: "",
                    presentacionId = itemMap["presentacionId"] as? String ?: "",
                    presentacionNombre = itemMap["presentacionNombre"] as? String ?: "",
                    cantidad = (itemMap["cantidad"] as? Number)?.toInt() ?: 0,
                    precioUnitario = (itemMap["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                    subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    requiereReceta = itemMap["requiereReceta"] as? Boolean ?: false,
                    lotesConsumidos = emptyList(),
                    cantidadDevuelta = 0
                )
            } ?: emptyList()

            val subtotal = (data["subtotal"] as? Number)?.toDouble() ?: items.sumOf { it.precioUnitario * it.cantidad }
            val descuento = (data["descuento"] as? Number)?.toDouble() ?: 0.0
            val total = (data["total"] as? Number)?.toDouble() ?: (subtotal - descuento)

            VentaSuspendida(
                id = id,
                items = items,
                cliente = cliente,
                subtotal = subtotal,
                descuento = descuento,
                total = total,
                nota = data["nota"] as? String ?: "",
                creadoPorId = data["creadoPorId"] as? String ?: "",
                creadoPorNombre = data["creadoPorNombre"] as? String ?: "",
                fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando VentaSuspendida $id: ${e.message}", e)
            null
        }
    }
}
