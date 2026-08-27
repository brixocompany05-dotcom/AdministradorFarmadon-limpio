package com.app.administradorfarmadon.configuracion.usuarios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.usuarios.datos.RolFarmacia
import com.app.administradorfarmadon.configuracion.usuarios.datos.UsuarioFarmacia
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun UsuariosFilterTabs(
    selectedRol: String?,
    roles: List<RolFarmacia>,
    usuarios: List<UsuarioFarmacia>,
    onRolSelected: (String?) -> Unit,
    verDadasDeBaja: Boolean = false,
    totalDadasDeBaja: Int = 0,
    onToggleDadasDeBaja: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listaActivos = usuarios.filter { !it.dadoDeBaja }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Fila ESTABLE: las pestañas nunca se intercambian entre sí.
        // DADOS DE BAJA es una pestaña hermana al final, no un modo que
        // reemplace a las demás (cero sub-menus incómodos).

        val isTodosSelected = !verDadasDeBaja && selectedRol == null
        TabItem(
            label = "TODOS",
            count = listaActivos.size,
            isSelected = isTodosSelected,
            onClick = {
                onToggleDadasDeBaja() // apaga verDadasDeBaja si estaba encendida
                onRolSelected(null)
            }
        )

        roles.forEach { rol ->
            val isSelected = !verDadasDeBaja && selectedRol == rol.nombre
            val count = listaActivos.count { it.rolNombre.equals(rol.nombre, ignoreCase = true) || it.rolId == rol.id }
            TabItem(
                label = rol.nombre,
                count = count,
                isSelected = isSelected,
                onClick = {
                    onToggleDadasDeBaja()
                    onRolSelected(rol.nombre)
                }
            )
        }

        TabItem(
            label = "DADOS DE BAJA",
            count = totalDadasDeBaja,
            isSelected = verDadasDeBaja,
            onClick = onToggleDadasDeBaja
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colores = TokensFarmadon.colores

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = if (isSelected) colores.textoPrincipal else colores.textoTerciario
            )

            if (count > 0) {
                Text(
                    text = "[ $count ]",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.5f)
                )
            }
        }

        // Línea indicadora inferior (Estilo Maestro)
        Box(
            modifier = Modifier
                .width(if (isSelected) 20.dp else 0.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(colores.textoPrincipal)
        )
    }
}
