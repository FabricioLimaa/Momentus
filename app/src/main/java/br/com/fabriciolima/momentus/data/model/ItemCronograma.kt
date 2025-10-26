package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import br.com.fabriciolima.momentus.util.DateSerializer
import br.com.fabriciolima.momentus.util.LocalTimeSerializer
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Keep
@Serializable
@Entity(
    tableName = "tabela_itens_cronograma",
    indices = [Index(value = ["rotinaId"]), Index(value = ["templateId"])]
)
data class ItemCronograma(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    val titulo: String = "",
    val descricao: String? = null,

    val data: Long? = null, // Para eventos únicos
    val diaDaSemana: String? = null, // Para eventos de template (SEG, TER, etc.)

    @Serializable(with = LocalTimeSerializer::class)
    @get:Exclude @set:Exclude
    var horarioInicio: LocalTime = LocalTime.of(0, 0),

    @Serializable(with = LocalTimeSerializer::class)
    @get:Exclude @set:Exclude
    var horarioTermino: LocalTime = LocalTime.of(0, 0),

    @ColumnInfo(defaultValue = "0")
    val ordem: Int = 0,

    val rotinaId: String = "",
    val templateId: String? = null,

    val googleCalendarEventId: String? = null,

    @Serializable(with = DateSerializer::class)
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
