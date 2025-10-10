package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class TemplateDetailActivity : ComponentActivity() {

    private val viewModel: TemplateDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val templateId = intent.getStringExtra("TEMPLATE_ID")
        if (templateId == null) {
            finish()
            return
        }

        setContent {
            // Chama o Composable da UI, que agora vive em seu próprio arquivo
            TemplateDetailScreen(
                templateId = templateId,
                viewModel = viewModel,
                onNavigateUp = { finish() }
            )
        }
    }
}
