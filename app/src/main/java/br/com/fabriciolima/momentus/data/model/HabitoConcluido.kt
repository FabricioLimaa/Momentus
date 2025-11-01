package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabela_habitos_concluidos")
data class HabitoConcluido(
    @PrimaryKey
    val itemCronogramaId: String = "",
    val dataConclusao: Long = 0L
)
