package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

/**
 * Factory para criar instâncias de CalendarViewModel com as dependências necessárias.
 * NOTA: Esta classe é provavelmente um legado, pois o Hilt já gerencia a criação de ViewModels.
 */
class CalendarViewModelFactory(
    private val repository: RotinaRepository,
    private val eventoRepository: EventoRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val auth: FirebaseAuth,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(repository, eventoRepository, googleSignInClient, auth, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
