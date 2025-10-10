package br.com.fabriciolima.momentus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.fabriciolima.momentus.data.model.Rotina

val defaultColors = listOf(
    Color(0xFFF44336), Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFC107),
    Color(0xFFFFEB3B), Color(0xFFCDDC39), Color(0xFF8BC34A), Color(0xFF4CAF50),
    Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFFF2A905),
    Color(0xFFF2C84B), Color(0xFFD94141), Color(0xFFF27C38), Color(0xFFBCAAA4)
)

fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    category: Rotina?,
    onDismiss: () -> Unit,
    onConfirm: (id: String?, name: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.nome ?: "") }
    var selectedColor by remember { 
        mutableStateOf(category?.cor?.let { Color(android.graphics.Color.parseColor(it)) } ?: defaultColors.first())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (category == null) "Nova Categoria" else "Editar Categoria", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Cor", style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(defaultColors) {
                        color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { 
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar") 
                    }
                    Button(onClick = { onConfirm(category?.id, name, selectedColor.toHexString()) }) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
