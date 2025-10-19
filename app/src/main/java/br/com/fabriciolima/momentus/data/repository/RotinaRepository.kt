package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.ItemCronogramaDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.database.WidgetEventItem
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.model.StatsResult
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.data.source.GoogleCalendarSource
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.ui.viewmodel.GoogleCalendarEvent
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RotinaRepository @Inject constructor(
    private val rotinaDao: RotinaDao,
    private val itemCronogramaDao: ItemCronogramaDao,
    private val templateDao: TemplateDao,
    private val metaDao: MetaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    private val googleCalendarSource: GoogleCalendarSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var rotinasListener: ListenerRegistration? = null
    private var templatesListener: ListenerRegistration? = null
    private var eventosListener: ListenerRegistration? = null

    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao.getRotinasComMetas()
    open val todosOsTemplatesComEventos: Flow<List<TemplateComEventos>> = templateDao.getTemplatesComEventos()
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = rotinaDao.getStats()

    private val userId: String?
        get() = auth.currentUser?.uid

    fun startListeningForChanges() {
        val userId = this.userId ?: return
        if (rotinasListener != null || templatesListener != null || eventosListener != null) return // Evita múltiplos listeners

        // Listener para Rotinas
        val rotinasCollection = firestore.collection("users").document(userId).collection("rotinas")
        rotinasListener = rotinasCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Erro ao escutar por mudanças nas rotinas.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<Rotina>()?.let {
                CoroutineScope(dispatcher).launch {
                    rotinaDao.insertAll(it)
                    Log.d("Firestore", "${it.size} rotinas sincronizadas em tempo real.")
                }
            }
        }

        // Listener para Templates
        val templatesCollection = firestore.collection("users").document(userId).collection("templates")
        templatesListener = templatesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Erro ao escutar por mudanças nos templates.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<Template>()?.let {
                CoroutineScope(dispatcher).launch {
                    templateDao.insertAll(it)
                    Log.d("Firestore", "${it.size} templates sincronizados em tempo real.")
                }
            }
        }

        // Listener para Eventos
        val eventosCollection = firestore.collection("users").document(userId).collection("eventos")
        eventosListener = eventosCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Erro ao escutar por mudanças nos eventos.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<ItemCronograma>()?.let {
                CoroutineScope(dispatcher).launch {
                    itemCronogramaDao.insertAll(it)
                    Log.d("Firestore", "${it.size} eventos sincronizados em tempo real.")
                }
            }
        }
    }

    fun stopListeningForChanges() {
        rotinasListener?.remove()
        templatesListener?.remove()
        eventosListener?.remove()
        rotinasListener = null
        templatesListener = null
        eventosListener = null
    }

    suspend fun syncAllDataToLocal() {
        syncRotinas()
        // syncTemplates() // TODO: Reativar após implementar timestamps
        // syncEventos() // TODO: Reativar após implementar timestamps
    }

    private suspend fun syncRotinas() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("rotinas")
            val localRotinasMap = rotinaDao.getAllSync().associateBy { it.id }
            val cloudRotinasMap = collectionRef.get().await().toObjects<Rotina>().associateBy { it.id }

            val itemsToUpload = localRotinasMap.filter { (id, local) ->
                val cloudItem = cloudRotinasMap[id]
                when {
                    cloudItem == null -> true // Se não existe na nuvem, faz upload
                    local.lastUpdated == null -> false // Data local nula, não faz upload
                    cloudItem.lastUpdated == null -> true // Data da nuvem nula, local é mais recente
                    else -> local.lastUpdated.after(cloudItem.lastUpdated) // Compara as datas
                }
            }.values

            val itemsToDownload = cloudRotinasMap.filter { (id, cloud) ->
                val localItem = localRotinasMap[id]
                when {
                    localItem == null -> true // Se não existe localmente, faz download
                    cloud.lastUpdated == null -> false // Data da nuvem nula, não faz download
                    localItem.lastUpdated == null -> true // Data local nula, nuvem é mais recente
                    else -> cloud.lastUpdated.after(localItem.lastUpdated)
                }
            }.values

            if (itemsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                itemsToUpload.forEach { batch.set(collectionRef.document(it.id), it) }
                batch.commit().await()
                Log.d("Firestore", "${itemsToUpload.size} rotinas locais enviadas para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                rotinaDao.insertAll(itemsToDownload.toList())
                Log.d("Firestore", "${itemsToDownload.size} rotinas da nuvem sincronizadas para o banco local.")
            }

        } catch (e: Exception) {
            Log.e("Firestore", "Erro ao sincronizar rotinas.", e)
        }
    }

    private suspend fun syncTemplates() = withContext(dispatcher) {
        // TODO: Implementar lógica de timestamps para templates
    }

    private suspend fun syncEventos() = withContext(dispatcher) {
        // TODO: Implementar lógica de timestamps para eventos
    }
    
    suspend fun clearAllLocalData() = withContext(dispatcher) {
        rotinaDao.clear()
        templateDao.clear()
        itemCronogramaDao.clear()
        metaDao.clear()
        habitoConcluidoDao.clear()
    }

    fun getItensParaWidget(data: LocalDate, allowedRotinaIds: Set<String>): List<ItemCronograma> {
        val epochDay = data.toEpochDay()
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)
        
        if (allowedRotinaIds.isEmpty()) return emptyList()
        
        return itemCronogramaDao.getForWidget(epochDay, dayOfWeekName, allowedRotinaIds)
    }

    fun getWidgetEvents(data: LocalDate, allowedRotinaIds: Set<String>): List<WidgetEventItem> {
        if (allowedRotinaIds.isEmpty()) return emptyList()

        val epochDay = data.toEpochDay()
        val dayOfWeekName = data.dayOfWeek.name.substring(0, 3)

        return itemCronogramaDao.getWidgetEventItems(epochDay, dayOfWeekName, allowedRotinaIds)
    }

    fun getTodasAsRotinasSync(): List<Rotina> {
        return rotinaDao.getAllSync()
    }
    
    fun getTodasAsRotinas(): Flow<List<Rotina>> {
        return rotinaDao.getAll()
    }

    fun getTemplateComEventos(templateId: Int): Flow<TemplateComEventos> {
        return templateDao.getTemplateComEventos(templateId)
    }

    suspend fun insertTemplate(template: Template) {
        templateDao.insert(template)
        userId?.let {
            firestore.collection("users").document(it).collection("templates").document(template.id)
                .set(template)
                .addOnSuccessListener { Log.d("Firestore", "Template ${template.id} salvo na nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao salvar template na nuvem", e) }
        }
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template)
        userId?.let {
            firestore.collection("users").document(it).collection("templates").document(template.id)
                .delete()
                .addOnSuccessListener { Log.d("Firestore", "Template ${template.id} deletado da nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao deletar template da nuvem", e) }
        }
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun getItemCronograma(itemId: String): ItemCronograma? {
        return itemCronogramaDao.getItemById(itemId)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
        userId?.let {
            firestore.collection("users").document(it).collection("eventos").document(item.id)
                .set(item)
                .addOnSuccessListener { Log.d("Firestore", "Evento ${item.id} salvo na nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao salvar evento na nuvem", e) }
        }
    }

    open suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
        userId?.let { userId ->
            val batch = firestore.batch()
            items.forEach {
                val docRef = firestore.collection("users").document(userId).collection("eventos").document(it.id)
                batch.set(docRef, it)
            }
            batch.commit()
                .addOnSuccessListener { Log.d("Firestore", "${items.size} eventos atualizados na nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao atualizar eventos na nuvem", e) }
        }
    }

    open suspend fun insertRotina(rotina: Rotina) {
        rotinaDao.insert(rotina)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(rotina.id)
                .set(rotina)
                .addOnSuccessListener { Log.d("Firestore", "Rotina ${rotina.id} salva na nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao salvar rotina na nuvem", e) }
        }
    }

    open suspend fun deleteRotina(rotina: Rotina) {
        rotinaDao.delete(rotina)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(rotina.id)
                .delete()
                .addOnSuccessListener { Log.d("Firestore", "Rotina ${rotina.id} deletada da nuvem.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Erro ao deletar rotina da nuvem", e) }
        }
    }

    fun getMetaParaRotina(rotinaId: String): Flow<Meta?> {
        return metaDao.getMetaParaRotina(rotinaId)
    }

    suspend fun salvarMeta(meta: Meta) {
        metaDao.insertOrUpdate(meta)
    }

    suspend fun marcarHabitoComoConcluido(itemCronogramaId: String) {
        val habito = HabitoConcluido(itemCronogramaId = itemCronogramaId, dataConclusao = System.currentTimeMillis())
        habitoConcluidoDao.insert(habito)
    }

    suspend fun desmarcarHabitoComoConcluido(itemCronogramaId: String) {
        habitoConcluidoDao.delete(itemCronogramaId)
    }

    suspend fun salvarEventoNoGoogle(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horarioInicio: LocalTime,
        horarioTermino: LocalTime,
        cor: String?
    ): Result<String?> = googleCalendarSource.saveEvent(titulo, descricao, data, horarioInicio, horarioTermino, cor)

    suspend fun atualizarEventoCompleto(item: ItemCronograma): Result<String?> = withContext(dispatcher) {
        try {
            val result = googleCalendarSource.updateEvent(item)
            if (result is Result.Success) {
                itemCronogramaDao.insert(item.copy(googleCalendarEventId = result.data))
            }
            result
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao atualizar evento.", e))
        }
    }

    suspend fun excluirEventoCompleto(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
        try {
            item.googleCalendarEventId?.let { googleCalendarSource.deleteEvent(it) }
            itemCronogramaDao.delete(item)
            userId?.let {
                firestore.collection("users").document(it).collection("eventos").document(item.id)
                    .delete()
                    .addOnSuccessListener { Log.d("Firestore", "Evento ${item.id} deletado da nuvem.") }
                    .addOnFailureListener { e -> Log.w("Firestore", "Erro ao deletar evento da nuvem", e) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir evento.", e))
        }
    }

    suspend fun fetchGoogleCalendarEvents(): Result<List<GoogleCalendarEvent>> {
        return googleCalendarSource.fetchEvents()
    }
}