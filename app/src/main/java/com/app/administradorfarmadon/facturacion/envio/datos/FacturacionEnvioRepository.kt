package com.app.administradorfarmadon.facturacion.envio.datos

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
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
 * Resultado estructurado del envío fiscal (F4-B).
 */
sealed class ResultadoEnvioFiscal {
    data class Aceptado(val docId: String, val cdrUrl: String, val pdfUrl: String) : ResultadoEnvioFiscal()
    data class Rechazado(val docId: String, val motivo: String, val numeroQuemado: Boolean = true) : ResultadoEnvioFiscal()
    data class Excepcion(val docId: String, val motivo: String) : ResultadoEnvioFiscal()
    data class Enviado(val docId: String, val docIdProveedor: String) : ResultadoEnvioFiscal()
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
 * R12/Auditoría: exitosos contiene ÚNICAMENTE comprobantes ACEPTADOS con constancia.
 */
data class ReporteEnvioLote(
    val totalProcesados: Int = 0,
    val exitosos: List<ItemResultadoLote> = emptyList(),
    val enTramite: List<ItemResultadoLote> = emptyList(),
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
     * Procesa el envío o consulta de un documento fiscal a APISUNAT/SUNAT
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

        // Pre-guards de serie, correlativo y total antes de quemar o consultar
        val serie = data["serie"] as? String ?: ""
        val correlativo = (data["correlativo"] as? Number)?.toLong() ?: 0L
        val total = (data["total"] as? Number)?.toDouble() ?: 0.0

        if (serie.isBlank()) {
            return@withContext ResultadoEnvioFiscal.Invalido(documentoId, "Serie del comprobante no configurada o vacía")
        }
        if (correlativo <= 0L) {
            return@withContext ResultadoEnvioFiscal.Invalido(documentoId, "Correlativo del comprobante ($correlativo) inválido (debe ser > 0)")
        }
        if (total <= 0.0) {
            return@withContext ResultadoEnvioFiscal.Invalido(documentoId, "Total del comprobante (S/ $total) inválido (debe ser > 0)")
        }

        val docModelo = FacturacionDocumento(
            id = documentoId,
            tipo = data["tipo"] as? String ?: "BOLETA",
            serie = serie,
            correlativo = correlativo,
            numeroCompleto = data["numeroCompleto"] as? String ?: "",
            clienteTipoDoc = data["clienteTipoDoc"] as? String ?: "NINGUNO",
            clienteNumeroDoc = data["clienteNumeroDoc"] as? String ?: "",
            clienteNombre = data["clienteNombre"] as? String ?: "Consumidor Final",
            ventaId = data["ventaId"] as? String ?: "",
            devolucionId = data["devolucionId"] as? String ?: "",
            sucursalId = data["sucursalId"] as? String ?: "",
            total = total,
            estadoEnvio = estadoActual,
            fechaMs = (data["fechaMs"] as? Number)?.toLong() ?: 0L,
            motivo = data["motivo"] as? String ?: "",
            moduloOrigen = data["moduloOrigen"] as? String ?: "POS"
        )

        // Si ya está ENVIADO con docIdProveedor, no se envía de nuevo: se consulta hasta obtener estado final
        val docIdProvGuardado = (data["documentIdProveedor"] as? String).orEmpty().trim()
        if (estadoActual == FacturacionDocumento.ESTADO_ENVIADO) {
            if (docIdProvGuardado.isBlank()) {
                return@withContext ResultadoEnvioFiscal.Invalido(
                    documentoId,
                    "Comprobante en estado ENVIADO sin identificador de proveedor para consultar."
                )
            }
            val consulta = apisunatClient.consultarDocumento(docIdProvGuardado, emisor.personaId, emisor.personaToken)
            val duracionMs = System.currentTimeMillis() - inicioMs
            return@withContext procesarResultadoEnvio(farmaciaId, docModelo, consulta, emisor, duracionMs, documentoId)
        }

        // 3. Ramificación según tipo de documento fiscal:
        val esBaja = docModelo.tipo.equals("COMUNICACION_BAJA", ignoreCase = true) ||
                docModelo.tipo.equals("BAJA", ignoreCase = true)

        if (esBaja) {
            return@withContext procesarComunicacionBaja(farmaciaId, emisor, docModelo, inicioMs)
        }

        // Obtener venta vinculada si existe
        val ventaVinculada = if (docModelo.ventaId.isNotBlank()) {
            docsRepo.obtenerVentaVinculada(farmaciaId, docModelo.sucursalId, docModelo.ventaId)
        } else null

        // Para Nota de Crédito, obtener la devolución vinculada para desglosar sus ítems reales
        var devolucionVinculada = if (docModelo.devolucionId.isNotBlank()) {
            docsRepo.obtenerDevolucionVinculada(farmaciaId, docModelo.sucursalId, docModelo.devolucionId)
        } else null

        // Anulación total de FACTURA (doc anul_* sin devolución): construir NC sintética desde la venta completa
        // para no dejar un comprobante imposible de enviar a SUNAT (pérdida fiscal).
        if (devolucionVinculada == null && docModelo.tipo.equals("NOTA_CREDITO", ignoreCase = true) && ventaVinculada != null) {
            val v = ventaVinculada
            val factorDesc = if (v.subtotal > 0.0) (v.total / v.subtotal).coerceIn(0.0, 1.0) else 1.0
            fun red2(x: Double) = kotlin.math.round(x * 100.0) / 100.0
            val itemsSint = v.items.map { it ->
                val precioNeto = red2(it.precioUnitario * factorDesc)
                val montoLinea = red2(precioNeto * it.cantidad)
                val costoU = if (it.cantidad > 0) red2(it.costoTotalReal / it.cantidad.toDouble()) else 0.0
                com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion(
                    productoId = it.productoId,
                    nombreProducto = it.nombreProducto,
                    presentacionNombre = it.presentacionNombre,
                    cantidad = it.cantidad,
                    precioUnitario = precioNeto,
                    monto = montoLinea,
                    costoUnitarioReal = costoU,
                    montoCosto = red2(it.costoTotalReal)
                )
            }
            devolucionVinculada = com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta(
                id = docModelo.devolucionId.ifBlank { "sint_${docModelo.id}" },
                ventaId = v.id,
                numeroVenta = v.numeroCompleto,
                serie = docModelo.serie,
                correlativo = docModelo.correlativo,
                numeroCompleto = docModelo.numeroCompleto,
                items = itemsSint,
                montoReembolso = docModelo.total,
                metodoReembolso = "EFECTIVO",
                motivo = docModelo.motivo.ifBlank { "ANULACIÓN TOTAL DE ${v.numeroCompleto}" },
                cajaSesionId = v.cajaSesionId,
                fechaMs = docModelo.fechaMs
            )
        }

        // 4. Construir payload oficial APISUNAT (sendBill)
        val payload = try {
            FacturacionPayloadBuilder.construirSendBillPayload(
                emisor = emisor,
                doc = docModelo,
                venta = ventaVinculada,
                devolucion = devolucionVinculada
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Payload inválido para documento $documentoId: ${e.message}")
            return@withContext ResultadoEnvioFiscal.Invalido(documentoId, e.message ?: "Datos inválidos para comprobante")
        }

        // 5. Emitir llamada HTTP a APISUNAT
        val resultadoHttp = apisunatClient.emitirDocumento(payload)
        val duracionMs = System.currentTimeMillis() - inicioMs

        return@withContext procesarResultadoEnvio(farmaciaId, docModelo, resultadoHttp, emisor, duracionMs, documentoId)
    }

    private suspend fun procesarResultadoEnvio(
        farmaciaId: String,
        docModelo: FacturacionDocumento,
        resultadoHttp: ApisunatResultado,
        emisor: EmisorFiscal,
        duracionMs: Long,
        documentoId: String
    ): ResultadoEnvioFiscal {
        return when (resultadoHttp) {
            is ApisunatResultado.Exito -> {
                var estadoFinal = resultadoHttp.estadoSunat
                var docIdProv = resultadoHttp.documentId
                var xmlUrl = resultadoHttp.xmlUrl
                var cdrUrl = resultadoHttp.cdrUrl
                var pdfUrl = resultadoHttp.pdfUrl
                var mensaje = resultadoHttp.mensajeRespuesta

                // Si quedó en ENVIADO con ID, realizamos hasta 2 consultas breves de estado (polling)
                if (estadoFinal == "ENVIADO" && docIdProv.isNotBlank()) {
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

                return when (estadoFinal) {
                    "ACEPTADO" -> {
                        // ACEPTADO exige docId y constancia física (CDR o XML firmado). Prohibido éxito sin constancia.
                        if (docIdProv.isBlank() || (cdrUrl.isBlank() && xmlUrl.isBlank())) {
                            val msgError = "Respuesta ACEPTADO sin constancia física de SUNAT (falta CDR/XML). Queda en verificación."
                            actualizarDocumentoYVenta(
                                farmaciaId = farmaciaId,
                                sucursalId = docModelo.sucursalId,
                                docId = documentoId,
                                ventaId = docModelo.ventaId,
                                tipoDocumento = docModelo.tipo,
                                nuevoEstado = FacturacionDocumento.ESTADO_PENDIENTE,
                                numeroQuemado = false,
                                documentIdProv = docIdProv,
                                ultimoError = msgError,
                                responseTimeMs = duracionMs
                            )
                            ResultadoEnvioFiscal.Excepcion(documentoId, msgError)
                        } else {
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
                        // EXCEPCION previa a validación SUNAT: NO quema número, vuelve a PENDIENTE para corrección
                        val motivoExc = mensaje.ifBlank { "Excepción técnica previa a validación SUNAT" }
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
                        // ENVIADO / EN_TRAMITE con ID: esperando respuesta definitiva
                        if (docIdProv.isBlank()) {
                            val msgError = "Respuesta ENVIADO sin identificador de proveedor para seguimiento."
                            actualizarDocumentoYVenta(
                                farmaciaId = farmaciaId,
                                sucursalId = docModelo.sucursalId,
                                docId = documentoId,
                                ventaId = docModelo.ventaId,
                                tipoDocumento = docModelo.tipo,
                                nuevoEstado = FacturacionDocumento.ESTADO_PENDIENTE,
                                numeroQuemado = false,
                                ultimoError = msgError,
                                responseTimeMs = duracionMs
                            )
                            ResultadoEnvioFiscal.Excepcion(documentoId, msgError)
                        } else {
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
                                motivo = mensaje.ifBlank { "En trámite ante SUNAT (esperando CDR)" },
                                ultimoError = "",
                                responseTimeMs = duracionMs
                            )
                            ResultadoEnvioFiscal.Enviado(documentoId, docIdProv)
                        }
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
                    // Error 400 de formato, validación previa o pasarela: número LIBRE para corregir y reintentar
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
        emisor: EmisorFiscal,
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
        // (no tiene documentIdProveedor o no está ACEPTADO), no se puede hacer voidBill.
        // Se marca como ANULADO localmente (jamás ACEPTADO sin papel SUNAT).
        if (docIdProveedorOriginal.isBlank() || estadoEnvioOriginal != FacturacionDocumento.ESTADO_ACEPTADO) {
            val batch = db.batch()
            val bajaRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(docModelo.id)
            val originalRef = FarmadonPaths.facturacionDocumentos(db, farmaciaId).document(originalId)

            val motivoBajaLocal = "Anulación local: comprobante original no transmitido previamente a SUNAT."
            batch.set(
                bajaRef,
                mapOf(
                    "estadoEnvio" to FacturacionDocumento.ESTADO_ANULADO,
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

                when (resultadoHttp.estadoSunat) {
                    "ACEPTADO" -> {
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
                    "RECHAZADO" -> {
                        batch.set(
                            bajaRef,
                            mapOf(
                                "estadoEnvio" to FacturacionDocumento.ESTADO_RECHAZADO,
                                "numeroQuemado" to true,
                                "documentIdProveedor" to resultadoHttp.documentId,
                                "motivo" to resultadoHttp.mensajeRespuesta.ifBlank { "Comunicación de baja rechazada por SUNAT" },
                                "ultimoError" to resultadoHttp.mensajeRespuesta,
                                "responseTimeMs" to duracionMs,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        batch.commit().await()
                        ResultadoEnvioFiscal.Rechazado(docModelo.id, resultadoHttp.mensajeRespuesta, numeroQuemado = true)
                    }
                    "EXCEPCION" -> {
                        batch.set(
                            bajaRef,
                            mapOf(
                                "estadoEnvio" to FacturacionDocumento.ESTADO_PENDIENTE,
                                "numeroQuemado" to false,
                                "documentIdProveedor" to resultadoHttp.documentId,
                                "ultimoError" to "Excepción técnica en baja: ${resultadoHttp.mensajeRespuesta}",
                                "responseTimeMs" to duracionMs,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        batch.commit().await()
                        ResultadoEnvioFiscal.Excepcion(docModelo.id, resultadoHttp.mensajeRespuesta)
                    }
                    else -> {
                        // ENVIADO: La baja quedó en trámite ante SUNAT
                        batch.set(
                            bajaRef,
                            mapOf(
                                "estadoEnvio" to FacturacionDocumento.ESTADO_ENVIADO,
                                "documentIdProveedor" to resultadoHttp.documentId,
                                "motivo" to resultadoHttp.mensajeRespuesta.ifBlank { "Comunicación de baja en trámite ante SUNAT" },
                                "responseTimeMs" to duracionMs,
                                "actualizadoEl" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        batch.commit().await()
                        ResultadoEnvioFiscal.Enviado(docModelo.id, resultadoHttp.documentId)
                    }
                }
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
                        "ultimoError" to "Falla de red en baja: ${resultadoHttp.mensaje}. Queda en cola.",
                        "responseTimeMs" to duracionMs,
                        "actualizadoEl" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                ResultadoEnvioFiscal.FallaRed(docModelo.id, resultadoHttp.mensaje)
            }
        }
    }

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
            "responseTimeMs" to responseTimeMs,
            "actualizadoEl" to FieldValue.serverTimestamp(),
            "reintentos" to FieldValue.increment(1)
        )

        if (documentIdProv.isNotBlank()) updatesDoc["documentIdProveedor"] = documentIdProv
        if (xmlUrl.isNotBlank()) updatesDoc["xmlUrl"] = xmlUrl
        if (cdrUrl.isNotBlank()) updatesDoc["cdrUrl"] = cdrUrl
        if (pdfUrl.isNotBlank()) updatesDoc["pdfUrl"] = pdfUrl
        if (motivo.isNotBlank()) updatesDoc["motivo"] = motivo
        updatesDoc["ultimoError"] = ultimoError

        // Registrar entrada en el historial de intentos
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

        // Sincronizar estadoFiscal en la venta vinculada ÚNICAMENTE para BOLETA o FACTURA
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
            docRef.set(updatesDoc, SetOptions.merge()).await()
        }
    }

    /**
     * Envía todos los comprobantes pendientes o en trámite de forma secuencial.
     * R12/Auditoría: exitosos contiene SOLO comprobantes ACEPTADOS con constancia.
     * Comprobantes ENVIADO van a enTramite; rechazados van a requierenAtencion.
     */
    suspend fun enviarLotePendientes(farmaciaId: String): ReporteEnvioLote = withContext(Dispatchers.IO) {
        val snapPendientes = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_PENDIENTE)
            .get()
            .await()

        val snapEnviados = FarmadonPaths.facturacionDocumentos(db, farmaciaId)
            .whereEqualTo("estadoEnvio", FacturacionDocumento.ESTADO_ENVIADO)
            .get()
            .await()

        val todosLosDocs = (snapPendientes.documents + snapEnviados.documents).distinctBy { it.id }

        val listaExitosos = mutableListOf<ItemResultadoLote>()
        val listaEnTramite = mutableListOf<ItemResultadoLote>()
        val listaRequierenAtencion = mutableListOf<ItemResultadoLote>()

        for (doc in todosLosDocs) {
            val quemado = doc.getBoolean("numeroQuemado") == true
            val numero = doc.getString("numeroCompleto") ?: doc.id
            if (quemado) {
                listaRequierenAtencion.add(
                    ItemResultadoLote(
                        docId = doc.id,
                        numeroCompleto = numero,
                        exito = false,
                        estado = "RECHAZADO",
                        motivo = "Número quemado por SUNAT previamente. Requiere emitir nuevo comprobante."
                    )
                )
                continue
            }

            val res = enviarDocumento(farmaciaId, doc.id)
            when (res) {
                is ResultadoEnvioFiscal.Aceptado -> {
                    listaExitosos.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = true,
                            estado = "ACEPTADO",
                            motivo = "Aceptado oficialmente por SUNAT con CDR firmado"
                        )
                    )
                }
                is ResultadoEnvioFiscal.Enviado -> {
                    listaEnTramite.add(
                        ItemResultadoLote(
                            docId = doc.id,
                            numeroCompleto = numero,
                            exito = false,
                            estado = "ENVIADO",
                            motivo = "En trámite ante SUNAT (ID: ${res.docIdProveedor}). Esperando CDR final."
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
                            motivo = "Error de conexión: ${res.error}. Quedó en cola de contingencia."
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
            }
        }

        ReporteEnvioLote(
            totalProcesados = todosLosDocs.size,
            exitosos = listaExitosos,
            enTramite = listaEnTramite,
            requierenAtencion = listaRequierenAtencion
        )
    }
}
