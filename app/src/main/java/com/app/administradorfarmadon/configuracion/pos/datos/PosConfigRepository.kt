package com.app.administradorfarmadon.configuracion.pos.datos

import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.configuracion.metodospago.datos.SucursalCatalogo
import com.app.administradorfarmadon.configuracion.pos.modelo.*
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesPaths
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de configuración del Punto de Venta (POS) por sede (R1/R3/R8).
 *
 * Cada sucursal tiene su propio documento:
 *   farmacias/{f}/sucursales/{s}/catalogos/posConfig
 *
 * Mantiene la regla R1 (aislamiento por farmacia y sede).
 * Si el documento no existe en Firestore, devuelve defaults seguros sin lanzar error.
 */
class PosConfigRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {

    private fun refPosConfig(sucursalId: String): DocumentReference {
        val f = SessionManager.clienteIdGarantizado
        val s = sucursalId.ifBlank { "principal" }
        return FarmadonPaths.sucursal(db, f, s).collection("catalogos").document("posConfig")
    }

    /** Lista de sucursales de la farmacia para el selector del administrador. */
    fun observarSucursales(): Flow<List<SucursalCatalogo>> = callbackFlow {
        val f = SessionManager.clienteIdGarantizado
        if (f.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val reg = SucursalesPaths.sucursales(db, f).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { doc ->
                SucursalCatalogo(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "Sede sin nombre"
                )
            } ?: emptyList()
            trySend(lista)
        }
        awaitClose { reg.remove() }
    }

