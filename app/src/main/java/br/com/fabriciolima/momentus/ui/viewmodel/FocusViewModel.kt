package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val timeLeftSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val mode: FocusMode = FocusMode.FOCUS,
    val completedSessions: Int = 0,
    val xpGained: Int = 0
)

enum class FocusMode {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            onTimerFinished()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        pauseTimer()
        val defaultSeconds = when (_uiState.value.mode) {
            FocusMode.FOCUS -> 25 * 60
            FocusMode.SHORT_BREAK -> 5 * 60
            FocusMode.LONG_BREAK -> 15 * 60
        }
        _uiState.update { it.copy(timeLeftSeconds = defaultSeconds, totalSeconds = defaultSeconds) }
    }

    private fun onTimerFinished() {
        _uiState.update { it.copy(isRunning = false) }
        
        if (_uiState.value.mode == FocusMode.FOCUS) {
            val sessions = _uiState.value.completedSessions + 1
            val xpReward = 10
            
            _uiState.update { 
                it.copy(
                    completedSessions = sessions,
                    xpGained = it.xpGained + xpReward
                )
            }
            
            viewModelScope.launch {
                userRepository.incrementPoints(xpReward.toLong())
            }

            // Auto-switch to break
            if (sessions % 4 == 0) setMode(FocusMode.LONG_BREAK) 
            else setMode(FocusMode.SHORT_BREAK)
        } else {
            setMode(FocusMode.FOCUS)
        }
    }

    fun setMode(mode: FocusMode) {
        pauseTimer()
        val seconds = when (mode) {
            FocusMode.FOCUS -> 25 * 60
            FocusMode.SHORT_BREAK -> 5 * 60
            FocusMode.LONG_BREAK -> 15 * 60
        }
        _uiState.update { 
            it.copy(
                mode = mode, 
                timeLeftSeconds = seconds, 
                totalSeconds = seconds,
                isRunning = false
            )
        }
    }
}
