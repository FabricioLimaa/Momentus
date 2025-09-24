// ARQUIVO: data/Template.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tabela_templates")
data class Template(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String
)