    /**
     * Escucha en vivo la configuración POS de la sucursal indicada.
     * Si no existe o no tiene datos, emite una instancia con los defaults seguros.
     */
    /**
     * Escucha en vivo la configuración POS de la sucursal indicada.
     * Si no existe en Firestore, emite una instancia marcada como borrador no guardado (R3/R12).
     */
    fun observar(sucursalId: String): Flow<PosConfig> = callbackFlow {
        val s = sucursalId.ifBlank { "principal" }
        val f = SessionManager.clienteIdGarantizado
        val reg = refPosConfig(s).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(PosConfig(farmaciaId = f, sucursalId = s, existeEnServidor = false, esBorrador = true))
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(PosConfig(farmaciaId = f, sucursalId = s, existeEnServidor = false, esBorrador = true))
                return@addSnapshotListener
            }
            trySend(mapearDocumentoAPosConfig(snap.data, f, s, existe = true))
        }
        awaitClose { reg.remove() }
    }

    /**
     * Obtiene una lectura puntual de la configuración POS de la sucursal.
     */
    suspend fun obtener(sucursalId: String): PosConfig {
        val s = sucursalId.ifBlank { "principal" }
        val f = SessionManager.clienteIdGarantizado
        return try {
            val snap = refPosConfig(s).get().await()
            if (snap != null && snap.exists()) {
                mapearDocumentoAPosConfig(snap.data, f, s, existe = true)
            } else {
                PosConfig(farmaciaId = f, sucursalId = s, existeEnServidor = false, esBorrador = true)
            }
        } catch (e: Exception) {
            PosConfig(farmaciaId = f, sucursalId = s, existeEnServidor = false, esBorrador = true)
        }
    }

    /**
     * Guarda la configuración del POS para una sucursal específica.
     * Solo permitido para roles Administrador o Dueño.
     */
    suspend fun guardar(
        sucursalId: String,
        config: PosConfig,
        usuarioId: String,
        usuarioNombre: String,
        rolUsuario: String
    ): Result<Unit> {
        val rolLimpio = rolUsuario.trim().lowercase()
        val esAdmin = rolLimpio == "administrador" || rolLimpio == "dueño" || rolLimpio == "dueno"
        if (!esAdmin) {
            return Result.failure(SecurityException("Solo los administradores pueden modificar las reglas del POS."))
        }

        val s = sucursalId.ifBlank { "principal" }
        val f = SessionManager.clienteIdGarantizado
        if (f.isBlank()) {
            return Result.failure(IllegalStateException("No hay una farmacia identificada en la sesión activa."))
        }

        return try {
            val docRef = refPosConfig(s)
            val ahoraMs = System.currentTimeMillis()
            val mapa = mapOf(
                "farmaciaId" to f,
                "sucursalId" to s,
                "esBorrador" to false,
                "descuento" to mapOf(
                    "maxPct" to config.descuento.maxPct,
                    "maxMonto" to config.descuento.maxMonto
                ),
                "caja" to mapOf(
                    "retiroMax" to config.caja.retiroMax,
                    "vueltoMax" to config.caja.vueltoMax,
                    "entregaCiegaTurno" to config.caja.entregaCiegaTurno
                ),
                "ticket" to mapOf(
                    "copias" to config.ticket.copias,
                    "pie" to config.ticket.pie.trim()
                ),
                "receta" to mapOf(
                    "exigirConfirmacion" to config.receta.exigirConfirmacion
                ),
                "guardadoPorId" to usuarioId,
                "guardadoPorNombre" to usuarioNombre.trim(),
                "guardadoPorRol" to rolUsuario.trim(),
                "guardadoEnMs" to ahoraMs,
                "actualizadoPorId" to usuarioId,
                "actualizadoPorNombre" to usuarioNombre.trim(),
                "actualizadoPorRol" to rolUsuario.trim(),
                "actualizadoEnMs" to ahoraMs
            )
            docRef.set(mapa, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aplica la configuración a todas las sedes de la farmacia (acción exclusiva de Sede Principal).
     */
    suspend fun aplicarATodasLasSedes(
        config: PosConfig,
        usuarioId: String,
        usuarioNombre: String,
        rolUsuario: String
    ): Result<Int> {
        val rolLimpio = rolUsuario.trim().lowercase()
        val esAdmin = rolLimpio == "administrador" || rolLimpio == "dueño" || rolLimpio == "dueno"
        if (!esAdmin) {
            return Result.failure(SecurityException("Solo los administradores pueden aplicar reglas masivas."))
        }
        val f = SessionManager.clienteIdGarantizado
        if (f.isBlank()) {
            return Result.failure(IllegalStateException("No hay farmacia activa en sesión."))
        }

        return try {
            val sucursalesSnap = SucursalesPaths.sucursales(db, f).get().await()
            val docs = sucursalesSnap.documents
            var contador = 0
            val ahoraMs = System.currentTimeMillis()

            for (doc in docs) {
                val sId = doc.id
                val docRef = refPosConfig(sId)
                val mapa = mapOf(
                    "farmaciaId" to f,
                    "sucursalId" to sId,
                    "esBorrador" to false,
                    "descuento" to mapOf(
                        "maxPct" to config.descuento.maxPct,
                        "maxMonto" to config.descuento.maxMonto
                    ),
                    "caja" to mapOf(
                        "retiroMax" to config.caja.retiroMax,
                        "vueltoMax" to config.caja.vueltoMax,
                        "entregaCiegaTurno" to config.caja.entregaCiegaTurno
                    ),
                    "ticket" to mapOf(
                        "copias" to config.ticket.copias,
                        "pie" to config.ticket.pie.trim()
                    ),
                    "receta" to mapOf(
                        "exigirConfirmacion" to config.receta.exigirConfirmacion
                    ),
                    "guardadoPorId" to usuarioId,
                    "guardadoPorNombre" to usuarioNombre.trim(),
                    "guardadoPorRol" to rolUsuario.trim(),
                    "guardadoEnMs" to ahoraMs,
                    "actualizadoPorId" to usuarioId,
                    "actualizadoPorNombre" to usuarioNombre.trim(),
                    "actualizadoPorRol" to rolUsuario.trim(),
                    "actualizadoEnMs" to ahoraMs
                )
                docRef.set(mapa, SetOptions.merge()).await()
                contador++
            }
            Result.success(contador)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapearDocumentoAPosConfig(
        raw: Map<String, Any?>?,
        farmaciaId: String,
        sucursalId: String,
        existe: Boolean = false
    ): PosConfig {
        if (raw == null) return PosConfig(farmaciaId = farmaciaId, sucursalId = sucursalId, existeEnServidor = false, esBorrador = true)

        val rawDescuento = raw["descuento"] as? Map<String, Any?>
        val descuento = PosDescuentoConfig(
            maxPct = (rawDescuento?.get("maxPct") as? Number)?.toDouble() ?: 10.0,
            maxMonto = (rawDescuento?.get("maxMonto") as? Number)?.toDouble() ?: 50.0
        )

        val rawCaja = raw["caja"] as? Map<String, Any?>
        val caja = PosCajaConfig(
            retiroMax = (rawCaja?.get("retiroMax") as? Number)?.toDouble() ?: 500.0,
            vueltoMax = (rawCaja?.get("vueltoMax") as? Number)?.toDouble() ?: 200.0,
            entregaCiegaTurno = rawCaja?.get("entregaCiegaTurno") as? Boolean ?: true
        )

        val rawTicket = raw["ticket"] as? Map<String, Any?>
        val ticket = PosTicketConfig(
            copias = (rawTicket?.get("copias") as? Number)?.toInt() ?: 1,
            pie = rawTicket?.get("pie")?.toString() ?: "Gracias por su compra"
        )

        val rawReceta = raw["receta"] as? Map<String, Any?>
        val receta = PosRecetaConfig(
            exigirConfirmacion = rawReceta?.get("exigirConfirmacion") as? Boolean ?: true
        )

        val esBorradorDoc = (raw["esBorrador"] as? Boolean) ?: false

        return PosConfig(
            farmaciaId = farmaciaId,
            sucursalId = sucursalId,
            descuento = descuento,
            caja = caja,
            ticket = ticket,
            receta = receta,
            actualizadoPorId = raw["actualizadoPorId"]?.toString() ?: "",
            actualizadoPorNombre = raw["actualizadoPorNombre"]?.toString() ?: "",
            actualizadoPorRol = raw["actualizadoPorRol"]?.toString() ?: "",
            actualizadoEnMs = (raw["actualizadoEnMs"] as? Number)?.toLong() ?: 0L,
            existeEnServidor = existe,
            esBorrador = esBorradorDoc
        )
    }
}
