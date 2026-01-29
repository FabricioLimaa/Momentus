package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.TermsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TermsActivity : ComponentActivity() {

    private val viewModel: TermsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                TermsScreen(
                    viewModel = viewModel,
                    onTermsAccepted = {
                        // Navega para a tela principal após aceitar
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TermsScreen(
    viewModel: TermsViewModel,
    onTermsAccepted: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val termsText = remember { context.assets.open("TERMS_AND_CONDITIONS.txt").bufferedReader().use { it.readText() } }
    val privacyText = remember { context.assets.open("PRIVACY_POLICY.txt").bufferedReader().use { it.readText() } }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isChecked = !isChecked }
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eu li e concordo com os Termos de Uso e a Política de Privacidade.")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.acceptTerms()
                                onTermsAccepted()
                            }
                        },
                        enabled = isChecked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aceitar e Continuar")
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Termos e Condições", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(termsText, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Política de Privacidade", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(privacyText, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
