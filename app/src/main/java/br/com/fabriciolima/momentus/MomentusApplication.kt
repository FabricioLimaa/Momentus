package br.com.fabriciolima.momentus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.fabriciolima.momentus.widget.WidgetUpdateWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MomentusApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        setupWidgetUpdateWorker()
        createNotificationChannels()
    }

    private fun setupWidgetUpdateWorker() {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // 1. Canal de INÍCIO
            val startChannel = NotificationChannel(
                "CHANNEL_INICIO",
                "Lembretes de Início",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para o início das rotinas"
                enableLights(true)
                enableVibration(true)
                
                // Para usar som personalizado, coloque o arquivo em res/raw/inicio_som.mp3
                val soundUri = Uri.parse("android.resource://$packageName/${R.raw.inicio_som}")
                setSound(soundUri, attributes)
            }

            // 2. Canal de TÉRMINO
            val endChannel = NotificationChannel(
                "CHANNEL_TERMINO",
                "Lembretes de Término",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para o término das rotinas"
                enableLights(true)
                enableVibration(true)
                
                // Para usar som personalizado, coloque o arquivo em res/raw/termino_som.mp3
                val soundUri = Uri.parse("android.resource://$packageName/${R.raw.termino_som}")
                setSound(soundUri, attributes)
            }

            notificationManager.createNotificationChannel(startChannel)
            notificationManager.createNotificationChannel(endChannel)
        }
    }
}
