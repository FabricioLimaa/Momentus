package br.com.fabriciolima.momentus.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    private object PreferencesKeys {
        val LAST_SEEN_VERSION_CODE = intPreferencesKey("last_seen_version_code")
    }

    private val userId: String?
        get() = auth.currentUser?.uid

    /**
     * Flow que emite o estado de autenticação (UID) do usuário.
     */
    private val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Flow que observa os dados do usuário no Firestore, reagindo a mudanças de login.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userData: Flow<UserData?> = authStateFlow.flatMapLatest { currentUserId ->
        if (currentUserId == null) {
            flowOf(null)
        } else {
            callbackFlow {
                val userDocRef = firestore.collection("users").document(currentUserId)
                val listenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Log.w(TAG, "Permissão negada ao acessar dados do usuário.")
                            trySend(null)
                        } else {
                            close(error)
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toObject(UserData::class.java))
                    } else {
                        trySend(null)
                    }
                }
                awaitClose { listenerRegistration.remove() }
            }
        }
    }

    val lastSeenVersionCode: Flow<Int> = dataStore.data.map {
        it[PreferencesKeys.LAST_SEEN_VERSION_CODE] ?: 0
    }

    suspend fun updateLastSeenVersionCode(versionCode: Int) {
        dataStore.edit {
            it[PreferencesKeys.LAST_SEEN_VERSION_CODE] = versionCode
        }
    }

    /**
     * Cria ou atualiza o perfil do usuário no Firestore com base no FirebaseUser atual.
     * Usa SetOptions.merge() para não apagar campos existentes (como pontos e streaks).
     */
    suspend fun createOrUpdateUser(firebaseUser: FirebaseUser) = withContext(dispatcher) {
        val userDocRef = firestore.collection("users").document(firebaseUser.uid)
        try {
            Log.d(TAG, "Sincronizando perfil para UID: ${firebaseUser.uid}")
            
            // Dados básicos que devem estar sempre atualizados
            val profileUpdates = mutableMapOf<String, Any?>()
            firebaseUser.displayName?.let { profileUpdates["display_name"] = it }
            firebaseUser.email?.let { profileUpdates["email"] = it }

            if (profileUpdates.isNotEmpty()) {
                userDocRef.set(profileUpdates, SetOptions.merge()).await()
                Log.i(TAG, "Perfil do usuário sincronizado com sucesso no Firestore.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao sincronizar perfil do usuário no Firestore.", e)
        }
    }

    suspend fun updateStreak(newStreak: Int) = withContext(dispatcher) {
        userId?.let {
            try {
                val userDocRef = firestore.collection("users").document(it)
                val document = userDocRef.get().await()
                val currentStreak = document.getLong("streak")?.toInt() ?: 0

                if (newStreak != currentStreak) {
                    userDocRef.update("streak", newStreak).await()
                    Log.d(TAG, "Streak atualizado na nuvem: $newStreak")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao atualizar streak na nuvem.", e)
            }
        }
    }

    suspend fun acceptTerms() = withContext(dispatcher) {
        userId?.let {
            val userDocRef = firestore.collection("users").document(it)
            val termsData = mapOf("terms_accepted" to true, "terms_accepted_version" to 1)
            userDocRef.set(termsData, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    suspend fun hasAcceptedTerms(): Boolean = withContext(dispatcher) {
        userId?.let {
            val userDoc = firestore.collection("users").document(it).get().await()
            return@withContext userDoc.getBoolean("terms_accepted") ?: false
        } ?: false
    }
}
