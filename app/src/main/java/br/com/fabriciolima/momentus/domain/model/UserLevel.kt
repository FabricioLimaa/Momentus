package br.com.fabriciolima.momentus.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Representa o nível e rank do usuário com base nos seus pontos totais.
 */
data class UserLevel(
    val level: Int,
    val rankName: String,
    val currentXp: Int,
    val nextLevelXp: Int,
    val progress: Float,
    val rankColor: Color
) {
    companion object {
        fun fromPoints(points: Int): UserLevel {
            return when {
                points < 100 -> UserLevel(1, "Iniciante", points, 100, points / 100f, Color(0xFF9E9E9E))
                points < 300 -> UserLevel(2, "Determinado", points - 100, 200, (points - 100) / 200f, Color(0xFF4CAF50))
                points < 600 -> UserLevel(3, "Focado", points - 300, 300, (points - 300) / 300f, Color(0xFF2196F3))
                points < 1000 -> UserLevel(4, "Constante", points - 600, 400, (points - 600) / 400f, Color(0xFF9C27B0))
                else -> {
                    val excessPoints = points - 1000
                    val levelsAbove5 = excessPoints / 500
                    val xpInCurrentLevel = excessPoints % 500
                    UserLevel(
                        level = 5 + levelsAbove5,
                        rankName = "Mestre da Rotina",
                        currentXp = xpInCurrentLevel,
                        nextLevelXp = 500,
                        progress = xpInCurrentLevel / 500f,
                        rankColor = Color(0xFFFFD700)
                    )
                }
            }
        }

        /**
         * Verifica se houve mudança de nível entre dois valores de pontuação.
         */
        fun didLevelUp(oldPoints: Int, newPoints: Int): Boolean {
            if (newPoints <= oldPoints) return false
            return fromPoints(newPoints).level > fromPoints(oldPoints).level
        }
    }
}
