// ARQUIVO: data/TemplateItem.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

// Esta tabela guarda cada item de um template.
// Ela tem uma chave estrangeira que a "linka" a um Template específico.
@Entity(
    tableName = "tabela_itens_template",
    foreignKeys = [ForeignKey(
        entity = Template::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE // Se um template for deletado, todos os seus itens também serão.
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