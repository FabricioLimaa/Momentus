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

    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao.getRotinasComMetas()
    open val todosOsTemplatesComEventos: Flow<List<TemplateComEventos>> = templateDao.getTemplatesComEventos()
    val todosOsItensDoCronograma: Flow<List<ItemCronograma>> = itemCronogramaDao.getAllItems()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = rotinaDao.getStats()

    private val userId: String?
        get() = auth.currentUser?.uid

    fun startListeningForChanges() {
        val currentUserId = userId ?: return
        if (rotinasListener != null) return

        val rotinasCollection = firestore.collection("users").document(currentUserId).collection("rotinas")
        rotinasListener = rotinasCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Erro ao escutar por mudanças nas rotinas.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val rotinasDaNuvem = snapshots.toObjects(Rotina::class.java)
                CoroutineScope(dispatcher).launch {
                    rotinaDao.insertAll(rotinasDaNuvem)
                    Log.d("Firestore", "${rotinasDaNuvem.size} rotinas sincronizadas em tempo real.")
                }
            }
        }
    }

    fun stopListeningForChanges() {
        rotinasListener?.remove()
        rotinasListener = null
    }

    suspend fun syncFirestoreToLocal() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext

        try {
            val snapshot = firestore.collection("users").document(currentUserId).collection("rotinas").get().await()
            val rotinasDaNuvem = snapshot.toObjects(Rotina::class.java)
            if (rotinasDaNuvem.isNotEmpty()) {
                rotinaDao.insertAll(rotinasDaNuvem)
                Log.d("Firestore", "${rotinasDaNuvem.size} rotinas sincronizadas da nuvem para o banco local.")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Erro ao sincronizar rotinas do Firestore.", e)
        }
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
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template)
    }

    fun getItensDoDia(dia: String): Flow<List<ItemCronograma>> {
        return itemCronogramaDao.getItemsByDayOfWeek(dia)
    }

    suspend fun getItemCronograma(itemId: String): ItemCronograma? {
        return itemCronogramaDao.getItemById(itemId)
    }

    suspend fun insertItemCronograma(item: ItemCronograma) {
        itemCronogramaDao.insert(item)
    }

    open suspend fun updateItensCronograma(items: List<ItemCronograma>) {
        itemCronogramaDao.updateAll(items)
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
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Falha ao excluir evento.", e))
        }
    }

    suspend fun fetchGoogleCalendarEvents(): Result<List<GoogleCalendarEvent>> {
        return googleCalendarSource.fetchEvents()
    }
}