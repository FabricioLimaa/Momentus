package br.com.fabriciolima.momentus.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.data.repository.TemplateRepository
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
import br.com.fabriciolima.momentus.di.VersionCode
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.domain.model.UserLevel
import br.com.fabriciolima.momentus.domain.usecase.DeleteScheduleItemUseCase
import br.com.fabriciolima.momentus.domain.usecase.MarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.SaveScheduleItemUseCase
import br.com.fabriciolima.momentus.domain.usecase.UnmarkHabitAsCompletedUseCase
import br.com.fabriciolima.momentus.domain.usecase.UpdateScheduleItemUseCase
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.util.SoundManager
import br.com.fabriciolima.momentus.util.SyncManager
import br.com.fabriciolima.momentus.util.UpdateProgress
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.api.client.util.DateTime
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

private const val TAG = "CalendarViewModel"

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
    val error: AppError? = null,
    val successMessage: String? = null,
    val dialogState: DialogState = DialogState.Hidden,
    val isLoading: Boolean = false,
    val isSelectionModeActive: Boolean = false,
    val selectedRotinaIds: Set<String> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val templateRepository: TemplateRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gamificationRepository: GamificationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val inAppUpdateManager: InAppUpdateManager,
    private val syncManager: SyncManager,
    private val soundManager: SoundManager,
    private val googleSignInClient: GoogleSignInClient,
    private val auth: FirebaseAuth,
    private val application: Application,
    private val markHabitAsCompletedUseCase: MarkHabitAsCompletedUseCase,
    private val unmarkHabitAsCompletedUseCase: UnmarkHabitAsCompletedUseCase,
    private val saveScheduleItemUseCase: SaveScheduleItemUseCase,
    private val updateScheduleItemUseCase: UpdateScheduleItemUseCase,
    private val deleteScheduleItemUseCase: DeleteScheduleItemUseCase,
    @VersionCode private val currentVersionCode: Int
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<LogoutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    private val _showCompletionAnimation = MutableSharedFlow<Unit>()
    val showCompletionAnimation = _showCompletionAnimation.asSharedFlow()

    private var lastKnownPoints: Int = 0
    private val jobs = mutableListOf<Job>()
    
    // Flag de controle para evitar animações indesejadas na inicialização
    private var isFullyLoaded = false

    val installStatus = inAppUpdateManager.installStatus

    val eventsForSelectedDate: StateFlow<EventsForDate> = combine(
        _uiState,
        _selectedDate
    ) { state, date ->
        val localRotinas = state.allScheduleItems.filter {
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
        Log.d(TAG, "ViewModel inicializado. Iniciando coleta de dados e listeners.")
        collectData()
        listenForSyncCompletion()
        listenForNewAchievements()
        checkIfNeedToShowUpdateBadge()
        listenForUpdateProgress()
        startFirebaseListeners()
        
        // Marca o app como carregado após um pequeno delay para garantir que os dados iniciais do Room/Firebase foram processados
        viewModelScope.launch {
            delay(2000)
            isFullyLoaded = true
            Log.d(TAG, "[ANIMATION_CONTROL] App marcado como totalmente carregado. Animações permitidas.")
        }
    }

    private fun startFirebaseListeners() {
        viewModelScope.launch {
            // Aguarda um curto período para garantir que o estado de Auth foi propagado no Firebase SDK
            delay(500)
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "Usuário não logado. Sincronização e listeners abortados.")
                return@launch
            }

            try {
                Log.d(TAG, "Iniciando listeners do Firebase para o usuário: ${currentUser.uid}")
                // Primeiro, sincroniza todos os dados que podem ter mudado enquanto o app estava fechado.
                categoryRepository.syncAllDataToLocal()
                // Só depois, começa a ouvir por mudanças em tempo real.
                categoryRepository.startListeningForChanges()
            } catch (e: Exception) {
                Log.e(TAG, "Erro de permissão ou rede ao iniciar listeners do Firestore", e)
                _uiState.update { it.copy(error = AppError.SyncError) }
            }
        }
    }

    private fun listenForSyncCompletion() {
        categoryRepository.initialSyncCompleted
            .onEach {
                Log.d(TAG, "[ACHIEVEMENT_FLOW] 3. Evento de sincronização concluída recebido.")
                runInitialAchievementCheck()
            }
            .launchIn(viewModelScope)
    }

    private fun runInitialAchievementCheck() {
        viewModelScope.launch {
            Log.d(TAG, "[ACHIEVEMENT_FLOW] 4. Disparando verificação inicial de conquistas.")
            categoryRepository.checkAchievements()
        }
    }

    private fun collectData() {
        scheduleRepository.allScheduleItems.onEach { items ->
            _uiState.update { it.copy(allScheduleItems = items) }
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
            
            // Lógica de detecção de Level Up
            val newPoints = finalData?.points ?: 0
            
            // Só dispara a animação se o app já estiver totalmente carregado E houver um ganho real de pontos
            if (isFullyLoaded && UserLevel.didLevelUp(lastKnownPoints, newPoints)) {
                Log.i(TAG, "[DOPAMINE] LEVEL UP DETECTADO! De $lastKnownPoints para $newPoints")
                viewModelScope.launch {
                    _showCompletionAnimation.emit(Unit)
                    soundManager.playAchievementSound()
                }
            }
            lastKnownPoints = newPoints

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
                    scheduleRepository.deleteItemsByIds(idsToDelete)
                    _uiState.update {
                        it.copy(
                            successMessage = "${idsToDelete.size} rotinas excluídas.",
                            isSelectionModeActive = false,
                            selectedRotinaIds = emptySet()
                        )
                    }
                    WidgetUpdater.requestUpdate(application)
                    syncManager.enqueueSync() // Sincronização imediata garantida
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = AppError.UnknownError(e)) }
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
                // Só mostra o diálogo de conquista se o app estiver totalmente carregado
                if (isFullyLoaded) {
                    _uiState.update { it.copy(newlyUnlockedAchievement = achievement) }
                    soundManager.playAchievementSound() // Feedback auditivo de conquista
                }
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
        gamificationRepository.stopListening()
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                stopAllDataCollection()

                scheduleRepository.clear()
                categoryRepository.clearAllLocalData()
                templateRepository.clear()
                gamificationRepository.clear()

                googleSignInClient.signOut()
                auth.signOut()

                _logoutEvent.emit(LogoutEvent.Success)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = AppError.UnknownError(e)) }
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
            val rotina = scheduleRepository.getItemById(rotinaId)
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
            val result = saveScheduleItemUseCase(titulo, descricao, data, horarioInicio, horarioTermino, category, salvarNoGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Rotina criada com sucesso!", dialogState = DialogState.Hidden) }
                    if (salvarNoGoogle) fetchGoogleCalendarEvents()
                    syncManager.enqueueSync() // Sincronização imediata garantida
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.error) }
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
            val result = updateScheduleItemUseCase(item, novoTitulo, novaDescricao, novaData, novoHorarioInicio, novoHorarioTermino, novaCategory, sincronizarComGoogle)
            _uiState.update { it.copy(isLoading = false) }

            when(result) {
                is Result.Success -> {
                     _uiState.update { it.copy(successMessage = "Rotina atualizada com sucesso!", dialogState = DialogState.Hidden) }
                    if(sincronizarComGoogle) fetchGoogleCalendarEvents()
                     WidgetUpdater.requestUpdate(application)
                     syncManager.enqueueSync() // Sincronização imediata garantida
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.error) }
                }
            }
        }
    }

    fun deleteRotina(item: ItemCronograma) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                alarmScheduler.cancel(item)
                when (val result = deleteScheduleItemUseCase(item)) {
                    is Result.Success -> {
                        fetchGoogleCalendarEvents()
                        _uiState.value = _uiState.value.copy(successMessage = "Rotina excluída com sucesso!", dialogState = DialogState.Hidden)
                        WidgetUpdater.requestUpdate(application)
                        syncManager.enqueueSync() // Sincronização imediata garantida
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.error)
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
            soundManager.playSuccessSound() // Feedback auditivo de tarefa

            val lastAnimationDate = userPreferencesRepository.userPreferencesFlow.first().lastAnimationDate
            val today = LocalDate.now()
            val lastDate = Instant.ofEpochMilli(lastAnimationDate).atZone(ZoneId.systemDefault()).toLocalDate()

            if (lastDate != today) {
                _showCompletionAnimation.emit(Unit)
                userPreferencesRepository.updateLastAnimationDate(System.currentTimeMillis())
            }
            syncManager.enqueueSync() // Sincronização imediata garantida
        }
    }

    fun deleteRotinasByIds(ids: Set<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                scheduleRepository.deleteItemsByIds(ids)
                _uiState.update {
                    it.copy(
                        successMessage = "${ids.size} rotinas excluídas.",
                        isSelectionModeActive = false,
                        selectedRotinaIds = emptySet()
                    )
                }
                WidgetUpdater.requestUpdate(application)
                syncManager.enqueueSync() // Sincronização imediata garantida
            } catch (e: Exception) {
                _uiState.update { it.copy(error = AppError.UnknownError(e)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun unmarkHabitAsCompleted(itemCronogramaId: String) {
        viewModelScope.launch {
            unmarkHabitAsCompletedUseCase(itemCronogramaId)
            syncManager.enqueueSync() // Sincronização imediata garantida
        }
    }

    fun fetchGoogleCalendarEvents() {
        viewModelScope.launch {
            val result = categoryRepository.fetchGoogleCalendarEvents()
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(googleCalendarEvents = result.data, error = null)
                is Result.Error -> _uiState.value = _uiState.value.copy(googleCalendarEvents = emptyList(), error = result.error)
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
