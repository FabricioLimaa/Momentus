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
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val eventoRepository: EventoRepository,
    private val gamificationRepository: GamificationRepository,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase, // Adicionado
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

    val currentStreak: Flow<Int> = habitoConcluidoDao.getAllCompletionDates().map { calculateStreak(it) }

    private val userId: String?
        get() = auth.currentUser?.uid

    fun startListeningForChanges() {
        val userId = this.userId ?: return
        if (categoriesListener != null) return // Evita múltiplos listeners

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
                    Log.d(TAG, "${it.size} categorias sincronizadas em tempo real.")
                }
            }
        }

        templateRepository.startListeningForChanges()
        eventoRepository.startListeningForChanges()
        gamificationRepository.startListeningForChanges() // Adicionado
    }

    fun stopListeningForChanges() {
        categoriesListener?.remove()
        categoriesListener = null
        templateRepository.stopListeningForChanges()
        eventoRepository.stopListeningForChanges()
        gamificationRepository.stopListeningForChanges() // Adicionado
        _syncStatus.value = SyncStatus.OFFLINE
    }

    suspend fun syncAllDataToLocal() {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            _syncMessage.value = "Verificando dados iniciais..."
            ensureDefaultCategoryExists()
            _syncMessage.value = "Sincronizando categorias..."
            syncCategories()
            _syncMessage.value = "Sincronizando templates..."
            templateRepository.syncTemplates()
            _syncMessage.value = "Sincronizando eventos..."
            eventoRepository.syncEventos()
            _syncMessage.value = "Sincronizando hábitos concluídos..."
            syncCompletedHabits()
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a sincronização geral.", e)
            _syncMessage.value = "Falha na sincronização."
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private suspend fun syncCompletedHabits() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        Log.d(TAG, "[SYNC] Iniciando sincronização de hábitos concluídos.")
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            val cloudHabits = collectionRef.get().await().toObjects<HabitoConcluido>()
            
            if (cloudHabits.isNotEmpty()) {
                habitoConcluidoDao.clear() 
                cloudHabits.forEach { habitoConcluidoDao.insert(it) }
                Log.d(TAG, "[SYNC] ${cloudHabits.size} hábitos concluídos foram baixados da nuvem para o Room.")
            } else {
                Log.d(TAG, "[SYNC] Nenhum hábito concluído encontrado na nuvem para sincronizar.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Erro ao sincronizar hábitos concluídos.", e)
            throw e
        }
    }

    private suspend fun ensureDefaultCategoryExists() {
        val defaultCategory = Category(
            id = "default-outros",
            nome = "Outros",
            cor = "#808080"
        )

        val existing = categoryDao.getById(defaultCategory.id)
        if (existing == null) {
            insertCategory(defaultCategory)
        }
    }

    private suspend fun syncCategories() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("categories")
            val localCategoriesMap = categoryDao.getAllSync().associateBy { it.id }
            val cloudCategoriesMap = collectionRef.get().await().toObjects<Category>().associateBy { it.id }

            val itemsToUpload = localCategoriesMap.filter { (id, local) ->
                val cloudItem = cloudCategoriesMap[id]
                when {
                    cloudItem == null -> true
                    local.lastUpdated == null -> false
                    cloudItem.lastUpdated == null -> true
                    else -> local.lastUpdated.after(cloudItem.lastUpdated)
                }
            }.values

            val itemsToDownload = cloudCategoriesMap.filter { (id, cloud) ->
                val localItem = localCategoriesMap[id]
                when {
                    localItem == null -> true
                    cloud.lastUpdated == null -> false
                    localItem.lastUpdated == null -> true
                    else -> cloud.lastUpdated.after(localItem.lastUpdated)
                }
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { batch.set(collectionRef.document(it.id), it) }
                batch.commit().await()
                Log.d(TAG, "${itemsToUpload.size} categorias locais enviadas para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                categoryDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "${itemsToDownload.size} categorias da nuvem sincronizadas para o banco local.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar categorias.", e)
            throw e
        }
    }

    suspend fun clearAllLocalData() = withContext(dispatcher) {
        Log.w(TAG, "Limpando todos os dados locais do banco de dados.")
        categoryDao.clear()
        templateRepository.clear()
        eventoRepository.clear()
        metaDao.clear()
        habitoConcluidoDao.clear()
    }

    fun getAllCategoriesSync(): List<Category> {
        return categoryDao.getAllSync()
    }

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAll()
    }

    fun getAllCompletionDates(): Flow<List<Long>> {
        return habitoConcluidoDao.getAllCompletionDates()
    }

    fun getStatsSummary(since: Long): Flow<List<StatsSummary>> {
        return habitoConcluidoDao.getConcluidosCountByCategory(since)
    }

    fun getSchedulableEventsForCategory(categoryId: String, since: Long): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getSchedulableEventsForCategory(categoryId, since)
    }

    open suspend fun insertCategory(category: Category) = withContext(dispatcher) {
        val currentUserId = userId ?: run {
            Log.w(TAG, "Tentativa de inserir categoria sem usuário logado.")
            return@withContext
        }

        val categoriesCollection = firestore.collection("users").document(currentUserId).collection("categories")

        Log.d(TAG, "Inserindo/Atualizando categoria: ID=${category.id}, Nome=${category.nome}")
        categoryDao.insert(category)
        categoriesCollection.document(category.id).set(category)
            .addOnSuccessListener { Log.d(TAG, "Categoria ${category.id} salva com sucesso no Firestore.") }
            .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar categoria ${category.id} no Firestore.", e) }
    }

    open suspend fun deleteCategory(category: Category) {
        Log.d(TAG, "Deletando categoria: ID=${category.id}, Nome=${category.nome}")
        categoryDao.delete(category)
        userId?.let {
            firestore.collection("users").document(it).collection("categories").document(category.id)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "Categoria ${category.id} deletada com sucesso do Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao deletar categoria ${category.id} do Firestore.", e) }
        }
    }

    fun getMetaForCategory(categoryId: String): Flow<Meta?> {
        return metaDao.getMetaForCategory(categoryId)
    }

    suspend fun saveMeta(meta: Meta) {
        metaDao.insertOrUpdate(meta)
    }

    suspend fun markHabitAsCompleted(itemCronogramaId: String) = withContext(dispatcher) {
        Log.d(TAG, "[SYNC] Iniciando marcação de hábito como concluído para o id: $itemCronogramaId")
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        
        habitoConcluidoDao.insert(habito)
        Log.d(TAG, "[LOCAL] Hábito $itemCronogramaId salvo no Room.")

        // Aciona a verificação de conquistas com os dados mais recentes
        checkAchievements()

        val currentUserId = userId
        if (currentUserId == null) {
            Log.w(TAG, "[FIREBASE] Usuário não logado. A conclusão do hábito $itemCronogramaId não será salva na nuvem.")
            return@withContext
        }

        firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            .document(itemCronogramaId)
            .set(habito)
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Hábito $itemCronogramaId salvo com sucesso na coleção correta: $COMPLETED_HABITS_COLLECTION") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao salvar hábito $itemCronogramaId no Firestore.", e) }
    }

    private suspend fun checkAchievements() {
        Log.d(TAG, "[HABIT] Acionando verificação de conquistas...")
        // Busca síncrona dos dados mais recentes para evitar race conditions
        val completionDatesMillis = habitoConcluidoDao.getAllCompletionDatesSync()
        val totalCompleted = completionDatesMillis.size
        val streakCount = calculateStreak(completionDatesMillis)
        Log.d(TAG, "[HABIT] Dados para verificação: totalCompleted=$totalCompleted, streakCount=$streakCount")
        
        checkAndUnlockAchievementsUseCase(streakCount, totalCompleted)
    }

    private fun calculateStreak(completionDatesMillis: List<Long>): Int {
        if (completionDatesMillis.isEmpty()) return 0

        val completionDates = completionDatesMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()

        if (completionDates.isEmpty()) return 0

        var currentStreak = 0
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // A sequência só pode começar hoje ou ontem.
        if (completionDates.first() != today && completionDates.first() != yesterday) return 0

        currentStreak = 1
        var lastDate = completionDates.first()

        for (i in 1 until completionDates.size) {
            val currentDate = completionDates[i]
            if (lastDate.minusDays(1) == currentDate) {
                currentStreak++
                lastDate = currentDate
            } else {
                break
            }
        }

        return currentStreak
    }

    suspend fun unmarkHabitAsCompleted(itemCronogramaId: String) = withContext(dispatcher) {
        Log.d(TAG, "[SYNC] Iniciando desmarcação de hábito como concluído para o id: $itemCronogramaId")

        habitoConcluidoDao.delete(itemCronogramaId)
        Log.d(TAG, "[LOCAL] Hábito $itemCronogramaId removido do Room.")

        val currentUserId = userId
        if (currentUserId == null) {
            Log.w(TAG, "[FIREBASE] Usuário não logado. A conclusão do hábito $itemCronogramaId não será removida da nuvem.")
            return@withContext
        }

        firestore.collection("users").document(currentUserId).collection(COMPLETED_HABITS_COLLECTION)
            .document(itemCronogramaId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Hábito $itemCronogramaId removido com sucesso da coleção correta: $COMPLETED_HABITS_COLLECTION") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao remover hábito $itemCronogramaId do Firestore.", e) }
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
                eventoRepository.insertItemCronograma(item.copy(googleCalendarEventId = result.data))
            }
            result
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao atualizar evento.", e))
        }
    }

    suspend fun deleteCompleteEvent(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            item.googleCalendarEventId?.let { googleCalendarSource.deleteEvent(it) }
            eventoRepository.excluirEventoCompleto(item)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir evento.", e))
        }
    }

    suspend fun fetchGoogleCalendarEvents(): Result<List<GoogleCalendarEvent>> {
        return googleCalendarSource.fetchEvents()
    }
}
