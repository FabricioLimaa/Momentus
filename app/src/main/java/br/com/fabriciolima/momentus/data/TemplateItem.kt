// ARQUIVO: data/TemplateItem.kt
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // Adicione este import
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tabela_itens_template",
    // --- MODIFICAÇÃO: Adicionamos o índice recomendado pelo Room ---
    indices = [Index(value = ["templateId"]), Index(value = ["rotinaId"])],
    foreignKeys = [ForeignKey(
        entity = Template::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class TemplateItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val diaDaSemana: String,
    val horarioInicio: String,
    val rotinaId: String
)