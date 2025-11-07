package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.lifecycleScope
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import br.com.fabriciolima.momentus.data.repository.UserRepository
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

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
                val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = null)

                userPreferences?.let {
                    LoginScreen(
                        isLoading = isLoading,
                        userPreferences = it,
                        onGoogleSignInClick = {
                            _isLoading.value = true
                            signInWithGoogle()
                        },
                        onEmailSignInClick = { email, password, rememberMe ->
                            signInWithEmail(email, password, rememberMe)
                        },
                        onUpdatePreferences = { email, rememberMe ->
                            lifecycleScope.launch {
                                userPreferencesRepository.updateUserEmail(email)
                                userPreferencesRepository.updateRememberMe(rememberMe)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Navega automaticamente se o usuário já estiver logado.
        // A lógica de "Lembrar-me" é tratada na UI.
        if (firebaseAuth.currentUser != null) {
            handleNavigation()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signInWithEmail(email: String, password: String, rememberMe: Boolean) {
        _isLoading.value = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    lifecycleScope.launch {
                        if (rememberMe) {
                            userPreferencesRepository.updateUserEmail(email)
                            userPreferencesRepository.updateRememberMe(true)
                        } else {
                            userPreferencesRepository.clear()
                        }
                        userRepository.createOrUpdateUser(firebaseUser)
                        Log.d("LoginActivity", "Email/Senha Auth SUCESSO. UID: ${firebaseUser.uid}")
                        handleNavigation()
                    }
                } else {
                    _isLoading.value = false
                    Toast.makeText(this, "Falha ao obter dados do usuário do Firebase.", Toast.LENGTH_LONG).show()
                }
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
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    lifecycleScope.launch { // Usando lifecycleScope para a coroutine
                        userRepository.createOrUpdateUser(firebaseUser)
                        // Para o login com Google, podemos simplesmente limpar as prefs de e-mail.
                        userPreferencesRepository.clear()
                        Log.d("LoginActivity", "Firebase Auth SUCESSO. UID: ${firebaseUser.uid}")
                        handleNavigation()
                    }
                } else {
                     _isLoading.value = false
                    Toast.makeText(this, "Falha ao obter dados do usuário do Firebase.", Toast.LENGTH_LONG).show()
                }
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
    userPreferences: br.com.fabriciolima.momentus.data.repository.UserPreferences,
    onGoogleSignInClick: () -> Unit,
    onEmailSignInClick: (String, String, Boolean) -> Unit,
    onUpdatePreferences: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailFields by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    LaunchedEffect(userPreferences) {
        if (userPreferences.rememberMe) {
            email = userPreferences.email
            rememberMe = true
            showEmailFields = true
        }
    }

    fun isEmailValid(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(): Boolean = password.length >= 6

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
                    .padding(bottom = 32.dp), // Espaço extra para o texto da versão
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

                // --- Botão Google ---
                Button(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isLoading && !showEmailFields) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Logo do Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text("Entrar com Google", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- Botão E-mail ---
                Button(
                    onClick = { showEmailFields = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Ícone de E-mail",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Entrar com E-mail", color = MaterialTheme.colorScheme.onPrimary)
                }

                // --- Campos de E-mail e Senha (aparecem ao clicar no botão) ---
                AnimatedVisibility(visible = showEmailFields) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            label = { Text("Senha") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = password.isNotEmpty() && !isPasswordValid()
                        )
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { 
                                    rememberMe = it
                                    onUpdatePreferences(email, it)
                                }
                            )
                            Text("Lembrar-me")
                        }
                        TextButton(
                            onClick = { 
                                context.startActivity(Intent(context, ForgotPasswordActivity::class.java))
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Esqueceu a senha?")
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onEmailSignInClick(email, password, rememberMe) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !isLoading && isEmailValid() && isPasswordValid()
                        ) {
                            if(isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Entrar")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Não tem uma conta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { 
                        val intent = Intent(context, SignUpActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Text("Crie uma aqui")
                    }
                }
            }

            val versionName = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: Exception) {
                "N/A"
            }

            Text(
                text = "Versão: $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
            )
        }
    }
}