package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

@Composable
fun DetailPresentacionesTable(product: MoldeProductos) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cabecera Técnica
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESENTACIÓN COMERCIAL",
                color = SaaSTextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.width(90.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "PRECIO VENTA",
                    color = SaaSTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        HorizontalDivider(color = SaaSBorder, thickness = 0.5.dp)

        product.presentaciones.forEach { pres ->
            val isPrincipal = product.presentacionPrincipalId == pres.presentacionId
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val nombrePresentacion = if (pres.cantidad > 0 && product.inventarioPerfilUnidadContenido.isNotBlank()) {
                    "${pres.nombre} í— ${ProductDetailMapper.formatoSinDecimalesInnecesarios(pres.cantidad.toDouble())} ${product.inventarioPerfilUnidadContenido}"
                } else pres.nombre
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = nombrePresentacion.uppercase(),
                            color = SaaSTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isPrincipal) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isPrincipal) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = SaaSPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(0.5.dp, SaaSPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "PREDETERMINADA", 
                                    color = SaaSPrimary, 
                                    fontSize = 7.sp, 
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                Box(Modifier.width(90.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "$${String.format("%.2f", pres.precioventa)}",
                        color = SaaSTextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = SaaSBorder.copy(alpha = 0.04f), thickness = 0.5.dp)
        }
    }
}
