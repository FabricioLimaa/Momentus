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
import br.com.fabriciolima.momentus.widget.EVENT_ID_KEY
import br.com.fabriciolima.momentus.widget.OPEN_NEW_EVENT_DIALOG_KEY
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val firebaseAuth: FirebaseAuth by lazy { Firebase.auth }

    private val _isLoading = mutableStateOf(false)

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        _isLoading.value = false
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
                val isLoading by remember { _isLoading }
                LoginScreen(
                    isLoading = isLoading,
                    onGoogleSignInClick = {
                        _isLoading.value = true
                        signInWithGoogle()
                    },
                    onEmailSignInClick = { email, password ->
                        signInWithEmail(email, password)
                    },
                    onEmailSignUpClick = { email, password ->
                        signUpWithEmail(email, password)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            handleNavigation()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signInWithEmail(email: String, password: String) {
        _isLoading.value = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                Log.d("LoginActivity", "Email/Senha Auth SUCESSO. UID: ${it.user?.uid}")
                handleNavigation() 
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.w("LoginActivity", "Email/Senha Auth FALHA", e)
                val message = when (e) {
                    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
                    else -> "Falha na autenticação. Tente novamente."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun signUpWithEmail(email: String, password: String) {
        _isLoading.value = true
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                Log.d("LoginActivity", "Email/Senha Cadastro SUCESSO. UID: ${it.user?.uid}")
                handleNavigation()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.w("LoginActivity", "Email/Senha Cadastro FALHA", e)
                val message = when (e) {
                    is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso por outra conta."
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres."
                    else -> "Falha no cadastro. Tente novamente."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            _isLoading.value = false
            Log.w("LoginActivity", "Falha no login com Google: code=" + e.statusCode)
            Toast.makeText(this, "Falha ao obter conta Google. Tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {                
                Log.d("LoginActivity", "Firebase Auth SUCESSO. UID: ${it.user?.uid}")
                handleNavigation()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.e("LoginActivity", "Firebase Auth FALHA", e)
                Toast.makeText(this, "Falha na autenticação com Firebase. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleNavigation() {
        val openNewEvent = intent.getBooleanExtra(OPEN_NEW_EVENT_DIALOG_KEY, false)
        val eventId = intent.getStringExtra(EVENT_ID_KEY)

        val targetIntent = if (openNewEvent || eventId != null) {
            Intent(this, CalendarActivity::class.java)
        } else {
            Intent(this, SplashActivity::class.java)
        }

        intent.extras?.let { targetIntent.putExtras(it) }
        startActivity(targetIntent)
        finish()
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit,
    onEmailSignInClick: (String, String) -> Unit,
    onEmailSignUpClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    fun isEmailValid(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(): Boolean = password.length >= 6

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_logo_round),
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

            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotEmpty() && !isEmailValid()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha (mínimo 6 caracteres)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && !isPasswordValid()
            )
            TextButton(onClick = { 
                if (email.isEmpty() || !isEmailValid()) {
                    Toast.makeText(context, "Por favor, insira um e-mail válido.", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { Toast.makeText(context, "E-mail de recuperação enviado para $email", Toast.LENGTH_LONG).show() }
                    .addOnFailureListener { e ->
                        val message = when (e) {
                            is FirebaseAuthInvalidUserException -> "Nenhuma conta encontrada com este e-mail."
                            else -> "Falha ao enviar e-mail de recuperação."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show() 
                    }
            }, modifier = Modifier.align(Alignment.End)) {
                Text("Esqueceu a senha?")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onEmailSignInClick(email, password) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && isEmailValid() && isPasswordValid()
            ) {
                if(isLoading) {
                     CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Entrar")
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Não tem uma conta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { onEmailSignUpClick(email, password) },
                    enabled = !isLoading && isEmailValid() && isPasswordValid()
                ) {
                    Text("Crie uma aqui")
                }
            }
        }
    }
}