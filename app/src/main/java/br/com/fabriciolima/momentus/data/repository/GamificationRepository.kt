package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import br.com.fabriciolima.momentus.data.database.UnlockedAchievementDao
import br.com.fabriciolima.momentus.data.model.Achievement
import br.com.fabriciolima.momentus.data.model.UnlockedAchievement
import br.com.fabriciolima.momentus.di.IoDispatcher
import br.com.fabriciolima.momentus.domain.AchievementsList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GamificationRepository"

@Singleton
class GamificationRepository @Inject constructor(
    private val unlockedAchievementDao: UnlockedAchievementDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _newlyUnlockedAchievement = MutableSharedFlow<Achievement>()
    val newlyUnlockedAchievement = _newlyUnlockedAchievement.asSharedFlow()

    private val userId: String?
        get() = auth.currentUser?.uid

    val unlockedAchievements = unlockedAchievementDao.getAll()

    fun getUnlockedAchievementIdsSync(): List<String> = unlockedAchievementDao.getAllIdsSync()

    suspend fun clear() = withContext(dispatcher) {
        Log.d(TAG, "Limpando todas as conquistas desbloqueadas do banco de dados local.")
        unlockedAchievementDao.clear()
    }

    suspend fun unlockAchievement(achievementId: String, points: Int) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 6. Desbloqueando conquista '$achievementId' para o usuário.")

        val unlocked = UnlockedAchievement(achievementId, Date())
        
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 7. Salvando no Room: ID=${unlocked.achievementId}, Data=${unlocked.dateUnlocked}")
        unlockedAchievementDao.insert(unlocked)

        val userDocRef = firestore.collection("users").document(currentUserId)
        val achievementDocRef = userDocRef.collection("unlocked_achievements").document(achievementId)
        
        Log.d(TAG, "[ACHIEVEMENT_FLOW] 8. Salvando no Firestore: ID=${unlocked.achievementId}, Data=${unlocked.dateUnlocked}")
        achievementDocRef.set(unlocked)
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Conquista '$achievementId' salva com sucesso no Firestore.") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao salvar conquista no Firestore.", e) }

        Log.d(TAG, "[ACHIEVEMENT_FLOW] 9. Atualizando pontuação com +$points pontos.")
        userDocRef.update("points", FieldValue.increment(points.toLong()))
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Pontuação do usuário atualizada.") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao atualizar a pontuação.", e) }

        AchievementsList.allAchievements.find { it.id == achievementId }?.let {
            _newlyUnlockedAchievement.emit(it)
        }
    }

    suspend fun syncUnlockedAchievements() = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        Log.d(TAG, "[SYNC] Iniciando sincronização de conquistas desbloqueadas.")
        try {
            val collectionRef = firestore.collection("users").document(currentUserId).collection("unlocked_achievements")
            
            // 1. Obter dados locais e da nuvem
            val localAchievements = unlockedAchievementDao.getAllIdsSync().toSet()
            val cloudAchievements = collectionRef.get().await().toObjects<UnlockedAchievement>()
            val cloudAchievementMap = cloudAchievements.associateBy { it.achievementId }

            // 2. Sincronizar da Nuvem para o Local (Download)
            val achievementsToDownload = cloudAchievements.filter { it.achievementId !in localAchievements }
            if (achievementsToDownload.isNotEmpty()) {
                achievementsToDownload.forEach { unlockedAchievementDao.insert(it) }
                Log.d(TAG, "[SYNC] Baixadas ${achievementsToDownload.size} novas conquistas da nuvem.")
            }

            // 3. Sincronizar do Local para a Nuvem (Upload)
            val achievementsToUpload = localAchievements.filter { it !in cloudAchievementMap.keys }
            if (achievementsToUpload.isNotEmpty()) {
                val batch = firestore.batch()
                achievementsToUpload.forEach { achievementId ->
                    val docRef = collectionRef.document(achievementId)
                    // Assumimos que a data é a atual se foi criada offline. O ideal seria ter a data no DAO.
                    batch.set(docRef, UnlockedAchievement(achievementId, Date()))
                }
                batch.commit().await()
                Log.d(TAG, "[SYNC] Enviadas ${achievementsToUpload.size} novas conquistas para a nuvem.")
            }

            if (achievementsToDownload.isEmpty() && achievementsToUpload.isEmpty()) {
                 Log.d(TAG, "[SYNC] Conquistas já estão sincronizadas.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Erro ao sincronizar conquistas desbloqueadas.", e)
            throw e
        }
    }
}
