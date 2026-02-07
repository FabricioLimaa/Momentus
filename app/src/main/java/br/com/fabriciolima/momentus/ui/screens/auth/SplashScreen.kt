/*package br.com.fabriciolima.momentus.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.screens.Routes
import br.com.fabriciolima.momentus.ui.viewmodel.SplashNavigationEvent
import br.com.fabriciolima.momentus.ui.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val syncMessage by viewModel.syncMessage.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    // 1. Observa o evento de navegação.
    // Quando o viewModel definir a rota, este efeito será acionado.
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is SplashNavigationEvent.NavigateTo -> {
                Log.d("NAV_DEBUG", "Evento de navegação detectado: ${event.route}. Navegando...")
                navController.navigate(event.route) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                    launchSingleTop = true
                }
                viewModel.onNavigationConsumed()
            }
            null -> { /* Estado inicial ou evento já consumido, não faz nada */ }
        }
    }

    // 2. Informa ao ViewModel que a UI está pronta.
    // Isso garante que a lógica de decisão só comece depois que a UI estiver ouvindo.
    LaunchedEffect(Unit) {
        Log.d("NAV_DEBUG", "SplashScreen UI está pronta. Sinalizando ao ViewModel.")
        viewModel.onUiReady()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_logo_round),
            contentDescription = "Logo Momentus",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = syncMessage, style = MaterialTheme.typography.bodyLarge)
    }
}
*/