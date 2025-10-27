package br.com.fabriciolima.momentus.domain.error

import androidx.annotation.StringRes
import br.com.fabriciolima.momentus.R

/**
 * Uma interface selada para representar erros de forma tipada em todo o aplicativo.
 * Isso permite que a UI reaja de forma diferente a cada tipo de erro.
 */
sealed interface AppError {
    val message: String? // Para erros dinâmicos vindos de APIs, etc.
    @get:StringRes
    val messageResId: Int? // Para mensagens de erro estáticas definidas em strings.xml

    /**
     * Um erro específico para quando o usuário tenta criar uma categoria com um nome que já existe.
     */
    data class DuplicateCategoryNameError(
        override val message: String? = null,
        @StringRes override val messageResId: Int? = R.string.error_duplicate_category_name
    ) : AppError

    /**
     * Um erro genérico para qualquer outra exceção não tratada especificamente.
     */
    data class UnknownError(
        override val message: String?,
        @StringRes override val messageResId: Int? = null
    ) : AppError
}
