package com.app.administradorfarmadon.compras.ui.componentes.reposicion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun TarjetaPedidoEnviado(
    pedido: PedidoCompra,
    simboloMoneda: String,
    onRecibirMercaderia: () -> Unit,
    onCerrarConAjuste: () -> Unit,
    onDescartarProducto: (String) -> Unit = {},
    onCancelar: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    var expandirHistorial by remember { mutableStateOf(false) }
    var expandirPendientes by remember { mutableStateOf(false) }
    var confirmarAjuste by remember { mutableStateOf(false) }
    var confirmarCancelacion by remember { mutableStateOf(false) }
    var productoParaDescartar by remember { mutableStateOf<ItemPedidoCompra?>(null) }

    val productosPendientes = pedido.items.filter { it.saldoPendiente > 0 }
    val unidadesPendientes = productosPendientes.sumOf { it.saldoPendiente }

    Surface(
        color = FDColors.SurfaceElevated.copy(alpha = 0.4f),
        shape = FDShapes.Medium,
        border = BorderStroke(s.borderWidth, FDColors.Border.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = pedido.proveedorNombre.uppercase(),
                        style = FDType.Body.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = (if (pedido.estado == "ENTREGA_PARCIAL") "PARCIAL DESDE" else "ENVIADO") +
                                ": ${pedido.fechaEmision} | ${pedido.items.size} ÍTEMS",
                        style = FDType.Label.copy(fontSize = 9.sp),
                        color = FDColors.TextTertiary
                    )
                }

                Surface(
                    color = FDColors.Primary.copy(alpha = 0.08f),
                    shape = FDShapes.XSmall
                ) {
                    Text(
                        text = if (pedido.estado == "ENTREGA_PARCIAL") "ENTREGA PARCIAL" else "ESPERANDO ENTREGA",
                        style = FDType.Label.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

            // Historial de Recepciones (Acordeón de Trazabilidad)
            if (pedido.recepciones.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(s.radiusInput * 0.75f))
                        .background(FDColors.TextPrimary.copy(alpha = 0.02f))
                        .border(
                            0.6.dp,
                            FDColors.Border.copy(alpha = 0.5f),
                            RoundedCornerShape(s.radiusInput * 0.75f)
                        )
                        .clickable { expandirHistorial = !expandirHistorial }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Trazabilidad: ${pedido.recepciones.size} entrega(s) física(s)",
                                style = FDType.Label.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (expandirHistorial) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = expandirHistorial) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                        ) {
                            pedido.recepciones.forEach { rec ->
                                Surface(
                                    color = FDColors.Surface,
                                    shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                    border = BorderStroke(
                                        s.borderWidth * 0.6f,
                                        FDColors.Border
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Factura: ${rec.numeroFactura} · Entrega: $simboloMoneda ${
                                                String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    rec.items.sumOf { it.costoTotalReal })
                                            }",
                                            style = FDType.Label.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FDColors.TextPrimary
                                        )
                                        Text(
                                            text = "Fecha: ${rec.fechaLegible}",
                                            style = FDType.BodySmall.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

            // Faltantes de la orden: visibles, contados y descartables uno por uno
            if (productosPendientes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(s.radiusInput * 0.75f))
                        .background(FDColors.TextPrimary.copy(alpha = 0.02f))
                        .border(
                            0.6.dp,
                            FDColors.Border.copy(alpha = 0.5f),
                            RoundedCornerShape(s.radiusInput * 0.75f)
                        )
                        .clickable { expandirPendientes = !expandirPendientes }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                tint = FDColors.Warning,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Faltan ${productosPendientes.size} producto(s) · $unidadesPendientes und.",
                                style = FDType.Label.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.Warning
                            )
                        }
                        Icon(
                            imageVector = if (expandirPendientes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = expandirPendientes) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            productosPendientes.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productoNombre,
                                            style = FDType.BodySmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Quedan ${item.saldoPendiente} de ${item.cantidad} pedidas",
                                            style = FDType.BodySmall.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                    IconButton(
                                        onClick = { productoParaDescartar = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RemoveShoppingCart,
                                            "Descartar faltante",
                                            tint = FDColors.Warning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

            // Footer de Acción Industrial
            FDBotonPrimario(
                texto = "INGRESAR MERCADERÍA A STOCK",
                onClick = onRecibirMercaderia,
                icono = Icons.Default.Inventory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(s.btnSmallH)
            )

            // UNA sola salida según la verdad de esta orden (jamás dos juntas):
            // nada recibido todavía CANCELAR ORDEN ya llegó algo CERRAR CON AJUSTE.
            if (pedido.estado == "ENVIADO") {
                OutlinedButton(
                    onClick = { confirmarCancelacion = true },
                    shape = RoundedCornerShape(s.radiusInput),
                    border = BorderStroke(s.borderWidth, FDColors.Warning.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Warning),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnSmallH * 0.95f)
                ) {
                    Text(
                        "CANCELAR ORDEN",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                FDBotonSecundario(
                    texto = "CERRAR CON AJUSTE",
                    onClick = { confirmarAjuste = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnSmallH * 0.95f)
                )
            }
        }
    }

    DialogosPedidoEnviado(
        proveedorNombre = pedido.proveedorNombre,
        unidadesPendientes = unidadesPendientes,
        confirmarAjuste = confirmarAjuste,
        confirmarCancelacion = confirmarCancelacion,
        productoParaDescartar = productoParaDescartar,
        s = s,
        onConfirmarAjuste = { confirmarAjuste = false; onCerrarConAjuste() },
        onCancelarAjuste = { confirmarAjuste = false },
        onConfirmarCancelacion = { confirmarCancelacion = false; onCancelar() },
        onCancelarCancelacion = { confirmarCancelacion = false },
        onConfirmarDescartar = { idProd ->
            productoParaDescartar = null
            onDescartarProducto(idProd)
        },
        onCancelarDescartar = { productoParaDescartar = null }
    )
}

