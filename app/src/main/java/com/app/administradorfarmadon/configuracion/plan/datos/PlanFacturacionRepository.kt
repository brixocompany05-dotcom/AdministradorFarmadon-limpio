package com.app.administradorfarmadon.configuracion.plan.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PlanFacturacionRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "PlanFacturacionRepo"
        private val TIMEZONE_LIMA = TimeZone.getTimeZone("America/Lima")
    }


    suspend fun resolverClienteId(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val userDoc = PlanFacturacionPaths.usuariosFarmacia(db).document(uid).get().await()
        return userDoc.getString("clienteId")
            ?: (userDoc.get("clienteIds") as? List<*>)?.firstOrNull()?.toString()
            ?: ""
    }

    /**
     * Fuente viva del estado de cuenta (R8 —” Verdad en Tiempo Real).
     * CUATRO fuentes escuchadas en vivo y combinadas:
     *   1. Suscripción (fuente primaria de BRIXO)
     *   2. Farmacia (estado pausado/suspendido + plan cuando aún no hay suscripción)
     *   3. Sedes activas (conteo real)
     *   4. Catálogo del ecosistema (features/nombre/precio que BRIXO edita)
     * Cualquier cambio en cualquiera de ellas repinta la pantalla al instante, sin
     * salir ni reabrir. Antes las fuentes 2—“4 se leían con .get() estático dentro
     * del evento de la suscripción: quedaban congeladas hasta el próximo movimiento.
     */
    fun observarPlanInfo(clienteId: String): Flow<PlanFacturacionInfo> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(PlanFacturacionInfo(estadoSuscripcion = "sin_suscripcion"))
            awaitClose {}
            return@callbackFlow
        }

        var subDoc: DocumentSnapshot? = null
        var estadoFarmacia = ""
        var farmaciaSinSub: Triple<String, String, Int>? = null // planId, nombre, maxSucursales
        var sucursalesActivas = 0
        var monedaCodigoVivo = "PEN"
        var monedaSimboloVivo = "S/"

        fun emitir() {
            val doc = subDoc
            if (doc == null || !doc.exists()) {
                val fb = farmaciaSinSub
                trySend(
                    PlanFacturacionInfo(
                        planId = fb?.first ?: "",
                        planNombre = fb?.second ?: "Sin Plan Asignado",
                        maxSucursales = fb?.third ?: 1,
                        sucursalesActivas = sucursalesActivas,
                        monedaCodigo = monedaCodigoVivo,
                        monedaSimbolo = monedaSimboloVivo,
                        estadoSuscripcion = "sin_suscripcion"
                    )
                )
                return
            }
            val features = (doc.get("planHerramientasContratadas") as? List<String>) ?: emptyList()
            val nombreEcosistema = doc.getString("planNombre") ?: "Plan Estándar"
            val precioEcosistema = (doc.get("precioAlContratar") as? Number)?.toDouble() ?: 0.0
            trySend(construirDesdeSuscripcion(doc, sucursalesActivas, features, nombreEcosistema, precioEcosistema, estadoFarmacia, monedaCodigoVivo, monedaSimboloVivo))
        }

        val farmaciaListener = PlanFacturacionPaths.farmacia(db, clienteId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "Error escuchando farmacia: ${err.message}", err)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    farmaciaSinSub = null
                    estadoFarmacia = ""
                    monedaCodigoVivo = "PEN"
                    monedaSimboloVivo = "S/"
                } else {
                    estadoFarmacia = (snap.getString("estado") ?: "").lowercase()
                    farmaciaSinSub = Triple(
                        snap.getString("planId") ?: "",
                        snap.getString("planNombre") ?: snap.getString("plan") ?: "Sin Plan Asignado",
                        1  // maxSuclusales: ya no en farmacia doc (B4 —” se lee de suscripción)
                    )
                    // Moneda viva por cliente (B4 —” per-client). Actualiza sin relogin.
                    monedaCodigoVivo = snap.getString("monedaOperativa")?.takeIf { it.isNotBlank() } ?: snap.getString("monedaCodigo") ?: "PEN"
                    monedaSimboloVivo = snap.getString("simboloMoneda")?.takeIf { it.isNotBlank() } ?: when (monedaCodigoVivo) {
                        "USD" -> "$"; "EUR" -> "──‚¬"; "COP" -> "$"; "CLP" -> "$"; "ARS" -> "$"; "VES" -> "Bs."; else -> "S/"
                    }
                    // Propaga a SessionManager para que MonedaHelper y caja reflejen el cambio en vivo
                    try {
                        if (monedaCodigoVivo != com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo ||
                            monedaSimboloVivo != com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo) {
                            com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo = monedaCodigoVivo
                            com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo = monedaSimboloVivo
                        }
                    } catch (_: Exception) {}
                }
                emitir()
            }

        val sucursalesListener = PlanFacturacionPaths.sucursales(db, clienteId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "Error escuchando sedes: ${err.message}", err)
                    return@addSnapshotListener
                }
                sucursalesActivas = snap?.size() ?: 0
                emitir()
            }

        val subListener = PlanFacturacionPaths.suscripciones(db, clienteId).limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando suscripción: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                subDoc = snapshot?.documents?.firstOrNull()
                emitir()
            }

        awaitClose {
            subListener.remove()
            farmaciaListener.remove()
            sucursalesListener.remove()
        }
    }

    /**
     * Construye el estado visible desde el documento de suscripción (puro, sin I/O).
     * Réplica EXACTA de la regla de BRIXO (CalculadorEstadoSuscripcion) —” ver comentario
     * de calcularEstadoSuscripcion(). El enriquecimiento en vivo (sedes, catálogo,
     * estado de farmacia) llega como parámetro desde observarPlanInfo().
     */
    private fun construirDesdeSuscripcion(
        doc: DocumentSnapshot,
        sucursalesActivas: Int,
        features: List<String>,
        nombreEcosistema: String,
        precioEcosistema: Double,
        estadoFarmacia: String,
        monedaCodigo: String = "PEN",
        monedaSimbolo: String = "S/"
    ): PlanFacturacionInfo {
            val planId = doc.getString("planId") ?: ""
            val periodo = doc.getString("periodo") ?: "mensual"
            val esPrueba = doc.getBoolean("esPrueba") ?: false
            val diasPrueba = (doc.getLong("diasPruebaContratados") ?: 0L).toInt()
            val maxSucursales = (doc.getLong("maxSucursalesAlContratar") ?: 1L).toInt()
            val precioAlContratar = (doc.get("precioAlContratar") as? Number)?.toDouble() ?: 0.0
            // precioPagado se deriva de precioAlContratar: BRIXO ya no escribe un campo redundante.
            val precioPagado = precioAlContratar
            val precioProximaRenovacion = (doc.get("precioProximaRenovacion") as? Number)?.toDouble()
            val saldoPendiente = (doc.get("saldoPendiente") as? Number)?.toDouble() ?: 0.0
            val pagosRealizados = (doc.getLong("pagosRealizados") ?: 0L).toInt()

            val fInicioDate = parsearFecha(doc.get("fechaInicio"))
            val fFinDate = parsearFecha(doc.get("fechaFin"))
            val fFinOriginalDate = parsearFecha(doc.get("fechaFinOriginal"))
            val ultimoPagoDate = parsearFecha(doc.get("ultimoPagoFecha"))

            // CAUSA RAíZ: Farmadon NO recalcula el estado con su propia lógica distinta.
            // Replica EXACTAMENTE la regla de BRIXO (CalculadorEstadoSuscripcion):
            // misma ventana de prueba (borde <=), misma condición de cortesía
            // (exige fechaFinOriginal vencida), mismo resultado. Así la app del
            // cajero y el panel del agente dicen SIEMPRE lo mismo.
            val calculo = calcularEstadoSuscripcion(
                fechaInicio = fInicioDate,
                fechaFin = fFinDate,
                fechaFinOriginal = fFinOriginalDate,
                esPrueba = esPrueba,
                diasPruebaContratados = diasPrueba,
                periodo = periodo,
                beneficioTipo = doc.getString("beneficioTipo") ?: "",
                beneficioValor = (doc.getLong("beneficioValor") ?: 0L).toInt(),
                ahoraMs = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
            )

            val estado = calculo.estado
            val enPrueba = calculo.esPrueba
            val tieneCortesia = calculo.esCortesia

            // Días restantes REALES: si hay cortesía vigente, BRIXO cuenta los días
            // regalados como acceso hoy, así que se suman al conteo (cierra BUG 3).
            val diasRestantesTotal = if (calculo.fFinMs > calculo.ahoraMs) {
                ((calculo.fFinMs - calculo.ahoraMs) / (24 * 60 * 60 * 1000)).toInt()
            } else 0

            val finPruebaMs = calculo.finPruebaMs
            val diasRestantesPrueba = if (enPrueba && finPruebaMs > calculo.ahoraMs) {
                ((finPruebaMs - calculo.ahoraMs) / (24 * 60 * 60 * 1000)).toInt()
            } else 0

            val beneficioMotivo = doc.getString("beneficioMotivo") ?: ""
            val beneficioFecha = formatearFecha(parsearFecha(doc.get("beneficioFechaOtorgamiento")))
            val diasCortesiaFinal = if (tieneCortesia) calculo.beneficioValor else 0
            val motivoCortesiaFinal = if (tieneCortesia) beneficioMotivo else ""
            val fechaOtorgamientoFinal = if (tieneCortesia && beneficioFecha != "—”") beneficioFecha else ""

            val fInicioMs = fInicioDate?.time ?: 0L
            val fFinMs = fFinDate?.time ?: 0L

            val estadoFinal = when {
                estadoFarmacia == "pausado" || estadoFarmacia == "pausada" -> "pausado"
                estadoFarmacia == "suspendido" || estadoFarmacia == "suspendida" -> "suspendido"
                else -> estado
            }

            // íšNICA FUENTE: herramientas del snapshot de suscripción (B4).
            // Sin fallback al plan vivo —” el contrato congelado es la verdad.
            return PlanFacturacionInfo(
                planId = planId,
                planNombre = nombreEcosistema,
                precioMensual = if (precioAlContratar > 0.0) precioAlContratar else precioEcosistema,
                precioProximaRenovacion = precioProximaRenovacion,
                periodo = periodo,
                maxSucursales = maxSucursales,
                sucursalesActivas = sucursalesActivas, // Regla H2: conteo real sin coerción artificial
                features = features,
                estadoSuscripcion = estadoFinal,
                esPrueba = esPrueba,
                diasPruebaContratados = diasPrueba,
                fechaInicio = if (fInicioMs > 0L) formatearFecha(fInicioDate) else "",
                fechaFin = if (fFinMs > 0L) formatearFecha(fFinDate) else "",
                fechaInicioMs = fInicioMs,
                fechaFinMs = fFinMs,
                fechaFinOriginal = if (fFinOriginalDate != null) formatearFecha(fFinOriginalDate) else "",
                diasRestantesTotal = diasRestantesTotal,
                diasRestantesPrueba = diasRestantesPrueba,
                enPeriodoPrueba = enPrueba,
                fechaFinPrueba = if (finPruebaMs > 0L) formatearFecha(Date(finPruebaMs)) else "",
                pagosRealizados = pagosRealizados,
                ultimoPagoFecha = formatearFecha(ultimoPagoDate),
                precioPagado = precioPagado,
                saldoPendiente = saldoPendiente,
                tieneBeneficioCortesia = tieneCortesia,
                diasCortesia = diasCortesiaFinal,
                motivoCortesia = motivoCortesiaFinal,
                fechaOtorgamientoCortesia = fechaOtorgamientoFinal,
                monedaCodigo = monedaCodigo,
                monedaSimbolo = monedaSimbolo,
                sincronizadoServidor = !doc.metadata.hasPendingWrites()
            )
    }

    fun observarHistorialPagos(clienteId: String): Flow<List<HistorialPagoItem>> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        var auditListener: com.google.firebase.firestore.ListenerRegistration? = null
        val suscripcionesRef = PlanFacturacionPaths.suscripciones(db, clienteId).limit(1)

        val subListener = suscripcionesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando suscripción para historial: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val doc = snapshot?.documents?.firstOrNull()
            if (doc == null || !doc.exists()) {
                auditListener?.remove()
                auditListener = null
                trySend(emptyList())
                return@addSnapshotListener
            }

            val subId = doc.id
            val auditoriaRef = PlanFacturacionPaths.auditoriaSuscripcion(db, clienteId, subId)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)

            auditListener?.remove()
            auditListener = auditoriaRef.addSnapshotListener { auditSnap, auditErr ->
                // Fallback solo si la colección de auditoría NO EXISTE (estado legítimo).
                // Un error real de lectura (permiso/red) NO se oculta como "sin comprobantes".
                if (auditErr != null) {
                    val esInexistente = (auditErr.message ?: "").contains("NOT_FOUND", ignoreCase = true)
                    if (esInexistente) {
                        @Suppress("UNCHECKED_CAST")
                        val historial = (doc.get("historialCambios") as? List<Map<String, Any>>) ?: emptyList()
                        val items = historial.mapIndexed { idx, h ->
                            val rawMonto = (h["monto"] as? Number)?.toDouble()
                                ?: (h["precio"] as? Number)?.toDouble()
                                ?: (h["valorNuevo"] as? Number)?.toDouble()
                                ?: (h["valorNuevo"] as? String)?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
                                ?: 0.0

                            HistorialPagoItem(
                                id = "hist_$idx",
                                fecha = formatearFecha(parsearFecha(h["fecha"])),
                                accion = h["accion"] as? String ?: "MOVIMIENTO",
                                concepto = traducirConcepto(h["accion"] as? String),
                                monto = rawMonto,
                                motivo = h["motivo"] as? String ?: "",
                                admin = h["admin"] as? String ?: "BRIXO Central"
                            )
                        }
                        trySend(items)
                        return@addSnapshotListener
                    }
                    // Error real: sube para que la pantalla lo informe con verdad.
                    Log.e(TAG, "Error leyendo auditoría de pagos: ${auditErr.message}", auditErr)
                    close(auditErr)
                    return@addSnapshotListener
                }

                val lista = (auditSnap?.documents ?: emptyList()).map { aDoc ->
                    @Suppress("UNCHECKED_CAST")
                    val meta = (aDoc.get("metadata") as? Map<String, Any>) ?: emptyMap()
                    val accion = aDoc.getString("accion") ?: "PAGO_REGISTRADO"
                    val precio = (meta["precio"] as? Number)?.toDouble()
                        ?: (meta["monto"] as? Number)?.toDouble()
                        ?: (aDoc.get("precio") as? Number)?.toDouble()
                        ?: (aDoc.get("monto") as? Number)?.toDouble()
                        ?: 0.0
                    val motivo = aDoc.getString("motivo") ?: (meta["motivo"] as? String ?: "")
                    val admin = aDoc.getString("adminEmail") ?: aDoc.getString("realizadoPor") ?: "BRIXO Central"
                    val fFinNue = aDoc.getString("fechaFinNueva") ?: ""
                    val banco = (meta["banco"] as? String) ?: aDoc.getString("banco") ?: ""
                    val numOp = (meta["numeroOperacion"] as? String) ?: (meta["op"] as? String) ?: aDoc.getString("numeroOperacion") ?: ""
                    val compUrl = (meta["comprobanteUrl"] as? String) ?: aDoc.getString("comprobanteUrl") ?: ""

                    HistorialPagoItem(
                        id = aDoc.id,
                        fecha = formatearFecha(parsearFecha(aDoc.get("fecha"))),
                        accion = accion,
                        concepto = traducirConcepto(accion),
                        monto = precio,
                        motivo = motivo,
                        admin = admin,
                        vigenciaHasta = formatearFecha(parsearFecha(fFinNue)),
                        banco = banco,
                        numeroOperacion = numOp,
                        comprobanteUrl = compUrl
                    )
                }

                trySend(lista)
            }
        }

        awaitClose {
            subListener.remove()
            auditListener?.remove()
            auditListener = null
        }
    }

    fun observarCanalesPagoBrixo(): Flow<BrixoCanalesPagoInfo> = callbackFlow {
        val docRef = db.collection("brixo_configuracion").document("empresa")

        val listener = docRef.addSnapshotListener { snap, error ->
            if (error != null) {
                // RAíZ: no maquillar permiso/red como "sin cuentas". Propagar error
                // para que ViewModel lo muestre veraz en canalesError; el combine
                // no se traba porque VM hace .catch { emit(vacío) + canalesError }.
                Log.e(TAG, "Error escuchando canales de pago: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            // snap == null ──†’ fallo de red/permiso real (no es "sin configuración").
            if (snap == null) {
                Log.e(TAG, "Respuesta nula de canales de pago (posible falla de red)")
                close(IllegalStateException("Respuesta nula de BRIXO —” verifica conexión o permisos de brixo_configuracion/empresa"))
                return@addSnapshotListener
            }

            // Documento ausente = BRIXO aún no publica cuentas (estado legítimo, no fallo).
            if (!snap.exists()) {
                trySend(BrixoCanalesPagoInfo())
                return@addSnapshotListener
            }

            val razonSocial = snap.getString("razonSocial") ?: ""
            val ruc = snap.getString("ruc") ?: ""
            val whatsapp = snap.getString("whatsappCobranzas") ?: ""
            val email = snap.getString("emailCobranzas") ?: ""
            val mensajeWa = snap.getString("mensajePrellenadoWhatsapp")
                ?: "Hola BRIXO, adjunto comprobante de pago de mi farmacia."

            @Suppress("UNCHECKED_CAST")
            val cuentasRaw = snap.get("cuentas") as? List<Map<String, Any>> ?: emptyList()
            val metodosActivos = cuentasRaw
                .filterNot { (it["id"] as? String)?.contains("default", ignoreCase = true) == true }
                .filter { (it["activo"] as? Boolean) != false }
                .map { map ->
                    MetodoPagoBrixoItem(
                        id = map["id"] as? String ?: "",
                        bancoNombre = map["bancoNombre"] as? String ?: "",
                        tipoCuenta = map["tipoCuenta"] as? String ?: "CORRIENTE",
                        moneda = map["moneda"] as? String ?: "PEN",
                        numeroCuenta = map["numeroCuenta"] as? String ?: "",
                        numeroCci = map["numeroCci"] as? String ?: "",
                        titular = map["titular"] as? String ?: razonSocial,
                        iconoKey = map["iconoKey"] as? String ?: ""
                    )
                }

            trySend(
                BrixoCanalesPagoInfo(
                    razonSocial = razonSocial,
                    ruc = ruc,
                    whatsappCobranzas = whatsapp,
                    emailCobranzas = email,
                    mensajePrellenado = mensajeWa,
                    metodosActivos = metodosActivos
                )
            )
        }

        awaitClose { listener.remove() }
    }

    fun observarCatalogoPlanes(): Flow<List<PlanCatalogoItem>> = callbackFlow {
        // Ruta canónica de BRIXO: compartido/ecosistema/planes (EcosistemaPaths).
        // Antes se leía db.collection("planes") en la raíz, sitio que BRIXO no usa,
        // por lo que el catálogo nunca cargaba (pantalla muerta disfrazada de "cargando").
        val planesRef = EcosistemaPaths.planes(db)
        val listener = planesRef.addSnapshotListener { snap, err ->
            if (err != null) {
                // RAíZ: no maquillar error de catálogo como lista vacía silenciosa.
                Log.e(TAG, "Error escuchando catálogo de planes: ${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents?.mapNotNull { doc ->
                val nombre = doc.getString("nombre") ?: doc.getString("planNombre") ?: ""
                if (nombre.isBlank()) null
                else {
                    // Moneda nace del país del plan —” verdad por plan, no S/ fijo
                    val paisIsoCat = doc.getString("paisIso")?.uppercase()?.trim() ?: ""
                    val (codigoCat, simboloCat) = when (paisIsoCat) {
                        "AR" -> "ARS" to "$"; "CL" -> "CLP" to "$"; "CO" -> "COP" to "$"
                        "EC" -> "USD" to "$"; "ES" -> "EUR" to "──‚¬"; "PE" -> "PEN" to "S/"
                        "VE" -> "VES" to "Bs."; else -> "PEN" to "S/"
                    }
                    PlanCatalogoItem(
                        id = doc.id,
                        nombre = nombre,
                        precioMensual = (doc.get("precioMensual") as? Number)?.toDouble()
                            ?: (doc.get("precio") as? Number)?.toDouble() ?: 0.0,
                        maxSucursales = (doc.getLong("maxSucursales") ?: 1L).toInt(),
                        diasPrueba = (doc.getLong("diasPrueba") ?: 0L).toInt(),
                        descripcion = doc.getString("descripcion") ?: "",
                        esRecomendado = doc.getBoolean("esRecomendado") ?: false,
                        activo = doc.getBoolean("activo") ?: true,
                        paisIso = paisIsoCat,
                        monedaCodigo = codigoCat,
                        monedaSimbolo = simboloCat
                    )
                }
            } ?: emptyList()
            trySend(lista.sortedBy { it.precioMensual })
        }
        awaitClose { listener.remove() }
    }

    fun observarUltimaSolicitudPago(clienteId: String): Flow<SolicitudPagoInfo?> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }

        val ref = db.collection("compartido").document("ecosistema").collection("solicitudes_pago")
            .whereEqualTo("clienteId", clienteId)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error escuchando solicitudes_pago: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }

            val docs = snapshot?.documents ?: emptyList()
            // Ordenar por fecha más reciente en memoria para evitar fallos por índices compuestos ausentes
            val masReciente = docs.maxByOrNull { doc ->
                parsearFecha(doc.get("updatedAt") ?: doc.get("createdAt") ?: doc.get("subsanadaAt"))?.time ?: 0L
            }

            if (masReciente == null || !masReciente.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            val estado = masReciente.getString("estado") ?: "PENDIENTE"
            val banco = masReciente.getString("banco") ?: ""
            val monto = (masReciente.get("montoReportado") as? Number)?.toDouble() ?: 0.0
            val numOp = masReciente.getString("numeroOperacion") ?: ""
            val compUrl = masReciente.getString("comprobanteUrl") ?: ""
            val mensajeBrixo = masReciente.getString("mensajeBrixo")
                ?: masReciente.getString("motivoObservacion")
                ?: masReciente.getString("motivoRechazo") ?: ""
            val notaCliente = masReciente.getString("notaAclaratoria")
                ?: masReciente.getString("notaAclaratoriaCliente") ?: ""
            val fReporte = formatearFecha(parsearFecha(masReciente.get("createdAt")))
            val fObs = formatearFecha(parsearFecha(masReciente.get("fechaObservacion") ?: masReciente.get("updatedAt")))

            trySend(
                SolicitudPagoInfo(
                    id = masReciente.id,
                    estado = estado,
                    banco = banco,
                    montoReportado = monto,
                    numeroOperacion = numOp,
                    comprobanteUrl = compUrl,
                    mensajeBrixo = mensajeBrixo,
                    notaAclaratoriaCliente = notaCliente,
                    fechaReporte = fReporte,
                    fechaObservacion = fObs
                )
            )
        }

        awaitClose { listener.remove() }
    }

    /**
     * CAUSA RAíZ: réplica EXACTA de la regla de BRIXO (CalculadorEstadoSuscripcion).
     * Farmadon no inventa su propia lógica de estado; usa la misma que el panel para
     * que cajero y agente vean SIEMPRE lo mismo. Misma ventana de prueba (borde <=)
     * y misma condición de cortesía (exige que la fechaFin ORIGINAL ya haya vencido).
     *
     * Resultado del cálculo, con los ms que lo componen para que el llamador cuente
     * los días restantes reales (sumando la cortesía cuando aplica).
     */
    private data class CalculoEstadoSuscripcion(
        val estado: String,
        val esPrueba: Boolean,
        val esCortesia: Boolean,
        val beneficioValor: Int,
        val ahoraMs: Long,
        val fFinMs: Long,
        val finPruebaMs: Long
    )

    private fun calcularEstadoSuscripcion(
        fechaInicio: Date?,
        fechaFin: Date?,
        fechaFinOriginal: Date?,
        esPrueba: Boolean,
        diasPruebaContratados: Int,
        periodo: String,
        beneficioTipo: String,
        beneficioValor: Int,
        ahoraMs: Long
    ): CalculoEstadoSuscripcion {
        val inicioMs = fechaInicio?.time ?: 0L
        val finMs = fechaFin?.time ?: 0L
        val finOriginalMs = fechaFinOriginal?.time ?: 0L

        // Sin fechas -> no hay suscripción que evaluar.
        if (inicioMs == 0L || finMs == 0L) {
            return CalculoEstadoSuscripcion("sin_suscripcion", false, false, 0, ahoraMs, 0L, 0L)
        }

        // Aún no inicia: respetamos la etiqueta histórica del documento.
        if (ahoraMs < inicioMs) {
            return CalculoEstadoSuscripcion("activa", false, false, 0, ahoraMs, finMs, 0L)
        }

        val finPruebaMs = if (diasPruebaContratados > 0 && inicioMs > 0L) {
            inicioMs + (diasPruebaContratados.toLong() * 24 * 60 * 60 * 1000)
        } else 0L

        // 1) PRUEBA: ventana [inicio, inicio + diasPrueba] inclusiva (borde <=, igual que BRIXO).
        if (esPrueba && diasPruebaContratados > 0 && ahoraMs <= finPruebaMs) {
            return CalculoEstadoSuscripcion("prueba", true, false, 0, ahoraMs, finMs, finPruebaMs)
        }

        // 2) Plan gratuito: activo durante su ventana, vencido después.
        if (periodo.equals("gratis", ignoreCase = true)) {
            val estado = if (ahoraMs <= finMs) "activa" else "vencida"
            return CalculoEstadoSuscripcion(estado, false, false, 0, ahoraMs, finMs, finPruebaMs)
        }

        // 3) Cortesía otorgada por BRIXO (beneficio DIAS_GRATIS) que es lo que da acceso hoy.
        //    SOLO cuenta si la fechaFin ORIGINAL ya venció (sin esto Farmadon la mostraba mal).
        val cortesiaVigente = beneficioTipo == "DIAS_GRATIS" && beneficioValor > 0
            && ahoraMs <= finMs
            && (finOriginalMs == 0L || ahoraMs > finOriginalMs)
        if (cortesiaVigente) {
            return CalculoEstadoSuscripcion("cortesia", false, true, beneficioValor, ahoraMs, finMs, finPruebaMs)
        }

        // 4) Plan pagado vigente (incluye renovaciones que extendieron fechaFin).
        if (ahoraMs <= finMs) {
            val estado = if ((finMs - ahoraMs) / (24 * 60 * 60 * 1000) <= 5) "por_vencer" else "activa"
            return CalculoEstadoSuscripcion(estado, false, false, 0, ahoraMs, finMs, finPruebaMs)
        }

        // 5) Sin acceso.
        return CalculoEstadoSuscripcion("vencida", false, false, 0, ahoraMs, finMs, finPruebaMs)
    }

    private fun traducirConcepto(accion: String?): String = when (accion) {
        "RENOVAR_SUSCRIPCION", "RENOVAR" -> "Renovación Mensual de Servicio"
        "ACTUALIZAR_SALDO" -> "Abono / Actualización de Saldo"
        "EXTENDER_SUSCRIPCION", "EXTENDER" -> "Días de Cortesía / Extensión"
        "CAMBIO_PLAN" -> "Actualización de Plan Comercial"
        "APROBAR_SOLICITUD_FARMACIA", "ALTA_INICIAL" -> "Alta Inicial y Activación de Servicio"
        else -> "Registro de Pago y Servicio"
    }

    private fun parsearFecha(valor: Any?): Date? = when (valor) {
        is Timestamp -> valor.toDate()
        is Date -> valor
        is Number -> {
            val num = valor.toLong()
            if (num > 100_000_000_000L) Date(num)
            else if (num > 0L) Date(num * 1000L)
            else null
        }
        is String -> {
            if (valor.isBlank()) null
            else {
                val formatos = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "dd/MM/yyyy"
                )
                var resultado: Date? = null
                for (fmt in formatos) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        resultado = sdf.parse(valor)
                        if (resultado != null) break
                    } catch (_: Exception) {}
                }
                resultado
            }
        }
        else -> null
    }


    private fun formatearFecha(date: Date?): String {
        if (date == null) return "—”"
        val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.forLanguageTag("es-PE")).apply {
            timeZone = TIMEZONE_LIMA
        }
        return sdf.format(date)
    }
}

