// ARQUIVO: data/database/ItemCronogramaDao.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data.database

import androidx.room.* // Mude para wildcard para incluir Transaction
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.StatsResult
import br.com.fabriciolima.momentus.data.TemplateItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCronogramaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemCronograma)

    @Delete
    suspend fun delete(item: ItemCronograma)

    @Query("SELECT * FROM tabela_itens_cronograma WHERE diaDaSemana = :dia ORDER BY horarioInicio ASC")
    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>>

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Esta é uma consulta SQL mais complexa:
    // 1. SELECT r.nome, r.cor, SUM(r.duracaoPadraoMinutos): Selecionamos o nome e a cor da rotina, e a SOMA dos minutos.
    // 2. FROM tabela_itens_cronograma AS ic: A partir da tabela de itens do cronograma.
    // 3. INNER JOIN tabela_de_rotinas AS r ON ic.rotinaId = r.id: Juntamos com a tabela de rotinas onde os IDs correspondem.
    // 4. GROUP BY r.nome, r.cor: Agrupamos os resultados por rotina para que o SUM() funcione corretamente.
    @Query("""
        SELECT r.nome AS nomeRotina, r.cor AS corRotina, SUM(r.duracaoPadraoMinutos) as totalMinutos
        FROM tabela_itens_cronograma AS ic
        INNER JOIN tabela_de_rotinas AS r ON ic.rotinaId = r.id
        GROUP BY r.nome, r.cor
    """)
    fun getStats(): Flow<List<StatsResult>>
    // --- MODIFICAÇÃO TERMINA AQUI ---

    // --- MODIFICAÇÃO INICIA AQUI ---
    @Query("DELETE FROM tabela_itens_cronograma")
    suspend fun deleteAllCronogramaItems()

    @Query("SELECT * FROM tabela_itens_template WHERE templateId = :templateId")
    suspend fun getItemsForTemplate(templateId: String): List<TemplateItem>

    @Transaction
    suspend fun loadTemplateIntoSchedule(templateId: String) {
        // 1. Apaga o cronograma atual.
        deleteAllCronogramaItems()
        // 2. Busca os itens do template selecionado.
        val templateItems = getItemsForTemplate(templateId)
        // 3. Converte os itens de template para itens de cronograma.
        val cronogramaItems = templateItems.map { item ->
            ItemCronograma(
                diaDaSemana = item.diaDaSemana,
                horarioInicio = item.horarioInicio,
                rotinaId = item.rotinaId
            )
        }
        // 4. Insere todos os novos itens no cronograma principal.
        cronogramaItems.forEach { insert(it) }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}