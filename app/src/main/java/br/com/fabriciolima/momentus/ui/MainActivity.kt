// ARQUIVO: ui/MainActivity.kt (CÓDIGO COMPLETO E REATORADO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.components.AppDrawer
import br.com.fabriciolima.momentus.ui.components.RotinaListItem
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.viewmodel.MainViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MomentusApplication).repository)
    }
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestScopes(Scope(CalendarScopes.CALENDAR)).build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MomentusTheme {
                val googleAccount by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(this)) }

                AppScaffold( // Chamamos nosso novo Composable que contém toda a tela
                    viewModel = viewModel,
                    googleAccount = googleAccount,
                    onNavigateToCalendar = { startActivity(Intent(this, CalendarActivity::class.java)) },
                    onNavigateToTemplates = { startActivity(Intent(this, TemplatesActivity::class.java)) },
                    onNavigateToStats = { startActivity(Intent(this, StatsActivity::class.java)) },
                    onLogout = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = Intent(this, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    viewModel: MainViewModel,
    googleAccount: GoogleSignInAccount?,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToStats: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val editorRotinaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // A atualização da lista já é gerenciada pelo LiveData, então não precisamos fazer nada aqui.
        }
    }

    // --- MODIFICAÇÃO: A estrutura principal da nossa tela agora é o ModalNavigationDrawer ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // O conteúdo do nosso menu lateral vem do Composable que criamos.
            AppDrawer(
                googleAccount = googleAccount,
                onCalendarClicked = onNavigateToCalendar,
                onTemplatesClicked = onNavigateToTemplates,
                onLogoutClicked = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        // O Scaffold é o layout principal da tela (barra de título, conteúdo, botão '+')
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Minhas Rotinas") },
                    navigationIcon = {
                        // O ícone de navegação agora abre o drawer.
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(painterResource(id = R.drawable.ic_menu), contentDescription = "Abrir Menu")
                        }
                    },
                    actions = {
                        // O ícone de estatísticas continua na barra de título.
                        IconButton(onClick = onNavigateToStats) {
                            Icon(painterResource(id = R.drawable.ic_statistics), contentDescription = "Estatísticas")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val intent = Intent(context, EditorRotinaComposeActivity::class.java)
                    editorRotinaLauncher.launch(intent)
                }) {
                    Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Adicionar Rotina")
                }
            }
        ) { paddingValues ->
            // O conteúdo da tela (a lista de rotinas) é o mesmo de antes.
            val rotinasComMetas by viewModel.rotinas.observeAsState(initial = emptyList())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (rotinasComMetas.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_list),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Nenhuma rotina cadastrada.", style = MaterialTheme.typography.titleMedium)
                        Text("Clique no '+' para começar", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rotinasComMetas, key = { it.rotina.id }) { rotinaComMeta ->
                            RotinaListItem(
                                rotinaComMeta = rotinaComMeta,
                                onItemClicked = {
                                    val intent = Intent(context, EditorRotinaComposeActivity::class.java)
                                    intent.putExtra("ROTINA_PARA_EDITAR", it.rotina)
                                    editorRotinaLauncher.launch(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}