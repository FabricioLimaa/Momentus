package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.fabriciolima.momentus.domain.model.UserLevel
import br.com.fabriciolima.momentus.ui.screens.CalendarScreen
import br.com.fabriciolima.momentus.ui.screens.MainActivity
import br.com.fabriciolima.momentus.ui.screens.UserAvatar
import br.com.fabriciolima.momentus.ui.screens.achievements.AchievementsScreen
import br.com.fabriciolima.momentus.ui.screens.auth.ForgotPasswordScreen
import br.com.fabriciolima.momentus.ui.screens.auth.LoginScreen
import br.com.fabriciolima.momentus.ui.screens.auth.SignUpScreen
import br.com.fabriciolima.momentus.ui.screens.category.CategoriesScreen
import br.com.fabriciolima.momentus.ui.screens.legal.LegalScreen
import br.com.fabriciolima.momentus.ui.screens.onboarding.OnboardingScreen
import br.com.fabriciolima.momentus.ui.screens.settings.SettingsScreen
import br.com.fabriciolima.momentus.ui.screens.stats.StatsScreen
import br.com.fabriciolima.momentus.ui.screens.template.TemplatesScreen
import br.com.fabriciolima.momentus.ui.screens.terms.TermsScreen
import br.com.fabriciolima.momentus.ui.screens.updates.UpdateNotesScreen
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.LogoutEvent
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// --- BUSCA: DEFINIÇÃO DE TELAS E ROTAS ---
// Centraliza todas as rotas do aplicativo com seus rótulos e ícones
sealed class Screen(val route: String, val label: String = "", val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Circle) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Terms : Screen("terms")
    
    // Abas principais (Bottom Bar e Tablet Rail)
    object Calendar : Screen("calendar", "Agenda", Icons.Default.CalendarToday)
    object Templates : Screen("templates", "Templates", Icons.Default.GridView)
    object Categories : Screen("categories", "Categorias", Icons.Default.Category)
    object Stats : Screen("stats", "Estatísticas", Icons.Default.Assessment)
    object Achievements : Screen("achievements", "Você", Icons.Default.Person)
    
    // Telas de suporte e configurações
    object Updates : Screen("updates", "Novidades", Icons.Default.NewReleases)
    object Legal : Screen("legal", "Informações", Icons.Default.Info)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

/**
 * Componente principal que gerencia o Scaffold adaptativo (Bottom Bar ou Navigation Rail)
 */
@Composable
fun AppScaffold(
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    windowSizeClass: WindowSizeClass,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ViewModel central para dados do usuário no Shell (Menu/Barra)
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    
    // --- BUSCA: ESCUTADOR DE LOGOUT ---
    LaunchedEffect(Unit) {
        calendarViewModel.logoutEvent.collect { event ->
            if (event is LogoutEvent.Success) {
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        }
    }

    if (isWideScreen) {
        // --- BUSCA: LAYOUT TABLET (NAVIGATION RAIL CENTRALIZADO) ---
        Row(modifier = Modifier.fillMaxSize()) {
            val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val photoUrl = googleAccount?.photoUrl ?: firebaseUser?.photoUrl
            val displayName = uiState.userData?.displayName ?: firebaseUser?.displayName ?: "Usuário"
            val userLevel = remember(uiState.userData?.points) {
                UserLevel.fromPoints(uiState.userData?.points ?: 0)
            }

            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Icon(
                        Icons.Default.CalendarMonth, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp).size(32.dp)
                    )
                }
            ) {
                // Menu centralizado verticalmente à esquerda
                Column(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val railItems = listOf(
                        Screen.Calendar, 
                        Screen.Templates, 
                        Screen.Categories, 
                        Screen.Stats, 
                        Screen.Updates, 
                        Screen.Settings
                    )
                    railItems.forEach { screen ->
                        NavigationRailItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }

                // --- BUSCA: SEÇÃO DE PERFIL NA BASE DO MENU LATERAL NO TABLET ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Achievements.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        UserAvatar(displayName = displayName, photoUrl = photoUrl?.toString(), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Lvl ${userLevel.level}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))

                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    googleAccount = googleAccount,
                    inAppUpdateManager = inAppUpdateManager,
                    windowSizeClass = windowSizeClass,
                    onMenuClick = {} 
                )
            }
        }
    } else {
        // --- BUSCA: LAYOUT CELULAR (BOTTOM NAVIGATION COM 5 ITENS) ---
        Scaffold(
            bottomBar = {
                val hideBottomBar = listOf(
                    Screen.Onboarding.route, 
                    Screen.Login.route, 
                    Screen.SignUp.route, 
                    Screen.ForgotPassword.route, 
                    Screen.Terms.route
                ).contains(currentRoute)
                
                if (!hideBottomBar) {
                    NavigationBar {
                        // Lista de 5 opções principais para a barra inferior no celular (Incluso Categorias)
                        val items = listOf(
                            Screen.Calendar, 
                            Screen.Templates, 
                            Screen.Categories, 
                            Screen.Stats, 
                            Screen.Achievements
                        )
                        items.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    if (screen == Screen.Achievements) {
                                        val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
                                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                                        val photoUrl = googleAccount?.photoUrl ?: firebaseUser?.photoUrl
                                        UserAvatar(
                                            displayName = uiState.userData?.displayName ?: "U", 
                                            photoUrl = photoUrl?.toString(), 
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(screen.icon, contentDescription = screen.label)
                                    }
                                },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    googleAccount = googleAccount,
                    inAppUpdateManager = inAppUpdateManager,
                    windowSizeClass = windowSizeClass,
                    onMenuClick = {} 
                )
            }
        }
    }
}

