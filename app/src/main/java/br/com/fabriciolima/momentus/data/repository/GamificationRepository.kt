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
    private var achievementsListener: ListenerRegistration? = null

    private val _newlyUnlockedAchievement = MutableSharedFlow<Achievement>()
    val newlyUnlockedAchievement = _newlyUnlockedAchievement.asSharedFlow()

    private val userId: String?
        get() = auth.currentUser?.uid

    val unlockedAchievements = unlockedAchievementDao.getAll()

    fun getUnlockedAchievementIdsSync(): List<String> = unlockedAchievementDao.getAllIdsSync()

    fun startListeningForChanges() {
        val currentUserId = userId ?: return
        if (achievementsListener != null) return

        val collectionRef = firestore.collection("users").document(currentUserId).collection("unlocked_achievements")
        achievementsListener = collectionRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Erro ao escutar por mudanças nas conquistas.", e)
                return@addSnapshotListener
            }
            val cloudAchievements = snapshots?.toObjects<UnlockedAchievement>() ?: emptyList()
            CoroutineScope(dispatcher).launch {
                unlockedAchievementDao.clear()
                cloudAchievements.forEach { unlockedAchievementDao.insert(it) }
                Log.d(TAG, "[SYNC] ${cloudAchievements.size} conquistas sincronizadas em tempo real.")
            }
        }
    }

    fun stopListeningForChanges() {
        achievementsListener?.remove()
        achievementsListener = null
    }

    suspend fun unlockAchievement(achievementId: String, points: Int) = withContext(dispatcher) {
        val currentUserId = userId ?: return@withContext
        Log.d(TAG, "[SYNC] Desbloqueando conquista '$achievementId' para o usuário.")

        val unlocked = UnlockedAchievement(achievementId, Date())
        
        Log.d(TAG, "[LOCAL] Salvando no Room: ID=${unlocked.achievementId}, Data=${unlocked.dateUnlocked}")
        unlockedAchievementDao.insert(unlocked)

        val userDocRef = firestore.collection("users").document(currentUserId)
        val achievementDocRef = userDocRef.collection("unlocked_achievements").document(achievementId)
        
        Log.d(TAG, "[FIREBASE] Salvando no Firestore: ID=${unlocked.achievementId}, Data=${unlocked.dateUnlocked}")
        achievementDocRef.set(unlocked)
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Conquista '$achievementId' salva com sucesso no Firestore.") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao salvar conquista no Firestore.", e) }

        userDocRef.update("points", FieldValue.increment(points.toLong()))
            .addOnSuccessListener { Log.d(TAG, "[FIREBASE] Pontuação do usuário atualizada com +$points pontos.") }
            .addOnFailureListener { e -> Log.w(TAG, "[FIREBASE] Falha ao atualizar a pontuação do usuário.", e) }

        AchievementsList.allAchievements.find { it.id == achievementId }?.let {
            _newlyUnlockedAchievement.emit(it)
        }
    }
}
