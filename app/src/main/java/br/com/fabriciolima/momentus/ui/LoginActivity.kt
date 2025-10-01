// ARQUIVO: ui/LoginActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            navigateToMain() // Se já está logado, vai direto para o Calendário
            return
        }
        setContent {
            MomentusTheme {
                LoginScreen(
                    onLoginSuccess = { navigateToMain() }
                )
            }
        }
    }

    private fun navigateToMain() {
        // --- MODIFICAÇÃO: O destino agora é a CalendarActivity ---
        val intent = Intent(this, CalendarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // --- CORREÇÃO: Lógica de Login Centralizada ---
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Este é o novo jeito de lidar com o resultado do login do Google em Compose
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Sucesso!
                task.getResult(ApiException::class.java)
                Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Falha no login com Google", e)
                Toast.makeText(context, "Falha no login. Código: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Login cancelado.", Toast.LENGTH_SHORT).show()
        }
    }
    // --- FIM DA CORREÇÃO ---

    // Função para iniciar o fluxo de login
    Surface(modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Usa a cor de fundo do novo tema
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                // 1. Usamos o novo logo que você importou
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo do App",
                modifier = Modifier.size(120.dp) // Aumentamos um pouco o tamanho
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Textos de Boas-vindas
            Text("Bem-vindo ao Momentus", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Faça login para continuar", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))

            // Botão de Login com Google
            OutlinedButton( // 2. Trocamos para OutlinedButton para um visual mais leve
                onClick = {
                    isLoading = true
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50) // Bordas mais arredondadas
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified // Mantém a cor original do logo do Google
                )
                Text("Continuar com o Google", modifier = Modifier.padding(start = 16.dp))
            }

            // Divisor
            Row(modifier = Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f))
                Text("OU", modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(modifier = Modifier.weight(1f))
            }

            // Campos de Email e Senha
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Botão de Entrar
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                // 3. Usamos as cores primárias do nosso novo tema
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Entrar")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MomentusTheme {
        LoginScreen(onLoginSuccess = {})
    }
}