package br.com.fabriciolima.momentus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mapeia o nome de um ícone (armazenado no banco de dados) para o seu ImageVector correspondente.
 */
@Composable
fun getIconForName(name: String?): ImageVector? {
    return when (name) {
        "Work" -> Icons.Default.Work
        "Home" -> Icons.Default.Home
        "Book" -> Icons.Default.MenuBook
        "Fitness" -> Icons.Default.FitnessCenter
        "Info" -> Icons.Default.Info
        else -> null
    }
}
