package br.com.fabriciolima.momentus.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.ui.AppScaffold
import br.com.fabriciolima.momentus.ui.Screen
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.SplashViewModel
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: SplashViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        setContent {
            // Observa as preferências globais para aplicar o tema dinâmico
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = null)

            MomentusTheme(
                overrideThemeMode = preferences?.themeMode,
                overridePrimaryColorHex = preferences?.primaryColorHex,
                overrideCornerRadiusDp = preferences?.cornerRadiusDp,
                overrideFontSizeMultiplier = preferences?.fontSizeMultiplier
            ) {
                val syncMessage by viewModel.syncMessage.collectAsState(initial = "Iniciando...")
                
                val startDestination by produceState<String?>(initialValue = null, viewModel) {
                    value = when (viewModel.checkUserStatus()) {
                        SplashViewModel.UserStatus.ONBOARDING_INCOMPLETE -> Screen.Onboarding.route
                        SplashViewModel.UserStatus.NOT_LOGGED_IN -> Screen.Login.route
                        SplashViewModel.UserStatus.TERMS_NOT_ACCEPTED -> Screen.Terms.route
                        else -> Screen.Calendar.route
                    }
                }

                if (startDestination == null) {
                    SplashScreen(syncMessage = syncMessage)
                } else {
                    val destination = startDestination!!
                    val calendarViewModel: CalendarViewModel = hiltViewModel()
                    val googleAccount = GoogleSignIn.getLastSignedInAccount(this)

                    AppScaffold(
                        startDestination = destination,
                        googleAccount = googleAccount,
                        inAppUpdateManager = inAppUpdateManager,
                        onLogout = { calendarViewModel.logout() }
                    )
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregisterListener()
    }
}
