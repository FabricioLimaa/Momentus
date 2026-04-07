package br.com.fabriciolima.momentus.util

import android.content.Context
import androidx.work.*
import br.com.fabriciolima.momentus.data.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Enfileira uma tarefa de sincronização imediata.
     * Usa ExistingWorkPolicy.REPLACE para garantir que múltiplas chamadas rápidas
     * resultem em apenas uma execução de sincronização.
     */
    fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.WORK_NAME + "_IMMEDIATE",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
