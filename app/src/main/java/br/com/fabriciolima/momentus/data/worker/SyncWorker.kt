package br.com.fabriciolima.momentus.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.GamificationRepository
import br.com.fabriciolima.momentus.data.repository.ScheduleRepository
import br.com.fabriciolima.momentus.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val TAG = "SyncWorker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val gamificationRepository: GamificationRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        Log.d(TAG, "[SYNC_ENGINE] Iniciando tarefa de sincronização em segundo plano.")

        try {
            // Executa as sincronizações em paralelo para maior eficiência
            val categorySync = async { categoryRepository.syncAllDataToLocal() }
            val scheduleSync = async { scheduleRepository.syncSchedule() }
            val achievementSync = async { gamificationRepository.syncUnlockedAchievements() }

            // Aguarda a conclusão de todas as tarefas e verifica os resultados
            val categoryResult = categorySync.await()
            val scheduleResult = scheduleSync.await()
            val achievementResult = achievementSync.await()
            
            val hasError = categoryResult is br.com.fabriciolima.momentus.util.Result.Error ||
                           scheduleResult is br.com.fabriciolima.momentus.util.Result.Error ||
                           achievementResult is br.com.fabriciolima.momentus.util.Result.Error

            if (hasError) {
                Log.w(TAG, "[SYNC_ENGINE] Sincronização parcial com erros. Solicitando retentativa.")
                Result.retry()
            } else {
                Log.i(TAG, "[SYNC_ENGINE] Sincronização concluída com sucesso. Atualizando Widget.")
                
                // Gatilho do Widget: Garante que os dados baixados apareçam na Home Screen
                WidgetUpdater.requestUpdate(applicationContext)
                
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC_ENGINE] Falha fatal na sincronização em segundo plano.", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "MomentusSyncWorker"
    }
}
