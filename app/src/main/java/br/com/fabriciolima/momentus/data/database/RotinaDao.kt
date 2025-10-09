package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaComMeta
import br.com.fabriciolima.momentus.data.StatsResult
import kotlinx.coroutines.flow.Flow

@Dao
interface RotinaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rotina: Rotina)

    @Delete
    suspend fun delete(rotina: Rotina)

    @Query("SELECT * FROM tabela_rotinas LEFT JOIN tabela_metas ON tabela_rotinas.id = tabela_metas.rotinaId ORDER BY nome ASC")
    fun getRotinasComMetas(): Flow<List<RotinaComMeta>>

    /**
     * NOVA CONSULTA SÍNCRONA PARA O WIDGET
     */
    @Query("SELECT * FROM tabela_rotinas")
    fun getAllSync(): List<Rotina>

    @Query("""
        SELECT
            r.nome AS nome_rotina,
            r.cor AS cor_rotina,
            SUM( (strftime('%s', i.horarioTermino) - strftime('%s', i.horarioInicio)) / 60 ) AS total_minutos
        FROM
            tabela_itens_cronograma AS i
        INNER JOIN
            tabela_rotinas AS r ON i.rotinaId = r.id
        GROUP BY
            r.id, r.nome, r.cor
        HAVING
            total_minutos > 0
        ORDER BY
            total_minutos DESC
    """)
    fun getStats(): Flow<List<StatsResult>>
}
