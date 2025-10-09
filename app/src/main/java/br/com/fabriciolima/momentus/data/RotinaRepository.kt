package br.com.fabriciolima.momentus.data

import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repositório central que abstrai o acesso às fontes de dados (DAOs).
 */
open class RotinaRepository(
    private val rotinaDao: RotinaDao,
    private val itemCronogramaDao: ItemCronogramaDao,
    private val templateDao: TemplateDao,
    private val metaDao: MetaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao
) {

    // --- Fluxos de Dados (para observação pela UI) ---

    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao.getRotinasComMetas()
    open val todosOsTemplatesComEventos: Flow<List<TemplateComEventos>> = templateDao.getTemplatesComEventos()
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()

    open val stats: Flow<List<StatsResult>> = rotinaDao.getStats()

    // --- Funções para o Widget (Síncronas) ---

    /**
     * NOVA FUNÇÃO PARA O WIDGET
     * Busca os itens de forma síncrona para um dia específico.
     */
    fun getItensParaWidget(data: LocalDate): List<ItemCronograma> {
        val epochDay = data.toEpochDay()
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)
        return itemCronogramaDao.getForWidget(epochDay, dayOfWeekName)
    }

    /**
     * NOVA FUNÇÃO PARA O WIDGET
     * Busca todas as rotinas de forma síncrona.
     */
    fun getTodasAsRotinasSync(): List<Rotina> {
        return rotinaDao.getAllSync()
    }

    // --- Funções de Template ---

    fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos> {
        return templateDao.getTemplateComEventos(templateId)
    }

    suspend fun insertTemplate(template: Template) {
        templateDao.insert(template)
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template)
    }

    // --- Funções de ItemCronograma (Evento) ---

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
    }

    suspend fun deleteItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.delete(item)
    }

    open suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
    }

    // --- Funções de Rotina (Categoria) ---

    suspend fun insertRotina(rotina: Rotina) {
        rotinaDao.insert(rotina)
    }

    suspend fun deleteRotina(rotina: Rotina) {
        rotinaDao.delete(rotina)
    }

    // --- Funções de Meta e Hábitos ---

    fun getMetaParaRotina(rotinaId: String): Flow<Meta?> {
        return metaDao.getMetaParaRotina(rotinaId)
    }

    suspend fun salvarMeta(meta: Meta) {
        metaDao.insertOrUpdate(meta)
    }

    suspend fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao.insert(habito)
    }

    suspend fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        habitoConcluidoDao.delete(itemCronogramaId)
    }
}
