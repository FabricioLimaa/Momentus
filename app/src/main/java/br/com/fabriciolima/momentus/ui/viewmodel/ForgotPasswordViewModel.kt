package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _resetPasswordResult = MutableSharedFlow<Result<Unit>>()
    val resetPasswordResult = _resetPasswordResult.asSharedFlow()

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener { 
                    viewModelScope.launch { _resetPasswordResult.emit(Result.Success(Unit)) }
                }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthInvalidUserException -> "Nenhuma conta encontrada com este e-mail."
                        else -> "Falha ao enviar e-mail de recuperação. Tente novamente."
                    }
                    viewModelScope.launch { _resetPasswordResult.emit(Result.Error(Exception(message))) }
                }
        }
    }
}
