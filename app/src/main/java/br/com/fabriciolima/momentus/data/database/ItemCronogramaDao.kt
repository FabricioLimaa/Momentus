// ARQUIVO: data/database/ItemCronogramaDao.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.StatsResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCronogramaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    @Delete
    suspend fun delete(item: ItemCronograma)

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia ORDER BY horarioInicio ASC")
    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>>

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Esta é uma consulta SQL mais complexa:
    // 1. SELECT r.nome, r.cor, SUM(r.duracaoPadraoMinutos): Selecionamos o nome e a cor da rotina, e a SOMA dos minutos.
    // 2. FROM tabela_itens_cronograma AS ic: A partir da tabela de itens do cronograma.
    // 3. INNER JOIN tabela_de_rotinas AS r ON ic.rotinaId = r.id: Juntamos com a tabela de rotinas onde os IDs correspondem.
    // 4. GROUP BY r.nome, r.cor: Agrupamos os resultados por rotina para que o SUM() funcione corretamente.
    @Query("""
        SELECT r.nome AS nomeRotina, r.cor AS corRotina, SUM(r.duracaoPadraoMinutos) as totalMinutos
        FROM tabela_itens_cronograma AS ic
        INNER JOIN tabela_de_rotinas AS r ON ic.rotinaId = r.id
        GROUP BY r.nome, r.cor
    """)
    fun getStats(): Flow<List<StatsResult>>
    // --- MODIFICAÇÃO TERMINA AQUI ---
}