package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.animacion

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DetailSwipeState(
    val screenWidthPx: Float,
    val closeThresholdPx: Float,
    val coroutineScope: CoroutineScope,
    val onClose: () -> Unit
) {
    var offsetX by mutableFloatStateOf(0f)
        private set

    val dragProgress: Float
        get() = (offsetX / screenWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

    suspend fun handleDragEnd() {
        if (offsetX > closeThresholdPx) {
            animate(
                initialValue = offsetX,
                targetValue = screenWidthPx,
                animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)
            ) { value, _ -> offsetX = value }
            onClose()
        } else {
            animate(
                initialValue = offsetX,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)
            ) { value, _ -> offsetX = value }
        }
    }

    fun updateOffset(delta: Float) {
        offsetX = (offsetX + delta).coerceAtLeast(0f)
    }
}

@Composable
fun rememberDetailSwipeState(onClose: () -> Unit): DetailSwipeState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val closeThresholdPx = with(density) { 120.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    return remember(screenWidthPx, closeThresholdPx, coroutineScope, onClose) {
        DetailSwipeState(screenWidthPx, closeThresholdPx, coroutineScope, onClose)
    }
}

fun Modifier.detailSwipeInput(state: DetailSwipeState): Modifier = this.pointerInput(state) {
    detectHorizontalDragGestures(
        onDragEnd = {
            state.coroutineScope.launch {
                state.handleDragEnd()
            }
        },
        onHorizontalDrag = { _, dragAmount ->
            state.updateOffset(dragAmount)
        }
    )
}
