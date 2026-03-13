package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fabriciolima.momentus.ui.screens.AppDrawerContent
import br.com.fabriciolima.momentus.ui.screens.CalendarScreen
import br.com.fabriciolima.momentus.ui.screens.MainActivity
import br.com.fabriciolima.momentus.ui.screens.achievements.AchievementsScreen
import br.com.fabriciolima.momentus.ui.screens.auth.ForgotPasswordScreen
import br.com.fabriciolima.momentus.ui.screens.auth.LoginScreen
import br.com.fabriciolima.momentus.ui.screens.auth.SignUpScreen
import br.com.fabriciolima.momentus.ui.screens.category.CategoriesScreen
import br.com.fabriciolima.momentus.ui.screens.legal.LegalScreen
import br.com.fabriciolima.momentus.ui.screens.onboarding.OnboardingScreen
import br.com.fabriciolima.momentus.ui.screens.stats.StatsScreen
import br.com.fabriciolima.momentus.ui.screens.template.TemplatesScreen
import br.com.fabriciolima.momentus.ui.screens.terms.TermsScreen
import br.com.fabriciolima.momentus.ui.screens.updates.UpdateNotesScreen
import br.com.fabriciolima.momentus.ui.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.LogoutEvent
import br.com.fabriciolima.momentus.util.InAppUpdateManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Terms : Screen("terms")
    object Calendar : Screen("calendar")
    object Templates : Screen("templates")
    object Categories : Screen("categories")
    object Stats : Screen("stats")
    object Achievements : Screen("achievements")
    object Updates : Screen("updates")
    object Legal : Screen("legal")
}

@Composable
fun AppScaffold(
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val calendarViewModel: CalendarViewModel = hiltViewModel()
    
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
            AppDrawerContent(
                userData = uiState.userData,
                account = googleAccount,
                showUpdateBadge = uiState.showUpdateBadge,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route)
                },
                onLogout = onLogout,
                onUpdatesClicked = calendarViewModel::onUpdatesClicked
            )
        }
    ) {
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            googleAccount = googleAccount,
            inAppUpdateManager = inAppUpdateManager,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    googleAccount: GoogleSignInAccount?,
    inAppUpdateManager: InAppUpdateManager,
    onMenuClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(navController = navController)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }
        composable(Screen.Terms.route) {
            TermsScreen(navController = navController)
        }
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
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onDateSelected = viewModel::selectDate,
                onMenuClick = onMenuClick,
                onAddNewEventClicked = viewModel::onAddNewEventClicked,
                onDialogDismiss = viewModel::onDialogDismiss,
                onSaveEvent = viewModel::saveSingleEvent,
                onUpdateEvent = viewModel::updateEvent,
                onShowDetailClicked = viewModel::onShowDetailClicked,
                onEditEventClicked = viewModel::onEditEventClicked,
                onConfirmDeleteClicked = viewModel::onConfirmDeleteClicked,
                onDeleteEvent = viewModel::deleteEvent,
                onMarkAsCompleted = viewModel::markHabitAsCompleted,
                onUnmarkAsCompleted = viewModel::unmarkHabitAsCompleted,
                onErrorShown = viewModel::onErrorShown,
                onSuccessMessageShown = viewModel::onSuccessMessageShown,
                onAchievementDialogDismissed = viewModel::onAchievementDialogDismissed,
                onCheckForUpdate = viewModel::checkForAppUpdate,
                onStartUpdate = { updateInfo ->
                    inAppUpdateManager.startUpdateFlow(updateInfo, context as Activity)
                },
                onCompleteUpdate = inAppUpdateManager::completeUpdate,
                onDismissUpdateDialog = viewModel::onUpdateDialogDismissed,
                onEventLongPressed = viewModel::onEventLongPressed,
                onEventClicked = viewModel::onEventClicked,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelectedEvents = viewModel::deleteSelectedEvents,
                onConfirmDeleteSelectedEvents = viewModel::confirmDeleteSelectedEvents
            )
        }

        composable(Screen.Templates.route) { 
            TemplatesScreen(navController = navController)
        }
        
        composable(Screen.Categories.route) { 
            CategoriesScreen(navController = navController)
        }

        composable(Screen.Stats.route) { 
            StatsScreen(navController = navController)
        }

        composable(Screen.Achievements.route) { 
            AchievementsScreen(navController = navController)
        }

        composable(Screen.Updates.route) { 
            UpdateNotesScreen(navController = navController)
        }

        composable(Screen.Legal.route) { 
            LegalScreen(navController = navController)
        }
    }
}
