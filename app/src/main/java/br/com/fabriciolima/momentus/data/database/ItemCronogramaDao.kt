package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import kotlinx.coroutines.flow.Flow

/**
 * DAO para a entidade ItemCronograma.
 */
@Dao
interface ItemCronogramaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    @Update
    suspend fun updateAll(items: List<ItemCronograma>)

    @Delete
    suspend fun delete(item: ItemCronograma)

    @Query("SELECT * FROM tabela_itens_cronograma")
    fun getAllItems(): Flow<List<ItemCronograma>>

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia")
    fun getItemsByDayOfWeek(dia: String): Flow<List<ItemCronograma>>

    /**
     * NOVA CONSULTA PARA O WIDGET
     * Busca de forma SÍNCRONA todos os itens que são de uma data específica OU de um dia da semana recorrente.
     * O epochDay é o número de dias desde 1970-01-01, facilitando a comparação de datas no SQLite.
     */
    @Query("""
        SELECT * FROM tabela_itens_cronograma 
        WHERE 
            (data / 86400000) = :epochDay 
            OR 
            (diaDaSemana = :dayOfWeekName AND data IS NULL)
    """)
    fun getForWidget(epochDay: Long, dayOfWeekName: String): List<ItemCronograma>

}
