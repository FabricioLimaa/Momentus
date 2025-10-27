package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabela_metas",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Meta(
    @PrimaryKey
    val categoryId: String,
    val metaMinutosSemanal: Int // Armazenaremos a meta em minutos
)
