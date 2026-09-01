package com.app.administradorfarmadon.compras.logica

import com.app.administradorfarmadon.compras.datos.LoteExistenteVista
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import java.text.SimpleDateFormat
import java.util.*

data class ItemPedidoCompra(
    val productoId: String = "",
    val productoNombre: String = "",
    val presentacion: String = "Und",
    val categoria: String = "",
    val codigo: String = "",
    val precioCompra: Double = 0.0,
    val cantidad: Int = 0,
    val cantidadRecibida: Int = 0
) {
    val subtotal: Double get() = cantidad * precioCompra
    val saldoPendiente: Int get() = (cantidad - cantidadRecibida).coerceAtLeast(0)
}

data class PedidoProveedor(
    val proveedorId: String,
    val proveedorNombre: String,
    val proveedorRuc: String = "",
    val proveedorTelefono: String = "",
    val items: List<ItemPedidoCompra>
) {
    val itemsValidos: List<ItemPedidoCompra> get() = items.filter { it.cantidad > 0 }
    val totalProductos: Int get() = itemsValidos.size
    val totalUnidades: Int get() = itemsValidos.sumOf { it.cantidad }
    val totalInversion: Double get() = itemsValidos.sumOf { it.subtotal }
    val tieneItems: Boolean get() = totalProductos > 0
}

/** Mercadería YA pedida y en camino para un producto (órdenes ENVIADO o PARCIAL). */
data class ProductoEnCamino(val unidades: Int, val ordenes: List<String>)

/**
 * Una línea de la factura leída EN VIVO para el diálogo de anulación (plan Anular Factura):
 * entró según papel vs lo que existe HOY en el estante. La app cuenta; la persona solo mira.
 */
data class LineaAnulacionVista(
    val productoId: String = "",
    val productoNombre: String = "",
    val loteNumero: String = "",
    val vencimiento: String = "",
    val entro: Double = 0.0,
    val hoy: Double = 0.0,
    val devuelve: Double = 0.0,
    val noVuelve: Double = 0.0,
    val loteExiste: Boolean = true,
    val productoExiste: Boolean = true
)

