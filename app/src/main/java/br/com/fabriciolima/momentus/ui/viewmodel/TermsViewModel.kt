package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.fabriciolima.momentus.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TermsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    suspend fun acceptTerms() {
        userRepository.acceptTerms()
    }
}
