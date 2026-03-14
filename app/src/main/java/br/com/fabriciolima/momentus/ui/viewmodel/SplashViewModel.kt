package br.com.fabriciolima.momentus.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val syncMessage = categoryRepository.syncMessage

    enum class UserStatus {
        NOT_LOGGED_IN,
        ONBOARDING_INCOMPLETE,
        TERMS_NOT_ACCEPTED,
        LOGGED_IN
    }

    suspend fun checkUserStatus(): UserStatus {
        val onboardingCompleted = context.dataStore.data
            .map { preferences ->
                preferences[ONBOARDING_COMPLETED] ?: false
            }.first()

        if (!onboardingCompleted) {
            return UserStatus.ONBOARDING_INCOMPLETE
        }
        
        return if (auth.currentUser == null) {
            UserStatus.NOT_LOGGED_IN
        } else {
            // DISPARO ASSÍNCRONO: Não aguardamos a sincronização para decidir a rota.
            // O CalendarViewModel já cuidará da sincronização ao iniciar.
            if (userRepository.hasAcceptedTerms()) {
                UserStatus.LOGGED_IN
            } else {
                UserStatus.TERMS_NOT_ACCEPTED
            }
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit {
            it[ONBOARDING_COMPLETED] = true
        }
    }
}
