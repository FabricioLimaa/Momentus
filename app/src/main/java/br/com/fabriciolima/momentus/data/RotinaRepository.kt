// ARQUIVO: data/RotinaRepository.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data

import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import kotlinx.coroutines.flow.Flow
import br.com.fabriciolima.momentus.data.StatsResult

class RotinaRepository(private val rotinaDao: RotinaDao, private val itemCronogramaDao: ItemCronogramaDao) {
    val todasAsRotinas: Flow<List<Rotina>> = rotinaDao.getAllRotinas()

    // --- MODIFICAÇÃO INICIA AQUI ---
    val stats: Flow<List<StatsResult>> = itemCronogramaDao.getStats()
    // --- MODIFICAÇÃO TERMINA AQUI ---

    suspend fun insert(rotina: Rotina) {
        rotinaDao.insert(rotina)
    }

    suspend fun delete(rotina: Rotina) {
        rotinaDao.delete(rotina)
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItensDoDia(dia)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Nova função que repassa a ordem de deleção para o ItemCronogramaDao.
    suspend fun deleteItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.delete(item)
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}