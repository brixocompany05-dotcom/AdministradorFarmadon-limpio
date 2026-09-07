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
    private const val KEY_DEVICE_ID = "pos_device_id_v1"
    @Volatile
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    /**
     * Cada tablet tiene su hoja de borrador propia y estable.
     * Sin esto, el mismo cajero en 2 aparatos se pisa la canasta: la última
     * en guardar tapa a la otra y al reiniciar una pierde su venta.
     */
    private fun idDispositivo(): String {
        val sp = prefs ?: return ""
        var id = sp.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString().take(8)
            try {
                sp.edit().putString(KEY_DEVICE_ID, id).commit()
            } catch (_: Exception) {
                return ""
            }
        }
        return id.trim()
    }

    private fun claveBase(farmaciaId: String, sucursalId: String, cajeroId: String = ""): String =
        if (cajeroId.isNotBlank()) "draft_${farmaciaId}_${sucursalId}_${cajeroId.trim()}"
        else "draft_${farmaciaId}_$sucursalId"

    private fun clave(farmaciaId: String, sucursalId: String, cajeroId: String = ""): String {
        val base = claveBase(farmaciaId, sucursalId, cajeroId)
        val dev = idDispositivo()
        return if (dev.isBlank()) base else "${base}_d_$dev"
    }

    fun guardarBorrador(
        context: Context? = null,
        farmaciaId: String,
        sucursalId: String,
        cajeroId: String = "",
        borrador: BorradorVentaLocal
    ) {
        if (context != null) init(context)
        val sp = prefs ?: return
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return
        if (borrador.items.isEmpty()) {
            limpiarBorrador(context, farmaciaId, sucursalId, cajeroId)
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
                itemObj.put("recetaVerificada", it.recetaVerificada)
                itemObj.put("loteSugerido", it.loteSugerido)
                itemObj.put("loteVencimientoSugerido", it.loteVencimientoSugerido)
                itemObj.put("ubicacionAnaquel", it.ubicacionAnaquel)
                itemObj.put("cantidadDevuelta", it.cantidadDevuelta)
                itemObj.put("costoTotalReal", it.costoTotalReal)
                itemsArr.put(itemObj)
            }
            json.put("items", itemsArr)

            sp.edit().putString(clave(farmaciaId, sucursalId, cajeroId), json.toString()).commit()
        } catch (e: Exception) {
            android.util.Log.e("VentaBorradorLocalStore", "Error guardando borrador: ${e.message}", e)
        }
    }

    fun obtenerBorrador(
        context: Context? = null,
        farmaciaId: String,
        sucursalId: String,
        cajeroId: String = ""
    ): BorradorVentaLocal? {
        if (context != null) init(context)
        val sp = prefs ?: return null
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return null
        // Rescate en orden: hoja de ESTE aparato, luego hoja vieja del cajero (antes del fix),
        // luego hoja legada sin cajero. Cada aparato recupera lo suyo.
        val raw = (if (cajeroId.isNotBlank()) sp.getString(clave(farmaciaId, sucursalId, cajeroId), null) else null)
            ?: (if (cajeroId.isNotBlank()) sp.getString(claveBase(farmaciaId, sucursalId, cajeroId), null) else null)
            ?: sp.getString(claveBase(farmaciaId, sucursalId), null)
            ?: return null
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
                            recetaVerificada = itObj.optBoolean("recetaVerificada", false),
                            loteSugerido = itObj.optString("loteSugerido", ""),
                            loteVencimientoSugerido = itObj.optString("loteVencimientoSugerido", ""),
                            ubicacionAnaquel = itObj.optString("ubicacionAnaquel", ""),
                            cantidadDevuelta = itObj.optInt("cantidadDevuelta", 0),
                            costoTotalReal = itObj.optDouble("costoTotalReal", 0.0)
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
        } catch (e: Exception) {
            android.util.Log.e("VentaBorradorLocalStore", "Error obteniendo borrador: ${e.message}", e)
            null
        }
    }

    fun limpiarBorrador(
        context: Context? = null,
        farmaciaId: String,
        sucursalId: String,
        cajeroId: String = ""
    ) {
        if (context != null) init(context)
        val sp = prefs ?: return
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return
        val editor = sp.edit()
        if (cajeroId.isNotBlank()) {
            editor.remove(clave(farmaciaId, sucursalId, cajeroId))
            editor.remove(claveBase(farmaciaId, sucursalId, cajeroId))
        }
        editor.remove(clave(farmaciaId, sucursalId))
        editor.remove(claveBase(farmaciaId, sucursalId))
        editor.commit()
    }

    /**
     * Higiene total: elimina todos los borradores locales en disco al cerrar sesión
     * o cambiar de cuenta, garantizando que ninguna sucursal o farmacia deje datos en memoria/disco.
     */
    fun limpiarTodo(context: Context? = null) {
        if (context != null) init(context)
        prefs?.edit()?.clear()?.commit()
    }
}
