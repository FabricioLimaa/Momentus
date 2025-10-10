package br.com.fabriciolima.momentus.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "tabela_itens_cronograma",
    foreignKeys = [
        ForeignKey(
            entity = Rotina::class,
            parentColumns = ["id"],
            childColumns = ["rotinaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["rotinaId"]), Index(value = ["templateId"])]
)
data class ItemCronograma(
    @PrimaryKey // IDs agora são Strings, então removemos o autoGenerate
    val id: String = java.util.UUID.randomUUID().toString(),

    val titulo: String,
    val descricao: String?,

    val data: Long?, // Para eventos únicos
    val diaDaSemana: String?, // Para eventos de template (SEG, TER, etc.)

    val horarioInicio: LocalTime,
    val horarioTermino: LocalTime,

    @ColumnInfo(defaultValue = "0")
    val ordem: Int = 0, // Ordem ainda pode ser um Int

    // Chaves estrangeiras
    val rotinaId: String,
    val templateId: String?,

    // MELHORIA 2: ID do evento do Google Calendar
    val googleCalendarEventId: String? = null
)
