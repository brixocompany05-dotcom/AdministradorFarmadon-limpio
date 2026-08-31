package com.app.administradorfarmadon.compras.saldoafavor.datos

import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import java.util.Locale

/**
 * Firebase del saldo a favor: leerlo y aplicarlo SIEMPRE dentro de la misma
 * transacción de la recepción (todo-o-nada, jamás duplicado).
 */
object SaldoAFavorFirestore {

    fun refSaldo(
        db: FirebaseFirestore,
        clienteId: String,
        sucursalId: String,
        proveedorId: String
    ): DocumentReference =
        FarmadonPaths.sucursal(db, clienteId, sucursalId)
            .collection("proveedores")
            .document(proveedorId)

    /** Abono registrado en la factura cuando el saldo a favor se usa como pago. */
    fun crearAbonoSaldoAFavor(
        id: String,
        monto: Double,
        facturaNumero: String,
        motivo: String,
        usuarioNombre: String,
        usuarioEmail: String,
        ahoraMs: Long,
        fechaLegible: String
    ): Map<String, Any> = mapOf(
        "id" to "saldo-$id",
        "fechaLegible" to fechaLegible,
        "fechaMs" to ahoraMs,
        "monto" to monto,
        "metodoPago" to "Saldo a favor del proveedor",
        "numeroOperacion" to "",
        "pagos" to listOf(
            mapOf(
                "metodoPago" to "Saldo a favor del proveedor",
                "monto" to monto,
                "numeroOperacion" to ""
            )
        ),
        "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
        "usuarioEmail" to usuarioEmail,
        "motivo" to motivo,
        "facturaNumero" to facturaNumero,
        "notas" to motivo
    )

    /**
     * Aplica el descuento dentro de la transacción: relee el saldo fresco (si otro
     * usuario lo cambió, aborta con la verdad), baja el saldo y deja historial
     * con justificación (factura, monto, quién, cuándo, motivo).
     */
    fun aplicarEnTransaccion(
        tx: Transaction,
        refSaldo: DocumentReference,
        saldoAUsar: Double,
        facturaId: String,
        facturaNumero: String,
        motivo: String,
        usuarioNombre: String,
        usuarioEmail: String,
        ahoraMs: Long,
        fechaLegible: String
    ) {
        val snap = tx.get(refSaldo)
        val saldoActual = snap.getDouble("saldoAFavor") ?: 0.0
        if (saldoAUsar > saldoActual + 0.01) {
            throw IllegalStateException(
                "El proveedor solo tiene " + String.format(Locale.US, "%.2f", saldoActual) +
                    " a favor; no puedes descontar " + String.format(Locale.US, "%.2f", saldoAUsar) +
                    ". Actualiza y vuelve a intentar."
            )
        }
        @Suppress("UNCHECKED_CAST")
        val historial = (snap.get("historialSaldoAFavor") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
        historial.add(
            mapOf(
                "id" to "uso-" + java.util.UUID.randomUUID().toString(),
                "tipo" to "SALDO_USADO_RECEPCION",
                "monto" to saldoAUsar,
                "facturaId" to facturaId,
                "facturaNumero" to facturaNumero,
                "motivo" to motivo,
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail
            )
        )
        tx.update(
            refSaldo,
            mapOf(
                "saldoAFavor" to (saldoActual - saldoAUsar).coerceAtLeast(0.0),
                "historialSaldoAFavor" to historial,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )
        )
    }

    /**
     * Registra una salida del saldo a favor (cobro en efectivo o declarado perdido):
     * relee el saldo fresco, valida el monto y deja historial con justificación.
     */
    fun registrarEgresoEnTransaccion(
        tx: Transaction,
        refSaldo: DocumentReference,
        monto: Double,
        tipo: String,
        documento: String,
        motivo: String,
        usuarioNombre: String,
        usuarioEmail: String,
        ahoraMs: Long,
        fechaLegible: String
    ) {
        val snap = tx.get(refSaldo)
        val saldoActual = snap.getDouble("saldoAFavor") ?: 0.0
        if (monto > saldoActual + 0.01) {
            throw IllegalStateException(
                "El proveedor solo tiene " + String.format(Locale.US, "%.2f", saldoActual) +
                    " a favor; no puedes registrar " + String.format(Locale.US, "%.2f", monto) + "."
            )
        }
        @Suppress("UNCHECKED_CAST")
        val historial = (snap.get("historialSaldoAFavor") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
        historial.add(
            mapOf(
                "id" to "salida-" + java.util.UUID.randomUUID().toString(),
                "tipo" to tipo,
                "monto" to monto,
                "documento" to documento.trim().uppercase(),
                "motivo" to motivo.trim(),
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail
            )
        )
        tx.update(
            refSaldo,
            mapOf(
                "saldoAFavor" to (saldoActual - monto).coerceAtLeast(0.0),
                "historialSaldoAFavor" to historial,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )
        )
    }

    /**
     * Registra un INGRESO al saldo a favor (ej: anulación de factura ya pagada donde
     * el proveedor queda debiendo): relee el saldo fresco dentro de la transacción,
     * suma el monto y deja historial con justificación (factura, quién, cuándo, motivo).
     */
    fun registrarIngresoEnTransaccion(
        tx: Transaction,
        refSaldo: DocumentReference,
        monto: Double,
        tipo: String,
        documento: String,
        motivo: String,
        usuarioNombre: String,
        usuarioEmail: String,
        ahoraMs: Long,
        fechaLegible: String
    ) {
        if (monto <= 0.0) return
        val snap = tx.get(refSaldo)
        val saldoActual = snap.getDouble("saldoAFavor") ?: 0.0
        @Suppress("UNCHECKED_CAST")
        val historial = (snap.get("historialSaldoAFavor") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
        historial.add(
            mapOf(
                "id" to "ingreso-" + java.util.UUID.randomUUID().toString(),
                "tipo" to tipo,
                "monto" to monto,
                "documento" to documento.trim().uppercase(),
                "motivo" to motivo.trim(),
                "fechaLegible" to fechaLegible,
                "fechaMs" to ahoraMs,
                "usuarioNombre" to usuarioNombre.ifBlank { "Administración" },
                "usuarioEmail" to usuarioEmail
            )
        )
        tx.update(
            refSaldo,
            mapOf(
                "saldoAFavor" to saldoActual + monto,
                "historialSaldoAFavor" to historial,
                "actualizadoEl" to FieldValue.serverTimestamp()
            )
        )
    }
}
