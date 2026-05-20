package br.com.fabriciolima.momentus.domain

import br.com.fabriciolima.momentus.data.model.Achievement

object AchievementsList {
    val allAchievements = listOf(
        // Conquistas de Primeiros Passos
        Achievement("FIRST_HABIT", "Primeiro Hábito!", "Você concluiu seu primeiro hábito.", 10L),
        Achievement("FIRST_TEMPLATE", "Tudo Organizado", "Você criou seu primeiro template.", 20L),

        // Conquistas de Sequência (Streak)
        Achievement("STREAK_3", "Pegando o Ritmo", "Manteve uma sequência de 3 dias.", 30L),
        Achievement("STREAK_7", "Semana Perfeita", "Manteve uma sequência de 7 dias.", 70L),
        Achievement("STREAK_30", "Mestre do Hábito", "Manteve uma sequência de 30 dias.", 300L),

        // Conquistas de Volume
        Achievement("COMPLETED_10", "Dez em Dez", "Você completou 10 hábitos no total.", 50L),
        Achievement("COMPLETED_50", "Meio Caminho Andado", "Você completou 50 hábitos no total.", 100L),
        Achievement("COMPLETED_100", "Centurião", "Você completou 100 hábitos no total.", 200L)
    )
}
