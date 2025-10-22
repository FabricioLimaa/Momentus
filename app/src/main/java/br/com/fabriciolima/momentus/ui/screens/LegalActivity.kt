package br.com.fabriciolima.momentus.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import java.io.IOException

class LegalActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("Termos de Uso", "Política de Privacidade", "Licença")

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Informações Legais") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
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
                            0 -> LegalContentViewer(fileName = "TERMS_AND_CONDITIONS.md")
                            1 -> LegalContentViewer(fileName = "PRIVACY_POLICY.md")
                            2 -> LegalContentViewer(fileName = "LICENSE.txt")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalContentViewer(fileName: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var text by remember { mutableStateOf("Carregando...") }

    // LaunchedEffect é a maneira correta de carregar dados ou executar
    // outras operações de longa duração (side-effects) em Compose.
    // Ele executa o bloco de código apenas quando a `key` (fileName) muda.
    LaunchedEffect(fileName) {
        try {
            text = context.assets.open(fileName).bufferedReader().use {
                it.readText()
            }
        } catch (e: IOException) {
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
