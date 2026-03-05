package br.com.fabriciolima.momentus.ui.screens.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Termos de Uso", "Política de Privacidade", "Licença")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informações Legais") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) })
                }
            }
            when (selectedTabIndex) {
                0 -> LegalContentViewer(fileName = "TERMS_AND_CONDITIONS.txt")
                1 -> LegalContentViewer(fileName = "PRIVACY_POLICY.txt")
                2 -> LegalContentViewer(fileName = "LICENSE.txt")
            }
        }
    }
}

@Composable
fun LegalContentViewer(fileName: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var text by remember { mutableStateOf("Carregando...") }

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
