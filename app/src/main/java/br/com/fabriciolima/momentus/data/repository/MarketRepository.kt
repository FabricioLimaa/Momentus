package br.com.fabriciolima.momentus.data.repository

import br.com.fabriciolima.momentus.data.model.ItemRarity
import br.com.fabriciolima.momentus.data.model.ItemType
import br.com.fabriciolima.momentus.data.model.MarketItem
import br.com.fabriciolima.momentus.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepository @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    // Itens da Loja com IDs únicos para mapeamento de ícones na UI
    private val initialItems = listOf(
        MarketItem("stk_rocket", "Foguete Neon", "Para rotinas que decolam.", null, null, ItemType.STICKER, ItemRarity.RARE, 150L),
        MarketItem("stk_coffee", "Café Lendário", "Essencial para o foco extremo.", null, null, ItemType.STICKER, ItemRarity.LEGENDARY, 200L),
        MarketItem("stk_zen", "Zen Master", "Mantenha a calma e o equilíbrio.", null, null, ItemType.STICKER, ItemRarity.COMMON, 50L),
        MarketItem("med_time", "Mestre do Tempo", "Conquista máxima de pontualidade.", null, null, ItemType.MEDAL, ItemRarity.EPIC, 500L),
        MarketItem("med_streak", "Chama Eterna", "Especialista em sequências longas.", null, null, ItemType.MEDAL, ItemRarity.LEGENDARY, 1000L),
        MarketItem("stk_bolt", "Energia Pura", "Dê um choque na procrastinação.", null, null, ItemType.STICKER, ItemRarity.RARE, 120L)
    )

    fun getAvailableItems(): Flow<List<MarketItem>> {
        val uid = userId ?: return flowOf(initialItems)
        return firestore.collection("users").document(uid).collection("inventory")
            .snapshots()
            .map { snapshot ->
                val ownedIds = snapshot.documents.map { it.id }.toSet()
                initialItems.map { it.copy(isOwned = ownedIds.contains(it.id)) }
            }
    }

    suspend fun purchaseItem(item: MarketItem): Result<Unit> = withContext(dispatcher) {
        val uid = userId ?: return@withContext Result.failure(Exception("Usuário não autenticado"))
        
        try {
            val user = userRepository.getUserDataSync() ?: return@withContext Result.failure(Exception("Dados do usuário não encontrados"))
            
            if (user.points < item.finalPrice) {
                return@withContext Result.failure(Exception("XP insuficiente! Continue focando para ganhar mais."))
            }

            // Verificação de segurança: já possui o item?
            val inventoryRef = firestore.collection("users").document(uid).collection("inventory").document(item.id)
            if (inventoryRef.get().await().exists()) {
                return@withContext Result.failure(Exception("Você já possui este item!"))
            }

            // 1. Deduzir pontos
            userRepository.incrementPoints(-(item.finalPrice))

            // 2. Salvar no inventário
            firestore.collection("users").document(uid)
                .collection("inventory").document(item.id)
                .set(mapOf("purchasedAt" to System.currentTimeMillis()))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
