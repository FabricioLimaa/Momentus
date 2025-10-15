package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import br.com.fabriciolima.momentus.ui.viewmodel.WidgetConfigurationViewModel
import br.com.fabriciolima.momentus.widget.EventWidgetStateKeys
import br.com.fabriciolima.momentus.widget.MomentusGlanceWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WidgetConfigurationActivity : ComponentActivity() {

    private val viewModel: WidgetConfigurationViewModel by viewModels()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MomentusTheme {
                val rotinas by viewModel.rotinas.collectAsState()
                ConfigurationScreen(rotinas = rotinas, onSave = { selectedIds ->
                    lifecycleScope.launch {
                        val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(this@WidgetConfigurationActivity, glanceId) { prefs ->
                            prefs[EventWidgetStateKeys.configuredRotinasKey] = selectedIds
                        }
                        MomentusGlanceWidget().update(this@WidgetConfigurationActivity, glanceId)
                    }
                    
                    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(Activity.RESULT_OK, resultValue)
                    finish()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(rotinas: List<Rotina>, onSave: (Set<String>) -> Unit) {
    val selectedRotinas = remember { mutableStateMapOf<String, Boolean>() }

    // Inicializa o mapa com todas as rotinas selecionadas por padrão
    if (rotinas.isNotEmpty() && selectedRotinas.isEmpty()) {
        rotinas.forEach { rotina ->
            selectedRotinas[rotina.id] = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configurar Widget") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Selecione as rotinas para exibir no widget:")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rotinas) {
                    rotina ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = selectedRotinas[rotina.id] ?: true,
                            onCheckedChange = { isChecked ->
                                selectedRotinas[rotina.id] = isChecked
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(rotina.nome)
                    }
                }
            }
            Button(
                onClick = { 
                    val selectedIds = selectedRotinas.filter { it.value }.keys
                    onSave(selectedIds)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
        }
    }
}
