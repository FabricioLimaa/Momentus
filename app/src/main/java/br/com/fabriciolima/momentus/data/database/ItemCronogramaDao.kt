package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fabriciolima.momentus.data.ItemCronograma
import kotlinx.coroutines.flow.Flow

/**
 * DAO para a entidade ItemCronograma.
 */
@Dao
interface ItemCronogramaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    /**
     * NOVO: Atualiza uma lista de itens em uma única transação.
     * Essencial para salvar a nova ordem após o drag-and-drop.
     */
    @Update
    suspend fun updateAll(items: List<ItemCronograma>)

    @Delete
    suspend fun delete(item: ItemCronograma)

    /**
     * Busca todos os itens do cronograma.
     * Usado principalmente pela tela principal do calendário.
     */
    @Query("SELECT * FROM tabela_itens_cronograma")
    fun getAllItems(): Flow<List<ItemCronograma>>

    /**
     * NOVO: Busca todos os itens de um dia específico da semana.
     * Essencial para a geração de eventos do Google Calendar.
     */
    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia")
    fun getItemsByDayOfWeek(dia: String): Flow<List<ItemCronograma>>

}
