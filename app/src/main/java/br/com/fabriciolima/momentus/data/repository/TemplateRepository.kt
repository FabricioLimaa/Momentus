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
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    private val scheduleRepository: ScheduleRepository,
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

        try {
            val templatesCollection = firestore.collection("users").document(currentUserId).collection("templates")
            templatesListener = templatesCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Erro no snapshot listener de templates: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener

                try {
                    val validTemplates = mutableListOf<Template>()
                    for (doc in snapshots.documents) {
                        try {
                            val template = doc.toObject<Template>()
                            if (template != null) {
                                validTemplates.add(template)
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Erro ao deserializar Template ${doc.id}", ex)
                        }
                    }

                    if (validTemplates.isNotEmpty()) {
                        CoroutineScope(dispatcher + SupervisorJob()).launch {
                            try {
                                templateDao.insertAll(validTemplates)
                            } catch (t: Throwable) {
                                Log.e(TAG, "Erro ao salvar templates no Room", t)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Erro inesperado ao processar snapshot de templates", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Falha crítica ao registrar listener de templates", t)
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
            val snapshot = collectionRef.get().await()
            val cloudTemplates = mutableListOf<Template>()
            for (doc in snapshot.documents) {
                try {
                    val tmpl = doc.toObject<Template>()
                    if (tmpl != null) {
                        cloudTemplates.add(tmpl)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Erro ao deserializar Template ${doc.id} no syncTemplates", ex)
                }
            }
            if (cloudTemplates.isNotEmpty()) {
                templateDao.insertAll(cloudTemplates)
            }
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

        // 1. Atualiza o template
        templateDao.insert(updatedTemplate)
        
        // 2. Remove as tarefas antigas associadas a este template
        scheduleRepository.deleteItemsByTemplateId(updatedTemplate.id)
        
        // 3. Insere as novas tarefas
        val uniqueEvents = eventos.distinctBy { "${it.titulo}-${it.horarioInicioString}" }
        scheduleRepository.insertAllItems(uniqueEvents)

        checkTemplateAchievements()

        userId?.let { userId ->
            firestore.collection("users").document(userId).collection("templates").document(updatedTemplate.id).set(updatedTemplate)
        }
    }

    suspend fun deleteTemplate(template: Template) {
        scheduleRepository.deleteItemsByTemplateId(template.id)
        templateDao.delete(template)
        userId?.let {
            firestore.collection("users").document(it).collection("templates").document(template.id).delete()
        }
    }
    
    suspend fun clear() = withContext(dispatcher){
        templateDao.clear()
    }
}
