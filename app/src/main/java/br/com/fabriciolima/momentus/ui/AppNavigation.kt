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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import br.com.fabriciolima.momentus.ui.screens.HomeScreen
import br.com.fabriciolima.momentus.ui.screens.MoreScreen
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

sealed class Screen(val route: String, val label: String = "", val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Circle) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Terms : Screen("terms")

    object Home : Screen("home", "Hoje", Icons.Default.Home)
    object Calendar : Screen("calendar", "Agenda", Icons.Default.CalendarToday)
    object Templates : Screen("templates", "Templates", Icons.Default.GridView)
    object Stats : Screen("stats", "Estatísticas", Icons.Default.Assessment)
    object More : Screen("more", "Mais", Icons.Default.Menu)

    object Achievements : Screen("achievements", "Você", Icons.Default.EmojiEvents)
    object Categories : Screen("categories", "Categorias", Icons.Default.Category)
    object Updates : Screen("updates", "Novidades", Icons.Default.NewReleases)
    object Legal : Screen("legal", "Informações", Icons.Default.Info)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

@Composable
fun AppScaffold(
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    windowSizeClass: WindowSizeClass,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Instância ÚNICA do ViewModel para todo o fluxo do Scaffold
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    
    // Identifica se estamos em fluxo de Autenticação (deve esconder menus)
    val isAuthScreen = listOf(
        Screen.Onboarding.route,
        Screen.Login.route,
        Screen.SignUp.route,
        Screen.ForgotPassword.route,
        Screen.Terms.route
    ).contains(currentRoute ?: startDestination) // Usa startDestination como fallback inicial

    LaunchedEffect(Unit) {
        calendarViewModel.logoutEvent.collect { event ->
            if (event is LogoutEvent.Success) {
                // Limpa TUDO e vai para o Login
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && !isAuthScreen

    if (isWideScreen) {
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
                    Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp).size(32.dp))
                }
            ) {
                Column(modifier = Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    val railItems = listOf(Screen.Home, Screen.Calendar, Screen.Templates, Screen.Stats, Screen.More)
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

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        navController.navigate(Screen.Achievements.route) { launchSingleTop = true; restoreState = true }
                    }) {
                        UserAvatar(displayName = displayName, photoUrl = photoUrl?.toString(), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Lvl ${userLevel.level}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = { calendarViewModel.logout() }) { // Usa o logout centralizado
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AppNavHost(navController, startDestination, googleAccount, inAppUpdateManager, windowSizeClass, calendarViewModel, {})
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                if (!isAuthScreen) {
                    NavigationBar(tonalElevation = 8.dp) {
                        val items = listOf(Screen.Home, Screen.Calendar, Screen.Templates, Screen.Stats, Screen.More)
                        items.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                alwaysShowLabel = screenWidth > 320.dp,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (screen == Screen.More) {
                                        Icon(Icons.Default.Menu, contentDescription = screen.label)
                                    } else {
                                        Icon(screen.icon, contentDescription = screen.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (screenWidth < 360.dp) 9.sp else 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
                AppNavHost(navController, startDestination, googleAccount, inAppUpdateManager, windowSizeClass, calendarViewModel, {})
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    windowSizeClass: WindowSizeClass,
    sharedViewModel: CalendarViewModel, // Recebe o ViewModel compartilhado
    onMenuClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController) }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }
        composable(Screen.Terms.route) { TermsScreen(navController) }

        composable(Screen.Home.route) {
            val uiState by sharedViewModel.uiState.collectAsStateWithLifecycle()
            val eventsForToday by sharedViewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
            val allCategories by sharedViewModel.allCategories.collectAsStateWithLifecycle()
            
            HomeScreen(
                uiState = uiState,
                eventsForToday = eventsForToday,
                allCategories = allCategories,
                windowSizeClass = windowSizeClass,
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onMarkAsCompleted = sharedViewModel::markHabitAsCompleted,
                onUnmarkAsCompleted = sharedViewModel::unmarkHabitAsCompleted,
                onAddNewRotinaClicked = sharedViewModel::onAddNewRotinaClicked,
                onSaveRotina = sharedViewModel::saveSingleRotina,
                onUpdateRotina = sharedViewModel::updateRotina,
                onDeleteRotina = sharedViewModel::deleteRotina,
                onShowDetailClicked = sharedViewModel::onShowDetailClicked,
                onEditRotinaClicked = sharedViewModel::onEditRotinaClicked,
                onConfirmDeleteClicked = sharedViewModel::onConfirmDeleteClicked,
                onDialogDismiss = sharedViewModel::onDialogDismiss
            )
        }

        composable(Screen.Calendar.route) {
            val uiState by sharedViewModel.uiState.collectAsStateWithLifecycle()
            val selectedDate by sharedViewModel.selectedDate.collectAsStateWithLifecycle()
            val allCategories by sharedViewModel.allCategories.collectAsStateWithLifecycle()
            val eventsForSelectedDate by sharedViewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
            val installStatus by sharedViewModel.installStatus.collectAsStateWithLifecycle()
            val context = LocalContext.current

            CalendarScreen(
                uiState = uiState,
                selectedDate = selectedDate,
                allCategories = allCategories,
                eventsForSelectedDate = eventsForSelectedDate,
                installStatus = installStatus,
                account = googleAccount,
                showCompletionAnimation = sharedViewModel.showCompletionAnimation,
                windowSizeClass = windowSizeClass,
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onDateSelected = sharedViewModel::selectDate,
                onMenuClick = onMenuClick,
                onAddNewRotinaClicked = sharedViewModel::onAddNewRotinaClicked,
                onDialogDismiss = sharedViewModel::onDialogDismiss,
                onSaveRotina = sharedViewModel::saveSingleRotina,
                onUpdateRotina = sharedViewModel::updateRotina,
                onShowDetailClicked = sharedViewModel::onShowDetailClicked,
                onEditRotinaClicked = sharedViewModel::onEditRotinaClicked,
                onConfirmDeleteClicked = sharedViewModel::onConfirmDeleteClicked,
                onDeleteRotina = sharedViewModel::deleteRotina,
                onMarkAsCompleted = sharedViewModel::markHabitAsCompleted,
                onUnmarkAsCompleted = sharedViewModel::unmarkHabitAsCompleted,
                onErrorShown = sharedViewModel::onErrorShown,
                onSuccessMessageShown = sharedViewModel::onSuccessMessageShown,
                onAchievementDialogDismissed = sharedViewModel::onAchievementDialogDismissed,
                onCheckForAppUpdate = sharedViewModel::checkForAppUpdate,
                onStartUpdate = { updateInfo -> inAppUpdateManager.startUpdateFlow(updateInfo, context as Activity) },
                onCompleteUpdate = inAppUpdateManager::completeUpdate,
                onDismissUpdateDialog = sharedViewModel::onUpdateDialogDismissed,
                onRotinaLongPressed = sharedViewModel::onRotinaLongPressed,
                onRotinaClicked = sharedViewModel::onRotinaClicked,
                onClearSelection = sharedViewModel::onClearSelection,
                onSelectAll = sharedViewModel::onSelectAll,
                onDeleteSelectedRotinas = sharedViewModel::deleteSelectedRotinas,
                onConfirmDeleteSelectedRotinas = sharedViewModel::confirmDeleteSelectedRotinas
            )
        }
        composable(Screen.Templates.route) { TemplatesScreen(navController) }
        composable(Screen.Categories.route) { CategoriesScreen(navController) }
        composable(Screen.Stats.route) { StatsScreen(navController) }
        composable(Screen.Achievements.route) { AchievementsScreen(navController) }
        composable(Screen.Updates.route) { UpdateNotesScreen(navController) }
        composable(Screen.Legal.route) { LegalScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        
        composable(Screen.More.route) {
            val uiState by sharedViewModel.uiState.collectAsStateWithLifecycle()
            MoreScreen(
                navController = navController,
                uiState = uiState,
                onLogout = { sharedViewModel.logout() } // Agora chama a mesma instância!
            )
        }
    }
}
