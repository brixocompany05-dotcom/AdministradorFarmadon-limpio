package com.app.administradorfarmadon.ventas.compartido.datos

import android.content.Context
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import org.json.JSONArray
import org.json.JSONObject

/**
 * Borrador persistente de la venta en curso (FASE 11 H1).
 * Guarda en almacenamiento local los datos del carrito e idempotencia para recuperarse
 * si la tablet se apaga o la aplicación se cierra inesperadamente en pleno cobro.
 */
data class BorradorVentaLocal(
    val idempotenciaId: String = "",
    val items: List<ItemVenta> = emptyList(),
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val tipoComprobante: String = "BOLETA",
    val descuento: Double = 0.0,
    val confirmoReceta: Boolean = false,
    val timestamp: Long = 0L
)

object VentaBorradorLocalStore {
    private const val PREFS_NAME = "farmadon_pos_drafts"
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun clave(farmaciaId: String, sucursalId: String): String = "draft_${farmaciaId}_$sucursalId"

    fun guardarBorrador(context: Context? = null, farmaciaId: String, sucursalId: String, borrador: BorradorVentaLocal) {
        if (context != null) init(context)
        val sp = prefs ?: return
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return
        if (borrador.items.isEmpty()) {
            limpiarBorrador(context, farmaciaId, sucursalId)
            return
        }

        try {
            val json = JSONObject()
            json.put("idempotenciaId", borrador.idempotenciaId)
            json.put("tipoComprobante", borrador.tipoComprobante)
            json.put("descuento", borrador.descuento)
            json.put("confirmoReceta", borrador.confirmoReceta)
            json.put("timestamp", borrador.timestamp)

            val clienteObj = JSONObject()
            clienteObj.put("tipoDocumento", borrador.cliente.tipoDocumento)
            clienteObj.put("numeroDocumento", borrador.cliente.numeroDocumento)
            clienteObj.put("nombre", borrador.cliente.nombre)
            clienteObj.put("clienteId", borrador.cliente.clienteId)
            json.put("cliente", clienteObj)

            val itemsArr = JSONArray()
            for (it in borrador.items) {
                val itemObj = JSONObject()
                itemObj.put("productoId", it.productoId)
                itemObj.put("nombreProducto", it.nombreProducto)
                itemObj.put("empaque", it.empaque)
                itemObj.put("presentacionId", it.presentacionId)
                itemObj.put("presentacionNombre", it.presentacionNombre)
                itemObj.put("cantidad", it.cantidad)
                itemObj.put("precioUnitario", it.precioUnitario)
                itemObj.put("subtotal", it.subtotal)
                itemObj.put("requiereReceta", it.requiereReceta)
                itemObj.put("loteSugerido", it.loteSugerido)
                itemObj.put("loteVencimientoSugerido", it.loteVencimientoSugerido)
                itemObj.put("ubicacionAnaquel", it.ubicacionAnaquel)
                itemObj.put("cantidadDevuelta", it.cantidadDevuelta)
                itemsArr.put(itemObj)
            }
            json.put("items", itemsArr)

            sp.edit().putString(clave(farmaciaId, sucursalId), json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun obtenerBorrador(context: Context? = null, farmaciaId: String, sucursalId: String): BorradorVentaLocal? {
        if (context != null) init(context)
        val sp = prefs ?: return null
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return null
        val raw = sp.getString(clave(farmaciaId, sucursalId), null) ?: return null
        return try {
            val json = JSONObject(raw)
            val idempotenciaId = json.optString("idempotenciaId", "")
            val tipoComprobante = json.optString("tipoComprobante", "BOLETA")
            val descuento = json.optDouble("descuento", 0.0)
            val confirmoReceta = json.optBoolean("confirmoReceta", false)
            val timestamp = json.optLong("timestamp", 0L)

            val clienteObj = json.optJSONObject("cliente")
            val cliente = if (clienteObj != null) {
                ClienteDeVenta(
                    tipoDocumento = clienteObj.optString("tipoDocumento", "NINGUNO"),
                    numeroDocumento = clienteObj.optString("numeroDocumento", ""),
                    nombre = clienteObj.optString("nombre", "Consumidor Final"),
                    clienteId = clienteObj.optString("clienteId", "")
                )
            } else ClienteDeVenta()

            val itemsArr = json.optJSONArray("items")
            val itemsList = mutableListOf<ItemVenta>()
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val itObj = itemsArr.getJSONObject(i)
                    itemsList.add(
                        ItemVenta(
                            productoId = itObj.optString("productoId", ""),
                            nombreProducto = itObj.optString("nombreProducto", ""),
                            empaque = itObj.optString("empaque", ""),
                            presentacionId = itObj.optString("presentacionId", ""),
                            presentacionNombre = itObj.optString("presentacionNombre", ""),
                            cantidad = itObj.optInt("cantidad", 1),
                            precioUnitario = itObj.optDouble("precioUnitario", 0.0),
                            subtotal = itObj.optDouble("subtotal", 0.0),
                            requiereReceta = itObj.optBoolean("requiereReceta", false),
                            loteSugerido = itObj.optString("loteSugerido", ""),
                            loteVencimientoSugerido = itObj.optString("loteVencimientoSugerido", ""),
                            ubicacionAnaquel = itObj.optString("ubicacionAnaquel", ""),
                            cantidadDevuelta = itObj.optInt("cantidadDevuelta", 0)
                        )
                    )
                }
            }

            if (itemsList.isEmpty()) null
            else BorradorVentaLocal(
                idempotenciaId = idempotenciaId,
                items = itemsList,
                cliente = cliente,
                tipoComprobante = tipoComprobante,
                descuento = descuento,
                confirmoReceta = confirmoReceta,
                timestamp = timestamp
            )
        } catch (_: Exception) {
            null
        }
    }

    fun limpiarBorrador(context: Context? = null, farmaciaId: String, sucursalId: String) {
        if (context != null) init(context)
        val sp = prefs ?: return
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return
        sp.edit().remove(clave(farmaciaId, sucursalId)).apply()
    }
}
