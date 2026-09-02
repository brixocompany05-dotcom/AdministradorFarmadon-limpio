package com.app.administradorfarmadon.inventario.compartido.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto

/**
 * FUENTE ÚNICA de verdad para convertir entre:
 *   - UNIDAD FÍSICA de inventario (lo que el empleado cuenta al recibir: caja, blíster, frasco)
 *   - UNIDAD INTERNA de contenido (cápsula, pastilla, ml, g… la unidad más pequeña que se vende)
 *
 * REGLA DE ORO:
 *   lote.cantidad                   = unidades FÍSICAS (cajas, frascos, etc.)
 *   PresentacionProducto.cantidad   = unidades de CONTENIDO en esa presentación (tabletas, mL, etc.)
 *   factorContenido                 = cantidad de la presentación mayor
 *
 * EJEMPLO COMPLETO — Panadol 180 Tab, stock 3 Cajas:
 *   Vender "Caja"    (cantidad=180) → 180÷180 = 1.0 caja descontada → quedan 2 Cajas (360 Tab)
 *   Vender "Tableta" (cantidad=1)   → 1÷180   = 0.006 caja descontada → quedan 2.994 Cajas (539 Tab)
 *
 * Ninguna pantalla vuelve a implementar este cálculo por su cuenta (cero copias, cero drift).
 */
object UnidadVentaHelper {

    /**
     * Nº de unidades internas que contiene una unidad física de inventario.
     * Prioridad:
     *  1) Presentación de mayor contenido (ej: "Caja de 100" → 100).
     *  2) Número en el texto de contenido del producto (ej: "100 cápsulas" → 100).
     *  3) 1 (producto que se cuenta y se vende en la misma unidad).
     */
    fun factorContenido(
        contenidoTexto: String?,
        presentaciones: List<PresentacionProducto>
    ): Double {
        // Unidad en la que habla el producto (L, ml, kg, g, Tab...). El usuario la ve así.
        val unidadProducto = (contenidoTexto?.let { extraerUnidadDeTexto(it) } ?: "").ifBlank {
            presentaciones.firstOrNull()?.unidadMedida ?: ""
        }
        // La presentación de mayor contenido, normalizada a la unidad del producto,
        // para que el factor sea coherente aunque mezclen L/ml o kg/g entre presentaciones.
        val mayorPresentacion = presentaciones.maxByOrNull { p ->
            PerfilUnidades.normalizarA(p.cantidad.toDouble(), p.unidadMedida, unidadProducto)
        }?.let { p ->
            PerfilUnidades.normalizarA(p.cantidad.toDouble(), p.unidadMedida, unidadProducto)
        } ?: 0.0
        if (mayorPresentacion > 1) return mayorPresentacion

        val numeroDelTexto = contenidoTexto
            ?.trim()
            ?.takeWhile { it.isDigit() || it == '.' || it == ',' }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
        if (numeroDelTexto != null && numeroDelTexto > 1.0) return numeroDelTexto

        return 1.0
    }

    /** Extrae la unidad si el texto de contenido la trae (ej: "1.5 Litros" → "Litros"). */
    private fun extraerUnidadDeTexto(texto: String): String {
        val partes = texto.trim().split(Regex("\\s+"))
        return if (partes.size > 1) partes.last() else ""
    }

    /**
     * Cuánto stock físico del lote consume una venta.
     * Ej: factor 100, vender 1 cápsula → 0.01 caja; vender 1 caja (100) → 1.0.
     */
    fun stockFisicoParaVender(cantidadUnidadesInternas: Double, factorContenido: Double): Double {
        if (cantidadUnidadesInternas <= 0.0) return 0.0
        val factor = factorContenido.coerceAtLeast(1.0)
        return (cantidadUnidadesInternas / factor).redondearA6()
    }

    /**
     * Costo real de una unidad interna a partir del costo de la unidad física.
     * Ej: caja $10 con 100 cápsulas → $0.10 cápsula. Siempre dividir, nunca multiplicar.
     */
    fun costoPorUnidadInterna(costoFisicoUnitario: Double, factorContenido: Double): Double {
        val factor = factorContenido.coerceAtLeast(1.0)
        return (costoFisicoUnitario / factor).redondearA6()
    }

    /**
     * Coherencia de fraccionamiento (R3):
     * Si se vende menos que una unidad física completa y el producto es sellado → inválido.
     */
    fun esFraccionCoherente(
        cantidadUnidadesInternas: Double,
        factorContenido: Double,
        permiteFraccionar: Boolean
    ): Boolean {
        if (cantidadUnidadesInternas <= 0.0) return false
        val factor = factorContenido.coerceAtLeast(1.0)
        val esFraccion = cantidadUnidadesInternas < factor
        return !esFraccion || permiteFraccionar
    }

    // ── DESCUENTO DE STOCK AL VENDER (cimiento para el módulo de ventas) ───

    /**
     * Qué lote y cuánto descontar de él.
     * El módulo de ventas aplica esta lista directamente a Firestore.
     */
    data class DescuentoLote(
        val loteId: String,
        val loteNumero: String,
        val cantidadADescontar: Double  // en unidades físicas (cajas)
    )

