package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.CategoryDao
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.StatsSummary
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.data.model.CategoryWithMeta
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.StatsResult
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.domain.usecase.CheckAndUnlockAchievementsUseCase
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CategoryRepository"
private const val COMPLETED_HABITS_COLLECTION = "completed_habits"

enum class SyncStatus { OFFLINE, SYNCING, CONNECTED }

@Singleton
open class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val metaDao: MetaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    private val itemCronogramaDao: ItemCronogramaDao,
    private val templateRepository: TemplateRepository,
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
    private val googleCalendarSource: GoogleCalendarSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private val repositoryScope = CoroutineScope(dispatcher + SupervisorJob())

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var categoriesListener: ListenerRegistration? = null

    init {
        observeAuthChanges()
    }

    private fun observeAuthChanges() {
        repositoryScope.launch {
            userRepository.authStateFlow.collect { userId ->
                if (userId != null) {
                    Log.d(TAG, "[AUTH_AUTO_SYNC] Usuário detectado: $userId. Aguardando estabilização do token...")
                    // Pequeno delay para garantir que o token de autenticação e App Check foram propagados para o Firestore
                    delay(2000)
                    Log.d(TAG, "[AUTH_AUTO_SYNC] Iniciando sincronização.")
                    syncAllDataToLocal()
                    startListeningForChanges()
                } else {
                    Log.d(TAG, "[AUTH_AUTO_SYNC] Usuário deslogado. Parando listeners.")
                    stopListeningForChanges()
                }
            }
        }
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow("Preparando...")
    val syncMessage = _syncMessage.asStateFlow()

    private val _initialSyncCompleted = MutableSharedFlow<Unit>()
    val initialSyncCompleted = _initialSyncCompleted.asSharedFlow()

    open val allCategoriesWithMetas: Flow<List<CategoryWithMeta>> = categoryDao.getCategoriesWithMetas()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = categoryDao.getStats()

    val currentStreak: Flow<Int> = habitoConcluidoDao.getAllCompletionDates()
        .map { calculateStreak(it) }
        .onEach { streak ->
            CoroutineScope(dispatcher + SupervisorJob()).launch {
                try {
                    userRepository.updateStreak(streak)
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao atualizar streak no usuário", e)
                }
            }
        }

    private val userId: String?
        get() = auth.currentUser?.uid

    suspend fun getStreakCalculationDetails(): String = withContext(dispatcher) {
        val completedHabits = habitoConcluidoDao.getAllSync()
        if (completedHabits.isEmpty()) {
            return@withContext "Nenhum hábito concluído encontrado no banco de dados local."
        }

        val details = StringBuilder("--- Detalhes do Cálculo de Streak ---\n")
        details.append("Total de Conclusões Encontradas: ${completedHabits.size}\n\n")

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        completedHabits.forEach { habit ->
            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(habit.dataConclusao), ZoneId.systemDefault())
            details.append("- ID da Rotina: ${habit.itemCronogramaId}\n")
            details.append("  Data de Conclusão: ${dateTime.format(formatter)}\n")
        }

        val timestamps = completedHabits.map { it.dataConclusao }
        val streakResult = calculateStreak(timestamps)

        details.append("\nResultado do Cálculo de Streak: $streakResult")
        details.append("\n--- Fim dos Detalhes ---")

        return@withContext details.toString()
    }

    suspend fun getCompletedRotinasForDate(date: LocalDate): List<ItemCronograma> = withContext(dispatcher) {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val completedIds = habitoConcluidoDao.getCompletedHabitIdsForDateRange(startOfDay, endOfDay)
        if (completedIds.isEmpty()) {
            return@withContext emptyList()
        }
        return@withContext itemCronogramaDao.getItemsByIds(completedIds)
    }

    fun startListeningForChanges() {
        val userId = this.userId ?: return
        if (categoriesListener != null) {
            return
        }
        Log.d(TAG, "[SYNC_DIAGNOSTIC] startListeningForChanges: Alterando status para SYNCING")
        _syncStatus.value = SyncStatus.SYNCING

        try {
            val categoriesCollection = firestore.collection("users").document(userId).collection("categories")
            categoriesListener = categoriesCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Erro ao escutar por mudanças nas categorias: ${e.message}")
                    _syncStatus.value = SyncStatus.OFFLINE
                    return@addSnapshotListener
                }
                Log.d(TAG, "[SYNC_DIAGNOSTIC] Listener de categorias ativo. Alterando status para CONNECTED")
                _syncStatus.value = SyncStatus.CONNECTED
                if (snapshots == null) return@addSnapshotListener

                try {
                    val validCategories = mutableListOf<Category>()
                    for (doc in snapshots.documents) {
                        try {
                            val cat = doc.toObject<Category>()
                            if (cat != null) {
                                validCategories.add(cat)
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Erro ao deserializar Category ${doc.id}", ex)
                        }
                    }

                    if (validCategories.isNotEmpty()) {
                        CoroutineScope(dispatcher + SupervisorJob()).launch {
                            try {
                                categoryDao.insertAll(validCategories)
                            } catch (t: Throwable) {
                                Log.e(TAG, "Erro ao salvar categorias no Room", t)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Erro inesperado ao processar snapshot de categorias", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Falha crítica ao registrar listener de categorias", t)
        }

        templateRepository.startListeningForChanges()
        scheduleRepository.startListeningForChanges()
    }

    fun stopListeningForChanges() {
        categoriesListener?.remove()
        categoriesListener = null
        templateRepository.stopListeningForChanges()
        scheduleRepository.stopListeningForChanges()
        _syncStatus.value = SyncStatus.OFFLINE
    }

    suspend fun syncAllDataToLocal(): Result<Unit> = withContext(dispatcher) {
        Log.d(TAG, "[SYNC_DIAGNOSTIC] Tentando executar syncAllDataToLocal. Status atual: ${_syncStatus.value}")
        if (_syncStatus.value != SyncStatus.OFFLINE) {
            Log.d(TAG, "[SYNC_DIAGNOSTIC] Sincronização já está em andamento ou conectada. Ignorando nova chamada.")
            return@withContext Result.Success(Unit)
        }
        _syncStatus.value = SyncStatus.SYNCING
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 1. Iniciando sincronização geral de dados.")
        try {
            _syncMessage.value = "Sincronizando conquistas..."
            gamificationRepository.syncUnlockedAchievements()
            _syncMessage.value = "Sincronizando hábitos concluídos..."
            syncCompletedHabits()
            _syncMessage.value = "Sincronizando cronograma..."
            val scheduleSyncResult = scheduleRepository.syncSchedule()
            if (scheduleSyncResult is Result.Error) {
                Log.e(TAG, "Falha na sincronização do cronograma.")
            }
            _syncMessage.value = "Sincronizando categorias..."
            syncCategories()
            _syncMessage.value = "Sincronizando metas..."
            syncMetas()
            _syncMessage.value = "Sincronizando templates..."
            templateRepository.syncTemplates()
            _syncMessage.value = "Sincronização concluída!"
            _syncStatus.value = SyncStatus.CONNECTED
            Log.d(TAG, "[ACHIEVEMENT_FLOW] 2. Sincronização concluída com sucesso. Notificando observadores.")
            _initialSyncCompleted.emit(Unit)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a sincronização dos dados.", e)
            _syncStatus.value = SyncStatus.OFFLINE
            Result.Error(AppError.SyncError)
        }
    }

    /**
     * Sincronização bidirecional de categorias entre Room e Firestore com estratégia Last-Write-Wins.
     */
    suspend fun syncCategories() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("categories")
            val localCategories = categoryDao.getAllSync().associateBy { it.id }
            
            val snapshot = collectionRef.get().await()
            val cloudCategories = mutableMapOf<String, Category>()
            for (doc in snapshot.documents) {
                try {
                    val cat = doc.toObject<Category>()
                    if (cat != null) cloudCategories[cat.id] = cat
                } catch (ex: Exception) {
                    Log.e(TAG, "Erro ao deserializar Category ${doc.id} no syncCategories", ex)
                }
            }

            // 1. Enviar categorias locais novas ou mais recentes
            val itemsToUpload = localCategories.filter { (id, local) ->
                val cloud = cloudCategories[id]
                cloud == null || (local.lastUpdated != null && cloud.lastUpdated != null && local.lastUpdated!!.after(cloud.lastUpdated))
            }.values

            // 2. Baixar categorias da nuvem novas ou mais recentes
            val itemsToDownload = cloudCategories.filter { (id, cloud) ->
                val local = localCategories[id]
                local == null || (cloud.lastUpdated != null && local.lastUpdated != null && cloud.lastUpdated!!.after(local.lastUpdated))
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { batch.set(collectionRef.document(it.id), it) }
                batch.commit().await()
                Log.d(TAG, "Sync: Upload de ${itemsToUpload.size} categorias concluído.")
            }

            if (itemsToDownload.isNotEmpty()) {
                categoryDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "Sync: Download de ${itemsToDownload.size} categorias concluído.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar categorias.", e)
        }
    }

    /**
     * Sincronização de metas.
     */
    suspend fun syncMetas() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("metas")
            val snapshot = collectionRef.get().await()
            val cloudMetas = mutableListOf<Meta>()
            for (doc in snapshot.documents) {
                try {
                    val meta = doc.toObject<Meta>()
                    if (meta != null) cloudMetas.add(meta)
                } catch (ex: Exception) {
                    Log.e(TAG, "Erro ao deserializar Meta ${doc.id} no syncMetas", ex)
                }
            }
            cloudMetas.forEach {
                try {
                    metaDao.insertOrUpdate(it)
                } catch (ex: Exception) {
                    Log.e(TAG, "Erro ao salvar meta no Room para categoryId ${it.categoryId}", ex)
                }
            }
            Log.d(TAG, "Sync: Download de ${cloudMetas.size} metas concluído.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar metas.", e)
        }
    }

    suspend fun syncCompletedHabits() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            val snapshot = collectionRef.get().await()
            val cloudHabits = mutableListOf<HabitoConcluido>()
            for (doc in snapshot.documents) {
                try {
                    val habit = doc.toObject<HabitoConcluido>()
                    if (habit != null) cloudHabits.add(habit)
                } catch (ex: Exception) {
                    Log.e(TAG, "Erro ao deserializar HabitoConcluido ${doc.id}", ex)
                }
            }
            if (cloudHabits.isNotEmpty()) {
                cloudHabits.forEach {
                    try {
                        habitoConcluidoDao.insert(it)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Erro ao inserir habitoConcluido no Room: ${it.itemCronogramaId}", ex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar hábitos concluídos.", e)
        }
    }

    suspend fun clearAllLocalData() = withContext(dispatcher) {
        categoryDao.clear()
        templateRepository.clear()
        scheduleRepository.clear()
        metaDao.clear()
        habitoConcluidoDao.clear()
    }

    fun getAllCategoriesSync(): List<Category> {
        return categoryDao.getAllSync()
    }

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAll()
    }

    open suspend fun insertCategory(category: Category) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        categoryDao.insert(category)
        try {
            firestore.collection("users").document(currentUserId)
                .collection("categories").document(category.id)
                .set(category).await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar categoria no Firestore", e)
        }
    }

    open suspend fun deleteCategory(category: Category) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        categoryDao.delete(category)
        try {
            firestore.collection("users").document(currentUserId)
                .collection("categories").document(category.id)
                .delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir categoria do Firestore", e)
        }
    }

    suspend fun markHabitAsCompleted(itemCronogramaId: String) = withContext(dispatcher) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao.insert(habito)
        checkAchievements()

        val currentUserId = userId ?: return@withContext
        try {
            firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
                .document(itemCronogramaId)
                .set(habito).await()
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao salvar hábito concluído no Firestore", e)
        }
    }

    suspend fun checkAchievements() = withContext(dispatcher) {
        val completionDatesMillis = habitoConcluidoDao.getAllCompletionDatesSync()
        val distinctCompletionDates = completionDatesMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()

        val totalCompleted = distinctCompletionDates.size
        val streakCount = calculateStreak(completionDatesMillis)
        val totalTemplates = templateRepository.getTemplatesCount()
        checkAndUnlockAchievementsUseCase(streakCount, totalCompleted, totalTemplates)
    }

    private fun calculateStreak(completionDatesMillis: List<Long>): Int {
        if (completionDatesMillis.isEmpty()) return 0
        val completionDates = completionDatesMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct().sorted()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        if (completionDates.last() != today && completionDates.last() != yesterday) return 0
        var currentStreak = 0
        var expectedDate: LocalDate? = null
        for (date in completionDates.reversed()) {
            if (expectedDate == null) { currentStreak = 1; expectedDate = date.minusDays(1) }
            else if (date == expectedDate) { currentStreak++; expectedDate = date.minusDays(1) }
            else break
        }
        return currentStreak
    }

    suspend fun unmarkHabitAsCompleted(itemCronogramaId: String) = withContext(dispatcher) {
        habitoConcluidoDao.delete(itemCronogramaId)
        val currentUserId = userId ?: return@withContext
        try {
            firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
                .document(itemCronogramaId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao excluir hábito concluído no Firestore", e)
        }
    }

    suspend fun saveMeta(meta: Meta) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        metaDao.insertOrUpdate(meta)
        // Sincroniza com o Firestore também
        try {
            firestore.collection("users").document(currentUserId)
                .collection("metas").document(meta.categoryId).set(meta).await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar meta no Firestore", e)
        }
    }

    suspend fun deleteMeta(categoryId: String) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        metaDao.deleteMetaForCategory(categoryId)
        // Remove do Firestore também
        try {
            firestore.collection("users").document(currentUserId)
                .collection("metas").document(categoryId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir meta do Firestore", e)
        }
    }

    suspend fun saveEventToGoogle(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        cor: String?
    ): Result<String?> = googleCalendarSource.saveEvent(titulo, descricao, data, horarioInicio, horarioTermino, cor)

    suspend fun updateCompleteEvent(item: ItemCronograma, cor: String?): Result<String?> = withContext(dispatcher) {
        try {
            val result = googleCalendarSource.updateEvent(item, cor)
            if (result is Result.Success) {
                scheduleRepository.insertItem(item.copy(googleCalendarEventId = result.data))
            }
            result
        } catch (e: Exception) { Result.Error(AppError.UnknownError(e)) }
    }

    suspend fun deleteCompleteEvent(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            item.googleCalendarEventId?.let { googleCalendarSource.deleteEvent(it) }
            scheduleRepository.deleteScheduleItem(item)
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(AppError.UnknownError(e)) }
    }

    suspend fun fetchGoogleCalendarEvents(): Result<List<GoogleCalendarEvent>> = googleCalendarSource.fetchEvents()
    fun getStatsSummary(since: Long): Flow<List<StatsSummary>> = habitoConcluidoDao.getConcluidosCountByCategory(since)
    fun getSchedulableEventsForCategory(id: String, s: Long): Flow<List<ItemCronograma>> = itemCronogramaDao.getSchedulableEventsForCategory(id, s)
    fun getAllCompletionDates(): Flow<List<Long>> = habitoConcluidoDao.getAllCompletionDates()
}
