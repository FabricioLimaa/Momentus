package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ItemCronogramaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemCronograma>)

    @Update
    suspend fun updateAll(items: List<ItemCronograma>)

    @Delete
    suspend fun delete(item: ItemCronograma)

    @Query("DELETE FROM tabela_itens_cronograma WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)

    @Query("SELECT * FROM tabela_itens_cronograma WHERE isDeleted = 0 ORDER BY horarioInicio ASC")
    fun getAllItems(): Flow<List<ItemCronograma>>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE isDeleted = 0")
    fun getAllSync(): List<ItemCronograma>

    @Query("SELECT * FROM tabela_itens_cronograma")
    fun getAllSyncIncludingDeleted(): List<ItemCronograma>

    @Query("DELETE FROM tabela_itens_cronograma WHERE isDeleted = 1")
    suspend fun permanentlyDeleteMarkedItems()

    @Query("SELECT * FROM tabela_itens_cronograma WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getItemsByIds(ids: List<String>): List<ItemCronograma>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE id IN (:ids)")
    suspend fun getItemsByIdsIncludingDeleted(ids: List<String>): List<ItemCronograma>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dayOfWeek AND isDeleted = 0")
    fun getItemsByDayOfWeek(dayOfWeek: String): Flow<List<ItemCronograma>>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE id = :itemId AND isDeleted = 0")
    suspend fun getItemById(itemId: String): ItemCronograma?

    @Query("SELECT id FROM tabela_itens_cronograma WHERE templateId = :templateId")
    suspend fun getIdsByTemplateId(templateId: String): List<String>

    @Query("DELETE FROM tabela_itens_cronograma WHERE templateId = :templateId")
    suspend fun deleteByTemplateId(templateId: String)

    @Query("SELECT id FROM tabela_itens_cronograma WHERE categoryId = :categoryId")
    suspend fun getIdsByCategoryId(categoryId: String): List<String>

    @Query("DELETE FROM tabela_itens_cronograma WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: String)

    /**
     * Consulta otimizada para o Widget.
     * Utiliza parâmetros de tempo explícitos para melhorar a performance dos índices.
     */
    @Query(
        """
        SELECT tic.id, tic.titulo, tic.horarioInicio, tic.horarioTermino, tic.descricao, c.nome as categoryName, c.cor as categoryColor, 
        CASE WHEN thc.itemCronogramaId IS NOT NULL THEN 1 ELSE 0 END as isCompleted 
        FROM tabela_itens_cronograma tic 
        JOIN categories c ON tic.categoryId = c.id 
        LEFT JOIN tabela_habitos_concluidos thc ON tic.id = thc.itemCronogramaId 
        AND thc.dataConclusao BETWEEN :startOfDayMillis AND :endOfDayMillis
        WHERE (tic.data BETWEEN :startOfDayMillis AND :endOfDayMillis OR tic.diaDaSemana = :dayOfWeekName) 
        AND tic.categoryId IN (:allowedCategoryIds) 
        AND tic.isDeleted = 0
        ORDER BY tic.horarioInicio ASC
        """
    )
    fun getWidgetEventItems(
        startOfDayMillis: Long, 
        endOfDayMillis: Long, 
        dayOfWeekName: String, 
        allowedCategoryIds: Set<String>
    ): List<WidgetEventItem>

    @Query("DELETE FROM tabela_itens_cronograma")
    suspend fun clear()

    @Query("""
        SELECT * 
        FROM tabela_itens_cronograma
        WHERE categoryId = :categoryId AND isDeleted = 0 AND (
            (data IS NOT NULL AND data >= :since) OR 
            (diaDaSemana IS NOT NULL)
        )
    """)
    fun getSchedulableEventsForCategory(categoryId: String, since: Long): Flow<List<ItemCronograma>>
}

data class WidgetEventItem(
    val id: String,
    val titulo: String,
    val horarioInicio: String,
    val horarioTermino: String,
    val descricao: String?,
    val categoryName: String,
    val categoryColor: String,
    val isCompleted: Boolean
)
