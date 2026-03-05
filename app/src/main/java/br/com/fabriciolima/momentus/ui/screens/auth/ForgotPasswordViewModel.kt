package br.com.fabriciolima.momentus.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onSendPasswordResetEmail(email: String) {
        _uiState.update { it.copy(isLoading = true) }
        
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener { 
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        successMessage = "E-mail de recuperação enviado para $email!"
                    )
                }
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is FirebaseAuthInvalidUserException -> "Nenhuma conta encontrada com este e-mail."
                    else -> "Falha ao enviar e-mail de recuperação."
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
