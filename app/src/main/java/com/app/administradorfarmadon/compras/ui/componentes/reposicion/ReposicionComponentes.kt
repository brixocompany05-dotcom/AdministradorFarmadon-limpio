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

/** Anchos de columna de la tabla de productos (una sola fuente: cabecera y filas). */
internal val AnchoColStock = 132.dp
internal val AnchoColEnCamino = 96.dp
internal val AnchoColPrecio = 104.dp
internal val AnchoColPedir = 172.dp

@Composable
fun MetricItemReposicion(label: String, value: String, color: Color) {
    val s = recordarMedidaAdaptativa()
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextTertiary
        )
        Text(
            text = value,
            style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black),
            color = color
        )
    }
}

/** Botón redondo − / + del catálogo (una sola línea por producto). */
@Composable
fun BotonMasMenos(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    habilitado: Boolean,
    onClick: () -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Surface(
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(s.radiusInput * 0.55f),
        border = BorderStroke(s.borderWidth * 0.8f, FDColors.Border),
        modifier = Modifier
            .size(s.btnMediumH)
            .clickable(enabled = habilitado, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (habilitado) FDColors.TextPrimary else FDColors.TextTertiary,
                modifier = Modifier.size(s.iconSmall)
            )
        }
    }
}

/** Grupo de catálogo que NO es un proveedor real (productos sin afiliar). */
fun esProveedorPlaceholder(nombre: String): Boolean =
    nombre.isBlank() || listOf("Droguería General / Sin Asignar", "N/A", "NA", "Genérico", "Sin asignar", "Sin Asignar")
        .any { it.equals(nombre.trim(), ignoreCase = true) }

