package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tabela_templates")
data class Template(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String
)