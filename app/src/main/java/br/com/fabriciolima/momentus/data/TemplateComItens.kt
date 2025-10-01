// ARQUIVO: data/TemplateComItens.kt
package br.com.fabriciolima.momentus.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TemplateComItens(
    @Embedded
    val template: Template,

    // --- CORREÇÃO DA RELAÇÃO MUITOS-PARA-MUITOS ---
    // Esta anotação agora diz ao Room:
    // 1. O 'pai' é o Template (usando a coluna 'id' do template).
    // 2. A 'entidade filha' é a Rotina (usando a coluna 'id' da rotina).
    // 3. A tabela que conecta os dois é a 'TemplateItem'.
    @Relation(
        parentColumn = "id", // Chave do Template
        entity = Rotina::class, // A entidade que queremos buscar
        entityColumn = "id", // Chave da Rotina
        associateBy = Junction(
            value = TemplateItem::class,
            parentColumn = "templateId",
            entityColumn = "rotinaId"
        )
    )
    val rotinas: List<Rotina>,

    // Esta relação 'um-para-muitos' para pegar os itens está correta.
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val itens: List<TemplateItem>
)