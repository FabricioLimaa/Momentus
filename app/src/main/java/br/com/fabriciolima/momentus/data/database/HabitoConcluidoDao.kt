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

    @Query("DELETE FROM tabela_habitos_concluidos")
    suspend fun clear()

    @Query("""
        SELECT r.id as rotinaId, r.nome as rotinaNome, r.cor as rotinaCor, COUNT(thc.itemCronogramaId) as concluidos
        FROM tabela_habitos_concluidos thc
        JOIN tabela_itens_cronograma tic ON thc.itemCronogramaId = tic.id
        JOIN tabela_rotinas r ON tic.rotinaId = r.id
        WHERE thc.dataConclusao >= :since
        GROUP BY r.id, r.nome, r.cor
    """)
    fun getConcluidosCountByRotina(since: Long): Flow<List<StatsSummary>>
}

data class StatsSummary(
    val rotinaId: String,
    val rotinaNome: String,
    val rotinaCor: String,
    val concluidos: Int
)
