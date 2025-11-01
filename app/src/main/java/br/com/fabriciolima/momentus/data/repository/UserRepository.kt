package br.com.fabriciolima.momentus.data.repository

import br.com.fabriciolima.momentus.data.model.UserData
import br.com.fabriciolima.momentus.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

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
                trySend(null)
            }
        }
        awaitClose { listenerRegistration.remove() }
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
