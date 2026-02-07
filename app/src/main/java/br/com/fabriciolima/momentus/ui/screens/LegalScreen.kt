/*package br.com.fabriciolima.momentus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.R
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onNavigateUp: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Termos de Uso", "Política de Privacidade", "Licença")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informações Legais") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) })
                }
            }
            when (selectedTabIndex) {
                0 -> LegalContent(resourceId = R.raw.terms_and_conditions)
                1 -> LegalContent(resourceId = R.raw.privacy_policy)
                2 -> LegalContent(resourceId = R.raw.license)
            }
        }
    }
}

@Composable
private fun LegalContent(resourceId: Int) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var text by remember { mutableStateOf("Carregando...") }

    LaunchedEffect(resourceId) {
        try {
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            text = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            text = "Falha ao carregar o documento."
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.verticalScroll(scrollState).padding(16.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}
*/