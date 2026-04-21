package br.com.fabriciolima.momentus.util

import android.content.Context
import android.media.MediaPlayer
import br.com.fabriciolima.momentus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Toca o som de sucesso ao concluir uma tarefa.
     */
    fun playSuccessSound() {
        playSound(R.raw.inicio_som)
    }

    /**
     * Toca o som de celebração ao ganhar uma conquista.
     */
    fun playAchievementSound() {
        playSound(R.raw.termino_som)
    }

    private fun playSound(resId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            // Falha silenciosa para não quebrar a experiência do usuário
        }
    }
}
