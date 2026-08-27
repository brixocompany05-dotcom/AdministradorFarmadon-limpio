package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica

import androidx.compose.ui.graphics.Color
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object ProductDetailMapper {

    private val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.US)

    fun formatearTipoMovimiento(tipo: String): String = when (tipo.uppercase()) {
        "CREACION_PRODUCTO"             -> "Creación"
        "INGRESO_INICIAL"               -> "Ingreso Inicial"
        "REABASTECIMIENTO"              -> "Reabastecimiento"
        "ENTRADA_COMPRA"                -> "Compra"
        "VENTA"                         -> "Venta"
        "DISPENSACION"                  -> "Dispensación"
        "AJUSTE_POSITIVO"               -> "Ajuste +"
        "AJUSTE_NEGATIVO"               -> "Ajuste -"
        "BAJA_LOTE"                     -> "Baja de Lote"
        "MERMA_DESCARTE"                -> "Merma / Descarte"
        "SALIDA_DEVOLUCION_PROVEEDOR"   -> "Devolución Proveedor"
        "SALIDA_CANJE_PROVEEDOR"         -> "Salida Canje"
        "ENTRADA_CANJE_PROVEEDOR"        -> "Entrada Canje"
        "BLOQUEO_CUARENTENA_LOTE"        -> "Cuarentena (Bloqueo)"
        "DESBLOQUEO_LOTE"                -> "Liberación Cuarentena"
        "ANULACION_ENTRADA"              -> "Anulación Ingreso"
        "AJUSTE_PRECIOS_PRESENTACIONES"  -> "Ajuste de Precios"
        else                            -> tipo.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    fun formatearClasificacion(raw: String): String = when (raw.uppercase()) {
        "VENTA_LIBRE"        -> "Venta libre"
        "CONTROLADO"         -> "Controlado"
        "RECETA_MEDICA"      -> "Receta médica"
        "REFRIGERADO"        -> "Refrigerado"
        else                 -> raw.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    fun formatearTemperatura(code: String): String =
        com.app.administradorfarmadon.inventario.compartido.modelo.CondicionTemperatura.etiqueta(code)

    fun diasHastaVencer(vencimiento: String): Int? = FechaVencimientoHelper.diasHastaVencer(vencimiento)

    fun colorVencimiento(dias: Int?): Color = when {
        dias == null      -> FDColors.TextSecondary
        dias < 0          -> FDColors.Error
        dias <= 30        -> FDColors.Warning
        dias <= 90        -> FDColors.Warning.copy(alpha = 0.7f)
        else              -> FDColors.TextPrimary // Neutro, no verde — antes Success
    }

    fun resolverFactorContenedor(p: MoldeProductos): Double =
        UnidadVentaHelper.factorContenido(p.contenido, p.presentaciones)

    fun formatoSinDecimalesInnecesarios(value: Double): String {
        return if (kotlin.math.abs(value - value.toInt()) < 0.0001) value.toInt().toString()
        else String.format(Locale.US, "%.2f", value)
    }

    fun resumenStockVisual(p: MoldeProductos, totalStock: Double): String {
        val singular = p.inventarioPerfilUnidadSingular.ifBlank { p.unidadVisualInventario.ifBlank { "unidad" } }
        val plural = p.inventarioPerfilUnidadPlural.ifBlank {
            if (singular.endsWith("s", ignoreCase = true)) singular else "${singular}s"
        }
        val factor = resolverFactorContenedor(p)
        val contenedores = (totalStock / factor).coerceAtLeast(0.0)
        val unidadContenida = p.inventarioPerfilUnidadContenido.ifBlank { p.unidadBase.ifBlank { "unidades" } }
        val nombreContenedor = if (kotlin.math.abs(contenedores - 1.0) < 0.0001) singular else plural
        val totalBase = formatoSinDecimalesInnecesarios(totalStock)
        val totalCont = formatoSinDecimalesInnecesarios(contenedores)
        return "$totalCont $nombreContenedor ($totalBase $unidadContenida)"
    }

    fun contenidoPorUnidadSufijo(p: MoldeProductos): String? {
        val contenido = p.inventarioPerfilContenidoPorUnidad.trim()
        val unidad = p.inventarioPerfilUnidadContenido.trim()
        if (contenido.isBlank() || unidad.isBlank()) return null
        val valor = contenido.replace(",", ".").toDoubleOrNull() ?: return null
        return "(${formatoSinDecimalesInnecesarios(valor)} $unidad c/u)"
    }

    fun resumenStockMinimoVisual(p: MoldeProductos): String {
        val minimoBase = p.stockMinimoBase
        if (minimoBase <= 0.0) return "No configurado"

        val singular = p.inventarioPerfilUnidadSingular.trim()
        val plural = p.inventarioPerfilUnidadPlural.trim().ifBlank {
            if (singular.endsWith("s", ignoreCase = true)) singular else "${singular}s"
        }
        val factor = resolverFactorContenedor(p)

        if (singular.isNotBlank() && factor > 0.0) {
            val unidades = minimoBase / factor
            val nombre = if (kotlin.math.abs(unidades - 1.0) < 0.0001) singular else plural
            return "${formatoSinDecimalesInnecesarios(unidades)} $nombre"
        }

        val unidadBase = p.unidadBase.ifBlank { p.inventarioPerfilUnidadContenido.ifBlank { "base" } }
        return "${formatoSinDecimalesInnecesarios(minimoBase)} $unidadBase"
    }

    fun formatRelativeDate(dateString: String): String {
        return try {
            val date = java.time.LocalDateTime.parse(dateString, dbFormatter)
            val now = java.time.LocalDateTime.now()
            val daysBetween = ChronoUnit.DAYS.between(date.toLocalDate(), now.toLocalDate())

            val timeStr = date.format(timeFormatter).lowercase()

            when (daysBetween.toInt()) {
                0 -> "Hoy, $timeStr"
                1 -> "Ayer, $timeStr"
                else -> date.format(fullDateFormatter).lowercase()
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun parseFechaEpochMillis(dateString: String): Long {
        return try {
            java.time.LocalDateTime.parse(dateString, dbFormatter)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
