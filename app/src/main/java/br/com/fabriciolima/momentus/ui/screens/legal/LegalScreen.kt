package br.com.fabriciolima.momentus.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.IOException
import androidx.compose.ui.unit.Dp
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    bottomBarPadding: Dp = 0.dp
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Termos", "Privacidade", "Licença")
    val snackbarHostState = remember { SnackbarHostState() }

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        Scaffold(
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = bottomBarPadding)
                ) { data ->
                    PremiumSnackbar(data)
                }
            },
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Termos e Privacidade",
                            fontWeight = FontWeight.ExtraBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if(selectedTabIndex == index) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 13.sp
                                ) 
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> LegalContentViewer(fileName = "TERMS_AND_CONDITIONS.txt", bottomPadding = bottomBarPadding)
                        1 -> LegalContentViewer(fileName = "PRIVACY_POLICY.txt", bottomPadding = bottomBarPadding)
                        2 -> LegalContentViewer(fileName = "LICENSE.txt", bottomPadding = bottomBarPadding)
                    }
                }
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
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .padding(bottom = bottomPadding + 40.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