// --- BUSCA: GERENCIADOR DE TELAS (NAVHOST) ---
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    windowSizeClass: WindowSizeClass,
    onMenuClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) { OnboardingScreen(navController = navController) }
        composable(Screen.Login.route) { LoginScreen(navController = navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController = navController) }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController = navController) }
        composable(Screen.Terms.route) { TermsScreen(navController = navController) }

        composable(Screen.Calendar.route) {
            val viewModel: CalendarViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
            val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
            val eventsForSelectedDate by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
            val installStatus by viewModel.installStatus.collectAsStateWithLifecycle()
            val context = LocalContext.current

            CalendarScreen(
                uiState = uiState,
                selectedDate = selectedDate,
                allCategories = allCategories,
                eventsForSelectedDate = eventsForSelectedDate,
                installStatus = installStatus,
                account = googleAccount,
                showCompletionAnimation = viewModel.showCompletionAnimation,
                windowSizeClass = windowSizeClass,
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onDateSelected = viewModel::selectDate,
                onMenuClick = onMenuClick,
                onAddNewRotinaClicked = viewModel::onAddNewRotinaClicked,
                onDialogDismiss = viewModel::onDialogDismiss,
                onSaveRotina = viewModel::saveSingleRotina,
                onUpdateRotina = viewModel::updateRotina,
                onShowDetailClicked = viewModel::onShowDetailClicked,
                onEditRotinaClicked = viewModel::onEditRotinaClicked,
                onConfirmDeleteClicked = viewModel::onConfirmDeleteClicked,
                onDeleteRotina = viewModel::deleteRotina,
                onMarkAsCompleted = viewModel::markHabitAsCompleted,
                onUnmarkAsCompleted = viewModel::unmarkHabitAsCompleted,
                onErrorShown = viewModel::onErrorShown,
                onSuccessMessageShown = viewModel::onSuccessMessageShown,
                onAchievementDialogDismissed = viewModel::onAchievementDialogDismissed,
                onCheckForAppUpdate = viewModel::checkForAppUpdate,
                onStartUpdate = { updateInfo ->
                    inAppUpdateManager.startUpdateFlow(updateInfo, context as Activity)
                },
                onCompleteUpdate = inAppUpdateManager::completeUpdate,
                onDismissUpdateDialog = viewModel::onUpdateDialogDismissed,
                onRotinaLongPressed = viewModel::onRotinaLongPressed,
                onRotinaClicked = viewModel::onRotinaClicked,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelectedRotinas = viewModel::deleteSelectedRotinas,
                onConfirmDeleteSelectedRotinas = viewModel::confirmDeleteSelectedRotinas
            )
        }
        composable(Screen.Templates.route) { TemplatesScreen(navController = navController) }
        composable(Screen.Categories.route) { CategoriesScreen(navController = navController) }
        composable(Screen.Stats.route) { StatsScreen(navController = navController) }
        composable(Screen.Achievements.route) { AchievementsScreen(navController = navController) }
        composable(Screen.Updates.route) { UpdateNotesScreen(navController = navController) }
        composable(Screen.Legal.route) { LegalScreen(navController = navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
    }
}