data class ComprasUiState(
    val tabSeleccionada: String = "REPOSICION",
    val subTabProveedor: String = "RESUMEN",
    val todosLosProductos: List<PharmProduct> = emptyList(),
    val proveedores: List<Proveedor> = emptyList(),
    val facturas: List<FacturaCompra> = emptyList(),
    val metodosPago: List<InstanciaPago> = emptyList(),
    val pedidosGuardados: List<PedidoCompra> = emptyList(),
    val pedidosPorProveedor: Map<String, Map<String, Int>> = emptyMap(),
    /** Quién aportó cada unidad del carrito (producto → usuario → cantidad). */
    val contribuidoresCarrito: Map<String, Map<String, Map<String, Int>>> = emptyMap(),
    val mostrarDialogoRecepcion: Boolean = false,
    val pedidoParaRecepcionar: PedidoCompra? = null,
    val facturaRecepcionExistente: FacturaCompra? = null,
    val procesandoRecepcion: Boolean = false,
    val cargandoIndiceRecepcion: Boolean = false,
    val indiceLotesOrden: Map<String, List<LoteExistenteVista>> = emptyMap(),
    val productoPendienteConfirmar: PharmProduct? = null,
    val cantidadExtraPropuesta: Int = 0,
    val proveedorSeleccionadoId: String? = null,
    val facturaSeleccionadaId: String? = null,
    val filtroEstadoFactura: String = "TODAS",
    val busquedaQuery: String = "",
    val cargando: Boolean = true,
    val guardandoProveedor: Boolean = false,
    val procesandoPago: Boolean = false,
    val enviandoPedido: Boolean = false,
    val procesandoEdicionPedido: Boolean = false,
    val procesandoEliminacionPedido: Boolean = false,
    val mostrarDialogoProveedor: Boolean = false,
    val proveedorEditando: Proveedor? = null,
    val mostrarDialogoAbono: Boolean = false,
    val facturaParaAbonarId: String? = null,
    val mostrarDialogoProrroga: Boolean = false,
    val facturaParaProrrogaId: String? = null,
    val procesandoProrroga: Boolean = false,
    // ── NOTA DE CRÉDITO / AJUSTE DE FACTURA (el papel se reduce con documento) ──
    val mostrarDialogoNotaCredito: Boolean = false,
    val facturaParaNotaCreditoId: String? = null,
    val procesandoNotaCredito: Boolean = false,
    // ── DIÁLOGO ANULAR FACTURA (plan Anular Factura: papel + producto + plata juntos) ──
    val mostrarDialogoAnulacion: Boolean = false,
    val facturaParaAnular: FacturaCompra? = null,
    val procesandoAnulacion: Boolean = false,
    val cargandoLineasAnulacion: Boolean = false,
    val lineasAnulacion: List<LineaAnulacionVista> = emptyList(),
    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val errorEscucha: String? = null,
    /** Una sola verdad de cierre: nombre del proveedor cuya orden se guardó con ÉXITO.
     *  La pantalla lo consume y cierra el detalle SOLO entonces (jamás al fallar). */
    val envioExitosoProveedor: String? = null
) {
    val totalProductosCatalogo: Int get() = todosLosProductos.size
    val totalProveedores: Int get() = proveedores.size

    val totalProductosPorAgotarse: Int
        get() = todosLosProductos.count { (it.minStock > 0 && it.stock <= it.minStock) || it.stock <= 0 }

    fun calcularCantidadSugerida(p: PharmProduct): Int {
        val base = if (p.minStock > 0) {
            (p.minStock - p.stock).coerceAtLeast(0)
        } else {
            if (p.stock <= 0) 1 else 0
        }
        // Neto real: lo que YA viene en camino de órdenes enviadas no se vuelve a pedir
        return (base - enCaminoDe(p)).coerceAtLeast(0)
    }

    fun enCaminoDe(p: PharmProduct): Int = enCaminoPorProducto[p.id]?.unidades ?: 0

    /** Órdenes ENVIADO/PARCIAL por producto: escudo anti doble pedido. */
    val enCaminoPorProducto: Map<String, ProductoEnCamino>
        get() {
            val unidades = mutableMapOf<String, Int>()
            val ordenes = mutableMapOf<String, MutableSet<String>>()
            pedidosGuardados
                .filter { it.estado == "ENVIADO" || it.estado == "ENTREGA_PARCIAL" }
                .forEach { orden ->
                    orden.items.forEach { item ->
                        val saldo = item.saldoPendiente
                        if (saldo > 0) {
                            unidades[item.productoId] = (unidades[item.productoId] ?: 0) + saldo
                            ordenes.getOrPut(item.productoId) { mutableSetOf() }.add(orden.numeroOrden)
                        }
                    }
                }
            return unidades.mapValues { (id, u) -> ProductoEnCamino(u, ordenes[id]?.toList() ?: emptyList()) }
        }

    // ── OBTENER EL PROVEEDOR COMERCIAL (DISTINTO DEL LABORATORIO FABRICANTE) ──
    fun resolverProveedorProducto(p: PharmProduct): String {
        val prov = p.proveedor.trim().takeUnless { esPlaceholderProveedor(it) }
        if (!prov.isNullOrBlank()) return prov
        val match = proveedores.find { it.nombre.equals(p.laboratory, ignoreCase = true) }
        if (match != null) return match.nombre
        if (p.laboratory.isNotBlank() && !esPlaceholderProveedor(p.laboratory)) return p.laboratory
        return "Droguería General / Sin Asignar"
    }

    /** Valores que NO son un proveedor real (vacío honesto o placeholder legado tipo "N/A"). */
    private fun esPlaceholderProveedor(nombre: String): Boolean =
        nombre.isBlank() || listOf("N/A", "NA", "Genérico", "Sin asignar", "Sin Asignar")
            .any { it.equals(nombre.trim(), ignoreCase = true) }

    // ── DEUDA CONTABLE VINCULADA POR PROVEEDOR (las anuladas deben 0 por construcción) ──
    fun deudaPendienteProveedor(proveedor: Proveedor): Double {
        return facturas
            .filter {
                (it.proveedorId == proveedor.id || it.proveedorNombre.equals(proveedor.nombre, ignoreCase = true)) &&
                        !it.esAnulada && !it.esTotalmentePagada
            }
            .sumOf { it.saldoPendienteReal }
    }

    fun facturasPendientesCountProveedor(proveedor: Proveedor): Int {
        return facturas
            .count {
                (it.proveedorId == proveedor.id || it.proveedorNombre.equals(proveedor.nombre, ignoreCase = true)) &&
                        !it.esAnulada && !it.esTotalmentePagada
            }
    }

    // ── PRODUCTOS AGRUPADOS POR PROVEEDOR COMERCIAL ──
    val productosAgrupadosPorProveedor: Map<String, List<PharmProduct>>
        get() {
            val query = busquedaQuery.trim()
            val filtrados = if (query.isBlank()) todosLosProductos
            else todosLosProductos.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.proveedor.contains(query, ignoreCase = true) ||
                        it.laboratory.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.code.contains(query)
            }
            return filtrados.groupBy { resolverProveedorProducto(it) }
        }

    // ── PEDIDOS ACTIVOS EN PREPARACIÓN (UN PEDIDO POR PROVEEDOR) ──
    val pedidosActivosPorProveedor: List<PedidoProveedor>
        get() {
            val resultado = mutableListOf<PedidoProveedor>()
            pedidosPorProveedor.forEach { (provNombre, carro) ->
                val prodsProv = todosLosProductos.filter { resolverProveedorProducto(it) == provNombre }
                val provEntidad = proveedores.find { it.nombre.equals(provNombre, ignoreCase = true) }
                val items = prodsProv.mapNotNull { p ->
                    val cant = carro[p.id] ?: 0
                    if (cant > 0) {
                        ItemPedidoCompra(
                            productoId = p.id,
                            productoNombre = p.name,
                            presentacion = p.empaque.ifBlank { "Und" },
                            categoria = p.category,
                            codigo = p.code,
                            precioCompra = Math.round(p.purchasePrice * 100.0) / 100.0,
                            cantidad = cant
                        )
                    } else null
                }
                if (items.isNotEmpty()) {
                    resultado.add(
                        PedidoProveedor(
                            proveedorId = provEntidad?.id ?: provNombre,
                            proveedorNombre = provNombre,
                            proveedorRuc = provEntidad?.idFiscal ?: "",
                            proveedorTelefono = provEntidad?.telefono ?: "",
                            items = items
                        )
                    )
                }
            }
            return resultado.sortedByDescending { it.totalInversion }
        }

    // ── 2. DIRECTORIO DE PROVEEDORES ──
    val proveedorSeleccionado: Proveedor?
        get() = proveedores.find { it.id == proveedorSeleccionadoId } ?: proveedores.firstOrNull()

    val productosDelProveedorSeleccionado: List<PharmProduct>
        get() {
            val prov = proveedorSeleccionado ?: return emptyList()
            val idsDeFacturasDelProv = facturas
                .filter { it.proveedorId == prov.id || it.proveedorNombre.equals(prov.nombre, ignoreCase = true) }
                .flatMap { it.items }
                .map { it.productoId }
                .filter { it.isNotBlank() }
                .toSet()

            return todosLosProductos.filter { p ->
                idsDeFacturasDelProv.contains(p.id) ||
                        p.proveedor.equals(prov.nombre, ignoreCase = true) ||
                        p.laboratory.equals(prov.nombre, ignoreCase = true) ||
                        resolverProveedorProducto(p).equals(prov.nombre, ignoreCase = true)
            }
        }

    val proveedoresFiltrados: List<Proveedor>
        get() {
            if (busquedaQuery.isBlank()) return proveedores
            return proveedores.filter {
                it.nombre.contains(busquedaQuery, ignoreCase = true) ||
                        it.idFiscal.contains(busquedaQuery) ||
                        it.contacto.contains(busquedaQuery, ignoreCase = true) ||
                        it.telefono.contains(busquedaQuery)
            }
        }

    // ── 3. CUENTAS POR PAGAR (100% FINANCIERO) ──
    // "Hoy" según hora del servidor: las alertas de vencimiento jamás dependen del reloj del celular.
    private val hoy: Calendar get() = Calendar.getInstance().apply {
        timeInMillis = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val sdfFecha: SimpleDateFormat get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun esFacturaVencida(factura: FacturaCompra): Boolean {
        if (factura.esTotalmentePagada) return false
        if (factura.fechaVencimientoPago.isBlank()) return false
        return try {
            val fechaVenc = sdfFecha.parse(factura.fechaVencimientoPago) ?: return false
            fechaVenc.before(hoy.time)
        } catch (e: Exception) {
            false
        }
    }

    val facturasPendientes: List<FacturaCompra>
        get() = facturas.filter { !it.esAnulada && !it.esTotalmentePagada }

    val facturasVencidas: List<FacturaCompra>
        get() = facturasPendientes.filter { esFacturaVencida(it) }

    val facturasPagadas: List<FacturaCompra>
        get() = facturas.filter { it.esTotalmentePagada }

    val facturaSeleccionada: FacturaCompra?
        get() = facturas.find { it.id == facturaSeleccionadaId } ?: facturas.firstOrNull()

    val facturasFiltradas: List<FacturaCompra>
        get() {
            val base = when (filtroEstadoFactura) {
                "PENDIENTES" -> facturasPendientes
                "VENCIDAS" -> facturasVencidas
                "PAGADAS" -> facturasPagadas
                else -> facturas
            }
            if (busquedaQuery.isBlank()) return base
            return base.filter {
                it.numeroFactura.contains(busquedaQuery, ignoreCase = true) ||
                        it.proveedorNombre.contains(busquedaQuery, ignoreCase = true) ||
                        it.rucProveedor.contains(busquedaQuery)
            }
        }
}
