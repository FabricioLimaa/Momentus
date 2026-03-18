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

private const val TAG = "RotinaRepository"

@Singleton
open class RotinaRepository @Inject constructor(
    private val itemCronogramaDao: ItemCronogramaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var rotinasListener: ListenerRegistration? = null
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

    val todosOsItensDaRotina: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()

    fun startListeningForChanges() {
        val currentUserId = this.userId
        Log.d(TAG, "Tentando iniciar listener. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, listener não iniciado.")
            return
        }
        if (rotinasListener != null) return

        val rotinasCollection = firestore.collection("users").document(currentUserId).collection("rotinas")
        rotinasListener = rotinasCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nas rotinas.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<ItemCronograma>()?.let {
                CoroutineScope(dispatcher).launch {
                    itemCronogramaDao.insertAll(it)
                    Log.d(TAG, "${it.size} rotinas sincronizadas em tempo real.")
                    triggerWidgetUpdate()
                }
            }
        }
    }

    fun stopListeningForChanges() {
        rotinasListener?.remove()
        rotinasListener = null
    }

    suspend fun syncRotinas() = withContext(dispatcher) {
        val currentUserId = userId
        Log.d(TAG, "Tentando sincronizar rotinas. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, sincronização não realizada.")
            return@withContext
        }

        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("rotinas")
            val localRotinasMap = itemCronogramaDao.getAllSync().associateBy { it.id }
            val cloudRotinasMap = collectionRef.get().await().toObjects<ItemCronograma>().associateBy { it.id }

            val itemsToUpload = localRotinasMap.filter { (id, local) ->
                val cloudItem = cloudRotinasMap[id]
                when {
                    cloudItem == null -> true
                    local.lastUpdated == null -> false
                    cloudItem.lastUpdated == null -> true
                    else -> local.lastUpdated!!.after(cloudItem.lastUpdated)
                }
            }.values

            val itemsToDownload = cloudRotinasMap.filter { (id, cloud) ->
                val localItem = localRotinasMap[id]
                when {
                    localItem == null -> true
                    cloud.lastUpdated == null -> false
                    localItem.lastUpdated == null -> true
                    else -> cloud.lastUpdated!!.after(localItem.lastUpdated)
                }
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { batch.set(collectionRef.document(it.id), it) }
                batch.commit().await()
                Log.d(TAG, "${itemsToUpload.size} rotinas locais enviadas para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                itemCronogramaDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "${itemsToDownload.size} rotinas da nuvem sincronizadas para o banco local.")
                triggerWidgetUpdate()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar rotinas.", e)
        }
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun getItemCronograma(itemId: String): ItemCronograma? {
        return itemCronogramaDao.getItemById(itemId)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        Log.d(TAG, "Inserindo rotina: ID=${item.id}, Título=${item.titulo}")
        itemCronogramaDao.insert(item)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(item.id)
                .set(item)
                .addOnSuccessListener { Log.d(TAG, "Rotina ${item.id} salva com sucesso no Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar rotina ${item.id} no Firestore.", e) }
        }
        triggerWidgetUpdate()
    }

    suspend fun insertAll(items: List<ItemCronograma>) {
        Log.d(TAG, "Inserindo ${items.size} rotinas em lote.")
        itemCronogramaDao.insertAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            items.forEach { item ->
                val docRef = firestore.collection("users").document(userId).collection("rotinas").document(item.id)
                batch.set(docRef, item)
            }
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "${items.size} rotinas inseridas na nuvem.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao inserir rotinas na nuvem", e) }
        }
        triggerWidgetUpdate()
    }

    suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        Log.d(TAG, "Atualizando ${items.size} rotinas em lote.")
        itemCronogramaDao.updateAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            items.forEach {
                val docRef = firestore.collection("users").document(userId).collection("rotinas").document(it.id)
                batch.set(docRef, it)
            }
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "${items.size} rotinas atualizadas na nuvem.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao atualizar rotinas na nuvem", e) }
        }
        triggerWidgetUpdate()
    }

    suspend fun excluirRotinaCompleta(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        Log.d(TAG, "Excluindo rotina: ID=${item.id}")
        try {
            habitoConcluidoDao.delete(item.id)
            itemCronogramaDao.delete(item)
            userId?.let {
                firestore.collection("users").document(it).collection("rotinas").document(item.id)
                    .delete()
                    .addOnSuccessListener { Log.d(TAG, "Rotina ${item.id} deletada com sucesso do Firestore.") }
                    .addOnFailureListener { e -> Log.w(TAG, "Erro ao deletar rotina ${item.id} do Firestore.", e) }
            }
            triggerWidgetUpdate()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir rotina.", e))
        }
    }

    suspend fun deleteRotinasByIds(ids: Set<String>) = withContext(dispatcher) {
        if (ids.isEmpty()) return@withContext
        Log.d(TAG, "Excluindo ${ids.size} rotinas em lote.")
        habitoConcluidoDao.deleteByIds(ids)
        itemCronogramaDao.deleteByIds(ids)
        userId?.let { currentUserId ->
            val batch = firestore.batch()
            val collectionRef = firestore.collection("users").document(currentUserId).collection("rotinas")
            ids.forEach {
                val docRef = collectionRef.document(it)
                batch.delete(docRef)
            }
            batch.commit().await()
        }
        triggerWidgetUpdate()
    }

    suspend fun deleteRotinasByTemplateId(templateId: String) = withContext(dispatcher) {
        Log.d(TAG, "Deletando rotinas do DB local para templateId: $templateId")
        val idsToDelete = itemCronogramaDao.getIdsByTemplateId(templateId).toSet()
        if (idsToDelete.isNotEmpty()) {
            habitoConcluidoDao.deleteByIds(idsToDelete)
            itemCronogramaDao.deleteByTemplateId(templateId)
        }

        userId?.let {
            val collectionRef = firestore.collection("users").document(it).collection("rotinas")
            val query = collectionRef.whereEqualTo("templateId", templateId)
            val batch = firestore.batch()
            val documents = query.get().await()
            documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Deletados ${documents.size()} rotinas do Firestore para templateId: $templateId")
        }
        triggerWidgetUpdate()
    }

    suspend fun deleteRotinasByCategoryId(categoryId: String) = withContext(dispatcher){
        Log.d(TAG, "Deletando rotinas por categoryId: $categoryId")
        val idsToDelete = itemCronogramaDao.getIdsByCategoryId(categoryId).toSet()
        if(idsToDelete.isNotEmpty()){
            habitoConcluidoDao.deleteByIds(idsToDelete)
            itemCronogramaDao.deleteByCategoryId(categoryId)
        }
        triggerWidgetUpdate()
    }

    fun getWidgetEvents(data: LocalDate, allowedCategoryIds: Set<String>): List<WidgetEventItem> {
        if (allowedCategoryIds.isEmpty()) return emptyList()

        val startOfDayMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDayMillis = data.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)

        return itemCronogramaDao.getWidgetEventItems(startOfDayMillis, endOfDayMillis, dayOfWeekName, allowedCategoryIds)
    }
    suspend fun clear() = withContext(dispatcher){
        Log.d(TAG, "Limpando todos os eventos do banco de dados local.")
        habitoConcluidoDao.clear()
        itemCronogramaDao.clear()
        triggerWidgetUpdate()
    }
}
