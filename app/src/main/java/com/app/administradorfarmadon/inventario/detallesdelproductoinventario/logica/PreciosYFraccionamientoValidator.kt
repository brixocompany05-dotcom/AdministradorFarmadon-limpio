package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica

import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import java.util.Locale

/**
 * Validador puro de reglas de negocio para políticas de precios y fraccionamiento.
 * Cero dependencias de UI. Fuente única de verdad matemática.
 */
object PreciosYFraccionamientoValidator {

    data class ErrorFila(
        val presentacionId: String,
        val mensaje: String,
        val esBloqueante: Boolean // true = no permite guardar, false = advertencia informativa
    )

    data class ResultadoValidacion(
        val esValidoParaGuardar: Boolean,
        val erroresPorId: Map<String, List<ErrorFila>> = emptyMap(),
        val hayVentaAPerdida: Boolean = false,
        val hayNombresDuplicados: Boolean = false
    )

    fun validar(
        presentaciones: List<PresentacionProducto>,
        costoBaseUnitario: Double,
        limiteContenidoMaestro: Int = 0,
        permiteFraccionar: Boolean = true
    ): ResultadoValidacion {
        if (presentaciones.isEmpty()) {
            return ResultadoValidacion(esValidoParaGuardar = false)
        }

        val erroresMap = mutableMapOf<String, MutableList<ErrorFila>>()
        val nombresRegistrados = mutableSetOf<String>()
        var hayBloqueante = false
        var hayPerdida = false
        var hayDuplicados = false

        val paqueteMayor = presentaciones.maxByOrNull { it.cantidad }
        val precioUnitarioReferencia = if (paqueteMayor != null && paqueteMayor.cantidad > 0 && paqueteMayor.precioventa > 0) {
            paqueteMayor.precioventa / paqueteMayor.cantidad
        } else 0.0

        for (pres in presentaciones) {
            val listaErrores = mutableListOf<ErrorFila>()
            val nombreLimpio = pres.nombre.trim().lowercase()

            // A. Validación de Nombre
            if (pres.nombre.isBlank()) {
                listaErrores.add(ErrorFila(pres.presentacionId, "El nombre no puede estar vacío", esBloqueante = true))
                hayBloqueante = true
            } else if (nombresRegistrados.contains(nombreLimpio)) {
                listaErrores.add(ErrorFila(pres.presentacionId, "Nombre duplicado: ya existe otra presentación llamada '${pres.nombre.trim()}'", esBloqueante = true))
                hayBloqueante = true
                hayDuplicados = true
            } else {
                nombresRegistrados.add(nombreLimpio)
            }

            // B. Validación de Contenido / Factor
            if (pres.cantidad <= 0) {
                listaErrores.add(ErrorFila(pres.presentacionId, "El contenido debe ser mínimo 1", esBloqueante = true))
                hayBloqueante = true
            } else if (limiteContenidoMaestro > 1 && pres.cantidad > limiteContenidoMaestro) {
                // Candado de la matemática de stock (UnidadVentaHelper): el factor de
                // conversión ES la presentación mayor; si una presentación supera el
                // contenido del envase recibido, cada venta descontaría fracciones
                // fantasmas y el stock físico dejaría de cuadrar. Se bloquea en raíz.
                listaErrores.add(ErrorFila(
                    pres.presentacionId,
                    "'${pres.nombre.ifBlank { "Esta presentación" }}' tiene ${pres.cantidad} y el envase completo trae $limiteContenidoMaestro. Una presentación no puede superar al envase; si vendes packs, registra esa entrada como envases aparte.",
                    esBloqueante = true
                ))
                hayBloqueante = true
            } else if (limiteContenidoMaestro > 1 && pres.cantidad == 1 && (pres.empaque.equals("Caja", ignoreCase = true) || pres.empaque.equals("Frasco", ignoreCase = true))) {
                listaErrores.add(ErrorFila(
                    pres.presentacionId,
                    "Aviso: Has puesto '${pres.empaque}' con 1 unidad. Si entregas el envase completo, pon $limiteContenidoMaestro. Si es venta suelta, cambia el envase a 'Unidad' o 'Pastilla'.",
                    esBloqueante = false
                ))
            }
            // B2. Coherencia envase/unidad
            // B2b. Fraccionamiento bloqueado si el producto es sellado
            if (!permiteFraccionar && limiteContenidoMaestro > 1 && pres.cantidad < limiteContenidoMaestro && pres.cantidad > 0) {
                listaErrores.add(ErrorFila(
                    pres.presentacionId,
                    "Producto sellado: no se puede vender fraccionado (${pres.cantidad} < $limiteContenidoMaestro). Crea uno a granel si necesitas fraccionar.",
                    esBloqueante = true
                ))
                hayBloqueante = true
            }
            if (pres.empaque.isNotBlank() && pres.unidadMedida.isNotBlank()) {
                val famPres = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.detectarFamiliaFisica(pres.empaque, pres.unidadMedida)
                val empOkP = famPres.empaquesCompatibles.any { it.equals(pres.empaque, ignoreCase = true) }
                val uniOkP = famPres.unidadesCompatibles.any { it.equals(pres.unidadMedida, ignoreCase = true) }
                if (!empOkP || !uniOkP) {
                    // Nace compatible: si la ficha nació bien, esta incoherencia es error de tipeo y debe bloquear
                    listaErrores.add(ErrorFila(
                        pres.presentacionId,
                        "Envase '${pres.empaque}' no combina con unidad '${pres.unidadMedida}'. Usa: ${famPres.empaquesCompatibles.take(3).joinToString(", ")}",
                        esBloqueante = true
                    ))
                    hayBloqueante = true
                }
            }

            // C. Validación de Precio al Público
            if (pres.precioventa <= 0.0) {
                listaErrores.add(ErrorFila(pres.presentacionId, "Debes asignar un precio de venta mayor a $0.00", esBloqueante = true))
                hayBloqueante = true
            } else {
                // D. Validación de Venta a Pérdida
                val costoUnitarioFisico = if (limiteContenidoMaestro > 1) {
                    costoBaseUnitario / limiteContenidoMaestro
                } else {
                    costoBaseUnitario
                }
                val costoProporcional = costoUnitarioFisico * pres.cantidad
                if (costoProporcional > 0 && pres.precioventa < costoProporcional) {
                    val perdida = costoProporcional - pres.precioventa
                    listaErrores.add(ErrorFila(
                        pres.presentacionId,
                        "Venta a pérdida: Margen negativo (-$ ${String.format("%.2f", perdida)})",
                        esBloqueante = false
                    ))
                    hayPerdida = true
                }

                // E. Validación de Coherencia Comercial (Fracción rinde menos que paquete mayor)
                val precioUnitario = pres.precioventa / pres.cantidad.coerceAtLeast(1)
                if (pres.presentacionId != paqueteMayor?.presentacionId && precioUnitarioReferencia > 0 && precioUnitario < precioUnitarioReferencia) {
                    listaErrores.add(ErrorFila(
                        pres.presentacionId,
                        "Precio unitario ($ ${String.format("%.2f", precioUnitario)}) menor al paquete mayor ($ ${String.format("%.2f", precioUnitarioReferencia)})",
                        esBloqueante = false
                    ))
                }

                // F. Validación de Monotonía Jerárquica (Un paquete más grande no puede costar menos o igual que una fracción menor)
                for (otra in presentaciones) {
                    if (pres.presentacionId != otra.presentacionId && pres.cantidad > otra.cantidad && pres.precioventa <= otra.precioventa && pres.precioventa > 0 && otra.precioventa > 0) {
                        listaErrores.add(ErrorFila(
                            pres.presentacionId,
                            "Inconsistencia: Contiene más unidades (${pres.cantidad}) pero cuesta igual o menos que '${otra.nombre.ifBlank { "otra presentación" }}' —” revisa promoción",
                            esBloqueante = false
                        ))
                    }
                }
            }

            if (listaErrores.isNotEmpty()) {
                erroresMap[pres.presentacionId] = listaErrores
            }
        }

        return ResultadoValidacion(
            esValidoParaGuardar = !hayBloqueante,
            erroresPorId = erroresMap,
            hayVentaAPerdida = hayPerdida,
            hayNombresDuplicados = hayDuplicados
        )
    }
}
