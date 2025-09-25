// ARQUIVO: MainViewModelTest.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.fakes.FakeRepository
import br.com.fabriciolima.momentus.utils.MainCoroutineRule
import br.com.fabriciolima.momentus.utils.getOrAwaitValue
import br.com.fabriciolima.momentus.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// --- MODIFICAÇÃO: Adicionamos esta anotação ---
@ExperimentalCoroutinesApi
class MainViewModelTest {

    // Regra para LiveData
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // --- MODIFICAÇÃO: Adicionamos a nova regra para Coroutines ---
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var mainViewModel: MainViewModel
    private lateinit var fakeRepository: FakeRepository

    @Before
    fun setupViewModel() {
        fakeRepository = FakeRepository()
        mainViewModel = MainViewModel(fakeRepository)
    }

    @Test
    fun addRotina_updatesLiveData() {
        // Given
        val novaRotina = Rotina(nome = "Teste", duracaoPadraoMinutos = 10, cor = "#FFFFFF")

        // When
        mainViewModel.addRotina(novaRotina)

        // Then
        val listaDeRotinasComMeta = mainViewModel.rotinas.getOrAwaitValue()
        val rotinaExisteNaLista = listaDeRotinasComMeta.any { it.rotina.id == novaRotina.id }

        assertTrue(rotinaExisteNaLista)
    }
}