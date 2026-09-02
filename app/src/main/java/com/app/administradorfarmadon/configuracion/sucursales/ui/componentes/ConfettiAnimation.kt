package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ParticulaConfeti(
    val xRelativa: Float,
    val yVelocidad: Float,
    val xVariacion: Float,
    val tamano: Float,
    val color: Color,
    val rotacionInicial: Float,
    val velocidadRotacion: Float
)

@Composable
fun ConfettiAnimation(
    mostrar: Boolean,
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!mostrar) return

    val particulas = remember {
        val coloresConfeti = listOf(
            Color(0xFF10B981), // Esmeralda Farmadon
            Color(0xFF3B82F6), // Azul
            Color(0xFFF59E0B), // Dorado
            Color(0xFFEC4899), // Rosa
            Color(0xFF8B5CF6), // Violeta
            Color(0xFF06B6D4)  // Cyan
        )
        List(60) {
            ParticulaConfeti(
                xRelativa = Random.nextFloat(),
                yVelocidad = Random.nextFloat() * 0.8f + 0.5f,
                xVariacion = (Random.nextFloat() - 0.5f) * 120f,
                tamano = Random.nextFloat() * 12f + 8f,
                color = coloresConfeti[Random.nextInt(coloresConfeti.size)],
                rotacionInicial = Random.nextFloat() * 360f,
                velocidadRotacion = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    val animacionProgreso = remember { Animatable(0f) }

    LaunchedEffect(mostrar) {
        if (mostrar) {
            animacionProgreso.snapTo(0f)
            animacionProgreso.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2400, easing = LinearOutSlowInEasing)
            )
            onTerminado()
        }
    }

    val progreso = animacionProgreso.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val anchoTotal = size.width
        val altoTotal = size.height

        particulas.forEach { p ->
            val y = progreso * altoTotal * p.yVelocidad * 1.2f
            val x = (p.xRelativa * anchoTotal) + (p.xVariacion * progreso)
            val alfa = (1f - progreso).coerceIn(0f, 1f)

            rotate(degrees = p.rotacionInicial + (p.velocidadRotacion * progreso), pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alfa),
                    topLeft = Offset(x, y),
                    size = Size(p.tamano, p.tamano * 0.6f)
                )
            }
        }
    }
}
