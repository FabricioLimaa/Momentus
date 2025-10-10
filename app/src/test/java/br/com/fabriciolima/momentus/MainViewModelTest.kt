// ARQUIVO: MainViewModelTest.kt (CÓDIGO CORRIGIDO)

package br.com.fabriciolima.momentus

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.fakes.FakeRepository
// CORREÇÃO: Pacote de utilitários foi renomeado de 'utils' para 'util'
import br.com.fabriciolima.momentus.utils.MainCoroutineRule
import br.com.fabriciolima.momentus.util.getOrAwaitValue
import br.com.fabriciolima.momentus.ui.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    // Regra para LiveData
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Regra para Coroutines
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var mainViewModel: MainViewModel
    private lateinit var fakeRepository: FakeRepository

    @Before
    fun setupViewModel() {
        fakeRepository = FakeRepository()
        mainViewModel = MainViewModel(fakeRepository)
    }

    // CORREÇÃO: Nome do teste, chamada de método e nome da propriedade LiveData atualizados.
    @Test
    fun insertRotina_updatesLiveData() {
        // Given
        // CORREÇÃO: Construtor atualizado para incluir os novos campos nullable.
        val novaRotina = Rotina(nome = "Teste", duracaoPadraoMinutos = 10, cor = "#FFFFFF", descricao = null, tag = null)

        // When
        // CORREÇÃO: O nome do método mudou de addRotina para insertRotina.
        mainViewModel.insertRotina(novaRotina)

        // Then
        // CORREÇÃO: O nome da propriedade LiveData mudou de rotinas para rotinasComMetas.
        val listaDeRotinasComMeta = mainViewModel.rotinasComMetas.getOrAwaitValue()
        val rotinaExisteNaLista = listaDeRotinasComMeta.any { it.rotina.id == novaRotina.id }

        assertTrue(rotinaExisteNaLista)
    }
}
