package br.com.fabriciolima.momentus.ui.components

import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import java.time.LocalTime

data class EventFormData(
    val titulo: String = "",
    val descricao: String = "",
    val horarioInicio: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val horarioTermino: LocalTime = LocalTime.now().withSecond(0).withNano(0).plusHours(1),
    val selectedRotina: Rotina? = null
) {
    companion object {
        fun fromItemCronograma(item: ItemCronograma, rotinas: List<Rotina>): EventFormData {
            return EventFormData(
                titulo = item.titulo,
                descricao = item.descricao ?: "",
                horarioInicio = item.horarioInicio,
                horarioTermino = item.horarioTermino,
                selectedRotina = rotinas.find { it.id == item.rotinaId }
            )
        }
    }
}
