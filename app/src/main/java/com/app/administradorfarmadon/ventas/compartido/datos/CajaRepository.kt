package com.app.administradorfarmadon.ventas.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.configuracion.pos.datos.PosConfigRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * CAJA REAL DEL POS (R3/R8/R10).
 *
 * Regla madre: UNA sucursal = UNA caja abierta como máximo.
 * Eso se garantiza en el servidor con transacciones sobre el puntero
 * `caja_sesiones/actual`: si dos tablets abren a la vez, solo una gana.
 *
 * Ventas, devoluciones e ingresos/retiros actualizan el puntero DENTRO de su
 * propia transacción (ver VentasRepository): el esperado de caja jamás se desfasa.
 */
class CajaRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    private val posConfigRepo = PosConfigRepository(db)

    companion object {
        private const val TAG = "CajaRepository"
        private val fmtLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    private fun ids(): Pair<String, String>? {
        val f = SessionManager.clienteIdGarantizado
        val s = SessionManager.sucursalIdEfectiva
        if (f.isBlank() || s.isBlank()) return null
        return f to s
    }

    // ───────────────────────────── LECTURA EN VIVO ─────────────────────────────

    /** Estado vivo de la caja de la sucursal (R8). Si no existe puntero todavía → caja cerrada. */
    fun observarEstadoCaja(): Flow<EstadoCaja> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        val ref = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando estado de caja: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val parsed = parseEstadoCaja(snap?.data)
            trySend(parsed)
        }
        awaitClose { reg.remove() }
    }

    /** Movimientos del turno indicado (ventas, devoluciones, ingresos, retiros), recientes primero. */
    fun observarMovimientosSesion(sesionId: String): Flow<List<MovimientoCaja>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            close(IllegalStateException("No hay sesión de farmacia activa."))
            return@callbackFlow
        }
        if (sesionId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId)
            .whereEqualTo("cajaSesionId", sesionId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "Error escuchando movimientos de caja: ${err.message}", err)
                    close(err)
                    return@addSnapshotListener
                }
                val lista = snap?.documents?.mapNotNull { parseMovimiento(it.id, it.data) }
                    ?.sortedByDescending { it.fechaMs }
                    ?: emptyList()
                trySend(lista)
            }
        awaitClose { reg.remove() }
    }

    // ───────────────────────────── APERTURA ─────────────────────────────

    /**
     * Abre la caja con un fondo inicial real. Atómica: si otra tablet ya abrió,
     * esta falla con mensaje claro (nunca abre dos cajas).
     */
    suspend fun abrirCaja(fondoInicial: Double): Result<CajaSesion> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (fondoInicial < 0.0) return Result.failure(IllegalArgumentException("El fondo inicial no puede ser negativo."))

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val sesionRef = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId).document()
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)

            db.runTransaction { tx ->
                val puntero = tx.get(pointerRef)
                val estadoActual = puntero.getString("estado") ?: CajaSesion.ESTADO_CERRADA
                if (estadoActual == CajaSesion.ESTADO_ABIERTA) {
                    val quien = puntero.getString("abiertoPorNombre") ?: "otra caja"
                    throw IllegalStateException("La caja ya está abierta (la abrió $quien). Debe cerrarla para volver a abrir.")
                }
                val sesionData = mapOf(
                    "id" to sesionRef.id,
                    "farmaciaId" to farmaciaId,
                    "sucursalId" to sucursalId,
                    "estado" to CajaSesion.ESTADO_ABIERTA,
                    "fondoInicial" to fondoInicial,
                    "aperturaMs" to ahoraMs,
                    "aperturaLegible" to fmtLegible.format(Date(ahoraMs)),
                    "abiertoPorId" to SessionManager.idCajera,
                    "abiertoPorNombre" to SessionManager.nombreUsuario
                )
                tx.set(sesionRef, sesionData)
                tx.set(
                    pointerRef,
                    mapOf(
                        "estado" to CajaSesion.ESTADO_ABIERTA,
                        "sesionId" to sesionRef.id,
                        "fondoInicial" to fondoInicial,
                        "aperturaMs" to ahoraMs,
                        "abiertoPorNombre" to SessionManager.nombreUsuario,
                        "ventasPorMetodo" to emptyMap<String, Double>(),
                        "ingresos" to 0.0,
                        "retiros" to 0.0,
                        "devolucionesEfectivo" to 0.0,
                        "cantidadVentas" to 0,
                        "cantidadDevoluciones" to 0
                    )
                )
            }.await()
            Result.success(
                CajaSesion(
                    id = sesionRef.id,
                    estado = CajaSesion.ESTADO_ABIERTA,
                    fondoInicial = fondoInicial,
                    aperturaMs = ahoraMs,
                    aperturaLegible = fmtLegible.format(Date(ahoraMs)),
                    abiertoPorId = SessionManager.idCajera,
                    abiertoPorNombre = SessionManager.nombreUsuario
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo caja: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── INGRESOS / RETIROS ─────────────────────────────

    /** Ingreso o retiro manual de efectivo (pago a proveedor menor, vuelto para banca, etc.). */
    suspend fun registrarMovimientoManual(
        tipo: String,
        monto: Double,
        motivo: String,
        autorizadoPorId: String = "",
        autorizadoPorNombre: String = "",
        autorizadoPorRol: String = ""
    ): Result<MovimientoCaja> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (tipo != MovimientoCaja.TIPO_INGRESO && tipo != MovimientoCaja.TIPO_RETIRO) {
            return Result.failure(IllegalArgumentException("Tipo de movimiento no válido."))
        }
        if (monto <= 0.0) return Result.failure(IllegalArgumentException("El monto debe ser mayor a 0."))
        if (motivo.trim().length < 4) return Result.failure(IllegalArgumentException("Describe el motivo del movimiento."))

        val posConfig = posConfigRepo.obtener(sucursalId)

        if (tipo == MovimientoCaja.TIPO_RETIRO) {
            if (monto > posConfig.caja.retiroMax) {
                val montoStr = String.format(Locale.US, "%.2f", monto)
                val maxStr = String.format(Locale.US, "%.2f", posConfig.caja.retiroMax)
                return Result.failure(
                    IllegalArgumentException(
                        "El retiro (S/ $montoStr) supera el retiro máximo permitido de la sede (máx S/ $maxStr)."
                    )
                )
            }
        }

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
            val movRef = FarmadonPaths.cajaMovimientos(db, farmaciaId, sucursalId).document()
            db.runTransaction { tx ->
                val puntero = tx.get(pointerRef)
                if (puntero.getString("estado") != CajaSesion.ESTADO_ABIERTA) {
                    throw IllegalStateException("La caja está cerrada. Ábrela para registrar movimientos.")
                }
                val sesionId = puntero.getString("sesionId").orEmpty()
                val campo = if (tipo == MovimientoCaja.TIPO_INGRESO) "ingresos" else "retiros"
                val actual = (puntero.get(campo) as? Number)?.toDouble() ?: 0.0

                if (tipo == MovimientoCaja.TIPO_RETIRO) {
                    val fondoInicial = (puntero.get("fondoInicial") as? Number)?.toDouble() ?: 0.0
                    @Suppress("UNCHECKED_CAST")
                    val ventasPorMetodo = (puntero.get("ventasPorMetodo") as? Map<String, Any>)
                        ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
                    val ventasEfectivo = ventasPorMetodo["EFECTIVO"] ?: 0.0
                    val ingresos = (puntero.get("ingresos") as? Number)?.toDouble() ?: 0.0
                    val saldoFinalEfectivo = fondoInicial + ventasEfectivo + ingresos - (actual + monto)
                    if (saldoFinalEfectivo < 0.0) {
                        throw IllegalStateException(
                            "Este retiro dejaría la caja con saldo negativo en efectivo (S/ ${String.format(Locale.US, "%.2f", saldoFinalEfectivo)}). Operación denegada."
                        )
                    }
                }

                tx.update(pointerRef, campo, actual + monto)
                tx.set(
                    movRef,
                    mapOf(
                        "id" to movRef.id,
                        "farmaciaId" to farmaciaId,
                        "sucursalId" to sucursalId,
                        "tipo" to tipo,
                        "metodoTipo" to "EFECTIVO",
                        "metodoNombre" to "Efectivo",
                        "monto" to if (tipo == MovimientoCaja.TIPO_RETIRO) -monto else monto,
                        "motivo" to motivo.trim(),
                        "referenciaId" to "",
                        "referenciaNumero" to "",
                        "cajaSesionId" to sesionId,
                        "usuarioId" to SessionManager.idCajera,
                        "usuarioNombre" to SessionManager.nombreUsuario,
                        "autorizadoPorId" to autorizadoPorId.trim(),
                        "autorizadoPorNombre" to autorizadoPorNombre.trim(),
                        "autorizadoPorRol" to autorizadoPorRol.trim(),
                        "fechaMs" to ahoraMs
                    )
                )
            }.await()
            Result.success(
                MovimientoCaja(
                    id = movRef.id, tipo = tipo, metodoTipo = "EFECTIVO", metodoNombre = "Efectivo",
                    monto = if (tipo == MovimientoCaja.TIPO_RETIRO) -monto else monto,
                    motivo = motivo.trim(), cajaSesionId = "", usuarioId = SessionManager.idCajera,
                    usuarioNombre = SessionManager.nombreUsuario,
                    autorizadoPorId = autorizadoPorId.trim(),
                    autorizadoPorNombre = autorizadoPorNombre.trim(),
                    autorizadoPorRol = autorizadoPorRol.trim(),
                    fechaMs = ahoraMs
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando movimiento de caja: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── CIERRE ─────────────────────────────

    /**
     * Cierra el turno: deja foto final (contado vs esperado y diferencia) en la sesión
     * y libera el puntero. Si otra tablet ya cerró, esta recibe el mensaje real.
     */
    suspend fun cerrarCaja(efectivoContado: Double, observaciones: String): Result<CajaSesion> {
        val (farmaciaId, sucursalId) = ids()
            ?: return Result.failure(IllegalStateException("No hay sesión de farmacia activa."))
        if (efectivoContado < 0.0) return Result.failure(IllegalArgumentException("El conteo no puede ser negativo."))

        return try {
            val ahoraMs = HoraServidor.ahoraMs()
            val pointerRef = FarmadonPaths.estadoCaja(db, farmaciaId, sucursalId)
            var sesionResultado: CajaSesion? = null

            db.runTransaction { tx ->
                val puntero = tx.get(pointerRef)
                if (puntero.getString("estado") != CajaSesion.ESTADO_ABIERTA) {
                    throw IllegalStateException("La caja ya está cerrada. No hay turno que cerrar.")
                }
                val sesionId = puntero.getString("sesionId").orEmpty()
                if (sesionId.isBlank() || sesionId == "actual") {
                    throw IllegalStateException("La caja abierta no tiene un turno válido asociado.")
                }
                val sesionRef = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId).document(sesionId)
                val sesionSnap = tx.get(sesionRef)

                @Suppress("UNCHECKED_CAST")
                val ventasPorMetodo = (puntero.get("ventasPorMetodo") as? Map<String, Any>)
                    ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
                val ingresos = (puntero.get("ingresos") as? Number)?.toDouble() ?: 0.0
                val retiros = (puntero.get("retiros") as? Number)?.toDouble() ?: 0.0
                val devolucionesEfectivo = (puntero.get("devolucionesEfectivo") as? Number)?.toDouble() ?: 0.0
                val fondoInicial = (puntero.get("fondoInicial") as? Number)?.toDouble() ?: 0.0
                val ventasEfectivo = ventasPorMetodo["EFECTIVO"] ?: 0.0
                val esperado = fondoInicial + ventasEfectivo + ingresos - retiros
                val diferencia = efectivoContado - esperado

                val aperturaMs = (sesionSnap.get("aperturaMs") as? Number)?.toLong()
                    ?: (puntero.get("aperturaMs") as? Number)?.toLong()
                    ?: ahoraMs

                val tzLima = TimeZone.getTimeZone("America/Lima")
                val calApertura = Calendar.getInstance(tzLima).apply { timeInMillis = aperturaMs }
                val calAhora = Calendar.getInstance(tzLima).apply { timeInMillis = ahoraMs }

                // Si se está cerrando en una fecha calendario posterior a la de apertura:
                val esCierreDeOtroDia = (calAhora.get(Calendar.YEAR) > calApertura.get(Calendar.YEAR)) ||
                    (calAhora.get(Calendar.YEAR) == calApertura.get(Calendar.YEAR) &&
                     calAhora.get(Calendar.DAY_OF_YEAR) > calApertura.get(Calendar.DAY_OF_YEAR))

                // Regla contable de farmacia: Si se cierra al día siguiente, la fecha contable
                // del turno pertenece al día en que se abrió, asignándole la hora final de esa jornada (23:59:59).
                val (cierreMsContable, cierreLegibleContable) = if (esCierreDeOtroDia && aperturaMs > 0L) {
                    val calFinalJornada = Calendar.getInstance(tzLima).apply {
                        timeInMillis = aperturaMs
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val finMs = calFinalJornada.timeInMillis
                    finMs to fmtLegible.format(Date(finMs))
                } else {
                    ahoraMs to fmtLegible.format(Date(ahoraMs))
                }

                val observacionesFinales = if (esCierreDeOtroDia) {
                    val fechaAbreStr = fmtLegible.format(Date(aperturaMs))
                    val fechaRealStr = fmtLegible.format(Date(ahoraMs))
                    val notaExtemporanea = "[Cierre de jornada anterior: abierto $fechaAbreStr, liquidado físicamente el $fechaRealStr]"
                    if (observaciones.isNotBlank()) "$notaExtemporanea ${observaciones.trim()}" else notaExtemporanea
                } else {
                    observaciones.trim()
                }

                val cierreData = mapOf(
                    "estado" to CajaSesion.ESTADO_CERRADA,
                    "cierreMs" to cierreMsContable,
                    "cierreLegible" to cierreLegibleContable,
                    "cierreExtemporaneo" to esCierreDeOtroDia,
                    "cierreFisicoRealMs" to ahoraMs,
                    "cierreFisicoRealLegible" to fmtLegible.format(Date(ahoraMs)),
                    "cerradoPorId" to SessionManager.idCajera,
                    "cerradoPorNombre" to SessionManager.nombreUsuario,
                    "efectivoContado" to efectivoContado,
                    "efectivoEsperado" to esperado,
                    "diferenciaEfectivo" to diferencia,
                    "observaciones" to observacionesFinales,
                    // Foto final del turno completo (para reportes sin recalcular)
                    "ventasPorMetodo" to ventasPorMetodo,
                    "totalVentas" to ventasPorMetodo.values.sum(),
                    "ingresos" to ingresos,
                    "retiros" to retiros,
                    "devolucionesEfectivo" to devolucionesEfectivo,
                    "cantidadVentas" to ((puntero.get("cantidadVentas") as? Number)?.toInt() ?: 0),
                    "cantidadDevoluciones" to ((puntero.get("cantidadDevoluciones") as? Number)?.toInt() ?: 0)
                )
                tx.update(sesionRef, cierreData)
                tx.set(
                    pointerRef,
                    mapOf(
                        "estado" to CajaSesion.ESTADO_CERRADA,
                        "sesionId" to "",
                        "fondoInicial" to 0.0,
                        "aperturaMs" to 0L,
                        "abiertoPorNombre" to "",
                        "ventasPorMetodo" to emptyMap<String, Double>(),
                        "ingresos" to 0.0,
                        "retiros" to 0.0,
                        "devolucionesEfectivo" to 0.0,
                        "cantidadVentas" to 0,
                        "cantidadDevoluciones" to 0
                    )
                )
                sesionResultado = CajaSesion(
                    id = sesionId,
                    estado = CajaSesion.ESTADO_CERRADA,
                    fondoInicial = (sesionSnap.get("fondoInicial") as? Number)?.toDouble() ?: fondoInicial,
                    aperturaMs = aperturaMs,
                    aperturaLegible = sesionSnap.getString("aperturaLegible") ?: (puntero.getString("aperturaLegible") ?: ""),
                    abiertoPorId = sesionSnap.getString("abiertoPorId") ?: "",
                    abiertoPorNombre = sesionSnap.getString("abiertoPorNombre") ?: (puntero.getString("abiertoPorNombre") ?: ""),
                    cierreMs = cierreMsContable,
                    cierreLegible = cierreLegibleContable,
                    cierreExtemporaneo = esCierreDeOtroDia,
                    cierreFisicoRealMs = ahoraMs,
                    cierreFisicoRealLegible = fmtLegible.format(Date(ahoraMs)),
                    cerradoPorId = SessionManager.idCajera,
                    cerradoPorNombre = SessionManager.nombreUsuario,
                    efectivoContado = efectivoContado,
                    efectivoEsperado = esperado,
                    diferenciaEfectivo = diferencia,
                    observaciones = observacionesFinales,
                    ventasPorMetodo = ventasPorMetodo,
                    totalVentas = ventasPorMetodo.values.sum(),
                    ingresos = ingresos,
                    retiros = retiros,
                    devolucionesEfectivo = devolucionesEfectivo,
                    cantidadVentas = ((puntero.get("cantidadVentas") as? Number)?.toInt() ?: 0),
                    cantidadDevoluciones = ((puntero.get("cantidadDevoluciones") as? Number)?.toInt() ?: 0)
                )
            }.await()
            val resultadoFinal = sesionResultado
                ?: return Result.failure(IllegalStateException("No se pudo completar el cierre de caja."))
            Result.success(resultadoFinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando caja: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────────────────────────── PARSERS ─────────────────────────────

    fun parseEstadoCaja(data: Map<String, Any>?): EstadoCaja {
        if (data == null) return EstadoCaja()
        @Suppress("UNCHECKED_CAST")
        val ventasPorMetodo = (data["ventasPorMetodo"] as? Map<String, Any>)
            ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
        return EstadoCaja(
            estado = data["estado"] as? String ?: CajaSesion.ESTADO_CERRADA,
            sesionId = data["sesionId"] as? String ?: "",
            fondoInicial = (data["fondoInicial"] as? Number)?.toDouble() ?: 0.0,
            aperturaMs = (data["aperturaMs"] as? Number)?.toLong() ?: 0L,
            abiertoPorNombre = data["abiertoPorNombre"] as? String ?: "",
            ventasPorMetodo = ventasPorMetodo,
            ingresos = (data["ingresos"] as? Number)?.toDouble() ?: 0.0,
            retiros = (data["retiros"] as? Number)?.toDouble() ?: 0.0,
            devolucionesEfectivo = (data["devolucionesEfectivo"] as? Number)?.toDouble() ?: 0.0,
            cantidadVentas = (data["cantidadVentas"] as? Number)?.toInt() ?: 0,
            cantidadDevoluciones = (data["cantidadDevoluciones"] as? Number)?.toInt() ?: 0
        )
    }

    fun parseMovimiento(id: String, data: Map<String, Any>?): MovimientoCaja? {
        if (data == null) return null
        return MovimientoCaja(
            id = id,
            tipo = data["tipo"] as? String ?: "",
            metodoTipo = data["metodoTipo"] as? String ?: "",
            metodoNombre = data["metodoNombre"] as? String ?: "",
            monto = (data["monto"] as? Number)?.toDouble() ?: 0.0,
            motivo = data["motivo"] as? String ?: "",
            referenciaId = data["referenciaId"] as? String ?: "",
            referenciaNumero = data["referenciaNumero"] as? String ?: "",
            cajaSesionId = data["cajaSesionId"] as? String ?: "",
            usuarioId = data["usuarioId"] as? String ?: "",
            usuarioNombre = data["usuarioNombre"] as? String ?: "",
            fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
            autorizadoPorId = data["autorizadoPorId"] as? String ?: "",
            autorizadoPorNombre = data["autorizadoPorNombre"] as? String ?: "",
            autorizadoPorRol = data["autorizadoPorRol"] as? String ?: ""
        )
    }

    // ───────────────────────────── AUDITORÍA HISTÓRICA DE CAJA ─────────────────────────────

    fun parseSesion(id: String, data: Map<String, Any>?): CajaSesion? {
        if (data == null || id == "actual") return null
        @Suppress("UNCHECKED_CAST")
        val ventasPorMetodo = (data["ventasPorMetodo"] as? Map<String, Any>)
            ?.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
        return CajaSesion(
            id = id,
            estado = data["estado"] as? String ?: CajaSesion.ESTADO_CERRADA,
            fondoInicial = (data["fondoInicial"] as? Number)?.toDouble() ?: 0.0,
            aperturaMs = (data["aperturaMs"] as? Number)?.toLong() ?: 0L,
            aperturaLegible = data["aperturaLegible"] as? String ?: "",
            abiertoPorId = data["abiertoPorId"] as? String ?: "",
            abiertoPorNombre = data["abiertoPorNombre"] as? String ?: "",
            cierreMs = (data["cierreMs"] as? Number)?.toLong() ?: 0L,
            cierreLegible = data["cierreLegible"] as? String ?: "",
            cierreExtemporaneo = data["cierreExtemporaneo"] as? Boolean ?: false,
            cierreFisicoRealMs = (data["cierreFisicoRealMs"] as? Number)?.toLong() ?: 0L,
            cierreFisicoRealLegible = data["cierreFisicoRealLegible"] as? String ?: "",
            cerradoPorId = data["cerradoPorId"] as? String ?: "",
            cerradoPorNombre = data["cerradoPorNombre"] as? String ?: "",
            efectivoContado = (data["efectivoContado"] as? Number)?.toDouble() ?: 0.0,
            efectivoEsperado = (data["efectivoEsperado"] as? Number)?.toDouble() ?: 0.0,
            diferenciaEfectivo = (data["diferenciaEfectivo"] as? Number)?.toDouble() ?: 0.0,
            observaciones = data["observaciones"] as? String ?: "",
            ventasPorMetodo = ventasPorMetodo,
            totalVentas = (data["totalVentas"] as? Number)?.toDouble() ?: ventasPorMetodo.values.sum(),
            ingresos = (data["ingresos"] as? Number)?.toDouble() ?: 0.0,
            retiros = (data["retiros"] as? Number)?.toDouble() ?: 0.0,
            devolucionesEfectivo = (data["devolucionesEfectivo"] as? Number)?.toDouble() ?: 0.0,
            cantidadVentas = (data["cantidadVentas"] as? Number)?.toInt() ?: 0,
            cantidadDevoluciones = (data["cantidadDevoluciones"] as? Number)?.toInt() ?: 0
        )
    }

    /**
     * Escucha en vivo el historial de turnos de caja (aperturas, cierres, arqueos).
     * Muestra las sesiones cerradas y la sesión activa en orden cronológico inverso.
     */
    fun observarHistorialSesiones(limite: Int = 50): Flow<List<CajaSesion>> = callbackFlow {
        val (farmaciaId, sucursalId) = ids() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FarmadonPaths.cajaSesiones(db, farmaciaId, sucursalId)
            .orderBy("aperturaMs", Query.Direction.DESCENDING)
            .limit((limite + 1).toLong())
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Error escuchando historial de sesiones de caja: ${err.message}", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { doc ->
                if (doc.id == "actual") null else parseSesion(doc.id, doc.data)
            }?.take(limite) ?: emptyList()
            trySend(lista)
        }
        awaitClose { reg.remove() }
    }

    /**
     * Regla R3/R12: Las cajas de jornadas anteriores NO se cierran automáticamente con diferencia 0 ficticia.
     * El usuario debe realizar el arqueo formal con conteo físico en la pantalla de Cierre de Caja.
     */
    suspend fun verificarYCerrarTurnoDiaAnterior(): Boolean {
        // Operación deshabilitada por regla de producto: la caja debe ser cerrada formalmente por el personal con conteo físico.
        return false
    }
}
