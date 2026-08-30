package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.plan.datos.BrixoCanalesPagoInfo
import com.app.administradorfarmadon.configuracion.plan.datos.MetodoPagoBrixoItem
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import java.net.URLEncoder

@Composable
fun BrixoCanalesPagoCard(
    canalesPago: BrixoCanalesPagoInfo,
    nombreFarmacia: String,
    rucFarmacia: String,
    error: String? = null,
    onIniciarAsentamiento: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colores.cardElevada,
        shape = RoundedCornerShape(s.radiusCard),
        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
    ) {
        Column(
            modifier = Modifier.padding(s.padCard),
            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            if (error != null) {
                Surface(
                    color = colores.alertaSutil,
                    shape = RoundedCornerShape(s.radiusChip),
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoAlerta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(s.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = colores.estadoAlerta,
                            modifier = Modifier.size(s.iconSmall)
                        )
                        Text(
                            text = error,
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                            color = colores.textoPrincipal
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge * 1.35f)
                        .background(colores.cardElevada, RoundedCornerShape(s.radiusChip))
                        .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
                Column {
                    Text(
                        text = "Canales Oficiales de Renovación BRIXO",
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (canalesPago.razonSocial.isNotBlank()) {
                            "${canalesPago.razonSocial}${if (canalesPago.ruc.isNotBlank()) " · RUC: ${canalesPago.ruc}" else ""}"
                        } else "BRIXO Central de Cobranzas y Suscripciones",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                        color = colores.textoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = colores.divisor, thickness = s.separatorH)

            if (canalesPago.metodosActivos.isEmpty()) {
                Text(
                    text = "Para renovar o consultar métodos de pago, comunícate directamente con tu asesor BRIXO.",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                    color = colores.textoSecundario
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                    canalesPago.metodosActivos.forEach { metodo ->
                        ItemCuentaBancaria(
                            metodo = metodo,
                            onCopiar = { texto, label ->
                                copiarAlPortapapeles(context, texto, label)
                            }
                        )
                    }
                }
            }

            Surface(
                color = colores.fondoBase,
                shape = RoundedCornerShape(s.radiusInput),
                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(s.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "¿Ya realizaste tu transferencia bancaria?",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                            color = colores.textoPrincipal
                        )
                        Text(
                            text = "Envía tu comprobante para que BRIXO valide tu abono y renueve tu servicio de inmediato.",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.88f),
                            color = colores.textoSecundario
                        )
                    }

                    Button(
                        onClick = onIniciarAsentamiento,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colores.textoPrincipal
                        ),
                        shape = RoundedCornerShape(s.radiusButton),
                        contentPadding = PaddingValues(horizontal = s.padCard * 0.85f, vertical = s.xs),
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(s.iconSmall),
                            tint = colores.botonPrimarioTexto
                        )
                        Spacer(Modifier.width(s.xs))
                        Text(
                            text = "VINCULAR ABONO",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = s.textLabel.value.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = colores.botonPrimarioTexto
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCuentaBancaria(
    metodo: MetodoPagoBrixoItem,
    onCopiar: (String, String) -> Unit
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    Surface(
        color = colores.fondoBase,
        shape = RoundedCornerShape(s.radiusInput),
        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.sm, vertical = s.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(s.iconLarge * 1.05f)
                    .background(colores.cardElevada, RoundedCornerShape(s.radiusChip)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (metodo.tipoCuenta == "BILLETERA_DIGITAL") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = colores.textoPrincipal,
                    modifier = Modifier.size(s.iconSmall)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                ) {
                    Text(
                        text = "${metodo.bancoNombre} (${if (metodo.moneda == "PEN") "Soles" else "Dólares"})",
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                        color = colores.textoPrincipal
                    )
                    Text(
                        text = "—¢ ${metodo.tipoCuenta}",
                        style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f),
                        color = colores.textoTerciario
                    )
                }

                Text(
                    text = "Cta: ${metodo.numeroCuenta}",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.SemiBold),
                    color = colores.textoSecundario
                )

                if (metodo.numeroCci.isNotBlank()) {
                    Text(
                        text = "CCI: ${metodo.numeroCci}",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.88f),
                        color = colores.textoTerciario
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f),
                horizontalAlignment = Alignment.End
            ) {
                OutlinedButton(
                    onClick = { onCopiar(metodo.numeroCuenta, "Cuenta ${metodo.bancoNombre}") },
                    shape = RoundedCornerShape(s.radiusChip * 0.6f),
                    contentPadding = PaddingValues(horizontal = s.xs, vertical = s.xs * 0.5f),
                    modifier = Modifier.height(s.btnSmallH * 0.73f),
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                    Spacer(Modifier.width(s.xs * 0.5f))
                    Text("COPIAR CTA", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f), color = colores.textoPrincipal)
                }

                if (metodo.numeroCci.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onCopiar(metodo.numeroCci, "CCI ${metodo.bancoNombre}") },
                        shape = RoundedCornerShape(s.radiusChip * 0.6f),
                        contentPadding = PaddingValues(horizontal = s.xs, vertical = s.xs * 0.5f),
                        modifier = Modifier.height(s.btnSmallH * 0.73f),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                        Spacer(Modifier.width(s.xs * 0.5f))
                        Text("COPIAR CCI", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f), color = colores.textoPrincipal)
                    }
                }
            }
        }
    }
}

private fun copiarAlPortapapeles(context: Context, texto: String, label: String) {
    try {
        val limpio = texto.replace(" ", "").replace("-", "")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, limpio)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "──œ“ $label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo copiar al portapapeles", Toast.LENGTH_SHORT).show()
    }
}

private fun abrirWhatsappCobranzas(
    context: Context,
    telefono: String,
    nombreFarmacia: String,
    rucFarmacia: String
) {
    try {
        val telLimpio = telefono.replace("+", "").replace(" ", "").replace("-", "")
        val msj = "Hola BRIXO, adjunto comprobante de pago de mi farmacia:\n" +
                "—¢ *Farmacia:* ${nombreFarmacia.ifBlank { "Mi Farmacia" }}\n" +
                (if (rucFarmacia.isNotBlank()) "—¢ *RUC:* $rucFarmacia\n" else "") +
                "Por favor confirmar renovación de nuestro plan."
        val url = "https://wa.me/$telLimpio?text=${URLEncoder.encode(msj, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
