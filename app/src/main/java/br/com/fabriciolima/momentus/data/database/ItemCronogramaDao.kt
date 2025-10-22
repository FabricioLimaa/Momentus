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

    @Query("SELECT * FROM tabela_itens_cronograma WHERE data = :date OR diaDaSemana = :dayOfWeekName")
    fun getWidgetEventItemsRaw(date: Long, dayOfWeekName: String): List<ItemCronograma>

    @Query(
        "SELECT tic.id, tic.titulo, tic.horarioInicio, tic.horarioTermino, tic.descricao, r.nome as nomeRotina, r.cor as corRotina " +
        "FROM tabela_itens_cronograma tic " +
        "JOIN tabela_rotinas r ON tic.rotinaId = r.id " +
        "WHERE (tic.data = :epochDay OR tic.diaDaSemana = :dayOfWeekName) AND tic.rotinaId IN (:allowedRotinaIds)"
    )
    fun getWidgetEventItems(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<WidgetEventItem>

    @Query(
        "SELECT * FROM tabela_itens_cronograma " +
        "WHERE (data = :epochDay OR diaDaSemana = :dayOfWeekName) AND rotinaId IN (:allowedRotinaIds)"
    )
    fun getForWidget(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<ItemCronograma>

    @Query("DELETE FROM tabela_itens_cronograma")
    suspend fun clear()
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
