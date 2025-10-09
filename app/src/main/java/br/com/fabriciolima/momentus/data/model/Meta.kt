package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import br.com.fabriciolima.momentus.data.model.Rotina

@Entity(
    tableName = "tabela_metas",
    foreignKeys = [ForeignKey(
        entity = Rotina::class,
        parentColumns = ["id"],
        childColumns = ["rotinaId"],
        onDelete = ForeignKey.CASCADE // Se a rotina for deletada, a meta também será.
    )]
)
data class Meta(
    @PrimaryKey
    val rotinaId: String, // A própria ID da rotina é a chave
    val metaMinutosSemanal: Int // Armazenaremos a meta em minutos
)