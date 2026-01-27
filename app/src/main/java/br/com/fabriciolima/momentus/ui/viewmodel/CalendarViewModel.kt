package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.di.VersionCode
import br.com.fabriciolima.momentus.domain.usecase.MarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.SaveEventUseCase
import br.com.fabriciolima.momentus.domain.usecase.UnmarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpdateEventUseCase
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.util.UpdateProgress
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.api.client.util.DateTime
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    data class ConfirmDeleteMultiple(val count: Int) : DialogState
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
    val categoriesMap: Map<String, Category> = emptyMap(),
    val completedHabitIds: Set<String> = emptySet(),
    val googleCalendarEvents: List<GoogleCalendarEvent> = emptyList(),
    val userData: UserData? = null,
    val streak: Int = 0,
    val newlyUnlockedAchievement: Achievement? = null,
    val updateInfo: AppUpdateInfo? = null,
    val showUpdateBadge: Boolean = false,
    val updateProgress: UpdateProgress? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val dialogState: DialogState = DialogState.Hidden,
    val isLoading: Boolean = false,
    // State for multiple selection
    val isSelectionModeActive: Boolean = false,
    val selectedEventIds: Set<String> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val eventoRepository: EventoRepository,
    private val templateRepository: TemplateRepository,
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val inAppUpdateManager: InAppUpdateManager,
    private val googleSignInClient: GoogleSignInClient,
    private val auth: FirebaseAuth,
    private val application: Application,
    private val markHabitAsCompletedUseCase: MarkHabitAsCompletedUseCase,
    private val unmarkHabitAsCompletedUseCase: UnmarkHabitAsCompletedUseCase,
    private val saveEventUseCase: SaveEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    @VersionCode private val currentVersionCode: Int
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<LogoutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    private var dataCollectionJob: Job? = null

    val installStatus = inAppUpdateManager.installStatus

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
        startDataCollection()
        listenForNewAchievements()
        checkIfNeedToShowUpdateBadge()
        listenForUpdateProgress()
    }

    // --- Multiple Selection Logic ---

    fun onEventLongPressed(eventId: String) {
        _uiState.update {
            it.copy(
                isSelectionModeActive = true,
                selectedEventIds = it.selectedEventIds + eventId
            )
        }
    }

    fun onEventClicked(eventId: String) {
        _uiState.update { currentState ->
            val newSelectedIds = if (currentState.selectedEventIds.contains(eventId)) {
                currentState.selectedEventIds - eventId
            } else {
                currentState.selectedEventIds + eventId
            }
            // If the last item is deselected, exit selection mode
            currentState.copy(
                selectedEventIds = newSelectedIds,
                isSelectionModeActive = newSelectedIds.isNotEmpty()
            )
        }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(isSelectionModeActive = false, selectedEventIds = emptySet()) }
    }

    fun onSelectAll() {
        _uiState.update { currentState ->
            val allVisibleEventIds = eventsForSelectedDate.value.localEvents.map { it.id }.toSet()
            currentState.copy(selectedEventIds = allVisibleEventIds)
        }
    }

    fun confirmDeleteSelectedEvents() {
        _uiState.update { it.copy(dialogState = DialogState.ConfirmDeleteMultiple(it.selectedEventIds.size)) }
    }

    fun deleteSelectedEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dialogState = DialogState.Hidden) }
            try {
                val idsToDelete = _uiState.value.selectedEventIds
                if (idsToDelete.isNotEmpty()) {
                    eventoRepository.deleteEventsByIds(idsToDelete)
                    _uiState.update {
                        it.copy(
                            successMessage = "${idsToDelete.size} eventos excluídos.",
                            isSelectionModeActive = false,
                            selectedEventIds = emptySet()
                        )
                    }
                    WidgetUpdater.requestUpdate(application)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao excluir eventos.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- End of Multiple Selection Logic ---

    private fun listenForUpdateProgress() {
        inAppUpdateManager.updateProgress
            .onEach { progress ->
                _uiState.update { it.copy(updateProgress = progress) }
            }
            .launchIn(viewModelScope)
    }

    private fun checkIfNeedToShowUpdateBadge() {
        viewModelScope.launch {
            val lastSeenVersionCode = userRepository.lastSeenVersionCode.first()
            _uiState.update { it.copy(showUpdateBadge = currentVersionCode > lastSeenVersionCode) }
        }
    }

    fun onUpdatesClicked() {
        viewModelScope.launch {
            userRepository.updateLastSeenVersionCode(currentVersionCode)
            _uiState.update { it.copy(showUpdateBadge = false) }
        }
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            val updateInfo = inAppUpdateManager.checkForUpdate()
            _uiState.update { it.copy(updateInfo = updateInfo) }
        }
    }

    fun onUpdateDialogDismissed() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    private fun startDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = combine(
            eventoRepository.todosOsItensDoCronograma,
            categoryRepository.allCategoriesWithMetas,
            categoryRepository.idsHabitosConcluidos,
            userRepository.userData,
            categoryRepository.currentStreak
        ) { allItems, categoriesWithMetas, completedIds, userData, streak ->
            val categoriesMap = categoriesWithMetas.associateBy({ it.category.id }, { it.category })
            _uiState.update { currentState ->
                currentState.copy(
                    allScheduleItems = allItems,
                    categoriesMap = categoriesMap,
                    completedHabitIds = completedIds.toSet(),
                    userData = userData,
                    streak = streak
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun listenForNewAchievements() {
        viewModelScope.launch {
            gamificationRepository.newlyUnlockedAchievement.collect { achievement ->
                _uiState.update { it.copy(newlyUnlockedAchievement = achievement) }
            }
        }
    }

    fun onAchievementDialogDismissed() {
        _uiState.update { it.copy(newlyUnlockedAchievement = null) }
    }

    override fun onCleared() {
        super.onCleared()
        dataCollectionJob?.cancel()
        categoryRepository.stopListeningForChanges()
        templateRepository.stopListeningForChanges()
        inAppUpdateManager.unregisterListener()
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Parar de ouvir mudanças do Firebase
                dataCollectionJob?.cancel()
                categoryRepository.stopListeningForChanges()
                templateRepository.stopListeningForChanges()

                // 2. Limpar todos os dados locais
                eventoRepository.clear()
                categoryRepository.clearAllLocalData()
                templateRepository.clear()
                gamificationRepository.clear()

                // 3. Fazer logout dos serviços de autenticação
                googleSignInClient.signOut().await()
                auth.signOut()

                // 4. Emitir evento de sucesso
                _logoutEvent.emit(LogoutEvent.Success)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao fazer logout: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    val allCategories: StateFlow<List<Category>> = categoryRepository.allCategoriesWithMetas.map { categoriesWithMetas ->
        categoriesWithMetas.map(CategoryWithMeta::category)
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

    fun saveSingleEvent(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        category: Category,
        salvarNoGoogle: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = saveEventUseCase(titulo, descricao, data, horarioInicio, horarioTermino, category, salvarNoGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Evento criado com sucesso!", dialogState = DialogState.Hidden) }
                    if (salvarNoGoogle) fetchGoogleCalendarEvents()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun updateEvent(
        item: ItemCronograma,
        novoTitulo: String,
        novaDescricao: String?,
        novaData: LocalDate,
        novoHorarioInicio: LocalTime,
        novoHorarioTermino: LocalTime,
        novaCategory: Category,
        sincronizarComGoogle: Boolean
    ) {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) }
            val result = updateEventUseCase(item, novoTitulo, novaDescricao, novaData, novoHorarioInicio, novoHorarioTermino, novaCategory, sincronizarComGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when(result) {
                is Result.Success -> {
                     _uiState.update { it.copy(successMessage = "Evento atualizado com sucesso!", dialogState = DialogState.Hidden) }
                    if(sincronizarComGoogle) fetchGoogleCalendarEvents()
                     WidgetUpdater.requestUpdate(application)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun deleteEvent(item: ItemCronograma) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                alarmScheduler.cancel(item)
                when (val result = categoryRepository.deleteCompleteEvent(item)) {
                    is Result.Success -> {
                        fetchGoogleCalendarEvents()
                        _uiState.value = _uiState.value.copy(successMessage = "Evento excluído com sucesso!", dialogState = DialogState.Hidden)
                        WidgetUpdater.requestUpdate(application)
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

    fun markHabitAsCompleted(itemCronogramaId: String) {
        viewModelScope.launch {
            markHabitAsCompletedUseCase(itemCronogramaId)
        }
    }

    fun unmarkHabitAsCompleted(itemCronogramaId: String) {
        viewModelScope.launch {
            unmarkHabitAsCompletedUseCase(itemCronogramaId)
        }
    }

    fun fetchGoogleCalendarEvents() {
        viewModelScope.launch {
            when (val result = categoryRepository.fetchGoogleCalendarEvents()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(googleCalendarEvents = result.data, error = null)
                is Result.Error -> _uiState.value = _uiState.value.copy(googleCalendarEvents = emptyList(), error = result.exception.message)
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
