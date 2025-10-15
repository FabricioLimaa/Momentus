package br.com.fabriciolima.momentus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Classe de dados para transportar informações de eventos para o widget.
 * Combina dados do ItemCronograma e da Rotina.
 */
data class WidgetEventItem(
    val id: String,
    val titulo: String,
    val horarioInicio: LocalTime,
    val horarioTermino: LocalTime,
    val nomeRotina: String,
    val corRotina: String?
)

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

    @Query("SELECT * FROM tabela_itens_cronograma WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItemCronograma?

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia")
    fun getItemsByDayOfWeek(dia: String): Flow<List<ItemCronograma>>

    /**
     * Busca de forma SÍNCRONA todos os itens que são de uma data específica OU de um dia da semana recorrente,
     * E que pertencem a uma das rotinas permitidas.
     */
    @Query("""
        SELECT * FROM tabela_itens_cronograma 
        WHERE 
            (
                (data / 86400000) = :epochDay 
                OR 
                (diaDaSemana = :dayOfWeekName AND data IS NULL)
            )
            AND rotinaId IN (:allowedRotinaIds)
    """)
    fun getForWidget(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<ItemCronograma>


    /**
     * NOVA CONSULTA OTIMIZADA PARA O WIDGET.
     * Busca e une os dados do evento e da rotina em uma única chamada.
     */
    @Query("""
        SELECT
            i.id,
            i.titulo,
            i.horarioInicio,
            i.horarioTermino,
            r.nome AS nomeRotina,
            r.cor AS corRotina
        FROM
            tabela_itens_cronograma AS i
        INNER JOIN
            tabela_rotinas AS r ON i.rotinaId = r.id
        WHERE
            (
                (i.data / 86400000) = :epochDay
                OR
                (i.diaDaSemana = :dayOfWeekName AND i.data IS NULL)
            )
            AND i.rotinaId IN (:allowedRotinaIds)
        ORDER BY i.horarioInicio ASC
    """)
    fun getWidgetEventItems(epochDay: Long, dayOfWeekName: String, allowedRotinaIds: Set<String>): List<WidgetEventItem>
}
