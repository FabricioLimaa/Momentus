// ARQUIVO: data/database/MetaDao.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.Meta
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: Meta)

    @Query("SELECT * FROM tabela_metas WHERE rotinaId = :rotinaId")
    fun getMetaParaRotina(rotinaId: String): Flow<Meta?> // Pode ser nula se não houver meta
}