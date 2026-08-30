package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.logica

import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct

/**
 * Lógica pura de cálculo de alertas —” sin dependencias de Android/Compose.
 * Responsable de analizar inventario y generar alertas automáticas.
 */
object InventarioAlertasLogic {

    data class AlertaProducto(
        val productId: String,
        val productName: String,
        val tipo: TipoAlerta,
        val mensaje: String,
        val severidad: Severidad,
        val valorActual: String,
        val valorReferencia: String
    )

    enum class TipoAlerta {
        STOCK_AGOTADO,
        STOCK_BAJO,
        VENCIMIENTO_CRITICO,   // menos de 30 días
        VENCIMIENTO_PROXIMO,   // 30-90 días
        VENCIDO,
        MARGEN_BAJO
    }

    enum class Severidad {
        CRITICA, ALTA, MEDIA;
    }

    fun calcularAlertas(productos: List<PharmProduct>): List<AlertaProducto> {
        val hoy = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
        val alertas = mutableListOf<AlertaProducto>()

        productos.forEach { p ->
            // ──”€──”€ ALERTAS DE STOCK ──”€──”€
            if (p.stock <= 0) {
                alertas.add(
                    AlertaProducto(
                        productId = p.id,
                        productName = p.name,
                        tipo = TipoAlerta.STOCK_AGOTADO,
                        mensaje = "Sin stock disponible",
                        severidad = Severidad.CRITICA,
                        valorActual = "0 uds",
                        valorReferencia = "Mín: ${p.minStock} uds"
                    )
                )
            } else if (p.minStock > 0 && p.stock <= p.minStock) {
                alertas.add(
                    AlertaProducto(
                        productId = p.id,
                        productName = p.name,
                        tipo = TipoAlerta.STOCK_BAJO,
                        mensaje = if (p.stock == p.minStock) "Stock en nivel mínimo (${p.minStock} uds)" else "Stock por debajo del mínimo",
                        severidad = Severidad.ALTA,
                        valorActual = "${p.stock} uds",
                        valorReferencia = "Mín: ${p.minStock} uds"
                    )
                )
            }

            // ──”€──”€ ALERTAS DE VENCIMIENTO ──”€──”€
            if (p.expiryTimestamp > 0L) {
                val diasRestantes = ((p.expiryTimestamp - hoy) / 86_400_000).toInt()
                when {
                    diasRestantes < 0 -> alertas.add(
                        AlertaProducto(
                            productId = p.id,
                            productName = p.name,
                            tipo = TipoAlerta.VENCIDO,
                            mensaje = "Producto vencido hace ${-diasRestantes} días",
                            severidad = Severidad.CRITICA,
                            valorActual = p.expiryDate,
                            valorReferencia = "Vencido"
                        )
                    )
                    diasRestantes <= 30 -> alertas.add(
                        AlertaProducto(
                            productId = p.id,
                            productName = p.name,
                            tipo = TipoAlerta.VENCIMIENTO_CRITICO,
                            mensaje = "Vence en $diasRestantes días",
                            severidad = Severidad.CRITICA,
                            valorActual = p.expiryDate,
                            valorReferencia = "< 30 días"
                        )
                    )
                    diasRestantes <= 90 -> alertas.add(
                        AlertaProducto(
                            productId = p.id,
                            productName = p.name,
                            tipo = TipoAlerta.VENCIMIENTO_PROXIMO,
                            mensaje = "Vence en $diasRestantes días",
                            severidad = Severidad.ALTA,
                            valorActual = p.expiryDate,
                            valorReferencia = "< 90 días"
                        )
                    )
                }
            }
        }

        // Ordenar: CRITICA primero, luego ALTA, luego MEDIA
        return alertas.sortedBy { it.severidad.ordinal }
    }

    fun contarPorSeveridad(alertas: List<AlertaProducto>): Map<Severidad, Int> =
        alertas.groupBy { it.severidad }.mapValues { it.value.size }

    fun contarPorTipo(alertas: List<AlertaProducto>): Map<TipoAlerta, Int> =
        alertas.groupBy { it.tipo }.mapValues { it.value.size }
}

