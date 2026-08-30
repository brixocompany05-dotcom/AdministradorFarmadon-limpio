package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

/**
 * Tarjeta de Acción Geográfica para Selección de Dirección.
 * Elimina la estética de "campo de texto" y destaca la importancia de la ubicación.
 */
@Composable
fun FDTarjetaDireccionProfesional(
    direccion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    error: String? = null,
    advertencia: String? = null,
    s: MedidaAdaptativa
) {
    val colores = TokensFarmadon.colores
    val estaVacia = direccion.isBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(s.gapTiny)
    ) {
        Text(
            text = "DIRECCIí“N DE LA FARMACIA",
            style = TokensFarmadon.tipografia.etiqueta.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = if (error != null) colores.estadoPeligro else colores.textoTerciario
        )

        Surface(
            onClick = onClick,
            enabled = habilitado,
            shape = TokensFarmadon.formas.grande,
            color = when {
                !habilitado -> colores.superficieDefecto.copy(alpha = 0.5f)
                !estaVacia -> colores.botonPrimarioFondo.copy(alpha = 0.04f)
                else -> colores.inputFondo
            },
            border = BorderStroke(
                width = 0.5.dp,                color = when {
                    error != null -> colores.estadoPeligro
                    advertencia != null -> colores.estadoAlerta
                    !estaVacia -> colores.botonPrimarioFondo.copy(alpha = 0.3f)
                    else -> colores.inputBorde.copy(alpha = 0.5f)                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = s.padCard, vertical = s.padCardLarge * 0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icono representativo (Mapa o Ubicación)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (estaVacia) Icons.Default.Map else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (estaVacia) colores.textoTerciario else colores.botonPrimarioFondo,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (estaVacia) {
                        Text(
                            text = "Tocar para ubicar en el mapa",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 16.sp),
                            color = if (habilitado) colores.textoPrincipal else colores.textoTerciario
                        )
                        Text(
                            text = "Selecciona la posición exacta de tu negocio",
                            style = TokensFarmadon.tipografia.leyenda,
                            color = colores.textoTerciario
                        )
                    } else {
                        Text(
                            text = direccion,
                            style = TokensFarmadon.tipografia.cuerpo.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = colores.textoPrincipal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colores.estadoExito,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Ubicación Verificada",
                                style = TokensFarmadon.tipografia.leyenda.copy(fontWeight = FontWeight.Bold),
                                color = colores.estadoExito
                            )
                        }
                    }
                }

                if (habilitado) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (estaVacia) colores.textoTerciario.copy(alpha = 0.3f) else colores.botonPrimarioFondo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = TokensFarmadon.tipografia.leyenda,
                color = colores.estadoPeligro,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else if (advertencia != null) {
            Text(
                text = advertencia,
                style = TokensFarmadon.tipografia.leyenda,
                color = colores.estadoAlerta,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
