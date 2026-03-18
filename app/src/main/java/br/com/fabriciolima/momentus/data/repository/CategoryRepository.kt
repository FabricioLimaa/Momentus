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
import br.com.fabriciolima.momentus.domain.usecase.CheckAndUnlockAchievementsUseCase
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    private val rotinaRepository: RotinaRepository,
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
    private val googleCalendarSource: GoogleCalendarSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var categoriesListener: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow("Preparando...")
    val syncMessage = _syncMessage.asStateFlow()

    open val allCategoriesWithMetas: Flow<List<CategoryWithMeta>> = categoryDao.getCategoriesWithMetas()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = categoryDao.getStats()

    val currentStreak: Flow<Int> = habitoConcluidoDao.getAllCompletionDates()
        .map { calculateStreak(it) }
        .onEach { streak ->
            CoroutineScope(dispatcher).launch {
                userRepository.updateStreak(streak)
            }
        }

    private val userId: String?
        get() = auth.currentUser?.uid

    fun startListeningForChanges() {
        val userId = this.userId ?: return
        if (categoriesListener != null) return

        _syncStatus.value = SyncStatus.SYNCING

        val categoriesCollection = firestore.collection("users").document(userId).collection("categories")
        categoriesListener = categoriesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nas categorias.", e)
                _syncStatus.value = SyncStatus.OFFLINE
                return@addSnapshotListener
            }
            _syncStatus.value = SyncStatus.CONNECTED
            snapshots?.toObjects<Category>()?.let {
                CoroutineScope(dispatcher).launch {
                    categoryDao.insertAll(it)
                }
            }
        }

        templateRepository.startListeningForChanges()
        rotinaRepository.startListeningForChanges()
    }

    fun stopListeningForChanges() {
        categoriesListener?.remove()
        categoriesListener = null
        templateRepository.stopListeningForChanges()
        rotinaRepository.stopListeningForChanges()
        _syncStatus.value = SyncStatus.OFFLINE
    }

    suspend fun syncAllDataToLocal() {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            _syncMessage.value = "Sincronizando rotinas..."
            rotinaRepository.syncRotinas()
            _syncMessage.value = "Sincronizando categorias..."
            syncCategories()
            _syncMessage.value = "Sincronizando templates..."
            templateRepository.syncTemplates()
            _syncMessage.value = "Sincronização concluída!"
            _syncStatus.value = SyncStatus.CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a sincronização.", e)
            _syncMessage.value = "Falha na sincronização."
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private suspend fun syncCategories() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("categories")
            val cloudCategories = collectionRef.get().await().toObjects<Category>()
            categoryDao.insertAll(cloudCategories)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar categorias.", e)
        }
    }

    suspend fun clearAllLocalData() = withContext(dispatcher) {
        categoryDao.clear()
        templateRepository.clear()
        rotinaRepository.clear()
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
        firestore.collection("users").document(currentUserId).collection("categories").document(category.id).set(category)
    }

    open suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
        userId?.let {
            firestore.collection("users").document(it).collection("categories").document(category.id).delete()
        }
    }

    suspend fun markHabitAsCompleted(itemCronogramaId: String) = withContext(dispatcher) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao.insert(habito)
        checkAchievements()

        val currentUserId = userId ?: return@withContext
        firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            .document(itemCronogramaId)
            .set(habito)
    }

    private suspend fun checkAchievements() {
        val completionDatesMillis = habitoConcluidoDao.getAllCompletionDatesSync()
        val totalCompleted = completionDatesMillis.size
        val streakCount = calculateStreak(completionDatesMillis)
        checkAndUnlockAchievementsUseCase(streakCount, totalCompleted)
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
        firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            .document(itemCronogramaId).delete()
    }

    suspend fun saveMeta(meta: Meta) {
        metaDao.insertOrUpdate(meta)
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
                rotinaRepository.insertItemCronograma(item.copy(googleCalendarEventId = result.data))
            }
            result
        } catch (e: Exception) { Result.Error(e) }
    }

    suspend fun deleteCompleteEvent(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            item.googleCalendarEventId?.let { googleCalendarSource.deleteEvent(it) }
            rotinaRepository.excluirRotinaCompleta(item)
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e) }
    }

    suspend fun fetchGoogleCalendarEvents(): Result<List<GoogleCalendarEvent>> = googleCalendarSource.fetchEvents()
    fun getStatsSummary(since: Long): Flow<List<StatsSummary>> = habitoConcluidoDao.getConcluidosCountByCategory(since)
    fun getSchedulableEventsForCategory(id: String, s: Long): Flow<List<ItemCronograma>> = itemCronogramaDao.getSchedulableEventsForCategory(id, s)
    fun getAllCompletionDates(): Flow<List<Long>> = habitoConcluidoDao.getAllCompletionDates()
}
