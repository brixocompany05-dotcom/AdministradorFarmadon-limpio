package com.app.administradorfarmadon.suscripcion.modelo

import com.google.firebase.firestore.DocumentSnapshot

data class SolicitudPago(
    val id: String = "",
    val clienteId: String = "",
    val nombreFarmacia: String = "",
    val ruc: String = "",
    val planId: String = "",
    val planNombre: String = "",
    val montoReportado: Double = 0.0,
    val banco: String = "", // BCP, BBVA, Interbank, Yape, Plin, Scotiabank, Banco de la Nación, Efectivo, Otro
    val numeroOperacion: String = "",
    val comprobanteUrl: String = "",
    val reportadoPorEmail: String = "",
    val reportadoPorNombre: String = "",
    val estado: String = "PENDIENTE", // "PENDIENTE", "APROBADO", "OBSERVADO"
    val motivoObservacion: String = "",
    val atendidoPorEmail: String = "",
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val sincronizadoServidor: Boolean = true
) {
    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_APROBADO = "APROBADO"
        const val ESTADO_OBSERVADO = "OBSERVADO"
        const val ESTADO_SUBSANADA = "SUBSANADA"

        fun fromSnapshot(doc: DocumentSnapshot): SolicitudPago? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            return SolicitudPago(
                id = doc.id,
                clienteId = data["clienteId"] as? String ?: "",
                nombreFarmacia = data["nombreFarmacia"] as? String ?: "",
                ruc = data["ruc"] as? String ?: "",
                planId = data["planId"] as? String ?: "",
                planNombre = data["planNombre"] as? String ?: "",
                montoReportado = (data["montoReportado"] as? Number)?.toDouble() ?: 0.0,
                banco = data["banco"] as? String ?: "BCP",
                numeroOperacion = data["numeroOperacion"] as? String ?: "",
                comprobanteUrl = data["comprobanteUrl"] as? String ?: "",
                reportadoPorEmail = data["reportadoPorEmail"] as? String ?: "",
                reportadoPorNombre = data["reportadoPorNombre"] as? String ?: "",
                estado = (data["estado"] as? String ?: ESTADO_PENDIENTE).uppercase(),
                motivoObservacion = data["motivoObservacion"] as? String ?: "",
                atendidoPorEmail = data["atendidoPorEmail"] as? String ?: "",
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"],
                sincronizadoServidor = !doc.metadata.hasPendingWrites()
            )
        }
    }
}
