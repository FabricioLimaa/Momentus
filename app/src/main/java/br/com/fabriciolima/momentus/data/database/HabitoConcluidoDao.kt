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
}
