package br.com.fabriciolima.momentus.domain.error

import androidx.annotation.StringRes
import br.com.fabriciolima.momentus.R

/**
 * Interface selada para representar erros de forma tipada.
 * Permite que a UI reaja de forma específica a cada falha (ex: botão de retry para rede).
 */
sealed class AppError(
    val message: String? = null,
    @StringRes val messageResId: Int? = null
) {
    // --- Erros de Domínio/Negócio ---
    data class DuplicateCategoryNameError(
        val name: String
    ) : AppError(messageResId = R.string.error_duplicate_category_name)

    object InvalidTimeError : AppError(messageResId = R.string.error_invalid_time)
    
    object EmptyNameError : AppError(messageResId = R.string.error_empty_name)

    // --- Erros de Rede/Sincronização ---
    object NetworkError : AppError(messageResId = R.string.error_no_internet)
    object SyncError : AppError(messageResId = R.string.error_sync_failed)

    // --- Erros de Autenticação/Segurança ---
    object AuthRequiredError : AppError(messageResId = R.string.error_auth_required)
    object SessionExpiredError : AppError(messageResId = R.string.error_session_expired)
    object ApiKeyError : AppError(messageResId = R.string.error_api_key_expired)
    
    /**
     * Erro genérico de autenticação com mensagem personalizada.
     */
    data class AuthError(val errorMessage: String) : AppError(message = errorMessage)

    // --- Erros de Integração Externa (Google Calendar) ---
    object GoogleCalendarPermissionError : AppError(messageResId = R.string.error_google_calendar_permission)

    // --- Erros Genéricos ---
    data class UnknownError(val throwable: Throwable) : AppError(message = throwable.message)
}
