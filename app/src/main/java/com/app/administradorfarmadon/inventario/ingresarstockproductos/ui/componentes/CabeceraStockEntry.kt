package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryState

@Composable
fun CabeceraStockEntry(
    state: StockEntryState,
    onBackClicked: () -> Unit,
    onScanGs1: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(FDColors.Background)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FDColors.SurfaceElevated)
                        .border(1.dp, FDColors.Border, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = FDColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Agregar stock",
                            style = FDType.Heading2.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FDColors.TextPrimary
                            )
                        )

                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = state.productoNombre.ifBlank { "CARGANDO..." }.uppercase(),
                                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FDColors.Primary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Categoría: ${state.categoria.ifBlank { "General" }}",
                            style = FDType.Caption.copy(fontSize = 12.sp, color = FDColors.TextSecondary)
                        )
                        Text("•", color = FDColors.Border)
                        Text(
                            text = "Laboratorio: ${state.laboratorio.ifBlank { "Genérico" }}",
                            style = FDType.Caption.copy(fontSize = 12.sp, color = FDColors.TextSecondary)
                        )
                        if (state.codigoBarras.isNotBlank()) {
                            Text("•", color = FDColors.Border)
                            Text(
                                text = "EAN: ${state.codigoBarras}",
                                style = FDType.Caption.copy(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = FDColors.TextTertiary)
                            )
                        }
                    }
                }
            }

            // Indicador de Estado del Formulario
            Surface(
                color = if (state.esFormularioValido) FDColors.Success.copy(alpha = 0.12f) else FDColors.Border.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (state.esFormularioValido) FDColors.Success.copy(alpha = 0.3f) else FDColors.Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (state.esFormularioValido) FDColors.Success else FDColors.TextTertiary, CircleShape)
                    )
                    Text(
                        text = if (state.esFormularioValido) "Listo para guardar" else "Falta completar",
                        style = FDType.Caption.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.esFormularioValido) FDColors.Success else FDColors.TextSecondary
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)
    }
}
