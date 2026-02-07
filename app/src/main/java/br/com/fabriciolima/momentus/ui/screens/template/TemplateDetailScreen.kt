package br.com.fabriciolima.momentus.ui.screens.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.ui.viewmodel.TemplateDetailViewModel

@Composable
fun TemplateDetailRoute(
    templateId: String,
    onNavigateUp: () -> Unit,
    viewModel: TemplateDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) {
        viewModel.loadTemplate(templateId)
    }

    TemplateDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    uiState: br.com.fabriciolima.momentus.ui.viewmodel.TemplateDetailUiState,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.template?.template?.nome ?: "Detalhes do Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(it), contentAlignment = Alignment.Center) {
            if (uiState.template != null) {
                Text("Detalhes do Template com ID: ${uiState.template.template.id}")
            } else {
                Text("Carregando template...")
            }
        }
    }
}
