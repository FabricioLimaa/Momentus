package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.client.util.DateTime
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

sealed interface DialogState {
    object Hidden : DialogState
    data class EditEvent(val event: ItemCronograma) : DialogState
    data class ShowDetail(val event: ItemCronograma) : DialogState
    data class ConfirmDelete(val event: ItemCronograma) : DialogState
    object AddNewEvent : DialogState
}

sealed interface LogoutEvent {
    object Success : LogoutEvent
}

data class GoogleCalendarEvent(
    val summary: String,
    val start: DateTime
)

data class EventsForDate(
    val localEvents: List<ItemCronograma> = emptyList(),
    val googleEvents: List<GoogleCalendarEvent> = emptyList()
)

data class CalendarUiState(
    val allScheduleItems: List<ItemCronograma> = emptyList(),
    val rotinasMap: Map<String, Rotina> = emptyMap(),
    val completedHabitIds: Set<String> = emptySet(),
    val googleCalendarEvents: List<GoogleCalendarEvent> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val dialogState: DialogState = DialogState.Hidden,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: RotinaRepository,
    private val eventoRepository: EventoRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val auth: FirebaseAuth,
    private val application: Application
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<LogoutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    val eventsForSelectedDate: StateFlow<EventsForDate> = combine(
        _uiState,
        _selectedDate
    ) { state, date ->
        val localEvents = state.allScheduleItems.filter {
            it.data != null && Instant.ofEpochMilli(it.data).atZone(ZoneOffset.UTC).toLocalDate() == date
        }
        val googleEvents = state.googleCalendarEvents.filter { event ->
            val instant = Instant.ofEpochMilli(event.start.value)
            instant.atZone(ZoneOffset.UTC).toLocalDate() == date
        }
        EventsForDate(localEvents, googleEvents)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsForDate()
    )

    init {
        repository.startListeningForChanges()

        viewModelScope.launch {
            combine(
                eventoRepository.todosOsItensDoCronograma,
                repository.todasAsRotinasComMetas,
                repository.idsHabitosConcluidos
            ) { allItems: List<ItemCronograma>, rotinasComMetas: List<RotinaComMeta>, completedIds: List<String> ->
                val rotinasMap = rotinasComMetas.associateBy({ it.rotina.id }, { it.rotina })
                Triple(allItems, rotinasMap, completedIds.toSet())
            }.collect { (allItems, rotinasMap, completedIds) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        allScheduleItems = allItems,
                        rotinasMap = rotinasMap,
                        completedHabitIds = completedIds
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListeningForChanges()
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Interrompe a sincronização em tempo real
                repository.stopListeningForChanges()

                // 2. Desconecta do Google Sign-In
                googleSignInClient.signOut().await()

                // 3. Desconecta do Firebase Auth
                auth.signOut()

                // 4. Limpa todos os dados locais
                repository.clearAllLocalData()

                // 5. Emite o evento de sucesso do logout
                _logoutEvent.emit(LogoutEvent.Success)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao fazer logout: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    val todasAsRotinas: StateFlow<List<Rotina>> = repository.todasAsRotinasComMetas.map { rotinasComMetas ->
        rotinasComMetas.map(RotinaComMeta::rotina)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onAddNewEventClicked() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.AddNewEvent)
    }

    fun onEditEventClicked(event: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.EditEvent(event))
    }

    fun onShowDetailClicked(event: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ShowDetail(event))
    }

    fun onConfirmDeleteClicked(event: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ConfirmDelete(event))
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.Hidden)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun showEventDetails(eventId: String) {
        viewModelScope.launch {
            val event = eventoRepository.getItemCronograma(eventId)
            if (event != null) {
                if (event.data != null) {
                    val eventDate = Instant.ofEpochMilli(event.data!!).atZone(ZoneOffset.UTC).toLocalDate()
                    selectDate(eventDate)
                }
                _uiState.update { currentState ->
                    currentState.copy(dialogState = DialogState.ShowDetail(event))
                }
            }
        }
    }

    fun salvarEventoUnico(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        rotina: Rotina,
        salvarNoGoogle: Boolean
    ) {
        if (horarioTermino.isBefore(horarioInicio) || horarioTermino == horarioInicio) {
            _uiState.value = _uiState.value.copy(error = "O horário de término deve ser depois do início.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                var googleEventId: String? = null

                if (salvarNoGoogle) {
                    when (val result = repository.salvarEventoNoGoogle(titulo, descricao, data, horarioInicio, horarioTermino, rotina.cor)) {
                        is Result.Success -> {
                            googleEventId = result.data
                            fetchGoogleCalendarEvents()
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(error = result.exception.message)
                        }
                    }
                }

                val novoItem = ItemCronograma(
                    titulo = titulo,
                    descricao = descricao,
                    data = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    diaDaSemana = null,
                    horarioInicio = horarioInicio,
                    horarioTermino = horarioTermino,
                    rotinaId = rotina.id,
                    templateId = null,
                    googleCalendarEventId = googleEventId
                )
                eventoRepository.insertItemCronograma(novoItem)

                _uiState.value = _uiState.value.copy(successMessage = "Evento criado com sucesso!", dialogState = DialogState.Hidden)
                WidgetUpdater.sendBroadcast(application)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun atualizarEvento(
        item: ItemCronograma,
        novoTitulo: String,
        novaDescricao: String?,
        novaData: LocalDate,
        novoHorarioInicio: LocalTime,
        novoHorarioTermino: LocalTime,
        novaRotina: Rotina,
        sincronizarComGoogle: Boolean
    ) {
        if (novoHorarioTermino.isBefore(novoHorarioInicio) || novoHorarioTermino == novoHorarioInicio) {
            _uiState.value = _uiState.value.copy(error = "O horário de término deve ser depois do início.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val itemAtualizado = item.copy(
                    titulo = novoTitulo,
                    descricao = novaDescricao,
                    data = novaData.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    horarioInicio = novoHorarioInicio,
                    horarioTermino = novoHorarioTermino,
                    rotinaId = novaRotina.id
                )

                if (sincronizarComGoogle) {
                    when (val result = repository.atualizarEventoCompleto(itemAtualizado, novaRotina.cor)) {
                        is Result.Success -> fetchGoogleCalendarEvents()
                        is Result.Error -> _uiState.value = _uiState.value.copy(error = result.exception.message ?: "Falha ao sincronizar atualização com o Google Calendar.")
                    }
                } else {
                    eventoRepository.insertItemCronograma(itemAtualizado)
                }

                _uiState.value = _uiState.value.copy(successMessage = "Evento atualizado com sucesso!", dialogState = DialogState.Hidden)
                WidgetUpdater.sendBroadcast(application)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun excluirEvento(item: ItemCronograma) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (val result = repository.excluirEventoCompleto(item)) {
                    is Result.Success -> {
                        fetchGoogleCalendarEvents()
                        _uiState.value = _uiState.value.copy(successMessage = "Evento excluído com sucesso!", dialogState = DialogState.Hidden)
                        WidgetUpdater.sendBroadcast(application)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.exception.message)
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.marcarHabitoComoConcluido(itemCronogramaId)
        }
    }

    fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        viewModelScope.launch {
            repository.desmarcarHabitoComoConcluido(itemCronogramaId)
        }
    }

    fun fetchGoogleCalendarEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (val result = repository.fetchGoogleCalendarEvents()) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(googleCalendarEvents = result.data, error = null)
                    is Result.Error -> _uiState.value = _uiState.value.copy(googleCalendarEvents = emptyList(), error = result.exception.message)
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onSuccessMessageShown() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
