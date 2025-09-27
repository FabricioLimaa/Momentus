// ARQUIVO: data/database/HabitoConcluidoDao.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.HabitoConcluido
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitoConcluidoDao {
    // Insere um novo hábito concluído. Se já existir, ele o substitui.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habito: HabitoConcluido)

    // Deleta um hábito concluído (caso o usuário desmarque a caixa).
    @Query("DELETE FROM tabela_habitos_concluidos WHERE itemCronogramaId = :itemCronogramaId")
    suspend fun delete(itemCronogramaId: String)

    // Busca todos os IDs dos hábitos concluídos para um determinado dia.
    // Retorna um Flow para que a UI se atualize automaticamente.
    @Query("SELECT itemCronogramaId FROM tabela_habitos_concluidos")
    fun getIdsConcluidos(): Flow<List<String>>
}