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
import com.google.firebase.firestore.toObjects
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
    private val rotinaRepository: RotinaRepository, // Atualizado: EventoRepository -> RotinaRepository
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
        if (currentUserId == null || templatesListener != null) return

        val templatesCollection = firestore.collection("users").document(currentUserId).collection("templates")
        templatesListener = templatesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            snapshots?.toObjects<Template>()?.let {
                CoroutineScope(dispatcher).launch {
                    templateDao.insertAll(it)
                }
            }
        }
    }

    fun stopListeningForChanges() {
        templatesListener?.remove()
        templatesListener = null
    }

    suspend fun syncTemplates() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext

        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("templates")
            val cloudTemplates = collectionRef.get().await().toObjects<Template>()
            templateDao.insertAll(cloudTemplates)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar templates.", e)
        }
    }

    fun getTemplateComEventos(templateId: String): Flow<TemplateComEventos?> {
        return templateDao.getTemplateComEventos(templateId)
    }

    suspend fun getTemplatesCount(): Int = withContext(dispatcher) {
        return@withContext templateDao.getCountSync()
    }

    suspend fun insertTemplate(template: Template) = withContext(dispatcher) {
        templateDao.insert(template)
        checkTemplateAchievements()

        userId?.let { userId ->
            firestore.collection("users").document(userId).collection("templates").document(template.id).set(template)
        }
    }

    private suspend fun checkTemplateAchievements() {
        val totalTemplates = templateDao.getCountSync()
        checkAndUnlockAchievementsUseCase(totalTemplates = totalTemplates)
    }

    suspend fun saveTemplateWithEvents(template: Template, eventos: List<ItemCronograma>) = withContext(dispatcher) {
        val updatedTemplate = template.copy(lastUpdated = Date())

        // --- CORREÇÃO DE DUPLICAÇÃO ---
        // 1. Atualiza o template
        templateDao.insert(updatedTemplate)
        
        // 2. Remove TODAS as rotinas antigas associadas a este template antes de inserir as novas
        rotinaRepository.deleteRotinasByTemplateId(updatedTemplate.id)
        
        // 3. Garante que as novas rotinas não tenham IDs duplicados e sejam únicas por título e horário
        val uniqueEvents = eventos.distinctBy { "${it.titulo}-${it.horarioInicioString}" }
        rotinaRepository.insertAll(uniqueEvents)

        checkTemplateAchievements()

        userId?.let { userId ->
            firestore.collection("users").document(userId).collection("templates").document(updatedTemplate.id).set(updatedTemplate)
        }
    }

    suspend fun deleteTemplate(template: Template) {
        rotinaRepository.deleteRotinasByTemplateId(template.id)
        templateDao.delete(template)
        userId?.let {
            firestore.collection("users").document(it).collection("templates").document(template.id).delete()
        }
    }
    
    suspend fun clear() = withContext(dispatcher){
        templateDao.clear()
    }
}
