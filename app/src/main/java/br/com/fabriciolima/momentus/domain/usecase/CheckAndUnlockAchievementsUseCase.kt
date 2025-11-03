package br.com.fabriciolima.momentus.domain.usecase

import android.util.Log
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.domain.AchievementsList
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "CheckAchievementsUseCase"

class CheckAndUnlockAchievementsUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository
) {

    suspend operator fun invoke(streakCount: Int = 0, totalCompleted: Int = 0, totalTemplates: Int = 0) {
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 5. Verificando conquistas com os dados: Streak: $streakCount, Total Hábitos: $totalCompleted, Total Templates: $totalTemplates")
        val unlockedAchievements = gamificationRepository.getUnlockedAchievementIdsSync().toSet()
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 5a. Conquistas já desbloqueadas no DB: $unlockedAchievements")

        AchievementsList.allAchievements.forEach { achievement ->
            if (achievement.id !in unlockedAchievements) {
                var shouldUnlock = false
                when (achievement.id) {
                    // Conquistas de Hábitos
                    "FIRST_HABIT" -> if (totalCompleted >= 1) shouldUnlock = true
                    "COMPLETED_10" -> if (totalCompleted >= 10) shouldUnlock = true
                    "COMPLETED_50" -> if (totalCompleted >= 50) shouldUnlock = true
                    "COMPLETED_100" -> if (totalCompleted >= 100) shouldUnlock = true
                    
                    // Conquistas de Sequência
                    "STREAK_3" -> if (streakCount >= 3) shouldUnlock = true
                    "STREAK_7" -> if (streakCount >= 7) shouldUnlock = true
                    "STREAK_30" -> if (streakCount >= 30) shouldUnlock = true

                    // Conquistas de Template
                    "FIRST_TEMPLATE" -> if (totalTemplates >= 1) shouldUnlock = true
                }

                if (shouldUnlock) {
                    Log.i(TAG, "[ACHIEVEMENT_FLOW] 5b. CONDIÇÃO ATINGIDA! Desbloqueando: ${achievement.name} (ID: ${achievement.id})")
                    gamificationRepository.unlockAchievement(achievement.id, achievement.points)
                }
            }
        }
    }
}
