package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitoConcluidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habito: HabitoConcluido)

    @Query("DELETE FROM tabela_habitos_concluidos WHERE itemCronogramaId = :itemCronogramaId")
    suspend fun delete(itemCronogramaId: String)

    @Query("SELECT itemCronogramaId FROM tabela_habitos_concluidos")
    fun getIdsConcluidos(): Flow<List<String>>

    @Query("SELECT dataConclusao FROM tabela_habitos_concluidos ORDER BY dataConclusao ASC")
    fun getAllCompletionDates(): Flow<List<Long>>

    @Query("SELECT dataConclusao FROM tabela_habitos_concluidos ORDER BY dataConclusao ASC")
    fun getAllCompletionDatesSync(): List<Long> // Adicionado

    @Query("SELECT COUNT(*) FROM tabela_habitos_concluidos")
    fun getCountSync(): Int

    @Query("DELETE FROM tabela_habitos_concluidos")
    suspend fun clear()

    @Query("""
        SELECT c.id as categoryId, c.nome as categoryName, c.cor as categoryColor, COUNT(thc.itemCronogramaId) as concluidos
        FROM tabela_habitos_concluidos thc
        JOIN tabela_itens_cronograma tic ON thc.itemCronogramaId = tic.id
        JOIN categories c ON tic.categoryId = c.id
        WHERE thc.dataConclusao >= :since
        GROUP BY c.id, c.nome, c.cor
    """)
    fun getConcluidosCountByCategory(since: Long): Flow<List<StatsSummary>>
}

data class StatsSummary(
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String,
    val concluidos: Int
)
