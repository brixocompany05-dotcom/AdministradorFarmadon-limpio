package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.configuracion.plan.datos.HistorialPagoItem
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import java.util.Locale

@Composable
fun ConstanciaPagoDialog(
    pago: HistorialPagoItem,
    nombreFarmacia: String,
    rucFarmacia: String,
    emisorRazonSocial: String = "",
    emisorRuc: String = "",
    onDismiss: () -> Unit
) {
    val colores = TokensFarmadon.colores
    val clipboardManager = LocalClipboardManager.current
    var copiadoExitoso by remember { mutableStateOf(false) }

    val emisorTexto = if (emisorRazonSocial.isNotBlank()) {
        "$emisorRazonSocial${if (emisorRuc.isNotBlank()) " (RUC: $emisorRuc)" else ""}"
    } else {
        "BRIXO Central"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colores.cardBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera con Ícono de Éxito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colores.exitoSutil, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, null, tint = colores.estadoExito, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Constancia Digital BRIXO",
                                style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = colores.textoPrincipal
                            )
                            Text(
                                text = "Comprobante de operación asentada",
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.sp),
                                color = colores.textoSecundario
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = colores.textoTerciario)
                    }
                }

                HorizontalDivider(color = colores.divisor, thickness = 0.5.dp)

                // Monto Principal
                Surface(
                    color = colores.fondoBase,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MONTO DEL MOVIMIENTO",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = colores.textoTerciario
                        )
                        Text(
                            text = "${com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo.ifBlank { "S/" }} ${String.format(Locale.US, "%.2f", pago.monto)}",
                            style = TokensFarmadon.tipografia.titulo1.copy(fontSize = 24.sp, fontWeight = FontWeight.Black),
                            color = colores.textoPrincipal
                        )
                        Text(
                            text = pago.concepto,
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = colores.textoPrincipal
                        )
                    }
                }

                // Desglose de Operación
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilaDetalleConstancia(label = "Emisor", valor = emisorTexto)
                    FilaDetalleConstancia(label = "Farmacia", valor = "${nombreFarmacia.ifBlank { "Mi Farmacia" }}${if (rucFarmacia.isNotBlank()) " (RUC: $rucFarmacia)" else ""}")
                    if (pago.banco.isNotBlank()) {
                        FilaDetalleConstancia(label = "Banco / Medio", valor = pago.banco)
                    }
                    if (pago.numeroOperacion.isNotBlank()) {
                        FilaDetalleConstancia(label = "N° Operación", valor = pago.numeroOperacion)
                    }
                    FilaDetalleConstancia(label = "Fecha y Hora", valor = pago.fecha)
                    FilaDetalleConstancia(label = "Asesor BRIXO", valor = pago.admin)
                    if (pago.vigenciaHasta.isNotBlank() && pago.vigenciaHasta != "—") {
                        FilaDetalleConstancia(label = "Nueva Vigencia", valor = pago.vigenciaHasta)
                    }
                    if (pago.motivo.isNotBlank()) {
                        FilaDetalleConstancia(label = "Observación / Nota", valor = pago.motivo)
                    }
                }

                // Foto de Comprobante si está disponible
                if (pago.comprobanteUrl.isNotBlank()) {
                    Surface(
                        color = colores.fondoBase,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "COMPROBANTE BANCARIO ADJUNTO",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = colores.textoTerciario
                            )
                            coil.compose.AsyncImage(
                                model = pago.comprobanteUrl,
                                contentDescription = "Voucher bancario",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }
                }

                HorizontalDivider(color = colores.divisor, thickness = 0.5.dp)

                // Acciones: Copiar Detalle Contable y Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textoContable = buildString {
                                appendLine("--- CONSTANCIA DE PAGO BRIXO ---")
                                appendLine("Emisor: $emisorTexto")
                                appendLine("Farmacia: ${nombreFarmacia.ifBlank { "Mi Farmacia" }}${if (rucFarmacia.isNotBlank()) " (RUC: $rucFarmacia)" else ""}")
                                appendLine("Concepto: ${pago.concepto}")
                                appendLine("Monto: ${com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo.ifBlank { "S/" }} ${String.format(Locale.US, "%.2f", pago.monto)}")
                                appendLine("Fecha: ${pago.fecha}")
                                appendLine("Asesor: ${pago.admin}")
                                if (pago.vigenciaHasta.isNotBlank() && pago.vigenciaHasta != "—") {
                                    appendLine("Vigencia hasta: ${pago.vigenciaHasta}")
                                }
                                // Verdad, no maquillaje: el dato solo garantiza que el movimiento
                                // fue registrado en la auditoría oficial de BRIXO. No afirmamos
                                // "aprobado/conciliado" porque ningún campo del sistema lo prueba.
                                val estadoContable = "REGISTRO AUDITADO POR BRIXO"
                                appendLine("Estado: $estadoContable")
                                appendLine("--------------------------------")
                            }
                            clipboardManager.setText(AnnotatedString(textoContable))
                            copiadoExitoso = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (copiadoExitoso) colores.estadoExito else colores.cardBorde)
                    ) {
                        Icon(
                            imageVector = if (copiadoExitoso) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (copiadoExitoso) colores.estadoExito else colores.textoPrincipal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (copiadoExitoso) "¡COPIADO!" else "COPIAR DATOS",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (copiadoExitoso) colores.estadoExito else colores.textoPrincipal
                            )
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colores.cardElevada),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde)
                    ) {
                        Text(
                            text = "CERRAR",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, color = colores.textoPrincipal)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDetalleConstancia(label: String, valor: String) {
    val colores = TokensFarmadon.colores
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.sp),
            color = colores.textoTerciario
        )
        Text(
            text = valor,
            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = colores.textoPrincipal,
            textAlign = TextAlign.End
        )
    }
}
