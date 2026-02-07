package br.com.fabriciolima.momentus.ui.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.UserPreferences
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
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

private const val TAG = "LoginViewModel"

data class LoginUiState(
    val isLoading: Boolean = false,
    val userPreferences: UserPreferences? = null,
    val error: String? = null
)

sealed class LoginEvent {
    data object NavigateToMain : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(userPreferences = prefs) }
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun signInWithEmail(email: String, password: String, rememberMe: Boolean) {
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
                        Log.d(TAG, "Email/Senha Auth SUCESSO. UID: ${firebaseUser.uid}")
                        _loginEvent.emit(LoginEvent.NavigateToMain)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        _loginEvent.emit(LoginEvent.ShowError("Falha ao obter dados do usuário."))
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false) }
                val message = when (e) {
                    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
                    else -> "Falha na autenticação. Tente novamente."
                }
                viewModelScope.launch {
                    _loginEvent.emit(LoginEvent.ShowError(message))
                }
                Log.w(TAG, "Email/Senha Auth FALHA", e)
            }
    }

    fun handleGoogleSignInResult(intent: Intent) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            _uiState.update { it.copy(isLoading = false) }
            Log.w(TAG, "Falha no login com Google: code=" + e.statusCode)
            viewModelScope.launch {
                _loginEvent.emit(LoginEvent.ShowError("Falha ao obter conta Google."))
            }
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
                        Log.d(TAG, "Firebase Auth SUCESSO. UID: ${firebaseUser.uid}")
                        _loginEvent.emit(LoginEvent.NavigateToMain)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        _loginEvent.emit(LoginEvent.ShowError("Falha ao obter dados do usuário do Firebase."))
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false) }
                Log.e(TAG, "Firebase Auth FALHA", e)
                 viewModelScope.launch {
                    _loginEvent.emit(LoginEvent.ShowError("Falha na autenticação com Firebase."))
                }
            }
    }

    fun updatePreferences(email: String, rememberMe: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserEmail(email)
            userPreferencesRepository.updateRememberMe(rememberMe)
        }
    }
    
    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
}
