// ARQUIVO: data/RotinaRepository.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data

import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import kotlinx.coroutines.flow.Flow

// 1. Adicione templateDao ao construtor
class RotinaRepository(
    private val rotinaDao: RotinaDao,
    private val itemCronogramaDao: ItemCronogramaDao,
    private val templateDao: TemplateDao
) {
    val todasAsRotinas: Flow<List<Rotina>> = rotinaDao.getAllRotinas()
    val stats: Flow<List<StatsResult>> = itemCronogramaDao.getStats()
    // --- MODIFICAÇÃO INICIA AQUI ---
    val todosOsTemplates: Flow<List<Template>> = templateDao.getAllTemplates()

    suspend fun saveCurrentScheduleAsTemplate(templateName: String) {
        val novoTemplate = Template(nome = templateName)
        templateDao.saveCurrentScheduleAsTemplate(novoTemplate)
    }

    suspend fun loadTemplateIntoSchedule(templateId: String) {
        itemCronogramaDao.loadTemplateIntoSchedule(templateId)
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.deleteTemplate(template)
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    suspend fun insert(rotina: Rotina) {
        rotinaDao.insert(rotina)
    }

    suspend fun delete(rotina: Rotina) {
        rotinaDao.delete(rotina)
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItensDoDia(dia)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Nova função que repassa a ordem de deleção para o ItemCronogramaDao.
    suspend fun deleteItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.delete(item)
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}