    /**
     * Calcula exactamente qué lotes descontar y cuánto, aplicando FEFO
     * (el que vence antes = sale primero), para una venta de [presentacion].
     *
     * @return Result.success(lista) si hay stock. Result.failure(mensaje claro) si no alcanza.
     *
     * EJEMPLO — Panadol 180 Tab. Lote A=1 caja (vence antes), Lote B=2 cajas:
     *   Vender "1 Tableta suelta" (cantidad=1):
     *     fisicoTotal = 1÷180 = 0.00556 → DescuentoLote(LoteA, 0.00556) ✅
     *   Vender "1 Caja" (cantidad=180):
     *     fisicoTotal = 1.0 → DescuentoLote(LoteA, 1.0) ✅
     *   Vender "2 Cajas" (cantidad=360):
     *     fisicoTotal = 2.0 → DescuentoLote(LoteA, 1.0) + DescuentoLote(LoteB, 1.0) ✅
     */
    fun calcularDescuentoFEFO(
        producto: MoldeProductos,
        presentacion: PresentacionProducto
    ): Result<List<DescuentoLote>> {
        val factor = factorContenido(producto.contenido, producto.presentaciones)
        // Unidad en la que "habla" el producto (L, ml, kg, g, Tab...). El usuario la ve así.
        val unidadProducto = producto.contenidoUnidad.ifBlank { producto.empaque }

        // Normalizar la presentación vendida a la unidad del producto (R3/cerebro 04):
        // si el producto está en "L" y venden "vaso 100 ml", convertir 100 ml → 0.1 L
        // antes de descontar. Así el consumo coincide física y matemáticamente sin importar
        // cómo escribió el usuario (L, ml, kg, g). El usuario nunca ve ml sueltos.
        val cantidadEnUnidadProducto = PerfilUnidades.normalizarA(
            presentacion.cantidad.toDouble(),
            presentacion.unidadMedida,
            unidadProducto
        )

        // 1. Validar que no se fraccione un producto sellado
        if (!esFraccionCoherente(cantidadEnUnidadProducto, factor, producto.permiteFraccionar)) {
            return Result.failure(
                Exception("Producto sellado: solo se vende en unidades completas.")
            )
        }

        val fisicoTotal = stockFisicoParaVender(cantidadEnUnidadProducto, factor)

        // 2. Validar stock suficiente en lotes VIGENTES (R3/Sanitaria: lotes vencidos nunca se venden)
        val vendibles = producto.lotes.values.filter { lote ->
            val dias = FechaVencimientoHelper.diasHastaVencer(lote.vencimiento)
            lote.cantidad > 0.0 && (dias == null || dias > 0)
        }
        val stockDisponible = vendibles.sumOf { it.cantidad.coerceAtLeast(0.0) }
        val stockVencido = producto.lotes.values.filter { lote ->
            val dias = FechaVencimientoHelper.diasHastaVencer(lote.vencimiento)
            dias != null && dias <= 0 && lote.cantidad > 0.0
        }.sumOf { it.cantidad }

        if (stockDisponible < fisicoTotal) {
            val disponibleContenido = (stockDisponible * factor).toLong()
            val unidad = producto.contenidoUnidad.ifBlank { producto.empaque.ifBlank { "unidades" } }
            val mensaje = if (stockDisponible <= 0.0 && stockVencido > 0.0) {
                "El stock de '${producto.nombre}' está vencido. Retíralo del anaquel (Inventario → Merma)."
            } else if (stockDisponible <= 0.0) {
                "Producto agotado."
            } else {
                "Stock insuficiente: quedan $disponibleContenido $unidad disponibles."
            }
            return Result.failure(Exception(mensaje))
        }

        // 3. ORDEN DE CONSUMO — CONTRATO PARA EL POS:
        //    a) PRIORIDAD DEL DUEÑO: si marcó un lote prioritario (con existencia y vigente), sale primero.
        //    b) Resto y sin prioridad: FEFO (primero vence, primero sale).
        //    El dueño decide su estrategia; el sistema la ejecuta sin preguntar en caja.
        val lotesEnOrden = run {
            val ordenados = vendibles.sortedBy { FechaVencimientoHelper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE }
            val prioId = producto.lotePrioritarioId.trim()
            val prioritario = ordenados.firstOrNull {
                it.loteId.equals(prioId, ignoreCase = true) || it.numero.equals(prioId, ignoreCase = true)
            }
            if (prioritario != null) listOf(prioritario) + ordenados.filter { it != prioritario } else ordenados
        }

        var restaPorDescontar = fisicoTotal
        val descuentos = mutableListOf<DescuentoLote>()

        for (lote in lotesEnOrden) {
            if (restaPorDescontar <= 0.0) break
            val descuentoEnEsteLote = minOf(lote.cantidad, restaPorDescontar).redondearA6()
            descuentos.add(
                DescuentoLote(
                    loteId = lote.loteId.ifBlank { FechaVencimientoHelper.llaveLote(lote.numero) },
                    loteNumero = lote.numero,
                    cantidadADescontar = descuentoEnEsteLote
                )
            )
            restaPorDescontar = (restaPorDescontar - descuentoEnEsteLote).redondearA6()
        }

        return Result.success(descuentos)
    }

    private fun Double.redondearA6(): Double =
        kotlin.math.round(this * 1_000_000.0) / 1_000_000.0
}