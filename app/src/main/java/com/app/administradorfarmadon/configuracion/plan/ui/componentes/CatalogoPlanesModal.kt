package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.configuracion.plan.datos.PlanCatalogoItem
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import java.net.URLEncoder
import java.util.Locale

@Composable
fun CatalogoPlanesModal(
    planes: List<PlanCatalogoItem>,
    planActualNombre: String,
    whatsappCobranzas: String,
    nombreFarmacia: String,
    rucFarmacia: String,
    catalogoError: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colores = TokensFarmadon.colores

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = colores.cardBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
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
                                .background(colores.cardElevada, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RocketLaunch, null, tint = colores.botonPrimarioFondo, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Catálogo de Planes Comerciales BRIXO",
                                style = TokensFarmadon.tipografia.titulo1.copy(fontSize = 18.sp),
                                color = colores.textoPrincipal
                            )
                            Text(
                                text = "Elige el plan ideal para escalar la cantidad de locales y herramientas de tu farmacia",
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.5.sp),
                                color = colores.textoSecundario
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = colores.textoTerciario)
                    }
                }

                HorizontalDivider(color = colores.divisor, thickness = 0.5.dp)

                // Grid de Planes (3 Columnas o Lista Responsive)
                if (planes.isEmpty()) {
                    if (catalogoError != null) {
                        // Verdad: el catálogo falló de verdad, no se queda en "cargando".
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.WarningAmber, null, tint = colores.estadoAlerta, modifier = Modifier.size(28.dp))
                            Text(
                                text = catalogoError,
                                style = TokensFarmadon.tipografia.cuerpo,
                                color = colores.textoSecundario,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Cargando catálogo oficial de planes desde la central BRIXO...",
                            style = TokensFarmadon.tipografia.cuerpo,
                            color = colores.textoSecundario,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        planes.forEach { plan ->
                            val esPlanActual = plan.nombre.equals(planActualNombre, ignoreCase = true)

                            TarjetaPlanCatalogo(
                                plan = plan,
                                esPlanActual = esPlanActual,
                                onSolicitarUpgrade = {
                                    solicitarUpgradeWhatsapp(
                                        context = context,
                                        telefono = whatsappCobranzas,
                                        nombreFarmacia = nombreFarmacia,
                                        rucFarmacia = rucFarmacia,
                                        planActual = planActualNombre,
                                        planDestino = plan.nombre
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Footer de Garantía BRIXO
                Surface(
                    color = colores.fondoBase,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, null, tint = colores.estadoExito, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Las ampliaciones de plan o sucursales se activan en tiempo real sin reiniciar ni perder datos.",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.sp),
                            color = colores.textoSecundario
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPlanCatalogo(
    plan: PlanCatalogoItem,
    esPlanActual: Boolean,
    onSolicitarUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = if (esPlanActual) colores.cardElevada else colores.fondoBase,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (plan.esRecomendado || esPlanActual) 1.5.dp else 1.dp,
            if (esPlanActual) colores.estadoExito else if (plan.esRecomendado) colores.botonPrimarioFondo else colores.cardBorde
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Tarjeta Plan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.nombre.uppercase(),
                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 14.sp, fontWeight = FontWeight.Black),
                    color = colores.textoPrincipal
                )

                if (esPlanActual) {
                    Surface(
                        color = colores.estadoExito.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ACTUAL",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                            color = colores.estadoExito,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (plan.esRecomendado) {
                    Surface(
                        color = colores.botonPrimarioFondo.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "POPULAR",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                            color = colores.botonPrimarioFondo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Precio
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "S/ ${String.format(Locale.US, "%.2f", plan.precioMensual)}",
                    style = TokensFarmadon.tipografia.titulo1.copy(fontSize = 20.sp, fontWeight = FontWeight.Black),
                    color = colores.textoPrincipal
                )
                Text(
                    text = " / mes",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 11.5.sp),
                    color = colores.textoTerciario,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            HorizontalDivider(color = colores.divisor, thickness = 0.5.dp)

            // Características Clave
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemBeneficioPlan(texto = "Hasta ${plan.maxSucursales} ${if (plan.maxSucursales == 1) "sucursal incluida" else "sucursales incluidas"}")
                ItemBeneficioPlan(texto = if (plan.diasPrueba > 0) "${plan.diasPrueba} días de bienvenida" else "Activación comercial inmediata")
                ItemBeneficioPlan(texto = "Facturación SUNAT en vivo")
                ItemBeneficioPlan(texto = "Lotes y Vencimientos DIGEMID")
            }

            Spacer(Modifier.weight(1f))

            // Botón de Acción
            if (esPlanActual) {
                Surface(
                    color = colores.cardBase,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PLAN EN USO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = colores.textoTerciario,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                Button(
                    onClick = onSolicitarUpgrade,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (plan.esRecomendado) colores.botonPrimarioFondo else colores.cardElevada
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colores.botonPrimarioFondo.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            text = "SOLICITAR UPGRADE",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (plan.esRecomendado) com.app.administradorfarmadon.disenotemaapp.ui.FDColors.PrimaryText else colores.textoPrincipal
                            )
                        )
                }
            }
        }
    }
}

@Composable
private fun ItemBeneficioPlan(texto: String) {
    val colores = TokensFarmadon.colores
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Check, null, tint = colores.estadoExito, modifier = Modifier.size(14.dp))
        Text(
            text = texto,
            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 11.5.sp),
            color = colores.textoSecundario,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun solicitarUpgradeWhatsapp(
    context: Context,
    telefono: String,
    nombreFarmacia: String,
    rucFarmacia: String,
    planActual: String,
    planDestino: String
) {
    try {
        val telLimpio = telefono.replace("+", "").replace(" ", "").replace("-", "")
        val msj = "Hola BRIXO, mi farmacia *${nombreFarmacia.ifBlank { "Mi Farmacia" }}* " +
                (if (rucFarmacia.isNotBlank()) "(RUC: *${rucFarmacia}*) " else "") +
                "cuenta con el plan *${planActual}* y deseamos solicitar una actualización al *${planDestino}*. " +
                "Por favor coordinar condiciones y habilitación."
        val url = "https://wa.me/$telLimpio?text=${URLEncoder.encode(msj, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
