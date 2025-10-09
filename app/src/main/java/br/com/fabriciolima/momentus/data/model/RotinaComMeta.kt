package br.com.fabriciolima.momentus.data.model

import androidx.room.Embedded

data class RotinaComMeta(
    @Embedded
    val rotina: Rotina,
    @Embedded
    val meta: Meta? // A meta pode ser nula se não tiver sido definida
)

// Forçando a recompilação para resolver o erro de ClassLoader
