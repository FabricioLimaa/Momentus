package br.com.fabriciolima.momentus.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe de dados que representa um Template com sua lista de eventos (ItemCronograma).
 * O Room usa esta classe para buscar um template e todos os seus eventos associados de uma só vez.
 */
data class TemplateComEventos(
    @Embedded
    val template: Template,

    @Relation(
        parentColumn = "id", // Chave primária da tabela de templates
        entityColumn = "templateId" // Chave estrangeira na tabela de itens do cronograma
    )
    val eventos: List<ItemCronograma>
)
