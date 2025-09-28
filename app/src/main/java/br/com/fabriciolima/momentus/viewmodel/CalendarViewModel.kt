// ARQUIVO: viewmodel/CalendarViewModel.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.viewmodel

import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(private val repository: RotinaRepository) : ViewModel() {

    private val _mesVisivel = MutableLiveData(YearMonth.now())
    val mesVisivel: LiveData<YearMonth> = _mesVisivel

    private val _dataSelecionada = MutableLiveData(LocalDate.now())
    val dataSelecionada: LiveData<LocalDate> = _dataSelecionada

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { listaComMetas ->
        listaComMetas.map { it.rotina }
    }.asLiveData()

    val eventosDoCronograma: LiveData<Map<LocalDate, List<ItemCronogramaCompletado>>> =
        // Combina os 7 fluxos de dados (um para cada dia da semana) com os hábitos concluídos
        combine(
            repository.getItensDoDia("DOM"), repository.getItensDoDia("SEG"),
            repository.getItensDoDia("TER"), repository.getItensDoDia("QUA"),
            repository.getItensDoDia("QUI"), repository.getItensDoDia("SEX"),
            repository.getItensDoDia("SÁB"), repository.idsHabitosConcluidos
        ) { arrays ->
            val itensPorDiaDaSemana = arrays.take(7).map { it as List<ItemCronograma> }
            val idsConcluidos = arrays[7] as List<String>
            val mapaEventos = mutableMapOf<LocalDate, MutableList<ItemCronogramaCompletado>>()
            val hoje = LocalDate.now()

            // Mapeia eventos para um ano à frente e um ano para trás
            for (i in -365..365) {
                val dataAtual = hoje.plusDays(i.toLong())
                val diaDaSemanaIndex = dataAtual.dayOfWeek.value % 7 // DOM=0, SEG=1...

                val itensParaEsteDia = itensPorDiaDaSemana[diaDaSemanaIndex]
                if (itensParaEsteDia.isNotEmpty()) {
                    val itensCompletados = itensParaEsteDia.map { item ->
                        ItemCronogramaCompletado(item = item, completado = idsConcluidos.contains(item.id))
                    }
                    mapaEventos[dataAtual] = itensCompletados.toMutableList()
                }
            }
            mapaEventos
        }.asLiveData()


    fun selecionarData(data: LocalDate) {
        _dataSelecionada.value = data
    }

    // --- MODIFICAÇÃO: Funções para navegar entre os meses ---
    fun irParaMesAnterior() {
        _mesVisivel.value = _mesVisivel.value?.minusMonths(1)
    }

    fun irParaProximoMes() {
        _mesVisivel.value = _mesVisivel.value?.plusMonths(1)
    }
}

// ... (A Factory continua a mesma)

// ... (A Factory continua a mesma)

class CalendarViewModelFactory(private val repository: RotinaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}