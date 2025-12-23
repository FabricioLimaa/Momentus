package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
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

private const val TAG = "EventoRepository"

@Singleton
open class EventoRepository @Inject constructor(
    private val itemCronogramaDao: ItemCronogramaDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var eventosListener: ListenerRegistration? = null

    private val userId: String?
        get() = auth.currentUser?.uid

    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()

    fun startListeningForChanges() {
        val currentUserId = this.userId
        Log.d(TAG, "Tentando iniciar listener. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, listener não iniciado.")
            return
        }
        if (eventosListener != null) return

        val eventosCollection = firestore.collection("users").document(currentUserId).collection("eventos")
        eventosListener = eventosCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nos eventos.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<ItemCronograma>()?.let {
                CoroutineScope(dispatcher).launch {
                    itemCronogramaDao.insertAll(it)
                    Log.d(TAG, "${it.size} eventos sincronizados em tempo real.")
                }
            }
        }
    }

    fun stopListeningForChanges() {
        eventosListener?.remove()
        eventosListener = null
    }

    suspend fun syncEventos() = withContext(dispatcher) {
        val currentUserId = userId
        Log.d(TAG, "Tentando sincronizar eventos. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, sincronização não realizada.")
            return@withContext
        }

        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("eventos")
            val localEventosMap = itemCronogramaDao.getAllSync().associateBy { it.id }
            val cloudEventosMap = collectionRef.get().await().toObjects<ItemCronograma>().associateBy { it.id }

            val itemsToUpload = localEventosMap.filter { (id, local) ->
                val cloudItem = cloudEventosMap[id]
                when {
                    cloudItem == null -> true
                    local.lastUpdated == null -> false
                    cloudItem.lastUpdated == null -> true
                    else -> local.lastUpdated!!.after(cloudItem.lastUpdated)
                }
            }.values

            val itemsToDownload = cloudEventosMap.filter { (id, cloud) ->
                val localItem = localEventosMap[id]
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
                Log.d(TAG, "${itemsToUpload.size} eventos locais enviados para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                itemCronogramaDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "${itemsToDownload.size} eventos da nuvem sincronizados para o banco local.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar eventos.", e)
        }
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun getItemCronograma(itemId: String): ItemCronograma? {
        return itemCronogramaDao.getItemById(itemId)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        Log.d(TAG, "Inserindo evento: ID=${item.id}, Título=${item.titulo}")
        itemCronogramaDao.insert(item)
        userId?.let {
            firestore.collection("users").document(it).collection("eventos").document(item.id)
                .set(item)
                .addOnSuccessListener { Log.d(TAG, "Evento ${item.id} salvo com sucesso no Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar evento ${item.id} no Firestore.", e) }
        }
    }
    
    suspend fun insertAll(items: List<ItemCronograma>) {
        Log.d(TAG, "Inserindo ${items.size} eventos em lote.")
        itemCronogramaDao.insertAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            items.forEach { item ->
                val docRef = firestore.collection("users").document(userId).collection("eventos").document(item.id)
                batch.set(docRef, item)
            }
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "${items.size} eventos inseridos na nuvem.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao inserir eventos na nuvem", e) }
        }
    }

    suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        Log.d(TAG, "Atualizando ${items.size} eventos em lote.")
        itemCronogramaDao.updateAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            items.forEach { 
                val docRef = firestore.collection("users").document(userId).collection("eventos").document(it.id)
                batch.set(docRef, it)
            }
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "${items.size} eventos atualizados na nuvem.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao atualizar eventos na nuvem", e) }
        }
    }

    suspend fun excluirEventoCompleto(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        Log.d(TAG, "Excluindo evento: ID=${item.id}")
        try {
            itemCronogramaDao.delete(item)
            userId?.let {
                firestore.collection("users").document(it).collection("eventos").document(item.id)
                    .delete()
                    .addOnSuccessListener { Log.d(TAG, "Evento ${item.id} deletado com sucesso do Firestore.") }
                    .addOnFailureListener { e -> Log.w(TAG, "Erro ao deletar evento ${item.id} do Firestore.", e) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir evento.", e))
        }
    }

    suspend fun deleteEventsByTemplateId(templateId: String) = withContext(dispatcher) {
        Log.d(TAG, "Deletando eventos do DB local para templateId: $templateId")
        itemCronogramaDao.deleteByTemplateId(templateId)
        
        userId?.let {
            val collectionRef = firestore.collection("users").document(it).collection("eventos")
            val query = collectionRef.whereEqualTo("templateId", templateId)
            val batch = firestore.batch()
            val documents = query.get().await()
            documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Deletados ${documents.size()} eventos do Firestore para templateId: $templateId")
        }
    }

    suspend fun deleteEventsByCategoryId(categoryId: String) {
        Log.d(TAG, "Deletando eventos por categoryId: $categoryId")
        itemCronogramaDao.deleteByCategoryId(categoryId)
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
        itemCronogramaDao.clear()
    }
}
