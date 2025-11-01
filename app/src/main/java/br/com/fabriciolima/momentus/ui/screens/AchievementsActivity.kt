package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AchievementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                AchievementsScreen(onNavigateBack = { finish() })
            }
        }
    }
}
