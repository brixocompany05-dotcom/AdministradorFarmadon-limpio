package com.app.administradorfarmadon.configuracion.sucursales.datos
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class InfoPlanCliente(
    val planId: String = "",
    val planNombre: String = "Plan Estándar",
    val maxSucursales: Int = 1,
    // Verdad honesta: la suscripción existe pero BRIXO no escribió el límite.
    // La app no inventa un "1/1": muestra que falta configuración del panel central.
    val limiteNoConfigurado: Boolean = false
)

class SucursalesRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "SucursalesRepository"
    }

    suspend fun resolverClienteId(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val userDoc = SucursalesPaths.usuariosFarmacia(db).document(uid).get().await()
        return userDoc.getString("clienteId")
            ?: (userDoc.get("clienteIds") as? List<*>)?.firstOrNull()?.toString()
            ?: ""
    }

    /**
     * Plan vigente de la farmacia. Combina farmacia doc (planId, planNombre)
     * con sesión de suscripción (maxSucursalesAlContratar). La suscripción
     * es la única fuente de maxSuclusales (B4 — contrato congelado). No se
     * consulta el catálogo de planes en vivo.
     */
    fun observarInfoPlan(clienteId: String): Flow<InfoPlanCliente> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(InfoPlanCliente())
            awaitClose {}
            return@callbackFlow
        }

        var planId = ""
        var planNombre = "Plan Estándar"
        var maxSucursalesSub: Int? = null
        var subDocumentoExiste = false

        fun emitir() {
            val maxFinal = maxSucursalesSub ?: 1
            // Si hay suscripción pero sin límite escrito, se reporta como no configurado
            // (sin suscripción alguna se mantiene el 1 cerrado: fail-closed honesto).
            val limiteNoConfigurado = subDocumentoExiste && maxSucursalesSub == null
            trySend(InfoPlanCliente(planId = planId, planNombre = planNombre, maxSucursales = maxFinal, limiteNoConfigurado = limiteNoConfigurado))
        }

        val farmaciaListener = SucursalesPaths.farmacia(db, clienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando plan de la farmacia: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    planId = snapshot.getString("planId") ?: ""
                    planNombre = snapshot.getString("planNombre") ?: "Plan Estándar"
                }
                emitir()
            }

        val subListener = SucursalesPaths.farmacia(db, clienteId).collection("suscripciones").limit(1)
            .addSnapshotListener { subSnapshot, subError ->
                if (subError != null) {
                    Log.e(TAG, "Error escuchando suscripción: ${subError.message}", subError)
                    return@addSnapshotListener
                }
                val subDoc = subSnapshot?.documents?.firstOrNull()
                subDocumentoExiste = subDoc != null
                maxSucursalesSub = (subDoc?.getLong("maxSucursalesAlContratar") ?: 0L).toInt().takeIf { it > 0 }
                emitir()
            }

        awaitClose {
            farmaciaListener.remove()
            subListener.remove()
        }
    }

    fun observarSucursales(clienteId: String): Flow<List<Sucursal>> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = SucursalesPaths.sucursales(db, clienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando sucursales: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val sucursales = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val geo = doc.getGeoPoint("ubicacionGeo")
                        Sucursal(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Sede Principal",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            latitud = geo?.latitude ?: doc.getDouble("latitud"),
                            longitud = geo?.longitude ?: doc.getDouble("longitud"),
                            esPrincipal = doc.getBoolean("esPrincipal") ?: false,
                            activa = doc.getBoolean("activa") ?: true,
                            responsable = doc.getString("responsable") ?: "",
                            codigoInterno = doc.getString("codigoInterno") ?: "",
                            fechaCreacion = doc.get("fechaCreacion")
                        )
                    } catch (e: Exception) {
                        // R3: cero fallas silenciosas. No descartamos la sede ni la ocultamos;
                        // la mostramos con datos de respaldo para que el dueño vea que existe
                        // y reportamos el error real en el log.
                        Log.e(TAG, "Sede ${doc.id} tiene datos corruptos: ${e.message}", e)
                        Sucursal(
                            id = doc.id,
                            nombre = "Sede con datos corruptos",
                            direccion = "Requiere revisión en BrixoPanel"
                        )
                    }
                } ?: emptyList()

                trySend(sucursales)
            }

        awaitClose { listener.remove() }
    }

    private fun validarPermisoPrincipal(accion: String) {
        val sucursalActiva = SessionManager.sucursalIdEfectiva.ifBlank { SessionManager.sucursalId }
        if (!sucursalActiva.equals("principal", ignoreCase = true)) {
            throw IllegalStateException("Solo la sede principal puede $accion.")
        }
    }

    suspend fun guardarSucursal(clienteId: String, sucursal: Sucursal, pagosSucursalNueva: Set<String>? = null) {
        if (clienteId.isBlank()) throw IllegalArgumentException("ID de farmacia inválido.")
        validarPermisoPrincipal("registrar o editar sucursales")

        // CONTRATO CONGELADO (B4): maxSuclusales está en la suscripción, no en el
        // doc de farmacia. Leemos antes de la tx para feedback rápido, pero la verdad
        // se re-lee DENTRO de la tx (raíz: evita que plan cambie entre lectura y tx).
        var maxPermitidoGlobal = 1
        val subSnap = SucursalesPaths.farmacia(db, clienteId).collection("suscripciones").limit(1).get().await()
        val subDoc = subSnap.documents.firstOrNull()
        val maxFromSub = (subDoc?.getLong("maxSucursalesAlContratar") ?: 0L).toInt().takeIf { it > 0 }
        if (maxFromSub != null) maxPermitidoGlobal = maxFromSub
        val subDocRef = subDoc?.reference

        db.runTransaction { tx ->
            val farmaciaRef = SucursalesPaths.farmacia(db, clienteId)
            val farmaciaSnap = tx.get(farmaciaRef)
            if (!farmaciaSnap.exists()) {
                throw IllegalStateException("No se encontró el registro de la farmacia.")
            }

            @Suppress("UNCHECKED_CAST")
            val rawSucursales = farmaciaSnap.get("sucursales") as? List<Map<String, Any?>> ?: emptyList()

            val nombreTrim = sucursal.nombre.trim()
            val direccionTrim = sucursal.direccion.trim()

            // Blindaje 1: Cero nombres de sede duplicados en la misma farmacia
            val nombreDuplicado = rawSucursales.any {
                it["id"] != sucursal.id && (it["nombre"] as? String)?.trim().equals(nombreTrim, ignoreCase = true)
            }
            if (nombreDuplicado) {
                throw IllegalStateException("NOMBRE_DUPLICADO: Ya existe una sede registrada con este nombre.")
            }

            // Blindaje 2: Cero direcciones físicas idénticas en la misma farmacia
            val direccionDuplicada = rawSucursales.any {
                it["id"] != sucursal.id && (it["direccion"] as? String)?.trim().equals(direccionTrim, ignoreCase = true)
            }
            if (direccionDuplicada) {
                throw IllegalStateException("DIRECCION_DUPLICADA: Ya existe una sede registrada en esta misma dirección.")
            }

                if (sucursal.id.isBlank()) {
                    // ── MODO CREACIÓN: Validar cupos en servidor ──
                    // RAÍZ: re-leer max dentro de la tx (evita carrera si BRIXO cambió plan entre get() y tx)
                    var maxTx = maxPermitidoGlobal
                    if (subDocRef != null) {
                        try {
                            val freshSnap = tx.get(subDocRef)
                            val freshMax = (freshSnap.getLong("maxSucursalesAlContratar") ?: 0L).toInt().takeIf { it > 0 }
                            if (freshMax != null) maxTx = freshMax
                        } catch (_: Exception) { /* conserva maxPermitidoGlobal si falla lectura tx */ }
                    }
                    // PARIDAD CON EL PANEL: solo sedes ACTIVAS consumen cupo
                    // (el panel cuenta la subcolección con activa==true). Las
                    // desactivadas quedan fuera del conteo — cero off-by-N.
                    val activasEnDoc = rawSucursales.count { it["activa"] as? Boolean != false }
                    if (activasEnDoc >= maxTx) {
                        throw IllegalStateException("LIMITE_ALCANZADO: Tu plan contratado permite hasta $maxTx sedes.")
                    }

                // Generar referencia y código interno secuencial
                // RAÍZ: no usar size+1 (colisiona tras borrado). Usar max sufijo numérico +1
                val col = SucursalesPaths.sucursales(db, clienteId)
                val docRef = col.document()
                val sucursalId = docRef.id
                val codigoSecuencial = sucursal.codigoInterno.trim().ifBlank {
                    val maxNum = rawSucursales.mapNotNull { m ->
                        (m["codigoInterno"] as? String)?.let { c ->
                            Regex("""SEDE-0*(\d+)""").find(c)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        }
                    }.maxOrNull() ?: rawSucursales.size
                    val siguiente = maxNum + 1
                    "SEDE-0$siguiente"
                }

                val data = mutableMapOf<String, Any?>(
                    "id" to sucursalId,
                    "clienteId" to clienteId,
                    "farmaciaId" to clienteId,
                    "sucursalId" to sucursalId,
                    "nombre" to sucursal.nombre.trim(),
                    "direccion" to sucursal.direccion.trim(),
                    "telefono" to sucursal.telefono.trim(),
                    "esPrincipal" to false,
                    "activa" to sucursal.activa,
                    "responsable" to sucursal.responsable.trim(),
                    "codigoInterno" to codigoSecuencial,
                    "fechaCreacion" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (sucursal.latitud != null && sucursal.longitud != null) {
                    data["ubicacionGeo"] = GeoPoint(sucursal.latitud, sucursal.longitud)
                    data["latitud"] = sucursal.latitud
                    data["longitud"] = sucursal.longitud
                }

                // 1. Escribir documento en subcolección
                tx.set(docRef, data)

                // 1b. CONTRATO DE MÉTODOS DE PAGO DE LA SEDE NUEVA (B4 — queda fijo al nacer):
                //     se crean SOLO las instancias de los tipos elegidos al crear la sede.
                //     La sede principal puede luego activar/desactivar aquí, pero las demás
                //     sedes ya creadas conservan su contrato sin pisarse entre sí.
                if (pagosSucursalNueva != null) {
                    val instancias = pagosSucursalNueva.sorted().associate { tipoId ->
                        UUID.randomUUID().toString() to mapOf(
                            "farmaciaId" to clienteId,
                            "sucursalId" to sucursalId,
                            "tipoId" to tipoId,
                            "activa" to true,
                            "datos" to emptyMap<String, String>()
                        )
                    }
                    if (instancias.isNotEmpty()) {
                        tx.set(
                            FarmadonPaths.sucursal(db, clienteId, sucursalId)
                                .collection("catalogos").document("metodosPago"),
                            mapOf("instancias" to instancias),
                            SetOptions.merge()
                        )
                    }
                }

                // 2. Array resumen = ESPEJO de la subcolección real (mismo tamaño, mismo id).
                //    Fuente de verdad: la subcolección. El array nunca miente porque
                //    siempre se reconstruye desde los documentos reales en el lock.
                val nuevaLista = rawSucursales + mapOf(
                    "id" to sucursalId,
                    "nombre" to sucursal.nombre.trim(),
                    "direccion" to sucursal.direccion.trim()
                )
                // JUSTICIA DE CUPO CONSISTENTE (paridad total): el techo compara
                // ACTIVAS contra ACTIVAS — sedes desactivadas jamás bloquean una
                // creación legítima ni simulan inconsistencia que no existe.
                val activasTrasCrear = nuevaLista.count { it["activa"] as? Boolean != false }
                if (activasTrasCrear > maxTx) {
                    throw IllegalStateException("INCONSISTENCIA_CUPO: Se superaría el cupo de $maxTx sedes activas del plan.")
                }
                tx.update(farmaciaRef, mapOf(
                    "sucursales" to nuevaLista,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 3. Registrar Evento de Auditoría Atómica (Ecosistema Compartido + Cliente)
                val userEmail = auth.currentUser?.email ?: "dueño@farmacia.com"
                val userUid = auth.currentUser?.uid ?: ""
                val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
                val auditLocalRef = SucursalesPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

                val auditData = mapOf(
                    "seccion" to "sucursales",
                    "actorUid" to userUid,
                    "actorEmail" to userEmail,
                    "actorRol" to "DUEÑO_FARMACIA",
                    "actorType" to "HUMAN",
                    "tenantId" to clienteId,
                    "entidad" to "sucursal",
                    "entidadId" to clienteId,
                    "sucursalId" to sucursalId,
                    "accion" to "CREACION_SUCURSAL",
                    "motivo" to "Registro de nueva sede operativa: ${sucursal.nombre.trim()} ($codigoSecuencial)",
                    "despues" to mapOf(
                        "nombre" to sucursal.nombre.trim(),
                        "direccion" to sucursal.direccion.trim(),
                        "telefono" to sucursal.telefono.trim(),
                        "responsable" to sucursal.responsable.trim(),
                        "codigoInterno" to codigoSecuencial
                    ),
                    "timestamp" to FieldValue.serverTimestamp()
                )
                tx.set(auditGlobalRef, auditData)
                tx.set(auditLocalRef, auditData)
            } else {
                // ── MODO EDICIÓN: Actualizar sede existente ──
                val docRef = SucursalesPaths.sucursales(db, clienteId).document(sucursal.id)
                val existingSnap = tx.get(docRef)
                val esPrincipalReal = existingSnap.getBoolean("esPrincipal") ?: sucursal.esPrincipal

                val nombreAnterior = existingSnap.getString("nombre") ?: ""
                val direccionAnterior = existingSnap.getString("direccion") ?: ""
                val telefonoAnterior = existingSnap.getString("telefono") ?: ""
                val responsableAnterior = existingSnap.getString("responsable") ?: ""
                val activaAnterior = existingSnap.getBoolean("activa") ?: true

                val data = mutableMapOf<String, Any?>(
                    "id" to sucursal.id,
                    "clienteId" to clienteId,
                    "farmaciaId" to clienteId,
                    "sucursalId" to sucursal.id,
                    "nombre" to sucursal.nombre.trim(),
                    "direccion" to sucursal.direccion.trim(),
                    "telefono" to sucursal.telefono.trim(),
                    "esPrincipal" to esPrincipalReal,
                    "activa" to sucursal.activa,
                    "responsable" to sucursal.responsable.trim(),
                    "codigoInterno" to sucursal.codigoInterno.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (sucursal.latitud != null && sucursal.longitud != null) {
                    data["ubicacionGeo"] = GeoPoint(sucursal.latitud, sucursal.longitud)
                    data["latitud"] = sucursal.latitud
                    data["longitud"] = sucursal.longitud
                }

                tx.update(docRef, data)

                // Actualizar entrada correspondiente en el array resumen (por id = única llave real)
                val listaActualizada = rawSucursales.map { item ->
                    if (item["id"] == sucursal.id) {
                        mapOf(
                            "id" to sucursal.id,
                            "nombre" to sucursal.nombre.trim(),
                            "direccion" to sucursal.direccion.trim()
                        )
                    } else {
                        item
                    }
                }
                tx.update(farmaciaRef, mapOf(
                    "sucursales" to listaActualizada,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // Registrar Evento de Auditoría Atómica por Edición
                val userEmail = auth.currentUser?.email ?: "dueño@farmacia.com"
                val userUid = auth.currentUser?.uid ?: ""
                val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
                val auditLocalRef = SucursalesPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

                val auditData = mapOf(
                    "seccion" to "sucursales",
                    "actorUid" to userUid,
                    "actorEmail" to userEmail,
                    "actorRol" to "DUEÑO_FARMACIA",
                    "actorType" to "HUMAN",
                    "tenantId" to clienteId,
                    "entidad" to "sucursal",
                    "entidadId" to clienteId,
                    "sucursalId" to sucursal.id,
                    "accion" to "EDICION_SUCURSAL",
                    "motivo" to "Edición de sede: ${sucursal.nombre.trim()} (${sucursal.codigoInterno.trim()})",
                    "antes" to mapOf(
                        "nombre" to nombreAnterior,
                        "direccion" to direccionAnterior,
                        "telefono" to telefonoAnterior,
                        "responsable" to responsableAnterior,
                        "activa" to activaAnterior
                    ),
                    "despues" to mapOf(
                        "nombre" to sucursal.nombre.trim(),
                        "direccion" to sucursal.direccion.trim(),
                        "telefono" to sucursal.telefono.trim(),
                        "responsable" to sucursal.responsable.trim(),
                        "activa" to sucursal.activa
                    ),
                    "timestamp" to FieldValue.serverTimestamp()
                )
                tx.set(auditGlobalRef, auditData)
                tx.set(auditLocalRef, auditData)
            }
        }.await()
    }

    suspend fun eliminarSucursal(clienteId: String, sucursalId: String) {
        validarPermisoPrincipal("activar, desactivar o eliminar sucursales")
        if (sucursalId == "principal") {
            throw IllegalStateException("La Sede Principal es el ancla del negocio y no puede ser eliminada.")
        }
        if (clienteId.isBlank()) throw IllegalArgumentException("ID de farmacia inválido.")

        // Consultar colaboradores vinculados a la sede antes de eliminarla
        val usuariosSubRef = SucursalesPaths.usuarios(db, clienteId)
        val usuariosAfectados = usuariosSubRef.whereEqualTo("sucursalId", sucursalId).get().await()

        db.runTransaction { tx ->
            val docRef = SucursalesPaths.sucursales(db, clienteId).document(sucursalId)
            val snap = tx.get(docRef)
            if (snap.exists() && snap.getBoolean("esPrincipal") == true) {
                throw IllegalStateException("La Sede Principal no puede ser eliminada.")
            }

            val nombreEliminado = snap.getString("nombre") ?: "Sede"
            val direccionEliminada = snap.getString("direccion") ?: ""

            val farmaciaRef = SucursalesPaths.farmacia(db, clienteId)
            val farmaciaSnap = tx.get(farmaciaRef)

            // 1. Reubicar colaboradores asignados a 'todas' para que sigan operativos
            // RAÍZ: re-leer cada colaborador dentro de la tx para evitar pisar cambios
            // concurrentes y no fallar si el doc ya fue movido/borrado.
            usuariosAfectados.documents.forEach { uDoc ->
                val uRef = SucursalesPaths.usuarios(db, clienteId).document(uDoc.id)
                val globalRef = SucursalesPaths.usuariosFarmacia(db).document(uDoc.id)
                try {
                    val freshSnap = tx.get(uRef)
                    if (!freshSnap.exists()) return@forEach
                    val sucActual = freshSnap.getString("sucursalId")
                    if (sucActual != sucursalId) return@forEach
                } catch (_: Exception) { return@forEach }
                tx.update(uRef, mapOf(
                    "sucursalId" to "todas",
                    "sucursalNombre" to "Todas las Sedes (Itinerante)",
                    "actualizadoEn" to FieldValue.serverTimestamp()
                ))
                tx.set(globalRef, mapOf(
                    "sucursalId" to "todas",
                    "sucursalNombre" to "Todas las Sedes (Itinerante)",
                    "updatedAt" to FieldValue.serverTimestamp()
                ), com.google.firebase.firestore.SetOptions.merge())
            }

            // 2. Eliminar documento de sucursal
            tx.delete(docRef)

            // 3. Limpiar array resumen en la farmacia (nada en el cliente)
            if (farmaciaSnap.exists()) {
                @Suppress("UNCHECKED_CAST")
                val rawSucursales = farmaciaSnap.get("sucursales") as? List<Map<String, Any?>> ?: emptyList()
                val listaFiltrada = rawSucursales.filter { it["id"] != sucursalId }
                tx.update(farmaciaRef, mapOf(
                    "sucursales" to listaFiltrada,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            // 4. Registrar Evento de Auditoría Atómica por Eliminación
            val userEmail = auth.currentUser?.email ?: "dueño@farmacia.com"
            val userUid = auth.currentUser?.uid ?: ""
            val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
            val auditLocalRef = SucursalesPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

            val auditData = mapOf(
                "seccion" to "sucursales",
                "actorUid" to userUid,
                "actorEmail" to userEmail,
                "actorRol" to "DUEÑO_FARMACIA",
                "actorType" to "HUMAN",
                "tenantId" to clienteId,
                "entidad" to "sucursal",
                "entidadId" to clienteId,
                "sucursalId" to sucursalId,
                "accion" to "ELIMINACION_SUCURSAL",
                "motivo" to "Baja de sede: $nombreEliminado ($direccionEliminada). ${usuariosAfectados.size()} colaboradores reubicados a Sede Itinerante.",
                "colaboradoresReubicados" to usuariosAfectados.size(),
                "antes" to mapOf(
                    "nombre" to nombreEliminado,
                    "direccion" to direccionEliminada
                ),
                "timestamp" to FieldValue.serverTimestamp()
            )
            tx.set(auditGlobalRef, auditData)
            tx.set(auditLocalRef, auditData)
        }.await()
        // Cierre del todo-o-nada: ningún colaborador puede quedar apuntando a una sede
        // que ya no existe (ni siquiera por un alta simultánea durante el borrado).
        // Se reintenta; si no cierra, se grita la verdad: jamás silencio ni éxito falso (R3/R9).
        var reubicacionCerrada = false
        repeat(3) { intento ->
            if (!reubicacionCerrada) {
                try {
                    val huerfanos = SucursalesPaths.usuarios(db, clienteId)
                        .whereEqualTo("sucursalId", sucursalId).get().await()
                    if (!huerfanos.isEmpty) {
                        val batch = db.batch()
                        huerfanos.documents.forEach { d ->
                            batch.update(d.reference, mapOf(
                                "sucursalId" to "todas",
                                "sucursalNombre" to "Todas las Sedes (Itinerante)",
                                "actualizadoEn" to FieldValue.serverTimestamp()
                            ))
                            val gRef = SucursalesPaths.usuariosFarmacia(db).document(d.id)
                            batch.set(gRef, mapOf(
                                "sucursalId" to "todas",
                                "sucursalNombre" to "Todas las Sedes (Itinerante)",
                                "updatedAt" to FieldValue.serverTimestamp()
                            ), com.google.firebase.firestore.SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                    reubicacionCerrada = true
                } catch (e: Exception) {
                    Log.e(TAG, "Reubicación post-borrado: intento ${intento + 1} falló: ${e.message}", e)
                    if (intento < 2) delay(1500)
                }
            }
        }
        if (!reubicacionCerrada) {
            throw IllegalStateException(
                "La sede fue eliminada, pero no se pudo confirmar la reubicación de colaboradores por un fallo de red. " +
                    "Nada quedó oculto: abre la pantalla Personal y confirma que nadie siga en la sede eliminada; " +
                    "si el problema persiste, contacta a soporte BRIXO."
            )
        }
    }
}
