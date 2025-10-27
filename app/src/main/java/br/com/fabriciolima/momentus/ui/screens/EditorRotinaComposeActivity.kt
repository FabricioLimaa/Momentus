package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.data.model.Category
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable

@AndroidEntryPoint
class EditorRotinaComposeActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryToEdit = getSerializable(intent, "CATEGORY_TO_EDIT", Category::class.java)

        setContent {
            MomentusTheme {
                EditorScreen(
                    initialCategory = categoryToEdit,
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onSave = { savedCategory ->
                        val resultIntent = Intent().putExtra("SAVED_CATEGORY", savedCategory)
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

// Função auxiliar para compatibilidade com versões antigas do Android
fun <T : Serializable?> getSerializable(intent: Intent, key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(key, clazz)
    } else {
        intent.getSerializableExtra(key) as? T
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialCategory: Category?,
    viewModel: MainViewModel?,
    onNavigateBack: () -> Unit,
    onSave: (Category) -> Unit
) {
    val context = LocalContext.current

    var nome by remember { mutableStateOf(initialCategory?.nome ?: "") }
    var descricao by remember { mutableStateOf(initialCategory?.descricao ?: "") }
    var tag by remember { mutableStateOf(initialCategory?.tag ?: "") }
    var duracao by remember { mutableStateOf(initialCategory?.duracaoPadraoMinutos?.toString() ?: "") }

    val cores = listOf(
        Color(0xFF3DDC84), Color(0xFF2A9371), Color(0xFF0A1A4A), Color(0xFF42A5F5),
        Color(0xFF7E57C2), Color(0xFFEC407A), Color(0xFFFF7043), Color(0xFF8D6E63)
    )
    val corInicial = initialCategory?.cor?.let { Color(android.graphics.Color.parseColor(it)) }
    var corSelecionada by remember { mutableStateOf(corInicial ?: cores.first()) }

    val isEditing = initialCategory != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Categoria" else "Nova Categoria") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (nome.isBlank()) {
                        Toast.makeText(context, "O nome da categoria é obrigatório.", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }
                    val duracaoMinutos = duracao.toIntOrNull() ?: 0

                    val categoryToSave = Category(
                        id = initialCategory?.id ?: java.util.UUID.randomUUID().toString(),
                        nome = nome,
                        duracaoPadraoMinutos = duracaoMinutos,
                        cor = String.format("#%06X", (0xFFFFFF and corSelecionada.toArgb())),
                        descricao = descricao.takeIf { it.isNotBlank() },
                        tag = tag.takeIf { it.isNotBlank() }
                    )

                    viewModel?.insertCategory(categoryToSave)
                    Toast.makeText(context, "Categoria salva!", Toast.LENGTH_SHORT).show()
                    onSave(categoryToSave)
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Salvar Categoria", tint = MaterialTheme.colorScheme.onSecondary)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
            }
            item {
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                OutlinedTextField(
                    value = tag, onValueChange = { tag = it },
                    label = { Text("Tag (ex: estudo, trabalho)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
            }
            item {
                OutlinedTextField(
                    value = duracao, onValueChange = { duracao = it },
                    label = { Text("Duração padrão (em minutos)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cor da Categoria", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ColorPicker(
                    cores = cores,
                    corSelecionada = corSelecionada,
                    onColorSelected = { corSelecionada = it }
                )
            }
        }
    }
}

@Composable
fun ColorPicker(
    cores: List<Color>,
    corSelecionada: Color,
    onColorSelected: (Color) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cores) { cor ->
            val isSelected = cor == corSelecionada
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cor)
                    .clickable { onColorSelected(cor) }
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Cor Selecionada",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    MomentusTheme {
        EditorScreen(
            initialCategory = null,
            viewModel = null,
            onNavigateBack = {},
            onSave = {}
        )
    }
}
