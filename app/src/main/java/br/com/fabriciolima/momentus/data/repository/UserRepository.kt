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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
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

    val userData: Flow<UserData?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val userDocRef = firestore.collection("users").document(userId)
        val listenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserData::class.java))
            } else {
                trySend(null) // Usuário autenticado mas sem dados no Firestore ainda.
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    val lastSeenVersionCode: Flow<Int> = dataStore.data.map {
        it[PreferencesKeys.LAST_SEEN_VERSION_CODE] ?: 0
    }

    suspend fun updateLastSeenVersionCode(versionCode: Int) {
        dataStore.edit {
            it[PreferencesKeys.LAST_SEEN_VERSION_CODE] = versionCode
        }
    }

    suspend fun createOrUpdateUser(firebaseUser: FirebaseUser) = withContext(dispatcher) {
        val userDocRef = firestore.collection("users").document(firebaseUser.uid)
        try {
            val document = userDocRef.get().await()
            if (!document.exists()) {
                Log.d(TAG, "Documento de usuário não encontrado para UID: ${firebaseUser.uid}. Criando novo documento.")
                val newUser = UserData(
                    displayName = firebaseUser.displayName,
                    email = firebaseUser.email
                )
                userDocRef.set(newUser).await()
                 Log.i(TAG, "Novo documento de usuário criado com sucesso no Firestore.")
            } else {
                Log.d(TAG, "Documento de usuário já existe para UID: ${firebaseUser.uid}. Nenhuma ação necessária.")
                // Opcional: Adicionar lógica para atualizar o documento se necessário.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar ou verificar documento do usuário no Firestore.", e)
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
