// ARQUIVO: data/database/ItemCronogramaDao.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.ItemCronograma
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCronogramaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Adiciona a anotação @Delete para remover um ItemCronograma do banco.
    @Delete
    suspend fun delete(item: ItemCronograma)
    // --- MODIFICAÇÃO TERMINA AQUI ---

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia ORDER BY horarioInicio ASC")
    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>>
}