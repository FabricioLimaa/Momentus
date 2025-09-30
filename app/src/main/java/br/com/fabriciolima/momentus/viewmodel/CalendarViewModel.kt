package br.com.fabriciolima.momentus.viewmodel

import android.app.Application
import androidx.lifecycle.*
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// O ViewModel precisa herdar de AndroidViewModel para o Application Context
class CalendarViewModel(private val repository: RotinaRepository, application: Application) : AndroidViewModel(application) {

    private val _mesVisivel = MutableLiveData(YearMonth.now())
    val mesVisivel: LiveData<YearMonth> = _mesVisivel

    private val _dataSelecionada = MutableLiveData(LocalDate.now())
    val dataSelecionada: LiveData<LocalDate> = _dataSelecionada

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { listaComMetas ->
        listaComMetas.map { it.rotina }
    }.asLiveData()

    val eventosDoCronograma: LiveData<Map<LocalDate, List<ItemCronogramaCompletado>>> =
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

            for (i in -365..365) {
                val dataAtual = hoje.plusDays(i.toLong())
                val diaDaSemanaIndex = if (dataAtual.dayOfWeek == DayOfWeek.SUNDAY) 0 else dataAtual.dayOfWeek.value

                val itensParaEsteDia = itensPorDiaDaSemana[diaDaSemanaIndex]
                if (itensParaEsteDia.isNotEmpty()) {
                    val itensCompletados = itensParaEsteDia.map { item ->
                        ItemCronogramaCompletado(item = item, completado = idsConcluidos.contains(item.id))
                    }
                    mapaEventos[dataAtual] = itensCompletados.sortedBy { it.item.horarioInicio }.toMutableList()
                }
            }
            mapaEventos
        }.asLiveData()


    fun selecionarData(data: LocalDate) {
        _dataSelecionada.value = data
    }

    fun irParaMesAnterior() {
        _mesVisivel.value = _mesVisivel.value?.minusMonths(1)
    }

    fun irParaProximoMes() {
        _mesVisivel.value = _mesVisivel.value?.plusMonths(1)
    }

    // --- FUNÇÃO QUE ESTAVA FALTANDO ---
    fun onHabitoConcluidoChanged(item: ItemCronograma, isChecked: Boolean) = viewModelScope.launch {
        if (isChecked) {
            repository.marcarHabitoComoConcluido(item.id)
        } else {
            repository.desmarcarHabitoComoConcluido(item.id)
        }
    }
}

// A Factory precisa passar o Application para o ViewModel
class CalendarViewModelFactory(
    private val repository: RotinaRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}