package com.app.administradorfarmadon.organizacion.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDDialogoContenedor
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDSelectorProfesional
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises
import com.app.administradorfarmadon.organizacion.datos.PaisInfo

/** Bandera del país como emoji Unicode nativo (sin imágenes ni librerías). */
@Composable
fun BanderaPais(pais: PaisInfo, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 30.dp, height = 20.dp)
            .background(TokensFarmadon.colores.superficieElevada, RoundedCornerShape(4.dp))
            .border(0.8.dp, TokensFarmadon.colores.cardBorde, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = pais.banderaEmoji,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Componente "llave en mano" que utiliza el Selector Profesional para mostrar el país.
 */
@Composable
fun SelectorPaisProfesional(
    paisIso: String,
    onPaisSeleccionado: (PaisInfo) -> Unit,
    habilitado: Boolean = true,
    error: String? = null,
    advertencia: String? = null,
    esObligatorio: Boolean = false
) {
    var mostrarDialogo by remember { mutableStateOf(value = false) }
    val paisActual = remember(paisIso) { CatalogoPaises.porIso(paisIso) }

    FDSelectorProfesional(
        etiqueta = "País de Operación",
        valor = paisActual?.nombre ?: "Seleccionar país...",
        onClick = { mostrarDialogo = true },
        habilitado = habilitado,
        iconoInicio = Icons.Default.Public,
        esObligatorio = esObligatorio,
        error = error,
        advertencia = advertencia,
        contenidoValor = if (paisActual != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BanderaPais(paisActual)
                    Text(
                        text = paisActual.nombre,
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.Bold),
                        color = TokensFarmadon.colores.botonPrimarioFondo
                    )
                }
            }
        } else null
    )

    DialogoSeleccionPais(
        visible = mostrarDialogo,
        onSeleccionar = onPaisSeleccionado,
        onDismiss = { mostrarDialogo = false }
    )
}

/**
 * Diálogo de selección de país para Tablet SaaS —” catálogo propio de BRIXO
 * (7 países operativos), sin contexto ni librerías externas.
 */
@Composable
fun DialogoSeleccionPais(
    visible: Boolean,
    onSeleccionar: (PaisInfo) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val colores = TokensFarmadon.colores
    var filtroInterno by remember { mutableStateOf("") }
    val catalogo = remember { CatalogoPaises.todos() }

    FDDialogoContenedor(
        titulo = "Seleccionar País",
        subtitulo = "Elige el país donde opera tu farmacia: define moneda, teléfono y documento.",
        onDismiss = onDismiss,
        iconoCabecera = Icons.Default.Public,
        anchoMaximo = 680.dp
    ) {
        // Regla "Bloquear Teclado": este diálogo tampoco abre el teclado si está activada.
        AplicarBloqueoTecladoVentana()
        OutlinedTextField(
            value = filtroInterno,
            onValueChange = { filtroInterno = it },
            placeholder = { Text("Buscar por nombre o código ISO...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = colores.textoTerciario) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colores.botonPrimarioFondo,
                unfocusedBorderColor = colores.cardBorde,
                focusedContainerColor = colores.inputFondo,
                unfocusedContainerColor = colores.inputFondo
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        )

        Spacer(Modifier.height(16.dp))

        val lista = remember(filtroInterno) {
            catalogo.filter {
                it.nombre.contains(filtroInterno.trim(), ignoreCase = true) ||
                        it.iso.contains(filtroInterno.trim(), ignoreCase = true)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lista, key = { it.iso }) { pais ->
                Surface(
                    onClick = { onSeleccionar(pais); onDismiss() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BanderaPais(pais)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pais.nombre,
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.Medium),
                                color = colores.textoPrincipal
                            )
                            Text(
                                text = "${pais.documentoNombre} · ${pais.prefijoTel}",
                                style = TokensFarmadon.tipografia.leyenda,
                                color = colores.textoTerciario
                            )
                        }
                        Surface(
                            color = colores.botonPrimarioFondo.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${pais.monedaIso} ${pais.simboloMoneda}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold),
                                color = colores.botonPrimarioFondo
                            )
                        }
                    }
                }
            }

            if (lista.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron países que coincidan con \"$filtroInterno\"",
                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                            color = colores.textoTerciario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
