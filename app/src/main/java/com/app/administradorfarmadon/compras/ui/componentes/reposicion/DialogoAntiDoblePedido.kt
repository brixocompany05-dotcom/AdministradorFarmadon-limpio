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


// ESCUDO ANTI DOBLE PEDIDO
@Composable
fun DialogoAntiDoblePedido(
    prod: PharmProduct,
    info: com.app.administradorfarmadon.compras.logica.ProductoEnCamino?,
    cantidadExtraPropuesta: Int,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    AlertDialog(
        onDismissRequest = onDescartar,
        containerColor = FDColors.SurfaceElevated,
        title = {
            Text(
                "Este producto ya está en camino",
                style = FDType.Heading3.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = FDColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)) {
                val ordenesTxt = info?.ordenes?.filter { o -> o.isNotBlank() }?.joinToString()
                    ?.let { o -> " (en orden $o)" } ?: ""
                Text(
                    "${prod.name}: ya tiene ${info?.unidades ?: 0} unidades en camino$ordenesTxt.",
                    style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    "¿Deseas sumar $cantidadExtraPropuesta unidades más a la reposición de este producto?",
                    style = FDType.BodySmall.copy(fontSize = 12.sp),
                    color = FDColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FDColors.Primary,
                    contentColor = FDColors.PrimaryText
                ),
                shape = RoundedCornerShape(s.radiusInput * 0.75f)
            ) {
                Text(
                    "SÍ, SUMAR $cantidadExtraPropuesta UNIDADES",
                    style = FDType.Label.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDescartar,
                shape = RoundedCornerShape(s.radiusInput * 0.75f)
            ) {
                Text(
                    "CANCELAR",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    )
}
