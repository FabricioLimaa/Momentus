// ARQUIVO: data/database/RotinaDao.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.Rotina
import kotlinx.coroutines.flow.Flow

@Dao
interface RotinaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rotina: Rotina)

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Adiciona a anotação @Delete, que instrui o Room a remover
    // o objeto 'rotina' passado como parâmetro da tabela.
    @Delete
    suspend fun delete(rotina: Rotina)
    // --- MODIFICAÇÃO TERMINA AQUI ---

    @Query("SELECT * FROM tabela_de_rotinas ORDER BY nome ASC")
    fun getAllRotinas(): Flow<List<Rotina>>
}