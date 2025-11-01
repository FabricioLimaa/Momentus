package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.domain.AchievementsList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

data class AchievementUiInfo( 
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val unlockedDate: Date? = null
)

data class AchievementsUiState(
    val achievements: List<AchievementUiInfo> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val streakCount: Int = 0,
    val points: Int = 0,
    val selectedAchievement: AchievementUiInfo? = null // Para o dialog
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    gamificationRepository: GamificationRepository,
    categoryRepository: CategoryRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        combine(
            gamificationRepository.unlockedAchievements,
            categoryRepository.currentStreak,
            userRepository.userData
        ) { unlockedAchievements, streak, userData ->
            val unlockedMap = unlockedAchievements.associateBy { it.achievementId }
            val allAchievementsInfo = AchievementsList.allAchievements.map { achievement ->
                val unlockedInfo = unlockedMap[achievement.id]
                AchievementUiInfo(
                    achievement = achievement,
                    isUnlocked = unlockedInfo != null,
                    unlockedDate = unlockedInfo?.dateUnlocked
                )
            }

            _uiState.update {
                it.copy(
                    achievements = allAchievementsInfo.sortedByDescending { it.isUnlocked },
                    unlockedCount = unlockedMap.size,
                    totalCount = AchievementsList.allAchievements.size,
                    streakCount = streak,
                    points = userData?.points ?: 0
                )
            }
        }.launchIn(viewModelScope) // Corrigido: Coletando o Flow
    }

    fun onAchievementClicked(info: AchievementUiInfo) {
        _uiState.update { it.copy(selectedAchievement = info) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(selectedAchievement = null) }
    }
}
