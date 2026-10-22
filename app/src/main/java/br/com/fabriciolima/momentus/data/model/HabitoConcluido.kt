package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
@Entity(tableName = "tabela_habitos_concluidos")
data class HabitoConcluido(
    @PrimaryKey
    val itemCronogramaId: String = "",
    val dataConclusao: Long = 0L
)
