// ARQUIVO: data/TemplateEvent.kt
package br.com.fabriciolima.momentus.data

// Classe auxiliar para gerenciar os eventos na UI antes de salvar no banco
data class TemplateEvent(
    val titulo: String,
    val descricao: String?,
    val horarioInicio: String,
    val horarioTermino: String,
    val categoria: Rotina
)