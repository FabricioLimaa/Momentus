// ARQUIVO: ui/NewEventActivity.kt (CÓDIGO CORRIGIDO E COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModel
import br.com.fabriciolima.momentus.viewmodel.CalendarViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NewEventActivity : ComponentActivity() {

    // CORREÇÃO: Removendo o parâmetro 'application' que causava o erro de 'Type mismatch'
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory((application as MomentusApplication).repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialDateMillis = intent.getLongExtra("INITIAL_DATE", -1L)
        val initialDate = if (initialDateMillis != -1L) {
            Instant.ofEpochMilli(initialDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            LocalDate.now()
        }

        setContent {
            MomentusTheme {
                // Agora a propriedade 'todasAsRotinas' existe e o código compila.
                val todasAsRotinas by viewModel.todasAsRotinas.observeAsState(emptyList())

                NewEventScreen(
                    dataInicial = initialDate,
                    todasAsRotinas = todasAsRotinas,
                    onSave = { titulo, desc, data, inicio, fim, categoria ->
                        // Agora o método 'salvarEventoUnico' existe e o código compila.
                        viewModel.salvarEventoUnico(titulo, desc, data, inicio, fim, categoria)
                        Toast.makeText(this, "Evento criado!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventScreen(
    dataInicial: LocalDate,
    todasAsRotinas: List<Rotina>,
    onSave: (titulo: String, desc: String, data: LocalDate, inicio: LocalTime, fim: LocalTime, categoria: Rotina) -> Unit,
    onClose: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(dataInicial) }
    var horarioInicio by remember { mutableStateOf(LocalTime.now().withMinute(0).withSecond(0)) }
    var horarioTermino by remember { mutableStateOf(horarioInicio.plusHours(1)) }
    var rotinaSelecionada by remember { mutableStateOf(todasAsRotinas.firstOrNull()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerTermino by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    LaunchedEffect(horarioInicio) {
        if (!horarioTermino.isAfter(horarioInicio)) {
            horarioTermino = horarioInicio.plusHours(1)
        }
    }

    val isSaveEnabled = titulo.isNotBlank() && rotinaSelecionada != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Evento") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (horarioTermino.isAfter(horarioInicio)) {
                                onSave(titulo, descricao, data, horarioInicio, horarioTermino, rotinaSelecionada!!)
                            } else {
                                Toast.makeText(context, "O horário de término deve ser posterior ao de início.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text("Salvar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), isError = titulo.isBlank())

            OutlinedTextField(
                value = data.format(dateFormatter), onValueChange = {}, readOnly = true,
                label = { Text("Data") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Selecionar Data") }
            )

            OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = horarioInicio.format(timeFormatter), onValueChange = {}, readOnly = true,
                    label = { Text("Início") }, modifier = Modifier.weight(1f).clickable { showTimePickerInicio = true },
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Início") }
                )
                OutlinedTextField(
                    value = horarioTermino.format(timeFormatter), onValueChange = {}, readOnly = true,
                    label = { Text("Término") }, modifier = Modifier.weight(1f).clickable { showTimePickerTermino = true },
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Selecionar Término") }
                )
            }

            CategorySelector(
                rotinas = todasAsRotinas,
                selecionada = rotinaSelecionada,
                onSelected = { rotinaSelecionada = it }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        data = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerInicio) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                horarioInicio = LocalTime.of(hourOfDay, minute)
                showTimePickerInicio = false
            },
            horarioInicio.hour,
            horarioInicio.minute,
            true
        ).apply {
            setOnDismissListener { showTimePickerInicio = false }
            show()
        }
    }

    if (showTimePickerTermino) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                horarioTermino = LocalTime.of(hourOfDay, minute)
                showTimePickerTermino = false
            },
            horarioTermino.hour,
            horarioTermino.minute,
            true
        ).apply {
            setOnDismissListener { showTimePickerTermino = false }
            show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    rotinas: List<Rotina>,
    selecionada: Rotina?,
    onSelected: (Rotina) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selecionada?.nome ?: "Selecione a Categoria",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rotinas.forEach { rotina ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val cor = try { androidx.compose.ui.graphics.Color(Color.parseColor(rotina.cor)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(12.dp).background(cor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(rotina.nome)
                        }
                    },
                    onClick = {
                        onSelected(rotina)
                        expanded = false
                    }
                )
            }
        }
    }
}
