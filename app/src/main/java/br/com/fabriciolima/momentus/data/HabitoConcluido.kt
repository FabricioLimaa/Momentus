// ARQUIVO: data/HabitoConcluido.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabela_habitos_concluidos")
data class HabitoConcluido(
    // Usaremos o ID do ItemCronograma como a chave primária,
    // garantindo que um item só possa ser marcado como concluído uma vez.
    @PrimaryKey
    val itemCronogramaId: String,
    val dataConclusao: Long // Salvaremos a data como um número (timestamp)
)