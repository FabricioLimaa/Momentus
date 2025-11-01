package br.com.fabriciolima.momentus.data.model

/**
 * Representa a definição de uma conquista (achievement) no sistema de gamificação.
 * Não é uma entidade do Room, mas um modelo para definir as conquistas disponíveis.
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val points: Int
)
