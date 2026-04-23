package br.com.fabriciolima.momentus.domain.usecase

import br.com.fabriciolima.momentus.domain.error.AppError
import br.com.fabriciolima.momentus.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Caso de Uso responsável por limpar todos os dados do usuário na nuvem (Firestore).
 */
class ClearCloudDataUseCase @Inject constructor() {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend operator fun invoke(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthRequiredError)
        
        return try {
            val userDocRef = firestore.collection("users").document(userId)
            
            // Coleções para limpar
            val collections = listOf("eventos", "categories", "completed_habits", "unlocked_achievements", "templates")
            
            collections.forEach { collectionName ->
                val documents = userDocRef.collection(collectionName).get().await()
                if (!documents.isEmpty) {
                    val batch = firestore.batch()
                    documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e))
        }
    }
}
