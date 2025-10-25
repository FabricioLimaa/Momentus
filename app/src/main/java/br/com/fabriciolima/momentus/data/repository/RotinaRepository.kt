package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.HabitoConcluidoDao
import br.com.fabriciolima.momentus.data.database.MetaDao
import br.com.fabriciolima.momentus.data.database.RotinaDao
import br.com.fabriciolima.momentus.data.model.HabitoConcluido
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.data.model.RotinaComMeta
import br.com.fabriciolima.momentus.data.model.StatsResult
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RotinaRepository"

enum class SyncStatus { OFFLINE, SYNCING, CONNECTED }

@Singleton
open class RotinaRepository @Inject constructor(
    private val rotinaDao: RotinaDao,
    private val metaDao: MetaDao,
    private val habitoConcluidoDao: HabitoConcluidoDao,
    private val templateRepository: TemplateRepository,
    private val eventoRepository: EventoRepository,
    private val googleCalendarSource: GoogleCalendarSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var rotinasListener: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow("Preparando...")
    val syncMessage = _syncMessage.asStateFlow()

    open val todasAsRotinasComMetas: Flow<List<RotinaComMeta>> = rotinaDao.getRotinasComMetas()
    val idsHabitosConcluidos: Flow<List<String>> = habitoConcluidoDao.getIdsConcluidos()
    open val stats: Flow<List<StatsResult>> = rotinaDao.getStats()

    private val userId: String?
        get() = auth.currentUser?.uid

    fun startListeningForChanges() {
        val userId = this.userId ?: return
        if (rotinasListener != null) return // Evita múltiplos listeners

        _syncStatus.value = SyncStatus.SYNCING

        val rotinasCollection = firestore.collection("users").document(userId).collection("rotinas")
        rotinasListener = rotinasCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nas rotinas.", e)
                _syncStatus.value = SyncStatus.OFFLINE
                return@addSnapshotListener
            }
            _syncStatus.value = SyncStatus.CONNECTED
            snapshots?.toObjects<Rotina>()?.let {
                CoroutineScope(dispatcher).launch {
                    rotinaDao.insertAll(it)
                    Log.d(TAG, "${it.size} rotinas sincronizadas em tempo real.")
                }
            }
        }

        templateRepository.startListeningForChanges()
        eventoRepository.startListeningForChanges()
    }

    fun stopListeningForChanges() {
        rotinasListener?.remove()
        rotinasListener = null
        templateRepository.stopListeningForChanges()
        eventoRepository.stopListeningForChanges()
        _syncStatus.value = SyncStatus.OFFLINE
    }

    suspend fun syncAllDataToLocal() {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            _syncMessage.value = "Verificando dados iniciais..."
            ensureDefaultRotinaExists()
            _syncMessage.value = "Sincronizando rotinas..."
            syncRotinas()
            _syncMessage.value = "Sincronizando templates..."
            templateRepository.syncTemplates()
            _syncMessage.value = "Sincronizando eventos..."
            eventoRepository.syncEventos()
            _syncMessage.value = "Sincronização concluída!"
            _syncStatus.value = SyncStatus.CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a sincronização geral.", e)
            _syncMessage.value = "Falha na sincronização."
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private suspend fun ensureDefaultRotinaExists() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        val rotinasCollection = firestore.collection("users").document(currentUserId).collection("rotinas")
        try {
            val querySnapshot = rotinasCollection.whereEqualTo("nome", "Outros").limit(1).get().await()
            if (querySnapshot.isEmpty) {
                Log.d(TAG, "Nenhuma rotina 'Outros' encontrada. Criando rotina padrão.")
                val defaultRotina = Rotina(
                    id = java.util.UUID.randomUUID().toString(),
                    nome = "Outros",
                    cor = "#808080"
                )
                insertRotina(defaultRotina)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar ou criar a rotina padrão 'Outros'.", e)
        }
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
                    cloudItem == null -> true
                    local.lastUpdated == null -> false
                    cloudItem.lastUpdated == null -> true
                    else -> local.lastUpdated.after(cloudItem.lastUpdated)
                }
            }.values

            val itemsToDownload = cloudRotinasMap.filter { (id, cloud) ->
                val localItem = localRotinasMap[id]
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
                Log.d(TAG, "${itemsToUpload.size} rotinas locais enviadas para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                rotinaDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "${itemsToDownload.size} rotinas da nuvem sincronizadas para o banco local.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar rotinas.", e)
            throw e
        }
    }

    suspend fun clearAllLocalData() = withContext(dispatcher) {
        Log.w(TAG, "Limpando todos os dados locais do banco de dados.")
        rotinaDao.clear()
        templateRepository.clear()
        eventoRepository.clear()
        metaDao.clear()
        habitoConcluidoDao.clear()
    }

    fun getTodasAsRotinasSync(): List<Rotina> {
        return rotinaDao.getAllSync()
    }

    fun getTodasAsRotinas(): Flow<List<Rotina>> {
        return rotinaDao.getAll()
    }

    open suspend fun insertRotina(rotina: Rotina) {
        Log.d(TAG, "Inserindo/Atualizando rotina: ID=${rotina.id}, Nome=${rotina.nome}")
        rotinaDao.insert(rotina)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(rotina.id)
                .set(rotina)
                .addOnSuccessListener { Log.d(TAG, "Rotina ${rotina.id} salva com sucesso no Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar rotina ${rotina.id} no Firestore.", e) }
        }
    }

    open suspend fun deleteRotina(rotina: Rotina) {
        Log.d(TAG, "Deletando rotina: ID=${rotina.id}, Nome=${rotina.nome}")
        rotinaDao.delete(rotina)
        userId?.let {
            firestore.collection("users").document(it).collection("rotinas").document(rotina.id)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "Rotina ${rotina.id} deletada com sucesso do Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao deletar rotina ${rotina.id} do Firestore.", e) }
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

    suspend fun atualizarEventoCompleto(item: ItemCronograma, cor: String?): Result<String?> = withContext(dispatcher) {
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

    suspend fun excluirEventoCompleto(item: ItemCronograma): Result<Unit> = withContext(dispatcher) {
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
