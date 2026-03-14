package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.RotinaRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.di.VersionCode
import br.com.fabriciolima.momentus.domain.usecase.DeleteRotinaUseCase
import br.com.fabriciolima.momentus.domain.usecase.MarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.SaveRotinaUseCase
import br.com.fabriciolima.momentus.domain.usecase.UnmarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpdateRotinaUseCase
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

sealed interface DialogState {
    object Hidden : DialogState
    data class EditRotina(val rotina: ItemCronograma) : DialogState
    data class ShowDetail(val rotina: ItemCronograma) : DialogState
    data class ConfirmDelete(val rotina: ItemCronograma) : DialogState
    data class ConfirmDeleteMultiple(val count: Int) : DialogState
    object AddNewRotina : DialogState
}

sealed interface LogoutEvent {
    object Success : LogoutEvent
}

data class GoogleCalendarEvent(
    val summary: String,
    val start: DateTime
)

data class EventsForDate(
    val localRotinas: List<ItemCronograma> = emptyList(),
    val googleEvents: List<GoogleCalendarEvent> = emptyList()
)

data class CalendarUiState(
    val allRotinaItems: List<ItemCronograma> = emptyList(),
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
    val isSelectionModeActive: Boolean = false,
    val selectedRotinaIds: Set<String> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val rotinaRepository: RotinaRepository,
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
    private val saveRotinaUseCase: SaveRotinaUseCase,
    private val updateRotinaUseCase: UpdateRotinaUseCase,
    private val deleteRotinaUseCase: DeleteRotinaUseCase,
    @VersionCode private val currentVersionCode: Int
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<LogoutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    private val jobs = mutableListOf<Job>()

    val installStatus = inAppUpdateManager.installStatus

    val eventsForSelectedDate: StateFlow<EventsForDate> = combine(
        _uiState,
        _selectedDate
    ) { state, date ->
        val localRotinas = state.allRotinaItems.filter {
            val itemDate = it.data
            itemDate != null && Instant.ofEpochMilli(itemDate).atZone(ZoneOffset.UTC).toLocalDate() == date
        }
        val googleEvents = state.googleCalendarEvents.filter { event ->
            val instant = Instant.ofEpochMilli(event.start.value)
            instant.atZone(ZoneOffset.UTC).toLocalDate() == date
        }
        EventsForDate(localRotinas, googleEvents)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsForDate()
    )

    init {
        collectData()
        
        listenForNewAchievements()
        checkIfNeedToShowUpdateBadge()
        listenForUpdateProgress()
        
        if (auth.currentUser != null) {
            startFirebaseListeners()
        }
    }

    private fun startFirebaseListeners() {
        viewModelScope.launch {
            categoryRepository.startListeningForChanges()
            rotinaRepository.startListeningForChanges()
            templateRepository.startListeningForChanges()
            categoryRepository.syncAllDataToLocal()
        }
    }

    private fun collectData() {
        rotinaRepository.todosOsItensDaRotina.onEach { items ->
            _uiState.update { it.copy(allRotinaItems = items) }
        }.launchIn(viewModelScope).also { jobs.add(it) }

        categoryRepository.allCategoriesWithMetas.onEach { categoriesWithMetas ->
            val categoriesMap = categoriesWithMetas.associateBy({ it.category.id }, { it.category })
            _uiState.update { it.copy(categoriesMap = categoriesMap) }
        }.launchIn(viewModelScope).also { jobs.add(it) }

        categoryRepository.idsHabitosConcluidos.onEach { ids ->
            _uiState.update { it.copy(completedHabitIds = ids.toSet()) }
        }.launchIn(viewModelScope).also { jobs.add(it) }

        userRepository.userData.onEach { data ->
            val finalData = data ?: auth.currentUser?.let { user ->
                UserData(displayName = user.displayName, email = user.email)
            }
            _uiState.update { it.copy(userData = finalData) }
        }.launchIn(viewModelScope).also { jobs.add(it) }

        categoryRepository.currentStreak.onEach { streak ->
            _uiState.update { it.copy(streak = streak) }
        }.launchIn(viewModelScope).also { jobs.add(it) }
    }

    // --- Multiple Selection Logic ---

    fun onRotinaLongPressed(rotinaId: String) {
        _uiState.update {
            it.copy(
                isSelectionModeActive = true,
                selectedRotinaIds = it.selectedRotinaIds + rotinaId
            )
        }
    }

    fun onRotinaClicked(rotinaId: String) {
        _uiState.update { currentState ->
            val newSelectedIds = if (currentState.selectedRotinaIds.contains(rotinaId)) {
                currentState.selectedRotinaIds - rotinaId
            } else {
                currentState.selectedRotinaIds + rotinaId
            }
            currentState.copy(
                selectedRotinaIds = newSelectedIds,
                isSelectionModeActive = newSelectedIds.isNotEmpty()
            )
        }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(isSelectionModeActive = false, selectedRotinaIds = emptySet()) }
    }

    fun onSelectAll() {
        _uiState.update { currentState ->
            val allVisibleRotinaIds = eventsForSelectedDate.value.localRotinas.map { it.id }.toSet()
            currentState.copy(selectedRotinaIds = allVisibleRotinaIds)
        }
    }

    fun confirmDeleteSelectedRotinas() {
        _uiState.update { it.copy(dialogState = DialogState.ConfirmDeleteMultiple(it.selectedRotinaIds.size)) }
    }

    fun deleteSelectedRotinas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dialogState = DialogState.Hidden) }
            try {
                val idsToDelete = _uiState.value.selectedRotinaIds
                if (idsToDelete.isNotEmpty()) {
                    rotinaRepository.deleteRotinasByIds(idsToDelete)
                    _uiState.update {
                        it.copy(
                            successMessage = "${idsToDelete.size} rotinas excluídas.",
                            isSelectionModeActive = false,
                            selectedRotinaIds = emptySet()
                        )
                    }
                    WidgetUpdater.requestUpdate(application)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao excluir rotinas.") }
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
            userRepository.lastSeenVersionCode.collect { lastSeenVersionCode ->
                _uiState.update { it.copy(showUpdateBadge = currentVersionCode > lastSeenVersionCode) }
            }
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
        stopAllDataCollection()
        inAppUpdateManager.unregisterListener()
    }

    private fun stopAllDataCollection() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        categoryRepository.stopListeningForChanges()
        rotinaRepository.stopListeningForChanges()
        templateRepository.stopListeningForChanges()
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                stopAllDataCollection()

                rotinaRepository.clear()
                categoryRepository.clearAllLocalData()
                templateRepository.clear()
                gamificationRepository.clear()

                googleSignInClient.signOut()
                auth.signOut()

                _logoutEvent.emit(LogoutEvent.Success)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao fazer logout: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    val allCategories: StateFlow<List<Category>> = categoryRepository.allCategoriesWithMetas
        .map { list -> list.map { it.category } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onAddNewRotinaClicked() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.AddNewRotina)
    }

    fun onEditRotinaClicked(rotina: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.EditRotina(rotina))
    }

    fun onShowDetailClicked(rotina: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ShowDetail(rotina))
    }

    fun onConfirmDeleteClicked(rotina: ItemCronograma) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ConfirmDelete(rotina))
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.Hidden)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun showRotinaDetails(rotinaId: String) {
        viewModelScope.launch {
            val rotina = rotinaRepository.getItemCronograma(rotinaId)
            if (rotina != null) {
                val rotinaDateLong = rotina.data
                if (rotinaDateLong != null) {
                    val rotinaDate = Instant.ofEpochMilli(rotinaDateLong).atZone(ZoneOffset.UTC).toLocalDate()
                    selectDate(rotinaDate)
                }
                _uiState.update { currentState ->
                    currentState.copy(dialogState = DialogState.ShowDetail(rotina))
                }
            }
        }
    }

    fun saveSingleRotina(
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
            val result = saveRotinaUseCase(titulo, descricao, data, horarioInicio, horarioTermino, category, salvarNoGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Rotina criada com sucesso!", dialogState = DialogState.Hidden) }
                    if (salvarNoGoogle) fetchGoogleCalendarEvents()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun updateRotina(
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
            val result = updateRotinaUseCase(item, novoTitulo, novaDescricao, novaData, novoHorarioInicio, novoHorarioTermino, novaCategory, sincronizarComGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when(result) {
                is Result.Success -> {
                     _uiState.update { it.copy(successMessage = "Rotina atualizada com sucesso!", dialogState = DialogState.Hidden) }
                    if(sincronizarComGoogle) fetchGoogleCalendarEvents()
                     WidgetUpdater.requestUpdate(application)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun deleteRotina(item: ItemCronograma) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                alarmScheduler.cancel(item)
                when (val result = categoryRepository.deleteCompleteEvent(item)) {
                    is Result.Success -> {
                        fetchGoogleCalendarEvents()
                        _uiState.value = _uiState.value.copy(successMessage = "Rotina excluída com sucesso!", dialogState = DialogState.Hidden)
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

    fun deleteRotinasByIds(ids: Set<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                rotinaRepository.deleteRotinasByIds(ids)
                _uiState.update {
                    it.copy(
                        successMessage = "${ids.size} rotinas excluídas.",
                        isSelectionModeActive = false,
                        selectedRotinaIds = emptySet()
                    )
                }
                WidgetUpdater.requestUpdate(application)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falha ao excluir rotinas.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun unmarkHabitAsCompleted(itemCronogramaId: String) {
        viewModelScope.launch {
            unmarkHabitAsCompletedUseCase(itemCronogramaId)
        }
    }

    fun fetchGoogleCalendarEvents() {
        viewModelScope.launch {
            val result = categoryRepository.fetchGoogleCalendarEvents()
            when (result) {
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
