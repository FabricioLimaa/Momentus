// ARQUIVO: viewmodel/MainViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.launch


class MainViewModel(private val repository: RotinaRepository) : ViewModel() {

    val rotinas: LiveData<List<Rotina>> = repository.todasAsRotinas.asLiveData()

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Variável para guardar a última rotina que foi deletada.
    // Ela fica na memória, pronta para ser restaurada se o usuário clicar em "Desfazer".
    private var ultimaRotinaDeletada: Rotina? = null
    // --- MODIFICAÇÃO TERMINA AQUI ---

    fun addRotina(novaRotina: Rotina) = viewModelScope.launch {
        repository.insert(novaRotina)
    }

    fun deleteRotina(rotina: Rotina) = viewModelScope.launch {
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Antes de deletar, guardamos a rotina na nossa variável temporária.
        ultimaRotinaDeletada = rotina
        // --- MODIFICAÇÃO TERMINA AQUI ---
        repository.delete(rotina)
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 3. Nova função para reinserir a última rotina deletada.
    // O Room irá simplesmente adicioná-la de volta ao banco de dados.
    fun reinsereRotina() = viewModelScope.launch {
        ultimaRotinaDeletada?.let {
            repository.insert(it)
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}