package com.app.administradorfarmadon.facturacion.configuracion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.facturacion.configuracion.datos.ActaCambioEmisor
import com.app.administradorfarmadon.facturacion.configuracion.datos.DetalleCambioEmisor
import com.app.administradorfarmadon.facturacion.configuracion.logica.FacturacionConfigUiState

/**
 * Pestaña Enterprise de Auditoría Fiscal.
 * Presenta el acta append-only e inmutable de quién cambió los datos tributarios,
 * en qué fecha y hora exacta, y cuál fue el valor anterior vs. el nuevo.
 */
@Composable
fun PestanaAuditoria(
    state: FacturacionConfigUiState,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = FDSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
    ) {
        // ── 1. CABECERA RESUMEN EJECUTIVO (METRICAS EN VIVO) ──
        val totalEventos = state.historial.size
        val totalExitosos = state.historial.count { it.verificadoOk }
        val totalFallidos = state.historial.count { !it.verificadoOk }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            TarjetaMetricaAuditoria(
                titulo = "TOTAL EVENTOS",
                valor = totalEventos.toString(),
                subtitulo = "Actas registradas en Firebase",
                icono = Icons.Default.HistoryEdu,
                color = FDColors.Primary,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetricaAuditoria(
                titulo = "VERIFICADOS SUNAT",
                valor = totalExitosos.toString(),
                subtitulo = "Conexión validada OK",
                icono = Icons.Default.CheckCircle,
                color = FDColors.Success,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetricaAuditoria(
                titulo = "RECHAZADOS / FALLOS",
                valor = totalFallidos.toString(),
                subtitulo = "Rechazados por APISUNAT",
                icono = Icons.Default.WarningAmber,
                color = if (totalFallidos > 0) FDColors.Error else FDColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
        }

        // ── 2. LISTADO CRONOLÓGICO DE AUDITORÍA ──
        if (state.cargandoHistorial && state.historial.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FDColors.Primary, modifier = Modifier.size(36.dp))
            }
        } else if (state.historial.isEmpty()) {
            // Estado vacío honesto (Regla R12)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(FDSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(FDColors.PrimarySubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Sin modificaciones registradas",
                            style = FDType.Heading2.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Aún no se han efectuado cambios en el Emisor Fiscal. Toda edición de RUC, token o credenciales registrará aquí fecha, hora, responsable y diferencias de datos.",
                            style = FDType.BodySmall,
                            color = FDColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.md),
                contentPadding = PaddingValues(bottom = FDSpacing.xxl)
            ) {
                items(state.historial, key = { it.id }) { acta ->
                    TarjetaActaAuditoria(acta = acta)
                }
            }
        }
    }
}

@Composable
private fun TarjetaMetricaAuditoria(
    titulo: String,
    valor: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(FDSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(FDShapes.Small)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    text = titulo,
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                    color = FDColors.TextTertiary
                )
                Text(
                    text = valor,
                    style = FDType.Heading1.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = subtitulo,
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TarjetaActaAuditoria(
    acta: ActaCambioEmisor
) {
    val esExitoso = acta.verificadoOk
    val colorBorde = if (esExitoso) FDColors.Border else FDColors.Error.copy(alpha = 0.5f)
    val colorBadge = if (esExitoso) FDColors.Success else FDColors.Error

    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Large,
        border = BorderStroke(1.dp, colorBorde),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            // ── CABECERA DEL ACTA (FECHA, HORA, ESTADO Y USUARIO) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorBadge.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (esExitoso) Icons.Default.Verified else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = colorBadge,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = if (esExitoso) "GUARDADO VERIFICADO EN APISUNAT" else "INTENTO RECHAZADO POR APISUNAT",
                        style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterPremium),
                        color = colorBadge
                    )
                }

                // Fecha y Hora exacta (dd/MM/yyyy HH:mm:ss)
                Surface(
                    color = FDColors.Background,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, FDColors.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            text = acta.fechaLegible.ifBlank { "Fecha pendiente" },
                            style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                            color = FDColors.TextPrimary
                        )
                    }
                }
            }

            // ── METADATOS DE RESPONSABILIDAD: QUIÉN Y DESDE DÓNDE ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                ) {
                    Icon(Icons.Default.Person, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Responsable: ",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    Text(
                        text = "${acta.usuarioNombre.ifBlank { "Administrador" }} (${acta.usuarioEmail.ifBlank { "cuenta principal" }})",
                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = FDColors.TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                ) {
                    Icon(Icons.Default.Storefront, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Sede: ",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    Text(
                        text = if (acta.sedeId.equals("principal", ignoreCase = true)) "Sede Principal" else acta.sedeId,
                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = FDColors.TextPrimary
                    )
                }
            }

            // ── DETALLE DEL FALLO SI APISUNAT RECHAZÓ ──
            if (acta.ultimoError.isNotBlank()) {
                Surface(
                    color = FDColors.Error.copy(alpha = 0.08f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FDSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = FDColors.Error, modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = "Motivo de rechazo de la API:",
                                style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                                color = FDColors.Error
                            )
                            Text(
                                text = acta.ultimoError,
                                style = FDType.BodySmall,
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // ── QUÉ CAMBIÓ: TABLA DE DIFERENCIAS (ANTES VS DESPUÉS) ──
            if (acta.cambios.isNotEmpty()) {
                Surface(
                    color = FDColors.Background,
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(FDSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                    ) {
                        Text(
                            text = "CAMPOS MODIFICADOS EN ESTE EVENTO (${acta.cambios.size})",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = FDColors.TextTertiary
                        )

                        HorizontalDivider(color = FDColors.Border)

                        for (cambio in acta.cambios) {
                            FilaDiferenciaAuditoria(cambio = cambio)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDiferenciaAuditoria(
    cambio: DetalleCambioEmisor
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
    ) {
        // Nombre del campo
        Surface(
            color = FDColors.Primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = cambio.campo,
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                color = FDColors.Primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Valor anterior
        Text(
            text = cambio.antes.ifBlank { "(sin valor previo)" },
            style = FDType.BodySmall.copy(
                textDecoration = if (cambio.antes.isNotBlank()) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = FDColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = FDColors.TextTertiary,
            modifier = Modifier.size(14.dp)
        )

        // Nuevo valor aplicado
        Text(
            text = cambio.despues.ifBlank { "(vacío)" },
            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
            color = FDColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )
    }
}
