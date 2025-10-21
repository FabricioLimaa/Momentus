package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.util.GoogleAuthUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _isSigningIn = mutableStateOf(false)

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        _isSigningIn.value = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
             Toast.makeText(this, "Login com Google cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleAuthUtils.getGoogleSignInOptions(this)
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MomentusTheme {
                val isSigningIn by remember { _isSigningIn }
                LoginScreen(
                    isSigningIn = isSigningIn,
                    onGoogleSignInClick = {
                        _isSigningIn.value = true
                        signInWithGoogle()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Se o usuário já estiver logado, pule para a tela de carregamento/sincronização
        if (firebaseAuth.currentUser != null) {
            navigateToLoadingScreen()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            _isSigningIn.value = false
            Log.w("LoginActivity", "Falha no login com Google: code=" + e.statusCode)
            Toast.makeText(this, "Falha ao obter conta Google. Tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                Log.d("LoginActivity", "Firebase Auth SUCESSO. UID: ${it.user?.uid}")
                navigateToLoadingScreen()
            }
            .addOnFailureListener { e ->
                _isSigningIn.value = false
                Log.e("LoginActivity", "Firebase Auth FALHA", e)
                Toast.makeText(this, "Falha na autenticação com Firebase. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToLoadingScreen() {
        // Propaga a Intent original para a próxima tela
        val intent = Intent(this, SplashActivity::class.java).apply {
            // Copia os extras (como EVENT_ID_KEY) da Intent que iniciou a LoginActivity
            this@LoginActivity.intent.extras?.let { putExtras(it) }
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(isSigningIn: Boolean, onGoogleSignInClick: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()), // Permite rolagem
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo Momentus",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Momentus", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Organize suas metas, conquiste seu dia.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- Botão Google ---
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isSigningIn,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Logo do Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Entrar com Google", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            
            Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text("OU", modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            // --- Campos de E-mail e Senha ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation()
            )
            TextButton(onClick = { 
                Toast.makeText(context, "Funcionalidade não implementada.", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.align(Alignment.End)) {
                Text("Esqueceu a senha?")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Botão de Sign In com E-mail ---
            Button(
                onClick = { 
                    Toast.makeText(context, "Login com E-mail/Senha não implementado.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Entrar")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Não tem uma conta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { 
                    Toast.makeText(context, "Funcionalidade não implementada.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Crie uma aqui")
                }
            }
        }
    }
}