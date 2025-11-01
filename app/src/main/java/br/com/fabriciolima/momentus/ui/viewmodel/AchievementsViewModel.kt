package br.com.fabriciolima.momentus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.domain.AchievementsList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Classe de dados para combinar uma conquista com seu estado de desbloqueio
data class AchievementUiInfo( 
    val achievement: Achievement,
    val isUnlocked: Boolean
)

data class AchievementsUiState(
    val achievements: List<AchievementUiInfo> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val streakCount: Int = 0,
    val points: Int = 0 // Adicionado
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    gamificationRepository: GamificationRepository,
    categoryRepository: CategoryRepository,
    userRepository: UserRepository // Adicionado
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = combine(
        gamificationRepository.unlockedAchievements,
        categoryRepository.currentStreak,
        userRepository.userData
    ) { unlockedAchievements, streak, userData -> // Corrigido
        val unlockedIds = unlockedAchievements.map { it.achievementId }.toSet()
        val allAchievementsInfo = AchievementsList.allAchievements.map { achievement ->
            AchievementUiInfo(
                achievement = achievement,
                isUnlocked = achievement.id in unlockedIds
            )
        }

        AchievementsUiState(
            achievements = allAchievementsInfo.sortedByDescending { it.isUnlocked },
            unlockedCount = unlockedIds.size,
            totalCount = AchievementsList.allAchievements.size,
            streakCount = streak,
            points = userData?.points ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AchievementsUiState()
    )
}
