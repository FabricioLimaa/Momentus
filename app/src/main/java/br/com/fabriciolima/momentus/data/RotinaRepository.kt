// ARQUIVO: data/RotinaRepository.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data

import br.com.fabriciolima.momentus.data.database.* // Usamos wildcard
import br.com.fabriciolima.momentus.data.TemplateComItens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

open class RotinaRepository(
    private val rotinaDao: RotinaDao?,
    private val itemCronogramaDao: ItemCronogramaDao?,
    private val templateDao: TemplateDao?,
    private val metaDao: MetaDao?,
    private val habitoConcluidoDao: HabitoConcluidoDao?
) {
    // Marcamos como 'open' para permitir a sobrescrita no FakeRepository
    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao?.getRotinasComMetas() ?: emptyFlow()
    open val stats: Flow<List<StatsResult>> = itemCronogramaDao?.getStats() ?: emptyFlow()
    open val todosOsTemplatesComItens: Flow<List<TemplateComItens>> = templateDao?.getTemplatesComItens() ?: emptyFlow()
    // --- MODIFICAÇÃO INICIA AQUI ---
    open val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao?.getIdsConcluidos() ?: emptyFlow()

    // --- MODIFICAÇÃO INICIA AQUI ---
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao?.getAllItems() ?: emptyFlow()
    // --- MODIFICAÇÃO TERMINA AQUI ---

    suspend fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao?.insert(habito)
    }

    suspend fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        habitoConcluidoDao?.delete(itemCronogramaId)
    }

    open fun getMetaParaRotina(rotinaId: String): Flow<Meta?> {
        return metaDao?.getMetaParaRotina(rotinaId) ?: emptyFlow()
    }

    open suspend fun salvarMeta(meta: Meta) {
        metaDao?.insertOrUpdate(meta)
    }

    open suspend fun saveCurrentScheduleAsTemplate(templateName: String) {
        val novoTemplate = Template(nome = templateName)
        templateDao?.saveCurrentScheduleAsTemplate(novoTemplate)
    }

    open suspend fun loadTemplateIntoSchedule(templateId: String) {
        itemCronogramaDao?.loadTemplateIntoSchedule(templateId)
    }

    open suspend fun deleteTemplate(template: Template) {
        templateDao?.deleteTemplate(template)
    }

    open suspend fun insert(rotina: Rotina) {
        rotinaDao?.insert(rotina)
    }

    open suspend fun delete(rotina: Rotina) {
        rotinaDao?.delete(rotina)
    }

    open fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao?.getItensDoDia(dia) ?: emptyFlow()
    }

    open suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao?.insert(item)
    }

    open suspend fun deleteItemCronograma(item: ItemCronograma) {
        itemCronogramaDao?.delete(item)
    }

    // --- MODIFICAÇÃO: Adicione esta nova função ---
    open suspend fun saveCompleteTemplate(templateName: String, events: List<TemplateEvent>) {
        val novoTemplate = Template(nome = templateName)
        // A lógica para converter TemplateEvent para TemplateItem e salvar precisa ser feita aqui
        // Por enquanto, vamos apenas criar a função para resolver o erro.
        templateDao?.insertTemplate(novoTemplate)
    }
}