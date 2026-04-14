package br.com.fabriciolima.momentus.data.repository

import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.util.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface que unifica as operações de agendamento e cronograma.
 * Serve como ponte para a migração completa do EventoRepository para o ScheduleRepository.
 */
interface IScheduleRepository {
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>>
    val allScheduleItems: Flow<List<ItemCronograma>>
    
    fun startListeningForChanges()
    fun stopListeningForChanges()
    
    suspend fun syncSchedule(): Result<Unit>
    suspend fun syncEventos(): Result<Unit>
    
    fun getItemsForDay(day: String): Flow<List<ItemCronograma>>
    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>>
    
    suspend fun getItemById(itemId: String): ItemCronograma?
    suspend fun getItemCronograma(itemId: String): ItemCronograma?
    
    suspend fun insertItem(item: ItemCronograma)
    suspend fun insertItemCronograma(item: ItemCronograma)
    
    suspend fun insertAllItems(items: List<ItemCronograma>)
    suspend fun insertAll(items: List<ItemCronograma>)
    
    suspend fun updateItems(items: List<ItemCronograma>)
    suspend fun updateItensCronograma(items: List<ItemCronograma>)
    
    suspend fun deleteScheduleItem(item: ItemCronograma): Result<Unit>
    suspend fun excluirEventoCompleto(item: ItemCronograma): Result<Unit>
    
    suspend fun deleteItemsByIds(ids: Set<String>)
    suspend fun deleteEventsByIds(ids: Set<String>)
    
    suspend fun deleteItemsByTemplateId(templateId: String)
    suspend fun deleteEventsByTemplateId(templateId: String)
    
    suspend fun deleteItemsByCategoryId(categoryId: String)
    suspend fun deleteEventsByCategoryId(categoryId: String)
    
    fun getWidgetEvents(data: LocalDate, allowedCategoryIds: Set<String>): List<WidgetEventItem>
    suspend fun clear()
}
