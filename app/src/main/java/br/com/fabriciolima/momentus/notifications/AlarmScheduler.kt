package br.com.fabriciolima.momentus.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        const val TYPE_START = "START"
        const val TYPE_END = "END"
    }

    fun schedule(item: ItemCronograma) {
        Log.d("AlarmScheduler", "Agendando alarmes para: ${item.titulo}")
        
        // Agenda Início
        scheduleAlarm(item, item.horarioInicio, TYPE_START)
        
        // Agenda Término
        scheduleAlarm(item, item.horarioTermino, TYPE_END)
    }

    private fun scheduleAlarm(item: ItemCronograma, time: LocalTime, type: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, item.id)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, item.titulo)
            putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, type)
        }

        // RequestCode único para início e fim para não sobrescrever
        val requestCode = if (type == TYPE_START) item.id.hashCode() else item.id.hashCode() + 31

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        if (item.data != null) {
            val eventDate = Instant.ofEpochMilli(item.data!!).atZone(ZoneOffset.UTC).toLocalDate()
            val eventLocalTime = eventDate.atTime(time)
            val triggerTimeInMillis = eventLocalTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerTimeInMillis > System.currentTimeMillis()) {
                Log.d("AlarmScheduler", "Agendando alarme $type para '${item.titulo}' às ${sdf.format(triggerTimeInMillis)}")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeInMillis,
                    pendingIntent
                )
            }
        } else if (item.diaDaSemana != null) {
            val calendar = Calendar.getInstance().apply {
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
                set(Calendar.DAY_OF_WEEK, dayOfWeekInt)
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            Log.d("AlarmScheduler", "Agendando alarme RECORRENTE $type para '${item.titulo}' às ${sdf.format(calendar.timeInMillis)}")
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }

    fun cancel(item: ItemCronograma) {
        val intent = Intent(context, AlarmReceiver::class.java)
        
        // Cancela Início
        val pendingStart = PendingIntent.getBroadcast(
            context, item.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingStart)

        // Cancela Término
        val pendingEnd = PendingIntent.getBroadcast(
            context, item.id.hashCode() + 31, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingEnd)
    }
}
