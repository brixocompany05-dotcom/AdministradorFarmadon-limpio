package com.app.administradorfarmadon.inventario.compartido.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper
import java.util.Calendar

/**
 * Controles compartidos del workspace de lotes:
 * - StepperCantidad: [ − ] [ N ] [ + ] en lugar de escribir a mano.
 * - SelectorVencimiento: selector de MES/AÑO con el formato estándar MM/AAAA.
 */

@Composable
internal fun StepperCantidad(
    cantidad: Int,
    onCantidadChange: (Int) -> Unit,
    minimo: Int = 1,
    maximo: Int = Int.MAX_VALUE,
    unidad: String = "",
    modifier: Modifier = Modifier
) {
    val techo = maximo.coerceAtLeast(minimo)
    val actual = cantidad.coerceIn(minimo, techo)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BotonStepper(icono = Icons.Default.Remove, habilitado = actual > minimo, onClick = { onCantidadChange(actual - 1) })
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, FDColors.BorderStrong),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (unidad.isBlank()) "$actual" else "$actual $unidad",
                    style = FDType.Numeric.copy(fontSize = 22.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
            }
        }
        BotonStepper(icono = Icons.Default.Add, habilitado = actual < techo, onClick = { onCantidadChange(actual + 1) })
    }
}

@Composable
private fun BotonStepper(icono: ImageVector, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (habilitado) FDColors.BorderStrong else FDColors.Border.copy(alpha = 0.4f)),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icono,
                null,
                tint = if (habilitado) FDColors.TextPrimary else FDColors.TextTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun SelectorVencimiento(
    vencimiento: String,
    onVencimientoChange: (String) -> Unit,
    label: String = "Vencimiento",
    modifier: Modifier = Modifier
) {
    val ahora = remember { Calendar.getInstance() }
    val mesInicial = vencimiento.split("/").firstOrNull()?.toIntOrNull()?.coerceIn(1, 12)
        ?: (ahora.get(Calendar.MONTH) + 1)
    val anioInicial = vencimiento.split("/").lastOrNull()?.toIntOrNull()
        ?: ahora.get(Calendar.YEAR)
    var mes by remember(vencimiento) { mutableIntStateOf(mesInicial) }
    var anio by remember(vencimiento) { mutableIntStateOf(anioInicial) }
    val anioActual = ahora.get(Calendar.YEAR)
    val anios = (anioActual..anioActual + 15).map { it.toString() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
            color = FDColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SelectorDesplegable(
                label = "MES",
                opciones = (1..12).map { "%02d".format(it) },
                seleccionado = "%02d".format(mes),
                onSeleccion = {
                    mes = it.toInt()
                    onVencimientoChange(FechaVencimientoHelper.formatear(mes, anio))
                },
                modifier = Modifier.weight(1f)
            )
            SelectorDesplegable(
                label = "AÑO",
                opciones = anios,
                seleccionado = anio.toString(),
                onSeleccion = {
                    anio = it.toInt()
                    onVencimientoChange(FechaVencimientoHelper.formatear(mes, anio))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectorDesplegable(
    label: String,
    opciones: List<String>,
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
            color = FDColors.TextTertiary
        )
        Box {
            Surface(
                onClick = { expandido = true },
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FDColors.BorderStrong),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(seleccionado, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                    Icon(Icons.Default.ArrowDropDown, null, tint = FDColors.TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                opciones.forEach { op ->
                    DropdownMenuItem(
                        text = { Text(op, style = FDType.Body.copy(fontSize = 13.sp)) },
                        onClick = { onSeleccion(op); expandido = false }
                    )
                }
            }
        }
    }
}
