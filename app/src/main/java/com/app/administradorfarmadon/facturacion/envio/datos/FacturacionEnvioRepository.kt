package com.app.administradorfarmadon.facturacion.envio.datos

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.facturacion.configuracion.datos.FacturacionConfigRepository
import com.app.administradorfarmadon.facturacion.documentos.datos.FacturacionDocumentosRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Resultado estructurado del procesamiento de un envío fiscal.
 */
sealed class ResultadoEnvioFiscal {
    data class Aceptado(val docId: String, val cdrUrl: String, val pdfUrl: String) : ResultadoEnvioFiscal()
    data class Rechazado(val docId: String, val motivo: String, val numeroQuemado: Boolean = true) : ResultadoEnvioFiscal()
    data class Excepcion(val docId: String, val motivo: String) : ResultadoEnvioFiscal()
    data class Enviado(val docId: String, val documentIdProveedor: String) : ResultadoEnvioFiscal()
    data class FallaRed(val docId: String, val error: String) : ResultadoEnvioFiscal()
    data class Invalido(val docId: String, val motivo: String) : ResultadoEnvioFiscal()
}

/**
 * Detalle individual de un comprobante procesado en un envío por lote (F5-C).
 */
data class ItemResultadoLote(
    val docId: String,
    val numeroCompleto: String,
    val exito: Boolean,
    val estado: String,
    val motivo: String
)

/**
 * Reporte consolidado de un envío por lote con lista individual de comprobantes (F5-C).
 */
data class ReporteEnvioLote(
    val totalProcesados: Int = 0,
    val exitosos: List<ItemResultadoLote> = emptyList(),
    val requierenAtencion: List<ItemResultadoLote> = emptyList()
)

class FacturacionEnvioRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val apisunatClient: ApisunatClient = ApisunatClient(),
    private val configRepo: FacturacionConfigRepository = FacturacionConfigRepository(db),
    private val docsRepo: FacturacionDocumentosRepository = FacturacionDocumentosRepository(db)
) {
    companion object {
        private const val TAG = "FacturacionEnvioRepo"
    }

    /**
     * Procesa el envío de un documento fiscal pendiente a APISUNAT/SUNAT
     * ejecutando la máquina de estados estricta sin pérdida ni invención.
     */
    suspend fun enviarDocumento(farmaciaId: String, documentoId: String): ResultadoEnvioFiscal = withContext(Dispatchers.IO) {
        val inicioMs = System.currentTimeMillis()

        // 1. Obtener emisor fiscal de la farmacia
        val emisor = configRepo.obtenerEmisor(farmaciaId)
        if (emisor == null || !emisor.estaCompleta) {
            return@withContext ResultadoEnvioFiscal.Invalido(
                documentoId,
                "El emisor fiscal no está configurado o verificado. Configúralo en Sede Principal."
            )
        }

        // 2. Leer documento de Firestore
        val docRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(documentoId)
        val docSnap = docRef.get().await()
        if (!docSnap.exists()) {
            return@withContext ResultadoEnvioFiscal.Invalido(documentoId, "El documento $documentoId no existe en la bandeja.")
        }

        val data = docSnap.data ?: emptyMap()
        val estadoActual = data["estadoEnvio"] as? String ?: FacturacionDocumento.ESTADO_PENDIENTE
        val numeroQuemado = data["numeroQuemado"] as? Boolean ?: false

        // Regla madre SUNAT: correlativo quemado jamás se reintenta
        if (numeroQuemado) {
            val motivoRechazo = data["motivo"] as? String ?: "Correlativo quemado por rechazo SUNAT"
            return@withContext ResultadoEnvioFiscal.Rechazado(
                documentoId,
                "SUNAT quemó esta serie y número. No se puede reintentar: $motivoRechazo",
                numeroQuemado = true
            )
        }

        if (estadoActual == FacturacionDocumento.ESTADO_ACEPTADO) {
            return@withContext ResultadoEnvioFiscal.Aceptado(
                documentoId,
                data["cdrUrl"] as? String ?: "",
                data["pdfUrl"] as? String ?: ""
            )
        }

        val docModelo = FacturacionDocumento(
            id = documentoId,
            tipo = data["tipo"] as? String ?: "BOLETA",
            serie = data["serie"] as? String ?: "",
            correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L,
            numeroCompleto = data["numeroCompleto"] as? String ?: "",
            clienteTipoDoc = data["clienteTipoDoc"] as? String ?: "NINGUNO",
            clienteNumeroDoc = data["clienteNumeroDoc"] as? String ?: "",
            clienteNombre = data["clienteNombre"] as? String ?: "Consumidor Final",
            ventaId = data["ventaId"] as? String ?: "",
            devolucionId = data["devolucionId"] as? String ?: "",
            sucursalId = data["sucursalId"] as? String ?: "",
            total = (data["total"] as? Number)?.toDouble() ?: 0.0,
            estadoEnvio = estadoActual,
            fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
            motivo = data["motivo"] as? String ?: "",
            moduloOrigen = data["moduloOrigen"] as? String ?: "POS"
        )

        // 3. Ramificación según tipo de documento fiscal:
        // FIX C-F2: COMUNICACION_BAJA no es un comprobante de venta, sino una anulación vía voidBill
        val esBaja = docModelo.tipo.equals("COMUNICACION_BAJA", ignoreCase = true) ||
                docModelo.tipo.equals("BAJA", ignoreCase = true)

        if (esBaja) {
            return@withContext procesarComunicacionBaja(farmaciaId, emisor, docModelo, inicioMs)
        }

        // Obtener venta vinculada si existe
        val ventaVinculada = if (docModelo.ventaId.isNotBlank()) {
            docsRepo.obtenerVentaVinculada(farmaciaId, docModelo.sucursalId, docModelo.ventaId)
        } else null

        // FIX C-F1: Para Nota de Crédito, obtener la devolución vinculada para desglosar sus ítems reales
        val devolucionVinculada = if (docModelo.devolucionId.isNotBlank()) {
            docsRepo.obtenerDevolucionVinculada(farmaciaId, docModelo.sucursalId, docModelo.devolucionId)
        } else null

        // 4. Construir payload oficial APISUNAT (sendBill)
        val payload = FacturacionPayloadBuilder.construirSendBillPayload(
            emisor = emisor,
            doc = docModelo,
            venta = ventaVinculada,
            devolucion = devolucionVinculada
        )

        // 5. Emitir llamada HTTP a APISUNAT
        val resultadoHttp = apisunatClient.emitirDocumento(payload)
        val duracionMs = System.currentTimeMillis() - inicioMs

        // 6. Procesar máquina de estados SUNAT
        when (resultadoHttp) {
            is ApisunatResultado.Exito -> {
                var estadoFinal = resultadoHttp.estadoSunat
                var docIdProv = resultadoHttp.documentId
                var xmlUrl = resultadoHttp.xmlUrl
                var cdrUrl = resultadoHttp.cdrUrl
                var pdfUrl = resultadoHttp.pdfUrl
                var mensaje = resultadoHttp.mensajeRespuesta

                // Si quedó en ENVIADO o EN_PROCESO, hacemos hasta 2 consultas rápidas de estado (polling)
                if ((estadoFinal == "ENVIADO" || estadoFinal == "EN_PROCESO") && docIdProv.isNotBlank()) {
                    repeat(2) {
                        delay(1200)
                        val consulta = apisunatClient.consultarDocumento(docIdProv, emisor.personaId, emisor.personaToken)
                        if (consulta is ApisunatResultado.Exito) {
                            estadoFinal = consulta.estadoSunat
                            if (consulta.xmlUrl.isNotBlank()) xmlUrl = consulta.xmlUrl
                            if (consulta.cdrUrl.isNotBlank()) cdrUrl = consulta.cdrUrl
                            if (consulta.pdfUrl.isNotBlank()) pdfUrl = consulta.pdfUrl
                            if (consulta.mensajeRespuesta.isNotBlank()) mensaje = consulta.mensajeRespuesta
                        }
                    }
                }

                when (estadoFinal) {
                    "ACEPTADO" -> {
                        actualizarDocumentoYVenta(
                            farmaciaId = farmaciaId,
                            sucursalId = docModelo.sucursalId,
                            docId = documentoId,
                            ventaId = docModelo.ventaId,
                            tipoDocumento = docModelo.tipo,
                            nuevoEstado = FacturacionDocumento.ESTADO_ACEPTADO,
                            numeroQuemado = false,
                            documentIdProv = docIdProv,
                            xmlUrl = xmlUrl,
                            cdrUrl = cdrUrl,
                            pdfUrl = pdfUrl,
                            motivo = mensaje.ifBlank { "Comprobante aceptado por SUNAT" },
                            ultimoError = "",
                            responseTimeMs = duracionMs
                        )
                        ResultadoEnvioFiscal.Aceptado(documentoId, cdrUrl, pdfUrl)
                    }

                    "RECHAZADO" -> {
                        // RECHAZADO: SUNAT quemó la numeración
                        val motivoRechazo = mensaje.ifBlank { "Rechazado por SUNAT" }
                        actualizarDocumentoYVenta(
                            farmaciaId = farmaciaId,
                            sucursalId = docModelo.sucursalId,
                            docId = documentoId,
                            ventaId = docModelo.ventaId,
                            tipoDocumento = docModelo.tipo,
                            nuevoEstado = FacturacionDocumento.ESTADO_RECHAZADO,
                            numeroQuemado = true,
                            documentIdProv = docIdProv,
                            xmlUrl = xmlUrl,
                            cdrUrl = cdrUrl,
                            pdfUrl = pdfUrl,
                            motivo = "SUNAT RECHAZÓ: $motivoRechazo",
                            ultimoError = motivoRechazo,
                            responseTimeMs = duracionMs
                        )
                        ResultadoEnvioFiscal.Rechazado(documentoId, motivoRechazo, numeroQuemado = true)
                    }

                    "EXCEPCION" -> {
                        // EXCEPCION: NO quema número, vuelve a PENDIENTE
                        val motivoExc = mensaje.ifBlank { "Excepción técnica en servidor SUNAT" }
                        actualizarDocumentoYVenta(
                            farmaciaId = farmaciaId,
                            sucursalId = docModelo.sucursalId,
                            docId = documentoId,
                            ventaId = docModelo.ventaId,
                            tipoDocumento = docModelo.tipo,
                            nuevoEstado = FacturacionDocumento.ESTADO_PENDIENTE,
                            numeroQuemado = false,
                            documentIdProv = docIdProv,
                            xmlUrl = xmlUrl,
                            cdrUrl = cdrUrl,
                            pdfUrl = pdfUrl,
                            motivo = "",
                            ultimoError = "SUNAT EXCEPCIÓN: $motivoExc",
                            responseTimeMs = duracionMs
                        )
                        ResultadoEnvioFiscal.Excepcion(documentoId, motivoExc)
                    }

                    else -> {
                        // Permanece en ENVIADO
                        actualizarDocumentoYVenta(
                            farmaciaId = farmaciaId,
                            sucursalId = docModelo.sucursalId,
                            docId = documentoId,
                            ventaId = docModelo.ventaId,
                            tipoDocumento = docModelo.tipo,
                            nuevoEstado = FacturacionDocumento.ESTADO_ENVIADO,
                            numeroQuemado = false,
                            documentIdProv = docIdProv,
                            xmlUrl = xmlUrl,
                            cdrUrl = cdrUrl,
                            pdfUrl = pdfUrl,
                            motivo = mensaje,
                            ultimoError = "",
                            responseTimeMs = duracionMs
                        )
                        ResultadoEnvioFiscal.Enviado(documentoId, docIdProv)
                    }
                }
            }

            is ApisunatResultado.ErrorProveedor -> {
                if (resultadoHttp.esRechazoSunat) {
                    actualizarDocumentoYVenta(
                        farmaciaId = farmaciaId,
                        sucursalId = docModelo.sucursalId,
                        docId = documentoId,
                        ventaId = docModelo.ventaId,
                        tipoDocumento = docModelo.tipo,
                        nuevoEstado = FacturacionDocumento.ESTADO_RECHAZADO,
                        numeroQuemado = true,
                        motivo = "SUNAT RECHAZÓ: ${resultadoHttp.mensaje}",
                        ultimoError = resultadoHttp.mensaje,
                        responseTimeMs = duracionMs
                    )
                    ResultadoEnvioFiscal.Rechazado(documentoId, resultadoHttp.mensaje, numeroQuemado = true)
                } else {
                    // Error de formato, validación previa o servidor: no quema número
                    actualizarDocumentoYVenta(
                        farmaciaId = farmaciaId,
                        sucursalId = docModelo.sucursalId,
                        docId = documentoId,
                        ventaId = docModelo.ventaId,
                        tipoDocumento = docModelo.tipo,
                        nuevoEstado = FacturacionDocumento.ESTADO_PENDIENTE,
                        numeroQuemado = false,
                        ultimoError = resultadoHttp.mensaje,
                        responseTimeMs = duracionMs
                    )
                    ResultadoEnvioFiscal.Excepcion(documentoId, resultadoHttp.mensaje)
                }
            }

            is ApisunatResultado.FallaRed -> {
                // Sin internet o timeout: queda PENDIENTE con mensaje honesto
                actualizarDocumentoYVenta(
                    farmaciaId = farmaciaId,
                    sucursalId = docModelo.sucursalId,
                    docId = documentoId,
                    ventaId = docModelo.ventaId,
                    tipoDocumento = docModelo.tipo,
                    nuevoEstado = FacturacionDocumento.ESTADO_PENDIENTE,
                    numeroQuemado = false,
                    ultimoError = "Falla de red: ${resultadoHttp.mensaje}. Queda en cola de contingencia.",
                    responseTimeMs = duracionMs
                )
                ResultadoEnvioFiscal.FallaRed(documentoId, resultadoHttp.mensaje)
            }
        }
    }

    /**
     * Procesa la anulación fiscal de un comprobante vía voidBill o anulación local (Fix C-F2).
     */
    private suspend fun procesarComunicacionBaja(
        farmaciaId: String,
        emisor: com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal,
        docModelo: FacturacionDocumento,
        inicioMs: Long
    ): ResultadoEnvioFiscal {
        // 1. Buscar el comprobante original (BOLETA o FACTURA) emitido para esta venta
        val snapOriginal = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .whereEqualTo("ventaId", docModelo.ventaId)
            .get()
            .await()

        val docOriginalSnap = snapOriginal.documents.firstOrNull { doc ->
            val tipo = (doc.getString("tipo") ?: "").uppercase()
            tipo == "BOLETA" || tipo == "FACTURA"
        }

        if (docOriginalSnap == null) {
            return ResultadoEnvioFiscal.Invalido(
                docModelo.id,
                "No se encontró comprobante original (BOLETA/FACTURA) vinculado a la venta ${docModelo.ventaId}"
            )
        }

        val originalId = docOriginalSnap.id
        val docIdProveedorOriginal = docOriginalSnap.getString("documentIdProveedor").orEmpty()
        val estadoEnvioOriginal = docOriginalSnap.getString("estadoEnvio").orEmpty()

        // 2. CASO A: Si el documento original nunca fue transmitido a APISUNAT/SUNAT
        // (no tiene documentIdProveedor o no está ACEPTADO), no se puede hacer voidBill
        // porque jamás existió en los servidores del proveedor.
        if (docIdProveedorOriginal.isBlank() || estadoEnvioOriginal != FacturacionDocumento.ESTADO_ACEPTADO) {
            val batch = db.batch()
            val bajaRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docModelo.id)
            val originalRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(originalId)

            val motivoBajaLocal = "Anulación local: el comprobante original no requirió baja externa ante SUNAT al no haber sido transmitido."
            batch.set(
                bajaRef,
                mapOf(
                    "estadoEnvio" to FacturacionDocumento.ESTADO_ACEPTADO,
                    "numeroQuemado" to false,
                    "motivo" to motivoBajaLocal,
                    "actualizadoEl" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.set(
                originalRef,
                mapOf(
                    "estadoEnvio" to FacturacionDocumento.ESTADO_ANULADO,
                    "motivo" to "Anulado localmente antes de transmisión SUNAT",
                    "actualizadoEl" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.commit().await()
            return ResultadoEnvioFiscal.Aceptado(docModelo.id, "", "")
        }

        // 3. CASO B: El comprobante original SÍ nació en APISUNAT/SUNAT. Se envía voidBill oficial.
        val payloadVoid = mapOf(
            "personaId" to emisor.personaId.trim(),
            "personaToken" to emisor.personaToken.trim(),
            "documentId" to docIdProveedorOriginal,
            "motivo" to docModelo.motivo.ifBlank { "ANULACIÓN DE LA OPERACIÓN" }
        )

        val resultadoHttp = apisunatClient.anularDocumento(payloadVoid)
        val duracionMs = System.currentTimeMillis() - inicioMs

        return when (resultadoHttp) {
            is ApisunatResultado.Exito -> {
                val batch = db.batch()
                val bajaRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docModelo.id)
                val originalRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(originalId)

                batch.set(
                    bajaRef,
                    mapOf(
                        "estadoEnvio" to FacturacionDocumento.ESTADO_ACEPTADO,
                        "documentIdProveedor" to resultadoHttp.documentId,
                        "motivo" to resultadoHttp.mensajeRespuesta.ifBlank { "Comunicación de baja aceptada por SUNAT" },
                        "xmlUrl" to resultadoHttp.xmlUrl,
                        "cdrUrl" to resultadoHttp.cdrUrl,
                        "pdfUrl" to resultadoHttp.pdfUrl,
                        "responseTimeMs" to duracionMs,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                batch.set(
                    originalRef,
                    mapOf(
                        "estadoEnvio" to FacturacionDocumento.ESTADO_ANULADO,
                        "motivo" to "Anulado oficialmente en SUNAT mediante Comunicación de Baja",
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                batch.commit().await()
                ResultadoEnvioFiscal.Aceptado(docModelo.id, resultadoHttp.cdrUrl, resultadoHttp.pdfUrl)
            }

            is ApisunatResultado.ErrorProveedor -> {
                val bajaRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docModelo.id)
                bajaRef.set(
                    mapOf(
                        "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                        "ultimoError" to "Falla al tramitar baja en APISUNAT: ${resultadoHttp.mensaje}",
                        "responseTimeMs" to duracionMs,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                ResultadoEnvioFiscal.Excepcion(docModelo.id, resultadoHttp.mensaje)
            }

            is ApisunatResultado.FallaRed -> {
                val bajaRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docModelo.id)
                bajaRef.set(
                    mapOf(
                        "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                        "ultimoError" to "Falla de red al tramitar baja: ${resultadoHttp.mensaje}. En cola de contingencia.",
                        "responseTimeMs" to duracionMs,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                ResultadoEnvioFiscal.FallaRed(docModelo.id, resultadoHttp.mensaje)
            }
        }
    }

    /**
     * Sincroniza atómicamente el estado tanto en facturacion_documentos como en la venta original.
     * FIX C-F3: Sincroniza estadoFiscal en la venta SOLO para BOLETA o FACTURA.
     */
    private suspend fun actualizarDocumentoYVenta(
        farmaciaId: String,
        sucursalId: String,
        docId: String,
        ventaId: String,
        tipoDocumento: String,
        nuevoEstado: String,
        numeroQuemado: Boolean,
        documentIdProv: String = "",
        xmlUrl: String = "",
        cdrUrl: String = "",
        pdfUrl: String = "",
        motivo: String = "",
        ultimoError: String = "",
        responseTimeMs: Long = 0L
    ) {
        val updatesDoc = mutableMapOf<String, Any?>(
            "estadoEnvio" to nuevoEstado,
            "numeroQuemado" to numeroQuemado,
            "actualizadoEl" to FieldValue.serverTimestamp(),
            "responseTimeMs" to responseTimeMs,
            "reintentos" to FieldValue.increment(1)
        )

        if (documentIdProv.isNotBlank()) updatesDoc["documentIdProveedor"] = documentIdProv
        if (xmlUrl.isNotBlank()) updatesDoc["xmlUrl"] = xmlUrl
        if (cdrUrl.isNotBlank()) updatesDoc["cdrUrl"] = cdrUrl
        if (pdfUrl.isNotBlank()) updatesDoc["pdfUrl"] = pdfUrl
        if (motivo.isNotBlank()) updatesDoc["motivo"] = motivo
        updatesDoc["ultimoError"] = ultimoError

        // Registrar entrada en el diario/historial de intentos (Regla de auditoría)
        val intentoEntrada = mapOf(
            "fechaMs" to System.currentTimeMillis(),
            "estado" to nuevoEstado,
            "error" to ultimoError,
            "motivo" to motivo,
            "duracionMs" to responseTimeMs
        )
        updatesDoc["historialIntentos"] = FieldValue.arrayUnion(intentoEntrada)

        val docRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docId)
        val batch = db.batch()
        batch.set(docRef, updatesDoc, SetOptions.merge())

        // FIX C-F3: Sincronizar estadoFiscal en la venta vinculada ÚNICAMENTE para BOLETA o FACTURA.
        // Las Notas de Crédito y Bajas viven en su propio documento en facturacion_documentos sin alterar el estado de la venta.
        val esComprobanteDirectoDeVenta = tipoDocumento.equals("BOLETA", ignoreCase = true) ||
                tipoDocumento.equals("FACTURA", ignoreCase = true)

        if (esComprobanteDirectoDeVenta && ventaId.isNotBlank() && sucursalId.isNotBlank()) {
            val ventaRef = FarmadonPaths.ventas(db, farmaciaId, sucursalId).document(ventaId)
            batch.set(ventaRef, mapOf("estadoFiscal" to nuevoEstado), SetOptions.merge())
        }

        try {
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error comiteando batch atómico fiscal doc $docId / venta $ventaId: ${e.message}", e)
            // Fallback si la venta no existiera o tuviera permisos distintos
            docRef.set(updatesDoc, SetOptions.merge()).await()
        }
    }

    /**
     * Envía todos los comprobantes que están en estado PENDIENTE de forma secuencial.
     * Retorna un reporte detallado documento por documento (F5-C).
     */
    suspend fun enviarLotePendientes(farmaciaId: String): ReporteEnvioLote = withContext(Dispatchers.IO) {
        val snap = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_PENDIENTE)
            .get()
            .await()

        val listaExitosos = mutableListOf<ItemResultadoLote>()
        val listaRequierenAtencion = mutableListOf<ItemResultadoLote>()

        for (doc in snap.documents) {
            val quemado = doc.getBoolean("numeroQuemado") == true
            val numero = doc.getString("numeroCompleto") ?: doc.id
            if (quemado) continue // Numeración quemada no se reintenta

            val res = enviarDocumento(farmaciaId, doc.id)
            when (res) {
                is ResultadoEnvioFiscal.Aceptado -> {
                    listaExitosos.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = true,
                            estado = "ACEPTADO",
                            motivo = "Aceptado correctamente por SUNAT"
                        )
                    )
                }
                is ResultadoEnvioFiscal.Rechazado -> {
                    listaRequierenAtencion.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = false,
                            estado = "RECHAZADO",
                            motivo = res.motivo
                        )
                    )
                }
                is ResultadoEnvioFiscal.Excepcion -> {
                    listaRequierenAtencion.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = false,
                            estado = "EXCEPCIÓN",
                            motivo = res.motivo
                        )
                    )
                }
                is ResultadoEnvioFiscal.FallaRed -> {
                    listaRequierenAtencion.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = false,
                            estado = "FALLA_RED",
                            motivo = "Error de conexión: ${res.error}. Quedó en cola para reintento automático."
                        )
                    )
                }
                is ResultadoEnvioFiscal.Invalido -> {
                    listaRequierenAtencion.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = false,
                            estado = "INVÁLIDO",
                            motivo = res.motivo
                        )
                    )
                }
                is ResultadoEnvioFiscal.Enviado -> {
                    listaExitosos.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = true,
                            estado = "ENVIADO",
                            motivo = "Enviado a SUNAT (esperando respuesta)"
                        )
                    )
                }
            }
        }

        ReporteEnvioLote(
            totalProcesados = listaExitosos.size + listaRequierenAtencion.size,
            exitosos = listaExitosos,
            requierenAtencion = listaRequierenAtencion
        )
    }
}
