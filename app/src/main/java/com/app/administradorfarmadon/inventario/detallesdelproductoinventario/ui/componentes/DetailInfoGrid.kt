package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailInfoGrid(product: MoldeProductos) {
    val singular = product.inventarioPerfilUnidadSingular.ifBlank { product.unidadVisualInventario.ifBlank { "unidad" } }
    val contenido = product.inventarioPerfilContenidoPorUnidad
    val unidadContenido = product.inventarioPerfilUnidadContenido
    
    val perfilVisual = if (contenido.isNotBlank() && unidadContenido.isNotBlank()) {
        "1 $singular ($contenido $unidadContenido)"
    } else {
        product.inventarioPerfilResumen.ifBlank { "1 $singular" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Primera Fila: Identidad Técnica (3 Columnas)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoField("CÓDIGO DE BARRAS", product.codigo.ifBlank { "SIN CÓDIGO" }, Modifier.weight(1f))
            InfoField("UBICACIÓN", product.ubicacion.ifBlank { product.ubicacionId }.ifBlank { "GENERAL" }.uppercase(), Modifier.weight(0.7f))
            val controlLabel = if (product.clasificacionControl.uppercase() == "CONTROLADO" || product.requiereReceta) "SÍ" else "NO"
            InfoField("RECETA", controlLabel, Modifier.weight(0.6f))
        }

        // Segunda Fila: Perfil y Clasificación
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoField("PERFIL INVENTARIO", perfilVisual.uppercase(), Modifier.weight(1.2f))
            val concentracion = if (product.concentracion.isNotBlank()) "${product.concentracion} ${product.concentracionUnidad}" else "Sin especificar"
            InfoField("CONCENTRACIÓN", concentracion.uppercase(), Modifier.weight(1f))
            val estadoStr = if (product.activo) "ACTIVO" else "INACTIVO"
            InfoField("ESTADO", estadoStr, Modifier.weight(0.7f))
        }

        // Categorías (Estilo SaaS Premium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CATEGORIZACIÓN",
                color = SaaSTextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Priorizar categoriasLista si existe, de lo contrario usar categoriaPrincipal
                val todasLasCategorias = product.categoriasLista.ifEmpty { 
                    listOf(product.categoriaPrincipal).filter { it.isNotBlank() } 
                }.distinct()

                if (todasLasCategorias.isEmpty()) {
                    Text(
                        text = "SIN CATEGORÍA ASIGNADA",
                        color = SaaSTextSecondary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    todasLasCategorias.forEachIndexed { index, cat ->
                        val isPrincipal = index == 0
                        Surface(
                            color = if (isPrincipal) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (isPrincipal) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = cat.uppercase(),
                                color = if (isPrincipal) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = if (isPrincipal) FontWeight.Black else FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label, 
            color = SaaSTextSecondary, 
            fontSize = 7.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 1.sp
        )
        Text(
            text = value, 
            color = SaaSTextPrimary, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Medium, 
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis
        )
    }
}
