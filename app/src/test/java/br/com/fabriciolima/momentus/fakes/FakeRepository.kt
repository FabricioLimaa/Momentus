package br.com.fabriciolima.momentus.fakes

import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaComMeta
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRepository : RotinaRepository(null, null, null, null) {

    private val rotinasFlow = MutableStateFlow<List<RotinaComMeta>>(emptyList())

    // Usamos 'override' para sobrescrever a propriedade da classe pai
    override val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinasFlow

    override suspend fun insert(rotina: Rotina) {
        val listaAtual = rotinasFlow.value.toMutableList()
        val index = listaAtual.indexOfFirst { it.rotina.id == rotina.id }
        val rotinaComMeta = RotinaComMeta(rotina = rotina, meta = null)

        if (index == -1) {
            listaAtual.add(rotinaComMeta)
        } else {
            listaAtual[index] = rotinaComMeta
        }
        rotinasFlow.value = listaAtual
    }

    override suspend fun delete(rotina: Rotina) {
        val listaAtual = rotinasFlow.value.toMutableList()
        listaAtual.removeAll { it.rotina.id == rotina.id }
        rotinasFlow.value = listaAtual
    }
}