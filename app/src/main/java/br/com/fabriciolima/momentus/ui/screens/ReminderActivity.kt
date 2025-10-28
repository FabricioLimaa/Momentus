package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.notifications.AlarmReceiver
import br.com.fabriciolima.momentus.notifications.NotificationActionEntryPoint
import br.com.fabriciolima.momentus.notifications.NotificationActionReceiver
import br.com.fabriciolima.momentus.ui.theme.MomentusTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val eventId = intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_ID)
        val message = intent.getStringExtra(AlarmReceiver.EXTRA_MESSAGE)

        if (eventId == null || message == null) {
            finish()
            return
        }

        setContent {
            MomentusTheme {
                ReminderDialog(
                    eventName = message,
                    onSnooze = {
                        val snoozeIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                            action = NotificationActionReceiver.ACTION_SNOOZE
                            putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
                        }
                        sendBroadcast(snoozeIntent)
                        finish()
                    },
                    onComplete = {
                        val hiltEntryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            NotificationActionEntryPoint::class.java
                        )
                        val categoryRepository = hiltEntryPoint.categoryRepository()
                        MainScope().launch {
                            categoryRepository.markHabitAsCompleted(eventId)
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ReminderDialog(
    eventName: String,
    onSnooze: () -> Unit,
    onComplete: () -> Unit
) {
    Dialog(onDismissRequest = onSnooze) { // Adiar se o usuário dispensar o diálogo
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Lembrete de Rotina", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(eventName, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSnooze) {
                        Text("Adiar 15 min")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onComplete) {
                        Text("Concluir")
                    }
                }
            }
        }
    }
}
