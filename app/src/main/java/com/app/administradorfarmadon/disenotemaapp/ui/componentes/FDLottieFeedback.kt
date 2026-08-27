package com.app.administradorfarmadon.disenotemaapp.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

/**
 * Componente unificado para renderizar animaciones Lottie con control de fotogramas y fallback visual.
 */
@Composable
fun FDLottieFeedback(
    resId: Int,
    isLoop: Boolean = true,
    size: Dp = 90.dp,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (isLoop) LottieConstants.IterateForever else 1,
        isPlaying = true
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                color = FDColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
