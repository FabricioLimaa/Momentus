package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.StatsViewModel
import br.com.fabriciolima.momentus.viewmodel.StatsViewModelFactory

class StatsActivity : ComponentActivity() {

    private val viewModel: StatsViewModel by viewModels {
        StatsViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                StatsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
