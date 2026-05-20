package br.com.fabriciolima.momentus.data.model

import androidx.compose.ui.graphics.Color

enum class ItemRarity(val label: String, val color: Color, val priceMultiplier: Long) {
    COMMON("Comum", Color(0xFF94A3B8), 1L),
    RARE("Raro", Color(0xFF3B82F6), 5L),
    EPIC("Épico", Color(0xFFA855F7), 15L),
    LEGENDARY("Lendário", Color(0xFFF59E0B), 50L)
}

enum class ItemType {
    STICKER, MEDAL, FRAME
}

data class MarketItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconRes: Int? = null,
    val iconUrl: String? = null,
    val type: ItemType = ItemType.STICKER,
    val rarity: ItemRarity = ItemRarity.COMMON,
    val basePrice: Long = 100L,
    val isOwned: Boolean = false
) {
    val finalPrice: Long get() = basePrice * rarity.priceMultiplier
}
