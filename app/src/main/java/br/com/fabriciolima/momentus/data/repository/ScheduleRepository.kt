package br.com.fabriciolima.momentus.data.repository

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdateWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScheduleRepository"
private const val EVENTS_COLLECTION = "eventos"

@Singleton
open class ScheduleRepository @Inject constructor(
    private val itemCronogramaDao: ItemCronogramaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : IScheduleRepository { // Implementação da interface unificadora

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var scheduleListener: ListenerRegistration? = null
    private val workManager = WorkManager.getInstance(context)

    private fun triggerWidgetUpdate() {
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        workManager.enqueueUniqueWork(
            WidgetUpdateWorker.WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private val userId: String?
        get() = auth.currentUser?.uid

    override val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()
    override val allScheduleItems: Flow<List<ItemCronograma>> = todosOsItensDoCronograma

    override fun startListeningForChanges() {
        val currentUserId = this.userId
        if (currentUserId == null || scheduleListener != null) return

        try {
            val scheduleCollection = firestore.collection("users").document(currentUserId).collection(EVENTS_COLLECTION)
            scheduleListener = scheduleCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Erro no SnapshotListener de eventos: ${e.code}", e)
                    return@addSnapshotListener
                }
                snapshots?.toObjects<ItemCronograma>()?.let { cloudItems ->
                    CoroutineScope(dispatcher).launch {
                        // Filtra itens para não sobrescrever o isDeleted local com false vindo da nuvem se o local for mais novo
                        val localItems = itemCronogramaDao.getAllSyncIncludingDeleted().associateBy { it.id }
                        val filteredItems = cloudItems.filter { cloud ->
                            val local = localItems[cloud.id]
                            local == null || local.lastUpdated == null || cloud.lastUpdated == null || cloud.lastUpdated!!.after(local.lastUpdated)
                        }
                        
                        if (filteredItems.isNotEmpty()) {
                            itemCronogramaDao.insertAll(filteredItems)
                            triggerWidgetUpdate()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha crítica ao registrar listener de eventos", e)
        }
    }

    override fun stopListeningForChanges() {
        scheduleListener?.remove()
        scheduleListener = null
    }

    override suspend fun syncSchedule(): Result<Unit> = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext Result.Error(AppError.AuthRequiredError)
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection(EVENTS_COLLECTION)
            
            // 1. Coleta itens locais (incluindo marcados como deletados para sincronizar a remoção)
            val localItems = itemCronogramaDao.getAllSyncIncludingDeleted().associateBy { it.id }
            
            // 2. Coleta itens da nuvem
            val cloudItems = collectionRef.get().await().toObjects<ItemCronograma>().associateBy { it.id }

            // 3. Itens para Upload: Novos, mais recentes ou recém-deletados localmente
            val itemsToUpload = localItems.filter { (id, local) ->
                val cloud = cloudItems[id]
                cloud == null || (local.lastUpdated != null && cloud.lastUpdated != null && local.lastUpdated!!.after(cloud.lastUpdated))
            }.values

            // 4. Itens para Download: Novos ou mais recentes da nuvem
            val itemsToDownload = cloudItems.filter { (id, cloud) ->
                val local = localItems[id]
                local == null || (cloud.lastUpdated != null && local.lastUpdated != null && cloud.lastUpdated!!.after(local.lastUpdated))
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { item ->
                    if (item.isDeleted) {
                        // Se está deletado localmente, remove da nuvem
                        batch.delete(collectionRef.document(item.id))
                    } else {
                        // Senão, atualiza/insere
                        batch.set(collectionRef.document(item.id), item)
                    }
                }
                batch.commit().await()
                Log.d(TAG, "[SYNC] Upload de ${itemsToUpload.size} itens concluído.")
            }

            if (itemsToDownload.isNotEmpty()) {
                // Filtrar apenas o que não está deletado na nuvem (se houver essa marcação lá)
                val activeDownloads = itemsToDownload.filter { !it.isDeleted }
                itemCronogramaDao.insertAll(activeDownloads)
                triggerWidgetUpdate()
                Log.d(TAG, "[SYNC] Download de ${activeDownloads.size} itens concluído.")
            }
            
            // 5. Limpeza local: Após o upload/delete bem sucedido, removemos do banco local
            itemCronogramaDao.permanentlyDeleteMarkedItems()

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Erro ao sincronizar cronograma", e)
            Result.Error(AppError.SyncError)
        }
    }

    override suspend fun syncEventos() = syncSchedule()

    override fun getItemsForDay(day: String): Flow<List<ItemCronograma>> = itemCronogramaDao.getItemsByDayOfWeek(day)
    override fun getItensDoDia(dia: String) = getItemsForDay(dia)

    override suspend fun getItemById(itemId: String): ItemCronograma? = itemCronogramaDao.getItemById(itemId)
    override suspend fun getItemCronograma(itemId: String) = getItemById(itemId)

    override suspend fun insertItem(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
        userId?.let { firestore.collection("users").document(it).collection(EVENTS_COLLECTION).document(item.id).set(item) }
        triggerWidgetUpdate()
    }
    override suspend fun insertItemCronograma(item: ItemCronograma) = insertItem(item)

    override suspend fun insertAllItems(items: List<ItemCronograma>) {
        itemCronogramaDao.insertAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection(EVENTS_COLLECTION)
            items.forEach { batch.set(collection.document(it.id), it) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }
    override suspend fun insertAll(items: List<ItemCronograma>) = insertAllItems(items)

    override suspend fun updateItems(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection(EVENTS_COLLECTION)
            items.forEach { batch.set(collection.document(it.id), it) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }
    override suspend fun updateItensCronograma(items: List<ItemCronograma>) = updateItems(items)

    override suspend fun deleteScheduleItem(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            habitoConcluidoDao.delete(item.id)
            
            // Soft Delete: Marca como deletado e atualiza o timestamp
            val deletedItem = item.copy(isDeleted = true, lastUpdated = java.util.Date())
            itemCronogramaDao.insert(deletedItem)
            
            // Tentativa de delete na nuvem (sem bloquear se falhar por rede)
            userId?.let { uid ->
                firestore.collection("users").document(uid)
                    .collection(EVENTS_COLLECTION).document(item.id).delete()
                    .addOnFailureListener { Log.w(TAG, "Falha ao deletar da nuvem, será sincronizado depois.") }
            }

            triggerWidgetUpdate()
            Result.Success(Unit)
        } catch (e: Exception) { 
            Log.e(TAG, "Erro ao deletar item", e)
            Result.Error(AppError.UnknownError(e)) 
        }
    }
    override suspend fun excluirEventoCompleto(item: ItemCronograma) = deleteScheduleItem(item)

    override suspend fun deleteItemsByIds(ids: Set<String>) = withContext(dispatcher) {
        if (ids.isEmpty()) return@withContext
        habitoConcluidoDao.deleteByIds(ids)
        
        val now = java.util.Date()
        val itemsToDelete = itemCronogramaDao.getItemsByIdsIncludingDeleted(ids.toList()).map {
            it.copy(isDeleted = true, lastUpdated = now)
        }
        itemCronogramaDao.insertAll(itemsToDelete)

        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection(EVENTS_COLLECTION)
            ids.forEach { batch.delete(collection.document(it)) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }
    override suspend fun deleteEventsByIds(ids: Set<String>) = deleteItemsByIds(ids)

    override suspend fun deleteItemsByTemplateId(templateId: String) = withContext(dispatcher) {
        val idsToDelete = itemCronogramaDao.getIdsByTemplateId(templateId).toSet()
        if (idsToDelete.isNotEmpty()) deleteItemsByIds(idsToDelete)
    }
    override suspend fun deleteEventsByTemplateId(templateId: String) = deleteItemsByTemplateId(templateId)

    override suspend fun deleteItemsByCategoryId(categoryId: String) = withContext(dispatcher) {
        val idsToDelete = itemCronogramaDao.getIdsByCategoryId(categoryId).toSet()
        if (idsToDelete.isNotEmpty()) deleteItemsByIds(idsToDelete)
    }
    override suspend fun deleteEventsByCategoryId(categoryId: String) = deleteItemsByCategoryId(categoryId)

    override fun getWidgetEvents(data: LocalDate, allowedCategoryIds: Set<String>): List<WidgetEventItem> {
        if (allowedCategoryIds.isEmpty()) return emptyList()
        val startOfDayMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDayMillis = data.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)
        return itemCronogramaDao.getWidgetEventItems(startOfDayMillis, endOfDayMillis, dayOfWeekName, allowedCategoryIds)
    }

    override suspend fun clear() = withContext(dispatcher) {
        habitoConcluidoDao.clear()
        itemCronogramaDao.clear()
        triggerWidgetUpdate()
    }
}
