package br.com.fabriciolima.momentus.data.model

import java.util.UUID

/**
 * Classe de dados que representa um evento na UI de criação de template.
 * Serve como um objeto de transferência de dados (DTO) entre a UI e a ViewModel.
 */
data class TemplateEvent(
    val id: UUID = UUID.randomUUID(), // Adiciona um ID único para cada evento
    val titulo: String,
    val descricao: String?,
    val horarioInicio: String,
    val horarioTermino: String,
    val categoria: Rotina
)
