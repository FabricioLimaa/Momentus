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
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
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

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)

    private val userId: String?
        get() = auth.currentUser?.uid

    val unlockedAchievements = unlockedAchievementDao.getAll()

    fun getUnlockedAchievementIdsSync(): List<String> = unlockedAchievementDao.getAllIdsSync()

    fun stopListening() {
        _syncStatus.value = SyncStatus.OFFLINE
    }

    suspend fun clear() = withContext(dispatcher) {
        Log.d(TAG, "Limpando todas as conquistas desbloqueadas do banco de dados local.")
        unlockedAchievementDao.clear()
        _syncStatus.value = SyncStatus.OFFLINE
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
        if (_syncStatus.value != SyncStatus.OFFLINE) {
            Log.d(TAG, "[SYNC] Sincronização de conquistas já está em andamento ou conectada. Ignorando.")
            return@withContext
        }
        _syncStatus.value = SyncStatus.SYNCING
        
        val currentUserId = userId ?: run {
            _syncStatus.value = SyncStatus.OFFLINE
            return@withContext
        }
        Log.d(TAG, "[SYNC_DIAGNOSTIC] Iniciando sincronização de conquistas para o usuário: $currentUserId")
        
        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                val collectionRef = firestore.collection("users").document(currentUserId).collection("unlocked_achievements")
                
                // Tentativa de leitura
                val snapshot = collectionRef.get().await()
                Log.d(TAG, "[SYNC_DIAGNOSTIC] Firebase query executada. Documentos encontrados: ${snapshot.size()}.")
                
                val cloudAchievements = snapshot.toObjects<UnlockedAchievement>()
                val localAchievements = unlockedAchievementDao.getAllIdsSync().toSet()
                val cloudAchievementMap = cloudAchievements.associateBy { it.achievementId }

                val achievementsToDownload = cloudAchievements.filter { it.achievementId !in localAchievements }
                if (achievementsToDownload.isNotEmpty()) {
                    achievementsToDownload.forEach { unlockedAchievementDao.insert(it) }
                    Log.d(TAG, "[SYNC] Baixadas ${achievementsToDownload.size} novas conquistas da nuvem.")
                }

                val achievementsToUpload = localAchievements.filter { it !in cloudAchievementMap.keys }
                if (achievementsToUpload.isNotEmpty()) {
                    val batch = firestore.batch()
                    achievementsToUpload.forEach { achievementId ->
                        val docRef = collectionRef.document(achievementId)
                        batch.set(docRef, UnlockedAchievement(achievementId, Date()))
                    }
                    batch.commit().await()
                    Log.d(TAG, "[SYNC] Enviadas ${achievementsToUpload.size} novas conquistas para a nuvem.")
                }

                _syncStatus.value = SyncStatus.CONNECTED
                return@withContext // Sucesso
            } catch (e: Exception) {
                lastException = e
                if (e is com.google.firebase.firestore.FirebaseFirestoreException && 
                    e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    retryCount++
                    Log.w(TAG, "[SYNC_RETRY] Permissão negada. Tentativa $retryCount de $maxRetries... Aguardando 2s.")
                    delay(2000)
                } else {
                    break // Se não for erro de permissão, não adianta tentar de novo
                }
            }
        }

        Log.e(TAG, "[SYNC_DIAGNOSTIC] Falha final após retentativas.", lastException)
        _syncStatus.value = SyncStatus.OFFLINE
        if (lastException != null) throw lastException
    }
}
