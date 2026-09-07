package com.app.administradorfarmadon.compras.pagos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.pagos.logica.EtiquetaMetodoPago
import com.app.administradorfarmadon.compras.pagos.logica.PagosMixtosEditorState
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import java.util.Locale

/**
 * Editor visual del PAGO MIXTO. Un solo paso: todos los métodos están a la vista
 * como chips; tocar uno abre su cajón (monto + operación). Los que ya se usaron
 * quedan marcados y se pueden quitar. CERO reglas de negocio: vive en [PagosMixtosEditorState].
 */
@Composable
fun PagosMixtosEditor(
    estado: PagosMixtosEditorState,
    modificador: Modifier = Modifier,
    soloLectura: Boolean = false
) {
    val s = recordarMedidaAdaptativa()
    val colores = TokensFarmadon.colores
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }

    Column(modifier = modificador, verticalArrangement = Arrangement.spacedBy(s.sm)) {
        Text(
            "¿CÓMO SE PAGÓ?",
            style = TokensFarmadon.tipografia.etiqueta.copy(
                fontSize = s.textLabel.value.sp * 0.92f,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            ),
            color = colores.textoTerciario
        )

        // Todos los métodos visibles como botones: tocar uno lo agrega. Se acomodan
        // en varias líneas (FlowRow) para que nunca se aprieten ni queden cortados.
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(s.xs),
            verticalArrangement = Arrangement.spacedBy(s.xs)
        ) {
            estado.opcionesMetodo.forEach { opcion ->
                val usado = estado.filas.any { it.metodo == opcion }
                Surface(
                    color = if (usado) colores.estadoExito else colores.cardElevada,
                    shape = RoundedCornerShape(s.radiusButton),
                    border = BorderStroke(
                        s.borderWidth,
                        if (usado) colores.estadoExito else colores.cardBorde
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(s.radiusButton))
                        .clickable(enabled = !soloLectura) {
                            if (usado) {
                                estado.quitarPorMetodo(opcion)
                            } else {
                                estado.agregarMetodoEspecifico(opcion)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = s.sm + 2.dp, vertical = s.xs + 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                    ) {
                        if (usado) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = colores.textoInvertido,
                                modifier = Modifier.size(s.iconTiny)
                            )
                        }
                        Text(
                            opcion,
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp * 0.95f,
                                fontWeight = if (usado) FontWeight.Black else FontWeight.Medium
                            ),
                            color = if (usado) colores.textoInvertido else colores.textoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Si es método único con auto-completar activado (ej. Recepción al contado):
        // Se muestra tarjeta limpia con el total sin pedir escribir el monto.
        if (estado.autoCompletarTotalUnicoMetodo && estado.filas.size == 1) {
            val fila = estado.filas.first()
            val necesitaOperacion = EtiquetaMetodoPago.requiereOperacion(fila.metodo)
            var operacionAbierta by remember(fila.id) { mutableStateOf(fila.operacion.isNotBlank()) }

            Surface(
                color = colores.cardBase,
                shape = RoundedCornerShape(s.radiusCard * 0.75f),
                border = BorderStroke(s.borderWidth, colores.cardBorde),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(s.padCard),
                    verticalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Text(
                                fila.metodo,
                                style = TokensFarmadon.tipografia.titulo3.copy(
                                    fontSize = s.textBody.value.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = colores.textoPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = colores.estadoExito.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(s.radiusChip)
                            ) {
                                Text(
                                    "100% al contado",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                        fontSize = s.textLabel.value.sp * 0.8f,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colores.estadoExito,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Text(
                                "$simboloMoneda " + String.format(Locale.US, "%.2f", estado.montoMaximoActual),
                                style = TokensFarmadon.tipografia.titulo3.copy(
                                    fontSize = s.textBody.value.sp * 1.05f,
                                    fontWeight = FontWeight.Black
                                ),
                                color = colores.textoPrincipal
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar método",
                                tint = colores.textoTerciario,
                                modifier = Modifier
                                    .size(s.iconSmall)
                                    .clip(RoundedCornerShape(s.radiusChip))
                                    .clickable(enabled = !soloLectura) { estado.quitarPorMetodo(fila.metodo) }
                            )
                        }
                    }

                    if (necesitaOperacion) {
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(s.radiusChip),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(s.radiusChip))
                                .clickable { operacionAbierta = !operacionAbierta }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = s.xs * 0.5f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                            ) {
                                Text(
                                    if (operacionAbierta) "Ocultar N° de operación" else "Añadir N° de operación (opcional)",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                        fontSize = s.textLabel.value.sp * 0.88f,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colores.textoPrincipal
                                )
                                Icon(
                                    if (operacionAbierta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = colores.textoTerciario,
                                    modifier = Modifier.size(s.iconTiny)
                                )
                            }
                        }
                        if (operacionAbierta || fila.operacion.isNotBlank()) {
                            OutlinedTextField(
                                value = fila.operacion,
                                onValueChange = { estado.cambiarOperacion(fila.id, it) },
                                enabled = !soloLectura,
                                label = { Text("N° de operación", fontSize = s.textLabel.value.sp * 0.82f) },
                                singleLine = true,
                                textStyle = TokensFarmadon.tipografia.cuerpo.copy(
                                    fontSize = s.textInput.value.sp,
                                    color = colores.textoPrincipal
                                ),
                                shape = RoundedCornerShape(s.radiusInput),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colores.textoPrincipal,
                                    unfocusedBorderColor = colores.cardBorde,
                                    focusedContainerColor = colores.fondoBase,
                                    unfocusedContainerColor = colores.fondoBase
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            // Cajones de cada método elegido (pagos divididos o edición manual).
            estado.filas.forEach { fila ->
                val excede = (fila.montoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0) > estado.montoMaximoParaFila(fila.id) + 0.01
                val necesitaOperacion = EtiquetaMetodoPago.requiereOperacion(fila.metodo)
                var operacionAbierta by remember(fila.id) { mutableStateOf(fila.operacion.isNotBlank()) }
                Surface(
                    color = colores.cardBase,
                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                    border = BorderStroke(s.borderWidth, if (excede) colores.estadoPeligro.copy(alpha = 0.55f) else colores.cardBorde),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(s.padCard),
                        verticalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                fila.metodo,
                                modifier = Modifier.weight(1f),
                                style = TokensFarmadon.tipografia.titulo3.copy(
                                    fontSize = s.textBody.value.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = colores.textoPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar método",
                                tint = colores.textoTerciario,
                                modifier = Modifier
                                    .size(s.iconSmall)
                                    .clip(RoundedCornerShape(s.radiusChip))
                                    .clickable(enabled = !soloLectura) { estado.quitarPorMetodo(fila.metodo) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(s.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = fila.montoTexto,
                                onValueChange = { estado.cambiarMonto(fila.id, it) },
                                enabled = !soloLectura,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Monto a pagar ($simboloMoneda)", fontSize = s.textLabel.value.sp * 0.82f) },
                                isError = excede,
                                textStyle = TokensFarmadon.tipografia.cuerpo.copy(
                                    fontSize = s.textInput.value.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colores.textoPrincipal
                                ),
                                shape = RoundedCornerShape(s.radiusInput),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (excede) colores.estadoPeligro else colores.textoPrincipal,
                                    unfocusedBorderColor = if (excede) colores.estadoPeligro else colores.cardBorde,
                                    focusedContainerColor = colores.fondoBase,
                                    unfocusedContainerColor = colores.fondoBase,
                                    focusedTextColor = colores.textoPrincipal,
                                    unfocusedTextColor = colores.textoPrincipal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // N° de operación: solo donde existe de verdad (Yape, Plin, transferencia, cheque).
                        // Se expande/contrae para no estorbar cuando no se necesita.
                        if (necesitaOperacion) {
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(s.radiusChip),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(s.radiusChip))
                                    .clickable { operacionAbierta = !operacionAbierta }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = s.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                ) {
                                    Text(
                                        if (operacionAbierta) "Ocultar N° de operación" else "Añadir N° de operación (opcional)",
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                            fontSize = s.textLabel.value.sp * 0.9f,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colores.textoPrincipal
                                    )
                                    Icon(
                                        if (operacionAbierta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null,
                                        tint = colores.textoTerciario,
                                        modifier = Modifier.size(s.iconTiny)
                                    )
                                }
                            }
                            if (operacionAbierta || fila.operacion.isNotBlank()) {
                                OutlinedTextField(
                                    value = fila.operacion,
                                    onValueChange = { estado.cambiarOperacion(fila.id, it) },
                                    enabled = !soloLectura,
                                    label = { Text("N° de operación", fontSize = s.textLabel.value.sp * 0.82f) },
                                    singleLine = true,
                                    textStyle = TokensFarmadon.tipografia.cuerpo.copy(
                                        fontSize = s.textInput.value.sp,
                                        color = colores.textoPrincipal
                                    ),
                                    shape = RoundedCornerShape(s.radiusInput),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colores.textoPrincipal,
                                        unfocusedBorderColor = colores.cardBorde,
                                        focusedContainerColor = colores.fondoBase,
                                        unfocusedContainerColor = colores.fondoBase
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (excede) {
                            Text(
                                "Ojo: este monto supera el total de la compra ($simboloMoneda " + String.format(
                                    java.util.Locale.US, "%.2f", estado.montoMaximoActual
                                ) + ")",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                    fontSize = s.textBody.value.sp * 0.9f,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colores.estadoPeligro
                            )
                        }
                    }
                }
            }
        }

        // Guía y estado de distribución en vivo cuando hay 2 o más métodos
        if (estado.filas.size > 1) {
            val estaExcedido = estado.sumaExcedeTotal || estado.algunaPorcionExcede
            val estaCompleto = estado.cuadra && kotlin.math.abs(estado.sumaPorciones - estado.montoMaximoActual) <= 0.01
            Surface(
                color = when {
                    estaExcedido -> colores.peligroSutil
                    estaCompleto -> colores.estadoExito.copy(alpha = 0.12f)
                    else -> colores.cardBase
                },
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(
                    s.borderWidth,
                    when {
                        estaExcedido -> colores.estadoPeligro.copy(alpha = 0.5f)
                        estaCompleto -> colores.estadoExito.copy(alpha = 0.5f)
                        else -> colores.cardBorde
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Icon(
                        imageVector = when {
                            estaExcedido -> Icons.Default.WarningAmber
                            estaCompleto -> Icons.Default.Check
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when {
                            estaExcedido -> colores.estadoPeligro
                            estaCompleto -> colores.estadoExito
                            else -> colores.textoTerciario
                        },
                        modifier = Modifier.size(s.iconSmall)
                    )
                    Text(
                        text = estado.estadoVerificacion,
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textBody.value.sp * 0.92f,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            estaExcedido -> colores.estadoPeligro
                            estaCompleto -> colores.estadoExito
                            else -> colores.textoSecundario
                        }
                    )
                }
            }
        }

    }
}
