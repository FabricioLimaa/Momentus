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
    // Se 'data' for nulo, é um item de rotina semanal.
    // Se 'data' tiver um valor, é um evento único com data específica.
    val data: Long?, // Armazenaremos a data como um número (timestamp)
    // O dia da semana agora também pode ser nulo.
    val diaDaSemana: String?,
    // --- MODIFICAÇÃO TERMINA AQUI ---

    val horarioInicio: String,
    val rotinaId: String // Para eventos únicos, podemos criar uma rotina "genérica"
)