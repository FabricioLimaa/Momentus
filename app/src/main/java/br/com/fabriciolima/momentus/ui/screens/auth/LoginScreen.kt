package br.com.fabriciolima.momentus.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.ui.Screen
import br.com.fabriciolima.momentus.ui.viewmodel.LoginEvent
import br.com.fabriciolima.momentus.ui.viewmodel.LoginViewModel
import br.com.fabriciolima.momentus.ui.theme.EmeraldGreen
import br.com.fabriciolima.momentus.util.GoogleAuthUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            viewModel.handleGoogleSignInResult(data)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.NavigateToMain -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is LoginEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailFields by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userPreferences) {
        uiState.userPreferences?.let { prefs ->
            if (prefs.rememberMe) {
                email = prefs.email
                rememberMe = true
                showEmailFields = true
            }
        }
    }

    fun isEmailValid(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(): Boolean = password.length >= 6

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height * 0.7f)
                    quadraticBezierTo(width * 0.5f, height * 0.85f, width, height * 0.65f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path = path, color = EmeraldGreen.copy(alpha = 0.05f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_logo_round),
                    contentDescription = "Logo Momentus",
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Momentus",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Organize suas metas, conquiste seu dia.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(64.dp))

                Button(
                    onClick = {
                        viewModel.setLoading(true)
                        val gso = GoogleAuthUtils.getGoogleSignInOptions(context)
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                ) {
                    if (uiState.isLoading && !showEmailFields) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Entrar com Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showEmailFields = !showEmailFields },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Entrar com E-mail", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                AnimatedVisibility(visible = showEmailFields) {
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen) },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it; viewModel.updatePreferences(email, it) }, colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen))
                            Text("Lembrar-me", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { navController.navigate(Screen.ForgotPassword.route) }) {
                                Text("Esqueceu a senha?", color = EmeraldGreen)
                            }
                        }

                        Button(
                            onClick = { viewModel.signInWithEmail(email, password, rememberMe) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !uiState.isLoading && isEmailValid() && isPasswordValid(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            if(uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Entrar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Comece gratuitamente", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "+10.000 pessoas já estão transformando\nsuas rotinas.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), textAlign = TextAlign.Center, lineHeight = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Não tem uma conta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { navController.navigate(Screen.SignUp.route) }) {
                        Text("Crie uma aqui", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
            }

            val versionName = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: Exception) { "N/A" }

            Text(text = "Versão: $versionName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp))
        }
    }
}
