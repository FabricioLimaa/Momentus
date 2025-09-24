// ARQUIVO: data/StatsResult.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data

// Esta classe não é uma tabela, é apenas um "recipiente" para
// o resultado da nossa consulta SQL customizada.
data class StatsResult(
    val nomeRotina: String,
    val corRotina: String,
    val totalMinutos: Int
)