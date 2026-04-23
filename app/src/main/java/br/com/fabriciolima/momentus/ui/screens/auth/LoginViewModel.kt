package br.com.fabriciolima.momentus.ui.screens.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.AppThemeMode
import br.com.fabriciolima.momentus.data.repository.UserPreferences
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userPreferences: UserPreferences = UserPreferences(
        email = "", 
        rememberMe = false, 
        lastAnimationDate = 0L,
        themeMode = AppThemeMode.SYSTEM,
        primaryColorHex = null,
        cornerRadiusDp = 12,
        fontSizeMultiplier = 1.0f,
        animationsEnabled = true,
        soundEnabled = true,
        hapticEnabled = true,
        lastSyncTimestamp = 0L
    )
)

sealed interface NavigationEvent {
    object NavigateToCalendar : NavigationEvent
    data class StartGoogleSignIn(val intent: Intent) : NavigationEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(userPreferences = prefs) }
            }
        }
    }

    fun onGoogleSignInClicked(googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.StartGoogleSignIn(googleSignInClient.signInIntent))
        }
    }

    fun onGoogleSignInResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInTask(task)
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Login com Google cancelado.") }
        }
    }

    private fun handleGoogleSignInTask(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w("LoginViewModel", "Falha no login com Google: code=" + e.statusCode)
            _uiState.update { it.copy(isLoading = false, error = "Falha ao obter conta Google.") }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    viewModelScope.launch {
                        userRepository.createOrUpdateUser(firebaseUser)
                        userPreferencesRepository.clear()
                        _navigationEvent.emit(NavigationEvent.NavigateToCalendar)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Falha ao obter dados do usuário do Firebase.") }
                }
            }
            .addOnFailureListener { e ->
                 _uiState.update { it.copy(isLoading = false, error = "Falha na autenticação com Firebase.") }
            }
    }

    fun onEmailSignInClicked(email: String, password: String, rememberMe: Boolean) {
        _uiState.update { it.copy(isLoading = true) }
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    viewModelScope.launch {
                        if (rememberMe) {
                            userPreferencesRepository.updateUserEmail(email)
                            userPreferencesRepository.updateRememberMe(true)
                        } else {
                            userPreferencesRepository.clear()
                        }
                        userRepository.createOrUpdateUser(firebaseUser)
                        _navigationEvent.emit(NavigationEvent.NavigateToCalendar)
                    }
                } else {
                     _uiState.update { it.copy(isLoading = false, error = "Falha ao obter dados do usuário do Firebase.") }
                }
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
                    else -> "Falha na autenticação."
                }
                 _uiState.update { it.copy(isLoading = false, error = message) }
            }
    }

    fun onUpdatePreferences(email: String, rememberMe: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserEmail(email)
            userPreferencesRepository.updateRememberMe(rememberMe)
        }
    }
    
    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
