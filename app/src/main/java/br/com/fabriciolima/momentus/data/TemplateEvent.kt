package br.com.fabriciolima.momentus.data

/**
 * Classe de dados que representa um evento na UI de criação de template.
 * Serve como um objeto de transferência de dados (DTO) entre a UI e a ViewModel.
 */
data class TemplateEvent(
    val titulo: String,
    val descricao: String?,
    val horarioInicio: String,
    val horarioTermino: String,
    val categoria: Rotina
)
