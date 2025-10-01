// ARQUIVO: data/ItemCronograma.kt
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tabela_itens_cronograma",
    indices = [Index(value = ["rotinaId"])],
    foreignKeys = [ForeignKey(
        entity = Rotina::class,
        parentColumns = ["id"],
        childColumns = ["rotinaId"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class ItemCronograma(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Adicionamos título e descrição próprios para cada evento.
    val titulo: String,
    val descricao: String?,
    // --- MODIFICAÇÃO TERMINA AQUI ---

    val data: Long?,
    val diaDaSemana: String?,
    val horarioInicio: String,
    // --- MODIFICAÇÃO: Adicionamos o horário de término para eventos únicos ---
    val horarioTermino: String?,
    // --- MODIFICAÇÃO TERMINA AQUI ---
    val rotinaId: String
)