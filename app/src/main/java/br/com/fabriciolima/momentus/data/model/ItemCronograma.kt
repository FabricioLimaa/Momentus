package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Keep
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
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    val titulo: String = "",
    val descricao: String? = null,

    val data: Long? = null, // Para eventos únicos
    val diaDaSemana: String? = null, // Para eventos de template (SEG, TER, etc.)

    @get:Exclude @set:Exclude
    var horarioInicio: LocalTime = LocalTime.of(0, 0),

    @get:Exclude @set:Exclude
    var horarioTermino: LocalTime = LocalTime.of(0, 0),

    @ColumnInfo(defaultValue = "0")
    val ordem: Int = 0,

    val rotinaId: String = "",
    val templateId: String? = null,

    val googleCalendarEventId: String? = null,

    @ServerTimestamp
    val lastUpdated: Date? = null
) {
    // Propriedades para serialização do LocalTime no Firestore
    var horarioInicioString: String
        get() = horarioInicio.format(DateTimeFormatter.ISO_LOCAL_TIME)
        set(value) { horarioInicio = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME) }

    var horarioTerminoString: String
        get() = horarioTermino.format(DateTimeFormatter.ISO_LOCAL_TIME)
        set(value) { horarioTermino = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME) }
}
