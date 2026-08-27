package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.25f)
) {
    Canvas(modifier = modifier.size(width = 48.dp, height = 16.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height * 0.7f)
            quadraticTo(size.width * 0.25f, size.height * 0.2f, size.width * 0.5f, size.height * 0.5f)
            quadraticTo(size.width * 0.75f, size.height * 0.8f, size.width, size.height * 0.1f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
    }
}
