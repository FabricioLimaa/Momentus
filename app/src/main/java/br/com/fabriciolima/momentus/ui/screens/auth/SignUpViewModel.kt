package br.com.fabriciolima.momentus.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface SignUpNavigationEvent {
    object NavigateToLogin : SignUpNavigationEvent
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SignUpNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun onSignUpClicked(name: String, email: String, pass: String) {
        _uiState.update { it.copy(isLoading = true) }
        
        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "Falha ao obter ID do usuário.") }
                    return@addOnSuccessListener
                }
                
                val user = mapOf(
                    "display_name" to name,
                    "email" to email,
                    "points" to 0
                )

                firestore.collection("users").document(userId).set(user)
                    .addOnSuccessListener { 
                        Log.d("SignUpViewModel", "Usuário criado com sucesso no Firestore")
                        viewModelScope.launch {
                             _navigationEvent.emit(SignUpNavigationEvent.NavigateToLogin)
                        }
                    }
                    .addOnFailureListener { e -> 
                        Log.w("SignUpViewModel", "Falha ao salvar dados do usuário no Firestore", e)
                        _uiState.update { it.copy(isLoading = false, error = "Falha ao salvar dados do usuário.") }
                    }
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres."
                    else -> e.message ?: "Ocorreu uma falha no cadastro."
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
