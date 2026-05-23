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
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    navController: NavController,
    bottomBarPadding: Dp = 0.dp
) {
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
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) })
                }
            }
            when (selectedTabIndex) {
                0 -> LegalContentViewer(fileName = "TERMS_AND_CONDITIONS.txt", bottomPadding = bottomBarPadding)
                1 -> LegalContentViewer(fileName = "PRIVACY_POLICY.txt", bottomPadding = bottomBarPadding)
                2 -> LegalContentViewer(fileName = "LICENSE.txt", bottomPadding = bottomBarPadding)
            }
        }
    }
}

@Composable
fun LegalContentViewer(fileName: String, bottomPadding: Dp) {
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

    Column(modifier = Modifier
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .padding(bottom = bottomPadding + 32.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}
