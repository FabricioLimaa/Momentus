// ARQUIVO: MomentusApplication.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import br.com.fabriciolima.momentus.data.RotinaRepository
import br.com.fabriciolima.momentus.data.database.AppDatabase

class MomentusApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { RotinaRepository(database.rotinaDao(), database.itemCronogramaDao(), database.templateDao()) }

    // --- MODIFICAÇÃO INICIA AQUI ---
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // O canal de notificação só é necessário para Android 8.0 (API 26) e superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lembretes de Rotina"
            val descriptionText = "Notificações para avisar sobre o início de rotinas agendadas."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("LEMBRETE_ROTINA_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            // Registra o canal com o sistema.
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}