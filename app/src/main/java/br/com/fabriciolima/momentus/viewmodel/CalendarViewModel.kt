// ARQUIVO: viewmodel/CalendarViewModel.kt (CÓDIGO COMPLETO E FINAL)

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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalendarViewModel(private val repository: RotinaRepository, application: Application) : AndroidViewModel(application) {

    private val _mesVisivel = MutableLiveData(YearMonth.now())
    val mesVisivel: LiveData<YearMonth> = _mesVisivel

    private val _dataSelecionada = MutableLiveData(LocalDate.now())
    val dataSelecionada: LiveData<LocalDate> = _dataSelecionada

    val todasAsRotinas: LiveData<List<Rotina>> = repository.todasAsRotinasComMetas.map { listaComMetas ->
        listaComMetas.map { it.rotina }
    }.asLiveData()

    // --- GRANDE MODIFICAÇÃO: Lógica final para mapear todos os tipos de evento ---
    val eventosDoCronograma: LiveData<Map<LocalDate, List<ItemCronogramaCompletado>>> =
        // Combina o fluxo de todos os itens agendados com o fluxo dos hábitos concluídos
        repository.todosOsItensDoCronograma.combine(repository.idsHabitosConcluidos) { todosOsItens, idsConcluidos ->
            val mapaEventos = mutableMapOf<LocalDate, MutableList<ItemCronogramaCompletado>>()

            // Separa os itens em dois grupos: recorrentes e únicos
            val itensRecorrentes = todosOsItens.filter { it.diaDaSemana != null }
            val eventosUnicos = todosOsItens.filter { it.data != null }

            // 1. Adiciona os eventos únicos ao mapa
            eventosUnicos.forEach { evento ->
                val data = Instant.ofEpochMilli(evento.data!!).atZone(ZoneId.systemDefault()).toLocalDate()
                val eventoCompletado = ItemCronogramaCompletado(item = evento, completado = idsConcluidos.contains(evento.id))
                mapaEventos.getOrPut(data) { mutableListOf() }.add(eventoCompletado)
            }

            // 2. Adiciona os eventos recorrentes (rotinas) ao mapa
            if (itensRecorrentes.isNotEmpty()) {
                val hoje = LocalDate.now()
                for (i in -365..365) { // Gera para um ano no passado e um no futuro
                    val dataAtual = hoje.plusDays(i.toLong())
                    val diaDaSemanaAtual = if (dataAtual.dayOfWeek == DayOfWeek.SUNDAY) "DOM" else dataAtual.dayOfWeek.name.substring(0, 3)

                    itensRecorrentes.filter { it.diaDaSemana == diaDaSemanaAtual }.forEach { itemRecorrente ->
                        val itemCompletado = ItemCronogramaCompletado(item = itemRecorrente, completado = idsConcluidos.contains(itemRecorrente.id))
                        mapaEventos.getOrPut(dataAtual) { mutableListOf() }.add(itemCompletado)
                    }
                }
            }

            // Ordena os itens de cada dia por horário
            mapaEventos.forEach { (_, lista) ->
                lista.sortBy { it.item.horarioInicio }
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

    fun onHabitoConcluidoChanged(item: ItemCronograma, isChecked: Boolean) = viewModelScope.launch {
        if (isChecked) {
            repository.marcarHabitoComoConcluido(item.id)
        } else {
            repository.desmarcarHabitoComoConcluido(item.id)
        }
    }

    fun salvarEventoUnico(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        inicio: java.time.LocalTime,
        fim: java.time.LocalTime,
        rotina: Rotina
    ) = viewModelScope.launch {
        val duracao = java.time.Duration.between(inicio, fim).toMinutes().toInt()
        val rotinaEventoUnico = Rotina(
            nome = titulo,
            descricao = descricao,
            tag = rotina.tag,
            cor = rotina.cor,
            duracaoPadraoMinutos = duracao
        )
        repository.insert(rotinaEventoUnico)
        val novoItem = ItemCronograma(
            data = data.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            diaDaSemana = null,
            horarioInicio = inicio.format(DateTimeFormatter.ofPattern("HH:mm")),
            rotinaId = rotinaEventoUnico.id
        )
        repository.insertItemCronograma(novoItem)
    }
}

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