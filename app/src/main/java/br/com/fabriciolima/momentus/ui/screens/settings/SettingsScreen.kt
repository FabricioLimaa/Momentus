package br.com.fabriciolima.momentus.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.repository.AppThemeMode
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.unit.Dp
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    bottomBarPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDataDeleted) {
        if (uiState.isDataDeleted) {
            snackbarHostState.showSnackbar("Dados locais limpos após exclusão na nuvem.")
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    MomentusTheme(
        overrideThemeMode = uiState.themeMode,
        overridePrimaryColorHex = uiState.primaryColorHex,
        overrideCornerRadiusDp = uiState.cornerRadius,
        overrideFontSizeMultiplier = uiState.fontSizeMultiplier
    ) {
        Scaffold(
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    PremiumSnackbar(data)
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = bottomBarPadding + 32.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Personalização",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Modo do Tema
                item {
                    Column {
                        Text("Modo do Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ThemeModeSelector(
                            selectedMode = uiState.themeMode,
                            onModeSelected = viewModel::setThemeMode
                        )
                    }
                }

                // Cor Principal
                item {
                    Column {
                        Text("Cor Principal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryColorGrid(
                            selectedColorHex = uiState.primaryColorHex,
                            onColorSelected = viewModel::setPrimaryColor
                        )
                    }
                }

                // Tamanho da Fonte
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tamanho da Fonte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${(uiState.fontSizeMultiplier * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Amostra do texto",
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Slider(
                            value = uiState.fontSizeMultiplier,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 0.8f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Feedback Sensorial
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Experiência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        
                        SettingsSwitchRow(
                            title = "Efeitos Sonoros",
                            subtitle = "Tocar sons ao completar tarefas",
                            checked = uiState.soundEnabled,
                            onCheckedChange = viewModel::setSoundEnabled
                        )

                        SettingsSwitchRow(
                            title = "Feedback Tátil",
                            subtitle = "Vibrar ao interagir com o app",
                            checked = uiState.hapticEnabled,
                            onCheckedChange = viewModel::setHapticEnabled
                        )
                    }
                }

                // Botões de Ação de Tema
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = viewModel::saveThemeSettings,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = viewModel::resetToDefaults,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resetar")
                        }
                    }
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }

                // Sincronização
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudSync, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sincronização",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Conta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(uiState.userEmail, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Último Backup", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(uiState.lastSyncTime, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = viewModel::syncNow,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizar Agora", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Limpar Dados da Nuvem")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Dados") },
            text = { Text("Isso removerá permanentemente seus eventos e categorias da nuvem. O aplicativo local será limpo após a exclusão.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllCloudData(); showDeleteConfirmDialog = false }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange, 
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun ThemeModeSelector(
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(selectedMode) {
                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                            AppThemeMode.DARK -> Icons.Default.DarkMode
                            AppThemeMode.SYSTEM -> Icons.Default.SettingsSuggest
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = when(selectedMode) {
                        AppThemeMode.LIGHT -> "Claro"
                        AppThemeMode.DARK -> "Escuro"
                        AppThemeMode.SYSTEM -> "Sistema"
                    })
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Claro") }, onClick = { onModeSelected(AppThemeMode.LIGHT); expanded = false })
            DropdownMenuItem(text = { Text("Escuro") }, onClick = { onModeSelected(AppThemeMode.DARK); expanded = false })
            DropdownMenuItem(text = { Text("Sistema") }, onClick = { onModeSelected(AppThemeMode.SYSTEM); expanded = false })
        }
    }
}

@Composable
fun PrimaryColorGrid(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#34D399" to "Verde",
        "#6366F1" to "Azul",
        "#A855F7" to "Violeta",
        "#EC4899" to "Rosa",
        "#F97316" to "Laranja",
        "#EAB308" to "Amarelo"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.take(3).forEach { (hex, name) ->
                ColorOptionMinimal(hex, name, selectedColorHex.equals(hex, true), { onColorSelected(hex) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.drop(3).forEach { (hex, name) ->
                ColorOptionMinimal(hex, name, selectedColorHex.equals(hex, true), { onColorSelected(hex) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ColorOptionMinimal(
    hex: String,
    name: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Surface(
        onClick = onSelect,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
