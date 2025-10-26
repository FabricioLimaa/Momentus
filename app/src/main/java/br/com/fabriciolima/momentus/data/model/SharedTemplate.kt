package br.com.fabriciolima.momentus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedTemplate(
    val template: Template,
    val eventos: List<ItemCronograma>
)
