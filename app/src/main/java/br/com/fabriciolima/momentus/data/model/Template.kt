package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.fabriciolima.momentus.util.DateSerializer
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Keep
@Serializable
@Entity(tableName = "tabela_templates")
data class Template(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val descricao: String? = null,
    @Serializable(with = DateSerializer::class)
    @ServerTimestamp
    val lastUpdated: Date? = null
)
