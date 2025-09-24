// ARQUIVO: data/ItemCronograma.kt
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tabela_itens_cronograma",
    foreignKeys = [ForeignKey(
        entity = Rotina::class,
        parentColumns = ["id"],
        childColumns = ["rotinaId"],
        onDelete = ForeignKey.CASCADE // Se uma rotina for deletada, os itens agendados com ela também serão.
    )]
)
data class ItemCronograma(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val diaDaSemana: String, // "DOM", "SEG", "TER", etc.
    val horarioInicio: String, // "09:00"
    val rotinaId: String // "Link" para a rotina correspondente
)