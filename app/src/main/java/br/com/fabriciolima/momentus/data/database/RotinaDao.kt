// ARQUIVO: data/database/RotinaDao.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data.database

import androidx.room.*
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaComMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface RotinaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rotina: Rotina)

    @Delete
    suspend fun delete(rotina: Rotina)

    // MODIFICAÇÃO: Substituímos a query antiga por esta.
    // LEFT JOIN garante que todas as rotinas sejam retornadas,
    // mesmo que não tenham uma meta correspondente.
    @Query("""
        SELECT * FROM tabela_de_rotinas
        LEFT JOIN tabela_metas ON tabela_de_rotinas.id = tabela_metas.rotinaId
        ORDER BY nome ASC
    """)
    fun getRotinasComMetas(): Flow<List<RotinaComMeta>>
}