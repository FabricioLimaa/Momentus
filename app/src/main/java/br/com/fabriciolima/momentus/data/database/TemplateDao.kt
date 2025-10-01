// ARQUIVO: data/database/TemplateDao.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data.database

import androidx.room.*
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.data.TemplateItem
import br.com.fabriciolima.momentus.data.TemplateComItens
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM tabela_templates ORDER BY nome ASC")
    fun getTemplatesComItens(): Flow<List<TemplateComItens>>

    @Insert
    suspend fun insertTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    // As próximas funções são para a lógica de "Salvar como Template"
    @Query("SELECT * FROM tabela_itens_cronograma")
    suspend fun getItensCronogramaAtuais(): List<ItemCronograma>

    @Insert
    suspend fun insertAllTemplateItems(items: List<TemplateItem>)

    // Uma transação garante que todas as operações dentro dela aconteçam
    // com sucesso, ou nenhuma delas acontece. Isso evita corrupção de dados.
    @Transaction
    suspend fun saveCurrentScheduleAsTemplate(template: Template) {
        insertTemplate(template)
        val cronogramaAtual = getItensCronogramaAtuais()
        // --- CORREÇÃO AQUI ---
        // Adicionamos um '.filter { it.rotinaId != null }' para garantir
        // que estamos lidando apenas com itens que têm uma rotina associada.
        val templateItems = cronogramaAtual.filter { it.rotinaId != null }.map { item ->
            TemplateItem(
                templateId = template.id,
                diaDaSemana = item.diaDaSemana ?: "", // Garante que não seja nulo
                horarioInicio = item.horarioInicio,
                rotinaId = item.rotinaId // Agora o compilador sabe que não é nulo
            )
        }
        insertAllTemplateItems(templateItems)
    }
}