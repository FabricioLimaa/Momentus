package br.com.fabriciolima.momentus.ui.screens.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fabriciolima.momentus.ui.theme.*
import br.com.fabriciolima.momentus.ui.viewmodel.FocusMode
import br.com.fabriciolima.momentus.ui.viewmodel.FocusViewModel
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar
import br.com.fabriciolima.momentus.ui.util.AdaptiveOrientationWrapper
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import java.util.Locale
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    windowSizeClass: WindowSizeClass,
    bottomBarPadding: Dp = 0.dp,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    AdaptiveOrientationWrapper(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        LaunchedEffect(uiState.error, uiState.successMessage) {
            uiState.error?.let { error ->
                snackbarHostState.showSnackbar(error.message ?: "Erro no Modo Foco")
                viewModel.onErrorShown()
            }
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onSuccessMessageShown()
            }
        }

        if (uiState.showSettingsDialog) {
            FocusSettingsDialog(
                currentFocus = uiState.focusDurationMinutes,
                currentShortBreak = uiState.shortBreakMinutes,
                currentLongBreak = uiState.longBreakMinutes,
                onDismiss = { viewModel.setShowSettingsDialog(false) },
                onConfirm = { f, s, l -> viewModel.updateDurations(f, s, l) }
            )
        }

        Scaffold(
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    PremiumSnackbar(data)
                }
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Modo Foco", 
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = (-0.5).sp
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Sutil gradiente de fundo adaptativo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when (uiState.mode) {
                            FocusMode.FOCUS -> "Hora de concentrar"
                            FocusMode.SHORT_BREAK -> "Pausa curta"
                            FocusMode.LONG_BREAK -> "Pausa longa"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Timer Circular
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(280.dp)
                    ) {
                        val progress = uiState.timeLeftSeconds.toFloat() / uiState.totalSeconds.toFloat()
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(durationMillis = 1000),
                            label = "timerProgress"
                        )

                        // Trilho do progresso
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )

                        // Progresso Ativo
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 12.dp,
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )

                        // Tempo Restante
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val minutes = uiState.timeLeftSeconds / 60
                            val seconds = uiState.timeLeftSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (uiState.isRunning) {
                                Text(
                                    text = "EM ANDAMENTO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Controles de Modo
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeButton(
                            label = "Foco", 
                            isSelected = uiState.mode == FocusMode.FOCUS,
                            onClick = { viewModel.setMode(FocusMode.FOCUS) }
                        )
                        ModeButton(
                            label = "Pausa", 
                            isSelected = uiState.mode == FocusMode.SHORT_BREAK,
                            onClick = { viewModel.setMode(FocusMode.SHORT_BREAK) }
                        )
                        ModeButton(
                            label = "Longa", 
                            isSelected = uiState.mode == FocusMode.LONG_BREAK,
                            onClick = { viewModel.setMode(FocusMode.LONG_BREAK) }
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Botões de Ação Principais
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { viewModel.toggleTimer() },
                            modifier = Modifier
                                .height(72.dp)
                                .width(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (uiState.isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isRunning) "PAUSAR" else "FOCO",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setShowSettingsDialog(true) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Stats de Sessão (Glassmorphism)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FocusStatCard(
                            title = "XP Ganhos", 
                            value = "+${uiState.xpGained}", 
                            icon = Icons.Default.Bolt,
                            modifier = Modifier.weight(1f)
                        )
                        FocusStatCard(
                            title = "Ciclos", 
                            value = uiState.completedSessions.toString(), 
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(bottomBarPadding + 20.dp))
                }
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        elevation = null
    ) {
        Text(text = label, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
fun FocusStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FocusSettingsDialog(
    currentFocus: Int,
    currentShortBreak: Int,
    currentLongBreak: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var focus by remember { mutableStateOf(currentFocus.toFloat()) }
    var shortBreak by remember { mutableStateOf(currentShortBreak.toFloat()) }
    var longBreak by remember { mutableStateOf(currentLongBreak.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Tempos", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeSlider(label = "Foco", value = focus, onValueChange = { focus = it }, range = 5f..60f)
                TimeSlider(label = "Pausa Curta", value = shortBreak, onValueChange = { shortBreak = it }, range = 1f..15f)
                TimeSlider(label = "Pausa Longa", value = longBreak, onValueChange = { longBreak = it }, range = 10f..45f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(focus.toInt(), shortBreak.toInt(), longBreak.toInt()) }) {
                Text("Salvar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun TimeSlider(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = "${value.toInt()} min", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
