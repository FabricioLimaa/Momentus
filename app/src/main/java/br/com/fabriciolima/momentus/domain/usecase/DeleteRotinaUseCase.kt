package br.com.fabriciolima.momentus.domain.usecase

import android.app.Application
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.notifications.AlarmScheduler
import br.com.fabriciolima.momentus.util.Result
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import javax.inject.Inject

class DeleteRotinaUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val application: Application
) {
    suspend operator fun invoke(item: ItemCronograma): Result<Unit> {
        // Primeiro, cancela qualquer alarme pendente para esta rotina
        alarmScheduler.cancel(item)

        // Então, deleta a rotina do repositório (que lida com deleção local e remota)
        val deleteResult = categoryRepository.deleteCompleteEvent(item)

        // Se a deleção foi bem sucedida, atualiza o widget
        if (deleteResult is Result.Success) {
            WidgetUpdater.requestUpdate(application)
        }

        return deleteResult
    }
}
