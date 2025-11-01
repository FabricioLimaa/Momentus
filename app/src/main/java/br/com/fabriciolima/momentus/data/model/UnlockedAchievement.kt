package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa uma conquista que foi desbloqueada por um usuário.
 */
@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievement(
    @PrimaryKey
    val achievementId: String = "",
    val timestamp: Long = 0L
)
