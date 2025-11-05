package br.com.fabriciolima.momentus.data.repository

import android.content.Context
import android.util.Log
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.domain.usecase.CheckAndUnlockAchievementsUseCase
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObjects
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TemplateRepository"

@Singleton
open class TemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templateDao: TemplateDao,
    private val eventoRepository: EventoRepository,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase, 
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var templatesListener: ListenerRegistration? = null

    private val userId: String?
        get() = auth.currentUser?.uid

    val todosOsTemplatesComEventos: Flow<List<TemplateComEventos>> = templateDao.getTemplatesComEventos()

    fun startListeningForChanges() {
        val currentUserId = this.userId
        Log.d(TAG, "Tentando iniciar listener. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, listener não iniciado.")
            return
        }
        if (templatesListener != null) return

        val templatesCollection = firestore.collection("users").document(currentUserId).collection("templates")
        templatesListener = templatesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nos templates.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<Template>()?.let {
                CoroutineScope(dispatcher).launch {
                    templateDao.insertAll(it)
                    Log.d(TAG, "${it.size} templates sincronizados em tempo real.")
                    eventoRepository.startListeningForChanges()
                }
            }
        }
    }

    fun stopListeningForChanges() {
        templatesListener?.remove()
        templatesListener = null
        eventoRepository.stopListeningForChanges()
    }

    suspend fun syncTemplates() = withContext(dispatcher) {
        val currentUserId = userId
        Log.d(TAG, "Tentando sincronizar templates. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w(TAG, "UID nulo, sincronização não realizada.")
            return@withContext
        }

        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("templates")
            val localTemplatesMap = templateDao.getAllSync().associateBy { it.id }
            val cloudTemplatesMap = collectionRef.get().await().toObjects<Template>().associateBy { it.id }

            val itemsToUpload = localTemplatesMap.filter { (id, local) ->
                val cloudItem = cloudTemplatesMap[id]
                when {
                    cloudItem == null -> true
                    local.lastUpdated == null -> false
                    cloudItem.lastUpdated == null -> true
                    else -> local.lastUpdated!!.after(cloudItem.lastUpdated)
                }
            }.values

            val itemsToDownload = cloudTemplatesMap.filter { (id, cloud) ->
                val localItem = localTemplatesMap[id]
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
                Log.d(TAG, "${itemsToUpload.size} templates locais enviados para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                templateDao.insertAll(itemsToDownload.toList())
                Log.d(TAG, "${itemsToDownload.size} templates da nuvem sincronizados para o banco local.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar templates.", e)
        }
    }

    fun getTemplateComEventos(templateId: String): Flow<TemplateComEventos?> {
        return templateDao.getTemplateComEventos(templateId)
    }

    suspend fun insertTemplate(template: Template) = withContext(dispatcher) {
        Log.d(TAG, "ROOM_UPDATE: Iniciando atualização do template no banco de dados local: ID=${template.id}")
        templateDao.insert(template)
        Log.i(TAG, "ROOM_UPDATE_SUCCESS: Template ID=${template.id} salvo no Room com sucesso.")

        checkTemplateAchievements()

        userId?.let { userId ->
            Log.d(TAG, "FIREBASE_SYNC: Iniciando sincronização com Firebase para o template ID=${template.id}")
            firestore.collection("users").document(userId).collection("templates").document(template.id)
                .set(template)
                .addOnSuccessListener { 
                    Log.i(TAG, "FIREBASE_SYNC_SUCCESS: Template ID=${template.id} salvo com sucesso no Firestore.")
                    WidgetUpdater.requestUpdate(context)
                }
                .addOnFailureListener { e -> 
                    Log.w(TAG, "FIREBASE_SYNC_FAILURE: Erro ao salvar template ID=${template.id} no Firestore.", e) 
                }
        } ?: Log.w(TAG, "FIREBASE_SYNC_SKIPPED: UID do usuário nulo. Sincronização com Firebase ignorada para o template ID=${template.id}.")
    }

    private suspend fun checkTemplateAchievements() {
        val totalTemplates = templateDao.getCountSync()
        Log.d(TAG, "[TEMPLATE_FLOW] Acionando verificação de conquistas com contagem total: $totalTemplates")
        checkAndUnlockAchievementsUseCase(totalTemplates = totalTemplates)
    }

    suspend fun saveTemplateWithEvents(template: Template, eventos: List<ItemCronograma>) = withContext(dispatcher) {
        Log.d(TAG, "TRANSACTION_UPDATE: Iniciando atualização transacional do template ID=${template.id}")
        val updatedTemplate = template.copy(lastUpdated = Date())

        Log.d(TAG, "ROOM_UPDATE: Atualizando template e eventos no Room para o template ID=${updatedTemplate.id}")
        templateDao.insert(updatedTemplate) // Usando insert (upsert) para garantir a atualização
        eventoRepository.deleteEventsByTemplateId(updatedTemplate.id)
        eventoRepository.insertAll(eventos)
        Log.i(TAG, "ROOM_UPDATE_SUCCESS: Template e eventos do ID=${updatedTemplate.id} atualizados no Room.")

        checkTemplateAchievements()

        userId?.let { userId ->
            Log.d(TAG, "FIREBASE_SYNC: Iniciando sincronização com Firebase para o template ID=${updatedTemplate.id}")
            firestore.collection("users").document(userId).collection("templates").document(updatedTemplate.id)
                .set(updatedTemplate)
                .addOnSuccessListener { 
                    Log.i(TAG, "FIREBASE_SYNC_SUCCESS: Template ID=${updatedTemplate.id} salvo com sucesso no Firestore.")
                    WidgetUpdater.requestUpdate(context)
                }
                .addOnFailureListener { e -> 
                    Log.w(TAG, "FIREBASE_SYNC_FAILURE: Erro ao salvar template ID=${updatedTemplate.id} no Firestore.", e) 
                }
        } ?: Log.w(TAG, "FIREBASE_SYNC_SKIPPED: UID do usuário nulo. Sincronização com Firebase ignorada para o template ID=${updatedTemplate.id}.")
    }

    suspend fun deleteTemplate(template: Template) {
        Log.d(TAG, "Deletando template: ID=${template.id}, Nome=${template.nome}")
        templateDao.delete(template)
        WidgetUpdater.requestUpdate(context)
        userId?.let {
            firestore.collection("users").document(it).collection("templates").document(template.id)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "Template ${template.id} deletado com sucesso do Firestore.") }
                .addOnFailureListener { e -> Log.w(TAG, "Erro ao deletar template ${template.id} do Firestore.", e) }
        }
    }
    suspend fun clear() = withContext(dispatcher){
        Log.d(TAG, "Limpando todos os templates do banco de dados local.")
        templateDao.clear()
    }
}
