package br.com.fabriciolima.momentus.data

import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

open class RotinaRepository(
    private val rotinaDao: RotinaDao?,
    private val itemCronogramaDao: ItemCronogramaDao?,
    private val templateDao: TemplateDao?,
    private val metaDao: MetaDao?
) {
    // Marcamos como 'open' para permitir a sobrescrita no FakeRepository
    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao?.getRotinasComMetas() ?: emptyFlow()
    open val stats: Flow<List<StatsResult>> = itemCronogramaDao?.getStats() ?: emptyFlow()
    open val todosOsTemplates: Flow<List<Template>> = templateDao?.getAllTemplates() ?: emptyFlow()

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
}