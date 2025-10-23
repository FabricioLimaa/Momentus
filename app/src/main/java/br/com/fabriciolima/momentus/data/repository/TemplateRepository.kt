package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.TemplateDao
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Template
import br.com.fabriciolima.momentus.data.model.TemplateComEventos
import br.com.fabriciolima.momentus.di.IoDispatcher
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val eventoRepository: EventoRepository,
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
        Log.d("FirestoreDebug", "TemplateRepository: Tentando iniciar listener. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w("FirestoreDebug", "TemplateRepository: UID nulo, listener não iniciado.")
            return
        }
        if (templatesListener != null) return

        val templatesCollection = firestore.collection("users").document(currentUserId).collection("templates")
        templatesListener = templatesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Erro ao escutar por mudanças nos templates.", e)
                return@addSnapshotListener
            }
            snapshots?.toObjects<Template>()?.let {
                CoroutineScope(dispatcher).launch {
                    templateDao.insertAll(it)
                    Log.d("Firestore", "${it.size} templates sincronizados em tempo real.")
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
        Log.d("FirestoreDebug", "TemplateRepository: Tentando sincronizar templates. UID: $currentUserId")
        if (currentUserId == null) {
            Log.w("FirestoreDebug", "TemplateRepository: UID nulo, sincronização não realizada.")
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
                Log.d("Firestore", "${itemsToUpload.size} templates locais enviados para a nuvem.")
            }

            if (itemsToDownload.isNotEmpty()) {
                templateDao.insertAll(itemsToDownload.toList())
                Log.d("Firestore", "${itemsToDownload.size} templates da nuvem sincronizados para o banco local.")
            }

        } catch (e: Exception) {
            Log.e("Firestore", "Erro ao sincronizar templates.", e)
        }
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

    suspend fun saveTemplateWithEvents(template: Template, eventos: List<ItemCronograma>) = withContext(dispatcher) {
        templateDao.update(template)
        eventoRepository.deleteEventsByTemplateId(template.id)
        eventoRepository.insertAll(eventos)
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
    suspend fun clear() = withContext(dispatcher){
        templateDao.clear()
    }
}
