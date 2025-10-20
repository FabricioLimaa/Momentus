package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

@Keep
@Entity(tableName = "tabela_templates")
data class Template(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    @ServerTimestamp
    val lastUpdated: Date? = null
)
