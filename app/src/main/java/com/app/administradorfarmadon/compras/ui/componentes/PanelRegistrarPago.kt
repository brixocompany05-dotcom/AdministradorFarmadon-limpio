package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle
import com.app.administradorfarmadon.compras.pagos.logica.EtiquetaMetodoPago
import com.app.administradorfarmadon.compras.pagos.logica.PagosMixtosEditorState
import com.app.administradorfarmadon.compras.pagos.ui.PagosMixtosEditor
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

/**
 * PANEL de registro de pago/abono a un proveedor (workspace, sin diálogos).
 * Izquierda: la persona elige métodos y escribe cuánto pagó con cada uno.
 * Derecha: resumen EN VIVO de cómo va quedando el pago + botón de guardado.
 */
@Composable
fun PanelRegistrarPago(
    factura: FacturaCompra,
    metodosPago: List<InstanciaPago> = emptyList(),
    procesando: Boolean = false,
    estadoFactura: String? = null,
    onVolver: () -> Unit,
    onGuardar: (
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        pagos: List<PagoDetalle>
    ) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val colores = TokensFarmadon.colores
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    val esAnulada = estadoFactura.equals("ANULADA", ignoreCase = true)
    val saldoPendiente = if (esAnulada) 0.0 else factura.saldoPendienteReal

    BackHandler { if (!procesando) onVolver() }

    val opcionesMetodo = remember(metodosPago) {
        metodosPago.filter { EtiquetaMetodoPago.esValidaParaProveedor(it) }
            .map { EtiquetaMetodoPago.deInstancia(it) }
    }
    val sinMetodosReales = opcionesMetodo.isEmpty()

    val estadoEditor = remember(factura.id, opcionesMetodo) {
        PagosMixtosEditorState(
            opcionesMetodo = opcionesMetodo,
            pagosIniciales = emptyList(),
            montoMaximo = saldoPendiente
        )
    }

    // H3: el tope vive con la factura. Si otra persona abona mientras este panel
    // está abierto, el máximo se actualiza solo (nunca se muestra un límite viejo).
    LaunchedEffect(saldoPendiente) {
        estadoEditor.actualizarMontoMaximo(saldoPendiente)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.fondoBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val ancho = maxWidth.value
        val padH = s.padScreenH
        val padV = s.padScreenV
        val gapColumnas = s.gapColumnas(ancho)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                IconButton(
                    onClick = { if (!procesando) onVolver() },
                    modifier = Modifier
                        .size(s.btnSmallH)
                        .clip(RoundedCornerShape(s.radiusButton))
                        .background(colores.cardElevada)
                        .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusButton))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "REGISTRAR PAGO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontFamily = InterPremium
                        ),
                        color = colores.textoTerciario
                    )
                    Text(
                        "Factura ${factura.numeroFactura} · ${factura.proveedorNombre}",
                        style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp, fontFamily = InterPremium),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = colores.cardElevada,
                    shape = RoundedCornerShape(s.radiusChip),
                    border = BorderStroke(s.borderWidth, colores.cardBorde)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = s.padCard, vertical = s.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Icon(Icons.Default.Payments, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                        Text(
                            "SALDO: $simboloMoneda " + String.format(Locale.US, "%.2f", saldoPendiente),
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterPremium
                            ),
                            color = colores.textoPrincipal
                        )
                    }
                }
            }

            // Área de trabajo: IZQUIERDA (elegir y rellenar) | DERECHA (resumen en vivo)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapColumnas)
            ) {
                // ── Panel izquierdo: cómo se pagó ──
                Surface(
                    color = colores.cardBase,
                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                    border = BorderStroke(s.borderWidth, colores.cardBorde),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(s.padCardLarge),
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        if (esAnulada) {
                            Surface(
                                color = colores.peligroSutil,
                                shape = RoundedCornerShape(s.radiusChip),
                                border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Esta factura fue anulada. No se puede registrar un pago.",
                                    modifier = Modifier.padding(s.padCard),
                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                        fontSize = s.textBody.value.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterPremium
                                    ),
                                    color = colores.estadoPeligro
                                )
                            }
                        } else if (sinMetodosReales) {
                            Surface(
                                color = colores.peligroSutil,
                                shape = RoundedCornerShape(s.radiusChip),
                                border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "No hay métodos de pago activos para proveedores. Activa uno en Configuración → Métodos de Pago.",
                                    modifier = Modifier.padding(s.padCard),
                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                        fontSize = s.textBody.value.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterPremium
                                    ),
                                    color = colores.estadoPeligro
                                )
                            }
                        } else {
                            PagosMixtosEditor(estado = estadoEditor, soloLectura = procesando)
                        }
                    }
                }

                // ── Panel derecho: cómo va quedando (resumen en vivo) ──
                Surface(
                    color = colores.cardElevada,
                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                    border = BorderStroke(s.borderWidth, colores.cardBorde),
                    modifier = Modifier.width(s.anchoRiel(ancho)).fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(s.padCardLarge),
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        // Contenido con scroll independiente: nunca se aprieta al crecer.
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            Text(
                                "CÓMO VA QUEDANDO",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontSize = s.textLabel.value.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontFamily = InterPremium
                                ),
                                color = colores.textoTerciario
                            )

                            FilaResumen("Saldo de la factura", "$simboloMoneda " + String.format(Locale.US, "%.2f", saldoPendiente), s, colores)

                            val pagado = estadoEditor.sumaPorciones
                            FilaResumen(
                                "Pagado ahora",
                                "$simboloMoneda " + String.format(Locale.US, "%.2f", pagado),
                                s,
                                colores,
                                color = colores.estadoExito
                            )

                            val queda = (saldoPendiente - pagado).coerceAtLeast(0.0)
                            FilaResumen(
                                "Queda por pagar",
                                "$simboloMoneda " + String.format(Locale.US, "%.2f", queda),
                                s,
                                colores,
                                color = if (queda <= 0.01 && pagado > 0.0) colores.estadoExito else colores.estadoAlerta
                            )

                            HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH)

                            Text(
                                "DETALLE",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontSize = s.textLabel.value.sp * 0.9f,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    fontFamily = InterPremium
                                ),
                                color = colores.textoTerciario
                            )
                            if (estadoEditor.pagos.isEmpty()) {
                                Text(
                                    "Todavía no indicaste ningún método.",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                        fontSize = s.textBody.value.sp * 0.92f,
                                        fontFamily = InterPremium
                                    ),
                                    color = colores.textoTerciario
                                )
                            } else {
                                estadoEditor.pagos.forEach { pago ->
                                    Surface(
                                        color = colores.cardBase,
                                        shape = RoundedCornerShape(s.radiusChip),
                                        border = BorderStroke(s.borderWidth, colores.cardBorde),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    EtiquetaMetodoPago.nombreCorto(pago.metodoPago),
                                                    style = TokensFarmadon.tipografia.titulo3.copy(
                                                        fontSize = s.textBody.value.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = InterPremium
                                                    ),
                                                    color = colores.textoPrincipal,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                if (pago.numeroOperacion.isNotBlank()) {
                                                    Text(
                                                        "Operación: ${pago.numeroOperacion}",
                                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                            fontSize = s.textLabel.value.sp * 0.85f,
                                                            fontFamily = InterPremium
                                                        ),
                                                        color = colores.textoTerciario
                                                    )
                                                }
                                            }
                                            Text(
                                                "$simboloMoneda " + String.format(Locale.US, "%.2f", pago.monto),
                                                style = TokensFarmadon.tipografia.titulo3.copy(
                                                    fontSize = s.textBody.value.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = InterPremium
                                                ),
                                                color = colores.textoPrincipal
                                            )
                                        }
                                    }
                                }
                            }

                            if (estadoEditor.algunaPorcionExcede || estadoEditor.sumaExcedeTotal) {
                                Surface(
                                    color = colores.peligroSutil,
                                    shape = RoundedCornerShape(s.radiusChip),
                                    border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        estadoEditor.estadoVerificacion,
                                        modifier = Modifier.padding(s.padCard),
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                            fontSize = s.textBody.value.sp * 0.9f,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterPremium
                                        ),
                                        color = colores.estadoPeligro
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val pagos = estadoEditor.pagos
                                onGuardar(
                                    pagos.sumOf { it.monto },
                                    pagos.firstOrNull()?.metodoPago ?: "",
                                    pagos.firstOrNull()?.numeroOperacion ?: "",
                                    pagos
                                )
                            },
                            enabled = !procesando && !esAnulada && estadoEditor.cuadra,
                            modifier = Modifier.fillMaxWidth().height(s.btnLargeH),
                            shape = RoundedCornerShape(s.radiusButton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colores.botonPrimarioFondo,
                                contentColor = colores.botonPrimarioTexto,
                                disabledContainerColor = colores.textoPrincipal.copy(alpha = 0.06f),
                                disabledContentColor = colores.textoTerciario.copy(alpha = 0.4f)
                            )
                        ) {
                            if (procesando) {
                                CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = colores.botonPrimarioTexto, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(s.iconSmall))
                                Spacer(Modifier.width(s.xs))
                                Text(
                                    "GUARDAR PAGO",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontSize = s.textLabel.value.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                        fontFamily = InterPremium
                                    )
                                )
                            }
                        }
                        Text(
                            when {
                                procesando -> "Guardando el pago… no cierres la pantalla."
                                sinMetodosReales -> "Activa un método de pago para continuar."
                                estadoEditor.cuadra -> "Listo para guardar."
                                estadoEditor.filas.isEmpty() -> "Elige al menos un método de pago."
                                else -> "Falta escribir el monto de algún método."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                fontSize = s.textLabel.value.sp * 0.9f,
                                fontFamily = InterPremium
                            ),
                            color = colores.textoTerciario
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaResumen(
    etiqueta: String,
    valor: String,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon,
    color: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            etiqueta,
            style = TokensFarmadon.tipografia.cuerpo.copy(
                fontSize = s.textBody.value.sp,
                fontFamily = InterPremium
            ),
            color = colores.textoSecundario
        )
        Text(
            valor,
            style = TokensFarmadon.tipografia.titulo3.copy(
                fontSize = s.textBody.value.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterPremium
            ),
            color = color ?: colores.textoPrincipal
        )
    }
}
