package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import br.com.fabriciolima.momentus.R

@Composable
fun SuccessCelebration(
    onFinished: () -> Unit
) {
    // Detecta se o sistema está em modo escuro
    val isDarkTheme = isSystemInDarkTheme()

    // Escolhe o arquivo Lottie com base no tema
    val lottieResId = if (isDarkTheme) {
        R.raw.success_dark
    } else {
        R.raw.success_light
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieResId))
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    LaunchedEffect(progress) {
        if (progress == 1f) {
            onFinished()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(300.dp)
        )
    }
}
