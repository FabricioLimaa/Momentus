package br.com.fabriciolima.momentus.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(item: ItemCronograma) {
        Log.d("AlarmScheduler", "[ENTROU] Função schedule chamada para o item: ${item.titulo}")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, item.id)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, item.titulo)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        if (item.data != null) { // Evento com data específica
            // 1. Interpreta o Long salvo como um Instant em UTC e pega a LocalDate correta.
            val eventDate = Instant.ofEpochMilli(item.data!!).atZone(ZoneOffset.UTC).toLocalDate()
            
            // 2. Combina a data correta com a hora do evento.
            val eventLocalTime = eventDate.atTime(item.horarioInicio)

            // 3. Obtém o momento exato no fuso horário do usuário para agendar o alarme.
            val triggerTimeInMillis = eventLocalTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val calendar = Calendar.getInstance().apply {
                timeInMillis = triggerTimeInMillis
            }
            
            // Agendar 1 minuto antes para teste
            calendar.add(Calendar.MINUTE, -0)

            val alarmTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            Log.d("AlarmScheduler", "[DEBUG] Tempo do alarme calculado: $alarmTime (${sdf.format(calendar.time)})")
            Log.d("AlarmScheduler", "[DEBUG] Tempo atual do sistema:   $currentTime (${sdf.format(currentTime)})")

            if (alarmTime > currentTime) {
                Log.d("AlarmScheduler", "[SUCESSO] Condição válida. Agendando alarme ÚNICO para o evento '${item.titulo}' (ID: ${item.id})")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            } else {
                Log.w("AlarmScheduler", "[FALHA] O tempo do alarme (${sdf.format(alarmTime)}) já passou. O alarme para '${item.titulo}' não será agendado.")
            }

        } else if (item.diaDaSemana != null) { // Evento recorrente de template
            val calendarRecorrente = Calendar.getInstance()
            val hora = item.horarioInicio.hour
            val minuto = item.horarioInicio.minute

            val dayOfWeekInt = when (item.diaDaSemana) {
                "DOM" -> Calendar.SUNDAY
                "SEG" -> Calendar.MONDAY
                "TER" -> Calendar.TUESDAY
                "QUA" -> Calendar.WEDNESDAY
                "QUI" -> Calendar.THURSDAY
                "SEX" -> Calendar.FRIDAY
                "SAB" -> Calendar.SATURDAY
                else -> return
            }

            calendarRecorrente.set(Calendar.DAY_OF_WEEK, dayOfWeekInt)
            calendarRecorrente.set(Calendar.HOUR_OF_DAY, hora)
            calendarRecorrente.set(Calendar.MINUTE, minuto)
            calendarRecorrente.set(Calendar.SECOND, 0)
            calendarRecorrente.set(Calendar.MILLISECOND, 0)

            if (calendarRecorrente.timeInMillis < System.currentTimeMillis()) {
                calendarRecorrente.add(Calendar.WEEK_OF_YEAR, 1)
            }
            
            Log.d("AlarmScheduler", "Agendando alarme RECORRENTE para o evento '${item.titulo}' (ID: ${item.id}) para ${sdf.format(calendarRecorrente.time)}")

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendarRecorrente.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }

    fun cancel(item: ItemCronograma) {
        Log.d("AlarmScheduler", "Cancelando alarme para o evento '${item.titulo}' (ID: ${item.id})")
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
