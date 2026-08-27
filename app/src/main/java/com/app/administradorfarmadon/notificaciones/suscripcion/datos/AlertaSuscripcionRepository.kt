package com.app.administradorfarmadon.notificaciones.suscripcion.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

class AlertaSuscripcionRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "AlertaSuscripcionRepo"
        private const val MAX_HISTORIAL_LEIDAS = 30
        private val TIMEZONE_LIMA = TimeZone.getTimeZone("America/Lima")
    }

    fun escucharAlertaPendiente(
        clienteId: String,
        usuarioUid: String,
        esAdminODueno: Boolean
    ): Flow<AlertaSuscripcionItem?> = callbackFlow {
        if (clienteId.isBlank() || usuarioUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // R1: Aislamiento por farmacia
        if (clienteId != SessionManager.clienteIdGarantizado) {
            trySend(null)
            close()
            return@callbackFlow
        }

        var leidasIds = emptySet<String>()
        // Memoria del plan conocido por suscripción: si BRIXO lo cambia en vivo,
        // avisamos UNA vez (la primera carga siembra la memoria sin avisar).
        val ultimoPlanVisto = mutableMapOf<String, String>()
        var ultimoSnapshotSuscripcion: com.google.firebase.firestore.DocumentSnapshot? = null
        var ultimoSnapshotPagoObservado: com.google.firebase.firestore.DocumentSnapshot? = null

        fun evaluarYEmitir() {
            val subDoc = ultimoSnapshotSuscripcion
            if (subDoc == null || !subDoc.exists()) {
                trySend(null)
                return
            }

            val estado = subDoc.getString("estado") ?: "activa"

            // Si está suspendida o vencida, no emitir alertas flotantes positivas (el bloqueo total se encarga)
            if (estado == "suspendido" || estado == "suspendida") {
                trySend(null)
                return
            }

            val fFinDate = parsearFecha(subDoc.get("fechaFin"))
            val fInicioDate = parsearFecha(subDoc.get("fechaInicio"))
            val fUltimoPagoDate = parsearFecha(subDoc.get("ultimoPagoFecha"))

            val sdfId = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TIMEZONE_LIMA }
            val sdfHumano = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.forLanguageTag("es-PE")).apply { timeZone = TIMEZONE_LIMA }

            val fFinIdStr = fFinDate?.let { sdfId.format(it) } ?: ""
            val fFinHumana = fFinDate?.let { sdfHumano.format(it) } ?: "fecha próxima"

            val diasPrueba = (subDoc.getLong("diasPruebaContratados") ?: 0L).toInt()
            val beneficioTipo = subDoc.getString("beneficioTipo") ?: ""
            val beneficioValor = (subDoc.getLong("beneficioValor") ?: 0L).toInt()
            val pagosRealizados = (subDoc.getLong("pagosRealizados") ?: 0L).toInt()
            val esPrimerAlta = pagosRealizados <= 1 && (fUltimoPagoDate == null || fInicioDate == null || fUltimoPagoDate.time <= fInicioDate.time + 86400000L)

            val ahoraMs = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
            val fInicioMs = fInicioDate?.time ?: ahoraMs
            val fFinMs = fFinDate?.time ?: (ahoraMs + 30L * 24 * 60 * 60 * 1000)
            val finPruebaMs = if (diasPrueba > 0) fInicioMs + (diasPrueba.toLong() * 24 * 60 * 60 * 1000) else 0L

            val diasRestantesTotal = ((fFinMs - ahoraMs) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)

            // Lista de Candidatas en orden de prioridad de negocio
            val candidatas = mutableListOf<AlertaSuscripcionItem>()

            // 0. COMPROBANTE DE PAGO OBSERVADO POR BRIXO (Máxima Prioridad - Solo Admin/Dueño)
            val pagoObs = ultimoSnapshotPagoObservado
            if (pagoObs != null && pagoObs.exists()) {
                val est = pagoObs.getString("estado") ?: ""
                if (est.equals("OBSERVADO", ignoreCase = true) ||
                    est.equals("OBSERVADA", ignoreCase = true) ||
                    est.equals("RECHAZADO", ignoreCase = true) ||
                    est.equals("RECHAZADA", ignoreCase = true)) {
                    val mensajeBrixo = pagoObs.getString("mensajeBrixo")
                        ?: pagoObs.getString("motivoObservacion")
                        ?: "BRIXO observó tu comprobante de pago."
                    val obsId = "PAGO_OBS_${pagoObs.id}_${pagoObs.get("updatedAt")?.hashCode() ?: 0}"
                    candidatas.add(
                        AlertaSuscripcionItem(
                            id = obsId,
                            tipo = TipoAlertaSuscripcion.COMPROBANTE_OBSERVADO,
                            titulo = "⚠️ Comprobante Observado por BRIXO",
                            mensaje = mensajeBrixo,
                            fechaReferencia = "",
                            esPersistente = true,
                            autoHideSegundos = 0,
                            esSoloAdminODueno = true
                        )
                    )
                }
            }

            // 0c. ACUSE AMBIENTE — COMPROBANTE EN REVISIÓN (H4 sellado):
            // tras cerrar el diálogo de envío, la farmacia SABE que su voucher
            // está vivo en la bandeja de BRIXO, sin abrir Mi Plan a ciegas.
            // Persistente hasta decisión del panel; silencioso en APROBADO
            // porque esa verdad ya llega por "Plan Renovado con Éxito".
            if (pagoObs != null && pagoObs.exists()) {
                val estEnCurso = (pagoObs.getString("estado") ?: "").uppercase()
                if (estEnCurso == "PENDIENTE" || estEnCurso == "SUBSANADA") {
                    val enRevId = "PAGO_ENREV_${pagoObs.id}_${(pagoObs.get("updatedAt") ?: "").hashCode()}"
                    candidatas.add(
                        AlertaSuscripcionItem(
                            id = enRevId,
                            tipo = TipoAlertaSuscripcion.COMPROBANTE_EN_REVISION,
                            titulo = "📨 Comprobante enviado — En revisión por BRIXO",
                            mensaje = if (estEnCurso == "SUBSANADA")
                                "Recibimos tu reenvío corregido. BRIXO lo validará y tu plan se extenderá al aprobarlo."
                            else "Recibimos tu comprobante (${pagoObs.getString("banco") ?: ""} · OP ${pagoObs.getString("numeroOperacion") ?: "—"}). Te avisamos apenas sea validado.",
                            fechaReferencia = "",
                            esPersistente = true,
                            autoHideSegundos = 0,
                            esSoloAdminODueno = true
                        )
                    )
                }
            }

            // 0b. CAMBIO DE PLAN CONFIRMADO POR BRIXO (Solo Admin/Dueño)
            val planIdSub = subDoc.getString("planId") ?: ""
            val planNombreSub = subDoc.getString("planNombre") ?: ""
            val planFirmaActual = "$planIdSub|$planNombreSub"
            // Sello único del evento: cada escritura del panel tiene su updatedAt,
            // así A→B→A→B avisa en CADA cambio real sin duplicar ni callarse.
            val selloEventoMs = subDoc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            val planPrevio = ultimoPlanVisto[subDoc.id]
            if (!planPrevio.isNullOrBlank() && planPrevio != planFirmaActual && fFinIdStr.isNotBlank()) {
                candidatas.add(
                    AlertaSuscripcionItem(
                        id = "PLAN_CAMBIO_${planIdSub}_${selloEventoMs}",
                        tipo = TipoAlertaSuscripcion.CAMBIO_PLAN,
                        titulo = if (planNombreSub.isNotBlank()) "Tu plan ahora es $planNombreSub"
                                 else "Tu suscripción fue actualizada por BRIXO",
                        mensaje = "Los cambios ya están activos en tu sistema. Consulta Mi Plan para ver los detalles.",
                        fechaReferencia = fFinHumana,
                        esPersistente = false,
                        autoHideSegundos = 8,
                        esSoloAdminODueno = true
                    )
                )
            }
            ultimoPlanVisto[subDoc.id] = planFirmaActual

            // 1. AVISO URGENTE 48 HORAS (Solo Admin/Dueño)
            if (diasRestantesTotal in 0..2 && estado != "vencida" && fFinIdStr.isNotBlank()) {
                candidatas.add(
                    AlertaSuscripcionItem(
                        id = "AVISO_48H_$fFinIdStr",
                        tipo = TipoAlertaSuscripcion.AVISO_PREVENTIVO_48H,
                        titulo = "⚠️ Tu Plan Finaliza en ${if (diasRestantesTotal == 0) "pocas horas" else "$diasRestantesTotal días"}",
                        mensaje = "Tu vigencia termina el $fFinHumana. Regulariza tu mensualidad con BRIXO para evitar pausas en el servicio.",
                        fechaReferencia = fFinHumana,
                        esPersistente = true,
                        autoHideSegundos = 0,
                        esSoloAdminODueno = true
                    )
                )
            }

            // 2. PRÓRROGA DE CORTESÍA OTORGADA (Todos los usuarios)
            if (beneficioTipo == "DIAS_GRATIS" && beneficioValor > 0 && fFinIdStr.isNotBlank()) {
                candidatas.add(
                    AlertaSuscripcionItem(
                        id = "CORTESIA_${fFinIdStr}_${beneficioValor}D",
                        tipo = TipoAlertaSuscripcion.CORTESIA_OTORGADA,
                        titulo = "🎁 Días de Cortesía Otorgados por BRIXO",
                        mensaje = "Cuentas con $beneficioValor días de prórroga comercial activa (vigencia hasta el $fFinHumana).",
                        fechaReferencia = fFinHumana,
                        esPersistente = false,
                        autoHideSegundos = 8,
                        esSoloAdminODueno = false
                    )
                )
            }

            // 3. PAGO / RENOVACIÓN vs BIENVENIDA INICIAL
            if (pagosRealizados > 0 && fFinIdStr.isNotBlank()) {
                if (esPrimerAlta) {
                    candidatas.add(
                        AlertaSuscripcionItem(
                            id = "BIENVENIDA_$fFinIdStr",
                            tipo = TipoAlertaSuscripcion.BIENVENIDA_INICIAL,
                            titulo = "🎉 ¡Bienvenido a Farmadon!",
                            mensaje = "Tu servicio está activo y configurado con vigencia hasta el $fFinHumana.",
                            fechaReferencia = fFinHumana,
                            esPersistente = false,
                            autoHideSegundos = 7,
                            esSoloAdminODueno = false
                        )
                    )
                } else {
                    candidatas.add(
                        AlertaSuscripcionItem(
                            id = "PAGO_$fFinIdStr",
                            tipo = TipoAlertaSuscripcion.PAGO_EXITOSO,
                            titulo = "✓ ¡Plan Renovado con Éxito!",
                            mensaje = "Tu pago ha sido registrado por BRIXO. Servicio activo hasta el $fFinHumana.",
                            fechaReferencia = fFinHumana,
                            esPersistente = false,
                            autoHideSegundos = 6,
                            esSoloAdminODueno = false
                        )
                    )
                }
            }

            // 4. FIN DE PRUEBA INICIAL (Todos los usuarios)
            if (diasPrueba > 0 && finPruebaMs > 0L && ahoraMs >= finPruebaMs) {
                val fPruebaIdStr = sdfId.format(Date(finPruebaMs))
                candidatas.add(
                    AlertaSuscripcionItem(
                        id = "FIN_PRUEBA_$fPruebaIdStr",
                        tipo = TipoAlertaSuscripcion.FIN_PRUEBA,
                        titulo = "🎉 Días de Prueba Concluidos",
                        mensaje = "Tus $diasPrueba días de bienvenida concluyeron con éxito. Tu contrato mensual está activo hasta el $fFinHumana.",
                        fechaReferencia = fFinHumana,
                        esPersistente = false,
                        autoHideSegundos = 8,
                        esSoloAdminODueno = false
                    )
                )
            }

            // 5. AVISO PREVENTIVO 7 DÍAS (Solo Admin/Dueño)
            if (diasRestantesTotal in 3..7 && estado != "vencida" && fFinIdStr.isNotBlank()) {
                candidatas.add(
                    AlertaSuscripcionItem(
                        id = "AVISO_7D_$fFinIdStr",
                        tipo = TipoAlertaSuscripcion.AVISO_PREVENTIVO_7D,
                        titulo = "Próximo Vencimiento en $diasRestantesTotal Días",
                        mensaje = "Tu plan vence el $fFinHumana. Recuerda coordinar tu renovación con anticipación.",
                        fechaReferencia = fFinHumana,
                        esPersistente = false,
                        autoHideSegundos = 8,
                        esSoloAdminODueno = true
                    )
                )
            }

            // Filtrar por ID no leído y permisos de rol
            val alertaSeleccionada = candidatas.firstOrNull { candidata ->
                candidata.id !in leidasIds && (!candidata.esSoloAdminODueno || esAdminODueno)
            }

            trySend(alertaSeleccionada)
        }

        // Listener 1: Acuses de lectura del usuario (Colección aislada R6)
        val leidasRef = FarmadonPaths.farmacia(db, clienteId)
            .collection("notificaciones_leidas").document(usuarioUid)

        val regLeidas: ListenerRegistration = leidasRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando notificaciones_leidas: ${err.message}")
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val lista = (snap?.get("leidas") as? List<String>) ?: emptyList()
            leidasIds = lista.toSet()
            evaluarYEmitir()
        }

        // Listener 2: Suscripción corporativa en tiempo real (Regla R8)
        val subQuery = FarmadonPaths.suscripciones(db, clienteId).limit(1)

        val regSuscripcion: ListenerRegistration = subQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando suscripciones: ${err.message}")
                return@addSnapshotListener
            }
            ultimoSnapshotSuscripcion = snap?.documents?.firstOrNull()
            evaluarYEmitir()
        }

        // Listener 3: Solicitudes de pago observadas en tiempo real (Toma y Dame)
        val pagosQuery = db.collection("compartido").document("ecosistema").collection("solicitudes_pago")
            .whereEqualTo("clienteId", clienteId)

        val regPagos: ListenerRegistration = pagosQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando solicitudes_pago para alertas: ${err.message}")
                return@addSnapshotListener
            }
            val docs = snap?.documents ?: emptyList()
            ultimoSnapshotPagoObservado = docs.maxByOrNull { doc ->
                parsearFecha(doc.get("updatedAt") ?: doc.get("createdAt") ?: doc.get("subsanadaAt"))?.time ?: 0L
            }
            evaluarYEmitir()
        }

        awaitClose {
            regLeidas.remove()
            regSuscripcion.remove()
            regPagos.remove()
        }

    }

    fun marcarAlertaLeidaSilenciosa(
        clienteId: String,
        usuarioUid: String,
        alertaId: String
    ) {
        if (clienteId.isBlank() || usuarioUid.isBlank() || alertaId.isBlank()) return
        if (clienteId != SessionManager.clienteIdGarantizado) return

        val ref = FarmadonPaths.farmacia(db, clienteId)
            .collection("notificaciones_leidas").document(usuarioUid)

        ref.get().addOnSuccessListener { snap ->
            @Suppress("UNCHECKED_CAST")
            val actuales = (snap.get("leidas") as? List<String>) ?: emptyList()
            if (alertaId in actuales) return@addOnSuccessListener

            val nuevaLista = (actuales + alertaId).takeLast(MAX_HISTORIAL_LEIDAS)

            ref.set(
                mapOf(
                    "usuarioUid" to usuarioUid,
                    "leidas" to nuevaLista,
                    "ultimaLectura" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { e ->
                Log.w(TAG, "Fallo silencioso guardando acuse de lectura: ${e.message}")
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Fallo obteniendo notificaciones_leidas: ${e.message}")
        }
    }

    private fun parsearFecha(raw: Any?): Date? = when (raw) {
        is Timestamp -> raw.toDate()
        is Date -> raw
        is Number -> {
            val num = raw.toLong()
            if (num > 100_000_000_000L) Date(num)
            else if (num > 0L) Date(num * 1000L)
            else null
        }
        is String -> {
            if (raw.isBlank()) null
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
                            timeZone = TIMEZONE_LIMA
                        }
                        resultado = sdf.parse(raw)
                        if (resultado != null) break
                    } catch (_: Exception) {}
                }
                resultado
            }
        }
        else -> null
    }
}

