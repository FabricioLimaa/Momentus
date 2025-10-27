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

    @Query("SELECT * FROM tabela_itens_cronograma ORDER BY horarioInicio ASC")
    fun getAllItems(): Flow<List<ItemCronograma>>

    @Query("SELECT * FROM tabela_itens_cronograma")
    fun getAllSync(): List<ItemCronograma>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dayOfWeek")
    fun getItemsByDayOfWeek(dayOfWeek: String): Flow<List<ItemCronograma>>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItemCronograma?

    @Query("DELETE FROM tabela_itens_cronograma WHERE templateId = :templateId")
    suspend fun deleteByTemplateId(templateId: String)

    @Query("DELETE FROM tabela_itens_cronograma WHERE categoryId = :categoryId") // Corrigido
    suspend fun deleteByCategoryId(categoryId: String) // Corrigido

    @Query(
        "SELECT tic.id, tic.titulo, tic.horarioInicio, tic.horarioTermino, tic.descricao, c.nome as nomeRotina, c.cor as corRotina " +
        "FROM tabela_itens_cronograma tic " +
        "JOIN categories c ON tic.categoryId = c.id " + // Corrigido
        "WHERE (tic.data BETWEEN :startOfDayMillis AND :endOfDayMillis OR tic.diaDaSemana = :dayOfWeekName) AND tic.categoryId IN (:allowedCategoryIds) " +
        "ORDER BY tic.horarioInicio ASC"
    )
    fun getWidgetEventItems(startOfDayMillis: Long, endOfDayMillis: Long, dayOfWeekName: String, allowedCategoryIds: Set<String>): List<WidgetEventItem>

    @Query("DELETE FROM tabela_itens_cronograma")
    suspend fun clear()

    @Query("""
        SELECT * 
        FROM tabela_itens_cronograma
        WHERE categoryId = :categoryId AND (
            (data IS NOT NULL AND data >= :since) OR 
            (diaDaSemana IS NOT NULL)
        )
    """)
    fun getSchedulableEventsForCategory(categoryId: String, since: Long): Flow<List<ItemCronograma>> // Corrigido
}

data class WidgetEventItem(
    val id: String,
    val titulo: String,
    val horarioInicio: String,
    val horarioTermino: String,
    val descricao: String?,
    val nomeRotina: String,
    val corRotina: String
)
