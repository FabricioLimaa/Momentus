package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.theme.*

import kotlinx.coroutines.launch

@Composable
fun SplashScreen(syncMessage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Animação do Glow (Pulso)
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Animações de Entrada (Logo subindo e Fade In)
    val startAnimation = remember { Animatable(0f) }
    val offsetY = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        // Executa em paralelo
        launch {
            startAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(1500, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepNavyBackground, MaterialTheme.colorScheme.onPrimary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glow Verde ao Fundo
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(glowScale)
                .alpha(glowAlpha * startAnimation.value)
                .blur(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(y = offsetY.value.dp)
                .alpha(startAnimation.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_logo_round),
                contentDescription = "Logo Momentus",
                modifier = Modifier
                    .size(140.dp)
                    .scale(startAnimation.value.coerceAtLeast(0.8f))
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Momentus",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimaryDark,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Organize suas metas, conquiste seu dia.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondaryDark,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Mensagem de sincronização discreta
            Text(
                text = syncMessage.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
