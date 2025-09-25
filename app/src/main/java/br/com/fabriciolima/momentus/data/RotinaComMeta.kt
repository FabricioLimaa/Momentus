// ARQUIVO: data/RotinaComMeta.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.data

import androidx.room.Embedded

data class RotinaComMeta(
    @Embedded
    val rotina: Rotina,
    @Embedded
    val meta: Meta? // A meta pode ser nula se não tiver sido definida
)