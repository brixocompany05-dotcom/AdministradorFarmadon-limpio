package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleUnidades
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configuración — REDISEÑO TOTAL Enterprise Quiet (2026)
 * Filosofía: no es un dashboard con hero + stepper pesado. Es Ajustes tranquilo:
 * cabecera tipográfica mínima, navegación silenciosa 32% | detalle 68% con aire,
 * un solo scroll padre, sin tarjetas que compiten, sin bordes gruesos, sin semáforos chillones.
 */
@Composable
fun ModuloConfiguracionPrincipal(
    product: MoldeProductos,
    movements: List<com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario> = emptyList(),
    ubicacionesDisponibles: List<String>,
    onGuardarConfiguracion: (ubicacion: String, stockMinimo: Double, activo: Boolean, diasAlertaVencimiento: Int, nuevoCodigo: String?, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onGenerarCodigoUnico: suspend () -> String = { "" },
    onVerificarDuplicadoCodigo: suspend (String) -> String? = { null },
    onMarcarEtiquetaImpresa: () -> Unit = {},
    isPrivileged: Boolean = true,
    onEliminarProducto: (motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onEliminadoExito: () -> Unit = {}
) {
    val s = recordarMedidaAdaptativa()
    val scope = rememberCoroutineScope()
    var seccion by remember { mutableStateOf(SeccionConfiguracion.UBICACION) }

    val codigoOriginal = remember(product.codigo) { product.codigo.trim() }
    var ubicacionState by remember(product.ubicacion) { mutableStateOf(product.ubicacion.trim()) }
    var stockMinimoState by remember(product.stockMinimoBase) { mutableStateOf(product.stockMinimoBase) }
    var diasState by remember(product.diasAlertaVencimiento) { mutableStateOf(if (product.diasAlertaVencimiento in 15..365) product.diasAlertaVencimiento else 90) }
    var activoState by remember(product.activo) { mutableStateOf(product.activo) }
    var codigoState by remember(codigoOriginal) { mutableStateOf(codigoOriginal) }
    var estadoAuto by remember { mutableStateOf(EstadoAutoGuardado.REPOSO) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    fun persistir(
        nuevaUbicacion: String = ubicacionState,
        nuevoStockMinimo: Double = stockMinimoState,
        nuevoActivo: Boolean = activoState,
        nuevosDias: Int = diasState,
        nuevoCodigo: String? = null
    ) {
        ubicacionState = nuevaUbicacion
        stockMinimoState = nuevoStockMinimo
        activoState = nuevoActivo
        diasState = nuevosDias
        if (nuevoCodigo != null) codigoState = nuevoCodigo.trim()
        estadoAuto = EstadoAutoGuardado.GUARDANDO
        mensajeError = null
        scope.launch {
            onGuardarConfiguracion(nuevaUbicacion, nuevoStockMinimo, nuevoActivo, nuevosDias, nuevoCodigo) { res ->
                if (res.isSuccess) {
                    estadoAuto = EstadoAutoGuardado.GUARDADO
                    scope.launch {
                        delay(2500)
                        if (estadoAuto == EstadoAutoGuardado.GUARDADO) estadoAuto = EstadoAutoGuardado.REPOSO
                    }
                } else {
                    estadoAuto = EstadoAutoGuardado.ERROR
                    mensajeError = res.exceptionOrNull()?.message ?: "Error al sincronizar."
                }
            }
        }
    }

    val unidadMenu = product.inventarioPerfilUnidadSingular.ifBlank { product.empaque }.ifBlank { product.unidadBase }.ifBlank { product.inventarioPerfilUnidadContenido }.ifBlank { "Unidad" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Cabecera quiet — tipográfica, no card
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nombre.ifBlank { "Producto" },
                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                    color = FDColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${product.categoriaPrincipal.ifBlank { "General" }}  ·  ${product.stockDisponibleUnidades.toInt()} ${product.empaque.ifBlank { "Unid" }}  ·  ${if (product.activo) "Activo" else "Pausado"}  ·  ${ubicacionState.ifBlank { "Sin ubicación" }}",
                    style = FDType.Caption.copy(fontSize = 11.5.sp, fontFamily = InterPremium),
                    color = FDColors.TextSecondary,
                    maxLines = 1
                )
            }
            AutoSaveBadge(estado = estadoAuto, mensajeError = mensajeError, onReintentar = { persistir() })
        }

        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f), thickness = 0.5.dp)

        // ── Cuerpo 32 | 68 — navegación quiet + detalle con scroll único
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ConfiguracionMenuLateral(
                seccionSeleccionada = seccion,
                onSeleccionarSeccion = { seccion = it },
                ubicacionActual = ubicacionState,
                stockMinimoActual = stockMinimoState,
                diasVencimientoActual = diasState,
                unidadStock = unidadMenu,
                isActivo = activoState,
                codigoActual = codigoState,
                modifier = Modifier.widthIn(min = 260.dp, max = 320.dp).weight(0.34f).fillMaxHeight()
            )

            // Panel derecho — una sola superficie, scroll padre, contenido sin scrolls anidados
            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(s.radiusCard))
                    .background(FDColors.Surface)
                    .padding(0.dp)
            ) {
                // Header del detalle — título + subtítulo de la sección, sin pastilla gritona
                val (tituloSec, descSec) = when (seccion) {
                    SeccionConfiguracion.UBICACION -> "Ubicación física" to "Pasillo, vitrina o zona donde se encuentra."
                    SeccionConfiguracion.STOCK_MINIMO -> "Stock mínimo" to "Umbral que dispara la alerta de reposición."
                    SeccionConfiguracion.ALERTA_VENCIMIENTO -> "Alerta por vencimiento" to "Con cuánta anticipación avisar por canje."
                    SeccionConfiguracion.ESTADO_OPERATIVO -> "Estado operativo" to "Si se muestra para vender en mostrador."
                    SeccionConfiguracion.CODIGO_BARRAS -> "Código y etiquetas" to "Generar, verificar e imprimir."
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = tituloSec,
                        style = FDType.Heading3.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                        color = FDColors.TextPrimary
                    )
                    Text(descSec, style = FDType.Caption.copy(fontSize = 11.5.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.30f), thickness = 0.5.dp)

                // Contenido scrolleable ÚNICO — los Panel* ya no abren su propio scroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (seccion) {
                        SeccionConfiguracion.UBICACION -> PanelUbicacionAlmacen(
                            ubicacionSeleccionada = ubicacionState,
                            onUbicacionChange = { persistir(nuevaUbicacion = it) },
                            ubicacionesDisponibles = ubicacionesDisponibles
                        )
                        SeccionConfiguracion.STOCK_MINIMO -> {
                            val total = product.stockDisponibleUnidades
                            PanelStockMinimo(
                                stockMinimoActual = stockMinimoState,
                                stockTotalActual = total,
                                unidadBase = unidadMenu,
                                onStockMinimoChange = { persistir(nuevoStockMinimo = it.coerceAtLeast(1.0)) }
                            )
                        }
                        SeccionConfiguracion.ALERTA_VENCIMIENTO -> PanelAlertaVencimiento(
                            diasVencimientoActual = diasState,
                            lotes = product.lotes,
                            onDiasVencimientoChange = { persistir(nuevosDias = it) }
                        )
                        SeccionConfiguracion.ESTADO_OPERATIVO -> {
                            PanelEstadoOperativo(isActivo = activoState, onActivoChange = { persistir(nuevoActivo = it) })
                            if (seccion == SeccionConfiguracion.ESTADO_OPERATIVO) {
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.30f), thickness = 0.5.dp)
                                PanelEliminarProducto(
                                    product = product,
                                    movements = movements,
                                    isPrivileged = isPrivileged,
                                    onEliminar = onEliminarProducto,
                                    onEliminadoExito = onEliminadoExito
                                )
                            }
                        }
                        SeccionConfiguracion.CODIGO_BARRAS -> PanelCodigoBarrasYEtiquetas(
                            producto = product.copy(
                                productoBase = product.productoBase.copy(codigo = codigoState),
                                loteInfo = product.loteInfo.copy(ubicacion = ubicacionState)
                            ),
                            onCodigoChange = { persistir(nuevoCodigo = it) },
                            onGenerarCodigoUnico = onGenerarCodigoUnico,
                            onVerificarDuplicadoCodigo = onVerificarDuplicadoCodigo,
                            onMarcarEtiquetaImpresa = onMarcarEtiquetaImpresa
                        )
                    }
                }
            }
        }
    }
}
