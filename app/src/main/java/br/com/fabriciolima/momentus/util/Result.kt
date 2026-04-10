package br.com.fabriciolima.momentus.util

import br.com.fabriciolima.momentus.domain.error.AppError

/**
 * Uma classe utilitária para representar o resultado de operações assíncronas.
 * Agora utiliza AppError para fornecer erros tipados à UI.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
}
