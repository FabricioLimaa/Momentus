package br.com.fabriciolima.momentus.ui.components

import br.com.fabriciolima.momentus.data.model.Rotina
import java.time.LocalTime
import java.util.UUID

data class EventFormData(
    val id: UUID = UUID.randomUUID(),
    var titulo: String = "",
    var descricao: String = "",
    var selectedRotina: Rotina? = null,
    var horarioInicio: LocalTime = LocalTime.of(9, 0),
    var horarioTermino: LocalTime = LocalTime.of(10, 0)
)
