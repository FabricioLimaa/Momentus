package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.AppThemeMode
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.domain.usecase.ClearCloudDataUseCase
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.util.SyncManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val primaryColorHex: String = "#3DDC84", // Verde padrão
    val cornerRadius: Int = 12,
    val fontSizeMultiplier: Float = 1.0f,
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val userEmail: String = "",
    val lastSyncTime: String = "Nunca",
    val isLoading: Boolean = false,
    val isDataDeleted: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val clearCloudDataUseCase: ClearCloudDataUseCase,
    private val syncManager: SyncManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentPreferences()
    }

    private fun loadCurrentPreferences() {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val syncTime = if (prefs.lastSyncTimestamp > 0) dateFormat.format(Date(prefs.lastSyncTimestamp)) else "Nunca"
                
                _uiState.update { it.copy(
                    themeMode = prefs.themeMode,
                    primaryColorHex = prefs.primaryColorHex ?: "#3DDC84",
                    cornerRadius = prefs.cornerRadiusDp,
                    fontSizeMultiplier = prefs.fontSizeMultiplier,
                    animationsEnabled = prefs.animationsEnabled,
                    soundEnabled = prefs.soundEnabled,
                    hapticEnabled = prefs.hapticEnabled,
                    userEmail = auth.currentUser?.email ?: "Não conectado",
                    lastSyncTime = syncTime
                )}
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) { _uiState.update { it.copy(themeMode = mode) } }
    fun setPrimaryColor(hex: String) { _uiState.update { it.copy(primaryColorHex = hex) } }
    fun setCornerRadius(dp: Int) { _uiState.update { it.copy(cornerRadius = dp) } }
    fun setFontSize(multiplier: Float) { _uiState.update { it.copy(fontSizeMultiplier = multiplier) } }
    fun setAnimationsEnabled(enabled: Boolean) { _uiState.update { it.copy(animationsEnabled = enabled) } }
    fun setSoundEnabled(enabled: Boolean) { _uiState.update { it.copy(soundEnabled = enabled) } }
    fun setHapticEnabled(enabled: Boolean) { _uiState.update { it.copy(hapticEnabled = enabled) } }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            syncManager.enqueueSync()
            _uiState.update { it.copy(isLoading = false, successMessage = "Sincronização iniciada!") }
        }
    }

    fun saveThemeSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            preferencesRepository.updateThemeMode(state.themeMode)
            preferencesRepository.updatePrimaryColor(state.primaryColorHex)
            preferencesRepository.updateCornerRadius(state.cornerRadius)
            preferencesRepository.updateFontSize(state.fontSizeMultiplier)
            preferencesRepository.updateAnimationsEnabled(state.animationsEnabled)
            preferencesRepository.updateSoundEnabled(state.soundEnabled)
            preferencesRepository.updateHapticEnabled(state.hapticEnabled)
            _uiState.update { it.copy(successMessage = "Configurações salvas!") }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.clear()
            _uiState.update { it.copy(successMessage = "Configurações resetadas!") }
        }
    }

    fun clearAllCloudData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = clearCloudDataUseCase()
            if (result is Result.Success) {
                categoryRepository.clearAllLocalData()
                _uiState.update { it.copy(isLoading = false, isDataDeleted = true, successMessage = "Dados da nuvem excluídos!") }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao excluir dados da nuvem.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
