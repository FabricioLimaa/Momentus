// ARQUIVO: ui/CalendarActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentusTheme {
                CalendarScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    // O DatePickerState gerencia o estado do calendário, como a data selecionada.
    val datePickerState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendário") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Este é o componente de calendário do Material 3.
            DatePicker(
                state = datePickerState,
                modifier = Modifier.weight(1f),
                // Escondemos o botão de OK, pois vamos exibir os eventos do dia selecionado
                // diretamente abaixo do calendário no futuro.
                showModeToggle = false,
                title = null,
                headline = null
            )

            // TODO: Aqui, abaixo do DatePicker, nós adicionaremos a lista de eventos
            // para o dia que for selecionado no calendário.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    MomentusTheme {
        CalendarScreen()
    }
}