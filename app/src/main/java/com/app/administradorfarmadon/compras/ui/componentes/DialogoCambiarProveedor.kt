package com.app.administradorfarmadon.compras.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct

@Composable
fun DialogoCambiarProveedor(
    producto: PharmProduct,
    proveedores: List<Proveedor>,
    guardando: Boolean = false,
    errorGuardado: String? = null,
    onGuardar: (Proveedor) -> Unit,
    onDesvincular: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val proveedorActualNombre = producto.proveedor.trim()
    val tieneProveedorActual = proveedorActualNombre.isNotBlank() &&
        !listOf("SIN PROVEEDOR", "N/A", "GENÉRICO", "GENERICO", "SIN ASIGNAR").any { it.equals(proveedorActualNombre, ignoreCase = true) }

    var busquedaProveedor by rememberSaveable { mutableStateOf("") }
    var proveedorSeleccionado by remember(producto.id) {
        mutableStateOf(proveedores.firstOrNull {
            (producto.proveedorId.isNotBlank() && it.id == producto.proveedorId) ||
            it.nombre.equals(proveedorActualNombre, ignoreCase = true) ||
            (it.idFiscal.isNotBlank() && it.idFiscal == proveedorActualNombre)
        })
    }

    val proveedoresFiltrados = remember(proveedores, busquedaProveedor) {
        val q = busquedaProveedor.trim().lowercase()
        if (q.isBlank()) proveedores
        else proveedores.filter { prov ->
            prov.nombre.lowercase().contains(q) ||
                prov.idFiscal.lowercase().contains(q) ||
                prov.contacto.lowercase().contains(q)
        }
    }

    val esMismoProveedor = proveedorSeleccionado != null && (
        (producto.proveedorId.isNotBlank() && proveedorSeleccionado?.id == producto.proveedorId) ||
        proveedorSeleccionado?.nombre?.equals(proveedorActualNombre, ignoreCase = true) == true
    )

    Dialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 480.dp, max = 580.dp)
                .fillMaxWidth(0.52f)
                .fillMaxHeight(0.86f)
                .heightIn(min = 500.dp),
            shape = RoundedCornerShape(18.dp),
            color = FDColors.SurfaceElevated,
            border = BorderStroke(1.dp, FDColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── CABECERA ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FDColors.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (tieneProveedorActual) "CAMBIAR PROVEEDOR HABITUAL" else "VINCULAR A PROVEEDOR",
                                style = FDType.Heading3.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Define a qué droguería se le comprará este producto",
                                style = FDType.Caption.copy(fontSize = 11.5.sp),
                                color = FDColors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !guardando,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 1.dp)

                // ── TARJETA DEL PRODUCTO ──
                Surface(
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FDColors.Primary.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Medication,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = producto.name,
                                style = FDType.Heading3.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            val detalles = listOfNotNull(
                                producto.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                                producto.empaque.takeIf { it.isNotBlank() },
                                "Stock: ${producto.stock}u"
                            ).joinToString(" · ")
                            Text(
                                text = detalles,
                                style = FDType.Caption.copy(fontSize = 11.sp),
                                color = FDColors.TextTertiary
                            )
                        }

                        // Proveedor Actual
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (tieneProveedorActual) FDColors.Primary.copy(alpha = 0.12f) else FDColors.Warning.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (tieneProveedorActual) FDColors.Primary.copy(alpha = 0.4f) else FDColors.Warning.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (tieneProveedorActual) Icons.Default.Business else Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = if (tieneProveedorActual) FDColors.Primary else FDColors.Warning,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (tieneProveedorActual) proveedorActualNombre else "Sin proveedor",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                    color = if (tieneProveedorActual) FDColors.Primary else FDColors.Warning,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // ── BUSCADOR DE PROVEEDOR CÓMODO Y ESPACIOSO ──
                CampoBuscadorModerno(
                    busqueda = busquedaProveedor,
                    onBusquedaChange = { busquedaProveedor = it },
                    placeholder = "Buscar droguería por nombre o RUC...",
                    altura = 48.dp
                )

                // ── LISTA DE PROVEEDORES (OCUPA EL ALTO RESTANTE) ──
                Text(
                    text = "SELECCIONA EL NUEVO PROVEEDOR:",
                    style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp),
                    color = FDColors.TextTertiary
                )

                Surface(
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (proveedoresFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (busquedaProveedor.isNotBlank()) "No se encontró ninguna droguería con '$busquedaProveedor'" else "No hay proveedores registrados",
                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(proveedoresFiltrados, key = { it.id }) { prov ->
                                val estaSeleccionado = proveedorSeleccionado?.id == prov.id
                                Surface(
                                    onClick = { proveedorSeleccionado = prov },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (estaSeleccionado) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (estaSeleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (estaSeleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (estaSeleccionado) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prov.nombre,
                                                style = FDType.Body.copy(
                                                    fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = if (estaSeleccionado) FDColors.Primary else FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val datosFiscales = listOfNotNull(
                                                if (prov.idFiscal.isNotBlank()) "RUC: ${prov.idFiscal}" else null,
                                                prov.contacto.takeIf { it.isNotBlank() }
                                            ).joinToString(" · ")
                                            if (datosFiscales.isNotBlank()) {
                                                Text(
                                                    text = datosFiscales,
                                                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                                                    color = FDColors.TextTertiary
                                                )
                                            }
                                        }

                                        if (prov.nombre.equals(proveedorActualNombre, ignoreCase = true)) {
                                            Text(
                                                text = "(Actual)",
                                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── GARANTÍA DE AUDITORÍA ──
                Surface(
                    color = FDColors.Success.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = FDColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Garantía de Auditoría: No altera facturas anteriores, lotes ni Kardex.",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                }

                errorGuardado?.let { err ->
                    Surface(
                        color = FDColors.Error.copy(alpha = 0.08f),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                            color = FDColors.Error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // ── BOTONES DE ACCIÓN ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FDBotonSecundario(
                        texto = "CANCELAR",
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tieneProveedorActual && onDesvincular != null) {
                            FDBotonSecundario(
                                texto = "DESVINCULAR",
                                onClick = onDesvincular,
                                icono = Icons.Default.LinkOff,
                                modifier = Modifier.height(34.dp)
                            )
                        }

                        FDBotonPrimario(
                            texto = if (tieneProveedorActual) "GUARDAR CAMBIO" else "VINCULAR",
                            onClick = {
                                proveedorSeleccionado?.let { onGuardar(it) }
                            },
                            habilitado = !guardando && proveedorSeleccionado != null && !esMismoProveedor,
                            cargando = guardando,
                            icono = Icons.Default.Check,
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }
        }
    }
}
