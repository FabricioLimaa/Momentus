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

@Singleton
open class ScheduleRepository @Inject constructor(
    private val itemCronogramaDao: ItemCronogramaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) {
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

    val allScheduleItems: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()

    fun startListeningForChanges() {
        val currentUserId = this.userId
        Log.d(TAG, "Iniciando listener do cronograma. UID: $currentUserId")
        if (currentUserId == null || scheduleListener != null) return

        val scheduleCollection = firestore.collection("users").document(currentUserId).collection("rotinas")
        scheduleListener = scheduleCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar mudanças no cronograma.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<ItemCronograma>()?.let {
                CoroutineScope(dispatcher).launch {
                    itemCronogramaDao.insertAll(it)
                    Log.d(TAG, "${it.size} tarefas sincronizadas em tempo real.")
                    triggerWidgetUpdate()
                }
            }
        }
    }

    fun stopListeningForChanges() {
        scheduleListener?.remove()
        scheduleListener = null
    }

    suspend fun syncSchedule() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext

        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("rotinas")
            val localItems = itemCronogramaDao.getAllSync().associateBy { it.id }
            val cloudItems = collectionRef.get().await().toObjects<ItemCronograma>().associateBy { it.id }

            val itemsToUpload = localItems.filter { (id, local) ->
                val cloud = cloudItems[id]
                cloud == null || (local.lastUpdated != null && cloud.lastUpdated != null && local.lastUpdated!!.after(cloud.lastUpdated))
            }.values

            val itemsToDownload = cloudItems.filter { (id, cloud) ->
                val local = localItems[id]
                local == null || (cloud.lastUpdated != null && local.lastUpdated != null && cloud.lastUpdated!!.after(local.lastUpdated))
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { batch.set(collectionRef.document(it.id), it) }
                batch.commit().await()
            }

            if (itemsToDownload.isNotEmpty()) {
                itemCronogramaDao.insertAll(itemsToDownload.toList())
                triggerWidgetUpdate()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar cronograma.", e)
        }
    }

    fun getItemsForDay(day: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(day)
    }

    suspend fun getItemById(itemId: String): ItemCronograma? {
        return itemCronogramaDao.getItemById(itemId)
    }

    suspend fun insertItem(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(item.id).set(item)
        }
        triggerWidgetUpdate()
    }

    suspend fun insertAllItems(items: List<ItemCronograma>) {
        itemCronogramaDao.insertAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("rotinas")
            items.forEach { batch.set(collection.document(it.id), it) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }

    suspend fun updateItems(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("rotinas")
            items.forEach { batch.set(collection.document(it.id), it) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }

    suspend fun deleteScheduleItem(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            habitoConcluidoDao.delete(item.id)
            itemCronogramaDao.delete(item)
            userId?.let {
                firestore.collection("users").document(it).collection("rotinas").document(item.id).delete()
            }
            triggerWidgetUpdate()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteItemsByIds(ids: Set<String>) = withContext(dispatcher) {
        if (ids.isEmpty()) return@withContext
        habitoConcluidoDao.deleteByIds(ids)
        itemCronogramaDao.deleteByIds(ids)
        userId?.let { userId ->
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("rotinas")
            ids.forEach { batch.delete(collection.document(it)) }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }

    suspend fun deleteItemsByTemplateId(templateId: String) = withContext(dispatcher) {
        val idsToDelete = itemCronogramaDao.getIdsByTemplateId(templateId).toSet()
        if (idsToDelete.isNotEmpty()) {
            deleteItemsByIds(idsToDelete)
        }
    }

    suspend fun deleteItemsByCategoryId(categoryId: String) = withContext(dispatcher) {
        val idsToDelete = itemCronogramaDao.getIdsByCategoryId(categoryId).toSet()
        if (idsToDelete.isNotEmpty()) {
            deleteItemsByIds(idsToDelete)
        }
    }

    fun getWidgetEvents(data: LocalDate, allowedCategoryIds: Set<String>): List<WidgetEventItem> {
        if (allowedCategoryIds.isEmpty()) return emptyList()
        val startOfDayMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDayMillis = data.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)
        return itemCronogramaDao.getWidgetEventItems(startOfDayMillis, endOfDayMillis, dayOfWeekName, allowedCategoryIds)
    }

    suspend fun clear() = withContext(dispatcher) {
        habitoConcluidoDao.clear()
        itemCronogramaDao.clear()
        triggerWidgetUpdate()
    }
}
