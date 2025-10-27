package br.com.fabriciolima.momentus.data.model

import androidx.room.Embedded

data class CategoryWithMeta(
    @Embedded
    val category: Category,
    @Embedded
    val meta: Meta? // A meta pode ser nula se não tiver sido definida
)
