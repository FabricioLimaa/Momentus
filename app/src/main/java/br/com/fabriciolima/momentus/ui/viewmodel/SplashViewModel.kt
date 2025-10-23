package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.fabriciolima.momentus.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    enum class UserStatus {
        NOT_LOGGED_IN,
        TERMS_NOT_ACCEPTED,
        LOGGED_IN
    }

    suspend fun checkUserStatus(): UserStatus {
        return if (auth.currentUser == null) {
            UserStatus.NOT_LOGGED_IN
        } else {
            if (userRepository.hasAcceptedTerms()) {
                UserStatus.LOGGED_IN
            } else {
                UserStatus.TERMS_NOT_ACCEPTED
            }
        }
    }
}
