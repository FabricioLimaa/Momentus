package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Keep
@Entity(tableName = "tabela_templates")
data class Template(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = ""
)
