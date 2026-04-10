package br.com.fabriciolima.momentus.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _signUpResult = MutableSharedFlow<Result<Unit>>()
    val signUpResult = _signUpResult.asSharedFlow()

    fun signUpUser(
        name: String,
        email: String,
        pass: String,
    ) {
        viewModelScope.launch {
            firebaseAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: run {
                        viewModelScope.launch { 
                            _signUpResult.emit(Result.Error(AppError.AuthError("Falha ao obter ID do usuário.")))
                        }
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
                            viewModelScope.launch { _signUpResult.emit(Result.Success(Unit)) }
                         }
                        .addOnFailureListener { e -> 
                            Log.w("SignUpViewModel", "Falha ao salvar dados do usuário no Firestore", e)
                            viewModelScope.launch { _signUpResult.emit(Result.Error(AppError.UnknownError(e))) }
                        }
                }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
                        is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres."
                        else -> e.message ?: "Ocorreu uma falha no cadastro. Tente novamente."
                    }
                     viewModelScope.launch { _signUpResult.emit(Result.Error(AppError.AuthError(message))) }
                }
        }
    }
}
