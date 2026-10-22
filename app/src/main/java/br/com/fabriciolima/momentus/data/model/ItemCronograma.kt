package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import br.com.fabriciolima.momentus.util.DateSerializer
import br.com.fabriciolima.momentus.util.LocalTimeSerializer
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Keep
@IgnoreExtraProperties
@Serializable
@Entity(
    tableName = "tabela_itens_cronograma",
    indices = [Index(value = ["categoryId"]), Index(value = ["templateId"])] // Corrigido
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

    val ordem: Int = 0,

    val categoryId: String = "", // Corrigido
    val templateId: String? = null,

    val googleCalendarEventId: String? = null,

    @Serializable(with = DateSerializer::class)
    @ServerTimestamp
    val lastUpdated: java.util.Date? = null,

    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false
) {
    // Construtor vazio para o Firestore
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        titulo = "",
        horarioInicio = java.time.LocalTime.of(0, 0),
        horarioTermino = java.time.LocalTime.of(0, 0),
        categoryId = "",
        isDeleted = false
    )
    // Propriedades para serialização do LocalTime no Firestore
    var horarioInicioString: String
        get() = horarioInicio.format(DateTimeFormatter.ISO_LOCAL_TIME)
        set(value) {
            horarioInicio = parseSafeTime(value)
        }

    var horarioTerminoString: String
        get() = horarioTermino.format(DateTimeFormatter.ISO_LOCAL_TIME)
        set(value) {
            horarioTermino = parseSafeTime(value)
        }

    companion object {
        private fun parseSafeTime(value: String?): LocalTime {
            if (value.isNullOrBlank()) return LocalTime.of(0, 0)
            val trimmed = value.trim()
            return try {
                LocalTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_TIME)
            } catch (e: Exception) {
                try {
                    val parts = trimmed.split(":")
                    if (parts.size >= 2) {
                        val h = parts[0].toIntOrNull() ?: 0
                        val m = parts[1].toIntOrNull() ?: 0
                        val s = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0
                        LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59), s.coerceIn(0, 59))
                    } else {
                        LocalTime.of(0, 0)
                    }
                } catch (e2: Exception) {
                    LocalTime.of(0, 0)
                }
            }
        }
    }
}
