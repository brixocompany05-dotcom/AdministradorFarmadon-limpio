package com.app.administradorfarmadon.inventario.ingresarstockproductos.logica

import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modos de ingreso físico.
 */
enum class ModoIngresoStock {
    POR_EMPAQUE_MULTIPLO, // Cajas x Unidades
    UNIDADES_DIRECTAS     // Unidades sueltas
}

/**
 * Condiciones comerciales de pago al proveedor.
 */
enum class TipoCondicionPago(val etiqueta: String) {
    CONTADO("Contado"),
    CREDITO_15("Crédito 15 días"),
    CREDITO_30("Crédito 30 días"),
    CREDITO_45("Crédito 45 días"),
    CREDITO_60("Crédito 60 días"),
    CREDITO_PERSONALIZADO("Personalizado")
}

/**
 * Estado inmutable de recepción de mercadería y asentamiento de lote.
 * Estructura limpia, modular y con responsabilidades bien separadas (1+1=2).
 */
data class StockEntryState(
    // ── 1. CONTEXTO DEL PRODUCTO MAESTRO ──
    val productoId: String = "",
    val productoNombre: String = "",
    val categoria: String = "",
    val laboratorio: String = "",
    val empaque: String = "Caja",
    val codigoBarras: String = "",
    val stockActual: Double = 0.0,
    val lotesExistentes: Map<String, LoteProducto> = emptyMap(),
    val esRefrigerado: Boolean = false,
    val principioActivo: String = "",
    val clasificacionControl: String = "VENTA_LIBRE",
    val requiereReceta: Boolean = false,
    // Perfil de contenido: cuántas unidades de contenido trae 1 unidad del producto.
    // Ej: Caja de 100 Tabletas → contenidoPorUnidad=100, unidadContenido="Tab"
    // Se usa solo para mostrar la equivalencia en el resumen visual del ingreso.
    val contenidoPorUnidad: Int = 1,
    val unidadContenido: String = "",

    // ── 2. IDENTIFICACIÓN Y VENCIMIENTO DEL LOTE ──
    val numeroLote: String = "",
    val avisoLoteEnOtroProducto: String = "",
    val mostrarSugerenciaUltimaFactura: Boolean = false,
    val ultimaFacturaSugerida: FacturaCompra? = null,
    val mesVencimiento: Int = 0,
    val anoVencimiento: Int = 0,
    val camposBloqueadosPorLoteExistente: Boolean = false,

    // ── 3. CANTIDAD FÍSICA Y BONIFICACIÓN COMERCIAL ──
    // Sin selección por defecto — usuario debe elegir Por cajas o Por unidades
    val modoIngreso: ModoIngresoStock? = null,
    val cantidadBultos: String = "",
    val unidadesPorBulto: String = "",
    val cantidadDirecta: String = "",
    val activarBonificacion: Boolean = false,
    val cantidadBonificacion: String = "",

    // ── 4. COSTOS DE ADQUISICIÓN Y PRECIOS ──
    val costoCompraLote: String = "",
    val precioVentaActual: Double = 0.0,
    val ultimoPrecioCompraRegistrado: Double = 0.0,

    // ── 5. COMPROBANTE, TOPE Y CONCILIACIÓN DE FACTURA ──
    val numeroFactura: String = "",
    val montoTotalFactura: String = "",
    val facturaDetectadaInfo: FacturaCompra? = null,
    val tipoCondicionPago: TipoCondicionPago = TipoCondicionPago.CONTADO,
    val diasCreditoPersonalizado: String = "",

    // ── 6. PROVEEDOR / DISTRIBUIDOR ──
    val proveedoresDisponibles: List<Proveedor> = emptyList(),
    val proveedorNombre: String = "",
    val proveedorIdSeleccionado: String = "",
    val rucProveedorSeleccionado: String = "",
    val mostrarDialogoProveedores: Boolean = false,
    val busquedaProveedorDialogo: String = "",
    val mostrarFormularioNuevoProveedor: Boolean = false,
    val nuevoProveedorNombre: String = "",
    val nuevoProveedorRuc: String = "",
    val nuevoProveedorTelefono: String = "",
    val nuevoProveedorContacto: String = "",
    val nuevoProveedorEmail: String = "",
    val nuevoProveedorDireccion: String = "",

    // ── 7. REPOSICIÓN Y VINCULACIÓN DE COMPRAS ──
    val stockMinimoActual: Double = 0.0,
    val stockMinimoPersonalizado: String = "",
    val stockMinimoEditando: Boolean = false,
    val mostrarDialogoVincularPedido: Boolean = false,
    val pedidoPendienteVinculable: com.app.administradorfarmadon.compras.datos.PedidoCompra? = null,

    // ── 8. ESTADOS DE OPERACIÓN Y UI ──
    val estaCargando: Boolean = false,
    val estaGuardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
) {
    val stockMinimoPropuesto: Int
        get() = (nuevoStockProyectado * 0.2).toInt().coerceAtLeast(1)

    val stockMinimoAplicar: Int
        get() = stockMinimoPersonalizado.toIntOrNull() ?: if (stockMinimoActual > 0) stockMinimoActual.toInt() else stockMinimoPropuesto

    val mostrarPropuestaStockMinimo: Boolean
        get() = stockMinimoActual <= 0 && nuevoStockProyectado > 0
    // ── FECHA DE VENCIMIENTO Y ALERTAS FEFO ──
    val fechaVencimiento: String
        get() = if (mesVencimiento in 1..12 && anoVencimiento >= 2020) String.format("%02d/%d", mesVencimiento, anoVencimiento) else ""

    val loteExistenteDetectado: LoteProducto?
        get() {
            val cleanKey = FechaVencimientoHelper.llaveLote(numeroLote)
            if (cleanKey.isBlank()) return null
            return lotesExistentes.entries.firstOrNull {
                it.key.equals(cleanKey, ignoreCase = true) || it.value.numero.equals(numeroLote.trim(), ignoreCase = true)
            }?.value
        }

    val stockActualDelLote: Double
        get() {
            val l = loteExistenteDetectado ?: return 0.0
            return l.cantidad + l.cantidadBloqueada
        }

    val diasHastaVencerCalc: Int?
        get() = FechaVencimientoHelper.diasHastaVencer(fechaVencimiento)

    val mesesParaVencer: Int
        get() {
            val dias = diasHastaVencerCalc ?: return Int.MAX_VALUE
            // Aproximación solo para compatibilidad histórica; la verdad es en días.
            return dias / 30
        }

    val esVencimientoCorto: Boolean
        get() {
            val dias = diasHastaVencerCalc ?: return false
            return dias in 1..180
        }

    val esVencidoOInvalido: Boolean
        get() {
            if (fechaVencimiento.isBlank()) return true
            val dias = diasHastaVencerCalc ?: return true
            return dias <= 0
        }

    // ── CÁLCULO DIRECTO DE CANTIDADES (SUMA SIMPLE 1+1=2) ──
    val cantidadCompradaBase: Double
        get() = when (modoIngreso) {
            ModoIngresoStock.POR_EMPAQUE_MULTIPLO -> {
                val bultos = cantidadBultos.toDoubleOrNull() ?: 0.0
                val unidades = unidadesPorBulto.toDoubleOrNull() ?: 0.0
                if (bultos <=0 || unidades <=0) 0.0 else bultos * unidades
            }
            ModoIngresoStock.UNIDADES_DIRECTAS -> {
                cantidadDirecta.toDoubleOrNull() ?: 0.0
            }
            null -> 0.0
        }

    val bonificacionUnidadesTotales: Double
        get() {
            if (!activarBonificacion) return 0.0
            val cantBonif = cantidadBonificacion.toDoubleOrNull() ?: 0.0
            if (cantBonif <= 0) return 0.0
            // Bonificación sigue el mismo modo que el ingreso principal:
            // - POR_EMPAQUE_MULTIPLO → cantBonif son CAJAS de regalo, se multiplica por unidadesPorBulto (mismo factor)
            // - UNIDADES_DIRECTAS   → cantBonif son UNIDADES sueltas directas
            // Así el empleado nunca necesita multiplicar manualmente.
            return when (modoIngreso) {
                ModoIngresoStock.POR_EMPAQUE_MULTIPLO -> {
                    val unidades = unidadesPorBulto.toDoubleOrNull() ?: 0.0
                    if (unidades > 0) cantBonif * unidades else cantBonif
                }
                else -> cantBonif
            }
        }

    val cantidadTotalIngresada: Double
        get() = cantidadCompradaBase + bonificacionUnidadesTotales

    val nuevoStockProyectado: Double
        get() = stockActual + cantidadTotalIngresada

    // ── VALORIZACIÓN FINANCIERA ──
    val costoTotalIngreso: Double
        get() = costoCompraLote.toDoubleOrNull() ?: 0.0

    // Mercadería comprada sin precio real corrompe margen y valorización — se bloquea.
    val faltaCostoDeCompra: Boolean
        get() = cantidadCompradaBase > 0 && costoTotalIngreso <= 0

    // R3: el costo por unidad se reparte solo entre las unidades COMPRADAS.
    // La bonificación (regalo) entra al stock físico pero no se pagó, así que
    // no debe inflar el divisor ni distorsionar el costo real del lote.
    val costoUnitarioCalculado: Double
        get() = if (cantidadCompradaBase > 0 && costoTotalIngreso > 0) {
            costoTotalIngreso / cantidadCompradaBase
        } else 0.0

    val margenGananciaEstimado: Double?
        get() {
            if (costoUnitarioCalculado <= 0 || precioVentaActual <= 0) return null
            return ((precioVentaActual - costoUnitarioCalculado) / precioVentaActual) * 100.0
        }

    val diferenciaCostoPorcentual: Double?
        get() {
            if (costoUnitarioCalculado <= 0 || ultimoPrecioCompraRegistrado <= 0) return null
            return ((costoUnitarioCalculado - ultimoPrecioCompraRegistrado) / ultimoPrecioCompraRegistrado) * 100.0
        }

    // ── CONTROL DE FACTURA Y TOPE EN PAPEL ──
    val montoTotalFacturaDouble: Double
        get() = if (facturaDetectadaInfo != null) {
            facturaDetectadaInfo.montoTotal
        } else {
            montoTotalFactura.toDoubleOrNull() ?: 0.0
        }

    val montoAcumuladoProyectadoFactura: Double
        get() = if (facturaDetectadaInfo != null) {
            facturaDetectadaInfo.montoAcumulado + costoTotalIngreso
        } else {
            costoTotalIngreso
        }

    val excedeMontoFactura: Boolean
        get() = montoTotalFacturaDouble > 0 && montoAcumuladoProyectadoFactura > (montoTotalFacturaDouble + 0.01)

    val porcentajeFacturaProgreso: Float
        get() = if (montoTotalFacturaDouble > 0) {
            (montoAcumuladoProyectadoFactura / montoTotalFacturaDouble).toFloat().coerceIn(0f, 1f)
        } else 1f

    // ── CONDICIÓN DE PAGO Y VENCIMIENTO DE CUENTA ──
    val diasCreditoPersonalizadoValido: Boolean
        get() = tipoCondicionPago != TipoCondicionPago.CREDITO_PERSONALIZADO ||
                ((diasCreditoPersonalizado.toIntOrNull() ?: 0) > 0)

    val fechaVencimientoPagoCalculada: String
        get() {
            val dias = when (tipoCondicionPago) {
                TipoCondicionPago.CONTADO -> 0
                TipoCondicionPago.CREDITO_15 -> 15
                TipoCondicionPago.CREDITO_30 -> 30
                TipoCondicionPago.CREDITO_45 -> 45
                TipoCondicionPago.CREDITO_60 -> 60
                TipoCondicionPago.CREDITO_PERSONALIZADO -> diasCreditoPersonalizado.toIntOrNull() ?: 0
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            if (tipoCondicionPago == TipoCondicionPago.CONTADO) return sdf.format(Date())
            if (dias <= 0) return ""
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dias)
            return sdf.format(cal.time)
        }

    val condicionPagoResumen: String
        get() = when (tipoCondicionPago) {
            TipoCondicionPago.CONTADO -> "Contado (Cancelado en recepción)"
            TipoCondicionPago.CREDITO_PERSONALIZADO ->
                if (diasCreditoPersonalizadoValido) "Crédito $diasCreditoPersonalizado días (Vence: $fechaVencimientoPagoCalculada)"
                else "⚠️ Define los días de crédito para fijar la fecha de pago"
            else -> "${tipoCondicionPago.etiqueta} (Vence: $fechaVencimientoPagoCalculada)"
        }

    // ── ETIQUETAS NATURALES DEL EMPAQUE DEL PRODUCTO ──
    val nombreEmpaquePlural: String
        get() {
            val emp = empaque.trim()
            if (emp.isBlank()) return "Unidades"
            return when {
                emp.endsWith("a", ignoreCase = true) || emp.endsWith("e", ignoreCase = true) || emp.endsWith("o", ignoreCase = true) || emp.endsWith("u", ignoreCase = true) || emp.endsWith("i", ignoreCase = true) -> "${emp}s"
                else -> "${emp}es"
            }
        }

    val etiquetaModoMultiplo: String
        get() = "Por paquetes / bultos"

    val etiquetaModoDirecto: String
        get() = "$nombreEmpaquePlural sueltas (Directo)"

    val etiquetaCantidadBultos: String
        get() = "Bultos / paquetes comprados *"

    val etiquetaUnidadesPorBulto: String
        get() = "$nombreEmpaquePlural por bulto *"

    val etiquetaCantidadDirecta: String
        get() = "$nombreEmpaquePlural compradas *"

    // ── FILTROS Y VALIDACIONES SANITARIAS / CONTABLES ──
    val esMedicamentoControlado: Boolean
        get() = clasificacionControl.uppercase() in listOf("PSICOTROPICO", "CONTROLADO", "ESTUPEFACIENTE") || (requiereReceta && clasificacionControl.uppercase() != "VENTA_LIBRE")

    val tieneFaltaComprobanteEnControlado: Boolean
        get() = esMedicamentoControlado && (numeroFactura.isBlank() || proveedorNombre.isBlank())

    val proveedoresFiltrados: List<Proveedor>
        get() {
            val q = busquedaProveedorDialogo.trim().lowercase()
            if (q.isBlank()) return proveedoresDisponibles
            return proveedoresDisponibles.filter {
                it.nombre.lowercase().contains(q) || it.idFiscal.contains(q)
            }
        }

    val tieneInconsistenciaFacturaSinProveedor: Boolean
        get() = numeroFactura.isNotBlank() && proveedorNombre.isBlank()

    val tieneVencimientoNoCoincideConLoteExistente: Boolean
        get() {
            val lote = loteExistenteDetectado ?: return false
            val normIngresado = FechaVencimientoHelper.normalizar(fechaVencimiento) ?: fechaVencimiento
            val normExistente = FechaVencimientoHelper.normalizar(lote.vencimiento) ?: lote.vencimiento
            if (normIngresado.isBlank() || normExistente.isBlank()) return false
            return normIngresado != normExistente
        }

    val faltaMontoTotalEnFacturaNueva: Boolean
        get() = numeroFactura.isNotBlank() && facturaDetectadaInfo == null && proveedorNombre.isNotBlank() && montoTotalFacturaDouble <= 0

    val tieneUnidadesPorCajaInvalida: Boolean
        get() = modoIngreso == ModoIngresoStock.POR_EMPAQUE_MULTIPLO && (unidadesPorBulto.toIntOrNull() ?: 0) <= 0

    val tieneBonificacionActivadaSinCantidad: Boolean
        get() = activarBonificacion && (cantidadBonificacion.isBlank() || (cantidadBonificacion.toDoubleOrNull() ?: 0.0) <= 0)

    val esFormularioValido: Boolean
        get() = productoId.isNotBlank() &&
                numeroLote.isNotBlank() &&
                modoIngreso != null &&
                fechaVencimiento.isNotBlank() &&
                cantidadTotalIngresada > 0 &&
                !tieneUnidadesPorCajaInvalida &&
                !esVencidoOInvalido &&
                !tieneInconsistenciaFacturaSinProveedor &&
                !tieneFaltaComprobanteEnControlado &&
                !excedeMontoFactura &&
                !tieneVencimientoNoCoincideConLoteExistente &&
                !faltaMontoTotalEnFacturaNueva &&
                !tieneBonificacionActivadaSinCantidad &&
                diasCreditoPersonalizadoValido &&
                !faltaCostoDeCompra &&
                !estaGuardando
}
