package com.app.administradorfarmadon.compras.logica

/**
 * Estados formales y canónicos de una orden de compra hacia un proveedor.
 */
enum class EstadoPedidoCompra(val id: String, val etiqueta: String) {
    BORRADOR("BORRADOR", "Borrador"),
    ENVIADO("ENVIADO", "En camino"),
    ENTREGA_PARCIAL("ENTREGA_PARCIAL", "Entrega Parcial"),
    RECIBIDO("RECIBIDO", "Recibido Conforme"),
    COMPLETADA_AJUSTE("COMPLETADA_AJUSTE", "Cerrado con Ajuste"),
    CANCELADO("CANCELADO", "Cancelado");

    companion object {
        fun desdeString(valor: String): EstadoPedidoCompra {
            return entries.firstOrNull { it.id.equals(valor.trim(), ignoreCase = true) } ?: ENVIADO
        }
    }
}

/**
 * MÁQUINA DE ESTADOS FORMAL · CICLO DE VIDA DE PEDIDOS DE COMPRA
 *
 * Flujo canónico:
 * BORRADOR
 *    ↓
 * ENVIADO ──────────────┬──────────────┐
 *    ↓                  │              │
 * ENTREGA_PARCIAL ──────┤              │
 *    ↓                  ↓              ↓
 * RECIBIDO      COMPLETADA_AJUSTE   CANCELADO
 * (COMPLETADA_AJUSTE también se alcanza directo desde ENVIADO cuando el
 * descarte por producto entierra todo lo pendiente antes de la primera entrega)
 *    ↓                  ↓              ↓
 * └─────────────────────┴──────────────┘
 *                       ↓
 *                   HISTORIAL (Estados Terminales Inmutables)
 *
 * Reglas:
 * 1. Los estados terminales (RECIBIDO, COMPLETADA_AJUSTE, CANCELADO) son inmutables.
 * 2. Ninguna orden terminal puede volver a recibir, cancelar ni transicionar.
 * 3. Las acciones de UI se derivan exclusivamente del estado real vigente.
 */
object MaquinaEstadosPedido {

    /**
     * Valida si una transición de estado de [actual] hacia [nuevo] es legal.
     */
    fun puedeTransicionar(actual: String, nuevo: String): Boolean {
        val origen = EstadoPedidoCompra.desdeString(actual)
        val destino = EstadoPedidoCompra.desdeString(nuevo)

        if (origen == destino) {
            // Entrega parcial puede recibir otra entrega parcial consecutiva
            return origen == EstadoPedidoCompra.ENTREGA_PARCIAL
        }

        return when (origen) {
            EstadoPedidoCompra.BORRADOR -> destino in listOf(
                EstadoPedidoCompra.ENVIADO,
                EstadoPedidoCompra.CANCELADO
            )
            EstadoPedidoCompra.ENVIADO -> destino in listOf(
                EstadoPedidoCompra.ENTREGA_PARCIAL,
                EstadoPedidoCompra.RECIBIDO,
                EstadoPedidoCompra.COMPLETADA_AJUSTE,
                EstadoPedidoCompra.CANCELADO
            )
            EstadoPedidoCompra.ENTREGA_PARCIAL -> destino in listOf(
                EstadoPedidoCompra.ENTREGA_PARCIAL,
                EstadoPedidoCompra.RECIBIDO,
                EstadoPedidoCompra.COMPLETADA_AJUSTE
            )
            // Estados terminales: inmutables. Toda transición posterior está prohibida.
            EstadoPedidoCompra.RECIBIDO -> false
            EstadoPedidoCompra.COMPLETADA_AJUSTE -> false
            EstadoPedidoCompra.CANCELADO -> false
        }
    }

    /**
     * Determina si la orden puede recibir mercadería física.
     * Solo órdenes en camino (ENVIADO o ENTREGA_PARCIAL) pueden recibir.
     */
    fun puedeRecibir(estado: String): Boolean {
        val st = EstadoPedidoCompra.desdeString(estado)
        return st == EstadoPedidoCompra.ENVIADO || st == EstadoPedidoCompra.ENTREGA_PARCIAL
    }

    /**
     * Determina si la orden puede ser cancelada.
     * Solo órdenes enviadas que no han recibido ningún paquete físico pueden cancelarse.
     */
    fun puedeCancelar(estado: String, recepcionesCount: Int = 0): Boolean {
        val st = EstadoPedidoCompra.desdeString(estado)
        return st == EstadoPedidoCompra.ENVIADO && recepcionesCount == 0
    }

    /**
     * Determina si la orden puede cerrarse con ajuste por quiebre de stock en droguería.
     * Solo aplica si ya hubo al menos una entrega parcial y quedan faltantes que nunca llegarán.
     */
    fun puedeCerrarConAjuste(estado: String, recepcionesCount: Int = 1): Boolean {
        val st = EstadoPedidoCompra.desdeString(estado)
        return st == EstadoPedidoCompra.ENTREGA_PARCIAL && recepcionesCount > 0
    }

    /**
     * Filtro oficial para la pestaña "Pedido a Proveedor / En camino".
     * Solo órdenes activas que esperan entregas.
     */
    fun perteneceAEnCamino(estado: String): Boolean {
        val st = EstadoPedidoCompra.desdeString(estado)
        return st == EstadoPedidoCompra.ENVIADO || st == EstadoPedidoCompra.ENTREGA_PARCIAL
    }

    /**
     * Filtro oficial para la pestaña "Historial de Pedidos".
     * Incluye órdenes concluidas (recibidas, con ajuste, canceladas) Y parciales,
     * que aparecen como "pendientes por completar". La parcial sigue viva en
     * "En camino" para recibir; aquí deja rastro auditable de lo ya llegado.
     */
    fun perteneceAHistorial(estado: String): Boolean {
        val st = EstadoPedidoCompra.desdeString(estado)
        return st in listOf(
            EstadoPedidoCompra.ENTREGA_PARCIAL,
            EstadoPedidoCompra.RECIBIDO,
            EstadoPedidoCompra.COMPLETADA_AJUSTE,
            EstadoPedidoCompra.CANCELADO
        )
    }
}
