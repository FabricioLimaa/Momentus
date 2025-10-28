package br.com.fabriciolima.momentus.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.fabriciolima.momentus.data.repository.CategoryRepository
import br.com.fabriciolima.momentus.data.repository.EventoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationActionEntryPoint {
    fun categoryRepository(): CategoryRepository
    fun eventoRepository(): EventoRepository // Adicionado
}

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_AS_COMPLETED = "br.com.fabriciolima.momentus.ACTION_MARK_AS_COMPLETED"
        const val ACTION_SNOOZE = "br.com.fabriciolima.momentus.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationAction", "[RECEBIDO] Ação da notificação recebida: ${intent.action}")
        val eventId = intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_ID)
        Log.d("NotificationAction", "[DADOS] eventId: $eventId")

        if (eventId == null) return

        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationActionEntryPoint::class.java
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_MARK_AS_COMPLETED -> {
                val categoryRepository = hiltEntryPoint.categoryRepository()
                MainScope().launch {
                    Log.d("NotificationAction", "[AÇÃO] Chamando markHabitAsCompleted para o evento $eventId")
                    categoryRepository.markHabitAsCompleted(eventId)
                    notificationManager.cancel(eventId.hashCode())
                    Log.d("NotificationAction", "[CONCLUÍDO] Notificação para o evento $eventId removida.")
                }
            }
            ACTION_SNOOZE -> {
                val eventoRepository = hiltEntryPoint.eventoRepository()
                val alarmScheduler = AlarmScheduler(context)
                MainScope().launch {
                    Log.d("NotificationAction", "[AÇÃO] Adiar o evento $eventId")
                    val originalEvent = eventoRepository.getItemCronograma(eventId)
                    if (originalEvent != null) {
                        val snoozedEvent = originalEvent.copy(
                            horarioInicio = originalEvent.horarioInicio.plusMinutes(15),
                            horarioTermino = originalEvent.horarioTermino.plusMinutes(15)
                        )
                        eventoRepository.insertItemCronograma(snoozedEvent) // Salva a atualização
                        alarmScheduler.schedule(snoozedEvent) // Reagenda o alarme
                        notificationManager.cancel(eventId.hashCode())
                        Log.d("NotificationAction", "[CONCLUÍDO] Evento $eventId adiado e reagendado.")
                    } else {
                        Log.e("NotificationAction", "[ERRO] Evento com ID $eventId não encontrado para adiar.")
                    }
                }
            }
        }
    }
}
